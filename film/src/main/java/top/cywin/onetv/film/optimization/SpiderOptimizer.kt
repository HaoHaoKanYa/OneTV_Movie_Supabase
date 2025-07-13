package top.cywin.onetv.film.optimization

import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.catvod.Spider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 解析器性能优化器
 * 
 * 基于 FongMi/TV 的性能优化策略
 * 提供解析器的性能监控、缓存优化和错误处理增强
 * 
 * 功能：
 * - 性能监控和统计
 * - 智能缓存管理
 * - 错误处理和重试
 * - 并发控制
 * - 内存优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpiderOptimizer {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPIDER_OPTIMIZER"
        
        // 性能阈值
        private const val SLOW_REQUEST_THRESHOLD = 5000L // 5秒
        private const val ERROR_RATE_THRESHOLD = 0.3 // 30%
        private const val CACHE_SIZE_LIMIT = 100 // 缓存条目限制
        private const val MEMORY_THRESHOLD = 0.8 // 内存使用阈值
    }
    
    // 性能统计
    private val performanceStats = ConcurrentHashMap<String, SpiderPerformanceStats>()
    
    // 缓存管理
    private val cacheManager = SpiderCacheManager()
    
    // 错误处理
    private val errorHandler = SpiderErrorHandler()
    
    // 并发控制
    private val concurrencyController = SpiderConcurrencyController()
    
    /**
     * 🚀 优化 Spider 执行
     */
    suspend fun optimizeExecution(
        spiderKey: String,
        operation: String,
        execution: suspend () -> String
    ): String {
        val startTime = System.currentTimeMillis()
        
        return try {
            // 检查缓存
            val cacheKey = "$spiderKey:$operation"
            cacheManager.get(cacheKey)?.let { cachedResult ->
                logDebug("📦 从缓存获取结果: $cacheKey")
                recordSuccess(spiderKey, operation, System.currentTimeMillis() - startTime)
                return cachedResult
            }
            
            // 并发控制
            concurrencyController.execute(spiderKey) {
                // 执行操作
                val result = execution()
                
                // 缓存结果
                if (result.isNotEmpty() && !result.contains("error")) {
                    cacheManager.put(cacheKey, result)
                }
                
                val duration = System.currentTimeMillis() - startTime
                recordSuccess(spiderKey, operation, duration)
                
                // 检查性能
                if (duration > SLOW_REQUEST_THRESHOLD) {
                    logWarning("⚠️ 慢请求检测: $spiderKey.$operation 耗时 ${duration}ms")
                }
                
                result
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            recordError(spiderKey, operation, duration, e)
            
            // 错误处理和重试
            errorHandler.handleError(spiderKey, operation, e, execution)
        }
    }
    
    /**
     * 📊 记录成功执行
     */
    private fun recordSuccess(spiderKey: String, operation: String, duration: Long) {
        val stats = getOrCreateStats(spiderKey)
        stats.recordSuccess(operation, duration)
    }
    
    /**
     * ❌ 记录错误执行
     */
    private fun recordError(spiderKey: String, operation: String, duration: Long, error: Exception) {
        val stats = getOrCreateStats(spiderKey)
        stats.recordError(operation, duration, error)
        
        logError("❌ Spider 执行错误: $spiderKey.$operation", error)
    }
    
    /**
     * 📈 获取或创建性能统计
     */
    private fun getOrCreateStats(spiderKey: String): SpiderPerformanceStats {
        return performanceStats.computeIfAbsent(spiderKey) { SpiderPerformanceStats(spiderKey) }
    }
    
    /**
     * 📊 获取性能报告
     */
    fun getPerformanceReport(): Map<String, Any> {
        val report = mutableMapOf<String, Any>()
        
        performanceStats.forEach { (spiderKey, stats) ->
            report[spiderKey] = stats.getReport()
        }
        
        report["cache_stats"] = cacheManager.getStats()
        report["error_stats"] = errorHandler.getStats()
        report["concurrency_stats"] = concurrencyController.getStats()
        
        return report
    }
    
    /**
     * 🔧 优化建议
     */
    fun getOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        performanceStats.forEach { (spiderKey, stats) ->
            val report = stats.getReport()
            
            // 检查错误率
            val errorRate = report["error_rate"] as? Double ?: 0.0
            if (errorRate > ERROR_RATE_THRESHOLD) {
                suggestions.add("$spiderKey 错误率过高 (${String.format("%.1f", errorRate * 100)}%)，建议检查网络连接或站点状态")
            }
            
            // 检查平均响应时间
            val avgDuration = report["avg_duration"] as? Long ?: 0L
            if (avgDuration > SLOW_REQUEST_THRESHOLD) {
                suggestions.add("$spiderKey 响应时间过长 (${avgDuration}ms)，建议优化网络请求或增加缓存")
            }
        }
        
        // 检查缓存使用情况
        val cacheStats = cacheManager.getStats()
        val hitRate = cacheStats["hit_rate"] as? Double ?: 0.0
        if (hitRate < 0.5) {
            suggestions.add("缓存命中率较低 (${String.format("%.1f", hitRate * 100)}%)，建议调整缓存策略")
        }
        
        return suggestions
    }
    
    /**
     * 🧹 清理和优化
     */
    fun cleanup() {
        logDebug("🧹 开始 Spider 优化器清理...")
        
        // 清理过期统计
        val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // 24小时前
        performanceStats.values.removeIf { it.lastUpdateTime < cutoffTime }
        
        // 清理缓存
        cacheManager.cleanup()
        
        // 清理错误记录
        errorHandler.cleanup()
        
        logDebug("✅ Spider 优化器清理完成")
    }
    
    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }
    
    private fun logWarning(message: String) {
        Log.w(TAG, message)
    }
    
    private fun logError(message: String, error: Exception) {
        Log.e(TAG, message, error)
    }
}

/**
 * Spider 性能统计
 */
class SpiderPerformanceStats(private val spiderKey: String) {
    
    private val operationStats = ConcurrentHashMap<String, OperationStats>()
    var lastUpdateTime = System.currentTimeMillis()
        private set
    
    fun recordSuccess(operation: String, duration: Long) {
        val stats = getOrCreateOperationStats(operation)
        stats.recordSuccess(duration)
        lastUpdateTime = System.currentTimeMillis()
    }
    
    fun recordError(operation: String, duration: Long, error: Exception) {
        val stats = getOrCreateOperationStats(operation)
        stats.recordError(duration, error)
        lastUpdateTime = System.currentTimeMillis()
    }
    
    private fun getOrCreateOperationStats(operation: String): OperationStats {
        return operationStats.computeIfAbsent(operation) { OperationStats(operation) }
    }
    
    fun getReport(): Map<String, Any> {
        val totalSuccess = operationStats.values.sumOf { it.successCount.get() }
        val totalError = operationStats.values.sumOf { it.errorCount.get() }
        val totalDuration = operationStats.values.sumOf { it.totalDuration.get() }
        val totalRequests = totalSuccess + totalError
        
        return mapOf(
            "spider_key" to spiderKey,
            "total_requests" to totalRequests,
            "success_count" to totalSuccess,
            "error_count" to totalError,
            "error_rate" to if (totalRequests > 0) totalError.toDouble() / totalRequests else 0.0,
            "avg_duration" to if (totalRequests > 0) totalDuration / totalRequests else 0L,
            "operations" to operationStats.mapValues { it.value.getReport() },
            "last_update" to lastUpdateTime
        )
    }
}

/**
 * 操作统计
 */
class OperationStats(private val operation: String) {
    
    val successCount = AtomicLong(0)
    val errorCount = AtomicLong(0)
    val totalDuration = AtomicLong(0)
    private val recentErrors = mutableListOf<String>()
    
    fun recordSuccess(duration: Long) {
        successCount.incrementAndGet()
        totalDuration.addAndGet(duration)
    }
    
    fun recordError(duration: Long, error: Exception) {
        errorCount.incrementAndGet()
        totalDuration.addAndGet(duration)
        
        synchronized(recentErrors) {
            recentErrors.add(error.message ?: "Unknown error")
            if (recentErrors.size > 10) {
                recentErrors.removeAt(0)
            }
        }
    }
    
    fun getReport(): Map<String, Any> {
        val total = successCount.get() + errorCount.get()
        return mapOf(
            "operation" to operation,
            "success_count" to successCount.get(),
            "error_count" to errorCount.get(),
            "error_rate" to if (total > 0) errorCount.get().toDouble() / total else 0.0,
            "avg_duration" to if (total > 0) totalDuration.get() / total else 0L,
            "recent_errors" to synchronized(recentErrors) { recentErrors.toList() }
        )
    }
}

/**
 * Spider 缓存管理器
 */
class SpiderCacheManager {
    
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    
    fun get(key: String): String? {
        val entry = cache[key]
        
        return if (entry != null && !entry.isExpired()) {
            hitCount.incrementAndGet()
            entry.value
        } else {
            if (entry != null) {
                cache.remove(key)
            }
            missCount.incrementAndGet()
            null
        }
    }
    
    fun put(key: String, value: String, ttl: Long = 5 * 60 * 1000L) { // 默认5分钟
        if (cache.size >= SpiderOptimizer.CACHE_SIZE_LIMIT) {
            cleanup()
        }
        
        cache[key] = CacheEntry(value, System.currentTimeMillis() + ttl)
    }
    
    fun cleanup() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value.expiryTime < now }
    }
    
    fun getStats(): Map<String, Any> {
        val hits = hitCount.get()
        val misses = missCount.get()
        val total = hits + misses
        
        return mapOf(
            "cache_size" to cache.size,
            "hit_count" to hits,
            "miss_count" to misses,
            "hit_rate" to if (total > 0) hits.toDouble() / total else 0.0
        )
    }
    
    private data class CacheEntry(
        val value: String,
        val expiryTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiryTime
    }
}

/**
 * Spider 错误处理器
 */
class SpiderErrorHandler {
    
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val lastErrors = ConcurrentHashMap<String, String>()
    
    suspend fun handleError(
        spiderKey: String,
        operation: String,
        error: Exception,
        retryExecution: suspend () -> String
    ): String {
        val errorKey = "$spiderKey:$operation"
        val count = errorCounts.computeIfAbsent(errorKey) { AtomicLong(0) }
        count.incrementAndGet()
        lastErrors[errorKey] = error.message ?: "Unknown error"
        
        // 简单的重试逻辑
        return if (count.get() <= 3 && shouldRetry(error)) {
            delay(1000L * count.get()) // 递增延迟
            try {
                retryExecution()
            } catch (retryError: Exception) {
                buildErrorResponse(retryError.message ?: "Retry failed")
            }
        } else {
            buildErrorResponse(error.message ?: "Operation failed")
        }
    }
    
    private fun shouldRetry(error: Exception): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("timeout") || 
               message.contains("connection") || 
               message.contains("network")
    }
    
    private fun buildErrorResponse(message: String): String {
        return """{"error": "$message", "list": [], "class": []}"""
    }
    
    fun cleanup() {
        errorCounts.clear()
        lastErrors.clear()
    }
    
    fun getStats(): Map<String, Any> {
        return mapOf(
            "error_types" to errorCounts.size,
            "total_errors" to errorCounts.values.sumOf { it.get() },
            "recent_errors" to lastErrors.toMap()
        )
    }
}

/**
 * Spider 并发控制器
 */
class SpiderConcurrencyController {
    
    private val semaphores = ConcurrentHashMap<String, Semaphore>()
    private val activeRequests = ConcurrentHashMap<String, AtomicLong>()
    
    suspend fun <T> execute(spiderKey: String, block: suspend () -> T): T {
        val semaphore = semaphores.computeIfAbsent(spiderKey) { Semaphore(3) } // 每个Spider最多3个并发
        val activeCount = activeRequests.computeIfAbsent(spiderKey) { AtomicLong(0) }
        
        semaphore.acquire()
        activeCount.incrementAndGet()
        
        return try {
            block()
        } finally {
            activeCount.decrementAndGet()
            semaphore.release()
        }
    }
    
    fun getStats(): Map<String, Any> {
        return mapOf(
            "active_spiders" to semaphores.size,
            "active_requests" to activeRequests.mapValues { it.value.get() }
        )
    }
}
