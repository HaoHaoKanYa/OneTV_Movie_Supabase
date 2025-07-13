package top.cywin.onetv.film.spider.special

import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient

/**
 * TVBus 特殊解析器
 * 
 * 基于 FongMi/TV 的 TVBus 站点特殊解析实现
 * 针对 TVBus 站点的特殊数据格式和加密方式进行解析
 * 
 * 功能：
 * - TVBus 站点首页解析
 * - TVBus 分类内容解析
 * - TVBus 详情页解析
 * - TVBus 搜索功能
 * - TVBus 播放链接解析
 * - TVBus 特殊加密解密
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class TvbusSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_TVBUS_SPIDER"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        private const val REFERER = "https://www.tvbus.tv/"
    }
    
    private lateinit var networkClient: NetworkClient
    
    override fun init(context: android.content.Context, extend: String) {
        super.init(context, extend)
        networkClient = NetworkClient.Builder()
            .userAgent(USER_AGENT)
            .timeout(15000L)
            .addHeader("Referer", REFERER)
            .build()
        
        Log.d(TAG, "🕷️ TvbusSpider 初始化完成")
    }
    
    override fun getName(): String = "TVBus"
    
    override fun getApi(): String = api
    
    override fun getType(): Int = 3 // 特殊类型
    
    override fun isSearchable(): Boolean = true
    
    override fun isFilterable(): Boolean = false
    
    /**
     * 🏠 获取首页内容
     */
    override fun homeContent(filter: Boolean): String {
        Log.d(TAG, "🏠 获取 TVBus 首页内容")
        
        return try {
            val url = "$api/"
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
        Log.d(TAG, "📂 获取 TVBus 分类内容: tid=$tid, pg=$pg")
        
        return try {
            val url = buildCategoryUrl(tid, pg)
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
        Log.d(TAG, "🔍 TVBus 搜索: key=$key, quick=$quick")
        
        return try {
            val encodedKey = StringUtils.urlEncode(key)
            val url = "$api/search?q=$encodedKey"
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
        Log.d(TAG, "📄 获取 TVBus 详情: ids=$ids")
        
        return try {
            if (ids.isEmpty()) {
                return createEmptyResult()
            }
            
            val id = ids[0]
            val url = "$api/detail/$id"
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
        Log.d(TAG, "🎬 获取 TVBus 播放内容: flag=$flag, id=$id")
        
        return try {
            // TVBus 需要特殊解密处理
            val playUrl = decryptPlayUrl(id)
            
            JsonUtils.createJsonObject(mapOf(
                "parse" to 0,
                "playUrl" to "",
                "url" to playUrl,
                "header" to mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to REFERER
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
            // TVBus 首页 HTML 解析
            val classes = mutableListOf<Map<String, Any>>()
            
            // 使用正则表达式提取分类信息
            val categoryPattern = """<a[^>]*href="[^"]*category/(\d+)"[^>]*>([^<]+)</a>""".toRegex()
            val matches = categoryPattern.findAll(response)
            
            matches.forEach { match ->
                val typeId = match.groupValues[1]
                val typeName = match.groupValues[2].trim()
                
                if (typeId.isNotEmpty() && typeName.isNotEmpty()) {
                    classes.add(mapOf(
                        "type_id" to typeId,
                        "type_name" to typeName
                    ))
                }
            }
            
            // 如果没有找到分类，添加默认分类
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
            val vodList = mutableListOf<Map<String, Any>>()
            
            // 使用正则表达式提取视频信息
            val videoPattern = """<div[^>]*class="[^"]*video-item[^"]*"[^>]*>.*?<a[^>]*href="[^"]*detail/([^"]+)"[^>]*>.*?<img[^>]*src="([^"]*)"[^>]*>.*?<h3[^>]*>([^<]+)</h3>.*?<span[^>]*>([^<]*)</span>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = videoPattern.findAll(response)
            
            matches.forEach { match ->
                val vodId = match.groupValues[1]
                val vodPic = match.groupValues[2]
                val vodName = match.groupValues[3].trim()
                val vodRemarks = match.groupValues[4].trim()
                
                if (vodId.isNotEmpty() && vodName.isNotEmpty()) {
                    vodList.add(mapOf(
                        "vod_id" to vodId,
                        "vod_name" to vodName,
                        "vod_pic" to vodPic,
                        "vod_remarks" to vodRemarks
                    ))
                }
            }
            
            JsonUtils.createJsonObject(mapOf(
                "list" to vodList,
                "page" to 1,
                "pagecount" to 1,
                "limit" to vodList.size,
                "total" to vodList.size
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
            // 提取基本信息
            val titlePattern = """<h1[^>]*>([^<]+)</h1>""".toRegex()
            val picPattern = """<img[^>]*src="([^"]*)"[^>]*class="[^"]*poster[^"]*"""".toRegex()
            val contentPattern = """<div[^>]*class="[^"]*content[^"]*"[^>]*>([^<]+)</div>""".toRegex()
            
            val vodName = titlePattern.find(response)?.groupValues?.get(1)?.trim() ?: ""
            val vodPic = picPattern.find(response)?.groupValues?.get(1) ?: ""
            val vodContent = contentPattern.find(response)?.groupValues?.get(1)?.trim() ?: ""
            
            // 提取播放链接
            val playFromList = mutableListOf<String>()
            val playUrlList = mutableListOf<String>()
            
            val playPattern = """<div[^>]*class="[^"]*play-source[^"]*"[^>]*data-source="([^"]*)"[^>]*>.*?<ul[^>]*>(.*?)</ul>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = playPattern.findAll(response)
            
            matches.forEach { match ->
                val source = match.groupValues[1]
                val urlsHtml = match.groupValues[2]
                
                playFromList.add(source)
                
                val urlPattern = """<a[^>]*href="[^"]*play/([^"]+)"[^>]*>([^<]+)</a>""".toRegex()
                val urlMatches = urlPattern.findAll(urlsHtml)
                
                val episodes = mutableListOf<String>()
                urlMatches.forEach { urlMatch ->
                    val playId = urlMatch.groupValues[1]
                    val episodeName = urlMatch.groupValues[2].trim()
                    episodes.add("$episodeName\$$playId")
                }
                
                playUrlList.add(episodes.joinToString("#"))
            }
            
            val detailItem = mapOf(
                "vod_id" to (extractVodId(response) ?: ""),
                "vod_name" to vodName,
                "vod_pic" to vodPic,
                "vod_content" to vodContent,
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
    private fun buildCategoryUrl(tid: String, pg: String): String {
        return "$api/category/$tid?page=$pg"
    }
    
    /**
     * 🔓 解密播放链接
     */
    private fun decryptPlayUrl(encryptedUrl: String): String {
        return try {
            // TVBus 特殊解密逻辑
            if (encryptedUrl.startsWith("http")) {
                encryptedUrl
            } else {
                // 简单的 Base64 解码示例
                val decoded = StringUtils.base64Decode(encryptedUrl)
                if (decoded.startsWith("http")) {
                    decoded
                } else {
                    "$api/play/$encryptedUrl"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解密播放链接失败", e)
            encryptedUrl
        }
    }
    
    /**
     * 🆔 提取视频ID
     */
    private fun extractVodId(response: String): String? {
        val idPattern = """data-id="([^"]+)"""".toRegex()
        return idPattern.find(response)?.groupValues?.get(1)
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
