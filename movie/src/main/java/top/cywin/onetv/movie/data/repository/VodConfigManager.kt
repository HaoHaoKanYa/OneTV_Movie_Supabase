package top.cywin.onetv.movie.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.config.AppConfigManager
import top.cywin.onetv.movie.data.models.*
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 点播配置管理器 (完全基于OneMoVie VodConfig架构)
 * 实现单例模式，支持配置加载、站点管理、解析器管理、仓库配置等
 */
@Singleton
class VodConfigManager @Inject constructor(
    private val context: Context,
    private val appConfigManager: AppConfigManager
) {

    companion object {
        private const val TAG = "VodConfigManager"

        @Volatile
        private var INSTANCE: VodConfigManager? = null

        fun get(): VodConfigManager {
            return INSTANCE ?: throw IllegalStateException("VodConfigManager not initialized")
        }

        fun initialize(instance: VodConfigManager) {
            INSTANCE = instance
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("movie_config", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // 核心配置数据 (参考OneMoVie架构)
    private var config: VodConfig? = null
    private var sites: List<VodSite> = emptyList()
    private var parses: List<VodParse> = emptyList()
    private var home: VodSite? = null
    private var parse: VodParse? = null
    private var depotConfigs: List<VodDepotConfig> = emptyList()

    init {
        initialize(this)
    }

    /**
     * 加载配置 (参考OneMoVie load方法)
     */
    suspend fun load(config: VodConfig, callback: ConfigCallback? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                this@VodConfigManager.config = config
                clear()

                val jsonContent = fetchConfigJson(config.url)
                parseConfig(jsonContent, callback)

                Result.success("配置加载成功")
            } catch (e: Exception) {
                Log.e(TAG, "配置加载失败", e)
                loadCache(callback, e)
            }
        }
    }

    /**
     * 获取配置JSON (通过Edge Functions)
     */
    private suspend fun fetchConfigJson(url: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val requestUrl = "${appConfigManager.getSupabaseUrl()}/functions/v1/vod-config?url=$encodedUrl"

        val request = Request.Builder()
            .url(requestUrl)
            .header("Authorization", "Bearer ${appConfigManager.getApiKey()}")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            return response.body?.string() ?: throw Exception("配置内容为空")
        } else {
            throw Exception("获取配置失败: ${response.code}")
        }
    }

    /**
     * 解析配置JSON (参考OneMoVie parseConfig)
     */
    private fun parseConfig(jsonContent: String, callback: ConfigCallback?) {
        val jsonObject = Json.parseToJsonElement(jsonContent).jsonObject

        // 检查是否为仓库配置
        if (jsonObject.containsKey("urls")) {
            parseDepot(jsonObject, callback)
            return
        }

        // 解析站点
        initSites(jsonObject)

        // 解析解析器
        initParses(jsonObject)

        // 设置默认站点和解析器
        initDefaults()

        // 缓存配置
        config?.copy(json = jsonContent)?.let { saveConfig(it) }

        callback?.onSuccess("配置加载成功")
    }

    /**
     * 初始化站点 (参考OneMoVie initSite)
     */
    private fun initSites(jsonObject: JsonObject) {
        val sitesArray = jsonObject["sites"]?.jsonArray ?: return

        sites = sitesArray.map { element ->
            json.decodeFromJsonElement<VodSite>(element).apply {
                // 转换URL (参考OneMoVie UrlUtil.convert)
                api = convertUrl(api)
                ext = convertUrl(ext)
                // 设置JAR包
                jar = parseJar(this, jsonObject["spider"]?.jsonPrimitive?.content ?: "")
            }
        }

        // 设置默认站点
        val homeKey = config?.home
        if (!homeKey.isNullOrEmpty()) {
            home = sites.find { it.key == homeKey }
        }
        if (home == null && sites.isNotEmpty()) {
            home = sites.first()
        }
    }

    /**
     * 初始化解析器 (参考OneMoVie initParse)
     */
    private fun initParses(jsonObject: JsonObject) {
        val parsesArray = jsonObject["parses"]?.jsonArray ?: return

        parses = parsesArray.map { element ->
            json.decodeFromJsonElement<VodParse>(element)
        }.toMutableList()

        // 添加神解析器 (参考OneMoVie Parse.god())
        if (parses.isNotEmpty()) {
            parses = listOf(VodParse.god()) + parses
        }

        // 设置默认解析器
        val parseKey = config?.parse
        if (!parseKey.isNullOrEmpty()) {
            parse = parses.find { it.name == parseKey }
        }
        if (parse == null && parses.isNotEmpty()) {
            parse = parses.first()
        }
    }

    /**
     * 设置默认值
     */
    private fun initDefaults() {
        // 设置默认站点
        if (home == null && sites.isNotEmpty()) {
            home = sites.first()
        }

        // 设置默认解析器
        if (parse == null && parses.isNotEmpty()) {
            parse = parses.first()
        }
    }

    /**
     * 清除数据
     */
    private fun clear() {
        sites = emptyList()
        parses = emptyList()
        home = null
        parse = null
        depotConfigs = emptyList()
    }

    /**
     * 解析仓库配置 (参考OneMoVie parseDepot)
     */
    private fun parseDepot(jsonObject: JsonObject, callback: ConfigCallback?) {
        val urlsArray = jsonObject["urls"]?.jsonArray ?: return

        depotConfigs = urlsArray.map { element ->
            val configObject = element.jsonObject
            VodDepotConfig(
                name = configObject["name"]?.jsonPrimitive?.content ?: "未知配置",
                url = configObject["url"]?.jsonPrimitive?.content ?: "",
                desc = configObject["desc"]?.jsonPrimitive?.content ?: ""
            )
        }

        callback?.onDepotSuccess(depotConfigs)
    }

    /**
     * 加载缓存配置 (参考OneMoVie loadCache)
     */
    private fun loadCache(callback: ConfigCallback?, error: Exception): Result<String> {
        val cachedConfig = loadConfigFromPrefs()
        return if (cachedConfig != null) {
            config = cachedConfig
            parseConfig(cachedConfig.json, callback)
            Result.success("使用缓存配置")
        } else {
            callback?.onError("配置加载失败: ${error.message}")
            Result.failure(error)
        }
    }

    /**
     * 解析JAR包地址 (参考OneMoVie parseJar)
     */
    private fun parseJar(site: VodSite, spider: String): String {
        return when {
            site.jar.isNotEmpty() -> convertUrl(site.jar)
            spider.isNotEmpty() -> convertUrl(spider)
            else -> ""
        }
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): VodConfig? {
        return config ?: loadConfigFromPrefs()
    }

    /**
     * 获取所有站点 (参考OneMoVie getSites)
     */
    fun getSites(): List<VodSite> = sites

    /**
     * 获取所有解析器 (参考OneMoVie getParses)
     */
    fun getParses(): List<VodParse> = parses

    /**
     * 获取默认站点 (参考OneMoVie getHome)
     */
    fun getHomeSite(): VodSite? = home

    /**
     * 获取当前解析器 (参考OneMoVie getParse)
     */
    fun getParse(): VodParse? = parse

    /**
     * 根据名称获取解析器
     */
    fun getParse(name: String): VodParse? {
        return parses.find { it.name == name }
    }

    /**
     * 获取当前站点
     */
    fun getCurrentSite(siteKey: String = ""): VodSite? {
        return if (siteKey.isNotEmpty()) {
            sites.find { it.key == siteKey }
        } else {
            home
        }
    }

    /**
     * 设置当前站点
     */
    fun setCurrentSite(siteKey: String) {
        currentSiteKey = siteKey
        prefs.edit().putString("current_site_key", siteKey).apply()
    }

    /**
     * 获取所有站点
     */
    fun getAllSites(): List<VodSite> {
        return getCurrentConfig()?.sites ?: emptyList()
    }

    /**
     * 获取可搜索的站点
     */
    fun getSearchableSites(): List<VodSite> {
        return getAllSites().filter { it.isSearchable() }
    }

    /**
     * 获取解析器列表
     */
    fun getParses(): List<VodParse> {
        return getCurrentConfig()?.parses ?: emptyList()
    }

    /**
     * 根据flag获取解析器
     */
    fun getParseByFlag(flag: String): VodParse? {
        return getParses().find { it.name == flag }
    }

    /**
     * URL转换 (参考OneMoVie UrlUtil.convert)
     */
    private fun convertUrl(url: String): String {
        if (url.isEmpty()) return url
        
        // 处理相对路径
        if (url.startsWith("./")) {
            return url.substring(2)
        }
        
        // 处理base64编码
        if (url.startsWith("data:")) {
            return url
        }
        
        // 处理本地文件
        if (url.startsWith("file://")) {
            return url
        }
        
        // 处理HTTP/HTTPS
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        // 默认添加https
        return if (url.contains("://")) url else "https://$url"
    }

    /**
     * 保存配置到SharedPreferences
     */
    private fun saveConfigToPrefs(config: VodConfig) {
        try {
            val configJson = json.encodeToString(VodConfig.serializer(), config)
            prefs.edit()
                .putString("vod_config", configJson)
                .putLong("config_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从SharedPreferences加载配置
     */
    private fun loadConfigFromPrefs(): VodConfig? {
        return try {
            val configJson = prefs.getString("vod_config", null) ?: return null
            val config = json.decodeFromString(VodConfig.serializer(), configJson)
            
            // 恢复当前站点
            currentSiteKey = prefs.getString("current_site_key", "") ?: ""
            if (currentSiteKey.isEmpty() && config.sites.isNotEmpty()) {
                currentSiteKey = config.sites.first().key
            }
            
            config
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查配置是否过期
     */
    fun isConfigExpired(): Boolean {
        val timestamp = prefs.getLong("config_timestamp", 0)
        val now = System.currentTimeMillis()
        val expireTime = 24 * 60 * 60 * 1000 // 24小时
        
        return (now - timestamp) > expireTime
    }

    /**
     * 清除配置
     */
    fun clearConfig() {
        currentConfig = null
        currentSiteKey = ""
        prefs.edit().clear().apply()
    }

    /**
     * 获取配置统计信息
     */
    fun getConfigStats(): Map<String, Int> {
        val config = getCurrentConfig()
        return mapOf(
            "sites" to (config?.sites?.size ?: 0),
            "searchable_sites" to getSearchableSites().size,
            "parses" to (config?.parses?.size ?: 0),
            "lives" to (config?.lives?.size ?: 0)
        )
    }

    /**
     * 验证配置有效性
     */
    fun validateConfig(config: VodConfig): Boolean {
        return config.sites.isNotEmpty() && 
               config.sites.any { it.api.isNotEmpty() }
    }
}
