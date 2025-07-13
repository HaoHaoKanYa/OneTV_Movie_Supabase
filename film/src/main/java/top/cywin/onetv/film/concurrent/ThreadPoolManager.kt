package top.cywin.onetv.film.concurrent

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 线程池管理器
 * 
 * 基于 FongMi/TV 的线程池管理实现
 * 提供多种类型的线程池和智能调度
 * 
 * 功能：
 * - 多类型线程池管理
 * - 动态线程池调整
 * - 线程池监控
 * - 任务队列管理
 * - 性能优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object ThreadPoolManager {
    
    private const val TAG = "ONETV_FILM_THREAD_POOL_MANAGER"
    
    // 线程池配置
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    private val CORE_POOL_SIZE = maxOf(2, CPU_COUNT)
    private val MAX_POOL_SIZE = CPU_COUNT * 2
    private val KEEP_ALIVE_TIME = 60L
    private val QUEUE_CAPACITY = 128
    
    // 线程池实例
    private val threadPools = ConcurrentHashMap<String, ThreadPoolExecutor>()
    private val poolStats = ConcurrentHashMap<String, ThreadPoolStats>()
    
    // 默认线程池
    val defaultPool: ThreadPoolExecutor by lazy { 
        createThreadPool("default", CORE_POOL_SIZE, MAX_POOL_SIZE)
    }
    
    val ioPool: ThreadPoolExecutor by lazy { 
        createThreadPool("io", CORE_POOL_SIZE * 2, MAX_POOL_SIZE * 2)
    }
    
    val computePool: ThreadPoolExecutor by lazy { 
        createThreadPool("compute", CORE_POOL_SIZE, CORE_POOL_SIZE)
    }
    
    val networkPool: ThreadPoolExecutor by lazy { 
        createThreadPool("network", CORE_POOL_SIZE, MAX_POOL_SIZE)
    }
    
    val cachePool: ThreadPoolExecutor by lazy { 
        createThreadPool("cache", 2, 4)
    }
    
    val singlePool: ThreadPoolExecutor by lazy { 
        createThreadPool("single", 1, 1)
    }
    
    /**
     * 🏗️ 创建线程池
     */
    fun createThreadPool(
        name: String,
        corePoolSize: Int = CORE_POOL_SIZE,
        maximumPoolSize: Int = MAX_POOL_SIZE,
        keepAliveTime: Long = KEEP_ALIVE_TIME,
        queueCapacity: Int = QUEUE_CAPACITY
    ): ThreadPoolExecutor {
        
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, "OneTV-$name-${Thread.currentThread().id}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
                uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, exception ->
                    Log.e(TAG, "❌ 线程异常: ${thread.name}", exception)
                }
            }
        }
        
        val rejectedExecutionHandler = RejectedExecutionHandler { runnable, executor ->
            Log.w(TAG, "⚠️ 任务被拒绝: $name 线程池")
            poolStats[name]?.recordRejection()
            
            // 尝试在调用线程中执行
            try {
                runnable.run()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 拒绝任务执行失败: $name", e)
            }
        }
        
        val workQueue: BlockingQueue<Runnable> = when {
            queueCapacity <= 0 -> SynchronousQueue()
            queueCapacity == Int.MAX_VALUE -> LinkedBlockingQueue()
            else -> LinkedBlockingQueue(queueCapacity)
        }
        
        val executor = ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            rejectedExecutionHandler
        )
        
        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true)
        
        // 注册线程池
        threadPools[name] = executor
        poolStats[name] = ThreadPoolStats(name)
        
        Log.d(TAG, "🏗️ 创建线程池: $name, 核心: $corePoolSize, 最大: $maximumPoolSize")
        
        return executor
    }
    
    /**
     * 🔍 获取线程池
     */
    fun getThreadPool(name: String): ThreadPoolExecutor? {
        return threadPools[name]
    }
    
    /**
     * 📋 提交任务到指定线程池
     */
    fun submit(poolName: String, task: Runnable): Future<*>? {
        val pool = threadPools[poolName]
        return if (pool != null && !pool.isShutdown) {
            poolStats[poolName]?.recordSubmission()
            pool.submit(task)
        } else {
            Log.w(TAG, "⚠️ 线程池不存在或已关闭: $poolName")
            null
        }
    }
    
    /**
     * 📋 提交可调用任务到指定线程池
     */
    fun <T> submit(poolName: String, task: Callable<T>): Future<T>? {
        val pool = threadPools[poolName]
        return if (pool != null && !pool.isShutdown) {
            poolStats[poolName]?.recordSubmission()
            pool.submit(task)
        } else {
            Log.w(TAG, "⚠️ 线程池不存在或已关闭: $poolName")
            null
        }
    }
    
    /**
     * 📋 执行任务到指定线程池
     */
    fun execute(poolName: String, task: Runnable): Boolean {
        val pool = threadPools[poolName]
        return if (pool != null && !pool.isShutdown) {
            try {
                poolStats[poolName]?.recordSubmission()
                pool.execute(task)
                true
            } catch (e: RejectedExecutionException) {
                Log.w(TAG, "⚠️ 任务被拒绝: $poolName", e)
                false
            }
        } else {
            Log.w(TAG, "⚠️ 线程池不存在或已关闭: $poolName")
            false
        }
    }
    
    /**
     * 🔧 调整线程池大小
     */
    fun adjustPoolSize(poolName: String, corePoolSize: Int, maximumPoolSize: Int) {
        val pool = threadPools[poolName]
        if (pool != null) {
            pool.corePoolSize = corePoolSize
            pool.maximumPoolSize = maximumPoolSize
            Log.d(TAG, "🔧 调整线程池大小: $poolName, 核心: $corePoolSize, 最大: $maximumPoolSize")
        }
    }
    
    /**
     * 📊 获取线程池状态
     */
    fun getPoolStatus(poolName: String): Map<String, Any>? {
        val pool = threadPools[poolName] ?: return null
        val stats = poolStats[poolName]
        
        return mapOf(
            "name" to poolName,
            "core_pool_size" to pool.corePoolSize,
            "max_pool_size" to pool.maximumPoolSize,
            "current_pool_size" to pool.poolSize,
            "active_count" to pool.activeCount,
            "task_count" to pool.taskCount,
            "completed_task_count" to pool.completedTaskCount,
            "queue_size" to pool.queue.size,
            "queue_remaining_capacity" to pool.queue.remainingCapacity(),
            "is_shutdown" to pool.isShutdown,
            "is_terminated" to pool.isTerminated,
            "is_terminating" to pool.isTerminating,
            "largest_pool_size" to pool.largestPoolSize,
            "keep_alive_time" to pool.getKeepAliveTime(TimeUnit.SECONDS),
            "submissions" to (stats?.submissions?.get() ?: 0L),
            "rejections" to (stats?.rejections?.get() ?: 0L),
            "utilization" to calculateUtilization(pool)
        )
    }
    
    /**
     * 📊 获取所有线程池状态
     */
    fun getAllPoolStatus(): Map<String, Map<String, Any>> {
        return threadPools.keys.associateWith { poolName ->
            getPoolStatus(poolName) ?: emptyMap()
        }
    }
    
    /**
     * 📈 计算线程池利用率
     */
    private fun calculateUtilization(pool: ThreadPoolExecutor): Double {
        val maxPoolSize = pool.maximumPoolSize
        val activeCount = pool.activeCount
        return if (maxPoolSize > 0) activeCount.toDouble() / maxPoolSize else 0.0
    }
    
    /**
     * 🔍 监控线程池健康状态
     */
    fun monitorPoolHealth(): Map<String, String> {
        val healthStatus = mutableMapOf<String, String>()
        
        threadPools.forEach { (name, pool) ->
            val status = when {
                pool.isTerminated -> "TERMINATED"
                pool.isShutdown -> "SHUTDOWN"
                pool.isTerminating -> "TERMINATING"
                pool.activeCount >= pool.maximumPoolSize -> "OVERLOADED"
                pool.queue.size >= pool.queue.remainingCapacity() -> "QUEUE_FULL"
                pool.activeCount == 0 && pool.queue.isEmpty() -> "IDLE"
                else -> "HEALTHY"
            }
            healthStatus[name] = status
        }
        
        return healthStatus
    }
    
    /**
     * 🚨 检测线程池问题
     */
    fun detectPoolIssues(): List<String> {
        val issues = mutableListOf<String>()
        
        threadPools.forEach { (name, pool) ->
            // 检测队列积压
            if (pool.queue.size > QUEUE_CAPACITY * 0.8) {
                issues.add("线程池 $name 队列积压严重 (${pool.queue.size}/${QUEUE_CAPACITY})")
            }
            
            // 检测线程池过载
            if (pool.activeCount >= pool.maximumPoolSize) {
                issues.add("线程池 $name 已达到最大容量 (${pool.activeCount}/${pool.maximumPoolSize})")
            }
            
            // 检测拒绝率
            val stats = poolStats[name]
            if (stats != null) {
                val rejectionRate = stats.getRejectionRate()
                if (rejectionRate > 0.1) {
                    issues.add("线程池 $name 拒绝率过高 (${String.format("%.1f%%", rejectionRate * 100)})")
                }
            }
            
            // 检测长时间运行的任务
            if (pool.activeCount > 0 && pool.completedTaskCount == 0L) {
                issues.add("线程池 $name 可能存在长时间运行的任务")
            }
        }
        
        return issues
    }
    
    /**
     * 🔧 自动优化线程池
     */
    fun autoOptimize() {
        Log.d(TAG, "🔧 开始自动优化线程池...")
        
        threadPools.forEach { (name, pool) ->
            val stats = poolStats[name] ?: return@forEach
            
            // 基于历史数据调整线程池大小
            val avgActiveCount = pool.completedTaskCount.toDouble() / maxOf(1, pool.taskCount)
            val queueUtilization = pool.queue.size.toDouble() / QUEUE_CAPACITY
            
            when {
                // 如果队列经常满，增加线程数
                queueUtilization > 0.8 && pool.maximumPoolSize < CPU_COUNT * 4 -> {
                    val newMaxSize = minOf(pool.maximumPoolSize + 1, CPU_COUNT * 4)
                    pool.maximumPoolSize = newMaxSize
                    Log.d(TAG, "🔧 增加线程池 $name 最大大小到 $newMaxSize")
                }
                
                // 如果线程利用率低，减少核心线程数
                avgActiveCount < pool.corePoolSize * 0.5 && pool.corePoolSize > 1 -> {
                    val newCoreSize = maxOf(pool.corePoolSize - 1, 1)
                    pool.corePoolSize = newCoreSize
                    Log.d(TAG, "🔧 减少线程池 $name 核心大小到 $newCoreSize")
                }
            }
        }
        
        Log.d(TAG, "✅ 线程池自动优化完成")
    }
    
    /**
     * 🧹 清理空闲线程池
     */
    fun cleanupIdlePools() {
        val idlePools = threadPools.filter { (_, pool) ->
            pool.activeCount == 0 && 
            pool.queue.isEmpty() && 
            pool.completedTaskCount > 0 &&
            System.currentTimeMillis() - (poolStats[pool.toString()]?.lastActivityTime ?: 0L) > 300000L // 5分钟
        }
        
        idlePools.forEach { (name, pool) ->
            if (name !in listOf("default", "io", "compute", "network")) { // 保留核心线程池
                pool.purge() // 清理已取消的任务
                Log.d(TAG, "🧹 清理空闲线程池: $name")
            }
        }
    }
    
    /**
     * 🛑 关闭指定线程池
     */
    fun shutdownPool(poolName: String, timeoutSeconds: Long = 5) {
        val pool = threadPools.remove(poolName)
        poolStats.remove(poolName)
        
        if (pool != null) {
            try {
                pool.shutdown()
                if (!pool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    Log.w(TAG, "⚠️ 线程池 $poolName 未能在 ${timeoutSeconds}s 内关闭，强制关闭")
                    pool.shutdownNow()
                }
                Log.d(TAG, "✅ 线程池 $poolName 已关闭")
            } catch (e: InterruptedException) {
                Log.e(TAG, "❌ 关闭线程池 $poolName 被中断", e)
                pool.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }
    
    /**
     * 🛑 关闭所有线程池
     */
    fun shutdownAll(timeoutSeconds: Long = 10) {
        Log.d(TAG, "🛑 关闭所有线程池...")
        
        val poolNames = threadPools.keys.toList()
        poolNames.forEach { poolName ->
            shutdownPool(poolName, timeoutSeconds)
        }
        
        Log.d(TAG, "✅ 所有线程池已关闭")
    }
}

/**
 * 线程池统计
 */
class ThreadPoolStats(val name: String) {
    val submissions = AtomicLong(0)
    val rejections = AtomicLong(0)
    @Volatile var lastActivityTime = System.currentTimeMillis()
    
    fun recordSubmission() {
        submissions.incrementAndGet()
        lastActivityTime = System.currentTimeMillis()
    }
    
    fun recordRejection() {
        rejections.incrementAndGet()
        lastActivityTime = System.currentTimeMillis()
    }
    
    fun getRejectionRate(): Double {
        val total = submissions.get() + rejections.get()
        return if (total > 0) rejections.get().toDouble() / total else 0.0
    }
    
    fun getStats(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "submissions" to submissions.get(),
            "rejections" to rejections.get(),
            "rejection_rate" to getRejectionRate(),
            "last_activity_time" to lastActivityTime
        )
    }
}

/**
 * 线程池配置
 */
data class ThreadPoolConfig(
    val name: String,
    val corePoolSize: Int = ThreadPoolManager.CORE_POOL_SIZE,
    val maximumPoolSize: Int = ThreadPoolManager.MAX_POOL_SIZE,
    val keepAliveTime: Long = ThreadPoolManager.KEEP_ALIVE_TIME,
    val queueCapacity: Int = ThreadPoolManager.QUEUE_CAPACITY,
    val allowCoreThreadTimeOut: Boolean = true,
    val preStartAllCoreThreads: Boolean = false
) {
    
    fun isValid(): Boolean {
        return corePoolSize > 0 && 
               maximumPoolSize >= corePoolSize && 
               keepAliveTime >= 0 && 
               queueCapacity >= 0
    }
}

/**
 * 线程池工厂
 */
object ThreadPoolFactory {
    
    /**
     * 🏭 创建 IO 密集型线程池
     */
    fun createIOPool(name: String = "io"): ThreadPoolExecutor {
        return ThreadPoolManager.createThreadPool(
            name = name,
            corePoolSize = ThreadPoolManager.CPU_COUNT * 2,
            maximumPoolSize = ThreadPoolManager.CPU_COUNT * 4,
            queueCapacity = 256
        )
    }
    
    /**
     * 🏭 创建计算密集型线程池
     */
    fun createComputePool(name: String = "compute"): ThreadPoolExecutor {
        return ThreadPoolManager.createThreadPool(
            name = name,
            corePoolSize = ThreadPoolManager.CPU_COUNT,
            maximumPoolSize = ThreadPoolManager.CPU_COUNT,
            queueCapacity = 64
        )
    }
    
    /**
     * 🏭 创建网络请求线程池
     */
    fun createNetworkPool(name: String = "network"): ThreadPoolExecutor {
        return ThreadPoolManager.createThreadPool(
            name = name,
            corePoolSize = 4,
            maximumPoolSize = 8,
            queueCapacity = 128
        )
    }
    
    /**
     * 🏭 创建单线程池
     */
    fun createSingleThreadPool(name: String = "single"): ThreadPoolExecutor {
        return ThreadPoolManager.createThreadPool(
            name = name,
            corePoolSize = 1,
            maximumPoolSize = 1,
            queueCapacity = Int.MAX_VALUE
        )
    }
    
    /**
     * 🏭 创建缓存线程池
     */
    fun createCachedPool(name: String = "cached"): ThreadPoolExecutor {
        return ThreadPoolManager.createThreadPool(
            name = name,
            corePoolSize = 0,
            maximumPoolSize = Int.MAX_VALUE,
            keepAliveTime = 60L,
            queueCapacity = 0 // SynchronousQueue
        )
    }
    
    /**
     * 🏭 根据配置创建线程池
     */
    fun createFromConfig(config: ThreadPoolConfig): ThreadPoolExecutor {
        require(config.isValid()) { "无效的线程池配置: $config" }
        
        return ThreadPoolManager.createThreadPool(
            name = config.name,
            corePoolSize = config.corePoolSize,
            maximumPoolSize = config.maximumPoolSize,
            keepAliveTime = config.keepAliveTime,
            queueCapacity = config.queueCapacity
        ).apply {
            allowCoreThreadTimeOut(config.allowCoreThreadTimeOut)
            if (config.preStartAllCoreThreads) {
                prestartAllCoreThreads()
            }
        }
    }
}
