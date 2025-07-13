package top.cywin.onetv.film.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.data.models.VodSite
import java.util.concurrent.ConcurrentHashMap

/**
 * 引擎管理器
 * 
 * 基于 FongMi/TV 的多引擎架构实现
 * 管理所有解析引擎，提供智能回退机制
 * 
 * 支持的引擎：
 * - JavaScript 引擎 (QuickJS)
 * - XPath 引擎
 * - Python 引擎
 * - Java 引擎
 * 
 * 功能：
 * - 引擎自动选择
 * - 多引擎回退
 * - 性能监控
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EngineManager {
    
    companion object {
        private const val TAG = "ONETV_FILM_ENGINE_MANAGER"
    }
    
    // 引擎实例缓存
    private val engines = ConcurrentHashMap<EngineType, Engine>()
    
    // 引擎性能统计
    private val engineStats = ConcurrentHashMap<EngineType, EngineStats>()
    
    // 是否已初始化
    private var isInitialized = false
    
    /**
     * 🔧 初始化引擎管理器
     */
    suspend fun initialize(context: Context? = null) = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "📌 引擎管理器已初始化，跳过重复初始化")
            return@withContext
        }
        
        try {
            Log.d(TAG, "🔧 初始化引擎管理器...")
            
            // 初始化所有引擎
            initializeEngines(context)
            
            // 初始化统计
            initializeStats()
            
            isInitialized = true
            Log.d(TAG, "✅ 引擎管理器初始化完成")
            Log.d(TAG, "🎯 可用引擎: ${engines.keys.joinToString(", ")}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 引擎管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * ⚡ 执行解析操作（带回退机制）
     */
    suspend fun executeWithFallback(
        site: VodSite,
        operation: String,
        params: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        checkInitialized()
        
        val engineOrder = determineEngineOrder(site)
        Log.d(TAG, "🎯 解析引擎顺序: ${engineOrder.joinToString(" -> ")}")
        
        var lastException: Exception? = null
        
        for (engineType in engineOrder) {
            try {
                val engine = engines[engineType]
                if (engine == null) {
                    Log.w(TAG, "⚠️ 引擎不可用: $engineType")
                    continue
                }
                
                Log.d(TAG, "🚀 尝试使用引擎: $engineType")
                val startTime = System.currentTimeMillis()
                
                val result = engine.execute(site, operation, params)
                
                val duration = System.currentTimeMillis() - startTime
                recordEngineSuccess(engineType, duration)
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ 引擎执行成功: $engineType (${duration}ms)")
                    return@withContext result
                } else {
                    Log.w(TAG, "⚠️ 引擎执行失败: $engineType", result.exceptionOrNull())
                    lastException = result.exceptionOrNull() as? Exception
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "❌ 引擎异常: $engineType", e)
                recordEngineFailure(engineType)
                lastException = e
                continue
            }
        }
        
        Log.e(TAG, "💥 所有引擎都失败了")
        Result.failure(lastException ?: Exception("All engines failed"))
    }
    
    /**
     * 🎯 确定引擎执行顺序
     */
    private fun determineEngineOrder(site: VodSite): List<EngineType> {
        return when {
            // JavaScript 文件优先使用 JavaScript 引擎
            site.api.endsWith(".js") -> listOf(
                EngineType.JAVASCRIPT,
                EngineType.XPATH,
                EngineType.JAVA
            )
            
            // 类型3（自定义Spider）优先使用 XPath 引擎
            site.type == 3 -> listOf(
                EngineType.XPATH,
                EngineType.JAVASCRIPT,
                EngineType.JAVA
            )
            
            // Python 脚本优先使用 Python 引擎
            site.api.contains("python") || site.api.endsWith(".py") -> listOf(
                EngineType.PYTHON,
                EngineType.JAVASCRIPT,
                EngineType.XPATH
            )
            
            // AppYs 接口优先使用 Java 引擎
            site.type == 1 -> listOf(
                EngineType.JAVA,
                EngineType.XPATH,
                EngineType.JAVASCRIPT
            )
            
            // 默认顺序
            else -> listOf(
                EngineType.JAVA,
                EngineType.XPATH,
                EngineType.JAVASCRIPT,
                EngineType.PYTHON
            )
        }
    }
    
    /**
     * 🔧 初始化所有引擎
     */
    private suspend fun initializeEngines(context: Context?) {
        Log.d(TAG, "🔧 初始化所有引擎...")
        
        // 初始化 JavaScript 引擎
        try {
            val jsEngine = JavaScriptEngine()
            jsEngine.initialize(context)
            engines[EngineType.JAVASCRIPT] = jsEngine
            Log.d(TAG, "✅ JavaScript 引擎初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 引擎初始化失败", e)
        }
        
        // 初始化 XPath 引擎
        try {
            val xpathEngine = XPathEngine()
            xpathEngine.initialize(context)
            engines[EngineType.XPATH] = xpathEngine
            Log.d(TAG, "✅ XPath 引擎初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ XPath 引擎初始化失败", e)
        }
        
        // 初始化 Python 引擎
        try {
            val pythonEngine = PythonEngine()
            pythonEngine.initialize(context)
            engines[EngineType.PYTHON] = pythonEngine
            Log.d(TAG, "✅ Python 引擎初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Python 引擎初始化失败", e)
        }
        
        // 初始化 Java 引擎
        try {
            val javaEngine = JavaEngine()
            javaEngine.initialize(context)
            engines[EngineType.JAVA] = javaEngine
            Log.d(TAG, "✅ Java 引擎初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Java 引擎初始化失败", e)
        }
        
        Log.d(TAG, "🎯 引擎初始化完成，可用引擎数量: ${engines.size}")
    }
    
    /**
     * 📊 初始化统计
     */
    private fun initializeStats() {
        EngineType.values().forEach { engineType ->
            engineStats[engineType] = EngineStats()
        }
    }
    
    /**
     * 📈 记录引擎成功
     */
    private fun recordEngineSuccess(engineType: EngineType, duration: Long) {
        engineStats[engineType]?.let { stats ->
            stats.successCount++
            stats.totalDuration += duration
            stats.lastUsed = System.currentTimeMillis()
        }
    }
    
    /**
     * 📉 记录引擎失败
     */
    private fun recordEngineFailure(engineType: EngineType) {
        engineStats[engineType]?.let { stats ->
            stats.failureCount++
            stats.lastUsed = System.currentTimeMillis()
        }
    }
    
    /**
     * 🔍 获取引擎
     */
    fun getEngine(engineType: EngineType): Engine? {
        return engines[engineType]
    }
    
    /**
     * 📊 获取引擎统计
     */
    fun getEngineStats(): Map<EngineType, EngineStats> {
        return engineStats.toMap()
    }
    
    /**
     * 📊 获取引擎状态
     */
    fun getEngineStatus(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "available_engines" to engines.keys.map { it.name },
            "engine_count" to engines.size,
            "stats" to engineStats.mapKeys { it.key.name }
        )
    }
    
    /**
     * 🔍 检查引擎是否可用
     */
    fun isEngineAvailable(engineType: EngineType): Boolean {
        return engines.containsKey(engineType)
    }
    
    /**
     * ✅ 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("EngineManager not initialized")
        }
    }
    
    /**
     * 🛑 关闭引擎管理器
     */
    fun shutdown() {
        Log.d(TAG, "🛑 关闭引擎管理器...")
        
        engines.values.forEach { engine ->
            try {
                engine.cleanup()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 引擎清理失败", e)
            }
        }
        
        engines.clear()
        engineStats.clear()
        isInitialized = false
        
        Log.d(TAG, "✅ 引擎管理器已关闭")
    }
}

/**
 * 引擎类型枚举
 */
enum class EngineType {
    JAVASCRIPT,  // JavaScript 引擎 (QuickJS)
    XPATH,       // XPath 引擎
    PYTHON,      // Python 引擎
    JAVA         // Java 引擎
}

/**
 * 引擎统计数据
 */
data class EngineStats(
    var successCount: Long = 0,
    var failureCount: Long = 0,
    var totalDuration: Long = 0,
    var lastUsed: Long = 0
) {
    val averageDuration: Long
        get() = if (successCount > 0) totalDuration / successCount else 0
    
    val successRate: Double
        get() = if (successCount + failureCount > 0) {
            successCount.toDouble() / (successCount + failureCount)
        } else 0.0
}

/**
 * 引擎接口
 */
interface Engine {
    suspend fun initialize(context: Context?)
    suspend fun execute(site: VodSite, operation: String, params: Map<String, Any>): Result<String>
    fun cleanup()
}
