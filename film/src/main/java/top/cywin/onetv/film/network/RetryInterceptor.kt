package top.cywin.onetv.film.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicLong

/**
 * 重试拦截器
 * 基于 FongMi/TV 标准实现
 * 
 * 提供智能的网络请求重试功能
 * 
 * 功能：
 * - 智能重试策略
 * - 指数退避算法
 * - 错误类型识别
 * - 重试条件判断
 * - 统计监控
 * - 自适应超时
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 10000L,
    private val backoffMultiplier: Double = 2.0,
    private val retryOnConnectionFailure: Boolean = true,
    private val retryOnTimeout: Boolean = true,
    private val retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_RETRY_INTERCEPTOR"
        private const val RETRY_COUNT_HEADER = "X-Retry-Count"
        private const val RETRY_DELAY_HEADER = "X-Retry-Delay"
    }
    
    // 统计信息
    private val totalRequests = AtomicLong(0)
    private val retriedRequests = AtomicLong(0)
    private val successAfterRetry = AtomicLong(0)
    private val finalFailures = AtomicLong(0)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        totalRequests.incrementAndGet()
        
        var lastException: IOException? = null
        var response: Response? = null
        var retryCount = 0
        
        while (retryCount <= maxRetries) {
            try {
                // 添加重试信息到请求头
                val requestWithRetryInfo = if (retryCount > 0) {
                    originalRequest.newBuilder()
                        .addHeader(RETRY_COUNT_HEADER, retryCount.toString())
                        .build()
                } else {
                    originalRequest
                }
                
                Log.d(TAG, "🌐 执行请求 (尝试 ${retryCount + 1}/${maxRetries + 1}): ${originalRequest.url}")
                
                response = chain.proceed(requestWithRetryInfo)
                
                // 检查响应是否需要重试
                if (shouldRetryResponse(response, retryCount)) {
                    Log.w(TAG, "⚠️ 响应需要重试: ${response.code} - ${originalRequest.url}")
                    response.close()
                    
                    if (retryCount < maxRetries) {
                        val delay = calculateDelay(retryCount)
                        Log.d(TAG, "⏰ 等待 ${delay}ms 后重试")
                        Thread.sleep(delay)
                        retryCount++
                        retriedRequests.incrementAndGet()
                        continue
                    } else {
                        finalFailures.incrementAndGet()
                        Log.e(TAG, "❌ 达到最大重试次数，请求失败: ${originalRequest.url}")
                        return response
                    }
                } else {
                    // 请求成功
                    if (retryCount > 0) {
                        successAfterRetry.incrementAndGet()
                        Log.d(TAG, "✅ 重试后请求成功: ${originalRequest.url}")
                    }
                    
                    // 添加重试信息到响应头
                    return response.newBuilder()
                        .addHeader(RETRY_COUNT_HEADER, retryCount.toString())
                        .build()
                }
                
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "❌ 请求异常 (尝试 ${retryCount + 1}/${maxRetries + 1}): ${e.message}")
                
                // 检查异常是否需要重试
                if (shouldRetryException(e, retryCount)) {
                    if (retryCount < maxRetries) {
                        val delay = calculateDelay(retryCount)
                        Log.d(TAG, "⏰ 等待 ${delay}ms 后重试")
                        Thread.sleep(delay)
                        retryCount++
                        retriedRequests.incrementAndGet()
                        continue
                    } else {
                        finalFailures.incrementAndGet()
                        Log.e(TAG, "❌ 达到最大重试次数，抛出异常: ${originalRequest.url}")
                        throw e
                    }
                } else {
                    finalFailures.incrementAndGet()
                    Log.e(TAG, "❌ 异常不可重试，直接抛出: ${e.javaClass.simpleName}")
                    throw e
                }
            }
        }
        
        // 如果到这里，说明所有重试都失败了
        finalFailures.incrementAndGet()
        if (lastException != null) {
            throw lastException
        } else {
            return response!!
        }
    }
    
    /**
     * 🔍 判断响应是否需要重试
     */
    private fun shouldRetryResponse(response: Response, retryCount: Int): Boolean {
        // 检查状态码
        if (retryableStatusCodes.contains(response.code)) {
            Log.d(TAG, "🔍 状态码 ${response.code} 需要重试")
            return true
        }
        
        // 检查特殊情况
        when (response.code) {
            429 -> {
                // 限流，检查 Retry-After 头
                val retryAfter = response.header("Retry-After")
                if (retryAfter != null) {
                    Log.d(TAG, "🔍 收到限流响应，Retry-After: $retryAfter")
                    return true
                }
            }
            
            503 -> {
                // 服务不可用，通常是临时的
                Log.d(TAG, "🔍 服务不可用，尝试重试")
                return true
            }
        }
        
        return false
    }
    
    /**
     * 🔍 判断异常是否需要重试
     */
    private fun shouldRetryException(exception: IOException, retryCount: Int): Boolean {
        return when (exception) {
            is SocketTimeoutException -> {
                if (retryOnTimeout) {
                    Log.d(TAG, "🔍 超时异常，尝试重试")
                    true
                } else {
                    Log.d(TAG, "🔍 超时异常，但重试已禁用")
                    false
                }
            }
            
            is UnknownHostException -> {
                if (retryOnConnectionFailure) {
                    Log.d(TAG, "🔍 DNS解析失败，尝试重试")
                    true
                } else {
                    Log.d(TAG, "🔍 DNS解析失败，但连接失败重试已禁用")
                    false
                }
            }
            
            is IOException -> {
                if (retryOnConnectionFailure && isRetryableIOException(exception)) {
                    Log.d(TAG, "🔍 可重试的IO异常: ${exception.javaClass.simpleName}")
                    true
                } else {
                    Log.d(TAG, "🔍 不可重试的IO异常: ${exception.javaClass.simpleName}")
                    false
                }
            }
            
            else -> {
                Log.d(TAG, "🔍 未知异常类型，不重试: ${exception.javaClass.simpleName}")
                false
            }
        }
    }
    
    /**
     * 🔍 判断IO异常是否可重试
     */
    private fun isRetryableIOException(exception: IOException): Boolean {
        val message = exception.message?.lowercase() ?: ""
        
        // 可重试的错误消息模式
        val retryablePatterns = listOf(
            "connection reset",
            "connection refused",
            "network is unreachable",
            "no route to host",
            "broken pipe",
            "connection timed out",
            "read timed out",
            "write timed out"
        )
        
        return retryablePatterns.any { pattern ->
            message.contains(pattern)
        }
    }
    
    /**
     * ⏰ 计算重试延迟
     */
    private fun calculateDelay(retryCount: Int): Long {
        // 指数退避算法
        val delay = (baseDelayMs * Math.pow(backoffMultiplier, retryCount.toDouble())).toLong()
        
        // 添加随机抖动，避免惊群效应
        val jitter = (delay * 0.1 * Math.random()).toLong()
        
        val finalDelay = (delay + jitter).coerceAtMost(maxDelayMs)
        
        Log.d(TAG, "⏰ 计算重试延迟: 基础=${baseDelayMs}ms, 重试次数=$retryCount, 最终延迟=${finalDelay}ms")
        
        return finalDelay
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val total = totalRequests.get()
        val retried = retriedRequests.get()
        val successAfterRetryCount = successAfterRetry.get()
        val failures = finalFailures.get()
        
        val retryRate = if (total > 0) (retried.toDouble() / total * 100) else 0.0
        val successAfterRetryRate = if (retried > 0) (successAfterRetryCount.toDouble() / retried * 100) else 0.0
        val failureRate = if (total > 0) (failures.toDouble() / total * 100) else 0.0
        
        return mapOf(
            "total_requests" to total,
            "retried_requests" to retried,
            "success_after_retry" to successAfterRetryCount,
            "final_failures" to failures,
            "retry_rate" to retryRate,
            "success_after_retry_rate" to successAfterRetryRate,
            "failure_rate" to failureRate,
            "max_retries" to maxRetries,
            "base_delay_ms" to baseDelayMs,
            "max_delay_ms" to maxDelayMs,
            "backoff_multiplier" to backoffMultiplier
        )
    }
    
    /**
     * 🔄 重置统计
     */
    fun resetStats() {
        totalRequests.set(0)
        retriedRequests.set(0)
        successAfterRetry.set(0)
        finalFailures.set(0)
        Log.d(TAG, "🔄 重试拦截器统计已重置")
    }
    
    /**
     * 📋 获取配置信息
     */
    fun getConfig(): Map<String, Any> {
        return mapOf(
            "max_retries" to maxRetries,
            "base_delay_ms" to baseDelayMs,
            "max_delay_ms" to maxDelayMs,
            "backoff_multiplier" to backoffMultiplier,
            "retry_on_connection_failure" to retryOnConnectionFailure,
            "retry_on_timeout" to retryOnTimeout,
            "retryable_status_codes" to retryableStatusCodes.toList()
        )
    }
    
    /**
     * 🔧 创建配置构建器
     */
    class Builder {
        private var maxRetries: Int = 3
        private var baseDelayMs: Long = 1000L
        private var maxDelayMs: Long = 10000L
        private var backoffMultiplier: Double = 2.0
        private var retryOnConnectionFailure: Boolean = true
        private var retryOnTimeout: Boolean = true
        private var retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504)
        
        fun maxRetries(maxRetries: Int) = apply { this.maxRetries = maxRetries }
        fun baseDelay(delayMs: Long) = apply { this.baseDelayMs = delayMs }
        fun maxDelay(delayMs: Long) = apply { this.maxDelayMs = delayMs }
        fun backoffMultiplier(multiplier: Double) = apply { this.backoffMultiplier = multiplier }
        fun retryOnConnectionFailure(retry: Boolean) = apply { this.retryOnConnectionFailure = retry }
        fun retryOnTimeout(retry: Boolean) = apply { this.retryOnTimeout = retry }
        fun retryableStatusCodes(codes: Set<Int>) = apply { this.retryableStatusCodes = codes }
        
        fun build(): RetryInterceptor {
            return RetryInterceptor(
                maxRetries = maxRetries,
                baseDelayMs = baseDelayMs,
                maxDelayMs = maxDelayMs,
                backoffMultiplier = backoffMultiplier,
                retryOnConnectionFailure = retryOnConnectionFailure,
                retryOnTimeout = retryOnTimeout,
                retryableStatusCodes = retryableStatusCodes
            )
        }
    }
    
    companion object {
        /**
         * 🏗️ 创建默认重试拦截器
         */
        fun createDefault(): RetryInterceptor {
            return Builder().build()
        }
        
        /**
         * 🏗️ 创建快速重试拦截器（适用于实时性要求高的场景）
         */
        fun createFast(): RetryInterceptor {
            return Builder()
                .maxRetries(2)
                .baseDelay(500L)
                .maxDelay(2000L)
                .build()
        }
        
        /**
         * 🏗️ 创建耐心重试拦截器（适用于重要但不紧急的请求）
         */
        fun createPatient(): RetryInterceptor {
            return Builder()
                .maxRetries(5)
                .baseDelay(2000L)
                .maxDelay(30000L)
                .backoffMultiplier(1.5)
                .build()
        }
        
        /**
         * 🏗️ 创建保守重试拦截器（仅重试明确可重试的错误）
         */
        fun createConservative(): RetryInterceptor {
            return Builder()
                .maxRetries(2)
                .retryOnConnectionFailure(false)
                .retryableStatusCodes(setOf(429, 503, 504))
                .build()
        }
    }
}
