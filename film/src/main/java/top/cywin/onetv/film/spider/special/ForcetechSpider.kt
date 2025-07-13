package top.cywin.onetv.film.spider.special

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * 强制技术特殊解析器
 * 
 * 基于 FongMi/TV 的强制技术站点特殊解析实现
 * 针对强制技术站点的特殊数据格式和技术要求进行解析
 * 
 * 功能：
 * - 强制技术站点首页解析
 * - 强制技术分类内容解析
 * - 强制技术详情页解析
 * - 强制技术搜索功能
 * - 强制技术播放链接解析
 * - 强制技术特殊协议处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ForcetechSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_FORCETECH_SPIDER"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        private const val API_VERSION = "v2"
    }
    
    private lateinit var networkClient: NetworkClient
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(20000L) // 强制技术可能需要更长的超时时间
            .addHeader("Accept", "application/json")
            .build()
        
        Log.d(TAG, "🕷️ ForcetechSpider 初始化完成")
    }
    
    override fun getName(): String = "强制技术"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 3 // 特殊类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = true
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取强制技术首页内容")
        
        return try {
            val url = "$api/$API_VERSION/home"
            val response = networkClient.get(url)
            
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ 首页响应为空")
                return createEmptyResult()
            }
            
            parseHomeContent(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取首页内容失败", e)
            createErrorResult("获取首页失败: ${e.message}")
        }
    }
    
    /**
     * 📂 获取分类内容
     */
    override fun categoryContent(tid: String, pg: String, filter: Boolean, extend: Map<String, String>): String {
        Log.d(TAG, "📂 获取强制技术分类内容: tid=$tid, pg=$pg")
        
        return try {
            val url = buildCategoryUrl(tid, pg, extend)
            val response = networkClient.get(url)
            
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ 分类响应为空")
                return createEmptyResult()
            }
            
            parseCategoryContent(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取分类内容失败", e)
            createErrorResult("获取分类失败: ${e.message}")
        }
    }
    
    /**
     * 🔍 搜索内容
     */
    override fun searchContent(key: String, quick: Boolean): String {
        Log.d(TAG, "🔍 强制技术搜索: key=$key, quick=$quick")
        
        return try {
            val encodedKey = StringUtils.urlEncode(key)
            val url = "$api/$API_VERSION/search?q=$encodedKey&quick=${if (quick) 1 else 0}"
            val response = networkClient.get(url)
            
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ 搜索响应为空")
                return createEmptyResult()
            }
            
            parseSearchContent(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索失败", e)
            createErrorResult("搜索失败: ${e.message}")
        }
    }
    
    /**
     * 📄 获取详情内容
     */
    override fun detailContent(ids: List<String>): String {
        Log.d(TAG, "📄 获取强制技术详情: ids=$ids")
        
        return try {
            if (ids.isEmpty()) {
                return createEmptyResult()
            }
            
            val id = ids[0]
            val url = "$api/$API_VERSION/detail/$id"
            val response = networkClient.get(url)
            
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ 详情响应为空")
                return createEmptyResult()
            }
            
            parseDetailContent(response)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取详情失败", e)
            createErrorResult("获取详情失败: ${e.message}")
        }
    }
    
    /**
     * 🎬 获取播放内容
     */
    override fun playerContent(flag: String, id: String, vipFlags: List<String>): String {
        Log.d(TAG, "🎬 获取强制技术播放内容: flag=$flag, id=$id")
        
        return try {
            // 强制技术可能需要特殊的播放链接处理
            val playUrl = processPlayUrl(flag, id)
            val needParse = determineParseMode(flag, playUrl)
            
            JsonUtils.createJsonObject(mapOf(
                "parse" to if (needParse) 1 else 0,
                "playUrl" to "",
                "url" to playUrl,
                "header" to mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to api
                ),
                "flag" to flag
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放内容失败", e)
            createErrorResult("获取播放内容失败: ${e.message}")
        }
    }
    
    /**
     * 🏠 解析首页内容
     */
    private fun parseHomeContent(response: String): String {
        return try {
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 首页 JSON 解析失败")
                return createEmptyResult()
            }
            
            val success = JsonUtils.getBoolean(jsonObject, "success") ?: false
            if (!success) {
                Log.w(TAG, "⚠️ 首页请求不成功")
                return createEmptyResult()
            }
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            val categories = JsonUtils.getJsonArray(data, "categories") ?: emptyList()
            
            val classes = mutableListOf<Map<String, Any>>()
            
            categories.forEach { category ->
                if (category is Map<*, *>) {
                    val typeId = category["id"]?.toString() ?: ""
                    val typeName = category["name"]?.toString() ?: ""
                    val typeFlag = category["flag"]?.toString() ?: ""
                    
                    if (typeId.isNotEmpty() && typeName.isNotEmpty()) {
                        classes.add(mapOf(
                            "type_id" to typeId,
                            "type_name" to typeName,
                            "type_flag" to typeFlag
                        ))
                    }
                }
            }
            
            // 如果没有分类，添加默认分类
            if (classes.isEmpty()) {
                classes.addAll(getDefaultCategories())
            }
            
            JsonUtils.createJsonObject(mapOf(
                "class" to classes
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析首页内容失败", e)
            createErrorResult("解析首页失败: ${e.message}")
        }
    }
    
    /**
     * 📂 解析分类内容
     */
    private fun parseCategoryContent(response: String): String {
        return try {
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 分类 JSON 解析失败")
                return createEmptyResult()
            }
            
            val success = JsonUtils.getBoolean(jsonObject, "success") ?: false
            if (!success) {
                Log.w(TAG, "⚠️ 分类请求不成功")
                return createEmptyResult()
            }
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            val list = JsonUtils.getJsonArray(data, "items") ?: emptyList()
            val vodList = mutableListOf<Map<String, Any>>()
            
            list.forEach { item ->
                if (item is Map<*, *>) {
                    val vodItem = mapOf(
                        "vod_id" to (item["id"]?.toString() ?: ""),
                        "vod_name" to (item["title"]?.toString() ?: ""),
                        "vod_pic" to (item["poster"]?.toString() ?: ""),
                        "vod_remarks" to (item["status"]?.toString() ?: ""),
                        "vod_year" to (item["year"]?.toString() ?: ""),
                        "vod_area" to (item["region"]?.toString() ?: ""),
                        "vod_actor" to (item["actors"]?.toString() ?: ""),
                        "vod_director" to (item["director"]?.toString() ?: "")
                    )
                    vodList.add(vodItem)
                }
            }
            
            val pagination = JsonUtils.getJsonObject(data, "pagination")
            val currentPage = JsonUtils.getInt(pagination, "current") ?: 1
            val totalPages = JsonUtils.getInt(pagination, "total_pages") ?: 1
            val total = JsonUtils.getInt(pagination, "total") ?: vodList.size
            
            JsonUtils.createJsonObject(mapOf(
                "list" to vodList,
                "page" to currentPage,
                "pagecount" to totalPages,
                "limit" to 20,
                "total" to total
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析分类内容失败", e)
            createErrorResult("解析分类失败: ${e.message}")
        }
    }
    
    /**
     * 🔍 解析搜索内容
     */
    private fun parseSearchContent(response: String): String {
        return parseCategoryContent(response) // 搜索结果格式与分类相同
    }
    
    /**
     * 📄 解析详情内容
     */
    private fun parseDetailContent(response: String): String {
        return try {
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ 详情 JSON 解析失败")
                return createEmptyResult()
            }
            
            val success = JsonUtils.getBoolean(jsonObject, "success") ?: false
            if (!success) {
                Log.w(TAG, "⚠️ 详情请求不成功")
                return createEmptyResult()
            }
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            if (data == null) {
                Log.w(TAG, "⚠️ 详情数据为空")
                return createEmptyResult()
            }
            
            // 解析播放源
            val playFromList = mutableListOf<String>()
            val playUrlList = mutableListOf<String>()
            
            val playData = JsonUtils.getJsonArray(data, "play_sources") ?: emptyList()
            playData.forEach { playItem ->
                if (playItem is Map<*, *>) {
                    val source = playItem["name"]?.toString() ?: ""
                    val episodes = JsonUtils.getJsonArray(playItem, "episodes") ?: emptyList()
                    
                    if (source.isNotEmpty()) {
                        playFromList.add(source)
                        
                        val episodeUrls = mutableListOf<String>()
                        episodes.forEach { episode ->
                            if (episode is Map<*, *>) {
                                val name = episode["title"]?.toString() ?: ""
                                val url = episode["url"]?.toString() ?: ""
                                if (name.isNotEmpty() && url.isNotEmpty()) {
                                    episodeUrls.add("$name\$$url")
                                }
                            }
                        }
                        playUrlList.add(episodeUrls.joinToString("#"))
                    }
                }
            }
            
            val detailItem = mapOf(
                "vod_id" to (data["id"]?.toString() ?: ""),
                "vod_name" to (data["title"]?.toString() ?: ""),
                "vod_pic" to (data["poster"]?.toString() ?: ""),
                "type_name" to (data["category"]?.toString() ?: ""),
                "vod_year" to (data["year"]?.toString() ?: ""),
                "vod_area" to (data["region"]?.toString() ?: ""),
                "vod_remarks" to (data["status"]?.toString() ?: ""),
                "vod_actor" to (data["actors"]?.toString() ?: ""),
                "vod_director" to (data["director"]?.toString() ?: ""),
                "vod_content" to (data["description"]?.toString() ?: ""),
                "vod_play_from" to playFromList.joinToString("$$$"),
                "vod_play_url" to playUrlList.joinToString("$$$")
            )
            
            JsonUtils.createJsonObject(mapOf(
                "list" to listOf(detailItem)
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析详情内容失败", e)
            createErrorResult("解析详情失败: ${e.message}")
        }
    }
    
    /**
     * 🔗 构建分类URL
     */
    private fun buildCategoryUrl(tid: String, pg: String, extend: Map<String, String>): String {
        val baseUrl = "$api/$API_VERSION/category/$tid?page=$pg"
        
        if (extend.isEmpty()) {
            return baseUrl
        }
        
        val params = mutableListOf<String>()
        extend.forEach { (key, value) ->
            if (value.isNotEmpty()) {
                params.add("$key=${StringUtils.urlEncode(value)}")
            }
        }
        
        return if (params.isNotEmpty()) {
            "$baseUrl&${params.joinToString("&")}"
        } else {
            baseUrl
        }
    }
    
    /**
     * 🎬 处理播放链接
     */
    private fun processPlayUrl(flag: String, id: String): String {
        return try {
            when {
                id.startsWith("http") -> id
                id.contains("m3u8") -> id
                id.contains("mp4") -> id
                else -> "$api/$API_VERSION/play?flag=$flag&id=${StringUtils.urlEncode(id)}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 处理播放链接失败", e)
            id
        }
    }
    
    /**
     * 🎯 确定解析模式
     */
    private fun determineParseMode(flag: String, playUrl: String): Boolean {
        return when {
            playUrl.contains("m3u8") -> false // 直播流不需要解析
            playUrl.contains("mp4") -> false // 直接视频不需要解析
            playUrl.startsWith("http") && (playUrl.contains("youku") || playUrl.contains("iqiyi") || playUrl.contains("qq")) -> true // VIP站点需要解析
            else -> false
        }
    }
    
    /**
     * 📋 获取默认分类
     */
    private fun getDefaultCategories(): List<Map<String, Any>> {
        return listOf(
            mapOf("type_id" to "1", "type_name" to "电影", "type_flag" to "movie"),
            mapOf("type_id" to "2", "type_name" to "电视剧", "type_flag" to "tv"),
            mapOf("type_id" to "3", "type_name" to "综艺", "type_flag" to "variety"),
            mapOf("type_id" to "4", "type_name" to "动漫", "type_flag" to "anime"),
            mapOf("type_id" to "5", "type_name" to "纪录片", "type_flag" to "documentary")
        )
    }
    
    /**
     * 📝 创建空结果
     */
    private fun createEmptyResult(): String {
        return JsonUtils.createJsonObject(mapOf(
            "list" to emptyList<Any>(),
            "class" to emptyList<Any>()
        ))
    }
    
    /**
     * ❌ 创建错误结果
     */
    private fun createErrorResult(message: String): String {
        return JsonUtils.createJsonObject(mapOf(
            "error" to message,
            "list" to emptyList<Any>(),
            "class" to emptyList<Any>()
        ))
    }
}
