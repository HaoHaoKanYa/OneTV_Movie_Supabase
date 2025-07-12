package top.cywin.onetv.movie.data.spider

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodSite
import top.cywin.onetv.movie.data.models.VodResponse

/**
 * Spider处理器 - 统一处理TVBOX Spider站点
 * 支持JAR包和JavaScript两种类型的Spider
 */
class SpiderProcessor(private val context: Context) {
    
    private val jarManager = JarManager(context)
    private val jsEngine = JavaScriptEngine(context)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * 初始化Spider处理器
     */
    suspend fun initialize(): Boolean {
        return try {
            Log.d("ONETV_MOVIE", "🔄 初始化Spider处理器")
            
            // 初始化JavaScript引擎
            val jsInitResult = jsEngine.initialize()
            if (!jsInitResult) {
                Log.w("ONETV_MOVIE", "⚠️ JavaScript引擎初始化失败")
            }
            
            Log.d("ONETV_MOVIE", "✅ Spider处理器初始化完成")
            true
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider处理器初始化失败", e)
            false
        }
    }
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(site: VodSite): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 Spider获取首页内容: ${site.name}")
            
            when {
                // JavaScript类型Spider
                isJavaScriptSpider(site) -> {
                    getHomeContentFromJS(site)
                }
                
                // JAR包类型Spider
                isJarSpider(site) -> {
                    getHomeContentFromJar(site)
                }
                
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知Spider类型")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类列表
     */
    suspend fun getCategories(site: VodSite): List<VodClass> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 Spider获取分类列表: ${site.name}")
            
            when {
                // JavaScript类型Spider
                isJavaScriptSpider(site) -> {
                    getCategoriesFromJS(site)
                }
                
                // JAR包类型Spider
                isJarSpider(site) -> {
                    getCategoriesFromJar(site)
                }
                
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知Spider类型，返回空分类")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类内容
     */
    suspend fun getCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 Spider获取分类内容: ${site.name}, 分类: $typeId, 页码: $page")
            
            when {
                // JavaScript类型Spider
                isJavaScriptSpider(site) -> {
                    getCategoryContentFromJS(site, typeId, page)
                }
                
                // JAR包类型Spider
                isJarSpider(site) -> {
                    getCategoryContentFromJar(site, typeId, page)
                }
                
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知Spider类型")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索内容
     */
    suspend fun search(site: VodSite, keyword: String): List<VodItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("ONETV_MOVIE", "🔄 Spider搜索: ${site.name}, 关键词: $keyword")
            
            when {
                // JavaScript类型Spider
                isJavaScriptSpider(site) -> {
                    searchFromJS(site, keyword)
                }
                
                // JAR包类型Spider
                isJarSpider(site) -> {
                    searchFromJar(site, keyword)
                }
                
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ 未知Spider类型")
                    emptyList()
                }
            }
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ Spider搜索失败", e)
            emptyList()
        }
    }
    
    // ==================== JavaScript Spider 处理 ====================
    
    /**
     * 判断是否为JavaScript Spider (TVBOX标准识别)
     */
    private fun isJavaScriptSpider(site: VodSite): Boolean {
        val api = site.api.lowercase()
        val isJsFile = api.endsWith(".js") || api.endsWith(".min.js")
        val hasJsKeywords = api.contains("drpy") || api.contains("hipy") ||
                           api.contains("javascript") || api.contains("js")
        val isGithubJs = api.contains("github.com") && (api.contains(".js") || api.contains("drpy") || api.contains("hipy"))

        val result = isJsFile || hasJsKeywords || isGithubJs

        if (result) {
            Log.d("ONETV_MOVIE", "✅ 识别为JavaScript Spider: ${site.name} - ${site.api}")
        } else {
            Log.d("ONETV_MOVIE", "❌ 不是JavaScript Spider: ${site.name} - ${site.api}")
        }

        return result
    }
    
    /**
     * 从JavaScript获取首页内容
     */
    private suspend fun getHomeContentFromJS(site: VodSite): List<VodItem> {
        return try {
            // 加载JavaScript脚本
            val loadResult = jsEngine.loadScript(site.api)
            if (!loadResult) {
                Log.w("ONETV_MOVIE", "⚠️ JavaScript脚本加载失败")
                return emptyList()
            }
            
            // 调用获取首页内容的函数
            jsEngine.getHomeContent(site)
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JavaScript获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JavaScript获取分类列表
     */
    private suspend fun getCategoriesFromJS(site: VodSite): List<VodClass> {
        return try {
            // 加载JavaScript脚本
            val loadResult = jsEngine.loadScript(site.api)
            if (!loadResult) {
                Log.w("ONETV_MOVIE", "⚠️ JavaScript脚本加载失败，返回空分类")
                return emptyList()
            }
            
            // 调用获取分类的函数
            jsEngine.getCategories(site)
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JavaScript获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JavaScript获取分类内容
     */
    private suspend fun getCategoryContentFromJS(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            // 加载JavaScript脚本
            val loadResult = jsEngine.loadScript(site.api)
            if (!loadResult) {
                Log.w("ONETV_MOVIE", "⚠️ JavaScript脚本加载失败")
                return emptyList()
            }
            
            // 调用获取分类内容的函数
            jsEngine.getCategoryContent(site, typeId, page)
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JavaScript获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JavaScript搜索
     */
    private suspend fun searchFromJS(site: VodSite, keyword: String): List<VodItem> {
        return try {
            // 加载JavaScript脚本
            val loadResult = jsEngine.loadScript(site.api)
            if (!loadResult) {
                Log.w("ONETV_MOVIE", "⚠️ JavaScript脚本加载失败")
                return emptyList()
            }
            
            // 调用搜索函数
            jsEngine.search(site, keyword)
            
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JavaScript搜索失败", e)
            emptyList()
        }
    }
    
    // ==================== JAR Spider 处理 ====================
    
    /**
     * 判断是否为JAR Spider
     */
    private fun isJarSpider(site: VodSite): Boolean {
        return site.jar.isNotEmpty() && site.jar.endsWith(".jar")
    }
    
    /**
     * 从JAR获取首页内容
     */
    private suspend fun getHomeContentFromJar(site: VodSite): List<VodItem> {
        return try {
            val result = jarManager.invokeMethod(site, "Spider", "homeContent", true)
            parseJarResult(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JAR获取分类列表
     */
    private suspend fun getCategoriesFromJar(site: VodSite): List<VodClass> {
        return try {
            val result = jarManager.invokeMethod(site, "Spider", "homeContent", true)
            parseJarCategories(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JAR获取分类内容
     */
    private suspend fun getCategoryContentFromJar(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            val result = jarManager.invokeMethod(site, "Spider", "categoryContent", typeId, page.toString(), true, emptyMap<String, String>())
            parseJarResult(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 从JAR搜索
     */
    private suspend fun searchFromJar(site: VodSite, keyword: String): List<VodItem> {
        return try {
            val result = jarManager.invokeMethod(site, "Spider", "searchContent", keyword, true)
            parseJarResult(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ JAR搜索失败", e)
            emptyList()
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 解析JAR返回结果
     */
    private fun parseJarResult(result: Any?): List<VodItem> {
        return try {
            when (result) {
                is String -> {
                    val response = json.decodeFromString<VodResponse>(result)
                    response.list ?: emptyList()
                }
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ JAR返回结果类型不支持: ${result?.javaClass?.simpleName}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析JAR结果失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析JAR分类结果
     */
    private fun parseJarCategories(result: Any?): List<VodClass> {
        return try {
            when (result) {
                is String -> {
                    val response = json.decodeFromString<VodResponse>(result)
                    response.classes
                }
                else -> {
                    Log.w("ONETV_MOVIE", "⚠️ JAR返回结果类型不支持: ${result?.javaClass?.simpleName}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析JAR分类结果失败", e)
            emptyList()
        }
    }
    

    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            jarManager.clearCache()
            jsEngine.cleanup()
            Log.d("ONETV_MOVIE", "🧹 Spider处理器资源已清理")
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 清理Spider处理器资源失败", e)
        }
    }
}
