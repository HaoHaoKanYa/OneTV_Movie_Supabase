package top.cywin.onetv.film.concurrent

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 并发工具类
 * 
 * 基于 FongMi/TV 的并发处理工具实现
 * 提供协程、线程池、并发控制等工具方法
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object ConcurrentUtils {
    
    private const val TAG = "ONETV_FILM_CONCURRENT_UTILS"
    
    /**
     * 🔄 并行执行任务并收集结果
     */
    suspend fun <T, R> parallelMap(
        items: List<T>,
        maxConcurrency: Int = 4,
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
     * 🔄 并行执行任务并过滤结果
     */
    suspend fun <T, R> parallelMapNotNull(
        items: List<T>,
        maxConcurrency: Int = 4,
        transform: suspend (T) -> R?
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
        }.awaitAll().filterNotNull()
    }
    
    /**
     * 🔄 并行执行任务并收集成功结果
     */
    suspend fun <T, R> parallelMapCatching(
        items: List<T>,
        maxConcurrency: Int = 4,
        transform: suspend (T) -> R
    ): List<Result<R>> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        
        items.map { item ->
            async {
                semaphore.acquire()
                try {
                    runCatching { transform(item) }
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
    }
    
    /**
     * ⏱️ 带超时的并行执行
     */
    suspend fun <T, R> parallelMapWithTimeout(
        items: List<T>,
        maxConcurrency: Int = 4,
        timeoutMillis: Long = 30000L,
        transform: suspend (T) -> R
    ): List<R?> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        
        items.map { item ->
            async {
                semaphore.acquire()
                try {
                    withTimeoutOrNull(timeoutMillis) {
                        transform(item)
                    }
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll()
    }
    
    /**
     * 🔄 批量处理
     */
    suspend fun <T, R> batchProcess(
        items: List<T>,
        batchSize: Int = 10,
        maxConcurrency: Int = 4,
        processor: suspend (List<T>) -> List<R>
    ): List<R> = withContext(Dispatchers.IO) {
        val batches = items.chunked(batchSize)
        val semaphore = Semaphore(maxConcurrency)
        
        batches.map { batch ->
            async {
                semaphore.acquire()
                try {
                    processor(batch)
                } finally {
                    semaphore.release()
                }
            }
        }.awaitAll().flatten()
    }
    
    /**
     * 🔄 重试执行
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMillis: Long = 1000L,
        maxDelayMillis: Long = 10000L,
        factor: Double = 2.0,
        action: suspend () -> T
    ): T? {
        var currentDelay = initialDelayMillis
        
        repeat(maxRetries) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                Log.w(TAG, "🔄 重试执行失败 (${attempt + 1}/$maxRetries): ${e.message}")
                
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
                }
            }
        }
        
        return null
    }
    
    /**
     * 🔄 条件重试
     */
    suspend fun <T> retryWhen(
        maxRetries: Int = 3,
        delayMillis: Long = 1000L,
        predicate: (Exception) -> Boolean,
        action: suspend () -> T
    ): T? {
        repeat(maxRetries) { attempt ->
            try {
                return action()
            } catch (e: Exception) {
                if (predicate(e) && attempt < maxRetries - 1) {
                    Log.w(TAG, "🔄 条件重试 (${attempt + 1}/$maxRetries): ${e.message}")
                    delay(delayMillis)
                } else {
                    throw e
                }
            }
        }
        
        return null
    }
    
    /**
     * ⏱️ 带超时的执行
     */
    suspend fun <T> withTimeoutOrDefault(
        timeoutMillis: Long,
        defaultValue: T,
        action: suspend () -> T
    ): T {
        return try {
            withTimeout(timeoutMillis) {
                action()
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "⏱️ 执行超时，返回默认值: ${timeoutMillis}ms")
            defaultValue
        }
    }
    
    /**
     * 🔀 竞争执行（返回最快的结果）
     */
    suspend fun <T> raceExecution(
        vararg actions: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        select<T> {
            actions.forEach { action ->
                async { action() }.onAwait { it }
            }
        }
    }
    
    /**
     * 🔀 竞争执行（返回所有结果）
     */
    suspend fun <T> raceExecutionAll(
        timeoutMillis: Long = 30000L,
        vararg actions: suspend () -> T
    ): List<T> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMillis) {
            actions.map { action ->
                async { action() }
            }.awaitAll()
        } ?: emptyList()
    }
    
    /**
     * 📊 并发计数器
     */
    class ConcurrentCounter {
        private val count = AtomicLong(0)
        
        fun increment(): Long = count.incrementAndGet()
        fun decrement(): Long = count.decrementAndGet()
        fun get(): Long = count.get()
        fun set(value: Long) = count.set(value)
        fun reset() = count.set(0)
    }
    
    /**
     * 🚦 并发限制器
     */
    class ConcurrentLimiter(private val maxConcurrency: Int) {
        private val semaphore = Semaphore(maxConcurrency)
        private val activeCount = AtomicInteger(0)
        
        suspend fun <T> execute(action: suspend () -> T): T {
            semaphore.acquire()
            activeCount.incrementAndGet()
            
            return try {
                action()
            } finally {
                activeCount.decrementAndGet()
                semaphore.release()
            }
        }
        
        fun getActiveCount(): Int = activeCount.get()
        fun getAvailablePermits(): Int = semaphore.availablePermits()
    }
    
    /**
     * 📦 批处理器
     */
    class BatchProcessor<T, R>(
        private val batchSize: Int = 10,
        private val maxWaitTime: Long = 1000L,
        private val processor: suspend (List<T>) -> List<R>
    ) {
        private val buffer = mutableListOf<T>()
        private val results = Channel<R>(Channel.UNLIMITED)
        private var lastProcessTime = System.currentTimeMillis()
        
        suspend fun add(item: T) {
            synchronized(buffer) {
                buffer.add(item)
                
                if (buffer.size >= batchSize || 
                    (System.currentTimeMillis() - lastProcessTime) >= maxWaitTime) {
                    processBatch()
                }
            }
        }
        
        private suspend fun processBatch() {
            if (buffer.isEmpty()) return
            
            val batch = buffer.toList()
            buffer.clear()
            lastProcessTime = System.currentTimeMillis()
            
            try {
                val batchResults = processor(batch)
                batchResults.forEach { result ->
                    results.send(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 批处理失败", e)
            }
        }
        
        fun getResults(): ReceiveChannel<R> = results
    }
    
    /**
     * 🔄 流式处理工具
     */
    object FlowUtils {
        
        /**
         * 🔄 并行处理流
         */
        fun <T, R> Flow<T>.parallelMap(
            concurrency: Int = 4,
            transform: suspend (T) -> R
        ): Flow<R> = flatMapMerge(concurrency) { value ->
            flow { emit(transform(value)) }
        }
        
        /**
         * 📦 批量处理流
         */
        fun <T> Flow<T>.batch(
            size: Int,
            timeoutMillis: Long = 1000L
        ): Flow<List<T>> = flow {
            val batch = mutableListOf<T>()
            var lastEmitTime = System.currentTimeMillis()
            
            collect { value ->
                batch.add(value)
                
                if (batch.size >= size || 
                    (System.currentTimeMillis() - lastEmitTime) >= timeoutMillis) {
                    emit(batch.toList())
                    batch.clear()
                    lastEmitTime = System.currentTimeMillis()
                }
            }
            
            if (batch.isNotEmpty()) {
                emit(batch.toList())
            }
        }
        
        /**
         * 🔄 重试流
         */
        fun <T> Flow<T>.retryWithDelay(
            retries: Int = 3,
            delayMillis: Long = 1000L
        ): Flow<T> = retryWhen { cause, attempt ->
            if (attempt < retries) {
                delay(delayMillis * attempt)
                true
            } else {
                false
            }
        }
        
        /**
         * ⏱️ 超时处理流
         */
        fun <T> Flow<T>.timeoutWithDefault(
            timeoutMillis: Long,
            defaultValue: T
        ): Flow<T> = flow {
            try {
                withTimeout(timeoutMillis) {
                    collect { emit(it) }
                }
            } catch (e: TimeoutCancellationException) {
                emit(defaultValue)
            }
        }
        
        /**
         * 🚦 限流
         */
        fun <T> Flow<T>.throttle(
            intervalMillis: Long
        ): Flow<T> = flow {
            var lastEmitTime = 0L
            
            collect { value ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastEmitTime >= intervalMillis) {
                    emit(value)
                    lastEmitTime = currentTime
                }
            }
        }
        
        /**
         * 🔄 去重
         */
        fun <T> Flow<T>.distinctUntilChanged(
            keySelector: (T) -> Any = { it }
        ): Flow<T> = flow {
            var lastKey: Any? = null
            
            collect { value ->
                val key = keySelector(value)
                if (key != lastKey) {
                    emit(value)
                    lastKey = key
                }
            }
        }
    }
    
    /**
     * 🔧 协程作用域工具
     */
    object ScopeUtils {
        
        /**
         * 🏗️ 创建 IO 作用域
         */
        fun createIOScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        
        /**
         * 🏗️ 创建主线程作用域
         */
        fun createMainScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Main + SupervisorJob())
        }
        
        /**
         * 🏗️ 创建默认作用域
         */
        fun createDefaultScope(): CoroutineScope {
            return CoroutineScope(Dispatchers.Default + SupervisorJob())
        }
        
        /**
         * 🏗️ 创建带名称的作用域
         */
        fun createNamedScope(name: String, dispatcher: CoroutineDispatcher = Dispatchers.IO): CoroutineScope {
            return CoroutineScope(dispatcher + SupervisorJob() + CoroutineName(name))
        }
    }
    
    /**
     * 📊 性能监控工具
     */
    object PerformanceUtils {
        
        /**
         * ⏱️ 测量执行时间
         */
        suspend fun <T> measureTime(
            tag: String = "Performance",
            action: suspend () -> T
        ): Pair<T, Long> {
            val startTime = System.currentTimeMillis()
            val result = action()
            val duration = System.currentTimeMillis() - startTime
            
            Log.d(TAG, "⏱️ $tag 执行时间: ${duration}ms")
            
            return Pair(result, duration)
        }
        
        /**
         * 📊 监控协程性能
         */
        suspend fun <T> monitorCoroutine(
            name: String,
            action: suspend () -> T
        ): T = withContext(CoroutineName(name)) {
            val startTime = System.currentTimeMillis()
            
            try {
                val result = action()
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "📊 协程 $name 完成，耗时: ${duration}ms")
                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "📊 协程 $name 失败，耗时: ${duration}ms", e)
                throw e
            }
        }
    }
}
