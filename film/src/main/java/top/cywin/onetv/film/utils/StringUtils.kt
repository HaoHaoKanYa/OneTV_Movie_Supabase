package top.cywin.onetv.film.utils

import android.util.Log
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets

/**
 * 字符串工具类
 * 
 * 基于 FongMi/TV 的字符串处理工具实现
 * 提供字符串处理、编码解码、加密解密等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object StringUtils {
    
    private const val TAG = "ONETV_FILM_STRING_UTILS"
    
    /**
     * 🔍 检查字符串是否为空或空白
     */
    fun isEmpty(str: String?): Boolean {
        return str.isNullOrBlank()
    }
    
    /**
     * 🔍 检查字符串是否不为空
     */
    fun isNotEmpty(str: String?): Boolean {
        return !isEmpty(str)
    }
    
    /**
     * 🔧 安全获取字符串，避免空指针
     */
    fun safe(str: String?): String {
        return str ?: ""
    }
    
    /**
     * ✂️ 截取字符串
     */
    fun substring(str: String?, start: Int, end: Int = -1): String {
        if (isEmpty(str)) return ""
        
        val safeStr = str!!
        val safeStart = maxOf(0, start)
        val safeEnd = if (end == -1) safeStr.length else minOf(safeStr.length, end)
        
        return if (safeStart < safeEnd) {
            safeStr.substring(safeStart, safeEnd)
        } else {
            ""
        }
    }
    
    /**
     * 🔍 查找字符串之间的内容
     */
    fun substringBetween(str: String?, start: String, end: String): String {
        if (isEmpty(str) || isEmpty(start) || isEmpty(end)) return ""
        
        val startIndex = str!!.indexOf(start)
        if (startIndex == -1) return ""
        
        val contentStart = startIndex + start.length
        val endIndex = str.indexOf(end, contentStart)
        if (endIndex == -1) return ""
        
        return str.substring(contentStart, endIndex)
    }
    
    /**
     * 🔍 查找所有匹配的内容
     */
    fun substringBetweenAll(str: String?, start: String, end: String): List<String> {
        if (isEmpty(str) || isEmpty(start) || isEmpty(end)) return emptyList()
        
        val results = mutableListOf<String>()
        var searchStart = 0
        
        while (true) {
            val startIndex = str!!.indexOf(start, searchStart)
            if (startIndex == -1) break
            
            val contentStart = startIndex + start.length
            val endIndex = str.indexOf(end, contentStart)
            if (endIndex == -1) break
            
            results.add(str.substring(contentStart, endIndex))
            searchStart = endIndex + end.length
        }
        
        return results
    }
    
    /**
     * 🔍 正则表达式查找
     */
    fun findByRegex(str: String?, pattern: String, group: Int = 1): String {
        if (isEmpty(str) || isEmpty(pattern)) return ""
        
        return try {
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(str!!)
            
            if (matcher.find() && group <= matcher.groupCount()) {
                matcher.group(group) ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 正则表达式查找失败: $pattern", e)
            ""
        }
    }
    
    /**
     * 🔍 正则表达式查找所有匹配
     */
    fun findAllByRegex(str: String?, pattern: String, group: Int = 1): List<String> {
        if (isEmpty(str) || isEmpty(pattern)) return emptyList()
        
        return try {
            val results = mutableListOf<String>()
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(str!!)
            
            while (matcher.find()) {
                if (group <= matcher.groupCount()) {
                    matcher.group(group)?.let { results.add(it) }
                }
            }
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "❌ 正则表达式查找失败: $pattern", e)
            emptyList()
        }
    }
    
    /**
     * 🔄 替换字符串
     */
    fun replace(str: String?, target: String, replacement: String): String {
        if (isEmpty(str) || isEmpty(target)) return safe(str)
        return str!!.replace(target, replacement)
    }
    
    /**
     * 🔄 正则表达式替换
     */
    fun replaceByRegex(str: String?, pattern: String, replacement: String): String {
        if (isEmpty(str) || isEmpty(pattern)) return safe(str)
        
        return try {
            str!!.replace(Regex(pattern), replacement)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 正则表达式替换失败: $pattern", e)
            safe(str)
        }
    }
    
    /**
     * 🧹 清理字符串（移除 HTML 标签和多余空白）
     */
    fun clean(str: String?): String {
        if (isEmpty(str)) return ""
        
        return str!!
            .replace(Regex("<[^>]*>"), "") // 移除 HTML 标签
            .replace(Regex("&[a-zA-Z]+;"), "") // 移除 HTML 实体
            .replace(Regex("\\s+"), " ") // 合并多个空白字符
            .trim()
    }
    
    /**
     * 🔗 URL 编码
     */
    fun urlEncode(str: String?, charset: String = "UTF-8"): String {
        if (isEmpty(str)) return ""
        
        return try {
            URLEncoder.encode(str!!, charset)
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 编码失败", e)
            safe(str)
        }
    }
    
    /**
     * 🔗 URL 解码
     */
    fun urlDecode(str: String?, charset: String = "UTF-8"): String {
        if (isEmpty(str)) return ""
        
        return try {
            URLDecoder.decode(str!!, charset)
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 解码失败", e)
            safe(str)
        }
    }
    
    /**
     * 🔐 Base64 编码
     */
    fun base64Encode(str: String?): String {
        if (isEmpty(str)) return ""
        
        return try {
            android.util.Base64.encodeToString(str!!.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64 编码失败", e)
            ""
        }
    }
    
    /**
     * 🔐 Base64 解码
     */
    fun base64Decode(str: String?): String {
        if (isEmpty(str)) return ""
        
        return try {
            val bytes = android.util.Base64.decode(str!!, android.util.Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64 解码失败", e)
            ""
        }
    }
    
    /**
     * 🔐 MD5 哈希
     */
    fun md5(str: String?): String {
        if (isEmpty(str)) return ""
        
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(str!!.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ MD5 哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 SHA256 哈希
     */
    fun sha256(str: String?): String {
        if (isEmpty(str)) return ""
        
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(str!!.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SHA256 哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 AES 加密
     */
    fun aesEncrypt(str: String?, key: String): String {
        if (isEmpty(str) || isEmpty(key)) return ""
        
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encrypted = cipher.doFinal(str!!.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES 加密失败", e)
            ""
        }
    }
    
    /**
     * 🔐 AES 解密
     */
    fun aesDecrypt(str: String?, key: String): String {
        if (isEmpty(str) || isEmpty(key)) return ""
        
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            
            val encrypted = android.util.Base64.decode(str!!, android.util.Base64.NO_WRAP)
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES 解密失败", e)
            ""
        }
    }
    
    /**
     * 📅 格式化时间戳
     */
    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            Log.e(TAG, "❌ 时间戳格式化失败", e)
            timestamp.toString()
        }
    }
    
    /**
     * 📅 解析时间字符串
     */
    fun parseTimestamp(timeStr: String?, pattern: String = "yyyy-MM-dd HH:mm:ss"): Long {
        if (isEmpty(timeStr)) return 0L
        
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(timeStr!!)?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "❌ 时间字符串解析失败", e)
            0L
        }
    }
    
    /**
     * 📏 格式化文件大小
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> String.format("%.1fKB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1fMB", bytes / 1024.0 / 1024.0)
            else -> String.format("%.1fGB", bytes / 1024.0 / 1024.0 / 1024.0)
        }
    }
    
    /**
     * ⏱️ 格式化持续时间
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d:%02d", minutes, seconds % 60)
            else -> "${seconds}s"
        }
    }
    
    /**
     * 🔍 检查是否为有效的 URL
     */
    fun isValidUrl(url: String?): Boolean {
        if (isEmpty(url)) return false
        
        return try {
            val uri = java.net.URI(url!!)
            uri.scheme in setOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔍 检查是否为有效的邮箱
     */
    fun isValidEmail(email: String?): Boolean {
        if (isEmpty(email)) return false
        
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return email!!.matches(Regex(emailPattern))
    }
    
    /**
     * 🔍 检查是否为数字
     */
    fun isNumeric(str: String?): Boolean {
        if (isEmpty(str)) return false
        
        return try {
            str!!.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * 🔢 安全转换为整数
     */
    fun toInt(str: String?, defaultValue: Int = 0): Int {
        if (isEmpty(str)) return defaultValue
        
        return try {
            str!!.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
    
    /**
     * 🔢 安全转换为长整数
     */
    fun toLong(str: String?, defaultValue: Long = 0L): Long {
        if (isEmpty(str)) return defaultValue
        
        return try {
            str!!.toLong()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
    
    /**
     * 🔢 安全转换为浮点数
     */
    fun toDouble(str: String?, defaultValue: Double = 0.0): Double {
        if (isEmpty(str)) return defaultValue
        
        return try {
            str!!.toDouble()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
    
    /**
     * 🎲 生成随机字符串
     */
    fun randomString(length: Int, chars: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"): String {
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
    
    /**
     * 🔍 计算字符串相似度
     */
    fun similarity(str1: String?, str2: String?): Double {
        if (str1 == str2) return 1.0
        if (isEmpty(str1) || isEmpty(str2)) return 0.0
        
        val s1 = str1!!.lowercase()
        val s2 = str2!!.lowercase()
        
        val maxLength = maxOf(s1.length, s2.length)
        if (maxLength == 0) return 1.0
        
        val distance = levenshteinDistance(s1, s2)
        return (maxLength - distance).toDouble() / maxLength
    }
    
    /**
     * 📏 计算编辑距离
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) {
            dp[i][0] = i
        }
        
        for (j in 0..str2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[str1.length][str2.length]
    }
}
