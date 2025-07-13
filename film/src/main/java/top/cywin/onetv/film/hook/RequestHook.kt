package top.cywin.onetv.film.hook

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.utils.StringUtils
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * 请求拦截 Hook
 * 基于 FongMi/TV 标准实现
 * 
 * 提供HTTP请求的拦截和修改功能
 * 
 * 功能：
 * - 请求URL重写
 * - 请求头修改
 * - 参数注入
 * - 用户代理伪装
 * - 防盗链处理
 * - 请求缓存控制
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class RequestHook : Hook, RequestHook {
    
    companion object {
        private const val TAG = "ONETV_FILM_REQUEST_HOOK"
    }
    
    override val name: String = "RequestHook"
    override val description: String = "HTTP请求拦截和修改"
    override val priority: Int = 50
    
    // 统计信息
    private val stats = HookStats(name)
    private val processedCount = AtomicLong(0)
    private val modifiedCount = AtomicLong(0)
    
    // 配置
    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:89.0) Gecko/20100101 Firefox/89.0"
    )
    
    // URL重写规则
    private val urlRewriteRules = mutableMapOf<String, String>()
    
    // 默认请求头
    private val defaultHeaders = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3",
        "Accept-Encoding" to "gzip, deflate",
        "Connection" to "keep-alive",
        "Upgrade-Insecure-Requests" to "1",
        "Cache-Control" to "max-age=0"
    )
    
    override fun matches(context: HookContext): Boolean {
        return context is RequestHookContext
    }
    
    override suspend fun processRequest(request: HookRequest): HookRequest = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "🌐 处理请求: ${request.url}")
            processedCount.incrementAndGet()
            
            val modifiedRequest = request.copy()
            var isModified = false
            
            // 1. URL重写
            val rewrittenUrl = rewriteUrl(modifiedRequest.url)
            if (rewrittenUrl != modifiedRequest.url) {
                modifiedRequest.url = rewrittenUrl
                isModified = true
                Log.d(TAG, "🔄 URL重写: ${request.url} -> $rewrittenUrl")
            }
            
            // 2. 添加默认请求头
            for ((key, value) in defaultHeaders) {
                if (!modifiedRequest.headers.containsKey(key)) {
                    modifiedRequest.addHeader(key, value)
                    isModified = true
                }
            }
            
            // 3. 用户代理处理
            if (!modifiedRequest.headers.containsKey("User-Agent")) {
                val userAgent = selectUserAgent(modifiedRequest.url)
                modifiedRequest.addHeader("User-Agent", userAgent)
                isModified = true
                Log.d(TAG, "🤖 设置User-Agent: $userAgent")
            }
            
            // 4. 防盗链处理
            val referer = generateReferer(modifiedRequest.url)
            if (referer != null && !modifiedRequest.headers.containsKey("Referer")) {
                modifiedRequest.addHeader("Referer", referer)
                isModified = true
                Log.d(TAG, "🔗 设置Referer: $referer")
            }
            
            // 5. 特殊站点处理
            applySpecialSiteRules(modifiedRequest)
            
            // 6. 缓存控制
            applyCacheControl(modifiedRequest)
            
            if (isModified) {
                modifiedCount.incrementAndGet()
                Log.d(TAG, "✅ 请求已修改: ${modifiedRequest.url}")
            }
            
            // 记录统计
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.success(modifiedRequest), duration)
            
            modifiedRequest
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 请求处理失败: ${request.url}", e)
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.failure(e.message ?: "请求处理失败", e), duration)
            throw e
        }
    }
    
    /**
     * 🔄 URL重写
     */
    private fun rewriteUrl(url: String): String {
        for ((pattern, replacement) in urlRewriteRules) {
            if (url.contains(pattern)) {
                return url.replace(pattern, replacement)
            }
        }
        
        // 通用URL优化
        return when {
            // HTTP转HTTPS
            url.startsWith("http://") && shouldUpgradeToHttps(url) -> {
                url.replace("http://", "https://")
            }
            // 移除追踪参数
            url.contains("?") -> {
                removeTrackingParams(url)
            }
            else -> url
        }
    }
    
    /**
     * 🤖 选择用户代理
     */
    private fun selectUserAgent(url: String): String {
        return when {
            url.contains("mobile") || url.contains("m.") -> {
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1"
            }
            url.contains("api") -> {
                "okhttp/4.9.0"
            }
            else -> {
                userAgents.random()
            }
        }
    }
    
    /**
     * 🔗 生成Referer
     */
    private fun generateReferer(url: String): String? {
        return try {
            val urlObj = URL(url)
            val host = urlObj.host
            
            when {
                host.contains("douban") -> "https://movie.douban.com/"
                host.contains("imdb") -> "https://www.imdb.com/"
                host.contains("tmdb") -> "https://www.themoviedb.org/"
                host.contains("youtube") -> "https://www.youtube.com/"
                host.contains("bilibili") -> "https://www.bilibili.com/"
                else -> "${urlObj.protocol}://${urlObj.host}/"
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 生成Referer失败: $url", e)
            null
        }
    }
    
    /**
     * 🎯 应用特殊站点规则
     */
    private fun applySpecialSiteRules(request: HookRequest) {
        val url = request.url
        
        when {
            // 优酷特殊处理
            url.contains("youku.com") -> {
                request.addHeader("X-Requested-With", "XMLHttpRequest")
                request.addHeader("Origin", "https://www.youku.com")
            }
            
            // 爱奇艺特殊处理
            url.contains("iqiyi.com") -> {
                request.addHeader("X-Requested-With", "XMLHttpRequest")
                request.addHeader("Origin", "https://www.iqiyi.com")
            }
            
            // 腾讯视频特殊处理
            url.contains("qq.com") -> {
                request.addHeader("X-Requested-With", "XMLHttpRequest")
                request.addHeader("Origin", "https://v.qq.com")
            }
            
            // B站特殊处理
            url.contains("bilibili.com") -> {
                request.addHeader("Origin", "https://www.bilibili.com")
                request.addHeader("Sec-Fetch-Site", "same-site")
                request.addHeader("Sec-Fetch-Mode", "cors")
            }
            
            // API接口特殊处理
            url.contains("/api/") -> {
                request.addHeader("Accept", "application/json, text/plain, */*")
                request.addHeader("Content-Type", "application/json")
            }
        }
    }
    
    /**
     * 💾 应用缓存控制
     */
    private fun applyCacheControl(request: HookRequest) {
        val url = request.url
        
        when {
            // 图片资源缓存
            url.matches(".*\\.(jpg|jpeg|png|gif|webp).*".toRegex()) -> {
                request.addHeader("Cache-Control", "max-age=86400") // 1天
            }
            
            // 视频资源缓存
            url.matches(".*\\.(mp4|m3u8|ts|flv).*".toRegex()) -> {
                request.addHeader("Cache-Control", "max-age=3600") // 1小时
            }
            
            // API接口不缓存
            url.contains("/api/") -> {
                request.addHeader("Cache-Control", "no-cache")
                request.addHeader("Pragma", "no-cache")
            }
        }
    }
    
    /**
     * 🔒 是否应该升级到HTTPS
     */
    private fun shouldUpgradeToHttps(url: String): Boolean {
        val host = try {
            URL(url).host
        } catch (e: Exception) {
            return false
        }
        
        // 已知支持HTTPS的站点
        val httpsHosts = setOf(
            "douban.com", "imdb.com", "tmdb.org", "youtube.com",
            "bilibili.com", "iqiyi.com", "youku.com", "qq.com"
        )
        
        return httpsHosts.any { host.contains(it) }
    }
    
    /**
     * 🧹 移除追踪参数
     */
    private fun removeTrackingParams(url: String): String {
        val trackingParams = setOf(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "msclkid", "_ga", "_gid", "ref", "from"
        )
        
        return try {
            val urlObj = URL(url)
            val query = urlObj.query ?: return url
            
            val cleanParams = query.split("&")
                .filter { param ->
                    val key = param.split("=")[0]
                    !trackingParams.contains(key)
                }
                .joinToString("&")
            
            if (cleanParams.isEmpty()) {
                "${urlObj.protocol}://${urlObj.host}${urlObj.path}"
            } else {
                "${urlObj.protocol}://${urlObj.host}${urlObj.path}?$cleanParams"
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 移除追踪参数失败: $url", e)
            url
        }
    }
    
    /**
     * 🔧 添加URL重写规则
     */
    fun addUrlRewriteRule(pattern: String, replacement: String) {
        urlRewriteRules[pattern] = replacement
        Log.d(TAG, "🔧 添加URL重写规则: $pattern -> $replacement")
    }
    
    /**
     * 🗑️ 移除URL重写规则
     */
    fun removeUrlRewriteRule(pattern: String) {
        urlRewriteRules.remove(pattern)
        Log.d(TAG, "🗑️ 移除URL重写规则: $pattern")
    }
    
    override fun getStats(): Map<String, Any> {
        return stats.getReport() + mapOf(
            "processed_count" to processedCount.get(),
            "modified_count" to modifiedCount.get(),
            "modification_rate" to if (processedCount.get() > 0) {
                (modifiedCount.get().toDouble() / processedCount.get() * 100)
            } else 0.0,
            "url_rewrite_rules" to urlRewriteRules.size
        )
    }
    
    override fun cleanup() {
        urlRewriteRules.clear()
        Log.d(TAG, "🧹 RequestHook 资源已清理")
    }
}
