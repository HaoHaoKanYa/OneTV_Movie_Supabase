package top.cywin.onetv.film.proxy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Hosts 重定向管理器
 * 基于 FongMi/TV 标准实现
 * 
 * 提供域名到IP地址的映射管理功能
 * 
 * 功能：
 * - 域名IP映射管理
 * - 动态hosts更新
 * - DNS缓存管理
 * - 域名解析优化
 * - 访问统计监控
 * - 持久化存储
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object HostsManager {
    
    private const val TAG = "ONETV_FILM_HOSTS_MANAGER"
    private const val HOSTS_FILE = "film_hosts.json"
    private const val DNS_CACHE_TTL = 300000L // 5分钟
    private const val MAX_CACHE_SIZE = 1000
    
    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // JSON序列化器
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    
    // Hosts映射
    private val hostsMap = ConcurrentHashMap<String, HostEntry>()
    
    // DNS缓存
    private val dnsCache = ConcurrentHashMap<String, DnsCacheEntry>()
    
    // 统计信息
    private val resolveCount = AtomicLong(0)
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    
    // 缓存目录
    private lateinit var cacheDir: File
    private var initialized = false
    
    /**
     * 🔧 初始化Hosts管理器
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        cacheDir = File(context.cacheDir, "hosts_cache").apply {
            if (!exists()) mkdirs()
        }
        
        loadHostsFromFile()
        startCleanupTask()
        initialized = true
        
        Log.d(TAG, "🔧 Hosts管理器初始化完成")
    }
    
    /**
     * 🔧 添加域名映射
     */
    fun addHost(domain: String, ip: String, description: String = "", ttl: Long = 0L) {
        try {
            val entry = HostEntry(
                domain = domain.lowercase(),
                ip = ip,
                description = description,
                ttl = if (ttl > 0) ttl else Long.MAX_VALUE,
                createTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis(),
                enabled = true
            )
            
            hostsMap[domain.lowercase()] = entry
            
            // 清除相关DNS缓存
            clearDnsCache(domain)
            
            // 保存到文件
            saveHostsToFile()
            
            Log.d(TAG, "🔧 添加域名映射: $domain -> $ip")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 添加域名映射失败: $domain -> $ip", e)
        }
    }
    
    /**
     * 🗑️ 移除域名映射
     */
    fun removeHost(domain: String) {
        try {
            val removed = hostsMap.remove(domain.lowercase())
            if (removed != null) {
                // 清除相关DNS缓存
                clearDnsCache(domain)
                
                // 保存到文件
                saveHostsToFile()
                
                Log.d(TAG, "🗑️ 移除域名映射: $domain")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 移除域名映射失败: $domain", e)
        }
    }
    
    /**
     * 🔍 解析域名
     */
    suspend fun resolveHost(domain: String): String? = withContext(Dispatchers.IO) {
        try {
            resolveCount.incrementAndGet()
            val lowerDomain = domain.lowercase()
            
            // 1. 检查Hosts映射
            val hostEntry = hostsMap[lowerDomain]
            if (hostEntry != null && hostEntry.enabled && !hostEntry.isExpired()) {
                hitCount.incrementAndGet()
                Log.d(TAG, "🎯 Hosts映射命中: $domain -> ${hostEntry.ip}")
                return@withContext hostEntry.ip
            }
            
            // 2. 检查DNS缓存
            val cacheEntry = dnsCache[lowerDomain]
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 DNS缓存命中: $domain -> ${cacheEntry.ip}")
                return@withContext cacheEntry.ip
            }
            
            // 3. 执行DNS解析
            val resolvedIp = performDnsLookup(domain)
            if (resolvedIp != null) {
                // 缓存DNS结果
                cacheDnsResult(lowerDomain, resolvedIp)
                Log.d(TAG, "🔍 DNS解析成功: $domain -> $resolvedIp")
                return@withContext resolvedIp
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "❌ 域名解析失败: $domain")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 域名解析异常: $domain", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 🔍 执行DNS查询
     */
    private suspend fun performDnsLookup(domain: String): String? = withContext(Dispatchers.IO) {
        try {
            val address = InetAddress.getByName(domain)
            address.hostAddress
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ DNS查询失败: $domain", e)
            null
        }
    }
    
    /**
     * 💾 缓存DNS结果
     */
    private fun cacheDnsResult(domain: String, ip: String) {
        try {
            // 检查缓存大小
            if (dnsCache.size >= MAX_CACHE_SIZE) {
                cleanupOldDnsCache()
            }
            
            val cacheEntry = DnsCacheEntry(
                domain = domain,
                ip = ip,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + DNS_CACHE_TTL
            )
            
            dnsCache[domain] = cacheEntry
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ DNS缓存失败: $domain", e)
        }
    }
    
    /**
     * 🧹 清理DNS缓存
     */
    private fun clearDnsCache(domain: String) {
        dnsCache.remove(domain.lowercase())
    }
    
    /**
     * 🧹 清理旧的DNS缓存
     */
    private fun cleanupOldDnsCache() {
        try {
            val expiredEntries = dnsCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredEntries.forEach { domain ->
                dnsCache.remove(domain)
            }
            
            // 如果还是太多，移除最旧的条目
            if (dnsCache.size >= MAX_CACHE_SIZE) {
                val oldestEntries = dnsCache.entries
                    .sortedBy { it.value.createTime }
                    .take(MAX_CACHE_SIZE / 4) // 移除25%
                    .map { it.key }
                
                oldestEntries.forEach { domain ->
                    dnsCache.remove(domain)
                }
            }
            
            Log.d(TAG, "🧹 DNS缓存清理完成，当前大小: ${dnsCache.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ DNS缓存清理失败", e)
        }
    }
    
    /**
     * 📂 从文件加载Hosts
     */
    private fun loadHostsFromFile() {
        scope.launch {
            try {
                val hostsFile = File(cacheDir, HOSTS_FILE)
                if (hostsFile.exists()) {
                    val jsonContent = hostsFile.readText()
                    val hostsList = json.decodeFromString<List<HostEntry>>(jsonContent)
                    
                    for (entry in hostsList) {
                        if (!entry.isExpired()) {
                            hostsMap[entry.domain] = entry
                        }
                    }
                    
                    Log.d(TAG, "📂 Hosts文件加载完成: ${hostsMap.size} 条记录")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hosts文件加载失败", e)
            }
        }
    }
    
    /**
     * 💾 保存Hosts到文件
     */
    private fun saveHostsToFile() {
        scope.launch {
            try {
                val hostsFile = File(cacheDir, HOSTS_FILE)
                val hostsList = hostsMap.values.toList()
                val jsonContent = json.encodeToString(hostsList)
                
                hostsFile.writeText(jsonContent)
                
                Log.d(TAG, "💾 Hosts文件保存完成: ${hostsList.size} 条记录")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Hosts文件保存失败", e)
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
                cleanupExpiredEntries()
            }
        }
    }
    
    /**
     * 🧹 清理过期条目
     */
    private suspend fun cleanupExpiredEntries() = withContext(Dispatchers.IO) {
        try {
            // 清理过期的Hosts条目
            val expiredHosts = hostsMap.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredHosts.forEach { domain ->
                hostsMap.remove(domain)
            }
            
            // 清理过期的DNS缓存
            cleanupOldDnsCache()
            
            if (expiredHosts.isNotEmpty()) {
                saveHostsToFile()
                Log.d(TAG, "🧹 清理过期条目: Hosts ${expiredHosts.size} 个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期条目失败", e)
        }
    }
    
    /**
     * 📋 获取所有域名映射
     */
    fun getAllHosts(): List<HostEntry> {
        return hostsMap.values.toList()
    }
    
    /**
     * 🔧 启用/禁用域名映射
     */
    fun setHostEnabled(domain: String, enabled: Boolean) {
        val entry = hostsMap[domain.lowercase()]
        if (entry != null) {
            hostsMap[domain.lowercase()] = entry.copy(
                enabled = enabled,
                updateTime = System.currentTimeMillis()
            )
            
            if (!enabled) {
                clearDnsCache(domain)
            }
            
            saveHostsToFile()
            Log.d(TAG, "🔧 域名映射状态更新: $domain -> $enabled")
        }
    }
    
    /**
     * 🧹 清空所有映射
     */
    fun clearHosts() {
        hostsMap.clear()
        dnsCache.clear()
        saveHostsToFile()
        Log.d(TAG, "🧹 所有域名映射已清空")
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        val totalRequests = resolveCount.get()
        val hitRate = if (totalRequests > 0) {
            (hitCount.get().toDouble() / totalRequests * 100)
        } else 0.0
        
        return mapOf(
            "hosts_count" to hostsMap.size,
            "dns_cache_size" to dnsCache.size,
            "resolve_count" to resolveCount.get(),
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "hit_rate" to hitRate,
            "enabled_hosts" to hostsMap.values.count { it.enabled },
            "expired_hosts" to hostsMap.values.count { it.isExpired() }
        )
    }
    
    /**
     * 🔄 重置统计
     */
    fun resetStats() {
        resolveCount.set(0)
        hitCount.set(0)
        missCount.set(0)
    }
    
    /**
     * 🛑 关闭管理器
     */
    fun shutdown() {
        scope.cancel()
        saveHostsToFile()
        Log.d(TAG, "🛑 Hosts管理器已关闭")
    }
}

/**
 * Hosts条目数据类
 */
@Serializable
data class HostEntry(
    val domain: String,
    val ip: String,
    val description: String = "",
    val ttl: Long = Long.MAX_VALUE,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val enabled: Boolean = true
) {
    
    /**
     * ⏰ 检查是否过期
     */
    fun isExpired(): Boolean {
        return if (ttl == Long.MAX_VALUE) {
            false
        } else {
            System.currentTimeMillis() > createTime + ttl
        }
    }
    
    /**
     * 📋 复制条目
     */
    fun copy(
        domain: String = this.domain,
        ip: String = this.ip,
        description: String = this.description,
        ttl: Long = this.ttl,
        enabled: Boolean = this.enabled
    ): HostEntry {
        return HostEntry(
            domain = domain,
            ip = ip,
            description = description,
            ttl = ttl,
            createTime = this.createTime,
            updateTime = System.currentTimeMillis(),
            enabled = enabled
        )
    }
}

/**
 * DNS缓存条目数据类
 */
private data class DnsCacheEntry(
    val domain: String,
    val ip: String,
    val createTime: Long,
    val expireTime: Long
) {
    
    /**
     * ⏰ 检查是否过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expireTime
    }
}
