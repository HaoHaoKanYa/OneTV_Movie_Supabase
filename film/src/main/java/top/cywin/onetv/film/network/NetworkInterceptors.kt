package top.cywin.onetv.film.network

import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.hook.HookRequest
import top.cywin.onetv.film.hook.HookResponse
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * 网络拦截器集合
 * 
 * 基于 FongMi/TV 的网络拦截器实现
 * 提供统计、重试、Hook 集成等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 统计拦截器
 */
class StatsInterceptor(private val networkStats: NetworkStats) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_STATS_INTERCEPTOR"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            val responseSize = response.body?.contentLength() ?: 0L
            
            networkStats.recordRequest(
                method = request.method,
                statusCode = response.code,
                duration = duration,
                responseSize = responseSize
            )
            
            Log.d(TAG, "📊 请求统计: ${request.method} ${request.url} - ${response.code} (${duration}ms)")
            response
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            networkStats.recordError(request.method, e)
            
            Log.e(TAG, "❌ 请求失败: ${request.method} ${request.url} (${duration}ms)", e)
            throw e
        }
    }
}

/**
 * 重试拦截器
 */
class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_RETRY_INTERCEPTOR"
        private const val RETRY_DELAY = 1000L // 1秒
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                if (attempt > 0) {
                    Log.d(TAG, "🔄 重试请求 (${attempt}/${maxRetries}): ${request.method} ${request.url}")
                    Thread.sleep(RETRY_DELAY * attempt) // 递增延迟
                }
                
                val response = chain.proceed(request)
                
                // 检查是否需要重试（5xx 错误或特定的 4xx 错误）
                if (shouldRetry(response.code) && attempt < maxRetries) {
                    response.close()
                    Log.w(TAG, "⚠️ 服务器错误，准备重试: ${response.code}")
                    continue
                }
                
                return response
                
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    Log.w(TAG, "⚠️ 网络错误，准备重试: ${e.message}")
                } else {
                    Log.e(TAG, "❌ 重试次数已用完: ${e.message}")
                }
            }
        }
        
        throw lastException ?: IOException("重试失败")
    }
    
    /**
     * 🔍 判断是否应该重试
     */
    private fun shouldRetry(statusCode: Int): Boolean {
        return when (statusCode) {
            in 500..599 -> true // 服务器错误
            408 -> true // 请求超时
            429 -> true // 请求过多
            else -> false
        }
    }
}

/**
 * User-Agent 拦截器
 */
class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 如果请求中没有 User-Agent，则添加默认的
        val request = if (!originalRequest.headers.names().contains("User-Agent")) {
            originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(request)
    }
}

/**
 * Hook 拦截器
 */
class HookInterceptor(private val hookManager: HookManager) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_HOOK_INTERCEPTOR"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        return try {
            // 执行请求 Hook
            val hookRequest = HookRequest(
                url = originalRequest.url.toString(),
                method = originalRequest.method,
                headers = originalRequest.headers.toMap().toMutableMap(),
                body = originalRequest.body?.let { body ->
                    val buffer = okio.Buffer()
                    body.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""
            )
            
            val processedRequest = runBlocking {
                hookManager.executeRequestHooks(hookRequest)
            }
            
            // 构建新的请求
            val newRequestBuilder = originalRequest.newBuilder()
                .url(processedRequest.url)
            
            // 更新请求头
            processedRequest.headers.forEach { (key, value) ->
                newRequestBuilder.header(key, value)
            }
            
            val newRequest = newRequestBuilder.build()
            
            // 执行请求
            val response = chain.proceed(newRequest)
            
            // 执行响应 Hook
            val hookResponse = HookResponse(
                statusCode = response.code,
                headers = response.headers.toMap().toMutableMap(),
                body = response.body?.string() ?: ""
            )
            
            val processedResponse = runBlocking {
                hookManager.executeResponseHooks(hookResponse)
            }
            
            // 构建新的响应
            val newResponseBuilder = response.newBuilder()
            
            // 更新响应头
            processedResponse.headers.forEach { (key, value) ->
                newResponseBuilder.header(key, value)
            }
            
            // 更新响应体
            val newBody = okhttp3.ResponseBody.create(
                response.body?.contentType(),
                processedResponse.body
            )
            
            newResponseBuilder.body(newBody).build()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Hook 拦截器执行失败", e)
            chain.proceed(originalRequest)
        }
    }
}

/**
 * 缓存拦截器
 */
class CacheInterceptor(private val cacheManager: NetworkCacheManager) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_CACHE_INTERCEPTOR"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // 只缓存 GET 请求
        if (request.method != "GET") {
            return chain.proceed(request)
        }
        
        val cacheKey = generateCacheKey(request)
        
        // 检查缓存
        val cachedResponse = cacheManager.get(cacheKey)
        if (cachedResponse != null) {
            Log.d(TAG, "📦 从缓存获取响应: ${request.url}")
            return cachedResponse
        }
        
        // 执行请求
        val response = chain.proceed(request)
        
        // 缓存响应（只缓存成功的响应）
        if (response.isSuccessful && shouldCache(request, response)) {
            cacheManager.put(cacheKey, response)
            Log.d(TAG, "💾 缓存响应: ${request.url}")
        }
        
        return response
    }
    
    /**
     * 🔑 生成缓存键
     */
    private fun generateCacheKey(request: okhttp3.Request): String {
        return "${request.method}:${request.url}"
    }
    
    /**
     * 🔍 判断是否应该缓存
     */
    private fun shouldCache(request: okhttp3.Request, response: Response): Boolean {
        // 检查 Cache-Control 头
        val cacheControl = response.header("Cache-Control")
        if (cacheControl?.contains("no-cache") == true || 
            cacheControl?.contains("no-store") == true) {
            return false
        }
        
        // 检查内容类型
        val contentType = response.header("Content-Type")
        return when {
            contentType?.startsWith("application/json") == true -> true
            contentType?.startsWith("text/") == true -> true
            contentType?.startsWith("application/xml") == true -> true
            else -> false
        }
    }
}

/**
 * 压缩拦截器
 */
class CompressionInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 添加压缩支持
        val request = originalRequest.newBuilder()
            .header("Accept-Encoding", "gzip, deflate")
            .build()
        
        return chain.proceed(request)
    }
}

/**
 * 安全拦截器
 */
class SecurityInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_SECURITY_INTERCEPTOR"
        
        // 危险的请求头
        private val DANGEROUS_HEADERS = setOf(
            "X-Forwarded-For",
            "X-Real-IP",
            "X-Originating-IP"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 检查 URL 安全性
        if (!isUrlSafe(originalRequest.url.toString())) {
            throw SecurityException("不安全的 URL: ${originalRequest.url}")
        }
        
        // 移除危险的请求头
        val requestBuilder = originalRequest.newBuilder()
        DANGEROUS_HEADERS.forEach { header ->
            if (originalRequest.header(header) != null) {
                requestBuilder.removeHeader(header)
                Log.w(TAG, "⚠️ 移除危险请求头: $header")
            }
        }
        
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
    
    /**
     * 🔍 检查 URL 安全性
     */
    private fun isUrlSafe(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            
            // 检查协议
            if (uri.scheme !in setOf("http", "https")) {
                return false
            }
            
            // 检查主机
            val host = uri.host?.lowercase()
            if (host == null || host.isEmpty()) {
                return false
            }
            
            // 检查本地地址（可选）
            if (host in setOf("localhost", "127.0.0.1", "0.0.0.0")) {
                Log.w(TAG, "⚠️ 检测到本地地址: $host")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 安全检查失败: $url", e)
            false
        }
    }
}

/**
 * 网络统计
 */
class NetworkStats {
    
    private val requestCount = AtomicLong(0)
    private val successCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val totalDuration = AtomicLong(0)
    private val totalResponseSize = AtomicLong(0)
    
    private val methodStats = mutableMapOf<String, MethodStats>()
    private val statusCodeStats = mutableMapOf<Int, AtomicLong>()
    private val recentErrors = mutableListOf<String>()
    
    /**
     * 📊 记录请求
     */
    @Synchronized
    fun recordRequest(method: String, statusCode: Int, duration: Long, responseSize: Long) {
        requestCount.incrementAndGet()
        totalDuration.addAndGet(duration)
        totalResponseSize.addAndGet(responseSize)
        
        if (statusCode in 200..299) {
            successCount.incrementAndGet()
        } else {
            errorCount.incrementAndGet()
        }
        
        // 记录方法统计
        val methodStat = methodStats.computeIfAbsent(method) { MethodStats() }
        methodStat.count.incrementAndGet()
        methodStat.totalDuration.addAndGet(duration)
        
        // 记录状态码统计
        statusCodeStats.computeIfAbsent(statusCode) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * ❌ 记录错误
     */
    @Synchronized
    fun recordError(method: String, error: Exception) {
        errorCount.incrementAndGet()
        
        val errorMessage = "${error.javaClass.simpleName}: ${error.message}"
        recentErrors.add(errorMessage)
        
        // 只保留最近的 10 个错误
        if (recentErrors.size > 10) {
            recentErrors.removeAt(0)
        }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val total = requestCount.get()
        
        return mapOf(
            "total_requests" to total,
            "success_count" to successCount.get(),
            "error_count" to errorCount.get(),
            "success_rate" to if (total > 0) successCount.get().toDouble() / total else 0.0,
            "average_duration" to if (total > 0) totalDuration.get() / total else 0L,
            "total_response_size" to totalResponseSize.get(),
            "average_response_size" to if (total > 0) totalResponseSize.get() / total else 0L,
            "method_stats" to methodStats.mapValues { it.value.getStats() },
            "status_code_stats" to statusCodeStats.mapValues { it.value.get() },
            "recent_errors" to recentErrors.toList()
        )
    }
    
    /**
     * 🧹 清理统计
     */
    @Synchronized
    fun clear() {
        requestCount.set(0)
        successCount.set(0)
        errorCount.set(0)
        totalDuration.set(0)
        totalResponseSize.set(0)
        methodStats.clear()
        statusCodeStats.clear()
        recentErrors.clear()
    }
}

/**
 * 方法统计
 */
class MethodStats {
    val count = AtomicLong(0)
    val totalDuration = AtomicLong(0)
    
    fun getStats(): Map<String, Any> {
        val requests = count.get()
        return mapOf(
            "count" to requests,
            "average_duration" to if (requests > 0) totalDuration.get() / requests else 0L
        )
    }
}
