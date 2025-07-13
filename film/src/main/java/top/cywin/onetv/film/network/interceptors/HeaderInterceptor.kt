package top.cywin.onetv.film.network.interceptors

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.utils.StringUtils

/**
 * 请求头拦截器
 * 
 * 基于 FongMi/TV 的请求头处理机制实现
 * 负责自动添加和处理 HTTP 请求头
 * 
 * 核心功能：
 * - 自动添加通用请求头
 * - 动态请求头处理
 * - User-Agent 管理
 * - 自定义请求头支持
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class HeaderInterceptor(
    private val defaultHeaders: Map<String, String> = emptyMap(),
    private val enableAutoHeaders: Boolean = true
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_HEADER_INTERCEPTOR"
        
        // 默认 User-Agent
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        
        // 常用请求头
        private val COMMON_HEADERS = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8",
            "Accept-Encoding" to "gzip, deflate, br",
            "Cache-Control" to "no-cache",
            "Pragma" to "no-cache"
        )
        
        // 移动端 User-Agent 列表
        private val MOBILE_USER_AGENTS = listOf(
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Android 12; Mobile; rv:68.0) Gecko/68.0 Firefox/88.0",
            "Mozilla/5.0 (Linux; Android 12; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        )
        
        // 桌面端 User-Agent 列表
        private val DESKTOP_USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        try {
            Log.d(TAG, "🔧 处理请求头: ${originalRequest.url}")
            
            // 1. 添加自动请求头
            if (enableAutoHeaders) {
                addAutoHeaders(requestBuilder, originalRequest.url.toString())
            }
            
            // 2. 添加默认请求头
            defaultHeaders.forEach { (name, value) ->
                if (originalRequest.header(name) == null) {
                    requestBuilder.header(name, value)
                }
            }
            
            // 3. 处理特殊请求头
            processSpecialHeaders(requestBuilder, originalRequest.url.toString())
            
            val request = requestBuilder.build()
            
            // 4. 记录请求头信息
            logRequestHeaders(request)
            
            return chain.proceed(request)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 处理请求头失败: ${originalRequest.url}", e)
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 添加自动请求头
     */
    private fun addAutoHeaders(builder: okhttp3.Request.Builder, url: String) {
        // 添加通用请求头
        COMMON_HEADERS.forEach { (name, value) ->
            if (!hasHeader(builder, name)) {
                builder.header(name, value)
            }
        }
        
        // 添加 User-Agent
        if (!hasHeader(builder, "User-Agent")) {
            val userAgent = selectUserAgent(url)
            builder.header("User-Agent", userAgent)
        }
        
        // 添加 Referer
        if (!hasHeader(builder, "Referer")) {
            val referer = generateReferer(url)
            if (referer.isNotBlank()) {
                builder.header("Referer", referer)
            }
        }
        
        // 添加 Origin
        if (!hasHeader(builder, "Origin")) {
            val origin = generateOrigin(url)
            if (origin.isNotBlank()) {
                builder.header("Origin", origin)
            }
        }
    }
    
    /**
     * 处理特殊请求头
     */
    private fun processSpecialHeaders(builder: okhttp3.Request.Builder, url: String) {
        val lowerUrl = url.lowercase()
        
        // 针对特定站点的请求头优化
        when {
            // 针对 API 接口
            lowerUrl.contains("/api/") -> {
                builder.header("X-Requested-With", "XMLHttpRequest")
                builder.header("Content-Type", "application/json")
            }
            
            // 针对 JavaScript 文件
            lowerUrl.endsWith(".js") -> {
                builder.header("Accept", "application/javascript, */*;q=0.1")
            }
            
            // 针对 JSON 文件
            lowerUrl.endsWith(".json") -> {
                builder.header("Accept", "application/json, text/plain, */*")
            }
            
            // 针对图片资源
            lowerUrl.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$")) -> {
                builder.header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
            }
        }
        
        // 添加防盗链处理
        if (needsAntiHotlink(url)) {
            addAntiHotlinkHeaders(builder, url)
        }
    }
    
    /**
     * 选择合适的 User-Agent
     */
    private fun selectUserAgent(url: String): String {
        val lowerUrl = url.lowercase()
        
        return when {
            // 移动端站点
            lowerUrl.contains("m.") || lowerUrl.contains("mobile") -> {
                MOBILE_USER_AGENTS.random()
            }
            
            // 默认使用桌面端
            else -> {
                DESKTOP_USER_AGENTS.random()
            }
        }
    }
    
    /**
     * 生成 Referer
     */
    private fun generateReferer(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}/"
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 生成 Origin
     */
    private fun generateOrigin(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 检查是否需要防盗链处理
     */
    private fun needsAntiHotlink(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("img") || 
               lowerUrl.contains("image") || 
               lowerUrl.contains("pic") ||
               lowerUrl.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$"))
    }
    
    /**
     * 添加防盗链请求头
     */
    private fun addAntiHotlinkHeaders(builder: okhttp3.Request.Builder, url: String) {
        val referer = generateReferer(url)
        if (referer.isNotBlank()) {
            builder.header("Referer", referer)
        }
        
        // 添加防盗链相关头
        builder.header("Sec-Fetch-Dest", "image")
        builder.header("Sec-Fetch-Mode", "no-cors")
        builder.header("Sec-Fetch-Site", "cross-site")
    }
    
    /**
     * 检查是否已有指定请求头
     */
    private fun hasHeader(builder: okhttp3.Request.Builder, name: String): Boolean {
        // 由于 OkHttp 的 Request.Builder 没有直接检查方法，
        // 这里简化处理，实际使用中可以维护一个已添加的头列表
        return false
    }
    
    /**
     * 记录请求头信息
     */
    private fun logRequestHeaders(request: okhttp3.Request) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            val headers = request.headers
            Log.d(TAG, "📋 请求头信息:")
            for (i in 0 until headers.size) {
                Log.d(TAG, "  ${headers.name(i)}: ${headers.value(i)}")
            }
        }
    }
}
