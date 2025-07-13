package top.cywin.onetv.film.jar

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL
import java.security.MessageDigest
import java.util.jar.JarFile
import java.util.jar.Manifest
import java.util.zip.ZipEntry

/**
 * JAR 工具类
 * 
 * 基于 FongMi/TV 的 JAR 工具实现
 * 提供 JAR 包处理的通用工具方法
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object JarUtils {
    
    private const val TAG = "ONETV_FILM_JAR_UTILS"
    
    /**
     * 🔍 验证 JAR 文件
     */
    fun validateJarFile(jarFile: File): Boolean {
        return try {
            if (!jarFile.exists() || !jarFile.isFile) {
                return false
            }
            
            JarFile(jarFile).use { jar ->
                // 检查是否有 MANIFEST.MF
                val manifest = jar.manifest
                if (manifest == null) {
                    Log.w(TAG, "⚠️ JAR 文件缺少 MANIFEST.MF: ${jarFile.name}")
                }
                
                // 检查是否有类文件
                val entries = jar.entries()
                var hasClassFile = false
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class")) {
                        hasClassFile = true
                        break
                    }
                }
                
                if (!hasClassFile) {
                    Log.w(TAG, "⚠️ JAR 文件中没有找到类文件: ${jarFile.name}")
                }
                
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 文件验证失败: ${jarFile.name}", e)
            false
        }
    }
    
    /**
     * 📋 提取 JAR 清单信息
     */
    fun extractManifestInfo(jarFile: File): Map<String, String> {
        val manifestInfo = mutableMapOf<String, String>()
        
        try {
            JarFile(jarFile).use { jar ->
                val manifest = jar.manifest
                if (manifest != null) {
                    val attributes = manifest.mainAttributes
                    
                    // 提取常用属性
                    attributes.getValue("Implementation-Title")?.let {
                        manifestInfo["title"] = it
                    }
                    
                    attributes.getValue("Implementation-Version")?.let {
                        manifestInfo["version"] = it
                    }
                    
                    attributes.getValue("Implementation-Vendor")?.let {
                        manifestInfo["vendor"] = it
                    }
                    
                    attributes.getValue("Bundle-Name")?.let {
                        manifestInfo["bundle_name"] = it
                    }
                    
                    attributes.getValue("Bundle-Version")?.let {
                        manifestInfo["bundle_version"] = it
                    }
                    
                    attributes.getValue("Bundle-Vendor")?.let {
                        manifestInfo["bundle_vendor"] = it
                    }
                    
                    attributes.getValue("Bundle-Description")?.let {
                        manifestInfo["description"] = it
                    }
                    
                    attributes.getValue("Main-Class")?.let {
                        manifestInfo["main_class"] = it
                    }
                    
                    attributes.getValue("Class-Path")?.let {
                        manifestInfo["class_path"] = it
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 提取清单信息失败: ${jarFile.name}", e)
        }
        
        return manifestInfo
    }
    
    /**
     * 🔍 扫描 JAR 中的类
     */
    fun scanJarClasses(jarFile: File, packageFilter: String? = null): List<String> {
        val classes = mutableListOf<String>()
        
        try {
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    
                    if (entry.name.endsWith(".class") && !entry.name.contains("$")) {
                        val className = entry.name.replace("/", ".").removeSuffix(".class")
                        
                        // 应用包过滤器
                        if (packageFilter == null || className.startsWith(packageFilter)) {
                            classes.add(className)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 扫描 JAR 类失败: ${jarFile.name}", e)
        }
        
        return classes
    }
    
    /**
     * 🔍 查找 Spider 类
     */
    fun findSpiderClasses(jarFile: File): List<String> {
        val spiderClasses = mutableListOf<String>()
        
        try {
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    
                    if (entry.name.endsWith(".class") && !entry.name.contains("$")) {
                        val className = entry.name.replace("/", ".").removeSuffix(".class")
                        
                        // 检查类名是否包含 Spider
                        if (className.contains("Spider", ignoreCase = true)) {
                            spiderClasses.add(className)
                        } else {
                            // 检查类内容是否继承自 Spider
                            try {
                                val classBytes = jar.getInputStream(entry).readBytes()
                                if (isSpiderClass(classBytes)) {
                                    spiderClasses.add(className)
                                }
                            } catch (e: Exception) {
                                // 忽略单个类的检查错误
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 查找 Spider 类失败: ${jarFile.name}", e)
        }
        
        return spiderClasses
    }
    
    /**
     * 🔍 检查是否为 Spider 类
     */
    private fun isSpiderClass(classBytes: ByteArray): Boolean {
        return try {
            val classContent = String(classBytes, Charsets.ISO_8859_1)
            classContent.contains("top/cywin/onetv/film/catvod/Spider") ||
            classContent.contains("Spider") && classContent.contains("homeContent")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔐 计算 JAR 文件哈希
     */
    suspend fun calculateJarHash(jarFile: File, algorithm: String = "SHA-256"): String = withContext(Dispatchers.IO) {
        try {
            val md = MessageDigest.getInstance(algorithm)
            
            jarFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 计算 JAR 哈希失败: ${jarFile.name}", e)
            ""
        }
    }
    
    /**
     * 📏 获取 JAR 文件信息
     */
    fun getJarFileInfo(jarFile: File): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        
        try {
            info["name"] = jarFile.name
            info["size"] = jarFile.length()
            info["size_mb"] = String.format("%.2f", jarFile.length() / 1024.0 / 1024.0)
            info["last_modified"] = jarFile.lastModified()
            info["exists"] = jarFile.exists()
            info["readable"] = jarFile.canRead()
            
            if (jarFile.exists()) {
                JarFile(jarFile).use { jar ->
                    val entries = jar.entries()
                    var classCount = 0
                    var resourceCount = 0
                    
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.endsWith(".class")) {
                            classCount++
                        } else if (!entry.isDirectory) {
                            resourceCount++
                        }
                    }
                    
                    info["class_count"] = classCount
                    info["resource_count"] = resourceCount
                    info["total_entries"] = classCount + resourceCount
                }
                
                // 添加清单信息
                val manifestInfo = extractManifestInfo(jarFile)
                info["manifest"] = manifestInfo
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取 JAR 文件信息失败: ${jarFile.name}", e)
            info["error"] = e.message ?: "Unknown error"
        }
        
        return info
    }
    
    /**
     * 📥 下载 JAR 文件
     */
    suspend fun downloadJar(
        jarUrl: String,
        outputFile: File,
        timeout: Long = 30000L,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 开始下载 JAR: $jarUrl")
            
            val url = URL(jarUrl)
            val connection = url.openConnection()
            connection.connectTimeout = timeout.toInt()
            connection.readTimeout = timeout.toInt()
            connection.setRequestProperty("User-Agent", "OneTV-JarUtils/1.0")
            
            val contentLength = connection.contentLengthLong
            var downloadedBytes = 0L
            
            connection.getInputStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // 调用进度回调
                        progressCallback?.invoke(downloadedBytes, contentLength)
                    }
                }
            }
            
            Log.d(TAG, "✅ JAR 下载完成: ${outputFile.name}, 大小: ${downloadedBytes / 1024}KB")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 下载失败: $jarUrl", e)
            
            // 删除不完整的文件
            if (outputFile.exists()) {
                outputFile.delete()
            }
            
            false
        }
    }
    
    /**
     * 🔍 比较 JAR 文件
     */
    suspend fun compareJarFiles(jarFile1: File, jarFile2: File): JarComparisonResult = withContext(Dispatchers.IO) {
        try {
            if (!jarFile1.exists() || !jarFile2.exists()) {
                return@withContext JarComparisonResult(
                    identical = false,
                    sizeDifference = 0L,
                    hashMatch = false,
                    manifestMatch = false,
                    classCountDifference = 0,
                    differences = listOf("一个或多个文件不存在")
                )
            }
            
            val differences = mutableListOf<String>()
            
            // 比较文件大小
            val sizeDiff = jarFile2.length() - jarFile1.length()
            if (sizeDiff != 0L) {
                differences.add("文件大小不同: ${sizeDiff}字节")
            }
            
            // 比较哈希值
            val hash1 = calculateJarHash(jarFile1)
            val hash2 = calculateJarHash(jarFile2)
            val hashMatch = hash1 == hash2
            
            if (!hashMatch) {
                differences.add("文件哈希值不同")
            }
            
            // 比较清单信息
            val manifest1 = extractManifestInfo(jarFile1)
            val manifest2 = extractManifestInfo(jarFile2)
            val manifestMatch = manifest1 == manifest2
            
            if (!manifestMatch) {
                differences.add("清单信息不同")
            }
            
            // 比较类数量
            val classes1 = scanJarClasses(jarFile1)
            val classes2 = scanJarClasses(jarFile2)
            val classCountDiff = classes2.size - classes1.size
            
            if (classCountDiff != 0) {
                differences.add("类数量不同: ${classCountDiff}")
            }
            
            JarComparisonResult(
                identical = differences.isEmpty(),
                sizeDifference = sizeDiff,
                hashMatch = hashMatch,
                manifestMatch = manifestMatch,
                classCountDifference = classCountDiff,
                differences = differences
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 文件比较失败", e)
            JarComparisonResult(
                identical = false,
                sizeDifference = 0L,
                hashMatch = false,
                manifestMatch = false,
                classCountDifference = 0,
                differences = listOf("比较过程中发生错误: ${e.message}")
            )
        }
    }
    
    /**
     * 🧹 清理临时 JAR 文件
     */
    fun cleanupTempJars(tempDir: File, maxAge: Long = 24 * 60 * 60 * 1000L) {
        try {
            if (!tempDir.exists() || !tempDir.isDirectory) {
                return
            }
            
            val now = System.currentTimeMillis()
            var cleanedCount = 0
            
            tempDir.listFiles { file ->
                file.name.endsWith(".jar") && (now - file.lastModified()) > maxAge
            }?.forEach { file ->
                if (file.delete()) {
                    cleanedCount++
                    Log.d(TAG, "🗑️ 清理临时 JAR: ${file.name}")
                }
            }
            
            if (cleanedCount > 0) {
                Log.d(TAG, "🧹 清理完成，删除 $cleanedCount 个临时 JAR 文件")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理临时 JAR 文件失败", e)
        }
    }
    
    /**
     * 📊 获取 JAR 统计信息
     */
    fun getJarStats(jarFiles: List<File>): Map<String, Any> {
        var totalSize = 0L
        var totalClasses = 0
        var validJars = 0
        val vendors = mutableSetOf<String>()
        
        jarFiles.forEach { jarFile ->
            if (validateJarFile(jarFile)) {
                validJars++
                totalSize += jarFile.length()
                
                val classes = scanJarClasses(jarFile)
                totalClasses += classes.size
                
                val manifestInfo = extractManifestInfo(jarFile)
                manifestInfo["vendor"]?.let { vendors.add(it) }
                manifestInfo["bundle_vendor"]?.let { vendors.add(it) }
            }
        }
        
        return mapOf(
            "total_jars" to jarFiles.size,
            "valid_jars" to validJars,
            "invalid_jars" to (jarFiles.size - validJars),
            "total_size_bytes" to totalSize,
            "total_size_mb" to String.format("%.2f", totalSize / 1024.0 / 1024.0),
            "total_classes" to totalClasses,
            "average_classes_per_jar" to if (validJars > 0) totalClasses / validJars else 0,
            "unique_vendors" to vendors.size,
            "vendors" to vendors.toList()
        )
    }
}

/**
 * JAR 比较结果
 */
data class JarComparisonResult(
    val identical: Boolean,
    val sizeDifference: Long,
    val hashMatch: Boolean,
    val manifestMatch: Boolean,
    val classCountDifference: Int,
    val differences: List<String>
)
