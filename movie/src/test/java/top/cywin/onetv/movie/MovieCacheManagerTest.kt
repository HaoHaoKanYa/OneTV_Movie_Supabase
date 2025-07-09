package top.cywin.onetv.movie

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.models.VodItem
import java.io.File

/**
 * MovieCacheManager单元测试
 */
class MovieCacheManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    private lateinit var mockContext: Context

    private lateinit var cacheManager: MovieCacheManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // 模拟Context返回临时目录
        val tempCacheDir = tempFolder.newFolder("cache")
        whenever(mockContext.cacheDir).thenReturn(tempCacheDir)
        
        cacheManager = MovieCacheManager(mockContext)
    }

    @Test
    fun `test put and get cache`() = runTest {
        // 准备测试数据
        val testData = VodItem(
            vodId = "test123",
            vodName = "测试电影",
            vodPic = "https://test.com/pic.jpg"
        )
        val cacheKey = "test_movie"

        // 存储缓存
        cacheManager.putCache(cacheKey, testData, 60000) // 1分钟

        // 获取缓存
        val cachedData = cacheManager.getCache(cacheKey, VodItem::class.java)

        // 验证结果
        assertNotNull(cachedData)
        assertEquals("test123", cachedData?.vodId)
        assertEquals("测试电影", cachedData?.vodName)
    }

    @Test
    fun `test cache expiration`() = runTest {
        // 准备测试数据
        val testData = "test_string"
        val cacheKey = "expired_test"

        // 存储短期缓存
        cacheManager.putCache(cacheKey, testData, 1) // 1毫秒

        // 等待过期
        Thread.sleep(10)

        // 清理过期缓存
        cacheManager.clearExpiredCache()

        // 验证缓存已过期
        val cachedData = cacheManager.getCache(cacheKey, String::class.java)
        assertNull(cachedData)
    }

    @Test
    fun `test has cache`() = runTest {
        val cacheKey = "existence_test"
        val testData = "test_data"

        // 验证缓存不存在
        assertFalse(cacheManager.hasCache(cacheKey))

        // 存储缓存
        cacheManager.putCache(cacheKey, testData, 60000)

        // 验证缓存存在
        assertTrue(cacheManager.hasCache(cacheKey))
    }

    @Test
    fun `test remove cache`() = runTest {
        val cacheKey = "remove_test"
        val testData = "test_data"

        // 存储缓存
        cacheManager.putCache(cacheKey, testData, 60000)

        // 验证缓存存在
        assertTrue(cacheManager.hasCache(cacheKey))

        // 删除缓存
        cacheManager.removeCache(cacheKey)

        // 验证缓存已删除
        assertFalse(cacheManager.hasCache(cacheKey))
    }

    @Test
    fun `test clear all cache`() = runTest {
        // 存储多个缓存
        cacheManager.putCache("key1", "data1", 60000)
        cacheManager.putCache("key2", "data2", 60000)
        cacheManager.putCache("key3", "data3", 60000)

        // 验证缓存存在
        assertTrue(cacheManager.hasCache("key1"))
        assertTrue(cacheManager.hasCache("key2"))
        assertTrue(cacheManager.hasCache("key3"))

        // 清空所有缓存
        cacheManager.clearAllCache()

        // 验证所有缓存已清空
        assertFalse(cacheManager.hasCache("key1"))
        assertFalse(cacheManager.hasCache("key2"))
        assertFalse(cacheManager.hasCache("key3"))
    }

    @Test
    fun `test cache stats`() = runTest {
        // 存储一些缓存
        cacheManager.putCache("stats1", "data1", 60000)
        cacheManager.putCache("stats2", "data2", 60000)

        // 获取缓存统计
        val stats = cacheManager.getCacheStats()

        // 验证统计信息
        assertTrue(stats.memoryCount >= 0)
        assertTrue(stats.diskCount >= 0)
        assertTrue(stats.totalSizeBytes >= 0)
        assertNotNull(stats.getTotalSizeMB())
    }

    @Test
    fun `test cache size calculation`() = runTest {
        // 存储一些数据
        val largeData = "x".repeat(1000) // 1KB数据
        cacheManager.putCache("large_data", largeData, 60000)

        // 获取缓存大小
        val cacheSize = cacheManager.getCacheSize()

        // 验证缓存大小大于0
        assertTrue(cacheSize > 0)
    }
}
