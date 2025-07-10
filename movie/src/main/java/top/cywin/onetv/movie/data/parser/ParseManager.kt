package top.cywin.onetv.movie.data.parser

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import top.cywin.onetv.movie.data.models.VodParse
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 解析管理器 - 基于OneMoVie架构的多解析器系统
 * 支持嗅探、JSON、WebView等多种解析方式
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class ParseManager() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    /**
     * 解析结果数据类
     */
    data class ParseResult(
        val success: Boolean,
        val playUrl: String,
        val headers: Map<String, String> = emptyMap(),
        val error: String? = null
    ) {
        companion object {
            fun success(playUrl: String, headers: Map<String, String> = emptyMap()): ParseResult {
                return ParseResult(true, playUrl, headers)
            }
            
            fun failure(error: String): ParseResult {
                return ParseResult(false, "", error = error)
            }
        }
    }
    
    /**
     * 解析播放地址 (主入口)
     */
    suspend fun parsePlayUrl(
        url: String,
        parse: VodParse?,
        flag: String = ""
    ): String {
        return try {
            when {
                // 直链播放
                isDirectUrl(url) -> url
                
                // 使用解析器解析
                parse != null -> {
                    val result = parseWithParser(url, parse, flag)
                    if (result.success) result.playUrl else url
                }
                
                // 尝试嗅探解析
                else -> {
                    val result = sniffPlayUrl(url)
                    if (result.success) result.playUrl else url
                }
            }
        } catch (e: Exception) {
            Log.e("ParseManager", "解析失败: ${e.message}")
            url // 解析失败返回原地址
        }
    }
    
    /**
     * 使用解析器解析地址
     */
    private suspend fun parseWithParser(
        url: String,
        parse: VodParse,
        flag: String
    ): ParseResult {
        return withContext(Dispatchers.IO) {
            try {
                when (parse.type) {
                    0 -> parseWebUrl(url, parse, flag) // Web解析
                    1 -> parseJsonUrl(url, parse, flag) // JSON解析
                    2 -> parseRegexUrl(url, parse, flag) // 正则解析
                    3 -> parseXPathUrl(url, parse, flag) // XPath解析
                    else -> ParseResult.failure("不支持的解析器类型: ${parse.type}")
                }
            } catch (e: Exception) {
                ParseResult.failure("解析器执行失败: ${e.message}")
            }
        }
    }
    
    /**
     * Web解析 (类型0)
     */
    private suspend fun parseWebUrl(url: String, parse: VodParse, flag: String): ParseResult {
        try {
            val parseUrl = buildParseUrl(parse.url, url, flag)
            
            val requestBuilder = Request.Builder().url(parseUrl)
            
            // 添加解析器配置的请求头
            parse.header.forEach { (key, value) ->
                requestBuilder.header(key, value)
            }
            
            // 添加默认请求头
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            requestBuilder.header("Referer", "https://www.baidu.com/")
            
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val extractedUrl = extractPlayUrlFromResponse(responseBody)
                if (extractedUrl.isNotEmpty() && extractedUrl != responseBody) {
                    return ParseResult.success(extractedUrl)
                }
            }
            
            return ParseResult.failure("Web解析未找到播放地址")
            
        } catch (e: Exception) {
            return ParseResult.failure("Web解析失败: ${e.message}")
        }
    }
    
    /**
     * JSON解析 (类型1)
     */
    private suspend fun parseJsonUrl(url: String, parse: VodParse, flag: String): ParseResult {
        try {
            val requestBody = Json.encodeToString(
                kotlinx.serialization.json.buildJsonObject {
                    put("url", kotlinx.serialization.json.JsonPrimitive(url))
                    put("flag", kotlinx.serialization.json.JsonPrimitive(flag))
                    parse.ext.forEach { (key, value) ->
                        put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
                    }
                }
            )
            
            val request = Request.Builder()
                .url(parse.url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            parse.header.forEach { (key, value) ->
                request.newBuilder().header(key, value)
            }
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                
                // 尝试多种可能的字段名
                val possibleFields = listOf("url", "link", "src", "play_url", "video_url", "m3u8")
                for (field in possibleFields) {
                    val playUrl = jsonResponse[field]?.jsonPrimitive?.content
                    if (!playUrl.isNullOrEmpty() && isValidPlayUrl(playUrl)) {
                        return ParseResult.success(playUrl)
                    }
                }
            }
            
            return ParseResult.failure("JSON解析未找到播放地址")
            
        } catch (e: Exception) {
            return ParseResult.failure("JSON解析失败: ${e.message}")
        }
    }
    
    /**
     * 正则解析 (类型2)
     */
    private suspend fun parseRegexUrl(url: String, parse: VodParse, flag: String): ParseResult {
        try {
            val parseUrl = buildParseUrl(parse.url, url, flag)
            
            val request = Request.Builder()
                .url(parseUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                // 从扩展配置中获取正则表达式
                val regexPattern = parse.ext["regex"]?.toString()
                if (!regexPattern.isNullOrEmpty()) {
                    val regex = regexPattern.toRegex()
                    val match = regex.find(responseBody)
                    if (match != null && match.groupValues.size > 1) {
                        val playUrl = match.groupValues[1].replace("\\/", "/")
                        if (isValidPlayUrl(playUrl)) {
                            return ParseResult.success(playUrl)
                        }
                    }
                }
            }
            
            return ParseResult.failure("正则解析未找到播放地址")
            
        } catch (e: Exception) {
            return ParseResult.failure("正则解析失败: ${e.message}")
        }
    }
    
    /**
     * XPath解析 (类型3)
     */
    private suspend fun parseXPathUrl(url: String, parse: VodParse, flag: String): ParseResult {
        // XPath解析需要HTML解析库，这里提供基础实现
        return ParseResult.failure("XPath解析暂未实现")
    }
    
    /**
     * 嗅探解析 (无解析器时使用)
     */
    private suspend fun sniffPlayUrl(url: String): ParseResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                val extractedUrl = extractPlayUrlFromResponse(responseBody)
                if (extractedUrl.isNotEmpty() && isValidPlayUrl(extractedUrl)) {
                    ParseResult.success(extractedUrl)
                } else {
                    ParseResult.failure("嗅探未找到有效播放地址")
                }
            } else {
                ParseResult.failure("嗅探请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            ParseResult.failure("嗅探解析失败: ${e.message}")
        }
    }
    
    /**
     * 构建解析URL
     */
    private fun buildParseUrl(parseUrl: String, videoUrl: String, flag: String): String {
        return parseUrl
            .replace("{url}", URLEncoder.encode(videoUrl, "UTF-8"))
            .replace("{flag}", URLEncoder.encode(flag, "UTF-8"))
            .replace("{{url}}", URLEncoder.encode(videoUrl, "UTF-8"))
    }
    
    /**
     * 从响应中提取播放地址
     */
    private fun extractPlayUrlFromResponse(response: String): String {
        // 常见的播放地址提取规则
        val patterns = listOf(
            "\"url\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"link\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"src\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"video\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "player_aaaa\\s*=\\s*\"([^\"]+)\"".toRegex(),
            "var\\s+urls\\s*=\\s*\"([^\"]+)\"".toRegex(),
            "source\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "file\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "https?://[^\\s\"'<>]+\\.(?:mp4|m3u8|flv|avi|mkv|ts)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(response)
            if (match != null) {
                val url = if (match.groupValues.size > 1) {
                    match.groupValues[1]
                } else {
                    match.value
                }
                val cleanUrl = url.replace("\\/", "/")
                if (isValidPlayUrl(cleanUrl)) {
                    return cleanUrl
                }
            }
        }
        
        return ""
    }
    
    /**
     * 判断是否为直链
     */
    private fun isDirectUrl(url: String): Boolean {
        val directExtensions = listOf(".mp4", ".m3u8", ".flv", ".avi", ".mkv", ".ts", ".mov")
        return directExtensions.any { url.contains(it, ignoreCase = true) } ||
               (url.startsWith("http") && !url.contains("url=") && !url.contains("v="))
    }
    
    /**
     * 验证播放地址有效性
     */
    private fun isValidPlayUrl(url: String): Boolean {
        if (url.isEmpty() || url.length < 10) return false
        
        return url.startsWith("http") && (
            url.contains(".mp4") ||
            url.contains(".m3u8") ||
            url.contains(".flv") ||
            url.contains(".avi") ||
            url.contains(".mkv") ||
            url.contains(".ts") ||
            url.contains("video") ||
            url.contains("play")
        )
    }
}
