package top.cywin.onetv.movie.data.spider

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.network.TvboxNetworkConfig
import java.io.File
import java.io.FileOutputStream
import java.net.URLClassLoader
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * JAR包管理器 - 用于下载、缓存和加载TVBOX Spider站点的JAR包
 * 支持动态类加载和方法调用
 */
class JarManager(private val context: Context) {
    
    private val httpClient = TvboxNetworkConfig.createTvboxHttpClient()
    private val jarCache = ConcurrentHashMap<String, JarInfo>()
    private val classLoaderCache = ConcurrentHashMap<String, URLClassLoader>()
    
    // JAR包缓存目录
    private val jarCacheDir = File(context.cacheDir, "spider_jars").apply {
        if (!exists()) mkdirs()
    }
    
    /**
     * JAR包信息
     */
    data class JarInfo(
        val url: String,
        val localPath: String,
        val md5: String,
        val downloadTime: Long,
        val isLoaded: Boolean = false
    )
    
    /**
     * 加载JAR包
     */
    suspend fun loadJar(site: VodSite): Result<URLClassLoader> = withContext(Dispatchers.IO) {
        try {
            if (site.jar.isBlank()) {
                return@withContext Result.failure(Exception("JAR包URL为空"))
            }
            
            Log.d("ONETV_MOVIE", "🔄 开始加载JAR包: ${site.jar}")
            
            // 检查缓存
            val cachedClassLoader = classLoaderCache[site.jar]
            if (cachedClassLoader != null) {
                Log.d("ONETV_MOVIE", "✅ 从缓存获取JAR包ClassLoader")
                return@withContext Result.success(cachedClassLoader)
            }
            
            // 下载JAR包
            val jarFile = downloadJar(site.jar)
            if (!jarFile.exists()) {
                return@withContext Result.failure(Exception("JAR包下载失败"))
            }
            
            // 创建ClassLoader
            val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()), this::class.java.classLoader)
            
            // 缓存ClassLoader
            classLoaderCache[site.jar] = classLoader
            
            Log.d("ONETV_MOVIE", "✅ JAR包加载成功: ${jarFile.name}")
            Result.success(classLoader)
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR包加载失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 下载JAR包
     */
    private suspend fun downloadJar(jarUrl: String): File = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🌐 开始下载JAR包: $jarUrl")
            
            // 生成本地文件名
            val fileName = generateFileName(jarUrl)
            val localFile = File(jarCacheDir, fileName)
            
            // 检查本地缓存
            val cachedJar = jarCache[jarUrl]
            if (cachedJar != null && File(cachedJar.localPath).exists()) {
                // 检查文件是否过期（24小时）
                val isExpired = System.currentTimeMillis() - cachedJar.downloadTime > 24 * 60 * 60 * 1000
                if (!isExpired) {
                    Log.d("ONETV_MOVIE", "✅ 使用缓存的JAR包: ${cachedJar.localPath}")
                    return@withContext File(cachedJar.localPath)
                }
            }
            
            // 下载JAR包
            val request = Request.Builder()
                .url(jarUrl)
                .header("User-Agent", "OneTV/2.1.1")
                .build()
            
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("下载失败: HTTP ${response.code}")
                }
                
                response.body?.let { body ->
                    FileOutputStream(localFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                    
                    // 计算MD5
                    val md5 = calculateMD5(localFile)
                    
                    // 更新缓存
                    jarCache[jarUrl] = JarInfo(
                        url = jarUrl,
                        localPath = localFile.absolutePath,
                        md5 = md5,
                        downloadTime = System.currentTimeMillis()
                    )
                    
                    Log.d("ONETV_MOVIE", "✅ JAR包下载完成: ${localFile.name}, 大小: ${localFile.length()} bytes")
                    localFile
                } ?: throw Exception("响应体为空")
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR包下载失败", e)
            throw e
        }
    }
    
    /**
     * 调用JAR包中的方法
     */
    suspend fun invokeMethod(
        site: VodSite,
        className: String,
        methodName: String,
        vararg params: Any
    ): Any? = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 调用JAR包方法: $className.$methodName")
            
            // 加载JAR包
            val classLoaderResult = loadJar(site)
            if (classLoaderResult.isFailure) {
                throw classLoaderResult.exceptionOrNull() ?: Exception("JAR包加载失败")
            }
            
            val classLoader = classLoaderResult.getOrThrow()
            
            // 加载类
            val clazz = classLoader.loadClass(className)
            val instance = clazz.getDeclaredConstructor().newInstance()
            
            // 获取方法
            val paramTypes = params.map { it::class.java }.toTypedArray()
            val method = clazz.getDeclaredMethod(methodName, *paramTypes)
            
            // 调用方法
            val result = method.invoke(instance, *params)
            
            Log.d("ONETV_MOVIE", "✅ JAR包方法调用成功")
            result
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR包方法调用失败", e)
            null
        }
    }
    
    /**
     * 获取JAR包中的Spider实例
     */
    suspend fun getSpiderInstance(site: VodSite): Any? = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 获取Spider实例")
            
            // 加载JAR包
            val classLoaderResult = loadJar(site)
            if (classLoaderResult.isFailure) {
                throw classLoaderResult.exceptionOrNull() ?: Exception("JAR包加载失败")
            }
            
            val classLoader = classLoaderResult.getOrThrow()
            
            // 尝试常见的Spider类名
            val possibleClassNames = listOf(
                "com.github.catvod.spider.Spider",
                "com.github.catvod.crawler.Spider",
                "Spider",
                "Main"
            )
            
            for (className in possibleClassNames) {
                try {
                    val clazz = classLoader.loadClass(className)
                    val instance = clazz.getDeclaredConstructor().newInstance()
                    Log.d("ONETV_MOVIE", "✅ 找到Spider类: $className")
                    return@withContext instance
                } catch (e: ClassNotFoundException) {
                    // 继续尝试下一个类名
                    continue
                }
            }
            
            Log.w("ONETV_MOVIE", "⚠️ 未找到Spider类")
            null
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取Spider实例失败", e)
            null
        }
    }
    
    /**
     * 生成文件名
     */
    private fun generateFileName(url: String): String {
        val md5 = url.hashCode().toString()
        return "spider_$md5.jar"
    }
    
    /**
     * 计算文件MD5
     */
    private fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 计算MD5失败", e)
            ""
        }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        try {
            // 清理ClassLoader缓存
            classLoaderCache.values.forEach { classLoader ->
                try {
                    classLoader.close()
                } catch (e: Exception) {
                    Log.w("ONETV_MOVIE", "关闭ClassLoader失败", e)
                }
            }
            classLoaderCache.clear()
            
            // 清理JAR文件缓存
            jarCache.clear()
            
            // 删除缓存文件
            jarCacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".jar")) {
                    file.delete()
                }
            }
            
            Log.d("ONETV_MOVIE", "🧹 JAR包缓存已清理")
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 清理JAR包缓存失败", e)
        }
    }
    
    /**
     * 获取缓存信息
     */
    fun getCacheInfo(): Map<String, JarInfo> {
        return jarCache.toMap()
    }
}
