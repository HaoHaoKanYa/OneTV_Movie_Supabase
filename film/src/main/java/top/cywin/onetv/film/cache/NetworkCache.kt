package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 网络缓存
 * 
 * 基于 FongMi/TV 的网络请求缓存实现
 * 提供内存和磁盘双重缓存机制
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class NetworkCache private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_NETWORK_CACHE"
        private const val CACHE_DIR = "network_cache"
        private const val MAX_MEMORY_SIZE = 10 * 1024 * 1024L // 10MB
        private const val MAX_DISK_SIZE = 50 * 1024 * 1024L // 50MB
        
        @Volatile
        private var INSTANCE: NetworkCache? = null
        
        fun getInstance(context: Context): NetworkCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 缓存项数据类
    data class CacheItem(
        val key: String,
        val data: ByteArray,
        val timestamp: Long,
        val expireTime: Long,
        val size: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as CacheItem
            
            if (key != other.key) return false
            if (!data.contentEquals(other.data)) return false
            if (timestamp != other.timestamp) return false
            if (expireTime != other.expireTime) return false
            if (size != other.size) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + data.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + expireTime.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR)
    private val memoryCache = ConcurrentHashMap<String, CacheItem>()
    
    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // 清理过期缓存
        cleanupExpiredCache()
    }
    
    /**
     * 💾 存储缓存项
     */
    fun put(key: String, data: ByteArray, expireTimeMillis: Long = Long.MAX_VALUE) {
        try {
            val cacheItem = CacheItem(
                key = key,
                data = data,
                timestamp = System.currentTimeMillis(),
                expireTime = expireTimeMillis,
                size = data.size.toLong()
            )
            
            // 存储到内存缓存
            memoryCache[key] = cacheItem
            putCount.incrementAndGet()
            
            // 检查内存缓存大小
            checkMemoryCacheSize()
            
            // 异步存储到磁盘
            saveToDisk(cacheItem)
            
            Log.d(TAG, "缓存存储: $key, 大小=${data.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "缓存存储失败: $key", e)
        }
    }
    
    /**
     * 📖 获取缓存项
     */
    fun get(key: String): ByteArray? {
        return try {
            // 先从内存缓存获取
            var cacheItem = memoryCache[key]
            
            // 如果内存中没有，从磁盘加载
            if (cacheItem == null) {
                cacheItem = loadFromDisk(key)
                if (cacheItem != null) {
                    memoryCache[key] = cacheItem
                }
            }
            
            // 检查是否过期
            if (cacheItem != null && !isExpired(cacheItem)) {
                hitCount.incrementAndGet()
                Log.d(TAG, "缓存命中: $key")
                cacheItem.data
            } else {
                if (cacheItem != null) {
                    // 移除过期缓存
                    remove(key)
                }
                missCount.incrementAndGet()
                Log.d(TAG, "缓存未命中: $key")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "缓存获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * ❌ 移除缓存项
     */
    fun remove(key: String) {
        try {
            memoryCache.remove(key)
            
            val diskFile = File(cacheDir, key.hashCode().toString())
            if (diskFile.exists()) {
                diskFile.delete()
            }
            
            Log.d(TAG, "缓存移除: $key")
            
        } catch (e: Exception) {
            Log.e(TAG, "缓存移除失败: $key", e)
        }
    }
    
    /**
     * 🧹 清空所有缓存
     */
    fun evictAll() {
        try {
            memoryCache.clear()
            
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
            
            evictionCount.addAndGet(memoryCache.size.toLong())
            Log.d(TAG, "清空所有缓存")
            
        } catch (e: Exception) {
            Log.e(TAG, "清空缓存失败", e)
        }
    }
    
    /**
     * 📊 获取缓存大小
     */
    fun size(): Long {
        return try {
            memoryCache.values.sumOf { it.size }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 📊 获取命中次数
     */
    fun hitCount(): Long = hitCount.get()
    
    /**
     * 📊 获取未命中次数
     */
    fun missCount(): Long = missCount.get()
    
    /**
     * 📊 获取请求次数
     */
    fun requestCount(): Long = hitCount.get() + missCount.get()
    
    /**
     * 📊 获取放入次数
     */
    fun putCount(): Long = putCount.get()
    
    /**
     * 📊 获取驱逐次数
     */
    fun evictionCount(): Long = evictionCount.get()
    
    /**
     * 🔧 检查缓存项是否过期
     */
    private fun isExpired(cacheItem: CacheItem): Boolean {
        return System.currentTimeMillis() > cacheItem.expireTime
    }
    
    /**
     * 🔧 检查内存缓存大小
     */
    private fun checkMemoryCacheSize() {
        val currentSize = memoryCache.values.sumOf { it.size }
        if (currentSize > MAX_MEMORY_SIZE) {
            // 移除最旧的缓存项
            val oldestKey = memoryCache.entries
                .minByOrNull { it.value.timestamp }
                ?.key
            
            if (oldestKey != null) {
                memoryCache.remove(oldestKey)
                evictionCount.incrementAndGet()
                Log.d(TAG, "内存缓存驱逐: $oldestKey")
            }
        }
    }
    
    /**
     * 🔧 保存到磁盘
     */
    private fun saveToDisk(cacheItem: CacheItem) {
        try {
            val diskFile = File(cacheDir, cacheItem.key.hashCode().toString())
            diskFile.writeBytes(cacheItem.data)
        } catch (e: Exception) {
            Log.e(TAG, "磁盘保存失败: ${cacheItem.key}", e)
        }
    }
    
    /**
     * 🔧 从磁盘加载
     */
    private fun loadFromDisk(key: String): CacheItem? {
        return try {
            val diskFile = File(cacheDir, key.hashCode().toString())
            if (diskFile.exists()) {
                val data = diskFile.readBytes()
                CacheItem(
                    key = key,
                    data = data,
                    timestamp = diskFile.lastModified(),
                    expireTime = Long.MAX_VALUE, // 磁盘缓存默认不过期
                    size = data.size.toLong()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "磁盘加载失败: $key", e)
            null
        }
    }
    
    /**
     * 🔧 清理过期缓存
     */
    private fun cleanupExpiredCache() {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = memoryCache.entries
                .filter { currentTime > it.value.expireTime }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                remove(key)
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "清理过期缓存: ${expiredKeys.size}个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "清理过期缓存失败", e)
        }
    }
}
