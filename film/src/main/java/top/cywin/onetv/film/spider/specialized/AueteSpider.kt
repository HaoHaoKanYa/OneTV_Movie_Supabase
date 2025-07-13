package top.cywin.onetv.film.spider.specialized

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * Auete 专用解析器
 * 
 * 基于 FongMi/TV 的 Auete 站点专用解析实现
 * 针对 Auete 站点的特殊数据格式和接口进行优化
 * 
 * 功能：
 * - Auete 站点首页解析
 * - Auete 分类内容解析
 * - Auete 详情页解析
 * - Auete 搜索功能
 * - Auete 播放链接解析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class AueteSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_AUETE_SPIDER"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    
    private lateinit var networkClient: NetworkClient
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(15000L)
            .build()
        
        Log.d(TAG, "🕷️ AueteSpider 初始化完成")
    }
    
    override fun getName(): String = "Auete"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 1 // JSON 类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = true
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取 Auete 首页内容")
        
        return try {
            val url = "$api/api.php/provide/vod/?ac=list"
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
        Log.d(TAG, "📂 获取 Auete 分类内容: tid=$tid, pg=$pg")
        
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
        Log.d(TAG, "🔍 Auete 搜索: key=$key, quick=$quick")
        
        return try {
            val encodedKey = StringUtils.urlEncode(key)
            val url = "$api/api.php/provide/vod/?ac=list&wd=$encodedKey"
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
        Log.d(TAG, "📄 获取 Auete 详情: ids=$ids")
        
        return try {
            if (ids.isEmpty()) {
                return createEmptyResult()
            }
            
            val id = ids[0]
            val url = "$api/api.php/provide/vod/?ac=detail&ids=$id"
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
        Log.d(TAG, "🎬 获取 Auete 播放内容: flag=$flag, id=$id")
        
        return try {
            // Auete 通常直接返回播放链接
            val playUrl = if (id.startsWith("http")) {
                id
            } else {
                "$api/play/$id"
            }
            
            JsonUtils.createJsonObject(mapOf(
                "parse" to 0,
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
            
            val classes = mutableListOf<Map<String, Any>>()
            
            // 添加默认分类
            classes.add(mapOf(
                "type_id" to "1",
                "type_name" to "电影"
            ))
            classes.add(mapOf(
                "type_id" to "2", 
                "type_name" to "电视剧"
            ))
            classes.add(mapOf(
                "type_id" to "3",
                "type_name" to "综艺"
            ))
            classes.add(mapOf(
                "type_id" to "4",
                "type_name" to "动漫"
            ))
            
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
            
            val list = JsonUtils.getJsonArray(jsonObject, "list") ?: emptyList()
            val vodList = mutableListOf<Map<String, Any>>()
            
            list.forEach { item ->
                if (item is Map<*, *>) {
                    val vodItem = mapOf(
                        "vod_id" to (item["vod_id"] ?: ""),
                        "vod_name" to (item["vod_name"] ?: ""),
                        "vod_pic" to (item["vod_pic"] ?: ""),
                        "vod_remarks" to (item["vod_remarks"] ?: "")
                    )
                    vodList.add(vodItem)
                }
            }
            
            JsonUtils.createJsonObject(mapOf(
                "list" to vodList,
                "page" to (JsonUtils.getInt(jsonObject, "page") ?: 1),
                "pagecount" to (JsonUtils.getInt(jsonObject, "pagecount") ?: 1),
                "limit" to (JsonUtils.getInt(jsonObject, "limit") ?: 20),
                "total" to (JsonUtils.getInt(jsonObject, "total") ?: vodList.size)
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
            
            val list = JsonUtils.getJsonArray(jsonObject, "list") ?: emptyList()
            if (list.isEmpty()) {
                Log.w(TAG, "⚠️ 详情列表为空")
                return createEmptyResult()
            }
            
            val vodInfo = list[0] as? Map<*, *> ?: return createEmptyResult()
            
            val detailItem = mapOf(
                "vod_id" to (vodInfo["vod_id"] ?: ""),
                "vod_name" to (vodInfo["vod_name"] ?: ""),
                "vod_pic" to (vodInfo["vod_pic"] ?: ""),
                "type_name" to (vodInfo["type_name"] ?: ""),
                "vod_year" to (vodInfo["vod_year"] ?: ""),
                "vod_area" to (vodInfo["vod_area"] ?: ""),
                "vod_remarks" to (vodInfo["vod_remarks"] ?: ""),
                "vod_actor" to (vodInfo["vod_actor"] ?: ""),
                "vod_director" to (vodInfo["vod_director"] ?: ""),
                "vod_content" to (vodInfo["vod_content"] ?: ""),
                "vod_play_from" to (vodInfo["vod_play_from"] ?: ""),
                "vod_play_url" to (vodInfo["vod_play_url"] ?: "")
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
        val baseUrl = "$api/api.php/provide/vod/?ac=list&t=$tid&pg=$pg"
        
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
