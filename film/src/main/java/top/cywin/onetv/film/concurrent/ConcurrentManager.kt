package top.cywin.onetv.film.concurrent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 并发管理器
 * 
 * 基于 FongMi/TV 的并发处理实现
 * 提供线程池管理、协程调度、任务队列等功能
 * 
 * 功能：
 * - 线程池管理
 * - 协程作用域管理
 * - 任务队列和调度
 * - 并发限制和控制
 * - 性能监控
 * - 错误处理和恢复
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ConcurrentManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_FILM_CONCURRENT_MANAGER"
        private const val DEFAULT_CORE_POOL_SIZE = 4
        private const val DEFAULT_MAX_POOL_SIZE = 8
        private const val DEFAULT_KEEP_ALIVE_TIME = 60L
        private const val DEFAULT_QUEUE_CAPACITY = 100
    }
    
    // 线程池配置
    private val corePoolSize = maxOf(2, Runtime.getRuntime().availableProcessors())
    private val maxPoolSize = corePoolSize * 2
    private val keepAliveTime = DEFAULT_KEEP_ALIVE_TIME
    private val queueCapacity = DEFAULT_QUEUE_CAPACITY
    
    // 主线程池
    private val mainExecutor: ThreadPoolExecutor by lazy {
        createThreadPoolExecutor("main", corePoolSize, maxPoolSize)
    }
    
    // IO 线程池
    private val ioExecutor: ThreadPoolExecutor by lazy {
        createThreadPoolExecutor("io", corePoolSize * 2, maxPoolSize * 2)
    }
    
    // 计算线程池
    private val computeExecutor: ThreadPoolExecutor by lazy {
        createThreadPoolExecutor("compute", corePoolSize, corePoolSize)
    }
    
    // 协程作用域
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 任务队列
    private val taskQueue = Channel<ConcurrentTask>(Channel.UNLIMITED)
    
    // 并发统计
    private val concurrentStats = ConcurrentStats()
    
    // 事件流
    private val _events = MutableSharedFlow<ConcurrentEvent>()
    val events: SharedFlow<ConcurrentEvent> = _events.asSharedFlow()
    
    // 任务处理器
    private var taskProcessor: Job? = null
    
    init {
        startTaskProcessor()
        Log.d(TAG, "🏗️ 并发管理器初始化完成")
    }
    
    /**
     * 🏗️ 创建线程池
     */
    private fun createThreadPoolExecutor(name: String, coreSize: Int, maxSize: Int): ThreadPoolExecutor {
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, "OneTV-$name-${Thread.currentThread().id}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }
        
        val rejectedExecutionHandler = RejectedExecutionHandler { runnable, executor ->
            Log.w(TAG, "⚠️ 任务被拒绝执行: $name 线程池")
            concurrentStats.recordRejection(name)
            
            // 尝试在调用线程中执行
            try {
                runnable.run()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 任务执行失败: $name", e)
            }
        }
        
        return ThreadPoolExecutor(
            coreSize,
            maxSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(queueCapacity),
            threadFactory,
            rejectedExecutionHandler
        )
    }
    
    /**
     * 🚀 启动任务处理器
     */
    private fun startTaskProcessor() {
        taskProcessor = ioScope.launch {
            while (isActive) {
                try {
                    val task = taskQueue.receive()
                    processTask(task)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 任务处理器异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 任务处理器已启动")
    }
    
    /**
     * 🔄 处理任务
     */
    private suspend fun processTask(task: ConcurrentTask) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "🔄 开始处理任务: ${task.id}")
            
            _events.emit(ConcurrentEvent.TaskStarted(task.id, task.type))
            concurrentStats.recordTaskStart(task.type)
            
            // 根据任务类型选择执行器
            val result = when (task.type) {
                TaskType.IO -> withContext(Dispatchers.IO) { task.execute() }
                TaskType.COMPUTE -> withContext(Dispatchers.Default) { task.execute() }
                TaskType.MAIN -> withContext(Dispatchers.Main) { task.execute() }
                TaskType.NETWORK -> withContext(Dispatchers.IO) { task.execute() }
                TaskType.DATABASE -> withContext(Dispatchers.IO) { task.execute() }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            concurrentStats.recordTaskComplete(task.type, duration)
            _events.emit(ConcurrentEvent.TaskCompleted(task.id, task.type, duration))
            
            Log.d(TAG, "✅ 任务完成: ${task.id}, 耗时: ${duration}ms")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            
            concurrentStats.recordTaskError(task.type, e)
            _events.emit(ConcurrentEvent.TaskFailed(task.id, task.type, e.message ?: "Unknown error"))
            
            Log.e(TAG, "❌ 任务失败: ${task.id}", e)
        }
    }
    
    /**
     * 📋 提交任务
     */
    suspend fun submitTask(task: ConcurrentTask): Boolean {
        return try {
            taskQueue.send(task)
            Log.d(TAG, "📋 任务已提交: ${task.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 任务提交失败: ${task.id}", e)
            false
        }
    }
    
    /**
     * 📋 提交 IO 任务
     */
    suspend fun submitIOTask(id: String, action: suspend () -> Any?): Boolean {
        val task = ConcurrentTask(id, TaskType.IO, action)
        return submitTask(task)
    }
    
    /**
     * 📋 提交计算任务
     */
    suspend fun submitComputeTask(id: String, action: suspend () -> Any?): Boolean {
        val task = ConcurrentTask(id, TaskType.COMPUTE, action)
        return submitTask(task)
    }
    
    /**
     * 📋 提交网络任务
     */
    suspend fun submitNetworkTask(id: String, action: suspend () -> Any?): Boolean {
        val task = ConcurrentTask(id, TaskType.NETWORK, action)
        return submitTask(task)
    }
    
    /**
     * 📋 提交数据库任务
     */
    suspend fun submitDatabaseTask(id: String, action: suspend () -> Any?): Boolean {
        val task = ConcurrentTask(id, TaskType.DATABASE, action)
        return submitTask(task)
    }
    
    /**
     * 🔄 并行执行多个任务
     */
    suspend fun <T> executeParallel(
        tasks: List<suspend () -> T>,
        maxConcurrency: Int = corePoolSize
    ): List<T> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        
        tasks.map { task ->
            async {
                semaphore.acquire()
                try {
                    task()
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
    }
    
    /**
     * 🔄 并行执行并收集结果
     */
    suspend fun <T, R> executeParallelAndCollect(
        items: List<T>,
        maxConcurrency: Int = corePoolSize,
        transform: suspend (T) -> R
    ): List<R> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        
        items.map { item ->
            async {
                semaphore.acquire()
                try {
                    transform(item)
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
    }
    
    /**
     * ⏱️ 带超时的任务执行
     */
    suspend fun <T> executeWithTimeout(
        timeoutMillis: Long,
        action: suspend () -> T
    ): T? = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMillis) {
                action()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "⏱️ 任务执行超时: ${timeoutMillis}ms")
            null
        }
    }
    
    /**
     * 🔄 重试执行任务
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        delayMillis: Long = 1000L,
        action: suspend () -> T
    ): T? {
        repeat(maxRetries) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                Log.w(TAG, "🔄 任务执行失败，尝试重试 (${attempt + 1}/$maxRetries)", e)
                if (attempt < maxRetries - 1) {
                    delay(delayMillis * (attempt + 1))
                }
            }
        }
        return null
    }
    
    /**
     * 📊 获取线程池状态
     */
    fun getThreadPoolStatus(): Map<String, Any> {
        return mapOf(
            "main_pool" to getExecutorStatus("main", mainExecutor),
            "io_pool" to getExecutorStatus("io", ioExecutor),
            "compute_pool" to getExecutorStatus("compute", computeExecutor)
        )
    }
    
    /**
     * 📊 获取执行器状态
     */
    private fun getExecutorStatus(name: String, executor: ThreadPoolExecutor): Map<String, Any> {
        return mapOf(
            "name" to name,
            "core_pool_size" to executor.corePoolSize,
            "max_pool_size" to executor.maximumPoolSize,
            "active_count" to executor.activeCount,
            "pool_size" to executor.poolSize,
            "task_count" to executor.taskCount,
            "completed_task_count" to executor.completedTaskCount,
            "queue_size" to executor.queue.size,
            "is_shutdown" to executor.isShutdown,
            "is_terminated" to executor.isTerminated
        )
    }
    
    /**
     * 📊 获取并发统计
     */
    fun getConcurrentStats(): Map<String, Any> {
        return concurrentStats.getStats()
    }
    
    /**
     * 🧹 清理统计
     */
    fun clearStats() {
        concurrentStats.clear()
    }
    
    /**
     * 🛑 关闭并发管理器
     */
    fun shutdown() {
        try {
            Log.d(TAG, "🛑 关闭并发管理器...")
            
            // 停止任务处理器
            taskProcessor?.cancel()
            
            // 关闭协程作用域
            mainScope.cancel()
            ioScope.cancel()
            defaultScope.cancel()
            
            // 关闭线程池
            shutdownExecutor("main", mainExecutor)
            shutdownExecutor("io", ioExecutor)
            shutdownExecutor("compute", computeExecutor)
            
            Log.d(TAG, "✅ 并发管理器关闭完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 并发管理器关闭失败", e)
        }
    }
    
    /**
     * 🛑 关闭执行器
     */
    private fun shutdownExecutor(name: String, executor: ThreadPoolExecutor) {
        try {
            executor.shutdown()
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                Log.w(TAG, "⚠️ $name 线程池未能在 5 秒内关闭，强制关闭")
                executor.shutdownNow()
            }
            Log.d(TAG, "✅ $name 线程池已关闭")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 关闭 $name 线程池失败", e)
        }
    }
}

/**
 * 并发任务
 */
data class ConcurrentTask(
    val id: String,
    val type: TaskType,
    val action: suspend () -> Any?
) {
    suspend fun execute(): Any? = action()
}

/**
 * 任务类型
 */
enum class TaskType {
    IO,         // IO 密集型任务
    COMPUTE,    // 计算密集型任务
    MAIN,       // 主线程任务
    NETWORK,    // 网络任务
    DATABASE    // 数据库任务
}

/**
 * 并发事件
 */
sealed class ConcurrentEvent {
    data class TaskStarted(val taskId: String, val type: TaskType) : ConcurrentEvent()
    data class TaskCompleted(val taskId: String, val type: TaskType, val duration: Long) : ConcurrentEvent()
    data class TaskFailed(val taskId: String, val type: TaskType, val error: String) : ConcurrentEvent()
    data class PoolOverloaded(val poolName: String, val queueSize: Int) : ConcurrentEvent()
}

/**
 * 并发统计
 */
class ConcurrentStats {
    
    private val taskCounts = mutableMapOf<TaskType, AtomicLong>()
    private val completedCounts = mutableMapOf<TaskType, AtomicLong>()
    private val errorCounts = mutableMapOf<TaskType, AtomicLong>()
    private val totalDurations = mutableMapOf<TaskType, AtomicLong>()
    private val rejectionCounts = mutableMapOf<String, AtomicLong>()
    
    init {
        TaskType.values().forEach { type ->
            taskCounts[type] = AtomicLong(0)
            completedCounts[type] = AtomicLong(0)
            errorCounts[type] = AtomicLong(0)
            totalDurations[type] = AtomicLong(0)
        }
    }
    
    fun recordTaskStart(type: TaskType) {
        taskCounts[type]?.incrementAndGet()
    }
    
    fun recordTaskComplete(type: TaskType, duration: Long) {
        completedCounts[type]?.incrementAndGet()
        totalDurations[type]?.addAndGet(duration)
    }
    
    fun recordTaskError(type: TaskType, error: Exception) {
        errorCounts[type]?.incrementAndGet()
    }
    
    fun recordRejection(poolName: String) {
        rejectionCounts.computeIfAbsent(poolName) { AtomicLong(0) }.incrementAndGet()
    }
    
    fun getStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        TaskType.values().forEach { type ->
            val taskCount = taskCounts[type]?.get() ?: 0L
            val completedCount = completedCounts[type]?.get() ?: 0L
            val errorCount = errorCounts[type]?.get() ?: 0L
            val totalDuration = totalDurations[type]?.get() ?: 0L
            
            stats["${type.name.lowercase()}_tasks"] = taskCount
            stats["${type.name.lowercase()}_completed"] = completedCount
            stats["${type.name.lowercase()}_errors"] = errorCount
            stats["${type.name.lowercase()}_avg_duration"] = if (completedCount > 0) totalDuration / completedCount else 0L
            stats["${type.name.lowercase()}_success_rate"] = if (taskCount > 0) completedCount.toDouble() / taskCount else 0.0
        }
        
        stats["rejections"] = rejectionCounts.mapValues { it.value.get() }
        
        return stats
    }
    
    fun clear() {
        taskCounts.values.forEach { it.set(0) }
        completedCounts.values.forEach { it.set(0) }
        errorCounts.values.forEach { it.set(0) }
        totalDurations.values.forEach { it.set(0) }
        rejectionCounts.values.forEach { it.set(0) }
    }
}
