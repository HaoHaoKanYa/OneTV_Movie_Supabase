package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.data.models.VodConfigResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 配置专用缓存
 * 基于 FongMi/TV 标准实现
 * 
 * 专门用于缓存各种配置信息，提供高效的配置管理
 * 
 * 功能：
 * - 站点配置缓存
 * - 应用配置缓存
 * - 用户配置缓存
 * - 智能过期管理
 * - 版本控制
 * - 增量更新
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object ConfigCache {
    
    private const val TAG = "ONETV_FILM_CONFIG_CACHE"
    private const val CACHE_DIR = "config_cache"
    private const val METADATA_FILE = "config_cache_metadata.json"
    private const val DEFAULT_TTL = 7200000L // 2小时
    private const val SITE_CONFIG_TTL = 3600000L // 1小时
    private const val APP_CONFIG_TTL = 1800000L // 30分钟
    
    // 内存缓存
    private val configCache = ConcurrentHashMap<String, ConfigEntry>()
    private val siteConfigCache = ConcurrentHashMap<String, VodSite>()
    private val appConfigCache = ConcurrentHashMap<String, String>()
    
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
     * 🔧 初始化配置缓存
     */
    fun initialize(context: Context) {
        if (initialized) return
        
        cacheDir = File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
        
        loadPersistedConfigs()
        startCleanupTask()
        initialized = true
        
        Log.d(TAG, "🔧 配置缓存初始化完成")
    }
    
    /**
     * 💾 缓存站点配置
     */
    suspend fun putSiteConfig(siteKey: String, config: VodSite): Boolean = withContext(Dispatchers.IO) {
        try {
            siteConfigCache[siteKey] = config
            
            val entry = ConfigEntry(
                key = "site_$siteKey",
                value = json.encodeToString(config),
                type = ConfigType.SITE,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + SITE_CONFIG_TTL,
                version = config.hashCode().toString()
            )
            
            configCache["site_$siteKey"] = entry
            saveConfigToDisk(entry)
            
            putCount.incrementAndGet()
            Log.d(TAG, "💾 站点配置已缓存: $siteKey")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 站点配置缓存失败: $siteKey", e)
            false
        }
    }
    
    /**
     * 📦 获取站点配置
     */
    suspend fun getSiteConfig(siteKey: String): VodSite? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            var config = siteConfigCache[siteKey]
            if (config != null) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 站点配置内存缓存命中: $siteKey")
                return@withContext config
            }
            
            // 从持久化缓存获取
            val entry = configCache["site_$siteKey"] ?: loadConfigFromDisk("site_$siteKey")
            if (entry != null && !entry.isExpired()) {
                config = json.decodeFromString<VodSite>(entry.value)
                siteConfigCache[siteKey] = config
                
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 站点配置磁盘缓存命中: $siteKey")
                return@withContext config
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 站点配置缓存未命中: $siteKey")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 站点配置获取失败: $siteKey", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 💾 缓存应用配置
     */
    suspend fun putAppConfig(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            appConfigCache[key] = value
            
            val entry = ConfigEntry(
                key = "app_$key",
                value = value,
                type = ConfigType.APP,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + APP_CONFIG_TTL,
                version = value.hashCode().toString()
            )
            
            configCache["app_$key"] = entry
            saveConfigToDisk(entry)
            
            putCount.incrementAndGet()
            Log.d(TAG, "💾 应用配置已缓存: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 应用配置缓存失败: $key", e)
            false
        }
    }
    
    /**
     * 📦 获取应用配置
     */
    suspend fun getAppConfig(key: String): String? = withContext(Dispatchers.IO) {
        try {
            // 先从内存缓存获取
            var config = appConfigCache[key]
            if (config != null) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 应用配置内存缓存命中: $key")
                return@withContext config
            }
            
            // 从持久化缓存获取
            val entry = configCache["app_$key"] ?: loadConfigFromDisk("app_$key")
            if (entry != null && !entry.isExpired()) {
                config = entry.value
                appConfigCache[key] = config
                
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 应用配置磁盘缓存命中: $key")
                return@withContext config
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 应用配置缓存未命中: $key")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 应用配置获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 💾 缓存完整配置响应
     */
    suspend fun putConfigResponse(key: String, response: VodConfigResponse): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = ConfigEntry(
                key = "response_$key",
                value = json.encodeToString(response),
                type = ConfigType.RESPONSE,
                createTime = System.currentTimeMillis(),
                expireTime = System.currentTimeMillis() + DEFAULT_TTL,
                version = response.hashCode().toString()
            )
            
            configCache["response_$key"] = entry
            saveConfigToDisk(entry)
            
            putCount.incrementAndGet()
            Log.d(TAG, "💾 配置响应已缓存: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置响应缓存失败: $key", e)
            false
        }
    }
    
    /**
     * 📦 获取完整配置响应
     */
    suspend fun getConfigResponse(key: String): VodConfigResponse? = withContext(Dispatchers.IO) {
        try {
            val entry = configCache["response_$key"] ?: loadConfigFromDisk("response_$key")
            if (entry != null && !entry.isExpired()) {
                hitCount.incrementAndGet()
                Log.d(TAG, "📦 配置响应缓存命中: $key")
                return@withContext json.decodeFromString<VodConfigResponse>(entry.value)
            }
            
            missCount.incrementAndGet()
            Log.d(TAG, "📦 配置响应缓存未命中: $key")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置响应获取失败: $key", e)
            missCount.incrementAndGet()
            null
        }
    }
    
    /**
     * 🔄 检查配置版本
     */
    fun checkConfigVersion(key: String, currentVersion: String): Boolean {
        val entry = configCache[key]
        return entry?.version == currentVersion
    }
    
    /**
     * 🗑️ 移除配置
     */
    suspend fun removeConfig(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            configCache.remove(key)
            siteConfigCache.remove(key.removePrefix("site_"))
            appConfigCache.remove(key.removePrefix("app_"))
            
            val file = File(cacheDir, "$key.config")
            if (file.exists()) {
                file.delete()
            }
            
            Log.d(TAG, "🗑️ 配置已移除: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置移除失败: $key", e)
            false
        }
    }
    
    /**
     * 🧹 清空所有配置缓存
     */
    suspend fun clearConfig(): Boolean = withContext(Dispatchers.IO) {
        try {
            configCache.clear()
            siteConfigCache.clear()
            appConfigCache.clear()
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".config")) {
                    file.delete()
                }
            }
            
            Log.d(TAG, "🧹 所有配置缓存已清空")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空配置缓存失败", e)
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
            "config_cache_size" to configCache.size,
            "site_config_cache_size" to siteConfigCache.size,
            "app_config_cache_size" to appConfigCache.size,
            "hit_count" to hitCount.get(),
            "miss_count" to missCount.get(),
            "put_count" to putCount.get(),
            "hit_rate" to hitRate,
            "disk_config_files" to getDiskConfigFileCount()
        )
    }
    
    /**
     * 💾 保存配置到磁盘
     */
    private suspend fun saveConfigToDisk(entry: ConfigEntry) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "${entry.key}.config")
            val jsonString = json.encodeToString(entry)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置磁盘保存失败: ${entry.key}", e)
        }
    }
    
    /**
     * 📂 从磁盘加载配置
     */
    private suspend fun loadConfigFromDisk(key: String): ConfigEntry? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, "$key.config")
            if (file.exists()) {
                val jsonString = file.readText()
                return@withContext json.decodeFromString<ConfigEntry>(jsonString)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置磁盘加载失败: $key", e)
            null
        }
    }
    
    /**
     * 📋 加载持久化配置
     */
    private fun loadPersistedConfigs() {
        scope.launch {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".config")) {
                        val key = file.nameWithoutExtension
                        val entry = loadConfigFromDisk(key)
                        if (entry != null && !entry.isExpired()) {
                            configCache[key] = entry
                        }
                    }
                }
                Log.d(TAG, "📋 持久化配置加载完成: ${configCache.size} 个")
            } catch (e: Exception) {
                Log.e(TAG, "❌ 持久化配置加载失败", e)
            }
        }
    }
    
    /**
     * 🧹 启动清理任务
     */
    private fun startCleanupTask() {
        scope.launch {
            while (true) {
                delay(600000L) // 10分钟
                cleanupExpiredConfigs()
            }
        }
    }
    
    /**
     * 🧹 清理过期配置
     */
    private suspend fun cleanupExpiredConfigs() = withContext(Dispatchers.IO) {
        try {
            val expiredKeys = configCache.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                removeConfig(key)
            }
            
            if (expiredKeys.isNotEmpty()) {
                Log.d(TAG, "🧹 清理过期配置: ${expiredKeys.size} 个")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期配置失败", e)
        }
    }
    
    /**
     * 📊 获取磁盘配置文件数量
     */
    private fun getDiskConfigFileCount(): Int {
        return try {
            cacheDir.listFiles()?.count { it.name.endsWith(".config") } ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 配置类型枚举
     */
    enum class ConfigType {
        SITE, APP, USER, RESPONSE
    }
    
    /**
     * 配置条目数据类
     */
    @kotlinx.serialization.Serializable
    private data class ConfigEntry(
        val key: String,
        val value: String,
        val type: ConfigType,
        val createTime: Long,
        val expireTime: Long,
        val version: String,
        val lastAccessTime: Long = createTime
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
}
