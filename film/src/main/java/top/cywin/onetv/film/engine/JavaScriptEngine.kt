package top.cywin.onetv.film.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.data.models.VodSite
import java.util.concurrent.ConcurrentHashMap

/**
 * JavaScript 引擎实现
 * 
 * 基于 FongMi/TV 的 JavaScript 解析引擎
 * 使用 QuickJS 执行 JavaScript 代码
 * 
 * 功能：
 * - JavaScript 脚本执行
 * - CatVod 函数调用
 * - 脚本缓存管理
 * - 错误处理和恢复
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class JavaScriptEngine : Engine {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAVASCRIPT_ENGINE"
    }
    
    // QuickJS 引擎实例
    private val quickJSEngine = QuickJSEngine()
    
    // 脚本缓存
    private val scriptCache = ConcurrentHashMap<String, String>()
    
    // 站点初始化状态
    private val siteInitialized = ConcurrentHashMap<String, Boolean>()
    
    // 是否已初始化
    private var isInitialized = false
    
    override suspend fun initialize(context: Context?) {
        if (isInitialized) {
            Log.d(TAG, "📌 JavaScript 引擎已初始化，跳过重复初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 初始化 JavaScript 引擎...")
            
            // 初始化 QuickJS
            quickJSEngine.initialize(context)
            
            isInitialized = true
            Log.d(TAG, "✅ JavaScript 引擎初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 引擎初始化失败", e)
            throw e
        }
    }
    
    override suspend fun execute(
        site: VodSite,
        operation: String,
        params: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            checkInitialized()
            
            Log.d(TAG, "🚀 执行 JavaScript 操作: $operation")
            
            // 确保站点脚本已加载
            ensureSiteScriptLoaded(site)
            
            // 执行对应的操作
            val result = when (operation) {
                "homeContent" -> executeHomeContent(params)
                "categoryContent" -> executeCategoryContent(params)
                "detailContent" -> executeDetailContent(params)
                "searchContent" -> executeSearchContent(params)
                "playerContent" -> executePlayerContent(params)
                else -> throw IllegalArgumentException("Unsupported operation: $operation")
            }
            
            Log.d(TAG, "✅ JavaScript 操作执行成功: $operation")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 操作执行失败: $operation", e)
            Result.failure(e)
        }
    }
    
    /**
     * 🔧 确保站点脚本已加载
     */
    private suspend fun ensureSiteScriptLoaded(site: VodSite) {
        val siteKey = site.key
        
        if (siteInitialized[siteKey] == true) {
            Log.d(TAG, "📌 站点脚本已加载: $siteKey")
            return
        }
        
        try {
            Log.d(TAG, "📦 加载站点脚本: $siteKey")
            
            // 获取脚本内容
            val scriptContent = getScriptContent(site)
            
            // 设置站点信息
            val siteInfoScript = """
                var HOST = '${extractHost(site.api)}';
                var siteKey = '${site.key}';
                var siteName = '${site.name}';
                var siteApi = '${site.api}';
                var siteType = ${site.type};
                var siteExt = ${site.ext};
            """.trimIndent()
            
            // 执行站点信息脚本
            quickJSEngine.evaluateScript(siteInfoScript)
            
            // 执行主脚本
            quickJSEngine.evaluateScript(scriptContent)
            
            // 标记为已初始化
            siteInitialized[siteKey] = true
            
            Log.d(TAG, "✅ 站点脚本加载成功: $siteKey")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 站点脚本加载失败: $siteKey", e)
            throw e
        }
    }
    
    /**
     * 📜 获取脚本内容
     */
    private suspend fun getScriptContent(site: VodSite): String {
        val scriptUrl = site.api
        
        // 从缓存获取
        scriptCache[scriptUrl]?.let { cachedScript ->
            Log.d(TAG, "📦 从缓存获取脚本: $scriptUrl")
            return cachedScript
        }
        
        // 下载脚本（这里简化处理，实际需要网络请求）
        val scriptContent = if (scriptUrl.startsWith("http")) {
            // TODO: 实际下载脚本
            Log.d(TAG, "🌐 下载脚本: $scriptUrl")
            generateMockScript(site)
        } else {
            // 本地脚本或内置脚本
            Log.d(TAG, "📁 加载本地脚本: $scriptUrl")
            generateMockScript(site)
        }
        
        // 缓存脚本
        scriptCache[scriptUrl] = scriptContent
        
        return scriptContent
    }
    
    /**
     * 🏠 执行 homeContent
     */
    private suspend fun executeHomeContent(params: Map<String, Any>): String {
        val filter = params["filter"] as? Boolean ?: false
        
        return if (quickJSEngine.hasFunction("homeContent")) {
            quickJSEngine.callFunction("homeContent", arrayOf(filter))
        } else {
            // 默认实现
            """{"class":[{"type_id":"1","type_name":"电影"},{"type_id":"2","type_name":"电视剧"}]}"""
        }
    }
    
    /**
     * 📋 执行 categoryContent
     */
    private suspend fun executeCategoryContent(params: Map<String, Any>): String {
        val tid = params["tid"] as? String ?: ""
        val pg = params["pg"] as? String ?: "1"
        val filter = params["filter"] as? Boolean ?: false
        val extend = params["extend"] as? Map<String, String> ?: emptyMap()
        
        return if (quickJSEngine.hasFunction("categoryContent")) {
            quickJSEngine.callFunction("categoryContent", arrayOf(tid, pg, filter, extend))
        } else {
            // 默认实现
            """{"list":[{"vod_id":"123","vod_name":"测试视频","vod_pic":"","vod_remarks":""}],"page":$pg,"pagecount":1}"""
        }
    }
    
    /**
     * 🎭 执行 detailContent
     */
    private suspend fun executeDetailContent(params: Map<String, Any>): String {
        val ids = params["ids"] as? List<String> ?: emptyList()
        
        return if (quickJSEngine.hasFunction("detailContent")) {
            quickJSEngine.callFunction("detailContent", arrayOf(ids))
        } else {
            // 默认实现
            val vodId = ids.firstOrNull() ?: "123"
            """{"list":[{"vod_id":"$vodId","vod_name":"测试视频","vod_content":"测试内容","vod_play_from":"播放源","vod_play_url":"第1集${'$'}http://example.com/play1"}]}"""
        }
    }
    
    /**
     * 🔍 执行 searchContent
     */
    private suspend fun executeSearchContent(params: Map<String, Any>): String {
        val key = params["key"] as? String ?: ""
        val quick = params["quick"] as? Boolean ?: false
        
        return if (quickJSEngine.hasFunction("searchContent")) {
            quickJSEngine.callFunction("searchContent", arrayOf(key, quick))
        } else {
            // 默认实现
            """{"list":[{"vod_id":"123","vod_name":"搜索结果: $key","vod_pic":"","vod_remarks":""}]}"""
        }
    }
    
    /**
     * ▶️ 执行 playerContent
     */
    private suspend fun executePlayerContent(params: Map<String, Any>): String {
        val flag = params["flag"] as? String ?: ""
        val id = params["id"] as? String ?: ""
        val vipFlags = params["vipFlags"] as? List<String> ?: emptyList()
        
        return if (quickJSEngine.hasFunction("playerContent")) {
            quickJSEngine.callFunction("playerContent", arrayOf(flag, id, vipFlags))
        } else {
            // 默认实现
            """{"parse":0,"playUrl":"http://example.com/video.mp4","url":"http://example.com/video.mp4"}"""
        }
    }
    
    /**
     * 🏭 生成模拟脚本（用于测试）
     */
    private fun generateMockScript(site: VodSite): String {
        return """
            function homeContent(filter) {
                return JSON.stringify({
                    "class": [
                        {"type_id": "1", "type_name": "电影"},
                        {"type_id": "2", "type_name": "电视剧"},
                        {"type_id": "3", "type_name": "综艺"},
                        {"type_id": "4", "type_name": "动漫"}
                    ]
                });
            }
            
            function categoryContent(tid, pg, filter, extend) {
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": "test_" + tid + "_" + pg,
                            "vod_name": "测试视频 " + pg,
                            "vod_pic": "https://example.com/pic.jpg",
                            "vod_remarks": "更新至第" + pg + "集"
                        }
                    ],
                    "page": parseInt(pg),
                    "pagecount": 10,
                    "limit": 20,
                    "total": 200
                });
            }
            
            function detailContent(ids) {
                var vodId = ids[0] || "123";
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": vodId,
                            "vod_name": "测试视频详情",
                            "vod_pic": "https://example.com/pic.jpg",
                            "vod_content": "这是一个测试视频的详细介绍...",
                            "vod_year": "2023",
                            "vod_area": "中国",
                            "vod_actor": "测试演员",
                            "vod_director": "测试导演",
                            "vod_play_from": "播放源1$$$播放源2",
                            "vod_play_url": "第1集${'$'}http://example.com/play1#第2集${'$'}http://example.com/play2$$$第1集${'$'}http://example.com/play3#第2集${'$'}http://example.com/play4"
                        }
                    ]
                });
            }
            
            function searchContent(key, quick) {
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": "search_123",
                            "vod_name": "搜索结果: " + key,
                            "vod_pic": "https://example.com/pic.jpg",
                            "vod_remarks": "搜索匹配"
                        }
                    ]
                });
            }
            
            function playerContent(flag, id, vipFlags) {
                return JSON.stringify({
                    "parse": 0,
                    "playUrl": "http://example.com/video/" + id + ".mp4",
                    "url": "http://example.com/video/" + id + ".mp4",
                    "header": {
                        "User-Agent": UA
                    }
                });
            }
        """.trimIndent()
    }
    
    /**
     * 🌐 提取主机名
     */
    private fun extractHost(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}"
        } catch (e: Exception) {
            url.substringBefore("/", url)
        }
    }
    
    /**
     * ✅ 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("JavaScript engine not initialized")
        }
    }
    
    override fun cleanup() {
        Log.d(TAG, "🧹 清理 JavaScript 引擎...")
        
        try {
            quickJSEngine.cleanup()
            scriptCache.clear()
            siteInitialized.clear()
            isInitialized = false
            
            Log.d(TAG, "✅ JavaScript 引擎清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 引擎清理失败", e)
        }
    }
    
    /**
     * 📊 获取引擎状态
     */
    fun getEngineStatus(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "quickjs_status" to quickJSEngine.getEngineStatus(),
            "cached_scripts" to scriptCache.size,
            "initialized_sites" to siteInitialized.size
        )
    }
}
