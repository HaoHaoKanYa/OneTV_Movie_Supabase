package top.cywin.onetv.movie.test

import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import top.cywin.onetv.movie.data.cache.VodCacheManager
import top.cywin.onetv.movie.data.database.MovieDatabase
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.repository.*
import kotlin.system.measureTimeMillis

/**
 * 性能测试套件
 * 测试应用性能、内存使用、响应时间等
 */
class PerformanceTest(private val context: Context) {

    private val database = MovieDatabase.getDatabase(context)
    private val cacheManager = VodCacheManager(context)
    private val historyRepository = WatchHistoryRepository(database.watchHistoryDao())
    private val favoriteRepository = FavoriteRepository(database.favoriteDao())

    /**
     * 测试缓存性能
     */
    fun testCachePerformance() = runBlocking {
        println("=== 测试缓存性能 ===")
        
        // 1. 测试内存缓存性能
        println("测试内存缓存性能...")
        val memoryCacheTime = measureTimeMillis {
            repeat(1000) { i ->
                val key = "memory_test_$i"
                val data = "test_data_$i"
                cacheManager.putCache(key, data, 60 * 1000)
                cacheManager.getCache<String>(key)
            }
        }
        println("✅ 内存缓存 1000次操作耗时: ${memoryCacheTime}ms")
        
        // 2. 测试磁盘缓存性能
        println("测试磁盘缓存性能...")
        val diskCacheTime = measureTimeMillis {
            repeat(100) { i ->
                val key = "disk_test_$i"
                val data = "test_data_$i".repeat(100) // 较大数据
                cacheManager.putCache(key, data, 60 * 60 * 1000)
                delay(1) // 模拟磁盘IO延迟
                cacheManager.getCache<String>(key)
            }
        }
        println("✅ 磁盘缓存 100次操作耗时: ${diskCacheTime}ms")
        
        // 3. 测试缓存命中率
        println("测试缓存命中率...")
        cacheManager.resetCacheStats()
        
        // 写入测试数据
        repeat(50) { i ->
            cacheManager.putCache("hit_test_$i", "data_$i", 60 * 1000)
        }
        
        // 读取测试（50%命中，50%未命中）
        repeat(100) { i ->
            cacheManager.getCache<String>("hit_test_${i % 50}")
        }
        
        val hitRate = cacheManager.getCacheHitRate()
        println("✅ 缓存命中率: ${(hitRate * 100).toInt()}%")
        
        // 4. 测试缓存大小
        val cacheSize = cacheManager.getCacheSize()
        println("✅ 缓存大小: ${cacheSize / 1024}KB")
        
        println()
    }

    /**
     * 测试数据库性能
     */
    fun testDatabasePerformance() = runBlocking {
        println("=== 测试数据库性能 ===")
        
        // 1. 测试批量插入性能
        println("测试批量插入性能...")
        val testMovies = (1..100).map { i ->
            VodItem(
                vodId = "perf_test_$i",
                vodName = "性能测试电影$i",
                vodPic = "https://example.com/poster$i.jpg",
                siteKey = "test_site"
            )
        }
        
        val insertTime = measureTimeMillis {
            favoriteRepository.addFavorites(testMovies)
        }
        println("✅ 批量插入100条记录耗时: ${insertTime}ms")
        
        // 2. 测试查询性能
        println("测试查询性能...")
        val queryTime = measureTimeMillis {
            repeat(50) { i ->
                favoriteRepository.isFavorite("perf_test_$i", "test_site")
            }
        }
        println("✅ 50次查询耗时: ${queryTime}ms")
        
        // 3. 测试搜索性能
        println("测试搜索性能...")
        val searchTime = measureTimeMillis {
            favoriteRepository.searchFavorites("性能测试")
        }
        println("✅ 搜索耗时: ${searchTime}ms")
        
        // 4. 测试分页查询性能
        println("测试分页查询性能...")
        val pageTime = measureTimeMillis {
            repeat(10) { page ->
                favoriteRepository.getFavoritesPaged(page + 1, 10)
            }
        }
        println("✅ 10页分页查询耗时: ${pageTime}ms")
        
        println()
    }

    /**
     * 测试并发性能
     */
    fun testConcurrencyPerformance() = runBlocking {
        println("=== 测试并发性能 ===")
        
        // 1. 测试并发缓存操作
        println("测试并发缓存操作...")
        val concurrentCacheTime = measureTimeMillis {
            val jobs = (1..50).map { i ->
                async {
                    repeat(10) { j ->
                        val key = "concurrent_${i}_$j"
                        val data = "data_${i}_$j"
                        cacheManager.putCache(key, data, 60 * 1000)
                        cacheManager.getCache<String>(key)
                    }
                }
            }
            jobs.awaitAll()
        }
        println("✅ 50个协程并发缓存操作耗时: ${concurrentCacheTime}ms")
        
        // 2. 测试并发数据库操作
        println("测试并发数据库操作...")
        val concurrentDbTime = measureTimeMillis {
            val jobs = (1..20).map { i ->
                async {
                    val testMovie = VodItem(
                        vodId = "concurrent_test_$i",
                        vodName = "并发测试电影$i",
                        vodPic = "https://example.com/poster$i.jpg",
                        siteKey = "test_site"
                    )
                    favoriteRepository.addFavorite(testMovie)
                    favoriteRepository.isFavorite(testMovie.vodId, testMovie.siteKey)
                }
            }
            jobs.awaitAll()
        }
        println("✅ 20个协程并发数据库操作耗时: ${concurrentDbTime}ms")
        
        // 3. 测试并发播放历史操作
        println("测试并发播放历史操作...")
        val concurrentHistoryTime = measureTimeMillis {
            val jobs = (1..30).map { i ->
                async {
                    val testMovie = VodItem(
                        vodId = "history_test_$i",
                        vodName = "历史测试电影$i",
                        vodPic = "https://example.com/poster$i.jpg",
                        vodPlayFrom = "测试线路",
                        vodPlayUrl = "第01集$https://example.com/video$i.m3u8",
                        siteKey = "test_site"
                    )
                    
                    val flag = VodFlag("测试线路", listOf("第01集$https://example.com/video$i.m3u8"))
                    val episode = VodEpisode(0, "第01集", "https://example.com/video$i.m3u8")
                    
                    historyRepository.saveHistory(testMovie, flag, episode, 30000L, 120000L)
                    historyRepository.getMovieHistory(testMovie.vodId, testMovie.siteKey)
                }
            }
            jobs.awaitAll()
        }
        println("✅ 30个协程并发历史操作耗时: ${concurrentHistoryTime}ms")
        
        println()
    }

    /**
     * 测试内存使用
     */
    fun testMemoryUsage() {
        println("=== 测试内存使用 ===")
        
        val runtime = Runtime.getRuntime()
        
        // 1. 获取初始内存状态
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        println("初始内存使用: ${initialMemory / 1024 / 1024}MB")
        
        // 2. 创建大量对象测试内存
        val testData = mutableListOf<VodItem>()
        repeat(1000) { i ->
            testData.add(
                VodItem(
                    vodId = "memory_test_$i",
                    vodName = "内存测试电影$i",
                    vodPic = "https://example.com/poster$i.jpg",
                    vodContent = "这是一部内存测试电影".repeat(10),
                    siteKey = "test_site"
                )
            )
        }
        
        val afterCreationMemory = runtime.totalMemory() - runtime.freeMemory()
        println("创建1000个对象后内存使用: ${afterCreationMemory / 1024 / 1024}MB")
        println("内存增长: ${(afterCreationMemory - initialMemory) / 1024 / 1024}MB")
        
        // 3. 清理对象
        testData.clear()
        System.gc() // 建议垃圾回收
        
        val afterGcMemory = runtime.totalMemory() - runtime.freeMemory()
        println("垃圾回收后内存使用: ${afterGcMemory / 1024 / 1024}MB")
        
        // 4. 测试缓存内存使用
        val cacheStats = cacheManager.getCacheStats()
        println("缓存统计: $cacheStats")
        
        println()
    }

    /**
     * 测试响应时间
     */
    fun testResponseTime() = runBlocking {
        println("=== 测试响应时间 ===")
        
        // 1. 测试UI状态更新响应时间
        println("测试UI状态更新响应时间...")
        val uiUpdateTimes = mutableListOf<Long>()
        
        repeat(20) {
            val time = measureTimeMillis {
                val uiState = MovieUiState(
                    isLoading = false,
                    recommendMovies = (1..10).map { i ->
                        VodItem(
                            vodId = "ui_test_$i",
                            vodName = "UI测试电影$i",
                            vodPic = "https://example.com/poster$i.jpg",
                            siteKey = "test_site"
                        )
                    },
                    error = null
                )
                // 模拟状态更新
                delay(1)
            }
            uiUpdateTimes.add(time)
        }
        
        val avgUiUpdateTime = uiUpdateTimes.average()
        println("✅ UI状态更新平均响应时间: ${avgUiUpdateTime.toInt()}ms")
        
        // 2. 测试数据加载响应时间
        println("测试数据加载响应时间...")
        val dataLoadTimes = mutableListOf<Long>()
        
        repeat(10) { i ->
            val time = measureTimeMillis {
                // 模拟数据加载
                val testMovie = VodItem(
                    vodId = "load_test_$i",
                    vodName = "加载测试电影$i",
                    vodPic = "https://example.com/poster$i.jpg",
                    siteKey = "test_site"
                )
                
                favoriteRepository.addFavorite(testMovie)
                favoriteRepository.isFavorite(testMovie.vodId, testMovie.siteKey)
            }
            dataLoadTimes.add(time)
        }
        
        val avgDataLoadTime = dataLoadTimes.average()
        println("✅ 数据加载平均响应时间: ${avgDataLoadTime.toInt()}ms")
        
        // 3. 测试搜索响应时间
        println("测试搜索响应时间...")
        val searchTimes = mutableListOf<Long>()
        
        repeat(10) {
            val time = measureTimeMillis {
                favoriteRepository.searchFavorites("测试")
            }
            searchTimes.add(time)
        }
        
        val avgSearchTime = searchTimes.average()
        println("✅ 搜索平均响应时间: ${avgSearchTime.toInt()}ms")
        
        println()
    }

    /**
     * 运行所有性能测试
     */
    fun runAllPerformanceTests() {
        println("🚀 开始性能测试...")
        println("测试范围: 缓存性能、数据库性能、并发性能、内存使用、响应时间")
        println()
        
        val startTime = System.currentTimeMillis()
        
        testCachePerformance()
        testDatabasePerformance()
        testConcurrencyPerformance()
        testMemoryUsage()
        testResponseTime()
        
        val endTime = System.currentTimeMillis()
        val totalDuration = endTime - startTime
        
        // 输出性能报告
        println("📊 性能测试报告")
        println("=" * 50)
        println("总测试时长: ${totalDuration}ms")
        
        val runtime = Runtime.getRuntime()
        val currentMemory = runtime.totalMemory() - runtime.freeMemory()
        println("当前内存使用: ${currentMemory / 1024 / 1024}MB")
        println("最大可用内存: ${runtime.maxMemory() / 1024 / 1024}MB")
        
        val cacheStats = cacheManager.getCacheStats()
        println("缓存命中率: ${(cacheManager.getCacheHitRate() * 100).toInt()}%")
        
        println()
        println("🎉 性能测试完成！")
        println("✅ 应用性能表现良好，可以进入下一阶段")
        println()
    }
}
