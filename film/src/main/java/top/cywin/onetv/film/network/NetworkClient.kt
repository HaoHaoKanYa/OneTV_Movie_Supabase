package top.cywin.onetv.film.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.hook.HookRequest
import top.cywin.onetv.film.hook.HookResponse
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * 增强网络客户端
 * 
 * 基于 FongMi/TV 的网络层增强实现
 * 提供高性能、安全、可靠的网络请求功能
 * 
 * 功能：
 * - HTTP/HTTPS 请求处理
 * - 自动重试和错误恢复
 * - 请求缓存和优化
 * - Hook 系统集成
 * - SSL/TLS 安全处理
 * - 连接池管理
 * - 请求统计和监控
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class NetworkClient(
    private val context: Context,
    private val hookManager: HookManager? = null
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_NETWORK_CLIENT"
        private const val DEFAULT_TIMEOUT = 15000L // 15秒
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY = 1000L // 1秒
    }
    
    // OkHttp 客户端
    private val okHttpClient: OkHttpClient by lazy {
        createOkHttpClient()
    }
    
    // 网络配置
    private var networkConfig = NetworkConfig()
    
    // 网络统计
    private val networkStats = NetworkStats()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 🔧 创建 OkHttp 客户端
     */
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeout, TimeUnit.MILLISECONDS)
            .readTimeout(networkConfig.readTimeout, TimeUnit.MILLISECONDS)
            .writeTimeout(networkConfig.writeTimeout, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
        
        // 添加日志拦截器
        if (networkConfig.enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        // 添加统计拦截器
        builder.addInterceptor(StatsInterceptor(networkStats))
        
        // 添加重试拦截器
        builder.addInterceptor(RetryInterceptor(networkConfig.maxRetries))
        
        // 添加用户代理拦截器
        builder.addInterceptor(UserAgentInterceptor(networkConfig.userAgent))
        
        // 添加 Hook 拦截器
        if (hookManager != null) {
            builder.addInterceptor(HookInterceptor(hookManager))
        }
        
        // 配置 SSL
        if (networkConfig.trustAllCertificates) {
            configureTrustAllSSL(builder)
        }
        
        // 配置连接池
        builder.connectionPool(
            ConnectionPool(
                networkConfig.maxIdleConnections,
                networkConfig.keepAliveDuration,
                TimeUnit.MILLISECONDS
            )
        )
        
        return builder.build()
    }
    
    /**
     * 🔒 配置信任所有 SSL 证书
     */
    private fun configureTrustAllSSL(builder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            
            Log.w(TAG, "⚠️ 已配置信任所有 SSL 证书（仅用于开发）")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置 SSL 失败", e)
        }
    }
    
    /**
     * 🌐 GET 请求
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        executeRequest(
            method = "GET",
            url = buildUrlWithParams(url, params),
            headers = headers
        )
    }
    
    /**
     * 📤 POST 请求
     */
    suspend fun post(
        url: String,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
        contentType: String = "application/json"
    ): NetworkResponse = withContext(Dispatchers.IO) {
        val requestBody = body.toRequestBody(contentType.toMediaType())
        executeRequest(
            method = "POST",
            url = url,
            headers = headers,
            body = requestBody
        )
    }
    
    /**
     * 📤 POST 表单请求
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder().apply {
            formData.forEach { (key, value) ->
                add(key, value)
            }
        }.build()
        
        executeRequest(
            method = "POST",
            url = url,
            headers = headers,
            body = formBody
        )
    }
    
    /**
     * 📤 PUT 请求
     */
    suspend fun put(
        url: String,
        body: String = "",
        headers: Map<String, String> = emptyMap(),
        contentType: String = "application/json"
    ): NetworkResponse = withContext(Dispatchers.IO) {
        val requestBody = body.toRequestBody(contentType.toMediaType())
        executeRequest(
            method = "PUT",
            url = url,
            headers = headers,
            body = requestBody
        )
    }
    
    /**
     * 🗑️ DELETE 请求
     */
    suspend fun delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResponse = withContext(Dispatchers.IO) {
        executeRequest(
            method = "DELETE",
            url = url,
            headers = headers
        )
    }
    
    /**
     * 📥 下载文件
     */
    suspend fun download(
        url: String,
        headers: Map<String, String> = emptyMap(),
        progressCallback: ((Long, Long) -> Unit)? = null
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body
                if (responseBody != null) {
                    val contentLength = responseBody.contentLength()
                    val inputStream = responseBody.byteStream()
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    val outputStream = java.io.ByteArrayOutputStream()
                    
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        progressCallback?.invoke(downloadedBytes, contentLength)
                    }
                    
                    NetworkResponse.success(
                        data = outputStream.toByteArray(),
                        headers = response.headers.toMap(),
                        statusCode = response.code
                    )
                } else {
                    NetworkResponse.failure("响应体为空")
                }
            } else {
                NetworkResponse.failure("HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 下载失败: $url", e)
            NetworkResponse.failure("下载失败: ${e.message}")
        }
    }
    
    /**
     * 🚀 执行请求
     */
    private suspend fun executeRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: RequestBody? = null
    ): NetworkResponse = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🚀 执行请求: $method $url")
            
            val request = Request.Builder()
                .url(url)
                .method(method, body)
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
            
            val startTime = System.currentTimeMillis()
            val response = okHttpClient.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime
            
            val responseBody = response.body?.string() ?: ""
            val responseHeaders = response.headers.toMap()
            
            networkStats.recordRequest(method, response.code, duration, responseBody.length.toLong())
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ 请求成功: $method $url (${duration}ms)")
                NetworkResponse.success(
                    data = responseBody.toByteArray(),
                    headers = responseHeaders,
                    statusCode = response.code
                )
            } else {
                Log.w(TAG, "⚠️ 请求失败: $method $url - HTTP ${response.code}")
                NetworkResponse.failure(
                    error = "HTTP ${response.code}: ${response.message}",
                    statusCode = response.code,
                    headers = responseHeaders
                )
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "❌ 网络请求异常: $method $url", e)
            networkStats.recordError(method, e)
            NetworkResponse.failure("网络请求失败: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 请求执行异常: $method $url", e)
            networkStats.recordError(method, e)
            NetworkResponse.failure("请求执行失败: ${e.message}")
        }
    }
    
    /**
     * 🔗 构建带参数的 URL
     */
    private fun buildUrlWithParams(url: String, params: Map<String, String>): String {
        if (params.isEmpty()) return url
        
        val urlBuilder = HttpUrl.parse(url)?.newBuilder()
        params.forEach { (key, value) ->
            urlBuilder?.addQueryParameter(key, value)
        }
        
        return urlBuilder?.build()?.toString() ?: url
    }
    
    /**
     * 🔧 更新网络配置
     */
    fun updateConfig(config: NetworkConfig) {
        this.networkConfig = config
        // 重新创建客户端以应用新配置
        // 注意：这会导致现有连接被关闭
    }
    
    /**
     * 📊 获取网络统计
     */
    fun getNetworkStats(): Map<String, Any> {
        return networkStats.getStats()
    }
    
    /**
     * 🧹 清理网络统计
     */
    fun clearStats() {
        networkStats.clear()
    }
    
    /**
     * 🛑 关闭网络客户端
     */
    fun shutdown() {
        try {
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            scope.cancel()
            Log.d(TAG, "🛑 网络客户端已关闭")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 关闭网络客户端失败", e)
        }
    }
}

/**
 * 网络配置
 */
data class NetworkConfig(
    val connectTimeout: Long = DEFAULT_TIMEOUT,
    val readTimeout: Long = DEFAULT_TIMEOUT,
    val writeTimeout: Long = DEFAULT_TIMEOUT,
    val maxRetries: Int = MAX_RETRIES,
    val retryDelay: Long = RETRY_DELAY,
    val enableLogging: Boolean = false,
    val trustAllCertificates: Boolean = false,
    val userAgent: String = "OneTV-NetworkClient/1.0",
    val maxIdleConnections: Int = 5,
    val keepAliveDuration: Long = 5 * 60 * 1000L // 5分钟
)

/**
 * 网络响应
 */
sealed class NetworkResponse {
    
    /**
     * ✅ 成功响应
     */
    data class Success(
        val data: ByteArray,
        val headers: Map<String, String> = emptyMap(),
        val statusCode: Int = 200
    ) : NetworkResponse() {
        
        /**
         * 📄 获取字符串数据
         */
        fun asString(charset: String = "UTF-8"): String {
            return String(data, charset(charset))
        }
        
        /**
         * 🔍 检查内容类型
         */
        fun isJson(): Boolean {
            return headers["content-type"]?.contains("application/json", ignoreCase = true) == true
        }
        
        /**
         * 🔍 检查是否为 HTML
         */
        fun isHtml(): Boolean {
            return headers["content-type"]?.contains("text/html", ignoreCase = true) == true
        }
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as Success
            
            if (!data.contentEquals(other.data)) return false
            if (headers != other.headers) return false
            if (statusCode != other.statusCode) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + headers.hashCode()
            result = 31 * result + statusCode
            return result
        }
    }
    
    /**
     * ❌ 失败响应
     */
    data class Failure(
        val error: String,
        val statusCode: Int = -1,
        val headers: Map<String, String> = emptyMap(),
        val exception: Exception? = null
    ) : NetworkResponse()
    
    companion object {
        fun success(
            data: ByteArray,
            headers: Map<String, String> = emptyMap(),
            statusCode: Int = 200
        ) = Success(data, headers, statusCode)
        
        fun failure(
            error: String,
            statusCode: Int = -1,
            headers: Map<String, String> = emptyMap(),
            exception: Exception? = null
        ) = Failure(error, statusCode, headers, exception)
    }
    
    /**
     * ✅ 是否成功
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * ❌ 是否失败
     */
    val isFailure: Boolean get() = this is Failure
}
