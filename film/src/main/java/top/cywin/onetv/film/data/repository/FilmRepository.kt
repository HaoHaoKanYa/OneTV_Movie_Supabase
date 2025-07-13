package top.cywin.onetv.film.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import top.cywin.onetv.film.cache.CacheFactory
import top.cywin.onetv.film.cache.VodContentCache
import top.cywin.onetv.film.concurrent.ConcurrentUtils
import top.cywin.onetv.film.data.datasource.*
import top.cywin.onetv.film.data.models.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 影视仓库
 * 
 * 基于 FongMi/TV 的数据仓库实现
 * 统一管理本地和远程数据源，提供缓存和并发处理
 * 
 * 功能：
 * - VOD 内容管理
 * - 播放历史管理
 * - 收藏管理
 * - 下载管理
 * - 配置管理
 * - 数据同步
 * - 缓存管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class FilmRepository(
    private val context: Context,
    private val localDataSource: LocalDataSourceImpl,
    private val remoteDataSource: RemoteDataSourceImpl
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_REPOSITORY"
        private const val CACHE_EXPIRE_TIME = 30 * 60 * 1000L // 30分钟
    }
    
    // 缓存
    private val vodContentCache = CacheFactory.getVodContentCache(context)
    private val configCache = CacheFactory.getConfigCache(context)
    private val searchCache = CacheFactory.getSearchCache(context)
    
    // 内存缓存
    private val siteConfigsCache = ConcurrentHashMap<String, VodSite>()
    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()

    // 真实数据源管理器
    private val realDataSourceManager = top.cywin.onetv.film.data.datasource.RealDataSourceManager.getInstance()

    /**
     * 🔧 初始化仓库
     */
    fun initialize() {
        try {
            Log.d(TAG, "🔧 初始化 FilmRepository")

            // 初始化真实数据源管理器
            realDataSourceManager.initialize(context)

            Log.d(TAG, "✅ FilmRepository 初始化完成")

        } catch (e: Exception) {
            Log.e(TAG, "❌ FilmRepository 初始化失败", e)
            throw e
        }
    }
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        Log.d(TAG, "🏗️ FilmRepository 初始化完成")
    }
    
    // ==================== VOD 内容管理 ====================
    
    /**
     * 🏠 获取首页内容
     */
    suspend fun getHomeContent(siteKey: String, forceRefresh: Boolean = false): Result<VodHomeResult> {
        return try {
            Log.d(TAG, "🏠 获取首页内容: $siteKey, forceRefresh: $forceRefresh")
            
            // 检查缓存
            if (!forceRefresh) {
                val cachedContent = vodContentCache.getHomeContent(siteKey)
                if (cachedContent != null) {
                    Log.d(TAG, "📦 使用缓存的首页内容: $siteKey")
                    return Result.success(parseHomeContent(cachedContent, siteKey))
                }
            }
            
            // 从远程获取
            val result = remoteDataSource.getHomeContent(siteKey, false)
            if (result.isSuccess) {
                val homeResult = result.getOrNull()!!
                // 缓存结果
                val jsonContent = serializeHomeContent(homeResult)
                vodContentCache.putHomeContent(siteKey, jsonContent)
                
                Log.d(TAG, "✅ 首页内容获取成功: $siteKey")
                Result.success(homeResult)
            } else {
                Log.w(TAG, "⚠️ 首页内容获取失败: $siteKey")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取首页内容异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 📂 获取分类内容
     */
    suspend fun getCategoryContent(
        siteKey: String,
        tid: String,
        page: Int = 1,
        forceRefresh: Boolean = false,
        extend: Map<String, String> = emptyMap()
    ): Result<VodCategoryResult> {
        return try {
            Log.d(TAG, "📂 获取分类内容: $siteKey, tid: $tid, page: $page")
            
            // 检查缓存
            if (!forceRefresh && page == 1) {
                val cachedContent = vodContentCache.getCategoryContent(siteKey, tid, page)
                if (cachedContent != null) {
                    Log.d(TAG, "📦 使用缓存的分类内容: $siteKey")
                    return Result.success(parseCategoryContent(cachedContent, siteKey, tid))
                }
            }
            
            // 从远程获取
            val result = remoteDataSource.getCategoryContent(siteKey, tid, page, false, extend)
            if (result.isSuccess) {
                val categoryResult = result.getOrNull()!!
                // 缓存第一页结果
                if (page == 1) {
                    val jsonContent = serializeCategoryContent(categoryResult)
                    vodContentCache.putCategoryContent(siteKey, tid, page, jsonContent)
                }
                
                Log.d(TAG, "✅ 分类内容获取成功: $siteKey, 数量: ${categoryResult.list.size}")
                Result.success(categoryResult)
            } else {
                Log.w(TAG, "⚠️ 分类内容获取失败: $siteKey")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分类内容异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 📄 获取详情内容
     */
    suspend fun getDetailContent(
        siteKey: String,
        vodId: String,
        forceRefresh: Boolean = false
    ): Result<VodInfo?> {
        return try {
            Log.d(TAG, "📄 获取详情内容: $siteKey, vodId: $vodId")
            
            // 检查缓存
            if (!forceRefresh) {
                val cachedContent = vodContentCache.getDetailContent(siteKey, vodId)
                if (cachedContent != null) {
                    Log.d(TAG, "📦 使用缓存的详情内容: $siteKey")
                    return Result.success(parseDetailContent(cachedContent))
                }
            }
            
            // 从远程获取
            val result = remoteDataSource.getDetailContent(siteKey, listOf(vodId))
            if (result.isSuccess) {
                val vodList = result.getOrNull()!!
                val vodInfo = vodList.firstOrNull()
                
                if (vodInfo != null) {
                    // 缓存结果
                    val jsonContent = serializeDetailContent(vodInfo)
                    vodContentCache.putDetailContent(siteKey, vodId, jsonContent)
                    
                    Log.d(TAG, "✅ 详情内容获取成功: $siteKey")
                    Result.success(vodInfo)
                } else {
                    Log.w(TAG, "⚠️ 详情内容为空: $siteKey")
                    Result.success(null)
                }
            } else {
                Log.w(TAG, "⚠️ 详情内容获取失败: $siteKey")
                Result.failure(result.exceptionOrNull() ?: Exception("详情内容获取失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取详情内容异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 🔍 搜索内容
     */
    suspend fun searchContent(
        keyword: String,
        siteKeys: List<String> = emptyList(),
        page: Int = 1,
        forceRefresh: Boolean = false
    ): Result<Map<String, VodSearchResult>> {
        return try {
            Log.d(TAG, "🔍 搜索内容: $keyword, sites: ${siteKeys.size}, page: $page")
            
            // 检查缓存
            if (!forceRefresh && page == 1) {
                val cachedResults = searchCache.getSearchResult(keyword, siteKeys)
                if (cachedResults != null) {
                    Log.d(TAG, "📦 使用缓存的搜索结果: $keyword")
                    return Result.success(parseSearchResults(cachedResults))
                }
            }
            
            // 确定要搜索的站点
            val targetSites = if (siteKeys.isEmpty()) {
                getSites().getOrNull()?.map { it.key } ?: emptyList()
            } else {
                siteKeys
            }
            
            // 并行搜索多个站点
            val searchResults = ConcurrentUtils.parallelMapNotNull(
                items = targetSites,
                maxConcurrency = 3
            ) { siteKey ->
                try {
                    val result = remoteDataSource.searchContent(siteKey, keyword, page, false)
                    if (result.isSuccess) {
                        siteKey to result.getOrNull()!!
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 站点搜索失败: $siteKey", e)
                    null
                }
            }.toMap()
            
            // 缓存第一页结果
            if (page == 1 && searchResults.isNotEmpty()) {
                val jsonContent = serializeSearchResults(searchResults)
                searchCache.putSearchResult(keyword, targetSites, jsonContent)
            }
            
            Log.d(TAG, "✅ 搜索完成: $keyword, 站点数: ${searchResults.size}")
            Result.success(searchResults)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索内容异常: $keyword", e)
            Result.failure(e)
        }
    }
    
    /**
     * ▶️ 获取播放内容
     */
    suspend fun getPlayContent(
        siteKey: String,
        flag: String,
        id: String,
        vipUrl: List<String> = emptyList()
    ): Result<String> {
        return try {
            Log.d(TAG, "▶️ 获取播放内容: $siteKey, flag: $flag, id: $id")
            
            val result = remoteDataSource.getPlayContent(siteKey, flag, id, vipUrl)
            if (result.isSuccess) {
                Log.d(TAG, "✅ 播放内容获取成功: $siteKey")
            } else {
                Log.w(TAG, "⚠️ 播放内容获取失败: $siteKey")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放内容异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    // ==================== 站点管理 ====================
    
    /**
     * 📋 获取站点列表
     */
    suspend fun getSites(forceRefresh: Boolean = false): Result<List<VodSite>> {
        return try {
            Log.d(TAG, "📋 获取站点列表, forceRefresh: $forceRefresh")
            
            if (!forceRefresh && siteConfigsCache.isNotEmpty()) {
                Log.d(TAG, "📦 使用缓存的站点列表")
                return Result.success(siteConfigsCache.values.toList())
            }
            
            // 优先从本地获取
            val localResult = localDataSource.getSites()
            if (localResult.isSuccess) {
                val localSites = localResult.getOrNull()!!
                if (localSites.isNotEmpty()) {
                    // 更新内存缓存
                    siteConfigsCache.clear()
                    localSites.forEach { site ->
                        siteConfigsCache[site.key] = site
                    }
                    
                    Log.d(TAG, "✅ 从本地获取站点列表: ${localSites.size}")
                    return Result.success(localSites)
                }
            }
            
            // 从远程获取
            val remoteResult = remoteDataSource.getSites()
            if (remoteResult.isSuccess) {
                val remoteSites = remoteResult.getOrNull()!!
                
                // 保存到本地
                remoteSites.forEach { site ->
                    localDataSource.saveSiteConfig(site)
                    siteConfigsCache[site.key] = site
                }
                
                Log.d(TAG, "✅ 从远程获取站点列表: ${remoteSites.size}")
                Result.success(remoteSites)
            } else {
                Log.w(TAG, "⚠️ 获取站点列表失败")
                remoteResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点列表异常", e)
            Result.failure(e)
        }
    }
    
    /**
     * 🔧 获取站点配置
     */
    suspend fun getSiteConfig(siteKey: String): Result<VodSite?> {
        return try {
            // 先从内存缓存获取
            val cachedSite = siteConfigsCache[siteKey]
            if (cachedSite != null) {
                return Result.success(cachedSite)
            }
            
            // 从本地获取
            val localResult = localDataSource.getSiteConfig(siteKey)
            if (localResult.isSuccess) {
                val site = localResult.getOrNull()
                if (site != null) {
                    siteConfigsCache[siteKey] = site
                }
                return localResult
            }
            
            // 从远程获取
            val remoteResult = remoteDataSource.getSiteConfig(siteKey)
            if (remoteResult.isSuccess) {
                val site = remoteResult.getOrNull()
                if (site != null) {
                    // 保存到本地和缓存
                    localDataSource.saveSiteConfig(site)
                    siteConfigsCache[siteKey] = site
                }
            }
            
            remoteResult
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点配置异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 💾 保存站点配置
     */
    suspend fun saveSiteConfig(site: VodSite): Result<Boolean> {
        return try {
            val result = localDataSource.saveSiteConfig(site)
            if (result.isSuccess) {
                // 更新内存缓存
                siteConfigsCache[site.key] = site
                Log.d(TAG, "💾 站点配置保存成功: ${site.key}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存站点配置异常: ${site.key}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 🗑️ 删除站点配置
     */
    suspend fun deleteSiteConfig(siteKey: String): Result<Boolean> {
        return try {
            val result = localDataSource.deleteSiteConfig(siteKey)
            if (result.isSuccess) {
                // 从内存缓存移除
                siteConfigsCache.remove(siteKey)
                Log.d(TAG, "🗑️ 站点配置删除成功: $siteKey")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除站点配置异常: $siteKey", e)
            Result.failure(e)
        }
    }
    
    // ==================== 播放历史管理 ====================
    
    /**
     * 📋 获取播放历史
     */
    suspend fun getPlayHistories(
        userId: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PlayHistory>> {
        return localDataSource.getPlayHistories(userId, limit, offset)
    }
    
    /**
     * 💾 保存播放历史
     */
    suspend fun savePlayHistory(history: PlayHistory): Result<Boolean> {
        return localDataSource.savePlayHistory(history)
    }
    
    /**
     * 🗑️ 删除播放历史
     */
    suspend fun deletePlayHistory(id: String): Result<Boolean> {
        return localDataSource.deletePlayHistory(id)
    }
    
    /**
     * 🧹 清空播放历史
     */
    suspend fun clearPlayHistories(userId: String = ""): Result<Boolean> {
        return localDataSource.clearPlayHistories(userId)
    }
    
    // ==================== 收藏管理 ====================
    
    /**
     * 📋 获取收藏列表
     */
    suspend fun getFavorites(
        userId: String = "",
        category: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<FavoriteInfo>> {
        return localDataSource.getFavorites(userId, category, limit, offset)
    }
    
    /**
     * 💾 添加收藏
     */
    suspend fun addFavorite(favorite: FavoriteInfo): Result<Boolean> {
        return localDataSource.addFavorite(favorite)
    }
    
    /**
     * 🗑️ 删除收藏
     */
    suspend fun deleteFavorite(id: String): Result<Boolean> {
        return localDataSource.deleteFavorite(id)
    }
    
    /**
     * 🧹 清空收藏
     */
    suspend fun clearFavorites(userId: String = "", category: String = ""): Result<Boolean> {
        return localDataSource.clearFavorites(userId, category)
    }
    
    // ==================== 配置管理 ====================
    
    /**
     * 🔧 获取应用配置
     */
    suspend fun getAppConfig(): Result<AppConfig> {
        return localDataSource.getAppConfig()
    }
    
    /**
     * 💾 保存应用配置
     */
    suspend fun saveAppConfig(config: AppConfig): Result<Boolean> {
        return localDataSource.saveAppConfig(config)
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 🧹 清理缓存
     */
    suspend fun clearCache() {
        try {
            vodContentCache.clear()
            configCache.clear()
            searchCache.clear()
            siteConfigsCache.clear()
            lastUpdateTimes.clear()
            
            Log.d(TAG, "🧹 缓存清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存清理失败", e)
        }
    }
    
    /**
     * 📊 获取仓库统计
     */
    fun getRepositoryStats(): Map<String, Any> {
        return mapOf(
            "sites_cache_size" to siteConfigsCache.size,
            "last_update_times" to lastUpdateTimes.size,
            "vod_cache_stats" to vodContentCache.getStats(),
            "config_cache_stats" to configCache.getStats(),
            "search_cache_stats" to searchCache.getStats()
        )
    }
    
    // ==================== 辅助方法 ====================
    
    private fun parseHomeContent(jsonContent: String, siteKey: String): VodHomeResult {
        // 解析首页内容的实现
        return VodHomeResult(siteKey = siteKey)
    }
    
    private fun serializeHomeContent(homeResult: VodHomeResult): String {
        // 序列化首页内容的实现
        return "{}"
    }
    
    private fun parseCategoryContent(jsonContent: String, siteKey: String, tid: String): VodCategoryResult {
        // 解析分类内容的实现
        return VodCategoryResult(siteKey = siteKey, typeId = tid)
    }
    
    private fun serializeCategoryContent(categoryResult: VodCategoryResult): String {
        // 序列化分类内容的实现
        return "{}"
    }
    
    private fun parseDetailContent(jsonContent: String): VodInfo {
        // 解析详情内容的实现
        return VodInfo("", "")
    }
    
    private fun serializeDetailContent(vodInfo: VodInfo): String {
        // 序列化详情内容的实现
        return "{}"
    }
    
    private fun parseSearchResults(jsonContent: String): Map<String, VodSearchResult> {
        // 解析搜索结果的实现
        return emptyMap()
    }
    
    private fun serializeSearchResults(searchResults: Map<String, VodSearchResult>): String {
        // 序列化搜索结果的实现
        return "{}"
    }
}
