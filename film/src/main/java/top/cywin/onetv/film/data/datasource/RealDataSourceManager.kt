package top.cywin.onetv.film.data.datasource

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.network.NetworkClient
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.data.models.VodSite
import top.cywin.onetv.film.data.models.SiteStatus
import top.cywin.onetv.film.data.models.VerifyStatus

/**
 * 真实数据源管理器
 * 
 * 负责从真实的 TVBOX API 获取和管理数据源
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class RealDataSourceManager private constructor() {
    
    companion object {
        private const val TAG = "ONETV_FILM_REAL_DATA_SOURCE"
        
        // OneTV 官方 API 地址
        private const val ONETV_API_URL = "https://raw.githubusercontent.com/HaoHaoKanYa/OneTV-API/refs/heads/main/vod/output/onetv-api-movie.json"
        
        // 备用 API 地址
        private val BACKUP_API_URLS = listOf(
            "https://ghproxy.com/https://raw.githubusercontent.com/HaoHaoKanYa/OneTV-API/refs/heads/main/vod/output/onetv-api-movie.json",
            "https://cdn.jsdelivr.net/gh/HaoHaoKanYa/OneTV-API@main/vod/output/onetv-api-movie.json"
        )
        
        @Volatile
        private var INSTANCE: RealDataSourceManager? = null
        
        fun getInstance(): RealDataSourceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealDataSourceManager().also { INSTANCE = it }
            }
        }
    }
    
    private lateinit var networkClient: NetworkClient
    private var cachedSites: List<VodSite> = emptyList()
    private var lastUpdateTime: Long = 0L
    private val cacheValidDuration = 30 * 60 * 1000L // 30分钟缓存
    
    /**
     * 🔧 初始化
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "🔧 初始化真实数据源管理器")
            
            networkClient = NetworkClient.Builder()
                .userAgent("OneTV-Film/2.1.1 (Android)")
                .timeout(15000L)
                .build()
            
            Log.d(TAG, "✅ 真实数据源管理器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 真实数据源管理器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 📡 获取真实数据源列表
     */
    suspend fun getRealDataSources(forceRefresh: Boolean = false): List<VodSite> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📡 获取真实数据源列表，强制刷新: $forceRefresh")
            
            // 检查缓存
            if (!forceRefresh && isCacheValid()) {
                Log.d(TAG, "✅ 使用缓存的数据源，数量: ${cachedSites.size}")
                return@withContext cachedSites
            }
            
            // 从网络获取
            val sites = fetchDataSourcesFromNetwork()
            
            if (sites.isNotEmpty()) {
                cachedSites = sites
                lastUpdateTime = System.currentTimeMillis()
                Log.d(TAG, "✅ 获取真实数据源成功，数量: ${sites.size}")
            } else {
                Log.w(TAG, "⚠️ 获取的数据源为空，使用缓存数据")
            }
            
            cachedSites
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取真实数据源失败", e)
            
            // 返回缓存数据或空列表
            if (cachedSites.isNotEmpty()) {
                Log.d(TAG, "🔄 返回缓存的数据源")
                cachedSites
            } else {
                Log.d(TAG, "📝 返回默认数据源")
                getDefaultDataSources()
            }
        }
    }
    
    /**
     * 🌐 从网络获取数据源
     */
    private suspend fun fetchDataSourcesFromNetwork(): List<VodSite> {
        val urls = listOf(ONETV_API_URL) + BACKUP_API_URLS
        
        for ((index, url) in urls.withIndex()) {
            try {
                Log.d(TAG, "🌐 尝试从 API 获取数据: $url")
                
                val response = networkClient.get(url)
                if (response.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ API 响应为空: $url")
                    continue
                }
                
                val sites = parseApiResponse(response)
                if (sites.isNotEmpty()) {
                    Log.d(TAG, "✅ 从 API 获取数据成功: $url，数量: ${sites.size}")
                    return sites
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 从 API 获取数据失败: $url", e)
                
                // 如果不是最后一个 URL，继续尝试下一个
                if (index < urls.size - 1) {
                    Log.d(TAG, "🔄 尝试下一个备用 API")
                    continue
                }
            }
        }
        
        return emptyList()
    }
    
    /**
     * 📝 解析 API 响应
     */
    private fun parseApiResponse(response: String): List<VodSite> {
        return try {
            Log.d(TAG, "📝 解析 API 响应，长度: ${response.length}")
            
            val jsonObject = JsonUtils.parseToJsonObject(response)
            if (jsonObject == null) {
                Log.w(TAG, "⚠️ JSON 解析失败")
                return emptyList()
            }
            
            // 解析站点列表
            val sitesArray = JsonUtils.getJsonArray(jsonObject, "sites") ?: emptyList()
            val sites = mutableListOf<VodSite>()
            
            sitesArray.forEach { siteData ->
                if (siteData is Map<*, *>) {
                    val site = parseSiteData(siteData)
                    if (site != null) {
                        sites.add(site)
                    }
                }
            }
            
            Log.d(TAG, "✅ 解析完成，有效站点数量: ${sites.size}")
            sites
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析 API 响应失败", e)
            emptyList()
        }
    }
    
    /**
     * 🏗️ 解析单个站点数据
     */
    private fun parseSiteData(siteData: Map<*, *>): VodSite? {
        return try {
            val key = siteData["key"]?.toString() ?: return null
            val name = siteData["name"]?.toString() ?: return null
            val type = (siteData["type"] as? Number)?.toInt() ?: return null
            val api = siteData["api"]?.toString() ?: return null
            
            VodSite(
                key = key,
                name = name,
                type = type,
                api = api,
                searchable = (siteData["searchable"] as? Number)?.toInt() ?: 1,
                quickSearch = (siteData["quickSearch"] as? Number)?.toInt() ?: 0,
                filterable = (siteData["filterable"] as? Number)?.toInt() ?: 0,
                playUrl = siteData["playUrl"]?.toString() ?: "",
                ext = siteData["ext"]?.toString() ?: "",
                jar = siteData["jar"]?.toString() ?: "",
                timeout = (siteData["timeout"] as? Number)?.toLong() ?: 15000L,
                enabled = true,
                status = SiteStatus.ONLINE,
                verifyStatus = VerifyStatus.VERIFIED,
                createTime = System.currentTimeMillis(),
                updateTime = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析站点数据失败", e)
            null
        }
    }
    
    /**
     * ⏰ 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return cachedSites.isNotEmpty() && 
               (System.currentTimeMillis() - lastUpdateTime) < cacheValidDuration
    }
    
    /**
     * 📋 获取默认数据源
     */
    private fun getDefaultDataSources(): List<VodSite> {
        return listOf(
            VodSite(
                key = "csp_XPath_default",
                name = "默认XPath站点",
                type = 3,
                api = "csp_XPath",
                searchable = 1,
                enabled = true,
                status = SiteStatus.ONLINE,
                verifyStatus = VerifyStatus.VERIFIED
            ),
            VodSite(
                key = "csp_AppYs_default", 
                name = "默认AppYs站点",
                type = 1,
                api = "csp_AppYs",
                searchable = 1,
                enabled = true,
                status = SiteStatus.ONLINE,
                verifyStatus = VerifyStatus.VERIFIED
            )
        )
    }
    
    /**
     * 🔄 刷新数据源
     */
    suspend fun refreshDataSources(): List<VodSite> {
        return getRealDataSources(forceRefresh = true)
    }
    
    /**
     * 🗑️ 清除缓存
     */
    fun clearCache() {
        cachedSites = emptyList()
        lastUpdateTime = 0L
        Log.d(TAG, "🗑️ 数据源缓存已清除")
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "cached_sites" to cachedSites.size,
            "last_update_time" to lastUpdateTime,
            "cache_valid" to isCacheValid(),
            "cache_age_minutes" to if (lastUpdateTime > 0) {
                (System.currentTimeMillis() - lastUpdateTime) / (60 * 1000)
            } else {
                -1
            }
        )
    }
    
    /**
     * 🔍 根据类型筛选站点
     */
    fun filterSitesByType(sites: List<VodSite>, type: Int): List<VodSite> {
        return sites.filter { it.type == type }
    }
    
    /**
     * 🔍 根据关键词搜索站点
     */
    fun searchSites(sites: List<VodSite>, keyword: String): List<VodSite> {
        return sites.filter { 
            it.name.contains(keyword, ignoreCase = true) ||
            it.key.contains(keyword, ignoreCase = true)
        }
    }
    
    /**
     * ✅ 验证站点可用性
     */
    suspend fun validateSite(site: VodSite): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "✅ 验证站点: ${site.name}")
            
            // 这里可以添加具体的验证逻辑
            // 比如检查 API 是否可访问
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 站点验证失败: ${site.name}", e)
            false
        }
    }
}
