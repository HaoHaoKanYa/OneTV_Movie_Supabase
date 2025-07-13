package top.cywin.onetv.film.hook

import android.util.Log

/**
 * Hook 基础接口
 * 
 * 基于 FongMi/TV 的 Hook 机制实现
 * 提供请求、响应和播放器的拦截和处理功能
 * 
 * 功能：
 * - 请求拦截和修改
 * - 响应拦截和处理
 * - 播放器链接处理
 * - 自定义处理逻辑
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
interface Hook {
    
    /**
     * 🔧 Hook 名称
     */
    val name: String
    
    /**
     * 📋 Hook 描述
     */
    val description: String
    
    /**
     * 🎯 Hook 优先级（数值越小优先级越高）
     */
    val priority: Int get() = 100
    
    /**
     * ✅ 是否启用
     */
    val enabled: Boolean get() = true
    
    /**
     * 🔍 是否匹配当前请求/响应
     */
    fun matches(context: HookContext): Boolean
    
    /**
     * 🚀 执行 Hook 处理
     */
    suspend fun execute(context: HookContext): HookResult
    
    /**
     * 📊 获取 Hook 统计信息
     */
    fun getStats(): Map<String, Any> = emptyMap()
    
    /**
     * 🧹 清理资源
     */
    fun cleanup() {}
}

/**
 * 请求 Hook 接口
 */
interface RequestHook : Hook {
    
    /**
     * 🌐 处理请求
     */
    suspend fun processRequest(request: HookRequest): HookRequest
    
    override suspend fun execute(context: HookContext): HookResult {
        return if (context is RequestHookContext) {
            val processedRequest = processRequest(context.request)
            HookResult.success(processedRequest)
        } else {
            HookResult.skip("Not a request context")
        }
    }
}

/**
 * 响应 Hook 接口
 */
interface ResponseHook : Hook {
    
    /**
     * 📥 处理响应
     */
    suspend fun processResponse(response: HookResponse): HookResponse
    
    override suspend fun execute(context: HookContext): HookResult {
        return if (context is ResponseHookContext) {
            val processedResponse = processResponse(context.response)
            HookResult.success(processedResponse)
        } else {
            HookResult.skip("Not a response context")
        }
    }
}

/**
 * 播放器 Hook 接口
 */
interface PlayerHook : Hook {
    
    /**
     * ▶️ 处理播放器链接
     */
    suspend fun processPlayerUrl(playerUrl: HookPlayerUrl): HookPlayerUrl
    
    override suspend fun execute(context: HookContext): HookResult {
        return if (context is PlayerHookContext) {
            val processedUrl = processPlayerUrl(context.playerUrl)
            HookResult.success(processedUrl)
        } else {
            HookResult.skip("Not a player context")
        }
    }
}

/**
 * Hook 上下文基类
 */
sealed class HookContext {
    abstract val timestamp: Long
    abstract val metadata: Map<String, Any>
}

/**
 * 请求 Hook 上下文
 */
data class RequestHookContext(
    val request: HookRequest,
    override val timestamp: Long = System.currentTimeMillis(),
    override val metadata: Map<String, Any> = emptyMap()
) : HookContext()

/**
 * 响应 Hook 上下文
 */
data class ResponseHookContext(
    val response: HookResponse,
    override val timestamp: Long = System.currentTimeMillis(),
    override val metadata: Map<String, Any> = emptyMap()
) : HookContext()

/**
 * 播放器 Hook 上下文
 */
data class PlayerHookContext(
    val playerUrl: HookPlayerUrl,
    override val timestamp: Long = System.currentTimeMillis(),
    override val metadata: Map<String, Any> = emptyMap()
) : HookContext()

/**
 * Hook 请求数据
 */
data class HookRequest(
    val url: String,
    val method: String = "GET",
    val headers: MutableMap<String, String> = mutableMapOf(),
    val body: String = "",
    val params: MutableMap<String, String> = mutableMapOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    
    /**
     * 🔧 添加请求头
     */
    fun addHeader(key: String, value: String): HookRequest {
        headers[key] = value
        return this
    }
    
    /**
     * 🔧 添加参数
     */
    fun addParam(key: String, value: String): HookRequest {
        params[key] = value
        return this
    }
    
    /**
     * 🔧 设置元数据
     */
    fun setMetadata(key: String, value: Any): HookRequest {
        metadata[key] = value
        return this
    }
    
    /**
     * 📋 复制请求
     */
    fun copy(): HookRequest {
        return HookRequest(
            url = url,
            method = method,
            headers = headers.toMutableMap(),
            body = body,
            params = params.toMutableMap(),
            metadata = metadata.toMutableMap()
        )
    }
}

/**
 * Hook 响应数据
 */
data class HookResponse(
    val statusCode: Int,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val body: String = "",
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    
    /**
     * ✅ 是否成功
     */
    val isSuccess: Boolean get() = statusCode in 200..299
    
    /**
     * 🔧 添加响应头
     */
    fun addHeader(key: String, value: String): HookResponse {
        headers[key] = value
        return this
    }
    
    /**
     * 🔧 设置元数据
     */
    fun setMetadata(key: String, value: Any): HookResponse {
        metadata[key] = value
        return this
    }
    
    /**
     * 📋 复制响应
     */
    fun copy(): HookResponse {
        return HookResponse(
            statusCode = statusCode,
            headers = headers.toMutableMap(),
            body = body,
            metadata = metadata.toMutableMap()
        )
    }
}

/**
 * Hook 播放器链接数据
 */
data class HookPlayerUrl(
    val originalUrl: String,
    val processedUrl: String = originalUrl,
    val headers: MutableMap<String, String> = mutableMapOf(),
    val parse: Int = 0,
    val flag: String = "",
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    
    /**
     * 🔧 添加请求头
     */
    fun addHeader(key: String, value: String): HookPlayerUrl {
        headers[key] = value
        return this
    }
    
    /**
     * 🔧 设置元数据
     */
    fun setMetadata(key: String, value: Any): HookPlayerUrl {
        metadata[key] = value
        return this
    }
    
    /**
     * 📋 复制播放器链接
     */
    fun copy(): HookPlayerUrl {
        return HookPlayerUrl(
            originalUrl = originalUrl,
            processedUrl = processedUrl,
            headers = headers.toMutableMap(),
            parse = parse,
            flag = flag,
            metadata = metadata.toMutableMap()
        )
    }
}

/**
 * Hook 执行结果
 */
sealed class HookResult {
    
    /**
     * ✅ 成功结果
     */
    data class Success(val data: Any?) : HookResult()
    
    /**
     * ❌ 失败结果
     */
    data class Failure(val error: String, val exception: Exception? = null) : HookResult()
    
    /**
     * ⏭️ 跳过结果
     */
    data class Skip(val reason: String) : HookResult()
    
    /**
     * 🛑 停止结果（停止后续 Hook 执行）
     */
    data class Stop(val data: Any?) : HookResult()
    
    companion object {
        fun success(data: Any? = null) = Success(data)
        fun failure(error: String, exception: Exception? = null) = Failure(error, exception)
        fun skip(reason: String) = Skip(reason)
        fun stop(data: Any? = null) = Stop(data)
    }
}

/**
 * Hook 统计信息
 */
data class HookStats(
    val hookName: String,
    var executionCount: Long = 0,
    var successCount: Long = 0,
    var failureCount: Long = 0,
    var skipCount: Long = 0,
    var totalDuration: Long = 0,
    var lastExecutionTime: Long = 0,
    val recentErrors: MutableList<String> = mutableListOf()
) {
    
    /**
     * 📊 记录执行
     */
    fun recordExecution(result: HookResult, duration: Long) {
        executionCount++
        totalDuration += duration
        lastExecutionTime = System.currentTimeMillis()
        
        when (result) {
            is HookResult.Success -> successCount++
            is HookResult.Failure -> {
                failureCount++
                addRecentError(result.error)
            }
            is HookResult.Skip -> skipCount++
            is HookResult.Stop -> successCount++
        }
    }
    
    /**
     * ❌ 添加最近错误
     */
    private fun addRecentError(error: String) {
        recentErrors.add(error)
        if (recentErrors.size > 10) {
            recentErrors.removeAt(0)
        }
    }
    
    /**
     * 📈 获取成功率
     */
    val successRate: Double
        get() = if (executionCount > 0) successCount.toDouble() / executionCount else 0.0
    
    /**
     * ⏱️ 获取平均执行时间
     */
    val averageDuration: Long
        get() = if (executionCount > 0) totalDuration / executionCount else 0
    
    /**
     * 📋 获取统计报告
     */
    fun getReport(): Map<String, Any> {
        return mapOf(
            "hook_name" to hookName,
            "execution_count" to executionCount,
            "success_count" to successCount,
            "failure_count" to failureCount,
            "skip_count" to skipCount,
            "success_rate" to successRate,
            "average_duration" to averageDuration,
            "last_execution_time" to lastExecutionTime,
            "recent_errors" to recentErrors.toList()
        )
    }
}
