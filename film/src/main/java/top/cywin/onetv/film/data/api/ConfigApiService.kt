package top.cywin.onetv.film.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.cache.CacheFactory
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.network.HttpClient
import top.cywin.onetv.film.spider.SpiderManager
import java.util.concurrent.ConcurrentHashMap

/**
 * 配置 API 服务
 * 
 * 基于 FongMi/TV 标准的配置管理服务
 * 负责配置文件的加载、解析和管理
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class ConfigApiService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ONETV_CONFIG_API"
        private const val CONFIG_CACHE_KEY = "api_config"
        private const val SITES_CACHE_KEY = "sites_config"
        private const val PARSES_CACHE_KEY = "parses_config"
        
        @Volatile
        private var INSTANCE: ConfigApiService? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): ConfigApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigApiService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // 核心组件
    private val httpClient = HttpClient.getInstance(context)
    private val spiderManager = SpiderManager.getInstance(context)
    private val configCache = CacheFactory.getConfigCache(context)
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
    // 配置数据
    private val _config = MutableStateFlow<VodConfigResponse?>(null)
    val config: StateFlow<VodConfigResponse?> = _config.asStateFlow()
    
    private val _sites = MutableStateFlow<List<VodSite>>(emptyList())
    val sites: StateFlow<List<VodSite>> = _sites.asStateFlow()
    
    private val _parses = MutableStateFlow<List<VodParse>>(emptyList())
    val parses: StateFlow<List<VodParse>> = _parses.asStateFlow()
    
    // 内存缓存
    private val sitesMap = ConcurrentHashMap<String, VodSite>()
    private val parsesMap = ConcurrentHashMap<String, VodParse>()
    private val activatedSites = ConcurrentHashMap<String, Boolean>()
    
    // 状态
    private var isLoading = false
    private var lastLoadTime = 0L
    
    /**
     * 🔧 加载配置文件
     */
    suspend fun loadConfig(configUrl: String): Result<VodConfigResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (isLoading) {
                    return@withContext Result.failure(Exception("配置正在加载中"))
                }
                
                isLoading = true
                Log.d(TAG, "开始加载配置: $configUrl")
                
                // 1. 下载配置文件
                val response = httpClient.get(configUrl)
                if (!response.isSuccessful) {
                    throw Exception("配置下载失败: ${response.code}")
                }
                
                val configText = response.body?.string() ?: throw Exception("配置内容为空")
                
                // 2. 解析配置
                val config = json.decodeFromString<VodConfigResponse>(configText)
                
                // 3. 验证配置
                if (config.isEmpty()) {
                    throw Exception("配置文件为空")
                }
                
                // 4. 处理站点配置
                processSites(config.sites)
                
                // 5. 处理解析器配置
                processParses(config.parses)
                
                // 6. 加载 JAR 文件
                if (config.spider.isNotEmpty()) {
                    spiderManager.loadJar(config.spider)
                }
                
                // 7. 缓存配置
                cacheConfig(config)
                
                // 8. 更新状态
                _config.value = config
                lastLoadTime = System.currentTimeMillis()
                
                Log.d(TAG, "配置加载成功: 站点=${config.sites.size}, 解析器=${config.parses.size}")
                Result.success(config)
                
            } catch (e: Exception) {
                Log.e(TAG, "配置加载失败", e)
                Result.failure(e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 🔧 从缓存加载配置
     */
    suspend fun loadFromCache(): Result<VodConfigResponse?> {
        return withContext(Dispatchers.IO) {
            try {
                val configJson = configCache.get(CONFIG_CACHE_KEY)
                if (configJson.isNullOrEmpty()) {
                    return@withContext Result.success(null)
                }
                
                val config = json.decodeFromString<VodConfigResponse>(configJson)
                
                // 恢复站点和解析器
                processSites(config.sites)
                processParses(config.parses)
                
                _config.value = config
                lastLoadTime = System.currentTimeMillis()
                
                Log.d(TAG, "从缓存加载配置成功")
                Result.success(config)
                
            } catch (e: Exception) {
                Log.e(TAG, "从缓存加载配置失败", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 🔧 处理站点配置
     */
    private suspend fun processSites(sites: List<VodSite>) {
        sitesMap.clear()
        
        val enabledSites = sites.filter { !it.isHidden() }
        enabledSites.forEach { site ->
            sitesMap[site.key] = site
            activatedSites[site.key] = true
        }
        
        _sites.value = enabledSites
        
        // 缓存站点配置
        try {
            val sitesJson = json.encodeToString(VodSite.serializer(), enabledSites)
            configCache.put(SITES_CACHE_KEY, sitesJson)
        } catch (e: Exception) {
            Log.w(TAG, "缓存站点配置失败", e)
        }
        
        Log.d(TAG, "处理站点配置完成: ${enabledSites.size}个站点")
    }
    
    /**
     * 🔧 处理解析器配置
     */
    private suspend fun processParses(parses: List<VodParse>) {
        parsesMap.clear()
        
        val enabledParses = parses.filter { it.type != null && it.type >= 0 }
        enabledParses.forEach { parse ->
            parsesMap[parse.name] = parse
        }
        
        _parses.value = enabledParses
        
        // 缓存解析器配置
        try {
            val parsesJson = json.encodeToString(VodParse.serializer(), enabledParses)
            configCache.put(PARSES_CACHE_KEY, parsesJson)
        } catch (e: Exception) {
            Log.w(TAG, "缓存解析器配置失败", e)
        }
        
        Log.d(TAG, "处理解析器配置完成: ${enabledParses.size}个解析器")
    }
    
    /**
     * 🔧 缓存配置
     */
    private suspend fun cacheConfig(config: VodConfigResponse) {
        try {
            val configJson = json.encodeToString(VodConfigResponse.serializer(), config)
            configCache.put(CONFIG_CACHE_KEY, configJson)
        } catch (e: Exception) {
            Log.w(TAG, "缓存配置失败", e)
        }
    }
    
    /**
     * 🔍 根据 key 获取站点
     */
    fun getSite(key: String): VodSite? {
        return sitesMap[key]
    }
    
    /**
     * 🔍 根据名称获取解析器
     */
    fun getParse(name: String): VodParse? {
        return parsesMap[name]
    }
    
    /**
     * 🔍 获取所有启用的站点
     */
    fun getEnabledSites(): List<VodSite> {
        return _sites.value
    }
    
    /**
     * 🔍 获取所有启用的解析器
     */
    fun getEnabledParses(): List<VodParse> {
        return _parses.value
    }
    
    /**
     * 🔍 获取可搜索的站点
     */
    fun getSearchableSites(): List<VodSite> {
        return _sites.value.filter { it.searchable == 1 }
    }
    
    /**
     * 🔍 检查站点是否激活
     */
    fun isSiteActivated(key: String): Boolean {
        return activatedSites[key] == true
    }
    
    /**
     * 🔧 激活/停用站点
     */
    fun setSiteActivated(key: String, activated: Boolean) {
        activatedSites[key] = activated
    }
    
    /**
     * 🔍 是否有配置
     */
    fun hasConfig(): Boolean {
        return _config.value != null
    }
    
    /**
     * 🔍 是否正在加载
     */
    fun isLoading(): Boolean {
        return isLoading
    }
    
    /**
     * 🔍 获取最后加载时间
     */
    fun getLastLoadTime(): Long {
        return lastLoadTime
    }
    
    /**
     * 🧹 清除配置
     */
    suspend fun clearConfig() {
        withContext(Dispatchers.IO) {
            _config.value = null
            _sites.value = emptyList()
            _parses.value = emptyList()
            
            sitesMap.clear()
            parsesMap.clear()
            activatedSites.clear()
            
            configCache.clear()
            spiderManager.clearJar()
            
            lastLoadTime = 0L
            
            Log.d(TAG, "配置已清除")
        }
    }
    
    /**
     * 📊 获取配置统计信息
     */
    fun getStats(): Map<String, Any> {
        val config = _config.value
        return mapOf(
            "has_config" to hasConfig(),
            "is_loading" to isLoading(),
            "sites_count" to _sites.value.size,
            "parses_count" to _parses.value.size,
            "searchable_sites" to getSearchableSites().size,
            "activated_sites" to activatedSites.values.count { it },
            "has_spider" to (config?.spider?.isNotEmpty() == true),
            "last_load_time" to lastLoadTime,
            "config_version" to (config?.version ?: ""),
            "config_source" to (config?.source ?: "")
        )
    }
}
