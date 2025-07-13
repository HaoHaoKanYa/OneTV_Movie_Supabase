package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.cywin.onetv.film.concurrent.ConcurrentUtils
import top.cywin.onetv.film.utils.DateTimeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存优化器
 * 
 * 基于 FongMi/TV 的缓存优化系统实现
 * 提供缓存预热、智能清理、性能优化等功能
 * 
 * 功能：
 * - 缓存预热
 * - 智能清理
 * - 性能监控
 * - 自动优化
 * - 缓存分析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class CacheOptimizer(
    private val context: Context,
    private val cacheManager: CacheManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_CACHE_OPTIMIZER"
        private const val WARMUP_BATCH_SIZE = 5
        private const val ANALYSIS_INTERVAL = 3600000L // 1小时
        private const val OPTIMIZATION_INTERVAL = 1800000L // 30分钟
    }
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 预热配置
    private var warmupConfig = CacheWarmupConfig()
    
    // 优化统计
    private val optimizationStats = OptimizationStats()
    
    // 事件流
    private val _events = MutableSharedFlow<CacheOptimizationEvent>()
    val events: SharedFlow<CacheOptimizationEvent> = _events.asSharedFlow()
    
    // 分析任务
    private var analysisJob: Job? = null
    private var optimizationJob: Job? = null
    
    // 访问模式分析
    private val accessPatterns = ConcurrentHashMap<String, AccessPattern>()
    
    init {
        startAnalysisTask()
        startOptimizationTask()
        Log.d(TAG, "🏗️ 缓存优化器初始化完成")
    }
    
    /**
     * 🔥 配置缓存预热
     */
    fun configureWarmup(config: CacheWarmupConfig) {
        warmupConfig = config
        Log.d(TAG, "🔥 缓存预热配置已更新")
    }
    
    /**
     * 🔥 执行缓存预热
     */
    suspend fun performWarmup(
        warmupKeys: List<String> = warmupConfig.warmupKeys,
        warmupProvider: suspend (String) -> Pair<Any?, CacheStrategy>?
    ) {
        if (!warmupConfig.enabled || warmupKeys.isEmpty()) {
            Log.d(TAG, "🔥 缓存预热已禁用或无预热键")
            return
        }
        
        Log.d(TAG, "🔥 开始缓存预热，键数量: ${warmupKeys.size}")
        
        try {
            _events.emit(CacheOptimizationEvent.WarmupStarted(warmupKeys.size))
            
            // 延迟启动预热
            delay(warmupConfig.warmupDelay)
            
            val startTime = System.currentTimeMillis()
            var successCount = 0
            var errorCount = 0
            
            // 并行预热
            val results = ConcurrentUtils.parallelMapCatching(
                items = warmupKeys,
                maxConcurrency = warmupConfig.maxConcurrency
            ) { key ->
                withTimeout(warmupConfig.timeout) {
                    val result = warmupProvider(key)
                    if (result != null) {
                        val (value, strategy) = result
                        if (value != null) {
                            cacheManager.put(key, value, strategy = strategy)
                            Log.d(TAG, "🔥 预热成功: $key")
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
            
            // 统计结果
            results.forEach { result ->
                if (result.isSuccess && result.getOrNull() == true) {
                    successCount++
                } else {
                    errorCount++
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            optimizationStats.recordWarmup(successCount, errorCount, duration)
            
            _events.emit(CacheOptimizationEvent.WarmupCompleted(successCount, errorCount, duration))
            
            Log.d(TAG, "✅ 缓存预热完成，成功: $successCount, 失败: $errorCount, 耗时: ${duration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存预热失败", e)
            _events.emit(CacheOptimizationEvent.WarmupFailed(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * 🧹 智能清理缓存
     */
    suspend fun performSmartCleanup() {
        Log.d(TAG, "🧹 开始智能缓存清理...")
        
        try {
            val startTime = System.currentTimeMillis()
            var cleanedCount = 0
            var freedSize = 0L
            
            _events.emit(CacheOptimizationEvent.CleanupStarted)
            
            // 分析访问模式
            val patterns = analyzeAccessPatterns()
            
            // 清理策略：
            // 1. 清理过期条目
            // 2. 清理长时间未访问的条目
            // 3. 清理访问频率低的条目
            
            val currentTime = System.currentTimeMillis()
            val cleanupThreshold = currentTime - 7 * 24 * 60 * 60 * 1000L // 7天
            
            patterns.forEach { (key, pattern) ->
                if (pattern.shouldCleanup(currentTime, cleanupThreshold)) {
                    if (cacheManager.remove(key)) {
                        cleanedCount++
                        freedSize += pattern.estimatedSize
                        Log.d(TAG, "🗑️ 清理缓存条目: $key")
                    }
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            optimizationStats.recordCleanup(cleanedCount, freedSize, duration)
            
            _events.emit(CacheOptimizationEvent.CleanupCompleted(cleanedCount, freedSize, duration))
            
            Log.d(TAG, "✅ 智能清理完成，清理条目: $cleanedCount, 释放空间: ${freedSize / 1024}KB, 耗时: ${duration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 智能清理失败", e)
            _events.emit(CacheOptimizationEvent.CleanupFailed(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * 📊 分析访问模式
     */
    private fun analyzeAccessPatterns(): Map<String, AccessPattern> {
        // 这里应该从缓存管理器获取实际的访问数据
        // 暂时返回当前记录的模式
        return accessPatterns.toMap()
    }
    
    /**
     * 📈 记录访问模式
     */
    fun recordAccess(key: String, size: Long = 0L) {
        val pattern = accessPatterns.computeIfAbsent(key) { AccessPattern(key) }
        pattern.recordAccess(size)
    }
    
    /**
     * 🔍 分析缓存性能
     */
    suspend fun analyzeCachePerformance(): CachePerformanceReport {
        Log.d(TAG, "🔍 分析缓存性能...")
        
        val stats = cacheManager.getStats()
        val patterns = analyzeAccessPatterns()
        
        val hitRate = stats["hit_rate"] as? Double ?: 0.0
        val memoryUsage = stats["memory_size_bytes"] as? Long ?: 0L
        val diskUsage = stats["disk_size_bytes"] as? Long ?: 0L
        val totalEntries = stats["memory_entries"] as? Int ?: 0
        
        // 计算热点数据
        val hotKeys = patterns.values
            .sortedByDescending { it.accessCount.get() }
            .take(10)
            .map { it.key }
        
        // 计算冷数据
        val coldKeys = patterns.values
            .filter { it.getLastAccessAge() > 24 * 60 * 60 * 1000L } // 24小时未访问
            .map { it.key }
        
        // 性能建议
        val recommendations = generateRecommendations(hitRate, memoryUsage, diskUsage, patterns)
        
        val report = CachePerformanceReport(
            hitRate = hitRate,
            memoryUsage = memoryUsage,
            diskUsage = diskUsage,
            totalEntries = totalEntries,
            hotKeys = hotKeys,
            coldKeys = coldKeys,
            recommendations = recommendations,
            analysisTime = System.currentTimeMillis()
        )
        
        _events.emit(CacheOptimizationEvent.PerformanceAnalyzed(report))
        
        Log.d(TAG, "✅ 缓存性能分析完成，命中率: ${String.format("%.2f%%", hitRate * 100)}")
        
        return report
    }
    
    /**
     * 💡 生成优化建议
     */
    private fun generateRecommendations(
        hitRate: Double,
        memoryUsage: Long,
        diskUsage: Long,
        patterns: Map<String, AccessPattern>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 命中率建议
        if (hitRate < 0.5) {
            recommendations.add("命中率较低(${String.format("%.1f%%", hitRate * 100)})，建议增加缓存容量或调整缓存策略")
        }
        
        // 内存使用建议
        val memoryUsageMB = memoryUsage / 1024.0 / 1024.0
        if (memoryUsageMB > 100) {
            recommendations.add("内存使用量较高(${String.format("%.1f", memoryUsageMB)}MB)，建议清理冷数据")
        }
        
        // 磁盘使用建议
        val diskUsageMB = diskUsage / 1024.0 / 1024.0
        if (diskUsageMB > 500) {
            recommendations.add("磁盘使用量较高(${String.format("%.1f", diskUsageMB)}MB)，建议执行清理")
        }
        
        // 访问模式建议
        val coldDataCount = patterns.values.count { it.getLastAccessAge() > 24 * 60 * 60 * 1000L }
        if (coldDataCount > patterns.size * 0.3) {
            recommendations.add("存在较多冷数据($coldDataCount 个)，建议定期清理")
        }
        
        // 热点数据建议
        val hotDataCount = patterns.values.count { it.accessCount.get() > 10 }
        if (hotDataCount > 0) {
            recommendations.add("发现 $hotDataCount 个热点数据，建议优先保留在内存中")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("缓存性能良好，无需特殊优化")
        }
        
        return recommendations
    }
    
    /**
     * 🚀 启动分析任务
     */
    private fun startAnalysisTask() {
        analysisJob = scope.launch {
            while (isActive) {
                try {
                    analyzeCachePerformance()
                    delay(ANALYSIS_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 缓存分析任务异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 缓存分析任务已启动")
    }
    
    /**
     * 🚀 启动优化任务
     */
    private fun startOptimizationTask() {
        optimizationJob = scope.launch {
            while (isActive) {
                try {
                    performSmartCleanup()
                    delay(OPTIMIZATION_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 缓存优化任务异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 缓存优化任务已启动")
    }
    
    /**
     * 📊 获取优化统计
     */
    fun getOptimizationStats(): Map<String, Any> {
        return optimizationStats.getStats()
    }
    
    /**
     * 🧹 清理优化统计
     */
    fun clearOptimizationStats() {
        optimizationStats.clear()
    }
    
    /**
     * 🛑 关闭优化器
     */
    fun shutdown() {
        analysisJob?.cancel()
        optimizationJob?.cancel()
        scope.cancel()
        
        Log.d(TAG, "🛑 缓存优化器已关闭")
    }
}

/**
 * 访问模式
 */
class AccessPattern(val key: String) {
    val accessCount = AtomicLong(0)
    val totalSize = AtomicLong(0)
    @Volatile var lastAccessTime = System.currentTimeMillis()
    @Volatile var firstAccessTime = System.currentTimeMillis()
    @Volatile var estimatedSize = 0L
    
    fun recordAccess(size: Long = 0L) {
        accessCount.incrementAndGet()
        lastAccessTime = System.currentTimeMillis()
        if (size > 0) {
            totalSize.addAndGet(size)
            estimatedSize = totalSize.get() / accessCount.get()
        }
    }
    
    fun getLastAccessAge(): Long {
        return System.currentTimeMillis() - lastAccessTime
    }
    
    fun getAccessFrequency(): Double {
        val ageHours = (System.currentTimeMillis() - firstAccessTime) / 1000.0 / 3600.0
        return if (ageHours > 0) accessCount.get() / ageHours else 0.0
    }
    
    fun shouldCleanup(currentTime: Long, threshold: Long): Boolean {
        // 清理条件：
        // 1. 长时间未访问
        // 2. 访问频率很低
        // 3. 只访问过一次且时间较久
        
        val age = currentTime - lastAccessTime
        val frequency = getAccessFrequency()
        val count = accessCount.get()
        
        return age > threshold || 
               (frequency < 0.1 && age > 24 * 60 * 60 * 1000L) || 
               (count == 1L && age > 6 * 60 * 60 * 1000L)
    }
}

/**
 * 优化统计
 */
class OptimizationStats {
    val warmupCount = AtomicLong(0)
    val warmupSuccessCount = AtomicLong(0)
    val warmupErrorCount = AtomicLong(0)
    val warmupTotalDuration = AtomicLong(0)
    
    val cleanupCount = AtomicLong(0)
    val cleanupItemCount = AtomicLong(0)
    val cleanupFreedSize = AtomicLong(0)
    val cleanupTotalDuration = AtomicLong(0)
    
    fun recordWarmup(successCount: Int, errorCount: Int, duration: Long) {
        warmupCount.incrementAndGet()
        warmupSuccessCount.addAndGet(successCount.toLong())
        warmupErrorCount.addAndGet(errorCount.toLong())
        warmupTotalDuration.addAndGet(duration)
    }
    
    fun recordCleanup(itemCount: Int, freedSize: Long, duration: Long) {
        cleanupCount.incrementAndGet()
        cleanupItemCount.addAndGet(itemCount.toLong())
        cleanupFreedSize.addAndGet(freedSize)
        cleanupTotalDuration.addAndGet(duration)
    }
    
    fun getStats(): Map<String, Any> {
        val warmupAvgDuration = if (warmupCount.get() > 0) warmupTotalDuration.get() / warmupCount.get() else 0L
        val cleanupAvgDuration = if (cleanupCount.get() > 0) cleanupTotalDuration.get() / cleanupCount.get() else 0L
        
        return mapOf(
            "warmup_count" to warmupCount.get(),
            "warmup_success_count" to warmupSuccessCount.get(),
            "warmup_error_count" to warmupErrorCount.get(),
            "warmup_avg_duration_ms" to warmupAvgDuration,
            "cleanup_count" to cleanupCount.get(),
            "cleanup_item_count" to cleanupItemCount.get(),
            "cleanup_freed_size_bytes" to cleanupFreedSize.get(),
            "cleanup_freed_size_mb" to String.format("%.2f", cleanupFreedSize.get() / 1024.0 / 1024.0),
            "cleanup_avg_duration_ms" to cleanupAvgDuration
        )
    }
    
    fun clear() {
        warmupCount.set(0)
        warmupSuccessCount.set(0)
        warmupErrorCount.set(0)
        warmupTotalDuration.set(0)
        cleanupCount.set(0)
        cleanupItemCount.set(0)
        cleanupFreedSize.set(0)
        cleanupTotalDuration.set(0)
    }
}

/**
 * 缓存性能报告
 */
data class CachePerformanceReport(
    val hitRate: Double,
    val memoryUsage: Long,
    val diskUsage: Long,
    val totalEntries: Int,
    val hotKeys: List<String>,
    val coldKeys: List<String>,
    val recommendations: List<String>,
    val analysisTime: Long
) {
    
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "hit_rate" to String.format("%.2f%%", hitRate * 100),
            "memory_usage_mb" to String.format("%.2f", memoryUsage / 1024.0 / 1024.0),
            "disk_usage_mb" to String.format("%.2f", diskUsage / 1024.0 / 1024.0),
            "total_entries" to totalEntries,
            "hot_keys_count" to hotKeys.size,
            "cold_keys_count" to coldKeys.size,
            "recommendations_count" to recommendations.size,
            "analysis_time" to DateTimeUtils.formatTimestamp(analysisTime)
        )
    }
}

/**
 * 缓存优化事件
 */
sealed class CacheOptimizationEvent {
    data class WarmupStarted(val keyCount: Int) : CacheOptimizationEvent()
    data class WarmupCompleted(val successCount: Int, val errorCount: Int, val duration: Long) : CacheOptimizationEvent()
    data class WarmupFailed(val error: String) : CacheOptimizationEvent()
    
    object CleanupStarted : CacheOptimizationEvent()
    data class CleanupCompleted(val cleanedCount: Int, val freedSize: Long, val duration: Long) : CacheOptimizationEvent()
    data class CleanupFailed(val error: String) : CacheOptimizationEvent()
    
    data class PerformanceAnalyzed(val report: CachePerformanceReport) : CacheOptimizationEvent()
    data class OptimizationRecommendation(val recommendation: String) : CacheOptimizationEvent()
}
