package top.cywin.onetv.film.utils

import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

/**
 * 加密解密工具类
 * 基于 FongMi/TV 标准实现
 * 
 * 提供常用的加密解密和哈希功能
 * 
 * 功能：
 * - 哈希算法（MD5、SHA系列）
 * - 对称加密（AES、DES）
 * - Base64编解码
 * - HMAC签名验证
 * - 随机数生成
 * - 密钥管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object CryptoUtils {
    
    private const val TAG = "ONETV_FILM_CRYPTO_UTILS"
    
    // 算法常量
    object Algorithms {
        const val MD5 = "MD5"
        const val SHA1 = "SHA-1"
        const val SHA256 = "SHA-256"
        const val SHA512 = "SHA-512"
        const val AES = "AES"
        const val AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding"
        const val AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding"
        const val DES = "DES"
        const val DES_CBC_PKCS5 = "DES/CBC/PKCS5Padding"
        const val HMAC_SHA256 = "HmacSHA256"
        const val HMAC_SHA1 = "HmacSHA1"
    }
    
    // 默认密钥和IV
    private const val DEFAULT_AES_KEY = "OneTV2025FilmKey"
    private const val DEFAULT_IV = "OneTV2025FilmIV"
    
    /**
     * 🔐 MD5 哈希
     */
    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance(Algorithms.MD5)
            val digest = md.digest(input.toByteArray())
            bytesToHex(digest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ MD5哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 MD5 哈希（字节数组）
     */
    fun md5(input: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance(Algorithms.MD5)
            val digest = md.digest(input)
            bytesToHex(digest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ MD5哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 SHA-1 哈希
     */
    fun sha1(input: String): String {
        return try {
            val sha = MessageDigest.getInstance(Algorithms.SHA1)
            val digest = sha.digest(input.toByteArray())
            bytesToHex(digest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ SHA-1哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 SHA-256 哈希
     */
    fun sha256(input: String): String {
        return try {
            val sha = MessageDigest.getInstance(Algorithms.SHA256)
            val digest = sha.digest(input.toByteArray())
            bytesToHex(digest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ SHA-256哈希失败", e)
            ""
        }
    }
    
    /**
     * 🔐 SHA-512 哈希
     */
    fun sha512(input: String): String {
        return try {
            val sha = MessageDigest.getInstance(Algorithms.SHA512)
            val digest = sha.digest(input.toByteArray())
            bytesToHex(digest)
        } catch (e: Exception) {
            Log.e(TAG, "❌ SHA-512哈希失败", e)
            ""
        }
    }
    
    /**
     * 📝 Base64 编码
     */
    fun base64Encode(input: String): String {
        return try {
            Base64.getEncoder().encodeToString(input.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64编码失败", e)
            ""
        }
    }
    
    /**
     * 📝 Base64 编码（字节数组）
     */
    fun base64Encode(input: ByteArray): String {
        return try {
            Base64.getEncoder().encodeToString(input)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64编码失败", e)
            ""
        }
    }
    
    /**
     * 📝 Base64 解码
     */
    fun base64Decode(input: String): String {
        return try {
            String(Base64.getDecoder().decode(input))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64解码失败", e)
            ""
        }
    }
    
    /**
     * 📝 Base64 解码到字节数组
     */
    fun base64DecodeToBytes(input: String): ByteArray {
        return try {
            Base64.getDecoder().decode(input)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Base64解码失败", e)
            ByteArray(0)
        }
    }
    
    /**
     * 🔐 AES 加密（默认密钥）
     */
    fun aesEncrypt(data: String): String {
        return aesEncrypt(data, DEFAULT_AES_KEY)
    }
    
    /**
     * 🔐 AES 加密（自定义密钥）
     */
    fun aesEncrypt(data: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance(Algorithms.AES_ECB_PKCS5)
            val secretKey = SecretKeySpec(padKey(key, 16).toByteArray(), Algorithms.AES)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(data.toByteArray())
            base64Encode(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES加密失败", e)
            ""
        }
    }
    
    /**
     * 🔓 AES 解密（默认密钥）
     */
    fun aesDecrypt(encryptedData: String): String {
        return aesDecrypt(encryptedData, DEFAULT_AES_KEY)
    }
    
    /**
     * 🔓 AES 解密（自定义密钥）
     */
    fun aesDecrypt(encryptedData: String, key: String): String {
        return try {
            val cipher = Cipher.getInstance(Algorithms.AES_ECB_PKCS5)
            val secretKey = SecretKeySpec(padKey(key, 16).toByteArray(), Algorithms.AES)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decrypted = cipher.doFinal(base64DecodeToBytes(encryptedData))
            String(decrypted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES解密失败", e)
            ""
        }
    }
    
    /**
     * 🔐 AES-CBC 加密
     */
    fun aesEncryptCBC(data: String, key: String, iv: String = DEFAULT_IV): String {
        return try {
            val cipher = Cipher.getInstance(Algorithms.AES_CBC_PKCS5)
            val secretKey = SecretKeySpec(padKey(key, 16).toByteArray(), Algorithms.AES)
            val ivSpec = IvParameterSpec(padKey(iv, 16).toByteArray())
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(data.toByteArray())
            base64Encode(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES-CBC加密失败", e)
            ""
        }
    }
    
    /**
     * 🔓 AES-CBC 解密
     */
    fun aesDecryptCBC(encryptedData: String, key: String, iv: String = DEFAULT_IV): String {
        return try {
            val cipher = Cipher.getInstance(Algorithms.AES_CBC_PKCS5)
            val secretKey = SecretKeySpec(padKey(key, 16).toByteArray(), Algorithms.AES)
            val ivSpec = IvParameterSpec(padKey(iv, 16).toByteArray())
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decrypted = cipher.doFinal(base64DecodeToBytes(encryptedData))
            String(decrypted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES-CBC解密失败", e)
            ""
        }
    }
    
    /**
     * 🔐 HMAC-SHA256 签名
     */
    fun hmacSha256(data: String, key: String): String {
        return try {
            val mac = Mac.getInstance(Algorithms.HMAC_SHA256)
            val secretKey = SecretKeySpec(key.toByteArray(), Algorithms.HMAC_SHA256)
            mac.init(secretKey)
            val signature = mac.doFinal(data.toByteArray())
            bytesToHex(signature)
        } catch (e: Exception) {
            Log.e(TAG, "❌ HMAC-SHA256签名失败", e)
            ""
        }
    }
    
    /**
     * 🔐 HMAC-SHA1 签名
     */
    fun hmacSha1(data: String, key: String): String {
        return try {
            val mac = Mac.getInstance(Algorithms.HMAC_SHA1)
            val secretKey = SecretKeySpec(key.toByteArray(), Algorithms.HMAC_SHA1)
            mac.init(secretKey)
            val signature = mac.doFinal(data.toByteArray())
            bytesToHex(signature)
        } catch (e: Exception) {
            Log.e(TAG, "❌ HMAC-SHA1签名失败", e)
            ""
        }
    }
    
    /**
     * ✅ 验证HMAC-SHA256签名
     */
    fun verifyHmacSha256(data: String, key: String, signature: String): Boolean {
        return try {
            val computedSignature = hmacSha256(data, key)
            computedSignature.equals(signature, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ HMAC-SHA256验证失败", e)
            false
        }
    }
    
    /**
     * 🎲 生成随机字符串
     */
    fun generateRandomString(length: Int): String {
        return try {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = SecureRandom()
            (1..length)
                .map { chars[random.nextInt(chars.length)] }
                .joinToString("")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 随机字符串生成失败", e)
            ""
        }
    }
    
    /**
     * 🎲 生成随机字节数组
     */
    fun generateRandomBytes(length: Int): ByteArray {
        return try {
            val random = SecureRandom()
            val bytes = ByteArray(length)
            random.nextBytes(bytes)
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "❌ 随机字节生成失败", e)
            ByteArray(0)
        }
    }
    
    /**
     * 🔑 生成AES密钥
     */
    fun generateAESKey(): String {
        return try {
            val keyGenerator = KeyGenerator.getInstance(Algorithms.AES)
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            base64Encode(secretKey.encoded)
        } catch (e: Exception) {
            Log.e(TAG, "❌ AES密钥生成失败", e)
            ""
        }
    }
    
    /**
     * 🔀 XOR 加密/解密
     */
    fun xorCrypt(data: String, key: String): String {
        return try {
            val dataBytes = data.toByteArray()
            val keyBytes = key.toByteArray()
            val result = ByteArray(dataBytes.size)
            
            for (i in dataBytes.indices) {
                result[i] = dataBytes[i] xor keyBytes[i % keyBytes.size]
            }
            
            base64Encode(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ XOR加密失败", e)
            ""
        }
    }
    
    /**
     * 🔀 XOR 解密
     */
    fun xorDecrypt(encryptedData: String, key: String): String {
        return try {
            val dataBytes = base64DecodeToBytes(encryptedData)
            val keyBytes = key.toByteArray()
            val result = ByteArray(dataBytes.size)
            
            for (i in dataBytes.indices) {
                result[i] = dataBytes[i] xor keyBytes[i % keyBytes.size]
            }
            
            String(result)
        } catch (e: Exception) {
            Log.e(TAG, "❌ XOR解密失败", e)
            ""
        }
    }
    
    /**
     * 🔧 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 🔧 十六进制字符串转字节数组
     */
    fun hexToBytes(hex: String): ByteArray {
        return try {
            val cleanHex = hex.replace(" ", "").replace("-", "")
            ByteArray(cleanHex.length / 2) { i ->
                cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 十六进制转换失败", e)
            ByteArray(0)
        }
    }
    
    /**
     * 🔧 填充或截断密钥到指定长度
     */
    private fun padKey(key: String, length: Int): String {
        return when {
            key.length == length -> key
            key.length > length -> key.substring(0, length)
            else -> key.padEnd(length, '0')
        }
    }
    
    /**
     * 🔐 简单字符串混淆
     */
    fun obfuscateString(input: String): String {
        return try {
            val timestamp = System.currentTimeMillis().toString()
            val combined = "$input|$timestamp"
            val encrypted = aesEncrypt(combined)
            base64Encode(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 字符串混淆失败", e)
            input
        }
    }
    
    /**
     * 🔓 简单字符串反混淆
     */
    fun deobfuscateString(obfuscated: String): String {
        return try {
            val decoded = base64Decode(obfuscated)
            val decrypted = aesDecrypt(decoded)
            val parts = decrypted.split("|")
            if (parts.size >= 2) parts[0] else decrypted
        } catch (e: Exception) {
            Log.e(TAG, "❌ 字符串反混淆失败", e)
            obfuscated
        }
    }
    
    /**
     * 📊 获取支持的算法列表
     */
    fun getSupportedAlgorithms(): Map<String, List<String>> {
        return mapOf(
            "hash" to listOf(Algorithms.MD5, Algorithms.SHA1, Algorithms.SHA256, Algorithms.SHA512),
            "symmetric" to listOf(Algorithms.AES, Algorithms.DES),
            "mac" to listOf(Algorithms.HMAC_SHA256, Algorithms.HMAC_SHA1),
            "encoding" to listOf("Base64", "Hex")
        )
    }
}
