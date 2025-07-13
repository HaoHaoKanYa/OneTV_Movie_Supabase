package top.cywin.onetv.film.network

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.hook.HookManager

/**
 * 网络系统完整测试
 * 
 * 测试第八阶段实现的所有网络功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class NetworkSystemTest {
    
    private lateinit var context: Context
    private lateinit var hookManager: HookManager
    private lateinit var networkClient: NetworkClient
    private lateinit var networkCacheManager: NetworkCacheManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        hookManager = HookManager()
        networkClient = NetworkClient(context, hookManager)
        networkCacheManager = NetworkCacheManager(context)
    }
    
    @Test
    fun testNetworkConfig() {
        // 测试默认配置
        val defaultConfig = NetworkConfig()
        assertEquals(15000L, defaultConfig.connectTimeout)
        assertEquals(15000L, defaultConfig.readTimeout)
        assertEquals(15000L, defaultConfig.writeTimeout)
        assertEquals(3, defaultConfig.maxRetries)
        assertEquals(1000L, defaultConfig.retryDelay)
        assertFalse(defaultConfig.enableLogging)
        assertFalse(defaultConfig.trustAllCertificates)
        assertEquals("OneTV-NetworkClient/1.0", defaultConfig.userAgent)
        assertEquals(5, defaultConfig.maxIdleConnections)
        assertEquals(5 * 60 * 1000L, defaultConfig.keepAliveDuration)
        
        // 测试自定义配置
        val customConfig = NetworkConfig(
            connectTimeout = 30000L,
            readTimeout = 30000L,
            writeTimeout = 30000L,
            maxRetries = 5,
            retryDelay = 2000L,
            enableLogging = true,
            trustAllCertificates = true,
            userAgent = "Custom-Agent/1.0",
            maxIdleConnections = 10,
            keepAliveDuration = 10 * 60 * 1000L
        )
        
        assertEquals(30000L, customConfig.connectTimeout)
        assertEquals(30000L, customConfig.readTimeout)
        assertEquals(30000L, customConfig.writeTimeout)
        assertEquals(5, customConfig.maxRetries)
        assertEquals(2000L, customConfig.retryDelay)
        assertTrue(customConfig.enableLogging)
        assertTrue(customConfig.trustAllCertificates)
        assertEquals("Custom-Agent/1.0", customConfig.userAgent)
        assertEquals(10, customConfig.maxIdleConnections)
        assertEquals(10 * 60 * 1000L, customConfig.keepAliveDuration)
    }
    
    @Test
    fun testNetworkResponse() {
        // 测试成功响应
        val successData = "test response data".toByteArray()
        val successHeaders = mapOf("Content-Type" to "application/json")
        val successResponse = NetworkResponse.success(successData, successHeaders, 200)
        
        assertTrue(successResponse.isSuccess)
        assertFalse(successResponse.isFailure)
        assertTrue(successResponse is NetworkResponse.Success)
        
        val success = successResponse as NetworkResponse.Success
        assertArrayEquals(successData, success.data)
        assertEquals(successHeaders, success.headers)
        assertEquals(200, success.statusCode)
        assertEquals("test response data", success.asString())
        assertTrue(success.isJson())
        assertFalse(success.isHtml())
        
        // 测试失败响应
        val failureResponse = NetworkResponse.failure("Network error", 500)
        
        assertFalse(failureResponse.isSuccess)
        assertTrue(failureResponse.isFailure)
        assertTrue(failureResponse is NetworkResponse.Failure)
        
        val failure = failureResponse as NetworkResponse.Failure
        assertEquals("Network error", failure.error)
        assertEquals(500, failure.statusCode)
        assertEquals(emptyMap<String, String>(), failure.headers)
        assertNull(failure.exception)
        
        // 测试 HTML 响应
        val htmlHeaders = mapOf("Content-Type" to "text/html")
        val htmlResponse = NetworkResponse.success("<!DOCTYPE html>".toByteArray(), htmlHeaders)
        val htmlSuccess = htmlResponse as NetworkResponse.Success
        assertTrue(htmlSuccess.isHtml())
        assertFalse(htmlSuccess.isJson())
    }
    
    @Test
    fun testNetworkStats() {
        val stats = NetworkStats()
        
        // 测试初始状态
        assertEquals(0L, stats.requestCount.get())
        assertEquals(0L, stats.successCount.get())
        assertEquals(0L, stats.errorCount.get())
        assertEquals(0L, stats.totalDuration.get())
        assertEquals(0L, stats.totalResponseSize.get())
        
        // 测试记录请求
        stats.recordRequest("GET", 200, 1000L, 1024L)
        assertEquals(1L, stats.requestCount.get())
        assertEquals(1L, stats.successCount.get())
        assertEquals(0L, stats.errorCount.get())
        assertEquals(1000L, stats.totalDuration.get())
        assertEquals(1024L, stats.totalResponseSize.get())
        
        stats.recordRequest("POST", 500, 2000L, 512L)
        assertEquals(2L, stats.requestCount.get())
        assertEquals(1L, stats.successCount.get())
        assertEquals(1L, stats.errorCount.get())
        assertEquals(3000L, stats.totalDuration.get())
        assertEquals(1536L, stats.totalResponseSize.get())
        
        // 测试记录错误
        stats.recordError("GET", RuntimeException("Test error"))
        assertEquals(2L, stats.errorCount.get())
        
        // 测试获取统计
        val statsMap = stats.getStats()
        assertNotNull(statsMap)
        assertTrue(statsMap.containsKey("total_requests"))
        assertTrue(statsMap.containsKey("success_count"))
        assertTrue(statsMap.containsKey("error_count"))
        assertTrue(statsMap.containsKey("success_rate"))
        assertTrue(statsMap.containsKey("average_duration"))
        
        assertEquals(2L, statsMap["total_requests"])
        assertEquals(1L, statsMap["success_count"])
        assertEquals(2L, statsMap["error_count"])
        assertEquals(0.5, statsMap["success_rate"] as Double, 0.01)
        assertEquals(1500L, statsMap["average_duration"])
        
        // 测试清理统计
        stats.clear()
        assertEquals(0L, stats.requestCount.get())
        assertEquals(0L, stats.successCount.get())
        assertEquals(0L, stats.errorCount.get())
    }
    
    @Test
    fun testMethodStats() {
        val methodStats = MethodStats()
        
        // 测试初始状态
        assertEquals(0L, methodStats.count.get())
        assertEquals(0L, methodStats.totalDuration.get())
        
        // 测试记录数据
        methodStats.count.incrementAndGet()
        methodStats.totalDuration.addAndGet(1000L)
        
        methodStats.count.incrementAndGet()
        methodStats.totalDuration.addAndGet(2000L)
        
        assertEquals(2L, methodStats.count.get())
        assertEquals(3000L, methodStats.totalDuration.get())
        
        // 测试获取统计
        val stats = methodStats.getStats()
        assertEquals(2L, stats["count"])
        assertEquals(1500L, stats["average_duration"])
    }
    
    @Test
    fun testCacheEntry() {
        val headers = mapOf("Content-Type" to "application/json")
        val cacheEntry = CacheEntry(
            key = "test_key",
            fileName = "test.cache",
            url = "https://example.com/api",
            method = "GET",
            statusCode = 200,
            headers = headers,
            contentType = "application/json",
            contentLength = 1024L,
            createTime = System.currentTimeMillis(),
            lastAccessTime = System.currentTimeMillis(),
            expiryTime = System.currentTimeMillis() + 3600000L,
            etag = "\"abc123\"",
            lastModified = "Wed, 21 Oct 2015 07:28:00 GMT"
        )
        
        assertEquals("test_key", cacheEntry.key)
        assertEquals("test.cache", cacheEntry.fileName)
        assertEquals("https://example.com/api", cacheEntry.url)
        assertEquals("GET", cacheEntry.method)
        assertEquals(200, cacheEntry.statusCode)
        assertEquals(headers, cacheEntry.headers)
        assertEquals("application/json", cacheEntry.contentType)
        assertEquals(1024L, cacheEntry.contentLength)
        assertEquals("\"abc123\"", cacheEntry.etag)
        assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", cacheEntry.lastModified)
        
        // 测试过期检查
        assertFalse(cacheEntry.isExpired())
        
        val expiredEntry = cacheEntry.copy(expiryTime = System.currentTimeMillis() - 1000L)
        assertTrue(expiredEntry.isExpired())
        
        // 测试摘要信息
        val summary = cacheEntry.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("key"))
        assertTrue(summary.containsKey("url"))
        assertTrue(summary.containsKey("method"))
        assertTrue(summary.containsKey("status_code"))
        assertTrue(summary.containsKey("content_type"))
        assertTrue(summary.containsKey("is_expired"))
        
        assertEquals("test_key", summary["key"])
        assertEquals("https://example.com/api", summary["url"])
        assertEquals("GET", summary["method"])
        assertEquals(200, summary["status_code"])
        assertEquals("application/json", summary["content_type"])
        assertFalse(summary["is_expired"] as Boolean)
    }
    
    @Test
    fun testNetworkCacheStats() {
        val cacheStats = NetworkCacheStats()
        
        // 测试初始状态
        assertEquals(0L, cacheStats.hitCount.get())
        assertEquals(0L, cacheStats.missCount.get())
        assertEquals(0L, cacheStats.putCount.get())
        assertEquals(0L, cacheStats.removeCount.get())
        assertEquals(0L, cacheStats.totalPutSize.get())
        assertEquals(0L, cacheStats.totalRemoveSize.get())
        assertEquals(0.0, cacheStats.getHitRate(), 0.01)
        
        // 测试记录操作
        cacheStats.recordHit()
        cacheStats.recordHit()
        cacheStats.recordMiss()
        cacheStats.recordPut(1024L)
        cacheStats.recordRemove(512L)
        
        assertEquals(2L, cacheStats.hitCount.get())
        assertEquals(1L, cacheStats.missCount.get())
        assertEquals(1L, cacheStats.putCount.get())
        assertEquals(1L, cacheStats.removeCount.get())
        assertEquals(1024L, cacheStats.totalPutSize.get())
        assertEquals(512L, cacheStats.totalRemoveSize.get())
        
        // 命中率应该是 2/3 ≈ 0.67
        assertEquals(0.67, cacheStats.getHitRate(), 0.01)
        
        // 测试重置
        cacheStats.reset()
        assertEquals(0L, cacheStats.hitCount.get())
        assertEquals(0L, cacheStats.missCount.get())
        assertEquals(0.0, cacheStats.getHitRate(), 0.01)
    }
    
    @Test
    fun testNetworkClientBasicFunctionality() {
        // 测试网络客户端基本功能
        assertNotNull(networkClient)
        
        // 测试获取统计信息
        val stats = networkClient.getNetworkStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("total_requests"))
        assertTrue(stats.containsKey("success_count"))
        assertTrue(stats.containsKey("error_count"))
        
        // 测试清理统计
        networkClient.clearStats()
        val clearedStats = networkClient.getNetworkStats()
        assertEquals(0L, clearedStats["total_requests"])
        assertEquals(0L, clearedStats["success_count"])
        assertEquals(0L, clearedStats["error_count"])
    }
    
    @Test
    fun testNetworkCacheManagerBasicFunctionality() {
        // 测试缓存管理器基本功能
        assertNotNull(networkCacheManager)
        
        // 测试获取统计信息
        val stats = networkCacheManager.getStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("cache_entries"))
        assertTrue(stats.containsKey("cache_size_bytes"))
        assertTrue(stats.containsKey("cache_size_mb"))
        assertTrue(stats.containsKey("hit_count"))
        assertTrue(stats.containsKey("miss_count"))
        assertTrue(stats.containsKey("hit_rate"))
        
        assertEquals(0, stats["cache_entries"])
        assertEquals(0L, stats["cache_size_bytes"])
        assertEquals(0L, stats["hit_count"])
        assertEquals(0L, stats["miss_count"])
        assertEquals(0.0, stats["hit_rate"])
        
        // 测试获取所有缓存条目
        val entries = networkCacheManager.getAllCacheEntries()
        assertNotNull(entries)
        assertTrue(entries.isEmpty())
    }
    
    @Test
    fun testNetworkConfigUpdate() {
        // 测试网络配置更新
        val newConfig = NetworkConfig(
            connectTimeout = 20000L,
            readTimeout = 20000L,
            writeTimeout = 20000L,
            maxRetries = 5,
            enableLogging = true,
            userAgent = "Updated-Agent/1.0"
        )
        
        // 更新配置不应该抛出异常
        assertDoesNotThrow {
            networkClient.updateConfig(newConfig)
        }
    }
    
    @Test
    fun testNetworkClientShutdown() {
        // 测试网络客户端关闭
        assertDoesNotThrow {
            networkClient.shutdown()
        }
    }
    
    @Test
    fun testNetworkCacheManagerShutdown() {
        // 测试缓存管理器关闭
        assertDoesNotThrow {
            networkCacheManager.shutdown()
        }
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}
