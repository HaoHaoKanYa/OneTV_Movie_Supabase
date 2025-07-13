package top.cywin.onetv.film.spider.base

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import top.cywin.onetv.film.network.EnhancedOkHttpManager
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * Spider 基础类
 * 基于 FongMi/TV 标准实现
 * 
 * 这是所有解析器的基础类，定义了标准的解析接口和通用功能
 * 
 * 功能：
 * - 标准解析接口定义
 * - 通用网络请求处理
 * - JSON 数据解析
 * - 错误处理和日志
 * - 缓存管理
 * - 配置管理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
abstract class Spider {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPIDER_BASE"
        
        // 默认请求头
        val DEFAULT_HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language" to "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3",
            "Accept-Encoding" to "gzip, deflate",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1"
        )
    }
    
    // HTTP 管理器
    protected val httpManager = EnhancedOkHttpManager()
    
    // JSON 解析器
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    // 站点配置
    protected var siteKey: String = ""
    protected var api: String = ""
    protected var ext: String = ""
    protected var headers: Map<String, String> = emptyMap()
    
    // 缓存
    private val responseCache = ConcurrentHashMap<String, String>()
    
    /**
     * 🔧 初始化 Spider
     */
    open fun init(context: Context, extend: String) {
        this.ext = extend
        Log.d(TAG, "🔧 Spider 初始化: ${this::class.simpleName}")
    }
    
    /**
     * 🏠 获取首页内容
     * 
     * @param filter 是否启用筛选
     * @return JSON 格式的首页数据
     */
    abstract suspend fun homeContent(filter: Boolean): String
    
    /**
     * 📂 获取分类内容
     * 
     * @param tid 分类ID
     * @param pg 页码
     * @param filter 是否启用筛选
     * @param extend 扩展参数
     * @return JSON 格式的分类数据
     */
    abstract suspend fun categoryContent(
        tid: String, 
        pg: String, 
        filter: Boolean, 
        extend: HashMap<String, String>
    ): String
    
    /**
     * 📄 获取详情内容
     * 
     * @param ids 视频ID列表
     * @return JSON 格式的详情数据
     */
    abstract suspend fun detailContent(ids: List<String>): String
    
    /**
     * 🔍 搜索内容
     * 
     * @param key 搜索关键词
     * @param quick 是否快速搜索
     * @return JSON 格式的搜索结果
     */
    abstract suspend fun searchContent(key: String, quick: Boolean): String
    
    /**
     * ▶️ 获取播放内容
     * 
     * @param flag 播放源标识
     * @param id 播放ID
     * @param vipFlags VIP标识列表
     * @return JSON 格式的播放数据
     */
    abstract suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String
    
    /**
     * 🌐 发送 GET 请求
     */
    protected suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        try {
            val allHeaders = DEFAULT_HEADERS + this@Spider.headers + headers
            httpManager.getString(url, allHeaders)
        } catch (e: Exception) {
            Log.e(TAG, "❌ GET 请求失败: $url", e)
            throw e
        }
    }
    
    /**
     * 📤 发送 POST 请求
     */
    protected suspend fun post(
        url: String, 
        body: String, 
        headers: Map<String, String> = emptyMap(),
        mediaType: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        try {
            val allHeaders = DEFAULT_HEADERS + this@Spider.headers + headers
            httpManager.postString(url, body, allHeaders, mediaType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ POST 请求失败: $url", e)
            throw e
        }
    }
    
    /**
     * 💾 带缓存的 GET 请求
     */
    protected suspend fun getWithCache(
        url: String, 
        headers: Map<String, String> = emptyMap(),
        useCache: Boolean = true,
        cacheTime: Long = 300000L // 5分钟
    ): String = withContext(Dispatchers.IO) {
        val cacheKey = generateCacheKey(url, headers)
        
        if (useCache && responseCache.containsKey(cacheKey)) {
            Log.d(TAG, "📦 从缓存获取: $url")
            return@withContext responseCache[cacheKey]!!
        }
        
        val response = get(url, headers)
        
        if (useCache) {
            responseCache[cacheKey] = response
            Log.d(TAG, "💾 缓存响应: $url")
        }
        
        response
    }
    
    /**
     * 🔧 生成缓存键
     */
    private fun generateCacheKey(url: String, headers: Map<String, String>): String {
        return StringUtils.md5("$url${headers.hashCode()}")
    }
    
    /**
     * 📋 创建标准响应
     */
    protected fun createStandardResponse(builder: JsonObjectBuilder.() -> Unit): String {
        return json.encodeToString(buildJsonObject(builder))
    }
    
    /**
     * 📋 创建空响应
     */
    protected fun createEmptyResult(): String {
        return createStandardResponse {
            put("code", 1)
            put("msg", "success")
            put("page", 1)
            put("pagecount", 0)
            put("limit", 20)
            put("total", 0)
            put("list", JsonArray(emptyList()))
        }
    }
    
    /**
     * ❌ 创建错误响应
     */
    protected fun createErrorResult(message: String): String {
        return createStandardResponse {
            put("code", 0)
            put("msg", message)
            put("page", 1)
            put("pagecount", 0)
            put("limit", 20)
            put("total", 0)
            put("list", JsonArray(emptyList()))
        }
    }
    
    /**
     * 📝 记录调试日志
     */
    protected fun logDebug(message: String) {
        Log.d(TAG, "🐛 [${this::class.simpleName}] $message")
    }
    
    /**
     * ⚠️ 记录警告日志
     */
    protected fun logWarning(message: String, throwable: Throwable? = null) {
        Log.w(TAG, "⚠️ [${this::class.simpleName}] $message", throwable)
    }
    
    /**
     * ❌ 记录错误日志
     */
    protected fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "❌ [${this::class.simpleName}] $message", throwable)
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        responseCache.clear()
        Log.d(TAG, "🧹 缓存已清理")
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cache_size" to responseCache.size,
            "cache_keys" to responseCache.keys.toList()
        )
    }
}
