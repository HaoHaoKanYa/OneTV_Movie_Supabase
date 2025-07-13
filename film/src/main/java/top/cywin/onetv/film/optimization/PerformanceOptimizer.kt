package top.cywin.onetv.film.optimization

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.concurrent.ConcurrentManager
import top.cywin.onetv.film.cache.CacheManager
import top.cywin.onetv.film.network.NetworkClient
import top.cywin.onetv.film.utils.DateTimeUtils
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

/**
 * 性能优化器
 * 
 * 基于 FongMi/TV 的性能优化系统实现
 * 提供自动性能监控、分析和优化建议
 * 
 * 功能：
 * - 性能监控和分析
 * - 自动优化建议
 * - 资源使用优化
 * - 内存管理优化
 * - 网络性能优化
 * - 缓存策略优化
 * - 并发性能优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class PerformanceOptimizer(
    private val context: Context,
    private val spiderManager: SpiderManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PERFORMANCE_OPTIMIZER"
        private const val MONITORING_INTERVAL = 60000L // 1分钟
        private const val OPTIMIZATION_INTERVAL = 300000L // 5分钟
        private const val MEMORY_THRESHOLD = 0.8f // 80%
        private const val CPU_THRESHOLD = 0.7f // 70%
        private const val NETWORK_LATENCY_THRESHOLD = 2000L // 2秒
    }
    
    // 组件引用
    private val concurrentManager = spiderManager.getConcurrentManager()
    private val cacheManager = spiderManager.getCacheManager()
    private val networkClient = spiderManager.getNetworkClient()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 性能监控
    private val performanceMonitor = PerformanceMonitor()
    private val optimizationEngine = OptimizationEngine()
    
    // 事件流
    private val _events = MutableSharedFlow<OptimizationEvent>()
    val events: SharedFlow<OptimizationEvent> = _events.asSharedFlow()
    
    // 监控任务
    private var monitoringJob: Job? = null
    private var optimizationJob: Job? = null
    
    // 优化统计
    private val optimizationStats = OptimizationStatistics()
    
    init {
        startMonitoring()
        startOptimization()
        Log.d(TAG, "🏗️ 性能优化器初始化完成")
    }
    
    /**
     * 🚀 启动性能监控
     */
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    performanceMonitor.collectMetrics()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 性能监控异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 性能监控已启动")
    }
    
    /**
     * 🚀 启动自动优化
     */
    private fun startOptimization() {
        optimizationJob = scope.launch {
            while (isActive) {
                try {
                    performAutoOptimization()
                    delay(OPTIMIZATION_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 自动优化异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 自动优化已启动")
    }
    
    /**
     * 🔍 执行性能分析
     */
    suspend fun analyzePerformance(): PerformanceReport {
        Log.d(TAG, "🔍 执行性能分析...")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 收集当前性能指标
            val metrics = performanceMonitor.getCurrentMetrics()
            
            // 分析各个组件性能
            val memoryAnalysis = analyzeMemoryUsage()
            val cpuAnalysis = analyzeCpuUsage()
            val networkAnalysis = analyzeNetworkPerformance()
            val cacheAnalysis = analyzeCachePerformance()
            val concurrentAnalysis = analyzeConcurrentPerformance()
            
            // 生成优化建议
            val recommendations = optimizationEngine.generateRecommendations(
                memoryAnalysis, cpuAnalysis, networkAnalysis, cacheAnalysis, concurrentAnalysis
            )
            
            // 计算性能评分
            val performanceScore = calculatePerformanceScore(
                memoryAnalysis, cpuAnalysis, networkAnalysis, cacheAnalysis, concurrentAnalysis
            )
            
            val report = PerformanceReport(
                timestamp = System.currentTimeMillis(),
                performanceScore = performanceScore,
                memoryAnalysis = memoryAnalysis,
                cpuAnalysis = cpuAnalysis,
                networkAnalysis = networkAnalysis,
                cacheAnalysis = cacheAnalysis,
                concurrentAnalysis = concurrentAnalysis,
                recommendations = recommendations,
                metrics = metrics
            )
            
            val duration = System.currentTimeMillis() - startTime
            optimizationStats.recordAnalysis(duration, recommendations.size)
            
            _events.emit(OptimizationEvent.AnalysisCompleted(report))
            
            Log.d(TAG, "✅ 性能分析完成，评分: $performanceScore, 耗时: ${duration}ms")
            
            return report
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 性能分析失败", e)
            throw e
        }
    }
    
    /**
     * 🔧 执行自动优化
     */
    private suspend fun performAutoOptimization() {
        Log.d(TAG, "🔧 执行自动优化...")
        
        try {
            val report = analyzePerformance()
            val appliedOptimizations = mutableListOf<String>()
            
            // 应用自动优化建议
            report.recommendations.forEach { recommendation ->
                if (recommendation.autoApplicable) {
                    try {
                        applyOptimization(recommendation)
                        appliedOptimizations.add(recommendation.type)
                        Log.d(TAG, "✅ 应用优化: ${recommendation.type}")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 应用优化失败: ${recommendation.type}", e)
                    }
                }
            }
            
            if (appliedOptimizations.isNotEmpty()) {
                optimizationStats.recordOptimization(appliedOptimizations.size)
                _events.emit(OptimizationEvent.OptimizationApplied(appliedOptimizations))
                
                Log.d(TAG, "🔧 自动优化完成，应用数: ${appliedOptimizations.size}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 自动优化失败", e)
        }
    }
    
    /**
     * 🧠 分析内存使用
     */
    private fun analyzeMemoryUsage(): MemoryAnalysis {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        val usageRatio = usedMemory.toFloat() / maxMemory.toFloat()
        val availableMemory = maxMemory - usedMemory
        
        val status = when {
            usageRatio > 0.9f -> MemoryStatus.CRITICAL
            usageRatio > 0.8f -> MemoryStatus.HIGH
            usageRatio > 0.6f -> MemoryStatus.MODERATE
            else -> MemoryStatus.NORMAL
        }
        
        return MemoryAnalysis(
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory,
            availableMemory = availableMemory,
            usageRatio = usageRatio,
            status = status
        )
    }
    
    /**
     * ⚡ 分析CPU使用
     */
    private fun analyzeCpuUsage(): CpuAnalysis {
        // 获取线程池状态
        val threadPoolStatus = concurrentManager.getThreadPoolStatus()
        val totalThreads = threadPoolStatus.values.sumOf { 
            (it["current_pool_size"] as? Int) ?: 0 
        }
        val activeThreads = threadPoolStatus.values.sumOf { 
            (it["active_count"] as? Int) ?: 0 
        }
        
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val threadUtilization = if (totalThreads > 0) activeThreads.toFloat() / totalThreads.toFloat() else 0f
        
        val status = when {
            threadUtilization > 0.9f -> CpuStatus.HIGH
            threadUtilization > 0.7f -> CpuStatus.MODERATE
            else -> CpuStatus.NORMAL
        }
        
        return CpuAnalysis(
            cpuCores = cpuCores,
            totalThreads = totalThreads,
            activeThreads = activeThreads,
            threadUtilization = threadUtilization,
            status = status
        )
    }
    
    /**
     * 🌐 分析网络性能
     */
    private fun analyzeNetworkPerformance(): NetworkAnalysis {
        val networkStats = networkClient.getStats()
        
        val totalRequests = (networkStats["total_requests"] as? Long) ?: 0L
        val successRequests = (networkStats["success_requests"] as? Long) ?: 0L
        val failedRequests = (networkStats["failed_requests"] as? Long) ?: 0L
        val averageLatency = (networkStats["average_latency"] as? Long) ?: 0L
        
        val successRate = if (totalRequests > 0) successRequests.toFloat() / totalRequests.toFloat() else 0f
        val errorRate = if (totalRequests > 0) failedRequests.toFloat() / totalRequests.toFloat() else 0f
        
        val status = when {
            errorRate > 0.1f || averageLatency > NETWORK_LATENCY_THRESHOLD -> NetworkStatus.POOR
            errorRate > 0.05f || averageLatency > 1000L -> NetworkStatus.MODERATE
            else -> NetworkStatus.GOOD
        }
        
        return NetworkAnalysis(
            totalRequests = totalRequests,
            successRequests = successRequests,
            failedRequests = failedRequests,
            successRate = successRate,
            errorRate = errorRate,
            averageLatency = averageLatency,
            status = status
        )
    }
    
    /**
     * 🗄️ 分析缓存性能
     */
    private fun analyzeCachePerformance(): CacheAnalysis {
        val cacheStats = cacheManager.getStats()
        
        val hitCount = (cacheStats["hit_count"] as? Long) ?: 0L
        val missCount = (cacheStats["miss_count"] as? Long) ?: 0L
        val hitRate = (cacheStats["hit_rate"] as? Double) ?: 0.0
        val memoryUsage = (cacheStats["memory_size_bytes"] as? Long) ?: 0L
        val diskUsage = (cacheStats["disk_size_bytes"] as? Long) ?: 0L
        
        val status = when {
            hitRate < 0.5 -> CacheStatus.POOR
            hitRate < 0.7 -> CacheStatus.MODERATE
            else -> CacheStatus.GOOD
        }
        
        return CacheAnalysis(
            hitCount = hitCount,
            missCount = missCount,
            hitRate = hitRate,
            memoryUsage = memoryUsage,
            diskUsage = diskUsage,
            status = status
        )
    }
    
    /**
     * ⚡ 分析并发性能
     */
    private fun analyzeConcurrentPerformance(): ConcurrentAnalysis {
        val concurrentStats = concurrentManager.getConcurrentStats()
        
        val totalTasks = (concurrentStats["total_tasks"] as? Long) ?: 0L
        val completedTasks = (concurrentStats["completed_tasks"] as? Long) ?: 0L
        val failedTasks = (concurrentStats["failed_tasks"] as? Long) ?: 0L
        val averageExecutionTime = (concurrentStats["average_execution_time"] as? Long) ?: 0L
        
        val successRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
        val failureRate = if (totalTasks > 0) failedTasks.toFloat() / totalTasks.toFloat() else 0f
        
        val status = when {
            failureRate > 0.1f || averageExecutionTime > 5000L -> ConcurrentStatus.POOR
            failureRate > 0.05f || averageExecutionTime > 2000L -> ConcurrentStatus.MODERATE
            else -> ConcurrentStatus.GOOD
        }
        
        return ConcurrentAnalysis(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            failedTasks = failedTasks,
            successRate = successRate,
            failureRate = failureRate,
            averageExecutionTime = averageExecutionTime,
            status = status
        )
    }
    
    /**
     * 📊 计算性能评分
     */
    private fun calculatePerformanceScore(
        memoryAnalysis: MemoryAnalysis,
        cpuAnalysis: CpuAnalysis,
        networkAnalysis: NetworkAnalysis,
        cacheAnalysis: CacheAnalysis,
        concurrentAnalysis: ConcurrentAnalysis
    ): Float {
        val memoryScore = when (memoryAnalysis.status) {
            MemoryStatus.NORMAL -> 100f
            MemoryStatus.MODERATE -> 80f
            MemoryStatus.HIGH -> 60f
            MemoryStatus.CRITICAL -> 30f
        }
        
        val cpuScore = when (cpuAnalysis.status) {
            CpuStatus.NORMAL -> 100f
            CpuStatus.MODERATE -> 70f
            CpuStatus.HIGH -> 40f
        }
        
        val networkScore = when (networkAnalysis.status) {
            NetworkStatus.GOOD -> 100f
            NetworkStatus.MODERATE -> 70f
            NetworkStatus.POOR -> 40f
        }
        
        val cacheScore = when (cacheAnalysis.status) {
            CacheStatus.GOOD -> 100f
            CacheStatus.MODERATE -> 70f
            CacheStatus.POOR -> 40f
        }
        
        val concurrentScore = when (concurrentAnalysis.status) {
            ConcurrentStatus.GOOD -> 100f
            ConcurrentStatus.MODERATE -> 70f
            ConcurrentStatus.POOR -> 40f
        }
        
        // 加权平均
        return (memoryScore * 0.25f + cpuScore * 0.2f + networkScore * 0.2f + 
                cacheScore * 0.2f + concurrentScore * 0.15f)
    }
    
    /**
     * 🔧 应用优化
     */
    private suspend fun applyOptimization(recommendation: OptimizationRecommendation) {
        when (recommendation.type) {
            "memory_cleanup" -> {
                System.gc()
                cacheManager.clear()
            }
            "cache_optimization" -> {
                // 应用缓存优化
            }
            "thread_pool_adjustment" -> {
                // 调整线程池配置
            }
            "network_optimization" -> {
                // 应用网络优化
            }
        }
    }
    
    /**
     * 📊 获取优化统计
     */
    fun getOptimizationStats(): Map<String, Any> {
        return optimizationStats.getStats()
    }
    
    /**
     * 🛑 关闭优化器
     */
    fun shutdown() {
        monitoringJob?.cancel()
        optimizationJob?.cancel()
        scope.cancel()
        
        Log.d(TAG, "🛑 性能优化器已关闭")
    }
}

/**
 * 性能监控器
 */
class PerformanceMonitor {
    private val metrics = ConcurrentHashMap<String, Any>()
    
    fun collectMetrics() {
        val runtime = Runtime.getRuntime()
        
        metrics["timestamp"] = System.currentTimeMillis()
        metrics["total_memory"] = runtime.totalMemory()
        metrics["free_memory"] = runtime.freeMemory()
        metrics["max_memory"] = runtime.maxMemory()
        metrics["used_memory"] = runtime.totalMemory() - runtime.freeMemory()
        metrics["available_processors"] = runtime.availableProcessors()
    }
    
    fun getCurrentMetrics(): Map<String, Any> = metrics.toMap()
}

/**
 * 优化引擎
 */
class OptimizationEngine {
    
    fun generateRecommendations(
        memoryAnalysis: MemoryAnalysis,
        cpuAnalysis: CpuAnalysis,
        networkAnalysis: NetworkAnalysis,
        cacheAnalysis: CacheAnalysis,
        concurrentAnalysis: ConcurrentAnalysis
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        // 内存优化建议
        if (memoryAnalysis.status == MemoryStatus.HIGH || memoryAnalysis.status == MemoryStatus.CRITICAL) {
            recommendations.add(
                OptimizationRecommendation(
                    type = "memory_cleanup",
                    priority = Priority.HIGH,
                    description = "内存使用率过高，建议清理缓存和执行垃圾回收",
                    autoApplicable = true,
                    impact = "减少内存使用，提升应用稳定性"
                )
            )
        }
        
        // 缓存优化建议
        if (cacheAnalysis.status == CacheStatus.POOR) {
            recommendations.add(
                OptimizationRecommendation(
                    type = "cache_optimization",
                    priority = Priority.MEDIUM,
                    description = "缓存命中率较低，建议调整缓存策略",
                    autoApplicable = false,
                    impact = "提升数据访问速度，减少网络请求"
                )
            )
        }
        
        // 网络优化建议
        if (networkAnalysis.status == NetworkStatus.POOR) {
            recommendations.add(
                OptimizationRecommendation(
                    type = "network_optimization",
                    priority = Priority.HIGH,
                    description = "网络性能较差，建议优化网络配置",
                    autoApplicable = false,
                    impact = "提升网络请求成功率和响应速度"
                )
            )
        }
        
        // 并发优化建议
        if (concurrentAnalysis.status == ConcurrentStatus.POOR) {
            recommendations.add(
                OptimizationRecommendation(
                    type = "concurrent_optimization",
                    priority = Priority.MEDIUM,
                    description = "并发任务失败率较高，建议调整并发配置",
                    autoApplicable = false,
                    impact = "提升任务执行成功率和效率"
                )
            )
        }
        
        return recommendations
    }
}
