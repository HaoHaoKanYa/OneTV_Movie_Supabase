package top.cywin.onetv.film.proxy

import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * 代理工具类
 * 
 * 基于 FongMi/TV 的代理工具实现
 * 提供缓存管理、带宽控制和连接处理等功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 本地代理缓存管理器
 */
class LocalProxyCacheManager(private var maxCacheSize: Long) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_CACHE"
        private const val CLEANUP_INTERVAL = 60000L // 1分钟
    }
    
    // 缓存存储
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheSize = AtomicLong(0)
    
    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    
    // 清理任务
    private var cleanupJob: Job? = null
    
    /**
     * 📦 获取缓存
     */
    fun get(key: String): ByteArray? {
        val entry = cache[key]
        
        return if (entry != null && !entry.isExpired()) {
            hitCount.incrementAndGet()
            entry.updateAccessTime()
            entry.data
        } else {
            if (entry != null) {
                remove(key)
            }
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 💾 存储缓存
     */
    fun put(key: String, data: ByteArray, ttl: Long = 3600000L) { // 默认1小时
        if (data.size > maxCacheSize / 10) { // 单个缓存不能超过总大小的10%
            Log.w(TAG, "⚠️ 缓存项过大，跳过: ${data.size} bytes")
            return
        }
        
        // 检查空间并清理
        ensureSpace(data.size.toLong())
        
        val entry = CacheEntry(
            data = data,
            createdTime = System.currentTimeMillis(),
            accessTime = System.currentTimeMillis(),
            ttl = ttl
        )
        
        cache[key] = entry
        cacheSize.addAndGet(data.size.toLong())
        
        Log.d(TAG, "💾 缓存存储: $key, 大小: ${data.size} bytes")
    }
    
    /**
     * 🗑️ 移除缓存
     */
    fun remove(key: String): Boolean {
        val entry = cache.remove(key)
        return if (entry != null) {
            cacheSize.addAndGet(-entry.data.size.toLong())
            Log.d(TAG, "🗑️ 缓存移除: $key")
            true
        } else {
            false
        }
    }
    
    /**
     * 🧹 清空所有缓存
     */
    fun clearAll() {
        cache.clear()
        cacheSize.set(0)
        Log.d(TAG, "🧹 所有缓存已清空")
    }
    
    /**
     * 🔧 确保有足够空间
     */
    private fun ensureSpace(requiredSize: Long) {
        while (cacheSize.get() + requiredSize > maxCacheSize && cache.isNotEmpty()) {
            evictLRU()
        }
    }
    
    /**
     * 🗑️ LRU 淘汰
     */
    private fun evictLRU() {
        val oldestEntry = cache.entries.minByOrNull { it.value.accessTime }
        if (oldestEntry != null) {
            remove(oldestEntry.key)
            evictionCount.incrementAndGet()
        }
    }
    
    /**
     * 🚀 启动清理任务
     */
    fun startCleanup() {
        cleanupJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    cleanup()
                    delay(CLEANUP_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 缓存清理异常", e)
                }
            }
        }
    }
    
    /**
     * 🧹 清理过期缓存
     */
    private fun cleanup() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries.filter { it.value.isExpired(now) }.map { it.key }
        
        expiredKeys.forEach { key ->
            remove(key)
        }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "🧹 清理过期缓存: ${expiredKeys.size} 项")
        }
    }
    
    /**
     * 🛑 停止清理任务
     */
    fun stop() {
        cleanupJob?.cancel()
    }
    
    /**
     * 🔧 更新最大缓存大小
     */
    fun updateMaxSize(newMaxSize: Long) {
        maxCacheSize = newMaxSize
        ensureSpace(0) // 立即检查并清理
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val hits = hitCount.get()
        val misses = missCount.get()
        val total = hits + misses
        
        return mapOf(
            "cache_size" to cache.size,
            "cache_size_bytes" to cacheSize.get(),
            "max_cache_size" to maxCacheSize,
            "hit_count" to hits,
            "miss_count" to misses,
            "hit_rate" to if (total > 0) hits.toDouble() / total else 0.0,
            "eviction_count" to evictionCount.get()
        )
    }
    
    /**
     * 🔑 生成缓存键
     */
    fun generateKey(url: String, headers: Map<String, String> = emptyMap()): String {
        val content = url + headers.entries.sortedBy { it.key }.joinToString { "${it.key}:${it.value}" }
        return md5(content)
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * 缓存条目
 */
data class CacheEntry(
    val data: ByteArray,
    val createdTime: Long,
    var accessTime: Long,
    val ttl: Long
) {
    
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return now - createdTime > ttl
    }
    
    fun updateAccessTime() {
        accessTime = System.currentTimeMillis()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as CacheEntry
        
        if (!data.contentEquals(other.data)) return false
        if (createdTime != other.createdTime) return false
        if (accessTime != other.accessTime) return false
        if (ttl != other.ttl) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + createdTime.hashCode()
        result = 31 * result + accessTime.hashCode()
        result = 31 * result + ttl.hashCode()
        return result
    }
}

/**
 * 带宽管理器
 */
class BandwidthManager(private var maxBandwidth: Long) {
    
    companion object {
        private const val TAG = "ONETV_FILM_BANDWIDTH_MANAGER"
        private const val WINDOW_SIZE = 1000L // 1秒窗口
    }
    
    // 带宽统计
    private val bytesTransferred = AtomicLong(0)
    private val lastResetTime = AtomicLong(System.currentTimeMillis())
    
    // 连接带宽跟踪
    private val connectionBandwidth = ConcurrentHashMap<String, ConnectionBandwidth>()
    
    /**
     * 🚦 检查是否可以传输
     */
    fun canTransfer(connectionId: String, bytes: Long): Boolean {
        if (maxBandwidth <= 0) return true // 无限制
        
        val now = System.currentTimeMillis()
        val windowStart = lastResetTime.get()
        
        // 重置窗口
        if (now - windowStart >= WINDOW_SIZE) {
            if (lastResetTime.compareAndSet(windowStart, now)) {
                bytesTransferred.set(0)
                connectionBandwidth.clear()
            }
        }
        
        val currentUsage = bytesTransferred.get()
        return currentUsage + bytes <= maxBandwidth
    }
    
    /**
     * 📊 记录传输
     */
    fun recordTransfer(connectionId: String, bytes: Long) {
        bytesTransferred.addAndGet(bytes)
        
        val connBandwidth = connectionBandwidth.computeIfAbsent(connectionId) {
            ConnectionBandwidth(connectionId)
        }
        connBandwidth.addBytes(bytes)
    }
    
    /**
     * ⏱️ 计算延迟
     */
    fun calculateDelay(connectionId: String, bytes: Long): Long {
        if (maxBandwidth <= 0) return 0
        
        val currentUsage = bytesTransferred.get()
        if (currentUsage + bytes <= maxBandwidth) return 0
        
        // 计算需要等待的时间
        val excessBytes = (currentUsage + bytes) - maxBandwidth
        return (excessBytes * WINDOW_SIZE) / maxBandwidth
    }
    
    /**
     * 🔧 更新最大带宽
     */
    fun updateMaxBandwidth(newMaxBandwidth: Long) {
        maxBandwidth = newMaxBandwidth
        Log.d(TAG, "🔧 带宽限制更新: $maxBandwidth bytes/s")
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        val windowStart = lastResetTime.get()
        val currentUsage = bytesTransferred.get()
        
        return mapOf(
            "max_bandwidth" to maxBandwidth,
            "current_usage" to currentUsage,
            "usage_percentage" to if (maxBandwidth > 0) (currentUsage.toDouble() / maxBandwidth) * 100 else 0.0,
            "window_remaining" to maxOf(0, WINDOW_SIZE - (now - windowStart)),
            "active_connections" to connectionBandwidth.size
        )
    }
}

/**
 * 连接带宽统计
 */
data class ConnectionBandwidth(
    val connectionId: String,
    private val bytesTransferred: AtomicLong = AtomicLong(0),
    private val startTime: Long = System.currentTimeMillis()
) {
    
    fun addBytes(bytes: Long) {
        bytesTransferred.addAndGet(bytes)
    }
    
    fun getBytesTransferred(): Long = bytesTransferred.get()
    
    fun getAverageBandwidth(): Long {
        val duration = System.currentTimeMillis() - startTime
        return if (duration > 0) (bytesTransferred.get() * 1000) / duration else 0
    }
}

/**
 * 本地代理连接处理器
 */
class LocalProxyConnection(
    private val connectionId: String,
    private val clientSocket: Socket,
    private val server: LocalProxy,
    private val hookManager: HookManager,
    private val cacheManager: LocalProxyCacheManager,
    private val bandwidthManager: BandwidthManager,
    private val config: LocalProxyConfig
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_LOCAL_PROXY_CONNECTION"
        private const val BUFFER_SIZE = 16384
    }
    
    private val isClosed = AtomicBoolean(false)
    
    /**
     * 🔗 处理连接
     */
    suspend fun handle() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔗 处理本地代理连接: $connectionId")
            
            val inputStream = clientSocket.getInputStream()
            val outputStream = clientSocket.getOutputStream()
            
            // 解析 HTTP 请求
            val request = parseHttpRequest(inputStream)
            if (request != null) {
                // 处理请求
                val response = processRequest(request)
                
                // 发送响应
                sendResponse(outputStream, response)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 本地代理连接处理异常: $connectionId", e)
        } finally {
            close()
        }
    }
    
    /**
     * 📥 解析 HTTP 请求
     */
    private suspend fun parseHttpRequest(inputStream: InputStream): ProxyRequest? {
        // 实现 HTTP 请求解析逻辑
        // 这里简化处理，实际应该完整解析 HTTP 协议
        return null
    }
    
    /**
     * 🚀 处理请求
     */
    private suspend fun processRequest(request: ProxyRequest): ProxyResponse {
        // 检查缓存
        val cacheKey = cacheManager.generateKey(request.url, request.headers)
        val cachedData = cacheManager.get(cacheKey)
        
        if (cachedData != null) {
            Log.d(TAG, "📦 从缓存获取: ${request.url}")
            return ProxyResponse(
                statusCode = 200,
                body = String(cachedData, Charsets.UTF_8)
            )
        }
        
        // 发送实际请求
        return try {
            val response = server.handleHttpRequest(request)
            
            // 缓存响应
            if (response.statusCode == 200 && response.body.isNotEmpty()) {
                cacheManager.put(cacheKey, response.body.toByteArray())
            }
            
            response
        } catch (e: Exception) {
            ProxyResponse(
                statusCode = 500,
                body = "请求处理失败: ${e.message}"
            )
        }
    }
    
    /**
     * 📤 发送响应
     */
    private suspend fun sendResponse(outputStream: OutputStream, response: ProxyResponse) {
        // 实现 HTTP 响应发送逻辑
        // 包括带宽控制和压缩处理
    }
    
    /**
     * 🔒 关闭连接
     */
    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                clientSocket.close()
                Log.d(TAG, "🔒 本地代理连接关闭: $connectionId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 关闭连接失败: $connectionId", e)
            }
        }
    }
}
