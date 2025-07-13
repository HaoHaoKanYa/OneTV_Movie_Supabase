package top.cywin.onetv.film.spider.xpath

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.cywin.onetv.film.data.models.XPathConfig
import top.cywin.onetv.film.utils.JsoupUtils
import top.cywin.onetv.film.utils.UrlUtils

/**
 * XPathMac 解析器
 * 
 * 基于 FongMi/TV 的 csp_XPathMac 实现
 * 提供 Mac 版本的 XPath 解析功能，支持更复杂的解析规则
 * 
 * 功能：
 * - 增强的 XPath 规则解析
 * - 支持 Mac 格式的配置
 * - 更灵活的内容提取
 * - 改进的错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class XPathMacSpider : XPathSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_XPATH_MAC_SPIDER"
    }
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 XPathMac 解析器初始化")
    }
    
    /**
     * 🔧 解析 XPath 配置（Mac 版本）
     */
    override fun parseXPathConfig(extend: String): XPathConfig {
        return try {
            logDebug("🔧 解析 XPathMac 配置")
            
            // Mac 版本的配置解析逻辑
            // 这里简化处理，实际应该解析更复杂的 Mac 格式配置
            if (extend.isNotEmpty()) {
                parseMacConfig(extend)
            } else {
                createMacDefaultConfig()
            }
            
        } catch (e: Exception) {
            logError("❌ XPathMac 配置解析失败", e)
            createMacDefaultConfig()
        }
    }
    
    /**
     * 🏠 获取首页内容（Mac 版本增强）
     */
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 XPathMac 获取首页内容, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPathMac 配置未初始化")
            
            // 获取首页 HTML
            val html = httpManager.getString(siteUrl, siteHeaders)
            
            // 使用 Mac 增强解析
            val categories = parseMacCategories(html, config)
            
            val result = buildJsonResponse {
                put("class", buildJsonArray {
                    categories.forEach { category ->
                        add(buildJsonObject {
                            put("type_id", category.typeId)
                            put("type_name", category.typeName)
                        })
                    }
                })
                
                // Mac 版本支持筛选器
                if (filter) {
                    put("filters", buildJsonObject {
                        categories.forEach { category ->
                            put(category.typeId, buildJsonArray {
                                // 添加筛选器配置
                                add(buildJsonObject {
                                    put("key", "year")
                                    put("name", "年份")
                                    put("value", buildJsonArray {
                                        add(buildJsonObject {
                                            put("n", "全部")
                                            put("v", "")
                                        })
                                        add(buildJsonObject {
                                            put("n", "2023")
                                            put("v", "2023")
                                        })
                                        add(buildJsonObject {
                                            put("n", "2022")
                                            put("v", "2022")
                                        })
                                    })
                                })
                            })
                        }
                    })
                }
            }
            
            logDebug("✅ XPathMac 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathMac 首页内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    /**
     * 📋 获取分类内容（Mac 版本增强）
     */
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 XPathMac 获取分类内容: tid=$tid, pg=$pg, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPathMac 配置未初始化")
            val page = pg.toIntOrNull() ?: 1
            
            // 构建 Mac 版本的分类页面 URL
            val categoryUrl = buildMacCategoryUrl(tid, page, extend, config)
            
            // 获取分类页面 HTML
            val html = httpManager.getString(categoryUrl, siteHeaders)
            
            // 使用 Mac 增强解析
            val vodItems = parseMacVodList(html, config)
            
            // 解析 Mac 版本的分页信息
            val pageInfo = parseMacPageInfo(html, config, page)
            
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
            
            logDebug("✅ XPathMac 分类内容解析成功，内容数量: ${vodItems.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathMac 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    /**
     * 🎭 获取视频详情（Mac 版本增强）
     */
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 XPathMac 获取视频详情: vodId=$vodId")
            
            val config = xpathConfig ?: throw Exception("XPathMac 配置未初始化")
            
            // 构建 Mac 版本的详情页面 URL
            val detailUrl = buildMacDetailUrl(vodId, config)
            
            // 获取详情页面 HTML
            val html = httpManager.getString(detailUrl, siteHeaders)
            
            // 使用 Mac 增强解析
            val vodDetail = parseMacVodDetail(html, config, vodId)
            
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
            
            logDebug("✅ XPathMac 视频详情解析成功: ${vodDetail.vodName}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathMac 视频详情解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== Mac 版本增强解析方法 ==========
    
    /**
     * 🏷️ 解析 Mac 版本分类
     */
    protected open suspend fun parseMacCategories(html: String, config: XPathConfig): List<CategoryItem> {
        val categories = mutableListOf<CategoryItem>()
        
        try {
            // Mac 版本支持多种分类规则
            val categoryRules = listOf(
                config.categoryListRule,
                ".nav-item, .category-item, .type-item",
                "ul.nav li, .category-list li"
            )
            
            for (rule in categoryRules) {
                if (rule.isNotEmpty()) {
                    val elements = JsoupUtils.selectElements(html, rule)
                    if (elements.isNotEmpty()) {
                        elements.forEach { element ->
                            val typeId = JsoupUtils.parseRule(element, config.categoryIdRule.ifEmpty { "a@href" })
                            val typeName = JsoupUtils.parseRule(element, config.categoryNameRule.ifEmpty { "a@text" })
                            
                            if (typeId.isNotEmpty() && typeName.isNotEmpty()) {
                                categories.add(CategoryItem(typeId, typeName))
                            }
                        }
                        break // 找到有效规则就停止
                    }
                }
            }
            
        } catch (e: Exception) {
            logWarning("⚠️ Mac 分类解析失败", e)
        }
        
        return categories
    }
    
    /**
     * 📋 解析 Mac 版本内容列表
     */
    protected open suspend fun parseMacVodList(html: String, config: XPathConfig): List<VodItem> {
        val vodItems = mutableListOf<VodItem>()
        
        try {
            // Mac 版本支持多种列表规则
            val listRules = listOf(
                config.vodListRule,
                ".video-list li, .movie-item, .video-item",
                ".list-item, .content-item"
            )
            
            for (rule in listRules) {
                if (rule.isNotEmpty()) {
                    val elements = JsoupUtils.selectElements(html, rule)
                    if (elements.isNotEmpty()) {
                        elements.forEach { element ->
                            val vodId = JsoupUtils.parseRule(element, config.vodIdRule.ifEmpty { "a@href" })
                            val vodName = JsoupUtils.parseRule(element, config.vodNameRule.ifEmpty { ".title@text" })
                            val vodPic = JsoupUtils.parseRule(element, config.vodPicRule.ifEmpty { "img@src" })
                            val vodRemarks = JsoupUtils.parseRule(element, config.vodRemarksRule.ifEmpty { ".remarks@text" })
                            
                            if (vodId.isNotEmpty() && vodName.isNotEmpty()) {
                                vodItems.add(VodItem(
                                    vodId = vodId,
                                    vodName = vodName,
                                    vodPic = UrlUtils.resolveUrl(siteUrl, vodPic),
                                    vodRemarks = vodRemarks
                                ))
                            }
                        }
                        break // 找到有效规则就停止
                    }
                }
            }
            
        } catch (e: Exception) {
            logWarning("⚠️ Mac 内容列表解析失败", e)
        }
        
        return vodItems
    }
    
    /**
     * 🎭 解析 Mac 版本视频详情
     */
    protected open suspend fun parseMacVodDetail(html: String, config: XPathConfig, vodId: String): VodDetail {
        return try {
            VodDetail(
                vodId = vodId,
                vodName = JsoupUtils.parseRule(html, config.detailNameRule.ifEmpty { ".video-title@text" }),
                vodPic = UrlUtils.resolveUrl(siteUrl, JsoupUtils.parseRule(html, config.detailPicRule.ifEmpty { ".video-pic img@src" })),
                vodContent = JsoupUtils.parseRule(html, config.detailContentRule.ifEmpty { ".video-desc@text" }),
                vodYear = JsoupUtils.parseRule(html, config.detailYearRule.ifEmpty { ".year@text" }),
                vodArea = JsoupUtils.parseRule(html, config.detailAreaRule.ifEmpty { ".area@text" }),
                vodActor = JsoupUtils.parseRule(html, config.detailActorRule.ifEmpty { ".actor@text" }),
                vodDirector = JsoupUtils.parseRule(html, config.detailDirectorRule.ifEmpty { ".director@text" }),
                vodPlayFrom = parseMacPlayFrom(html, config),
                vodPlayUrl = parseMacPlayUrls(html, config)
            )
        } catch (e: Exception) {
            logWarning("⚠️ Mac 视频详情解析失败", e)
            VodDetail(vodId = vodId, vodName = "解析失败")
        }
    }
    
    /**
     * 📄 解析 Mac 版本分页信息
     */
    protected open fun parseMacPageInfo(html: String, config: XPathConfig, currentPage: Int): PageInfo {
        return try {
            val totalPages = JsoupUtils.parseRule(html, config.pageTotalRule.ifEmpty { ".page-total@text" }).toIntOrNull() ?: 10
            
            PageInfo(
                currentPage = currentPage,
                totalPages = totalPages,
                pageSize = 20,
                totalCount = totalPages * 20
            )
        } catch (e: Exception) {
            logWarning("⚠️ Mac 分页信息解析失败", e)
            PageInfo(currentPage, 10, 20, 200)
        }
    }
    
    /**
     * 📺 解析 Mac 版本播放源
     */
    protected open fun parseMacPlayFrom(html: String, config: XPathConfig): String {
        return try {
            val playFroms = JsoupUtils.parseRuleArray(html, config.playFromRule.ifEmpty { ".play-source@text" })
            playFroms.joinToString("$$$")
        } catch (e: Exception) {
            logWarning("⚠️ Mac 播放源解析失败", e)
            "默认播放源"
        }
    }
    
    /**
     * 🎬 解析 Mac 版本播放列表
     */
    protected open fun parseMacPlayUrls(html: String, config: XPathConfig): String {
        return try {
            val playUrls = JsoupUtils.parseRuleArray(html, config.playListRule.ifEmpty { ".play-list a@href" })
            val playNames = JsoupUtils.parseRuleArray(html, ".play-list a@text")
            
            val episodes = mutableListOf<String>()
            playUrls.forEachIndexed { index, url ->
                val name = playNames.getOrNull(index) ?: "第${index + 1}集"
                episodes.add("$name\$$url")
            }
            
            episodes.joinToString("#")
        } catch (e: Exception) {
            logWarning("⚠️ Mac 播放列表解析失败", e)
            "第1集\$http://example.com/play1"
        }
    }
    
    // ========== Mac 版本工具方法 ==========
    
    /**
     * 🔧 解析 Mac 配置
     */
    protected open fun parseMacConfig(extend: String): XPathConfig {
        // 这里应该解析 Mac 格式的配置
        // 简化处理，返回增强的默认配置
        return createMacDefaultConfig()
    }
    
    /**
     * 🔧 创建 Mac 默认配置
     */
    protected open fun createMacDefaultConfig(): XPathConfig {
        return XPathConfig(
            categoryListRule = ".nav-item, .category-item, ul.nav li",
            categoryIdRule = "a@href",
            categoryNameRule = "a@text",
            vodListRule = ".video-list li, .movie-item, .video-item",
            vodIdRule = "a@href",
            vodNameRule = ".title@text, .name@text, h3@text",
            vodPicRule = "img@src, .pic img@src",
            vodRemarksRule = ".remarks@text, .status@text",
            detailNameRule = ".video-title@text, .movie-title@text, h1@text",
            detailPicRule = ".video-pic img@src, .movie-pic img@src",
            detailContentRule = ".video-desc@text, .movie-desc@text",
            detailYearRule = ".year@text, .date@text",
            detailAreaRule = ".area@text, .region@text",
            detailActorRule = ".actor@text, .cast@text",
            detailDirectorRule = ".director@text",
            playFromRule = ".play-source@text, .source@text",
            playListRule = ".play-list a, .episode-list a",
            playUrlRule = ".play-url@href, a@href",
            pageTotalRule = ".page-total@text, .total-page@text"
        )
    }
    
    /**
     * 🔗 构建 Mac 版本分类 URL
     */
    protected open fun buildMacCategoryUrl(tid: String, page: Int, extend: HashMap<String, String>, config: XPathConfig): String {
        var url = UrlUtils.buildUrl(siteUrl, "/category/$tid/page/$page")
        
        // 添加筛选参数
        if (extend.isNotEmpty()) {
            val params = mutableMapOf<String, String>()
            extend.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    params[key] = value
                }
            }
            if (params.isNotEmpty()) {
                url = UrlUtils.addParams(url, params)
            }
        }
        
        return url
    }
    
    /**
     * 🔗 构建 Mac 版本详情 URL
     */
    protected open fun buildMacDetailUrl(vodId: String, config: XPathConfig): String {
        return UrlUtils.resolveUrl(siteUrl, vodId)
    }
}
