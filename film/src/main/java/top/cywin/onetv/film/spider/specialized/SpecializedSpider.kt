package top.cywin.onetv.film.spider.specialized

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.network.OkHttpManager
import top.cywin.onetv.film.utils.JsoupUtils
import top.cywin.onetv.film.utils.UrlUtils
import java.util.regex.Pattern

/**
 * 专用解析器基类
 * 
 * 基于 FongMi/TV 的专用解析器架构
 * 为特定站点提供定制化的解析功能
 * 
 * 功能：
 * - 站点特定的解析逻辑
 * - 自定义请求头和参数
 * - 特殊的数据处理
 * - 错误处理和重试
 * - 缓存和优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
abstract class SpecializedSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPECIALIZED_SPIDER"
    }
    
    // HTTP 管理器
    protected val httpManager = OkHttpManager()
    
    // JSON 解析器
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // 站点特定配置
    protected var siteConfig: SpecializedConfig? = null
    
    // 请求缓存
    protected val requestCache = mutableMapOf<String, String>()
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        
        try {
            // 解析站点特定配置
            siteConfig = parseSpecializedConfig(extend)
            logDebug("✅ 专用解析器配置解析成功")
        } catch (e: Exception) {
            logError("❌ 专用解析器配置解析失败", e)
            siteConfig = createDefaultConfig()
        }
    }
    
    // ========== 抽象方法 ==========
    
    /**
     * 🔧 解析站点特定配置
     */
    protected abstract fun parseSpecializedConfig(extend: String): SpecializedConfig
    
    /**
     * 🔧 创建默认配置
     */
    protected abstract fun createDefaultConfig(): SpecializedConfig
    
    /**
     * 🌐 获取站点特定的请求头
     */
    protected abstract fun getSpecializedHeaders(): Map<String, String>
    
    /**
     * 🔗 构建站点特定的 URL
     */
    protected abstract fun buildSpecializedUrl(path: String, params: Map<String, String> = emptyMap()): String
    
    // ========== 通用工具方法 ==========
    
    /**
     * 🌐 发送 GET 请求（带缓存）
     */
    protected suspend fun getWithCache(url: String, useCache: Boolean = true): String = withContext(Dispatchers.IO) {
        if (useCache && requestCache.containsKey(url)) {
            logDebug("📦 从缓存获取: $url")
            return@withContext requestCache[url]!!
        }
        
        val headers = getSpecializedHeaders() + siteHeaders
        val response = httpManager.getString(url, headers)
        
        if (useCache) {
            requestCache[url] = response
        }
        
        response
    }
    
    /**
     * 📤 发送 POST 请求
     */
    protected suspend fun postRequest(
        url: String,
        body: String,
        contentType: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        val headers = getSpecializedHeaders() + siteHeaders
        httpManager.postString(url, body, headers, contentType)
    }
    
    /**
     * 📤 发送表单请求
     */
    protected suspend fun postForm(url: String, formData: Map<String, String>): String = withContext(Dispatchers.IO) {
        val headers = getSpecializedHeaders() + siteHeaders
        httpManager.postForm(url, formData, headers)
    }
    
    /**
     * 🔍 提取文本内容
     */
    protected fun extractText(html: String, rule: String): String {
        return try {
            JsoupUtils.parseRule(html, rule).trim()
        } catch (e: Exception) {
            logWarning("⚠️ 文本提取失败: rule=$rule", e)
            ""
        }
    }
    
    /**
     * 🔍 提取文本列表
     */
    protected fun extractTextList(html: String, rule: String): List<String> {
        return try {
            JsoupUtils.parseRuleArray(html, rule).filter { it.isNotEmpty() }
        } catch (e: Exception) {
            logWarning("⚠️ 文本列表提取失败: rule=$rule", e)
            emptyList()
        }
    }
    
    /**
     * 🔗 解析链接
     */
    protected fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return UrlUtils.resolveUrl(baseUrl, relativeUrl)
    }
    
    /**
     * 🔍 正则表达式匹配
     */
    protected fun regexFind(text: String, pattern: String, group: Int = 1): String {
        return try {
            val matcher = Pattern.compile(pattern).matcher(text)
            if (matcher.find() && group <= matcher.groupCount()) {
                matcher.group(group) ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            logWarning("⚠️ 正则匹配失败: pattern=$pattern", e)
            ""
        }
    }
    
    /**
     * 🔍 正则表达式匹配所有
     */
    protected fun regexFindAll(text: String, pattern: String, group: Int = 1): List<String> {
        return try {
            val results = mutableListOf<String>()
            val matcher = Pattern.compile(pattern).matcher(text)
            
            while (matcher.find()) {
                if (group <= matcher.groupCount()) {
                    matcher.group(group)?.let { results.add(it) }
                }
            }
            
            results
        } catch (e: Exception) {
            logWarning("⚠️ 正则匹配所有失败: pattern=$pattern", e)
            emptyList()
        }
    }
    
    /**
     * 🔧 解析 JSON 响应
     */
    protected fun parseJsonResponse(response: String): JsonObject? {
        return try {
            json.parseToJsonElement(response).jsonObject
        } catch (e: Exception) {
            logWarning("⚠️ JSON 解析失败", e)
            null
        }
    }
    
    /**
     * 🔧 构建标准响应
     */
    protected fun buildStandardResponse(block: JsonObjectBuilder.() -> Unit): String {
        return buildJsonResponse(block)
    }
    
    /**
     * 🔧 处理播放链接
     */
    protected suspend fun processPlayUrl(rawUrl: String): String {
        return try {
            when {
                rawUrl.startsWith("http") -> rawUrl
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("/") -> resolveUrl(siteUrl, rawUrl)
                else -> rawUrl
            }
        } catch (e: Exception) {
            logWarning("⚠️ 播放链接处理失败: $rawUrl", e)
            rawUrl
        }
    }
    
    /**
     * 🔧 处理图片链接
     */
    protected fun processImageUrl(rawUrl: String): String {
        return try {
            when {
                rawUrl.startsWith("http") -> rawUrl
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("/") -> resolveUrl(siteUrl, rawUrl)
                rawUrl.isEmpty() -> ""
                else -> resolveUrl(siteUrl, rawUrl)
            }
        } catch (e: Exception) {
            logWarning("⚠️ 图片链接处理失败: $rawUrl", e)
            rawUrl
        }
    }
    
    /**
     * 🔧 清理文本内容
     */
    protected fun cleanText(text: String): String {
        return text.trim()
            .replace("\\s+".toRegex(), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    }
    
    /**
     * 🔧 格式化播放列表
     */
    protected fun formatPlayList(episodes: List<Pair<String, String>>): String {
        return episodes.joinToString("#") { (name, url) ->
            "$name\$$url"
        }
    }
    
    /**
     * 🔧 格式化播放源
     */
    protected fun formatPlayFrom(sources: List<String>): String {
        return sources.joinToString("$$$")
    }
    
    /**
     * 🧹 清理缓存
     */
    protected fun clearCache() {
        requestCache.clear()
        logDebug("🧹 请求缓存已清理")
    }
    
    /**
     * 📊 获取缓存统计
     */
    protected fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cached_requests" to requestCache.size,
            "cache_keys" to requestCache.keys.take(10) // 只显示前10个键
        )
    }
    
    /**
     * 🔧 验证响应有效性
     */
    protected fun isValidResponse(response: String): Boolean {
        return response.isNotEmpty() && 
               !response.contains("404") && 
               !response.contains("error") &&
               !response.contains("Error")
    }
    
    /**
     * 🔧 处理错误响应
     */
    protected fun handleErrorResponse(error: String): String {
        logError("❌ 处理错误响应: $error")
        return buildErrorResponse(error)
    }
    
    override fun destroy() {
        super.destroy()
        clearCache()
        logDebug("✅ 专用解析器清理完成")
    }
}

/**
 * 专用解析器配置
 */
data class SpecializedConfig(
    val siteName: String = "",
    val baseUrl: String = "",
    val apiVersion: String = "1.0",
    val timeout: Int = 15,
    val retryCount: Int = 3,
    val enableCache: Boolean = true,
    val customHeaders: Map<String, String> = emptyMap(),
    val customParams: Map<String, String> = emptyMap(),
    val specialRules: Map<String, String> = emptyMap()
) {
    
    /**
     * 🔍 检查配置是否有效
     */
    fun isValid(): Boolean {
        return siteName.isNotEmpty() && baseUrl.isNotEmpty()
    }
    
    /**
     * 🔧 获取配置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "site_name" to siteName,
            "base_url" to baseUrl,
            "api_version" to apiVersion,
            "timeout" to timeout,
            "retry_count" to retryCount,
            "enable_cache" to enableCache,
            "custom_headers_count" to customHeaders.size,
            "custom_params_count" to customParams.size,
            "special_rules_count" to specialRules.size
        )
    }
}
