package top.cywin.onetv.film.jar

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * JAR 系统完整测试
 * 
 * 测试第七阶段实现的所有 JAR 功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarSystemTest {
    
    private lateinit var context: Context
    private lateinit var jarLoader: JarLoader
    private lateinit var jarManager: JarManager
    private lateinit var jarCacheManager: JarCacheManager
    private lateinit var jarSecurityManager: JarSecurityManager
    private lateinit var jarUpdateManager: JarUpdateManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        jarLoader = JarLoader(context)
        jarManager = JarManager(context)
        jarCacheManager = JarCacheManager(context)
        jarSecurityManager = JarSecurityManager()
        jarUpdateManager = JarUpdateManager(context, jarManager)
    }
    
    @Test
    fun testJarModels() {
        // 测试 JarInfo
        val jarInfo = JarInfo(
            key = "test_jar",
            name = "测试 JAR",
            version = "1.0.0",
            vendor = "OneTV",
            description = "测试用 JAR 包",
            url = "https://example.com/test.jar",
            filePath = "/path/to/test.jar",
            fileSize = 1024L,
            spiderClasses = listOf("com.example.TestSpider"),
            loadTime = System.currentTimeMillis(),
            checksum = "abc123"
        )
        
        assertTrue(jarInfo.isValid())
        assertEquals("test_jar", jarInfo.key)
        assertEquals("测试 JAR", jarInfo.name)
        assertEquals("1.0.0", jarInfo.version)
        assertEquals(1, jarInfo.spiderClasses.size)
        
        val summary = jarInfo.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("key"))
        assertTrue(summary.containsKey("name"))
        assertTrue(summary.containsKey("spider_count"))
        
        // 测试 JarConfig
        val jarConfig = JarConfig(
            url = "https://example.com/test.jar",
            name = "测试配置",
            enabled = true,
            autoUpdate = false,
            priority = 100
        )
        
        assertTrue(jarConfig.isValid())
        assertEquals("https://example.com/test.jar", jarConfig.url)
        assertTrue(jarConfig.enabled)
        assertFalse(jarConfig.autoUpdate)
        
        val configKey = jarConfig.generateKey()
        assertNotNull(configKey)
        assertTrue(configKey.isNotEmpty())
        
        // 测试 JarManagerConfig
        val managerConfig = JarManagerConfig(
            maxCacheSize = 100 * 1024 * 1024L,
            maxJarSize = 10 * 1024 * 1024L,
            downloadTimeout = 30000L,
            enableAutoUpdate = true,
            enableSecurityCheck = true
        )
        
        assertTrue(managerConfig.isValid())
        assertEquals(100 * 1024 * 1024L, managerConfig.maxCacheSize)
        assertTrue(managerConfig.enableAutoUpdate)
        assertTrue(managerConfig.enableSecurityCheck)
    }
    
    @Test
    fun testJarLoadResult() {
        val jarInfo = JarInfo(
            key = "test",
            name = "Test",
            version = "1.0.0",
            vendor = "Test",
            description = "Test",
            url = "https://example.com/test.jar",
            filePath = "/test.jar",
            fileSize = 1024L,
            spiderClasses = emptyList(),
            loadTime = System.currentTimeMillis(),
            checksum = "test"
        )
        
        // 测试成功结果
        val successResult = JarLoadResult.success(jarInfo)
        assertTrue(successResult.isSuccess)
        assertFalse(successResult.isFailure)
        assertFalse(successResult.isLoading)
        assertTrue(successResult is JarLoadResult.Success)
        assertEquals(jarInfo, (successResult as JarLoadResult.Success).jarInfo)
        
        // 测试失败结果
        val failureResult = JarLoadResult.failure("测试错误")
        assertFalse(failureResult.isSuccess)
        assertTrue(failureResult.isFailure)
        assertFalse(failureResult.isLoading)
        assertTrue(failureResult is JarLoadResult.Failure)
        assertEquals("测试错误", (failureResult as JarLoadResult.Failure).error)
        
        // 测试加载中结果
        val loadingResult = JarLoadResult.loading(50, "加载中...")
        assertFalse(loadingResult.isSuccess)
        assertFalse(loadingResult.isFailure)
        assertTrue(loadingResult.isLoading)
        assertTrue(loadingResult is JarLoadResult.Loading)
        assertEquals(50, (loadingResult as JarLoadResult.Loading).progress)
        assertEquals("加载中...", loadingResult.message)
    }
    
    @Test
    fun testJarUpdateInfo() {
        val updateInfo = JarUpdateInfo(
            jarKey = "test_jar",
            currentVersion = "1.0.0",
            latestVersion = "1.1.0",
            updateAvailable = true,
            updateUrl = "https://example.com/test-1.1.0.jar",
            updateSize = 2048L,
            releaseNotes = "修复了一些问题",
            updateTime = System.currentTimeMillis()
        )
        
        assertTrue(updateInfo.needsUpdate())
        assertEquals("test_jar", updateInfo.jarKey)
        assertEquals("1.0.0", updateInfo.currentVersion)
        assertEquals("1.1.0", updateInfo.latestVersion)
        assertTrue(updateInfo.updateAvailable)
        
        // 测试无更新情况
        val noUpdateInfo = updateInfo.copy(
            latestVersion = "1.0.0",
            updateAvailable = false
        )
        
        assertFalse(noUpdateInfo.needsUpdate())
        assertFalse(noUpdateInfo.updateAvailable)
    }
    
    @Test
    fun testJarSecurityInfo() {
        val securityInfo = JarSecurityInfo(
            checksum = "abc123",
            signature = "signature",
            trusted = true,
            permissions = listOf("网络访问", "文件系统访问"),
            scanResult = SecurityScanResult.SAFE
        )
        
        assertTrue(securityInfo.isSafe())
        assertEquals("abc123", securityInfo.checksum)
        assertTrue(securityInfo.trusted)
        assertEquals(SecurityScanResult.SAFE, securityInfo.scanResult)
        assertEquals(2, securityInfo.permissions.size)
        
        // 测试不安全情况
        val unsafeInfo = securityInfo.copy(
            trusted = false,
            scanResult = SecurityScanResult.DANGEROUS
        )
        
        assertFalse(unsafeInfo.isSafe())
        assertFalse(unsafeInfo.trusted)
        assertEquals(SecurityScanResult.DANGEROUS, unsafeInfo.scanResult)
    }
    
    @Test
    fun testJarPerformanceMetrics() {
        val metrics = JarPerformanceMetrics(
            jarKey = "test_jar",
            loadTime = 1000L,
            classLoadTime = 500L,
            memoryUsage = 1024 * 1024L, // 1MB
            spiderCreateTime = 100L,
            executionCount = 10L,
            averageExecutionTime = 200L,
            errorCount = 1L,
            lastExecutionTime = System.currentTimeMillis()
        )
        
        assertEquals("test_jar", metrics.jarKey)
        assertEquals(1000L, metrics.loadTime)
        assertEquals(10L, metrics.executionCount)
        assertEquals(1L, metrics.errorCount)
        
        val summary = metrics.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("jar_key"))
        assertTrue(summary.containsKey("load_time_ms"))
        assertTrue(summary.containsKey("execution_count"))
        assertTrue(summary.containsKey("error_rate"))
        
        val errorRate = summary["error_rate"] as Double
        assertEquals(0.1, errorRate, 0.01) // 1/10 = 0.1
    }
    
    @Test
    fun testJarLoaderStats() {
        val jarInfo1 = JarInfo(
            key = "jar1", name = "JAR1", version = "1.0.0", vendor = "Test",
            description = "", url = "", filePath = "", fileSize = 1024L,
            spiderClasses = listOf("Spider1", "Spider2"), loadTime = 0L, checksum = ""
        )
        
        val jarInfo2 = JarInfo(
            key = "jar2", name = "JAR2", version = "1.0.0", vendor = "Test",
            description = "", url = "", filePath = "", fileSize = 2048L,
            spiderClasses = listOf("Spider3"), loadTime = 0L, checksum = ""
        )
        
        val stats = JarLoaderStats(
            loadedJars = 2,
            cachedClasses = 5,
            cacheSize = 3072L,
            jarInfos = listOf(jarInfo1, jarInfo2)
        )
        
        assertEquals(2, stats.loadedJars)
        assertEquals(5, stats.cachedClasses)
        assertEquals(3072L, stats.cacheSize)
        assertEquals(2, stats.jarInfos.size)
        
        val summary = stats.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("loaded_jars"))
        assertTrue(summary.containsKey("cached_classes"))
        assertTrue(summary.containsKey("cache_size_mb"))
        assertTrue(summary.containsKey("total_spiders"))
        
        val totalSpiders = summary["total_spiders"] as Int
        assertEquals(3, totalSpiders) // 2 + 1
    }
    
    @Test
    fun testSecurityScanResult() {
        // 测试枚举值
        assertEquals(4, SecurityScanResult.values().size)
        assertTrue(SecurityScanResult.values().contains(SecurityScanResult.SAFE))
        assertTrue(SecurityScanResult.values().contains(SecurityScanResult.WARNING))
        assertTrue(SecurityScanResult.values().contains(SecurityScanResult.DANGEROUS))
        assertTrue(SecurityScanResult.values().contains(SecurityScanResult.UNKNOWN))
    }
    
    @Test
    fun testJarStatus() {
        // 测试枚举值
        assertEquals(7, JarStatus.values().size)
        assertTrue(JarStatus.values().contains(JarStatus.UNKNOWN))
        assertTrue(JarStatus.values().contains(JarStatus.DOWNLOADING))
        assertTrue(JarStatus.values().contains(JarStatus.VALIDATING))
        assertTrue(JarStatus.values().contains(JarStatus.LOADING))
        assertTrue(JarStatus.values().contains(JarStatus.LOADED))
        assertTrue(JarStatus.values().contains(JarStatus.ERROR))
        assertTrue(JarStatus.values().contains(JarStatus.UNLOADED))
    }
    
    @Test
    fun testJarEvents() {
        val jarInfo = JarInfo(
            key = "test", name = "Test", version = "1.0.0", vendor = "Test",
            description = "", url = "", filePath = "", fileSize = 1024L,
            spiderClasses = emptyList(), loadTime = 0L, checksum = ""
        )
        
        // 测试加载开始事件
        val loadStarted = JarEvent.LoadStarted("test_jar", "https://example.com/test.jar")
        assertEquals("test_jar", loadStarted.jarKey)
        assertEquals("https://example.com/test.jar", loadStarted.url)
        
        // 测试加载成功事件
        val loadSuccess = JarEvent.LoadSuccess(jarInfo)
        assertEquals(jarInfo, loadSuccess.jarInfo)
        
        // 测试加载失败事件
        val loadFailure = JarEvent.LoadFailure("test_jar", "加载失败", null)
        assertEquals("test_jar", loadFailure.jarKey)
        assertEquals("加载失败", loadFailure.error)
        
        // 测试卸载事件
        val unloaded = JarEvent.Unloaded("test_jar")
        assertEquals("test_jar", unloaded.jarKey)
        
        // 测试 Spider 创建事件
        val spiderCreated = JarEvent.SpiderCreated("test_jar", "TestSpider", true)
        assertEquals("test_jar", spiderCreated.jarKey)
        assertEquals("TestSpider", spiderCreated.className)
        assertTrue(spiderCreated.success)
        
        // 测试安全警告事件
        val securityWarning = JarEvent.SecurityWarning("test_jar", "检测到可疑代码")
        assertEquals("test_jar", securityWarning.jarKey)
        assertEquals("检测到可疑代码", securityWarning.warning)
    }
    
    @Test
    fun testJarCacheEntry() {
        val jarInfo = JarInfo(
            key = "test", name = "Test JAR", version = "1.0.0", vendor = "Test",
            description = "", url = "", filePath = "", fileSize = 1024L,
            spiderClasses = emptyList(), loadTime = 0L, checksum = ""
        )
        
        val cacheEntry = CacheEntry(
            key = "test_key",
            filePath = "/cache/test.jar",
            fileSize = 2048L,
            jarInfo = jarInfo,
            createTime = System.currentTimeMillis(),
            lastAccessTime = System.currentTimeMillis(),
            accessCount = 5L
        )
        
        assertEquals("test_key", cacheEntry.key)
        assertEquals("/cache/test.jar", cacheEntry.filePath)
        assertEquals(2048L, cacheEntry.fileSize)
        assertEquals(jarInfo, cacheEntry.jarInfo)
        assertEquals(5L, cacheEntry.accessCount)
        
        val summary = cacheEntry.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("key"))
        assertTrue(summary.containsKey("jar_name"))
        assertTrue(summary.containsKey("jar_version"))
        assertTrue(summary.containsKey("file_size_mb"))
        assertTrue(summary.containsKey("access_count"))
    }
    
    @Test
    fun testCacheStats() {
        val stats = CacheStats()
        
        // 测试初始状态
        assertEquals(0L, stats.hitCount.get())
        assertEquals(0L, stats.missCount.get())
        assertEquals(0L, stats.putCount.get())
        assertEquals(0L, stats.removeCount.get())
        assertEquals(0.0, stats.getHitRate(), 0.01)
        
        // 测试记录操作
        stats.recordHit()
        stats.recordHit()
        stats.recordMiss()
        stats.recordPut(1024L)
        stats.recordRemove(512L)
        
        assertEquals(2L, stats.hitCount.get())
        assertEquals(1L, stats.missCount.get())
        assertEquals(1L, stats.putCount.get())
        assertEquals(1L, stats.removeCount.get())
        assertEquals(1024L, stats.totalPutSize.get())
        assertEquals(512L, stats.totalRemoveSize.get())
        
        // 命中率应该是 2/3 ≈ 0.67
        assertEquals(0.67, stats.getHitRate(), 0.01)
        
        // 测试重置
        stats.reset()
        assertEquals(0L, stats.hitCount.get())
        assertEquals(0L, stats.missCount.get())
        assertEquals(0.0, stats.getHitRate(), 0.01)
    }
    
    @Test
    fun testJarUpdateResult() {
        val jarInfo = JarInfo(
            key = "test", name = "Test", version = "1.1.0", vendor = "Test",
            description = "", url = "", filePath = "", fileSize = 1024L,
            spiderClasses = emptyList(), loadTime = 0L, checksum = ""
        )
        
        // 测试成功结果
        val successResult = JarUpdateResult.success(jarInfo)
        assertTrue(successResult.isSuccess)
        assertFalse(successResult.isFailure)
        assertTrue(successResult is JarUpdateResult.Success)
        assertEquals(jarInfo, (successResult as JarUpdateResult.Success).jarInfo)
        
        // 测试失败结果
        val failureResult = JarUpdateResult.failure("更新失败")
        assertFalse(failureResult.isSuccess)
        assertTrue(failureResult.isFailure)
        assertTrue(failureResult is JarUpdateResult.Failure)
        assertEquals("更新失败", (failureResult as JarUpdateResult.Failure).error)
    }
    
    @Test
    fun testJarUpdateEvents() {
        val updateInfo = JarUpdateInfo(
            jarKey = "test_jar",
            currentVersion = "1.0.0",
            latestVersion = "1.1.0",
            updateAvailable = true,
            updateUrl = "https://example.com/test-1.1.0.jar",
            updateSize = 2048L,
            releaseNotes = "新版本",
            updateTime = System.currentTimeMillis()
        )
        
        // 测试更新可用事件
        val updateAvailable = JarUpdateEvent.UpdateAvailable(updateInfo)
        assertEquals(updateInfo, updateAvailable.updateInfo)
        
        // 测试更新开始事件
        val updateStarted = JarUpdateEvent.UpdateStarted("test_jar")
        assertEquals("test_jar", updateStarted.jarKey)
        
        // 测试更新完成事件
        val updateCompleted = JarUpdateEvent.UpdateCompleted("test_jar", "1.0.0", "1.1.0")
        assertEquals("test_jar", updateCompleted.jarKey)
        assertEquals("1.0.0", updateCompleted.oldVersion)
        assertEquals("1.1.0", updateCompleted.newVersion)
        
        // 测试更新失败事件
        val updateFailed = JarUpdateEvent.UpdateFailed("test_jar", "网络错误")
        assertEquals("test_jar", updateFailed.jarKey)
        assertEquals("网络错误", updateFailed.error)
        
        // 测试更新回滚事件
        val updateRolledBack = JarUpdateEvent.UpdateRolledBack("test_jar", "更新失败")
        assertEquals("test_jar", updateRolledBack.jarKey)
        assertEquals("更新失败", updateRolledBack.reason)
        
        // 测试检查失败事件
        val checkFailed = JarUpdateEvent.CheckFailed("test_jar", "网络超时")
        assertEquals("test_jar", checkFailed.jarKey)
        assertEquals("网络超时", checkFailed.error)
    }
    
    @Test
    fun testJarDependency() {
        val dependency = JarDependency(
            name = "common-lib",
            version = "2.0.0",
            url = "https://example.com/common-lib-2.0.0.jar",
            required = true,
            loaded = false
        )
        
        assertTrue(dependency.isValid())
        assertEquals("common-lib", dependency.name)
        assertEquals("2.0.0", dependency.version)
        assertTrue(dependency.required)
        assertFalse(dependency.loaded)
        
        // 测试无效依赖
        val invalidDependency = dependency.copy(name = "", version = "")
        assertFalse(invalidDependency.isValid())
    }
}
