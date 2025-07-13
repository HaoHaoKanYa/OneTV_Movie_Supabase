package top.cywin.onetv.film.jar

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile
import java.util.zip.ZipEntry

/**
 * JAR 安全管理器
 * 
 * 基于 FongMi/TV 的 JAR 安全检查实现
 * 提供 JAR 包的安全验证、恶意代码检测和权限控制
 * 
 * 功能：
 * - JAR 包完整性验证
 * - 恶意代码检测
 * - 权限控制
 * - 白名单管理
 * - 安全策略配置
 * - 风险评估
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarSecurityManager {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAR_SECURITY_MANAGER"
        
        // 危险类名模式
        private val DANGEROUS_CLASS_PATTERNS = setOf(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.io.FileWriter",
            "java.io.FileOutputStream",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.lang.reflect.Method",
            "javax.script.ScriptEngine"
        )
        
        // 危险方法模式
        private val DANGEROUS_METHOD_PATTERNS = setOf(
            "exec",
            "getRuntime",
            "loadLibrary",
            "deleteOnExit",
            "setSecurityManager",
            "getProperty",
            "setProperty"
        )
        
        // 允许的域名
        private val TRUSTED_DOMAINS = setOf(
            "github.com",
            "gitee.com",
            "jsdelivr.net",
            "raw.githubusercontent.com"
        )
    }
    
    /**
     * 🔍 验证 JAR 包安全性
     */
    suspend fun validateJarSecurity(jarFile: File, jarUrl: String): JarSecurityInfo = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 开始 JAR 安全验证: ${jarFile.name}")
            
            // 计算校验和
            val checksum = calculateChecksum(jarFile)
            
            // 检查文件大小
            validateFileSize(jarFile)
            
            // 检查来源域名
            val trustedSource = validateSource(jarUrl)
            
            // 扫描 JAR 内容
            val scanResult = scanJarContent(jarFile)
            
            // 检查权限
            val permissions = extractPermissions(jarFile)
            
            // 综合评估
            val trusted = trustedSource && scanResult == SecurityScanResult.SAFE
            
            val securityInfo = JarSecurityInfo(
                checksum = checksum,
                signature = "", // 暂不实现数字签名验证
                trusted = trusted,
                permissions = permissions,
                scanResult = scanResult
            )
            
            Log.d(TAG, "✅ JAR 安全验证完成: ${if (trusted) "可信" else "不可信"}")
            securityInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 安全验证失败", e)
            JarSecurityInfo(
                checksum = "",
                signature = "",
                trusted = false,
                permissions = emptyList(),
                scanResult = SecurityScanResult.DANGEROUS
            )
        }
    }
    
    /**
     * 📏 验证文件大小
     */
    private fun validateFileSize(jarFile: File) {
        val maxSize = 50 * 1024 * 1024L // 50MB
        if (jarFile.length() > maxSize) {
            throw SecurityException("JAR 文件过大: ${jarFile.length() / 1024 / 1024}MB > ${maxSize / 1024 / 1024}MB")
        }
    }
    
    /**
     * 🌐 验证来源
     */
    private fun validateSource(jarUrl: String): Boolean {
        return try {
            val url = java.net.URL(jarUrl)
            val host = url.host.lowercase()
            
            // 检查是否为可信域名
            val trusted = TRUSTED_DOMAINS.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
            
            if (!trusted) {
                Log.w(TAG, "⚠️ 不可信的来源域名: $host")
            }
            
            trusted
        } catch (e: Exception) {
            Log.e(TAG, "❌ 验证来源失败: $jarUrl", e)
            false
        }
    }
    
    /**
     * 🔍 扫描 JAR 内容
     */
    private fun scanJarContent(jarFile: File): SecurityScanResult {
        return try {
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                var riskLevel = 0
                val suspiciousEntries = mutableListOf<String>()
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    
                    // 检查路径遍历
                    if (isPathTraversal(entry)) {
                        Log.w(TAG, "⚠️ 检测到路径遍历: ${entry.name}")
                        riskLevel += 10
                        suspiciousEntries.add(entry.name)
                    }
                    
                    // 检查可疑文件
                    if (isSuspiciousFile(entry)) {
                        Log.w(TAG, "⚠️ 检测到可疑文件: ${entry.name}")
                        riskLevel += 5
                        suspiciousEntries.add(entry.name)
                    }
                    
                    // 检查类文件
                    if (entry.name.endsWith(".class")) {
                        val classRisk = scanClassFile(jar, entry)
                        riskLevel += classRisk
                        if (classRisk > 0) {
                            suspiciousEntries.add(entry.name)
                        }
                    }
                }
                
                // 根据风险等级返回结果
                when {
                    riskLevel >= 20 -> {
                        Log.w(TAG, "❌ JAR 包被标记为危险，风险等级: $riskLevel")
                        SecurityScanResult.DANGEROUS
                    }
                    riskLevel >= 10 -> {
                        Log.w(TAG, "⚠️ JAR 包存在安全警告，风险等级: $riskLevel")
                        SecurityScanResult.WARNING
                    }
                    riskLevel > 0 -> {
                        Log.d(TAG, "ℹ️ JAR 包存在轻微风险，风险等级: $riskLevel")
                        SecurityScanResult.WARNING
                    }
                    else -> {
                        Log.d(TAG, "✅ JAR 包安全扫描通过")
                        SecurityScanResult.SAFE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 内容扫描失败", e)
            SecurityScanResult.UNKNOWN
        }
    }
    
    /**
     * 🔍 检查路径遍历
     */
    private fun isPathTraversal(entry: ZipEntry): Boolean {
        val name = entry.name
        return name.contains("..") || 
               name.startsWith("/") || 
               name.contains("\\") ||
               name.contains(":")
    }
    
    /**
     * 🔍 检查可疑文件
     */
    private fun isSuspiciousFile(entry: ZipEntry): Boolean {
        val name = entry.name.lowercase()
        
        // 检查可疑扩展名
        val suspiciousExtensions = setOf(
            ".exe", ".dll", ".so", ".dylib", ".bat", ".sh", ".cmd", ".ps1"
        )
        
        return suspiciousExtensions.any { name.endsWith(it) } ||
               name.contains("native") ||
               name.contains("jni") ||
               name.startsWith("META-INF/") && name.endsWith(".SF")
    }
    
    /**
     * 🔍 扫描类文件
     */
    private fun scanClassFile(jar: JarFile, entry: ZipEntry): Int {
        return try {
            jar.getInputStream(entry).use { inputStream ->
                val classBytes = inputStream.readBytes()
                scanClassBytes(classBytes, entry.name)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 扫描类文件失败: ${entry.name}", e)
            1 // 轻微风险
        }
    }
    
    /**
     * 🔍 扫描类字节码
     */
    private fun scanClassBytes(classBytes: ByteArray, className: String): Int {
        val classContent = String(classBytes, Charsets.ISO_8859_1)
        var riskLevel = 0
        
        // 检查危险类引用
        DANGEROUS_CLASS_PATTERNS.forEach { pattern ->
            if (classContent.contains(pattern)) {
                Log.w(TAG, "⚠️ 检测到危险类引用: $pattern in $className")
                riskLevel += 3
            }
        }
        
        // 检查危险方法调用
        DANGEROUS_METHOD_PATTERNS.forEach { pattern ->
            if (classContent.contains(pattern)) {
                Log.w(TAG, "⚠️ 检测到危险方法调用: $pattern in $className")
                riskLevel += 2
            }
        }
        
        // 检查反射使用
        if (classContent.contains("java/lang/reflect/") ||
            classContent.contains("getDeclaredMethod") ||
            classContent.contains("setAccessible")) {
            Log.w(TAG, "⚠️ 检测到反射使用: $className")
            riskLevel += 1
        }
        
        // 检查网络访问
        if (classContent.contains("java/net/") ||
            classContent.contains("HttpURLConnection") ||
            classContent.contains("Socket")) {
            Log.d(TAG, "ℹ️ 检测到网络访问: $className")
            // 网络访问是正常的，不增加风险等级
        }
        
        return riskLevel
    }
    
    /**
     * 🔑 提取权限信息
     */
    private fun extractPermissions(jarFile: File): List<String> {
        val permissions = mutableListOf<String>()
        
        try {
            JarFile(jarFile).use { jar ->
                val manifest = jar.manifest
                if (manifest != null) {
                    val attributes = manifest.mainAttributes
                    
                    // 检查权限相关的属性
                    attributes.getValue("Permissions")?.let { permissions.add("Permissions: $it") }
                    attributes.getValue("Trusted-Only")?.let { permissions.add("Trusted-Only: $it") }
                    attributes.getValue("Trusted-Library")?.let { permissions.add("Trusted-Library: $it") }
                }
                
                // 扫描类文件中的权限使用
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class")) {
                        try {
                            jar.getInputStream(entry).use { inputStream ->
                                val classBytes = inputStream.readBytes()
                                val classContent = String(classBytes, Charsets.ISO_8859_1)
                                
                                // 检查文件系统访问
                                if (classContent.contains("java/io/File") ||
                                    classContent.contains("FileInputStream") ||
                                    classContent.contains("FileOutputStream")) {
                                    permissions.add("文件系统访问")
                                }
                                
                                // 检查网络访问
                                if (classContent.contains("java/net/") ||
                                    classContent.contains("HttpURLConnection")) {
                                    permissions.add("网络访问")
                                }
                                
                                // 检查系统属性访问
                                if (classContent.contains("System.getProperty") ||
                                    classContent.contains("System.setProperty")) {
                                    permissions.add("系统属性访问")
                                }
                                
                                // 检查反射权限
                                if (classContent.contains("java/lang/reflect/")) {
                                    permissions.add("反射访问")
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略单个类文件的读取错误
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 提取权限信息失败", e)
        }
        
        return permissions.distinct()
    }
    
    /**
     * 🔐 计算文件校验和
     */
    private fun calculateChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 计算校验和失败", e)
            ""
        }
    }
    
    /**
     * ✅ 验证 JAR 包是否可信
     */
    fun isJarTrusted(jarFile: File, jarUrl: String): Boolean {
        return try {
            // 简化的可信验证
            val url = java.net.URL(jarUrl)
            val host = url.host.lowercase()
            
            TRUSTED_DOMAINS.any { domain ->
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔍 快速安全检查
     */
    fun quickSecurityCheck(jarFile: File): SecurityScanResult {
        return try {
            // 检查文件大小
            if (jarFile.length() > 50 * 1024 * 1024L) {
                return SecurityScanResult.WARNING
            }
            
            // 检查文件是否为有效的 JAR
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                var classCount = 0
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    
                    // 检查路径遍历
                    if (isPathTraversal(entry)) {
                        return SecurityScanResult.DANGEROUS
                    }
                    
                    // 统计类文件
                    if (entry.name.endsWith(".class")) {
                        classCount++
                    }
                    
                    // 检查可疑文件
                    if (isSuspiciousFile(entry)) {
                        return SecurityScanResult.WARNING
                    }
                }
                
                // 如果没有类文件，可能有问题
                if (classCount == 0) {
                    return SecurityScanResult.WARNING
                }
            }
            
            SecurityScanResult.SAFE
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 快速安全检查失败", e)
            SecurityScanResult.UNKNOWN
        }
    }
}
