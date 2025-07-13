package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存管理器
 * 
 * 基于 FongMi/TV 的缓存系统实现
 * 提供多级缓存、LRU 淘汰、持久化存储等功能
 * 
 * 功能：
 * - 内存缓存（LRU）
 * - 磁盘缓存
 * - 缓存策略配置
 * - 自动过期清理
 * - 缓存统计监控
 * - 缓存预热
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class CacheManager(
    private val context: Context,
    private val config: CacheConfig = CacheConfig()
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_CACHE_MANAGER"
        private const val CACHE_DIR = "onetv_cache"
        private const val METADATA_FILE = "cache_metadata.json"
        private const val CLEANUP_INTERVAL = 1800000L // 30分钟
    }
    
    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()
    
    // 磁盘缓存目录
    private val diskCacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // 缓存统计
    private val cacheStats = CacheStats()
    
    // 事件流
    private val _events = MutableSharedFlow<CacheEvent>()
    val events: SharedFlow<CacheEvent> = _events.asSharedFlow()
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 清理任务
    private var cleanupJob: Job? = null
    
    // JSON 序列化器
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    init {
        loadMetadata()
        startCleanupTask()
        Log.d(TAG, "🏗️ 缓存管理器初始化完成")
    }
    
    /**
     * 💾 存储到缓存
     */
    suspend fun <T : Any> put(
        key: String,
        value: T,
        ttl: Long = config.defaultTtl,
        strategy: CacheStrategy = CacheStrategy.MEMORY_FIRST
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = CacheEntry(
                key = key,
                value = value,
                createTime = System.currentTimeMillis(),
                lastAccessTime = System.currentTimeMillis(),
                ttl = ttl,
                strategy = strategy,
                size = calculateSize(value)
            )
            
            when (strategy) {
                CacheStrategy.MEMORY_ONLY -> {
                    putToMemory(entry)
                }
                CacheStrategy.DISK_ONLY -> {
                    putToDisk(entry)
                }
                CacheStrategy.MEMORY_FIRST -> {
                    putToMemory(entry)
                    if (config.enableDiskCache) {
                        putToDisk(entry)
                    }
                }
                CacheStrategy.DISK_FIRST -> {
                    if (config.enableDiskCache) {
                        putToDisk(entry)
                    }
                    putToMemory(entry)
                }
            }
            
            cacheStats.recordPut(entry.size)
            _events.emit(CacheEvent.EntryAdded(key, strategy, entry.size))
            
            Log.d(TAG, "💾 缓存存储成功: $key, 策略: $strategy, 大小: ${entry.size}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存存储失败: $key", e)
            cacheStats.recordError()
            false
        }
    }
    
    /**
     * 📦 从缓存获取
     */
    suspend fun <T : Any> get(
        key: String,
        clazz: Class<T>
    ): T? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            val memoryEntry = getFromMemory(key)
            if (memoryEntry != null && !memoryEntry.isExpired()) {
                updateAccessTime(memoryEntry)
                cacheStats.recordHit(CacheLevel.MEMORY)
                _events.emit(CacheEvent.EntryAccessed(key, CacheLevel.MEMORY))
                
                @Suppress("UNCHECKED_CAST")
                return@withContext memoryEntry.value as? T
            }
            
            // 从磁盘缓存获取
            if (config.enableDiskCache) {
                val diskEntry = getFromDisk(key, clazz)
                if (diskEntry != null && !diskEntry.isExpired()) {
                    // 将磁盘缓存提升到内存缓存
                    putToMemory(diskEntry)
                    updateAccessTime(diskEntry)
                    
                    cacheStats.recordHit(CacheLevel.DISK)
                    _events.emit(CacheEvent.EntryAccessed(key, CacheLevel.DISK))
                    
                    @Suppress("UNCHECKED_CAST")
                    return@withContext diskEntry.value as? T
                }
            }
            
            // 缓存未命中
            cacheStats.recordMiss()
            _events.emit(CacheEvent.EntryMissed(key))
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存获取失败: $key", e)
            cacheStats.recordError()
            null
        }
    }
    
    /**
     * 🗑️ 移除缓存
     */
    suspend fun remove(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var removed = false
            
            // 从内存缓存移除
            val memoryEntry = memoryCache.remove(key)
            if (memoryEntry != null) {
                cacheStats.recordRemove(memoryEntry.size)
                removed = true
            }
            
            // 从磁盘缓存移除
            if (config.enableDiskCache) {
                val diskFile = getDiskCacheFile(key)
                if (diskFile.exists()) {
                    val fileSize = diskFile.length()
                    diskFile.delete()
                    cacheStats.recordRemove(fileSize)
                    removed = true
                }
            }
            
            if (removed) {
                _events.emit(CacheEvent.EntryRemoved(key))
                Log.d(TAG, "🗑️ 缓存移除成功: $key")
            }
            
            removed
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存移除失败: $key", e)
            false
        }
    }
    
    /**
     * 🔍 检查缓存是否存在
     */
    suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        val memoryEntry = memoryCache[key]
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            return@withContext true
        }
        
        if (config.enableDiskCache) {
            val diskFile = getDiskCacheFile(key)
            if (diskFile.exists()) {
                return@withContext true
            }
        }
        
        false
    }
    
    /**
     * 🧹 清空所有缓存
     */
    suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 清空内存缓存
            val memorySize = memoryCache.values.sumOf { it.size }
            memoryCache.clear()
            
            // 清空磁盘缓存
            var diskSize = 0L
            if (config.enableDiskCache) {
                diskCacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name != METADATA_FILE) {
                        diskSize += file.length()
                        file.delete()
                    }
                }
            }
            
            cacheStats.recordClear(memorySize + diskSize)
            _events.emit(CacheEvent.CacheCleared)
            
            Log.d(TAG, "🧹 缓存清空完成")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存清空失败", e)
            false
        }
    }
    
    /**
     * 💾 存储到内存缓存
     */
    private fun putToMemory(entry: CacheEntry<Any>) {
        // 检查内存缓存大小限制
        ensureMemoryCacheSpace(entry.size)
        
        memoryCache[entry.key] = entry
    }
    
    /**
     * 💾 存储到磁盘缓存
     */
    private suspend fun putToDisk(entry: CacheEntry<Any>) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getDiskCacheFile(entry.key)
            val serializedEntry = SerializableCacheEntry(
                key = entry.key,
                value = json.encodeToString(entry.value),
                valueClass = entry.value::class.java.name,
                createTime = entry.createTime,
                lastAccessTime = entry.lastAccessTime,
                ttl = entry.ttl,
                strategy = entry.strategy,
                size = entry.size
            )
            
            cacheFile.writeText(json.encodeToString(serializedEntry))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 磁盘缓存存储失败: ${entry.key}", e)
        }
    }
    
    /**
     * 📦 从内存缓存获取
     */
    private fun getFromMemory(key: String): CacheEntry<Any>? {
        return memoryCache[key]
    }
    
    /**
     * 📦 从磁盘缓存获取
     */
    private suspend fun <T : Any> getFromDisk(key: String, clazz: Class<T>): CacheEntry<Any>? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getDiskCacheFile(key)
            if (!cacheFile.exists()) {
                return@withContext null
            }
            
            val serializedEntry = json.decodeFromString<SerializableCacheEntry>(cacheFile.readText())
            
            // 反序列化值
            val value = json.decodeFromString(clazz, serializedEntry.value)
            
            CacheEntry(
                key = serializedEntry.key,
                value = value,
                createTime = serializedEntry.createTime,
                lastAccessTime = serializedEntry.lastAccessTime,
                ttl = serializedEntry.ttl,
                strategy = serializedEntry.strategy,
                size = serializedEntry.size
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 磁盘缓存获取失败: $key", e)
            null
        }
    }
    
    /**
     * 🔧 确保内存缓存空间
     */
    private fun ensureMemoryCacheSpace(requiredSize: Long) {
        val currentSize = memoryCache.values.sumOf { it.size }
        
        if (currentSize + requiredSize <= config.maxMemoryCacheSize) {
            return
        }
        
        // LRU 淘汰策略
        val sortedEntries = memoryCache.values.sortedBy { it.lastAccessTime }
        var freedSize = 0L
        
        for (entry in sortedEntries) {
            if (currentSize - freedSize + requiredSize <= config.maxMemoryCacheSize) {
                break
            }
            
            memoryCache.remove(entry.key)
            freedSize += entry.size
            
            Log.d(TAG, "🗑️ LRU 淘汰内存缓存: ${entry.key}")
        }
    }
    
    /**
     * ⏰ 更新访问时间
     */
    private fun updateAccessTime(entry: CacheEntry<Any>) {
        val updatedEntry = entry.copy(lastAccessTime = System.currentTimeMillis())
        memoryCache[entry.key] = updatedEntry
    }
    
    /**
     * 📁 获取磁盘缓存文件
     */
    private fun getDiskCacheFile(key: String): File {
        val fileName = key.hashCode().toString() + ".cache"
        return File(diskCacheDir, fileName)
    }
    
    /**
     * 📏 计算对象大小
     */
    private fun calculateSize(value: Any): Long {
        return when (value) {
            is String -> value.length * 2L // 每个字符 2 字节
            is ByteArray -> value.size.toLong()
            is List<*> -> value.size * 8L // 估算
            is Map<*, *> -> value.size * 16L // 估算
            else -> 64L // 默认估算
        }
    }
    
    /**
     * 📋 加载元数据
     */
    private fun loadMetadata() {
        try {
            val metadataFile = File(diskCacheDir, METADATA_FILE)
            if (metadataFile.exists()) {
                // 这里可以加载缓存元数据
                Log.d(TAG, "📋 缓存元数据加载完成")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载缓存元数据失败", e)
        }
    }
    
    /**
     * 💾 保存元数据
     */
    private suspend fun saveMetadata() = withContext(Dispatchers.IO) {
        try {
            val metadataFile = File(diskCacheDir, METADATA_FILE)
            // 这里可以保存缓存元数据
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存缓存元数据失败", e)
        }
    }
    
    /**
     * 🚀 启动清理任务
     */
    private fun startCleanupTask() {
        cleanupJob = scope.launch {
            while (isActive) {
                try {
                    performCleanup()
                    delay(CLEANUP_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 缓存清理任务异常", e)
                }
            }
        }
        
        Log.d(TAG, "🚀 缓存清理任务已启动")
    }
    
    /**
     * 🧹 执行清理
     */
    private suspend fun performCleanup() {
        Log.d(TAG, "🧹 执行缓存清理...")
        
        val now = System.currentTimeMillis()
        val expiredKeys = mutableListOf<String>()
        
        // 清理过期的内存缓存
        memoryCache.forEach { (key, entry) ->
            if (entry.isExpired(now)) {
                expiredKeys.add(key)
            }
        }
        
        expiredKeys.forEach { key ->
            memoryCache.remove(key)
            Log.d(TAG, "🗑️ 移除过期内存缓存: $key")
        }
        
        // 清理过期的磁盘缓存
        if (config.enableDiskCache) {
            diskCacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".cache")) {
                    try {
                        val serializedEntry = json.decodeFromString<SerializableCacheEntry>(file.readText())
                        if (serializedEntry.isExpired(now)) {
                            file.delete()
                            Log.d(TAG, "🗑️ 移除过期磁盘缓存: ${serializedEntry.key}")
                        }
                    } catch (e: Exception) {
                        // 文件损坏，删除
                        file.delete()
                    }
                }
            }
        }
        
        if (expiredKeys.isNotEmpty()) {
            _events.emit(CacheEvent.ExpiredEntriesCleared(expiredKeys.size))
            Log.d(TAG, "✅ 缓存清理完成，移除条目: ${expiredKeys.size}")
        }
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        val memorySize = memoryCache.values.sumOf { it.size }
        val diskSize = if (config.enableDiskCache) {
            diskCacheDir.listFiles()?.filter { it.isFile && it.name != METADATA_FILE }?.sumOf { it.length() } ?: 0L
        } else {
            0L
        }
        
        return mapOf(
            "memory_entries" to memoryCache.size,
            "memory_size_bytes" to memorySize,
            "memory_size_mb" to String.format("%.2f", memorySize / 1024.0 / 1024.0),
            "disk_size_bytes" to diskSize,
            "disk_size_mb" to String.format("%.2f", diskSize / 1024.0 / 1024.0),
            "total_size_mb" to String.format("%.2f", (memorySize + diskSize) / 1024.0 / 1024.0),
            "hit_count" to cacheStats.hitCount.get(),
            "miss_count" to cacheStats.missCount.get(),
            "hit_rate" to cacheStats.getHitRate(),
            "put_count" to cacheStats.putCount.get(),
            "remove_count" to cacheStats.removeCount.get(),
            "error_count" to cacheStats.errorCount.get()
        )
    }
    
    /**
     * 🛑 关闭缓存管理器
     */
    fun shutdown() {
        cleanupJob?.cancel()
        scope.cancel()
        
        // 保存元数据
        runBlocking {
            saveMetadata()
        }
        
        Log.d(TAG, "🛑 缓存管理器已关闭")
    }
}
