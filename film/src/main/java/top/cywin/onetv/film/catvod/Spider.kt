package top.cywin.onetv.film.catvod

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * CatVod Spider 基础接口
 * 
 * 完全按照 FongMi/TV 的 CatVod 接口实现
 * 这是所有解析器的基础接口，定义了标准的 TVBOX 解析方法
 * 
 * 核心方法：
 * - homeContent: 获取首页分类
 * - categoryContent: 获取分类内容列表
 * - detailContent: 获取视频详情
 * - searchContent: 搜索内容
 * - playerContent: 获取播放链接
 * 
 * @author OneTV Team
 * @since 2025-07-12
 * @version 1.0 (基于 FongMi/TV CatVod)
 */
abstract class Spider {
    
    companion object {
        private const val TAG = "ONETV_FILM_SPIDER"
        
        // JSON 序列化器
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
    
    // Spider 基础信息
    protected var siteKey: String = ""
    protected var siteName: String = ""
    protected var siteUrl: String = ""
    protected var siteHeaders: Map<String, String> = emptyMap()
    protected var context: Context? = null
    protected var extend: String = ""
    
    /**
     * 🏠 获取首页内容
     * 
     * @param filter 是否启用筛选
     * @return JSON 格式的分类数据
     * 
     * 返回格式：
     * {
     *   "class": [
     *     {"type_id": "1", "type_name": "电影"},
     *     {"type_id": "2", "type_name": "电视剧"}
     *   ]
     * }
     */
    abstract suspend fun homeContent(filter: Boolean): String
    
    /**
     * 📋 获取分类内容列表
     * 
     * @param tid 分类ID
     * @param pg 页码
     * @param filter 是否启用筛选
     * @param extend 扩展参数
     * @return JSON 格式的内容列表
     * 
     * 返回格式：
     * {
     *   "list": [
     *     {
     *       "vod_id": "123",
     *       "vod_name": "电影名称",
     *       "vod_pic": "封面图片",
     *       "vod_remarks": "备注信息"
     *     }
     *   ],
     *   "page": 1,
     *   "pagecount": 10,
     *   "limit": 20,
     *   "total": 200
     * }
     */
    abstract suspend fun categoryContent(
        tid: String, 
        pg: String, 
        filter: Boolean, 
        extend: HashMap<String, String>
    ): String
    
    /**
     * 🎭 获取视频详情
     * 
     * @param ids 视频ID列表
     * @return JSON 格式的详情数据
     * 
     * 返回格式：
     * {
     *   "list": [
     *     {
     *       "vod_id": "123",
     *       "vod_name": "电影名称",
     *       "vod_pic": "封面图片",
     *       "vod_content": "剧情介绍",
     *       "vod_year": "2023",
     *       "vod_area": "中国",
     *       "vod_actor": "演员列表",
     *       "vod_director": "导演",
     *       "vod_play_from": "播放源1$$$播放源2",
     *       "vod_play_url": "第1集$url1#第2集$url2$$$第1集$url3#第2集$url4"
     *     }
     *   ]
     * }
     */
    abstract suspend fun detailContent(ids: List<String>): String
    
    /**
     * 🔍 搜索内容
     * 
     * @param key 搜索关键词
     * @param quick 是否快速搜索
     * @return JSON 格式的搜索结果
     * 
     * 返回格式：
     * {
     *   "list": [
     *     {
     *       "vod_id": "123",
     *       "vod_name": "电影名称",
     *       "vod_pic": "封面图片",
     *       "vod_remarks": "备注信息"
     *     }
     *   ]
     * }
     */
    abstract suspend fun searchContent(key: String, quick: Boolean): String
    
    /**
     * ▶️ 获取播放链接
     * 
     * @param flag 播放源标识
     * @param id 播放链接ID
     * @param vipFlags VIP标识列表
     * @return JSON 格式的播放信息
     * 
     * 返回格式：
     * {
     *   "parse": 0,
     *   "playUrl": "实际播放链接",
     *   "url": "实际播放链接",
     *   "header": {"User-Agent": "..."}
     * }
     */
    abstract suspend fun playerContent(flag: String, id: String, vipFlags: List<String>): String
    
    // ========== FongMi/TV 扩展方法 ==========
    
    /**
     * 🔧 初始化 Spider
     * 
     * @param context Android 上下文
     * @param extend 扩展配置
     */
    open fun init(context: Context, extend: String) {
        this.context = context
        this.extend = extend
        Log.d(TAG, "🔧 Spider 初始化: ${this.javaClass.simpleName}")
        Log.d(TAG, "📄 扩展配置: $extend")
    }
    
    /**
     * 🗑️ 销毁 Spider
     */
    open fun destroy() {
        Log.d(TAG, "🗑️ Spider 销毁: ${this.javaClass.simpleName}")
        context = null
    }
    
    /**
     * 🎬 是否需要手动检查视频
     */
    open fun manualVideoCheck(): Boolean = false
    
    /**
     * 📹 是否为视频格式
     * 
     * @param url 链接地址
     * @return 是否为视频格式
     */
    open fun isVideoFormat(url: String): Boolean {
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", 
            ".webm", ".m4v", ".3gp", ".ts", ".m3u8"
        )
        return videoExtensions.any { url.lowercase().contains(it) }
    }
    
    /**
     * 📺 获取直播内容
     *
     * 基于 FongMi/TV 的 liveContent 方法
     * 用于解析直播源数据
     *
     * @param url 直播源链接
     * @return 解析后的直播源数据
     */
    open suspend fun liveContent(url: String): String {
        logDebug("📺 获取直播内容: $url")

        return try {
            // 默认实现：直接返回 URL 内容
            // 子类可以重写此方法实现特定的直播解析逻辑
            ""
        } catch (e: Exception) {
            logError("❌ 获取直播内容失败", e)
            ""
        }
    }

    /**
     * 🔧 本地代理
     *
     * 基于 FongMi/TV 的 localProxy 方法
     * 用于本地代理处理
     *
     * @param param 代理参数
     * @return 代理处理结果
     */
    open suspend fun localProxy(param: Map<String, String>): String {
        logDebug("🔧 本地代理: $param")

        return try {
            // 默认实现：返回空字符串
            // 子类可以重写此方法实现特定的代理逻辑
            ""
        } catch (e: Exception) {
            logError("❌ 本地代理失败", e)
            ""
        }
    }

    /**
     * ⚡ 执行动作
     *
     * 基于 FongMi/TV 的 action 方法
     * 用于执行特定动作
     *
     * @param action 动作名称
     * @return 动作执行结果
     */
    open suspend fun action(action: String): String {
        logDebug("⚡ 执行动作: $action")

        return try {
            // 默认实现：返回空字符串
            // 子类可以重写此方法实现特定的动作处理
            ""
        } catch (e: Exception) {
            logError("❌ 执行动作失败", e)
            ""
        }
    }

    /**
     * 🔗 获取真实链接
     *
     * @param url 原始链接
     * @return 真实链接
     */
    open suspend fun getRealUrl(url: String): String = url
    
    /**
     * 📊 获取 Spider 信息
     */
    open fun getSpiderInfo(): Map<String, Any> {
        return mapOf(
            "name" to this.javaClass.simpleName,
            "siteKey" to siteKey,
            "siteName" to siteName,
            "siteUrl" to siteUrl,
            "initialized" to (context != null)
        )
    }
    
    // ========== 工具方法 ==========
    
    /**
     * 🏗️ 构建 JSON 响应
     */
    protected fun buildJsonResponse(builder: JsonObject.() -> Unit): String {
        return buildJsonObject(builder).toString()
    }
    
    /**
     * ❌ 构建错误响应
     */
    protected fun buildErrorResponse(message: String?): String {
        return buildJsonResponse {
            put("error", message ?: "Unknown error")
            put("list", buildJsonArray {})
        }
    }
    
    /**
     * ✅ 构建成功响应
     */
    protected fun buildSuccessResponse(data: Any): String {
        return when (data) {
            is String -> data
            else -> json.encodeToString(JsonObject.serializer(), data as JsonObject)
        }
    }
    
    /**
     * 🔧 设置站点信息
     */
    fun setSiteInfo(key: String, name: String, url: String, headers: Map<String, String> = emptyMap()) {
        this.siteKey = key
        this.siteName = name
        this.siteUrl = url
        this.siteHeaders = headers
        
        Log.d(TAG, "🔧 设置站点信息:")
        Log.d(TAG, "   Key: $key")
        Log.d(TAG, "   Name: $name") 
        Log.d(TAG, "   URL: $url")
        Log.d(TAG, "   Headers: ${headers.size} 个")
    }
    
    /**
     * 📝 记录调试信息
     */
    protected fun logDebug(message: String) {
        Log.d(TAG, "[${this.javaClass.simpleName}] $message")
    }
    
    /**
     * ⚠️ 记录警告信息
     */
    protected fun logWarning(message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[${this.javaClass.simpleName}] $message", throwable)
    }
    
    /**
     * ❌ 记录错误信息
     */
    protected fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[${this.javaClass.simpleName}] $message", throwable)
    }
}
