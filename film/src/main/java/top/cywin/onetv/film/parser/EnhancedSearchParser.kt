package top.cywin.onetv.film.parser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.delay
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.concurrent.ConcurrentSearcher
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.data.models.VodResponse
import top.cywin.onetv.film.data.models.VodItem
import top.cywin.onetv.film.data.models.SearchResult
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.cache.CacheManager

/**
 * 增强搜索解析器
 * 
 * 基于 FongMi/TV 的搜索解析系统
 * 负责智能搜索和搜索结果优化
 * 
 * 核心功能：
 * - 智能搜索策略
 * - 搜索结果排序和过滤
 * - 搜索建议和联想
 * - 搜索历史管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EnhancedSearchParser(
    private val context: Context,
    private val spiderManager: SpiderManager,
    private val concurrentSearcher: ConcurrentSearcher,
    private val cacheManager: CacheManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_ENHANCED_SEARCH_PARSER"
        private const val SEARCH_CACHE_DURATION = 10 * 60 * 1000L // 10分钟
        private const val SUGGESTION_CACHE_DURATION = 60 * 60 * 1000L // 1小时
        private const val HISTORY_CACHE_DURATION = 7 * 24 * 60 * 60 * 1000L // 7天
        private const val MAX_SEARCH_HISTORY = 50
        private const val MAX_CONCURRENT_SEARCHES = 8
    }
    
    /**
     * 智能搜索
     * 
     * 这是 FongMi/TV 搜索的核心入口，提供智能搜索策略
     */
    suspend fun smartSearch(
        keyword: String,
        sites: List<VodSite> = emptyList(),
        searchType: SearchType = SearchType.COMPREHENSIVE
    ): Result<SearchResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 开始智能搜索: $keyword, 类型: $searchType")
            
            // 1. 预处理搜索关键词
            val processedKeyword = preprocessKeyword(keyword)
            
            // 2. 检查搜索缓存
            val cacheKey = "smart_search:${StringUtils.md5(processedKeyword)}:${searchType.name}"
            val cachedResult = getCachedSearchResult(cacheKey)
            if (cachedResult != null) {
                Log.d(TAG, "✅ 使用缓存搜索结果")
                return@withContext Result.success(cachedResult)
            }
            
            // 3. 执行智能搜索策略
            val searchResult = when (searchType) {
                SearchType.QUICK -> executeQuickSearch(processedKeyword, sites)
                SearchType.COMPREHENSIVE -> executeComprehensiveSearch(processedKeyword, sites)
                SearchType.PRECISE -> executePreciseSearch(processedKeyword, sites)
                SearchType.FUZZY -> executeFuzzySearch(processedKeyword, sites)
            }
            
            // 4. 优化搜索结果
            val optimizedResult = optimizeSearchResults(searchResult, processedKeyword)
            
            // 5. 缓存搜索结果
            cacheSearchResult(cacheKey, optimizedResult)
            
            // 6. 保存搜索历史
            saveSearchHistory(processedKeyword)
            
            Log.d(TAG, "✅ 智能搜索完成，结果数量: ${optimizedResult.totalResults}")
            Result.success(optimizedResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 智能搜索失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    /**
     * 快速搜索
     * 
     * 优先搜索响应快的站点，适合实时搜索
     */
    private suspend fun executeQuickSearch(keyword: String, sites: List<VodSite>): SearchResult {
        return supervisorScope {
            val targetSites = if (sites.isEmpty()) {
                spiderManager.getAvailableSites()
                    .filter { it.searchable == 1 && it.quickSearch == 1 }
                    .sortedBy { it.timeout }
                    .take(5) // 只搜索前5个最快的站点
            } else {
                sites.filter { it.quickSearch == 1 }
            }
            
            Log.d(TAG, "⚡ 快速搜索: ${targetSites.size} 个站点")
            
            val searchTasks = targetSites.map { site ->
                async {
                    try {
                        val spider = spiderManager.getSpider(site.key)
                        if (spider != null) {
                            val resultJson = spider.searchContent(keyword, true) // 快速搜索
                            val vodResponse = JsonUtils.parseVodResponse(resultJson)
                            SiteSearchResult(site, vodResponse, true)
                        } else {
                            SiteSearchResult(site, VodResponse(list = emptyList()), false)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ 快速搜索失败: ${site.key}", e)
                        SiteSearchResult(site, VodResponse(list = emptyList()), false)
                    }
                }
            }
            
            val results = searchTasks.awaitAll()
            aggregateSearchResults(results, keyword, SearchType.QUICK)
        }
    }
    
    /**
     * 综合搜索
     * 
     * 搜索所有可用站点，提供最全面的结果
     */
    private suspend fun executeComprehensiveSearch(keyword: String, sites: List<VodSite>): SearchResult {
        return supervisorScope {
            val targetSites = if (sites.isEmpty()) {
                spiderManager.getAvailableSites()
                    .filter { it.searchable == 1 }
            } else {
                sites.filter { it.searchable == 1 }
            }
            
            Log.d(TAG, "🌐 综合搜索: ${targetSites.size} 个站点")
            
            // 分批搜索，避免过多并发
            val batchSize = MAX_CONCURRENT_SEARCHES
            val results = mutableListOf<SiteSearchResult>()
            
            targetSites.chunked(batchSize).forEach { batch ->
                val batchTasks = batch.map { site ->
                    async {
                        try {
                            val spider = spiderManager.getSpider(site.key)
                            if (spider != null) {
                                val resultJson = spider.searchContent(keyword, false)
                                val vodResponse = JsonUtils.parseVodResponse(resultJson)
                                SiteSearchResult(site, vodResponse, true)
                            } else {
                                SiteSearchResult(site, VodResponse(list = emptyList()), false)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ 综合搜索失败: ${site.key}", e)
                            SiteSearchResult(site, VodResponse(list = emptyList()), false)
                        }
                    }
                }
                
                results.addAll(batchTasks.awaitAll())
                
                // 批次间稍作延迟，避免过载
                if (targetSites.size > batchSize) {
                    delay(100)
                }
            }
            
            aggregateSearchResults(results, keyword, SearchType.COMPREHENSIVE)
        }
    }
    
    /**
     * 精确搜索
     * 
     * 使用精确匹配策略，适合已知内容名称的搜索
     */
    private suspend fun executePreciseSearch(keyword: String, sites: List<VodSite>): SearchResult {
        // 精确搜索使用更严格的匹配条件
        val result = executeComprehensiveSearch(keyword, sites)
        
        // 过滤精确匹配的结果
        val preciseResults = result.results.map { siteResult ->
            val filteredItems = siteResult.vodResponse.list.filter { item ->
                item.vod_name.contains(keyword, ignoreCase = true) ||
                item.vod_name.replace(" ", "").contains(keyword.replace(" ", ""), ignoreCase = true)
            }
            siteResult.copy(vodResponse = siteResult.vodResponse.copy(list = filteredItems))
        }
        
        return result.copy(
            results = preciseResults,
            searchType = SearchType.PRECISE,
            totalResults = preciseResults.sumOf { it.vodResponse.list.size }
        )
    }
    
    /**
     * 模糊搜索
     * 
     * 使用模糊匹配策略，适合不确定内容名称的搜索
     */
    private suspend fun executeFuzzySearch(keyword: String, sites: List<VodSite>): SearchResult {
        // 生成模糊搜索关键词
        val fuzzyKeywords = generateFuzzyKeywords(keyword)
        
        val allResults = mutableListOf<SiteSearchResult>()
        
        // 对每个模糊关键词进行搜索
        fuzzyKeywords.forEach { fuzzyKeyword ->
            try {
                val result = executeQuickSearch(fuzzyKeyword, sites)
                allResults.addAll(result.results)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 模糊搜索失败: $fuzzyKeyword", e)
            }
        }
        
        // 合并和去重结果
        val mergedResults = mergeSiteResults(allResults)
        
        return SearchResult(
            keyword = keyword,
            results = mergedResults,
            totalResults = mergedResults.sumOf { it.vodResponse.list.size },
            searchType = SearchType.FUZZY,
            searchTime = System.currentTimeMillis(),
            suggestions = emptyList()
        )
    }
    
    /**
     * 获取搜索建议
     */
    suspend fun getSearchSuggestions(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💡 获取搜索建议: $keyword")
            
            // 1. 检查建议缓存
            val cacheKey = "suggestions:${StringUtils.md5(keyword)}"
            val cachedSuggestions = getCachedSuggestions(cacheKey)
            if (cachedSuggestions != null) {
                return@withContext Result.success(cachedSuggestions)
            }
            
            // 2. 生成搜索建议
            val suggestions = mutableSetOf<String>()
            
            // 从搜索历史中获取建议
            val historyKeywords = getSearchHistory()
            suggestions.addAll(
                historyKeywords.filter { 
                    it.contains(keyword, ignoreCase = true) 
                }.take(5)
            )
            
            // 生成相关关键词
            suggestions.addAll(generateRelatedKeywords(keyword))
            
            val finalSuggestions = suggestions.take(10).toList()
            
            // 3. 缓存建议
            cacheSuggestions(cacheKey, finalSuggestions)
            
            Log.d(TAG, "✅ 搜索建议生成完成: ${finalSuggestions.size} 个")
            Result.success(finalSuggestions)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取搜索建议失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    /**
     * 预处理搜索关键词
     */
    private fun preprocessKeyword(keyword: String): String {
        return keyword.trim()
            .replace(Regex("\\s+"), " ") // 合并多个空格
            .replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]"), "") // 移除特殊字符，保留中英文数字空格
    }
    
    /**
     * 生成模糊搜索关键词
     */
    private fun generateFuzzyKeywords(keyword: String): List<String> {
        val fuzzyKeywords = mutableSetOf<String>()
        
        // 原始关键词
        fuzzyKeywords.add(keyword)
        
        // 移除空格的版本
        fuzzyKeywords.add(keyword.replace(" ", ""))
        
        // 分词搜索
        if (keyword.contains(" ")) {
            keyword.split(" ").forEach { word ->
                if (word.length >= 2) {
                    fuzzyKeywords.add(word)
                }
            }
        }
        
        // 中文分字搜索（如果关键词较短）
        if (keyword.length in 2..4 && keyword.matches(Regex("[\\u4e00-\\u9fa5]+"))) {
            for (i in 0 until keyword.length - 1) {
                fuzzyKeywords.add(keyword.substring(i, i + 2))
            }
        }
        
        return fuzzyKeywords.toList()
    }
    
    /**
     * 生成相关关键词
     */
    private fun generateRelatedKeywords(keyword: String): List<String> {
        val related = mutableListOf<String>()
        
        // 添加常见后缀
        val suffixes = listOf("电影", "电视剧", "综艺", "动漫", "纪录片")
        suffixes.forEach { suffix ->
            if (!keyword.contains(suffix)) {
                related.add("$keyword$suffix")
            }
        }
        
        // 添加年份相关
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        for (year in (currentYear - 2)..currentYear) {
            related.add("$keyword $year")
        }
        
        return related.take(5)
    }
    
    /**
     * 聚合搜索结果
     */
    private fun aggregateSearchResults(
        results: List<SiteSearchResult>,
        keyword: String,
        searchType: SearchType
    ): SearchResult {
        val totalResults = results.sumOf { it.vodResponse.list.size }
        
        return SearchResult(
            keyword = keyword,
            results = results,
            totalResults = totalResults,
            searchType = searchType,
            searchTime = System.currentTimeMillis(),
            suggestions = emptyList()
        )
    }
    
    /**
     * 优化搜索结果
     */
    private fun optimizeSearchResults(searchResult: SearchResult, keyword: String): SearchResult {
        val optimizedResults = searchResult.results.map { siteResult ->
            val sortedItems = siteResult.vodResponse.list
                .sortedWith(compareByDescending<VodItem> { item ->
                    // 精确匹配优先
                    if (item.vod_name.equals(keyword, ignoreCase = true)) 3
                    else if (item.vod_name.contains(keyword, ignoreCase = true)) 2
                    else if (item.vod_name.replace(" ", "").contains(keyword.replace(" ", ""), ignoreCase = true)) 1
                    else 0
                }.thenByDescending { it.vod_time })
            
            siteResult.copy(vodResponse = siteResult.vodResponse.copy(list = sortedItems))
        }
        
        return searchResult.copy(results = optimizedResults)
    }
    
    /**
     * 合并站点结果
     */
    private fun mergeSiteResults(results: List<SiteSearchResult>): List<SiteSearchResult> {
        return results.groupBy { it.site.key }
            .map { (_, siteResults) ->
                val firstResult = siteResults.first()
                val allItems = siteResults.flatMap { it.vodResponse.list }
                val uniqueItems = allItems.distinctBy { "${it.vod_name}_${it.vod_id}" }
                
                firstResult.copy(
                    vodResponse = firstResult.vodResponse.copy(list = uniqueItems)
                )
            }
    }
    
    /**
     * 保存搜索历史
     */
    private suspend fun saveSearchHistory(keyword: String) {
        try {
            val history = getSearchHistory().toMutableList()
            history.remove(keyword) // 移除重复项
            history.add(0, keyword) // 添加到开头
            
            // 限制历史记录数量
            if (history.size > MAX_SEARCH_HISTORY) {
                history.subList(MAX_SEARCH_HISTORY, history.size).clear()
            }
            
            cacheManager.put("search_history", history, HISTORY_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 保存搜索历史失败", e)
        }
    }
    
    /**
     * 获取搜索历史
     */
    private suspend fun getSearchHistory(): List<String> {
        return try {
            cacheManager.get("search_history", List::class.java) as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 缓存相关方法
     */
    private suspend fun getCachedSearchResult(cacheKey: String): SearchResult? {
        return try {
            cacheManager.get(cacheKey, SearchResult::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun cacheSearchResult(cacheKey: String, result: SearchResult) {
        try {
            cacheManager.put(cacheKey, result, SEARCH_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存搜索结果失败", e)
        }
    }
    
    private suspend fun getCachedSuggestions(cacheKey: String): List<String>? {
        return try {
            cacheManager.get(cacheKey, List::class.java) as? List<String>
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun cacheSuggestions(cacheKey: String, suggestions: List<String>) {
        try {
            cacheManager.put(cacheKey, suggestions, SUGGESTION_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存搜索建议失败", e)
        }
    }
    
    /**
     * 清除搜索缓存
     */
    suspend fun clearSearchCache() {
        try {
            cacheManager.clearByPrefix("smart_search:")
            cacheManager.clearByPrefix("suggestions:")
            Log.d(TAG, "✅ 搜索缓存已清除")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 清除搜索缓存失败", e)
        }
    }
    
    /**
     * 获取搜索统计信息
     */
    fun getSearchStats(): Map<String, Any> {
        return mapOf(
            "parser_type" to "EnhancedSearchParser",
            "cache_enabled" to true,
            "search_cache_duration_minutes" to (SEARCH_CACHE_DURATION / 60000),
            "max_concurrent_searches" to MAX_CONCURRENT_SEARCHES,
            "max_search_history" to MAX_SEARCH_HISTORY,
            "search_types" to SearchType.values().map { it.name },
            "features" to listOf("智能搜索", "搜索建议", "搜索历史", "模糊匹配", "结果优化")
        )
    }
}

/**
 * 搜索类型枚举
 */
enum class SearchType {
    QUICK,          // 快速搜索
    COMPREHENSIVE,  // 综合搜索
    PRECISE,        // 精确搜索
    FUZZY          // 模糊搜索
}

/**
 * 站点搜索结果
 */
data class SiteSearchResult(
    val site: VodSite,
    val vodResponse: VodResponse,
    val success: Boolean
)
