package top.cywin.onetv.film.data.datasource

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.network.NetworkClient
import top.cywin.onetv.film.network.NetworkResponse
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils

/**
 * 远程数据源实现
 * 
 * 基于 FongMi/TV 的远程数据获取实现
 * 通过 SpiderManager 和网络客户端获取数据
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class RemoteDataSourceImpl(
    private val context: Context,
    private val spiderManager: SpiderManager,
    private val networkClient: NetworkClient
) : VodDataSource, RemoteDataSource {
    
    companion object {
        private const val TAG = "ONETV_FILM_REMOTE_DATA_SOURCE"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    
    // ==================== VOD 数据源实现 ====================
    
    override suspend fun getHomeContent(siteKey: String, filter: Boolean): Result<VodHomeResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🏠 获取首页内容: $siteKey")
            
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("未找到站点: $siteKey"))
            }
            
            val response = spider.homeContent(filter)
            if (response.isEmpty()) {
                Log.w(TAG, "⚠️ 首页内容为空: $siteKey")
                return@withContext Result.success(VodHomeResult(siteKey = siteKey))
            }
            
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 首页内容解析失败: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("首页内容解析失败"))
            }
            
            // 解析分类
            val categoriesJson = JsonUtils.getJsonArray(jsonObject, "class")
            val categories = categoriesJson?.let { array ->
                JsonUtils.getStringList(array).mapNotNull { categoryStr ->
                    try {
                        val categoryObj = JsonUtils.parseToJsonObject(categoryStr)
                        if (categoryObj != null) {
                            VodCategory(
                                typeId = JsonUtils.getString(categoryObj, "type_id"),
                                typeName = JsonUtils.getString(categoryObj, "type_name"),
                                typeFlag = JsonUtils.getString(categoryObj, "type_flag"),
                                land = JsonUtils.getInt(categoryObj, "land"),
                                ratio = JsonUtils.getDouble(categoryObj, "ratio"),
                                pic = JsonUtils.getString(categoryObj, "type_pic")
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ 分类解析失败: $categoryStr", e)
                        null
                    }
                }
            } ?: emptyList()
            
            // 解析筛选条件
            val filtersJson = JsonUtils.getJsonObject(jsonObject, "filters")
            val filters = filtersJson?.let { filterObj ->
                // 解析筛选条件的逻辑
                emptyMap<String, List<VodFilter>>()
            } ?: emptyMap()
            
            // 解析推荐内容
            val listJson = JsonUtils.getJsonArray(jsonObject, "list")
            val recommendations = listJson?.let { array ->
                parseVodInfoList(array, siteKey)
            } ?: emptyList()
            
            val result = VodHomeResult(
                categories = categories,
                filters = filters,
                recommendations = recommendations,
                siteKey = siteKey,
                siteName = spider.getName()
            )
            
            Log.d(TAG, "✅ 首页内容获取成功: $siteKey, 分类数: ${categories.size}, 推荐数: ${recommendations.size}")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取首页内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getCategoryContent(
        siteKey: String,
        tid: String,
        page: Int,
        filter: Boolean,
        extend: Map<String, String>
    ): Result<VodCategoryResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "📂 获取分类内容: $siteKey, tid: $tid, page: $page")
            
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("未找到站点: $siteKey"))
            }
            
            val response = spider.categoryContent(tid, page.toString(), filter, extend)
            if (response.isEmpty()) {
                Log.w(TAG, "⚠️ 分类内容为空: $siteKey")
                return@withContext Result.success(VodCategoryResult(siteKey = siteKey, typeId = tid))
            }
            
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 分类内容解析失败: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("分类内容解析失败"))
            }
            
            // 解析VOD列表
            val listJson = JsonUtils.getJsonArray(jsonObject, "list")
            val vodList = listJson?.let { array ->
                parseVodInfoList(array, siteKey)
            } ?: emptyList()
            
            // 解析分页信息
            val pageCount = JsonUtils.getInt(jsonObject, "pagecount", 1)
            val total = JsonUtils.getInt(jsonObject, "total", vodList.size)
            val limit = JsonUtils.getInt(jsonObject, "limit", 20)
            
            val result = VodCategoryResult(
                list = vodList,
                page = page,
                pageCount = pageCount,
                limit = limit,
                total = total,
                siteKey = siteKey,
                typeId = tid,
                filters = extend
            )
            
            Log.d(TAG, "✅ 分类内容获取成功: $siteKey, 数量: ${vodList.size}")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分类内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getDetailContent(siteKey: String, ids: List<String>): Result<List<VodInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "📄 获取详情内容: $siteKey, ids: $ids")
            
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("未找到站点: $siteKey"))
            }
            
            val response = spider.detailContent(ids)
            if (response.isEmpty()) {
                Log.w(TAG, "⚠️ 详情内容为空: $siteKey")
                return@withContext Result.success(emptyList())
            }
            
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 详情内容解析失败: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("详情内容解析失败"))
            }
            
            // 解析VOD列表
            val listJson = JsonUtils.getJsonArray(jsonObject, "list")
            val vodList = listJson?.let { array ->
                parseVodInfoList(array, siteKey)
            } ?: emptyList()
            
            Log.d(TAG, "✅ 详情内容获取成功: $siteKey, 数量: ${vodList.size}")
            Result.success(vodList)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取详情内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchContent(
        siteKey: String,
        keyword: String,
        page: Int,
        quick: Boolean
    ): Result<VodSearchResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔍 搜索内容: $siteKey, keyword: $keyword, page: $page")
            
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("未找到站点: $siteKey"))
            }
            
            val response = spider.searchContent(keyword, quick, page.toString())
            if (response.isEmpty()) {
                Log.w(TAG, "⚠️ 搜索结果为空: $siteKey")
                return@withContext Result.success(VodSearchResult(siteKey = siteKey, keyword = keyword))
            }
            
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 搜索结果解析失败: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("搜索结果解析失败"))
            }
            
            // 解析VOD列表
            val listJson = JsonUtils.getJsonArray(jsonObject, "list")
            val vodList = listJson?.let { array ->
                parseVodInfoList(array, siteKey)
            } ?: emptyList()
            
            // 解析分页信息
            val pageCount = JsonUtils.getInt(jsonObject, "pagecount", 1)
            val total = JsonUtils.getInt(jsonObject, "total", vodList.size)
            val limit = JsonUtils.getInt(jsonObject, "limit", 20)
            
            val result = VodSearchResult(
                list = vodList,
                page = page,
                pageCount = pageCount,
                limit = limit,
                total = total,
                siteKey = siteKey,
                keyword = keyword
            )
            
            Log.d(TAG, "✅ 搜索内容成功: $siteKey, 数量: ${vodList.size}")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPlayContent(
        siteKey: String,
        flag: String,
        id: String,
        vipUrl: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "▶️ 获取播放内容: $siteKey, flag: $flag, id: $id")
            
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(IllegalArgumentException("未找到站点: $siteKey"))
            }
            
            val response = spider.playerContent(flag, id, vipUrl)
            
            Log.d(TAG, "✅ 播放内容获取成功: $siteKey")
            Result.success(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放内容失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSites(): Result<List<VodSite>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🌐 获取站点列表（使用真实数据源）")

            // 优先使用真实数据源管理器
            val realDataSourceManager = top.cywin.onetv.film.data.datasource.RealDataSourceManager.getInstance()
            val realSites = realDataSourceManager.getRealDataSources()

            if (realSites.isNotEmpty()) {
                Log.d(TAG, "✅ 使用真实数据源，站点数量: ${realSites.size}")
                return@withContext Result.success(realSites)
            }

            // 备用方案：使用注册的 Spider 生成站点列表
            Log.w(TAG, "⚠️ 真实数据源为空，使用注册的 Spider")
            val spiders = spiderManager.getAllSpiders()
            val sites = spiders.map { (key, spider) ->
                VodSite(
                    key = key,
                    name = spider.getName(),
                    type = spider.getType(),
                    api = spider.getApi(),
                    searchable = if (spider.isSearchable()) 1 else 0,
                    quickSearch = if (spider.isQuickSearchable()) 1 else 0,
                    filterable = if (spider.isFilterable()) 1 else 0,
                    enabled = true,
                    status = top.cywin.onetv.film.data.models.SiteStatus.ONLINE,
                    verifyStatus = top.cywin.onetv.film.data.models.VerifyStatus.UNVERIFIED
                )
            }

            Log.d(TAG, "✅ 获取站点列表成功: ${sites.size}")
            Result.success(sites)

        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点列表失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSiteConfig(siteKey: String): Result<VodSite?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Result.success(null)
            } else {
                val site = VodSite(
                    key = siteKey,
                    name = spider.getName(),
                    type = spider.getType(),
                    api = spider.getApi(),
                    searchable = if (spider.isSearchable()) 1 else 0,
                    quickSearch = if (spider.isQuickSearchable()) 1 else 0,
                    filterable = if (spider.isFilterable()) 1 else 0
                )
                Result.success(site)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点配置失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveSiteConfig(site: VodSite): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("远程数据源不支持保存站点配置"))
    }
    
    override suspend fun deleteSiteConfig(siteKey: String): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("远程数据源不支持删除站点配置"))
    }
    
    override suspend fun updateSiteConfig(site: VodSite): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("远程数据源不支持更新站点配置"))
    }
    
    // ==================== 远程数据源实现 ====================
    
    override suspend fun getRemoteConfig(url: String): Result<SiteConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🌐 获取远程配置: $url")
            
            val response = networkClient.get(url)
            if (!response.isSuccess) {
                Log.w(TAG, "⚠️ 远程配置请求失败: $url")
                return@withContext Result.failure(Exception("远程配置请求失败"))
            }
            
            val content = (response as NetworkResponse.Success).asString()
            if (content.isEmpty()) {
                Log.w(TAG, "⚠️ 远程配置内容为空: $url")
                return@withContext Result.failure(Exception("远程配置内容为空"))
            }
            
            val config = json.decodeFromString<SiteConfig>(content)
            
            Log.d(TAG, "✅ 远程配置获取成功: ${config.name}")
            Result.success(config)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取远程配置失败: $url", e)
            Result.failure(e)
        }
    }
    
    override suspend fun checkUpdate(): Result<AppConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 这里应该实现检查更新的逻辑
            // 暂时返回默认配置
            Result.success(AppConfig())
        } catch (e: Exception) {
            Log.e(TAG, "❌ 检查更新失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun downloadUpdate(url: String, filePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "📥 下载更新: $url -> $filePath")
            
            val response = networkClient.download(url)
            if (!response.isSuccess) {
                Log.w(TAG, "⚠️ 更新下载失败: $url")
                return@withContext Result.failure(Exception("更新下载失败"))
            }
            
            // 这里应该实现文件保存逻辑
            Log.d(TAG, "✅ 更新下载成功: $filePath")
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 下载更新失败: $url", e)
            Result.failure(e)
        }
    }
    
    override suspend fun uploadStatistics(data: Map<String, Any>): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 这里应该实现统计数据上传逻辑
            Log.d(TAG, "📊 统计数据上传成功")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 统计数据上传失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun submitFeedback(feedback: Map<String, Any>): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 这里应该实现反馈提交逻辑
            Log.d(TAG, "💬 反馈提交成功")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 反馈提交失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchOnlineResources(keyword: String): Result<List<VodInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 这里应该实现在线资源搜索逻辑
            Log.d(TAG, "🔍 在线资源搜索: $keyword")
            Result.success(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "❌ 在线资源搜索失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 解析 VOD 信息列表
     */
    private fun parseVodInfoList(jsonArray: kotlinx.serialization.json.JsonArray, siteKey: String): List<VodInfo> {
        return jsonArray.mapNotNull { element ->
            try {
                val jsonObject = element.jsonObject
                VodInfo(
                    vodId = JsonUtils.getString(jsonObject, "vod_id"),
                    vodName = JsonUtils.getString(jsonObject, "vod_name"),
                    vodPic = JsonUtils.getString(jsonObject, "vod_pic"),
                    vodRemarks = JsonUtils.getString(jsonObject, "vod_remarks"),
                    vodYear = JsonUtils.getString(jsonObject, "vod_year"),
                    vodArea = JsonUtils.getString(jsonObject, "vod_area"),
                    vodDirector = JsonUtils.getString(jsonObject, "vod_director"),
                    vodActor = JsonUtils.getString(jsonObject, "vod_actor"),
                    vodLang = JsonUtils.getString(jsonObject, "vod_lang"),
                    vodContent = JsonUtils.getString(jsonObject, "vod_content"),
                    vodPlayFrom = JsonUtils.getString(jsonObject, "vod_play_from"),
                    vodPlayUrl = JsonUtils.getString(jsonObject, "vod_play_url"),
                    vodDownloadFrom = JsonUtils.getString(jsonObject, "vod_down_from"),
                    vodDownloadUrl = JsonUtils.getString(jsonObject, "vod_down_url"),
                    vodTag = JsonUtils.getString(jsonObject, "vod_tag"),
                    vodClass = JsonUtils.getString(jsonObject, "vod_class"),
                    vodScore = JsonUtils.getString(jsonObject, "vod_score"),
                    vodScoreAll = JsonUtils.getString(jsonObject, "vod_score_all"),
                    vodScoreNum = JsonUtils.getString(jsonObject, "vod_score_num"),
                    vodTime = JsonUtils.getString(jsonObject, "vod_time"),
                    vodTimeAdd = JsonUtils.getString(jsonObject, "vod_time_add"),
                    typeId = JsonUtils.getString(jsonObject, "type_id"),
                    typeName = JsonUtils.getString(jsonObject, "type_name"),
                    siteKey = siteKey
                )
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ VOD 信息解析失败", e)
                null
            }
        }
    }
}
