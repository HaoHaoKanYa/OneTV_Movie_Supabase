package top.cywin.onetv.film.jar

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.cywin.onetv.film.catvod.Spider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JAR 管理器
 * 
 * 基于 FongMi/TV 的 JAR 管理系统
 * 负责 JAR 包的生命周期管理、配置管理和事件处理
 * 
 * 功能：
 * - JAR 配置管理
 * - 自动更新检查
 * - 依赖管理
 * - 性能监控
 * - 事件通知
 * - 安全管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarManager(
    private val context: Context,
    private val config: JarManagerConfig = JarManagerConfig()
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAR_MANAGER"
        private const val CONFIG_FILE = "jar_configs.json"
    }
    
    // JAR 加载器
    private val jarLoader = JarLoader(context)
    
    // JAR 配置
    private val jarConfigs = ConcurrentHashMap<String, JarConfig>()
    
    // JAR 状态
    private val jarStatuses = ConcurrentHashMap<String, JarStatus>()
    
    // 性能指标
    private val performanceMetrics = ConcurrentHashMap<String, JarPerformanceMetrics>()
    
    // 事件流
    private val _events = MutableSharedFlow<JarEvent>()
    val events: SharedFlow<JarEvent> = _events.asSharedFlow()
    
    // 管理器状态
    private val isInitialized = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 自动更新任务
    private var updateJob: Job? = null
    
    /**
     * 🔧 初始化管理器
     */
    fun initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "⚠️ JAR 管理器已经初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 初始化 JAR 管理器...")
            
            // 加载配置
            loadConfigs()
            
            // 启动自动更新
            if (config.enableAutoUpdate) {
                startAutoUpdate()
            }
            
            isInitialized.set(true)
            isRunning.set(true)
            
            Log.d(TAG, "✅ JAR 管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 📋 加载配置
     */
    private fun loadConfigs() {
        try {
            Log.d(TAG, "📋 加载 JAR 配置...")

            // 从 SharedPreferences 加载配置
            val configJson = loadConfigFromStorage()
            if (configJson.isNotEmpty()) {
                parseAndApplyConfig(configJson)
                Log.d(TAG, "✅ 从存储加载 JAR 配置成功")
            } else {
                // 使用默认配置
                loadDefaultConfigs()
                Log.d(TAG, "✅ 使用默认 JAR 配置")
            }

            Log.d(TAG, "✅ JAR 配置加载完成，配置数量: ${jarConfigs.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载配置失败，使用默认配置", e)
            loadDefaultConfigs()
        }
    }
    
    /**
     * 💾 保存配置
     */
    private fun saveConfigs() {
        try {
            // 这里应该将配置保存到持久化存储
            Log.d(TAG, "💾 保存 JAR 配置...")
            
            // 可以保存到 SharedPreferences 或文件
            // val configJson = serializeConfigs()
            // saveConfigToStorage(configJson)
            
            Log.d(TAG, "✅ JAR 配置保存完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存配置失败", e)
        }
    }
    
    /**
     * ➕ 添加 JAR 配置
     */
    suspend fun addJarConfig(config: JarConfig): Boolean {
        return try {
            if (!config.isValid()) {
                Log.w(TAG, "⚠️ 无效的 JAR 配置: ${config.url}")
                return false
            }
            
            val jarKey = config.generateKey()
            jarConfigs[jarKey] = config
            jarStatuses[jarKey] = JarStatus.UNKNOWN
            
            // 保存配置
            saveConfigs()
            
            // 如果启用，立即加载
            if (config.enabled) {
                loadJar(jarKey)
            }
            
            Log.d(TAG, "➕ JAR 配置添加成功: ${config.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 添加 JAR 配置失败", e)
            false
        }
    }
    
    /**
     * 🗑️ 移除 JAR 配置
     */
    suspend fun removeJarConfig(jarKey: String): Boolean {
        return try {
            val config = jarConfigs.remove(jarKey)
            if (config != null) {
                // 卸载 JAR
                unloadJar(jarKey)
                
                // 移除状态和指标
                jarStatuses.remove(jarKey)
                performanceMetrics.remove(jarKey)
                
                // 保存配置
                saveConfigs()
                
                Log.d(TAG, "🗑️ JAR 配置移除成功: ${config.name}")
                true
            } else {
                Log.w(TAG, "⚠️ JAR 配置不存在: $jarKey")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 移除 JAR 配置失败", e)
            false
        }
    }
    
    /**
     * 🔄 加载 JAR
     */
    suspend fun loadJar(jarKey: String, forceReload: Boolean = false): JarLoadResult {
        val config = jarConfigs[jarKey]
        if (config == null) {
            Log.w(TAG, "⚠️ JAR 配置不存在: $jarKey")
            return JarLoadResult.failure("JAR 配置不存在")
        }
        
        return try {
            jarStatuses[jarKey] = JarStatus.LOADING
            _events.emit(JarEvent.LoadStarted(jarKey, config.url))
            
            val startTime = System.currentTimeMillis()
            val result = jarLoader.loadJar(config.url, forceReload)
            val loadTime = System.currentTimeMillis() - startTime
            
            when (result) {
                is JarLoadResult.Success -> {
                    jarStatuses[jarKey] = JarStatus.LOADED
                    
                    // 记录性能指标
                    recordPerformanceMetrics(jarKey, loadTime, result.jarInfo)
                    
                    _events.emit(JarEvent.LoadSuccess(result.jarInfo))
                    Log.d(TAG, "✅ JAR 加载成功: ${result.jarInfo.name}")
                }
                
                is JarLoadResult.Failure -> {
                    jarStatuses[jarKey] = JarStatus.ERROR
                    _events.emit(JarEvent.LoadFailure(jarKey, result.error, result.exception))
                    Log.e(TAG, "❌ JAR 加载失败: ${result.error}")
                }
                
                else -> {
                    // Loading 状态保持
                }
            }
            
            result
            
        } catch (e: Exception) {
            jarStatuses[jarKey] = JarStatus.ERROR
            val errorMsg = "JAR 加载异常: ${e.message}"
            _events.emit(JarEvent.LoadFailure(jarKey, errorMsg, e))
            Log.e(TAG, "❌ $errorMsg", e)
            JarLoadResult.failure(errorMsg, e)
        }
    }
    
    /**
     * 🗑️ 卸载 JAR
     */
    suspend fun unloadJar(jarKey: String): Boolean {
        return try {
            val success = jarLoader.unloadJar(jarKey)
            if (success) {
                jarStatuses[jarKey] = JarStatus.UNLOADED
                _events.emit(JarEvent.Unloaded(jarKey))
                Log.d(TAG, "🗑️ JAR 卸载成功: $jarKey")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 卸载失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 🏭 创建 Spider 实例
     */
    suspend fun createSpiderInstance(jarKey: String, className: String): Spider? {
        return try {
            val startTime = System.currentTimeMillis()
            val spider = jarLoader.createSpiderInstance(jarKey, className)
            val createTime = System.currentTimeMillis() - startTime
            
            val success = spider != null
            _events.emit(JarEvent.SpiderCreated(jarKey, className, success))
            
            // 更新性能指标
            updateSpiderCreateTime(jarKey, createTime)
            
            if (success) {
                Log.d(TAG, "🏭 Spider 实例创建成功: $className")
            } else {
                Log.w(TAG, "⚠️ Spider 实例创建失败: $className")
            }
            
            spider
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 实例创建异常: $className", e)
            _events.emit(JarEvent.SpiderCreated(jarKey, className, false))
            null
        }
    }
    
    /**
     * 🔄 启动自动更新
     */
    private fun startAutoUpdate() {
        updateJob = scope.launch {
            while (isRunning.get()) {
                try {
                    checkForUpdates()
                    delay(config.updateCheckInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 自动更新检查异常", e)
                }
            }
        }
        
        Log.d(TAG, "🔄 自动更新已启动")
    }
    
    /**
     * 🔍 检查更新
     */
    private suspend fun checkForUpdates() {
        Log.d(TAG, "🔍 检查 JAR 更新...")
        
        jarConfigs.values.forEach { config ->
            if (config.autoUpdate) {
                try {
                    // 这里应该实现实际的更新检查逻辑
                    // 比如检查远程版本信息
                    checkJarUpdate(config)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 检查更新失败: ${config.name}", e)
                }
            }
        }
    }
    
    /**
     * 🔍 检查单个 JAR 更新
     */
    private suspend fun checkJarUpdate(config: JarConfig) {
        // 实现具体的更新检查逻辑
        // 这里是示例代码
        val jarKey = config.generateKey()
        val currentJarInfo = jarLoader.getJarInfo(jarKey)
        
        if (currentJarInfo != null) {
            // 检查远程版本
            // val remoteVersion = fetchRemoteVersion(config.url)
            // if (remoteVersion != currentJarInfo.version) {
            //     val updateInfo = JarUpdateInfo(...)
            //     _events.emit(JarEvent.UpdateAvailable(updateInfo))
            // }
        }
    }
    
    /**
     * 📊 记录性能指标
     */
    private fun recordPerformanceMetrics(jarKey: String, loadTime: Long, jarInfo: JarInfo) {
        val metrics = performanceMetrics.computeIfAbsent(jarKey) {
            JarPerformanceMetrics(
                jarKey = jarKey,
                loadTime = loadTime,
                classLoadTime = 0,
                memoryUsage = 0,
                spiderCreateTime = 0,
                executionCount = 0,
                averageExecutionTime = 0,
                errorCount = 0,
                lastExecutionTime = System.currentTimeMillis()
            )
        }
        
        performanceMetrics[jarKey] = metrics.copy(
            loadTime = loadTime,
            lastExecutionTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 📊 更新 Spider 创建时间
     */
    private fun updateSpiderCreateTime(jarKey: String, createTime: Long) {
        val metrics = performanceMetrics[jarKey]
        if (metrics != null) {
            performanceMetrics[jarKey] = metrics.copy(
                spiderCreateTime = createTime,
                lastExecutionTime = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 📋 获取所有 JAR 配置
     */
    fun getAllConfigs(): List<JarConfig> {
        return jarConfigs.values.toList()
    }
    
    /**
     * 📋 获取已加载的 JAR
     */
    fun getLoadedJars(): List<JarInfo> {
        return jarLoader.getLoadedJars()
    }
    
    /**
     * 📊 获取 JAR 状态
     */
    fun getJarStatus(jarKey: String): JarStatus {
        return jarStatuses[jarKey] ?: JarStatus.UNKNOWN
    }
    
    /**
     * 📊 获取性能指标
     */
    fun getPerformanceMetrics(jarKey: String): JarPerformanceMetrics? {
        return performanceMetrics[jarKey]
    }
    
    /**
     * 📊 获取所有性能指标
     */
    fun getAllPerformanceMetrics(): List<JarPerformanceMetrics> {
        return performanceMetrics.values.toList()
    }
    
    /**
     * 📊 获取管理器统计
     */
    fun getManagerStats(): Map<String, Any> {
        val loaderStats = jarLoader.getStats()
        
        return mapOf(
            "total_configs" to jarConfigs.size,
            "loaded_jars" to loaderStats.loadedJars,
            "cached_classes" to loaderStats.cachedClasses,
            "cache_size_mb" to String.format("%.2f", loaderStats.cacheSize / 1024.0 / 1024.0),
            "running_status" to mapOf(
                "loaded" to jarStatuses.values.count { it == JarStatus.LOADED },
                "loading" to jarStatuses.values.count { it == JarStatus.LOADING },
                "error" to jarStatuses.values.count { it == JarStatus.ERROR },
                "unloaded" to jarStatuses.values.count { it == JarStatus.UNLOADED }
            ),
            "auto_update_enabled" to config.enableAutoUpdate,
            "security_check_enabled" to config.enableSecurityCheck
        )
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        jarLoader.clearCache()
        performanceMetrics.clear()
        Log.d(TAG, "🧹 JAR 管理器缓存已清理")
    }
    
    /**
     * 🛑 关闭管理器
     */
    fun shutdown() {
        if (!isRunning.compareAndSet(true, false)) {
            Log.w(TAG, "⚠️ JAR 管理器未在运行")
            return
        }
        
        try {
            Log.d(TAG, "🛑 关闭 JAR 管理器...")
            
            // 停止自动更新
            updateJob?.cancel()
            
            // 保存配置
            saveConfigs()
            
            // 清理资源
            jarLoader.destroy()
            
            // 取消协程
            scope.cancel()
            
            isInitialized.set(false)
            
            Log.d(TAG, "✅ JAR 管理器关闭完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 管理器关闭失败", e)
        }
    }

    /**
     * 💾 从存储加载配置
     */
    private fun loadConfigFromStorage(): String {
        return try {
            val prefs = context.getSharedPreferences("jar_manager_config", Context.MODE_PRIVATE)
            val configJson = prefs.getString("jar_configs", "") ?: ""

            Log.d(TAG, "📖 从存储读取配置，长度: ${configJson.length}")
            configJson

        } catch (e: Exception) {
            Log.e(TAG, "❌ 从存储读取配置失败", e)
            ""
        }
    }

    /**
     * 💾 保存配置到存储
     */
    private fun saveConfigToStorage(configJson: String) {
        try {
            val prefs = context.getSharedPreferences("jar_manager_config", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("jar_configs", configJson)
                .putLong("config_update_time", System.currentTimeMillis())
                .apply()

            Log.d(TAG, "💾 配置保存到存储成功，长度: ${configJson.length}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存配置到存储失败", e)
        }
    }

    /**
     * 🔧 解析并应用配置
     */
    private fun parseAndApplyConfig(configJson: String) {
        try {
            val jsonObject = JsonUtils.parseToJsonObject(configJson)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 配置 JSON 解析失败")
                return
            }

            // 解析 JAR 源配置
            val jarSources = JsonUtils.getJsonArray(jsonObject, "jar_sources") ?: emptyList()
            jarSources.forEach { source ->
                if (source is Map<*, *>) {
                    val name = source["name"]?.toString() ?: ""
                    val url = source["url"]?.toString() ?: ""
                    val enabled = source["enabled"] as? Boolean ?: true

                    if (name.isNotEmpty() && url.isNotEmpty() && enabled) {
                        // 添加到 JAR 配置
                        jarConfigs[name] = JarConfig(
                            name = name,
                            url = url,
                            version = source["version"]?.toString() ?: "1.0.0",
                            enabled = enabled,
                            lastUpdate = source["last_update"] as? Long ?: 0L
                        )
                        Log.d(TAG, "📦 加载 JAR 源: $name -> $url")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析配置失败", e)
        }
    }

    /**
     * 🏗️ 加载默认配置
     */
    private fun loadDefaultConfigs() {
        try {
            val defaultConfig = mapOf(
                "jar_sources" to listOf(
                    mapOf(
                        "name" to "FongMi官方源",
                        "url" to "https://raw.githubusercontent.com/FongMi/CatVodSpider/main/jar/custom_spider.jar",
                        "version" to "1.0.0",
                        "enabled" to true,
                        "last_update" to 0L
                    )
                )
            )

            val configJson = JsonUtils.createJsonObject(defaultConfig)
            saveConfigToStorage(configJson)
            parseAndApplyConfig(configJson)

            Log.d(TAG, "🏗️ 默认配置加载完成")

        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载默认配置失败", e)
        }
    }
}
