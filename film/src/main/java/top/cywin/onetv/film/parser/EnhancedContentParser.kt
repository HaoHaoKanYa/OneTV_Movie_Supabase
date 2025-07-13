package top.cywin.onetv.film.parser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.concurrent.ConcurrentSearcher
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.data.models.VodResponse
import top.cywin.onetv.film.data.models.VodItem
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.cache.CacheManager

/**
 * 增强内容解析器
 * 
 * 基于 FongMi/TV 的内容解析系统
 * 负责统一管理所有 Spider 的内容解析
 * 
 * 核心功能：
 * - 统一内容解析接口
 * - 多站点并发搜索
 * - 内容缓存管理
 * - 解析结果聚合
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EnhancedContentParser(
    private val context: Context,
    private val spiderManager: SpiderManager,
    private val concurrentSearcher: ConcurrentSearcher,
    private val cacheManager: CacheManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_ENHANCED_CONTENT_PARSER"
        private const val CONTENT_CACHE_DURATION = 10 * 60 * 1000L // 10分钟
        private const val SEARCH_CACHE_DURATION = 5 * 60 * 1000L // 5分钟
    }
    
    /**
     * 搜索内容
     * 
     * 这是 FongMi/TV 内容搜索的核心入口
     */
    suspend fun searchContent(
        keyword: String,
        sites: List<VodSite> = emptyList(),
        quick: Boolean = false
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 开始搜索内容: $keyword, 站点数: ${sites.size}, 快速搜索: $quick")
            
            // 1. 检查搜索缓存
            val cacheKey = "search:${StringUtils.md5(keyword)}:${sites.map { it.key }.sorted().joinToString(",")}"
            val cachedResult = getCachedSearchResult(cacheKey)
            if (cachedResult != null && !quick) {
                Log.d(TAG, "✅ 使用缓存搜索结果")
                return@withContext Result.success(cachedResult)
            }
            
            // 2. 执行搜索
            val searchResults = if (sites.isEmpty()) {
                // 搜索所有可用站点
                searchAllSites(keyword, quick)
            } else {
                // 搜索指定站点
                searchSpecificSites(keyword, sites, quick)
            }
            
            // 3. 聚合搜索结果
            val aggregatedResult = aggregateSearchResults(searchResults, keyword)
            
            // 4. 缓存搜索结果
            if (!quick) {
                cacheSearchResult(cacheKey, aggregatedResult)
            }
            
            Log.d(TAG, "✅ 搜索完成，结果数量: ${aggregatedResult.list.size}")
            Result.success(aggregatedResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索内容失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取分类内容
     */
    suspend fun getCategoryContent(
        siteKey: String,
        tid: String,
        page: Int = 1,
        filters: Map<String, String> = emptyMap()
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📂 获取分类内容: $siteKey, tid=$tid, page=$page")
            
            // 1. 检查内容缓存
            val cacheKey = "category:$siteKey:$tid:$page:${filters.hashCode()}"
            val cachedResult = getCachedContentResult(cacheKey)
            if (cachedResult != null) {
                Log.d(TAG, "✅ 使用缓存分类内容")
                return@withContext Result.success(cachedResult)
            }
            
            // 2. 获取 Spider 实例
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(Exception("Spider not found: $siteKey"))
            }
            
            // 3. 执行分类内容获取
            val resultJson = spider.categoryContent(tid, page.toString(), true, HashMap(filters))
            val vodResponse = JsonUtils.parseVodResponse(resultJson)
            
            // 4. 缓存结果
            cacheContentResult(cacheKey, vodResponse)
            
            Log.d(TAG, "✅ 分类内容获取完成: ${vodResponse.list.size} 个项目")
            Result.success(vodResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分类内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取详情内容
     */
    suspend fun getDetailContent(
        siteKey: String,
        ids: List<String>
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📄 获取详情内容: $siteKey, ids=$ids")
            
            // 1. 检查详情缓存
            val cacheKey = "detail:$siteKey:${ids.sorted().joinToString(",")}"
            val cachedResult = getCachedContentResult(cacheKey)
            if (cachedResult != null) {
                Log.d(TAG, "✅ 使用缓存详情内容")
                return@withContext Result.success(cachedResult)
            }
            
            // 2. 获取 Spider 实例
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(Exception("Spider not found: $siteKey"))
            }
            
            // 3. 执行详情内容获取
            val resultJson = spider.detailContent(ids)
            val vodResponse = JsonUtils.parseVodResponse(resultJson)
            
            // 4. 缓存结果
            cacheContentResult(cacheKey, vodResponse)
            
            Log.d(TAG, "✅ 详情内容获取完成: ${vodResponse.list.size} 个详情")
            Result.success(vodResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取详情内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(siteKey: String): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🏠 获取首页内容: $siteKey")
            
            // 1. 检查首页缓存
            val cacheKey = "home:$siteKey"
            val cachedResult = getCachedContentResult(cacheKey)
            if (cachedResult != null) {
                Log.d(TAG, "✅ 使用缓存首页内容")
                return@withContext Result.success(cachedResult)
            }
            
            // 2. 获取 Spider 实例
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(Exception("Spider not found: $siteKey"))
            }
            
            // 3. 执行首页内容获取
            val resultJson = spider.homeContent(true)
            val vodResponse = JsonUtils.parseVodResponse(resultJson)
            
            // 4. 缓存结果
            cacheContentResult(cacheKey, vodResponse)
            
            Log.d(TAG, "✅ 首页内容获取完成: ${vodResponse.list.size} 个项目")
            Result.success(vodResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取首页内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 搜索所有站点
     */
    private suspend fun searchAllSites(keyword: String, quick: Boolean): List<VodResponse> {
        return supervisorScope {
            val availableSites = spiderManager.getAvailableSites()
            val searchableSites = availableSites.filter { it.searchable == 1 }
            
            Log.d(TAG, "🔍 搜索所有站点: ${searchableSites.size} 个可搜索站点")
            
            searchableSites.map { site ->
                async {
                    try {
                        val spider = spiderManager.getSpider(site.key)
                        if (spider != null) {
                            val resultJson = spider.searchContent(keyword, quick)
                            JsonUtils.parseVodResponse(resultJson)
                        } else {
                            VodResponse(list = emptyList())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ 站点搜索失败: ${site.key}", e)
                        VodResponse(list = emptyList())
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * 搜索指定站点
     */
    private suspend fun searchSpecificSites(
        keyword: String,
        sites: List<VodSite>,
        quick: Boolean
    ): List<VodResponse> {
        return supervisorScope {
            sites.map { site ->
                async {
                    try {
                        val spider = spiderManager.getSpider(site.key)
                        if (spider != null) {
                            val resultJson = spider.searchContent(keyword, quick)
                            JsonUtils.parseVodResponse(resultJson)
                        } else {
                            VodResponse(list = emptyList())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ 站点搜索失败: ${site.key}", e)
                        VodResponse(list = emptyList())
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * 聚合搜索结果
     */
    private fun aggregateSearchResults(results: List<VodResponse>, keyword: String): VodResponse {
        val allItems = mutableListOf<VodItem>()
        
        results.forEach { response ->
            allItems.addAll(response.list)
        }
        
        // 去重和排序
        val uniqueItems = allItems.distinctBy { "${it.vod_name}_${it.vod_id}" }
            .sortedByDescending { it.vod_time }
        
        return VodResponse(
            list = uniqueItems,
            total = uniqueItems.size,
            page = 1,
            pagecount = 1,
            limit = uniqueItems.size
        )
    }
    
    /**
     * 获取缓存搜索结果
     */
    private suspend fun getCachedSearchResult(cacheKey: String): VodResponse? {
        return try {
            cacheManager.get(cacheKey, VodResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 缓存搜索结果
     */
    private suspend fun cacheSearchResult(cacheKey: String, result: VodResponse) {
        try {
            cacheManager.put(cacheKey, result, SEARCH_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存搜索结果失败", e)
        }
    }
    
    /**
     * 获取缓存内容结果
     */
    private suspend fun getCachedContentResult(cacheKey: String): VodResponse? {
        return try {
            cacheManager.get(cacheKey, VodResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 缓存内容结果
     */
    private suspend fun cacheContentResult(cacheKey: String, result: VodResponse) {
        try {
            cacheManager.put(cacheKey, result, CONTENT_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存内容结果失败", e)
        }
    }
    
    /**
     * 清除内容缓存
     */
    suspend fun clearContentCache() {
        try {
            cacheManager.clearByPrefix("search:")
            cacheManager.clearByPrefix("category:")
            cacheManager.clearByPrefix("detail:")
            cacheManager.clearByPrefix("home:")
            Log.d(TAG, "✅ 内容缓存已清除")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 清除内容缓存失败", e)
        }
    }
    
    /**
     * 获取内容解析统计信息
     */
    fun getContentStats(): Map<String, Any> {
        return mapOf(
            "parser_type" to "EnhancedContentParser",
            "cache_enabled" to true,
            "content_cache_duration_minutes" to (CONTENT_CACHE_DURATION / 60000),
            "search_cache_duration_minutes" to (SEARCH_CACHE_DURATION / 60000),
            "features" to listOf("并发搜索", "结果聚合", "智能缓存", "多站点支持")
        )
    }
}
