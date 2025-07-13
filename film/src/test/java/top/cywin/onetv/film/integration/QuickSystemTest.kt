package top.cywin.onetv.film.integration

import android.util.Log
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.engine.EngineManager
import top.cywin.onetv.film.jar.JarManager
import top.cywin.onetv.film.network.NetworkClient
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.data.models.VodSite

/**
 * 快速系统测试
 * 
 * 验证 OneTV Film 系统的核心功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class QuickSystemTest {
    
    companion object {
        private const val TAG = "ONETV_FILM_QUICK_TEST"
    }
    
    @Test
    fun testSystemInitialization() {
        Log.d(TAG, "🧪 开始系统初始化测试")
        
        try {
            // 测试 SpiderManager 初始化
            val spiderManager = SpiderManager.getInstance()
            assertNotNull("SpiderManager 应该不为空", spiderManager)
            
            // 测试 EngineManager 初始化
            val engineManager = EngineManager.getInstance()
            assertNotNull("EngineManager 应该不为空", engineManager)
            
            // 测试 JarManager 初始化
            val jarManager = JarManager.getInstance()
            assertNotNull("JarManager 应该不为空", jarManager)
            
            Log.d(TAG, "✅ 系统初始化测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 系统初始化测试失败", e)
            fail("系统初始化失败: ${e.message}")
        }
    }
    
    @Test
    fun testNetworkClient() {
        Log.d(TAG, "🧪 开始网络客户端测试")
        
        try {
            val networkClient = NetworkClient.Builder()
                .userAgent("OneTV-Test/1.0.0")
                .timeout(10000L)
                .build()
            
            assertNotNull("NetworkClient 应该不为空", networkClient)
            
            Log.d(TAG, "✅ 网络客户端测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 网络客户端测试失败", e)
            fail("网络客户端测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testJsonUtils() {
        Log.d(TAG, "🧪 开始 JSON 工具测试")
        
        try {
            val testData = mapOf(
                "name" to "测试站点",
                "type" to 1,
                "api" to "https://test.com/api",
                "enabled" to true
            )
            
            val jsonString = JsonUtils.createJsonObject(testData)
            assertNotNull("JSON 字符串应该不为空", jsonString)
            assertTrue("JSON 字符串应该包含测试数据", jsonString.contains("测试站点"))
            
            val parsedObject = JsonUtils.parseToJsonObject(jsonString)
            assertNotNull("解析的 JSON 对象应该不为空", parsedObject)
            
            val name = JsonUtils.getString(parsedObject, "name")
            assertEquals("解析的名称应该匹配", "测试站点", name)
            
            Log.d(TAG, "✅ JSON 工具测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JSON 工具测试失败", e)
            fail("JSON 工具测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testVodSiteModel() {
        Log.d(TAG, "🧪 开始 VodSite 模型测试")
        
        try {
            val vodSite = VodSite(
                key = "test_site",
                name = "测试站点",
                type = 1,
                api = "https://test.com/api",
                searchable = 1,
                enabled = true
            )
            
            assertNotNull("VodSite 应该不为空", vodSite)
            assertEquals("站点 key 应该匹配", "test_site", vodSite.key)
            assertEquals("站点名称应该匹配", "测试站点", vodSite.name)
            assertEquals("站点类型应该匹配", 1, vodSite.type)
            assertTrue("站点应该启用", vodSite.enabled)
            assertFalse("不应该是 JAR 站点", vodSite.isJarSite())
            
            Log.d(TAG, "✅ VodSite 模型测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ VodSite 模型测试失败", e)
            fail("VodSite 模型测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testSpiderRegistration() {
        Log.d(TAG, "🧪 开始 Spider 注册测试")
        
        try {
            val spiderManager = SpiderManager.getInstance()
            
            // 检查是否注册了预期的 Spider
            val expectedSpiders = listOf(
                "csp_XPath",
                "csp_XPathMac", 
                "csp_AppYs",
                "csp_JavaScript",
                "csp_YydsAli1",
                "csp_Thunder",
                "csp_Drpy",
                "csp_AliDrive",
                "csp_Quark",
                "csp_Baidu"
            )
            
            expectedSpiders.forEach { spiderApi ->
                val hasSpider = spiderManager.hasSpider(spiderApi)
                assertTrue("应该注册了 Spider: $spiderApi", hasSpider)
            }
            
            Log.d(TAG, "✅ Spider 注册测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 注册测试失败", e)
            fail("Spider 注册测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testEngineTypes() {
        Log.d(TAG, "🧪 开始引擎类型测试")
        
        try {
            val engineManager = EngineManager.getInstance()
            
            // 检查是否支持预期的引擎类型
            val supportedEngines = engineManager.getSupportedEngines()
            
            assertTrue("应该支持 XPath 引擎", supportedEngines.contains("XPATH"))
            assertTrue("应该支持 JavaScript 引擎", supportedEngines.contains("JAVASCRIPT"))
            assertTrue("应该支持 QuickJS 引擎", supportedEngines.contains("QUICKJS"))
            assertTrue("应该支持 Python 引擎", supportedEngines.contains("PYTHON"))
            assertTrue("应该支持 Java 引擎", supportedEngines.contains("JAVA"))
            
            Log.d(TAG, "✅ 引擎类型测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 引擎类型测试失败", e)
            fail("引擎类型测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testSystemStats() {
        Log.d(TAG, "🧪 开始系统统计测试")
        
        try {
            val spiderManager = SpiderManager.getInstance()
            val stats = spiderManager.getStats()
            
            assertNotNull("统计信息应该不为空", stats)
            assertTrue("统计信息应该包含注册的 Spider 数量", stats.containsKey("registered_spiders"))
            
            val registeredCount = stats["registered_spiders"] as? Int ?: 0
            assertTrue("应该注册了多个 Spider", registeredCount > 0)
            
            Log.d(TAG, "📊 系统统计: $stats")
            Log.d(TAG, "✅ 系统统计测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 系统统计测试失败", e)
            fail("系统统计测试失败: ${e.message}")
        }
    }
    
    @Test
    fun testSystemHealthCheck() {
        Log.d(TAG, "🧪 开始系统健康检查")
        
        try {
            // 检查关键组件是否正常
            val components = mapOf(
                "SpiderManager" to SpiderManager.getInstance(),
                "EngineManager" to EngineManager.getInstance(),
                "JarManager" to JarManager.getInstance()
            )
            
            components.forEach { (name, component) ->
                assertNotNull("$name 应该正常初始化", component)
                Log.d(TAG, "✅ $name 健康检查通过")
            }
            
            Log.d(TAG, "✅ 系统健康检查全部通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 系统健康检查失败", e)
            fail("系统健康检查失败: ${e.message}")
        }
    }
    
    @Test
    fun testPerformanceBaseline() {
        Log.d(TAG, "🧪 开始性能基准测试")
        
        try {
            val startTime = System.currentTimeMillis()
            
            // 执行一些基本操作
            val spiderManager = SpiderManager.getInstance()
            val stats = spiderManager.getStats()
            
            val networkClient = NetworkClient.Builder().build()
            
            val testJson = JsonUtils.createJsonObject(mapOf("test" to "data"))
            val parsedJson = JsonUtils.parseToJsonObject(testJson)
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            assertTrue("基本操作应该在 1 秒内完成", duration < 1000)
            
            Log.d(TAG, "⏱️ 性能基准: ${duration}ms")
            Log.d(TAG, "✅ 性能基准测试通过")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 性能基准测试失败", e)
            fail("性能基准测试失败: ${e.message}")
        }
    }
}
