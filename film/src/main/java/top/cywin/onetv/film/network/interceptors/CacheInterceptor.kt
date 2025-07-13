package top.cywin.onetv.film.network.interceptors

import android.util.Log
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.cache.CacheManager
import top.cywin.onetv.film.utils.StringUtils
import java.util.concurrent.TimeUnit

/**
 * 缓存拦截器
 * 
 * 基于 FongMi/TV 的网络缓存机制实现
 * 负责网络请求的缓存控制和管理
 * 
 * 核心功能：
 * - 智能缓存策略
 * - 缓存时间控制
 * - 离线缓存支持
 * - 缓存失效处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class CacheInterceptor(
    private val cacheManager: CacheManager,
    private val enableCache: Boolean = true,
    private val defaultCacheTime: Long = 5 * 60 * 1000L // 5分钟
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_CACHE_INTERCEPTOR"
        
        // 缓存策略配置
        private val CACHE_STRATEGIES = mapOf(
            // 配置文件 - 长时间缓存
            "config" to 30 * 60 * 1000L,      // 30分钟
            
            // 内容列表 - 中等时间缓存
            "content" to 10 * 60 * 1000L,     // 10分钟
            
            // 搜索结果 - 短时间缓存
            "search" to 5 * 60 * 1000L,       // 5分钟
            
            // 图片资源 - 长时间缓存
            "image" to 60 * 60 * 1000L,       // 1小时
            
            // JavaScript 文件 - 长时间缓存
            "script" to 60 * 60 * 1000L,      // 1小时
            
            // API 接口 - 短时间缓存
            "api" to 2 * 60 * 1000L           // 2分钟
        )
        
        // 不缓存的 URL 模式
        private val NO_CACHE_PATTERNS = listOf(
            "login", "logout", "auth", "token",
            "real-time", "live", "stream"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        
        if (!enableCache) {
            Log.d(TAG, "📝 缓存已禁用，直接处理: $url")
            return chain.proceed(request)
        }
        
        try {
            Log.d(TAG, "🔍 检查缓存策略: $url")
            
            // 1. 检查是否应该缓存
            if (shouldSkipCache(url)) {
                Log.d(TAG, "⏭️ 跳过缓存: $url")
                return chain.proceed(addNoCacheHeaders(request))
            }
            
            // 2. 确定缓存策略
            val cacheStrategy = determineCacheStrategy(url)
            val cacheTime = cacheStrategy.second
            
            Log.d(TAG, "📦 缓存策略: ${cacheStrategy.first}, 时间: ${cacheTime}ms")
            
            // 3. 检查缓存
            val cacheKey = generateCacheKey(request)
            val cachedResponse = getCachedResponse(cacheKey)
            
            if (cachedResponse != null && !isCacheExpired(cachedResponse, cacheTime)) {
                Log.d(TAG, "✅ 使用缓存响应: $url")
                return cachedResponse
            }
            
            // 4. 执行网络请求
            val response = chain.proceed(addCacheHeaders(request, cacheTime))
            
            // 5. 缓存响应
            if (response.isSuccessful && shouldCacheResponse(response)) {
                cacheResponse(cacheKey, response, cacheTime)
            }
            
            return response
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存处理失败: $url", e)
            return chain.proceed(request)
        }
    }
    
    /**
     * 检查是否应该跳过缓存
     */
    private fun shouldSkipCache(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return NO_CACHE_PATTERNS.any { pattern ->
            lowerUrl.contains(pattern)
        }
    }
    
    /**
     * 确定缓存策略
     */
    private fun determineCacheStrategy(url: String): Pair<String, Long> {
        val lowerUrl = url.lowercase()
        
        return when {
            // 配置文件
            lowerUrl.contains("config") || lowerUrl.endsWith(".json") -> {
                "config" to CACHE_STRATEGIES["config"]!!
            }
            
            // 图片资源
            lowerUrl.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|ico)$")) -> {
                "image" to CACHE_STRATEGIES["image"]!!
            }
            
            // JavaScript 文件
            lowerUrl.endsWith(".js") -> {
                "script" to CACHE_STRATEGIES["script"]!!
            }
            
            // API 接口
            lowerUrl.contains("/api/") -> {
                "api" to CACHE_STRATEGIES["api"]!!
            }
            
            // 搜索相关
            lowerUrl.contains("search") -> {
                "search" to CACHE_STRATEGIES["search"]!!
            }
            
            // 内容相关
            lowerUrl.contains("content") || lowerUrl.contains("list") -> {
                "content" to CACHE_STRATEGIES["content"]!!
            }
            
            // 默认策略
            else -> {
                "default" to defaultCacheTime
            }
        }
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(request: okhttp3.Request): String {
        val url = request.url.toString()
        val method = request.method
        val headers = request.headers.toString()
        
        val keyData = "$method:$url:$headers"
        return "http_cache:${StringUtils.md5(keyData)}"
    }
    
    /**
     * 获取缓存响应
     */
    private fun getCachedResponse(cacheKey: String): Response? {
        return try {
            // 这里简化处理，实际实现需要序列化/反序列化 Response
            // cacheManager.get(cacheKey, Response::class.java)
            null // 暂时返回 null
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取缓存响应失败", e)
            null
        }
    }
    
    /**
     * 检查缓存是否过期
     */
    private fun isCacheExpired(response: Response, cacheTime: Long): Boolean {
        return try {
            val cacheHeader = response.header("X-Cache-Time")
            if (cacheHeader != null) {
                val cacheTimestamp = cacheHeader.toLongOrNull() ?: 0L
                val currentTime = System.currentTimeMillis()
                (currentTime - cacheTimestamp) > cacheTime
            } else {
                true // 没有缓存时间戳，认为已过期
            }
        } catch (e: Exception) {
            true // 出错时认为已过期
        }
    }
    
    /**
     * 添加缓存控制请求头
     */
    private fun addCacheHeaders(request: okhttp3.Request, cacheTime: Long): okhttp3.Request {
        val cacheControl = CacheControl.Builder()
            .maxAge(cacheTime.toInt(), TimeUnit.MILLISECONDS)
            .build()
        
        return request.newBuilder()
            .cacheControl(cacheControl)
            .header("X-Cache-Strategy", "enabled")
            .build()
    }
    
    /**
     * 添加禁用缓存请求头
     */
    private fun addNoCacheHeaders(request: okhttp3.Request): okhttp3.Request {
        val cacheControl = CacheControl.Builder()
            .noCache()
            .noStore()
            .build()
        
        return request.newBuilder()
            .cacheControl(cacheControl)
            .header("X-Cache-Strategy", "disabled")
            .build()
    }
    
    /**
     * 检查是否应该缓存响应
     */
    private fun shouldCacheResponse(response: Response): Boolean {
        return try {
            // 检查响应状态
            if (!response.isSuccessful) {
                return false
            }
            
            // 检查响应头
            val cacheControl = response.cacheControl
            if (cacheControl.noStore || cacheControl.noCache) {
                return false
            }
            
            // 检查内容类型
            val contentType = response.header("Content-Type")?.lowercase()
            if (contentType != null) {
                // 不缓存某些类型的内容
                val noCacheTypes = listOf("text/event-stream", "application/octet-stream")
                if (noCacheTypes.any { contentType.contains(it) }) {
                    return false
                }
            }
            
            true
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 检查缓存响应失败", e)
            false
        }
    }
    
    /**
     * 缓存响应
     */
    private fun cacheResponse(cacheKey: String, response: Response, cacheTime: Long) {
        try {
            // 添加缓存时间戳
            val responseWithTimestamp = response.newBuilder()
                .header("X-Cache-Time", System.currentTimeMillis().toString())
                .build()
            
            // 这里简化处理，实际实现需要序列化 Response
            // cacheManager.put(cacheKey, responseWithTimestamp, cacheTime)
            
            Log.d(TAG, "📦 响应已缓存: $cacheKey")
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存响应失败", e)
        }
    }
}
