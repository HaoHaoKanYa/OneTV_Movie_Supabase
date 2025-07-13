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

/**
 * XPathMacFilter 解析器
 * 
 * 基于 FongMi/TV 的 csp_XPathMacFilter 实现
 * 结合 XPathMac 和 XPathFilter 的功能
 * 
 * 功能：
 * - Mac 版本的增强解析
 * - 完整的过滤器支持
 * - 多规则回退机制
 * - 智能过滤器检测
 * - 高级筛选功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class XPathMacFilterSpider : XPathMacSpider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_XPATH_MAC_FILTER_SPIDER"
    }
    
    // 过滤器缓存
    protected val filterCache = mutableMapOf<String, List<FilterGroup>>()
    
    // 过滤器检测缓存
    protected val filterDetectionCache = mutableMapOf<String, Boolean>()
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 XPathMacFilter 解析器初始化")
    }
    
    /**
     * 🏠 获取首页内容（Mac + Filter 版本）
     */
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 XPathMacFilter 获取首页内容, filter=$filter")
            
            val config = xpathConfig ?: throw Exception("XPathMacFilter 配置未初始化")
            
            // 获取首页 HTML
            val html = httpManager.getString(siteUrl, siteHeaders)
            
            // 使用 Mac 增强解析分类
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
                
                // 添加 Mac 增强过滤器配置
                if (filter) {
                    put("filters", buildJsonObject {
                        categories.forEach { category ->
                            val filters = parseMacFilters(html, config, category.typeId)
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
            
            logDebug("✅ XPathMacFilter 首页内容解析成功，分类数量: ${categories.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathMacFilter 首页内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    /**
     * 📋 获取分类内容（Mac + Filter 版本）
     */
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 XPathMacFilter 获取分类内容: tid=$tid, pg=$pg, filter=$filter, extend=$extend")
            
            val config = xpathConfig ?: throw Exception("XPathMacFilter 配置未初始化")
            val page = pg.toIntOrNull() ?: 1
            
            // 构建 Mac 版本带过滤器的分类页面 URL
            val categoryUrl = buildMacFilterCategoryUrl(tid, page, extend, config)
            
            // 获取分类页面 HTML
            val html = httpManager.getString(categoryUrl, siteHeaders)
            
            // 使用 Mac 增强解析内容列表
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
            
            logDebug("✅ XPathMacFilter 分类内容解析成功，内容数量: ${vodItems.size}")
            result
            
        } catch (e: Exception) {
            logError("❌ XPathMacFilter 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== Mac 增强过滤器解析方法 ==========
    
    /**
     * 🔍 解析 Mac 增强过滤器
     */
    protected open fun parseMacFilters(html: String, config: XPathConfig, typeId: String): List<FilterGroup> {
        // 从缓存获取
        filterCache[typeId]?.let { cachedFilters ->
            logDebug("📦 从缓存获取 Mac 过滤器: $typeId")
            return cachedFilters
        }
        
        val filters = mutableListOf<FilterGroup>()
        
        try {
            // 智能检测过滤器
            if (detectFilters(html, typeId)) {
                // 解析年份过滤器（Mac 增强版）
                val yearFilter = parseMacYearFilter(html, config)
                if (yearFilter != null) {
                    filters.add(yearFilter)
                }
                
                // 解析地区过滤器（Mac 增强版）
                val areaFilter = parseMacAreaFilter(html, config)
                if (areaFilter != null) {
                    filters.add(areaFilter)
                }
                
                // 解析类型过滤器（Mac 增强版）
                val genreFilter = parseMacGenreFilter(html, config)
                if (genreFilter != null) {
                    filters.add(genreFilter)
                }
                
                // 解析排序过滤器（Mac 增强版）
                val sortFilter = parseMacSortFilter(html, config)
                if (sortFilter != null) {
                    filters.add(sortFilter)
                }
                
                // 解析状态过滤器（Mac 增强版）
                val statusFilter = parseMacStatusFilter(html, config)
                if (statusFilter != null) {
                    filters.add(statusFilter)
                }
                
                // 解析语言过滤器（Mac 增强版）
                val languageFilter = parseMacLanguageFilter(html, config)
                if (languageFilter != null) {
                    filters.add(languageFilter)
                }
                
                // 解析自定义过滤器（Mac 增强版）
                val customFilters = parseMacCustomFilters(html, config)
                filters.addAll(customFilters)
            } else {
                // 使用默认过滤器
                filters.addAll(createDefaultMacFilters())
            }
            
            // 缓存过滤器
            filterCache[typeId] = filters
            
        } catch (e: Exception) {
            logWarning("⚠️ Mac 过滤器解析失败", e)
            filters.addAll(createDefaultMacFilters())
        }
        
        return filters
    }
    
    /**
     * 🔍 智能检测过滤器
     */
    protected open fun detectFilters(html: String, typeId: String): Boolean {
        // 从缓存获取检测结果
        filterDetectionCache[typeId]?.let { cached ->
            return cached
        }
        
        val hasFilters = try {
            // 检测常见的过滤器元素
            val filterSelectors = listOf(
                ".filter", ".screen", ".condition",
                ".year-filter", ".area-filter", ".genre-filter",
                ".filter-box", ".screen-box", ".condition-box"
            )
            
            filterSelectors.any { selector ->
                JsoupUtils.hasElement(html, selector)
            }
        } catch (e: Exception) {
            logWarning("⚠️ 过滤器检测失败", e)
            false
        }
        
        // 缓存检测结果
        filterDetectionCache[typeId] = hasFilters
        
        logDebug("🔍 过滤器检测结果: $typeId -> $hasFilters")
        return hasFilters
    }
    
    /**
     * 📅 解析 Mac 年份过滤器
     */
    protected open fun parseMacYearFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val yearRules = listOf(
                ".year-filter a, .filter-year a",
                ".screen-year a, .condition-year a",
                ".filter .year a, .screen .year a"
            )
            
            for (rule in yearRules) {
                val years = JsoupUtils.parseRuleArray(html, rule)
                if (years.isNotEmpty()) {
                    val filterValues = mutableListOf<FilterValue>()
                    filterValues.add(FilterValue("全部", ""))
                    
                    years.forEach { year ->
                        val yearText = year.trim()
                        if (yearText.matches("\\d{4}".toRegex()) || yearText == "全部") {
                            val value = if (yearText == "全部") "" else yearText
                            filterValues.add(FilterValue(yearText, value))
                        }
                    }
                    
                    if (filterValues.size > 1) {
                        return FilterGroup("year", "年份", filterValues)
                    }
                }
            }
            
            // 生成默认年份过滤器
            createDefaultMacYearFilter()
        } catch (e: Exception) {
            logWarning("⚠️ Mac 年份过滤器解析失败", e)
            createDefaultMacYearFilter()
        }
    }
    
    /**
     * 🌍 解析 Mac 地区过滤器
     */
    protected open fun parseMacAreaFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val areaRules = listOf(
                ".area-filter a, .filter-area a",
                ".screen-area a, .condition-area a",
                ".filter .area a, .screen .area a"
            )
            
            for (rule in areaRules) {
                val areas = JsoupUtils.parseRuleArray(html, rule)
                if (areas.isNotEmpty()) {
                    val filterValues = mutableListOf<FilterValue>()
                    filterValues.add(FilterValue("全部", ""))
                    
                    areas.forEach { area ->
                        val areaText = area.trim()
                        if (areaText.isNotEmpty() && areaText != "全部") {
                            filterValues.add(FilterValue(areaText, areaText))
                        }
                    }
                    
                    if (filterValues.size > 1) {
                        return FilterGroup("area", "地区", filterValues)
                    }
                }
            }
            
            // 生成默认地区过滤器
            createDefaultMacAreaFilter()
        } catch (e: Exception) {
            logWarning("⚠️ Mac 地区过滤器解析失败", e)
            createDefaultMacAreaFilter()
        }
    }
    
    /**
     * 🎭 解析 Mac 类型过滤器
     */
    protected open fun parseMacGenreFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val genreRules = listOf(
                ".genre-filter a, .filter-genre a",
                ".screen-genre a, .condition-genre a",
                ".filter .genre a, .screen .genre a",
                ".type-filter a, .filter-type a"
            )
            
            for (rule in genreRules) {
                val genres = JsoupUtils.parseRuleArray(html, rule)
                if (genres.isNotEmpty()) {
                    val filterValues = mutableListOf<FilterValue>()
                    filterValues.add(FilterValue("全部", ""))
                    
                    genres.forEach { genre ->
                        val genreText = genre.trim()
                        if (genreText.isNotEmpty() && genreText != "全部") {
                            filterValues.add(FilterValue(genreText, genreText))
                        }
                    }
                    
                    if (filterValues.size > 1) {
                        return FilterGroup("genre", "类型", filterValues)
                    }
                }
            }
            
            // 生成默认类型过滤器
            createDefaultMacGenreFilter()
        } catch (e: Exception) {
            logWarning("⚠️ Mac 类型过滤器解析失败", e)
            createDefaultMacGenreFilter()
        }
    }
    
    /**
     * 📊 解析 Mac 排序过滤器
     */
    protected open fun parseMacSortFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            createDefaultMacSortFilter()
        } catch (e: Exception) {
            logWarning("⚠️ Mac 排序过滤器解析失败", e)
            null
        }
    }
    
    /**
     * 📺 解析 Mac 状态过滤器
     */
    protected open fun parseMacStatusFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val statusRules = listOf(
                ".status-filter a, .filter-status a",
                ".screen-status a, .condition-status a",
                ".filter .status a, .screen .status a"
            )
            
            for (rule in statusRules) {
                val statuses = JsoupUtils.parseRuleArray(html, rule)
                if (statuses.isNotEmpty()) {
                    val filterValues = mutableListOf<FilterValue>()
                    filterValues.add(FilterValue("全部", ""))
                    
                    statuses.forEach { status ->
                        val statusText = status.trim()
                        if (statusText.isNotEmpty() && statusText != "全部") {
                            filterValues.add(FilterValue(statusText, statusText))
                        }
                    }
                    
                    if (filterValues.size > 1) {
                        return FilterGroup("status", "状态", filterValues)
                    }
                }
            }
            
            // 生成默认状态过滤器
            createDefaultMacStatusFilter()
        } catch (e: Exception) {
            logWarning("⚠️ Mac 状态过滤器解析失败", e)
            createDefaultMacStatusFilter()
        }
    }
    
    /**
     * 🌐 解析 Mac 语言过滤器
     */
    protected open fun parseMacLanguageFilter(html: String, config: XPathConfig): FilterGroup? {
        return try {
            val languageRules = listOf(
                ".language-filter a, .filter-language a",
                ".screen-language a, .condition-language a",
                ".filter .language a, .screen .language a"
            )
            
            for (rule in languageRules) {
                val languages = JsoupUtils.parseRuleArray(html, rule)
                if (languages.isNotEmpty()) {
                    val filterValues = mutableListOf<FilterValue>()
                    filterValues.add(FilterValue("全部", ""))
                    
                    languages.forEach { language ->
                        val languageText = language.trim()
                        if (languageText.isNotEmpty() && languageText != "全部") {
                            filterValues.add(FilterValue(languageText, languageText))
                        }
                    }
                    
                    if (filterValues.size > 1) {
                        return FilterGroup("language", "语言", filterValues)
                    }
                }
            }
            
            null // 语言过滤器不是必需的
        } catch (e: Exception) {
            logWarning("⚠️ Mac 语言过滤器解析失败", e)
            null
        }
    }
    
    /**
     * 🔧 解析 Mac 自定义过滤器
     */
    protected open fun parseMacCustomFilters(html: String, config: XPathConfig): List<FilterGroup> {
        val customFilters = mutableListOf<FilterGroup>()
        
        try {
            // 这里可以根据具体网站的过滤器结构进行解析
            // 示例：解析更多自定义过滤器
            
        } catch (e: Exception) {
            logWarning("⚠️ Mac 自定义过滤器解析失败", e)
        }
        
        return customFilters
    }
    
    // ========== 默认 Mac 过滤器创建 ==========
    
    /**
     * 🔧 创建默认 Mac 过滤器
     */
    protected open fun createDefaultMacFilters(): List<FilterGroup> {
        return listOf(
            createDefaultMacYearFilter(),
            createDefaultMacAreaFilter(),
            createDefaultMacGenreFilter(),
            createDefaultMacSortFilter(),
            createDefaultMacStatusFilter()
        ).filterNotNull()
    }
    
    /**
     * 📅 创建默认 Mac 年份过滤器
     */
    protected open fun createDefaultMacYearFilter(): FilterGroup {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val filterValues = mutableListOf<FilterValue>()
        
        filterValues.add(FilterValue("全部", ""))
        for (year in currentYear downTo currentYear - 15) {
            filterValues.add(FilterValue(year.toString(), year.toString()))
        }
        
        return FilterGroup("year", "年份", filterValues)
    }
    
    /**
     * 🌍 创建默认 Mac 地区过滤器
     */
    protected open fun createDefaultMacAreaFilter(): FilterGroup {
        val areas = listOf("全部", "大陆", "香港", "台湾", "美国", "韩国", "日本", "泰国", "英国", "法国", "德国", "意大利", "西班牙", "印度", "其他")
        val filterValues = areas.map { area ->
            FilterValue(area, if (area == "全部") "" else area)
        }
        
        return FilterGroup("area", "地区", filterValues)
    }
    
    /**
     * 🎭 创建默认 Mac 类型过滤器
     */
    protected open fun createDefaultMacGenreFilter(): FilterGroup {
        val genres = listOf("全部", "动作", "喜剧", "爱情", "科幻", "恐怖", "剧情", "战争", "悬疑", "动画", "犯罪", "奇幻", "冒险", "家庭", "传记", "历史", "音乐", "运动", "西部", "惊悚")
        val filterValues = genres.map { genre ->
            FilterValue(genre, if (genre == "全部") "" else genre)
        }
        
        return FilterGroup("genre", "类型", filterValues)
    }
    
    /**
     * 📊 创建默认 Mac 排序过滤器
     */
    protected open fun createDefaultMacSortFilter(): FilterGroup {
        val sorts = listOf(
            FilterValue("最新", "time"),
            FilterValue("最热", "hits"),
            FilterValue("评分", "score"),
            FilterValue("名称", "name")
        )
        
        return FilterGroup("sort", "排序", sorts)
    }
    
    /**
     * 📺 创建默认 Mac 状态过滤器
     */
    protected open fun createDefaultMacStatusFilter(): FilterGroup {
        val statuses = listOf(
            FilterValue("全部", ""),
            FilterValue("连载", "连载"),
            FilterValue("完结", "完结"),
            FilterValue("预告", "预告")
        )
        
        return FilterGroup("status", "状态", statuses)
    }
    
    // ========== URL 构建方法 ==========
    
    /**
     * 🔗 构建 Mac 版本带过滤器的分类 URL
     */
    protected open fun buildMacFilterCategoryUrl(
        tid: String,
        page: Int,
        extend: HashMap<String, String>,
        config: XPathConfig
    ): String {
        return buildMacCategoryUrl(tid, page, extend, config)
    }
    
    /**
     * 🧹 清理所有缓存
     */
    fun clearAllCache() {
        filterCache.clear()
        filterDetectionCache.clear()
        logDebug("🧹 Mac 过滤器所有缓存已清理")
    }
    
    /**
     * 📊 获取 Mac 过滤器统计
     */
    fun getMacFilterStats(): Map<String, Any> {
        return mapOf(
            "cached_filters" to filterCache.size,
            "cached_detections" to filterDetectionCache.size,
            "filter_keys" to filterCache.keys.toList(),
            "detection_keys" to filterDetectionCache.keys.toList()
        )
    }
}
