package top.cywin.onetv.film.jar

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * JAR 缓存管理器
 * 
 * 基于 FongMi/TV 的 JAR 缓存管理实现
 * 提供智能缓存策略、LRU 淘汰和持久化存储
 * 
 * 功能：
 * - 智能缓存策略
 * - LRU 淘汰算法
 * - 持久化存储
 * - 缓存预热
 * - 统计监控
 * - 自动清理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JarCache(
    private val context: Context,
    private val maxCacheSize: Long = 500 * 1024 * 1024L // 500MB
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAR_CACHE_MANAGER"
        private const val CACHE_DIR = "jar_cache"
        private const val METADATA_FILE = "cache_metadata.json"
        private const val CLEANUP_INTERVAL = 3600000L // 1小时
    }
    
    // 缓存目录
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // 缓存元数据
    private val cacheMetadata = ConcurrentHashMap<String, CacheEntry>()
    
    // 缓存统计
    private val cacheStats = CacheStats()
    
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
    }
    
    /**
     * 💾 存储 JAR 到缓存
     */
    suspend fun put(jarKey: String, jarData: ByteArray, jarInfo: JarInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💾 存储 JAR 到缓存: $jarKey")
            
            // 检查缓存空间
            ensureCacheSpace(jarData.size.toLong())
            
            // 写入文件
            val jarFile = File(cacheDir, "$jarKey.jar")
            jarFile.writeBytes(jarData)
            
            // 更新元数据
            val cacheEntry = CacheEntry(
                key = jarKey,
                filePath = jarFile.absolutePath,
                fileSize = jarData.size.toLong(),
                jarInfo = jarInfo,
                createTime = System.currentTimeMillis(),
                lastAccessTime = System.currentTimeMillis(),
                accessCount = 1
            )
            
            cacheMetadata[jarKey] = cacheEntry
            
            // 更新统计
            cacheStats.recordPut(jarData.size.toLong())
            
            // 保存元数据
            saveMetadata()
            
            Log.d(TAG, "✅ JAR 缓存存储成功: $jarKey, 大小: ${jarData.size / 1024}KB")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JAR 缓存存储失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 📦 从缓存获取 JAR
     */
    suspend fun get(jarKey: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cacheEntry = cacheMetadata[jarKey]
            if (cacheEntry == null) {
                cacheStats.recordMiss()
                return@withContext null
            }
            
            val jarFile = File(cacheEntry.filePath)
            if (!jarFile.exists()) {
                // 文件不存在，移除元数据
                cacheMetadata.remove(jarKey)
                saveMetadata()
                cacheStats.recordMiss()
                return@withContext null
            }
            
            // 更新访问信息
            val updatedEntry = cacheEntry.copy(
                lastAccessTime = System.currentTimeMillis(),
                accessCount = cacheEntry.accessCount + 1
            )
            cacheMetadata[jarKey] = updatedEntry
            
            // 读取文件
            val jarData = jarFile.readBytes()
            
            // 更新统计
            cacheStats.recordHit()
            
            Log.d(TAG, "📦 从缓存获取 JAR: $jarKey, 大小: ${jarData.size / 1024}KB")
            jarData
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 从缓存获取 JAR 失败: $jarKey", e)
            cacheStats.recordMiss()
            null
        }
    }
    
    /**
     * 🔍 检查缓存是否存在
     */
    fun contains(jarKey: String): Boolean {
        val cacheEntry = cacheMetadata[jarKey] ?: return false
        val jarFile = File(cacheEntry.filePath)
        return jarFile.exists()
    }
    
    /**
     * 📋 获取缓存信息
     */
    fun getCacheInfo(jarKey: String): CacheEntry? {
        return cacheMetadata[jarKey]
    }
    
    /**
     * 🗑️ 移除缓存
     */
    suspend fun remove(jarKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheEntry = cacheMetadata.remove(jarKey)
            if (cacheEntry != null) {
                val jarFile = File(cacheEntry.filePath)
                if (jarFile.exists()) {
                    jarFile.delete()
                }
                
                cacheStats.recordRemove(cacheEntry.fileSize)
                saveMetadata()
                
                Log.d(TAG, "🗑️ 缓存移除成功: $jarKey")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 缓存移除失败: $jarKey", e)
            false
        }
    }
    
    /**
     * 🧹 清空所有缓存
     */
    suspend fun clear(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧹 清空所有缓存...")
            
            // 删除所有缓存文件
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".jar")) {
                    file.delete()
                }
            }
            
            // 清空元数据
            cacheMetadata.clear()
            cacheStats.reset()
            
            // 保存元数据
            saveMetadata()
            
            Log.d(TAG, "✅ 所有缓存已清空")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空缓存失败", e)
            false
        }
    }
    
    /**
     * 🔧 确保缓存空间
     */
    private suspend fun ensureCacheSpace(requiredSize: Long) {
        val currentSize = getCurrentCacheSize()
        
        if (currentSize + requiredSize <= maxCacheSize) {
            return
        }
        
        Log.d(TAG, "🔧 缓存空间不足，开始清理...")
        
        // LRU 淘汰策略
        val sortedEntries = cacheMetadata.values.sortedBy { it.lastAccessTime }
        var freedSize = 0L
        
        for (entry in sortedEntries) {
            if (currentSize - freedSize + requiredSize <= maxCacheSize) {
                break
            }
            
            remove(entry.key)
            freedSize += entry.fileSize
            
            Log.d(TAG, "🗑️ LRU 淘汰缓存: ${entry.key}")
        }
        
        Log.d(TAG, "✅ 缓存清理完成，释放空间: ${freedSize / 1024 / 1024}MB")
    }
    
    /**
     * 📊 获取当前缓存大小
     */
    private fun getCurrentCacheSize(): Long {
        return cacheMetadata.values.sumOf { it.fileSize }
    }
    
    /**
     * 📋 加载元数据
     */
    private fun loadMetadata() {
        try {
            val metadataFile = File(cacheDir, METADATA_FILE)
            if (!metadataFile.exists()) {
                return
            }
            
            val metadataJson = metadataFile.readText()
            val metadata = json.decodeFromString<Map<String, CacheEntry>>(metadataJson)
            
            // 验证文件是否存在
            metadata.forEach { (key, entry) ->
                val jarFile = File(entry.filePath)
                if (jarFile.exists()) {
                    cacheMetadata[key] = entry
                }
            }
            
            Log.d(TAG, "📋 缓存元数据加载完成，条目数: ${cacheMetadata.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载缓存元数据失败", e)
        }
    }
    
    /**
     * 💾 保存元数据
     */
    private suspend fun saveMetadata() = withContext(Dispatchers.IO) {
        try {
            val metadataFile = File(cacheDir, METADATA_FILE)
            val metadataJson = json.encodeToString(cacheMetadata.toMap())
            metadataFile.writeText(metadataJson)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存缓存元数据失败", e)
        }
    }
    
    /**
     * 🚀 启动清理任务
     */
    private fun startCleanupTask() {
        cleanupJob = scope.launch {
            while (true) {
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
        val expiredEntries = mutableListOf<String>()
        
        // 查找过期条目（超过7天未访问）
        cacheMetadata.forEach { (key, entry) ->
            if (now - entry.lastAccessTime > 7 * 24 * 3600 * 1000L) {
                expiredEntries.add(key)
            }
        }
        
        // 移除过期条目
        expiredEntries.forEach { key ->
            remove(key)
            Log.d(TAG, "🗑️ 移除过期缓存: $key")
        }
        
        // 检查缓存大小限制
        if (getCurrentCacheSize() > maxCacheSize) {
            ensureCacheSpace(0)
        }
        
        if (expiredEntries.isNotEmpty()) {
            Log.d(TAG, "✅ 缓存清理完成，移除条目: ${expiredEntries.size}")
        }
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        val currentSize = getCurrentCacheSize()
        
        return mapOf(
            "cache_entries" to cacheMetadata.size,
            "cache_size_bytes" to currentSize,
            "cache_size_mb" to String.format("%.2f", currentSize / 1024.0 / 1024.0),
            "max_cache_size_mb" to String.format("%.2f", maxCacheSize / 1024.0 / 1024.0),
            "cache_usage_percent" to String.format("%.1f", (currentSize.toDouble() / maxCacheSize) * 100),
            "hit_count" to cacheStats.hitCount.get(),
            "miss_count" to cacheStats.missCount.get(),
            "hit_rate" to cacheStats.getHitRate(),
            "put_count" to cacheStats.putCount.get(),
            "remove_count" to cacheStats.removeCount.get()
        )
    }
    
    /**
     * 📋 获取所有缓存条目
     */
    fun getAllCacheEntries(): List<CacheEntry> {
        return cacheMetadata.values.toList()
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
        
        Log.d(TAG, "🛑 JAR 缓存管理器已关闭")
    }
}

/**
 * 缓存条目
 */
@kotlinx.serialization.Serializable
data class CacheEntry(
    val key: String,                    // 缓存键
    val filePath: String,               // 文件路径
    val fileSize: Long,                 // 文件大小
    val jarInfo: JarInfo,               // JAR 信息
    val createTime: Long,               // 创建时间
    val lastAccessTime: Long,           // 最后访问时间
    val accessCount: Long               // 访问次数
) {
    
    /**
     * 📊 获取摘要信息
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "key" to key,
            "jar_name" to jarInfo.name,
            "jar_version" to jarInfo.version,
            "file_size_mb" to String.format("%.2f", fileSize / 1024.0 / 1024.0),
            "access_count" to accessCount,
            "last_access" to lastAccessTime
        )
    }
}

/**
 * 缓存统计
 */
class CacheStats {
    
    val hitCount = AtomicLong(0)
    val missCount = AtomicLong(0)
    val putCount = AtomicLong(0)
    val removeCount = AtomicLong(0)
    val totalPutSize = AtomicLong(0)
    val totalRemoveSize = AtomicLong(0)
    
    fun recordHit() {
        hitCount.incrementAndGet()
    }
    
    fun recordMiss() {
        missCount.incrementAndGet()
    }
    
    fun recordPut(size: Long) {
        putCount.incrementAndGet()
        totalPutSize.addAndGet(size)
    }
    
    fun recordRemove(size: Long) {
        removeCount.incrementAndGet()
        totalRemoveSize.addAndGet(size)
    }
    
    fun getHitRate(): Double {
        val total = hitCount.get() + missCount.get()
        return if (total > 0) hitCount.get().toDouble() / total else 0.0
    }
    
    fun reset() {
        hitCount.set(0)
        missCount.set(0)
        putCount.set(0)
        removeCount.set(0)
        totalPutSize.set(0)
        totalRemoveSize.set(0)
    }
}
