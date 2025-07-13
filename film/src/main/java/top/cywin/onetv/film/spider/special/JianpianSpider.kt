package top.cywin.onetv.film.spider.special

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * 简片特殊解析器
 * 
 * 基于 FongMi/TV 的简片站点特殊解析实现
 * 针对简片站点的特殊数据格式和API进行解析
 * 
 * 功能：
 * - 简片站点首页解析
 * - 简片分类内容解析
 * - 简片详情页解析
 * - 简片搜索功能
 * - 简片播放链接解析
 * - 简片特殊格式处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JianpianSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_JIANPIAN_SPIDER"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    private lateinit var networkClient: NetworkClient
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(15000L)
            .build()
        
        Log.d(TAG, "🕷️ JianpianSpider 初始化完成")
    }
    
    override fun getName(): String = "简片"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 3 // 特殊类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = true
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取简片首页内容")
        
        return try {
            val url = "$api/api/home"
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
        Log.d(TAG, "📂 获取简片分类内容: tid=$tid, pg=$pg")
        
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
        Log.d(TAG, "🔍 简片搜索: key=$key, quick=$quick")
        
        return try {
            val encodedKey = StringUtils.urlEncode(key)
            val url = "$api/api/search?keyword=$encodedKey"
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
        Log.d(TAG, "📄 获取简片详情: ids=$ids")
        
        return try {
            if (ids.isEmpty()) {
                return createEmptyResult()
            }
            
            val id = ids[0]
            val url = "$api/api/detail?id=$id"
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
        Log.d(TAG, "🎬 获取简片播放内容: flag=$flag, id=$id")
        
        return try {
            // 简片需要特殊的播放链接处理
            val playUrl = processPlayUrl(flag, id)
            
            JsonUtils.createJsonObject(mapOf(
                "parse" to 1, // 需要解析
                "playUrl" to "",
                "url" to playUrl,
                "header" to mapOf(
                    "User-Agent" to USER_AGENT
                )
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
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            val categories = JsonUtils.getJsonArray(data, "categories") ?: emptyList()
            
            val classes = mutableListOf<Map<String, Any>>()
            
            categories.forEach { category ->
                if (category is Map<*, *>) {
                    val typeId = category["id"]?.toString() ?: ""
                    val typeName = category["name"]?.toString() ?: ""
                    
                    if (typeId.isNotEmpty() && typeName.isNotEmpty()) {
                        classes.add(mapOf(
                            "type_id" to typeId,
                            "type_name" to typeName
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
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            val list = JsonUtils.getJsonArray(data, "list") ?: emptyList()
            val vodList = mutableListOf<Map<String, Any>>()
            
            list.forEach { item ->
                if (item is Map<*, *>) {
                    val vodItem = mapOf(
                        "vod_id" to (item["id"]?.toString() ?: ""),
                        "vod_name" to (item["title"]?.toString() ?: ""),
                        "vod_pic" to (item["poster"]?.toString() ?: ""),
                        "vod_remarks" to (item["remarks"]?.toString() ?: "")
                    )
                    vodList.add(vodItem)
                }
            }
            
            val pagination = JsonUtils.getJsonObject(data, "pagination")
            val currentPage = JsonUtils.getInt(pagination, "current_page") ?: 1
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
            
            val data = JsonUtils.getJsonObject(jsonObject, "data")
            if (data == null) {
                Log.w(TAG, "⚠️ 详情数据为空")
                return createEmptyResult()
            }
            
            // 解析播放源
            val playFromList = mutableListOf<String>()
            val playUrlList = mutableListOf<String>()
            
            val playData = JsonUtils.getJsonArray(data, "play_data") ?: emptyList()
            playData.forEach { playItem ->
                if (playItem is Map<*, *>) {
                    val source = playItem["source"]?.toString() ?: ""
                    val episodes = JsonUtils.getJsonArray(playItem, "episodes") ?: emptyList()
                    
                    if (source.isNotEmpty()) {
                        playFromList.add(source)
                        
                        val episodeUrls = mutableListOf<String>()
                        episodes.forEach { episode ->
                            if (episode is Map<*, *>) {
                                val name = episode["name"]?.toString() ?: ""
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
                "vod_area" to (data["area"]?.toString() ?: ""),
                "vod_remarks" to (data["remarks"]?.toString() ?: ""),
                "vod_actor" to (data["actor"]?.toString() ?: ""),
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
        val baseUrl = "$api/api/category?id=$tid&page=$pg"
        
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
            // 简片的播放链接可能需要特殊处理
            when {
                id.startsWith("http") -> id
                id.contains("m3u8") -> id
                else -> "$api/api/play?flag=$flag&id=$id"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 处理播放链接失败", e)
            id
        }
    }
    
    /**
     * 📋 获取默认分类
     */
    private fun getDefaultCategories(): List<Map<String, Any>> {
        return listOf(
            mapOf("type_id" to "1", "type_name" to "电影"),
            mapOf("type_id" to "2", "type_name" to "电视剧"),
            mapOf("type_id" to "3", "type_name" to "综艺"),
            mapOf("type_id" to "4", "type_name" to "动漫"),
            mapOf("type_id" to "5", "type_name" to "纪录片")
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
