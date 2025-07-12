package top.cywin.onetv.movie.data.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.models.VodResponse
import top.cywin.onetv.movie.data.network.TvboxNetworkConfig
import java.util.concurrent.TimeUnit

/**
 * APP站点处理器 - 处理TVBOX APP类型站点（type=3）
 * 支持移动应用接口、自定义API格式等
 */
class AppSiteProcessor(private val context: Context) {
    
    private val httpClient = TvboxNetworkConfig.createTvboxHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(site: VodSite): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "📱 APP站点获取首页内容: ${site.name}")

            // 检查是否为真正的APP站点
            if (!isTrueAppSite(site)) {
                Log.d("ONETV_MOVIE", "⚠️ 站点 ${site.name} 不是真正的APP站点，跳过APP处理")
                return@withContext emptyList()
            }

            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ APP站点API为空")
                return@withContext emptyList()
            }
            
            // 构建请求
            val request = buildAppRequest(site, "home")
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ APP站点请求失败: HTTP ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                Log.d("ONETV_MOVIE", "✅ APP站点响应成功，长度: ${responseBody.length}")
                
                // 解析响应
                parseAppResponse(responseBody)
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类列表
     */
    suspend fun getCategories(site: VodSite): List<VodClass> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "📱 APP站点获取分类列表: ${site.name}")

            // 检查是否为真正的APP站点
            if (!isTrueAppSite(site)) {
                Log.d("ONETV_MOVIE", "⚠️ 站点 ${site.name} 不是真正的APP站点，跳过APP处理")
                return@withContext emptyList()
            }

            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ APP站点API为空")
                return@withContext emptyList()
            }
            
            // 构建请求
            val request = buildAppRequest(site, "categories")
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ APP站点分类请求失败: HTTP ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                Log.d("ONETV_MOVIE", "✅ APP站点分类响应成功，长度: ${responseBody.length}")
                
                // 解析分类响应
                parseAppCategories(responseBody)
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类内容
     */
    suspend fun getCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "📱 APP站点获取分类内容: ${site.name}, 分类: $typeId, 页码: $page")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ APP站点API为空")
                return@withContext emptyList()
            }
            
            // 构建请求
            val request = buildAppRequest(site, "category", mapOf(
                "type" to typeId,
                "page" to page.toString()
            ))
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ APP站点分类内容请求失败: HTTP ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                Log.d("ONETV_MOVIE", "✅ APP站点分类内容响应成功，长度: ${responseBody.length}")
                
                // 解析响应
                parseAppResponse(responseBody)
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索内容
     */
    suspend fun search(site: VodSite, keyword: String): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "📱 APP站点搜索: ${site.name}, 关键词: $keyword")
            
            if (site.api.isBlank()) {
                Log.w("ONETV_MOVIE", "⚠️ APP站点API为空")
                return@withContext emptyList()
            }
            
            // 构建搜索请求
            val request = buildAppRequest(site, "search", mapOf("keyword" to keyword))
            
            // 执行请求
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ONETV_MOVIE", "⚠️ APP站点搜索请求失败: HTTP ${response.code}")
                    return@withContext emptyList()
                }
                
                val responseBody = response.body?.string() ?: ""
                Log.d("ONETV_MOVIE", "✅ APP站点搜索响应成功，长度: ${responseBody.length}")
                
                // 解析响应
                parseAppResponse(responseBody)
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ APP站点搜索失败", e)
            emptyList()
        }
    }
    
    /**
     * 构建APP请求
     */
    private fun buildAppRequest(site: VodSite, action: String, params: Map<String, String> = emptyMap()): Request {
        val requestBuilder = Request.Builder()
        
        // 根据不同的action构建不同的请求
        when (action) {
            "home" -> {
                // 首页请求
                requestBuilder.url("${site.api}?ac=list")
            }
            "categories" -> {
                // 分类请求
                requestBuilder.url("${site.api}?ac=list")
            }
            "category" -> {
                // 分类内容请求
                val typeId = params["type"] ?: "1"
                val page = params["page"] ?: "1"
                requestBuilder.url("${site.api}?ac=list&t=$typeId&pg=$page")
            }
            "search" -> {
                // 搜索请求
                val keyword = params["keyword"] ?: ""
                requestBuilder.url("${site.api}?ac=list&wd=$keyword")
            }
            else -> {
                requestBuilder.url(site.api)
            }
        }
        
        // 添加通用请求头
        requestBuilder
            .header("User-Agent", "OneTV/2.1.1")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        
        // 添加站点特定的请求头
        site.getHeaderMap().forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 解析APP响应
     */
    private fun parseAppResponse(responseBody: String): List<VodItem> {
        return try {
            // 尝试解析为标准TVBOX格式
            val response = json.decodeFromString<VodResponse>(responseBody)
            response.list ?: emptyList()
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析APP响应失败，尝试其他格式", e)
            
            // 尝试解析为其他格式
            parseAlternativeFormat(responseBody)
        }
    }
    
    /**
     * 解析APP分类响应
     */
    private fun parseAppCategories(responseBody: String): List<VodClass> {
        return try {
            // 尝试解析为标准TVBOX格式
            val response = json.decodeFromString<VodResponse>(responseBody)
            response.classes
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析APP分类响应失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析其他格式的响应
     */
    private fun parseAlternativeFormat(responseBody: String): List<VodItem> {
        return try {
            // TODO: 实现其他APP格式的解析
            Log.d("ONETV_MOVIE", "🔄 尝试解析其他APP格式")
            emptyList()
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析其他APP格式失败", e)
            emptyList()
        }
    }

    /**
     * 检查是否为真正的APP站点
     */
    private fun isTrueAppSite(site: VodSite): Boolean {
        val api = site.api.lowercase()

        // 如果是JavaScript文件，不是APP站点
        if (api.endsWith(".js") || api.contains("drpy") || api.contains("hipy")) {
            Log.d("ONETV_MOVIE", "❌ 检测到JavaScript文件，不是APP站点: ${site.api}")
            return false
        }

        // 如果API包含csp_前缀，通常是Spider站点
        if (api.startsWith("csp_") || site.name.contains("csp_")) {
            Log.d("ONETV_MOVIE", "❌ 检测到CSP前缀，不是APP站点: ${site.api}")
            return false
        }

        // 如果有JAR包，通常是Spider站点
        if (site.jar.isNotEmpty()) {
            Log.d("ONETV_MOVIE", "❌ 检测到JAR包，不是APP站点: ${site.jar}")
            return false
        }

        // 真正的APP站点应该是HTTP API且返回JSON
        val isValidApp = api.startsWith("http") && !api.endsWith(".js")

        if (isValidApp) {
            Log.d("ONETV_MOVIE", "✅ 确认为APP站点: ${site.api}")
        } else {
            Log.d("ONETV_MOVIE", "❌ 不是有效的APP站点: ${site.api}")
        }

        return isValidApp
    }

}
