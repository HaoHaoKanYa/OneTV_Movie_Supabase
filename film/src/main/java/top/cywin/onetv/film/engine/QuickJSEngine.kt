package top.cywin.onetv.film.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * QuickJS JavaScript 引擎
 * 
 * 基于 FongMi/TV 的 QuickJS 集成实现
 * 提供完整的 JavaScript 执行环境
 * 
 * 功能：
 * - JavaScript 代码执行
 * - 函数调用
 * - 全局对象注入
 * - 内存管理
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class QuickJSEngine {
    
    companion object {
        private const val TAG = "ONETV_FILM_QUICKJS_ENGINE"
        
        init {
            try {
                System.loadLibrary("film-native")
                Log.d(TAG, "✅ QuickJS 原生库加载成功")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ QuickJS 原生库加载失败", e)
            }
        }
    }
    
    // JavaScript 上下文
    private var jsContext: Long = 0
    
    // 是否已初始化
    private var isInitialized = false
    
    // 脚本缓存
    private val scriptCache = ConcurrentHashMap<String, String>()
    
    // JSON 序列化器
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    /**
     * 🔧 初始化 QuickJS 引擎
     */
    suspend fun initialize(context: Context? = null) = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "📌 QuickJS 引擎已初始化，跳过重复初始化")
            return@withContext
        }
        
        try {
            Log.d(TAG, "🔧 初始化 QuickJS 引擎...")
            
            // 创建 JavaScript 上下文
            jsContext = createJSContext()
            if (jsContext == 0L) {
                throw RuntimeException("Failed to create QuickJS context")
            }
            
            // 初始化全局对象
            initializeGlobalObjects()
            
            // 注入工具函数
            injectUtilityFunctions()
            
            isInitialized = true
            Log.d(TAG, "✅ QuickJS 引擎初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ QuickJS 引擎初始化失败", e)
            cleanup()
            throw e
        }
    }
    
    /**
     * 📜 执行 JavaScript 代码
     */
    suspend fun evaluateScript(script: String): String = withContext(Dispatchers.IO) {
        checkInitialized()
        
        try {
            Log.d(TAG, "📜 执行 JavaScript 代码 (${script.length} 字符)")
            
            val result = nativeEvaluateScript(jsContext, script)
            
            if (result.startsWith("ERROR:")) {
                throw RuntimeException("JavaScript execution failed: $result")
            }
            
            Log.d(TAG, "✅ JavaScript 代码执行成功")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 代码执行失败", e)
            throw e
        }
    }
    
    /**
     * 🔧 调用 JavaScript 函数
     */
    suspend fun callFunction(functionName: String, args: Array<Any>): String = withContext(Dispatchers.IO) {
        checkInitialized()
        
        try {
            Log.d(TAG, "🔧 调用 JavaScript 函数: $functionName")
            
            val argsJson = json.encodeToString(args)
            val result = nativeCallFunction(jsContext, functionName, argsJson)
            
            if (result.startsWith("ERROR:")) {
                throw RuntimeException("Function call failed: $result")
            }
            
            Log.d(TAG, "✅ JavaScript 函数调用成功: $functionName")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ JavaScript 函数调用失败: $functionName", e)
            throw e
        }
    }
    
    /**
     * 📦 加载并缓存脚本
     */
    suspend fun loadScript(scriptUrl: String, scriptContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📦 加载脚本: $scriptUrl")
            
            // 缓存脚本内容
            scriptCache[scriptUrl] = scriptContent
            
            // 执行脚本
            evaluateScript(scriptContent)
            
            Log.d(TAG, "✅ 脚本加载成功: $scriptUrl")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 脚本加载失败: $scriptUrl", e)
            false
        }
    }
    
    /**
     * 🔍 检查函数是否存在
     */
    suspend fun hasFunction(functionName: String): Boolean = withContext(Dispatchers.IO) {
        checkInitialized()
        
        try {
            val result = nativeHasFunction(jsContext, functionName)
            Log.d(TAG, "🔍 检查函数存在性: $functionName = $result")
            result
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 检查函数存在性失败: $functionName", e)
            false
        }
    }
    
    /**
     * 🌐 初始化全局对象
     */
    private suspend fun initializeGlobalObjects() {
        Log.d(TAG, "🌐 初始化全局对象...")
        
        val initScript = """
            // 全局变量
            var HOST = '';
            var MOBILE_UA = 'Mozilla/5.0 (Linux; Android 11; M2007J3SC Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045714 Mobile Safari/537.36';
            var PC_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36';
            var UA = MOBILE_UA;
            
            // 控制台对象
            var console = {
                log: function() {
                    var args = Array.prototype.slice.call(arguments);
                    nativeLog('LOG', args.join(' '));
                },
                warn: function() {
                    var args = Array.prototype.slice.call(arguments);
                    nativeLog('WARN', args.join(' '));
                },
                error: function() {
                    var args = Array.prototype.slice.call(arguments);
                    nativeLog('ERROR', args.join(' '));
                }
            };
            
            // JSON 对象增强
            if (!JSON.stringify) {
                JSON.stringify = function(obj) {
                    return nativeJsonStringify(obj);
                };
            }
            
            if (!JSON.parse) {
                JSON.parse = function(str) {
                    return nativeJsonParse(str);
                };
            }
        """.trimIndent()
        
        evaluateScript(initScript)
        Log.d(TAG, "✅ 全局对象初始化完成")
    }
    
    /**
     * 🛠️ 注入工具函数
     */
    private suspend fun injectUtilityFunctions() {
        Log.d(TAG, "🛠️ 注入工具函数...")
        
        val utilityScript = """
            // HTTP 请求函数
            var req = function(url, options) {
                options = options || {};
                return nativeHttpRequest(url, JSON.stringify(options));
            };
            
            // HTML 解析函数
            var pdfh = function(html, rule) {
                return nativeParseHtml(html, rule, 'single');
            };
            
            var pdfa = function(html, rule) {
                return nativeParseHtml(html, rule, 'array');
            };
            
            // URL 处理函数
            var urljoin = function(base, path) {
                return nativeUrlJoin(base, path);
            };
            
            // 字符串处理函数
            var base64Encode = function(str) {
                return nativeBase64Encode(str);
            };
            
            var base64Decode = function(str) {
                return nativeBase64Decode(str);
            };
            
            // 正则表达式增强
            var matchAll = function(str, regex) {
                return nativeMatchAll(str, regex);
            };
            
            // 时间函数
            var sleep = function(ms) {
                return nativeSleep(ms);
            };
            
            // 代理函数
            var getProxyUrl = function(local) {
                return nativeGetProxyUrl(local || false);
            };
        """.trimIndent()
        
        evaluateScript(utilityScript)
        Log.d(TAG, "✅ 工具函数注入完成")
    }
    
    /**
     * ✅ 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized || jsContext == 0L) {
            throw IllegalStateException("QuickJS engine not initialized")
        }
    }
    
    /**
     * 🧹 清理资源
     */
    fun cleanup() {
        Log.d(TAG, "🧹 清理 QuickJS 引擎资源...")
        
        try {
            if (jsContext != 0L) {
                destroyJSContext(jsContext)
                jsContext = 0
            }
            
            scriptCache.clear()
            isInitialized = false
            
            Log.d(TAG, "✅ QuickJS 引擎资源清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ QuickJS 引擎资源清理失败", e)
        }
    }
    
    /**
     * 📊 获取引擎状态
     */
    fun getEngineStatus(): Map<String, Any> {
        return mapOf(
            "initialized" to isInitialized,
            "context" to (jsContext != 0L),
            "cached_scripts" to scriptCache.size,
            "memory_usage" to getMemoryUsage()
        )
    }
    
    /**
     * 💾 获取内存使用情况
     */
    private fun getMemoryUsage(): Long {
        return if (jsContext != 0L) {
            try {
                nativeGetMemoryUsage(jsContext)
            } catch (e: Exception) {
                -1L
            }
        } else {
            0L
        }
    }
    
    // ========== Native 方法声明 ==========
    
    /**
     * 创建 JavaScript 上下文
     */
    private external fun createJSContext(): Long
    
    /**
     * 销毁 JavaScript 上下文
     */
    private external fun destroyJSContext(context: Long)
    
    /**
     * 执行 JavaScript 代码
     */
    private external fun nativeEvaluateScript(context: Long, script: String): String
    
    /**
     * 调用 JavaScript 函数
     */
    private external fun nativeCallFunction(context: Long, functionName: String, argsJson: String): String
    
    /**
     * 检查函数是否存在
     */
    private external fun nativeHasFunction(context: Long, functionName: String): Boolean
    
    /**
     * 获取内存使用情况
     */
    private external fun nativeGetMemoryUsage(context: Long): Long
}
