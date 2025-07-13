package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.spider.base.Spider
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Spider 专用缓存
 * 基于 FongMi/TV 标准实现
 * 
 * 专门为 Spider 解析器提供高性能缓存服务
 * 
 * 功能：
 * - Spider 实例缓存
 * - 解析结果缓存
 * - 智能过期管理
 * - 内存和磁盘双重缓存
 * - 统计监控
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object SpiderCache {
    
    private const val TAG = "ONETV_FILM_SPIDER_CACHE"
    private const val CACHE_DIR = "spider_cache"
    private const val METADATA_FILE = "spider_cache_metadata.json"
    private const val DEFAULT_TTL = 1800000L // 30分钟
    private const val MAX_MEMORY_CACHE_SIZE = 100
    
    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private val spiderInstanceCache = ConcurrentHashMap<String, Spider>()
    
    // 缓存统计
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // JSON 序列化器
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // 缓存目录
    private lateinit var cacheDir: File
    private var initialized = false
    
    /**
     * 🔧 初始化缓存
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        
        loadMetadata()
        startCleanupTask()
        initialized = true
        
        Log.d(TAG, "🔧 Spider 缓存初始化完成")
    }
    
    /**
     * 💾 缓存 Spider 实例
     */
    fun putSpider(key: String, spider: Spider) {
        try {
            spiderInstanceCache[key] = spider
            Log.d(TAG, "💾 Spider 实例已缓存: $key")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 实例缓存失败: $key", e)
        }
    }
    
    /**
     * 📦 获取 Spider 实例
     */
    fun getSpider(key: String): Spider? {
        return try {
            val spider = spiderInstanceCache[key]
            if (spider != null) {
                Log.d(TAG, "📦 Spider 实例缓存命中: $key")
                hitCount.incrementAndGet()
            } else {
                Log.d(TAG, "📦 Spider 实例缓存未命中: $key")
                missCount.incrementAndGet()
            }
            spider
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 实例获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 💾 缓存解析结果
     */
    suspend fun put(key: String, value: Any, ttl: Long = DEFAULT_TTL): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = CacheEntry(
                key = key,
                value = value,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + ttl,
                accessCount = 0
            )
            
            // 内存缓存
            if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
                evictOldestEntry()
            }
            memoryCache[key] = entry
            
            // 磁盘缓存
            saveToDisk(key, entry)
            
            putCount.incrementAndGet()
            Log.d(TAG, "💾 解析结果已缓存: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析结果缓存失败: $key", e)
            false
        }
    }
    
    /**
     * 📦 获取解析结果
     */
    suspend fun get(key: String): Any? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            var entry = memoryCache[key]
            
            // 如果内存缓存没有，从磁盘加载
            if (entry == null) {
                entry = loadFromDisk(key)
                if (entry != null) {
                    memoryCache[key] = entry
                }
            }
            
            if (entry != null) {
                // 检查是否过期
                if (entry.isExpired()) {
                    remove(key)
                    missCount.incrementAndGet()
                    Log.d(TAG, "📦 缓存已过期: $key")
                    return@withContext null
                }
                
                // 更新访问信息
                val updatedEntry = entry.copy(
                    accessCount = entry.accessCount + 1,
                    lastAccessTime = System.currentTimeMillis()
                )
                memoryCache[key] = updatedEntry
                
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 解析结果缓存命中: $key")
                return@withContext entry.value
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 解析结果缓存未命中: $key")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析结果获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 🗑️ 移除缓存项
     */
    suspend fun remove(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            memoryCache.remove(key)
            
            val diskFile = File(cacheDir, "$key.cache")
            if (diskFile.exists()) {
                diskFile.delete()
            }
            
            Log.d(TAG, "🗑️ 缓存项已移除: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存项移除失败: $key", e)
            false
        }
    }
    
    /**
     * 🧹 清空所有缓存
     */
    suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        try {
            memoryCache.clear()
            spiderInstanceCache.clear()
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".cache")) {
                    file.delete()
                }
            }
            
            Log.d(TAG, "🧹 所有缓存已清空")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空缓存失败", e)
            false
        }
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        val totalRequests = hitCount.get() + missCount.get()
        val hitRate = if (totalRequests > 0) {
            (hitCount.get().toDouble() / totalRequests * 100).toString() + "%"
        } else {
            "0%"
        }
        
        return mapOf(
            "memory_cache_size" to memoryCache.size,
            "spider_instance_cache_size" to spiderInstanceCache.size,
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "put_count" to putCount.get(),
            "hit_rate" to hitRate,
            "disk_cache_files" to getDiskCacheFileCount()
        )
    }
    
    /**
     * 🔄 淘汰最旧的缓存项
     */
    private fun evictOldestEntry() {
        val oldestEntry = memoryCache.values.minByOrNull { it.createTime }
        if (oldestEntry != null) {
            memoryCache.remove(oldestEntry.key)
            Log.d(TAG, "🔄 淘汰最旧缓存项: ${oldestEntry.key}")
        }
    }
    
    /**
     * 💾 保存到磁盘
     */
    private suspend fun saveToDisk(key: String, entry: CacheEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.cache")
            val jsonString = json.encodeToString(entry)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 磁盘保存失败: $key", e)
        }
    }
    
    /**
     * 📂 从磁盘加载
     */
    private suspend fun loadFromDisk(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.cache")
            if (file.exists()) {
                val jsonString = file.readText()
                return@withContext json.decodeFromString<CacheEntry>(jsonString)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 磁盘加载失败: $key", e)
            null
        }
    }
    
    /**
     * 📋 加载元数据
     */
    private fun loadMetadata() {
        // 实现元数据加载逻辑
    }
    
    /**
     * 🧹 启动清理任务
     */
    private fun startCleanupTask() {
        scope.launch {
            while (true) {
                delay(300000L) // 5分钟
                cleanupExpiredEntries()
            }
        }
    }
    
    /**
     * 🧹 清理过期条目
     */
    private suspend fun cleanupExpiredEntries() = withContext(Dispatchers.IO) {
        try {
            val expiredKeys = memoryCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                remove(key)
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "🧹 清理过期缓存项: ${expiredKeys.size} 个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期缓存失败", e)
        }
    }
    
    /**
     * 📊 获取磁盘缓存文件数量
     */
    private fun getDiskCacheFileCount(): Int {
        return try {
            cacheDir.listFiles()?.count { it.name.endsWith(".cache") } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 缓存条目数据类
     */
    @kotlinx.serialization.Serializable
    private data class CacheEntry(
        val key: String,
        val value: String, // 序列化后的值
        val createTime: Long,
        val expireTime: Long,
        val accessCount: Long,
        val lastAccessTime: Long = createTime
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
}
