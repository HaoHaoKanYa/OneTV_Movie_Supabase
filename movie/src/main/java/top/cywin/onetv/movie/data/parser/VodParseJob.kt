package top.cywin.onetv.movie.data.parser

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.callback.ParseCallback
import top.cywin.onetv.movie.data.models.VodParse
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 播放解析任务 (参考OneMoVie ParseJob)
 * 支持多种解析方式：嗅探、JSON、WebView、自定义
 */
class VodParseJob(private val callback: ParseCallback) {
    
    companion object {
        private const val TAG = "VodParseJob"
    }
    
    private val executor = Executors.newFixedThreadPool(2)
    private val infinite = Executors.newCachedThreadPool()
    private var parse: VodParse? = null
    private var job: Job? = null
    
    /**
     * 开始解析 (参考OneMoVie start方法)
     */
    fun start(result: VodResult, useParse: Boolean): VodParseJob {
        setParse(result, useParse)
        execute(result)
        return this
    }
    
    /**
     * 设置解析器 (参考OneMoVie setParse)
     */
    private fun setParse(result: VodResult, useParse: Boolean) {
        when {
            useParse -> parse = VodConfigManager.getInstance().getParse()
            result.playUrl.startsWith("json:") -> {
                parse = VodParse.get(1, result.playUrl.substring(5))
            }
            result.playUrl.startsWith("parse:") -> {
                parse = VodConfigManager.getInstance().getParse(result.playUrl.substring(6))
            }
            else -> parse = VodParse.get(0, result.playUrl)
        }
        
        parse?.setHeader(result.header)
        parse?.setClick(getClick(result))
    }
    
    /**
     * 获取点击地址
     */
    private fun getClick(result: VodResult): String {
        // 这里可以根据需要实现点击地址逻辑
        return ""
    }
    
    /**
     * 执行解析
     */
    private fun execute(result: VodResult) {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                doInBackground(result.key, result.playUrl, result.flag)
            } catch (e: Exception) {
                Log.e(TAG, "解析失败", e)
                callback.onParseError("解析失败: ${e.message}")
            }
        }
    }
    
    /**
     * 执行解析 (参考OneMoVie doInBackground)
     */
    private suspend fun doInBackground(key: String, webUrl: String, flag: String) {
        val currentParse = parse ?: return
        
        when (currentParse.type) {
            0 -> startWebParse(key, currentParse, webUrl) // 嗅探解析
            1 -> jsonParse(currentParse, webUrl, true)    // JSON解析
            2 -> webViewParse(currentParse, webUrl)       // WebView解析
            3 -> customParse(currentParse, webUrl)        // 自定义解析
            else -> callback.onParseError("不支持的解析类型: ${currentParse.type}")
        }
    }
    
    /**
     * 嗅探解析 (参考OneMoVie startWebParse)
     */
    private suspend fun startWebParse(key: String, parse: VodParse, webUrl: String) {
        try {
            // 如果是神解析器，直接返回原地址
            if (parse.isGod()) {
                callback.onParseSuccess(webUrl, parse.getAllHeaders())
                return
            }
            
            // 检查是否为直链
            if (isDirectUrl(webUrl)) {
                callback.onParseSuccess(webUrl, parse.getAllHeaders())
                return
            }
            
            // 使用解析器解析
            jsonParse(parse, webUrl, true)
            
        } catch (e: Exception) {
            Log.e(TAG, "嗅探解析失败", e)
            callback.onParseError("嗅探解析失败: ${e.message}")
        }
    }
    
    /**
     * JSON解析 (参考OneMoVie jsonParse)
     */
    private suspend fun jsonParse(parse: VodParse, webUrl: String, redirect: Boolean) {
        try {
            val parseUrl = parse.getParseUrl(URLEncoder.encode(webUrl, "UTF-8"))
            
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
                
            val request = Request.Builder()
                .url(parseUrl)
                .apply {
                    parse.getAllHeaders().forEach { (key, value) ->
                        header(key, value)
                    }
                }
                .build()
                
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                parseJsonResponse(responseBody, parse)
            } else {
                callback.onParseError("解析请求失败: ${response.code}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "JSON解析失败", e)
            callback.onParseError("JSON解析失败: ${e.message}")
        }
    }
    
    /**
     * 解析JSON响应
     */
    private fun parseJsonResponse(responseBody: String, parse: VodParse) {
        try {
            val jsonObject = Json.parseToJsonElement(responseBody).jsonObject
            val playUrl = jsonObject["url"]?.jsonPrimitive?.content
            
            if (!playUrl.isNullOrEmpty()) {
                callback.onParseSuccess(playUrl, parse.getAllHeaders())
            } else {
                // 尝试其他可能的字段
                val alternativeFields = listOf("link", "src", "video", "m3u8")
                var found = false
                
                for (field in alternativeFields) {
                    val url = jsonObject[field]?.jsonPrimitive?.content
                    if (!url.isNullOrEmpty()) {
                        callback.onParseSuccess(url, parse.getAllHeaders())
                        found = true
                        break
                    }
                }
                
                if (!found) {
                    callback.onParseError("解析失败：未找到播放地址")
                }
            }
        } catch (e: Exception) {
            // 如果不是JSON格式，尝试正则提取
            extractUrlFromText(responseBody, parse)
        }
    }
    
    /**
     * 从文本中提取播放地址
     */
    private fun extractUrlFromText(text: String, parse: VodParse) {
        val patterns = listOf(
            "\"url\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"link\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "\"src\"\\s*:\\s*\"([^\"]+)\"".toRegex(),
            "player_aaaa\\s*=\\s*\"([^\"]+)\"".toRegex(),
            "var\\s+urls\\s*=\\s*\"([^\"]+)\"".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val url = match.groupValues[1].replace("\\/", "/")
                callback.onParseSuccess(url, parse.getAllHeaders())
                return
            }
        }
        
        callback.onParseError("解析失败：未找到播放地址")
    }
    
    /**
     * WebView解析 (参考OneMoVie webViewParse)
     */
    private suspend fun webViewParse(parse: VodParse, webUrl: String) {
        // WebView解析需要在主线程中进行，这里暂时使用JSON解析替代
        jsonParse(parse, webUrl, true)
    }
    
    /**
     * 自定义解析 (参考OneMoVie customParse)
     */
    private suspend fun customParse(parse: VodParse, webUrl: String) {
        // 自定义解析逻辑，这里暂时使用JSON解析替代
        jsonParse(parse, webUrl, true)
    }
    
    /**
     * 判断是否为直链
     */
    private fun isDirectUrl(url: String): Boolean {
        val directExtensions = listOf(".mp4", ".m3u8", ".flv", ".avi", ".mkv", ".ts", ".mov", ".wmv")
        return directExtensions.any { url.contains(it, ignoreCase = true) } ||
               (url.startsWith("http") && !url.contains("url=") && !url.contains("v="))
    }
    
    /**
     * 取消解析
     */
    fun cancel() {
        job?.cancel()
    }
}

/**
 * 解析结果数据类
 */
data class VodResult(
    val key: String,
    val playUrl: String,
    val flag: String,
    val header: Map<String, String> = emptyMap()
)
