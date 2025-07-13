package top.cywin.onetv.film.optimization

import top.cywin.onetv.film.utils.DateTimeUtils
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 优化相关数据模型
 * 
 * 基于 FongMi/TV 的性能优化数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 性能报告
 */
data class PerformanceReport(
    val timestamp: Long,                        // 报告时间戳
    val performanceScore: Float,                // 性能评分 (0-100)
    val memoryAnalysis: MemoryAnalysis,         // 内存分析
    val cpuAnalysis: CpuAnalysis,               // CPU分析
    val networkAnalysis: NetworkAnalysis,       // 网络分析
    val cacheAnalysis: CacheAnalysis,           // 缓存分析
    val concurrentAnalysis: ConcurrentAnalysis, // 并发分析
    val recommendations: List<OptimizationRecommendation>, // 优化建议
    val metrics: Map<String, Any>               // 原始指标
) {
    
    /**
     * 📊 获取报告摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "timestamp" to DateTimeUtils.formatTimestamp(timestamp),
            "performance_score" to String.format("%.1f", performanceScore),
            "performance_grade" to getPerformanceGrade(),
            "memory_status" to memoryAnalysis.status.name,
            "cpu_status" to cpuAnalysis.status.name,
            "network_status" to networkAnalysis.status.name,
            "cache_status" to cacheAnalysis.status.name,
            "concurrent_status" to concurrentAnalysis.status.name,
            "recommendations_count" to recommendations.size,
            "high_priority_recommendations" to recommendations.count { it.priority == Priority.HIGH },
            "auto_applicable_recommendations" to recommendations.count { it.autoApplicable }
        )
    }
    
    /**
     * 🏆 获取性能等级
     */
    private fun getPerformanceGrade(): String {
        return when {
            performanceScore >= 90f -> "优秀"
            performanceScore >= 80f -> "良好"
            performanceScore >= 70f -> "一般"
            performanceScore >= 60f -> "较差"
            else -> "很差"
        }
    }
}

/**
 * 内存分析
 */
data class MemoryAnalysis(
    val totalMemory: Long,                      // 总内存
    val usedMemory: Long,                       // 已用内存
    val freeMemory: Long,                       // 空闲内存
    val maxMemory: Long,                        // 最大内存
    val availableMemory: Long,                  // 可用内存
    val usageRatio: Float,                      // 使用率
    val status: MemoryStatus                    // 内存状态
) {
    
    /**
     * 📊 获取内存摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "total_memory_mb" to String.format("%.2f", totalMemory / 1024.0 / 1024.0),
            "used_memory_mb" to String.format("%.2f", usedMemory / 1024.0 / 1024.0),
            "free_memory_mb" to String.format("%.2f", freeMemory / 1024.0 / 1024.0),
            "max_memory_mb" to String.format("%.2f", maxMemory / 1024.0 / 1024.0),
            "available_memory_mb" to String.format("%.2f", availableMemory / 1024.0 / 1024.0),
            "usage_ratio" to String.format("%.1f%%", usageRatio * 100),
            "status" to status.name,
            "status_description" to status.description
        )
    }
}

/**
 * 内存状态
 */
enum class MemoryStatus(val description: String) {
    NORMAL("正常"),
    MODERATE("中等"),
    HIGH("偏高"),
    CRITICAL("严重")
}

/**
 * CPU分析
 */
data class CpuAnalysis(
    val cpuCores: Int,                          // CPU核心数
    val totalThreads: Int,                      // 总线程数
    val activeThreads: Int,                     // 活跃线程数
    val threadUtilization: Float,               // 线程利用率
    val status: CpuStatus                       // CPU状态
) {
    
    /**
     * 📊 获取CPU摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "cpu_cores" to cpuCores,
            "total_threads" to totalThreads,
            "active_threads" to activeThreads,
            "thread_utilization" to String.format("%.1f%%", threadUtilization * 100),
            "status" to status.name,
            "status_description" to status.description
        )
    }
}

/**
 * CPU状态
 */
enum class CpuStatus(val description: String) {
    NORMAL("正常"),
    MODERATE("中等"),
    HIGH("偏高")
}

/**
 * 网络分析
 */
data class NetworkAnalysis(
    val totalRequests: Long,                    // 总请求数
    val successRequests: Long,                  // 成功请求数
    val failedRequests: Long,                   // 失败请求数
    val successRate: Float,                     // 成功率
    val errorRate: Float,                       // 错误率
    val averageLatency: Long,                   // 平均延迟
    val status: NetworkStatus                   // 网络状态
) {
    
    /**
     * 📊 获取网络摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "total_requests" to totalRequests,
            "success_requests" to successRequests,
            "failed_requests" to failedRequests,
            "success_rate" to String.format("%.1f%%", successRate * 100),
            "error_rate" to String.format("%.1f%%", errorRate * 100),
            "average_latency_ms" to averageLatency,
            "status" to status.name,
            "status_description" to status.description
        )
    }
}

/**
 * 网络状态
 */
enum class NetworkStatus(val description: String) {
    GOOD("良好"),
    MODERATE("一般"),
    POOR("较差")
}

/**
 * 缓存分析
 */
data class CacheAnalysis(
    val hitCount: Long,                         // 命中次数
    val missCount: Long,                        // 未命中次数
    val hitRate: Double,                        // 命中率
    val memoryUsage: Long,                      // 内存使用量
    val diskUsage: Long,                        // 磁盘使用量
    val status: CacheStatus                     // 缓存状态
) {
    
    /**
     * 📊 获取缓存摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "hit_count" to hitCount,
            "miss_count" to missCount,
            "total_requests" to (hitCount + missCount),
            "hit_rate" to String.format("%.1f%%", hitRate * 100),
            "memory_usage_mb" to String.format("%.2f", memoryUsage / 1024.0 / 1024.0),
            "disk_usage_mb" to String.format("%.2f", diskUsage / 1024.0 / 1024.0),
            "status" to status.name,
            "status_description" to status.description
        )
    }
}

/**
 * 缓存状态
 */
enum class CacheStatus(val description: String) {
    GOOD("良好"),
    MODERATE("一般"),
    POOR("较差")
}

/**
 * 并发分析
 */
data class ConcurrentAnalysis(
    val totalTasks: Long,                       // 总任务数
    val completedTasks: Long,                   // 完成任务数
    val failedTasks: Long,                      // 失败任务数
    val successRate: Float,                     // 成功率
    val failureRate: Float,                     // 失败率
    val averageExecutionTime: Long,             // 平均执行时间
    val status: ConcurrentStatus                // 并发状态
) {
    
    /**
     * 📊 获取并发摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "total_tasks" to totalTasks,
            "completed_tasks" to completedTasks,
            "failed_tasks" to failedTasks,
            "success_rate" to String.format("%.1f%%", successRate * 100),
            "failure_rate" to String.format("%.1f%%", failureRate * 100),
            "average_execution_time_ms" to averageExecutionTime,
            "status" to status.name,
            "status_description" to status.description
        )
    }
}

/**
 * 并发状态
 */
enum class ConcurrentStatus(val description: String) {
    GOOD("良好"),
    MODERATE("一般"),
    POOR("较差")
}

/**
 * 优化建议
 */
data class OptimizationRecommendation(
    val type: String,                           // 建议类型
    val priority: Priority,                     // 优先级
    val description: String,                    // 描述
    val autoApplicable: Boolean,                // 是否可自动应用
    val impact: String,                         // 预期影响
    val details: Map<String, Any> = emptyMap() // 详细信息
) {
    
    /**
     * 📊 获取建议摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "type" to type,
            "priority" to priority.name,
            "priority_level" to priority.level,
            "description" to description,
            "auto_applicable" to autoApplicable,
            "impact" to impact,
            "details_count" to details.size
        )
    }
}

/**
 * 优先级
 */
enum class Priority(val level: Int, val description: String) {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    CRITICAL(4, "紧急")
}

/**
 * 优化事件
 */
sealed class OptimizationEvent {
    
    /**
     * 分析完成
     */
    data class AnalysisCompleted(val report: PerformanceReport) : OptimizationEvent()
    
    /**
     * 优化应用
     */
    data class OptimizationApplied(val optimizations: List<String>) : OptimizationEvent()
    
    /**
     * 性能警告
     */
    data class PerformanceWarning(val component: String, val message: String) : OptimizationEvent()
    
    /**
     * 优化建议
     */
    data class OptimizationSuggestion(val recommendation: OptimizationRecommendation) : OptimizationEvent()
}

/**
 * 优化统计
 */
class OptimizationStatistics {
    
    private val analysisCount = AtomicLong(0)
    private val optimizationCount = AtomicLong(0)
    private val totalAnalysisTime = AtomicLong(0)
    private val totalRecommendations = AtomicLong(0)
    private val appliedOptimizations = AtomicLong(0)
    
    private val componentStats = mutableMapOf<String, ComponentStats>()
    
    /**
     * 📊 记录分析
     */
    fun recordAnalysis(duration: Long, recommendationCount: Int) {
        analysisCount.incrementAndGet()
        totalAnalysisTime.addAndGet(duration)
        totalRecommendations.addAndGet(recommendationCount.toLong())
    }
    
    /**
     * 🔧 记录优化
     */
    fun recordOptimization(count: Int) {
        optimizationCount.incrementAndGet()
        appliedOptimizations.addAndGet(count.toLong())
    }
    
    /**
     * 📈 记录组件统计
     */
    fun recordComponentStats(component: String, score: Float, status: String) {
        val stats = componentStats.computeIfAbsent(component) { ComponentStats() }
        stats.recordScore(score)
        stats.updateStatus(status)
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val avgAnalysisTime = if (analysisCount.get() > 0) {
            totalAnalysisTime.get() / analysisCount.get()
        } else {
            0L
        }
        
        val avgRecommendations = if (analysisCount.get() > 0) {
            totalRecommendations.get().toDouble() / analysisCount.get()
        } else {
            0.0
        }
        
        return mapOf(
            "analysis_count" to analysisCount.get(),
            "optimization_count" to optimizationCount.get(),
            "total_analysis_time_ms" to totalAnalysisTime.get(),
            "avg_analysis_time_ms" to avgAnalysisTime,
            "total_recommendations" to totalRecommendations.get(),
            "avg_recommendations" to String.format("%.1f", avgRecommendations),
            "applied_optimizations" to appliedOptimizations.get(),
            "component_stats" to componentStats.mapValues { it.value.getStats() }
        )
    }
    
    /**
     * 🧹 清理统计
     */
    fun clear() {
        analysisCount.set(0)
        optimizationCount.set(0)
        totalAnalysisTime.set(0)
        totalRecommendations.set(0)
        appliedOptimizations.set(0)
        componentStats.clear()
    }
}

/**
 * 组件统计
 */
class ComponentStats {
    
    private val scoreHistory = mutableListOf<Float>()
    private var currentStatus = "UNKNOWN"
    private val statusHistory = mutableListOf<String>()
    
    /**
     * 📊 记录评分
     */
    fun recordScore(score: Float) {
        scoreHistory.add(score)
        // 保持最近100个记录
        if (scoreHistory.size > 100) {
            scoreHistory.removeAt(0)
        }
    }
    
    /**
     * 🔄 更新状态
     */
    fun updateStatus(status: String) {
        if (currentStatus != status) {
            currentStatus = status
            statusHistory.add(status)
            // 保持最近50个状态记录
            if (statusHistory.size > 50) {
                statusHistory.removeAt(0)
            }
        }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val avgScore = if (scoreHistory.isNotEmpty()) {
            scoreHistory.average()
        } else {
            0.0
        }
        
        val minScore = scoreHistory.minOrNull() ?: 0f
        val maxScore = scoreHistory.maxOrNull() ?: 0f
        
        return mapOf(
            "current_status" to currentStatus,
            "avg_score" to String.format("%.1f", avgScore),
            "min_score" to minScore,
            "max_score" to maxScore,
            "score_history_size" to scoreHistory.size,
            "status_history_size" to statusHistory.size,
            "recent_scores" to scoreHistory.takeLast(10),
            "recent_statuses" to statusHistory.takeLast(5)
        )
    }
}

/**
 * 性能基准
 */
data class PerformanceBenchmark(
    val name: String,                           // 基准名称
    val category: String,                       // 分类
    val targetValue: Double,                    // 目标值
    val currentValue: Double,                   // 当前值
    val unit: String,                           // 单位
    val direction: BenchmarkDirection,          // 方向（越高越好/越低越好）
    val timestamp: Long = System.currentTimeMillis() // 时间戳
) {
    
    /**
     * 📊 计算达成率
     */
    fun getAchievementRate(): Double {
        return when (direction) {
            BenchmarkDirection.HIGHER_IS_BETTER -> {
                if (targetValue > 0) (currentValue / targetValue).coerceAtMost(1.0) else 0.0
            }
            BenchmarkDirection.LOWER_IS_BETTER -> {
                if (currentValue > 0) (targetValue / currentValue).coerceAtMost(1.0) else 1.0
            }
        }
    }
    
    /**
     * 🎯 是否达标
     */
    fun isTargetMet(): Boolean {
        return when (direction) {
            BenchmarkDirection.HIGHER_IS_BETTER -> currentValue >= targetValue
            BenchmarkDirection.LOWER_IS_BETTER -> currentValue <= targetValue
        }
    }
    
    /**
     * 📊 获取基准摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "category" to category,
            "target_value" to targetValue,
            "current_value" to currentValue,
            "unit" to unit,
            "direction" to direction.name,
            "achievement_rate" to String.format("%.1f%%", getAchievementRate() * 100),
            "target_met" to isTargetMet(),
            "timestamp" to DateTimeUtils.formatTimestamp(timestamp)
        )
    }
}

/**
 * 基准方向
 */
enum class BenchmarkDirection {
    HIGHER_IS_BETTER,   // 越高越好
    LOWER_IS_BETTER     // 越低越好
}
