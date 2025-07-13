package top.cywin.onetv.film.cache

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.proxy.ProxyManager
import java.io.File

/**
 * 代理缓存
 * 
 * 基于 FongMi/TV 的代理配置缓存实现
 * 用于持久化代理配置和状态
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class ProxyCache private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_PROXY_CACHE"
        private const val CACHE_DIR = "proxy_cache"
        private const val CONFIG_FILE = "proxy_configs.json"
        private const val STATUS_FILE = "proxy_status.json"
        
        @Volatile
        private var INSTANCE: ProxyCache? = null
        
        fun getInstance(context: Context): ProxyCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProxyCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val cacheDir = File(context.cacheDir, CACHE_DIR)
    private val configFile = File(cacheDir, CONFIG_FILE)
    private val statusFile = File(cacheDir, STATUS_FILE)
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 💾 保存代理配置列表
     */
    fun saveConfigs(configs: List<ProxyManager.ProxyConfig>) {
        try {
            val jsonString = json.encodeToString(configs)
            configFile.writeText(jsonString)
            Log.d(TAG, "保存代理配置: ${configs.size}个")
        } catch (e: Exception) {
            Log.e(TAG, "保存代理配置失败", e)
        }
    }
    
    /**
     * 📖 获取所有代理配置
     */
    fun getAllConfigs(): List<ProxyManager.ProxyConfig> {
        return try {
            if (!configFile.exists()) {
                return emptyList()
            }
            
            val jsonString = configFile.readText()
            json.decodeFromString<List<ProxyManager.ProxyConfig>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "读取代理配置失败", e)
            emptyList()
        }
    }
    
    /**
     * 💾 保存代理状态
     */
    fun saveStatus(statuses: Map<String, ProxyManager.ProxyStatus>) {
        try {
            val jsonString = json.encodeToString(statuses)
            statusFile.writeText(jsonString)
            Log.d(TAG, "保存代理状态: ${statuses.size}个")
        } catch (e: Exception) {
            Log.e(TAG, "保存代理状态失败", e)
        }
    }
    
    /**
     * 📖 获取所有代理状态
     */
    fun getAllStatus(): Map<String, ProxyManager.ProxyStatus> {
        return try {
            if (!statusFile.exists()) {
                return emptyMap()
            }
            
            val jsonString = statusFile.readText()
            json.decodeFromString<Map<String, ProxyManager.ProxyStatus>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "读取代理状态失败", e)
            emptyMap()
        }
    }
    
    /**
     * 🧹 清除所有缓存
     */
    fun clear() {
        try {
            if (configFile.exists()) {
                configFile.delete()
            }
            if (statusFile.exists()) {
                statusFile.delete()
            }
            Log.d(TAG, "清除代理缓存完成")
        } catch (e: Exception) {
            Log.e(TAG, "清除代理缓存失败", e)
        }
    }
    
    /**
     * 📊 获取缓存大小
     */
    fun getCacheSize(): Long {
        return try {
            var size = 0L
            if (configFile.exists()) {
                size += configFile.length()
            }
            if (statusFile.exists()) {
                size += statusFile.length()
            }
            size
        } catch (e: Exception) {
            0L
        }
    }
}
