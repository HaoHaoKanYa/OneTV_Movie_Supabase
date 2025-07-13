package top.cywin.onetv.film.hook

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.utils.RegexUtils
import java.util.concurrent.atomic.AtomicLong

/**
 * 响应拦截 Hook
 * 基于 FongMi/TV 标准实现
 * 
 * 提供HTTP响应的拦截和处理功能
 * 
 * 功能：
 * - 响应内容过滤
 * - JSON数据清理
 * - HTML内容提取
 * - 错误响应处理
 * - 内容格式转换
 * - 数据验证
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ResponseHook : Hook, ResponseHook {
    
    companion object {
        private const val TAG = "ONETV_FILM_RESPONSE_HOOK"
    }
    
    override val name: String = "ResponseHook"
    override val description: String = "HTTP响应拦截和处理"
    override val priority: Int = 60
    
    // 统计信息
    private val stats = HookStats(name)
    private val processedCount = AtomicLong(0)
    private val modifiedCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    
    // JSON解析器
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // 内容过滤规则
    private val contentFilters = mutableListOf<ContentFilter>()
    
    // 默认过滤规则
    init {
        addDefaultFilters()
    }
    
    override fun matches(context: HookContext): Boolean {
        return context is ResponseHookContext
    }
    
    override suspend fun processResponse(response: HookResponse): HookResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "📥 处理响应: ${response.statusCode}")
            processedCount.incrementAndGet()
            
            val modifiedResponse = response.copy()
            var isModified = false
            
            // 1. 检查响应状态
            if (!response.isSuccess) {
                Log.w(TAG, "⚠️ 响应状态异常: ${response.statusCode}")
                errorCount.incrementAndGet()
                return@withContext handleErrorResponse(modifiedResponse)
            }
            
            // 2. 内容类型检测
            val contentType = response.headers["Content-Type"] ?: response.headers["content-type"] ?: ""
            
            // 3. 根据内容类型处理
            when {
                contentType.contains("application/json") -> {
                    val processedBody = processJsonResponse(modifiedResponse.body)
                    if (processedBody != modifiedResponse.body) {
                        modifiedResponse.body = processedBody
                        isModified = true
                    }
                }
                
                contentType.contains("text/html") -> {
                    val processedBody = processHtmlResponse(modifiedResponse.body)
                    if (processedBody != modifiedResponse.body) {
                        modifiedResponse.body = processedBody
                        isModified = true
                    }
                }
                
                contentType.contains("text/xml") -> {
                    val processedBody = processXmlResponse(modifiedResponse.body)
                    if (processedBody != modifiedResponse.body) {
                        modifiedResponse.body = processedBody
                        isModified = true
                    }
                }
                
                else -> {
                    val processedBody = processTextResponse(modifiedResponse.body)
                    if (processedBody != modifiedResponse.body) {
                        modifiedResponse.body = processedBody
                        isModified = true
                    }
                }
            }
            
            // 4. 应用内容过滤器
            val filteredBody = applyContentFilters(modifiedResponse.body)
            if (filteredBody != modifiedResponse.body) {
                modifiedResponse.body = filteredBody
                isModified = true
            }
            
            // 5. 添加处理标记
            modifiedResponse.setMetadata("processed_by", name)
            modifiedResponse.setMetadata("processed_time", System.currentTimeMillis())
            
            if (isModified) {
                modifiedCount.incrementAndGet()
                Log.d(TAG, "✅ 响应已修改")
            }
            
            // 记录统计
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.success(modifiedResponse), duration)
            
            modifiedResponse
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 响应处理失败", e)
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.failure(e.message ?: "响应处理失败", e), duration)
            throw e
        }
    }
    
    /**
     * 📄 处理JSON响应
     */
    private fun processJsonResponse(body: String): String {
        return try {
            if (body.isBlank()) return body
            
            val jsonElement = json.parseToJsonElement(body)
            val processedJson = processJsonElement(jsonElement)
            
            json.encodeToString(JsonElement.serializer(), processedJson)
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ JSON处理失败，返回原内容", e)
            body
        }
    }
    
    /**
     * 🔧 处理JSON元素
     */
    private fun processJsonElement(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                buildJsonObject {
                    for ((key, value) in element) {
                        // 过滤敏感字段
                        if (!isSensitiveField(key)) {
                            put(key, processJsonElement(value))
                        }
                    }
                }
            }
            
            is JsonArray -> {
                buildJsonArray {
                    for (item in element) {
                        add(processJsonElement(item))
                    }
                }
            }
            
            is JsonPrimitive -> {
                if (element.isString) {
                    val content = element.content
                    val cleanContent = cleanTextContent(content)
                    JsonPrimitive(cleanContent)
                } else {
                    element
                }
            }
            
            else -> element
        }
    }
    
    /**
     * 🌐 处理HTML响应
     */
    private fun processHtmlResponse(body: String): String {
        return try {
            if (body.isBlank()) return body
            
            var processedBody = body
            
            // 移除脚本标签
            processedBody = processedBody.replace("<script[^>]*>.*?</script>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            
            // 移除样式标签
            processedBody = processedBody.replace("<style[^>]*>.*?</style>".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            
            // 移除注释
            processedBody = processedBody.replace("<!--.*?-->".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            
            // 清理多余空白
            processedBody = processedBody.replace("\\s+".toRegex(), " ")
            
            processedBody.trim()
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ HTML处理失败，返回原内容", e)
            body
        }
    }
    
    /**
     * 📋 处理XML响应
     */
    private fun processXmlResponse(body: String): String {
        return try {
            if (body.isBlank()) return body
            
            var processedBody = body
            
            // 移除XML注释
            processedBody = processedBody.replace("<!--.*?-->".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            
            // 清理多余空白
            processedBody = processedBody.replace(">\\s+<".toRegex(), "><")
            
            processedBody.trim()
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ XML处理失败，返回原内容", e)
            body
        }
    }
    
    /**
     * 📝 处理文本响应
     */
    private fun processTextResponse(body: String): String {
        return try {
            if (body.isBlank()) return body
            
            cleanTextContent(body)
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 文本处理失败，返回原内容", e)
            body
        }
    }
    
    /**
     * ❌ 处理错误响应
     */
    private fun handleErrorResponse(response: HookResponse): HookResponse {
        val errorResponse = response.copy()
        
        // 添加错误信息
        errorResponse.setMetadata("error_handled", true)
        errorResponse.setMetadata("original_status", response.statusCode)
        
        // 根据错误类型处理
        when (response.statusCode) {
            404 -> {
                errorResponse.body = createErrorJson("资源未找到", 404)
            }
            403 -> {
                errorResponse.body = createErrorJson("访问被拒绝", 403)
            }
            500 -> {
                errorResponse.body = createErrorJson("服务器内部错误", 500)
            }
            else -> {
                errorResponse.body = createErrorJson("请求失败", response.statusCode)
            }
        }
        
        return errorResponse
    }
    
    /**
     * 🧹 清理文本内容
     */
    private fun cleanTextContent(content: String): String {
        var cleanContent = content
        
        // 移除控制字符
        cleanContent = cleanContent.replace("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]".toRegex(), "")
        
        // 移除多余空白
        cleanContent = cleanContent.replace("\\s+".toRegex(), " ")
        
        // 移除HTML实体
        cleanContent = cleanContent.replace("&nbsp;", " ")
        cleanContent = cleanContent.replace("&amp;", "&")
        cleanContent = cleanContent.replace("&lt;", "<")
        cleanContent = cleanContent.replace("&gt;", ">")
        cleanContent = cleanContent.replace("&quot;", "\"")
        cleanContent = cleanContent.replace("&#39;", "'")
        
        return cleanContent.trim()
    }
    
    /**
     * 🔒 检查是否为敏感字段
     */
    private fun isSensitiveField(fieldName: String): Boolean {
        val sensitiveFields = setOf(
            "password", "token", "secret", "key", "auth",
            "session", "cookie", "credential", "private"
        )
        
        return sensitiveFields.any { fieldName.lowercase().contains(it) }
    }
    
    /**
     * 🔧 应用内容过滤器
     */
    private fun applyContentFilters(content: String): String {
        var filteredContent = content
        
        for (filter in contentFilters) {
            if (filter.enabled && filter.pattern.containsMatchIn(filteredContent)) {
                filteredContent = filter.apply(filteredContent)
                Log.d(TAG, "🔧 应用过滤器: ${filter.name}")
            }
        }
        
        return filteredContent
    }
    
    /**
     * 📋 创建错误JSON
     */
    private fun createErrorJson(message: String, code: Int): String {
        return json.encodeToString(buildJsonObject {
            put("code", code)
            put("msg", message)
            put("data", JsonNull)
            put("timestamp", System.currentTimeMillis())
        })
    }
    
    /**
     * 🔧 添加默认过滤器
     */
    private fun addDefaultFilters() {
        // 移除广告内容
        addContentFilter(ContentFilter(
            name = "广告过滤器",
            pattern = "(?i)(广告|advertisement|ads?)".toRegex(),
            replacement = "",
            enabled = true
        ))
        
        // 移除追踪代码
        addContentFilter(ContentFilter(
            name = "追踪代码过滤器",
            pattern = "(?i)(google-analytics|gtag|_ga|_gid)".toRegex(),
            replacement = "",
            enabled = true
        ))
    }
    
    /**
     * 🔧 添加内容过滤器
     */
    fun addContentFilter(filter: ContentFilter) {
        contentFilters.add(filter)
        Log.d(TAG, "🔧 添加内容过滤器: ${filter.name}")
    }
    
    /**
     * 🗑️ 移除内容过滤器
     */
    fun removeContentFilter(filterName: String) {
        contentFilters.removeAll { it.name == filterName }
        Log.d(TAG, "🗑️ 移除内容过滤器: $filterName")
    }
    
    override fun getStats(): Map<String, Any> {
        return stats.getReport() + mapOf(
            "processed_count" to processedCount.get(),
            "modified_count" to modifiedCount.get(),
            "error_count" to errorCount.get(),
            "modification_rate" to if (processedCount.get() > 0) {
                (modifiedCount.get().toDouble() / processedCount.get() * 100)
            } else 0.0,
            "error_rate" to if (processedCount.get() > 0) {
                (errorCount.get().toDouble() / processedCount.get() * 100)
            } else 0.0,
            "content_filters" to contentFilters.size
        )
    }
    
    override fun cleanup() {
        contentFilters.clear()
        Log.d(TAG, "🧹 ResponseHook 资源已清理")
    }
    
    /**
     * 内容过滤器数据类
     */
    data class ContentFilter(
        val name: String,
        val pattern: Regex,
        val replacement: String = "",
        val enabled: Boolean = true
    ) {
        fun apply(content: String): String {
            return if (enabled) {
                content.replace(pattern, replacement)
            } else {
                content
            }
        }
    }
}
