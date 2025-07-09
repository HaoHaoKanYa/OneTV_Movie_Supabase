package top.cywin.onetv.movie.data.cache

import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * movie模块专用缓存管理器 (三级缓存：内存 -> 磁盘)
 */
@Singleton
class MovieCacheManager @Inject constructor(
    private val context: Context
) {
    
    private val memoryCache = LruCache<String, Any>(50) // 内存缓存
    private val diskCacheDir = File(context.cacheDir, "movie_cache")
    
    init {
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }
    
    /**
     * 获取缓存 (二级缓存：内存 -> 磁盘)
     */
    suspend fun <T> getCache(key: String, clazz: Class<T>): T? {
        // 1. 内存缓存
        memoryCache.get(key)?.let { cached ->
            if (clazz.isInstance(cached)) {
                return clazz.cast(cached)
            }
        }
        
        // 2. 磁盘缓存
        val diskFile = File(diskCacheDir, key.hashCode().toString())
        if (diskFile.exists()) {
            try {
                val jsonData = diskFile.readText()
                val data = when (clazz) {
                    String::class.java -> jsonData.removeSurrounding("\"")
                    else -> Json.decodeFromString(Json.serializersModule.serializer(clazz), jsonData)
                }
                
                // 回写到内存缓存
                memoryCache.put(key, data as Any)
                return data as T
            } catch (e: Exception) {
                diskFile.delete()
            }
        }
        
        return null
    }
    
    /**
     * 存储缓存 (同时写入内存和磁盘缓存)
     */
    suspend fun <T> putCache(key: String, data: T, expireTimeMs: Long) {
        try {
            // 1. 内存缓存
            memoryCache.put(key, data as Any)
            
            // 2. 磁盘缓存 (异步写入)
            withContext(Dispatchers.IO) {
                val jsonData = Json.encodeToString(data)
                val diskFile = File(diskCacheDir, key.hashCode().toString())
                diskFile.writeText(jsonData)
                
                // 设置过期时间 (通过文件修改时间 + expireTimeMs)
                val expireTime = System.currentTimeMillis() + expireTimeMs
                diskFile.setLastModified(expireTime)
            }
        } catch (e: Exception) {
            Log.e("MovieCacheManager", "缓存存储失败: ${e.message}")
        }
    }
    
    /**
     * 清理过期缓存
     */
    suspend fun clearExpiredCache() {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            
            // 清理磁盘过期缓存
            diskCacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < currentTime) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return diskCacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * 清空所有缓存
     */
    suspend fun clearAllCache() {
        withContext(Dispatchers.IO) {
            memoryCache.evictAll()
            diskCacheDir.deleteRecursively()
            diskCacheDir.mkdirs()
        }
    }
    
    /**
     * 删除指定缓存
     */
    suspend fun removeCache(key: String) {
        withContext(Dispatchers.IO) {
            memoryCache.remove(key)
            val diskFile = File(diskCacheDir, key.hashCode().toString())
            if (diskFile.exists()) {
                diskFile.delete()
            }
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    suspend fun hasCache(key: String): Boolean {
        // 检查内存缓存
        if (memoryCache.get(key) != null) {
            return true
        }
        
        // 检查磁盘缓存
        val diskFile = File(diskCacheDir, key.hashCode().toString())
        return diskFile.exists() && diskFile.lastModified() > System.currentTimeMillis()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val memorySize = memoryCache.size()
        val diskSize = diskCacheDir.listFiles()?.size ?: 0
        val totalSize = getCacheSize()
        
        return CacheStats(
            memoryCount = memorySize,
            diskCount = diskSize,
            totalSizeBytes = totalSize
        )
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val memoryCount: Int,
    val diskCount: Int,
    val totalSizeBytes: Long
) {
    fun getTotalSizeMB(): String {
        return String.format("%.2f MB", totalSizeBytes / 1024.0 / 1024.0)
    }
}
