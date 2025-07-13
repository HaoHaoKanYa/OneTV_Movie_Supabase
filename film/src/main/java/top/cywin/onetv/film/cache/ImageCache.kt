package top.cywin.onetv.film.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 图片专用缓存
 * 基于 FongMi/TV 标准实现
 * 
 * 专门用于缓存影视海报、封面等图片资源
 * 
 * 功能：
 * - 图片内存缓存
 * - 图片磁盘缓存
 * - 图片压缩优化
 * - LRU 淘汰策略
 * - 缓存大小控制
 * - 异步加载
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object ImageCache {
    
    private const val TAG = "ONETV_FILM_IMAGE_CACHE"
    private const val CACHE_DIR = "image_cache"
    private const val DEFAULT_TTL = 86400000L // 24小时
    private const val MAX_MEMORY_CACHE_SIZE = 50 // 最大内存缓存数量
    private const val MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    private const val COMPRESS_QUALITY = 80 // 压缩质量
    private const val MAX_IMAGE_SIZE = 1024 // 最大图片尺寸
    
    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, ImageEntry>()
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    
    // 缓存统计
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val diskCacheSize = AtomicLong(0)
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 缓存目录
    private lateinit var cacheDir: File
    private var initialized = false
    
    /**
     * 🔧 初始化图片缓存
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        
        calculateDiskCacheSize()
        startCleanupTask()
        initialized = true
        
        Log.d(TAG, "🔧 图片缓存初始化完成")
    }
    
    /**
     * 💾 缓存图片数据
     */
    suspend fun putImage(url: String, imageData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(url)
            
            // 检查内存缓存大小
            if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
                evictOldestMemoryEntry()
            }
            
            val entry = ImageEntry(
                key = key,
                url = url,
                size = imageData.size,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + DEFAULT_TTL,
                accessCount = 0
            )
            
            // 内存缓存
            memoryCache[key] = entry
            
            // 磁盘缓存
            val success = saveImageToDisk(key, imageData)
            if (success) {
                diskCacheSize.addAndGet(imageData.size.toLong())
                putCount.incrementAndGet()
                Log.d(TAG, "💾 图片已缓存: $url")
                
                // 检查磁盘缓存大小
                if (diskCacheSize.get() > MAX_DISK_CACHE_SIZE) {
                    cleanupDiskCache()
                }
                
                return@withContext true
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片缓存失败: $url", e)
            false
        }
    }
    
    /**
     * 💾 缓存 Bitmap
     */
    suspend fun putBitmap(url: String, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(url)
            
            // 压缩 Bitmap
            val compressedBitmap = compressBitmap(bitmap)
            bitmapCache[key] = compressedBitmap
            
            // 转换为字节数组并保存到磁盘
            val imageData = bitmapToByteArray(compressedBitmap)
            return@withContext putImage(url, imageData)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bitmap 缓存失败: $url", e)
            false
        }
    }
    
    /**
     * 📦 获取图片数据
     */
    suspend fun getImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(url)
            
            // 检查内存缓存
            var entry = memoryCache[key]
            if (entry != null) {
                if (entry.isExpired()) {
                    removeImage(url)
                    missCount.incrementAndGet()
                    Log.d(TAG, "📦 图片已过期: $url")
                    return@withContext null
                }
                
                // 更新访问信息
                val updatedEntry = entry.copy(
                    accessCount = entry.accessCount + 1,
                    lastAccessTime = System.currentTimeMillis()
                )
                memoryCache[key] = updatedEntry
            }
            
            // 从磁盘加载
            val imageData = loadImageFromDisk(key)
            if (imageData != null) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 图片缓存命中: $url")
                return@withContext imageData
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 图片缓存未命中: $url")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片获取失败: $url", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 📦 获取 Bitmap
     */
    suspend fun getBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(url)
            
            // 先从 Bitmap 缓存获取
            val cachedBitmap = bitmapCache[key]
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 Bitmap 缓存命中: $url")
                return@withContext cachedBitmap
            }
            
            // 从图片数据创建 Bitmap
            val imageData = getImage(url)
            if (imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                if (bitmap != null) {
                    bitmapCache[key] = bitmap
                    return@withContext bitmap
                }
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Bitmap 获取失败: $url", e)
            null
        }
    }
    
    /**
     * 🗑️ 移除图片
     */
    suspend fun removeImage(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = generateCacheKey(url)
            
            val entry = memoryCache.remove(key)
            bitmapCache.remove(key)?.recycle()
            
            val file = File(cacheDir, "$key.img")
            if (file.exists()) {
                val fileSize = file.length()
                if (file.delete()) {
                    diskCacheSize.addAndGet(-fileSize)
                }
            }
            
            Log.d(TAG, "🗑️ 图片已移除: $url")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片移除失败: $url", e)
            false
        }
    }
    
    /**
     * 🧹 清空所有图片缓存
     */
    suspend fun clearImages(): Boolean = withContext(Dispatchers.IO) {
        try {
            memoryCache.clear()
            
            // 回收所有 Bitmap
            bitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            bitmapCache.clear()
            
            // 删除磁盘文件
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".img")) {
                    file.delete()
                }
            }
            
            diskCacheSize.set(0)
            Log.d(TAG, "🧹 所有图片缓存已清空")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空图片缓存失败", e)
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
            "bitmap_cache_size" to bitmapCache.size,
            "disk_cache_size_mb" to (diskCacheSize.get() / 1024 / 1024),
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "put_count" to putCount.get(),
            "hit_rate" to hitRate,
            "disk_image_files" to getDiskImageFileCount()
        )
    }
    
    /**
     * 🔧 生成缓存键
     */
    private fun generateCacheKey(url: String): String {
        return url.hashCode().toString()
    }
    
    /**
     * 🗜️ 压缩 Bitmap
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }
        
        val scale = minOf(
            MAX_IMAGE_SIZE.toFloat() / width,
            MAX_IMAGE_SIZE.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * 🔄 Bitmap 转字节数组
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, stream)
        return stream.toByteArray()
    }
    
    /**
     * 💾 保存图片到磁盘
     */
    private suspend fun saveImageToDisk(key: String, imageData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.img")
            FileOutputStream(file).use { output ->
                output.write(imageData)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片磁盘保存失败: $key", e)
            false
        }
    }
    
    /**
     * 📂 从磁盘加载图片
     */
    private suspend fun loadImageFromDisk(key: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.img")
            if (file.exists()) {
                return@withContext file.readBytes()
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 图片磁盘加载失败: $key", e)
            null
        }
    }
    
    /**
     * 🔄 淘汰最旧的内存缓存项
     */
    private fun evictOldestMemoryEntry() {
        val oldestEntry = memoryCache.values.minByOrNull { it.lastAccessTime }
        if (oldestEntry != null) {
            memoryCache.remove(oldestEntry.key)
            bitmapCache.remove(oldestEntry.key)?.recycle()
            Log.d(TAG, "🔄 淘汰最旧图片: ${oldestEntry.key}")
        }
    }
    
    /**
     * 🧹 清理磁盘缓存
     */
    private suspend fun cleanupDiskCache() = withContext(Dispatchers.IO) {
        try {
            val files = cacheDir.listFiles()?.filter { it.name.endsWith(".img") }
                ?.sortedBy { it.lastModified() } ?: return@withContext
            
            var currentSize = diskCacheSize.get()
            val targetSize = MAX_DISK_CACHE_SIZE * 0.8 // 清理到80%
            
            for (file in files) {
                if (currentSize <= targetSize) break
                
                val fileSize = file.length()
                if (file.delete()) {
                    currentSize -= fileSize
                    diskCacheSize.addAndGet(-fileSize)
                    
                    // 同时清理内存缓存
                    val key = file.nameWithoutExtension
                    memoryCache.remove(key)
                    bitmapCache.remove(key)?.recycle()
                }
            }
            
            Log.d(TAG, "🧹 磁盘缓存清理完成，当前大小: ${currentSize / 1024 / 1024}MB")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 磁盘缓存清理失败", e)
        }
    }
    
    /**
     * 📊 计算磁盘缓存大小
     */
    private fun calculateDiskCacheSize() {
        scope.launch {
            try {
                val totalSize = cacheDir.listFiles()
                    ?.filter { it.name.endsWith(".img") }
                    ?.sumOf { it.length() } ?: 0L
                
                diskCacheSize.set(totalSize)
                Log.d(TAG, "📊 磁盘缓存大小: ${totalSize / 1024 / 1024}MB")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 计算磁盘缓存大小失败", e)
            }
        }
    }
    
    /**
     * 🧹 启动清理任务
     */
    private fun startCleanupTask() {
        scope.launch {
            while (true) {
                delay(3600000L) // 1小时
                cleanupExpiredImages()
            }
        }
    }
    
    /**
     * 🧹 清理过期图片
     */
    private suspend fun cleanupExpiredImages() = withContext(Dispatchers.IO) {
        try {
            val expiredKeys = memoryCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                val entry = memoryCache[key]
                if (entry != null) {
                    removeImage(entry.url)
                }
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "🧹 清理过期图片: ${expiredKeys.size} 个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期图片失败", e)
        }
    }
    
    /**
     * 📊 获取磁盘图片文件数量
     */
    private fun getDiskImageFileCount(): Int {
        return try {
            cacheDir.listFiles()?.count { it.name.endsWith(".img") } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 图片条目数据类
     */
    private data class ImageEntry(
        val key: String,
        val url: String,
        val size: Int,
        val createTime: Long,
        val expireTime: Long,
        val accessCount: Long,
        val lastAccessTime: Long = createTime
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
}
