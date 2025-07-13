package top.cywin.onetv.film.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 文件工具类
 * 
 * 基于 FongMi/TV 的文件处理工具实现
 * 提供文件操作、压缩解压、哈希计算等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object FileUtils {
    
    private const val TAG = "ONETV_FILM_FILE_UTILS"
    private const val BUFFER_SIZE = 8192
    
    /**
     * 🔍 检查文件是否存在
     */
    fun exists(filePath: String?): Boolean {
        if (StringUtils.isEmpty(filePath)) return false
        return File(filePath!!).exists()
    }
    
    /**
     * 🔍 检查文件是否存在
     */
    fun exists(file: File?): Boolean {
        return file?.exists() == true
    }
    
    /**
     * 📁 创建目录
     */
    fun createDir(dirPath: String): Boolean {
        if (StringUtils.isEmpty(dirPath)) return false
        
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 创建目录失败: $dirPath", e)
            false
        }
    }
    
    /**
     * 📁 创建目录
     */
    fun createDir(dir: File?): Boolean {
        if (dir == null) return false
        
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 创建目录失败: ${dir.absolutePath}", e)
            false
        }
    }
    
    /**
     * 🗑️ 删除文件
     */
    fun deleteFile(filePath: String): Boolean {
        if (StringUtils.isEmpty(filePath)) return false
        
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除文件失败: $filePath", e)
            false
        }
    }
    
    /**
     * 🗑️ 删除文件
     */
    fun deleteFile(file: File?): Boolean {
        if (file == null) return false
        
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除文件失败: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * 🗑️ 递归删除目录
     */
    fun deleteDir(dirPath: String): Boolean {
        if (StringUtils.isEmpty(dirPath)) return false
        return deleteDir(File(dirPath))
    }
    
    /**
     * 🗑️ 递归删除目录
     */
    fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        
        return try {
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        deleteDir(file)
                    } else {
                        file.delete()
                    }
                }
            }
            dir.delete()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除目录失败: ${dir.absolutePath}", e)
            false
        }
    }
    
    /**
     * 📄 读取文件内容为字符串
     */
    suspend fun readFileToString(filePath: String, charset: String = "UTF-8"): String = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(filePath)) return@withContext ""
        return@withContext readFileToString(File(filePath), charset)
    }
    
    /**
     * 📄 读取文件内容为字符串
     */
    suspend fun readFileToString(file: File?, charset: String = "UTF-8"): String = withContext(Dispatchers.IO) {
        if (file == null || !file.exists()) return@withContext ""
        
        return@withContext try {
            file.readText(charset(charset))
        } catch (e: Exception) {
            Log.e(TAG, "❌ 读取文件失败: ${file.absolutePath}", e)
            ""
        }
    }
    
    /**
     * 📄 读取文件内容为字节数组
     */
    suspend fun readFileToBytes(filePath: String): ByteArray = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(filePath)) return@withContext ByteArray(0)
        return@withContext readFileToBytes(File(filePath))
    }
    
    /**
     * 📄 读取文件内容为字节数组
     */
    suspend fun readFileToBytes(file: File?): ByteArray = withContext(Dispatchers.IO) {
        if (file == null || !file.exists()) return@withContext ByteArray(0)
        
        return@withContext try {
            file.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 读取文件失败: ${file.absolutePath}", e)
            ByteArray(0)
        }
    }
    
    /**
     * 💾 写入字符串到文件
     */
    suspend fun writeStringToFile(filePath: String, content: String, charset: String = "UTF-8"): Boolean = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(filePath)) return@withContext false
        return@withContext writeStringToFile(File(filePath), content, charset)
    }
    
    /**
     * 💾 写入字符串到文件
     */
    suspend fun writeStringToFile(file: File?, content: String, charset: String = "UTF-8"): Boolean = withContext(Dispatchers.IO) {
        if (file == null) return@withContext false
        
        return@withContext try {
            // 确保父目录存在
            file.parentFile?.let { createDir(it) }
            
            file.writeText(content, charset(charset))
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 写入文件失败: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * 💾 写入字节数组到文件
     */
    suspend fun writeBytesToFile(filePath: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(filePath)) return@withContext false
        return@withContext writeBytesToFile(File(filePath), bytes)
    }
    
    /**
     * 💾 写入字节数组到文件
     */
    suspend fun writeBytesToFile(file: File?, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (file == null) return@withContext false
        
        return@withContext try {
            // 确保父目录存在
            file.parentFile?.let { createDir(it) }
            
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 写入文件失败: ${file.absolutePath}", e)
            false
        }
    }
    
    /**
     * 📋 复制文件
     */
    suspend fun copyFile(srcPath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(srcPath) || StringUtils.isEmpty(destPath)) return@withContext false
        return@withContext copyFile(File(srcPath), File(destPath))
    }
    
    /**
     * 📋 复制文件
     */
    suspend fun copyFile(srcFile: File?, destFile: File?): Boolean = withContext(Dispatchers.IO) {
        if (srcFile == null || destFile == null || !srcFile.exists()) return@withContext false
        
        return@withContext try {
            // 确保目标目录存在
            destFile.parentFile?.let { createDir(it) }
            
            srcFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 复制文件失败: ${srcFile.absolutePath} -> ${destFile.absolutePath}", e)
            false
        }
    }
    
    /**
     * 🔄 移动文件
     */
    suspend fun moveFile(srcPath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(srcPath) || StringUtils.isEmpty(destPath)) return@withContext false
        return@withContext moveFile(File(srcPath), File(destPath))
    }
    
    /**
     * 🔄 移动文件
     */
    suspend fun moveFile(srcFile: File?, destFile: File?): Boolean = withContext(Dispatchers.IO) {
        if (srcFile == null || destFile == null || !srcFile.exists()) return@withContext false
        
        return@withContext try {
            // 确保目标目录存在
            destFile.parentFile?.let { createDir(it) }
            
            // 先复制再删除原文件
            if (copyFile(srcFile, destFile)) {
                deleteFile(srcFile)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 移动文件失败: ${srcFile.absolutePath} -> ${destFile.absolutePath}", e)
            false
        }
    }
    
    /**
     * 📏 获取文件大小
     */
    fun getFileSize(filePath: String): Long {
        if (StringUtils.isEmpty(filePath)) return 0L
        return getFileSize(File(filePath))
    }
    
    /**
     * 📏 获取文件大小
     */
    fun getFileSize(file: File?): Long {
        return try {
            if (file?.exists() == true) file.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取文件大小失败: ${file?.absolutePath}", e)
            0L
        }
    }
    
    /**
     * 📏 获取目录大小
     */
    fun getDirSize(dirPath: String): Long {
        if (StringUtils.isEmpty(dirPath)) return 0L
        return getDirSize(File(dirPath))
    }
    
    /**
     * 📏 获取目录大小
     */
    fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        
        return try {
            var size = 0L
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirSize(file)
                } else {
                    file.length()
                }
            }
            size
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取目录大小失败: ${dir.absolutePath}", e)
            0L
        }
    }
    
    /**
     * 📋 列出目录中的文件
     */
    fun listFiles(dirPath: String, extension: String? = null): List<File> {
        if (StringUtils.isEmpty(dirPath)) return emptyList()
        return listFiles(File(dirPath), extension)
    }
    
    /**
     * 📋 列出目录中的文件
     */
    fun listFiles(dir: File?, extension: String? = null): List<File> {
        if (dir == null || !dir.exists() || !dir.isDirectory) return emptyList()
        
        return try {
            val files = dir.listFiles() ?: return emptyList()
            
            if (extension != null) {
                files.filter { it.isFile && it.name.endsWith(extension, ignoreCase = true) }
            } else {
                files.filter { it.isFile }
            }.toList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ 列出文件失败: ${dir.absolutePath}", e)
            emptyList()
        }
    }
    
    /**
     * 🔐 计算文件哈希值
     */
    suspend fun calculateFileHash(filePath: String, algorithm: String = "MD5"): String = withContext(Dispatchers.IO) {
        if (StringUtils.isEmpty(filePath)) return@withContext ""
        return@withContext calculateFileHash(File(filePath), algorithm)
    }
    
    /**
     * 🔐 计算文件哈希值
     */
    suspend fun calculateFileHash(file: File?, algorithm: String = "MD5"): String = withContext(Dispatchers.IO) {
        if (file == null || !file.exists()) return@withContext ""
        
        return@withContext try {
            val md = MessageDigest.getInstance(algorithm)
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 计算文件哈希失败: ${file.absolutePath}", e)
            ""
        }
    }
    
    /**
     * 📦 压缩文件到 ZIP
     */
    suspend fun zipFiles(files: List<File>, zipFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // 确保目标目录存在
            zipFile.parentFile?.let { createDir(it) }
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                files.forEach { file ->
                    if (file.exists()) {
                        addFileToZip(file, file.name, zipOut)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 压缩文件失败: ${zipFile.absolutePath}", e)
            false
        }
    }
    
    /**
     * 📦 添加文件到 ZIP
     */
    private fun addFileToZip(file: File, entryName: String, zipOut: ZipOutputStream) {
        if (file.isDirectory) {
            val dirEntry = ZipEntry("$entryName/")
            zipOut.putNextEntry(dirEntry)
            zipOut.closeEntry()
            
            file.listFiles()?.forEach { childFile ->
                addFileToZip(childFile, "$entryName/${childFile.name}", zipOut)
            }
        } else {
            val entry = ZipEntry(entryName)
            zipOut.putNextEntry(entry)
            
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    zipOut.write(buffer, 0, bytesRead)
                }
            }
            
            zipOut.closeEntry()
        }
    }
    
    /**
     * 📦 解压 ZIP 文件
     */
    suspend fun unzipFile(zipFile: File, destDir: File): Boolean = withContext(Dispatchers.IO) {
        if (!zipFile.exists()) return@withContext false
        
        return@withContext try {
            // 确保目标目录存在
            createDir(destDir)
            
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                
                while (entry != null) {
                    val entryFile = File(destDir, entry.name)
                    
                    // 防止路径遍历攻击
                    if (!entryFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        Log.w(TAG, "⚠️ 检测到不安全的 ZIP 条目: ${entry.name}")
                        entry = zipIn.nextEntry
                        continue
                    }
                    
                    if (entry.isDirectory) {
                        createDir(entryFile)
                    } else {
                        // 确保父目录存在
                        entryFile.parentFile?.let { createDir(it) }
                        
                        FileOutputStream(entryFile).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解压文件失败: ${zipFile.absolutePath}", e)
            false
        }
    }
    
    /**
     * 📋 获取文件信息
     */
    fun getFileInfo(filePath: String): Map<String, Any> {
        if (StringUtils.isEmpty(filePath)) return emptyMap()
        return getFileInfo(File(filePath))
    }
    
    /**
     * 📋 获取文件信息
     */
    fun getFileInfo(file: File?): Map<String, Any> {
        if (file == null) return emptyMap()
        
        return try {
            mapOf(
                "name" to file.name,
                "path" to file.absolutePath,
                "exists" to file.exists(),
                "is_file" to file.isFile,
                "is_directory" to file.isDirectory,
                "size" to if (file.exists()) file.length() else 0L,
                "size_formatted" to StringUtils.formatFileSize(if (file.exists()) file.length() else 0L),
                "last_modified" to if (file.exists()) file.lastModified() else 0L,
                "last_modified_formatted" to if (file.exists()) {
                    StringUtils.formatTimestamp(file.lastModified())
                } else {
                    ""
                },
                "can_read" to file.canRead(),
                "can_write" to file.canWrite(),
                "can_execute" to file.canExecute(),
                "parent" to (file.parent ?: ""),
                "extension" to file.extension
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取文件信息失败: ${file.absolutePath}", e)
            emptyMap()
        }
    }
    
    /**
     * 🧹 清理临时文件
     */
    fun cleanTempFiles(context: Context, maxAge: Long = 24 * 60 * 60 * 1000L) {
        try {
            val tempDir = context.cacheDir
            val now = System.currentTimeMillis()
            var cleanedCount = 0
            
            tempDir.listFiles()?.forEach { file ->
                if ((now - file.lastModified()) > maxAge) {
                    if (file.isDirectory) {
                        if (deleteDir(file)) cleanedCount++
                    } else {
                        if (deleteFile(file)) cleanedCount++
                    }
                }
            }
            
            if (cleanedCount > 0) {
                Log.d(TAG, "🧹 清理完成，删除 $cleanedCount 个临时文件/目录")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理临时文件失败", e)
        }
    }
    
    /**
     * 🔍 查找文件
     */
    fun findFiles(
        searchDir: File,
        namePattern: String? = null,
        extension: String? = null,
        recursive: Boolean = true
    ): List<File> {
        if (!searchDir.exists() || !searchDir.isDirectory) return emptyList()
        
        val results = mutableListOf<File>()
        
        try {
            searchDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    var matches = true
                    
                    // 检查文件名模式
                    if (namePattern != null && !file.name.contains(namePattern, ignoreCase = true)) {
                        matches = false
                    }
                    
                    // 检查扩展名
                    if (extension != null && !file.name.endsWith(extension, ignoreCase = true)) {
                        matches = false
                    }
                    
                    if (matches) {
                        results.add(file)
                    }
                } else if (file.isDirectory && recursive) {
                    results.addAll(findFiles(file, namePattern, extension, recursive))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 查找文件失败: ${searchDir.absolutePath}", e)
        }
        
        return results
    }
}
