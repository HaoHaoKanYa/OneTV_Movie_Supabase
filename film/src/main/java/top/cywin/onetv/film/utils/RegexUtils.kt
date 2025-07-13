package top.cywin.onetv.film.utils

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 正则表达式工具类
 * 基于 FongMi/TV 标准实现
 * 
 * 提供常用的正则表达式匹配和处理功能
 * 
 * 功能：
 * - URL提取和验证
 * - 视频链接识别
 * - HTML标签处理
 * - 文本内容清理
 * - 数据格式验证
 * - 正则缓存优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object RegexUtils {
    
    private const val TAG = "ONETV_FILM_REGEX_UTILS"
    
    // 正则表达式缓存
    private val regexCache = ConcurrentHashMap<String, Regex>()
    
    // 常用正则表达式模式
    object Patterns {
        // URL相关
        const val URL = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
        const val DOMAIN = "(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+" +
                          "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
        const val IP_ADDRESS = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                               "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
        
        // 视频相关
        const val VIDEO_URL = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+\\." +
                             "(mp4|m3u8|flv|avi|mkv|mov|wmv|webm|ts|m4v|3gp|rmvb)"
        const val M3U8_URL = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+\\.m3u8[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*"
        const val MP4_URL = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+\\.mp4[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*"
        
        // HTML相关
        const val HTML_TAG = "<[^>]*>"
        const val HTML_SCRIPT = "<script[^>]*>.*?</script>"
        const val HTML_STYLE = "<style[^>]*>.*?</style>"
        const val HTML_COMMENT = "<!--.*?-->"
        
        // 文本清理
        const val WHITESPACE = "\\s+"
        const val CHINESE_CHARS = "[\\u4e00-\\u9fa5]+"
        const val ENGLISH_CHARS = "[a-zA-Z]+"
        const val NUMBERS = "\\d+"
        
        // 数据验证
        const val EMAIL = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        const val PHONE = "1[3-9]\\d{9}"
        const val ID_CARD = "[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]"
    }
    
    /**
     * 🔧 获取缓存的正则表达式
     */
    private fun getCachedRegex(pattern: String, flags: Set<RegexOption> = emptySet()): Regex {
        val cacheKey = "$pattern:${flags.hashCode()}"
        return regexCache.getOrPut(cacheKey) {
            try {
                pattern.toRegex(flags)
            } catch (e: Exception) {
                Log.e(TAG, "❌ 正则表达式编译失败: $pattern", e)
                throw e
            }
        }
    }
    
    /**
     * 🌐 提取所有URL
     */
    fun extractUrls(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.URL)
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 🎬 提取视频链接
     */
    fun extractVideoUrls(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.VIDEO_URL, setOf(RegexOption.IGNORE_CASE))
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 视频链接提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 📺 提取M3U8链接
     */
    fun extractM3u8Urls(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.M3U8_URL, setOf(RegexOption.IGNORE_CASE))
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ M3U8链接提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 🎥 提取MP4链接
     */
    fun extractMp4Urls(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.MP4_URL, setOf(RegexOption.IGNORE_CASE))
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ MP4链接提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 🌐 验证URL格式
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.URL)
            regex.matches(url)
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL验证失败: $url", e)
            false
        }
    }
    
    /**
     * 🌐 验证域名格式
     */
    fun isValidDomain(domain: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.DOMAIN)
            regex.matches(domain)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 域名验证失败: $domain", e)
            false
        }
    }
    
    /**
     * 🌐 验证IP地址格式
     */
    fun isValidIpAddress(ip: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.IP_ADDRESS)
            regex.matches(ip)
        } catch (e: Exception) {
            Log.e(TAG, "❌ IP地址验证失败: $ip", e)
            false
        }
    }
    
    /**
     * 🧹 移除HTML标签
     */
    fun removeHtmlTags(html: String): String {
        return try {
            val regex = getCachedRegex(Patterns.HTML_TAG)
            html.replace(regex, "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML标签移除失败", e)
            html
        }
    }
    
    /**
     * 🧹 移除HTML脚本
     */
    fun removeHtmlScripts(html: String): String {
        return try {
            val regex = getCachedRegex(Patterns.HTML_SCRIPT, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            html.replace(regex, "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML脚本移除失败", e)
            html
        }
    }
    
    /**
     * 🧹 移除HTML样式
     */
    fun removeHtmlStyles(html: String): String {
        return try {
            val regex = getCachedRegex(Patterns.HTML_STYLE, setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            html.replace(regex, "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML样式移除失败", e)
            html
        }
    }
    
    /**
     * 🧹 移除HTML注释
     */
    fun removeHtmlComments(html: String): String {
        return try {
            val regex = getCachedRegex(Patterns.HTML_COMMENT, setOf(RegexOption.DOT_MATCHES_ALL))
            html.replace(regex, "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML注释移除失败", e)
            html
        }
    }
    
    /**
     * 🧹 清理HTML内容
     */
    fun cleanHtml(html: String): String {
        return try {
            var cleaned = html
            cleaned = removeHtmlScripts(cleaned)
            cleaned = removeHtmlStyles(cleaned)
            cleaned = removeHtmlComments(cleaned)
            cleaned = removeHtmlTags(cleaned)
            cleaned = normalizeWhitespace(cleaned)
            cleaned.trim()
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTML清理失败", e)
            html
        }
    }
    
    /**
     * 🧹 规范化空白字符
     */
    fun normalizeWhitespace(text: String): String {
        return try {
            val regex = getCachedRegex(Patterns.WHITESPACE)
            text.replace(regex, " ")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 空白字符规范化失败", e)
            text
        }
    }
    
    /**
     * 🔍 提取中文字符
     */
    fun extractChineseChars(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.CHINESE_CHARS)
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 中文字符提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 🔍 提取英文字符
     */
    fun extractEnglishChars(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.ENGLISH_CHARS)
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 英文字符提取失败", e)
            emptyList()
        }
    }
    
    /**
     * 🔍 提取数字
     */
    fun extractNumbers(text: String): List<String> {
        return try {
            val regex = getCachedRegex(Patterns.NUMBERS)
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 数字提取失败", e)
            emptyList()
        }
    }
    
    /**
     * ✉️ 验证邮箱格式
     */
    fun isValidEmail(email: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.EMAIL)
            regex.matches(email)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 邮箱验证失败: $email", e)
            false
        }
    }
    
    /**
     * 📱 验证手机号格式
     */
    fun isValidPhone(phone: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.PHONE)
            regex.matches(phone)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 手机号验证失败: $phone", e)
            false
        }
    }
    
    /**
     * 🆔 验证身份证号格式
     */
    fun isValidIdCard(idCard: String): Boolean {
        return try {
            val regex = getCachedRegex(Patterns.ID_CARD)
            regex.matches(idCard)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 身份证号验证失败: $idCard", e)
            false
        }
    }
    
    /**
     * 🔍 自定义正则匹配
     */
    fun matches(text: String, pattern: String, flags: Set<RegexOption> = emptySet()): Boolean {
        return try {
            val regex = getCachedRegex(pattern, flags)
            regex.containsMatchIn(text)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 自定义正则匹配失败: $pattern", e)
            false
        }
    }
    
    /**
     * 🔍 自定义正则查找
     */
    fun findAll(text: String, pattern: String, flags: Set<RegexOption> = emptySet()): List<String> {
        return try {
            val regex = getCachedRegex(pattern, flags)
            regex.findAll(text).map { it.value }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 自定义正则查找失败: $pattern", e)
            emptyList()
        }
    }
    
    /**
     * 🔄 自定义正则替换
     */
    fun replace(text: String, pattern: String, replacement: String, flags: Set<RegexOption> = emptySet()): String {
        return try {
            val regex = getCachedRegex(pattern, flags)
            text.replace(regex, replacement)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 自定义正则替换失败: $pattern", e)
            text
        }
    }
    
    /**
     * 🔍 分组匹配
     */
    fun findGroups(text: String, pattern: String, flags: Set<RegexOption> = emptySet()): List<List<String>> {
        return try {
            val regex = getCachedRegex(pattern, flags)
            regex.findAll(text).map { matchResult ->
                matchResult.groupValues
            }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 分组匹配失败: $pattern", e)
            emptyList()
        }
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        regexCache.clear()
        Log.d(TAG, "🧹 正则表达式缓存已清理")
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cache_size" to regexCache.size,
            "cache_keys" to regexCache.keys.toList()
        )
    }
    
    /**
     * 🔧 预编译常用正则表达式
     */
    fun precompileCommonPatterns() {
        try {
            // 预编译常用模式
            getCachedRegex(Patterns.URL)
            getCachedRegex(Patterns.VIDEO_URL, setOf(RegexOption.IGNORE_CASE))
            getCachedRegex(Patterns.M3U8_URL, setOf(RegexOption.IGNORE_CASE))
            getCachedRegex(Patterns.MP4_URL, setOf(RegexOption.IGNORE_CASE))
            getCachedRegex(Patterns.HTML_TAG)
            getCachedRegex(Patterns.WHITESPACE)
            getCachedRegex(Patterns.EMAIL)
            getCachedRegex(Patterns.PHONE)
            
            Log.d(TAG, "🔧 常用正则表达式预编译完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 预编译正则表达式失败", e)
        }
    }
}
