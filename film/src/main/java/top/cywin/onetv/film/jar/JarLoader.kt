package top.cywin.onetv.film.jar

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import top.cywin.onetv.film.catvod.Spider
import java.io.*
import java.net.URL
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipEntry

/**
 * JAR 加载器
 * 
 * 基于 FongMi/TV 的 JAR 动态加载实现
 * 提供 JAR 包的下载、验证、加载和管理功能
 * 
 * 功能：
 * - JAR 包下载和缓存
 * - JAR 包验证和安全检查
 * - 动态类加载
 * - Spider 实例化
 * - 版本管理
 * - 依赖解析
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAR_LOADER"
        private const val JAR_CACHE_DIR = "jar_cache"
        private const val MAX_JAR_SIZE = 50 * 1024 * 1024L // 50MB
        private const val DOWNLOAD_TIMEOUT = 30000L // 30秒
    }
    
    // JAR 缓存目录
    private val jarCacheDir: File by lazy {
        File(context.cacheDir, JAR_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // 类加载器缓存
    private val classLoaderCache = ConcurrentHashMap<String, URLClassLoader>()
    
    // JAR 信息缓存
    private val jarInfoCache = ConcurrentHashMap<String, JarInfo>()
    
    // Spider 类缓存
    private val spiderClassCache = ConcurrentHashMap<String, Class<out Spider>>()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * 🔄 加载 JAR 包
     */
    suspend fun loadJar(jarUrl: String, forceReload: Boolean = false): JarLoadResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 开始加载 JAR: $jarUrl")
            
            val jarKey = generateJarKey(jarUrl)
            
            // 检查缓存
            if (!forceReload && jarInfoCache.containsKey(jarKey)) {
                val cachedInfo = jarInfoCache[jarKey]!!
                Log.d(TAG, "📦 从缓存获取 JAR: ${cachedInfo.name}")
                return@withContext JarLoadResult.success(cachedInfo)
            }
            
            // 下载 JAR 包
            val jarFile = downloadJar(jarUrl, jarKey)
            
            // 验证 JAR 包
            validateJar(jarFile)
            
            // 解析 JAR 信息
            val jarInfo = parseJarInfo(jarFile, jarUrl, jarKey)
            
            // 创建类加载器
            val classLoader = createClassLoader(jarFile)
            
            // 加载 Spider 类
            val spiderClasses = loadSpiderClasses(classLoader, jarInfo)
            
            // 缓存信息
            jarInfoCache[jarKey] = jarInfo
            classLoaderCache[jarKey] = classLoader
            spiderClasses.forEach { (className, clazz) ->
                spiderClassCache["$jarKey:$className"] = clazz
            }
            
            Log.d(TAG, "✅ JAR 加载成功: ${jarInfo.name}, Spider 数量: ${spiderClasses.size}")
            JarLoadResult.success(jarInfo)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 加载失败: $jarUrl", e)
            JarLoadResult.failure(e.message ?: "JAR 加载失败", e)
        }
    }
    
    /**
     * 📥 下载 JAR 包
     */
    private suspend fun downloadJar(jarUrl: String, jarKey: String): File = withContext(Dispatchers.IO) {
        val jarFile = File(jarCacheDir, "$jarKey.jar")
        
        // 如果文件已存在且有效，直接返回
        if (jarFile.exists() && jarFile.length() > 0) {
            Log.d(TAG, "📁 JAR 文件已存在: ${jarFile.name}")
            return@withContext jarFile
        }
        
        Log.d(TAG, "📥 开始下载 JAR: $jarUrl")
        
        val url = URL(jarUrl)
        val connection = url.openConnection().apply {
            connectTimeout = DOWNLOAD_TIMEOUT.toInt()
            readTimeout = DOWNLOAD_TIMEOUT.toInt()
            setRequestProperty("User-Agent", "OneTV-JarLoader/1.0")
        }
        
        val contentLength = connection.contentLengthLong
        if (contentLength > MAX_JAR_SIZE) {
            throw IllegalArgumentException("JAR 文件过大: ${contentLength / 1024 / 1024}MB > ${MAX_JAR_SIZE / 1024 / 1024}MB")
        }
        
        connection.getInputStream().use { input ->
            jarFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var totalBytes = 0L
                var bytesRead: Int
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    
                    if (totalBytes > MAX_JAR_SIZE) {
                        jarFile.delete()
                        throw IllegalArgumentException("JAR 文件下载超过大小限制")
                    }
                }
                
                Log.d(TAG, "✅ JAR 下载完成: ${totalBytes / 1024}KB")
            }
        }
        
        jarFile
    }
    
    /**
     * 🔍 验证 JAR 包
     */
    private fun validateJar(jarFile: File) {
        try {
            JarFile(jarFile).use { jar ->
                // 检查是否为有效的 JAR 文件
                val manifest = jar.manifest
                Log.d(TAG, "🔍 JAR 验证通过: ${jarFile.name}")
                
                // 基本安全检查
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    
                    // 检查路径遍历攻击
                    if (entry.name.contains("..") || entry.name.startsWith("/")) {
                        throw SecurityException("检测到不安全的 JAR 条目: ${entry.name}")
                    }
                    
                    // 检查文件大小
                    if (entry.size > MAX_JAR_SIZE) {
                        throw SecurityException("JAR 条目过大: ${entry.name}")
                    }
                }
            }
        } catch (e: Exception) {
            jarFile.delete()
            throw IllegalArgumentException("JAR 文件验证失败: ${e.message}", e)
        }
    }
    
    /**
     * 📋 解析 JAR 信息
     */
    private fun parseJarInfo(jarFile: File, jarUrl: String, jarKey: String): JarInfo {
        JarFile(jarFile).use { jar ->
            val manifest = jar.manifest
            val mainAttributes = manifest?.mainAttributes
            
            val name = mainAttributes?.getValue("Implementation-Title") 
                ?: mainAttributes?.getValue("Bundle-Name")
                ?: jarFile.nameWithoutExtension
            
            val version = mainAttributes?.getValue("Implementation-Version")
                ?: mainAttributes?.getValue("Bundle-Version")
                ?: "1.0.0"
            
            val vendor = mainAttributes?.getValue("Implementation-Vendor")
                ?: mainAttributes?.getValue("Bundle-Vendor")
                ?: "Unknown"
            
            val description = mainAttributes?.getValue("Bundle-Description") ?: ""
            
            // 扫描 Spider 类
            val spiderClasses = scanSpiderClasses(jar)
            
            return JarInfo(
                key = jarKey,
                name = name,
                version = version,
                vendor = vendor,
                description = description,
                url = jarUrl,
                filePath = jarFile.absolutePath,
                fileSize = jarFile.length(),
                spiderClasses = spiderClasses,
                loadTime = System.currentTimeMillis(),
                checksum = calculateChecksum(jarFile)
            )
        }
    }
    
    /**
     * 🔍 扫描 Spider 类
     */
    private fun scanSpiderClasses(jar: JarFile): List<String> {
        val spiderClasses = mutableListOf<String>()
        
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            
            if (entry.name.endsWith(".class") && !entry.name.contains("$")) {
                val className = entry.name.replace("/", ".").removeSuffix(".class")
                
                // 简单的类名过滤，实际应该通过字节码分析
                if (className.contains("Spider") || className.contains("spider")) {
                    spiderClasses.add(className)
                }
            }
        }
        
        return spiderClasses
    }
    
    /**
     * 🏗️ 创建类加载器
     */
    private fun createClassLoader(jarFile: File): URLClassLoader {
        val jarUrl = jarFile.toURI().toURL()
        return URLClassLoader(arrayOf(jarUrl), this::class.java.classLoader)
    }
    
    /**
     * 📚 加载 Spider 类
     */
    private fun loadSpiderClasses(classLoader: URLClassLoader, jarInfo: JarInfo): Map<String, Class<out Spider>> {
        val spiderClasses = mutableMapOf<String, Class<out Spider>>()
        
        jarInfo.spiderClasses.forEach { className ->
            try {
                val clazz = classLoader.loadClass(className)
                
                // 检查是否继承自 Spider
                if (Spider::class.java.isAssignableFrom(clazz)) {
                    @Suppress("UNCHECKED_CAST")
                    spiderClasses[className] = clazz as Class<out Spider>
                    Log.d(TAG, "📚 加载 Spider 类: $className")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 无法加载类: $className", e)
            }
        }
        
        return spiderClasses
    }
    
    /**
     * 🏭 创建 Spider 实例
     */
    fun createSpiderInstance(jarKey: String, className: String): Spider? {
        return try {
            val fullKey = "$jarKey:$className"
            val spiderClass = spiderClassCache[fullKey]
            
            if (spiderClass != null) {
                val instance = spiderClass.getDeclaredConstructor().newInstance()
                Log.d(TAG, "🏭 创建 Spider 实例: $className")
                instance
            } else {
                Log.w(TAG, "⚠️ 未找到 Spider 类: $fullKey")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 创建 Spider 实例失败: $className", e)
            null
        }
    }
    
    /**
     * 📋 获取已加载的 JAR 信息
     */
    fun getLoadedJars(): List<JarInfo> {
        return jarInfoCache.values.toList()
    }
    
    /**
     * 📋 获取 JAR 信息
     */
    fun getJarInfo(jarKey: String): JarInfo? {
        return jarInfoCache[jarKey]
    }
    
    /**
     * 🗑️ 卸载 JAR
     */
    fun unloadJar(jarKey: String): Boolean {
        return try {
            // 移除缓存
            jarInfoCache.remove(jarKey)
            classLoaderCache.remove(jarKey)?.close()
            
            // 移除 Spider 类缓存
            val keysToRemove = spiderClassCache.keys.filter { it.startsWith("$jarKey:") }
            keysToRemove.forEach { spiderClassCache.remove(it) }
            
            // 删除文件
            val jarFile = File(jarCacheDir, "$jarKey.jar")
            if (jarFile.exists()) {
                jarFile.delete()
            }
            
            Log.d(TAG, "🗑️ JAR 卸载成功: $jarKey")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 卸载失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        try {
            // 关闭所有类加载器
            classLoaderCache.values.forEach { it.close() }
            
            // 清空缓存
            jarInfoCache.clear()
            classLoaderCache.clear()
            spiderClassCache.clear()
            
            // 删除缓存文件
            jarCacheDir.listFiles()?.forEach { it.delete() }
            
            Log.d(TAG, "🧹 JAR 缓存已清理")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理缓存失败", e)
        }
    }
    
    /**
     * 🔑 生成 JAR 键
     */
    private fun generateJarKey(jarUrl: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(jarUrl.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            jarUrl.hashCode().toString()
        }
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
            ""
        }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): JarLoaderStats {
        return JarLoaderStats(
            loadedJars = jarInfoCache.size,
            cachedClasses = spiderClassCache.size,
            cacheSize = jarCacheDir.listFiles()?.sumOf { it.length() } ?: 0L,
            jarInfos = jarInfoCache.values.toList()
        )
    }
    
    /**
     * 🧹 销毁加载器
     */
    fun destroy() {
        scope.cancel()
        clearCache()
        Log.d(TAG, "🧹 JAR 加载器已销毁")
    }
}
