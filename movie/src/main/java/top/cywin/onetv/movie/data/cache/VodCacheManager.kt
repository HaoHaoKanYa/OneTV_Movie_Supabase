package top.cywin.onetv.movie.data.cache

import android.content.Context
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import top.cywin.onetv.movie.data.models.VodConfig
import top.cywin.onetv.movie.data.models.VodItem
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 三级缓存管理器 (参考OneMoVie缓存架构)
 * 1. 内存缓存 (LruCache) - 最快访问
 * 2. 磁盘缓存 (File) - 持久化存储
 * 3. 数据库缓存 (Room) - 结构化数据
 */
@Singleton
class VodCacheManager @Inject constructor(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 协程作用域用于后台任务
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 1. 内存缓存 (LruCache) - 增加容量
    private val memoryCache = LruCache<String, CacheEntry>(200) // 增加到200个对象

    // 2. 磁盘缓存目录
    private val diskCacheDir = File(context.cacheDir, "movie_cache").apply {
        if (!exists()) mkdirs()
    }

    // 3. 配置缓存目录
    private val configCacheDir = File(context.filesDir, "movie_config").apply {
        if (!exists()) mkdirs()
    }

    // 4. 压缩缓存目录
    private val compressedCacheDir = File(context.cacheDir, "movie_cache_compressed").apply {
        if (!exists()) mkdirs()
    }

    // 5. 预加载缓存目录
    private val preloadCacheDir = File(context.cacheDir, "movie_preload").apply {
        if (!exists()) mkdirs()
    }

    // 缓存统计
    private var cacheHits = 0L
    private var cacheMisses = 0L
    private var lastCleanupTime = System.currentTimeMillis()

    init {
        // 启动后台清理任务
        startBackgroundCleanup()
    }

    /**
     * 启动后台清理任务
     */
    private fun startBackgroundCleanup() {
        cacheScope.launch {
            while (true) {
                try {
                    // 每小时执行一次清理
                    kotlinx.coroutines.delay(60 * 60 * 1000)

                    // 清理过期缓存
                    clearExpired()

                    // 检查缓存大小，如果超过限制则清理
                    val cacheSize = getCacheSize()
                    val maxCacheSize = 100 * 1024 * 1024 // 100MB

                    if (cacheSize > maxCacheSize) {
                        smartCleanup()
                    }

                    lastCleanupTime = System.currentTimeMillis()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 缓存条目
     */
    private data class CacheEntry(
        val data: Any,
        val timestamp: Long,
        val expireTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }

    /**
     * 通用缓存获取 (增加统计功能)
     */
    fun <T> getCache(key: String, clazz: Class<T>): T? {
        // 1. 先从内存缓存获取
        val memoryEntry = memoryCache.get(key)
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            cacheHits++
            return if (clazz.isInstance(memoryEntry.data)) clazz.cast(memoryEntry.data) else null
        }

        // 2. 从磁盘缓存获取
        val diskResult = getDiskCache(key, clazz)
        if (diskResult != null) {
            cacheHits++
            // 将热点数据提升到内存缓存
            val expireTime = System.currentTimeMillis() + 60 * 60 * 1000 // 1小时
            memoryCache.put(key, CacheEntry(diskResult as Any, System.currentTimeMillis(), expireTime))
        } else {
            cacheMisses++
        }

        return diskResult
    }

    /**
     * 通用缓存存储
     */
    fun <T> putCache(key: String, data: T, expireTimeMs: Long) {
        val expireTime = System.currentTimeMillis() + expireTimeMs
        
        // 1. 存储到内存缓存
        memoryCache.put(key, CacheEntry(data as Any, System.currentTimeMillis(), expireTime))
        
        // 2. 存储到磁盘缓存
        try {
            val dataJson = json.encodeToString(data)
            putDiskCache(key, dataJson, expireTime)
        } catch (e: Exception) {
            // 序列化失败时忽略磁盘缓存
        }
    }

    /**
     * 磁盘缓存获取
     */
    private fun <T> getDiskCache(key: String, clazz: Class<T>): T? {
        return try {
            val file = File(diskCacheDir, hashKey(key))
            if (!file.exists()) return null

            val content = file.readText()
            val cacheData = json.decodeFromString<DiskCacheData>(content)
            
            if (cacheData.isExpired()) {
                file.delete()
                return null
            }

            when (clazz) {
                String::class.java -> cacheData.data.removeSurrounding("\"") as T
                else -> json.decodeFromString<Any>(cacheData.data) as T
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 磁盘缓存存储
     */
    private fun putDiskCache(key: String, dataJson: String, expireTime: Long) {
        try {
            val file = File(diskCacheDir, hashKey(key))
            val cacheData = DiskCacheData(dataJson, expireTime)
            val content = json.encodeToString(DiskCacheData.serializer(), cacheData)
            
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 磁盘缓存数据结构
     */
    @kotlinx.serialization.Serializable
    private data class DiskCacheData(
        val data: String,
        val expireTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }

    /**
     * 配置缓存 - 特殊处理
     */
    fun saveConfig(config: VodConfig) {
        try {
            val configFile = File(configCacheDir, "current_config.json")
            val configJson = json.encodeToString(VodConfig.serializer(), config)
            val cacheData = DiskCacheData(configJson, System.currentTimeMillis() + 24 * 60 * 60 * 1000) // 24小时
            val content = json.encodeToString(DiskCacheData.serializer(), cacheData)
            
            configFile.writeText(content)
            
            // 同时存储到内存
            memoryCache.put("current_config", CacheEntry(config, System.currentTimeMillis(), cacheData.expireTime))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取配置缓存
     */
    fun getConfig(): VodConfig? {
        // 1. 先从内存获取
        val memoryEntry = memoryCache.get("current_config")
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            return memoryEntry.data as? VodConfig
        }

        // 2. 从磁盘获取
        return try {
            val configFile = File(configCacheDir, "current_config.json")
            if (!configFile.exists()) return null

            val content = configFile.readText()
            val cacheData = json.decodeFromString<DiskCacheData>(content)
            
            if (cacheData.isExpired()) {
                configFile.delete()
                return null
            }

            json.decodeFromString<VodConfig>(cacheData.data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        // 1. 清除内存缓存
        memoryCache.evictAll()
        
        // 2. 清除磁盘缓存
        diskCacheDir.listFiles()?.forEach { it.delete() }
        
        // 3. 清除配置缓存
        configCacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpired() = withContext(Dispatchers.IO) {
        // 1. 清除过期的磁盘缓存
        diskCacheDir.listFiles()?.forEach { file ->
            try {
                val content = file.readText()
                val cacheData = json.decodeFromString<DiskCacheData>(content)
                if (cacheData.isExpired()) {
                    file.delete()
                }
            } catch (e: Exception) {
                file.delete() // 损坏的文件直接删除
            }
        }
        
        // 2. 清除过期的配置缓存
        configCacheDir.listFiles()?.forEach { file ->
            try {
                val content = file.readText()
                val cacheData = json.decodeFromString<DiskCacheData>(content)
                if (cacheData.isExpired()) {
                    file.delete()
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
    }

    /**
     * 获取缓存大小
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        var size = 0L
        
        // 磁盘缓存大小
        diskCacheDir.listFiles()?.forEach { file ->
            size += file.length()
        }
        
        // 配置缓存大小
        configCacheDir.listFiles()?.forEach { file ->
            size += file.length()
        }
        
        size
    }

    /**
     * 删除指定缓存
     */
    fun removeCache(key: String) {
        // 1. 从内存删除
        memoryCache.remove(key)
        
        // 2. 从磁盘删除
        val file = File(diskCacheDir, hashKey(key))
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * 生成缓存键的哈希值
     */
    private fun hashKey(key: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(key.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            key.replace("[^a-zA-Z0-9]".toRegex(), "_")
        }
    }

    /**
     * 预加载内容 (智能预加载)
     */
    suspend fun preloadContent(items: List<VodItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            val cacheKey = "preload_${item.vodId}_${item.siteKey}"
            if (!hasCache(cacheKey)) {
                try {
                    putCache(cacheKey, item, 24 * 60 * 60 * 1000) // 24小时
                } catch (e: Exception) {
                    // 预加载失败不影响主流程
                }
            }
        }
    }

    /**
     * 智能清理缓存
     */
    suspend fun smartCleanup() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. 清理过期缓存
        clearExpired()

        // 2. 如果缓存大小超过限制，清理最少使用的缓存
        val maxCacheSize = 100 * 1024 * 1024L // 100MB
        val currentSize = getCacheSize()

        if (currentSize > maxCacheSize) {
            cleanupLeastUsed()
        }

        // 3. 压缩大文件
        compressLargeFiles()

        lastCleanupTime = now
    }

    /**
     * 压缩大文件
     */
    private suspend fun compressLargeFiles() = withContext(Dispatchers.IO) {
        diskCacheDir.listFiles()?.forEach { file ->
            if (file.length() > 1024 * 1024) { // 大于1MB的文件
                try {
                    val compressedFile = File(compressedCacheDir, "${file.name}.gz")
                    if (!compressedFile.exists()) {
                        compressFile(file, compressedFile)
                        file.delete() // 删除原文件
                    }
                } catch (e: Exception) {
                    // 压缩失败不影响主流程
                }
            }
        }
    }

    /**
     * 压缩文件
     */
    private fun compressFile(source: File, target: File) {
        source.inputStream().use { input ->
            GZIPOutputStream(target.outputStream()).use { gzipOut ->
                input.copyTo(gzipOut)
            }
        }
    }

    /**
     * 解压文件
     */
    private fun decompressFile(source: File, target: File) {
        GZIPInputStream(source.inputStream()).use { gzipIn ->
            target.outputStream().use { output ->
                gzipIn.copyTo(output)
            }
        }
    }

    /**
     * 清理最少使用的缓存
     */
    private suspend fun cleanupLeastUsed() = withContext(Dispatchers.IO) {
        val files = diskCacheDir.listFiles()?.sortedBy { it.lastModified() }
        val filesToDelete = files?.take(files.size / 4) // 删除25%最旧的文件

        filesToDelete?.forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // 删除失败不影响主流程
            }
        }
    }



    /**
     * 检查是否有缓存
     */
    private fun hasCache(key: String): Boolean {
        // 检查内存缓存
        val memoryEntry = memoryCache.get(key)
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            return true
        }

        // 检查磁盘缓存
        val file = File(diskCacheDir, hashKey(key))
        return file.exists()
    }

    /**
     * 获取缓存命中率
     */
    fun getCacheHitRate(): Double {
        val total = cacheHits + cacheMisses
        return if (total > 0) cacheHits.toDouble() / total else 0.0
    }

    /**
     * 获取详细缓存统计信息
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "memory_cache_size" to memoryCache.size(),
            "memory_cache_max_size" to memoryCache.maxSize(),
            "disk_cache_files" to (diskCacheDir.listFiles()?.size ?: 0),
            "config_cache_files" to (configCacheDir.listFiles()?.size ?: 0),
            "compressed_cache_files" to (compressedCacheDir.listFiles()?.size ?: 0),
            "preload_cache_files" to (preloadCacheDir.listFiles()?.size ?: 0),
            "cache_hits" to cacheHits,
            "cache_misses" to cacheMisses,
            "cache_hit_rate" to getCacheHitRate(),
            "last_cleanup_time" to lastCleanupTime
        )
    }

    /**
     * 手动触发缓存优化
     */
    suspend fun optimizeCache() = withContext(Dispatchers.IO) {
        smartCleanup()
    }
}
