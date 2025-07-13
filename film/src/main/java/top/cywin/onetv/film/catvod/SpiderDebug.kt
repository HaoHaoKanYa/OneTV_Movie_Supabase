package top.cywin.onetv.film.catvod

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Spider 调试工具
 * 
 * 基于 FongMi/TV 的调试机制实现
 * 提供 Spider 解析过程的详细调试信息
 * 
 * 功能：
 * - 解析过程跟踪
 * - 性能监控
 * - 错误诊断
 * - 日志记录
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object SpiderDebug {
    
    private const val TAG = "ONETV_FILM_SPIDER_DEBUG"
    
    // 调试开关
    var isDebugEnabled = true
    var isPerformanceMonitorEnabled = true
    var isDetailedLoggingEnabled = true
    
    // 性能统计
    private val performanceStats = mutableMapOf<String, MutableList<Long>>()
    private val errorStats = mutableMapOf<String, Int>()
    
    // 日期格式化
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 🐛 调试 Spider 操作
     */
    suspend fun debugSpiderOperation(
        spider: Spider,
        operation: String,
        params: Map<String, Any>,
        block: suspend () -> String
    ): String {
        if (!isDebugEnabled) {
            return block()
        }
        
        val startTime = System.currentTimeMillis()
        val spiderName = spider.javaClass.simpleName
        val operationKey = "${spiderName}_${operation}"
        
        return withContext(Dispatchers.IO) {
            try {
                logOperationStart(spiderName, operation, params)
                
                val result = block()
                
                val duration = System.currentTimeMillis() - startTime
                recordPerformance(operationKey, duration)
                
                logOperationSuccess(spiderName, operation, duration, result)
                
                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                recordError(operationKey)
                
                logOperationError(spiderName, operation, duration, e)
                
                // 返回错误响应
                spider.buildErrorResponse(e.message)
            }
        }
    }
    
    /**
     * 📊 记录性能数据
     */
    private fun recordPerformance(operationKey: String, duration: Long) {
        if (!isPerformanceMonitorEnabled) return
        
        performanceStats.getOrPut(operationKey) { mutableListOf() }.add(duration)
        
        // 保持最近100次记录
        val stats = performanceStats[operationKey]!!
        if (stats.size > 100) {
            stats.removeAt(0)
        }
    }
    
    /**
     * ❌ 记录错误统计
     */
    private fun recordError(operationKey: String) {
        errorStats[operationKey] = errorStats.getOrDefault(operationKey, 0) + 1
    }
    
    /**
     * 🚀 记录操作开始
     */
    private fun logOperationStart(spiderName: String, operation: String, params: Map<String, Any>) {
        if (!isDetailedLoggingEnabled) return
        
        val timestamp = dateFormat.format(Date())
        Log.d(TAG, "🚀 [$timestamp] $spiderName.$operation 开始")
        
        if (params.isNotEmpty()) {
            Log.d(TAG, "📋 参数: $params")
        }
    }
    
    /**
     * ✅ 记录操作成功
     */
    private fun logOperationSuccess(spiderName: String, operation: String, duration: Long, result: String) {
        val timestamp = dateFormat.format(Date())
        Log.d(TAG, "✅ [$timestamp] $spiderName.$operation 成功 (${duration}ms)")
        
        if (isDetailedLoggingEnabled) {
            // 解析结果统计
            try {
                val jsonResult = Json.parseToJsonElement(result).jsonObject
                val stats = analyzeJsonResult(jsonResult)
                Log.d(TAG, "📊 结果统计: $stats")
            } catch (e: Exception) {
                Log.d(TAG, "📊 结果长度: ${result.length} 字符")
            }
        }
        
        // 性能警告
        if (duration > 5000) {
            Log.w(TAG, "⚠️ 性能警告: $spiderName.$operation 耗时 ${duration}ms (>5秒)")
        }
    }
    
    /**
     * ❌ 记录操作错误
     */
    private fun logOperationError(spiderName: String, operation: String, duration: Long, error: Exception) {
        val timestamp = dateFormat.format(Date())
        Log.e(TAG, "❌ [$timestamp] $spiderName.$operation 失败 (${duration}ms)", error)
        
        // 错误分类
        val errorType = when (error) {
            is java.net.SocketTimeoutException -> "网络超时"
            is java.net.UnknownHostException -> "域名解析失败"
            is java.net.ConnectException -> "连接失败"
            is kotlinx.serialization.SerializationException -> "JSON解析失败"
            else -> "未知错误"
        }
        
        Log.e(TAG, "🏷️ 错误类型: $errorType")
        Log.e(TAG, "💬 错误信息: ${error.message}")
    }
    
    /**
     * 📊 分析 JSON 结果
     */
    private fun analyzeJsonResult(jsonObject: JsonObject): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        // 分析 list 数组
        jsonObject["list"]?.let { listElement ->
            try {
                val list = listElement.toString()
                val listSize = list.count { it == '{' } // 简单计算对象数量
                stats["list_count"] = listSize
            } catch (e: Exception) {
                stats["list_count"] = "解析失败"
            }
        }
        
        // 分析 class 数组
        jsonObject["class"]?.let { classElement ->
            try {
                val classList = classElement.toString()
                val classSize = classList.count { it == '{' }
                stats["class_count"] = classSize
            } catch (e: Exception) {
                stats["class_count"] = "解析失败"
            }
        }
        
        // 分析分页信息
        jsonObject["page"]?.let { stats["page"] = it.toString() }
        jsonObject["pagecount"]?.let { stats["pagecount"] = it.toString() }
        jsonObject["total"]?.let { stats["total"] = it.toString() }
        
        return stats
    }
    
    /**
     * 📈 获取性能统计
     */
    fun getPerformanceStats(): Map<String, Map<String, Any>> {
        return performanceStats.mapValues { (_, durations) ->
            if (durations.isEmpty()) {
                mapOf("count" to 0)
            } else {
                mapOf(
                    "count" to durations.size,
                    "avg" to durations.average().toLong(),
                    "min" to durations.minOrNull() ?: 0,
                    "max" to durations.maxOrNull() ?: 0,
                    "recent" to durations.takeLast(5)
                )
            }
        }
    }
    
    /**
     * ❌ 获取错误统计
     */
    fun getErrorStats(): Map<String, Int> {
        return errorStats.toMap()
    }
    
    /**
     * 📊 打印统计报告
     */
    fun printStatsReport() {
        if (!isDebugEnabled) return
        
        Log.i(TAG, "📊 ========== Spider 统计报告 ==========")
        
        // 性能统计
        Log.i(TAG, "⚡ 性能统计:")
        getPerformanceStats().forEach { (operation, stats) ->
            Log.i(TAG, "   $operation: $stats")
        }
        
        // 错误统计
        Log.i(TAG, "❌ 错误统计:")
        getErrorStats().forEach { (operation, count) ->
            Log.i(TAG, "   $operation: $count 次错误")
        }
        
        Log.i(TAG, "📊 ========== 报告结束 ==========")
    }
    
    /**
     * 🧹 清理统计数据
     */
    fun clearStats() {
        performanceStats.clear()
        errorStats.clear()
        Log.d(TAG, "🧹 统计数据已清理")
    }
    
    /**
     * 🔧 配置调试选项
     */
    fun configure(
        debugEnabled: Boolean = true,
        performanceMonitorEnabled: Boolean = true,
        detailedLoggingEnabled: Boolean = true
    ) {
        isDebugEnabled = debugEnabled
        isPerformanceMonitorEnabled = performanceMonitorEnabled
        isDetailedLoggingEnabled = detailedLoggingEnabled
        
        Log.i(TAG, "🔧 调试配置更新:")
        Log.i(TAG, "   调试开关: $debugEnabled")
        Log.i(TAG, "   性能监控: $performanceMonitorEnabled")
        Log.i(TAG, "   详细日志: $detailedLoggingEnabled")
    }
    
    /**
     * 🎯 测试 Spider
     */
    suspend fun testSpider(spider: Spider, testCases: List<TestCase>) {
        Log.i(TAG, "🎯 开始测试 Spider: ${spider.javaClass.simpleName}")
        
        testCases.forEach { testCase ->
            try {
                when (testCase.operation) {
                    "homeContent" -> {
                        debugSpiderOperation(spider, "homeContent", mapOf("filter" to testCase.params["filter"])) {
                            spider.homeContent(testCase.params["filter"] as? Boolean ?: false)
                        }
                    }
                    "categoryContent" -> {
                        debugSpiderOperation(spider, "categoryContent", testCase.params) {
                            spider.categoryContent(
                                testCase.params["tid"] as? String ?: "",
                                testCase.params["pg"] as? String ?: "1",
                                testCase.params["filter"] as? Boolean ?: false,
                                testCase.params["extend"] as? HashMap<String, String> ?: hashMapOf()
                            )
                        }
                    }
                    "searchContent" -> {
                        debugSpiderOperation(spider, "searchContent", testCase.params) {
                            spider.searchContent(
                                testCase.params["key"] as? String ?: "",
                                testCase.params["quick"] as? Boolean ?: false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🎯 测试用例失败: ${testCase.name}", e)
            }
        }
        
        Log.i(TAG, "🎯 Spider 测试完成")
        printStatsReport()
    }
    
    /**
     * 测试用例数据类
     */
    data class TestCase(
        val name: String,
        val operation: String,
        val params: Map<String, Any>
    )
}
