package top.cywin.onetv.film.catvod

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.data.models.VodSite

/**
 * CatVod Spider 测试
 * 
 * 测试 CatVod 核心架构的基础功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpiderTest {
    
    private lateinit var context: Context
    private lateinit var spiderManager: SpiderManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        spiderManager = SpiderManager(context)
    }
    
    @Test
    fun testSpiderNull() = runBlocking {
        val nullSpider = SpiderNull()
        
        // 测试初始化
        nullSpider.init(context, "")
        
        // 测试 homeContent
        val homeResult = nullSpider.homeContent(false)
        assertNotNull(homeResult)
        assertTrue(homeResult.contains("class"))
        
        // 测试 categoryContent
        val categoryResult = nullSpider.categoryContent("1", "1", false, hashMapOf())
        assertNotNull(categoryResult)
        assertTrue(categoryResult.contains("list"))
        assertTrue(categoryResult.contains("page"))
        
        // 测试 detailContent
        val detailResult = nullSpider.detailContent(listOf("123"))
        assertNotNull(detailResult)
        assertTrue(detailResult.contains("list"))
        
        // 测试 searchContent
        val searchResult = nullSpider.searchContent("测试", false)
        assertNotNull(searchResult)
        assertTrue(searchResult.contains("list"))
        
        // 测试 playerContent
        val playerResult = nullSpider.playerContent("", "123", emptyList())
        assertNotNull(playerResult)
        assertTrue(playerResult.contains("playUrl"))
        
        // 测试销毁
        nullSpider.destroy()
    }
    
    @Test
    fun testSpiderManager() = runBlocking {
        // 测试获取缓存统计
        val stats = spiderManager.getCacheStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("cached_spiders"))
        assertTrue(stats.containsKey("registered_types"))
        
        // 测试获取已注册的 Spider
        val registered = spiderManager.getRegisteredSpiders()
        assertNotNull(registered)
        
        // 测试清理缓存
        spiderManager.clearCache()
        
        // 测试销毁
        spiderManager.destroy()
    }
    
    @Test
    fun testVodSite() {
        // 测试创建默认站点
        val defaultSite = VodSite.createDefault("test", "测试站点", "csp_Test")
        assertTrue(defaultSite.isValid())
        assertTrue(defaultSite.isSearchable())
        assertEquals("自定义 Spider", defaultSite.getTypeDescription())
        
        // 测试创建 AppYs 站点
        val appysSite = VodSite.createAppYs("appys", "AppYs站点", "https://api.example.com")
        assertTrue(appysSite.isValid())
        assertEquals(1, appysSite.type)
        assertEquals("AppYs 接口", appysSite.getTypeDescription())
        
        // 测试创建 JavaScript 站点
        val jsSite = VodSite.createJavaScript("js", "JS站点", "https://example.com/spider.js")
        assertTrue(jsSite.isValid())
        assertEquals(3, jsSite.type)
        
        // 测试站点摘要
        val summary = defaultSite.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("key"))
        assertTrue(summary.containsKey("name"))
        assertTrue(summary.containsKey("type"))
    }
    
    @Test
    fun testSpiderDebug() = runBlocking {
        // 配置调试
        SpiderDebug.configure(
            debugEnabled = true,
            performanceMonitorEnabled = true,
            detailedLoggingEnabled = true
        )
        
        val nullSpider = SpiderNull()
        nullSpider.init(context, "")
        
        // 测试调试操作
        val result = SpiderDebug.debugSpiderOperation(
            spider = nullSpider,
            operation = "homeContent",
            params = mapOf("filter" to false)
        ) {
            nullSpider.homeContent(false)
        }
        
        assertNotNull(result)
        assertTrue(result.contains("class"))
        
        // 测试性能统计
        val performanceStats = SpiderDebug.getPerformanceStats()
        assertNotNull(performanceStats)
        
        // 测试错误统计
        val errorStats = SpiderDebug.getErrorStats()
        assertNotNull(errorStats)
        
        // 清理统计
        SpiderDebug.clearStats()
    }
    
    @Test
    fun testSpiderManagerWithSite() = runBlocking {
        val testSite = VodSite.createDefault("test", "测试站点", "csp_Test")
        
        // 测试检查 Spider 支持
        val isSupported = spiderManager.isSpiderSupported(testSite)
        // 由于还没有注册 csp_Test，应该返回 false
        assertFalse(isSupported)
        
        // 测试获取 Spider（应该返回 SpiderNull）
        val spider = spiderManager.getSpider(testSite)
        assertNotNull(spider)
        assertTrue(spider is SpiderNull)
        
        // 测试清理站点缓存
        spiderManager.clearSiteCache("test")
        
        // 测试 Spider 测试功能
        val testResult = spiderManager.testSpider(testSite)
        // 由于是 SpiderNull，测试应该失败
        assertFalse(testResult)
    }
    
    @Test
    fun testSpiderInfo() {
        val nullSpider = SpiderNull()
        nullSpider.init(context, "test_extend")
        nullSpider.setSiteInfo("test", "测试站点", "https://test.com", mapOf("User-Agent" to "Test"))
        
        val info = nullSpider.getSpiderInfo()
        assertNotNull(info)
        assertTrue(info.containsKey("name"))
        assertTrue(info.containsKey("siteKey"))
        assertTrue(info.containsKey("siteName"))
        assertTrue(info.containsKey("siteUrl"))
        assertTrue(info.containsKey("initialized"))
        
        assertEquals("test", info["siteKey"])
        assertEquals("测试站点", info["siteName"])
        assertEquals("https://test.com", info["siteUrl"])
        assertEquals(true, info["initialized"])
    }
    
    @Test
    fun testSpiderVideoFormat() {
        val nullSpider = SpiderNull()
        
        // 测试视频格式检测
        assertFalse(nullSpider.isVideoFormat("https://example.com/video.mp4"))
        assertFalse(nullSpider.isVideoFormat("https://example.com/video.mkv"))
        assertFalse(nullSpider.isVideoFormat("https://example.com/playlist.m3u8"))
        
        // SpiderNull 总是返回 false
        assertFalse(nullSpider.isVideoFormat("not_a_video.txt"))
    }
    
    @Test
    fun testSpiderManualVideoCheck() {
        val nullSpider = SpiderNull()
        
        // SpiderNull 不需要手动检查视频
        assertFalse(nullSpider.manualVideoCheck())
    }
}
