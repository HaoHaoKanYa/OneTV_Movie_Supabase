package top.cywin.onetv.movie.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 点播配置管理器 (完全参考OneMoVie VodConfig实现)
 */
@Singleton
class VodConfigManager @Inject constructor(
    private val cacheManager: MovieCacheManager
) {
    
    companion object {
        @Volatile
        private var INSTANCE: VodConfigManager? = null
        
        fun getInstance(): VodConfigManager {
            return INSTANCE ?: throw IllegalStateException("VodConfigManager not initialized")
        }
        
        internal fun setInstance(instance: VodConfigManager) {
            INSTANCE = instance
        }
    }
    
    private var config: VodConfigResponse? = null
    private var sites: List<VodSite> = emptyList()
    private var parses: List<VodParse> = emptyList()
    private var home: VodSite? = null
    private var parse: VodParse? = null
    
    init {
        setInstance(this)
    }
    
    /**
     * 加载配置 (参考OneMoVie loadConfig逻辑)
     */
    suspend fun load(configResponse: VodConfigResponse): Result<String> = withContext(Dispatchers.IO) {
        try {
            this@VodConfigManager.config = configResponse
            clear()
            
            // 解析站点
            initSites(configResponse)
            
            // 解析解析器
            initParses(configResponse)
            
            // 设置默认站点和解析器
            initDefaults()
            
            Result.success("配置加载成功")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清理配置
     */
    private fun clear() {
        sites = emptyList()
        parses = emptyList()
        home = null
        parse = null
    }
    
    /**
     * 初始化站点 (参考OneMoVie initSite)
     */
    private fun initSites(config: VodConfigResponse) {
        sites = config.sites.map { site ->
            site.copy(
                // 转换URL
                api = convertUrl(site.api),
                ext = convertUrl(site.ext),
                // 设置JAR包
                jar = parseJar(site, config.spider)
            )
        }
        
        // 设置默认站点
        home = sites.firstOrNull()
    }
    
    /**
     * 初始化解析器 (参考OneMoVie initParse)
     */
    private fun initParses(config: VodConfigResponse) {
        val parseList = config.parses.toMutableList()
        
        // 添加神解析器 (参考OneMoVie Parse.god())
        if (parseList.isNotEmpty()) {
            parseList.add(0, VodParse.god())
        }
        
        parses = parseList
        
        // 设置默认解析器
        parse = parses.firstOrNull()
    }
    
    /**
     * 设置默认值
     */
    private fun initDefaults() {
        if (home == null && sites.isNotEmpty()) {
            home = sites.first()
        }
        
        if (parse == null && parses.isNotEmpty()) {
            parse = parses.first()
        }
    }
    
    /**
     * 转换URL (处理相对路径等)
     */
    private fun convertUrl(url: String): String {
        // 简单的URL转换逻辑，可以根据需要扩展
        return url.trim()
    }
    
    /**
     * 解析JAR包地址
     */
    private fun parseJar(site: VodSite, spider: String): String {
        return when {
            site.jar.isNotEmpty() -> site.jar
            spider.isNotEmpty() -> spider
            else -> ""
        }
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): VodConfigResponse? = config
    
    /**
     * 获取所有站点
     */
    fun getSites(): List<VodSite> = sites
    
    /**
     * 获取所有解析器
     */
    fun getParses(): List<VodParse> = parses
    
    /**
     * 获取默认站点
     */
    fun getHomeSite(): VodSite? = home
    
    /**
     * 获取默认解析器
     */
    fun getParse(): VodParse? = parse
    
    /**
     * 根据key获取站点
     */
    fun getSite(key: String): VodSite? {
        return sites.find { it.key == key }
    }
    
    /**
     * 根据名称获取解析器
     */
    fun getParse(name: String): VodParse? {
        return parses.find { it.name == name }
    }
    
    /**
     * 设置默认站点
     */
    fun setHomeSite(site: VodSite) {
        home = site
    }
    
    /**
     * 设置默认解析器
     */
    fun setDefaultParse(parse: VodParse) {
        this.parse = parse
    }
    
    /**
     * 是否已加载配置
     */
    fun isConfigLoaded(): Boolean = config != null && sites.isNotEmpty()
    
    /**
     * 获取配置摘要
     */
    fun getConfigSummary(): String {
        return "站点: ${sites.size}, 解析器: ${parses.size}"
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): VodConfigResponse? = config

    /**
     * 获取当前站点
     */
    fun getCurrentSite(): VodSite? {
        // 如果没有配置站点，返回一个默认站点避免崩溃
        return home ?: getDefaultSite()
    }

    /**
     * 获取默认站点（临时解决方案）
     */
    private fun getDefaultSite(): VodSite {
        return VodSite(
            key = "default",
            name = "默认站点",
            api = "https://example.com/api.php/provide/vod/",
            ext = "",
            jar = "",
            type = 1,
            searchable = 1,
            changeable = 1,
            timeout = 30000,
            header = emptyMap(),
            style = null,
            categories = emptyList()
        )
    }

    /**
     * 设置当前站点
     */
    fun setCurrentSite(site: VodSite) {
        home = site
    }

    /**
     * 获取所有站点
     */
    fun getAllSites(): List<VodSite> = sites

    /**
     * 根据flag获取解析器
     */
    fun getParseByFlag(flag: String): VodParse? {
        return parses.find { it.name == flag || it.url.contains(flag) }
    }

    /**
     * 获取配置统计
     */
    fun getConfigStats(): Map<String, Any> {
        return mapOf(
            "sites" to sites.size,
            "parses" to parses.size,
            "loaded" to isConfigLoaded()
        )
    }
}
