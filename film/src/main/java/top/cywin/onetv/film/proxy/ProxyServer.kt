package top.cywin.onetv.film.proxy

import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.hook.HookPlayerUrl
import top.cywin.onetv.film.hook.HookRequest
import top.cywin.onetv.film.hook.HookResponse
import top.cywin.onetv.film.network.OkHttpManager
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 代理服务器
 * 
 * 基于 FongMi/TV 的本地代理服务器实现
 * 提供 HTTP 代理、播放器代理和资源代理功能
 * 
 * 功能：
 * - HTTP 请求代理
 * - 播放器链接代理
 * - 资源文件代理
 * - Hook 集成
 * - 缓存管理
 * - 统计监控
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ProxyServer(
    private val port: Int = 0, // 0 表示自动分配端口
    private val hookManager: HookManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_SERVER"
        private const val BUFFER_SIZE = 8192
        private const val MAX_CONNECTIONS = 50
    }
    
    // 服务器状态
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var actualPort: Int = 0
    
    // 协程管理
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // HTTP 客户端
    private val httpManager = OkHttpManager()
    
    // 连接管理
    private val activeConnections = ConcurrentHashMap<String, ProxyConnection>()
    private val connectionCount = AtomicLong(0)
    
    // 统计信息
    private val stats = ProxyStats()
    
    /**
     * 🚀 启动代理服务器
     */
    fun start(): Int {
        if (isRunning.get()) {
            Log.w(TAG, "⚠️ 代理服务器已经在运行")
            return actualPort
        }
        
        try {
            Log.d(TAG, "🚀 启动代理服务器...")
            
            // 创建服务器套接字
            serverSocket = ServerSocket(port)
            actualPort = serverSocket!!.localPort
            isRunning.set(true)
            
            // 启动服务器协程
            scope.launch {
                acceptConnections()
            }
            
            Log.d(TAG, "✅ 代理服务器启动成功，端口: $actualPort")
            return actualPort
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理服务器启动失败", e)
            throw e
        }
    }
    
    /**
     * 🛑 停止代理服务器
     */
    fun stop() {
        if (!isRunning.get()) {
            Log.w(TAG, "⚠️ 代理服务器未在运行")
            return
        }
        
        try {
            Log.d(TAG, "🛑 停止代理服务器...")
            
            isRunning.set(false)
            
            // 关闭服务器套接字
            serverSocket?.close()
            serverSocket = null
            
            // 关闭所有活动连接
            activeConnections.values.forEach { it.close() }
            activeConnections.clear()
            
            // 取消协程
            scope.cancel()
            
            Log.d(TAG, "✅ 代理服务器停止完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理服务器停止失败", e)
        }
    }
    
    /**
     * 🔗 获取代理 URL
     */
    fun getProxyUrl(originalUrl: String): String {
        return if (isRunning.get()) {
            "http://127.0.0.1:$actualPort/proxy?url=${URLEncoder.encode(originalUrl, "UTF-8")}"
        } else {
            originalUrl
        }
    }
    
    /**
     * 📊 获取服务器状态
     */
    fun getStatus(): ProxyServerStatus {
        return ProxyServerStatus(
            isRunning = isRunning.get(),
            port = actualPort,
            activeConnections = activeConnections.size,
            totalConnections = connectionCount.get(),
            stats = stats.getSnapshot()
        )
    }
    
    /**
     * 🔄 接受连接
     */
    private suspend fun acceptConnections() {
        while (isRunning.get()) {
            try {
                val clientSocket = serverSocket?.accept()
                if (clientSocket != null) {
                    handleConnection(clientSocket)
                }
            } catch (e: SocketException) {
                if (isRunning.get()) {
                    Log.e(TAG, "❌ 接受连接失败", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 处理连接异常", e)
            }
        }
    }
    
    /**
     * 🔗 处理连接
     */
    private fun handleConnection(clientSocket: Socket) {
        if (activeConnections.size >= MAX_CONNECTIONS) {
            Log.w(TAG, "⚠️ 连接数已达上限，拒绝新连接")
            clientSocket.close()
            return
        }
        
        val connectionId = "conn_${connectionCount.incrementAndGet()}"
        val connection = ProxyConnection(connectionId, clientSocket, this)
        
        activeConnections[connectionId] = connection
        
        scope.launch {
            try {
                connection.handle()
            } finally {
                activeConnections.remove(connectionId)
                connection.close()
            }
        }
    }
    
    /**
     * 🌐 处理 HTTP 请求
     */
    internal suspend fun handleHttpRequest(request: ProxyRequest): ProxyResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "🌐 处理 HTTP 请求: ${request.method} ${request.url}")
            
            // 创建 Hook 请求
            val hookRequest = HookRequest(
                url = request.url,
                method = request.method,
                headers = request.headers.toMutableMap(),
                body = request.body
            )
            
            // 执行请求 Hook
            val processedRequest = hookManager.executeRequestHooks(hookRequest)
            
            // 发送实际请求
            val response = when (processedRequest.method.uppercase()) {
                "GET" -> httpManager.getString(processedRequest.url, processedRequest.headers)
                "POST" -> httpManager.postString(processedRequest.url, processedRequest.body, processedRequest.headers)
                else -> throw UnsupportedOperationException("不支持的请求方法: ${processedRequest.method}")
            }
            
            // 创建 Hook 响应
            val hookResponse = HookResponse(
                statusCode = 200,
                body = response
            )
            
            // 执行响应 Hook
            val processedResponse = hookManager.executeResponseHooks(hookResponse)
            
            // 记录统计
            val duration = System.currentTimeMillis() - startTime
            stats.recordRequest(request.method, duration, true)
            
            return ProxyResponse(
                statusCode = processedResponse.statusCode,
                headers = processedResponse.headers,
                body = processedResponse.body
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP 请求处理失败: ${request.url}", e)
            
            val duration = System.currentTimeMillis() - startTime
            stats.recordRequest(request.method, duration, false)
            
            return ProxyResponse(
                statusCode = 500,
                body = "代理请求失败: ${e.message}"
            )
        }
    }
    
    /**
     * ▶️ 处理播放器请求
     */
    internal suspend fun handlePlayerRequest(url: String, headers: Map<String, String>): ProxyResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "▶️ 处理播放器请求: $url")
            
            // 创建 Hook 播放器 URL
            val hookPlayerUrl = HookPlayerUrl(
                originalUrl = url,
                headers = headers.toMutableMap()
            )
            
            // 执行播放器 Hook
            val processedPlayerUrl = hookManager.executePlayerHooks(hookPlayerUrl)
            
            // 获取实际播放内容
            val response = httpManager.getString(processedPlayerUrl.processedUrl, processedPlayerUrl.headers)
            
            // 记录统计
            val duration = System.currentTimeMillis() - startTime
            stats.recordPlayer(duration, true)
            
            return ProxyResponse(
                statusCode = 200,
                headers = processedPlayerUrl.headers,
                body = response
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 播放器请求处理失败: $url", e)
            
            val duration = System.currentTimeMillis() - startTime
            stats.recordPlayer(duration, false)
            
            return ProxyResponse(
                statusCode = 500,
                body = "播放器请求失败: ${e.message}"
            )
        }
    }
    
    /**
     * 📁 处理资源请求
     */
    internal suspend fun handleResourceRequest(path: String): ProxyResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "📁 处理资源请求: $path")
            
            // 这里可以处理本地资源文件
            // 例如：缓存的视频片段、字幕文件等
            
            val duration = System.currentTimeMillis() - startTime
            stats.recordResource(duration, true)
            
            return ProxyResponse(
                statusCode = 404,
                body = "资源未找到: $path"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 资源请求处理失败: $path", e)
            
            val duration = System.currentTimeMillis() - startTime
            stats.recordResource(duration, false)
            
            return ProxyResponse(
                statusCode = 500,
                body = "资源请求失败: ${e.message}"
            )
        }
    }
}

/**
 * 代理请求数据
 */
data class ProxyRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String = ""
)

/**
 * 代理响应数据
 */
data class ProxyResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

/**
 * 代理服务器状态
 */
data class ProxyServerStatus(
    val isRunning: Boolean,
    val port: Int,
    val activeConnections: Int,
    val totalConnections: Long,
    val stats: Map<String, Any>
)

/**
 * 代理统计
 */
class ProxyStats {
    
    private val httpRequests = AtomicLong(0)
    private val httpSuccesses = AtomicLong(0)
    private val httpTotalDuration = AtomicLong(0)
    
    private val playerRequests = AtomicLong(0)
    private val playerSuccesses = AtomicLong(0)
    private val playerTotalDuration = AtomicLong(0)
    
    private val resourceRequests = AtomicLong(0)
    private val resourceSuccesses = AtomicLong(0)
    private val resourceTotalDuration = AtomicLong(0)
    
    fun recordRequest(method: String, duration: Long, success: Boolean) {
        httpRequests.incrementAndGet()
        httpTotalDuration.addAndGet(duration)
        if (success) httpSuccesses.incrementAndGet()
    }
    
    fun recordPlayer(duration: Long, success: Boolean) {
        playerRequests.incrementAndGet()
        playerTotalDuration.addAndGet(duration)
        if (success) playerSuccesses.incrementAndGet()
    }
    
    fun recordResource(duration: Long, success: Boolean) {
        resourceRequests.incrementAndGet()
        resourceTotalDuration.addAndGet(duration)
        if (success) resourceSuccesses.incrementAndGet()
    }
    
    fun getSnapshot(): Map<String, Any> {
        val httpReqs = httpRequests.get()
        val playerReqs = playerRequests.get()
        val resourceReqs = resourceRequests.get()
        
        return mapOf(
            "http_requests" to httpReqs,
            "http_successes" to httpSuccesses.get(),
            "http_success_rate" to if (httpReqs > 0) httpSuccesses.get().toDouble() / httpReqs else 0.0,
            "http_avg_duration" to if (httpReqs > 0) httpTotalDuration.get() / httpReqs else 0L,
            
            "player_requests" to playerReqs,
            "player_successes" to playerSuccesses.get(),
            "player_success_rate" to if (playerReqs > 0) playerSuccesses.get().toDouble() / playerReqs else 0.0,
            "player_avg_duration" to if (playerReqs > 0) playerTotalDuration.get() / playerReqs else 0L,
            
            "resource_requests" to resourceReqs,
            "resource_successes" to resourceSuccesses.get(),
            "resource_success_rate" to if (resourceReqs > 0) resourceSuccesses.get().toDouble() / resourceReqs else 0.0,
            "resource_avg_duration" to if (resourceReqs > 0) resourceTotalDuration.get() / resourceReqs else 0L
        )
    }
}
