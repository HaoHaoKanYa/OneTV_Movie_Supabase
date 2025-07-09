package top.cywin.onetv.movie.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.cywin.onetv.movie.data.models.VodParse
import top.cywin.onetv.movie.data.models.VodSite
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * 播放地址解析器接口 (参考OneMoVie解析架构)
 */
interface VodParser {
    
    /**
     * 解析器类型
     */
    enum class Type(val value: Int) {
        SNIFF(0),    // 嗅探解析
        JSON(1),     // JSON解析
        WEBVIEW(2),  // WebView解析
        CUSTOM(3)    // 自定义解析
    }
    
    /**
     * 解析播放地址
     */
    suspend fun parse(
        url: String,
        site: VodSite,
        parse: VodParse? = null,
        flag: String = ""
    ): ParseResult
    
    /**
     * 获取解析器类型
     */
    fun getType(): Type
    
    /**
     * 是否支持该URL
     */
    fun canParse(url: String, site: VodSite): Boolean
}

/**
 * 解析结果
 */
data class ParseResult(
    val success: Boolean,
    val playUrl: String = "",
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String = "",
    val referer: String = "",
    val error: String? = null,
    val parseTime: Long = 0L
) {
    companion object {
        fun success(
            playUrl: String,
            headers: Map<String, String> = emptyMap(),
            userAgent: String = "",
            referer: String = "",
            parseTime: Long = 0L
        ) = ParseResult(
            success = true,
            playUrl = playUrl,
            headers = headers,
            userAgent = userAgent,
            referer = referer,
            parseTime = parseTime
        )
        
        fun failure(error: String, parseTime: Long = 0L) = ParseResult(
            success = false,
            error = error,
            parseTime = parseTime
        )
    }
}

/**
 * 解析器管理器 (参考OneMoVie ParseJob)
 */
class VodParserManager {
    
    private val parsers = mutableListOf<VodParser>()
    private val parseTimeout = 30_000L // 30秒超时
    
    init {
        // 注册默认解析器
        registerParser(SniffParser())
        registerParser(JsonParser())
        registerParser(WebViewParser())
        registerParser(CustomParser())
    }
    
    /**
     * 注册解析器
     */
    fun registerParser(parser: VodParser) {
        parsers.add(parser)
    }
    
    /**
     * 解析播放地址
     */
    suspend fun parsePlayUrl(
        url: String,
        site: VodSite,
        parse: VodParse? = null,
        flag: String = ""
    ): ParseResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        
        // 1. 如果指定了解析器，优先使用
        if (parse != null) {
            val parser = findParserByType(parse.type)
            if (parser != null) {
                val result = withTimeoutOrNull(parseTimeout) {
                    parser.parse(url, site, parse, flag)
                }
                if (result != null && result.success) {
                    return@withContext result.copy(parseTime = System.currentTimeMillis() - startTime)
                }
            }
        }
        
        // 2. 尝试所有可用的解析器
        for (parser in parsers) {
            if (parser.canParse(url, site)) {
                try {
                    val result = withTimeoutOrNull(parseTimeout) {
                        parser.parse(url, site, parse, flag)
                    }
                    
                    if (result != null && result.success) {
                        return@withContext result.copy(parseTime = System.currentTimeMillis() - startTime)
                    }
                } catch (e: Exception) {
                    // 继续尝试下一个解析器
                    continue
                }
            }
        }
        
        // 3. 所有解析器都失败
        ParseResult.failure(
            error = "所有解析器都无法解析该地址",
            parseTime = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * 根据类型查找解析器
     */
    private fun findParserByType(type: Int): VodParser? {
        return parsers.find { it.getType().value == type }
    }
    
    /**
     * 获取支持的解析器列表
     */
    fun getSupportedParsers(url: String, site: VodSite): List<VodParser> {
        return parsers.filter { it.canParse(url, site) }
    }
}

/**
 * 嗅探解析器 (参考OneMoVie SniffParser)
 */
class SniffParser : VodParser {

    companion object {
        // 视频文件扩展名
        private val VIDEO_EXTENSIONS = setOf(
            ".m3u8", ".mp4", ".flv", ".avi", ".mkv", ".mov", ".wmv", ".webm", ".ts"
        )

        // 视频URL模式
        private val VIDEO_PATTERNS = listOf(
            Pattern.compile(".*\\.(m3u8|mp4|flv|avi|mkv|mov|wmv|webm|ts)(\\?.*)?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/(live|stream|play|video)/.*", Pattern.CASE_INSENSITIVE)
        )

        // 需要进一步解析的URL模式
        private val PARSE_PATTERNS = listOf(
            Pattern.compile(".*\\.php\\?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/play/.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/video/.*", Pattern.CASE_INSENSITIVE)
        )
    }

    override suspend fun parse(
        url: String,
        site: VodSite,
        parse: VodParse?,
        flag: String
    ): ParseResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        try {
            // 1. 直接返回原始URL，适用于直链
            if (isDirectUrl(url)) {
                return@withContext ParseResult.success(
                    playUrl = url,
                    parseTime = System.currentTimeMillis() - startTime
                )
            }

            // 2. 尝试HTTP嗅探
            val sniffResult = sniffHttpUrl(url, site)
            if (sniffResult != null) {
                return@withContext ParseResult.success(
                    playUrl = sniffResult.playUrl,
                    headers = sniffResult.headers,
                    userAgent = sniffResult.userAgent,
                    referer = sniffResult.referer,
                    parseTime = System.currentTimeMillis() - startTime
                )
            }

            // 3. 尝试从HTML页面提取视频链接
            val extractResult = extractFromHtml(url, site)
            if (extractResult != null) {
                return@withContext ParseResult.success(
                    playUrl = extractResult,
                    parseTime = System.currentTimeMillis() - startTime
                )
            }

            ParseResult.failure(
                error = "无法嗅探到有效的播放地址",
                parseTime = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            ParseResult.failure(
                error = "嗅探解析失败: ${e.message}",
                parseTime = System.currentTimeMillis() - startTime
            )
        }
    }
    
    override fun getType(): VodParser.Type = VodParser.Type.SNIFF

    override fun canParse(url: String, site: VodSite): Boolean {
        return true // 嗅探解析器支持所有URL
    }

    /**
     * 判断是否为直链
     */
    private fun isDirectUrl(url: String): Boolean {
        return VIDEO_PATTERNS.any { it.matcher(url).matches() }
    }

    /**
     * HTTP嗅探
     */
    private suspend fun sniffHttpUrl(url: String, site: VodSite): SniffResult? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.instanceFollowRedirects = true

            val responseCode = connection.responseCode

            // 检查重定向
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                if (location != null && isDirectUrl(location)) {
                    return@withContext SniffResult(
                        playUrl = location,
                        headers = mapOf("Referer" to url),
                        userAgent = site.getUserAgent()
                    )
                }
            }

            // 检查Content-Type
            val contentType = connection.contentType?.lowercase()
            if (contentType != null) {
                when {
                    contentType.contains("video/") -> {
                        return@withContext SniffResult(
                            playUrl = url,
                            headers = mapOf("Referer" to site.api),
                            userAgent = site.getUserAgent()
                        )
                    }
                    contentType.contains("application/vnd.apple.mpegurl") -> {
                        return@withContext SniffResult(
                            playUrl = url,
                            headers = mapOf("Referer" to site.api),
                            userAgent = site.getUserAgent()
                        )
                    }
                }
            }

            connection.disconnect()
            null

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从HTML页面提取视频链接
     */
    private suspend fun extractFromHtml(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)

            val html = connection.inputStream.bufferedReader().use { it.readText() }

            // 常见的视频链接提取模式
            val patterns = listOf(
                Pattern.compile("\"(https?://[^\"]*\\.m3u8[^\"]*)\""),
                Pattern.compile("'(https?://[^']*\\.m3u8[^']*)'"),
                Pattern.compile("src=\"(https?://[^\"]*\\.(mp4|flv|avi|mkv)[^\"]*)\""),
                Pattern.compile("url:\"(https?://[^\"]*\\.(m3u8|mp4|flv)[^\"]*)\""),
                Pattern.compile("file:\"(https?://[^\"]*\\.(m3u8|mp4|flv)[^\"]*)\"")
            )

            for (pattern in patterns) {
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val videoUrl = matcher.group(1)
                    if (videoUrl != null && isDirectUrl(videoUrl)) {
                        return@withContext videoUrl
                    }
                }
            }

            connection.disconnect()
            null

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 嗅探结果数据类
     */
    private data class SniffResult(
        val playUrl: String,
        val headers: Map<String, String> = emptyMap(),
        val userAgent: String = "",
        val referer: String = ""
    )
}

/**
 * JSON解析器
 */
class JsonParser : VodParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun parse(
        url: String,
        site: VodSite,
        parse: VodParse?,
        flag: String
    ): ParseResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        try {
            // 1. 如果有解析器配置，使用解析器API
            if (parse != null && parse.url.isNotEmpty()) {
                val parseResult = parseWithApi(url, parse, site)
                if (parseResult != null) {
                    return@withContext ParseResult.success(
                        playUrl = parseResult,
                        parseTime = System.currentTimeMillis() - startTime
                    )
                }
            }

            // 2. 直接请求URL获取JSON
            val jsonResult = parseDirectJson(url, site)
            if (jsonResult != null) {
                return@withContext ParseResult.success(
                    playUrl = jsonResult,
                    parseTime = System.currentTimeMillis() - startTime
                )
            }

            ParseResult.failure(
                error = "JSON解析未找到有效播放地址",
                parseTime = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            ParseResult.failure(
                error = "JSON解析失败: ${e.message}",
                parseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun getType(): VodParser.Type = VodParser.Type.JSON

    override fun canParse(url: String, site: VodSite): Boolean {
        return url.contains("json") || url.contains("api") || url.contains("parse")
    }

    /**
     * 使用解析器API解析
     */
    private suspend fun parseWithApi(url: String, parse: VodParse, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            val parseUrl = "${parse.url}?url=$url"
            val connection = URL(parseUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonElement = json.parseToJsonElement(response)

            // 尝试不同的JSON结构
            val playUrl = when {
                jsonElement.jsonObject.containsKey("url") ->
                    jsonElement.jsonObject["url"]?.jsonPrimitive?.content
                jsonElement.jsonObject.containsKey("data") -> {
                    val data = jsonElement.jsonObject["data"]
                    data?.jsonObject?.get("url")?.jsonPrimitive?.content
                }
                jsonElement.jsonObject.containsKey("play_url") ->
                    jsonElement.jsonObject["play_url"]?.jsonPrimitive?.content
                jsonElement.jsonObject.containsKey("video") ->
                    jsonElement.jsonObject["video"]?.jsonPrimitive?.content
                else -> null
            }

            connection.disconnect()
            playUrl

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 直接解析JSON响应
     */
    private suspend fun parseDirectJson(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            // 尝试解析为JSON
            val jsonElement = json.parseToJsonElement(response)

            // 常见的播放地址字段
            val urlFields = listOf("url", "play_url", "video", "src", "file", "link", "stream")

            for (field in urlFields) {
                val playUrl = jsonElement.jsonObject[field]?.jsonPrimitive?.content
                if (playUrl != null && playUrl.startsWith("http")) {
                    return@withContext playUrl
                }
            }

            connection.disconnect()
            null

        } catch (e: Exception) {
            null
        }
    }
}

/**
 * WebView解析器 (需要在主线程中使用WebView)
 */
class WebViewParser : VodParser {

    override suspend fun parse(
        url: String,
        site: VodSite,
        parse: VodParse?,
        flag: String
    ): ParseResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        try {
            // WebView解析需要在主线程中进行，这里提供基础的HTML解析
            val htmlContent = fetchHtmlContent(url, site)
            if (htmlContent != null) {
                val extractedUrl = extractVideoUrlFromHtml(htmlContent)
                if (extractedUrl != null) {
                    return@withContext ParseResult.success(
                        playUrl = extractedUrl,
                        parseTime = System.currentTimeMillis() - startTime
                    )
                }
            }

            ParseResult.failure(
                error = "WebView解析未找到有效播放地址",
                parseTime = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            ParseResult.failure(
                error = "WebView解析失败: ${e.message}",
                parseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun getType(): VodParser.Type = VodParser.Type.WEBVIEW

    override fun canParse(url: String, site: VodSite): Boolean {
        return url.startsWith("http") && !url.contains(".m3u8") && !url.contains(".mp4") && !url.contains("json")
    }

    /**
     * 获取HTML内容
     */
    private suspend fun fetchHtmlContent(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val html = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            html

        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从HTML中提取视频URL
     */
    private fun extractVideoUrlFromHtml(html: String): String? {
        // JavaScript变量模式
        val jsPatterns = listOf(
            Pattern.compile("var\\s+\\w+\\s*=\\s*[\"'](https?://[^\"']*\\.m3u8[^\"']*)[\"']"),
            Pattern.compile("url\\s*:\\s*[\"'](https?://[^\"']*\\.m3u8[^\"']*)[\"']"),
            Pattern.compile("src\\s*:\\s*[\"'](https?://[^\"']*\\.m3u8[^\"']*)[\"']"),
            Pattern.compile("file\\s*:\\s*[\"'](https?://[^\"']*\\.m3u8[^\"']*)[\"']"),
            Pattern.compile("[\"'](https?://[^\"']*\\.m3u8[^\"']*)[\"']"),
            Pattern.compile("[\"'](https?://[^\"']*\\.mp4[^\"']*)[\"']")
        )

        for (pattern in jsPatterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val videoUrl = matcher.group(1)
                if (videoUrl != null && videoUrl.startsWith("http")) {
                    return videoUrl
                }
            }
        }

        return null
    }
}

/**
 * 自定义解析器 (处理特殊站点的解析逻辑)
 */
class CustomParser : VodParser {

    companion object {
        // 支持的特殊域名
        private val SUPPORTED_DOMAINS = setOf(
            "iqiyi.com", "youku.com", "qq.com", "bilibili.com",
            "mgtv.com", "sohu.com", "163.com", "sina.com"
        )
    }

    override suspend fun parse(
        url: String,
        site: VodSite,
        parse: VodParse?,
        flag: String
    ): ParseResult = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        try {
            // 根据URL域名选择不同的解析策略
            val domain = extractDomain(url)

            val result = when {
                domain.contains("iqiyi") -> parseIqiyi(url, site)
                domain.contains("youku") -> parseYouku(url, site)
                domain.contains("qq") -> parseQQ(url, site)
                domain.contains("bilibili") -> parseBilibili(url, site)
                else -> parseGeneric(url, site)
            }

            if (result != null) {
                ParseResult.success(
                    playUrl = result,
                    parseTime = System.currentTimeMillis() - startTime
                )
            } else {
                ParseResult.failure(
                    error = "自定义解析未找到有效播放地址",
                    parseTime = System.currentTimeMillis() - startTime
                )
            }

        } catch (e: Exception) {
            ParseResult.failure(
                error = "自定义解析失败: ${e.message}",
                parseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun getType(): VodParser.Type = VodParser.Type.CUSTOM

    override fun canParse(url: String, site: VodSite): Boolean {
        val domain = extractDomain(url)
        return SUPPORTED_DOMAINS.any { domain.contains(it) }
    }

    /**
     * 提取域名
     */
    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.lowercase()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 爱奇艺解析
     */
    private suspend fun parseIqiyi(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            // 爱奇艺的特殊解析逻辑
            // 这里只是示例，实际需要根据爱奇艺的API进行解析
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 优酷解析
     */
    private suspend fun parseYouku(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            // 优酷的特殊解析逻辑
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 腾讯视频解析
     */
    private suspend fun parseQQ(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            // 腾讯视频的特殊解析逻辑
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * B站解析
     */
    private suspend fun parseBilibili(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            // B站的特殊解析逻辑
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通用解析
     */
    private suspend fun parseGeneric(url: String, site: VodSite): String? = withContext(Dispatchers.IO) {
        try {
            // 通用的解析逻辑，尝试从页面中提取播放地址
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", site.getUserAgent())
            connection.setRequestProperty("Referer", site.api)

            val html = connection.inputStream.bufferedReader().use { it.readText() }

            // 尝试提取iframe中的视频地址
            val iframePattern = Pattern.compile("src=[\"'](https?://[^\"']*)[\"']")
            val matcher = iframePattern.matcher(html)

            while (matcher.find()) {
                val iframeUrl = matcher.group(1)
                if (iframeUrl != null && iframeUrl.contains("player")) {
                    // 递归解析iframe内容
                    return@withContext iframeUrl
                }
            }

            connection.disconnect()
            null

        } catch (e: Exception) {
            null
        }
    }
}
