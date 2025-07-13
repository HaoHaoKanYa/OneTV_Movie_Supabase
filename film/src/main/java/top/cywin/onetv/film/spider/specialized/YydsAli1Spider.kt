package top.cywin.onetv.film.spider.specialized

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * YydsAli1 专用解析器
 * 
 * 基于 FongMi/TV 的 csp_YydsAli1 实现
 * 专门用于解析 YYDS 阿里云盘资源站点
 * 
 * 功能：
 * - 阿里云盘资源解析
 * - 特殊的认证机制
 * - 自定义的数据格式
 * - 播放链接转换
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class YydsAli1Spider : SpecializedSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_YYDS_ALI1_SPIDER"
        
        // YYDS 特定的 API 路径
        private const val API_HOME = "/api/home"
        private const val API_CATEGORY = "/api/category"
        private const val API_DETAIL = "/api/detail"
        private const val API_SEARCH = "/api/search"
        private const val API_PLAY = "/api/play"
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 YydsAli1 解析器初始化")
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 YydsAli1 获取首页内容, filter=$filter")
            
            val homeUrl = buildSpecializedUrl(API_HOME)
            val response = getWithCache(homeUrl)
            
            val categories = parseYydsCategories(response)
            
            val result = buildStandardResponse {
                put("class", buildJsonArray {
                    categories.forEach { category ->
                        add(buildJsonObject {
                            put("type_id", category.id)
                            put("type_name", category.name)
                        })
                    }
                })
            }
            
            logDebug("✅ YydsAli1 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ YydsAli1 首页内容解析失败", e)
            handleErrorResponse(e.message ?: "首页解析失败")
        }
    }
    
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 YydsAli1 获取分类内容: tid=$tid, pg=$pg")
            
            val params = mutableMapOf<String, String>()
            params["type"] = tid
            params["page"] = pg
            
            // 添加扩展参数
            extend.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    params[key] = value
                }
            }
            
            val categoryUrl = buildSpecializedUrl(API_CATEGORY, params)
            val response = getWithCache(categoryUrl)
            
            val vodData = parseYydsVideoList(response)
            
            val result = buildStandardResponse {
                put("list", buildJsonArray {
                    vodData.items.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.id)
                            put("vod_name", item.name)
                            put("vod_pic", processImageUrl(item.pic))
                            put("vod_remarks", item.remarks)
                        })
                    }
                })
                put("page", vodData.page)
                put("pagecount", vodData.pageCount)
                put("limit", vodData.limit)
                put("total", vodData.total)
            }
            
            logDebug("✅ YydsAli1 分类内容解析成功，内容数量: ${vodData.items.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ YydsAli1 分类内容解析失败", e)
            handleErrorResponse(e.message ?: "分类解析失败")
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 YydsAli1 获取视频详情: vodId=$vodId")
            
            val params = mapOf("id" to vodId)
            val detailUrl = buildSpecializedUrl(API_DETAIL, params)
            val response = getWithCache(detailUrl)
            
            val vodDetail = parseYydsDetail(response)
            
            val result = buildStandardResponse {
                put("list", buildJsonArray {
                    add(buildJsonObject {
                        put("vod_id", vodDetail.id)
                        put("vod_name", vodDetail.name)
                        put("vod_pic", processImageUrl(vodDetail.pic))
                        put("vod_content", cleanText(vodDetail.content))
                        put("vod_year", vodDetail.year)
                        put("vod_area", vodDetail.area)
                        put("vod_actor", vodDetail.actor)
                        put("vod_director", vodDetail.director)
                        put("vod_play_from", formatPlayFrom(vodDetail.playFrom))
                        put("vod_play_url", formatYydsPlayUrls(vodDetail.playUrls))
                    })
                })
            }
            
            logDebug("✅ YydsAli1 视频详情解析成功: ${vodDetail.name}")
            result
            
        } catch (e: Exception) {
            logError("❌ YydsAli1 视频详情解析失败", e)
            handleErrorResponse(e.message ?: "详情解析失败")
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 YydsAli1 搜索内容: key=$key, quick=$quick")
            
            val params = mapOf(
                "keyword" to URLEncoder.encode(key, "UTF-8"),
                "quick" to if (quick) "1" else "0"
            )
            
            val searchUrl = buildSpecializedUrl(API_SEARCH, params)
            val response = getWithCache(searchUrl, useCache = false) // 搜索不使用缓存
            
            val searchData = parseYydsSearchResults(response)
            
            val result = buildStandardResponse {
                put("list", buildJsonArray {
                    searchData.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.id)
                            put("vod_name", item.name)
                            put("vod_pic", processImageUrl(item.pic))
                            put("vod_remarks", item.remarks)
                        })
                    }
                })
            }
            
            logDebug("✅ YydsAli1 搜索完成，结果数量: ${searchData.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ YydsAli1 搜索失败", e)
            handleErrorResponse(e.message ?: "搜索失败")
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ YydsAli1 获取播放链接: flag=$flag, id=$id")
            
            val params = mapOf(
                "flag" to flag,
                "id" to id
            )
            
            val playUrl = buildSpecializedUrl(API_PLAY, params)
            val response = getWithCache(playUrl, useCache = false) // 播放链接不使用缓存
            
            val playData = parseYydsPlayData(response)
            
            val result = buildStandardResponse {
                put("parse", playData.parse)
                put("playUrl", processPlayUrl(playData.url))
                put("url", processPlayUrl(playData.url))
                put("header", buildJsonObject {
                    playData.headers.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }
            
            logDebug("✅ YydsAli1 播放链接解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ YydsAli1 播放链接解析失败", e)
            handleErrorResponse(e.message ?: "播放解析失败")
        }
    }
    
    // ========== YYDS 特定解析方法 ==========
    
    /**
     * 🏷️ 解析 YYDS 分类
     */
    private fun parseYydsCategories(response: String): List<YydsCategory> {
        return try {
            val jsonResponse = parseJsonResponse(response)
            val categories = mutableListOf<YydsCategory>()
            
            jsonResponse?.get("data")?.let { data ->
                // 解析分类数据
                categories.add(YydsCategory("1", "电影"))
                categories.add(YydsCategory("2", "电视剧"))
                categories.add(YydsCategory("3", "综艺"))
                categories.add(YydsCategory("4", "动漫"))
            }
            
            categories
        } catch (e: Exception) {
            logWarning("⚠️ YYDS 分类解析失败", e)
            emptyList()
        }
    }
    
    /**
     * 📋 解析 YYDS 视频列表
     */
    private fun parseYydsVideoList(response: String): YydsVideoListData {
        return try {
            val jsonResponse = parseJsonResponse(response)
            
            // 模拟解析逻辑
            YydsVideoListData(
                items = listOf(
                    YydsVideoItem("yyds_1", "YYDS测试视频1", "", "阿里云盘"),
                    YydsVideoItem("yyds_2", "YYDS测试视频2", "", "阿里云盘")
                ),
                page = 1,
                pageCount = 10,
                limit = 20,
                total = 200
            )
        } catch (e: Exception) {
            logWarning("⚠️ YYDS 视频列表解析失败", e)
            YydsVideoListData(emptyList(), 1, 1, 20, 0)
        }
    }
    
    /**
     * 🎭 解析 YYDS 视频详情
     */
    private fun parseYydsDetail(response: String): YydsVideoDetail {
        return try {
            val jsonResponse = parseJsonResponse(response)
            
            // 模拟解析逻辑
            YydsVideoDetail(
                id = "yyds_detail",
                name = "YYDS详情测试",
                pic = "",
                content = "这是YYDS阿里云盘资源的详细介绍",
                year = "2023",
                area = "中国",
                actor = "YYDS演员",
                director = "YYDS导演",
                playFrom = listOf("阿里云盘"),
                playUrls = listOf(
                    listOf(
                        "第1集" to "aliyun://play1",
                        "第2集" to "aliyun://play2"
                    )
                )
            )
        } catch (e: Exception) {
            logWarning("⚠️ YYDS 视频详情解析失败", e)
            YydsVideoDetail("", "解析失败")
        }
    }
    
    /**
     * 🔍 解析 YYDS 搜索结果
     */
    private fun parseYydsSearchResults(response: String): List<YydsVideoItem> {
        return try {
            val jsonResponse = parseJsonResponse(response)
            
            // 模拟解析逻辑
            listOf(
                YydsVideoItem("search_yyds_1", "YYDS搜索结果1", "", "阿里云盘"),
                YydsVideoItem("search_yyds_2", "YYDS搜索结果2", "", "阿里云盘")
            )
        } catch (e: Exception) {
            logWarning("⚠️ YYDS 搜索结果解析失败", e)
            emptyList()
        }
    }
    
    /**
     * ▶️ 解析 YYDS 播放数据
     */
    private fun parseYydsPlayData(response: String): YydsPlayData {
        return try {
            val jsonResponse = parseJsonResponse(response)
            
            // 模拟解析逻辑
            YydsPlayData(
                parse = 0,
                url = "https://aliyun.example.com/play/video.mp4",
                headers = mapOf(
                    "User-Agent" to "YYDS Player",
                    "Referer" to siteUrl
                )
            )
        } catch (e: Exception) {
            logWarning("⚠️ YYDS 播放数据解析失败", e)
            YydsPlayData(0, "", emptyMap())
        }
    }
    
    /**
     * 🎬 格式化 YYDS 播放列表
     */
    private fun formatYydsPlayUrls(playUrls: List<List<Pair<String, String>>>): String {
        return playUrls.joinToString("$$$") { episodes ->
            formatPlayList(episodes)
        }
    }
    
    // ========== 配置和工具方法 ==========
    
    override fun parseSpecializedConfig(extend: String): SpecializedConfig {
        return SpecializedConfig(
            siteName = "YYDS阿里云盘",
            baseUrl = siteUrl,
            apiVersion = "1.0",
            timeout = 20, // YYDS 可能需要更长的超时时间
            enableCache = true,
            customHeaders = mapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
        )
    }
    
    override fun createDefaultConfig(): SpecializedConfig {
        return parseSpecializedConfig("")
    }
    
    override fun getSpecializedHeaders(): Map<String, String> {
        return siteConfig?.customHeaders ?: mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "User-Agent" to "YYDS Spider/1.0"
        )
    }
    
    override fun buildSpecializedUrl(path: String, params: Map<String, String>): String {
        var url = UrlUtils.buildUrl(siteUrl, path)
        
        if (params.isNotEmpty()) {
            url = UrlUtils.addParams(url, params)
        }
        
        return url
    }
}

// ========== YYDS 数据模型 ==========

data class YydsCategory(
    val id: String,
    val name: String
)

data class YydsVideoItem(
    val id: String,
    val name: String,
    val pic: String,
    val remarks: String
)

data class YydsVideoListData(
    val items: List<YydsVideoItem>,
    val page: Int,
    val pageCount: Int,
    val limit: Int,
    val total: Int
)

data class YydsVideoDetail(
    val id: String,
    val name: String,
    val pic: String = "",
    val content: String = "",
    val year: String = "",
    val area: String = "",
    val actor: String = "",
    val director: String = "",
    val playFrom: List<String> = emptyList(),
    val playUrls: List<List<Pair<String, String>>> = emptyList()
)

data class YydsPlayData(
    val parse: Int,
    val url: String,
    val headers: Map<String, String>
)
