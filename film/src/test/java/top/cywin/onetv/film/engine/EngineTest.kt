package top.cywin.onetv.film.engine

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.data.models.VodSite

/**
 * 引擎系统测试
 * 
 * 测试多引擎系统的基础功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EngineTest {
    
    private lateinit var context: Context
    private lateinit var engineManager: EngineManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        engineManager = EngineManager()
    }
    
    @Test
    fun testEngineManagerInitialization() = runBlocking {
        // 测试引擎管理器初始化
        engineManager.initialize(context)
        
        val status = engineManager.getEngineStatus()
        assertNotNull(status)
        assertTrue(status["initialized"] as Boolean)
        assertTrue((status["engine_count"] as Int) > 0)
        
        // 测试引擎可用性
        assertTrue(engineManager.isEngineAvailable(EngineType.JAVASCRIPT))
        assertTrue(engineManager.isEngineAvailable(EngineType.XPATH))
        assertTrue(engineManager.isEngineAvailable(EngineType.JAVA))
        assertTrue(engineManager.isEngineAvailable(EngineType.PYTHON))
    }
    
    @Test
    fun testJavaScriptEngine() = runBlocking {
        val jsEngine = JavaScriptEngine()
        jsEngine.initialize(context)
        
        val testSite = VodSite.createJavaScript("test", "测试站点", "https://example.com/spider.js")
        
        // 测试 homeContent
        val homeResult = jsEngine.execute(testSite, "homeContent", mapOf("filter" to false))
        assertTrue(homeResult.isSuccess)
        val homeContent = homeResult.getOrThrow()
        assertNotNull(homeContent)
        assertTrue(homeContent.contains("class"))
        
        // 测试 categoryContent
        val categoryResult = jsEngine.execute(testSite, "categoryContent", mapOf(
            "tid" to "1",
            "pg" to "1",
            "filter" to false,
            "extend" to emptyMap<String, String>()
        ))
        assertTrue(categoryResult.isSuccess)
        val categoryContent = categoryResult.getOrThrow()
        assertNotNull(categoryContent)
        assertTrue(categoryContent.contains("list"))
        
        // 测试引擎状态
        val status = jsEngine.getEngineStatus()
        assertNotNull(status)
        assertTrue(status["initialized"] as Boolean)
        
        // 清理
        jsEngine.cleanup()
    }
    
    @Test
    fun testQuickJSEngine() = runBlocking {
        val quickJSEngine = QuickJSEngine()
        quickJSEngine.initialize(context)
        
        // 测试脚本执行
        val script = """
            function test() {
                return "Hello QuickJS";
            }
        """.trimIndent()
        
        val result = quickJSEngine.evaluateScript(script)
        assertNotNull(result)
        assertFalse(result.startsWith("ERROR:"))
        
        // 测试函数检查
        val hasFunction = quickJSEngine.hasFunction("test")
        assertTrue(hasFunction)
        
        // 测试函数调用
        val callResult = quickJSEngine.callFunction("test", arrayOf())
        assertNotNull(callResult)
        assertFalse(callResult.startsWith("ERROR:"))
        
        // 测试引擎状态
        val status = quickJSEngine.getEngineStatus()
        assertNotNull(status)
        assertTrue(status["initialized"] as Boolean)
        assertTrue(status["context"] as Boolean)
        
        // 清理
        quickJSEngine.cleanup()
    }
    
    @Test
    fun testEngineManagerFallback() = runBlocking {
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
        val customSite = VodSite.createDefault("custom_test", "自定义测试站点", "csp_Test")
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
    fun testEngineStats() = runBlocking {
        engineManager.initialize(context)
        
        val testSite = VodSite.createJavaScript("test", "测试站点", "https://example.com/spider.js")
        
        // 执行多次操作以生成统计数据
        repeat(3) {
            engineManager.executeWithFallback(testSite, "homeContent", mapOf("filter" to false))
        }
        
        val stats = engineManager.getEngineStats()
        assertNotNull(stats)
        
        // 检查 JavaScript 引擎统计
        val jsStats = stats[EngineType.JAVASCRIPT]
        assertNotNull(jsStats)
        assertTrue(jsStats!!.successCount > 0)
        assertTrue(jsStats.lastUsed > 0)
        
        engineManager.shutdown()
    }
    
    @Test
    fun testEngineTypes() {
        // 测试引擎类型枚举
        val engineTypes = EngineType.values()
        assertEquals(4, engineTypes.size)
        assertTrue(engineTypes.contains(EngineType.JAVASCRIPT))
        assertTrue(engineTypes.contains(EngineType.XPATH))
        assertTrue(engineTypes.contains(EngineType.PYTHON))
        assertTrue(engineTypes.contains(EngineType.JAVA))
    }
    
    @Test
    fun testEngineStatsCalculation() {
        val stats = EngineStats()
        
        // 初始状态
        assertEquals(0, stats.successCount)
        assertEquals(0, stats.failureCount)
        assertEquals(0, stats.averageDuration)
        assertEquals(0.0, stats.successRate, 0.01)
        
        // 添加成功记录
        stats.successCount = 3
        stats.totalDuration = 1500
        assertEquals(500, stats.averageDuration)
        assertEquals(1.0, stats.successRate, 0.01)
        
        // 添加失败记录
        stats.failureCount = 1
        assertEquals(0.75, stats.successRate, 0.01)
    }
    
    @Test
    fun testEngineCleanup() = runBlocking {
        val jsEngine = JavaScriptEngine()
        jsEngine.initialize(context)
        
        // 验证初始化状态
        val statusBefore = jsEngine.getEngineStatus()
        assertTrue(statusBefore["initialized"] as Boolean)
        
        // 清理引擎
        jsEngine.cleanup()
        
        // 验证清理后状态
        val statusAfter = jsEngine.getEngineStatus()
        assertFalse(statusAfter["initialized"] as Boolean)
    }
    
    @Test
    fun testEngineManagerShutdown() = runBlocking {
        engineManager.initialize(context)
        
        // 验证初始化状态
        val statusBefore = engineManager.getEngineStatus()
        assertTrue(statusBefore["initialized"] as Boolean)
        
        // 关闭引擎管理器
        engineManager.shutdown()
        
        // 验证关闭后状态
        val statusAfter = engineManager.getEngineStatus()
        assertFalse(statusAfter["initialized"] as Boolean)
        assertEquals(0, statusAfter["engine_count"])
    }
}
