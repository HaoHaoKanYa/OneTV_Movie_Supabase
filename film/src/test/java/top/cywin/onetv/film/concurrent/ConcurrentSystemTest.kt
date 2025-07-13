package top.cywin.onetv.film.concurrent

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.cache.*

/**
 * 并发和缓存系统完整测试
 * 
 * 测试第九阶段实现的所有并发和缓存功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ConcurrentSystemTest {
    
    private lateinit var context: Context
    private lateinit var concurrentManager: ConcurrentManager
    private lateinit var cacheManager: CacheManager
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        concurrentManager = ConcurrentManager(context)
        cacheManager = CacheManager(context)
    }
    
    @Test
    fun testConcurrentTask() {
        // 测试并发任务
        val task = ConcurrentTask(
            id = "test_task",
            type = TaskType.IO,
            action = { "test_result" }
        )
        
        assertEquals("test_task", task.id)
        assertEquals(TaskType.IO, task.type)
        
        runTest {
            val result = task.execute()
            assertEquals("test_result", result)
        }
    }
    
    @Test
    fun testTaskType() {
        // 测试任务类型枚举
        assertEquals(5, TaskType.values().size)
        assertTrue(TaskType.values().contains(TaskType.IO))
        assertTrue(TaskType.values().contains(TaskType.COMPUTE))
        assertTrue(TaskType.values().contains(TaskType.MAIN))
        assertTrue(TaskType.values().contains(TaskType.NETWORK))
        assertTrue(TaskType.values().contains(TaskType.DATABASE))
    }
    
    @Test
    fun testConcurrentEvents() {
        // 测试并发事件
        val taskStarted = ConcurrentEvent.TaskStarted("task1", TaskType.IO)
        assertEquals("task1", taskStarted.taskId)
        assertEquals(TaskType.IO, taskStarted.type)
        
        val taskCompleted = ConcurrentEvent.TaskCompleted("task1", TaskType.IO, 1000L)
        assertEquals("task1", taskCompleted.taskId)
        assertEquals(TaskType.IO, taskCompleted.type)
        assertEquals(1000L, taskCompleted.duration)
        
        val taskFailed = ConcurrentEvent.TaskFailed("task1", TaskType.IO, "error")
        assertEquals("task1", taskFailed.taskId)
        assertEquals(TaskType.IO, taskFailed.type)
        assertEquals("error", taskFailed.error)
        
        val poolOverloaded = ConcurrentEvent.PoolOverloaded("main", 100)
        assertEquals("main", poolOverloaded.poolName)
        assertEquals(100, poolOverloaded.queueSize)
    }
    
    @Test
    fun testConcurrentStats() {
        val stats = ConcurrentStats()
        
        // 测试初始状态
        val initialStats = stats.getStats()
        assertEquals(0L, initialStats["io_tasks"])
        assertEquals(0L, initialStats["io_completed"])
        assertEquals(0L, initialStats["io_errors"])
        assertEquals(0.0, initialStats["io_success_rate"])
        
        // 测试记录操作
        stats.recordTaskStart(TaskType.IO)
        stats.recordTaskComplete(TaskType.IO, 1000L)
        
        val updatedStats = stats.getStats()
        assertEquals(1L, updatedStats["io_tasks"])
        assertEquals(1L, updatedStats["io_completed"])
        assertEquals(0L, updatedStats["io_errors"])
        assertEquals(1000L, updatedStats["io_avg_duration"])
        assertEquals(1.0, updatedStats["io_success_rate"])
        
        // 测试错误记录
        stats.recordTaskError(TaskType.IO, RuntimeException("test error"))
        val errorStats = stats.getStats()
        assertEquals(1L, errorStats["io_errors"])
        
        // 测试拒绝记录
        stats.recordRejection("main")
        val rejectionStats = stats.getStats()
        val rejections = rejectionStats["rejections"] as Map<String, Long>
        assertEquals(1L, rejections["main"])
        
        // 测试清理
        stats.clear()
        val clearedStats = stats.getStats()
        assertEquals(0L, clearedStats["io_tasks"])
        assertEquals(0L, clearedStats["io_completed"])
    }
    
    @Test
    fun testConcurrentManagerBasicFunctionality() {
        // 测试并发管理器基本功能
        assertNotNull(concurrentManager)
        
        // 测试获取线程池状态
        val threadPoolStatus = concurrentManager.getThreadPoolStatus()
        assertNotNull(threadPoolStatus)
        assertTrue(threadPoolStatus.containsKey("main_pool"))
        assertTrue(threadPoolStatus.containsKey("io_pool"))
        assertTrue(threadPoolStatus.containsKey("compute_pool"))
        
        val mainPool = threadPoolStatus["main_pool"] as Map<String, Any>
        assertEquals("main", mainPool["name"])
        assertTrue(mainPool["core_pool_size"] is Int)
        assertTrue(mainPool["max_pool_size"] is Int)
        
        // 测试获取统计信息
        val stats = concurrentManager.getConcurrentStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("io_tasks"))
        assertTrue(stats.containsKey("compute_tasks"))
        
        // 测试清理统计
        concurrentManager.clearStats()
        val clearedStats = concurrentManager.getConcurrentStats()
        assertEquals(0L, clearedStats["io_tasks"])
    }
    
    @Test
    fun testCacheEntry() {
        val entry = CacheEntry(
            key = "test_key",
            value = "test_value",
            createTime = System.currentTimeMillis(),
            lastAccessTime = System.currentTimeMillis(),
            ttl = 3600000L,
            strategy = CacheStrategy.MEMORY_FIRST,
            size = 1024L
        )
        
        assertEquals("test_key", entry.key)
        assertEquals("test_value", entry.value)
        assertEquals(CacheStrategy.MEMORY_FIRST, entry.strategy)
        assertEquals(1024L, entry.size)
        
        // 测试过期检查
        assertFalse(entry.isExpired())
        
        val expiredEntry = entry.copy(createTime = System.currentTimeMillis() - 7200000L) // 2小时前
        assertTrue(expiredEntry.isExpired())
        
        // 测试摘要信息
        val summary = entry.getSummary()
        assertNotNull(summary)
        assertTrue(summary.containsKey("key"))
        assertTrue(summary.containsKey("strategy"))
        assertTrue(summary.containsKey("size_bytes"))
        assertTrue(summary.containsKey("is_expired"))
        
        assertEquals("test_key", summary["key"])
        assertEquals("MEMORY_FIRST", summary["strategy"])
        assertEquals(1024L, summary["size_bytes"])
        assertFalse(summary["is_expired"] as Boolean)
    }
    
    @Test
    fun testSerializableCacheEntry() {
        val entry = SerializableCacheEntry(
            key = "test_key",
            value = "serialized_value",
            valueClass = "java.lang.String",
            createTime = System.currentTimeMillis(),
            lastAccessTime = System.currentTimeMillis(),
            ttl = 3600000L,
            strategy = CacheStrategy.DISK_ONLY,
            size = 512L
        )
        
        assertEquals("test_key", entry.key)
        assertEquals("serialized_value", entry.value)
        assertEquals("java.lang.String", entry.valueClass)
        assertEquals(CacheStrategy.DISK_ONLY, entry.strategy)
        assertEquals(512L, entry.size)
        
        // 测试过期检查
        assertFalse(entry.isExpired())
        
        val expiredEntry = entry.copy(createTime = System.currentTimeMillis() - 7200000L)
        assertTrue(expiredEntry.isExpired())
    }
    
    @Test
    fun testCacheStrategy() {
        // 测试缓存策略枚举
        assertEquals(4, CacheStrategy.values().size)
        assertTrue(CacheStrategy.values().contains(CacheStrategy.MEMORY_ONLY))
        assertTrue(CacheStrategy.values().contains(CacheStrategy.DISK_ONLY))
        assertTrue(CacheStrategy.values().contains(CacheStrategy.MEMORY_FIRST))
        assertTrue(CacheStrategy.values().contains(CacheStrategy.DISK_FIRST))
    }
    
    @Test
    fun testCacheLevel() {
        // 测试缓存级别枚举
        assertEquals(3, CacheLevel.values().size)
        assertTrue(CacheLevel.values().contains(CacheLevel.MEMORY))
        assertTrue(CacheLevel.values().contains(CacheLevel.DISK))
        assertTrue(CacheLevel.values().contains(CacheLevel.NETWORK))
    }
    
    @Test
    fun testCacheConfig() {
        // 测试默认配置
        val defaultConfig = CacheConfig()
        assertEquals(50 * 1024 * 1024L, defaultConfig.maxMemoryCacheSize)
        assertEquals(200 * 1024 * 1024L, defaultConfig.maxDiskCacheSize)
        assertEquals(3600000L, defaultConfig.defaultTtl)
        assertTrue(defaultConfig.enableDiskCache)
        assertFalse(defaultConfig.enableCompression)
        assertEquals(1800000L, defaultConfig.cleanupInterval)
        assertEquals(1000, defaultConfig.maxEntries)
        assertTrue(defaultConfig.enableMetrics)
        assertTrue(defaultConfig.isValid())
        
        // 测试自定义配置
        val customConfig = CacheConfig(
            maxMemoryCacheSize = 100 * 1024 * 1024L,
            maxDiskCacheSize = 500 * 1024 * 1024L,
            defaultTtl = 7200000L,
            enableDiskCache = false,
            enableCompression = true,
            cleanupInterval = 3600000L,
            maxEntries = 2000,
            enableMetrics = false
        )
        
        assertEquals(100 * 1024 * 1024L, customConfig.maxMemoryCacheSize)
        assertEquals(500 * 1024 * 1024L, customConfig.maxDiskCacheSize)
        assertEquals(7200000L, customConfig.defaultTtl)
        assertFalse(customConfig.enableDiskCache)
        assertTrue(customConfig.enableCompression)
        assertEquals(3600000L, customConfig.cleanupInterval)
        assertEquals(2000, customConfig.maxEntries)
        assertFalse(customConfig.enableMetrics)
        assertTrue(customConfig.isValid())
        
        // 测试无效配置
        val invalidConfig = CacheConfig(
            maxMemoryCacheSize = 0L,
            maxDiskCacheSize = -1L,
            defaultTtl = 0L
        )
        assertFalse(invalidConfig.isValid())
    }
    
    @Test
    fun testCacheEvents() {
        // 测试缓存事件
        val entryAdded = CacheEvent.EntryAdded("key1", CacheStrategy.MEMORY_FIRST, 1024L)
        assertEquals("key1", entryAdded.key)
        assertEquals(CacheStrategy.MEMORY_FIRST, entryAdded.strategy)
        assertEquals(1024L, entryAdded.size)
        
        val entryAccessed = CacheEvent.EntryAccessed("key1", CacheLevel.MEMORY)
        assertEquals("key1", entryAccessed.key)
        assertEquals(CacheLevel.MEMORY, entryAccessed.level)
        
        val entryMissed = CacheEvent.EntryMissed("key2")
        assertEquals("key2", entryMissed.key)
        
        val entryRemoved = CacheEvent.EntryRemoved("key1")
        assertEquals("key1", entryRemoved.key)
        
        val entryExpired = CacheEvent.EntryExpired("key3")
        assertEquals("key3", entryExpired.key)
        
        val cacheCleared = CacheEvent.CacheCleared
        assertNotNull(cacheCleared)
        
        val expiredCleared = CacheEvent.ExpiredEntriesCleared(5)
        assertEquals(5, expiredCleared.count)
        
        val spaceExhausted = CacheEvent.CacheSpaceExhausted(CacheLevel.MEMORY, 2048L)
        assertEquals(CacheLevel.MEMORY, spaceExhausted.level)
        assertEquals(2048L, spaceExhausted.requiredSize)
        
        val cacheError = CacheEvent.CacheError("put", "disk full")
        assertEquals("put", cacheError.operation)
        assertEquals("disk full", cacheError.error)
    }
    
    @Test
    fun testCacheStats() {
        val stats = CacheStats()
        
        // 测试初始状态
        assertEquals(0L, stats.hitCount.get())
        assertEquals(0L, stats.missCount.get())
        assertEquals(0L, stats.putCount.get())
        assertEquals(0L, stats.removeCount.get())
        assertEquals(0L, stats.errorCount.get())
        assertEquals(0.0, stats.getHitRate(), 0.01)
        
        // 测试记录操作
        stats.recordHit(CacheLevel.MEMORY)
        stats.recordHit(CacheLevel.DISK)
        stats.recordMiss()
        stats.recordPut(1024L)
        stats.recordRemove(512L)
        stats.recordError()
        
        assertEquals(2L, stats.hitCount.get())
        assertEquals(1L, stats.missCount.get())
        assertEquals(1L, stats.putCount.get())
        assertEquals(1L, stats.removeCount.get())
        assertEquals(1L, stats.errorCount.get())
        assertEquals(1024L, stats.totalPutSize.get())
        assertEquals(512L, stats.totalRemoveSize.get())
        
        // 命中率应该是 2/3 ≈ 0.67
        assertEquals(0.67, stats.getHitRate(), 0.01)
        
        // 测试级别命中率
        assertEquals(0.5, stats.getLevelHitRate(CacheLevel.MEMORY), 0.01)
        assertEquals(0.5, stats.getLevelHitRate(CacheLevel.DISK), 0.01)
        
        // 测试获取统计
        val statsMap = stats.getStats()
        assertEquals(2L, statsMap["hit_count"])
        assertEquals(1L, statsMap["miss_count"])
        assertEquals(3L, statsMap["total_requests"])
        assertEquals(0.67, statsMap["hit_rate"] as Double, 0.01)
        
        // 测试清理
        stats.clear()
        assertEquals(0L, stats.hitCount.get())
        assertEquals(0L, stats.missCount.get())
        assertEquals(0.0, stats.getHitRate(), 0.01)
    }
    
    @Test
    fun testCacheKeyBuilder() {
        // 测试缓存键构建器
        val key1 = CacheKeyBuilder.create()
            .add("prefix")
            .add("middle")
            .add("suffix")
            .build()
        assertEquals("prefix:middle:suffix", key1)
        
        val key2 = CacheKeyBuilder.create()
            .add("test")
            .add(123)
            .add(true)
            .build()
        assertEquals("test:123:true", key2)
        
        // 测试静态方法
        val key3 = CacheKeyBuilder.create("part1", "part2", 456)
        assertEquals("part1:part2:456", key3)
    }
    
    @Test
    fun testCacheResult() {
        // 测试成功结果
        val successResult = CacheResult.success("test_value", CacheLevel.MEMORY)
        assertTrue(successResult.isSuccess)
        assertFalse(successResult.isFailure)
        assertFalse(successResult.isMiss)
        assertTrue(successResult is CacheResult.Success)
        assertEquals("test_value", (successResult as CacheResult.Success).value)
        assertEquals(CacheLevel.MEMORY, successResult.level)
        
        // 测试失败结果
        val failureResult = CacheResult.failure<String>("error message")
        assertFalse(failureResult.isSuccess)
        assertTrue(failureResult.isFailure)
        assertFalse(failureResult.isMiss)
        assertTrue(failureResult is CacheResult.Failure)
        assertEquals("error message", (failureResult as CacheResult.Failure).error)
        
        // 测试未命中结果
        val missResult = CacheResult.miss<String>()
        assertFalse(missResult.isSuccess)
        assertFalse(missResult.isFailure)
        assertTrue(missResult.isMiss)
        assertTrue(missResult is CacheResult.Miss)
    }
    
    @Test
    fun testCacheManagerBasicFunctionality() {
        // 测试缓存管理器基本功能
        assertNotNull(cacheManager)
        
        // 测试获取统计信息
        val stats = cacheManager.getStats()
        assertNotNull(stats)
        assertTrue(stats.containsKey("memory_entries"))
        assertTrue(stats.containsKey("memory_size_bytes"))
        assertTrue(stats.containsKey("disk_size_bytes"))
        assertTrue(stats.containsKey("hit_count"))
        assertTrue(stats.containsKey("miss_count"))
        assertTrue(stats.containsKey("hit_rate"))
        
        assertEquals(0, stats["memory_entries"])
        assertEquals(0L, stats["memory_size_bytes"])
        assertEquals(0L, stats["hit_count"])
        assertEquals(0L, stats["miss_count"])
        assertEquals(0.0, stats["hit_rate"])
    }
    
    @Test
    fun testConcurrentUtils() = runTest {
        // 测试并行映射
        val items = listOf(1, 2, 3, 4, 5)
        val results = ConcurrentUtils.parallelMap(items, maxConcurrency = 2) { it * 2 }
        assertEquals(listOf(2, 4, 6, 8, 10), results)
        
        // 测试并行映射（过滤空值）
        val resultsNotNull = ConcurrentUtils.parallelMapNotNull(items, maxConcurrency = 2) { 
            if (it % 2 == 0) it * 2 else null 
        }
        assertEquals(listOf(4, 8), resultsNotNull)
        
        // 测试重试
        var attempts = 0
        val retryResult = ConcurrentUtils.retryWithBackoff(maxRetries = 3) {
            attempts++
            if (attempts < 3) throw RuntimeException("test error")
            "success"
        }
        assertEquals("success", retryResult)
        assertEquals(3, attempts)
    }
    
    @Test
    fun testConcurrentCounter() {
        val counter = ConcurrentUtils.ConcurrentCounter()
        
        assertEquals(0L, counter.get())
        assertEquals(1L, counter.increment())
        assertEquals(2L, counter.increment())
        assertEquals(1L, counter.decrement())
        assertEquals(1L, counter.get())
        
        counter.set(10L)
        assertEquals(10L, counter.get())
        
        counter.reset()
        assertEquals(0L, counter.get())
    }
    
    @Test
    fun testConcurrentLimiter() = runTest {
        val limiter = ConcurrentUtils.ConcurrentLimiter(2)
        
        assertEquals(0, limiter.getActiveCount())
        assertEquals(2, limiter.getAvailablePermits())
        
        val result = limiter.execute {
            assertEquals(1, limiter.getActiveCount())
            assertEquals(1, limiter.getAvailablePermits())
            "test_result"
        }
        
        assertEquals("test_result", result)
        assertEquals(0, limiter.getActiveCount())
        assertEquals(2, limiter.getAvailablePermits())
    }
}
