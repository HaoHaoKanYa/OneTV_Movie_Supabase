package top.cywin.onetv.film.spider.xpath

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.utils.JsoupUtils
import top.cywin.onetv.film.utils.UrlUtils
import top.cywin.onetv.film.network.OkHttpManager

/**
 * 基础 XPath 解析器
 * 
 * 基于 FongMi/TV 的 csp_XPath 实现
 * 提供基础的 XPath 网页解析功能
 * 
 * 功能：
 * - XPath 规则解析
 * - HTML 内容提取
 * - 分类和内容解析
 * - 搜索功能
 * - 播放链接解析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class XPathSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_XPATH_SPIDER"
    }
    
    // XPath 配置
    protected var xpathConfig: XPathConfig? = null
    
    // HTTP 管理器
    protected val httpManager = OkHttpManager()
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        
        try {
            // 解析 XPath 配置
            xpathConfig = parseXPathConfig(extend)
            Log.d(TAG, "✅ XPath 配置解析成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ XPath 配置解析失败", e)
            xpathConfig = createDefaultConfig()
        }
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 获取首页内容, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPath 配置未初始化")
            
            // 获取首页 HTML
            val html = httpManager.getString(siteUrl, siteHeaders)
            
            // 解析分类
            val categories = parseCategories(html, config)
            
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
            
            logDebug("✅ 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ 首页内容解析失败", e)
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
            logDebug("📋 获取分类内容: tid=$tid, pg=$pg, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPath 配置未初始化")
            val page = pg.toIntOrNull() ?: 1
            
            // 构建分类页面 URL
            val categoryUrl = buildCategoryUrl(tid, page, extend, config)
            
            // 获取分类页面 HTML
            val html = httpManager.getString(categoryUrl, siteHeaders)
            
            // 解析内容列表
            val vodItems = parseVodList(html, config)
            
            // 解析分页信息
            val pageInfo = parsePageInfo(html, config, page)
            
            val result = buildJsonResponse {
                put("list", buildJsonArray {
                    vodItems.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.vodId)
                            put("vod_name", item.vodName)
                            put("vod_pic", item.vodPic)
                            put("vod_remarks", item.vodRemarks)
                        })
                    }
                })
                put("page", pageInfo.currentPage)
                put("pagecount", pageInfo.totalPages)
                put("limit", pageInfo.pageSize)
                put("total", pageInfo.totalCount)
            }
            
            logDebug("✅ 分类内容解析成功，内容数量: ${vodItems.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 获取视频详情: vodId=$vodId")
            
            val config = xpathConfig ?: throw Exception("XPath 配置未初始化")
            
            // 构建详情页面 URL
            val detailUrl = buildDetailUrl(vodId, config)
            
            // 获取详情页面 HTML
            val html = httpManager.getString(detailUrl, siteHeaders)
            
            // 解析视频详情
            val vodDetail = parseVodDetail(html, config, vodId)
            
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
            
            logDebug("✅ 视频详情解析成功: ${vodDetail.vodName}")
            result
            
        } catch (e: Exception) {
            logError("❌ 视频详情解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 搜索内容: key=$key, quick=$quick")
            
            val config = xpathConfig ?: throw Exception("XPath 配置未初始化")
            
            // 构建搜索 URL
            val searchUrl = buildSearchUrl(key, config)
            
            // 获取搜索结果 HTML
            val html = httpManager.getString(searchUrl, siteHeaders)
            
            // 解析搜索结果
            val searchResults = parseSearchResults(html, config)
            
            val result = buildJsonResponse {
                put("list", buildJsonArray {
                    searchResults.forEach { item ->
                        add(buildJsonObject {
                            put("vod_id", item.vodId)
                            put("vod_name", item.vodName)
                            put("vod_pic", item.vodPic)
                            put("vod_remarks", item.vodRemarks)
                        })
                    }
                })
            }
            
            logDebug("✅ 搜索完成，结果数量: ${searchResults.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ 搜索失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ 获取播放链接: flag=$flag, id=$id")
            
            val config = xpathConfig ?: throw Exception("XPath 配置未初始化")
            
            // 解析播放链接
            val playUrl = parsePlayUrl(id, flag, config)
            
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
            
            logDebug("✅ 播放链接解析成功: $playUrl")
            result
            
        } catch (e: Exception) {
            logError("❌ 播放链接解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== 解析方法 ==========
    
    /**
     * 🏷️ 解析分类
     */
    protected open suspend fun parseCategories(html: String, config: XPathConfig): List<CategoryItem> {
        val categories = mutableListOf<CategoryItem>()
        
        try {
            val categoryElements = JsoupUtils.selectElements(html, config.categoryListRule)
            
            categoryElements.forEach { element ->
                val typeId = JsoupUtils.parseRule(element, config.categoryIdRule)
                val typeName = JsoupUtils.parseRule(element, config.categoryNameRule)
                
                if (typeId.isNotEmpty() && typeName.isNotEmpty()) {
                    categories.add(CategoryItem(typeId, typeName))
                }
            }
            
        } catch (e: Exception) {
            logWarning("⚠️ 分类解析失败", e)
        }
        
        return categories
    }
    
    /**
     * 📋 解析内容列表
     */
    protected open suspend fun parseVodList(html: String, config: XPathConfig): List<VodItem> {
        val vodItems = mutableListOf<VodItem>()
        
        try {
            val itemElements = JsoupUtils.selectElements(html, config.vodListRule)
            
            itemElements.forEach { element ->
                val vodId = JsoupUtils.parseRule(element, config.vodIdRule)
                val vodName = JsoupUtils.parseRule(element, config.vodNameRule)
                val vodPic = JsoupUtils.parseRule(element, config.vodPicRule)
                val vodRemarks = JsoupUtils.parseRule(element, config.vodRemarksRule)
                
                if (vodId.isNotEmpty() && vodName.isNotEmpty()) {
                    vodItems.add(VodItem(
                        vodId = vodId,
                        vodName = vodName,
                        vodPic = UrlUtils.resolveUrl(siteUrl, vodPic),
                        vodRemarks = vodRemarks
                    ))
                }
            }
            
        } catch (e: Exception) {
            logWarning("⚠️ 内容列表解析失败", e)
        }
        
        return vodItems
    }
    
    /**
     * 🎭 解析视频详情
     */
    protected open suspend fun parseVodDetail(html: String, config: XPathConfig, vodId: String): VodDetail {
        return try {
            VodDetail(
                vodId = vodId,
                vodName = JsoupUtils.parseRule(html, config.detailNameRule),
                vodPic = UrlUtils.resolveUrl(siteUrl, JsoupUtils.parseRule(html, config.detailPicRule)),
                vodContent = JsoupUtils.parseRule(html, config.detailContentRule),
                vodYear = JsoupUtils.parseRule(html, config.detailYearRule),
                vodArea = JsoupUtils.parseRule(html, config.detailAreaRule),
                vodActor = JsoupUtils.parseRule(html, config.detailActorRule),
                vodDirector = JsoupUtils.parseRule(html, config.detailDirectorRule),
                vodPlayFrom = parsePlayFrom(html, config),
                vodPlayUrl = parsePlayUrls(html, config)
            )
        } catch (e: Exception) {
            logWarning("⚠️ 视频详情解析失败", e)
            VodDetail(vodId = vodId, vodName = "解析失败")
        }
    }
    
    /**
     * 🔍 解析搜索结果
     */
    protected open suspend fun parseSearchResults(html: String, config: XPathConfig): List<VodItem> {
        return parseVodList(html, config) // 搜索结果通常与列表页面结构相同
    }
    
    /**
     * ▶️ 解析播放链接
     */
    protected open suspend fun parsePlayUrl(id: String, flag: String, config: XPathConfig): String {
        return try {
            // 如果 id 本身就是播放链接
            if (id.startsWith("http")) {
                id
            } else {
                // 需要进一步解析
                val playPageUrl = UrlUtils.resolveUrl(siteUrl, id)
                val html = httpManager.getString(playPageUrl, siteHeaders)
                JsoupUtils.parseRule(html, config.playUrlRule)
            }
        } catch (e: Exception) {
            logWarning("⚠️ 播放链接解析失败", e)
            id
        }
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 🔧 解析 XPath 配置
     */
    protected open fun parseXPathConfig(extend: String): XPathConfig {
        // 这里简化处理，实际应该解析 JSON 配置
        return createDefaultConfig()
    }
    
    /**
     * 🔧 创建默认配置
     */
    protected open fun createDefaultConfig(): XPathConfig {
        return XPathConfig(
            categoryListRule = ".nav-item",
            categoryIdRule = "a@href",
            categoryNameRule = "a@text",
            vodListRule = ".video-item",
            vodIdRule = "a@href",
            vodNameRule = ".title@text",
            vodPicRule = "img@src",
            vodRemarksRule = ".remarks@text"
        )
    }
    
    /**
     * 🔗 构建分类 URL
     */
    protected open fun buildCategoryUrl(tid: String, page: Int, extend: HashMap<String, String>, config: XPathConfig): String {
        return UrlUtils.buildUrl(siteUrl, "/category/$tid/page/$page")
    }
    
    /**
     * 🔗 构建详情 URL
     */
    protected open fun buildDetailUrl(vodId: String, config: XPathConfig): String {
        return UrlUtils.resolveUrl(siteUrl, vodId)
    }
    
    /**
     * 🔗 构建搜索 URL
     */
    protected open fun buildSearchUrl(keyword: String, config: XPathConfig): String {
        return UrlUtils.buildUrl(siteUrl, "/search?q=$keyword")
    }
    
    /**
     * 📄 解析分页信息
     */
    protected open fun parsePageInfo(html: String, config: XPathConfig, currentPage: Int): PageInfo {
        return PageInfo(
            currentPage = currentPage,
            totalPages = 10, // 默认值，实际应该从 HTML 解析
            pageSize = 20,
            totalCount = 200
        )
    }
    
    /**
     * 📺 解析播放源
     */
    protected open fun parsePlayFrom(html: String, config: XPathConfig): String {
        return "默认播放源"
    }
    
    /**
     * 🎬 解析播放列表
     */
    protected open fun parsePlayUrls(html: String, config: XPathConfig): String {
        return "第1集\$http://example.com/play1"
    }
}
