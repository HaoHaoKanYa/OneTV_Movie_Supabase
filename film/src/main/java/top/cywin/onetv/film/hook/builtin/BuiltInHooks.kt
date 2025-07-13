package top.cywin.onetv.film.hook.builtin

import android.util.Log
import top.cywin.onetv.film.hook.*

/**
 * 内置 Hook 实现
 * 
 * 基于 FongMi/TV 的内置 Hook 功能
 * 提供常用的请求、响应和播放器处理 Hook
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * User-Agent Hook
 * 自动添加或修改 User-Agent 请求头
 */
class UserAgentHook : RequestHook {
    
    override val name: String = "UserAgent"
    override val description: String = "自动添加或修改 User-Agent 请求头"
    override val priority: Int = 10
    
    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 11; M2007J3SC Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045714 Mobile Safari/537.36"
    
    override fun matches(context: HookContext): Boolean {
        return context is RequestHookContext
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest {
        val modifiedRequest = request.copy()
        
        // 如果没有 User-Agent 或者是默认的，则设置自定义的
        if (!modifiedRequest.headers.containsKey("User-Agent") || 
            modifiedRequest.headers["User-Agent"]?.contains("okhttp") == true) {
            
            modifiedRequest.addHeader("User-Agent", defaultUserAgent)
            Log.d("UserAgentHook", "✅ 设置 User-Agent: $defaultUserAgent")
        }
        
        return modifiedRequest
    }
}

/**
 * Referer Hook
 * 自动添加 Referer 请求头
 */
class RefererHook : RequestHook {
    
    override val name: String = "Referer"
    override val description: String = "自动添加 Referer 请求头"
    override val priority: Int = 20
    
    override fun matches(context: HookContext): Boolean {
        return context is RequestHookContext
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest {
        val modifiedRequest = request.copy()
        
        // 如果没有 Referer，则根据 URL 自动设置
        if (!modifiedRequest.headers.containsKey("Referer")) {
            val url = modifiedRequest.url
            val referer = extractDomain(url)
            
            if (referer.isNotEmpty()) {
                modifiedRequest.addHeader("Referer", referer)
                Log.d("RefererHook", "✅ 设置 Referer: $referer")
            }
        }
        
        return modifiedRequest
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * Cache Control Hook
 * 添加缓存控制请求头
 */
class CacheControlHook : RequestHook {
    
    override val name: String = "CacheControl"
    override val description: String = "添加缓存控制请求头"
    override val priority: Int = 30
    
    override fun matches(context: HookContext): Boolean {
        return context is RequestHookContext
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest {
        val modifiedRequest = request.copy()
        
        // 添加缓存控制头
        if (!modifiedRequest.headers.containsKey("Cache-Control")) {
            modifiedRequest.addHeader("Cache-Control", "no-cache")
        }
        
        if (!modifiedRequest.headers.containsKey("Pragma")) {
            modifiedRequest.addHeader("Pragma", "no-cache")
        }
        
        return modifiedRequest
    }
}

/**
 * Content Type Hook
 * 处理响应的 Content-Type
 */
class ContentTypeHook : ResponseHook {
    
    override val name: String = "ContentType"
    override val description: String = "处理响应的 Content-Type"
    override val priority: Int = 10
    
    override fun matches(context: HookContext): Boolean {
        return context is ResponseHookContext
    }
    
    override suspend fun processResponse(response: HookResponse): HookResponse {
        val modifiedResponse = response.copy()
        
        // 确保有正确的 Content-Type
        val contentType = modifiedResponse.headers["Content-Type"] ?: 
                         modifiedResponse.headers["content-type"]
        
        if (contentType == null) {
            // 根据响应内容推断 Content-Type
            val inferredType = inferContentType(modifiedResponse.body)
            if (inferredType.isNotEmpty()) {
                modifiedResponse.addHeader("Content-Type", inferredType)
                Log.d("ContentTypeHook", "✅ 推断 Content-Type: $inferredType")
            }
        }
        
        return modifiedResponse
    }
    
    private fun inferContentType(body: String): String {
        return when {
            body.trim().startsWith("{") || body.trim().startsWith("[") -> "application/json"
            body.trim().startsWith("<") -> "text/html"
            body.contains("#EXTM3U") -> "application/vnd.apple.mpegurl"
            else -> "text/plain"
        }
    }
}

/**
 * Encoding Hook
 * 处理响应编码
 */
class EncodingHook : ResponseHook {
    
    override val name: String = "Encoding"
    override val description: String = "处理响应编码"
    override val priority: Int = 20
    
    override fun matches(context: HookContext): Boolean {
        return context is ResponseHookContext
    }
    
    override suspend fun processResponse(response: HookResponse): HookResponse {
        val modifiedResponse = response.copy()
        
        // 处理编码问题
        val contentType = modifiedResponse.headers["Content-Type"] ?: 
                         modifiedResponse.headers["content-type"] ?: ""
        
        if (contentType.contains("text/") && !contentType.contains("charset")) {
            // 检测编码并添加
            val charset = detectCharset(modifiedResponse.body)
            if (charset.isNotEmpty()) {
                val newContentType = "$contentType; charset=$charset"
                modifiedResponse.addHeader("Content-Type", newContentType)
                Log.d("EncodingHook", "✅ 设置编码: $charset")
            }
        }
        
        return modifiedResponse
    }
    
    private fun detectCharset(body: String): String {
        return when {
            body.contains("charset=utf-8", ignoreCase = true) -> "utf-8"
            body.contains("charset=gbk", ignoreCase = true) -> "gbk"
            body.contains("charset=gb2312", ignoreCase = true) -> "gb2312"
            // 简单的中文检测
            body.contains("[\u4e00-\u9fa5]".toRegex()) -> "utf-8"
            else -> "utf-8"
        }
    }
}

/**
 * M3U8 Hook
 * 处理 M3U8 播放链接
 */
class M3U8Hook : PlayerHook {
    
    override val name: String = "M3U8"
    override val description: String = "处理 M3U8 播放链接"
    override val priority: Int = 10
    
    override fun matches(context: HookContext): Boolean {
        return context is PlayerHookContext && 
               (context.playerUrl.originalUrl.contains(".m3u8") || 
                context.playerUrl.originalUrl.contains("m3u8"))
    }
    
    override suspend fun processPlayerUrl(playerUrl: HookPlayerUrl): HookPlayerUrl {
        val modifiedUrl = playerUrl.copy()
        
        // 添加 M3U8 特定的请求头
        modifiedUrl.addHeader("Accept", "application/vnd.apple.mpegurl")
        
        // 如果没有 Referer，添加一个
        if (!modifiedUrl.headers.containsKey("Referer")) {
            val referer = extractDomain(modifiedUrl.originalUrl)
            if (referer.isNotEmpty()) {
                modifiedUrl.addHeader("Referer", referer)
            }
        }
        
        // 设置解析标志
        modifiedUrl.copy(parse = 0) // M3U8 通常不需要解析
        
        Log.d("M3U8Hook", "✅ 处理 M3U8 链接: ${modifiedUrl.originalUrl}")
        return modifiedUrl
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * MP4 Hook
 * 处理 MP4 播放链接
 */
class Mp4Hook : PlayerHook {
    
    override val name: String = "MP4"
    override val description: String = "处理 MP4 播放链接"
    override val priority: Int = 20
    
    override fun matches(context: HookContext): Boolean {
        return context is PlayerHookContext && 
               (context.playerUrl.originalUrl.contains(".mp4") || 
                context.playerUrl.originalUrl.contains("mp4"))
    }
    
    override suspend fun processPlayerUrl(playerUrl: HookPlayerUrl): HookPlayerUrl {
        val modifiedUrl = playerUrl.copy()
        
        // 添加 MP4 特定的请求头
        modifiedUrl.addHeader("Accept", "video/mp4,video/*;q=0.9,*/*;q=0.8")
        modifiedUrl.addHeader("Range", "bytes=0-")
        
        // 设置解析标志
        modifiedUrl.copy(parse = 0) // MP4 直链不需要解析
        
        Log.d("Mp4Hook", "✅ 处理 MP4 链接: ${modifiedUrl.originalUrl}")
        return modifiedUrl
    }
}

/**
 * Anti-Hotlink Hook
 * 防盗链处理
 */
class AntiHotlinkHook : RequestHook {
    
    override val name: String = "AntiHotlink"
    override val description: String = "防盗链处理"
    override val priority: Int = 5
    
    private val hotlinkDomains = setOf(
        "qq.com", "163.com", "sina.com", "sohu.com", "youku.com", "iqiyi.com"
    )
    
    override fun matches(context: HookContext): Boolean {
        if (context !is RequestHookContext) return false
        
        val url = context.request.url
        return hotlinkDomains.any { url.contains(it) }
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest {
        val modifiedRequest = request.copy()
        
        // 设置防盗链请求头
        val domain = extractDomain(request.url)
        modifiedRequest.addHeader("Referer", domain)
        modifiedRequest.addHeader("Origin", domain)
        
        // 移除可能暴露来源的请求头
        modifiedRequest.headers.remove("X-Forwarded-For")
        modifiedRequest.headers.remove("X-Real-IP")
        
        Log.d("AntiHotlinkHook", "✅ 处理防盗链: ${request.url}")
        return modifiedRequest
    }
    
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * CORS Hook
 * 跨域请求处理
 */
class CorsHook : RequestHook {
    
    override val name: String = "CORS"
    override val description: String = "跨域请求处理"
    override val priority: Int = 15
    
    override fun matches(context: HookContext): Boolean {
        return context is RequestHookContext
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest {
        val modifiedRequest = request.copy()
        
        // 添加 CORS 相关请求头
        modifiedRequest.addHeader("Access-Control-Request-Method", request.method)
        modifiedRequest.addHeader("Access-Control-Request-Headers", "Content-Type,Authorization")
        
        return modifiedRequest
    }
}

/**
 * Retry Hook
 * 重试处理
 */
class RetryHook : ResponseHook {
    
    override val name: String = "Retry"
    override val description: String = "重试处理"
    override val priority: Int = 100 // 低优先级，最后执行
    
    override fun matches(context: HookContext): Boolean {
        return context is ResponseHookContext && 
               (context.response.statusCode >= 500 || context.response.statusCode == 429)
    }
    
    override suspend fun processResponse(response: HookResponse): HookResponse {
        val modifiedResponse = response.copy()
        
        // 标记需要重试
        modifiedResponse.setMetadata("should_retry", true)
        modifiedResponse.setMetadata("retry_after", 1000L) // 1秒后重试
        
        Log.d("RetryHook", "✅ 标记重试: ${response.statusCode}")
        return modifiedResponse
    }
}
