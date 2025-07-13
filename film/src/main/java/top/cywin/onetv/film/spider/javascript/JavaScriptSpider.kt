package top.cywin.onetv.film.spider.javascript

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.engine.QuickJSEngine
import top.cywin.onetv.film.network.OkHttpManager
import top.cywin.onetv.film.utils.UrlUtils

/**
 * JavaScript Spider 实现
 * 
 * 基于 FongMi/TV 的 JavaScript 解析器实现
 * 使用 QuickJS 引擎执行 JavaScript 脚本
 * 
 * 功能：
 * - JavaScript 脚本加载和执行
 * - CatVod 标准函数调用
 * - 脚本缓存管理
 * - 错误处理和恢复
 * - 工具函数注入
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
open class JavaScriptSpider : Spider() {
    
    companion object {
        private const val TAG = "ONETV_FILM_JAVASCRIPT_SPIDER"
    }
    
    // QuickJS 引擎
    protected val quickJSEngine = QuickJSEngine()
    
    // HTTP 管理器
    protected val httpManager = OkHttpManager()
    
    // 脚本是否已加载
    private var scriptLoaded = false
    
    override fun init(context: Context, extend: String) {
        super.init(context, extend)
        logDebug("🔧 JavaScript Spider 初始化")
    }
    
    override suspend fun homeContent(filter: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🏠 JavaScript 获取首页内容, filter=$filter")
            
            // 确保脚本已加载
            ensureScriptLoaded()
            
            // 调用 JavaScript 函数
            val result = if (quickJSEngine.hasFunction("homeContent")) {
                quickJSEngine.callFunction("homeContent", arrayOf(filter))
            } else {
                buildDefaultHomeContent()
            }
            
            logDebug("✅ JavaScript 首页内容解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ JavaScript 首页内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun categoryContent(
        tid: String,
        pg: String,
        filter: Boolean,
        extend: HashMap<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            logDebug("📋 JavaScript 获取分类内容: tid=$tid, pg=$pg, filter=$filter")
            
            // 确保脚本已加载
            ensureScriptLoaded()
            
            // 调用 JavaScript 函数
            val result = if (quickJSEngine.hasFunction("categoryContent")) {
                quickJSEngine.callFunction("categoryContent", arrayOf(tid, pg, filter, extend))
            } else {
                buildDefaultCategoryContent(tid, pg)
            }
            
            logDebug("✅ JavaScript 分类内容解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ JavaScript 分类内容解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun detailContent(ids: List<String>): String = withContext(Dispatchers.IO) {
        try {
            val vodId = ids.firstOrNull() ?: throw Exception("视频ID为空")
            logDebug("🎭 JavaScript 获取视频详情: vodId=$vodId")
            
            // 确保脚本已加载
            ensureScriptLoaded()
            
            // 调用 JavaScript 函数
            val result = if (quickJSEngine.hasFunction("detailContent")) {
                quickJSEngine.callFunction("detailContent", arrayOf(ids))
            } else {
                buildDefaultDetailContent(vodId)
            }
            
            logDebug("✅ JavaScript 视频详情解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ JavaScript 视频详情解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun searchContent(key: String, quick: Boolean): String = withContext(Dispatchers.IO) {
        try {
            logDebug("🔍 JavaScript 搜索内容: key=$key, quick=$quick")
            
            // 确保脚本已加载
            ensureScriptLoaded()
            
            // 调用 JavaScript 函数
            val result = if (quickJSEngine.hasFunction("searchContent")) {
                quickJSEngine.callFunction("searchContent", arrayOf(key, quick))
            } else {
                buildDefaultSearchContent(key)
            }
            
            logDebug("✅ JavaScript 搜索完成")
            result
            
        } catch (e: Exception) {
            logError("❌ JavaScript 搜索失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    override suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String = withContext(Dispatchers.IO) {
        try {
            logDebug("▶️ JavaScript 获取播放链接: flag=$flag, id=$id")
            
            // 确保脚本已加载
            ensureScriptLoaded()
            
            // 调用 JavaScript 函数
            val result = if (quickJSEngine.hasFunction("playerContent")) {
                quickJSEngine.callFunction("playerContent", arrayOf(flag, id, vipFlags))
            } else {
                buildDefaultPlayerContent(id)
            }
            
            logDebug("✅ JavaScript 播放链接解析成功")
            result
            
        } catch (e: Exception) {
            logError("❌ JavaScript 播放链接解析失败", e)
            buildErrorResponse(e.message)
        }
    }
    
    // ========== JavaScript 脚本管理 ==========
    
    /**
     * 🔧 确保脚本已加载
     */
    protected open suspend fun ensureScriptLoaded() {
        if (scriptLoaded) {
            logDebug("📌 JavaScript 脚本已加载，跳过重复加载")
            return
        }
        
        try {
            logDebug("📦 加载 JavaScript 脚本...")
            
            // 初始化 QuickJS 引擎
            quickJSEngine.initialize()
            
            // 设置站点信息
            setupSiteInfo()
            
            // 注入工具函数
            injectUtilityFunctions()
            
            // 加载主脚本
            loadMainScript()
            
            scriptLoaded = true
            logDebug("✅ JavaScript 脚本加载完成")
            
        } catch (e: Exception) {
            logError("❌ JavaScript 脚本加载失败", e)
            throw e
        }
    }
    
    /**
     * 🔧 设置站点信息
     */
    protected open suspend fun setupSiteInfo() {
        val siteInfoScript = """
            var HOST = '${extractHost(siteUrl)}';
            var siteKey = '${siteKey}';
            var siteName = '${siteName}';
            var siteApi = '${siteUrl}';
            var MOBILE_UA = 'Mozilla/5.0 (Linux; Android 11; M2007J3SC Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/77.0.3865.120 MQQBrowser/6.2 TBS/045714 Mobile Safari/537.36';
            var PC_UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36';
            var UA = MOBILE_UA;
        """.trimIndent()
        
        quickJSEngine.evaluateScript(siteInfoScript)
    }
    
    /**
     * 🛠️ 注入工具函数
     */
    protected open suspend fun injectUtilityFunctions() {
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
            
            // 日志函数
            var log = function(msg) {
                console.log('[JS] ' + msg);
            };
        """.trimIndent()
        
        quickJSEngine.evaluateScript(utilityScript)
    }
    
    /**
     * 📜 加载主脚本
     */
    protected open suspend fun loadMainScript() {
        val scriptContent = if (siteUrl.endsWith(".js")) {
            // 下载远程脚本
            downloadScript(siteUrl)
        } else {
            // 生成默认脚本
            generateDefaultScript()
        }
        
        quickJSEngine.evaluateScript(scriptContent)
    }
    
    /**
     * 🌐 下载脚本
     */
    protected open suspend fun downloadScript(scriptUrl: String): String {
        return try {
            logDebug("🌐 下载脚本: $scriptUrl")
            val script = httpManager.getString(scriptUrl, siteHeaders)
            logDebug("✅ 脚本下载成功，长度: ${script.length}")
            script
        } catch (e: Exception) {
            logWarning("⚠️ 脚本下载失败，使用默认脚本", e)
            generateDefaultScript()
        }
    }
    
    /**
     * 🏭 生成默认脚本
     */
    protected open fun generateDefaultScript(): String {
        return """
            function homeContent(filter) {
                log('homeContent called with filter: ' + filter);
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
                log('categoryContent called: tid=' + tid + ', pg=' + pg);
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": "js_" + tid + "_" + pg,
                            "vod_name": "JavaScript 测试视频 " + pg,
                            "vod_pic": HOST + "/pic/" + tid + "_" + pg + ".jpg",
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
                log('detailContent called with vodId: ' + vodId);
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": vodId,
                            "vod_name": "JavaScript 测试视频详情",
                            "vod_pic": HOST + "/pic/" + vodId + ".jpg",
                            "vod_content": "这是一个通过 JavaScript 解析的测试视频详细介绍...",
                            "vod_year": "2023",
                            "vod_area": "中国",
                            "vod_actor": "JavaScript 演员",
                            "vod_director": "JavaScript 导演",
                            "vod_play_from": "JavaScript播放源1$$$JavaScript播放源2",
                            "vod_play_url": "第1集$" + HOST + "/play/" + vodId + "/1.mp4#第2集$" + HOST + "/play/" + vodId + "/2.mp4$$$第1集$" + HOST + "/play2/" + vodId + "/1.mp4#第2集$" + HOST + "/play2/" + vodId + "/2.mp4"
                        }
                    ]
                });
            }
            
            function searchContent(key, quick) {
                log('searchContent called: key=' + key + ', quick=' + quick);
                return JSON.stringify({
                    "list": [
                        {
                            "vod_id": "search_js_123",
                            "vod_name": "JavaScript 搜索结果: " + key,
                            "vod_pic": HOST + "/pic/search.jpg",
                            "vod_remarks": "搜索匹配"
                        }
                    ]
                });
            }
            
            function playerContent(flag, id, vipFlags) {
                log('playerContent called: flag=' + flag + ', id=' + id);
                return JSON.stringify({
                    "parse": 0,
                    "playUrl": HOST + "/video/" + id + ".mp4",
                    "url": HOST + "/video/" + id + ".mp4",
                    "header": {
                        "User-Agent": UA,
                        "Referer": HOST
                    }
                });
            }
        """.trimIndent()
    }
    
    // ========== 默认内容构建 ==========
    
    /**
     * 🏠 构建默认首页内容
     */
    protected open fun buildDefaultHomeContent(): String {
        return buildJsonResponse {
            put("class", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("type_id", "1")
                    put("type_name", "电影")
                })
                add(kotlinx.serialization.json.buildJsonObject {
                    put("type_id", "2")
                    put("type_name", "电视剧")
                })
            })
        }
    }
    
    /**
     * 📋 构建默认分类内容
     */
    protected open fun buildDefaultCategoryContent(tid: String, pg: String): String {
        return buildJsonResponse {
            put("list", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("vod_id", "default_${tid}_${pg}")
                    put("vod_name", "默认视频 $pg")
                    put("vod_pic", "")
                    put("vod_remarks", "第${pg}页")
                })
            })
            put("page", pg.toIntOrNull() ?: 1)
            put("pagecount", 10)
        }
    }
    
    /**
     * 🎭 构建默认详情内容
     */
    protected open fun buildDefaultDetailContent(vodId: String): String {
        return buildJsonResponse {
            put("list", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("vod_id", vodId)
                    put("vod_name", "默认视频详情")
                    put("vod_content", "默认视频内容")
                    put("vod_play_from", "默认播放源")
                    put("vod_play_url", "第1集\$http://example.com/play1")
                })
            })
        }
    }
    
    /**
     * 🔍 构建默认搜索内容
     */
    protected open fun buildDefaultSearchContent(key: String): String {
        return buildJsonResponse {
            put("list", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.buildJsonObject {
                    put("vod_id", "search_default")
                    put("vod_name", "搜索结果: $key")
                    put("vod_pic", "")
                    put("vod_remarks", "搜索")
                })
            })
        }
    }
    
    /**
     * ▶️ 构建默认播放内容
     */
    protected open fun buildDefaultPlayerContent(id: String): String {
        return buildJsonResponse {
            put("parse", 0)
            put("playUrl", "http://example.com/video/$id.mp4")
            put("url", "http://example.com/video/$id.mp4")
        }
    }
    
    /**
     * 🌐 提取主机名
     */
    protected open fun extractHost(url: String): String {
        return UrlUtils.extractDomain(url)
    }
    
    override fun destroy() {
        super.destroy()
        try {
            quickJSEngine.cleanup()
            scriptLoaded = false
            logDebug("✅ JavaScript Spider 清理完成")
        } catch (e: Exception) {
            logWarning("⚠️ JavaScript Spider 清理失败", e)
        }
    }
}
