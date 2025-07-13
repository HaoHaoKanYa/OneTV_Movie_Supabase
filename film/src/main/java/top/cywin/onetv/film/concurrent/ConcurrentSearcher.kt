package top.cywin.onetv.film.concurrent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import top.cywin.onetv.film.spider.base.Spider
import top.cywin.onetv.film.spider.base.SpiderFactory
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.cache.ContentCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 并发搜索器
 * 基于 FongMi/TV 标准实现
 * 
 * 提供高性能的多站点并发搜索功能
 * 
 * 功能：
 * - 多站点并发搜索
 * - 搜索结果聚合
 * - 智能超时控制
 * - 错误处理和重试
 * - 搜索结果缓存
 * - 实时进度反馈
 * - 搜索优先级控制
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ConcurrentSearcher(
    private val context: Context,
    private val maxConcurrency: Int = 10,
    private val searchTimeout: Long = 15000L,
    private val retryCount: Int = 2
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_CONCURRENT_SEARCHER"
    }
    
    // 协程作用域
    private val scope = CoroutineScope(
        Dispatchers.IO + 
        SupervisorJob() + 
        CoroutineName("ConcurrentSearcher")
    )
    
    // 搜索统计
    private val totalSearchCount = AtomicLong(0)
    private val successSearchCount = AtomicLong(0)
    private val failedSearchCount = AtomicLong(0)
    private val activeSearchCount = AtomicInteger(0)
    
    // JSON 解析器
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // 搜索结果缓存
    private val searchResultCache = ConcurrentHashMap<String, SearchResult>()
    
    /**
     * 🔍 并发搜索多个站点
     * 
     * @param keyword 搜索关键词
     * @param sites 站点列表
     * @param quick 是否快速搜索
     * @param progressCallback 进度回调
     * @return 搜索结果流
     */
    fun searchConcurrently(
        keyword: String,
        sites: List<VodSite>,
        quick: Boolean = false,
        progressCallback: ((SearchProgress) -> Unit)? = null
    ): Flow<SearchResult> = flow {
        
        Log.d(TAG, "🔍 开始并发搜索: keyword=$keyword, sites=${sites.size}")
        
        val searchId = generateSearchId(keyword, sites)
        val startTime = System.currentTimeMillis()
        
        // 检查缓存
        val cachedResult = getCachedSearchResult(keyword, sites)
        if (cachedResult != null) {
            Log.d(TAG, "📦 搜索结果缓存命中: $keyword")
            emit(cachedResult)
            return@flow
        }
        
        // 创建搜索进度
        val progress = SearchProgress(
            searchId = searchId,
            keyword = keyword,
            totalSites = sites.size,
            completedSites = 0,
            successfulSites = 0,
            failedSites = 0,
            startTime = startTime
        )
        
        progressCallback?.invoke(progress)
        
        try {
            // 并发搜索
            sites.asFlow()
                .flowOn(Dispatchers.IO)
                .map { site ->
                    async {
                        searchSingleSite(site, keyword, quick, searchId)
                    }
                }
                .buffer(maxConcurrency)
                .collect { deferred ->
                    try {
                        val result = withTimeoutOrNull(searchTimeout) {
                            deferred.await()
                        }
                        
                        if (result != null) {
                            // 更新进度
                            val updatedProgress = progress.copy(
                                completedSites = progress.completedSites + 1,
                                successfulSites = if (result.success) progress.successfulSites + 1 else progress.successfulSites,
                                failedSites = if (!result.success) progress.failedSites + 1 else progress.failedSites,
                                currentTime = System.currentTimeMillis()
                            )
                            progressCallback?.invoke(updatedProgress)
                            
                            // 发送结果
                            emit(result)
                            
                            // 缓存成功的结果
                            if (result.success && result.items.isNotEmpty()) {
                                cacheSearchResult(keyword, result)
                            }
                        } else {
                            Log.w(TAG, "⏰ 搜索超时: ${deferred}")
                            failedSearchCount.incrementAndGet()
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 搜索异常", e)
                        failedSearchCount.incrementAndGet()
                        
                        emit(SearchResult(
                            siteKey = "unknown",
                            siteName = "未知站点",
                            keyword = keyword,
                            success = false,
                            error = e.message ?: "搜索失败",
                            items = emptyList(),
                            searchTime = System.currentTimeMillis() - startTime
                        ))
                    }
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "❌ 并发搜索失败: $keyword", e)
            throw e
        } finally {
            activeSearchCount.decrementAndGet()
            Log.d(TAG, "✅ 并发搜索完成: $keyword, 耗时: ${System.currentTimeMillis() - startTime}ms")
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 🔍 搜索单个站点
     */
    private suspend fun searchSingleSite(
        site: VodSite,
        keyword: String,
        quick: Boolean,
        searchId: String
    ): SearchResult = withContext(Dispatchers.IO) {
        
        val startTime = System.currentTimeMillis()
        activeSearchCount.incrementAndGet()
        totalSearchCount.incrementAndGet()
        
        try {
            Log.d(TAG, "🔍 搜索站点: ${site.name} - $keyword")
            
            // 创建 Spider
            val spider = SpiderFactory.createSpider(
                type = site.type,
                api = site.api,
                context = context,
                extend = site.ext ?: "",
                useCache = true
            )
            
            if (spider == null) {
                Log.w(TAG, "⚠️ 无法创建 Spider: ${site.name}")
                return@withContext SearchResult(
                    siteKey = site.key,
                    siteName = site.name,
                    keyword = keyword,
                    success = false,
                    error = "无法创建解析器",
                    items = emptyList(),
                    searchTime = System.currentTimeMillis() - startTime
                )
            }
            
            // 执行搜索
            val searchResult = withRetry(retryCount) {
                spider.searchContent(keyword, quick)
            }
            
            // 解析搜索结果
            val items = parseSearchResult(searchResult)
            
            successSearchCount.incrementAndGet()
            
            SearchResult(
                siteKey = site.key,
                siteName = site.name,
                keyword = keyword,
                success = true,
                error = null,
                items = items,
                searchTime = System.currentTimeMillis() - startTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 站点搜索失败: ${site.name} - $keyword", e)
            failedSearchCount.incrementAndGet()
            
            SearchResult(
                siteKey = site.key,
                siteName = site.name,
                keyword = keyword,
                success = false,
                error = e.message ?: "搜索失败",
                items = emptyList(),
                searchTime = System.currentTimeMillis() - startTime
            )
        } finally {
            activeSearchCount.decrementAndGet()
        }
    }
    
    /**
     * 🔄 重试执行
     */
    private suspend fun <T> withRetry(
        times: Int,
        block: suspend () -> T
    ): T {
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "🔄 重试 ${attempt + 1}/$times", e)
                delay(1000L * (attempt + 1)) // 递增延迟
            }
        }
        return block() // 最后一次尝试
    }
    
    /**
     * 📋 解析搜索结果
     */
    private fun parseSearchResult(jsonResult: String): List<SearchItem> {
        return try {
            val jsonObject = json.parseToJsonElement(jsonResult).jsonObject
            val list = jsonObject["list"]?.jsonArray ?: return emptyList()
            
            list.mapNotNull { element ->
                try {
                    val item = element.jsonObject
                    SearchItem(
                        vodId = item["vod_id"]?.jsonPrimitive?.content ?: "",
                        vodName = item["vod_name"]?.jsonPrimitive?.content ?: "",
                        vodPic = item["vod_pic"]?.jsonPrimitive?.content ?: "",
                        vodRemarks = item["vod_remarks"]?.jsonPrimitive?.content ?: "",
                        vodYear = item["vod_year"]?.jsonPrimitive?.content ?: "",
                        vodArea = item["vod_area"]?.jsonPrimitive?.content ?: "",
                        vodDirector = item["vod_director"]?.jsonPrimitive?.content ?: "",
                        vodActor = item["vod_actor"]?.jsonPrimitive?.content ?: "",
                        vodContent = item["vod_content"]?.jsonPrimitive?.content ?: "",
                        typeId = item["type_id"]?.jsonPrimitive?.content ?: "",
                        typeName = item["type_name"]?.jsonPrimitive?.content ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 解析搜索项失败", e)
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析搜索结果失败", e)
            emptyList()
        }
    }
    
    /**
     * 🔧 生成搜索ID
     */
    private fun generateSearchId(keyword: String, sites: List<VodSite>): String {
        return "${keyword}_${sites.hashCode()}_${System.currentTimeMillis()}"
    }
    
    /**
     * 💾 缓存搜索结果
     */
    private suspend fun cacheSearchResult(keyword: String, result: SearchResult) {
        try {
            val cacheKey = "search_${keyword}_${result.siteKey}"
            ContentCache.putSearchContent(result.siteKey, keyword, json.encodeToString(result))
            searchResultCache[cacheKey] = result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存搜索结果失败", e)
        }
    }
    
    /**
     * 📦 获取缓存的搜索结果
     */
    private suspend fun getCachedSearchResult(keyword: String, sites: List<VodSite>): SearchResult? {
        return try {
            // 简单实现：返回第一个有缓存的站点结果
            for (site in sites) {
                val cacheKey = "search_${keyword}_${site.key}"
                val cachedResult = searchResultCache[cacheKey]
                if (cachedResult != null) {
                    return cachedResult
                }
                
                val cachedContent = ContentCache.getSearchContent(site.key, keyword)
                if (cachedContent != null) {
                    val result = json.decodeFromString<SearchResult>(cachedContent)
                    searchResultCache[cacheKey] = result
                    return result
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取缓存搜索结果失败", e)
            null
        }
    }
    
    /**
     * 📊 获取搜索统计
     */
    fun getSearchStats(): SearchStats {
        return SearchStats(
            totalSearches = totalSearchCount.get(),
            successfulSearches = successSearchCount.get(),
            failedSearches = failedSearchCount.get(),
            activeSearches = activeSearchCount.get(),
            cachedResults = searchResultCache.size,
            successRate = if (totalSearchCount.get() > 0) {
                (successSearchCount.get().toDouble() / totalSearchCount.get() * 100)
            } else 0.0
        )
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        searchResultCache.clear()
        Log.d(TAG, "🧹 搜索缓存已清理")
    }
    
    /**
     * 🛑 停止所有搜索
     */
    fun shutdown() {
        scope.cancel()
        Log.d(TAG, "🛑 并发搜索器已关闭")
    }
    
    /**
     * 搜索结果数据类
     */
    @kotlinx.serialization.Serializable
    data class SearchResult(
        val siteKey: String,
        val siteName: String,
        val keyword: String,
        val success: Boolean,
        val error: String? = null,
        val items: List<SearchItem>,
        val searchTime: Long
    )
    
    /**
     * 搜索项数据类
     */
    @kotlinx.serialization.Serializable
    data class SearchItem(
        val vodId: String,
        val vodName: String,
        val vodPic: String,
        val vodRemarks: String,
        val vodYear: String,
        val vodArea: String,
        val vodDirector: String,
        val vodActor: String,
        val vodContent: String,
        val typeId: String,
        val typeName: String
    )
    
    /**
     * 搜索进度数据类
     */
    data class SearchProgress(
        val searchId: String,
        val keyword: String,
        val totalSites: Int,
        val completedSites: Int,
        val successfulSites: Int,
        val failedSites: Int,
        val startTime: Long,
        val currentTime: Long = System.currentTimeMillis()
    ) {
        val progressPercentage: Int
            get() = if (totalSites > 0) (completedSites * 100 / totalSites) else 0
            
        val elapsedTime: Long
            get() = currentTime - startTime
    }
    
    /**
     * 搜索统计数据类
     */
    data class SearchStats(
        val totalSearches: Long,
        val successfulSearches: Long,
        val failedSearches: Long,
        val activeSearches: Int,
        val cachedResults: Int,
        val successRate: Double
    )
}
