package top.cywin.onetv.film.proxy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.hook.HookManager
import java.io.*
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 本地代理服务器增强版
 * 
 * 基于 FongMi/TV 的本地代理服务器增强实现
 * 提供更强大的代理功能和性能优化
 * 
 * 功能：
 * - 高性能 HTTP 代理
 * - 智能缓存管理
 * - 流媒体优化
 * - 断点续传支持
 * - 并发连接管理
 * - 带宽控制
 * - 访问控制
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class LocalProxy(
    private val context: Context,
    private val hookManager: HookManager,
    private val config: LocalProxyConfig = LocalProxyConfig()
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_LOCAL_PROXY_SERVER"
        private const val BUFFER_SIZE = 16384 // 16KB
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    }
    
    // 服务器状态
    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var actualPort: Int = 0
    
    // 协程管理
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 连接管理
    private val activeConnections = ConcurrentHashMap<String, LocalProxyConnection>()
    private val connectionCount = AtomicLong(0)
    
    // 缓存管理
    private val cacheManager = LocalProxyCacheManager(config.maxCacheSize)
    
    // 带宽管理
    private val bandwidthManager = BandwidthManager(config.maxBandwidth)
    
    // 访问控制
    private val accessController = AccessController(config.allowedHosts)
    
    // 统计信息
    private val stats = LocalProxyStats()
    
    /**
     * 🚀 启动本地代理服务器
     */
    fun start(): Int {
        if (isRunning.get()) {
            Log.w(TAG, "⚠️ 本地代理服务器已经在运行")
            return actualPort
        }
        
        try {
            Log.d(TAG, "🚀 启动本地代理服务器...")
            
            // 创建服务器套接字
            serverSocket = ServerSocket(config.port)
            actualPort = serverSocket!!.localPort
            isRunning.set(true)
            
            // 启动服务器协程
            scope.launch {
                acceptConnections()
            }
            
            // 启动缓存清理
            scope.launch {
                cacheManager.startCleanup()
            }
            
            Log.d(TAG, "✅ 本地代理服务器启动成功，端口: $actualPort")
            return actualPort
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 本地代理服务器启动失败", e)
            throw e
        }
    }
    
    /**
     * 🛑 停止本地代理服务器
     */
    fun stop() {
        if (!isRunning.get()) {
            Log.w(TAG, "⚠️ 本地代理服务器未在运行")
            return
        }
        
        try {
            Log.d(TAG, "🛑 停止本地代理服务器...")
            
            isRunning.set(false)
            
            // 关闭服务器套接字
            serverSocket?.close()
            serverSocket = null
            
            // 关闭所有活动连接
            activeConnections.values.forEach { it.close() }
            activeConnections.clear()
            
            // 停止缓存管理
            cacheManager.stop()
            
            // 取消协程
            scope.cancel()
            
            Log.d(TAG, "✅ 本地代理服务器停止完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 本地代理服务器停止失败", e)
        }
    }
    
    /**
     * 🔗 获取代理 URL
     */
    fun getProxyUrl(originalUrl: String, options: ProxyOptions = ProxyOptions()): String {
        return if (isRunning.get()) {
            val encodedUrl = URLEncoder.encode(originalUrl, "UTF-8")
            val params = mutableListOf<String>()
            
            params.add("url=$encodedUrl")
            
            if (options.enableCache) {
                params.add("cache=1")
            }
            
            if (options.enableRangeRequest) {
                params.add("range=1")
            }
            
            if (options.userAgent.isNotEmpty()) {
                params.add("ua=${URLEncoder.encode(options.userAgent, "UTF-8")}")
            }
            
            "http://127.0.0.1:$actualPort/proxy?${params.joinToString("&")}"
        } else {
            originalUrl
        }
    }
    
    /**
     * 📊 获取服务器状态
     */
    fun getStatus(): LocalProxyStatus {
        return LocalProxyStatus(
            isRunning = isRunning.get(),
            port = actualPort,
            activeConnections = activeConnections.size,
            totalConnections = connectionCount.get(),
            cacheStats = cacheManager.getStats(),
            bandwidthStats = bandwidthManager.getStats(),
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
        // 检查连接数限制
        if (activeConnections.size >= config.maxConnections) {
            Log.w(TAG, "⚠️ 连接数已达上限，拒绝新连接")
            clientSocket.close()
            stats.recordConnectionRejected()
            return
        }
        
        // 检查访问控制
        val clientAddress = clientSocket.inetAddress.hostAddress
        if (!accessController.isAllowed(clientAddress)) {
            Log.w(TAG, "⚠️ 访问被拒绝: $clientAddress")
            clientSocket.close()
            stats.recordAccessDenied(clientAddress)
            return
        }
        
        val connectionId = "conn_${connectionCount.incrementAndGet()}"
        val connection = LocalProxyConnection(
            connectionId = connectionId,
            clientSocket = clientSocket,
            server = this,
            hookManager = hookManager,
            cacheManager = cacheManager,
            bandwidthManager = bandwidthManager,
            config = config
        )
        
        activeConnections[connectionId] = connection
        stats.recordConnectionAccepted()
        
        scope.launch {
            try {
                connection.handle()
            } finally {
                activeConnections.remove(connectionId)
                connection.close()
                stats.recordConnectionClosed()
            }
        }
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        cacheManager.clearAll()
        Log.d(TAG, "🧹 缓存已清理")
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats(): Map<String, Any> {
        return cacheManager.getStats()
    }
    
    /**
     * 🔧 更新配置
     */
    fun updateConfig(newConfig: LocalProxyConfig) {
        // 更新缓存大小
        if (newConfig.maxCacheSize != config.maxCacheSize) {
            cacheManager.updateMaxSize(newConfig.maxCacheSize)
        }
        
        // 更新带宽限制
        if (newConfig.maxBandwidth != config.maxBandwidth) {
            bandwidthManager.updateMaxBandwidth(newConfig.maxBandwidth)
        }
        
        // 更新访问控制
        if (newConfig.allowedHosts != config.allowedHosts) {
            accessController.updateAllowedHosts(newConfig.allowedHosts)
        }
        
        Log.d(TAG, "🔧 配置已更新")
    }
}

/**
 * 本地代理配置
 */
data class LocalProxyConfig(
    val port: Int = 0,
    val maxConnections: Int = 100,
    val maxCacheSize: Long = MAX_CACHE_SIZE,
    val maxBandwidth: Long = 0, // 0 表示无限制
    val allowedHosts: Set<String> = setOf("127.0.0.1", "localhost"),
    val enableCompression: Boolean = true,
    val enableRangeRequest: Boolean = true,
    val connectionTimeout: Long = 30000L,
    val readTimeout: Long = 30000L
)

/**
 * 代理选项
 */
data class ProxyOptions(
    val enableCache: Boolean = true,
    val enableRangeRequest: Boolean = true,
    val userAgent: String = "",
    val referer: String = "",
    val headers: Map<String, String> = emptyMap()
)

/**
 * 本地代理服务器状态
 */
data class LocalProxyStatus(
    val isRunning: Boolean,
    val port: Int,
    val activeConnections: Int,
    val totalConnections: Long,
    val cacheStats: Map<String, Any>,
    val bandwidthStats: Map<String, Any>,
    val stats: Map<String, Any>
)

/**
 * 本地代理统计
 */
class LocalProxyStats {
    
    private val connectionsAccepted = AtomicLong(0)
    private val connectionsRejected = AtomicLong(0)
    private val connectionsClosed = AtomicLong(0)
    private val accessDeniedCount = AtomicLong(0)
    private val accessDeniedHosts = ConcurrentHashMap<String, Long>()
    
    private val requestsTotal = AtomicLong(0)
    private val requestsSuccess = AtomicLong(0)
    private val requestsFailed = AtomicLong(0)
    private val bytesTransferred = AtomicLong(0)
    
    fun recordConnectionAccepted() {
        connectionsAccepted.incrementAndGet()
    }
    
    fun recordConnectionRejected() {
        connectionsRejected.incrementAndGet()
    }
    
    fun recordConnectionClosed() {
        connectionsClosed.incrementAndGet()
    }
    
    fun recordAccessDenied(host: String) {
        accessDeniedCount.incrementAndGet()
        accessDeniedHosts[host] = (accessDeniedHosts[host] ?: 0) + 1
    }
    
    fun recordRequest(success: Boolean, bytesTransferred: Long = 0) {
        requestsTotal.incrementAndGet()
        if (success) {
            requestsSuccess.incrementAndGet()
        } else {
            requestsFailed.incrementAndGet()
        }
        this.bytesTransferred.addAndGet(bytesTransferred)
    }
    
    fun getSnapshot(): Map<String, Any> {
        val total = requestsTotal.get()
        
        return mapOf(
            "connections_accepted" to connectionsAccepted.get(),
            "connections_rejected" to connectionsRejected.get(),
            "connections_closed" to connectionsClosed.get(),
            "access_denied_count" to accessDeniedCount.get(),
            "access_denied_hosts" to accessDeniedHosts.toMap(),
            
            "requests_total" to total,
            "requests_success" to requestsSuccess.get(),
            "requests_failed" to requestsFailed.get(),
            "success_rate" to if (total > 0) requestsSuccess.get().toDouble() / total else 0.0,
            "bytes_transferred" to bytesTransferred.get()
        )
    }
}

/**
 * 访问控制器
 */
class AccessController(private var allowedHosts: Set<String>) {
    
    fun isAllowed(host: String): Boolean {
        return allowedHosts.isEmpty() || allowedHosts.contains(host) || allowedHosts.contains("*")
    }
    
    fun updateAllowedHosts(newAllowedHosts: Set<String>) {
        allowedHosts = newAllowedHosts
    }
}
