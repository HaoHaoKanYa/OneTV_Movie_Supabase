package top.cywin.onetv.film.hook

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.hook.builtin.*
import top.cywin.onetv.film.proxy.*

/**
 * Hook 系统和代理系统完整测试
 * 
 * 测试第六阶段实现的所有功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class HookSystemTest {
    
    private lateinit var context: Context
    private lateinit var hookManager: HookManager
    private lateinit var proxyManager: ProxyManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        hookManager = HookManager()
        proxyManager = ProxyManager(context, hookManager)
    }
    
    @Test
    fun testHookBasicFunctionality() = runBlocking {
        // 测试 Hook 基础功能
        
        // 创建测试 Hook
        val testHook = object : RequestHook {
            override val name: String = "TestHook"
            override val description: String = "测试 Hook"
            override val priority: Int = 50
            
            override fun matches(context: HookContext): Boolean {
                return context is RequestHookContext
            }
            
            override suspend fun processRequest(request: HookRequest): HookRequest {
                return request.copy().addHeader("Test-Header", "Test-Value")
            }
        }
        
        // 测试 Hook 属性
        assertEquals("TestHook", testHook.name)
        assertEquals("测试 Hook", testHook.description)
        assertEquals(50, testHook.priority)
        assertTrue(testHook.enabled)
        
        // 测试 Hook 匹配
        val requestContext = RequestHookContext(
            HookRequest("https://example.com")
        )
        assertTrue(testHook.matches(requestContext))
        
        // 测试 Hook 处理
        val originalRequest = HookRequest("https://example.com")
        val processedRequest = testHook.processRequest(originalRequest)
        
        assertEquals("https://example.com", processedRequest.url)
        assertTrue(processedRequest.headers.containsKey("Test-Header"))
        assertEquals("Test-Value", processedRequest.headers["Test-Header"])
    }
    
    @Test
    fun testHookManager() = runBlocking {
        hookManager.initialize()
        
        // 测试 Hook 注册
        val testRequestHook = UserAgentHook()
        hookManager.registerRequestHook(testRequestHook)
        
        val testResponseHook = ContentTypeHook()
        hookManager.registerResponseHook(testResponseHook)
        
        val testPlayerHook = M3U8Hook()
        hookManager.registerPlayerHook(testPlayerHook)
        
        // 测试已注册的 Hook
        val registeredHooks = hookManager.getRegisteredHooks()
        assertNotNull(registeredHooks)
        assertTrue(registeredHooks.containsKey("request_hooks"))
        assertTrue(registeredHooks.containsKey("response_hooks"))
        assertTrue(registeredHooks.containsKey("player_hooks"))
        
        // 测试请求 Hook 执行
        val request = HookRequest("https://example.com")
        val processedRequest = hookManager.executeRequestHooks(request)
        
        assertNotNull(processedRequest)
        assertTrue(processedRequest.headers.containsKey("User-Agent"))
        
        // 测试响应 Hook 执行
        val response = HookResponse(200, body = "{\"test\": \"data\"}")
        val processedResponse = hookManager.executeResponseHooks(response)
        
        assertNotNull(processedResponse)
        assertEquals(200, processedResponse.statusCode)
        
        // 测试播放器 Hook 执行
        val playerUrl = HookPlayerUrl("https://example.com/video.m3u8")
        val processedPlayerUrl = hookManager.executePlayerHooks(playerUrl)
        
        assertNotNull(processedPlayerUrl)
        assertTrue(processedPlayerUrl.headers.containsKey("Accept"))
        
        // 测试统计信息
        val stats = hookManager.getHookStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("request_hooks"))
        assertTrue(stats.containsKey("response_hooks"))
        assertTrue(stats.containsKey("player_hooks"))
        
        hookManager.shutdown()
    }
    
    @Test
    fun testBuiltInHooks() = runBlocking {
        // 测试 UserAgent Hook
        val userAgentHook = UserAgentHook()
        val request = HookRequest("https://example.com")
        val processedRequest = userAgentHook.processRequest(request)
        
        assertTrue(processedRequest.headers.containsKey("User-Agent"))
        assertFalse(processedRequest.headers["User-Agent"]!!.contains("okhttp"))
        
        // 测试 Referer Hook
        val refererHook = RefererHook()
        val requestWithoutReferer = HookRequest("https://example.com/path")
        val processedWithReferer = refererHook.processRequest(requestWithoutReferer)
        
        assertTrue(processedWithReferer.headers.containsKey("Referer"))
        assertEquals("https://example.com", processedWithReferer.headers["Referer"])
        
        // 测试 ContentType Hook
        val contentTypeHook = ContentTypeHook()
        val jsonResponse = HookResponse(200, body = "{\"test\": \"data\"}")
        val processedJsonResponse = contentTypeHook.processResponse(jsonResponse)
        
        assertTrue(processedJsonResponse.headers.containsKey("Content-Type"))
        assertEquals("application/json", processedJsonResponse.headers["Content-Type"])
        
        // 测试 M3U8 Hook
        val m3u8Hook = M3U8Hook()
        val m3u8Context = PlayerHookContext(
            HookPlayerUrl("https://example.com/playlist.m3u8")
        )
        assertTrue(m3u8Hook.matches(m3u8Context))
        
        val m3u8PlayerUrl = HookPlayerUrl("https://example.com/playlist.m3u8")
        val processedM3u8 = m3u8Hook.processPlayerUrl(m3u8PlayerUrl)
        
        assertTrue(processedM3u8.headers.containsKey("Accept"))
        assertEquals("application/vnd.apple.mpegurl", processedM3u8.headers["Accept"])
        
        // 测试 MP4 Hook
        val mp4Hook = Mp4Hook()
        val mp4Context = PlayerHookContext(
            HookPlayerUrl("https://example.com/video.mp4")
        )
        assertTrue(mp4Hook.matches(mp4Context))
        
        val mp4PlayerUrl = HookPlayerUrl("https://example.com/video.mp4")
        val processedMp4 = mp4Hook.processPlayerUrl(mp4PlayerUrl)
        
        assertTrue(processedMp4.headers.containsKey("Accept"))
        assertTrue(processedMp4.headers["Accept"]!!.contains("video/mp4"))
    }
    
    @Test
    fun testHookResult() {
        // 测试成功结果
        val successResult = HookResult.success("test data")
        assertTrue(successResult is HookResult.Success)
        assertEquals("test data", (successResult as HookResult.Success).data)
        
        // 测试失败结果
        val failureResult = HookResult.failure("test error")
        assertTrue(failureResult is HookResult.Failure)
        assertEquals("test error", (failureResult as HookResult.Failure).error)
        
        // 测试跳过结果
        val skipResult = HookResult.skip("test skip")
        assertTrue(skipResult is HookResult.Skip)
        assertEquals("test skip", (skipResult as HookResult.Skip).reason)
        
        // 测试停止结果
        val stopResult = HookResult.stop("test stop")
        assertTrue(stopResult is HookResult.Stop)
        assertEquals("test stop", (stopResult as HookResult.Stop).data)
    }
    
    @Test
    fun testHookStats() {
        val stats = HookStats("TestHook")
        
        // 测试初始状态
        assertEquals("TestHook", stats.hookName)
        assertEquals(0L, stats.executionCount)
        assertEquals(0L, stats.successCount)
        assertEquals(0L, stats.failureCount)
        assertEquals(0L, stats.skipCount)
        assertEquals(0.0, stats.successRate, 0.01)
        
        // 测试记录执行
        stats.recordExecution(HookResult.success(), 100L)
        assertEquals(1L, stats.executionCount)
        assertEquals(1L, stats.successCount)
        assertEquals(1.0, stats.successRate, 0.01)
        
        stats.recordExecution(HookResult.failure("error"), 200L)
        assertEquals(2L, stats.executionCount)
        assertEquals(1L, stats.failureCount)
        assertEquals(0.5, stats.successRate, 0.01)
        
        // 测试报告
        val report = stats.getReport()
        assertNotNull(report)
        assertTrue(report.containsKey("hook_name"))
        assertTrue(report.containsKey("execution_count"))
        assertTrue(report.containsKey("success_rate"))
    }
    
    @Test
    fun testProxyServer() = runBlocking {
        hookManager.initialize()
        
        val proxyServer = ProxyServer(0, hookManager)
        
        // 测试启动
        val port = proxyServer.start()
        assertTrue(port > 0)
        
        // 测试状态
        val status = proxyServer.getStatus()
        assertTrue(status.isRunning)
        assertEquals(port, status.port)
        assertEquals(0, status.activeConnections)
        
        // 测试代理 URL 生成
        val originalUrl = "https://example.com/test"
        val proxyUrl = proxyServer.getProxyUrl(originalUrl)
        assertTrue(proxyUrl.contains("127.0.0.1"))
        assertTrue(proxyUrl.contains(port.toString()))
        assertTrue(proxyUrl.contains("url="))
        
        // 测试停止
        proxyServer.stop()
        val stoppedStatus = proxyServer.getStatus()
        assertFalse(stoppedStatus.isRunning)
    }
    
    @Test
    fun testProxyManager() = runBlocking {
        proxyManager.initialize()
        
        // 测试管理器状态
        val status = proxyManager.getManagerStatus()
        assertTrue(status.isRunning)
        assertTrue(status.totalServers > 0)
        
        // 测试配置管理
        val configs = proxyManager.getAllConfigs()
        assertNotNull(configs)
        assertTrue(configs.isNotEmpty())
        
        // 测试代理 URL 生成
        val originalUrl = "https://example.com/test"
        val proxyUrl = proxyManager.getProxyUrl(originalUrl)
        assertTrue(proxyUrl.contains("127.0.0.1"))
        
        // 测试运行中的服务器
        val runningServers = proxyManager.getRunningServers()
        assertNotNull(runningServers)
        
        proxyManager.shutdown()
    }
    
    @Test
    fun testLocalProxyServer() = runBlocking {
        hookManager.initialize()
        
        val config = LocalProxyConfig(
            port = 0,
            maxConnections = 10,
            maxCacheSize = 1024 * 1024L // 1MB
        )
        
        val localProxyServer = LocalProxyServer(context, hookManager, config)
        
        // 测试启动
        val port = localProxyServer.start()
        assertTrue(port > 0)
        
        // 测试状态
        val status = localProxyServer.getStatus()
        assertTrue(status.isRunning)
        assertEquals(port, status.port)
        
        // 测试代理选项
        val options = ProxyOptions(
            enableCache = true,
            enableRangeRequest = true,
            userAgent = "Test-Agent"
        )
        
        val originalUrl = "https://example.com/video.mp4"
        val proxyUrl = localProxyServer.getProxyUrl(originalUrl, options)
        assertTrue(proxyUrl.contains("cache=1"))
        assertTrue(proxyUrl.contains("range=1"))
        assertTrue(proxyUrl.contains("ua="))
        
        // 测试缓存统计
        val cacheStats = localProxyServer.getCacheStats()
        assertNotNull(cacheStats)
        assertTrue(cacheStats.containsKey("cache_size"))
        
        // 测试停止
        localProxyServer.stop()
        val stoppedStatus = localProxyServer.getStatus()
        assertFalse(stoppedStatus.isRunning)
    }
    
    @Test
    fun testProxyCacheManager() {
        val cacheManager = LocalProxyCacheManager(1024 * 1024L) // 1MB
        
        // 测试缓存存储和获取
        val key = "test_key"
        val data = "test data".toByteArray()
        
        cacheManager.put(key, data)
        val retrievedData = cacheManager.get(key)
        
        assertNotNull(retrievedData)
        assertArrayEquals(data, retrievedData)
        
        // 测试缓存键生成
        val url = "https://example.com/test"
        val headers = mapOf("User-Agent" to "Test")
        val generatedKey = cacheManager.generateKey(url, headers)
        
        assertNotNull(generatedKey)
        assertTrue(generatedKey.isNotEmpty())
        assertEquals(32, generatedKey.length) // MD5 长度
        
        // 测试统计
        val stats = cacheManager.getStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("cache_size"))
        assertTrue(stats.containsKey("hit_count"))
        assertTrue(stats.containsKey("miss_count"))
        
        // 测试清空
        cacheManager.clearAll()
        val clearedData = cacheManager.get(key)
        assertNull(clearedData)
    }
    
    @Test
    fun testBandwidthManager() {
        val bandwidthManager = BandwidthManager(1024L) // 1KB/s
        
        // 测试传输检查
        assertTrue(bandwidthManager.canTransfer("conn1", 512L))
        assertTrue(bandwidthManager.canTransfer("conn1", 512L))
        assertFalse(bandwidthManager.canTransfer("conn1", 1L)) // 超出限制
        
        // 测试传输记录
        bandwidthManager.recordTransfer("conn1", 512L)
        
        // 测试延迟计算
        val delay = bandwidthManager.calculateDelay("conn1", 1024L)
        assertTrue(delay >= 0)
        
        // 测试统计
        val stats = bandwidthManager.getStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("max_bandwidth"))
        assertTrue(stats.containsKey("current_usage"))
        assertTrue(stats.containsKey("usage_percentage"))
        
        // 测试更新带宽
        bandwidthManager.updateMaxBandwidth(2048L)
        assertTrue(bandwidthManager.canTransfer("conn2", 1024L))
    }
    
    @Test
    fun testAccessController() {
        val allowedHosts = setOf("127.0.0.1", "localhost")
        val accessController = AccessController(allowedHosts)
        
        // 测试允许的主机
        assertTrue(accessController.isAllowed("127.0.0.1"))
        assertTrue(accessController.isAllowed("localhost"))
        
        // 测试不允许的主机
        assertFalse(accessController.isAllowed("192.168.1.1"))
        assertFalse(accessController.isAllowed("example.com"))
        
        // 测试通配符
        val wildcardController = AccessController(setOf("*"))
        assertTrue(wildcardController.isAllowed("any.host.com"))
        
        // 测试更新允许列表
        accessController.updateAllowedHosts(setOf("192.168.1.1"))
        assertTrue(accessController.isAllowed("192.168.1.1"))
        assertFalse(accessController.isAllowed("127.0.0.1"))
    }
}
