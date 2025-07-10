package top.cywin.onetv.movie.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import top.cywin.onetv.movie.data.cache.VodCacheManager
import top.cywin.onetv.movie.data.database.MovieDatabase
import top.cywin.onetv.movie.data.models.VodItem
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 性能优化工具类
 * 提供内存管理、性能监控、资源优化等功能
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class PerformanceOptimizer(
    private val context: Context,
    private val cacheManager: VodCacheManager,
    private val database: MovieDatabase
) {
    
    private val optimizerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 内存监控
    private var memoryThreshold = 0.8f // 内存使用阈值80%
    private var isMemoryOptimizationEnabled = true
    
    // 图片缓存优化
    private val imageCache = ConcurrentHashMap<String, WeakReference<Any>>()
    private val maxImageCacheSize = 50
    
    // 性能监控数据
    private val performanceMetrics = mutableMapOf<String, Long>()
    
    init {
        startMemoryMonitoring()
        startPerformanceMonitoring()
    }

    /**
     * 启动内存监控
     */
    private fun startMemoryMonitoring() {
        optimizerScope.launch {
            while (isActive) {
                try {
                    val runtime = Runtime.getRuntime()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val maxMemory = runtime.maxMemory()
                    val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
                    
                    if (memoryUsageRatio > memoryThreshold && isMemoryOptimizationEnabled) {
                        performMemoryOptimization()
                    }
                    
                    // 每30秒检查一次
                    delay(30_000)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 执行内存优化
     */
    private suspend fun performMemoryOptimization() = withContext(Dispatchers.IO) {
        try {
            println("🔧 执行内存优化...")
            
            // 1. 清理过期缓存
            cacheManager.clearExpired()
            
            // 2. 清理图片缓存
            cleanImageCache()
            
            // 3. 清理数据库过期数据
            cleanExpiredDatabaseData()
            
            // 4. 建议垃圾回收
            System.gc()
            
            println("✅ 内存优化完成")
            
        } catch (e: Exception) {
            println("❌ 内存优化失败: ${e.message}")
        }
    }

    /**
     * 清理图片缓存
     */
    private fun cleanImageCache() {
        val iterator = imageCache.iterator()
        var cleanedCount = 0
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                cleanedCount++
            }
        }
        
        // 如果缓存仍然过大，清理最旧的条目
        if (imageCache.size > maxImageCacheSize) {
            val entriesToRemove = imageCache.size - maxImageCacheSize
            val keys = imageCache.keys.take(entriesToRemove)
            keys.forEach { imageCache.remove(it) }
            cleanedCount += entriesToRemove
        }
        
        if (cleanedCount > 0) {
            println("🧹 清理了 $cleanedCount 个图片缓存条目")
        }
    }

    /**
     * 清理数据库过期数据
     */
    private suspend fun cleanExpiredDatabaseData() {
        try {
            val expireTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L // 30天前
            
            // 清理过期的搜索历史
            database.searchHistoryDao().deleteExpiredSearchHistory(expireTime)
            
            // 清理过期的缓存数据
            database.cacheDataDao().deleteExpiredCache()
            
            println("🗑️ 清理了过期的数据库数据")
            
        } catch (e: Exception) {
            println("❌ 数据库清理失败: ${e.message}")
        }
    }

    /**
     * 启动性能监控
     */
    private fun startPerformanceMonitoring() {
        optimizerScope.launch {
            while (isActive) {
                try {
                    // 监控关键性能指标
                    monitorCachePerformance()
                    monitorDatabasePerformance()
                    monitorMemoryUsage()
                    
                    // 每5分钟监控一次
                    delay(5 * 60 * 1000)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 监控缓存性能
     */
    private suspend fun monitorCachePerformance() {
        val startTime = System.currentTimeMillis()
        
        // 测试缓存读写性能
        val testKey = "perf_monitor_test"
        val testData = "performance_test_data"
        
        cacheManager.putCache(testKey, testData, 60 * 1000)
        cacheManager.getCache(testKey, String::class.java)
        
        val cacheOperationTime = System.currentTimeMillis() - startTime
        performanceMetrics["cache_operation_time"] = cacheOperationTime
        
        // 获取缓存统计
        val cacheStats = cacheManager.getCacheStats()
        val hitRate = cacheManager.getCacheHitRate()
        
        performanceMetrics["cache_hit_rate"] = (hitRate * 100).toLong()
        
        // 如果缓存性能下降，进行优化
        if (cacheOperationTime > 100 || hitRate < 0.7) {
            optimizeCachePerformance()
        }
    }

    /**
     * 优化缓存性能
     */
    private suspend fun optimizeCachePerformance() {
        try {
            println("🔧 优化缓存性能...")
            
            // 清理过期缓存
            cacheManager.clearExpired()
            
            println("✅ 缓存性能优化完成")
            
        } catch (e: Exception) {
            println("❌ 缓存性能优化失败: ${e.message}")
        }
    }

    /**
     * 监控数据库性能
     */
    private suspend fun monitorDatabasePerformance() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // 测试数据库查询性能
        try {
            database.favoriteDao().getFavoriteCount()
            database.watchHistoryDao().getHistoryCount()
            
            val dbOperationTime = System.currentTimeMillis() - startTime
            performanceMetrics["db_operation_time"] = dbOperationTime
            
            // 如果数据库性能下降，进行优化
            if (dbOperationTime > 500) {
                optimizeDatabasePerformance()
            }
            
        } catch (e: Exception) {
            println("❌ 数据库性能监控失败: ${e.message}")
        }
    }

    /**
     * 优化数据库性能
     */
    private suspend fun optimizeDatabasePerformance() = withContext(Dispatchers.IO) {
        try {
            println("🔧 优化数据库性能...")
            
            // 执行VACUUM优化数据库
            database.openHelper.writableDatabase.execSQL("VACUUM")
            
            // 清理过期数据
            cleanExpiredDatabaseData()
            
            println("✅ 数据库性能优化完成")
            
        } catch (e: Exception) {
            println("❌ 数据库性能优化失败: ${e.message}")
        }
    }

    /**
     * 监控内存使用
     */
    private fun monitorMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
        
        performanceMetrics["memory_usage_mb"] = usedMemory / 1024 / 1024
        performanceMetrics["memory_usage_percent"] = (memoryUsageRatio * 100).toLong()
    }

    /**
     * 优化图片加载
     */
    fun optimizeImageLoading(imageUrl: String): Flow<String> = flow {
        // 检查缓存
        val cachedImage = imageCache[imageUrl]?.get()
        if (cachedImage != null) {
            emit(imageUrl)
            return@flow
        }
        
        // 控制并发加载
        delay(50) // 防止过快请求
        
        // 缓存图片引用
        imageCache[imageUrl] = WeakReference(imageUrl)
        
        // 清理过大的缓存
        if (imageCache.size > maxImageCacheSize) {
            cleanImageCache()
        }
        
        emit(imageUrl)
    }.flowOn(Dispatchers.IO)

    /**
     * 优化列表加载
     */
    fun optimizeListLoading(items: List<VodItem>, pageSize: Int = 20): Flow<List<VodItem>> = flow {
        // 分页加载，避免一次性加载过多数据
        items.chunked(pageSize).forEach { chunk ->
            emit(chunk)
            delay(10) // 给UI时间渲染
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 防抖动处理
     */
    fun <T> Flow<T>.debounce(timeoutMillis: Long): Flow<T> = 
        this.debounce(timeoutMillis)

    /**
     * 获取性能报告
     */
    fun getPerformanceReport(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val currentMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return mapOf<String, Any>(
            "current_memory_mb" to (currentMemory / 1024 / 1024),
            "max_memory_mb" to (maxMemory / 1024 / 1024),
            "memory_usage_percent" to ((currentMemory.toFloat() / maxMemory.toFloat()) * 100).toInt(),
            "image_cache_size" to imageCache.size,
            "cache_hit_rate" to (performanceMetrics["cache_hit_rate"] ?: 0),
            "cache_operation_time" to (performanceMetrics["cache_operation_time"] ?: 0),
            "db_operation_time" to (performanceMetrics["db_operation_time"] ?: 0),
            "optimization_enabled" to isMemoryOptimizationEnabled
        )
    }

    /**
     * 设置内存优化开关
     */
    fun setMemoryOptimizationEnabled(enabled: Boolean) {
        isMemoryOptimizationEnabled = enabled
    }

    /**
     * 设置内存阈值
     */
    fun setMemoryThreshold(threshold: Float) {
        memoryThreshold = threshold.coerceIn(0.5f, 0.95f)
    }

    /**
     * 手动触发优化
     */
    fun triggerOptimization() {
        optimizerScope.launch {
            performMemoryOptimization()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        optimizerScope.cancel()
        imageCache.clear()
        performanceMetrics.clear()
    }
}
