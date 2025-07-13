package top.cywin.onetv.film.specialized

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.spider.custom.*
import top.cywin.onetv.film.spider.special.*

/**
 * 专用和特殊解析器测试
 * 
 * 测试第五阶段实现的所有解析器
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class SpecializedSpiderTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
    }
    
    @Test
    fun testSpecializedSpiderBase() = runBlocking {
        // 测试专用解析器基类功能
        val testSpider = object : SpecializedSpider() {
            override fun parseSpecializedConfig(extend: String): SpecializedConfig {
                return SpecializedConfig(
                    siteName = "测试站点",
                    baseUrl = "https://test.com",
                    enableCache = true
                )
            }
            
            override fun createDefaultConfig(): SpecializedConfig {
                return parseSpecializedConfig("")
            }
            
            override fun getSpecializedHeaders(): Map<String, String> {
                return mapOf("Test-Header" to "Test-Value")
            }
            
            override fun buildSpecializedUrl(path: String, params: Map<String, String>): String {
                return "https://test.com$path"
            }
        }
        
        testSpider.init(context, "")
        testSpider.setSiteInfo("test", "测试站点", "https://test.com", emptyMap())
        
        // 测试配置
        val config = testSpider.siteConfig
        assertNotNull(config)
        assertTrue(config!!.isValid())
        assertEquals("测试站点", config.siteName)
        
        // 测试工具方法
        val text = testSpider.cleanText("  测试文本  ")
        assertEquals("测试文本", text)
        
        val regex = testSpider.regexFind("test123abc", "(\\d+)")
        assertEquals("123", regex)
        
        val playList = testSpider.formatPlayList(listOf("第1集" to "url1", "第2集" to "url2"))
        assertEquals("第1集\$url1#第2集\$url2", playList)
        
        // 测试缓存统计
        val stats = testSpider.getCacheStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("cached_requests"))
        
        testSpider.destroy()
    }
    
    @Test
    fun testYydsAli1Spider() = runBlocking {
        val yydsSpider = YydsAli1Spider()
        yydsSpider.init(context, "")
        yydsSpider.setSiteInfo("yyds_test", "YYDS测试站点", "https://yyds.example.com", emptyMap())
        
        // 测试配置
        val config = yydsSpider.siteConfig
        assertNotNull(config)
        assertEquals("YYDS阿里云盘", config!!.siteName)
        assertTrue(config.enableCache)
        assertEquals(20, config.timeout)
        
        // 测试请求头
        val headers = yydsSpider.getSpecializedHeaders()
        assertNotNull(headers)
        assertTrue(headers.containsKey("Accept"))
        assertEquals("application/json", headers["Accept"])
        
        // 测试 URL 构建
        val homeUrl = yydsSpider.buildSpecializedUrl("/api/home")
        assertTrue(homeUrl.contains("/api/home"))
        
        val categoryUrl = yydsSpider.buildSpecializedUrl("/api/category", mapOf("type" to "1", "page" to "2"))
        assertTrue(categoryUrl.contains("/api/category"))
        assertTrue(categoryUrl.contains("type=1"))
        assertTrue(categoryUrl.contains("page=2"))
        
        // 测试数据模型
        val category = YydsCategory("1", "电影")
        assertEquals("1", category.id)
        assertEquals("电影", category.name)
        
        val videoItem = YydsVideoItem("123", "测试视频", "pic.jpg", "阿里云盘")
        assertEquals("123", videoItem.id)
        assertEquals("测试视频", videoItem.name)
        
        val videoListData = YydsVideoListData(listOf(videoItem), 1, 10, 20, 200)
        assertEquals(1, videoListData.items.size)
        assertEquals(1, videoListData.page)
        
        yydsSpider.destroy()
    }
    
    @Test
    fun testCokemvSpider() = runBlocking {
        val cokemvSpider = CokemvSpider()
        cokemvSpider.init(context, "")
        cokemvSpider.setSiteInfo("cokemv_test", "Cokemv测试站点", "https://cokemv.example.com", emptyMap())
        
        // 测试配置
        val config = cokemvSpider.siteConfig
        assertNotNull(config)
        assertEquals("Cokemv影视", config!!.siteName)
        assertTrue(config.enableCache)
        assertEquals(15, config.timeout)
        
        // 测试请求头
        val headers = cokemvSpider.getSpecializedHeaders()
        assertNotNull(headers)
        assertTrue(headers.containsKey("Accept"))
        
        // 测试 URL 构建
        val categoryUrl = cokemvSpider.buildSpecializedUrl("/vodshow", mapOf("id" to "1", "page" to "2"))
        assertTrue(categoryUrl.contains("/vodshow"))
        assertTrue(categoryUrl.contains("id=1"))
        
        // 测试数据模型
        val category = CokemvCategory("1", "电影")
        assertEquals("1", category.id)
        assertEquals("电影", category.name)
        
        val videoItem = CokemvVideoItem("123", "测试视频", "pic.jpg", "更新")
        assertEquals("123", videoItem.id)
        assertEquals("测试视频", videoItem.name)
        
        val pageInfo = CokemvPageInfo(1, 10, 200)
        assertEquals(1, pageInfo.page)
        assertEquals(10, pageInfo.pageCount)
        
        cokemvSpider.destroy()
    }
    
    @Test
    fun testSpecialSpiderBase() = runBlocking {
        // 测试特殊解析器基类功能
        val testSpider = object : SpecialSpider() {
            override fun parseSpecialConfig(extend: String): SpecialConfig {
                return SpecialConfig(
                    siteName = "特殊测试站点",
                    secretKey = "test_secret",
                    encryptionEnabled = true,
                    authRequired = true
                )
            }
            
            override fun createDefaultSpecialConfig(): SpecialConfig {
                return parseSpecialConfig("")
            }
            
            override suspend fun getAuthInfo(): Map<String, String> {
                return mapOf("Authorization" to "Bearer test_token")
            }
            
            override fun generateDynamicParams(): Map<String, String> {
                return mapOf("timestamp" to "1234567890", "nonce" to "test_nonce")
            }
        }
        
        testSpider.init(context, "")
        testSpider.setSiteInfo("special_test", "特殊测试站点", "https://special.example.com", emptyMap())
        
        // 测试配置
        val config = testSpider.specialConfig
        assertNotNull(config)
        assertTrue(config!!.isValid())
        assertEquals("特殊测试站点", config.siteName)
        assertTrue(config.encryptionEnabled)
        assertTrue(config.authRequired)
        
        // 测试加密工具
        val md5Hash = testSpider.md5("test")
        assertNotNull(md5Hash)
        assertTrue(md5Hash.isNotEmpty())
        assertEquals(32, md5Hash.length)
        
        val sha256Hash = testSpider.sha256("test")
        assertNotNull(sha256Hash)
        assertTrue(sha256Hash.isNotEmpty())
        assertEquals(64, sha256Hash.length)
        
        val base64Encoded = testSpider.base64Encode("test")
        assertEquals("dGVzdA==", base64Encoded)
        
        val base64Decoded = testSpider.base64Decode("dGVzdA==")
        assertEquals("test", base64Decoded)
        
        // 测试时间戳和随机字符串生成
        val timestamp = testSpider.generateTimestamp()
        assertNotNull(timestamp)
        assertTrue(timestamp.toLong() > 0)
        
        val nonce = testSpider.generateNonce(16)
        assertNotNull(nonce)
        assertEquals(16, nonce.length)
        
        // 测试会话管理
        testSpider.saveSessionData("test_key", "test_value")
        val sessionValue = testSpider.getSessionData("test_key")
        assertEquals("test_value", sessionValue)
        
        // 测试特殊字符解码
        val decoded = testSpider.decodeSpecialChars("&amp;&lt;&gt;&quot;")
        assertEquals("&<>\"", decoded)
        
        // 测试统计
        val stats = testSpider.getSpecialStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("session_data_count"))
        assertTrue(stats.containsKey("config_valid"))
        
        testSpider.destroy()
    }
    
    @Test
    fun testThunderSpider() = runBlocking {
        val thunderSpider = ThunderSpider()
        thunderSpider.init(context, "")
        thunderSpider.setSiteInfo("thunder_test", "Thunder测试站点", "https://thunder.example.com", emptyMap())
        
        // 测试配置
        val config = thunderSpider.specialConfig
        assertNotNull(config)
        assertEquals("Thunder迅雷", config!!.siteName)
        assertTrue(config.encryptionEnabled)
        assertTrue(config.authRequired)
        assertTrue(config.signatureRequired)
        assertEquals("thunder_secret_key_2023", config.secretKey)
        
        // 测试认证信息
        val authInfo = thunderSpider.getAuthInfo()
        assertNotNull(authInfo)
        // 由于没有实际认证，authInfo 可能为空
        
        // 测试动态参数生成
        val dynamicParams = thunderSpider.generateDynamicParams()
        assertNotNull(dynamicParams)
        assertTrue(dynamicParams.containsKey("timestamp"))
        assertTrue(dynamicParams.containsKey("nonce"))
        assertTrue(dynamicParams.containsKey("version"))
        
        // 测试数据模型
        val category = ThunderCategory("1", "电影")
        assertEquals("1", category.id)
        assertEquals("电影", category.name)
        
        val videoItem = ThunderVideoItem("123", "测试视频", "pic.jpg", "迅雷")
        assertEquals("123", videoItem.id)
        assertEquals("测试视频", videoItem.name)
        
        val authData = ThunderAuthData("access_token", "refresh_token", 1234567890L)
        assertEquals("access_token", authData.accessToken)
        assertEquals("refresh_token", authData.refreshToken)
        assertEquals(1234567890L, authData.expiresAt)
        
        thunderSpider.destroy()
    }
    
    @Test
    fun testSpecializedConfigValidation() {
        // 测试专用配置验证
        val validConfig = SpecializedConfig(
            siteName = "测试站点",
            baseUrl = "https://test.com",
            enableCache = true
        )
        assertTrue(validConfig.isValid())
        
        val invalidConfig = SpecializedConfig(
            siteName = "",
            baseUrl = "",
            enableCache = false
        )
        assertFalse(invalidConfig.isValid())
        
        // 测试配置摘要
        val summary = validConfig.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("site_name"))
        assertTrue(summary.containsKey("base_url"))
        assertTrue(summary.containsKey("enable_cache"))
    }
    
    @Test
    fun testSpecialConfigValidation() {
        // 测试特殊配置验证
        val validConfig = SpecialConfig(
            siteName = "特殊站点",
            secretKey = "secret",
            encryptionEnabled = true
        )
        assertTrue(validConfig.isValid())
        
        val invalidConfig = SpecialConfig(
            siteName = "",
            secretKey = "",
            encryptionEnabled = false
        )
        assertFalse(invalidConfig.isValid())
        
        // 测试配置摘要
        val summary = validConfig.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("site_name"))
        assertTrue(summary.containsKey("encryption_enabled"))
        assertTrue(summary.containsKey("auth_required"))
    }
    
    @Test
    fun testDataModelsSerialization() {
        // 测试数据模型的基本功能
        
        // YYDS 数据模型
        val yydsVideoDetail = YydsVideoDetail(
            id = "123",
            name = "测试视频",
            content = "测试内容",
            playFrom = listOf("阿里云盘"),
            playUrls = listOf(listOf("第1集" to "url1"))
        )
        assertEquals("123", yydsVideoDetail.id)
        assertEquals("测试视频", yydsVideoDetail.name)
        assertEquals(1, yydsVideoDetail.playFrom.size)
        assertEquals(1, yydsVideoDetail.playUrls.size)
        
        // Cokemv 数据模型
        val cokemvVideoDetail = CokemvVideoDetail(
            id = "456",
            name = "Cokemv测试",
            content = "Cokemv内容",
            playFrom = listOf("播放源1"),
            playUrls = listOf(listOf("第1集" to "url1"))
        )
        assertEquals("456", cokemvVideoDetail.id)
        assertEquals("Cokemv测试", cokemvVideoDetail.name)
        
        // Thunder 数据模型
        val thunderVideoDetail = ThunderVideoDetail(
            id = "789",
            name = "Thunder测试",
            content = "Thunder内容",
            playFrom = listOf("Thunder", "Magnet"),
            playUrls = listOf(listOf("第1集" to "thunder://url1"))
        )
        assertEquals("789", thunderVideoDetail.id)
        assertEquals("Thunder测试", thunderVideoDetail.name)
        assertEquals(2, thunderVideoDetail.playFrom.size)
    }
}
