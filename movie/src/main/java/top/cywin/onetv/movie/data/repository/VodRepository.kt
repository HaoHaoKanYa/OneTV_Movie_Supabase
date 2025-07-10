package top.cywin.onetv.movie.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.api.VodApiService
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.config.AppConfigManager
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.parser.ParseManager
import top.cywin.onetv.movie.data.parser.ParseResult
import top.cywin.onetv.movie.di.SiteApiService
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * 点播数据仓库 (参考OneMoVie Repository模式)
 */
@Singleton
class VodRepository @Inject constructor(
    private val context: Context,
    private val appConfigManager: AppConfigManager,
    private val cacheManager: MovieCacheManager,
    private val configManager: VodConfigManager,
    private val parseManager: ParseManager,
    @SiteApiService private val siteApiService: VodApiService
) {

    /**
     * 获取配置文件并初始化站点
     */
    suspend fun loadConfig(): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "开始加载配置")

            // 1. 尝试从缓存获取
            val cachedConfig = configManager.getCurrentConfig()
            if (cachedConfig != null) {
                Log.d("ONETV_MOVIE", "使用缓存配置")
                return@withContext Result.success(cachedConfig)
            }

            // 2. 初始化配置管理器
            Log.d("ONETV_MOVIE", "初始化配置管理器")
            val initResult = appConfigManager.initializeConfig()
            if (initResult.isFailure) {
                Log.e("ONETV_MOVIE", "配置管理器初始化失败", initResult.exceptionOrNull())
                // 不要直接失败，而是尝试使用默认配置
            }

            // 3. 创建默认配置响应
            Log.d("ONETV_MOVIE", "创建默认配置响应")
            val defaultConfigResponse = createDefaultConfigResponse()

            // 4. 加载到配置管理器
            val loadResult = configManager.load(defaultConfigResponse)
            if (loadResult.isSuccess) {
                Log.d("ONETV_MOVIE", "默认配置加载成功")
                Result.success(defaultConfigResponse)
            } else {
                Log.e("ONETV_MOVIE", "默认配置加载失败")
                Result.failure(Exception("配置加载失败"))
            }

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "配置加载异常", e)
            // 即使异常也尝试提供默认配置
            try {
                val defaultConfigResponse = createDefaultConfigResponse()
                Result.success(defaultConfigResponse)
            } catch (ex: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 创建默认配置响应（临时解决方案）
     */
    private fun createDefaultConfigResponse(): VodConfigResponse {
        val defaultSite = VodSite(
            key = "default",
            name = "默认站点",
            api = "https://example.com/api.php/provide/vod/",
            ext = "",
            jar = "",
            type = 1,
            searchable = 1,
            changeable = 1,
            timeout = 30000,
            header = emptyMap(),
            style = null,
            categories = listOf(
                VodClass(
                    typeId = "1",
                    typeName = "电影",
                    typeFlag = "1"
                ),
                VodClass(
                    typeId = "2",
                    typeName = "电视剧",
                    typeFlag = "1"
                ),
                VodClass(
                    typeId = "3",
                    typeName = "综艺",
                    typeFlag = "1"
                ),
                VodClass(
                    typeId = "4",
                    typeName = "动漫",
                    typeFlag = "1"
                )
            )
        )

        val defaultParse = VodParse(
            name = "默认解析",
            type = 1,
            url = "https://example.com/parse?url=",
            ext = emptyMap(),
            header = emptyMap()
        )

        return VodConfigResponse(
            spider = "",
            wallpaper = "",
            sites = listOf(defaultSite),
            parses = listOf(defaultParse),
            flags = listOf("qiyi", "qq", "youku", "mgtv"),
            ijk = emptyList(),
            ads = emptyList(),
            notice = "这是临时默认配置，请配置正确的服务器信息"
        )
    }

    /**
     * 获取站点分类
     */
    suspend fun getCategories(siteKey: String): Result<List<VodClass>> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点: $siteKey"))

            // 检查缓存
            val cacheKey = "categories_$siteKey"
            val cached = cacheManager.getCache(cacheKey, Array<VodClass>::class.java)?.toList()
            if (!cached.isNullOrEmpty()) {
                return@withContext Result.success(cached)
            }

            // 从网络获取 (这里需要实际的网络请求实现)
            // val response = siteApiService.getHomeContent(site.api)
            // val categories = response.`class` ?: emptyList()
            val categories = emptyList<VodClass>() // 临时返回空列表

            // 缓存结果
            cacheManager.putCache(cacheKey, categories.toTypedArray(), 24 * 60 * 60 * 1000L)

            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取内容列表
     */
    suspend fun getContentList(
        typeId: String,
        page: Int = 1,
        siteKey: String = "",
        filters: Map<String, String> = emptyMap()
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 构建缓存键
            val filterString = filters.entries.joinToString("&") { "${it.key}=${it.value}" }
            val cacheKey = "content_${siteKey}_${typeId}_${page}_${filterString.hashCode()}"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodResponse::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取 (这里需要实际的网络请求实现)
            // val response = siteApiService.getCategoryContent(...)
            val response = VodResponse(
                code = 1,
                msg = "success",
                page = page,
                pagecount = 1,
                limit = 20,
                total = 0,
                list = emptyList()
            )

            // 缓存结果
            cacheManager.putCache(cacheKey, response, 24 * 60 * 60 * 1000L)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 搜索内容
     */
    suspend fun searchContent(
        keyword: String,
        page: Int = 1,
        siteKey: String = ""
    ): Result<VodResponse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            if (!site.isSearchable()) {
                return@withContext Result.failure(Exception("该站点不支持搜索"))
            }

            // 构建缓存键
            val cacheKey = "search_${siteKey}_${keyword}_$page"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodResponse::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取
            val response = siteApiService.searchContent(
                url = site.api,
                keyword = keyword,
                page = page
            )

            // 缓存结果
            cacheManager.putCache(cacheKey, response, 5 * 60 * 1000) // 5分钟

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取内容详情
     */
    suspend fun getContentDetail(
        vodId: String,
        siteKey: String = ""
    ): Result<VodItem> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 构建缓存键
            val cacheKey = "detail_${siteKey}_$vodId"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, VodItem::class.java)
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取
            val response = siteApiService.getContentDetail(
                url = site.api,
                ids = vodId
            )

            val item = response.list?.firstOrNull()
                ?: return@withContext Result.failure(Exception("未找到内容详情"))

            // 设置站点信息
            val itemWithSite = item.copy(siteKey = siteKey)

            // 缓存结果
            cacheManager.putCache(cacheKey, itemWithSite, 60 * 60 * 1000) // 1小时

            Result.success(itemWithSite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取推荐内容
     */
    suspend fun getRecommendContent(siteKey: String = ""): Result<List<VodItem>> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 构建缓存键
            val cacheKey = "recommend_$siteKey"

            // 检查缓存
            val cached = cacheManager.getCache(cacheKey, Array<VodItem>::class.java)?.toList()
            if (cached != null) {
                return@withContext Result.success(cached)
            }

            // 从网络获取首页内容作为推荐
            val response = siteApiService.getHomeContent(site.api)
            val recommendList = response.list?.take(20) ?: emptyList()

            // 缓存结果
            cacheManager.putCache(cacheKey, recommendList, 30 * 60 * 1000) // 30分钟

            Result.success(recommendList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清除缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheManager.clearAllCache()
    }

    /**
     * 解析播放地址 (集成解析器系统)
     */
    suspend fun parsePlayUrl(
        url: String,
        siteKey: String,
        flag: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            // 获取解析器配置
            val parse = configManager.getParseByFlag(flag)

            // 使用解析器管理器解析
            val parseResult = parseManager.parsePlayUrl(url, parse, flag)

            if (parseResult.isNotEmpty()) {
                Result.success(parseResult)
            } else {
                Result.failure(Exception("解析失败"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取解析结果详情 (包含headers等信息)
     */
    suspend fun parsePlayUrlDetailed(
        url: String,
        siteKey: String,
        flag: String = ""
    ): Result<ParseResult> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
                ?: return@withContext Result.failure(Exception("未找到站点"))

            val parse = configManager.getParseByFlag(flag)
            val playUrl = parseManager.parsePlayUrl(url, parse, flag)

            val parseResult = ParseResult(
                success = playUrl.isNotEmpty(),
                playUrl = playUrl,
                error = if (playUrl.isEmpty()) "解析失败" else null
            )
            Result.success(parseResult)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取可用解析器列表
     */
    suspend fun getAvailableParsers(siteKey: String): List<VodParse> = withContext(Dispatchers.IO) {
        try {
            val config = configManager.getCurrentConfig()
            config?.parses ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取站点支持的解析器
     */
    suspend fun getSiteParsers(siteKey: String): List<VodParse> = withContext(Dispatchers.IO) {
        try {
            val site = configManager.getSite(siteKey)
            val config = configManager.getCurrentConfig()

            if (site != null && config != null) {
                // 根据站点配置筛选适合的解析器
                config.parses.filter { parse ->
                    // 这里可以添加更复杂的匹配逻辑
                    parse.type in listOf(0, 1, 2) // 支持的解析器类型
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 测试解析器可用性
     */
    suspend fun testParser(parse: VodParse, testUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val playUrl = parseManager.parsePlayUrl(testUrl, parse, "")
            Result.success(playUrl.isNotEmpty())
        } catch (e: Exception) {
            Result.success(false)
        }
    }

    /**
     * 获取缓存大小
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        cacheManager.getCacheSize()
    }
}
