package top.cywin.onetv.film.cache

import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存相关数据模型
 * 
 * 基于 FongMi/TV 的缓存数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 缓存条目
 */
data class CacheEntry<T : Any>(
    val key: String,                    // 缓存键
    val value: T,                       // 缓存值
    val createTime: Long,               // 创建时间
    val lastAccessTime: Long,           // 最后访问时间
    val ttl: Long,                      // 生存时间（毫秒）
    val strategy: CacheStrategy,        // 缓存策略
    val size: Long                      // 大小（字节）
) {
    
    /**
     * ⏰ 是否过期
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return ttl > 0 && (now - createTime) > ttl
    }
    
    /**
     * 📊 获取摘要信息
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "key" to key,
            "create_time" to createTime,
            "last_access_time" to lastAccessTime,
            "ttl" to ttl,
            "strategy" to strategy.name,
            "size_bytes" to size,
            "size_kb" to String.format("%.2f", size / 1024.0),
            "is_expired" to isExpired(),
            "age_seconds" to (System.currentTimeMillis() - createTime) / 1000
        )
    }
}

/**
 * 可序列化的缓存条目
 */
@Serializable
data class SerializableCacheEntry(
    val key: String,                    // 缓存键
    val value: String,                  // 序列化后的值
    val valueClass: String,             // 值的类名
    val createTime: Long,               // 创建时间
    val lastAccessTime: Long,           // 最后访问时间
    val ttl: Long,                      // 生存时间（毫秒）
    val strategy: CacheStrategy,        // 缓存策略
    val size: Long                      // 大小（字节）
) {
    
    /**
     * ⏰ 是否过期
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return ttl > 0 && (now - createTime) > ttl
    }
}

/**
 * 缓存策略
 */
enum class CacheStrategy {
    MEMORY_ONLY,    // 仅内存缓存
    DISK_ONLY,      // 仅磁盘缓存
    MEMORY_FIRST,   // 内存优先，同时存储到磁盘
    DISK_FIRST      // 磁盘优先，同时存储到内存
}

/**
 * 缓存级别
 */
enum class CacheLevel {
    MEMORY,         // 内存缓存
    DISK,           // 磁盘缓存
    NETWORK         // 网络缓存
}

/**
 * 缓存配置
 */
data class CacheConfig(
    val maxMemoryCacheSize: Long = 50 * 1024 * 1024L,  // 最大内存缓存大小 50MB
    val maxDiskCacheSize: Long = 200 * 1024 * 1024L,   // 最大磁盘缓存大小 200MB
    val defaultTtl: Long = 3600000L,                    // 默认 TTL 1小时
    val enableDiskCache: Boolean = true,                // 是否启用磁盘缓存
    val enableCompression: Boolean = false,             // 是否启用压缩
    val cleanupInterval: Long = 1800000L,               // 清理间隔 30分钟
    val maxEntries: Int = 1000,                         // 最大条目数
    val enableMetrics: Boolean = true                   // 是否启用指标收集
) {
    
    /**
     * ✅ 验证配置
     */
    fun isValid(): Boolean {
        return maxMemoryCacheSize > 0 && 
               maxDiskCacheSize > 0 && 
               defaultTtl > 0 && 
               cleanupInterval > 0 && 
               maxEntries > 0
    }
}

/**
 * 缓存事件
 */
sealed class CacheEvent {
    
    /**
     * 条目添加
     */
    data class EntryAdded(val key: String, val strategy: CacheStrategy, val size: Long) : CacheEvent()
    
    /**
     * 条目访问
     */
    data class EntryAccessed(val key: String, val level: CacheLevel) : CacheEvent()
    
    /**
     * 条目未命中
     */
    data class EntryMissed(val key: String) : CacheEvent()
    
    /**
     * 条目移除
     */
    data class EntryRemoved(val key: String) : CacheEvent()
    
    /**
     * 条目过期
     */
    data class EntryExpired(val key: String) : CacheEvent()
    
    /**
     * 缓存清空
     */
    object CacheCleared : CacheEvent()
    
    /**
     * 过期条目清理
     */
    data class ExpiredEntriesCleared(val count: Int) : CacheEvent()
    
    /**
     * 缓存空间不足
     */
    data class CacheSpaceExhausted(val level: CacheLevel, val requiredSize: Long) : CacheEvent()
    
    /**
     * 缓存错误
     */
    data class CacheError(val operation: String, val error: String) : CacheEvent()
}

/**
 * 缓存统计
 */
class CacheStats {
    
    val hitCount = AtomicLong(0)
    val missCount = AtomicLong(0)
    val putCount = AtomicLong(0)
    val removeCount = AtomicLong(0)
    val errorCount = AtomicLong(0)
    val totalPutSize = AtomicLong(0)
    val totalRemoveSize = AtomicLong(0)
    
    private val levelHitCounts = mutableMapOf<CacheLevel, AtomicLong>()
    
    init {
        CacheLevel.values().forEach { level ->
            levelHitCounts[level] = AtomicLong(0)
        }
    }
    
    fun recordHit(level: CacheLevel) {
        hitCount.incrementAndGet()
        levelHitCounts[level]?.incrementAndGet()
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
    
    fun recordClear(size: Long) {
        totalRemoveSize.addAndGet(size)
    }
    
    fun recordError() {
        errorCount.incrementAndGet()
    }
    
    fun getHitRate(): Double {
        val total = hitCount.get() + missCount.get()
        return if (total > 0) hitCount.get().toDouble() / total else 0.0
    }
    
    fun getLevelHitRate(level: CacheLevel): Double {
        val levelHits = levelHitCounts[level]?.get() ?: 0L
        val totalHits = hitCount.get()
        return if (totalHits > 0) levelHits.toDouble() / totalHits else 0.0
    }
    
    fun getStats(): Map<String, Any> {
        val total = hitCount.get() + missCount.get()
        
        return mapOf(
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "put_count" to putCount.get(),
            "remove_count" to removeCount.get(),
            "error_count" to errorCount.get(),
            "hit_rate" to getHitRate(),
            "total_requests" to total,
            "total_put_size_bytes" to totalPutSize.get(),
            "total_remove_size_bytes" to totalRemoveSize.get(),
            "level_hit_rates" to levelHitCounts.mapValues { (level, count) ->
                getLevelHitRate(level)
            },
            "level_hit_counts" to levelHitCounts.mapValues { it.value.get() }
        )
    }
    
    fun clear() {
        hitCount.set(0)
        missCount.set(0)
        putCount.set(0)
        removeCount.set(0)
        errorCount.set(0)
        totalPutSize.set(0)
        totalRemoveSize.set(0)
        levelHitCounts.values.forEach { it.set(0) }
    }
}

/**
 * 缓存键构建器
 */
class CacheKeyBuilder {
    
    private val parts = mutableListOf<String>()
    
    fun add(part: String): CacheKeyBuilder {
        parts.add(part)
        return this
    }
    
    fun add(part: Any): CacheKeyBuilder {
        parts.add(part.toString())
        return this
    }
    
    fun build(): String {
        return parts.joinToString(":")
    }
    
    companion object {
        fun create(): CacheKeyBuilder = CacheKeyBuilder()
        
        fun create(vararg parts: Any): String {
            return parts.joinToString(":")
        }
    }
}

/**
 * 缓存预热配置
 */
data class CacheWarmupConfig(
    val enabled: Boolean = false,              // 是否启用预热
    val warmupKeys: List<String> = emptyList(), // 预热键列表
    val warmupDelay: Long = 5000L,             // 预热延迟（毫秒）
    val maxConcurrency: Int = 3,               // 最大并发数
    val timeout: Long = 30000L                 // 超时时间（毫秒）
)

/**
 * 缓存压缩配置
 */
data class CacheCompressionConfig(
    val enabled: Boolean = false,              // 是否启用压缩
    val algorithm: CompressionAlgorithm = CompressionAlgorithm.GZIP, // 压缩算法
    val minSize: Long = 1024L,                 // 最小压缩大小
    val compressionLevel: Int = 6              // 压缩级别
)

/**
 * 压缩算法
 */
enum class CompressionAlgorithm {
    GZIP,       // GZIP 压缩
    DEFLATE,    // DEFLATE 压缩
    LZ4         // LZ4 压缩
}

/**
 * 缓存淘汰策略
 */
enum class EvictionPolicy {
    LRU,        // 最近最少使用
    LFU,        // 最少使用频率
    FIFO,       // 先进先出
    TTL         // 基于 TTL
}

/**
 * 缓存同步策略
 */
enum class SyncStrategy {
    NONE,           // 不同步
    WRITE_THROUGH,  // 写穿透
    WRITE_BEHIND,   // 写回
    REFRESH_AHEAD   // 提前刷新
}

/**
 * 缓存操作结果
 */
sealed class CacheResult<T> {
    
    /**
     * ✅ 成功结果
     */
    data class Success<T>(val value: T, val level: CacheLevel) : CacheResult<T>()
    
    /**
     * ❌ 失败结果
     */
    data class Failure<T>(val error: String, val exception: Exception? = null) : CacheResult<T>()
    
    /**
     * 📭 未命中结果
     */
    class Miss<T> : CacheResult<T>()
    
    companion object {
        fun <T> success(value: T, level: CacheLevel) = Success(value, level)
        fun <T> failure(error: String, exception: Exception? = null) = Failure<T>(error, exception)
        fun <T> miss() = Miss<T>()
    }
    
    /**
     * ✅ 是否成功
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * ❌ 是否失败
     */
    val isFailure: Boolean get() = this is Failure
    
    /**
     * 📭 是否未命中
     */
    val isMiss: Boolean get() = this is Miss
}

/**
 * 缓存监控指标
 */
data class CacheMetrics(
    val hitRate: Double,                        // 命中率
    val missRate: Double,                       // 未命中率
    val averageLoadTime: Long,                  // 平均加载时间
    val evictionCount: Long,                    // 淘汰次数
    val memoryUsage: Long,                      // 内存使用量
    val diskUsage: Long,                        // 磁盘使用量
    val entryCount: Int,                        // 条目数量
    val requestCount: Long,                     // 请求总数
    val errorRate: Double                       // 错误率
) {
    
    /**
     * 📊 获取摘要信息
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "hit_rate" to String.format("%.2f%%", hitRate * 100),
            "miss_rate" to String.format("%.2f%%", missRate * 100),
            "average_load_time_ms" to averageLoadTime,
            "eviction_count" to evictionCount,
            "memory_usage_mb" to String.format("%.2f", memoryUsage / 1024.0 / 1024.0),
            "disk_usage_mb" to String.format("%.2f", diskUsage / 1024.0 / 1024.0),
            "entry_count" to entryCount,
            "request_count" to requestCount,
            "error_rate" to String.format("%.2f%%", errorRate * 100)
        )
    }
}
