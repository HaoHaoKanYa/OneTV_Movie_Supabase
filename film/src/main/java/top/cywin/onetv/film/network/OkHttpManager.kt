package top.cywin.onetv.film.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp 网络管理器
 * 
 * 基于 FongMi/TV 的网络请求实现
 * 提供完整的 HTTP 请求功能
 * 
 * 功能：
 * - GET/POST 请求
 * - 请求头管理
 * - 超时控制
 * - 重试机制
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class OkHttpManager {
    
    companion object {
        private const val TAG = "ONETV_FILM_HTTP_MANAGER"
        
        // 默认请求头
        private val DEFAULT_HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 11; M2007J3SC Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045714 Mobile Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3",
            "Accept-Encoding" to "gzip, deflate",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1"
        )
    }
    
    // OkHttp 客户端
    private val client: OkHttpClient
    
    init {
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(LoggingInterceptor())
            .addInterceptor(RetryInterceptor())
            .build()
        
        Log.d(TAG, "🌐 OkHttp 客户端初始化完成")
    }
    
    /**
     * 🌐 GET 请求
     */
    suspend fun getString(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🌐 GET 请求: $url")
            
            val request = buildGetRequest(url, headers)
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            
            val body = response.body?.string() ?: ""
            Log.d(TAG, "✅ GET 请求成功: $url, 响应长度: ${body.length}")
            
            body
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ GET 请求失败: $url", e)
            throw e
        }
    }
    
    /**
     * 📤 POST 请求
     */
    suspend fun postString(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        mediaType: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 POST 请求: $url")
            
            val request = buildPostRequest(url, body, headers, mediaType)
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "✅ POST 请求成功: $url, 响应长度: ${responseBody.length}")
            
            responseBody
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ POST 请求失败: $url", e)
            throw e
        }
    }
    
    /**
     * 📤 POST 表单请求
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 POST 表单请求: $url")
            
            val request = buildFormRequest(url, formData, headers)
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }
            
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "✅ POST 表单请求成功: $url, 响应长度: ${responseBody.length}")
            
            responseBody
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ POST 表单请求失败: $url", e)
            throw e
        }
    }
    
    /**
     * 🔧 构建 GET 请求
     */
    private fun buildGetRequest(url: String, headers: Map<String, String>): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
        
        // 添加默认请求头
        DEFAULT_HEADERS.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 🔧 构建 POST 请求
     */
    private fun buildPostRequest(
        url: String,
        body: String,
        headers: Map<String, String>,
        mediaType: String
    ): Request {
        val requestBody = body.toRequestBody(mediaType.toMediaType())
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
        
        // 添加默认请求头
        DEFAULT_HEADERS.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 🔧 构建表单请求
     */
    private fun buildFormRequest(
        url: String,
        formData: Map<String, String>,
        headers: Map<String, String>
    ): Request {
        val formBuilder = FormBody.Builder()
        formData.forEach { (key, value) ->
            formBuilder.add(key, value)
        }
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(formBuilder.build())
        
        // 添加默认请求头
        DEFAULT_HEADERS.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // 添加自定义请求头
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 📊 获取响应头
     */
    suspend fun getHeaders(url: String, headers: Map<String, String> = emptyMap()): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 获取响应头: $url")
            
            val request = buildGetRequest(url, headers)
            val response = client.newCall(request).execute()
            
            val responseHeaders = mutableMapOf<String, String>()
            response.headers.forEach { (name, value) ->
                responseHeaders[name] = value
            }
            
            Log.d(TAG, "✅ 响应头获取成功: $url, 头部数量: ${responseHeaders.size}")
            responseHeaders
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 响应头获取失败: $url", e)
            emptyMap()
        }
    }
    
    /**
     * 🔍 检查 URL 可访问性
     */
    suspend fun checkUrl(url: String, headers: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 检查 URL 可访问性: $url")
            
            val request = Request.Builder()
                .url(url)
                .head() // 使用 HEAD 请求
                .apply {
                    DEFAULT_HEADERS.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
            
            val response = client.newCall(request).execute()
            val isAccessible = response.isSuccessful
            
            Log.d(TAG, "🔍 URL 可访问性检查结果: $url -> $isAccessible")
            isAccessible
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 可访问性检查失败: $url", e)
            false
        }
    }
}

/**
 * 日志拦截器
 */
private class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        Log.d("ONETV_FILM_HTTP", "🌐 请求: ${request.method} ${request.url}")
        
        val response = chain.proceed(request)
        val endTime = System.currentTimeMillis()
        
        Log.d("ONETV_FILM_HTTP", "✅ 响应: ${response.code} ${request.url} (${endTime - startTime}ms)")
        
        return response
    }
}

/**
 * 重试拦截器
 */
private class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        
        repeat(maxRetries) { attempt ->
            try {
                response?.close() // 关闭之前的响应
                response = chain.proceed(request)
                
                if (response!!.isSuccessful) {
                    return response!!
                }
                
                Log.w("ONETV_FILM_HTTP", "⚠️ 请求失败，尝试重试: ${attempt + 1}/$maxRetries, ${request.url}")
                
            } catch (e: IOException) {
                exception = e
                Log.w("ONETV_FILM_HTTP", "⚠️ 请求异常，尝试重试: ${attempt + 1}/$maxRetries, ${request.url}", e)
            }
        }
        
        return response ?: throw (exception ?: IOException("Max retries exceeded"))
    }
}
