package top.cywin.onetv.film.spider.special

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder

/**
 * Thunder 特殊解析器
 * 
 * 基于 FongMi/TV 的 csp_Thunder 实现
 * 专门用于解析迅雷相关的资源站点
 * 
 * 功能：
 * - 迅雷链接处理
 * - 特殊的认证机制
 * - 磁力链接转换
 * - 下载链接解析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ThunderSpider : SpecialSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_THUNDER_SPIDER"
        
        // Thunder 特定的 API 路径
        private const val API_AUTH = "/api/auth"
        private const val API_CATEGORIES = "/api/categories"
        private const val API_LIST = "/api/list"
        private const val API_DETAIL = "/api/detail"
        private const val API_SEARCH = "/api/search"
        private const val API_PARSE = "/api/parse"
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 Thunder 解析器初始化")
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 Thunder 获取首页内容, filter=$filter")
            
            // 首先进行认证
            performAuthentication()
            
            val categoriesUrl = buildThunderUrl(API_CATEGORIES)
            val response = sendAuthRequest(categoriesUrl)
            
            val categories = parseThunderCategories(response)
            
            val result = buildSpecialResponse {
                put("class", buildJsonArray {
                    categories.forEach { category ->
                        add(buildJsonObject {
                            put("type_id", category.id)
                            put("type_name", category.name)
                        })
                    }
                })
            }
            
            logDebug("✅ Thunder 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Thunder 首页内容解析失败", e)
            buildErrorResponse(e.message ?: "首页解析失败")
        }
    }
    
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 Thunder 获取分类内容: tid=$tid, pg=$pg")
            
            val params = mutableMapOf<String, String>()
            params["category"] = tid
            params["page"] = pg
            
            // 添加扩展参数
            extend.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    params[key] = value
                }
            }
            
            val listUrl = buildThunderUrl(API_LIST)
            val response = sendSignedRequest(listUrl, params)
            
            val vodData = parseThunderVideoList(response)
            
            val result = buildSpecialResponse {
                put("list", buildJsonArray {
                    vodData.items.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.id)
                            put("vod_name", item.name)
                            put("vod_pic", processThunderImageUrl(item.pic))
                            put("vod_remarks", item.remarks)
                        })
                    }
                })
                put("page", vodData.page)
                put("pagecount", vodData.pageCount)
                put("limit", vodData.limit)
                put("total", vodData.total)
            }
            
            logDebug("✅ Thunder 分类内容解析成功，内容数量: ${vodData.items.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Thunder 分类内容解析失败", e)
            buildErrorResponse(e.message ?: "分类解析失败")
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 Thunder 获取视频详情: vodId=$vodId")
            
            val params = mapOf("id" to vodId)
            val detailUrl = buildThunderUrl(API_DETAIL)
            val response = sendSignedRequest(detailUrl, params)
            
            val vodDetail = parseThunderDetail(response)
            
            val result = buildSpecialResponse {
                put("list", buildJsonArray {
                    add(buildJsonObject {
                        put("vod_id", vodDetail.id)
                        put("vod_name", vodDetail.name)
                        put("vod_pic", processThunderImageUrl(vodDetail.pic))
                        put("vod_content", decodeSpecialChars(vodDetail.content))
                        put("vod_year", vodDetail.year)
                        put("vod_area", vodDetail.area)
                        put("vod_actor", vodDetail.actor)
                        put("vod_director", vodDetail.director)
                        put("vod_play_from", formatPlayFrom(vodDetail.playFrom))
                        put("vod_play_url", formatThunderPlayUrls(vodDetail.playUrls))
                    })
                })
            }
            
            logDebug("✅ Thunder 视频详情解析成功: ${vodDetail.name}")
            result
            
        } catch (e: Exception) {
            logError("❌ Thunder 视频详情解析失败", e)
            buildErrorResponse(e.message ?: "详情解析失败")
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 Thunder 搜索内容: key=$key, quick=$quick")
            
            val params = mapOf(
                "keyword" to URLEncoder.encode(key, "UTF-8"),
                "quick" to if (quick) "1" else "0"
            )
            
            val searchUrl = buildThunderUrl(API_SEARCH)
            val response = sendSignedRequest(searchUrl, params)
            
            val searchData = parseThunderSearchResults(response)
            
            val result = buildSpecialResponse {
                put("list", buildJsonArray {
                    searchData.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.id)
                            put("vod_name", item.name)
                            put("vod_pic", processThunderImageUrl(item.pic))
                            put("vod_remarks", item.remarks)
                        })
                    }
                })
            }
            
            logDebug("✅ Thunder 搜索完成，结果数量: ${searchData.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ Thunder 搜索失败", e)
            buildErrorResponse(e.message ?: "搜索失败")
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ Thunder 获取播放链接: flag=$flag, id=$id")
            
            val params = mapOf(
                "flag" to flag,
                "id" to id
            )
            
            val parseUrl = buildThunderUrl(API_PARSE)
            val response = sendSignedRequest(parseUrl, params)
            
            val playData = parseThunderPlayData(response)
            
            val result = buildSpecialResponse {
                put("parse", playData.parse)
                put("playUrl", processThunderPlayUrl(playData.url))
                put("url", processThunderPlayUrl(playData.url))
                put("header", buildJsonObject {
                    playData.headers.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }
            
            logDebug("✅ Thunder 播放链接解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ Thunder 播放链接解析失败", e)
            buildErrorResponse(e.message ?: "播放解析失败")
        }
    }
    
    // ========== Thunder 特定方法 ==========
    
    /**
     * 🔐 执行认证
     */
    private suspend fun performAuthentication() {
        try {
            val authUrl = buildThunderUrl(API_AUTH)
            val authParams = generateAuthParams()
            
            val response = sendSignedRequest(authUrl, authParams)
            val authData = parseThunderAuthResponse(response)
            
            // 保存认证信息
            saveSessionData("access_token", authData.accessToken)
            saveSessionData("refresh_token", authData.refreshToken)
            saveSessionData("expires_at", authData.expiresAt.toString())
            
            logDebug("✅ Thunder 认证成功")
        } catch (e: Exception) {
            logError("❌ Thunder 认证失败", e)
            throw e
        }
    }
    
    /**
     * 🔑 生成认证参数
     */
    private fun generateAuthParams(): Map<String, String> {
        val timestamp = generateTimestamp()
        val nonce = generateNonce()
        
        return mapOf(
            "timestamp" to timestamp,
            "nonce" to nonce,
            "device_id" to generateDeviceId()
        )
    }
    
    /**
     * 📱 生成设备ID
     */
    private fun generateDeviceId(): String {
        val deviceInfo = "Thunder_Android_${System.currentTimeMillis()}"
        return md5(deviceInfo)
    }
    
    /**
     * 🔗 构建 Thunder URL
     */
    private fun buildThunderUrl(path: String): String {
        return UrlUtils.buildUrl(siteUrl, path)
    }
    
    /**
     * 🖼️ 处理 Thunder 图片 URL
     */
    private fun processThunderImageUrl(url: String): String {
        return when {
            url.startsWith("thunder://") -> decodeThunderUrl(url)
            url.startsWith("magnet:") -> "" // 磁力链接没有图片
            else -> processImageUrl(url)
        }
    }
    
    /**
     * ▶️ 处理 Thunder 播放 URL
     */
    private fun processThunderPlayUrl(url: String): String {
        return when {
            url.startsWith("thunder://") -> decodeThunderUrl(url)
            url.startsWith("magnet:") -> url // 磁力链接直接返回
            else -> processPlayUrl(url)
        }
    }
    
    /**
     * 🔓 解码迅雷链接
     */
    private fun decodeThunderUrl(thunderUrl: String): String {
        return try {
            val encoded = thunderUrl.removePrefix("thunder://")
            val decoded = base64Decode(encoded)
            
            // 移除 AA 和 ZZ 标记
            if (decoded.startsWith("AA") && decoded.endsWith("ZZ")) {
                decoded.substring(2, decoded.length - 2)
            } else {
                decoded
            }
        } catch (e: Exception) {
            logWarning("⚠️ 迅雷链接解码失败: $thunderUrl", e)
            thunderUrl
        }
    }
    
    // ========== Thunder 解析方法 ==========
    
    /**
     * 🏷️ 解析 Thunder 分类
     */
    private fun parseThunderCategories(response: String): List<ThunderCategory> {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val categories = mutableListOf<ThunderCategory>()
            
            jsonResponse?.get("data")?.jsonArray?.forEach { element ->
                val categoryObj = element.jsonObject
                val id = categoryObj["id"]?.jsonPrimitive?.content ?: ""
                val name = categoryObj["name"]?.jsonPrimitive?.content ?: ""
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    categories.add(ThunderCategory(id, name))
                }
            }
            
            if (categories.isEmpty()) {
                categories.addAll(getDefaultThunderCategories())
            }
            
            categories
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 分类解析失败", e)
            getDefaultThunderCategories()
        }
    }
    
    /**
     * 📋 解析 Thunder 视频列表
     */
    private fun parseThunderVideoList(response: String): ThunderVideoListData {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val items = mutableListOf<ThunderVideoItem>()
            
            jsonResponse?.get("data")?.jsonObject?.get("list")?.jsonArray?.forEach { element ->
                val itemObj = element.jsonObject
                val id = itemObj["id"]?.jsonPrimitive?.content ?: ""
                val name = itemObj["name"]?.jsonPrimitive?.content ?: ""
                val pic = itemObj["pic"]?.jsonPrimitive?.content ?: ""
                val remarks = itemObj["remarks"]?.jsonPrimitive?.content ?: ""
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    items.add(ThunderVideoItem(id, name, pic, remarks))
                }
            }
            
            val page = jsonResponse?.get("data")?.jsonObject?.get("page")?.jsonPrimitive?.intOrNull ?: 1
            val pageCount = jsonResponse?.get("data")?.jsonObject?.get("pagecount")?.jsonPrimitive?.intOrNull ?: 1
            val total = jsonResponse?.get("data")?.jsonObject?.get("total")?.jsonPrimitive?.intOrNull ?: 0
            
            ThunderVideoListData(items, page, pageCount, 20, total)
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 视频列表解析失败", e)
            ThunderVideoListData(emptyList(), 1, 1, 20, 0)
        }
    }
    
    /**
     * 🎭 解析 Thunder 视频详情
     */
    private fun parseThunderDetail(response: String): ThunderVideoDetail {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val detailObj = jsonResponse?.get("data")?.jsonObject
            
            ThunderVideoDetail(
                id = detailObj?.get("id")?.jsonPrimitive?.content ?: "",
                name = detailObj?.get("name")?.jsonPrimitive?.content ?: "",
                pic = detailObj?.get("pic")?.jsonPrimitive?.content ?: "",
                content = detailObj?.get("content")?.jsonPrimitive?.content ?: "",
                year = detailObj?.get("year")?.jsonPrimitive?.content ?: "",
                area = detailObj?.get("area")?.jsonPrimitive?.content ?: "",
                actor = detailObj?.get("actor")?.jsonPrimitive?.content ?: "",
                director = detailObj?.get("director")?.jsonPrimitive?.content ?: "",
                playFrom = listOf("Thunder", "Magnet"),
                playUrls = parseThunderPlayUrls(detailObj)
            )
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 视频详情解析失败", e)
            ThunderVideoDetail("", "解析失败")
        }
    }
    
    /**
     * 🔍 解析 Thunder 搜索结果
     */
    private fun parseThunderSearchResults(response: String): List<ThunderVideoItem> {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val items = mutableListOf<ThunderVideoItem>()
            
            jsonResponse?.get("data")?.jsonArray?.forEach { element ->
                val itemObj = element.jsonObject
                val id = itemObj["id"]?.jsonPrimitive?.content ?: ""
                val name = itemObj["name"]?.jsonPrimitive?.content ?: ""
                val pic = itemObj["pic"]?.jsonPrimitive?.content ?: ""
                val remarks = itemObj["remarks"]?.jsonPrimitive?.content ?: ""
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    items.add(ThunderVideoItem(id, name, pic, remarks))
                }
            }
            
            items
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 搜索结果解析失败", e)
            emptyList()
        }
    }
    
    /**
     * ▶️ 解析 Thunder 播放数据
     */
    private fun parseThunderPlayData(response: String): ThunderPlayData {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val playObj = jsonResponse?.get("data")?.jsonObject
            
            ThunderPlayData(
                parse = playObj?.get("parse")?.jsonPrimitive?.intOrNull ?: 0,
                url = playObj?.get("url")?.jsonPrimitive?.content ?: "",
                headers = mapOf(
                    "User-Agent" to "Thunder Player",
                    "Referer" to siteUrl
                )
            )
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 播放数据解析失败", e)
            ThunderPlayData(0, "", emptyMap())
        }
    }
    
    /**
     * 🔐 解析 Thunder 认证响应
     */
    private fun parseThunderAuthResponse(response: String): ThunderAuthData {
        return try {
            val jsonResponse = deepParseJson(response)?.jsonObject
            val authObj = jsonResponse?.get("data")?.jsonObject
            
            ThunderAuthData(
                accessToken = authObj?.get("access_token")?.jsonPrimitive?.content ?: "",
                refreshToken = authObj?.get("refresh_token")?.jsonPrimitive?.content ?: "",
                expiresAt = authObj?.get("expires_at")?.jsonPrimitive?.longOrNull ?: 0L
            )
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 认证响应解析失败", e)
            ThunderAuthData("", "", 0L)
        }
    }
    
    /**
     * 🎬 解析 Thunder 播放列表
     */
    private fun parseThunderPlayUrls(detailObj: JsonObject?): List<List<Pair<String, String>>> {
        return try {
            val playUrls = mutableListOf<List<Pair<String, String>>>()
            
            // 解析迅雷链接
            val thunderUrls = detailObj?.get("thunder_urls")?.jsonArray
            if (thunderUrls != null) {
                val episodes = mutableListOf<Pair<String, String>>()
                thunderUrls.forEachIndexed { index, element ->
                    val url = element.jsonPrimitive.content
                    episodes.add("第${index + 1}集" to url)
                }
                if (episodes.isNotEmpty()) {
                    playUrls.add(episodes)
                }
            }
            
            // 解析磁力链接
            val magnetUrls = detailObj?.get("magnet_urls")?.jsonArray
            if (magnetUrls != null) {
                val episodes = mutableListOf<Pair<String, String>>()
                magnetUrls.forEachIndexed { index, element ->
                    val url = element.jsonPrimitive.content
                    episodes.add("磁力${index + 1}" to url)
                }
                if (episodes.isNotEmpty()) {
                    playUrls.add(episodes)
                }
            }
            
            playUrls
        } catch (e: Exception) {
            logWarning("⚠️ Thunder 播放列表解析失败", e)
            emptyList()
        }
    }
    
    /**
     * 🎬 格式化 Thunder 播放列表
     */
    private fun formatThunderPlayUrls(playUrls: List<List<Pair<String, String>>>): String {
        return playUrls.joinToString("$$$") { episodes ->
            episodes.joinToString("#") { (name, url) ->
                "$name\$$url"
            }
        }
    }
    
    /**
     * 🔧 获取默认 Thunder 分类
     */
    private fun getDefaultThunderCategories(): List<ThunderCategory> {
        return listOf(
            ThunderCategory("1", "电影"),
            ThunderCategory("2", "电视剧"),
            ThunderCategory("3", "综艺"),
            ThunderCategory("4", "动漫"),
            ThunderCategory("5", "纪录片")
        )
    }
    
    // ========== 配置方法 ==========
    
    override fun parseSpecialConfig(extend: String): SpecialConfig {
        return SpecialConfig(
            siteName = "Thunder迅雷",
            secretKey = "thunder_secret_key_2023",
            apiVersion = "2.0",
            encryptionEnabled = true,
            authRequired = true,
            signatureRequired = true,
            customHeaders = mapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json"
            )
        )
    }
    
    override fun createDefaultSpecialConfig(): SpecialConfig {
        return parseSpecialConfig("")
    }
    
    override suspend fun getAuthInfo(): Map<String, String> {
        val accessToken = getSessionData("access_token") ?: ""
        return if (accessToken.isNotEmpty()) {
            mapOf("Authorization" to "Bearer $accessToken")
        } else {
            emptyMap()
        }
    }
    
    override fun generateDynamicParams(): Map<String, String> {
        return mapOf(
            "timestamp" to generateTimestamp(),
            "nonce" to generateNonce(),
            "version" to "2.0"
        )
    }
}

// ========== Thunder 数据模型 ==========

data class ThunderCategory(val id: String, val name: String)
data class ThunderVideoItem(val id: String, val name: String, val pic: String, val remarks: String)
data class ThunderVideoListData(val items: List<ThunderVideoItem>, val page: Int, val pageCount: Int, val limit: Int, val total: Int)
data class ThunderVideoDetail(val id: String, val name: String, val pic: String = "", val content: String = "", val year: String = "", val area: String = "", val actor: String = "", val director: String = "", val playFrom: List<String> = emptyList(), val playUrls: List<List<Pair<String, String>>> = emptyList())
data class ThunderPlayData(val parse: Int, val url: String, val headers: Map<String, String>)
data class ThunderAuthData(val accessToken: String, val refreshToken: String, val expiresAt: Long)
