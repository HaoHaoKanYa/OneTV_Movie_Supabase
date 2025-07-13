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
import top.cywin.onetv.film.data.repository.FilmRepository
import top.cywin.onetv.film.optimization.PerformanceBenchmark
import top.cywin.onetv.film.optimization.BenchmarkDirection
import top.cywin.onetv.film.concurrent.ConcurrentUtils
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * 性能基准测试
 * 
 * 基于 FongMi/TV 的性能基准测试实现
 * 测试系统各组件的性能指标和基准
 * 
 * 功能：
 * - 响应时间基准测试
 * - 吞吐量基准测试
 * - 内存使用基准测试
 * - 并发性能基准测试
 * - 缓存性能基准测试
 * - 网络性能基准测试
 * - 综合性能基准测试
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class PerformanceBenchmarkTest {
    
    companion object {
        private const val TAG = "ONETV_FILM_PERFORMANCE_BENCHMARK"
        private const val TEST_TIMEOUT = 120000L // 2分钟
        private const val WARMUP_ITERATIONS = 10
        private const val BENCHMARK_ITERATIONS = 100
        private const val CONCURRENT_USERS = 20
        private const val OPERATIONS_PER_USER = 50
    }
    
    private lateinit var context: Context
    private lateinit var spiderManager: SpiderManager
    private lateinit var filmRepository: FilmRepository
    
    // 基准结果
    private val benchmarkResults = mutableListOf<PerformanceBenchmark>()
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        spiderManager = SpiderManager(context)
        filmRepository = spiderManager.getFilmRepository()
        
        Log.d(TAG, "🏗️ 性能基准测试环境初始化完成")
    }
    
    /**
     * ⏱️ 响应时间基准测试
     */
    @Test
    fun testResponseTimeBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "⏱️ 开始响应时间基准测试")
        
        // 预热
        repeat(WARMUP_ITERATIONS) {
            performResponseTimeOperation()
        }
        
        // 基准测试
        val responseTimes = mutableListOf<Long>()
        
        repeat(BENCHMARK_ITERATIONS) {
            val responseTime = measureTimeMillis {
                performResponseTimeOperation()
            }
            responseTimes.add(responseTime)
        }
        
        // 计算统计指标
        val avgResponseTime = responseTimes.average()
        val minResponseTime = responseTimes.minOrNull() ?: 0L
        val maxResponseTime = responseTimes.maxOrNull() ?: 0L
        val p50ResponseTime = responseTimes.sorted()[responseTimes.size / 2]
        val p95ResponseTime = responseTimes.sorted()[(responseTimes.size * 0.95).toInt()]
        val p99ResponseTime = responseTimes.sorted()[(responseTimes.size * 0.99).toInt()]
        
        Log.d(TAG, "响应时间基准测试结果:")
        Log.d(TAG, "  平均响应时间: ${String.format("%.2f", avgResponseTime)}ms")
        Log.d(TAG, "  最小响应时间: ${minResponseTime}ms")
        Log.d(TAG, "  最大响应时间: ${maxResponseTime}ms")
        Log.d(TAG, "  P50响应时间: ${p50ResponseTime}ms")
        Log.d(TAG, "  P95响应时间: ${p95ResponseTime}ms")
        Log.d(TAG, "  P99响应时间: ${p99ResponseTime}ms")
        
        // 记录基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "平均响应时间",
                category = "响应时间",
                targetValue = 500.0, // 目标500ms
                currentValue = avgResponseTime,
                unit = "ms",
                direction = BenchmarkDirection.LOWER_IS_BETTER
            )
        )
        
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "P95响应时间",
                category = "响应时间",
                targetValue = 1000.0, // 目标1秒
                currentValue = p95ResponseTime.toDouble(),
                unit = "ms",
                direction = BenchmarkDirection.LOWER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("平均响应时间应小于1秒", avgResponseTime < 1000.0)
        assertTrue("P95响应时间应小于2秒", p95ResponseTime < 2000L)
        assertTrue("P99响应时间应小于5秒", p99ResponseTime < 5000L)
        
        Log.d(TAG, "✅ 响应时间基准测试完成")
    }
    
    /**
     * 🔄 响应时间测试操作
     */
    private suspend fun performResponseTimeOperation() {
        val operations = listOf(
            { filmRepository.getRepositoryStats() },
            { spiderManager.getConcurrentStats() },
            { spiderManager.getCacheStats() }
        )
        
        val operation = operations.random()
        operation()
    }
    
    /**
     * 📈 吞吐量基准测试
     */
    @Test
    fun testThroughputBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "📈 开始吞吐量基准测试")
        
        val testDuration = 30000L // 30秒
        val operationCount = AtomicLong(0)
        val errorCount = AtomicLong(0)
        
        val startTime = System.currentTimeMillis()
        val endTime = startTime + testDuration
        
        // 并发执行操作
        val jobs = (1..CONCURRENT_USERS).map {
            async {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        performThroughputOperation()
                        operationCount.incrementAndGet()
                    } catch (e: Exception) {
                        errorCount.incrementAndGet()
                    }
                    delay(10L) // 短暂延迟避免过度消耗资源
                }
            }
        }
        
        jobs.awaitAll()
        
        val actualDuration = System.currentTimeMillis() - startTime
        val totalOperations = operationCount.get()
        val totalErrors = errorCount.get()
        val throughput = totalOperations.toDouble() / (actualDuration / 1000.0)
        val errorRate = totalErrors.toDouble() / totalOperations
        
        Log.d(TAG, "吞吐量基准测试结果:")
        Log.d(TAG, "  测试时长: ${actualDuration}ms")
        Log.d(TAG, "  总操作数: $totalOperations")
        Log.d(TAG, "  错误数: $totalErrors")
        Log.d(TAG, "  吞吐量: ${String.format("%.2f", throughput)} ops/sec")
        Log.d(TAG, "  错误率: ${String.format("%.2f%%", errorRate * 100)}")
        
        // 记录基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "系统吞吐量",
                category = "吞吐量",
                targetValue = 100.0, // 目标100 ops/sec
                currentValue = throughput,
                unit = "ops/sec",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "错误率",
                category = "可靠性",
                targetValue = 0.01, // 目标1%
                currentValue = errorRate,
                unit = "%",
                direction = BenchmarkDirection.LOWER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("吞吐量应大于50 ops/sec", throughput > 50.0)
        assertTrue("错误率应小于5%", errorRate < 0.05)
        
        Log.d(TAG, "✅ 吞吐量基准测试完成")
    }
    
    /**
     * 🔄 吞吐量测试操作
     */
    private suspend fun performThroughputOperation() {
        val operations = listOf(
            {
                // 缓存操作
                val cacheManager = spiderManager.getCacheManager()
                val key = "throughput_test_${System.nanoTime()}"
                cacheManager.put(key, "test_value")
                cacheManager.get(key, String::class.java)
            },
            {
                // 并发操作
                val concurrentManager = spiderManager.getConcurrentManager()
                concurrentManager.executeWithTimeout(1000L) {
                    delay(1L)
                    "result"
                }
            },
            {
                // 统计操作
                filmRepository.getRepositoryStats()
            }
        )
        
        val operation = operations.random()
        operation()
    }
    
    /**
     * 🧠 内存使用基准测试
     */
    @Test
    fun testMemoryUsageBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🧠 开始内存使用基准测试")
        
        val runtime = Runtime.getRuntime()
        
        // 记录初始内存状态
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // 执行内存密集型操作
        val operations = mutableListOf<Any>()
        
        repeat(1000) {
            // 创建一些对象来模拟内存使用
            operations.add(createMemoryTestObject())
        }
        
        // 记录峰值内存使用
        val peakMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = peakMemory - initialMemory
        
        // 清理对象
        operations.clear()
        System.gc()
        delay(1000L) // 等待GC完成
        
        // 记录清理后内存
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryRecovered = peakMemory - finalMemory
        val recoveryRate = memoryRecovered.toDouble() / memoryIncrease
        
        Log.d(TAG, "内存使用基准测试结果:")
        Log.d(TAG, "  初始内存: ${String.format("%.2f", initialMemory / 1024.0 / 1024.0)}MB")
        Log.d(TAG, "  峰值内存: ${String.format("%.2f", peakMemory / 1024.0 / 1024.0)}MB")
        Log.d(TAG, "  内存增长: ${String.format("%.2f", memoryIncrease / 1024.0 / 1024.0)}MB")
        Log.d(TAG, "  最终内存: ${String.format("%.2f", finalMemory / 1024.0 / 1024.0)}MB")
        Log.d(TAG, "  内存回收: ${String.format("%.2f", memoryRecovered / 1024.0 / 1024.0)}MB")
        Log.d(TAG, "  回收率: ${String.format("%.1f%%", recoveryRate * 100)}")
        
        // 记录基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "内存回收率",
                category = "内存管理",
                targetValue = 0.8, // 目标80%
                currentValue = recoveryRate,
                unit = "%",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("内存回收率应大于70%", recoveryRate > 0.7)
        
        Log.d(TAG, "✅ 内存使用基准测试完成")
    }
    
    /**
     * 🔄 创建内存测试对象
     */
    private fun createMemoryTestObject(): Any {
        return mapOf(
            "id" to System.nanoTime(),
            "data" to ByteArray(1024), // 1KB数据
            "timestamp" to System.currentTimeMillis(),
            "metadata" to mapOf(
                "type" to "test",
                "version" to "1.0",
                "description" to "Memory test object for benchmark"
            )
        )
    }
    
    /**
     * ⚡ 并发性能基准测试
     */
    @Test
    fun testConcurrentPerformanceBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "⚡ 开始并发性能基准测试")
        
        val concurrentManager = spiderManager.getConcurrentManager()
        val taskCount = 100
        val completedTasks = AtomicInteger(0)
        val failedTasks = AtomicInteger(0)
        val totalExecutionTime = AtomicLong(0)
        
        val startTime = System.currentTimeMillis()
        
        // 并发执行任务
        val tasks = (1..taskCount).map { taskId ->
            async {
                val taskStartTime = System.currentTimeMillis()
                try {
                    val result = concurrentManager.executeWithTimeout(5000L) {
                        delay(50L) // 模拟工作
                        "Task $taskId result"
                    }
                    
                    val taskDuration = System.currentTimeMillis() - taskStartTime
                    totalExecutionTime.addAndGet(taskDuration)
                    
                    if (result != null) {
                        completedTasks.incrementAndGet()
                    } else {
                        failedTasks.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failedTasks.incrementAndGet()
                }
            }
        }
        
        tasks.awaitAll()
        
        val totalTime = System.currentTimeMillis() - startTime
        val completed = completedTasks.get()
        val failed = failedTasks.get()
        val successRate = completed.toDouble() / taskCount
        val avgExecutionTime = if (completed > 0) totalExecutionTime.get() / completed else 0L
        val throughput = taskCount.toDouble() / (totalTime / 1000.0)
        
        Log.d(TAG, "并发性能基准测试结果:")
        Log.d(TAG, "  任务总数: $taskCount")
        Log.d(TAG, "  完成任务: $completed")
        Log.d(TAG, "  失败任务: $failed")
        Log.d(TAG, "  成功率: ${String.format("%.1f%%", successRate * 100)}")
        Log.d(TAG, "  平均执行时间: ${avgExecutionTime}ms")
        Log.d(TAG, "  总耗时: ${totalTime}ms")
        Log.d(TAG, "  并发吞吐量: ${String.format("%.2f", throughput)} tasks/sec")
        
        // 记录基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "并发任务成功率",
                category = "并发性能",
                targetValue = 0.95, // 目标95%
                currentValue = successRate,
                unit = "%",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "并发吞吐量",
                category = "并发性能",
                targetValue = 50.0, // 目标50 tasks/sec
                currentValue = throughput,
                unit = "tasks/sec",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("并发任务成功率应大于90%", successRate > 0.9)
        assertTrue("并发吞吐量应大于20 tasks/sec", throughput > 20.0)
        
        Log.d(TAG, "✅ 并发性能基准测试完成")
    }
    
    /**
     * 🗄️ 缓存性能基准测试
     */
    @Test
    fun testCachePerformanceBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "🗄️ 开始缓存性能基准测试")
        
        val cacheManager = spiderManager.getCacheManager()
        val operationCount = 1000
        val hitCount = AtomicInteger(0)
        val missCount = AtomicInteger(0)
        
        // 预填充缓存
        repeat(operationCount / 2) { i ->
            cacheManager.put("cache_test_$i", "value_$i")
        }
        
        val startTime = System.currentTimeMillis()
        
        // 执行缓存操作
        repeat(operationCount) { i ->
            val key = "cache_test_${i % (operationCount / 2)}"
            val value = cacheManager.get(key, String::class.java)
            
            if (value != null) {
                hitCount.incrementAndGet()
            } else {
                missCount.incrementAndGet()
            }
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        val hits = hitCount.get()
        val misses = missCount.get()
        val hitRate = hits.toDouble() / operationCount
        val cacheOpsPerSecond = operationCount.toDouble() / (totalTime / 1000.0)
        
        Log.d(TAG, "缓存性能基准测试结果:")
        Log.d(TAG, "  操作总数: $operationCount")
        Log.d(TAG, "  缓存命中: $hits")
        Log.d(TAG, "  缓存未命中: $misses")
        Log.d(TAG, "  命中率: ${String.format("%.1f%%", hitRate * 100)}")
        Log.d(TAG, "  总耗时: ${totalTime}ms")
        Log.d(TAG, "  缓存操作速度: ${String.format("%.2f", cacheOpsPerSecond)} ops/sec")
        
        // 记录基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "缓存命中率",
                category = "缓存性能",
                targetValue = 0.8, // 目标80%
                currentValue = hitRate,
                unit = "%",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "缓存操作速度",
                category = "缓存性能",
                targetValue = 1000.0, // 目标1000 ops/sec
                currentValue = cacheOpsPerSecond,
                unit = "ops/sec",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("缓存命中率应大于70%", hitRate > 0.7)
        assertTrue("缓存操作速度应大于500 ops/sec", cacheOpsPerSecond > 500.0)
        
        Log.d(TAG, "✅ 缓存性能基准测试完成")
    }
    
    /**
     * 📊 综合性能基准测试
     */
    @Test
    fun testOverallPerformanceBenchmark() = runTest(timeout = TEST_TIMEOUT) {
        Log.d(TAG, "📊 开始综合性能基准测试")
        
        // 执行所有基准测试
        testResponseTimeBenchmark()
        testThroughputBenchmark()
        testMemoryUsageBenchmark()
        testConcurrentPerformanceBenchmark()
        testCachePerformanceBenchmark()
        
        // 计算综合评分
        val overallScore = calculateOverallScore()
        
        Log.d(TAG, "综合性能基准测试结果:")
        Log.d(TAG, "  基准测试数量: ${benchmarkResults.size}")
        Log.d(TAG, "  达标基准数量: ${benchmarkResults.count { it.isTargetMet() }}")
        Log.d(TAG, "  综合评分: ${String.format("%.1f", overallScore)}")
        
        // 输出详细基准结果
        benchmarkResults.forEach { benchmark ->
            val summary = benchmark.getSummary()
            Log.d(TAG, "  ${summary["name"]}: ${summary["current_value"]} ${summary["unit"]} " +
                    "(目标: ${summary["target_value"]} ${summary["unit"]}, " +
                    "达成率: ${summary["achievement_rate"]}, " +
                    "达标: ${summary["target_met"]})")
        }
        
        // 记录综合基准
        benchmarkResults.add(
            PerformanceBenchmark(
                name = "综合性能评分",
                category = "综合性能",
                targetValue = 80.0, // 目标80分
                currentValue = overallScore,
                unit = "分",
                direction = BenchmarkDirection.HIGHER_IS_BETTER
            )
        )
        
        // 性能断言
        assertTrue("综合性能评分应大于70分", overallScore > 70.0)
        assertTrue("至少80%的基准应达标", 
            benchmarkResults.count { it.isTargetMet() }.toDouble() / benchmarkResults.size > 0.8)
        
        Log.d(TAG, "✅ 综合性能基准测试完成")
    }
    
    /**
     * 📊 计算综合评分
     */
    private fun calculateOverallScore(): Double {
        if (benchmarkResults.isEmpty()) return 0.0
        
        val totalScore = benchmarkResults.sumOf { benchmark ->
            benchmark.getAchievementRate() * 100.0
        }
        
        return totalScore / benchmarkResults.size
    }
    
    /**
     * 📊 获取基准测试结果
     */
    fun getBenchmarkResults(): List<PerformanceBenchmark> {
        return benchmarkResults.toList()
    }
}
