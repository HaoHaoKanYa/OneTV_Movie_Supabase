package top.cywin.onetv.film.network

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import top.cywin.onetv.film.network.interceptor.*
import top.cywin.onetv.film.proxy.ProxyManager
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.utils.NetworkUtils
import java.io.File
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * HTTP 客户端
 * 
 * 基于 FongMi/TV 的网络客户端实现
 * 集成拦截器、缓存、代理等功能
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class HttpClient private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_HTTP_CLIENT"
        private const val CONNECT_TIMEOUT = 15L // 15秒
        private const val READ_TIMEOUT = 30L // 30秒
        private const val WRITE_TIMEOUT = 30L // 30秒
        private const val CACHE_SIZE = 50L * 1024 * 1024 // 50MB
        
        @Volatile
        private var INSTANCE: HttpClient? = null
        
        fun getInstance(context: Context): HttpClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HttpClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // OkHttp 客户端实例
    private val okHttpClient: OkHttpClient by lazy { createOkHttpClient() }
    
    // 依赖组件
    private val proxyManager by lazy { ProxyManager(context, HookManager.getInstance(context)) }
    private val hookManager by lazy { HookManager.getInstance(context) }

    // 拦截器实例
    private val headerInterceptor = HeaderInterceptor(context)
    private val proxyInterceptor by lazy { ProxyInterceptor(context, proxyManager, hookManager) }
    private val cacheInterceptor = CacheInterceptor(context)
    
    /**
     * 🔧 创建 OkHttp 客户端
     */
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            
            // 1. 设置超时时间
            connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            
            // 2. 设置连接池
            connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            
            // 3. 设置重试
            retryOnConnectionFailure(true)
            
            // 4. 添加拦截器
            addInterceptors()
            
            // 5. 设置缓存
            setupCache()
            
            // 6. 设置 SSL
            setupSSL()
            
            // 7. 设置 DNS
            setupDNS()
            
        }.build()
    }
    
    /**
     * 🔧 添加拦截器
     */
    private fun OkHttpClient.Builder.addInterceptors() {
        // 应用级拦截器
        addInterceptor(headerInterceptor)
        addInterceptor(proxyInterceptor)
        
        // 网络级拦截器
        addNetworkInterceptor(cacheInterceptor)
        
        // 日志拦截器 (仅在调试模式下)
        if (isDebugMode()) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("$TAG-HTTP", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            addInterceptor(loggingInterceptor)
        }
    }
    
    /**
     * 🔧 设置缓存
     */
    private fun OkHttpClient.Builder.setupCache() {
        try {
            val cacheDir = File(context.cacheDir, "http_cache")
            val cache = Cache(cacheDir, CACHE_SIZE)
            cache(cache)
            Log.d(TAG, "HTTP 缓存设置完成: ${cacheDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "HTTP 缓存设置失败", e)
        }
    }
    
    /**
     * 🔧 设置 SSL
     */
    private fun OkHttpClient.Builder.setupSSL() {
        try {
            // 创建信任所有证书的 TrustManager
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            
            // 创建 SSLContext
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            // 设置 SSL 套接字工厂
            sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            
            // 设置主机名验证器
            hostnameVerifier { _, _ -> true }
            
            Log.d(TAG, "SSL 配置完成")
        } catch (e: Exception) {
            Log.e(TAG, "SSL 配置失败", e)
        }
    }
    
    /**
     * 🔧 设置 DNS
     */
    private fun OkHttpClient.Builder.setupDNS() {
        try {
            // 使用系统默认 DNS
            dns(Dns.SYSTEM)
            Log.d(TAG, "DNS 配置完成")
        } catch (e: Exception) {
            Log.e(TAG, "DNS 配置失败", e)
        }
    }
    
    /**
     * 🌐 执行 GET 请求
     */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Response {
        return executeRequest {
            Request.Builder()
                .url(url)
                .get()
                .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
                .build()
        }
    }
    
    /**
     * 🌐 执行 POST 请求
     */
    suspend fun post(
        url: String, 
        body: RequestBody, 
        headers: Map<String, String> = emptyMap()
    ): Response {
        return executeRequest {
            Request.Builder()
                .url(url)
                .post(body)
                .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
                .build()
        }
    }
    
    /**
     * 🌐 执行 POST JSON 请求
     */
    suspend fun postJson(
        url: String, 
        json: String, 
        headers: Map<String, String> = emptyMap()
    ): Response {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        return post(url, body, headers)
    }
    
    /**
     * 🌐 执行表单 POST 请求
     */
    suspend fun postForm(
        url: String, 
        params: Map<String, String>, 
        headers: Map<String, String> = emptyMap()
    ): Response {
        val formBody = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
        }.build()
        return post(url, formBody, headers)
    }
    
    /**
     * 🌐 执行自定义请求
     */
    suspend fun execute(request: Request): Response {
        return executeRequest { request }
    }
    
    /**
     * 🌐 执行请求的通用方法
     */
    private suspend fun executeRequest(requestBuilder: () -> Request): Response {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = requestBuilder()
                val startTime = System.currentTimeMillis()
                
                Log.d(TAG, "执行请求: ${request.method} ${request.url}")
                
                val response = okHttpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                
                Log.d(TAG, "请求完成: ${response.code} ${request.url} (${duration}ms)")
                
                response
                
            } catch (e: Exception) {
                Log.e(TAG, "请求失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    /**
     * 🔧 创建请求构建器
     */
    fun newRequestBuilder(): Request.Builder {
        return Request.Builder()
    }
    
    /**
     * 🔧 创建表单构建器
     */
    fun newFormBuilder(): FormBody.Builder {
        return FormBody.Builder()
    }
    
    /**
     * 🔧 创建多部分构建器
     */
    fun newMultipartBuilder(): MultipartBody.Builder {
        return MultipartBody.Builder().setType(MultipartBody.FORM)
    }
    
    /**
     * 🔧 获取客户端实例
     */
    fun getOkHttpClient(): OkHttpClient {
        return okHttpClient
    }
    
    /**
     * 🔧 检查是否为调试模式
     */
    private fun isDebugMode(): Boolean {
        return try {
            val appInfo = context.applicationInfo
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 📊 获取网络统计信息
     */
    fun getNetworkStats(): Map<String, Any> {
        return try {
            val cache = okHttpClient.cache
            mapOf(
                "cache_size" to (cache?.size() ?: 0L),
                "cache_max_size" to (cache?.maxSize() ?: 0L),
                "cache_hit_count" to (cache?.hitCount() ?: 0),
                "cache_request_count" to (cache?.requestCount() ?: 0),
                "connection_pool_idle_count" to okHttpClient.connectionPool.idleConnectionCount(),
                "connection_pool_total_count" to okHttpClient.connectionPool.connectionCount(),
                "network_available" to NetworkUtils.isNetworkAvailable(context)
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取网络统计失败", e)
            emptyMap()
        }
    }
    
    /**
     * 🧹 清理资源
     */
    fun cleanup() {
        try {
            okHttpClient.cache?.close()
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
            Log.d(TAG, "HTTP 客户端资源清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "HTTP 客户端资源清理失败", e)
        }
    }
}
