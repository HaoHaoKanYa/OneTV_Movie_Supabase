package top.cywin.onetv.film.proxy

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.utils.StringUtils
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 代理规则
 * 基于 FongMi/TV 标准实现
 * 
 * 提供灵活的代理规则配置和管理功能
 * 
 * 功能：
 * - URL模式匹配
 * - 代理服务器配置
 * - 规则优先级管理
 * - 动态规则更新
 * - 规则统计监控
 * - 条件代理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
@Serializable
data class ProxyRule(
    val id: String,
    val name: String,
    val pattern: String,
    val target: String,
    val enabled: Boolean = true,
    val priority: Int = 100,
    val ruleType: ProxyRuleType = ProxyRuleType.URL_PATTERN,
    val conditions: List<ProxyCondition> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val timeout: Long = 30000L,
    val retryCount: Int = 3,
    val description: String = "",
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_RULE"
    }
    
    // 统计信息
    @kotlinx.serialization.Transient
    private val matchCount = AtomicLong(0)
    
    @kotlinx.serialization.Transient
    private val successCount = AtomicLong(0)
    
    @kotlinx.serialization.Transient
    private val failureCount = AtomicLong(0)
    
    /**
     * 🎯 检查URL是否匹配此规则
     */
    suspend fun matches(url: String, context: ProxyContext = ProxyContext()): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext false
        
        try {
            // 1. 基础模式匹配
            val basicMatch = when (ruleType) {
                ProxyRuleType.URL_PATTERN -> matchUrlPattern(url)
                ProxyRuleType.DOMAIN -> matchDomain(url)
                ProxyRuleType.PATH -> matchPath(url)
                ProxyRuleType.REGEX -> matchRegex(url)
                ProxyRuleType.EXACT -> matchExact(url)
            }
            
            if (!basicMatch) return@withContext false
            
            // 2. 条件匹配
            val conditionMatch = matchConditions(url, context)
            
            val result = basicMatch && conditionMatch
            
            if (result) {
                matchCount.incrementAndGet()
                Log.d(TAG, "🎯 规则匹配: $name -> $url")
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 规则匹配异常: $name", e)
            false
        }
    }
    
    /**
     * 🔧 应用代理规则
     */
    suspend fun apply(url: String, context: ProxyContext = ProxyContext()): ProxyResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔧 应用代理规则: $name")
            
            val processedUrl = processUrl(url)
            val processedHeaders = processHeaders(context.headers)
            
            successCount.incrementAndGet()
            
            ProxyResult(
                success = true,
                originalUrl = url,
                processedUrl = processedUrl,
                proxyServer = target,
                headers = processedHeaders,
                timeout = timeout,
                retryCount = retryCount,
                ruleId = id,
                ruleName = name
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理规则应用失败: $name", e)
            failureCount.incrementAndGet()
            
            ProxyResult(
                success = false,
                originalUrl = url,
                processedUrl = url,
                error = e.message ?: "代理规则应用失败",
                ruleId = id,
                ruleName = name
            )
        }
    }
    
    /**
     * 🎯 URL模式匹配
     */
    private fun matchUrlPattern(url: String): Boolean {
        return try {
            when {
                pattern.contains("*") -> {
                    // 通配符匹配
                    val regex = pattern.replace("*", ".*").toRegex()
                    regex.matches(url)
                }
                pattern.startsWith("http") -> {
                    // 前缀匹配
                    url.startsWith(pattern)
                }
                else -> {
                    // 包含匹配
                    url.contains(pattern)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ URL模式匹配失败: $pattern", e)
            false
        }
    }
    
    /**
     * 🌐 域名匹配
     */
    private fun matchDomain(url: String): Boolean {
        return try {
            val urlObj = URL(url)
            val domain = urlObj.host
            
            when {
                pattern.startsWith("*.") -> {
                    // 子域名匹配
                    val baseDomain = pattern.substring(2)
                    domain.endsWith(baseDomain)
                }
                else -> {
                    // 精确域名匹配
                    domain.equals(pattern, ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 域名匹配失败: $pattern", e)
            false
        }
    }
    
    /**
     * 📂 路径匹配
     */
    private fun matchPath(url: String): Boolean {
        return try {
            val urlObj = URL(url)
            val path = urlObj.path
            
            when {
                pattern.contains("*") -> {
                    val regex = pattern.replace("*", ".*").toRegex()
                    regex.matches(path)
                }
                pattern.startsWith("/") -> {
                    path.startsWith(pattern)
                }
                else -> {
                    path.contains(pattern)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 路径匹配失败: $pattern", e)
            false
        }
    }
    
    /**
     * 🔍 正则表达式匹配
     */
    private fun matchRegex(url: String): Boolean {
        return try {
            val regex = pattern.toRegex()
            regex.containsMatchIn(url)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 正则匹配失败: $pattern", e)
            false
        }
    }
    
    /**
     * 🎯 精确匹配
     */
    private fun matchExact(url: String): Boolean {
        return url == pattern
    }
    
    /**
     * ✅ 条件匹配
     */
    private suspend fun matchConditions(url: String, context: ProxyContext): Boolean = withContext(Dispatchers.IO) {
        if (conditions.isEmpty()) return@withContext true
        
        for (condition in conditions) {
            if (!condition.matches(url, context)) {
                return@withContext false
            }
        }
        
        true
    }
    
    /**
     * 🔧 处理URL
     */
    private fun processUrl(url: String): String {
        return try {
            when {
                target.startsWith("http") -> {
                    // 完整代理服务器URL
                    target
                }
                target.contains(":") -> {
                    // 代理服务器地址:端口
                    url // 保持原URL，由代理服务器处理
                }
                else -> {
                    // 简单替换
                    url.replace(pattern, target)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ URL处理失败: $url", e)
            url
        }
    }
    
    /**
     * 🔧 处理请求头
     */
    private fun processHeaders(originalHeaders: Map<String, String>): Map<String, String> {
        val processedHeaders = originalHeaders.toMutableMap()
        
        // 添加规则定义的请求头
        for ((key, value) in headers) {
            processedHeaders[key] = value
        }
        
        // 添加代理相关请求头
        if (target.startsWith("http")) {
            processedHeaders["X-Proxy-Rule"] = name
            processedHeaders["X-Proxy-Target"] = target
        }
        
        return processedHeaders
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val totalRequests = matchCount.get()
        val successRate = if (totalRequests > 0) {
            (successCount.get().toDouble() / totalRequests * 100)
        } else 0.0
        
        return mapOf(
            "rule_id" to id,
            "rule_name" to name,
            "match_count" to matchCount.get(),
            "success_count" to successCount.get(),
            "failure_count" to failureCount.get(),
            "success_rate" to successRate,
            "enabled" to enabled,
            "priority" to priority,
            "rule_type" to ruleType.name,
            "conditions_count" to conditions.size
        )
    }
    
    /**
     * 🔄 重置统计
     */
    fun resetStats() {
        matchCount.set(0)
        successCount.set(0)
        failureCount.set(0)
    }
    
    /**
     * 📋 复制规则
     */
    fun copy(
        id: String = this.id,
        name: String = this.name,
        pattern: String = this.pattern,
        target: String = this.target,
        enabled: Boolean = this.enabled,
        priority: Int = this.priority,
        ruleType: ProxyRuleType = this.ruleType,
        conditions: List<ProxyCondition> = this.conditions,
        headers: Map<String, String> = this.headers,
        timeout: Long = this.timeout,
        retryCount: Int = this.retryCount,
        description: String = this.description
    ): ProxyRule {
        return ProxyRule(
            id = id,
            name = name,
            pattern = pattern,
            target = target,
            enabled = enabled,
            priority = priority,
            ruleType = ruleType,
            conditions = conditions,
            headers = headers,
            timeout = timeout,
            retryCount = retryCount,
            description = description,
            createTime = this.createTime,
            updateTime = System.currentTimeMillis()
        )
    }
}

/**
 * 代理规则类型枚举
 */
@Serializable
enum class ProxyRuleType {
    URL_PATTERN,    // URL模式匹配
    DOMAIN,         // 域名匹配
    PATH,           // 路径匹配
    REGEX,          // 正则表达式匹配
    EXACT           // 精确匹配
}

/**
 * 代理条件
 */
@Serializable
data class ProxyCondition(
    val type: ProxyConditionType,
    val key: String,
    val value: String,
    val operator: ProxyOperator = ProxyOperator.EQUALS
) {
    
    /**
     * ✅ 检查条件是否匹配
     */
    suspend fun matches(url: String, context: ProxyContext): Boolean = withContext(Dispatchers.IO) {
        try {
            val actualValue = when (type) {
                ProxyConditionType.HEADER -> context.headers[key] ?: ""
                ProxyConditionType.PARAM -> extractUrlParam(url, key)
                ProxyConditionType.TIME -> System.currentTimeMillis().toString()
                ProxyConditionType.USER_AGENT -> context.headers["User-Agent"] ?: ""
                ProxyConditionType.REFERER -> context.headers["Referer"] ?: ""
            }
            
            when (operator) {
                ProxyOperator.EQUALS -> actualValue == value
                ProxyOperator.NOT_EQUALS -> actualValue != value
                ProxyOperator.CONTAINS -> actualValue.contains(value)
                ProxyOperator.NOT_CONTAINS -> !actualValue.contains(value)
                ProxyOperator.STARTS_WITH -> actualValue.startsWith(value)
                ProxyOperator.ENDS_WITH -> actualValue.endsWith(value)
                ProxyOperator.REGEX -> value.toRegex().containsMatchIn(actualValue)
                ProxyOperator.GREATER_THAN -> actualValue.toLongOrNull()?.let { it > value.toLongOrNull() ?: 0 } ?: false
                ProxyOperator.LESS_THAN -> actualValue.toLongOrNull()?.let { it < value.toLongOrNull() ?: 0 } ?: false
            }
            
        } catch (e: Exception) {
            Log.w("ProxyCondition", "⚠️ 条件匹配失败: $type.$key", e)
            false
        }
    }
    
    /**
     * 🔧 提取URL参数
     */
    private fun extractUrlParam(url: String, paramName: String): String {
        return try {
            val urlObj = URL(url)
            val query = urlObj.query ?: return ""
            
            query.split("&")
                .find { it.startsWith("$paramName=") }
                ?.substringAfter("=") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * 代理条件类型枚举
 */
@Serializable
enum class ProxyConditionType {
    HEADER,         // 请求头条件
    PARAM,          // URL参数条件
    TIME,           // 时间条件
    USER_AGENT,     // User-Agent条件
    REFERER         // Referer条件
}

/**
 * 代理操作符枚举
 */
@Serializable
enum class ProxyOperator {
    EQUALS,         // 等于
    NOT_EQUALS,     // 不等于
    CONTAINS,       // 包含
    NOT_CONTAINS,   // 不包含
    STARTS_WITH,    // 开始于
    ENDS_WITH,      // 结束于
    REGEX,          // 正则匹配
    GREATER_THAN,   // 大于
    LESS_THAN       // 小于
}

/**
 * 代理上下文
 */
data class ProxyContext(
    val headers: Map<String, String> = emptyMap(),
    val userAgent: String = "",
    val referer: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 代理结果
 */
data class ProxyResult(
    val success: Boolean,
    val originalUrl: String,
    val processedUrl: String,
    val proxyServer: String = "",
    val headers: Map<String, String> = emptyMap(),
    val timeout: Long = 30000L,
    val retryCount: Int = 3,
    val error: String = "",
    val ruleId: String = "",
    val ruleName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
