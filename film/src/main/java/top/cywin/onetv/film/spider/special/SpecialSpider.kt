package top.cywin.onetv.film.spider.special

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.network.EnhancedOkHttpManager
import top.cywin.onetv.film.utils.JsoupUtils
import top.cywin.onetv.film.utils.UrlUtils
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 特殊解析器基类
 * 
 * 基于 FongMi/TV 的特殊解析器架构
 * 为需要特殊处理的站点提供高级功能
 * 
 * 功能：
 * - 加密解密处理
 * - 特殊认证机制
 * - 复杂的数据处理
 * - 反爬虫处理
 * - 动态参数生成
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
abstract class SpecialSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPECIAL_SPIDER"
    }
    
    // HTTP 管理器
    protected val httpManager = EnhancedOkHttpManager()
    
    // JSON 解析器
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // 特殊配置
    protected var specialConfig: SpecialConfig? = null
    
    // 会话管理
    protected val sessionData = mutableMapOf<String, String>()
    
    // 动态参数缓存
    protected val dynamicParams = mutableMapOf<String, String>()
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        
        try {
            // 解析特殊配置
            specialConfig = parseSpecialConfig(extend)
            logDebug("✅ 特殊解析器配置解析成功")
        } catch (e: Exception) {
            logError("❌ 特殊解析器配置解析失败", e)
            specialConfig = createDefaultSpecialConfig()
        }
    }
    
    // ========== 抽象方法 ==========
    
    /**
     * 🔧 解析特殊配置
     */
    protected abstract fun parseSpecialConfig(extend: String): SpecialConfig
    
    /**
     * 🔧 创建默认特殊配置
     */
    protected abstract fun createDefaultSpecialConfig(): SpecialConfig
    
    /**
     * 🔐 获取认证信息
     */
    protected abstract suspend fun getAuthInfo(): Map<String, String>
    
    /**
     * 🔑 生成动态参数
     */
    protected abstract fun generateDynamicParams(): Map<String, String>
    
    // ========== 加密解密工具 ==========
    
    /**
     * 🔐 MD5 加密
     */
    protected fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logWarning("⚠️ MD5 加密失败", e)
            ""
        }
    }
    
    /**
     * 🔐 SHA256 加密
     */
    protected fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logWarning("⚠️ SHA256 加密失败", e)
            ""
        }
    }
    
    /**
     * 🔐 Base64 编码
     */
    protected fun base64Encode(input: String): String {
        return try {
            Base64.getEncoder().encodeToString(input.toByteArray())
        } catch (e: Exception) {
            logWarning("⚠️ Base64 编码失败", e)
            ""
        }
    }
    
    /**
     * 🔓 Base64 解码
     */
    protected fun base64Decode(input: String): String {
        return try {
            String(Base64.getDecoder().decode(input))
        } catch (e: Exception) {
            logWarning("⚠️ Base64 解码失败", e)
            ""
        }
    }
    
    /**
     * 🔐 AES 加密
     */
    protected fun aesEncrypt(data: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(encrypted)
        } catch (e: Exception) {
            logWarning("⚠️ AES 加密失败", e)
            ""
        }
    }
    
    /**
     * 🔓 AES 解密
     */
    protected fun aesDecrypt(encryptedData: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
            String(decrypted)
        } catch (e: Exception) {
            logWarning("⚠️ AES 解密失败", e)
            ""
        }
    }
    
    // ========== 特殊请求处理 ==========
    
    /**
     * 🌐 发送认证请求
     */
    protected suspend fun sendAuthRequest(url: String): String = withContext(Dispatchers.IO) {
        try {
            val authInfo = getAuthInfo()
            val headers = getSpecialHeaders() + authInfo + siteHeaders
            
            httpManager.getString(url, headers)
        } catch (e: Exception) {
            logError("❌ 认证请求失败", e)
            throw e
        }
    }
    
    /**
     * 🌐 发送签名请求
     */
    protected suspend fun sendSignedRequest(
        url: String,
        params: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        try {
            val dynamicParams = generateDynamicParams()
            val allParams = params + dynamicParams
            val signedUrl = signUrl(url, allParams)
            
            val headers = getSpecialHeaders() + siteHeaders
            httpManager.getString(signedUrl, headers)
        } catch (e: Exception) {
            logError("❌ 签名请求失败", e)
            throw e
        }
    }
    
    /**
     * 🔑 URL 签名
     */
    protected fun signUrl(url: String, params: Map<String, String>): String {
        return try {
            val sortedParams = params.toSortedMap()
            val paramString = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
            val signature = generateSignature(paramString)
            
            val finalParams = sortedParams + ("sign" to signature)
            UrlUtils.addParams(url, finalParams)
        } catch (e: Exception) {
            logWarning("⚠️ URL 签名失败", e)
            UrlUtils.addParams(url, params)
        }
    }
    
    /**
     * 🔑 生成签名
     */
    protected open fun generateSignature(data: String): String {
        val secretKey = specialConfig?.secretKey ?: "default_secret"
        return md5(data + secretKey)
    }
    
    /**
     * 🕐 生成时间戳
     */
    protected fun generateTimestamp(): String {
        return (System.currentTimeMillis() / 1000).toString()
    }
    
    /**
     * 🎲 生成随机字符串
     */
    protected fun generateNonce(length: Int = 16): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
    
    // ========== 数据处理工具 ==========
    
    /**
     * 🔍 深度解析 JSON
     */
    protected fun deepParseJson(response: String): JsonElement? {
        return try {
            // 尝试直接解析
            json.parseToJsonElement(response)
        } catch (e: Exception) {
            try {
                // 尝试提取 JSON 部分
                val jsonMatch = Regex("\\{.*\\}").find(response)
                if (jsonMatch != null) {
                    json.parseToJsonElement(jsonMatch.value)
                } else {
                    null
                }
            } catch (e2: Exception) {
                logWarning("⚠️ 深度 JSON 解析失败", e2)
                null
            }
        }
    }
    
    /**
     * 🔍 提取隐藏数据
     */
    protected fun extractHiddenData(html: String, pattern: String): String {
        return try {
            val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(html)
            match?.groupValues?.getOrNull(1) ?: ""
        } catch (e: Exception) {
            logWarning("⚠️ 隐藏数据提取失败", e)
            ""
        }
    }
    
    /**
     * 🔧 处理混淆数据
     */
    protected fun deobfuscateData(data: String): String {
        return try {
            // 常见的反混淆处理
            data.replace("\\x", "")
                .replace("\\u", "")
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        } catch (e: Exception) {
            logWarning("⚠️ 数据反混淆失败", e)
            data
        }
    }
    
    /**
     * 🔧 解码特殊字符
     */
    protected fun decodeSpecialChars(text: String): String {
        return try {
            text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&#34;", "\"")
        } catch (e: Exception) {
            logWarning("⚠️ 特殊字符解码失败", e)
            text
        }
    }
    
    // ========== 会话管理 ==========
    
    /**
     * 💾 保存会话数据
     */
    protected fun saveSessionData(key: String, value: String) {
        sessionData[key] = value
        logDebug("💾 保存会话数据: $key")
    }
    
    /**
     * 📖 获取会话数据
     */
    protected fun getSessionData(key: String): String? {
        return sessionData[key]
    }
    
    /**
     * 🧹 清理会话数据
     */
    protected fun clearSessionData() {
        sessionData.clear()
        logDebug("🧹 会话数据已清理")
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 🌐 获取特殊请求头
     */
    protected open fun getSpecialHeaders(): Map<String, String> {
        return specialConfig?.customHeaders ?: emptyMap()
    }
    
    /**
     * 🔧 构建特殊响应
     */
    protected fun buildSpecialResponse(block: JsonObjectBuilder.() -> Unit): String {
        return buildJsonResponse(block)
    }
    
    /**
     * 📊 获取特殊统计
     */
    protected fun getSpecialStats(): Map<String, Any> {
        return mapOf(
            "session_data_count" to sessionData.size,
            "dynamic_params_count" to dynamicParams.size,
            "config_valid" to (specialConfig?.isValid() ?: false)
        )
    }
    
    override fun destroy() {
        super.destroy()
        clearSessionData()
        dynamicParams.clear()
        logDebug("✅ 特殊解析器清理完成")
    }
}

/**
 * 特殊解析器配置
 */
data class SpecialConfig(
    val siteName: String = "",
    val secretKey: String = "",
    val apiVersion: String = "1.0",
    val encryptionEnabled: Boolean = false,
    val authRequired: Boolean = false,
    val signatureRequired: Boolean = false,
    val customHeaders: Map<String, String> = emptyMap(),
    val specialParams: Map<String, String> = emptyMap()
) {
    
    /**
     * 🔍 检查配置是否有效
     */
    fun isValid(): Boolean {
        return siteName.isNotEmpty()
    }
    
    /**
     * 🔧 获取配置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "site_name" to siteName,
            "api_version" to apiVersion,
            "encryption_enabled" to encryptionEnabled,
            "auth_required" to authRequired,
            "signature_required" to signatureRequired,
            "custom_headers_count" to customHeaders.size,
            "special_params_count" to specialParams.size
        )
    }
}
