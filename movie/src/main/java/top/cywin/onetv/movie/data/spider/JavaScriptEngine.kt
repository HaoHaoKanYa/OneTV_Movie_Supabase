package top.cywin.onetv.movie.data.spider

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodSite
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * JavaScript引擎 - 用于执行TVBOX Spider站点的JavaScript代码
 * 支持drpy、hipy等JavaScript框架
 */
class JavaScriptEngine(private val context: Context) {
    
    private var webView: WebView? = null
    private var isInitialized = false
    
    /**
     * 初始化JavaScript引擎
     */
    suspend fun initialize(): Boolean = withTimeout(10000) {
        return@withTimeout withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    Log.d("ONETV_MOVIE", "🔄 在主线程初始化WebView...")
                    webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        allowUniversalAccessFromFileURLs = true
                        allowFileAccessFromFileURLs = true
                    }
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (!isInitialized) {
                                isInitialized = true
                                Log.d("ONETV_MOVIE", "✅ JavaScript引擎初始化完成")
                                continuation.resume(true)
                            }
                        }
                        
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.e("ONETV_MOVIE", "❌ WebView错误: $description")
                            if (!isInitialized) {
                                continuation.resumeWithException(Exception("WebView初始化失败: $description"))
                            }
                        }
                    }
                    
                    // 添加JavaScript接口
                    addJavascriptInterface(JavaScriptInterface(), "Android")
                    
                    // 加载空白页面以初始化WebView
                    loadUrl("about:blank")
                }
                
                continuation.invokeOnCancellation {
                    cleanup()
                }
                
                } catch (e: Exception) {
                    Log.e("ONETV_MOVIE", "❌ JavaScript引擎初始化异常", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    /**
     * 加载并执行JavaScript代码
     */
    suspend fun loadScript(scriptUrl: String): Boolean = withTimeout(15000) {
        return@withTimeout withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    if (!isInitialized || webView == null) {
                        continuation.resumeWithException(Exception("JavaScript引擎未初始化"))
                        return@suspendCancellableCoroutine
                    }

                    Log.d("ONETV_MOVIE", "🔄 在主线程加载JavaScript脚本: $scriptUrl")

                    // 创建加载脚本的JavaScript代码
                    val loadScriptJs = """
                        (function() {
                            var script = document.createElement('script');
                            script.src = '$scriptUrl';
                            script.onload = function() {
                                Android.onScriptLoaded(true, 'Script loaded successfully');
                            };
                            script.onerror = function() {
                                Android.onScriptLoaded(false, 'Script load failed');
                            };
                            document.head.appendChild(script);
                        })();
                    """.trimIndent()

                    // 设置回调
                    scriptLoadCallback = { success, message ->
                        if (success) {
                            Log.d("ONETV_MOVIE", "✅ JavaScript脚本加载成功")
                            continuation.resume(true)
                        } else {
                            Log.e("ONETV_MOVIE", "❌ JavaScript脚本加载失败: $message")
                            continuation.resumeWithException(Exception("脚本加载失败: $message"))
                        }
                    }

                    // 在主线程执行加载脚本
                    webView?.evaluateJavascript(loadScriptJs, null)

                } catch (e: Exception) {
                    Log.e("ONETV_MOVIE", "❌ 加载JavaScript脚本异常", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    /**
     * 执行JavaScript函数并获取结果
     */
    suspend fun executeFunction(functionName: String, vararg params: String): String = withTimeout(10000) {
        return@withTimeout withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    if (!isInitialized || webView == null) {
                        continuation.resumeWithException(Exception("JavaScript引擎未初始化"))
                        return@suspendCancellableCoroutine
                    }

                    // 构建参数字符串
                    val paramString = params.joinToString(", ") { "'$it'" }
                    val jsCode = "$functionName($paramString)"

                    Log.d("ONETV_MOVIE", "🔄 在主线程执行JavaScript函数: $jsCode")

                    webView?.evaluateJavascript(jsCode) { result ->
                        try {
                            val cleanResult = result?.removeSurrounding("\"") ?: ""
                            Log.d("ONETV_MOVIE", "✅ JavaScript函数执行完成，结果长度: ${cleanResult.length}")
                            continuation.resume(cleanResult)
                        } catch (e: Exception) {
                            Log.e("ONETV_MOVIE", "❌ JavaScript函数结果处理异常", e)
                            continuation.resumeWithException(e)
                        }
                    }

                } catch (e: Exception) {
                    Log.e("ONETV_MOVIE", "❌ 执行JavaScript函数异常", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    /**
     * 获取首页内容
     */
    suspend fun getHomeContent(site: VodSite): List<VodItem> {
        return try {
            val result = executeFunction("getHomeContent")
            parseVodItems(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取首页内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类列表
     */
    suspend fun getCategories(site: VodSite): List<VodClass> {
        return try {
            val result = executeFunction("getCategories")
            parseVodClasses(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取分类列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取分类内容
     */
    suspend fun getCategoryContent(site: VodSite, typeId: String, page: Int): List<VodItem> {
        return try {
            val result = executeFunction("getCategoryContent", typeId, page.toString())
            parseVodItems(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 获取分类内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 搜索内容
     */
    suspend fun search(site: VodSite, keyword: String): List<VodItem> {
        return try {
            val result = executeFunction("search", keyword)
            parseVodItems(result)
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 搜索内容失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析VodItem列表
     */
    private fun parseVodItems(jsonString: String): List<VodItem> {
        return try {
            // TODO: 实现JSON解析逻辑
            Log.d("ONETV_MOVIE", "🔄 解析VodItem列表，JSON长度: ${jsonString.length}")
            emptyList()
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析VodItem列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析VodClass列表
     */
    private fun parseVodClasses(jsonString: String): List<VodClass> {
        return try {
            // TODO: 实现JSON解析逻辑
            Log.d("ONETV_MOVIE", "🔄 解析VodClass列表，JSON长度: ${jsonString.length}")
            emptyList()
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 解析VodClass列表失败", e)
            emptyList()
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            webView?.destroy()
            webView = null
            isInitialized = false
            Log.d("ONETV_MOVIE", "🧹 JavaScript引擎资源已清理")
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "❌ 清理JavaScript引擎资源异常", e)
        }
    }
    
    // 脚本加载回调
    private var scriptLoadCallback: ((Boolean, String) -> Unit)? = null
    
    /**
     * JavaScript接口类
     */
    inner class JavaScriptInterface {
        @JavascriptInterface
        fun onScriptLoaded(success: Boolean, message: String) {
            scriptLoadCallback?.invoke(success, message)
        }
        
        @JavascriptInterface
        fun log(message: String) {
            Log.d("ONETV_MOVIE", "📱 JS: $message")
        }
    }
}
