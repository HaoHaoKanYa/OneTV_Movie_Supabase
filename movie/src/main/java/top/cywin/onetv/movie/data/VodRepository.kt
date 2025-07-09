package top.cywin.onetv.movie.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.cywin.onetv.movie.data.api.VodApiService
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.di.ConfigApiService
import top.cywin.onetv.movie.di.SiteApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 点播数据仓库 - 按照TVBOX标准实现
 */
@Singleton
class VodRepository @Inject constructor(
    private val cacheManager: MovieCacheManager,
    @ConfigApiService private val configApiService: VodApiService,
    @SiteApiService private val siteApiService: VodApiService
) {
    
    private var currentConfig: VodConfigResponse? = null
    private var currentSite: VodSite? = null
    
    /**
     * 加载配置文件 (从Supabase获取onetv-api-movie.json)
     */
    suspend fun loadConfig(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            // 先尝试从缓存获取
            val cacheKey = "vod_config"
            val cached = cacheManager.getCache(cacheKey, VodConfigResponse::class.java)
            if (cached != null) {
                currentConfig = cached
                return@withContext Result.success(cached)
            }

            // 从Supabase获取配置
            val config = configApiService.getConfig()

            // 验证配置
            if (config.sites.isEmpty()) {
                return@withContext Result.failure(Exception("配置文件中没有可用站点"))
            }

            // 设置默认站点
            currentSite = config.sites.firstOrNull()
            currentConfig = config

            // 缓存配置 (缓存10分钟)
            cacheManager.putCache(cacheKey, config, 10 * 60 * 1000)

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取站点首页内容 (直接调用站点API，TVBOX标准)
     */
    suspend fun getHomeContent(siteKey: String? = null): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = getSiteByKey(siteKey) ?: return@withContext Result.failure(Exception("站点不存在"))

            // 直接调用站点API获取分类列表
            val response = siteApiService.getHomeContent(site.api)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取分类内容 (直接调用站点API，TVBOX标准)
     */
    suspend fun getCategoryContent(
        typeId: String,
        page: Int = 1,
        filters: Map<String, String> = emptyMap(),
        siteKey: String? = null
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = getSiteByKey(siteKey) ?: return@withContext Result.failure(Exception("站点不存在"))

            // 构建筛选参数
            val filterString = if (filters.isNotEmpty()) {
                Json.encodeToString(filters)
            } else ""

            // 直接调用站点API
            val response = siteApiService.getCategoryContent(
                url = site.api,
                typeId = typeId,
                page = page,
                filters = filterString
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 搜索内容 (直接调用站点API，TVBOX标准)
     */
    suspend fun searchContent(
        keyword: String,
        page: Int = 1,
        siteKey: String? = null
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = getSiteByKey(siteKey) ?: return@withContext Result.failure(Exception("站点不存在"))

            // 直接调用站点API
            val response = siteApiService.searchContent(
                url = site.api,
                keyword = keyword,
                page = page
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取内容详情 (直接调用站点API)
     */
    suspend fun getContentDetail(
        vodId: String,
        siteKey: String? = null
    ): Result<VodItem?> = withContext(Dispatchers.IO) {
        try {
            val site = getSiteByKey(siteKey) ?: return@withContext Result.failure(Exception("站点不存在"))
            
            // 直接调用站点API
            val response = siteApiService.getContentDetail(
                url = site.api,
                ids = vodId
            )
            
            val item = response.list.firstOrNull()
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): VodConfigResponse? = currentConfig
    
    /**
     * 获取所有站点
     */
    fun getSites(): List<VodSite> = currentConfig?.sites ?: emptyList()
    
    /**
     * 根据key获取站点
     */
    private fun getSiteByKey(siteKey: String?): VodSite? {
        return if (siteKey != null) {
            currentConfig?.sites?.find { it.key == siteKey }
        } else {
            currentSite ?: currentConfig?.sites?.firstOrNull()
        }
    }
    
    /**
     * 构建API URL
     */
    private fun buildApiUrl(baseUrl: String, params: Map<String, String>): String {
        val paramString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return if (baseUrl.contains("?")) {
            "$baseUrl&$paramString"
        } else {
            "$baseUrl?$paramString"
        }
    }
    
    /**
     * 切换站点
     */
    fun switchSite(siteKey: String) {
        currentSite = currentConfig?.sites?.find { it.key == siteKey }
    }
}
