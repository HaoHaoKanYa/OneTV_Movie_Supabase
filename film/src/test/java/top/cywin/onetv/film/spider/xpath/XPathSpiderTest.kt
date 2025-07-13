package top.cywin.onetv.film.spider.xpath

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.data.models.XPathConfig
import top.cywin.onetv.film.utils.JsoupUtils
import top.cywin.onetv.film.utils.UrlUtils

/**
 * XPath 解析器测试
 * 
 * 测试 XPath 解析器系列的功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class XPathSpiderTest {
    
    private lateinit var context: Context
    private lateinit var xpathSpider: XPathSpider
    private lateinit var xpathMacSpider: XPathMacSpider
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        xpathSpider = XPathSpider()
        xpathMacSpider = XPathMacSpider()
    }
    
    @Test
    fun testXPathConfig() {
        // 测试默认配置
        val defaultConfig = XPathConfig.createDefault()
        assertTrue(defaultConfig.isValid())
        assertNotNull(defaultConfig.categoryListRule)
        assertNotNull(defaultConfig.vodListRule)
        
        // 测试通用配置
        val genericConfig = XPathConfig.createGeneric()
        assertTrue(genericConfig.isValid())
        assertTrue(genericConfig.categoryListRule.contains("nav"))
        
        // 测试配置摘要
        val summary = defaultConfig.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("has_category_rules"))
        assertTrue(summary.containsKey("has_vod_rules"))
    }
    
    @Test
    fun testJsoupUtils() {
        val html = """
            <html>
                <body>
                    <div class="nav-item">
                        <a href="/category/1">电影</a>
                    </div>
                    <div class="nav-item">
                        <a href="/category/2">电视剧</a>
                    </div>
                    <div class="video-item">
                        <a href="/detail/123">
                            <img src="/pic/123.jpg" />
                            <div class="title">测试视频</div>
                            <div class="remarks">更新至第10集</div>
                        </a>
                    </div>
                </body>
            </html>
        """.trimIndent()
        
        // 测试元素选择
        val navItems = JsoupUtils.selectElements(html, ".nav-item")
        assertEquals(2, navItems.size)
        
        // 测试规则解析
        val firstCategoryName = JsoupUtils.parseRule(html, ".nav-item:first-child a@text")
        assertEquals("电影", firstCategoryName)
        
        val firstCategoryId = JsoupUtils.parseRule(html, ".nav-item:first-child a@href")
        assertEquals("/category/1", firstCategoryId)
        
        // 测试视频信息解析
        val videoTitle = JsoupUtils.parseRule(html, ".video-item .title@text")
        assertEquals("测试视频", videoTitle)
        
        val videoRemarks = JsoupUtils.parseRule(html, ".video-item .remarks@text")
        assertEquals("更新至第10集", videoRemarks)
        
        // 测试规则数组解析
        val categoryNames = JsoupUtils.parseRuleArray(html, ".nav-item a@text")
        assertEquals(2, categoryNames.size)
        assertEquals("电影", categoryNames[0])
        assertEquals("电视剧", categoryNames[1])
        
        // 测试元素存在性
        assertTrue(JsoupUtils.hasElement(html, ".nav-item"))
        assertFalse(JsoupUtils.hasElement(html, ".not-exist"))
        
        // 测试元素数量
        assertEquals(2, JsoupUtils.getElementCount(html, ".nav-item"))
        assertEquals(1, JsoupUtils.getElementCount(html, ".video-item"))
    }
    
    @Test
    fun testUrlUtils() {
        val baseUrl = "https://example.com"
        val relativePath = "/category/1"
        
        // 测试 URL 解析
        val resolvedUrl = UrlUtils.resolveUrl(baseUrl, relativePath)
        assertEquals("https://example.com/category/1", resolvedUrl)
        
        // 测试绝对 URL
        val absoluteUrl = "https://other.com/test"
        val resolvedAbsolute = UrlUtils.resolveUrl(baseUrl, absoluteUrl)
        assertEquals(absoluteUrl, resolvedAbsolute)
        
        // 测试域名提取
        val domain = UrlUtils.extractDomain("https://example.com/path/to/page")
        assertEquals("https://example.com", domain)
        
        // 测试主机名提取
        val host = UrlUtils.extractHost("https://example.com:8080/path")
        assertEquals("example.com", host)
        
        // 测试路径提取
        val path = UrlUtils.extractPath("https://example.com/path/to/page")
        assertEquals("/path/to/page", path)
        
        // 测试查询参数
        val urlWithParams = "https://example.com/search?q=test&page=1"
        val params = UrlUtils.parseQueryParams(urlWithParams)
        assertEquals(2, params.size)
        assertEquals("test", params["q"])
        assertEquals("1", params["page"])
        
        // 测试添加参数
        val urlWithNewParams = UrlUtils.addParams(baseUrl, mapOf("key" to "value", "page" to "2"))
        assertTrue(urlWithNewParams.contains("key=value"))
        assertTrue(urlWithNewParams.contains("page=2"))
        
        // 测试 URL 有效性
        assertTrue(UrlUtils.isValidUrl("https://example.com"))
        assertFalse(UrlUtils.isValidUrl("not-a-url"))
        
        // 测试 HTTPS 检查
        assertTrue(UrlUtils.isHttps("https://example.com"))
        assertFalse(UrlUtils.isHttps("http://example.com"))
        
        // 测试文件扩展名
        val extension = UrlUtils.getFileExtension("https://example.com/video.mp4")
        assertEquals("mp4", extension)
        
        // 测试文件名
        val fileName = UrlUtils.getFileName("https://example.com/path/video.mp4")
        assertEquals("video.mp4", fileName)
    }
    
    @Test
    fun testXPathSpider() = runBlocking {
        xpathSpider.init(context, "")
        xpathSpider.setSiteInfo("test", "测试站点", "https://example.com", emptyMap())
        
        // 测试 Spider 信息
        val spiderInfo = xpathSpider.getSpiderInfo()
        assertNotNull(spiderInfo)
        assertEquals("test", spiderInfo["siteKey"])
        assertEquals("测试站点", spiderInfo["siteName"])
        
        // 测试默认配置创建
        val defaultConfig = xpathSpider.createDefaultConfig()
        assertNotNull(defaultConfig)
        assertTrue(defaultConfig.isValid())
        
        // 由于没有真实的网络请求，这里主要测试方法调用不会抛异常
        try {
            // 这些方法会因为网络请求失败而抛异常，但我们主要测试方法存在
            xpathSpider.homeContent(false)
        } catch (e: Exception) {
            // 预期的网络异常
            assertTrue(e.message?.contains("failed") == true || e.message?.contains("error") == true)
        }
    }
    
    @Test
    fun testXPathMacSpider() = runBlocking {
        xpathMacSpider.init(context, "")
        xpathMacSpider.setSiteInfo("test_mac", "测试Mac站点", "https://example.com", emptyMap())
        
        // 测试 Mac 配置创建
        val macConfig = xpathMacSpider.createMacDefaultConfig()
        assertNotNull(macConfig)
        assertTrue(macConfig.isValid())
        
        // Mac 配置应该有更多的规则选项
        assertTrue(macConfig.categoryListRule.contains(","))
        assertTrue(macConfig.vodListRule.contains(","))
        
        // 测试 Spider 信息
        val spiderInfo = xpathMacSpider.getSpiderInfo()
        assertNotNull(spiderInfo)
        assertEquals("test_mac", spiderInfo["siteKey"])
        assertEquals("测试Mac站点", spiderInfo["siteName"])
    }
    
    @Test
    fun testXPathSpiderMethods() {
        // 测试 URL 构建方法
        val config = XPathConfig.createDefault()
        
        val categoryUrl = xpathSpider.buildCategoryUrl("1", 2, hashMapOf("year" to "2023"), config)
        assertTrue(categoryUrl.contains("/category/1"))
        assertTrue(categoryUrl.contains("/page/2"))
        
        val detailUrl = xpathSpider.buildDetailUrl("/detail/123", config)
        assertTrue(detailUrl.contains("/detail/123"))
        
        val searchUrl = xpathSpider.buildSearchUrl("测试", config)
        assertTrue(searchUrl.contains("测试") || searchUrl.contains("search"))
    }
    
    @Test
    fun testXPathMacSpiderMethods() {
        // 测试 Mac 版本的 URL 构建方法
        val config = XPathConfig.createDefault()
        
        val macCategoryUrl = xpathMacSpider.buildMacCategoryUrl("1", 2, hashMapOf("year" to "2023"), config)
        assertTrue(macCategoryUrl.contains("/category/1"))
        assertTrue(macCategoryUrl.contains("/page/2"))
        
        val macDetailUrl = xpathMacSpider.buildMacDetailUrl("/detail/123", config)
        assertTrue(macDetailUrl.contains("/detail/123"))
    }
    
    @Test
    fun testDataModels() {
        // 测试 CategoryItem
        val category = CategoryItem("1", "电影")
        assertEquals("1", category.typeId)
        assertEquals("电影", category.typeName)
        
        // 测试 VodItem
        val vodItem = VodItem("123", "测试视频", "https://example.com/pic.jpg", "更新至第10集")
        assertEquals("123", vodItem.vodId)
        assertEquals("测试视频", vodItem.vodName)
        assertEquals("https://example.com/pic.jpg", vodItem.vodPic)
        assertEquals("更新至第10集", vodItem.vodRemarks)
        
        // 测试 VodDetail
        val vodDetail = VodDetail(
            vodId = "123",
            vodName = "测试视频详情",
            vodPic = "https://example.com/pic.jpg",
            vodContent = "这是测试内容",
            vodYear = "2023",
            vodArea = "中国",
            vodActor = "测试演员",
            vodDirector = "测试导演",
            vodPlayFrom = "播放源1",
            vodPlayUrl = "第1集\$http://example.com/play1"
        )
        assertEquals("123", vodDetail.vodId)
        assertEquals("测试视频详情", vodDetail.vodName)
        assertEquals("2023", vodDetail.vodYear)
        
        // 测试 PageInfo
        val pageInfo = PageInfo(1, 10, 20, 200)
        assertEquals(1, pageInfo.currentPage)
        assertEquals(10, pageInfo.totalPages)
        assertEquals(20, pageInfo.pageSize)
        assertEquals(200, pageInfo.totalCount)
    }
}
