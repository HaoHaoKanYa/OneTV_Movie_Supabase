package top.cywin.onetv.film.spider.appys

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.network.OkHttpManager
import top.cywin.onetv.film.utils.UrlUtils
import java.net.URLEncoder

/**
 * AppYs 接口解析器
 * 
 * 基于 FongMi/TV 的 AppYs 标准接口实现
 * 支持标准的 AppYs API 调用和数据解析
 * 
 * AppYs 接口标准：
 * - ac=list: 获取分类列表
 * - ac=videolist: 获取视频列表
 * - ac=detail: 获取视频详情
 * - ac=search: 搜索视频
 * 
 * 功能：
 * - 标准 AppYs API 调用
 * - JSON 数据解析
 * - 分页处理
 * - 搜索功能
 * - 播放链接解析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class AppYsSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_APPYS_SPIDER"
    }
    
    // HTTP 管理器
    protected val httpManager = OkHttpManager()
    
    // JSON 解析器
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 AppYs 解析器初始化")
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 AppYs 获取首页内容, filter=$filter")
            
            // 调用 AppYs 分类接口
            val apiUrl = buildApiUrl("list")
            val response = httpManager.getString(apiUrl, siteHeaders)
            
            // 解析分类数据
            val categories = parseAppYsCategories(response)
            
            val result = buildJsonResponse {
                put("class", buildJsonArray {
                    categories.forEach { category ->
                        add(buildJsonObject {
                            put("type_id", category.typeId)
                            put("type_name", category.typeName)
                        })
                    }
                })
            }
            
            logDebug("✅ AppYs 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ AppYs 首页内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 AppYs 获取分类内容: tid=$tid, pg=$pg, filter=$filter")
            
            val page = pg.toIntOrNull() ?: 1
            
            // 构建 AppYs 视频列表接口 URL
            val apiUrl = buildVideoListUrl(tid, page, extend)
            val response = httpManager.getString(apiUrl, siteHeaders)
            
            // 解析视频列表数据
            val vodData = parseAppYsVideoList(response)
            
            val result = buildJsonResponse {
                put("list", buildJsonArray {
                    vodData.list.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.vodId)
                            put("vod_name", item.vodName)
                            put("vod_pic", item.vodPic)
                            put("vod_remarks", item.vodRemarks)
                        })
                    }
                })
                put("page", vodData.page)
                put("pagecount", vodData.pagecount)
                put("limit", vodData.limit)
                put("total", vodData.total)
            }
            
            logDebug("✅ AppYs 分类内容解析成功，内容数量: ${vodData.list.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ AppYs 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 AppYs 获取视频详情: vodId=$vodId")
            
            // 构建 AppYs 详情接口 URL
            val apiUrl = buildDetailUrl(vodId)
            val response = httpManager.getString(apiUrl, siteHeaders)
            
            // 解析视频详情数据
            val vodDetail = parseAppYsDetail(response)
            
            val result = buildJsonResponse {
                put("list", buildJsonArray {
                    add(buildJsonObject {
                        put("vod_id", vodDetail.vodId)
                        put("vod_name", vodDetail.vodName)
                        put("vod_pic", vodDetail.vodPic)
                        put("vod_content", vodDetail.vodContent)
                        put("vod_year", vodDetail.vodYear)
                        put("vod_area", vodDetail.vodArea)
                        put("vod_actor", vodDetail.vodActor)
                        put("vod_director", vodDetail.vodDirector)
                        put("vod_play_from", vodDetail.vodPlayFrom)
                        put("vod_play_url", vodDetail.vodPlayUrl)
                    })
                })
            }
            
            logDebug("✅ AppYs 视频详情解析成功: ${vodDetail.vodName}")
            result
            
        } catch (e: Exception) {
            logError("❌ AppYs 视频详情解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 AppYs 搜索内容: key=$key, quick=$quick")
            
            // 构建 AppYs 搜索接口 URL
            val apiUrl = buildSearchUrl(key)
            val response = httpManager.getString(apiUrl, siteHeaders)
            
            // 解析搜索结果数据
            val searchData = parseAppYsSearch(response)
            
            val result = buildJsonResponse {
                put("list", buildJsonArray {
                    searchData.list.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.vodId)
                            put("vod_name", item.vodName)
                            put("vod_pic", item.vodPic)
                            put("vod_remarks", item.vodRemarks)
                        })
                    }
                })
            }
            
            logDebug("✅ AppYs 搜索完成，结果数量: ${searchData.list.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ AppYs 搜索失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ AppYs 获取播放链接: flag=$flag, id=$id")
            
            // AppYs 接口中播放链接通常直接在详情中提供
            // 如果需要额外解析，可以调用播放接口
            val playUrl = parseAppYsPlayUrl(id, flag)
            
            val result = buildJsonResponse {
                put("parse", 0)
                put("playUrl", playUrl)
                put("url", playUrl)
                put("header", buildJsonObject {
                    siteHeaders.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }
            
            logDebug("✅ AppYs 播放链接解析成功: $playUrl")
            result
            
        } catch (e: Exception) {
            logError("❌ AppYs 播放链接解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== AppYs API 构建方法 ==========
    
    /**
     * 🔗 构建 API URL
     */
    protected open fun buildApiUrl(action: String): String {
        return UrlUtils.addParam(siteUrl, "ac", action)
    }
    
    /**
     * 🔗 构建视频列表 URL
     */
    protected open fun buildVideoListUrl(tid: String, page: Int, extend: HashMap<String, String>): String {
        var url = buildApiUrl("videolist")
        url = UrlUtils.addParam(url, "t", tid)
        url = UrlUtils.addParam(url, "pg", page.toString())
        
        // 添加扩展参数
        extend.forEach { (key, value) ->
            if (value.isNotEmpty()) {
                url = UrlUtils.addParam(url, key, value)
            }
        }
        
        return url
    }
    
    /**
     * 🔗 构建详情 URL
     */
    protected open fun buildDetailUrl(vodId: String): String {
        var url = buildApiUrl("detail")
        url = UrlUtils.addParam(url, "ids", vodId)
        return url
    }
    
    /**
     * 🔗 构建搜索 URL
     */
    protected open fun buildSearchUrl(keyword: String): String {
        var url = buildApiUrl("search")
        url = UrlUtils.addParam(url, "wd", URLEncoder.encode(keyword, "UTF-8"))
        return url
    }
    
    // ========== AppYs 数据解析方法 ==========
    
    /**
     * 🏷️ 解析 AppYs 分类数据
     */
    protected open fun parseAppYsCategories(response: String): List<AppYsCategory> {
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val classList = jsonResponse["class"]?.jsonArray ?: return emptyList()
            
            classList.mapNotNull { element ->
                val categoryObj = element.jsonObject
                val typeId = categoryObj["type_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val typeName = categoryObj["type_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                
                AppYsCategory(typeId, typeName)
            }
        } catch (e: Exception) {
            logWarning("⚠️ AppYs 分类数据解析失败", e)
            emptyList()
        }
    }
    
    /**
     * 📋 解析 AppYs 视频列表数据
     */
    protected open fun parseAppYsVideoList(response: String): AppYsVideoListData {
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            
            val list = jsonResponse["list"]?.jsonArray?.mapNotNull { element ->
                val vodObj = element.jsonObject
                AppYsVodItem(
                    vodId = vodObj["vod_id"]?.jsonPrimitive?.content ?: "",
                    vodName = vodObj["vod_name"]?.jsonPrimitive?.content ?: "",
                    vodPic = vodObj["vod_pic"]?.jsonPrimitive?.content ?: "",
                    vodRemarks = vodObj["vod_remarks"]?.jsonPrimitive?.content ?: ""
                )
            } ?: emptyList()
            
            AppYsVideoListData(
                list = list,
                page = jsonResponse["page"]?.jsonPrimitive?.intOrNull ?: 1,
                pagecount = jsonResponse["pagecount"]?.jsonPrimitive?.intOrNull ?: 1,
                limit = jsonResponse["limit"]?.jsonPrimitive?.intOrNull ?: 20,
                total = jsonResponse["total"]?.jsonPrimitive?.intOrNull ?: 0
            )
        } catch (e: Exception) {
            logWarning("⚠️ AppYs 视频列表数据解析失败", e)
            AppYsVideoListData(emptyList(), 1, 1, 20, 0)
        }
    }
    
    /**
     * 🎭 解析 AppYs 视频详情数据
     */
    protected open fun parseAppYsDetail(response: String): AppYsVodDetail {
        return try {
            val jsonResponse = json.parseToJsonElement(response).jsonObject
            val list = jsonResponse["list"]?.jsonArray ?: return AppYsVodDetail("", "解析失败")
            
            if (list.isEmpty()) return AppYsVodDetail("", "无数据")
            
            val vodObj = list[0].jsonObject
            
            AppYsVodDetail(
                vodId = vodObj["vod_id"]?.jsonPrimitive?.content ?: "",
                vodName = vodObj["vod_name"]?.jsonPrimitive?.content ?: "",
                vodPic = vodObj["vod_pic"]?.jsonPrimitive?.content ?: "",
                vodContent = vodObj["vod_content"]?.jsonPrimitive?.content ?: "",
                vodYear = vodObj["vod_year"]?.jsonPrimitive?.content ?: "",
                vodArea = vodObj["vod_area"]?.jsonPrimitive?.content ?: "",
                vodActor = vodObj["vod_actor"]?.jsonPrimitive?.content ?: "",
                vodDirector = vodObj["vod_director"]?.jsonPrimitive?.content ?: "",
                vodPlayFrom = vodObj["vod_play_from"]?.jsonPrimitive?.content ?: "",
                vodPlayUrl = vodObj["vod_play_url"]?.jsonPrimitive?.content ?: ""
            )
        } catch (e: Exception) {
            logWarning("⚠️ AppYs 视频详情数据解析失败", e)
            AppYsVodDetail("", "解析失败")
        }
    }
    
    /**
     * 🔍 解析 AppYs 搜索数据
     */
    protected open fun parseAppYsSearch(response: String): AppYsVideoListData {
        // 搜索结果格式与视频列表相同
        return parseAppYsVideoList(response)
    }
    
    /**
     * ▶️ 解析 AppYs 播放链接
     */
    protected open suspend fun parseAppYsPlayUrl(id: String, flag: String): String {
        return try {
            // 如果 id 本身就是播放链接
            if (id.startsWith("http")) {
                id
            } else {
                // 需要进一步解析或处理
                logDebug("🔧 解析播放链接: id=$id, flag=$flag")
                id
            }
        } catch (e: Exception) {
            logWarning("⚠️ AppYs 播放链接解析失败", e)
            id
        }
    }
}

// ========== AppYs 数据模型 ==========

/**
 * AppYs 分类数据模型
 */
data class AppYsCategory(
    val typeId: String,
    val typeName: String
)

/**
 * AppYs 视频项数据模型
 */
data class AppYsVodItem(
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val vodRemarks: String
)

/**
 * AppYs 视频列表数据模型
 */
data class AppYsVideoListData(
    val list: List<AppYsVodItem>,
    val page: Int,
    val pagecount: Int,
    val limit: Int,
    val total: Int
)

/**
 * AppYs 视频详情数据模型
 */
data class AppYsVodDetail(
    val vodId: String,
    val vodName: String,
    val vodPic: String = "",
    val vodContent: String = "",
    val vodYear: String = "",
    val vodArea: String = "",
    val vodActor: String = "",
    val vodDirector: String = "",
    val vodPlayFrom: String = "",
    val vodPlayUrl: String = ""
)
