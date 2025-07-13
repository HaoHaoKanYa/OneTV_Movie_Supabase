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
 * Cokemv 专用解析器
 * 
 * 基于 FongMi/TV 的 csp_Cokemv 实现
 * 专门用于解析 Cokemv 影视站点
 * 
 * 功能：
 * - Cokemv 特定的页面结构解析
 * - 自定义的搜索机制
 * - 特殊的播放链接处理
 * - 防盗链处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class CokemvSpider : SpecializedSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_COKEMV_SPIDER"
        
        // Cokemv 特定的路径
        private const val PATH_HOME = "/"
        private const val PATH_CATEGORY = "/vodshow"
        private const val PATH_DETAIL = "/voddetail"
        private const val PATH_SEARCH = "/vodsearch"
        private const val PATH_PLAY = "/vodplay"
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 Cokemv 解析器初始化")
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 Cokemv 获取首页内容, filter=$filter")
            
            val homeUrl = buildSpecializedUrl(PATH_HOME)
            val response = getWithCache(homeUrl)
            
            val categories = parseCokemvCategories(response)
            
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
            
            logDebug("✅ Cokemv 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Cokemv 首页内容解析失败", e)
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
            logDebug("📋 Cokemv 获取分类内容: tid=$tid, pg=$pg")
            
            val params = mutableMapOf<String, String>()
            params["id"] = tid
            params["page"] = pg
            
            // 添加筛选参数
            extend.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    when (key) {
                        "year" -> params["year"] = value
                        "area" -> params["area"] = value
                        "by" -> params["by"] = value
                        else -> params[key] = value
                    }
                }
            }
            
            val categoryUrl = buildSpecializedUrl(PATH_CATEGORY, params)
            val response = getWithCache(categoryUrl)
            
            val vodData = parseCokemvVideoList(response)
            
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
            
            logDebug("✅ Cokemv 分类内容解析成功，内容数量: ${vodData.items.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Cokemv 分类内容解析失败", e)
            handleErrorResponse(e.message ?: "分类解析失败")
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 Cokemv 获取视频详情: vodId=$vodId")
            
            val params = mapOf("id" to vodId)
            val detailUrl = buildSpecializedUrl(PATH_DETAIL, params)
            val response = getWithCache(detailUrl)
            
            val vodDetail = parseCokemvDetail(response)
            
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
                        put("vod_play_url", formatCokemvPlayUrls(vodDetail.playUrls))
                    })
                })
            }
            
            logDebug("✅ Cokemv 视频详情解析成功: ${vodDetail.name}")
            result
            
        } catch (e: Exception) {
            logError("❌ Cokemv 视频详情解析失败", e)
            handleErrorResponse(e.message ?: "详情解析失败")
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 Cokemv 搜索内容: key=$key, quick=$quick")
            
            val params = mapOf(
                "wd" to URLEncoder.encode(key, "UTF-8")
            )
            
            val searchUrl = buildSpecializedUrl(PATH_SEARCH, params)
            val response = getWithCache(searchUrl, useCache = false)
            
            val searchData = parseCokemvSearchResults(response)
            
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
            
            logDebug("✅ Cokemv 搜索完成，结果数量: ${searchData.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Cokemv 搜索失败", e)
            handleErrorResponse(e.message ?: "搜索失败")
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ Cokemv 获取播放链接: flag=$flag, id=$id")
            
            val params = mapOf(
                "id" to id,
                "sid" to "1",
                "nid" to "1"
            )
            
            val playUrl = buildSpecializedUrl(PATH_PLAY, params)
            val response = getWithCache(playUrl, useCache = false)
            
            val playData = parseCokemvPlayData(response)
            
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
            
            logDebug("✅ Cokemv 播放链接解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ Cokemv 播放链接解析失败", e)
            handleErrorResponse(e.message ?: "播放解析失败")
        }
    }
    
    // ========== Cokemv 特定解析方法 ==========
    
    /**
     * 🏷️ 解析 Cokemv 分类
     */
    private fun parseCokemvCategories(response: String): List<CokemvCategory> {
        return try {
            val categories = mutableListOf<CokemvCategory>()
            
            // 使用 CSS 选择器解析分类
            val categoryElements = extractTextList(response, ".nav-item a")
            
            categoryElements.forEachIndexed { index, name ->
                if (name.isNotEmpty()) {
                    categories.add(CokemvCategory((index + 1).toString(), name))
                }
            }
            
            // 如果没有解析到分类，使用默认分类
            if (categories.isEmpty()) {
                categories.addAll(getDefaultCokemvCategories())
            }
            
            categories
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 分类解析失败", e)
            getDefaultCokemvCategories()
        }
    }
    
    /**
     * 📋 解析 Cokemv 视频列表
     */
    private fun parseCokemvVideoList(response: String): CokemvVideoListData {
        return try {
            val items = mutableListOf<CokemvVideoItem>()
            
            // 解析视频项目
            val videoElements = extractTextList(response, ".video-item")
            
            videoElements.forEach { element ->
                val id = regexFind(element, "href=\"/voddetail/(\\d+)\\.html\"")
                val name = extractText(element, ".title")
                val pic = extractText(element, "img@src")
                val remarks = extractText(element, ".remarks")
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    items.add(CokemvVideoItem(id, name, pic, remarks))
                }
            }
            
            // 解析分页信息
            val pageInfo = parseCokemvPageInfo(response)
            
            CokemvVideoListData(items, pageInfo.page, pageInfo.pageCount, 20, pageInfo.total)
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 视频列表解析失败", e)
            CokemvVideoListData(emptyList(), 1, 1, 20, 0)
        }
    }
    
    /**
     * 🎭 解析 Cokemv 视频详情
     */
    private fun parseCokemvDetail(response: String): CokemvVideoDetail {
        return try {
            val id = regexFind(response, "voddetail/(\\d+)")
            val name = extractText(response, ".video-title")
            val pic = extractText(response, ".video-pic img@src")
            val content = extractText(response, ".video-desc")
            val year = extractText(response, ".year")
            val area = extractText(response, ".area")
            val actor = extractText(response, ".actor")
            val director = extractText(response, ".director")
            
            // 解析播放源和播放列表
            val playFrom = extractTextList(response, ".play-source")
            val playUrls = parseCokemvPlayUrls(response)
            
            CokemvVideoDetail(
                id = id,
                name = name,
                pic = pic,
                content = content,
                year = year,
                area = area,
                actor = actor,
                director = director,
                playFrom = playFrom,
                playUrls = playUrls
            )
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 视频详情解析失败", e)
            CokemvVideoDetail("", "解析失败")
        }
    }
    
    /**
     * 🔍 解析 Cokemv 搜索结果
     */
    private fun parseCokemvSearchResults(response: String): List<CokemvVideoItem> {
        return try {
            val items = mutableListOf<CokemvVideoItem>()
            
            // 解析搜索结果
            val searchElements = extractTextList(response, ".search-item")
            
            searchElements.forEach { element ->
                val id = regexFind(element, "href=\"/voddetail/(\\d+)\\.html\"")
                val name = extractText(element, ".title")
                val pic = extractText(element, "img@src")
                val remarks = extractText(element, ".remarks")
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    items.add(CokemvVideoItem(id, name, pic, remarks))
                }
            }
            
            items
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 搜索结果解析失败", e)
            emptyList()
        }
    }
    
    /**
     * ▶️ 解析 Cokemv 播放数据
     */
    private fun parseCokemvPlayData(response: String): CokemvPlayData {
        return try {
            // 从页面中提取播放链接
            val playUrl = regexFind(response, "\"url\":\"([^\"]+)\"")
            
            CokemvPlayData(
                parse = 0,
                url = playUrl,
                headers = mapOf(
                    "User-Agent" to "Cokemv Player",
                    "Referer" to siteUrl
                )
            )
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 播放数据解析失败", e)
            CokemvPlayData(0, "", emptyMap())
        }
    }
    
    /**
     * 📄 解析 Cokemv 分页信息
     */
    private fun parseCokemvPageInfo(response: String): CokemvPageInfo {
        return try {
            val currentPage = regexFind(response, "当前页:(\\d+)").toIntOrNull() ?: 1
            val totalPages = regexFind(response, "共(\\d+)页").toIntOrNull() ?: 10
            val total = regexFind(response, "共(\\d+)条").toIntOrNull() ?: 200
            
            CokemvPageInfo(currentPage, totalPages, total)
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 分页信息解析失败", e)
            CokemvPageInfo(1, 10, 200)
        }
    }
    
    /**
     * 🎬 解析 Cokemv 播放列表
     */
    private fun parseCokemvPlayUrls(response: String): List<List<Pair<String, String>>> {
        return try {
            val playUrls = mutableListOf<List<Pair<String, String>>>()
            
            // 解析播放列表
            val playLists = extractTextList(response, ".play-list")
            
            playLists.forEach { playList ->
                val episodes = mutableListOf<Pair<String, String>>()
                val episodeElements = extractTextList(playList, "a")
                
                episodeElements.forEach { episode ->
                    val name = extractText(episode, "@text")
                    val url = extractText(episode, "@href")
                    
                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        episodes.add(name to url)
                    }
                }
                
                if (episodes.isNotEmpty()) {
                    playUrls.add(episodes)
                }
            }
            
            playUrls
        } catch (e: Exception) {
            logWarning("⚠️ Cokemv 播放列表解析失败", e)
            emptyList()
        }
    }
    
    /**
     * 🎬 格式化 Cokemv 播放列表
     */
    private fun formatCokemvPlayUrls(playUrls: List<List<Pair<String, String>>>): String {
        return playUrls.joinToString("$$$") { episodes ->
            formatPlayList(episodes)
        }
    }
    
    /**
     * 🔧 获取默认 Cokemv 分类
     */
    private fun getDefaultCokemvCategories(): List<CokemvCategory> {
        return listOf(
            CokemvCategory("1", "电影"),
            CokemvCategory("2", "电视剧"),
            CokemvCategory("3", "综艺"),
            CokemvCategory("4", "动漫"),
            CokemvCategory("5", "纪录片")
        )
    }
    
    // ========== 配置和工具方法 ==========
    
    override fun parseSpecializedConfig(extend: String): SpecializedConfig {
        return SpecializedConfig(
            siteName = "Cokemv影视",
            baseUrl = siteUrl,
            apiVersion = "1.0",
            timeout = 15,
            enableCache = true,
            customHeaders = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language" to "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3"
            )
        )
    }
    
    override fun createDefaultConfig(): SpecializedConfig {
        return parseSpecializedConfig("")
    }
    
    override fun getSpecializedHeaders(): Map<String, String> {
        return siteConfig?.customHeaders ?: mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "User-Agent" to "Cokemv Spider/1.0"
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

// ========== Cokemv 数据模型 ==========

data class CokemvCategory(
    val id: String,
    val name: String
)

data class CokemvVideoItem(
    val id: String,
    val name: String,
    val pic: String,
    val remarks: String
)

data class CokemvVideoListData(
    val items: List<CokemvVideoItem>,
    val page: Int,
    val pageCount: Int,
    val limit: Int,
    val total: Int
)

data class CokemvVideoDetail(
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

data class CokemvPlayData(
    val parse: Int,
    val url: String,
    val headers: Map<String, String>
)

data class CokemvPageInfo(
    val page: Int,
    val pageCount: Int,
    val total: Int
)
