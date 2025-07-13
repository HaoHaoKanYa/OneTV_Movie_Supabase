package top.cywin.onetv.film.monitoring

import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.utils.DateTimeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 监控相关数据模型
 * 
 * 基于 FongMi/TV 的监控数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 系统指标
 */
class SystemMetrics {
    val startTime = System.currentTimeMillis()
    private val metrics = ConcurrentHashMap<String, Any>()
    private val metricHistory = ConcurrentHashMap<String, MutableList<MetricPoint>>()
    
    /**
     * 📊 记录内存指标
     */
    fun recordMemoryMetrics(
        timestamp: Long,
        totalMemory: Long,
        usedMemory: Long,
        freeMemory: Long,
        maxMemory: Long,
        usageRatio: Float
    ) {
        metrics["memory_total"] = totalMemory
        metrics["memory_used"] = usedMemory
        metrics["memory_free"] = freeMemory
        metrics["memory_max"] = maxMemory
        metrics["memory_usage_ratio"] = usageRatio
        metrics["memory_timestamp"] = timestamp
        
        recordMetricHistory("memory_usage_ratio", timestamp, usageRatio.toDouble())
    }
    
    /**
     * 🧵 记录线程指标
     */
    fun recordThreadMetrics(timestamp: Long, totalThreads: Int, activeThreads: Int) {
        metrics["thread_total"] = totalThreads
        metrics["thread_active"] = activeThreads
        metrics["thread_utilization"] = if (totalThreads > 0) activeThreads.toFloat() / totalThreads else 0f
        metrics["thread_timestamp"] = timestamp
        
        recordMetricHistory("thread_utilization", timestamp, 
            if (totalThreads > 0) activeThreads.toDouble() / totalThreads else 0.0)
    }
    
    /**
     * 🗄️ 记录缓存指标
     */
    fun recordCacheMetrics(timestamp: Long, hitCount: Long, missCount: Long, hitRate: Double) {
        metrics["cache_hit_count"] = hitCount
        metrics["cache_miss_count"] = missCount
        metrics["cache_hit_rate"] = hitRate
        metrics["cache_timestamp"] = timestamp
        
        recordMetricHistory("cache_hit_rate", timestamp, hitRate)
    }
    
    /**
     * 🌐 记录网络指标
     */
    fun recordNetworkMetrics(
        timestamp: Long,
        totalRequests: Long,
        successRequests: Long,
        failedRequests: Long,
        averageLatency: Long
    ) {
        metrics["total_requests"] = totalRequests
        metrics["success_requests"] = successRequests
        metrics["failed_requests"] = failedRequests
        metrics["average_latency"] = averageLatency
        metrics["network_timestamp"] = timestamp
        
        val successRate = if (totalRequests > 0) successRequests.toDouble() / totalRequests else 1.0
        recordMetricHistory("network_success_rate", timestamp, successRate)
        recordMetricHistory("network_latency", timestamp, averageLatency.toDouble())
    }
    
    /**
     * ⚡ 记录并发指标
     */
    fun recordConcurrentMetrics(timestamp: Long, totalTasks: Long, completedTasks: Long, failedTasks: Long) {
        metrics["concurrent_total_tasks"] = totalTasks
        metrics["concurrent_completed_tasks"] = completedTasks
        metrics["concurrent_failed_tasks"] = failedTasks
        metrics["concurrent_timestamp"] = timestamp
        
        val successRate = if (totalTasks > 0) completedTasks.toDouble() / totalTasks else 1.0
        recordMetricHistory("concurrent_success_rate", timestamp, successRate)
    }
    
    /**
     * 📈 记录指标历史
     */
    private fun recordMetricHistory(metricName: String, timestamp: Long, value: Double) {
        val history = metricHistory.computeIfAbsent(metricName) { mutableListOf() }
        history.add(MetricPoint(timestamp, value))
        
        // 保持最近1000个数据点
        if (history.size > 1000) {
            history.removeAt(0)
        }
    }
    
    /**
     * 📊 获取当前指标
     */
    fun getCurrentMetrics(): Map<String, Any> = metrics.toMap()
    
    /**
     * 📈 获取指标历史
     */
    fun getMetricHistory(metricName: String): List<MetricPoint> {
        return metricHistory[metricName]?.toList() ?: emptyList()
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "start_time" to DateTimeUtils.formatTimestamp(startTime),
            "uptime_ms" to (System.currentTimeMillis() - startTime),
            "metrics_count" to metrics.size,
            "history_metrics_count" to metricHistory.size,
            "total_data_points" to metricHistory.values.sumOf { it.size }
        )
    }
}

/**
 * 指标数据点
 */
data class MetricPoint(
    val timestamp: Long,
    val value: Double
)

/**
 * 健康状态
 */
class HealthStatus {
    private val healthChecks = ConcurrentHashMap<String, HealthCheckResult>()
    private var overallHealth = 1.0
    private var lastUpdateTime = System.currentTimeMillis()
    
    /**
     * 🔄 更新健康检查结果
     */
    fun updateHealthChecks(checks: Map<String, HealthCheckResult>) {
        healthChecks.clear()
        healthChecks.putAll(checks)
        lastUpdateTime = System.currentTimeMillis()
    }
    
    /**
     * 🔄 更新整体健康度
     */
    fun updateOverallHealth(health: Double) {
        overallHealth = health
        lastUpdateTime = System.currentTimeMillis()
    }
    
    /**
     * 📊 获取当前健康状态
     */
    fun getCurrentHealth(): Map<String, Any> {
        return mapOf(
            "overall_health" to overallHealth,
            "health_checks" to healthChecks.mapValues { it.value.toMap() },
            "last_update_time" to lastUpdateTime
        )
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val healthyCount = healthChecks.values.count { it.status == HealthCheckStatus.HEALTHY }
        val degradedCount = healthChecks.values.count { it.status == HealthCheckStatus.DEGRADED }
        val unhealthyCount = healthChecks.values.count { it.status == HealthCheckStatus.UNHEALTHY }
        
        return mapOf(
            "overall_health" to String.format("%.1f%%", overallHealth * 100),
            "total_checks" to healthChecks.size,
            "healthy_checks" to healthyCount,
            "degraded_checks" to degradedCount,
            "unhealthy_checks" to unhealthyCount,
            "last_update_time" to DateTimeUtils.formatTimestamp(lastUpdateTime)
        )
    }
}

/**
 * 健康检查结果
 */
data class HealthCheckResult(
    val status: HealthCheckStatus,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "status" to status.name,
            "message" to message,
            "details" to details,
            "timestamp" to DateTimeUtils.formatTimestamp(timestamp)
        )
    }
}

/**
 * 健康检查状态
 */
enum class HealthCheckStatus {
    HEALTHY,    // 健康
    DEGRADED,   // 降级
    UNHEALTHY   // 不健康
}

/**
 * 告警管理器
 */
class AlertManager {
    private val alerts = mutableListOf<Alert>()
    private val alertCounts = ConcurrentHashMap<AlertType, AtomicLong>()
    
    /**
     * ➕ 添加告警
     */
    fun addAlert(alert: Alert) {
        synchronized(alerts) {
            alerts.add(alert)
            // 保持最近1000个告警
            if (alerts.size > 1000) {
                alerts.removeAt(0)
            }
        }
        
        alertCounts.computeIfAbsent(alert.type) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * 📋 获取最近告警
     */
    fun getRecentAlerts(timeRangeMs: Long): List<Alert> {
        val cutoffTime = System.currentTimeMillis() - timeRangeMs
        return synchronized(alerts) {
            alerts.filter { it.timestamp >= cutoffTime }
        }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val recentAlerts = getRecentAlerts(24 * 60 * 60 * 1000L) // 24小时
        
        return mapOf(
            "total_alerts" to alerts.size,
            "recent_alerts_24h" to recentAlerts.size,
            "alert_counts_by_type" to alertCounts.mapValues { it.value.get() },
            "recent_alerts_by_severity" to recentAlerts.groupBy { it.severity }.mapValues { it.value.size }
        )
    }
}

/**
 * 告警
 */
data class Alert(
    val type: AlertType,
    val severity: AlertSeverity,
    val message: String,
    val timestamp: Long,
    val metrics: Map<String, Any> = emptyMap()
) {
    
    fun toMap(): Map<String, Any> {
        return mapOf(
            "type" to type.name,
            "severity" to severity.name,
            "message" to message,
            "timestamp" to DateTimeUtils.formatTimestamp(timestamp),
            "metrics" to metrics
        )
    }
}

/**
 * 告警类型
 */
enum class AlertType {
    MEMORY_HIGH,            // 内存使用率高
    NETWORK_ERROR_HIGH,     // 网络错误率高
    RESPONSE_TIME_HIGH,     // 响应时间长
    CACHE_HIT_RATE_LOW,     // 缓存命中率低
    CONCURRENT_FAILURE_HIGH, // 并发失败率高
    SYSTEM_UNHEALTHY        // 系统不健康
}

/**
 * 告警严重程度
 */
enum class AlertSeverity {
    INFO,       // 信息
    WARNING,    // 警告
    ERROR,      // 错误
    CRITICAL    // 严重
}

/**
 * 监控事件
 */
sealed class MonitoringEvent {
    data class AlertTriggered(val alert: Alert) : MonitoringEvent()
    data class HealthCheckCompleted(val results: Map<String, HealthCheckResult>, val overallHealth: Double) : MonitoringEvent()
    data class DiagnosticReportGenerated(val report: DiagnosticReport) : MonitoringEvent()
    data class MetricsCollected(val metrics: Map<String, Any>) : MonitoringEvent()
}

/**
 * 诊断报告
 */
data class DiagnosticReport(
    val timestamp: Long,
    val systemMetrics: Map<String, Any>,
    val healthStatus: Map<String, Any>,
    val alerts: List<Alert>,
    val diagnosticResults: Map<String, Any>,
    val recommendations: List<String>
) {
    
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "timestamp" to DateTimeUtils.formatTimestamp(timestamp),
            "overall_health" to (healthStatus["overall_health"] as? Double ?: 0.0),
            "alerts_count" to alerts.size,
            "critical_alerts_count" to alerts.count { it.severity == AlertSeverity.CRITICAL },
            "recommendations_count" to recommendations.size,
            "diagnostic_categories" to diagnosticResults.keys.size
        )
    }
}

/**
 * 系统诊断
 */
class SystemDiagnostics {
    
    /**
     * 🔍 执行诊断
     */
    suspend fun performDiagnostics(spiderManager: SpiderManager): Map<String, Any> {
        val diagnostics = mutableMapOf<String, Any>()
        
        // Spider 诊断
        diagnostics["spider_diagnostics"] = diagnoseSpiders(spiderManager)
        
        // 缓存诊断
        diagnostics["cache_diagnostics"] = diagnoseCache(spiderManager)
        
        // 并发诊断
        diagnostics["concurrent_diagnostics"] = diagnoseConcurrent(spiderManager)
        
        // 网络诊断
        diagnostics["network_diagnostics"] = diagnoseNetwork(spiderManager)
        
        // 内存诊断
        diagnostics["memory_diagnostics"] = diagnoseMemory()
        
        return diagnostics
    }
    
    /**
     * 🕷️ 诊断 Spider
     */
    private fun diagnoseSpiders(spiderManager: SpiderManager): Map<String, Any> {
        val spiders = spiderManager.getAllSpiders()
        
        return mapOf(
            "total_spiders" to spiders.size,
            "spider_types" to spiders.values.groupBy { it.getType() }.mapValues { it.value.size },
            "searchable_spiders" to spiders.values.count { it.isSearchable() },
            "filterable_spiders" to spiders.values.count { it.isFilterable() }
        )
    }
    
    /**
     * 🗄️ 诊断缓存
     */
    private fun diagnoseCache(spiderManager: SpiderManager): Map<String, Any> {
        val cacheStats = spiderManager.getCacheManager().getStats()
        
        return mapOf(
            "cache_stats" to cacheStats,
            "cache_efficiency" to calculateCacheEfficiency(cacheStats)
        )
    }
    
    /**
     * ⚡ 诊断并发
     */
    private fun diagnoseConcurrent(spiderManager: SpiderManager): Map<String, Any> {
        val concurrentStats = spiderManager.getConcurrentManager().getConcurrentStats()
        val threadPoolStatus = spiderManager.getConcurrentManager().getThreadPoolStatus()
        
        return mapOf(
            "concurrent_stats" to concurrentStats,
            "thread_pool_status" to threadPoolStatus,
            "thread_pool_efficiency" to calculateThreadPoolEfficiency(threadPoolStatus)
        )
    }
    
    /**
     * 🌐 诊断网络
     */
    private fun diagnoseNetwork(spiderManager: SpiderManager): Map<String, Any> {
        val networkStats = spiderManager.getNetworkClient().getStats()
        
        return mapOf(
            "network_stats" to networkStats,
            "network_reliability" to calculateNetworkReliability(networkStats)
        )
    }
    
    /**
     * 🧠 诊断内存
     */
    private fun diagnoseMemory(): Map<String, Any> {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory
        
        return mapOf(
            "total_memory" to totalMemory,
            "used_memory" to usedMemory,
            "free_memory" to freeMemory,
            "max_memory" to maxMemory,
            "usage_ratio" to (usedMemory.toDouble() / maxMemory),
            "memory_pressure" to calculateMemoryPressure(usedMemory, maxMemory)
        )
    }
    
    /**
     * 📊 计算缓存效率
     */
    private fun calculateCacheEfficiency(stats: Map<String, Any>): Double {
        val hitRate = (stats["hit_rate"] as? Double) ?: 0.0
        return hitRate
    }
    
    /**
     * 📊 计算线程池效率
     */
    private fun calculateThreadPoolEfficiency(status: Map<String, Map<String, Any>>): Double {
        if (status.isEmpty()) return 0.0
        
        val totalUtilization = status.values.sumOf { pool ->
            val activeCount = (pool["active_count"] as? Int) ?: 0
            val maxPoolSize = (pool["max_pool_size"] as? Int) ?: 1
            activeCount.toDouble() / maxPoolSize
        }
        
        return totalUtilization / status.size
    }
    
    /**
     * 📊 计算网络可靠性
     */
    private fun calculateNetworkReliability(stats: Map<String, Any>): Double {
        val totalRequests = (stats["total_requests"] as? Long) ?: 0L
        val successRequests = (stats["success_requests"] as? Long) ?: 0L
        
        return if (totalRequests > 0) {
            successRequests.toDouble() / totalRequests
        } else {
            1.0
        }
    }
    
    /**
     * 📊 计算内存压力
     */
    private fun calculateMemoryPressure(usedMemory: Long, maxMemory: Long): String {
        val ratio = usedMemory.toDouble() / maxMemory
        
        return when {
            ratio > 0.9 -> "HIGH"
            ratio > 0.7 -> "MEDIUM"
            else -> "LOW"
        }
    }
}
