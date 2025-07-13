package top.cywin.onetv.film.catvod

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.proxy.ProxyManager
import top.cywin.onetv.film.jar.JarManager
import top.cywin.onetv.film.jar.JarConfig
import top.cywin.onetv.film.jar.JarLoadResult
import top.cywin.onetv.film.network.NetworkClient
import top.cywin.onetv.film.network.NetworkConfig
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.FileUtils
import top.cywin.onetv.film.utils.DateTimeUtils
import top.cywin.onetv.film.concurrent.ConcurrentManager
import top.cywin.onetv.film.concurrent.TaskType
import top.cywin.onetv.film.concurrent.ConcurrentUtils
import top.cywin.onetv.film.cache.CacheManager
import top.cywin.onetv.film.cache.CacheFactory
import top.cywin.onetv.film.cache.VodContentCache
import top.cywin.onetv.film.cache.CacheStrategy
import top.cywin.onetv.film.data.datasource.LocalDataSourceImpl
import top.cywin.onetv.film.data.datasource.RemoteDataSourceImpl
import top.cywin.onetv.film.data.repository.FilmRepository
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.optimization.PerformanceOptimizer
import top.cywin.onetv.film.monitoring.SystemMonitor
import java.util.concurrent.ConcurrentHashMap

/**
 * Spider 管理器
 * 
 * 基于 FongMi/TV 的 Spider 管理机制实现
 * 负责 Spider 的创建、缓存、生命周期管理
 * 
 * 功能：
 * - Spider 实例缓存
 * - 生命周期管理
 * - 类型检测和创建
 * - 错误处理和回退
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpiderManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPIDER_MANAGER"
    }
    
    // Spider 实例缓存
    private val spiderCache = ConcurrentHashMap<String, Spider>()

    // Spider 类型注册表
    private val spiderRegistry = mutableMapOf<String, Class<out Spider>>()

    // 空 Spider 实例
    private val nullSpider = SpiderNull()

    // Hook 管理器
    private val hookManager = HookManager()

    // 代理管理器
    private val proxyManager = ProxyManager(context, hookManager)

    // JAR 管理器
    private val jarManager = JarManager(context)

    // 网络客户端
    private val networkClient = NetworkClient(context, hookManager)

    // 并发管理器
    private val concurrentManager = ConcurrentManager(context)

    // 缓存管理器
    private val cacheManager = CacheManager(context)

    // VOD 内容缓存
    private val vodContentCache = CacheFactory.getVodContentCache(context)

    // 数据源
    private val localDataSource = LocalDataSourceImpl(context)
    private val remoteDataSource = RemoteDataSourceImpl(context, this, networkClient)

    // 数据仓库
    private val filmRepository = FilmRepository(context, localDataSource, remoteDataSource)

    // 性能优化器
    private val performanceOptimizer = PerformanceOptimizer(context, this)

    // 系统监控器
    private val systemMonitor = SystemMonitor(context, this)
    
    init {
        Log.d(TAG, "🏗️ SpiderManager 初始化")

        // 初始化 Hook 管理器
        hookManager.initialize()

        // 初始化代理管理器
        proxyManager.initialize()

        // 初始化 JAR 管理器
        jarManager.initialize()

        // 配置网络客户端
        configureNetworkClient()

        // 初始化并发和缓存系统
        initializeConcurrentAndCache()

        registerBuiltInSpiders()
    }
    
    /**
     * 🕷️ 获取或创建 Spider
     */
    suspend fun getSpider(site: VodSite): Spider = withContext(Dispatchers.IO) {
        try {
            val cacheKey = "${site.key}_${site.api}"
            
            // 从缓存获取
            spiderCache[cacheKey]?.let { spider ->
                Log.d(TAG, "📦 从缓存获取 Spider: ${spider.javaClass.simpleName}")
                return@withContext spider
            }
            
            // 创建新的 Spider
            val spider = createSpider(site)
            
            // 初始化 Spider
            spider.init(context, site.ext.toString())
            spider.setSiteInfo(site.key, site.name, site.api, site.header)
            
            // 缓存 Spider
            spiderCache[cacheKey] = spider
            
            Log.d(TAG, "✨ 创建并缓存新 Spider: ${spider.javaClass.simpleName}")
            spider
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取 Spider 失败: ${site.key}", e)
            nullSpider
        }
    }
    
    /**
     * 🏭 创建 Spider 实例
     */
    private fun createSpider(site: VodSite): Spider {
        return when {
            // JavaScript 解析器
            site.api.endsWith(".js") -> {
                Log.d(TAG, "🔧 创建 JavaScript Spider")
                createJavaScriptSpider(site)
            }
            
            // 类型3：自定义 Spider
            site.type == 3 -> {
                Log.d(TAG, "🔧 创建自定义 Spider: ${site.api}")
                createCustomSpider(site)
            }
            
            // 类型1：AppYs 接口
            site.type == 1 -> {
                Log.d(TAG, "🔧 创建 AppYs Spider")
                createAppYsSpider(site)
            }
            
            // 其他类型
            else -> {
                Log.w(TAG, "⚠️ 未知站点类型: ${site.type}, 使用空 Spider")
                SpiderNull()
            }
        }
    }
    
    /**
     * 🔧 创建 JavaScript Spider
     */
    private fun createJavaScriptSpider(site: VodSite): Spider {
        // 这里将在第4天实现 JavaScript Spider
        Log.d(TAG, "📝 JavaScript Spider 将在第4天实现")
        return SpiderNull()
    }
    
    /**
     * 🔧 创建自定义 Spider
     */
    private fun createCustomSpider(site: VodSite): Spider {
        val spiderClass = spiderRegistry[site.api]
        
        return if (spiderClass != null) {
            try {
                val spider = spiderClass.getDeclaredConstructor().newInstance()
                Log.d(TAG, "✅ 成功创建自定义 Spider: ${site.api}")
                spider
            } catch (e: Exception) {
                Log.e(TAG, "❌ 创建自定义 Spider 失败: ${site.api}", e)
                SpiderNull()
            }
        } else {
            Log.w(TAG, "⚠️ 未找到 Spider 实现: ${site.api}")
            SpiderNull()
        }
    }
    
    /**
     * 🔧 创建 AppYs Spider
     */
    private fun createAppYsSpider(site: VodSite): Spider {
        // 这里将在第4天实现 AppYs Spider
        Log.d(TAG, "📝 AppYs Spider 将在第4天实现")
        return SpiderNull()
    }
    
    /**
     * 📋 注册内置 Spider
     */
    private fun registerBuiltInSpiders() {
        Log.d(TAG, "📋 注册内置 Spider 类型")

        // 注册 XPath 系列 Spider（阶段3：XPath解析器系列）
        registerSpider("csp_XPath", top.cywin.onetv.film.spider.xpath.XPathSpider::class.java)
        registerSpider("csp_XPathMac", top.cywin.onetv.film.spider.xpath.XPathMacSpider::class.java)
        registerSpider("csp_XPathMacFilter", top.cywin.onetv.film.spider.xpath.XPathMacFilterSpider::class.java)
        registerSpider("csp_XPathFilter", top.cywin.onetv.film.spider.xpath.XPathFilterSpider::class.java)

        // 注册 AppYs Spider（阶段4：接口解析器系列）
        registerSpider("csp_AppYs", top.cywin.onetv.film.spider.appys.AppYsSpider::class.java)

        // 注册 JavaScript Spider（阶段4：接口解析器系列）
        registerSpider("csp_JavaScript", top.cywin.onetv.film.spider.javascript.JavaScriptSpider::class.java)

        // 注册专用 Spider（阶段5：专用和特殊解析器）
        registerSpider("csp_YydsAli1", top.cywin.onetv.film.spider.specialized.YydsAli1Spider::class.java)
        registerSpider("csp_Cokemv", top.cywin.onetv.film.spider.specialized.CokemvSpider::class.java)
        registerSpider("csp_Auete", top.cywin.onetv.film.spider.specialized.AueteSpider::class.java)

        // 注册特殊 Spider（阶段5：专用和特殊解析器）
        registerSpider("csp_Thunder", top.cywin.onetv.film.spider.special.ThunderSpider::class.java)
        registerSpider("csp_Tvbus", top.cywin.onetv.film.spider.special.TvbusSpider::class.java)
        registerSpider("csp_Jianpian", top.cywin.onetv.film.spider.special.JianpianSpider::class.java)
        registerSpider("csp_Forcetech", top.cywin.onetv.film.spider.special.ForcetechSpider::class.java)

        // 注册 Drpy Python Spider
        registerSpider("csp_Drpy", top.cywin.onetv.film.spider.drpy.DrpySpider::class.java)

        // 注册云盘 Spider
        registerSpider("csp_AliDrive", top.cywin.onetv.film.spider.cloud.AliDriveSpider::class.java)
        registerSpider("csp_Quark", top.cywin.onetv.film.spider.cloud.QuarkSpider::class.java)
        registerSpider("csp_Baidu", top.cywin.onetv.film.spider.cloud.BaiduSpider::class.java)

        Log.d(TAG, "📋 内置 Spider 注册完成，当前注册数量: ${spiderRegistry.size}")
    }
    
    /**
     * 📝 注册 Spider 类型
     */
    fun registerSpider(api: String, spiderClass: Class<out Spider>) {
        spiderRegistry[api] = spiderClass
        Log.d(TAG, "📝 注册 Spider: $api -> ${spiderClass.simpleName}")
    }
    
    /**
     * 🗑️ 移除 Spider 类型
     */
    fun unregisterSpider(api: String) {
        spiderRegistry.remove(api)
        Log.d(TAG, "🗑️ 移除 Spider: $api")
    }
    
    /**
     * 📊 获取已注册的 Spider 类型
     */
    fun getRegisteredSpiders(): Map<String, String> {
        return spiderRegistry.mapValues { it.value.simpleName }
    }
    
    /**
     * 🧹 清理 Spider 缓存
     */
    fun clearCache() {
        Log.d(TAG, "🧹 清理 Spider 缓存")
        
        // 销毁所有 Spider
        spiderCache.values.forEach { spider ->
            try {
                spider.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 销毁 Spider 时出错", e)
            }
        }
        
        spiderCache.clear()
        Log.d(TAG, "✅ Spider 缓存已清理")
    }
    
    /**
     * 🧹 清理指定站点的 Spider
     */
    fun clearSiteCache(siteKey: String) {
        Log.d(TAG, "🧹 清理站点 Spider 缓存: $siteKey")
        
        val keysToRemove = spiderCache.keys.filter { it.startsWith("${siteKey}_") }
        
        keysToRemove.forEach { key ->
            spiderCache[key]?.let { spider ->
                try {
                    spider.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 销毁 Spider 时出错", e)
                }
            }
            spiderCache.remove(key)
        }
        
        Log.d(TAG, "✅ 站点 Spider 缓存已清理: $siteKey")
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cached_spiders" to spiderCache.size,
            "registered_types" to spiderRegistry.size,
            "spider_types" to spiderCache.values.groupBy { it.javaClass.simpleName }.mapValues { it.value.size }
        )
    }
    
    /**
     * 🔍 检查 Spider 是否支持
     */
    fun isSpiderSupported(site: VodSite): Boolean {
        return when {
            site.api.endsWith(".js") -> true // JavaScript 支持
            site.type == 3 && spiderRegistry.containsKey(site.api) -> true // 自定义 Spider 支持
            site.type == 1 -> true // AppYs 支持
            else -> false
        }
    }
    
    /**
     * 🎯 测试 Spider
     */
    suspend fun testSpider(site: VodSite): Boolean = withContext(Dispatchers.IO) {
        try {
            val spider = getSpider(site)
            
            if (spider is SpiderNull) {
                Log.w(TAG, "⚠️ Spider 测试失败: 获取到空 Spider")
                return@withContext false
            }
            
            // 测试 homeContent
            val result = SpiderDebug.debugSpiderOperation(spider, "homeContent", mapOf("filter" to false)) {
                spider.homeContent(false)
            }
            
            val success = result.isNotEmpty() && !result.contains("error")
            Log.d(TAG, "🎯 Spider 测试结果: ${if (success) "成功" else "失败"}")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 测试异常", e)
            false
        }
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        spiderCache.clear()
        Log.d(TAG, "🧹 Spider 缓存已清理")
    }

    /**
     * 🔗 获取 Hook 管理器
     */
    fun getHookManager(): HookManager {
        return hookManager
    }

    /**
     * 🔗 获取代理管理器
     */
    fun getProxyManager(): ProxyManager {
        return proxyManager
    }

    /**
     * 🔗 获取代理 URL
     */
    fun getProxyUrl(originalUrl: String): String {
        return proxyManager.getProxyUrl(originalUrl)
    }

    /**
     * 🔗 获取 JAR 管理器
     */
    fun getJarManager(): JarManager {
        return jarManager
    }

    /**
     * 📦 加载 JAR 包
     */
    suspend fun loadJarFromUrl(jarUrl: String, jarName: String = ""): JarLoadResult {
        val jarConfig = JarConfig(
            url = jarUrl,
            name = jarName.ifEmpty { "JAR_${System.currentTimeMillis()}" },
            enabled = true,
            autoUpdate = false
        )

        // 添加配置
        jarManager.addJarConfig(jarConfig)

        // 加载 JAR
        val jarKey = jarConfig.generateKey()
        return jarManager.loadJar(jarKey)
    }

    /**
     * 🏭 从 JAR 创建 Spider 实例
     */
    suspend fun createSpiderFromJar(jarUrl: String, className: String): Spider? {
        val jarKey = JarConfig(url = jarUrl).generateKey()
        return jarManager.createSpiderInstance(jarKey, className)
    }

    /**
     * 📋 获取已加载的 JAR 信息
     */
    fun getLoadedJars() = jarManager.getLoadedJars()

    /**
     * 📊 获取 JAR 统计信息
     */
    fun getJarStats() = jarManager.getManagerStats()

    /**
     * 🔧 配置网络客户端
     */
    private fun configureNetworkClient() {
        val networkConfig = NetworkConfig(
            connectTimeout = 15000L,
            readTimeout = 15000L,
            writeTimeout = 15000L,
            maxRetries = 3,
            retryDelay = 1000L,
            enableLogging = false,
            trustAllCertificates = false,
            userAgent = "OneTV-Spider/1.0",
            maxIdleConnections = 5,
            keepAliveDuration = 5 * 60 * 1000L
        )

        networkClient.updateConfig(networkConfig)
        Log.d(TAG, "🔧 网络客户端配置完成")
    }

    /**
     * 🔗 获取网络客户端
     */
    fun getNetworkClient(): NetworkClient {
        return networkClient
    }

    /**
     * 🌐 执行网络请求
     */
    suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        return try {
            val response = networkClient.get(url, headers)
            if (response.isSuccess) {
                (response as top.cywin.onetv.film.network.NetworkResponse.Success).asString()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP GET 请求失败: $url", e)
            ""
        }
    }

    /**
     * 🌐 执行 POST 请求
     */
    suspend fun httpPost(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
        return try {
            val response = networkClient.post(url, body, headers)
            if (response.isSuccess) {
                (response as top.cywin.onetv.film.network.NetworkResponse.Success).asString()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP POST 请求失败: $url", e)
            ""
        }
    }

    /**
     * 📊 获取网络统计
     */
    fun getNetworkStats() = networkClient.getNetworkStats()

    /**
     * 🧹 清理网络统计
     */
    fun clearNetworkStats() = networkClient.clearStats()

    /**
     * 🔧 工具方法：字符串处理
     */
    fun processString(input: String?): String {
        return StringUtils.clean(StringUtils.safe(input))
    }

    /**
     * 🔧 工具方法：JSON 解析
     */
    fun parseJson(jsonStr: String?): kotlinx.serialization.json.JsonObject? {
        return JsonUtils.parseToJsonObject(jsonStr)
    }

    /**
     * 🔧 工具方法：获取 JSON 字符串值
     */
    fun getJsonString(jsonObject: kotlinx.serialization.json.JsonObject?, key: String, defaultValue: String = ""): String {
        return JsonUtils.getString(jsonObject, key, defaultValue)
    }

    /**
     * 🔧 工具方法：格式化时间
     */
    fun formatTime(timestamp: Long): String {
        return DateTimeUtils.formatTimestamp(timestamp)
    }

    /**
     * 🔧 工具方法：获取相对时间
     */
    fun getRelativeTime(timestamp: Long): String {
        return DateTimeUtils.getRelativeTimeDescription(timestamp)
    }

    /**
     * 🔧 初始化并发和缓存系统
     */
    private fun initializeConcurrentAndCache() {
        Log.d(TAG, "🔧 初始化并发和缓存系统...")

        // 并发管理器已在构造函数中初始化
        // 缓存管理器已在构造函数中初始化

        Log.d(TAG, "✅ 并发和缓存系统初始化完成")
    }

    /**
     * 🔗 获取并发管理器
     */
    fun getConcurrentManager(): ConcurrentManager {
        return concurrentManager
    }

    /**
     * 🔗 获取缓存管理器
     */
    fun getCacheManager(): CacheManager {
        return cacheManager
    }

    /**
     * 🔗 获取 VOD 内容缓存
     */
    fun getVodContentCache(): VodContentCache {
        return vodContentCache
    }

    /**
     * 🔄 并行执行多个 Spider 操作
     */
    suspend fun executeSpidersParallel(
        siteKeys: List<String>,
        operation: suspend (Spider) -> String,
        maxConcurrency: Int = 3
    ): Map<String, String> {
        return ConcurrentUtils.parallelMapNotNull(
            items = siteKeys,
            maxConcurrency = maxConcurrency
        ) { siteKey ->
            try {
                val spider = getSpider(siteKey)
                if (spider != null) {
                    siteKey to operation(spider)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Spider 操作失败: $siteKey", e)
                null
            }
        }.toMap()
    }

    /**
     * 🔍 并行搜索多个站点
     */
    suspend fun searchParallel(
        siteKeys: List<String>,
        keyword: String,
        page: Int = 1,
        maxConcurrency: Int = 3
    ): Map<String, String> {
        return executeSpidersParallel(siteKeys, maxConcurrency = maxConcurrency) { spider ->
            spider.searchContent(keyword, false, page.toString())
        }
    }

    /**
     * 🏠 并行获取首页内容
     */
    suspend fun getHomeContentParallel(
        siteKeys: List<String>,
        maxConcurrency: Int = 3
    ): Map<String, String> {
        return executeSpidersParallel(siteKeys, maxConcurrency = maxConcurrency) { spider ->
            spider.homeContent(false)
        }
    }

    /**
     * 💾 缓存首页内容
     */
    suspend fun cacheHomeContent(siteKey: String, content: String): Boolean {
        return vodContentCache.putHomeContent(siteKey, content)
    }

    /**
     * 📦 获取缓存的首页内容
     */
    suspend fun getCachedHomeContent(siteKey: String): String? {
        return vodContentCache.getHomeContent(siteKey)
    }

    /**
     * 💾 缓存分类内容
     */
    suspend fun cacheCategoryContent(siteKey: String, tid: String, page: Int, content: String): Boolean {
        return vodContentCache.putCategoryContent(siteKey, tid, page, content)
    }

    /**
     * 📦 获取缓存的分类内容
     */
    suspend fun getCachedCategoryContent(siteKey: String, tid: String, page: Int): String? {
        return vodContentCache.getCategoryContent(siteKey, tid, page)
    }

    /**
     * 💾 缓存详情内容
     */
    suspend fun cacheDetailContent(siteKey: String, vodId: String, content: String): Boolean {
        return vodContentCache.putDetailContent(siteKey, vodId, content)
    }

    /**
     * 📦 获取缓存的详情内容
     */
    suspend fun getCachedDetailContent(siteKey: String, vodId: String): String? {
        return vodContentCache.getDetailContent(siteKey, vodId)
    }

    /**
     * 📋 提交并发任务
     */
    suspend fun submitTask(taskId: String, taskType: TaskType, action: suspend () -> Any?): Boolean {
        return concurrentManager.submitTask(
            top.cywin.onetv.film.concurrent.ConcurrentTask(taskId, taskType, action)
        )
    }

    /**
     * 📊 获取并发统计
     */
    fun getConcurrentStats() = concurrentManager.getConcurrentStats()

    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats() = cacheManager.getStats()

    /**
     * 🧹 清理缓存
     */
    suspend fun clearCache() = cacheManager.clear()

    /**
     * 🧹 清理站点缓存
     */
    suspend fun clearSiteCache(siteKey: String) {
        vodContentCache.clearSiteCache(siteKey)
    }

    /**
     * 🔗 获取数据仓库
     */
    fun getFilmRepository(): FilmRepository {
        return filmRepository
    }

    /**
     * 🏠 通过仓库获取首页内容
     */
    suspend fun getHomeContentFromRepository(siteKey: String, forceRefresh: Boolean = false): Result<VodHomeResult> {
        return filmRepository.getHomeContent(siteKey, forceRefresh)
    }

    /**
     * 📂 通过仓库获取分类内容
     */
    suspend fun getCategoryContentFromRepository(
        siteKey: String,
        tid: String,
        page: Int = 1,
        forceRefresh: Boolean = false,
        extend: Map<String, String> = emptyMap()
    ): Result<VodCategoryResult> {
        return filmRepository.getCategoryContent(siteKey, tid, page, forceRefresh, extend)
    }

    /**
     * 📄 通过仓库获取详情内容
     */
    suspend fun getDetailContentFromRepository(
        siteKey: String,
        vodId: String,
        forceRefresh: Boolean = false
    ): Result<VodInfo?> {
        return filmRepository.getDetailContent(siteKey, vodId, forceRefresh)
    }

    /**
     * 🔍 通过仓库搜索内容
     */
    suspend fun searchContentFromRepository(
        keyword: String,
        siteKeys: List<String> = emptyList(),
        page: Int = 1,
        forceRefresh: Boolean = false
    ): Result<Map<String, VodSearchResult>> {
        return filmRepository.searchContent(keyword, siteKeys, page, forceRefresh)
    }

    /**
     * ▶️ 通过仓库获取播放内容
     */
    suspend fun getPlayContentFromRepository(
        siteKey: String,
        flag: String,
        id: String,
        vipUrl: List<String> = emptyList()
    ): Result<String> {
        return filmRepository.getPlayContent(siteKey, flag, id, vipUrl)
    }

    /**
     * 📋 通过仓库获取站点列表
     */
    suspend fun getSitesFromRepository(forceRefresh: Boolean = false): Result<List<VodSite>> {
        return filmRepository.getSites(forceRefresh)
    }

    /**
     * 💾 通过仓库保存播放历史
     */
    suspend fun savePlayHistoryToRepository(history: PlayHistory): Result<Boolean> {
        return filmRepository.savePlayHistory(history)
    }

    /**
     * 📋 通过仓库获取播放历史
     */
    suspend fun getPlayHistoriesFromRepository(
        userId: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PlayHistory>> {
        return filmRepository.getPlayHistories(userId, limit, offset)
    }

    /**
     * 💾 通过仓库添加收藏
     */
    suspend fun addFavoriteToRepository(favorite: FavoriteInfo): Result<Boolean> {
        return filmRepository.addFavorite(favorite)
    }

    /**
     * 📋 通过仓库获取收藏列表
     */
    suspend fun getFavoritesFromRepository(
        userId: String = "",
        category: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<FavoriteInfo>> {
        return filmRepository.getFavorites(userId, category, limit, offset)
    }

    /**
     * 🔧 通过仓库获取应用配置
     */
    suspend fun getAppConfigFromRepository(): Result<AppConfig> {
        return filmRepository.getAppConfig()
    }

    /**
     * 💾 通过仓库保存应用配置
     */
    suspend fun saveAppConfigToRepository(config: AppConfig): Result<Boolean> {
        return filmRepository.saveAppConfig(config)
    }

    /**
     * 📊 获取仓库统计
     */
    fun getRepositoryStats(): Map<String, Any> {
        return filmRepository.getRepositoryStats()
    }

    /**
     * 🔧 获取性能优化器
     */
    fun getPerformanceOptimizer(): PerformanceOptimizer {
        return performanceOptimizer
    }

    /**
     * 📊 获取系统监控器
     */
    fun getSystemMonitor(): SystemMonitor {
        return systemMonitor
    }

    /**
     * 📊 执行性能分析
     */
    suspend fun analyzePerformance() = performanceOptimizer.analyzePerformance()

    /**
     * 🔍 生成诊断报告
     */
    suspend fun generateDiagnosticReport() = systemMonitor.generateDiagnosticReport()

    /**
     * 📊 获取优化统计
     */
    fun getOptimizationStats(): Map<String, Any> {
        return performanceOptimizer.getOptimizationStats()
    }

    /**
     * 📊 获取监控统计
     */
    fun getMonitoringStats(): Map<String, Any> {
        return systemMonitor.getMonitoringStats()
    }

    /**
     * 📊 获取完整系统统计
     */
    fun getCompleteSystemStats(): Map<String, Any> {
        return mapOf(
            "spider_stats" to getSpiderStats(),
            "concurrent_stats" to getConcurrentStats(),
            "cache_stats" to getCacheStats(),
            "network_stats" to getNetworkStats(),
            "repository_stats" to getRepositoryStats(),
            "optimization_stats" to getOptimizationStats(),
            "monitoring_stats" to getMonitoringStats(),
            "system_info" to mapOf(
                "timestamp" to System.currentTimeMillis(),
                "uptime" to (System.currentTimeMillis() - initTime),
                "version" to "2.1.1"
            )
        )
    }

    /**
     * 🛑 销毁管理器
     */
    fun destroy() {
        Log.d(TAG, "🛑 SpiderManager 销毁")

        // 清理缓存
        clearCache()
        spiderRegistry.clear()

        // 关闭并发管理器
        concurrentManager.shutdown()

        // 关闭缓存管理器
        cacheManager.shutdown()

        // 关闭网络客户端
        networkClient.shutdown()

        // 关闭 JAR 管理器
        jarManager.shutdown()

        // 关闭代理管理器
        proxyManager.shutdown()

        // 关闭 Hook 管理器
        hookManager.shutdown()

        Log.d(TAG, "✅ SpiderManager 销毁完成")
    }
}
