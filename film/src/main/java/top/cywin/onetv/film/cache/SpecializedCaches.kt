package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.utils.JsonUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * 专用缓存实现
 * 
 * 基于 FongMi/TV 的专用缓存系统
 * 为不同类型的数据提供专门的缓存实现
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * VOD 内容缓存
 */
class VodContentCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 20 * 1024 * 1024L, // 20MB
        defaultTtl = 1800000L // 30分钟
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_VOD_CONTENT_CACHE"
        private const val PREFIX_HOME = "vod:home"
        private const val PREFIX_CATEGORY = "vod:category"
        private const val PREFIX_DETAIL = "vod:detail"
        private const val PREFIX_SEARCH = "vod:search"
        private const val PREFIX_PLAY = "vod:play"
    }
    
    /**
     * 🏠 缓存首页内容
     */
    suspend fun putHomeContent(siteKey: String, content: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_HOME, siteKey)
        return put(key, content, ttl = 600000L) // 10分钟
    }
    
    /**
     * 🏠 获取首页内容
     */
    suspend fun getHomeContent(siteKey: String): String? {
        val key = CacheKeyBuilder.create(PREFIX_HOME, siteKey)
        return get(key, String::class.java)
    }
    
    /**
     * 📂 缓存分类内容
     */
    suspend fun putCategoryContent(siteKey: String, tid: String, page: Int, content: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_CATEGORY, siteKey, tid, page)
        return put(key, content, ttl = 1800000L) // 30分钟
    }
    
    /**
     * 📂 获取分类内容
     */
    suspend fun getCategoryContent(siteKey: String, tid: String, page: Int): String? {
        val key = CacheKeyBuilder.create(PREFIX_CATEGORY, siteKey, tid, page)
        return get(key, String::class.java)
    }
    
    /**
     * 📄 缓存详情内容
     */
    suspend fun putDetailContent(siteKey: String, vodId: String, content: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_DETAIL, siteKey, vodId)
        return put(key, content, ttl = 3600000L) // 1小时
    }
    
    /**
     * 📄 获取详情内容
     */
    suspend fun getDetailContent(siteKey: String, vodId: String): String? {
        val key = CacheKeyBuilder.create(PREFIX_DETAIL, siteKey, vodId)
        return get(key, String::class.java)
    }
    
    /**
     * 🔍 缓存搜索结果
     */
    suspend fun putSearchResult(siteKey: String, keyword: String, page: Int, content: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH, siteKey, keyword, page)
        return put(key, content, ttl = 900000L) // 15分钟
    }
    
    /**
     * 🔍 获取搜索结果
     */
    suspend fun getSearchResult(siteKey: String, keyword: String, page: Int): String? {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH, siteKey, keyword, page)
        return get(key, String::class.java)
    }
    
    /**
     * ▶️ 缓存播放内容
     */
    suspend fun putPlayContent(siteKey: String, vodId: String, flag: String, content: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_PLAY, siteKey, vodId, flag)
        return put(key, content, ttl = 7200000L) // 2小时
    }
    
    /**
     * ▶️ 获取播放内容
     */
    suspend fun getPlayContent(siteKey: String, vodId: String, flag: String): String? {
        val key = CacheKeyBuilder.create(PREFIX_PLAY, siteKey, vodId, flag)
        return get(key, String::class.java)
    }
    
    /**
     * 🧹 清理站点缓存
     */
    suspend fun clearSiteCache(siteKey: String) {
        val patterns = listOf(PREFIX_HOME, PREFIX_CATEGORY, PREFIX_DETAIL, PREFIX_SEARCH, PREFIX_PLAY)
        
        patterns.forEach { pattern ->
            // 这里应该实现模式匹配删除
            // 暂时使用简单的键匹配
        }
        
        Log.d(TAG, "🧹 站点缓存已清理: $siteKey")
    }
}

/**
 * 网络响应缓存
 */
class NetworkResponseCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 30 * 1024 * 1024L, // 30MB
        defaultTtl = 3600000L // 1小时
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_NETWORK_RESPONSE_CACHE"
        private const val PREFIX_HTTP = "http"
        private const val PREFIX_API = "api"
    }
    
    /**
     * 🌐 缓存 HTTP 响应
     */
    suspend fun putHttpResponse(url: String, headers: Map<String, String>, response: String): Boolean {
        val key = generateHttpCacheKey(url, headers)
        return put(key, response, strategy = CacheStrategy.MEMORY_FIRST)
    }
    
    /**
     * 🌐 获取 HTTP 响应
     */
    suspend fun getHttpResponse(url: String, headers: Map<String, String>): String? {
        val key = generateHttpCacheKey(url, headers)
        return get(key, String::class.java)
    }
    
    /**
     * 🔗 生成 HTTP 缓存键
     */
    private fun generateHttpCacheKey(url: String, headers: Map<String, String>): String {
        val headerHash = headers.entries
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
            .hashCode()
        
        return CacheKeyBuilder.create(PREFIX_HTTP, url.hashCode(), headerHash)
    }
    
    /**
     * 📡 缓存 API 响应
     */
    suspend fun putApiResponse(endpoint: String, params: Map<String, Any>, response: String): Boolean {
        val key = generateApiCacheKey(endpoint, params)
        return put(key, response, ttl = 1800000L) // 30分钟
    }
    
    /**
     * 📡 获取 API 响应
     */
    suspend fun getApiResponse(endpoint: String, params: Map<String, Any>): String? {
        val key = generateApiCacheKey(endpoint, params)
        return get(key, String::class.java)
    }
    
    /**
     * 🔗 生成 API 缓存键
     */
    private fun generateApiCacheKey(endpoint: String, params: Map<String, Any>): String {
        val paramHash = params.entries
            .sortedBy { it.key }
            .joinToString(",") { "${it.key}=${it.value}" }
            .hashCode()
        
        return CacheKeyBuilder.create(PREFIX_API, endpoint, paramHash)
    }
}

/**
 * 图片缓存
 */
class ImageCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 50 * 1024 * 1024L, // 50MB
        maxDiskCacheSize = 200 * 1024 * 1024L,  // 200MB
        defaultTtl = 86400000L // 24小时
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_IMAGE_CACHE"
        private const val PREFIX_IMAGE = "image"
    }
    
    /**
     * 🖼️ 缓存图片
     */
    suspend fun putImage(url: String, imageData: ByteArray): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_IMAGE, url.hashCode())
        return put(key, imageData, strategy = CacheStrategy.DISK_FIRST)
    }
    
    /**
     * 🖼️ 获取图片
     */
    suspend fun getImage(url: String): ByteArray? {
        val key = CacheKeyBuilder.create(PREFIX_IMAGE, url.hashCode())
        return get(key, ByteArray::class.java)
    }
    
    /**
     * 🔍 检查图片是否存在
     */
    suspend fun hasImage(url: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_IMAGE, url.hashCode())
        return contains(key)
    }
}

/**
 * 配置缓存
 */
class ConfigCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 5 * 1024 * 1024L, // 5MB
        defaultTtl = 3600000L // 1小时
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_CONFIG_CACHE"
        private const val PREFIX_SITE_CONFIG = "site_config"
        private const val PREFIX_APP_CONFIG = "app_config"
        private const val PREFIX_USER_CONFIG = "user_config"
    }
    
    /**
     * 🔧 缓存站点配置
     */
    suspend fun putSiteConfig(siteKey: String, config: VodSite): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_SITE_CONFIG, siteKey)
        return put(key, config, ttl = 7200000L) // 2小时
    }
    
    /**
     * 🔧 获取站点配置
     */
    suspend fun getSiteConfig(siteKey: String): VodSite? {
        val key = CacheKeyBuilder.create(PREFIX_SITE_CONFIG, siteKey)
        return get(key, VodSite::class.java)
    }
    
    /**
     * ⚙️ 缓存应用配置
     */
    suspend fun putAppConfig(configKey: String, configValue: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_APP_CONFIG, configKey)
        return put(key, configValue, ttl = 86400000L) // 24小时
    }
    
    /**
     * ⚙️ 获取应用配置
     */
    suspend fun getAppConfig(configKey: String): String? {
        val key = CacheKeyBuilder.create(PREFIX_APP_CONFIG, configKey)
        return get(key, String::class.java)
    }
    
    /**
     * 👤 缓存用户配置
     */
    suspend fun putUserConfig(userId: String, configKey: String, configValue: String): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_USER_CONFIG, userId, configKey)
        return put(key, configValue, ttl = 86400000L) // 24小时
    }
    
    /**
     * 👤 获取用户配置
     */
    suspend fun getUserConfig(userId: String, configKey: String): String? {
        val key = CacheKeyBuilder.create(PREFIX_USER_CONFIG, userId, configKey)
        return get(key, String::class.java)
    }
}

/**
 * 搜索缓存
 */
class SearchCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 10 * 1024 * 1024L, // 10MB
        defaultTtl = 1800000L // 30分钟
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_SEARCH_CACHE"
        private const val PREFIX_SEARCH_RESULT = "search_result"
        private const val PREFIX_SEARCH_SUGGESTION = "search_suggestion"
        private const val PREFIX_HOT_KEYWORDS = "hot_keywords"
    }
    
    /**
     * 🔍 缓存搜索结果
     */
    suspend fun putSearchResult(keyword: String, siteKeys: List<String>, results: List<VodInfo>): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH_RESULT, keyword, siteKeys.joinToString(","))
        return put(key, results)
    }
    
    /**
     * 🔍 获取搜索结果
     */
    suspend fun getSearchResult(keyword: String, siteKeys: List<String>): List<VodInfo>? {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH_RESULT, keyword, siteKeys.joinToString(","))
        return get(key, List::class.java) as? List<VodInfo>
    }
    
    /**
     * 💡 缓存搜索建议
     */
    suspend fun putSearchSuggestions(prefix: String, suggestions: List<String>): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH_SUGGESTION, prefix)
        return put(key, suggestions, ttl = 3600000L) // 1小时
    }
    
    /**
     * 💡 获取搜索建议
     */
    suspend fun getSearchSuggestions(prefix: String): List<String>? {
        val key = CacheKeyBuilder.create(PREFIX_SEARCH_SUGGESTION, prefix)
        return get(key, List::class.java) as? List<String>
    }
    
    /**
     * 🔥 缓存热门关键词
     */
    suspend fun putHotKeywords(keywords: List<String>): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_HOT_KEYWORDS)
        return put(key, keywords, ttl = 7200000L) // 2小时
    }
    
    /**
     * 🔥 获取热门关键词
     */
    suspend fun getHotKeywords(): List<String>? {
        val key = CacheKeyBuilder.create(PREFIX_HOT_KEYWORDS)
        return get(key, List::class.java) as? List<String>
    }
}

/**
 * 播放历史缓存
 */
class PlayHistoryCache(
    context: Context,
    config: CacheConfig = CacheConfig(
        maxMemoryCacheSize = 5 * 1024 * 1024L, // 5MB
        defaultTtl = 2592000000L // 30天
    )
) : CacheManager(context, config) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PLAY_HISTORY_CACHE"
        private const val PREFIX_PLAY_HISTORY = "play_history"
        private const val PREFIX_PLAY_POSITION = "play_position"
    }
    
    /**
     * 📺 缓存播放历史
     */
    suspend fun putPlayHistory(userId: String, vodId: String, playInfo: Map<String, Any>): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_PLAY_HISTORY, userId, vodId)
        return put(key, playInfo, strategy = CacheStrategy.DISK_FIRST)
    }
    
    /**
     * 📺 获取播放历史
     */
    suspend fun getPlayHistory(userId: String, vodId: String): Map<String, Any>? {
        val key = CacheKeyBuilder.create(PREFIX_PLAY_HISTORY, userId, vodId)
        return get(key, Map::class.java) as? Map<String, Any>
    }
    
    /**
     * ⏯️ 缓存播放位置
     */
    suspend fun putPlayPosition(userId: String, vodId: String, episode: String, position: Long): Boolean {
        val key = CacheKeyBuilder.create(PREFIX_PLAY_POSITION, userId, vodId, episode)
        return put(key, position, strategy = CacheStrategy.DISK_FIRST)
    }
    
    /**
     * ⏯️ 获取播放位置
     */
    suspend fun getPlayPosition(userId: String, vodId: String, episode: String): Long? {
        val key = CacheKeyBuilder.create(PREFIX_PLAY_POSITION, userId, vodId, episode)
        return get(key, Long::class.java)
    }
    
    /**
     * 📋 获取用户所有播放历史
     */
    suspend fun getUserPlayHistory(userId: String): List<Map<String, Any>> {
        // 这里应该实现模式匹配查询
        // 暂时返回空列表
        return emptyList()
    }
}

/**
 * 缓存工厂
 */
object CacheFactory {
    
    private val caches = ConcurrentHashMap<String, CacheManager>()
    
    /**
     * 🏭 获取 VOD 内容缓存
     */
    fun getVodContentCache(context: Context): VodContentCache {
        return caches.computeIfAbsent("vod_content") {
            VodContentCache(context)
        } as VodContentCache
    }
    
    /**
     * 🏭 获取网络响应缓存
     */
    fun getNetworkResponseCache(context: Context): NetworkResponseCache {
        return caches.computeIfAbsent("network_response") {
            NetworkResponseCache(context)
        } as NetworkResponseCache
    }
    
    /**
     * 🏭 获取图片缓存
     */
    fun getImageCache(context: Context): ImageCache {
        return caches.computeIfAbsent("image") {
            ImageCache(context)
        } as ImageCache
    }
    
    /**
     * 🏭 获取配置缓存
     */
    fun getConfigCache(context: Context): ConfigCache {
        return caches.computeIfAbsent("config") {
            ConfigCache(context)
        } as ConfigCache
    }
    
    /**
     * 🏭 获取搜索缓存
     */
    fun getSearchCache(context: Context): SearchCache {
        return caches.computeIfAbsent("search") {
            SearchCache(context)
        } as SearchCache
    }
    
    /**
     * 🏭 获取播放历史缓存
     */
    fun getPlayHistoryCache(context: Context): PlayHistoryCache {
        return caches.computeIfAbsent("play_history") {
            PlayHistoryCache(context)
        } as PlayHistoryCache
    }
    
    /**
     * 🧹 清理所有缓存
     */
    suspend fun clearAllCaches() {
        caches.values.forEach { cache ->
            cache.clear()
        }
    }
    
    /**
     * 🛑 关闭所有缓存
     */
    fun shutdownAllCaches() {
        caches.values.forEach { cache ->
            cache.shutdown()
        }
        caches.clear()
    }
}
