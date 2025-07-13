package top.cywin.onetv.film.monitoring

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.utils.DateTimeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 系统监控器
 * 
 * 基于 FongMi/TV 的系统监控实现
 * 提供实时系统监控、诊断和告警功能
 * 
 * 功能：
 * - 实时系统监控
 * - 性能指标收集
 * - 异常检测和告警
 * - 健康状态评估
 * - 诊断报告生成
 * - 自动恢复机制
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SystemMonitor(
    private val context: Context,
    private val spiderManager: SpiderManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_SYSTEM_MONITOR"
        private const val MONITORING_INTERVAL = 30000L // 30秒
        private const val HEALTH_CHECK_INTERVAL = 60000L // 1分钟
        private const val ALERT_THRESHOLD_MEMORY = 0.85f // 85%
        private const val ALERT_THRESHOLD_ERROR_RATE = 0.1f // 10%
        private const val ALERT_THRESHOLD_RESPONSE_TIME = 5000L // 5秒
    }
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 监控数据
    private val systemMetrics = SystemMetrics()
    private val healthStatus = HealthStatus()
    private val alertManager = AlertManager()
    
    // 事件流
    private val _events = MutableSharedFlow<MonitoringEvent>()
    val events: SharedFlow<MonitoringEvent> = _events.asSharedFlow()
    
    // 监控任务
    private var monitoringJob: Job? = null
    private var healthCheckJob: Job? = null
    
    // 诊断器
    private val diagnostics = SystemDiagnostics()
    
    init {
        startMonitoring()
        startHealthCheck()
        Log.d(TAG, "🏗️ 系统监控器初始化完成")
    }
    
    /**
     * 🚀 启动系统监控
     */
    private fun startMonitoring() {
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    collectSystemMetrics()
                    analyzeMetrics()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 系统监控异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 系统监控已启动")
    }
    
    /**
     * 🚀 启动健康检查
     */
    private fun startHealthCheck() {
        healthCheckJob = scope.launch {
            while (isActive) {
                try {
                    performHealthCheck()
                    delay(HEALTH_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 健康检查异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 健康检查已启动")
    }
    
    /**
     * 📊 收集系统指标
     */
    private suspend fun collectSystemMetrics() {
        val timestamp = System.currentTimeMillis()
        
        // 内存指标
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
        
        systemMetrics.recordMemoryMetrics(
            timestamp, totalMemory, usedMemory, freeMemory, maxMemory, memoryUsageRatio
        )
        
        // 线程指标
        val threadPoolStatus = spiderManager.getConcurrentManager().getThreadPoolStatus()
        val totalThreads = threadPoolStatus.values.sumOf { 
            (it["current_pool_size"] as? Int) ?: 0 
        }
        val activeThreads = threadPoolStatus.values.sumOf { 
            (it["active_count"] as? Int) ?: 0 
        }
        
        systemMetrics.recordThreadMetrics(timestamp, totalThreads, activeThreads)
        
        // 缓存指标
        val cacheStats = spiderManager.getCacheManager().getStats()
        val hitCount = (cacheStats["hit_count"] as? Long) ?: 0L
        val missCount = (cacheStats["miss_count"] as? Long) ?: 0L
        val hitRate = if (hitCount + missCount > 0) {
            hitCount.toDouble() / (hitCount + missCount)
        } else {
            0.0
        }
        
        systemMetrics.recordCacheMetrics(timestamp, hitCount, missCount, hitRate)
        
        // 网络指标
        val networkStats = spiderManager.getNetworkClient().getStats()
        val totalRequests = (networkStats["total_requests"] as? Long) ?: 0L
        val successRequests = (networkStats["success_requests"] as? Long) ?: 0L
        val failedRequests = (networkStats["failed_requests"] as? Long) ?: 0L
        val averageLatency = (networkStats["average_latency"] as? Long) ?: 0L
        
        systemMetrics.recordNetworkMetrics(
            timestamp, totalRequests, successRequests, failedRequests, averageLatency
        )
        
        // 并发指标
        val concurrentStats = spiderManager.getConcurrentManager().getConcurrentStats()
        val totalTasks = (concurrentStats["total_tasks"] as? Long) ?: 0L
        val completedTasks = (concurrentStats["completed_tasks"] as? Long) ?: 0L
        val failedTasks = (concurrentStats["failed_tasks"] as? Long) ?: 0L
        
        systemMetrics.recordConcurrentMetrics(timestamp, totalTasks, completedTasks, failedTasks)
        
        Log.d(TAG, "📊 系统指标收集完成")
    }
    
    /**
     * 🔍 分析指标
     */
    private suspend fun analyzeMetrics() {
        val currentMetrics = systemMetrics.getCurrentMetrics()
        
        // 内存告警检查
        val memoryUsageRatio = currentMetrics["memory_usage_ratio"] as? Float ?: 0f
        if (memoryUsageRatio > ALERT_THRESHOLD_MEMORY) {
            val alert = Alert(
                type = AlertType.MEMORY_HIGH,
                severity = AlertSeverity.WARNING,
                message = "内存使用率过高: ${String.format("%.1f%%", memoryUsageRatio * 100)}",
                timestamp = System.currentTimeMillis(),
                metrics = mapOf("memory_usage_ratio" to memoryUsageRatio)
            )
            alertManager.addAlert(alert)
            _events.emit(MonitoringEvent.AlertTriggered(alert))
        }
        
        // 网络错误率告警检查
        val totalRequests = currentMetrics["total_requests"] as? Long ?: 0L
        val failedRequests = currentMetrics["failed_requests"] as? Long ?: 0L
        if (totalRequests > 0) {
            val errorRate = failedRequests.toFloat() / totalRequests.toFloat()
            if (errorRate > ALERT_THRESHOLD_ERROR_RATE) {
                val alert = Alert(
                    type = AlertType.NETWORK_ERROR_HIGH,
                    severity = AlertSeverity.ERROR,
                    message = "网络错误率过高: ${String.format("%.1f%%", errorRate * 100)}",
                    timestamp = System.currentTimeMillis(),
                    metrics = mapOf("error_rate" to errorRate)
                )
                alertManager.addAlert(alert)
                _events.emit(MonitoringEvent.AlertTriggered(alert))
            }
        }
        
        // 响应时间告警检查
        val averageLatency = currentMetrics["average_latency"] as? Long ?: 0L
        if (averageLatency > ALERT_THRESHOLD_RESPONSE_TIME) {
            val alert = Alert(
                type = AlertType.RESPONSE_TIME_HIGH,
                severity = AlertSeverity.WARNING,
                message = "平均响应时间过长: ${averageLatency}ms",
                timestamp = System.currentTimeMillis(),
                metrics = mapOf("average_latency" to averageLatency)
            )
            alertManager.addAlert(alert)
            _events.emit(MonitoringEvent.AlertTriggered(alert))
        }
    }
    
    /**
     * 🏥 执行健康检查
     */
    private suspend fun performHealthCheck() {
        Log.d(TAG, "🏥 执行健康检查...")
        
        val healthChecks = mutableMapOf<String, HealthCheckResult>()
        
        // 检查 SpiderManager
        healthChecks["spider_manager"] = checkSpiderManagerHealth()
        
        // 检查缓存系统
        healthChecks["cache_system"] = checkCacheSystemHealth()
        
        // 检查并发系统
        healthChecks["concurrent_system"] = checkConcurrentSystemHealth()
        
        // 检查网络系统
        healthChecks["network_system"] = checkNetworkSystemHealth()
        
        // 检查数据仓库
        healthChecks["data_repository"] = checkDataRepositoryHealth()
        
        // 更新健康状态
        healthStatus.updateHealthChecks(healthChecks)
        
        // 计算整体健康评分
        val overallHealth = calculateOverallHealth(healthChecks)
        healthStatus.updateOverallHealth(overallHealth)
        
        _events.emit(MonitoringEvent.HealthCheckCompleted(healthChecks, overallHealth))
        
        Log.d(TAG, "✅ 健康检查完成，整体健康度: ${String.format("%.1f%%", overallHealth * 100)}")
    }
    
    /**
     * 🕷️ 检查 SpiderManager 健康状态
     */
    private suspend fun checkSpiderManagerHealth(): HealthCheckResult {
        return try {
            val spiders = spiderManager.getAllSpiders()
            val spiderCount = spiders.size
            
            if (spiderCount > 0) {
                HealthCheckResult(
                    status = HealthCheckStatus.HEALTHY,
                    message = "SpiderManager 正常，Spider 数量: $spiderCount",
                    details = mapOf("spider_count" to spiderCount)
                )
            } else {
                HealthCheckResult(
                    status = HealthCheckStatus.UNHEALTHY,
                    message = "SpiderManager 异常，无可用 Spider",
                    details = mapOf("spider_count" to 0)
                )
            }
        } catch (e: Exception) {
            HealthCheckResult(
                status = HealthCheckStatus.UNHEALTHY,
                message = "SpiderManager 检查失败: ${e.message}",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * 🗄️ 检查缓存系统健康状态
     */
    private suspend fun checkCacheSystemHealth(): HealthCheckResult {
        return try {
            val cacheManager = spiderManager.getCacheManager()
            val stats = cacheManager.getStats()
            val hitRate = (stats["hit_rate"] as? Double) ?: 0.0
            
            val status = when {
                hitRate > 0.7 -> HealthCheckStatus.HEALTHY
                hitRate > 0.5 -> HealthCheckStatus.DEGRADED
                else -> HealthCheckStatus.UNHEALTHY
            }
            
            HealthCheckResult(
                status = status,
                message = "缓存系统状态: ${status.name}，命中率: ${String.format("%.1f%%", hitRate * 100)}",
                details = mapOf("hit_rate" to hitRate)
            )
        } catch (e: Exception) {
            HealthCheckResult(
                status = HealthCheckStatus.UNHEALTHY,
                message = "缓存系统检查失败: ${e.message}",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * ⚡ 检查并发系统健康状态
     */
    private suspend fun checkConcurrentSystemHealth(): HealthCheckResult {
        return try {
            val concurrentManager = spiderManager.getConcurrentManager()
            val stats = concurrentManager.getConcurrentStats()
            val totalTasks = (stats["total_tasks"] as? Long) ?: 0L
            val completedTasks = (stats["completed_tasks"] as? Long) ?: 0L
            val failedTasks = (stats["failed_tasks"] as? Long) ?: 0L
            
            val successRate = if (totalTasks > 0) {
                completedTasks.toDouble() / totalTasks.toDouble()
            } else {
                1.0
            }
            
            val status = when {
                successRate > 0.9 -> HealthCheckStatus.HEALTHY
                successRate > 0.8 -> HealthCheckStatus.DEGRADED
                else -> HealthCheckStatus.UNHEALTHY
            }
            
            HealthCheckResult(
                status = status,
                message = "并发系统状态: ${status.name}，成功率: ${String.format("%.1f%%", successRate * 100)}",
                details = mapOf(
                    "total_tasks" to totalTasks,
                    "completed_tasks" to completedTasks,
                    "failed_tasks" to failedTasks,
                    "success_rate" to successRate
                )
            )
        } catch (e: Exception) {
            HealthCheckResult(
                status = HealthCheckStatus.UNHEALTHY,
                message = "并发系统检查失败: ${e.message}",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * 🌐 检查网络系统健康状态
     */
    private suspend fun checkNetworkSystemHealth(): HealthCheckResult {
        return try {
            val networkClient = spiderManager.getNetworkClient()
            val stats = networkClient.getStats()
            val totalRequests = (stats["total_requests"] as? Long) ?: 0L
            val successRequests = (stats["success_requests"] as? Long) ?: 0L
            val averageLatency = (stats["average_latency"] as? Long) ?: 0L
            
            val successRate = if (totalRequests > 0) {
                successRequests.toDouble() / totalRequests.toDouble()
            } else {
                1.0
            }
            
            val status = when {
                successRate > 0.9 && averageLatency < 2000L -> HealthCheckStatus.HEALTHY
                successRate > 0.8 && averageLatency < 5000L -> HealthCheckStatus.DEGRADED
                else -> HealthCheckStatus.UNHEALTHY
            }
            
            HealthCheckResult(
                status = status,
                message = "网络系统状态: ${status.name}，成功率: ${String.format("%.1f%%", successRate * 100)}，延迟: ${averageLatency}ms",
                details = mapOf(
                    "success_rate" to successRate,
                    "average_latency" to averageLatency
                )
            )
        } catch (e: Exception) {
            HealthCheckResult(
                status = HealthCheckStatus.UNHEALTHY,
                message = "网络系统检查失败: ${e.message}",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * 💾 检查数据仓库健康状态
     */
    private suspend fun checkDataRepositoryHealth(): HealthCheckResult {
        return try {
            val filmRepository = spiderManager.getFilmRepository()
            val stats = filmRepository.getRepositoryStats()
            
            // 检查基本功能
            val sitesResult = filmRepository.getSites()
            val isHealthy = sitesResult.isSuccess
            
            val status = if (isHealthy) {
                HealthCheckStatus.HEALTHY
            } else {
                HealthCheckStatus.UNHEALTHY
            }
            
            HealthCheckResult(
                status = status,
                message = "数据仓库状态: ${status.name}",
                details = stats
            )
        } catch (e: Exception) {
            HealthCheckResult(
                status = HealthCheckStatus.UNHEALTHY,
                message = "数据仓库检查失败: ${e.message}",
                details = mapOf("error" to e.message)
            )
        }
    }
    
    /**
     * 📊 计算整体健康度
     */
    private fun calculateOverallHealth(healthChecks: Map<String, HealthCheckResult>): Double {
        if (healthChecks.isEmpty()) return 0.0
        
        val totalScore = healthChecks.values.sumOf { result ->
            when (result.status) {
                HealthCheckStatus.HEALTHY -> 100.0
                HealthCheckStatus.DEGRADED -> 70.0
                HealthCheckStatus.UNHEALTHY -> 0.0
            }
        }
        
        return totalScore / healthChecks.size / 100.0
    }
    
    /**
     * 🔍 生成诊断报告
     */
    suspend fun generateDiagnosticReport(): DiagnosticReport {
        Log.d(TAG, "🔍 生成诊断报告...")
        
        val timestamp = System.currentTimeMillis()
        val currentMetrics = systemMetrics.getCurrentMetrics()
        val currentHealth = healthStatus.getCurrentHealth()
        val recentAlerts = alertManager.getRecentAlerts(24 * 60 * 60 * 1000L) // 24小时内
        
        // 执行深度诊断
        val diagnosticResults = diagnostics.performDiagnostics(spiderManager)
        
        val report = DiagnosticReport(
            timestamp = timestamp,
            systemMetrics = currentMetrics,
            healthStatus = currentHealth,
            alerts = recentAlerts,
            diagnosticResults = diagnosticResults,
            recommendations = generateRecommendations(currentMetrics, currentHealth, recentAlerts)
        )
        
        _events.emit(MonitoringEvent.DiagnosticReportGenerated(report))
        
        Log.d(TAG, "✅ 诊断报告生成完成")
        
        return report
    }
    
    /**
     * 💡 生成建议
     */
    private fun generateRecommendations(
        metrics: Map<String, Any>,
        health: Map<String, Any>,
        alerts: List<Alert>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 基于指标的建议
        val memoryUsageRatio = metrics["memory_usage_ratio"] as? Float ?: 0f
        if (memoryUsageRatio > 0.8f) {
            recommendations.add("内存使用率较高，建议清理缓存或增加内存限制")
        }
        
        // 基于健康状态的建议
        val overallHealth = health["overall_health"] as? Double ?: 0.0
        if (overallHealth < 0.8) {
            recommendations.add("系统整体健康度较低，建议检查各组件状态")
        }
        
        // 基于告警的建议
        if (alerts.isNotEmpty()) {
            val alertTypes = alerts.map { it.type }.distinct()
            alertTypes.forEach { alertType ->
                when (alertType) {
                    AlertType.MEMORY_HIGH -> recommendations.add("频繁内存告警，建议优化内存使用")
                    AlertType.NETWORK_ERROR_HIGH -> recommendations.add("网络错误率高，建议检查网络配置")
                    AlertType.RESPONSE_TIME_HIGH -> recommendations.add("响应时间长，建议优化性能")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("系统运行良好，无特殊建议")
        }
        
        return recommendations
    }
    
    /**
     * 📊 获取监控统计
     */
    fun getMonitoringStats(): Map<String, Any> {
        return mapOf(
            "system_metrics" to systemMetrics.getStats(),
            "health_status" to healthStatus.getStats(),
            "alert_manager" to alertManager.getStats(),
            "monitoring_uptime" to (System.currentTimeMillis() - systemMetrics.startTime)
        )
    }
    
    /**
     * 🛑 关闭监控器
     */
    fun shutdown() {
        monitoringJob?.cancel()
        healthCheckJob?.cancel()
        scope.cancel()
        
        Log.d(TAG, "🛑 系统监控器已关闭")
    }
}
