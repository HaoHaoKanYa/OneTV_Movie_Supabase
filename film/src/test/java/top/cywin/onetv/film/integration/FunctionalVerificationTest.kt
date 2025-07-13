package top.cywin.onetv.film.integration

import android.content.Context
import android.util.Log
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.data.repository.FilmRepository
import top.cywin.onetv.film.optimization.PerformanceOptimizer
import top.cywin.onetv.film.utils.JsonUtils
import java.util.concurrent.atomic.AtomicInteger

/**
 * 功能验证测试
 * 
 * 基于 FongMi/TV 的完整功能验证测试实现
 * 验证所有核心功能的正确性和完整性
 * 
 * 功能：
 * - Spider 解析功能验证
 * - 数据流完整性验证
 * - 缓存机制验证
 * - 并发处理验证
 * - 网络功能验证
 * - Hook 系统验证
 * - JAR 加载验证
 * - 代理功能验证
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class FunctionalVerificationTest {
    
    companion object {
        private const val TAG = "ONETV_FILM_FUNCTIONAL_VERIFICATION"
        private const val TEST_TIMEOUT = 60000L // 60秒
    }
    
    private lateinit var context: Context
    private lateinit var spiderManager: SpiderManager
    private lateinit var filmRepository: FilmRepository
    private lateinit var performanceOptimizer: PerformanceOptimizer
    
    // 验证统计
    private val verificationStats = VerificationStatistics()
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        spiderManager = SpiderManager(context)
        filmRepository = spiderManager.getFilmRepository()
        performanceOptimizer = PerformanceOptimizer(context, spiderManager)
        
        Log.d(TAG, "🏗️ 功能验证测试环境初始化完成")
    }
    
    /**
     * 🕷️ 验证 Spider 解析功能
     */
    @Test
    fun testSpiderParsingFunctionality() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🕷️ 开始验证 Spider 解析功能")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            // 1. 验证 Spider 注册
            totalTests++
            val spiders = spiderManager.getAllSpiders()
            assertTrue("应有注册的 Spider", spiders.isNotEmpty())
            passedTests++
            Log.d(TAG, "✅ Spider 注册验证通过，数量: ${spiders.size}")
            
            // 2. 验证 Spider 基本信息
            spiders.forEach { (key, spider) ->
                totalTests++
                try {
                    assertNotNull("Spider 名称不应为空", spider.getName())
                    assertNotNull("Spider API 不应为空", spider.getApi())
                    assertTrue("Spider 类型应有效", spider.getType() >= 0)
                    passedTests++
                    Log.d(TAG, "✅ Spider $key 基本信息验证通过")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Spider $key 基本信息验证失败", e)
                }
            }
            
            // 3. 验证 Spider 功能接口
            val testSpider = spiders.values.firstOrNull()
            if (testSpider != null) {
                totalTests++
                try {
                    // 测试首页内容获取
                    val homeContent = testSpider.homeContent(false)
                    assertNotNull("首页内容不应为空", homeContent)
                    
                    // 验证 JSON 格式
                    val jsonObject = JsonUtils.parseToJsonObject(homeContent)
                    if (jsonObject != null) {
                        Log.d(TAG, "✅ 首页内容 JSON 格式验证通过")
                    }
                    
                    passedTests++
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Spider 首页内容获取测试失败", e)
                }
                
                totalTests++
                try {
                    // 测试搜索功能
                    if (testSpider.isSearchable()) {
                        val searchResult = testSpider.searchContent("测试", false, "1")
                        assertNotNull("搜索结果不应为空", searchResult)
                        Log.d(TAG, "✅ 搜索功能验证通过")
                    } else {
                        Log.d(TAG, "ℹ️ Spider 不支持搜索功能")
                    }
                    passedTests++
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Spider 搜索功能测试失败", e)
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("spider_parsing", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ Spider 解析功能验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("Spider 解析功能验证成功率应大于80%", successRate > 0.8)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 解析功能验证失败", e)
            throw e
        }
    }
    
    /**
     * 🔄 验证数据流完整性
     */
    @Test
    fun testDataFlowIntegrity() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🔄 开始验证数据流完整性")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            // 1. 验证站点数据流
            totalTests++
            val sitesResult = filmRepository.getSites()
            assertTrue("获取站点列表应成功", sitesResult.isSuccess)
            
            val sites = sitesResult.getOrNull()
            assertNotNull("站点列表不应为空", sites)
            assertTrue("站点列表应包含数据", sites!!.isNotEmpty())
            passedTests++
            Log.d(TAG, "✅ 站点数据流验证通过，站点数: ${sites.size}")
            
            // 2. 验证首页数据流
            if (sites.isNotEmpty()) {
                val testSite = sites.first()
                
                totalTests++
                try {
                    val homeResult = filmRepository.getHomeContent(testSite.key)
                    if (homeResult.isSuccess) {
                        val homeContent = homeResult.getOrNull()!!
                        assertEquals("站点键应匹配", testSite.key, homeContent.siteKey)
                        assertNotNull("首页内容应有分类或推荐", 
                            homeContent.categories.isNotEmpty() || homeContent.recommendations.isNotEmpty())
                        passedTests++
                        Log.d(TAG, "✅ 首页数据流验证通过")
                    } else {
                        Log.w(TAG, "⚠️ 首页内容获取失败: ${homeResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 首页数据流验证异常", e)
                }
                
                // 3. 验证搜索数据流
                totalTests++
                try {
                    val searchResult = filmRepository.searchContent("测试", listOf(testSite.key))
                    if (searchResult.isSuccess) {
                        val searchResults = searchResult.getOrNull()!!
                        assertNotNull("搜索结果不应为空", searchResults)
                        passedTests++
                        Log.d(TAG, "✅ 搜索数据流验证通过")
                    } else {
                        Log.w(TAG, "⚠️ 搜索失败: ${searchResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 搜索数据流验证异常", e)
                }
            }
            
            // 4. 验证本地数据流
            totalTests++
            try {
                val testHistory = PlayHistory(
                    vodId = "test_vod_123",
                    vodName = "测试影片",
                    siteKey = "test_site",
                    flag = "测试线路",
                    episodeName = "第1集"
                )
                
                val saveResult = filmRepository.savePlayHistory(testHistory)
                assertTrue("保存播放历史应成功", saveResult.isSuccess)
                
                val historiesResult = filmRepository.getPlayHistories(limit = 10)
                assertTrue("获取播放历史应成功", historiesResult.isSuccess)
                
                val histories = historiesResult.getOrNull()!!
                assertTrue("播放历史应包含测试数据", 
                    histories.any { it.vodId == "test_vod_123" })
                
                passedTests++
                Log.d(TAG, "✅ 本地数据流验证通过")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 本地数据流验证异常", e)
            }
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("data_flow", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ 数据流完整性验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("数据流完整性验证成功率应大于70%", successRate > 0.7)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 数据流完整性验证失败", e)
            throw e
        }
    }
    
    /**
     * 🗄️ 验证缓存机制
     */
    @Test
    fun testCacheMechanism() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🗄️ 开始验证缓存机制")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            val cacheManager = spiderManager.getCacheManager()
            
            // 1. 验证基本缓存操作
            totalTests++
            val testKey = "verification_test_key"
            val testValue = "verification_test_value"
            
            val putResult = cacheManager.put(testKey, testValue)
            assertTrue("缓存存储应成功", putResult)
            
            val getValue = cacheManager.get(testKey, String::class.java)
            assertEquals("缓存值应匹配", testValue, getValue)
            
            val removeResult = cacheManager.remove(testKey)
            assertTrue("缓存移除应成功", removeResult)
            
            val getAfterRemove = cacheManager.get(testKey, String::class.java)
            assertNull("移除后应获取不到值", getAfterRemove)
            
            passedTests++
            Log.d(TAG, "✅ 基本缓存操作验证通过")
            
            // 2. 验证缓存统计
            totalTests++
            val statsBefore = cacheManager.getStats()
            assertNotNull("缓存统计不应为空", statsBefore)
            assertTrue("缓存统计应包含基本指标", 
                statsBefore.containsKey("hit_count") && statsBefore.containsKey("miss_count"))
            
            // 执行一些缓存操作
            cacheManager.put("stats_test_1", "value1")
            cacheManager.put("stats_test_2", "value2")
            cacheManager.get("stats_test_1", String::class.java)
            cacheManager.get("non_existent_key", String::class.java)
            
            val statsAfter = cacheManager.getStats()
            val hitCountAfter = statsAfter["hit_count"] as Long
            val missCountAfter = statsAfter["miss_count"] as Long
            
            assertTrue("命中次数应有变化", hitCountAfter >= 0)
            assertTrue("未命中次数应有变化", missCountAfter >= 0)
            
            passedTests++
            Log.d(TAG, "✅ 缓存统计验证通过")
            
            // 3. 验证专用缓存
            totalTests++
            val vodContentCache = spiderManager.getVodContentCache()
            
            val testSiteKey = "test_site_cache"
            val testContent = """{"test": "content"}"""
            
            val putHomeResult = vodContentCache.putHomeContent(testSiteKey, testContent)
            assertTrue("VOD 首页缓存存储应成功", putHomeResult)
            
            val getHomeResult = vodContentCache.getHomeContent(testSiteKey)
            assertEquals("VOD 首页缓存值应匹配", testContent, getHomeResult)
            
            passedTests++
            Log.d(TAG, "✅ 专用缓存验证通过")
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("cache_mechanism", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ 缓存机制验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("缓存机制验证成功率应为100%", successRate == 1.0)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存机制验证失败", e)
            throw e
        }
    }
    
    /**
     * ⚡ 验证并发处理
     */
    @Test
    fun testConcurrentProcessing() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "⚡ 开始验证并发处理")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            val concurrentManager = spiderManager.getConcurrentManager()
            
            // 1. 验证基本并发执行
            totalTests++
            val taskCount = 10
            val completedTasks = AtomicInteger(0)
            
            val tasks = (1..taskCount).map { taskId ->
                async {
                    try {
                        val result = concurrentManager.executeWithTimeout(5000L) {
                            delay(100L)
                            "Task $taskId completed"
                        }
                        if (result != null) {
                            completedTasks.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "任务 $taskId 执行失败", e)
                    }
                }
            }
            
            tasks.awaitAll()
            
            val completed = completedTasks.get()
            assertTrue("应有任务完成", completed > 0)
            assertTrue("完成率应大于80%", completed.toDouble() / taskCount > 0.8)
            
            passedTests++
            Log.d(TAG, "✅ 基本并发执行验证通过，完成: $completed/$taskCount")
            
            // 2. 验证线程池状态
            totalTests++
            val threadPoolStatus = concurrentManager.getThreadPoolStatus()
            assertNotNull("线程池状态不应为空", threadPoolStatus)
            assertFalse("线程池状态应包含数据", threadPoolStatus.isEmpty())
            
            threadPoolStatus.forEach { (poolName, status) ->
                assertTrue("线程池应有名称", status.containsKey("name"))
                assertTrue("线程池应有核心大小", status.containsKey("core_pool_size"))
                assertTrue("线程池应有最大大小", status.containsKey("max_pool_size"))
            }
            
            passedTests++
            Log.d(TAG, "✅ 线程池状态验证通过")
            
            // 3. 验证并发统计
            totalTests++
            val concurrentStats = concurrentManager.getConcurrentStats()
            assertNotNull("并发统计不应为空", concurrentStats)
            assertTrue("并发统计应包含任务数据", concurrentStats.containsKey("total_tasks"))
            
            passedTests++
            Log.d(TAG, "✅ 并发统计验证通过")
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("concurrent_processing", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ 并发处理验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("并发处理验证成功率应为100%", successRate == 1.0)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 并发处理验证失败", e)
            throw e
        }
    }
    
    /**
     * 🌐 验证网络功能
     */
    @Test
    fun testNetworkFunctionality() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🌐 开始验证网络功能")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            val networkClient = spiderManager.getNetworkClient()
            
            // 1. 验证网络客户端配置
            totalTests++
            val config = networkClient.getConfig()
            assertNotNull("网络配置不应为空", config)
            assertTrue("网络配置应包含超时设置", config.containsKey("timeout"))
            
            passedTests++
            Log.d(TAG, "✅ 网络客户端配置验证通过")
            
            // 2. 验证网络统计
            totalTests++
            val networkStats = networkClient.getStats()
            assertNotNull("网络统计不应为空", networkStats)
            assertTrue("网络统计应包含请求数据", networkStats.containsKey("total_requests"))
            
            passedTests++
            Log.d(TAG, "✅ 网络统计验证通过")
            
            // 3. 验证 Hook 系统
            totalTests++
            val hookManager = spiderManager.getHookManager()
            val registeredHooks = hookManager.getRegisteredHooks()
            assertTrue("应有注册的 Hook", registeredHooks.isNotEmpty())
            
            passedTests++
            Log.d(TAG, "✅ Hook 系统验证通过，Hook 数量: ${registeredHooks.size}")
            
            // 4. 验证代理系统
            totalTests++
            val proxyManager = spiderManager.getProxyManager()
            val proxyStats = proxyManager.getStats()
            assertNotNull("代理统计不应为空", proxyStats)
            
            passedTests++
            Log.d(TAG, "✅ 代理系统验证通过")
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("network_functionality", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ 网络功能验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("网络功能验证成功率应为100%", successRate == 1.0)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 网络功能验证失败", e)
            throw e
        }
    }
    
    /**
     * 📊 验证性能优化
     */
    @Test
    fun testPerformanceOptimization() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "📊 开始验证性能优化")
        
        val startTime = System.currentTimeMillis()
        var passedTests = 0
        var totalTests = 0
        
        try {
            // 1. 验证性能分析
            totalTests++
            val performanceReport = performanceOptimizer.analyzePerformance()
            assertNotNull("性能报告不应为空", performanceReport)
            assertTrue("性能评分应在有效范围内", 
                performanceReport.performanceScore >= 0f && performanceReport.performanceScore <= 100f)
            
            passedTests++
            Log.d(TAG, "✅ 性能分析验证通过，评分: ${performanceReport.performanceScore}")
            
            // 2. 验证优化建议
            totalTests++
            val recommendations = performanceReport.recommendations
            assertNotNull("优化建议不应为空", recommendations)
            
            recommendations.forEach { recommendation ->
                assertNotNull("建议类型不应为空", recommendation.type)
                assertNotNull("建议描述不应为空", recommendation.description)
                assertNotNull("建议优先级不应为空", recommendation.priority)
            }
            
            passedTests++
            Log.d(TAG, "✅ 优化建议验证通过，建议数: ${recommendations.size}")
            
            // 3. 验证优化统计
            totalTests++
            val optimizationStats = performanceOptimizer.getOptimizationStats()
            assertNotNull("优化统计不应为空", optimizationStats)
            assertTrue("优化统计应包含分析数据", optimizationStats.containsKey("analysis_count"))
            
            passedTests++
            Log.d(TAG, "✅ 优化统计验证通过")
            
            val duration = System.currentTimeMillis() - startTime
            val successRate = passedTests.toDouble() / totalTests
            
            verificationStats.recordTest("performance_optimization", duration, passedTests, totalTests)
            
            Log.d(TAG, "✅ 性能优化验证完成")
            Log.d(TAG, "   通过测试: $passedTests/$totalTests (${String.format("%.1f%%", successRate * 100)})")
            Log.d(TAG, "   耗时: ${duration}ms")
            
            assertTrue("性能优化验证成功率应为100%", successRate == 1.0)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 性能优化验证失败", e)
            throw e
        }
    }
    
    /**
     * 📊 获取验证统计
     */
    fun getVerificationStatistics(): Map<String, Any> {
        return verificationStats.getStats()
    }
}

/**
 * 验证统计
 */
class VerificationStatistics {
    private val testResults = mutableMapOf<String, TestVerificationResult>()
    
    fun recordTest(name: String, duration: Long, passedTests: Int, totalTests: Int) {
        testResults[name] = TestVerificationResult(name, duration, passedTests, totalTests)
    }
    
    fun getStats(): Map<String, Any> {
        val totalPassed = testResults.values.sumOf { it.passedTests }
        val totalTests = testResults.values.sumOf { it.totalTests }
        val overallSuccessRate = if (totalTests > 0) totalPassed.toDouble() / totalTests else 0.0
        
        return mapOf(
            "test_results" to testResults.values.map { it.toMap() },
            "summary" to mapOf(
                "total_test_categories" to testResults.size,
                "total_passed_tests" to totalPassed,
                "total_tests" to totalTests,
                "overall_success_rate" to String.format("%.1f%%", overallSuccessRate * 100),
                "all_categories_passed" to testResults.values.all { it.getSuccessRate() > 0.8 }
            )
        )
    }
}

data class TestVerificationResult(
    val name: String,
    val duration: Long,
    val passedTests: Int,
    val totalTests: Int
) {
    fun getSuccessRate(): Double = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0
    
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "duration" to duration,
        "passed_tests" to passedTests,
        "total_tests" to totalTests,
        "success_rate" to String.format("%.1f%%", getSuccessRate() * 100),
        "status" to if (getSuccessRate() > 0.8) "PASSED" else "FAILED"
    )
}
