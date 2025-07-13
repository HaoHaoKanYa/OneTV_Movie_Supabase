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
 * XPathFilter 解析器
 * 
 * 基于 FongMi/TV 的 csp_XPathFilter 实现
 * 提供带过滤功能的 XPath 解析器
 * 
 * 功能：
 * - 完整的过滤器支持
 * - 动态筛选条件
 * - 多级过滤
 * - 自定义过滤规则
 * - 过滤器缓存
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class XPathFilterSpider : XPathSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_XPATH_FILTER_SPIDER"
    }
    
    // 过滤器缓存
    protected val filterCache = mutableMapOf<String, List<FilterGroup>>()
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 XPathFilter 解析器初始化")
    }
    
    /**
     * 🏠 获取首页内容（带过滤器）
     */
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 XPathFilter 获取首页内容, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPathFilter 配置未初始化")
            
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
                
                // 添加过滤器配置
                if (filter) {
                    put("filters", buildJsonObject {
                        categories.forEach { category ->
                            val filters = parseFilters(html, config, category.typeId)
                            if (filters.isNotEmpty()) {
                                put(category.typeId, buildJsonArray {
                                    filters.forEach { filterGroup ->
                                        add(buildJsonObject {
                                            put("key", filterGroup.key)
                                            put("name", filterGroup.name)
                                            put("value", buildJsonArray {
                                                filterGroup.values.forEach { filterValue ->
                                                    add(buildJsonObject {
                                                        put("n", filterValue.name)
                                                        put("v", filterValue.value)
                                                    })
                                                }
                                            })
                                        })
                                    }
                                })
                            }
                        }
                    })
                }
            }
            
            logDebug("✅ XPathFilter 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathFilter 首页内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    /**
     * 📋 获取分类内容（带过滤器）
     */
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 XPathFilter 获取分类内容: tid=$tid, pg=$pg, filter=$filter, extend=$extend")
            
            val config = xpathConfig ?: throw Exception("XPathFilter 配置未初始化")
            val page = pg.toIntOrNull() ?: 1
            
            // 构建带过滤器的分类页面 URL
            val categoryUrl = buildFilterCategoryUrl(tid, page, extend, config)
            
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
            
            logDebug("✅ XPathFilter 分类内容解析成功，内容数量: ${vodItems.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathFilter 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== 过滤器解析方法 ==========
    
    /**
     * 🔍 解析过滤器
     */
    protected open fun parseFilters(html: String, config: XPathConfig, typeId: String): List<FilterGroup> {
        // 从缓存获取
        filterCache[typeId]?.let { cachedFilters ->
            logDebug("📦 从缓存获取过滤器: $typeId")
            return cachedFilters
        }
        
        val filters = mutableListOf<FilterGroup>()
        
        try {
            // 解析年份过滤器
            val yearFilter = parseYearFilter(html, config)
            if (yearFilter != null) {
                filters.add(yearFilter)
            }
            
            // 解析地区过滤器
            val areaFilter = parseAreaFilter(html, config)
            if (areaFilter != null) {
                filters.add(areaFilter)
            }
            
            // 解析类型过滤器
            val genreFilter = parseGenreFilter(html, config)
            if (genreFilter != null) {
                filters.add(genreFilter)
            }
            
            // 解析排序过滤器
            val sortFilter = parseSortFilter(html, config)
            if (sortFilter != null) {
                filters.add(sortFilter)
            }
            
            // 解析自定义过滤器
            val customFilters = parseCustomFilters(html, config)
            filters.addAll(customFilters)
            
            // 缓存过滤器
            filterCache[typeId] = filters
            
        } catch (e: Exception) {
            logWarning("⚠️ 过滤器解析失败", e)
        }
        
        return filters
    }
    
    /**
     * 📅 解析年份过滤器
     */
    protected open fun parseYearFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val yearRule = config.filterRule.ifEmpty { ".filter-year a" }
            val years = JsoupUtils.parseRuleArray(html, yearRule)
            
            if (years.isNotEmpty()) {
                val filterValues = mutableListOf<FilterValue>()
                filterValues.add(FilterValue("全部", ""))
                
                years.forEach { year ->
                    if (year.matches("\\d{4}".toRegex())) {
                        filterValues.add(FilterValue(year, year))
                    }
                }
                
                FilterGroup("year", "年份", filterValues)
            } else {
                // 生成默认年份过滤器
                createDefaultYearFilter()
            }
        } catch (e: Exception) {
            logWarning("⚠️ 年份过滤器解析失败", e)
            createDefaultYearFilter()
        }
    }
    
    /**
     * 🌍 解析地区过滤器
     */
    protected open fun parseAreaFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val areaRule = config.filterRule.ifEmpty { ".filter-area a" }
            val areas = JsoupUtils.parseRuleArray(html, areaRule)
            
            if (areas.isNotEmpty()) {
                val filterValues = mutableListOf<FilterValue>()
                filterValues.add(FilterValue("全部", ""))
                
                areas.forEach { area ->
                    filterValues.add(FilterValue(area, area))
                }
                
                FilterGroup("area", "地区", filterValues)
            } else {
                // 生成默认地区过滤器
                createDefaultAreaFilter()
            }
        } catch (e: Exception) {
            logWarning("⚠️ 地区过滤器解析失败", e)
            createDefaultAreaFilter()
        }
    }
    
    /**
     * 🎭 解析类型过滤器
     */
    protected open fun parseGenreFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val genreRule = config.filterRule.ifEmpty { ".filter-genre a" }
            val genres = JsoupUtils.parseRuleArray(html, genreRule)
            
            if (genres.isNotEmpty()) {
                val filterValues = mutableListOf<FilterValue>()
                filterValues.add(FilterValue("全部", ""))
                
                genres.forEach { genre ->
                    filterValues.add(FilterValue(genre, genre))
                }
                
                FilterGroup("genre", "类型", filterValues)
            } else {
                // 生成默认类型过滤器
                createDefaultGenreFilter()
            }
        } catch (e: Exception) {
            logWarning("⚠️ 类型过滤器解析失败", e)
            createDefaultGenreFilter()
        }
    }
    
    /**
     * 📊 解析排序过滤器
     */
    protected open fun parseSortFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            createDefaultSortFilter()
        } catch (e: Exception) {
            logWarning("⚠️ 排序过滤器解析失败", e)
            null
        }
    }
    
    /**
     * 🔧 解析自定义过滤器
     */
    protected open fun parseCustomFilters(html: String, config: XPathConfig): List<FilterGroup> {
        val customFilters = mutableListOf<FilterGroup>()
        
        try {
            // 这里可以根据具体网站的过滤器结构进行解析
            // 示例：解析状态过滤器
            val statusFilter = FilterGroup("status", "状态", listOf(
                FilterValue("全部", ""),
                FilterValue("连载", "连载"),
                FilterValue("完结", "完结")
            ))
            customFilters.add(statusFilter)
            
        } catch (e: Exception) {
            logWarning("⚠️ 自定义过滤器解析失败", e)
        }
        
        return customFilters
    }
    
    // ========== 默认过滤器创建 ==========
    
    /**
     * 📅 创建默认年份过滤器
     */
    protected open fun createDefaultYearFilter(): FilterGroup {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val filterValues = mutableListOf<FilterValue>()
        
        filterValues.add(FilterValue("全部", ""))
        for (year in currentYear downTo currentYear - 10) {
            filterValues.add(FilterValue(year.toString(), year.toString()))
        }
        
        return FilterGroup("year", "年份", filterValues)
    }
    
    /**
     * 🌍 创建默认地区过滤器
     */
    protected open fun createDefaultAreaFilter(): FilterGroup {
        val areas = listOf("全部", "大陆", "香港", "台湾", "美国", "韩国", "日本", "泰国", "英国", "法国", "其他")
        val filterValues = areas.map { area ->
            FilterValue(area, if (area == "全部") "" else area)
        }
        
        return FilterGroup("area", "地区", filterValues)
    }
    
    /**
     * 🎭 创建默认类型过滤器
     */
    protected open fun createDefaultGenreFilter(): FilterGroup {
        val genres = listOf("全部", "动作", "喜剧", "爱情", "科幻", "恐怖", "剧情", "战争", "悬疑", "动画", "犯罪", "奇幻", "冒险", "家庭")
        val filterValues = genres.map { genre ->
            FilterValue(genre, if (genre == "全部") "" else genre)
        }
        
        return FilterGroup("genre", "类型", filterValues)
    }
    
    /**
     * 📊 创建默认排序过滤器
     */
    protected open fun createDefaultSortFilter(): FilterGroup {
        val sorts = listOf(
            FilterValue("最新", "time"),
            FilterValue("最热", "hits"),
            FilterValue("评分", "score")
        )
        
        return FilterGroup("sort", "排序", sorts)
    }
    
    // ========== URL 构建方法 ==========
    
    /**
     * 🔗 构建带过滤器的分类 URL
     */
    protected open fun buildFilterCategoryUrl(
        tid: String,
        page: Int,
        extend: HashMap<String, String>,
        config: XPathConfig
    ): String {
        var url = UrlUtils.buildUrl(siteUrl, "/category/$tid/page/$page")
        
        // 添加过滤参数
        if (extend.isNotEmpty()) {
            val params = mutableMapOf<String, String>()
            
            extend.forEach { (key, value) ->
                if (value.isNotEmpty()) {
                    when (key) {
                        "year" -> params["year"] = value
                        "area" -> params["area"] = value
                        "genre" -> params["genre"] = value
                        "sort" -> params["sort"] = value
                        "status" -> params["status"] = value
                        else -> params[key] = value
                    }
                }
            }
            
            if (params.isNotEmpty()) {
                url = UrlUtils.addParams(url, params)
            }
        }
        
        return url
    }
    
    /**
     * 🧹 清理过滤器缓存
     */
    fun clearFilterCache() {
        filterCache.clear()
        logDebug("🧹 过滤器缓存已清理")
    }
    
    /**
     * 📊 获取过滤器统计
     */
    fun getFilterStats(): Map<String, Any> {
        return mapOf(
            "cached_filters" to filterCache.size,
            "filter_keys" to filterCache.keys.toList()
        )
    }
}

// ========== 过滤器数据模型 ==========

/**
 * 过滤器组
 */
data class FilterGroup(
    val key: String,
    val name: String,
    val values: List<FilterValue>
)

/**
 * 过滤器值
 */
data class FilterValue(
    val name: String,
    val value: String
)
