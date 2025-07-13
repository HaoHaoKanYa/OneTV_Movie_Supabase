package top.cywin.onetv.film.hook

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Hook 管理器
 * 
 * 基于 FongMi/TV 的 Hook 管理系统
 * 负责 Hook 的注册、执行和管理
 * 
 * 功能：
 * - Hook 注册和注销
 * - Hook 执行调度
 * - 优先级管理
 * - 性能监控
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class HookManager {
    
    companion object {
        private const val TAG = "ONETV_FILM_HOOK_MANAGER"
        
        // Hook 执行超时时间
        private const val HOOK_TIMEOUT = 10000L // 10秒
    }
    
    // Hook 注册表
    private val requestHooks = CopyOnWriteArrayList<RequestHook>()
    private val responseHooks = CopyOnWriteArrayList<ResponseHook>()
    private val playerHooks = CopyOnWriteArrayList<PlayerHook>()
    
    // Hook 统计
    private val hookStats = ConcurrentHashMap<String, HookStats>()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 是否已初始化
    private var initialized = false
    
    /**
     * 🔧 初始化 Hook 管理器
     */
    fun initialize() {
        if (initialized) {
            Log.w(TAG, "⚠️ Hook 管理器已经初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 初始化 Hook 管理器...")
            
            // 注册内置 Hook
            registerBuiltInHooks()
            
            initialized = true
            Log.d(TAG, "✅ Hook 管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Hook 管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 📋 注册内置 Hook
     */
    private fun registerBuiltInHooks() {
        Log.d(TAG, "📋 注册内置 Hook...")
        
        // 注册请求 Hook
        registerRequestHook(UserAgentHook())
        registerRequestHook(RefererHook())
        registerRequestHook(CacheControlHook())
        
        // 注册响应 Hook
        registerResponseHook(ContentTypeHook())
        registerResponseHook(EncodingHook())
        
        // 注册播放器 Hook
        registerPlayerHook(M3U8Hook())
        registerPlayerHook(Mp4Hook())
        
        Log.d(TAG, "✅ 内置 Hook 注册完成")
    }
    
    /**
     * 📝 注册请求 Hook
     */
    fun registerRequestHook(hook: RequestHook) {
        if (!requestHooks.contains(hook)) {
            requestHooks.add(hook)
            requestHooks.sortBy { it.priority }
            hookStats[hook.name] = HookStats(hook.name)
            Log.d(TAG, "📝 注册请求 Hook: ${hook.name}")
        }
    }
    
    /**
     * 📝 注册响应 Hook
     */
    fun registerResponseHook(hook: ResponseHook) {
        if (!responseHooks.contains(hook)) {
            responseHooks.add(hook)
            responseHooks.sortBy { it.priority }
            hookStats[hook.name] = HookStats(hook.name)
            Log.d(TAG, "📝 注册响应 Hook: ${hook.name}")
        }
    }
    
    /**
     * 📝 注册播放器 Hook
     */
    fun registerPlayerHook(hook: PlayerHook) {
        if (!playerHooks.contains(hook)) {
            playerHooks.add(hook)
            playerHooks.sortBy { it.priority }
            hookStats[hook.name] = HookStats(hook.name)
            Log.d(TAG, "📝 注册播放器 Hook: ${hook.name}")
        }
    }
    
    /**
     * 🗑️ 注销请求 Hook
     */
    fun unregisterRequestHook(hook: RequestHook) {
        if (requestHooks.remove(hook)) {
            hookStats.remove(hook.name)
            Log.d(TAG, "🗑️ 注销请求 Hook: ${hook.name}")
        }
    }
    
    /**
     * 🗑️ 注销响应 Hook
     */
    fun unregisterResponseHook(hook: ResponseHook) {
        if (responseHooks.remove(hook)) {
            hookStats.remove(hook.name)
            Log.d(TAG, "🗑️ 注销响应 Hook: ${hook.name}")
        }
    }
    
    /**
     * 🗑️ 注销播放器 Hook
     */
    fun unregisterPlayerHook(hook: PlayerHook) {
        if (playerHooks.remove(hook)) {
            hookStats.remove(hook.name)
            Log.d(TAG, "🗑️ 注销播放器 Hook: ${hook.name}")
        }
    }
    
    /**
     * 🚀 执行请求 Hook
     */
    suspend fun executeRequestHooks(request: HookRequest): HookRequest = withContext(Dispatchers.IO) {
        var currentRequest = request
        val context = RequestHookContext(currentRequest)
        
        Log.d(TAG, "🚀 执行请求 Hook: ${request.url}")
        
        for (hook in requestHooks) {
            if (!hook.enabled || !hook.matches(context)) {
                continue
            }
            
            try {
                val result = withTimeout(HOOK_TIMEOUT) {
                    val startTime = System.currentTimeMillis()
                    val hookResult = hook.execute(context.copy(request = currentRequest))
                    val duration = System.currentTimeMillis() - startTime
                    
                    // 记录统计
                    hookStats[hook.name]?.recordExecution(hookResult, duration)
                    
                    hookResult
                }
                
                when (result) {
                    is HookResult.Success -> {
                        if (result.data is HookRequest) {
                            currentRequest = result.data
                            Log.d(TAG, "✅ Hook 执行成功: ${hook.name}")
                        }
                    }
                    is HookResult.Stop -> {
                        if (result.data is HookRequest) {
                            currentRequest = result.data
                        }
                        Log.d(TAG, "🛑 Hook 执行停止: ${hook.name}")
                        break
                    }
                    is HookResult.Skip -> {
                        Log.d(TAG, "⏭️ Hook 跳过: ${hook.name} - ${result.reason}")
                    }
                    is HookResult.Failure -> {
                        Log.w(TAG, "❌ Hook 执行失败: ${hook.name} - ${result.error}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "⏰ Hook 执行超时: ${hook.name}")
                hookStats[hook.name]?.recordExecution(HookResult.failure("Timeout"), HOOK_TIMEOUT)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hook 执行异常: ${hook.name}", e)
                hookStats[hook.name]?.recordExecution(HookResult.failure(e.message ?: "Unknown error"), 0)
            }
        }
        
        currentRequest
    }
    
    /**
     * 🚀 执行响应 Hook
     */
    suspend fun executeResponseHooks(response: HookResponse): HookResponse = withContext(Dispatchers.IO) {
        var currentResponse = response
        val context = ResponseHookContext(currentResponse)
        
        Log.d(TAG, "🚀 执行响应 Hook: ${response.statusCode}")
        
        for (hook in responseHooks) {
            if (!hook.enabled || !hook.matches(context)) {
                continue
            }
            
            try {
                val result = withTimeout(HOOK_TIMEOUT) {
                    val startTime = System.currentTimeMillis()
                    val hookResult = hook.execute(context.copy(response = currentResponse))
                    val duration = System.currentTimeMillis() - startTime
                    
                    // 记录统计
                    hookStats[hook.name]?.recordExecution(hookResult, duration)
                    
                    hookResult
                }
                
                when (result) {
                    is HookResult.Success -> {
                        if (result.data is HookResponse) {
                            currentResponse = result.data
                            Log.d(TAG, "✅ Hook 执行成功: ${hook.name}")
                        }
                    }
                    is HookResult.Stop -> {
                        if (result.data is HookResponse) {
                            currentResponse = result.data
                        }
                        Log.d(TAG, "🛑 Hook 执行停止: ${hook.name}")
                        break
                    }
                    is HookResult.Skip -> {
                        Log.d(TAG, "⏭️ Hook 跳过: ${hook.name} - ${result.reason}")
                    }
                    is HookResult.Failure -> {
                        Log.w(TAG, "❌ Hook 执行失败: ${hook.name} - ${result.error}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "⏰ Hook 执行超时: ${hook.name}")
                hookStats[hook.name]?.recordExecution(HookResult.failure("Timeout"), HOOK_TIMEOUT)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hook 执行异常: ${hook.name}", e)
                hookStats[hook.name]?.recordExecution(HookResult.failure(e.message ?: "Unknown error"), 0)
            }
        }
        
        currentResponse
    }
    
    /**
     * 🚀 执行播放器 Hook
     */
    suspend fun executePlayerHooks(playerUrl: HookPlayerUrl): HookPlayerUrl = withContext(Dispatchers.IO) {
        var currentPlayerUrl = playerUrl
        val context = PlayerHookContext(currentPlayerUrl)
        
        Log.d(TAG, "🚀 执行播放器 Hook: ${playerUrl.originalUrl}")
        
        for (hook in playerHooks) {
            if (!hook.enabled || !hook.matches(context)) {
                continue
            }
            
            try {
                val result = withTimeout(HOOK_TIMEOUT) {
                    val startTime = System.currentTimeMillis()
                    val hookResult = hook.execute(context.copy(playerUrl = currentPlayerUrl))
                    val duration = System.currentTimeMillis() - startTime
                    
                    // 记录统计
                    hookStats[hook.name]?.recordExecution(hookResult, duration)
                    
                    hookResult
                }
                
                when (result) {
                    is HookResult.Success -> {
                        if (result.data is HookPlayerUrl) {
                            currentPlayerUrl = result.data
                            Log.d(TAG, "✅ Hook 执行成功: ${hook.name}")
                        }
                    }
                    is HookResult.Stop -> {
                        if (result.data is HookPlayerUrl) {
                            currentPlayerUrl = result.data
                        }
                        Log.d(TAG, "🛑 Hook 执行停止: ${hook.name}")
                        break
                    }
                    is HookResult.Skip -> {
                        Log.d(TAG, "⏭️ Hook 跳过: ${hook.name} - ${result.reason}")
                    }
                    is HookResult.Failure -> {
                        Log.w(TAG, "❌ Hook 执行失败: ${hook.name} - ${result.error}")
                    }
                }
                
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "⏰ Hook 执行超时: ${hook.name}")
                hookStats[hook.name]?.recordExecution(HookResult.failure("Timeout"), HOOK_TIMEOUT)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hook 执行异常: ${hook.name}", e)
                hookStats[hook.name]?.recordExecution(HookResult.failure(e.message ?: "Unknown error"), 0)
            }
        }
        
        currentPlayerUrl
    }
    
    /**
     * 📊 获取 Hook 统计信息
     */
    fun getHookStats(): Map<String, Any> {
        return mapOf(
            "request_hooks" to requestHooks.size,
            "response_hooks" to responseHooks.size,
            "player_hooks" to playerHooks.size,
            "total_hooks" to (requestHooks.size + responseHooks.size + playerHooks.size),
            "hook_stats" to hookStats.mapValues { it.value.getReport() }
        )
    }
    
    /**
     * 📋 获取已注册的 Hook 列表
     */
    fun getRegisteredHooks(): Map<String, List<String>> {
        return mapOf(
            "request_hooks" to requestHooks.map { "${it.name} (priority: ${it.priority})" },
            "response_hooks" to responseHooks.map { "${it.name} (priority: ${it.priority})" },
            "player_hooks" to playerHooks.map { "${it.name} (priority: ${it.priority})" }
        )
    }
    
    /**
     * 🧹 清理资源
     */
    fun shutdown() {
        Log.d(TAG, "🧹 关闭 Hook 管理器...")
        
        // 清理所有 Hook
        requestHooks.forEach { it.cleanup() }
        responseHooks.forEach { it.cleanup() }
        playerHooks.forEach { it.cleanup() }
        
        // 清空注册表
        requestHooks.clear()
        responseHooks.clear()
        playerHooks.clear()
        
        // 清空统计
        hookStats.clear()
        
        // 取消协程
        scope.cancel()
        
        initialized = false
        Log.d(TAG, "✅ Hook 管理器关闭完成")
    }
}
