package top.cywin.onetv.film.spider.base

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.network.EnhancedOkHttpManager
import top.cywin.onetv.film.utils.StringUtils
import java.util.regex.Pattern

/**
 * Spider 类型检测器
 * 基于 FongMi/TV 标准实现
 * 
 * 智能检测 API 接口类型，自动选择合适的 Spider 解析器
 * 
 * 功能：
 * - URL 模式检测
 * - 内容类型检测
 * - 响应格式分析
 * - 智能类型推断
 * - 多重检测策略
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object SpiderTypeDetector {
    
    private const val TAG = "ONETV_FILM_SPIDER_TYPE_DETECTOR"
    
    // HTTP 管理器
    private val httpManager = EnhancedOkHttpManager()
    
    // URL 模式映射
    private val urlPatterns = mapOf(
        // JavaScript 文件
        "javascript" to listOf(
            Pattern.compile(".*\\.js$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.mjs$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/js/.*", Pattern.CASE_INSENSITIVE)
        ),
        
        // AppYs 接口
        "appys" to listOf(
            Pattern.compile(".*/api\\.php/provide/vod/.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*/api/.*\\?ac=.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*appys.*", Pattern.CASE_INSENSITIVE)
        ),
        
        // XPath 接口
        "xpath" to listOf(
            Pattern.compile(".*xpath.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.xml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.html$", Pattern.CASE_INSENSITIVE)
        ),
        
        // Drpy 接口
        "drpy" to listOf(
            Pattern.compile(".*drpy.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.py$", Pattern.CASE_INSENSITIVE)
        ),
        
        // 云盘接口
        "aliyundrive" to listOf(
            Pattern.compile(".*aliyundrive.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*alipan.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*ali.*drive.*", Pattern.CASE_INSENSITIVE)
        ),
        
        "baidudrive" to listOf(
            Pattern.compile(".*baidu.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*pan\\.baidu.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*bdpan.*", Pattern.CASE_INSENSITIVE)
        ),
        
        "quarkdrive" to listOf(
            Pattern.compile(".*quark.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*夸克.*", Pattern.CASE_INSENSITIVE)
        )
    )
    
    // 内容特征映射
    private val contentPatterns = mapOf(
        "javascript" to listOf(
            "function",
            "var ",
            "let ",
            "const ",
            "=>",
            "spider",
            "homeContent",
            "categoryContent"
        ),
        
        "appys" to listOf(
            "\"code\":",
            "\"msg\":",
            "\"page\":",
            "\"pagecount\":",
            "\"list\":",
            "\"vod_id\":",
            "\"vod_name\":"
        ),
        
        "xpath" to listOf(
            "<html",
            "<div",
            "<span",
            "<a href",
            "class=",
            "id=",
            "xpath"
        ),
        
        "xml" to listOf(
            "<?xml",
            "<rss",
            "<channel",
            "<item",
            "<title>",
            "<link>",
            "<description>"
        )
    )
    
    /**
     * 🔍 检测 Spider 类型
     * 
     * @param api API 地址
     * @param performContentCheck 是否执行内容检测
     * @return 检测到的类型，未知返回 "unknown"
     */
    suspend fun detectType(api: String, performContentCheck: Boolean = true): String {
        return try {
            Log.d(TAG, "🔍 开始检测 Spider 类型: $api")
            
            // 1. URL 模式检测
            val urlType = detectByUrlPattern(api)
            if (urlType != "unknown") {
                Log.d(TAG, "✅ URL 模式检测成功: $urlType")
                return urlType
            }
            
            // 2. 内容检测（可选）
            if (performContentCheck) {
                val contentType = detectByContent(api)
                if (contentType != "unknown") {
                    Log.d(TAG, "✅ 内容检测成功: $contentType")
                    return contentType
                }
            }
            
            // 3. 智能推断
            val inferredType = inferTypeFromApi(api)
            Log.d(TAG, "🤖 智能推断结果: $inferredType")
            
            inferredType
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 类型检测失败: $api", e)
            "unknown"
        }
    }
    
    /**
     * 🔍 通过 URL 模式检测类型
     */
    private fun detectByUrlPattern(api: String): String {
        for ((type, patterns) in urlPatterns) {
            for (pattern in patterns) {
                if (pattern.matcher(api).matches()) {
                    Log.d(TAG, "🎯 URL 模式匹配: $type")
                    return type
                }
            }
        }
        return "unknown"
    }
    
    /**
     * 🔍 通过内容检测类型
     */
    private suspend fun detectByContent(api: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📄 开始内容检测: $api")
            
            // 获取内容
            val content = httpManager.getString(api, mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))
            
            if (content.isBlank()) {
                Log.w(TAG, "⚠️ 内容为空")
                return@withContext "unknown"
            }
            
            // 分析内容特征
            val detectedType = analyzeContent(content)
            Log.d(TAG, "📊 内容分析结果: $detectedType")
            
            detectedType
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容检测失败: $api", e)
            "unknown"
        }
    }
    
    /**
     * 📊 分析内容特征
     */
    private fun analyzeContent(content: String): String {
        val lowerContent = content.lowercase()
        val scores = mutableMapOf<String, Int>()
        
        // 计算每种类型的匹配分数
        for ((type, keywords) in contentPatterns) {
            var score = 0
            for (keyword in keywords) {
                if (lowerContent.contains(keyword.lowercase())) {
                    score++
                }
            }
            scores[type] = score
        }
        
        // 找到最高分数的类型
        val bestMatch = scores.maxByOrNull { it.value }
        
        return if (bestMatch != null && bestMatch.value > 0) {
            Log.d(TAG, "🏆 最佳匹配: ${bestMatch.key} (分数: ${bestMatch.value})")
            bestMatch.key
        } else {
            "unknown"
        }
    }
    
    /**
     * 🤖 智能推断类型
     */
    private fun inferTypeFromApi(api: String): String {
        val lowerApi = api.lowercase()
        
        return when {
            // 特定站点推断
            lowerApi.contains("cokemv") -> "csp_Cokemv"
            lowerApi.contains("auete") -> "csp_Auete"
            lowerApi.contains("yydsys") -> "csp_YydsAli1"
            lowerApi.contains("thunder") -> "csp_Thunder"
            lowerApi.contains("tvbus") -> "csp_Tvbus"
            lowerApi.contains("jianpian") -> "csp_Jianpian"
            lowerApi.contains("forcetech") -> "csp_Forcetech"
            
            // 通用接口推断
            lowerApi.contains("api.php") -> "appys"
            lowerApi.contains("/vod/") -> "appys"
            lowerApi.contains("provide") -> "appys"
            
            // 文件类型推断
            lowerApi.endsWith(".js") -> "javascript"
            lowerApi.endsWith(".py") -> "drpy"
            lowerApi.endsWith(".xml") -> "xpath"
            lowerApi.endsWith(".html") -> "xpath"
            
            // 默认推断
            else -> "appys" // 默认使用 AppYs 解析器
        }
    }
    
    /**
     * 🔍 批量检测类型
     */
    suspend fun detectTypes(apis: List<String>, performContentCheck: Boolean = false): Map<String, String> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, String>()
            
            for (api in apis) {
                try {
                    val type = detectType(api, performContentCheck)
                    results[api] = type
                    Log.d(TAG, "✅ 批量检测: $api -> $type")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 批量检测失败: $api", e)
                    results[api] = "unknown"
                }
            }
            
            results
        }
    }
    
    /**
     * 📋 获取支持的类型列表
     */
    fun getSupportedTypes(): List<String> {
        return urlPatterns.keys.toList() + listOf(
            "csp_Cokemv", "csp_Auete", "csp_YydsAli1",
            "csp_Thunder", "csp_Tvbus", "csp_Jianpian", "csp_Forcetech",
            "csp_AliDrive", "csp_BaiduDrive", "csp_QuarkDrive"
        )
    }
    
    /**
     * 📊 获取检测统计
     */
    fun getDetectionStats(): Map<String, Any> {
        return mapOf(
            "url_patterns" to urlPatterns.size,
            "content_patterns" to contentPatterns.size,
            "supported_types" to getSupportedTypes().size
        )
    }
}
