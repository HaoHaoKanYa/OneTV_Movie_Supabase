package top.cywin.onetv.movie.data.cache

import android.content.Context
import kotlinx.coroutines.*
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.repository.WatchHistoryRepository
import top.cywin.onetv.movie.MovieApp
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 缓存优化服务 (智能缓存管理)
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class CacheOptimizationService(
    private val context: Context,
    private val cacheManager: VodCacheManager,
    private val historyRepository: WatchHistoryRepository
) {

    // 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isOptimizing = false
    
    /**
     * 启动缓存优化服务
     */
    fun startOptimization() {
        serviceScope.launch {
            while (true) {
                try {
                    delay(2 * 60 * 60 * 1000) // 每2小时执行一次
                    
                    if (!isOptimizing) {
                        performOptimization()
                    }
                } catch (e: Exception) {
                    // 优化失败不影响主流程
                }
            }
        }
    }
    
    /**
     * 执行缓存优化
     */
    private suspend fun performOptimization() {
        isOptimizing = true
        
        try {
            // 1. 基础缓存清理
            cacheManager.smartCleanup()
            
            // 2. 智能预加载
            performSmartPreload()
            
            // 3. 缓存预热
            performCacheWarmup()
            
            // 4. 清理无用缓存
            cleanupUnusedCache()
            
        } finally {
            isOptimizing = false
        }
    }
    
    /**
     * 智能预加载 (基于用户行为)
     */
    private suspend fun performSmartPreload() {
        try {
            // 1. 获取用户最近观看的内容
            val recentHistory = historyRepository.getRecentHistory()
            
            // 2. 预加载相关内容
            recentHistory.forEach { history ->
                try {
                    // 预加载同类型的热门内容
                    val relatedResult = vodRepository.getContentList(
                        typeId = "1", // 根据历史记录的类型动态获取
                        page = 1,
                        siteKey = history.siteKey
                    )
                    
                    relatedResult.getOrNull()?.list?.take(5)?.let { items ->
                        cacheManager.preloadContent(items)
                    }
                } catch (e: Exception) {
                    // 预加载失败不影响主流程
                }
            }
        } catch (e: Exception) {
            // 预加载失败不影响主流程
        }
    }
    
    /**
     * 缓存预热 (预加载热门内容)
     */
    private suspend fun performCacheWarmup() {
        try {
            // 预加载首页推荐内容
            val recommendResult = vodRepository.getRecommendContent()
            recommendResult.getOrNull()?.let { items ->
                cacheManager.preloadContent(items.take(10))
            }
        } catch (e: Exception) {
            // 预热失败不影响主流程
        }
    }
    
    /**
     * 清理无用缓存
     */
    private suspend fun cleanupUnusedCache() {
        try {
            // 清理超过7天未访问的缓存
            val expireTime = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            cacheManager.clearExpired()
        } catch (e: Exception) {
            // 清理失败不影响主流程
        }
    }
    
    /**
     * 手动优化缓存
     */
    suspend fun manualOptimize(): OptimizationResult {
        return try {
            val startTime = System.currentTimeMillis()
            val beforeStats = cacheManager.getCacheStats()
            
            performOptimization()
            
            val afterStats = cacheManager.getCacheStats()
            val duration = System.currentTimeMillis() - startTime
            
            OptimizationResult(
                success = true,
                duration = duration,
                beforeStats = beforeStats,
                afterStats = afterStats
            )
        } catch (e: Exception) {
            OptimizationResult(
                success = false,
                error = e.message ?: "优化失败"
            )
        }
    }
    
    /**
     * 获取缓存健康状态
     */
    suspend fun getCacheHealth(): CacheHealth {
        val stats = cacheManager.getCacheStats()
        val hitRate = stats["cache_hit_rate"] as? Double ?: 0.0
        val cacheSize = cacheManager.getCacheSize()
        val maxSize = 200 * 1024 * 1024L // 200MB
        
        return CacheHealth(
            hitRate = hitRate,
            sizeUsage = cacheSize.toDouble() / maxSize,
            isHealthy = hitRate > 0.7 && cacheSize < maxSize * 0.8,
            recommendations = generateRecommendations(hitRate, cacheSize, maxSize)
        )
    }
    
    /**
     * 生成优化建议
     */
    private fun generateRecommendations(hitRate: Double, cacheSize: Long, maxSize: Long): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (hitRate < 0.5) {
            recommendations.add("缓存命中率较低，建议增加预加载")
        }
        
        if (cacheSize > maxSize * 0.9) {
            recommendations.add("缓存空间不足，建议清理旧缓存")
        }
        
        if (hitRate > 0.9 && cacheSize < maxSize * 0.3) {
            recommendations.add("缓存效果良好，可以适当增加缓存内容")
        }
        
        return recommendations
    }
    
    /**
     * 停止优化服务
     */
    fun stopOptimization() {
        serviceScope.cancel()
    }
}

/**
 * 优化结果
 */
data class OptimizationResult(
    val success: Boolean,
    val duration: Long = 0L,
    val beforeStats: Map<String, Any> = emptyMap(),
    val afterStats: Map<String, Any> = emptyMap(),
    val error: String? = null
)

/**
 * 缓存健康状态
 */
data class CacheHealth(
    val hitRate: Double,
    val sizeUsage: Double,
    val isHealthy: Boolean,
    val recommendations: List<String>
)
