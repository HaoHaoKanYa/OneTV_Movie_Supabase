package top.cywin.onetv.film.network.interceptors

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 重试拦截器
 * 
 * 基于 FongMi/TV 的网络重试机制实现
 * 负责网络请求的自动重试处理
 * 
 * 核心功能：
 * - 自动重试失败请求
 * - 智能重试策略
 * - 重试间隔控制
 * - 错误类型判断
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 1000L,
    private val enableBackoff: Boolean = true
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_RETRY_INTERCEPTOR"
        
        // 可重试的错误类型
        private val RETRYABLE_EXCEPTIONS = setOf(
            SocketTimeoutException::class.java,
            UnknownHostException::class.java,
            IOException::class.java
        )
        
        // 可重试的 HTTP 状态码
        private val RETRYABLE_STATUS_CODES = setOf(
            408, // Request Timeout
            429, // Too Many Requests
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: Exception? = null
        var response: Response? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                Log.d(TAG, "🔄 请求尝试 ${attempt + 1}/${maxRetries + 1}: ${request.url}")
                
                // 执行请求
                response?.close() // 关闭之前的响应
                response = chain.proceed(request)
                
                // 检查响应状态
                if (response!!.isSuccessful || !isRetryableStatusCode(response!!.code)) {
                    if (attempt > 0) {
                        Log.d(TAG, "✅ 重试成功: ${request.url} (尝试 ${attempt + 1})")
                    }
                    return response!!
                }
                
                Log.w(TAG, "⚠️ 请求失败，状态码: ${response!!.code}")
                lastException = IOException("HTTP ${response!!.code}: ${response!!.message}")
                
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 请求异常: ${e.message}")
                lastException = e
                
                // 检查是否为可重试的异常
                if (!isRetryableException(e)) {
                    Log.d(TAG, "❌ 不可重试的异常，直接抛出")
                    throw e
                }
            }
            
            // 最后一次尝试，不需要延迟
            if (attempt < maxRetries) {
                val delay = if (enableBackoff) {
                    retryDelayMs * (attempt + 1) // 递增延迟
                } else {
                    retryDelayMs // 固定延迟
                }
                
                Log.d(TAG, "⏳ 等待 ${delay}ms 后重试")
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("重试被中断", e)
                }
            }
        }
        
        // 所有重试都失败
        Log.e(TAG, "❌ 所有重试都失败: ${request.url}")
        throw lastException ?: IOException("所有重试都失败")
    }
    
    /**
     * 检查是否为可重试的异常
     */
    private fun isRetryableException(exception: Exception): Boolean {
        return RETRYABLE_EXCEPTIONS.any { retryableClass ->
            retryableClass.isAssignableFrom(exception.javaClass)
        }
    }
    
    /**
     * 检查是否为可重试的状态码
     */
    private fun isRetryableStatusCode(statusCode: Int): Boolean {
        return statusCode in RETRYABLE_STATUS_CODES
    }
}
