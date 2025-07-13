package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 内容专用缓存
 * 基于 FongMi/TV 标准实现
 * 
 * 专门用于缓存影视内容数据，提供高效的内容管理
 * 
 * 功能：
 * - 首页内容缓存
 * - 分类内容缓存
 * - 详情内容缓存
 * - 搜索结果缓存
 * - 播放链接缓存
 * - 智能过期管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object ContentCache {
    
    private const val TAG = "ONETV_FILM_CONTENT_CACHE"
    private const val CACHE_DIR = "content_cache"
    private const val DEFAULT_TTL = 1800000L // 30分钟
    private const val HOME_CONTENT_TTL = 3600000L // 1小时
    private const val CATEGORY_CONTENT_TTL = 1800000L // 30分钟
    private const val DETAIL_CONTENT_TTL = 7200000L // 2小时
    private const val SEARCH_CONTENT_TTL = 900000L // 15分钟
    private const val PLAYER_CONTENT_TTL = 300000L // 5分钟
    private const val MAX_CACHE_SIZE = 200
    
    // 内存缓存
    private val contentCache = ConcurrentHashMap<String, ContentEntry>()
    
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
     * 🔧 初始化内容缓存
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        
        loadPersistedContent()
        startCleanupTask()
        initialized = true
        
        Log.d(TAG, "🔧 内容缓存初始化完成")
    }
    
    /**
     * 💾 缓存首页内容
     */
    suspend fun putHomeContent(siteKey: String, content: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext putContent("home_$siteKey", content, HOME_CONTENT_TTL, ContentType.HOME)
    }
    
    /**
     * 📦 获取首页内容
     */
    suspend fun getHomeContent(siteKey: String): String? = withContext(Dispatchers.IO) {
        return@withContext getContent("home_$siteKey")
    }
    
    /**
     * 💾 缓存分类内容
     */
    suspend fun putCategoryContent(
        siteKey: String, 
        tid: String, 
        page: String, 
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        val key = "category_${siteKey}_${tid}_$page"
        return@withContext putContent(key, content, CATEGORY_CONTENT_TTL, ContentType.CATEGORY)
    }
    
    /**
     * 📦 获取分类内容
     */
    suspend fun getCategoryContent(siteKey: String, tid: String, page: String): String? = withContext(Dispatchers.IO) {
        val key = "category_${siteKey}_${tid}_$page"
        return@withContext getContent(key)
    }
    
    /**
     * 💾 缓存详情内容
     */
    suspend fun putDetailContent(siteKey: String, vodId: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val key = "detail_${siteKey}_$vodId"
        return@withContext putContent(key, content, DETAIL_CONTENT_TTL, ContentType.DETAIL)
    }
    
    /**
     * 📦 获取详情内容
     */
    suspend fun getDetailContent(siteKey: String, vodId: String): String? = withContext(Dispatchers.IO) {
        val key = "detail_${siteKey}_$vodId"
        return@withContext getContent(key)
    }
    
    /**
     * 💾 缓存搜索结果
     */
    suspend fun putSearchContent(siteKey: String, keyword: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val key = "search_${siteKey}_${keyword.hashCode()}"
        return@withContext putContent(key, content, SEARCH_CONTENT_TTL, ContentType.SEARCH)
    }
    
    /**
     * 📦 获取搜索结果
     */
    suspend fun getSearchContent(siteKey: String, keyword: String): String? = withContext(Dispatchers.IO) {
        val key = "search_${siteKey}_${keyword.hashCode()}"
        return@withContext getContent(key)
    }
    
    /**
     * 💾 缓存播放内容
     */
    suspend fun putPlayerContent(siteKey: String, flag: String, id: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val key = "player_${siteKey}_${flag}_${id.hashCode()}"
        return@withContext putContent(key, content, PLAYER_CONTENT_TTL, ContentType.PLAYER)
    }
    
    /**
     * 📦 获取播放内容
     */
    suspend fun getPlayerContent(siteKey: String, flag: String, id: String): String? = withContext(Dispatchers.IO) {
        val key = "player_${siteKey}_${flag}_${id.hashCode()}"
        return@withContext getContent(key)
    }
    
    /**
     * 💾 通用内容缓存
     */
    private suspend fun putContent(
        key: String, 
        content: String, 
        ttl: Long, 
        type: ContentType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查缓存大小，必要时清理
            if (contentCache.size >= MAX_CACHE_SIZE) {
                evictOldestEntries(10)
            }
            
            val entry = ContentEntry(
                key = key,
                content = content,
                type = type,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + ttl,
                accessCount = 0,
                size = content.length
            )
            
            contentCache[key] = entry
            saveContentToDisk(entry)
            
            putCount.incrementAndGet()
            Log.d(TAG, "💾 内容已缓存: $key (${type.name})")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容缓存失败: $key", e)
            false
        }
    }
    
    /**
     * 📦 通用内容获取
     */
    private suspend fun getContent(key: String): String? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            var entry = contentCache[key]
            
            // 如果内存缓存没有，从磁盘加载
            if (entry == null) {
                entry = loadContentFromDisk(key)
                if (entry != null) {
                    contentCache[key] = entry
                }
            }
            
            if (entry != null) {
                // 检查是否过期
                if (entry.isExpired()) {
                    removeContent(key)
                    missCount.incrementAndGet()
                    Log.d(TAG, "📦 内容已过期: $key")
                    return@withContext null
                }
                
                // 更新访问信息
                val updatedEntry = entry.copy(
                    accessCount = entry.accessCount + 1,
                    lastAccessTime = System.currentTimeMillis()
                )
                contentCache[key] = updatedEntry
                
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 内容缓存命中: $key (${entry.type.name})")
                return@withContext entry.content
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 内容缓存未命中: $key")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 🗑️ 移除内容
     */
    suspend fun removeContent(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            contentCache.remove(key)
            
            val file = File(cacheDir, "$key.content")
            if (file.exists()) {
                file.delete()
            }
            
            Log.d(TAG, "🗑️ 内容已移除: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容移除失败: $key", e)
            false
        }
    }
    
    /**
     * 🧹 清空所有内容缓存
     */
    suspend fun clearContent(): Boolean = withContext(Dispatchers.IO) {
        try {
            contentCache.clear()
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".content")) {
                    file.delete()
                }
            }
            
            Log.d(TAG, "🧹 所有内容缓存已清空")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空内容缓存失败", e)
            false
        }
    }
    
    /**
     * 🧹 按类型清空缓存
     */
    suspend fun clearContentByType(type: ContentType): Boolean = withContext(Dispatchers.IO) {
        try {
            val keysToRemove = contentCache.entries
                .filter { it.value.type == type }
                .map { it.key }
            
            keysToRemove.forEach { key ->
                removeContent(key)
            }
            
            Log.d(TAG, "🧹 ${type.name} 类型缓存已清空: ${keysToRemove.size} 个")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 按类型清空缓存失败: ${type.name}", e)
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
        
        val typeStats = ContentType.values().associate { type ->
            type.name to contentCache.values.count { it.type == type }
        }
        
        val totalSize = contentCache.values.sumOf { it.size }
        
        return mapOf(
            "content_cache_size" to contentCache.size,
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "put_count" to putCount.get(),
            "hit_rate" to hitRate,
            "total_content_size" to totalSize,
            "type_distribution" to typeStats,
            "disk_content_files" to getDiskContentFileCount()
        )
    }
    
    /**
     * 🔄 淘汰最旧的缓存项
     */
    private fun evictOldestEntries(count: Int) {
        val oldestEntries = contentCache.values
            .sortedBy { it.lastAccessTime }
            .take(count)
        
        oldestEntries.forEach { entry ->
            contentCache.remove(entry.key)
            Log.d(TAG, "🔄 淘汰旧内容: ${entry.key}")
        }
    }
    
    /**
     * 💾 保存内容到磁盘
     */
    private suspend fun saveContentToDisk(entry: ContentEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "${entry.key}.content")
            val jsonString = json.encodeToString(entry)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容磁盘保存失败: ${entry.key}", e)
        }
    }
    
    /**
     * 📂 从磁盘加载内容
     */
    private suspend fun loadContentFromDisk(key: String): ContentEntry? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.content")
            if (file.exists()) {
                val jsonString = file.readText()
                return@withContext json.decodeFromString<ContentEntry>(jsonString)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 内容磁盘加载失败: $key", e)
            null
        }
    }
    
    /**
     * 📋 加载持久化内容
     */
    private fun loadPersistedContent() {
        scope.launch {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".content")) {
                        val key = file.nameWithoutExtension
                        val entry = loadContentFromDisk(key)
                        if (entry != null && !entry.isExpired()) {
                            contentCache[key] = entry
                        }
                    }
                }
                Log.d(TAG, "📋 持久化内容加载完成: ${contentCache.size} 个")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 持久化内容加载失败", e)
            }
        }
    }
    
    /**
     * 🧹 启动清理任务
     */
    private fun startCleanupTask() {
        scope.launch {
            while (true) {
                delay(300000L) // 5分钟
                cleanupExpiredContent()
            }
        }
    }
    
    /**
     * 🧹 清理过期内容
     */
    private suspend fun cleanupExpiredContent() = withContext(Dispatchers.IO) {
        try {
            val expiredKeys = contentCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                removeContent(key)
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "🧹 清理过期内容: ${expiredKeys.size} 个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期内容失败", e)
        }
    }
    
    /**
     * 📊 获取磁盘内容文件数量
     */
    private fun getDiskContentFileCount(): Int {
        return try {
            cacheDir.listFiles()?.count { it.name.endsWith(".content") } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 内容类型枚举
     */
    enum class ContentType {
        HOME, CATEGORY, DETAIL, SEARCH, PLAYER
    }
    
    /**
     * 内容条目数据类
     */
    @kotlinx.serialization.Serializable
    private data class ContentEntry(
        val key: String,
        val content: String,
        val type: ContentType,
        val createTime: Long,
        val expireTime: Long,
        val accessCount: Long,
        val size: Int,
        val lastAccessTime: Long = createTime
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
}
