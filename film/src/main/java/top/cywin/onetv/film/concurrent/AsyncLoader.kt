package top.cywin.onetv.film.concurrent

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import top.cywin.onetv.film.spider.base.Spider
import top.cywin.onetv.film.spider.base.SpiderFactory
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.cache.ContentCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 异步加载器
 * 基于 FongMi/TV 标准实现
 * 
 * 提供高性能的异步内容加载功能
 * 
 * 功能：
 * - 异步内容加载
 * - 批量加载优化
 * - 加载队列管理
 * - 优先级控制
 * - 错误处理和重试
 * - 加载进度监控
 * - 智能缓存策略
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class AsyncLoader(
    private val context: Context,
    private val maxConcurrency: Int = 8,
    private val loadTimeout: Long = 30000L,
    private val retryCount: Int = 3
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_ASYNC_LOADER"
    }
    
    // 协程作用域
    private val scope = CoroutineScope(
        Dispatchers.IO + 
        SupervisorJob() + 
        CoroutineName("AsyncLoader")
    )
    
    // 加载队列
    private val loadQueue = Channel<LoadTask>(Channel.UNLIMITED)
    private val priorityQueue = Channel<LoadTask>(Channel.UNLIMITED)
    
    // 加载统计
    private val totalLoadCount = AtomicLong(0)
    private val successLoadCount = AtomicLong(0)
    private val failedLoadCount = AtomicLong(0)
    private val activeLoadCount = AtomicInteger(0)
    
    // 加载任务缓存
    private val loadTaskCache = ConcurrentHashMap<String, LoadResult>()
    
    init {
        startLoadWorkers()
    }
    
    /**
     * 🏠 异步加载首页内容
     */
    fun loadHomeContentAsync(
        site: VodSite,
        filter: Boolean = false,
        priority: LoadPriority = LoadPriority.NORMAL,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onProgress: (LoadProgress) -> Unit = {}
    ): LoadTask {
        
        val task = LoadTask(
            id = generateTaskId("home", site.key),
            type = LoadType.HOME,
            site = site,
            priority = priority,
            filter = filter,
            onSuccess = onSuccess,
            onError = onError,
            onProgress = onProgress,
            createTime = System.currentTimeMillis()
        )
        
        submitTask(task)
        return task
    }
    
    /**
     * 📂 异步加载分类内容
     */
    fun loadCategoryContentAsync(
        site: VodSite,
        tid: String,
        page: String = "1",
        filter: Boolean = false,
        extend: HashMap<String, String> = hashMapOf(),
        priority: LoadPriority = LoadPriority.NORMAL,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onProgress: (LoadProgress) -> Unit = {}
    ): LoadTask {
        
        val task = LoadTask(
            id = generateTaskId("category", site.key, tid, page),
            type = LoadType.CATEGORY,
            site = site,
            priority = priority,
            tid = tid,
            page = page,
            filter = filter,
            extend = extend,
            onSuccess = onSuccess,
            onError = onError,
            onProgress = onProgress,
            createTime = System.currentTimeMillis()
        )
        
        submitTask(task)
        return task
    }
    
    /**
     * 📄 异步加载详情内容
     */
    fun loadDetailContentAsync(
        site: VodSite,
        vodIds: List<String>,
        priority: LoadPriority = LoadPriority.HIGH,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onProgress: (LoadProgress) -> Unit = {}
    ): LoadTask {
        
        val task = LoadTask(
            id = generateTaskId("detail", site.key, vodIds.joinToString(",")),
            type = LoadType.DETAIL,
            site = site,
            priority = priority,
            vodIds = vodIds,
            onSuccess = onSuccess,
            onError = onError,
            onProgress = onProgress,
            createTime = System.currentTimeMillis()
        )
        
        submitTask(task)
        return task
    }
    
    /**
     * ▶️ 异步加载播放内容
     */
    fun loadPlayerContentAsync(
        site: VodSite,
        flag: String,
        id: String,
        vipFlags: List<String> = emptyList(),
        priority: LoadPriority = LoadPriority.URGENT,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onProgress: (LoadProgress) -> Unit = {}
    ): LoadTask {
        
        val task = LoadTask(
            id = generateTaskId("player", site.key, flag, id),
            type = LoadType.PLAYER,
            site = site,
            priority = priority,
            flag = flag,
            playId = id,
            vipFlags = vipFlags,
            onSuccess = onSuccess,
            onError = onError,
            onProgress = onProgress,
            createTime = System.currentTimeMillis()
        )
        
        submitTask(task)
        return task
    }
    
    /**
     * 📤 提交加载任务
     */
    private fun submitTask(task: LoadTask) {
        scope.launch {
            try {
                // 检查缓存
                val cachedResult = getCachedResult(task)
                if (cachedResult != null) {
                    Log.d(TAG, "📦 加载任务缓存命中: ${task.id}")
                    task.onSuccess(cachedResult.content)
                    return@launch
                }
                
                // 根据优先级选择队列
                val queue = if (task.priority == LoadPriority.URGENT) {
                    priorityQueue
                } else {
                    loadQueue
                }
                
                queue.send(task)
                Log.d(TAG, "📤 加载任务已提交: ${task.id} (${task.priority.name})")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 提交加载任务失败: ${task.id}", e)
                task.onError(e)
            }
        }
    }
    
    /**
     * 🏭 启动加载工作器
     */
    private fun startLoadWorkers() {
        // 高优先级工作器
        repeat(maxConcurrency / 2) { workerId ->
            scope.launch {
                while (true) {
                    try {
                        val task = priorityQueue.receive()
                        processLoadTask(task, "Priority-$workerId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 高优先级工作器异常: Priority-$workerId", e)
                        delay(1000L)
                    }
                }
            }
        }
        
        // 普通优先级工作器
        repeat(maxConcurrency / 2) { workerId ->
            scope.launch {
                while (true) {
                    try {
                        val task = loadQueue.receive()
                        processLoadTask(task, "Normal-$workerId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 普通优先级工作器异常: Normal-$workerId", e)
                        delay(1000L)
                    }
                }
            }
        }
        
        Log.d(TAG, "🏭 加载工作器已启动: $maxConcurrency 个")
    }
    
    /**
     * ⚙️ 处理加载任务
     */
    private suspend fun processLoadTask(task: LoadTask, workerId: String) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        activeLoadCount.incrementAndGet()
        totalLoadCount.incrementAndGet()
        
        try {
            Log.d(TAG, "⚙️ [$workerId] 开始处理任务: ${task.id}")
            
            // 更新进度
            task.onProgress(LoadProgress(
                taskId = task.id,
                status = LoadStatus.LOADING,
                progress = 0,
                message = "正在加载...",
                startTime = startTime
            ))
            
            // 创建 Spider
            val spider = SpiderFactory.createSpider(
                type = task.site.type,
                api = task.site.api,
                context = context,
                extend = task.site.ext ?: "",
                useCache = true
            )
            
            if (spider == null) {
                throw IllegalStateException("无法创建 Spider: ${task.site.name}")
            }
            
            // 执行加载
            val result = withTimeoutOrNull(loadTimeout) {
                withRetry(retryCount) {
                    executeLoadTask(spider, task)
                }
            }
            
            if (result != null) {
                // 缓存结果
                cacheResult(task, result)
                
                // 更新进度
                task.onProgress(LoadProgress(
                    taskId = task.id,
                    status = LoadStatus.SUCCESS,
                    progress = 100,
                    message = "加载完成",
                    startTime = startTime,
                    endTime = System.currentTimeMillis()
                ))
                
                // 回调成功
                task.onSuccess(result)
                successLoadCount.incrementAndGet()
                
                Log.d(TAG, "✅ [$workerId] 任务完成: ${task.id}, 耗时: ${System.currentTimeMillis() - startTime}ms")
                
            } else {
                throw TimeoutCancellationException("加载超时")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [$workerId] 任务失败: ${task.id}", e)
            
            // 更新进度
            task.onProgress(LoadProgress(
                taskId = task.id,
                status = LoadStatus.ERROR,
                progress = 0,
                message = e.message ?: "加载失败",
                startTime = startTime,
                endTime = System.currentTimeMillis()
            ))
            
            // 回调错误
            task.onError(e)
            failedLoadCount.incrementAndGet()
            
        } finally {
            activeLoadCount.decrementAndGet()
        }
    }
    
    /**
     * 🔄 执行具体的加载任务
     */
    private suspend fun executeLoadTask(spider: Spider, task: LoadTask): String {
        return when (task.type) {
            LoadType.HOME -> {
                spider.homeContent(task.filter)
            }
            LoadType.CATEGORY -> {
                spider.categoryContent(task.tid!!, task.page!!, task.filter, task.extend)
            }
            LoadType.DETAIL -> {
                spider.detailContent(task.vodIds!!)
            }
            LoadType.PLAYER -> {
                spider.playerContent(task.flag!!, task.playId!!, task.vipFlags)
            }
        }
    }
    
    /**
     * 🔄 重试执行
     */
    private suspend fun <T> withRetry(
        times: Int,
        block: suspend () -> T
    ): T {
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "🔄 重试 ${attempt + 1}/$times", e)
                delay(1000L * (attempt + 1))
            }
        }
        return block()
    }
    
    /**
     * 🔧 生成任务ID
     */
    private fun generateTaskId(vararg parts: String): String {
        return parts.joinToString("_") + "_" + System.currentTimeMillis()
    }
    
    /**
     * 💾 缓存结果
     */
    private suspend fun cacheResult(task: LoadTask, content: String) {
        try {
            val result = LoadResult(
                taskId = task.id,
                content = content,
                createTime = System.currentTimeMillis()
            )
            
            loadTaskCache[task.id] = result
            
            // 根据类型缓存到对应的缓存中
            when (task.type) {
                LoadType.HOME -> {
                    ContentCache.putHomeContent(task.site.key, content)
                }
                LoadType.CATEGORY -> {
                    ContentCache.putCategoryContent(task.site.key, task.tid!!, task.page!!, content)
                }
                LoadType.DETAIL -> {
                    task.vodIds?.firstOrNull()?.let { vodId ->
                        ContentCache.putDetailContent(task.site.key, vodId, content)
                    }
                }
                LoadType.PLAYER -> {
                    ContentCache.putPlayerContent(task.site.key, task.flag!!, task.playId!!, content)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存结果失败: ${task.id}", e)
        }
    }
    
    /**
     * 📦 获取缓存结果
     */
    private suspend fun getCachedResult(task: LoadTask): LoadResult? {
        return try {
            // 先从内存缓存获取
            loadTaskCache[task.id]?.let { return it }
            
            // 从内容缓存获取
            val cachedContent = when (task.type) {
                LoadType.HOME -> {
                    ContentCache.getHomeContent(task.site.key)
                }
                LoadType.CATEGORY -> {
                    ContentCache.getCategoryContent(task.site.key, task.tid!!, task.page!!)
                }
                LoadType.DETAIL -> {
                    task.vodIds?.firstOrNull()?.let { vodId ->
                        ContentCache.getDetailContent(task.site.key, vodId)
                    }
                }
                LoadType.PLAYER -> {
                    ContentCache.getPlayerContent(task.site.key, task.flag!!, task.playId!!)
                }
            }
            
            if (cachedContent != null) {
                val result = LoadResult(
                    taskId = task.id,
                    content = cachedContent,
                    createTime = System.currentTimeMillis()
                )
                loadTaskCache[task.id] = result
                return result
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取缓存结果失败: ${task.id}", e)
            null
        }
    }
    
    /**
     * 📊 获取加载统计
     */
    fun getLoadStats(): LoadStats {
        return LoadStats(
            totalLoads = totalLoadCount.get(),
            successfulLoads = successLoadCount.get(),
            failedLoads = failedLoadCount.get(),
            activeLoads = activeLoadCount.get(),
            cachedResults = loadTaskCache.size,
            successRate = if (totalLoadCount.get() > 0) {
                (successLoadCount.get().toDouble() / totalLoadCount.get() * 100)
            } else 0.0
        )
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        loadTaskCache.clear()
        Log.d(TAG, "🧹 加载缓存已清理")
    }
    
    /**
     * 🛑 关闭加载器
     */
    fun shutdown() {
        scope.cancel()
        loadQueue.close()
        priorityQueue.close()
        Log.d(TAG, "🛑 异步加载器已关闭")
    }
    
    /**
     * 加载任务数据类
     */
    data class LoadTask(
        val id: String,
        val type: LoadType,
        val site: VodSite,
        val priority: LoadPriority,
        val filter: Boolean = false,
        val tid: String? = null,
        val page: String? = null,
        val extend: HashMap<String, String> = hashMapOf(),
        val vodIds: List<String>? = null,
        val flag: String? = null,
        val playId: String? = null,
        val vipFlags: List<String> = emptyList(),
        val onSuccess: (String) -> Unit,
        val onError: (Throwable) -> Unit,
        val onProgress: (LoadProgress) -> Unit,
        val createTime: Long
    )
    
    /**
     * 加载结果数据类
     */
    private data class LoadResult(
        val taskId: String,
        val content: String,
        val createTime: Long
    )
    
    /**
     * 加载进度数据类
     */
    data class LoadProgress(
        val taskId: String,
        val status: LoadStatus,
        val progress: Int,
        val message: String,
        val startTime: Long,
        val endTime: Long? = null
    ) {
        val elapsedTime: Long
            get() = (endTime ?: System.currentTimeMillis()) - startTime
    }
    
    /**
     * 加载统计数据类
     */
    data class LoadStats(
        val totalLoads: Long,
        val successfulLoads: Long,
        val failedLoads: Long,
        val activeLoads: Int,
        val cachedResults: Int,
        val successRate: Double
    )
    
    /**
     * 加载类型枚举
     */
    enum class LoadType {
        HOME, CATEGORY, DETAIL, PLAYER
    }
    
    /**
     * 加载优先级枚举
     */
    enum class LoadPriority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    /**
     * 加载状态枚举
     */
    enum class LoadStatus {
        PENDING, LOADING, SUCCESS, ERROR
    }
}
