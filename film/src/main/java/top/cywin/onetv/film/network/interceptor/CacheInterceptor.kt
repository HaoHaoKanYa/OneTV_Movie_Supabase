package top.cywin.onetv.film.network.interceptor

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.cache.CacheFactory
import top.cywin.onetv.film.utils.NetworkUtils
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 缓存拦截器
 * 
 * 基于 FongMi/TV 的缓存策略实现
 * 支持智能缓存和离线访问
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class CacheInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_CACHE_INTERCEPTOR"
        
        // 缓存时间配置
        private const val CACHE_MAX_AGE_ONLINE = 5 * 60 // 在线时缓存5分钟
        private const val CACHE_MAX_AGE_OFFLINE = 7 * 24 * 60 * 60 // 离线时缓存7天
        private const val CACHE_MAX_STALE = 30 * 24 * 60 * 60 // 最大过期时间30天
        
        // 缓存控制头
        private const val HEADER_CACHE_CONTROL = "Cache-Control"
        private const val HEADER_PRAGMA = "Pragma"
        private const val HEADER_IF_MODIFIED_SINCE = "If-Modified-Since"
        private const val HEADER_LAST_MODIFIED = "Last-Modified"
        private const val HEADER_ETAG = "ETag"
        private const val HEADER_IF_NONE_MATCH = "If-None-Match"
    }
    
    private val networkCache = CacheFactory.getNetworkCache(context)
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        try {
            Log.d(TAG, "处理缓存请求: $url")
            
            // 1. 检查是否应该使用缓存
            if (!shouldUseCache(originalRequest)) {
                Log.d(TAG, "跳过缓存: $url")
                return chain.proceed(originalRequest)
            }
            
            // 2. 构建缓存请求
            val cacheRequest = buildCacheRequest(originalRequest)
            
            // 3. 检查网络状态
            val isOnline = NetworkUtils.isNetworkAvailable(context)
            
            // 4. 根据网络状态处理缓存
            return if (isOnline) {
                handleOnlineCache(chain, cacheRequest, url)
            } else {
                handleOfflineCache(chain, cacheRequest, url)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "缓存处理失败: $url", e)
            // 缓存失败时直接请求
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 判断是否应该使用缓存
     */
    private fun shouldUseCache(request: okhttp3.Request): Boolean {
        return try {
            val url = request.url.toString()
            val method = request.method
            
            // 1. 只缓存 GET 请求
            if (method != "GET") {
                return false
            }
            
            // 2. 检查请求头中的缓存控制
            val cacheControl = request.header(HEADER_CACHE_CONTROL)
            if (cacheControl?.contains("no-cache") == true || 
                cacheControl?.contains("no-store") == true) {
                return false
            }
            
            // 3. 检查 Pragma 头
            val pragma = request.header(HEADER_PRAGMA)
            if (pragma?.contains("no-cache") == true) {
                return false
            }
            
            // 4. 检查 URL 类型
            when {
                url.contains("/api/") -> true // API 请求缓存
                url.contains(".json") -> true // JSON 文件缓存
                url.contains(".xml") -> true // XML 文件缓存
                url.contains(".m3u8") -> false // 直播流不缓存
                url.contains(".ts") -> false // 视频片段不缓存
                url.contains(".mp4") -> false // 视频文件不缓存
                else -> true // 默认缓存
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "判断缓存策略失败", e)
            true // 默认使用缓存
        }
    }
    
    /**
     * 构建缓存请求
     */
    private fun buildCacheRequest(originalRequest: okhttp3.Request): okhttp3.Request {
        val builder = originalRequest.newBuilder()
        
        // 添加缓存相关头部
        val isOnline = NetworkUtils.isNetworkAvailable(context)
        
        if (isOnline) {
            // 在线时：优先使用缓存，但允许验证
            builder.header(HEADER_CACHE_CONTROL, "max-age=$CACHE_MAX_AGE_ONLINE")
        } else {
            // 离线时：强制使用缓存
            builder.header(HEADER_CACHE_CONTROL, "only-if-cached, max-stale=$CACHE_MAX_STALE")
        }
        
        return builder.build()
    }
    
    /**
     * 处理在线缓存
     */
    private fun handleOnlineCache(chain: Interceptor.Chain, request: okhttp3.Request, url: String): Response {
        return try {
            // 执行请求
            val response = chain.proceed(request)
            
            // 处理响应缓存
            val cacheResponse = buildCacheResponse(response)
            
            Log.d(TAG, "在线缓存处理完成: $url, 状态=${response.code}")
            cacheResponse
            
        } catch (e: Exception) {
            Log.e(TAG, "在线缓存处理失败: $url", e)
            throw e
        }
    }
    
    /**
     * 处理离线缓存
     */
    private fun handleOfflineCache(chain: Interceptor.Chain, request: okhttp3.Request, url: String): Response {
        return try {
            // 尝试从缓存获取
            val response = chain.proceed(request)
            
            if (response.code == 504) {
                // 网关超时，可能是离线状态
                Log.d(TAG, "离线状态，使用缓存: $url")
            }
            
            response
            
        } catch (e: Exception) {
            Log.e(TAG, "离线缓存处理失败: $url", e)
            throw e
        }
    }
    
    /**
     * 构建缓存响应
     */
    private fun buildCacheResponse(response: Response): Response {
        return try {
            val builder = response.newBuilder()
            
            // 移除可能干扰缓存的头部
            builder.removeHeader(HEADER_PRAGMA)
            builder.removeHeader("Expires")
            
            // 设置缓存控制头
            val isOnline = NetworkUtils.isNetworkAvailable(context)
            val maxAge = if (isOnline) CACHE_MAX_AGE_ONLINE else CACHE_MAX_AGE_OFFLINE
            
            builder.header(HEADER_CACHE_CONTROL, "public, max-age=$maxAge")
            
            // 添加自定义缓存标识
            builder.header("X-Cache-Strategy", if (isOnline) "online" else "offline")
            builder.header("X-Cache-Time", System.currentTimeMillis().toString())
            
            builder.build()
            
        } catch (e: Exception) {
            Log.w(TAG, "构建缓存响应失败", e)
            response
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(response: Response): Boolean {
        return try {
            val cacheControl = response.header(HEADER_CACHE_CONTROL)
            val cacheTime = response.header("X-Cache-Time")?.toLongOrNull()
            
            if (cacheTime == null) {
                return false
            }
            
            val currentTime = System.currentTimeMillis()
            val age = (currentTime - cacheTime) / 1000 // 转换为秒
            
            // 解析 max-age
            val maxAge = cacheControl?.let { cc ->
                val maxAgeMatch = Regex("max-age=(\\d+)").find(cc)
                maxAgeMatch?.groupValues?.get(1)?.toLongOrNull()
            } ?: CACHE_MAX_AGE_ONLINE.toLong()
            
            age <= maxAge
            
        } catch (e: Exception) {
            Log.w(TAG, "检查缓存有效性失败", e)
            false
        }
    }
    
    /**
     * 获取缓存键
     */
    private fun getCacheKey(request: okhttp3.Request): String {
        val url = request.url.toString()
        val method = request.method
        
        // 简单的缓存键生成策略
        return "${method}_${url.hashCode()}"
    }
    
    /**
     * 清理过期缓存
     */
    fun clearExpiredCache() {
        try {
            networkCache.evictAll()
            Log.d(TAG, "清理过期缓存完成")
        } catch (e: Exception) {
            Log.e(TAG, "清理过期缓存失败", e)
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        return try {
            mapOf(
                "cache_size" to networkCache.size(),
                "cache_hit_count" to networkCache.hitCount(),
                "cache_request_count" to networkCache.requestCount(),
                "cache_hit_rate" to (networkCache.hitCount().toDouble() / networkCache.requestCount().coerceAtLeast(1))
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取缓存统计失败", e)
            emptyMap()
        }
    }
}
