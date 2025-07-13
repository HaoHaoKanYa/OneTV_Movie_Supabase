package top.cywin.onetv.film.proxy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.hook.HookManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 代理管理器
 * 
 * 基于 FongMi/TV 的代理管理系统
 * 负责管理多个代理服务器实例和代理配置
 * 
 * 功能：
 * - 代理服务器管理
 * - 代理配置管理
 * - 负载均衡
 * - 健康检查
 * - 统计监控
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ProxyManager(
    private val context: Context,
    private val hookManager: HookManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_MANAGER"
        private const val DEFAULT_PROXY_PORT = 8080
        private const val HEALTH_CHECK_INTERVAL = 30000L // 30秒
    }
    
    // 代理服务器实例
    private val proxyServers = ConcurrentHashMap<String, ProxyServer>()
    
    // 代理配置
    private val proxyConfigs = ConcurrentHashMap<String, ProxyConfig>()
    
    // 管理器状态
    private val isInitialized = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    
    // 协程管理
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 健康检查
    private var healthCheckJob: Job? = null
    
    // 统计信息
    private val managerStats = ProxyManagerStats()
    
    /**
     * 🔧 初始化代理管理器
     */
    fun initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "⚠️ 代理管理器已经初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 初始化代理管理器...")
            
            // 加载默认配置
            loadDefaultConfigs()
            
            // 启动默认代理服务器
            startDefaultProxyServer()
            
            // 启动健康检查
            startHealthCheck()
            
            isInitialized.set(true)
            isRunning.set(true)
            
            Log.d(TAG, "✅ 代理管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 📋 加载默认配置
     */
    private fun loadDefaultConfigs() {
        Log.d(TAG, "📋 加载默认代理配置...")
        
        // 默认 HTTP 代理配置
        val httpConfig = ProxyConfig(
            name = "default_http",
            type = ProxyType.HTTP,
            port = DEFAULT_PROXY_PORT,
            enabled = true,
            description = "默认 HTTP 代理服务器"
        )
        proxyConfigs["default_http"] = httpConfig
        
        // 默认播放器代理配置
        val playerConfig = ProxyConfig(
            name = "default_player",
            type = ProxyType.PLAYER,
            port = DEFAULT_PROXY_PORT + 1,
            enabled = true,
            description = "默认播放器代理服务器"
        )
        proxyConfigs["default_player"] = playerConfig
        
        Log.d(TAG, "✅ 默认配置加载完成，配置数量: ${proxyConfigs.size}")
    }
    
    /**
     * 🚀 启动默认代理服务器
     */
    private fun startDefaultProxyServer() {
        Log.d(TAG, "🚀 启动默认代理服务器...")
        
        val config = proxyConfigs["default_http"]
        if (config != null && config.enabled) {
            startProxyServer(config)
        }
    }
    
    /**
     * 🚀 启动代理服务器
     */
    fun startProxyServer(config: ProxyConfig): Boolean {
        return try {
            if (proxyServers.containsKey(config.name)) {
                Log.w(TAG, "⚠️ 代理服务器已存在: ${config.name}")
                return true
            }
            
            Log.d(TAG, "🚀 启动代理服务器: ${config.name}")
            
            val proxyServer = ProxyServer(config.port, hookManager)
            val actualPort = proxyServer.start()
            
            proxyServers[config.name] = proxyServer
            
            // 更新配置中的实际端口
            config.actualPort = actualPort
            
            managerStats.recordServerStart(config.name)
            
            Log.d(TAG, "✅ 代理服务器启动成功: ${config.name}, 端口: $actualPort")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理服务器启动失败: ${config.name}", e)
            managerStats.recordServerError(config.name, e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 🛑 停止代理服务器
     */
    fun stopProxyServer(name: String): Boolean {
        return try {
            val proxyServer = proxyServers.remove(name)
            if (proxyServer != null) {
                Log.d(TAG, "🛑 停止代理服务器: $name")
                proxyServer.stop()
                managerStats.recordServerStop(name)
                Log.d(TAG, "✅ 代理服务器停止成功: $name")
                true
            } else {
                Log.w(TAG, "⚠️ 代理服务器不存在: $name")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理服务器停止失败: $name", e)
            managerStats.recordServerError(name, e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 🔄 重启代理服务器
     */
    fun restartProxyServer(name: String): Boolean {
        val config = proxyConfigs[name] ?: return false
        
        stopProxyServer(name)
        return startProxyServer(config)
    }
    
    /**
     * 🔗 获取代理 URL
     */
    fun getProxyUrl(originalUrl: String, serverName: String = "default_http"): String {
        val proxyServer = proxyServers[serverName]
        return proxyServer?.getProxyUrl(originalUrl) ?: originalUrl
    }
    
    /**
     * 📋 添加代理配置
     */
    fun addProxyConfig(config: ProxyConfig): Boolean {
        return try {
            if (proxyConfigs.containsKey(config.name)) {
                Log.w(TAG, "⚠️ 代理配置已存在: ${config.name}")
                return false
            }
            
            proxyConfigs[config.name] = config
            
            if (config.enabled) {
                startProxyServer(config)
            }
            
            Log.d(TAG, "✅ 代理配置添加成功: ${config.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理配置添加失败: ${config.name}", e)
            false
        }
    }
    
    /**
     * 🗑️ 移除代理配置
     */
    fun removeProxyConfig(name: String): Boolean {
        return try {
            stopProxyServer(name)
            val removed = proxyConfigs.remove(name)
            
            if (removed != null) {
                Log.d(TAG, "✅ 代理配置移除成功: $name")
                true
            } else {
                Log.w(TAG, "⚠️ 代理配置不存在: $name")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理配置移除失败: $name", e)
            false
        }
    }
    
    /**
     * 🔧 更新代理配置
     */
    fun updateProxyConfig(config: ProxyConfig): Boolean {
        return try {
            val oldConfig = proxyConfigs[config.name]
            if (oldConfig == null) {
                Log.w(TAG, "⚠️ 代理配置不存在: ${config.name}")
                return false
            }
            
            // 如果配置有变化，重启服务器
            if (oldConfig != config) {
                stopProxyServer(config.name)
                proxyConfigs[config.name] = config
                
                if (config.enabled) {
                    startProxyServer(config)
                }
            }
            
            Log.d(TAG, "✅ 代理配置更新成功: ${config.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理配置更新失败: ${config.name}", e)
            false
        }
    }
    
    /**
     * 🏥 启动健康检查
     */
    private fun startHealthCheck() {
        healthCheckJob = scope.launch {
            while (isRunning.get()) {
                try {
                    performHealthCheck()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 健康检查异常", e)
                }
            }
        }
        
        Log.d(TAG, "🏥 健康检查启动")
    }
    
    /**
     * 🏥 执行健康检查
     */
    private suspend fun performHealthCheck() {
        Log.d(TAG, "🏥 执行健康检查...")
        
        proxyServers.forEach { (name, server) ->
            try {
                val status = server.getStatus()
                if (!status.isRunning) {
                    Log.w(TAG, "⚠️ 代理服务器未运行: $name")
                    managerStats.recordHealthCheckFail(name)
                    
                    // 尝试重启
                    val config = proxyConfigs[name]
                    if (config != null && config.enabled) {
                        Log.d(TAG, "🔄 尝试重启代理服务器: $name")
                        restartProxyServer(name)
                    }
                } else {
                    managerStats.recordHealthCheckSuccess(name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 健康检查失败: $name", e)
                managerStats.recordHealthCheckFail(name)
            }
        }
    }
    
    /**
     * 📊 获取管理器状态
     */
    fun getManagerStatus(): ProxyManagerStatus {
        val serverStatuses = proxyServers.mapValues { (_, server) ->
            server.getStatus()
        }
        
        return ProxyManagerStatus(
            isRunning = isRunning.get(),
            totalServers = proxyServers.size,
            runningServers = serverStatuses.count { it.value.isRunning },
            configs = proxyConfigs.values.toList(),
            serverStatuses = serverStatuses,
            stats = managerStats.getSnapshot()
        )
    }
    
    /**
     * 📋 获取所有代理配置
     */
    fun getAllConfigs(): List<ProxyConfig> {
        return proxyConfigs.values.toList()
    }
    
    /**
     * 📋 获取运行中的服务器
     */
    fun getRunningServers(): List<String> {
        return proxyServers.filter { (_, server) ->
            server.getStatus().isRunning
        }.keys.toList()
    }
    
    /**
     * 🧹 关闭代理管理器
     */
    fun shutdown() {
        if (!isRunning.compareAndSet(true, false)) {
            Log.w(TAG, "⚠️ 代理管理器未在运行")
            return
        }
        
        try {
            Log.d(TAG, "🧹 关闭代理管理器...")
            
            // 停止健康检查
            healthCheckJob?.cancel()
            
            // 停止所有代理服务器
            proxyServers.keys.toList().forEach { name ->
                stopProxyServer(name)
            }
            
            // 清空配置
            proxyConfigs.clear()
            
            // 取消协程
            scope.cancel()
            
            isInitialized.set(false)
            
            Log.d(TAG, "✅ 代理管理器关闭完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理管理器关闭失败", e)
        }
    }
}

/**
 * 代理类型
 */
enum class ProxyType {
    HTTP,      // HTTP 代理
    PLAYER,    // 播放器代理
    RESOURCE   // 资源代理
}

/**
 * 代理配置
 */
data class ProxyConfig(
    val name: String,
    val type: ProxyType,
    val port: Int,
    var actualPort: Int = 0,
    val enabled: Boolean = true,
    val description: String = "",
    val maxConnections: Int = 50,
    val timeout: Long = 30000L,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 代理管理器状态
 */
data class ProxyManagerStatus(
    val isRunning: Boolean,
    val totalServers: Int,
    val runningServers: Int,
    val configs: List<ProxyConfig>,
    val serverStatuses: Map<String, ProxyServerStatus>,
    val stats: Map<String, Any>
)

/**
 * 代理管理器统计
 */
class ProxyManagerStats {
    
    private val serverStarts = mutableMapOf<String, Long>()
    private val serverStops = mutableMapOf<String, Long>()
    private val serverErrors = mutableMapOf<String, MutableList<String>>()
    private val healthCheckSuccesses = mutableMapOf<String, Long>()
    private val healthCheckFails = mutableMapOf<String, Long>()
    
    fun recordServerStart(name: String) {
        serverStarts[name] = (serverStarts[name] ?: 0) + 1
    }
    
    fun recordServerStop(name: String) {
        serverStops[name] = (serverStops[name] ?: 0) + 1
    }
    
    fun recordServerError(name: String, error: String) {
        serverErrors.computeIfAbsent(name) { mutableListOf() }.add(error)
    }
    
    fun recordHealthCheckSuccess(name: String) {
        healthCheckSuccesses[name] = (healthCheckSuccesses[name] ?: 0) + 1
    }
    
    fun recordHealthCheckFail(name: String) {
        healthCheckFails[name] = (healthCheckFails[name] ?: 0) + 1
    }
    
    fun getSnapshot(): Map<String, Any> {
        return mapOf(
            "server_starts" to serverStarts.toMap(),
            "server_stops" to serverStops.toMap(),
            "server_errors" to serverErrors.mapValues { it.value.toList() },
            "health_check_successes" to healthCheckSuccesses.toMap(),
            "health_check_fails" to healthCheckFails.toMap()
        )
    }
}
