package top.cywin.onetv.film.integration

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.engine.EngineManager
import top.cywin.onetv.film.spider.appys.AppYsSpider
import top.cywin.onetv.film.spider.javascript.JavaScriptSpider
import top.cywin.onetv.film.spider.xpath.*

/**
 * Spider 集成测试
 * 
 * 测试所有解析器的集成功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpiderIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var spiderManager: SpiderManager
    private lateinit var engineManager: EngineManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        spiderManager = SpiderManager(context)
        engineManager = EngineManager()
    }
    
    @Test
    fun testSpiderManagerIntegration() = runBlocking {
        // 初始化 Spider 管理器
        spiderManager.clearCache()
        
        // 测试已注册的 Spider 类型
        val registeredSpiders = spiderManager.getRegisteredSpiders()
        assertNotNull(registeredSpiders)
        assertTrue(registeredSpiders.containsKey("csp_XPath"))
        assertTrue(registeredSpiders.containsKey("csp_XPathMac"))
        assertTrue(registeredSpiders.containsKey("csp_XPathFilter"))
        assertTrue(registeredSpiders.containsKey("csp_XPathMacFilter"))
        assertTrue(registeredSpiders.containsKey("csp_AppYs"))
        assertTrue(registeredSpiders.containsKey("csp_JavaScript"))
        
        // 测试缓存统计
        val stats = spiderManager.getCacheStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("cached_spiders"))
        assertTrue(stats.containsKey("registered_types"))
    }
    
    @Test
    fun testXPathSpiderIntegration() = runBlocking {
        val xpathSpider = XPathSpider()
        xpathSpider.init(context, "")
        xpathSpider.setSiteInfo("xpath_test", "XPath测试站点", "https://example.com", emptyMap())
        
        // 测试 Spider 信息
        val spiderInfo = xpathSpider.getSpiderInfo()
        assertNotNull(spiderInfo)
        assertEquals("xpath_test", spiderInfo["siteKey"])
        assertEquals("XPath测试站点", spiderInfo["siteName"])
        
        // 测试配置创建
        val config = xpathSpider.createDefaultConfig()
        assertNotNull(config)
        assertTrue(config.isValid())
        
        // 测试 URL 构建
        val categoryUrl = xpathSpider.buildCategoryUrl("1", 2, hashMapOf(), config)
        assertTrue(categoryUrl.contains("/category/1"))
        
        val detailUrl = xpathSpider.buildDetailUrl("/detail/123", config)
        assertTrue(detailUrl.contains("/detail/123"))
        
        val searchUrl = xpathSpider.buildSearchUrl("测试", config)
        assertTrue(searchUrl.contains("测试") || searchUrl.contains("search"))
    }
    
    @Test
    fun testXPathMacSpiderIntegration() = runBlocking {
        val xpathMacSpider = XPathMacSpider()
        xpathMacSpider.init(context, "")
        xpathMacSpider.setSiteInfo("xpath_mac_test", "XPathMac测试站点", "https://example.com", emptyMap())
        
        // 测试 Mac 配置创建
        val macConfig = xpathMacSpider.createMacDefaultConfig()
        assertNotNull(macConfig)
        assertTrue(macConfig.isValid())
        
        // Mac 配置应该有更多的规则选项
        assertTrue(macConfig.categoryListRule.contains(","))
        assertTrue(macConfig.vodListRule.contains(","))
        
        // 测试 Mac URL 构建
        val macCategoryUrl = xpathMacSpider.buildMacCategoryUrl("1", 2, hashMapOf("year" to "2023"), macConfig)
        assertTrue(macCategoryUrl.contains("/category/1"))
        assertTrue(macCategoryUrl.contains("/page/2"))
    }
    
    @Test
    fun testXPathFilterSpiderIntegration() = runBlocking {
        val xpathFilterSpider = XPathFilterSpider()
        xpathFilterSpider.init(context, "")
        xpathFilterSpider.setSiteInfo("xpath_filter_test", "XPathFilter测试站点", "https://example.com", emptyMap())
        
        // 测试过滤器创建
        val yearFilter = xpathFilterSpider.createDefaultYearFilter()
        assertNotNull(yearFilter)
        assertEquals("year", yearFilter.key)
        assertEquals("年份", yearFilter.name)
        assertTrue(yearFilter.values.isNotEmpty())
        
        val areaFilter = xpathFilterSpider.createDefaultAreaFilter()
        assertNotNull(areaFilter)
        assertEquals("area", areaFilter.key)
        assertEquals("地区", areaFilter.name)
        
        val genreFilter = xpathFilterSpider.createDefaultGenreFilter()
        assertNotNull(genreFilter)
        assertEquals("genre", genreFilter.key)
        assertEquals("类型", genreFilter.name)
        
        // 测试过滤器统计
        val filterStats = xpathFilterSpider.getFilterStats()
        assertNotNull(filterStats)
        assertTrue(filterStats.containsKey("cached_filters"))
        
        // 测试缓存清理
        xpathFilterSpider.clearFilterCache()
    }
    
    @Test
    fun testXPathMacFilterSpiderIntegration() = runBlocking {
        val xpathMacFilterSpider = XPathMacFilterSpider()
        xpathMacFilterSpider.init(context, "")
        xpathMacFilterSpider.setSiteInfo("xpath_mac_filter_test", "XPathMacFilter测试站点", "https://example.com", emptyMap())
        
        // 测试 Mac 过滤器创建
        val macFilters = xpathMacFilterSpider.createDefaultMacFilters()
        assertNotNull(macFilters)
        assertTrue(macFilters.isNotEmpty())
        
        val macYearFilter = xpathMacFilterSpider.createDefaultMacYearFilter()
        assertNotNull(macYearFilter)
        assertEquals("year", macYearFilter.key)
        assertTrue(macYearFilter.values.size > 10) // Mac 版本应该有更多年份选项
        
        val macStatusFilter = xpathMacFilterSpider.createDefaultMacStatusFilter()
        assertNotNull(macStatusFilter)
        assertEquals("status", macStatusFilter.key)
        assertEquals("状态", macStatusFilter.name)
        
        // 测试 Mac 过滤器统计
        val macFilterStats = xpathMacFilterSpider.getMacFilterStats()
        assertNotNull(macFilterStats)
        assertTrue(macFilterStats.containsKey("cached_filters"))
        assertTrue(macFilterStats.containsKey("cached_detections"))
        
        // 测试缓存清理
        xpathMacFilterSpider.clearAllCache()
    }
    
    @Test
    fun testAppYsSpiderIntegration() = runBlocking {
        val appYsSpider = AppYsSpider()
        appYsSpider.init(context, "")
        appYsSpider.setSiteInfo("appys_test", "AppYs测试站点", "https://api.example.com", emptyMap())
        
        // 测试 API URL 构建
        val listUrl = appYsSpider.buildApiUrl("list")
        assertTrue(listUrl.contains("ac=list"))
        
        val videoListUrl = appYsSpider.buildVideoListUrl("1", 2, hashMapOf("year" to "2023"))
        assertTrue(videoListUrl.contains("ac=videolist"))
        assertTrue(videoListUrl.contains("t=1"))
        assertTrue(videoListUrl.contains("pg=2"))
        assertTrue(videoListUrl.contains("year=2023"))
        
        val detailUrl = appYsSpider.buildDetailUrl("123")
        assertTrue(detailUrl.contains("ac=detail"))
        assertTrue(detailUrl.contains("ids=123"))
        
        val searchUrl = appYsSpider.buildSearchUrl("测试")
        assertTrue(searchUrl.contains("ac=search"))
        assertTrue(searchUrl.contains("wd="))
    }
    
    @Test
    fun testJavaScriptSpiderIntegration() = runBlocking {
        val jsSpider = JavaScriptSpider()
        jsSpider.init(context, "")
        jsSpider.setSiteInfo("js_test", "JavaScript测试站点", "https://example.com/spider.js", emptyMap())
        
        // 测试默认脚本生成
        val defaultScript = jsSpider.generateDefaultScript()
        assertNotNull(defaultScript)
        assertTrue(defaultScript.contains("function homeContent"))
        assertTrue(defaultScript.contains("function categoryContent"))
        assertTrue(defaultScript.contains("function detailContent"))
        assertTrue(defaultScript.contains("function searchContent"))
        assertTrue(defaultScript.contains("function playerContent"))
        
        // 测试默认内容构建
        val homeContent = jsSpider.buildDefaultHomeContent()
        assertNotNull(homeContent)
        assertTrue(homeContent.contains("class"))
        
        val categoryContent = jsSpider.buildDefaultCategoryContent("1", "2")
        assertNotNull(categoryContent)
        assertTrue(categoryContent.contains("list"))
        assertTrue(categoryContent.contains("page"))
        
        val detailContent = jsSpider.buildDefaultDetailContent("123")
        assertNotNull(detailContent)
        assertTrue(detailContent.contains("list"))
        
        val searchContent = jsSpider.buildDefaultSearchContent("测试")
        assertNotNull(searchContent)
        assertTrue(searchContent.contains("list"))
        
        val playerContent = jsSpider.buildDefaultPlayerContent("123")
        assertNotNull(playerContent)
        assertTrue(playerContent.contains("playUrl"))
        
        // 测试主机名提取
        val host = jsSpider.extractHost("https://example.com/path/to/page")
        assertEquals("https://example.com", host)
        
        // 清理
        jsSpider.destroy()
    }
    
    @Test
    fun testEngineManagerIntegration() = runBlocking {
        engineManager.initialize(context)
        
        // 测试 JavaScript 站点
        val jsSite = VodSite.createJavaScript("js_test", "JS测试站点", "https://example.com/spider.js")
        val jsResult = engineManager.executeWithFallback(jsSite, "homeContent", mapOf("filter" to false))
        assertTrue(jsResult.isSuccess)
        
        // 测试 AppYs 站点
        val appysSite = VodSite.createAppYs("appys_test", "AppYs测试站点", "https://api.example.com")
        val appysResult = engineManager.executeWithFallback(appysSite, "homeContent", mapOf("filter" to false))
        assertTrue(appysResult.isSuccess)
        
        // 测试自定义 Spider 站点
        val customSite = VodSite.createDefault("custom_test", "自定义测试站点", "csp_XPath")
        val customResult = engineManager.executeWithFallback(customSite, "homeContent", mapOf("filter" to false))
        assertTrue(customResult.isSuccess)
        
        // 测试引擎统计
        val stats = engineManager.getEngineStats()
        assertNotNull(stats)
        assertTrue(stats.isNotEmpty())
        
        // 清理
        engineManager.shutdown()
    }
    
    @Test
    fun testSpiderManagerWithAllSpiders() = runBlocking {
        // 测试所有 Spider 类型的支持
        val xpathSite = VodSite.createDefault("xpath", "XPath站点", "csp_XPath")
        assertTrue(spiderManager.isSpiderSupported(xpathSite))
        
        val xpathMacSite = VodSite.createDefault("xpath_mac", "XPathMac站点", "csp_XPathMac")
        assertTrue(spiderManager.isSpiderSupported(xpathMacSite))
        
        val xpathFilterSite = VodSite.createDefault("xpath_filter", "XPathFilter站点", "csp_XPathFilter")
        assertTrue(spiderManager.isSpiderSupported(xpathFilterSite))
        
        val xpathMacFilterSite = VodSite.createDefault("xpath_mac_filter", "XPathMacFilter站点", "csp_XPathMacFilter")
        assertTrue(spiderManager.isSpiderSupported(xpathMacFilterSite))
        
        val appysSite = VodSite.createDefault("appys", "AppYs站点", "csp_AppYs")
        assertTrue(spiderManager.isSpiderSupported(appysSite))
        
        val jsSite = VodSite.createDefault("js", "JavaScript站点", "csp_JavaScript")
        assertTrue(spiderManager.isSpiderSupported(jsSite))
        
        // 测试不支持的 Spider
        val unknownSite = VodSite.createDefault("unknown", "未知站点", "csp_Unknown")
        assertFalse(spiderManager.isSpiderSupported(unknownSite))
    }
    
    @Test
    fun testDataModelsIntegration() {
        // 测试 AppYs 数据模型
        val appysCategory = top.cywin.onetv.film.spider.appys.AppYsCategory("1", "电影")
        assertEquals("1", appysCategory.typeId)
        assertEquals("电影", appysCategory.typeName)
        
        val appysVodItem = top.cywin.onetv.film.spider.appys.AppYsVodItem("123", "测试视频", "pic.jpg", "更新")
        assertEquals("123", appysVodItem.vodId)
        assertEquals("测试视频", appysVodItem.vodName)
        
        val appysVideoListData = top.cywin.onetv.film.spider.appys.AppYsVideoListData(
            listOf(appysVodItem), 1, 10, 20, 200
        )
        assertEquals(1, appysVideoListData.list.size)
        assertEquals(1, appysVideoListData.page)
        
        // 测试过滤器数据模型
        val filterValue = FilterValue("2023", "2023")
        assertEquals("2023", filterValue.name)
        assertEquals("2023", filterValue.value)
        
        val filterGroup = FilterGroup("year", "年份", listOf(filterValue))
        assertEquals("year", filterGroup.key)
        assertEquals("年份", filterGroup.name)
        assertEquals(1, filterGroup.values.size)
    }
}
