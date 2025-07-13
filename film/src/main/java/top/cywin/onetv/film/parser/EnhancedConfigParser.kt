package top.cywin.onetv.film.parser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import top.cywin.onetv.film.cache.CacheManager
import top.cywin.onetv.film.jar.JarLoader
import top.cywin.onetv.film.network.OkHttpManager
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.data.models.VodConfigResponse
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils

/**
 * 增强配置解析器
 * 
 * 基于 FongMi/TV 的配置解析系统
 * 负责解析 TVBOX 配置文件，这是整个系统的入口
 * 
 * 核心功能：
 * - TVBOX 配置文件解析
 * - 站点配置验证
 * - JAR 包配置处理
 * - 配置缓存管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EnhancedConfigParser(
    private val context: Context,
    private val httpManager: OkHttpManager,
    private val jarLoader: JarLoader,
    private val cacheManager: CacheManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_ENHANCED_CONFIG_PARSER"
        private const val CONFIG_CACHE_KEY = "tvbox_config"
        private const val CONFIG_CACHE_DURATION = 30 * 60 * 1000L // 30分钟
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * 解析配置 URL
     * 
     * 这是 FongMi/TV 配置解析的核心入口
     */
    suspend fun parseConfig(configUrl: String): Result<VodConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔧 开始解析配置: $configUrl")
            
            // 1. 检查缓存
            val cachedConfig = getCachedConfig(configUrl)
            if (cachedConfig != null) {
                Log.d(TAG, "✅ 使用缓存配置")
                return@withContext Result.success(cachedConfig)
            }
            
            // 2. 从网络获取配置
            val configData = httpManager.get(configUrl)
            
            // 3. 解析配置数据
            val configResponse = parseConfigData(configData, configUrl)
            
            // 4. 缓存配置
            cacheConfig(configUrl, configResponse)
            
            Log.d(TAG, "✅ 配置解析完成，站点数量: ${configResponse.sites.size}")
            Result.success(configResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置解析失败: $configUrl", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析配置数据
     * 
     * 按照 FongMi/TV 的配置格式解析
     */
    private suspend fun parseConfigData(configData: String, configUrl: String): VodConfigResponse {
        return try {
            val jsonElement = json.parseToJsonElement(configData)
            val jsonObject = jsonElement.jsonObject
            
            // 解析站点列表
            val sites = mutableListOf<VodSite>()
            
            // 处理 sites 字段
            jsonObject["sites"]?.jsonArray?.forEach { siteElement ->
                try {
                    val site = parseSiteConfig(siteElement)
                    if (site != null) {
                        sites.add(site)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 站点解析失败: ${e.message}")
                }
            }
            
            // 解析其他配置
            val spider = jsonObject["spider"]?.toString()?.trim('"') ?: ""
            val wallpaper = jsonObject["wallpaper"]?.toString()?.trim('"') ?: ""
            val lives = jsonObject["lives"]?.jsonArray?.map { it.toString().trim('"') } ?: emptyList()
            
            VodConfigResponse(
                sites = sites,
                spider = spider,
                wallpaper = wallpaper,
                lives = lives,
                configUrl = configUrl
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 配置数据解析失败", e)
            throw e
        }
    }
    
    /**
     * 解析单个站点配置
     * 
     * 按照 FongMi/TV 的站点配置格式
     */
    private fun parseSiteConfig(siteElement: JsonElement): VodSite? {
        return try {
            val siteObject = siteElement.jsonObject
            
            val key = siteObject["key"]?.toString()?.trim('"') ?: return null
            val name = siteObject["name"]?.toString()?.trim('"') ?: return null
            val type = siteObject["type"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            val api = siteObject["api"]?.toString()?.trim('"') ?: return null
            
            // 解析扩展字段
            val searchable = siteObject["searchable"]?.toString()?.trim('"')?.toIntOrNull() ?: 1
            val quickSearch = siteObject["quickSearch"]?.toString()?.trim('"')?.toIntOrNull() ?: 1
            val filterable = siteObject["filterable"]?.toString()?.trim('"')?.toIntOrNull() ?: 1
            val changeable = siteObject["changeable"]?.toString()?.trim('"')?.toIntOrNull() ?: 1
            
            // 解析 JAR 包配置
            val jar = siteObject["jar"]?.toString()?.trim('"') ?: ""
            
            // 解析扩展配置
            val ext = siteObject["ext"] ?: JsonUtils.createEmptyJsonElement()
            
            // 解析请求头
            val header = siteObject["header"]?.jsonObject?.mapValues { 
                it.value.toString().trim('"') 
            } ?: emptyMap()
            
            VodSite(
                key = key,
                name = name,
                type = type,
                api = api,
                searchable = searchable,
                quickSearch = quickSearch,
                filterable = filterable,
                changeable = changeable,
                jar = jar,
                ext = ext,
                header = header,
                enabled = true
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 站点配置解析失败: ${e.message}")
            null
        }
    }
    
    /**
     * 验证站点配置
     */
    private fun validateSiteConfig(site: VodSite): Boolean {
        return try {
            // 基本字段验证
            if (site.key.isBlank() || site.name.isBlank() || site.api.isBlank()) {
                return false
            }
            
            // API URL 验证
            if (!StringUtils.isValidUrl(site.api)) {
                return false
            }
            
            // 类型验证
            if (site.type !in 0..4) {
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 站点配置验证失败: ${site.key}", e)
            false
        }
    }
    
    /**
     * 获取缓存配置
     */
    private suspend fun getCachedConfig(configUrl: String): VodConfigResponse? {
        return try {
            val cacheKey = "$CONFIG_CACHE_KEY:${StringUtils.md5(configUrl)}"
            cacheManager.get(cacheKey, VodConfigResponse::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 获取缓存配置失败", e)
            null
        }
    }
    
    /**
     * 缓存配置
     */
    private suspend fun cacheConfig(configUrl: String, config: VodConfigResponse) {
        try {
            val cacheKey = "$CONFIG_CACHE_KEY:${StringUtils.md5(configUrl)}"
            cacheManager.put(cacheKey, config, CONFIG_CACHE_DURATION)
            Log.d(TAG, "✅ 配置已缓存: $cacheKey")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存配置失败", e)
        }
    }
    
    /**
     * 清除配置缓存
     */
    suspend fun clearConfigCache() {
        try {
            cacheManager.clearByPrefix(CONFIG_CACHE_KEY)
            Log.d(TAG, "✅ 配置缓存已清除")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 清除配置缓存失败", e)
        }
    }
    
    /**
     * 获取配置统计信息
     */
    fun getConfigStats(): Map<String, Any> {
        return mapOf(
            "parser_type" to "EnhancedConfigParser",
            "cache_enabled" to true,
            "cache_duration_minutes" to (CONFIG_CACHE_DURATION / 60000),
            "supported_formats" to listOf("JSON", "TVBOX"),
            "features" to listOf("缓存", "验证", "JAR支持", "扩展字段")
        )
    }
}
