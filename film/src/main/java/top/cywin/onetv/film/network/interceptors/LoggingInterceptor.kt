package top.cywin.onetv.film.network.interceptors

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 日志拦截器
 * 
 * 基于 FongMi/TV 的网络日志机制实现
 * 负责网络请求和响应的详细日志记录
 * 
 * 核心功能：
 * - 请求日志记录
 * - 响应日志记录
 * - 性能监控
 * - 错误追踪
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class LoggingInterceptor(
    private val logLevel: LogLevel = LogLevel.BASIC,
    private val enableBodyLogging: Boolean = false,
    private val maxBodyLength: Int = 1024
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_LOGGING_INTERCEPTOR"
    }
    
    enum class LogLevel {
        NONE,       // 不记录日志
        BASIC,      // 基本信息 (URL, 状态码, 耗时)
        HEADERS,    // 包含请求头和响应头
        BODY        // 包含请求体和响应体
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        if (logLevel == LogLevel.NONE) {
            return chain.proceed(request)
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 记录请求信息
            logRequest(request)
            
            // 执行请求
            val response = chain.proceed(request)
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // 记录响应信息
            logResponse(response, duration)
            
            return response
            
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.e(TAG, "❌ 请求失败: ${request.url} (耗时: ${duration}ms)", e)
            throw e
        }
    }
    
    /**
     * 记录请求信息
     */
    private fun logRequest(request: okhttp3.Request) {
        when (logLevel) {
            LogLevel.BASIC -> {
                Log.d(TAG, "🚀 ${request.method} ${request.url}")
            }
            
            LogLevel.HEADERS, LogLevel.BODY -> {
                Log.d(TAG, "🚀 ${request.method} ${request.url}")
                
                // 记录请求头
                val headers = request.headers
                if (headers.size > 0) {
                    Log.d(TAG, "📤 请求头:")
                    for (i in 0 until headers.size) {
                        Log.d(TAG, "  ${headers.name(i)}: ${headers.value(i)}")
                    }
                }
                
                // 记录请求体
                if (logLevel == LogLevel.BODY && enableBodyLogging) {
                    logRequestBody(request)
                }
            }
            
            LogLevel.NONE -> {
                // 不记录
            }
        }
    }
    
    /**
     * 记录响应信息
     */
    private fun logResponse(response: Response, duration: Long) {
        when (logLevel) {
            LogLevel.BASIC -> {
                Log.d(TAG, "✅ ${response.code} ${response.message} (耗时: ${duration}ms)")
            }
            
            LogLevel.HEADERS, LogLevel.BODY -> {
                Log.d(TAG, "✅ ${response.code} ${response.message} (耗时: ${duration}ms)")
                
                // 记录响应头
                val headers = response.headers
                if (headers.size > 0) {
                    Log.d(TAG, "📥 响应头:")
                    for (i in 0 until headers.size) {
                        Log.d(TAG, "  ${headers.name(i)}: ${headers.value(i)}")
                    }
                }
                
                // 记录响应体
                if (logLevel == LogLevel.BODY && enableBodyLogging) {
                    logResponseBody(response)
                }
            }
            
            LogLevel.NONE -> {
                // 不记录
            }
        }
    }
    
    /**
     * 记录请求体
     */
    private fun logRequestBody(request: okhttp3.Request) {
        try {
            val requestBody = request.body
            if (requestBody == null) {
                Log.d(TAG, "📤 请求体: (空)")
                return
            }
            
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            
            val contentType = requestBody.contentType()
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            
            val bodyString = buffer.readString(charset)
            val truncatedBody = if (bodyString.length > maxBodyLength) {
                "${bodyString.substring(0, maxBodyLength)}... (截断)"
            } else {
                bodyString
            }
            
            Log.d(TAG, "📤 请求体 (${requestBody.contentLength()} bytes):")
            Log.d(TAG, truncatedBody)
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 记录请求体失败", e)
        }
    }
    
    /**
     * 记录响应体
     */
    private fun logResponseBody(response: Response) {
        try {
            val responseBody = response.body
            if (responseBody == null) {
                Log.d(TAG, "📥 响应体: (空)")
                return
            }
            
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // 读取整个响应体
            
            val buffer = source.buffer
            val contentType = responseBody.contentType()
            val charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            
            if (isPlaintext(buffer)) {
                val bodyString = buffer.clone().readString(charset)
                val truncatedBody = if (bodyString.length > maxBodyLength) {
                    "${bodyString.substring(0, maxBodyLength)}... (截断)"
                } else {
                    bodyString
                }
                
                Log.d(TAG, "📥 响应体 (${buffer.size} bytes):")
                Log.d(TAG, truncatedBody)
            } else {
                Log.d(TAG, "📥 响应体: 二进制数据 (${buffer.size} bytes)")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 记录响应体失败", e)
        }
    }
    
    /**
     * 检查是否为纯文本
     */
    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = minOf(buffer.size, 64)
            buffer.copyTo(prefix, 0, byteCount)
            
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
