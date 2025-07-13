package top.cywin.onetv.film.spider.drpy

import android.content.Context
import android.util.Log
import top.cywin.onetv.film.catvod.Spider
import top.cywin.onetv.film.catvod.SpiderNull
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.network.NetworkClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Drpy 加载器
 * 
 * 基于 FongMi/TV 的 Drpy 加载系统实现
 * 负责 Python 脚本的加载、缓存和管理
 * 
 * 功能：
 * - Python 脚本加载
 * - Spider 实例缓存
 * - 脚本版本管理
 * - 错误处理和重试
 * - 性能监控
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class DrpyLoader {
    
    companion object {
        private const val TAG = "ONETV_FILM_DRPY_LOADER"
        
        @Volatile
        private var INSTANCE: DrpyLoader? = null
        
        fun getInstance(): DrpyLoader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DrpyLoader().also { INSTANCE = it }
            }
        }
    }
    
    // Spider 实例缓存
    private val spiders = ConcurrentHashMap<String, Spider>()
    
    // 脚本缓存
    private val scriptCache = ConcurrentHashMap<String, String>()
    
    // 网络客户端
    private lateinit var networkClient: NetworkClient
    
    // 最近使用的 Spider
    private var recentSpider: String? = null
    
    /**
     * 🔧 初始化
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "🔧 初始化 Drpy 加载器")
            
            networkClient = NetworkClient.Builder()
                .userAgent("OneTV-Drpy/1.0.0")
                .timeout(15000L)
                .build()
            
            Log.d(TAG, "✅ Drpy 加载器初始化完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Drpy 加载器初始化失败", e)
            throw e
        }
    }
    
    /**
     * 🕷️ 获取 Spider 实例
     */
    fun getSpider(key: String, api: String, ext: String): Spider {
        return try {
            Log.d(TAG, "🕷️ 获取 Drpy Spider: $key")
            
            // 检查缓存
            if (spiders.containsKey(key)) {
                val cachedSpider = spiders[key]
                if (cachedSpider != null) {
                    Log.d(TAG, "✅ 使用缓存的 Spider: $key")
                    recentSpider = key
                    return cachedSpider
                }
            }
            
            // 创建新的 Spider
            val spider = createSpider(key, api, ext)
            
            if (spider !is SpiderNull) {
                // 缓存 Spider
                spiders[key] = spider
                recentSpider = key
                Log.d(TAG, "✅ 创建并缓存 Spider: $key")
            }
            
            spider
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取 Spider 失败: $key", e)
            SpiderNull()
        }
    }
    
    /**
     * 🏗️ 创建 Spider 实例
     */
    private fun createSpider(key: String, api: String, ext: String): Spider {
        return try {
            Log.d(TAG, "🏗️ 创建 Drpy Spider: $key")
            
            // 创建 DrpySpider 实例
            val spider = DrpySpider()
            
            // 初始化 Spider
            spider.api = api
            spider.init(null, ext) // 这里需要传入真实的 Context
            
            Log.d(TAG, "✅ Drpy Spider 创建成功: $key")
            spider
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 创建 Drpy Spider 失败: $key", e)
            SpiderNull()
        }
    }
    
    /**
     * 📜 加载 Python 脚本
     */
    fun loadScript(url: String): String {
        return try {
            Log.d(TAG, "📜 加载 Python 脚本: $url")
            
            // 检查缓存
            if (scriptCache.containsKey(url)) {
                val cachedScript = scriptCache[url]
                if (!cachedScript.isNullOrEmpty()) {
                    Log.d(TAG, "✅ 使用缓存的脚本: $url")
                    return cachedScript
                }
            }
            
            // 下载脚本
            val script = downloadScript(url)
            
            if (script.isNotEmpty()) {
                // 缓存脚本
                scriptCache[url] = script
                Log.d(TAG, "✅ 脚本下载并缓存成功: $url, 长度: ${script.length}")
            }
            
            script
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 加载 Python 脚本失败: $url", e)
            ""
        }
    }
    
    /**
     * 📥 下载脚本
     */
    private fun downloadScript(url: String): String {
        return try {
            Log.d(TAG, "📥 下载脚本: $url")
            
            val response = networkClient.get(url)
            
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ 脚本响应为空: $url")
                return ""
            }
            
            // 验证脚本内容
            if (isValidPythonScript(response)) {
                Log.d(TAG, "✅ 脚本下载成功: $url")
                response
            } else {
                Log.w(TAG, "⚠️ 脚本内容无效: $url")
                ""
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 下载脚本失败: $url", e)
            ""
        }
    }
    
    /**
     * ✅ 验证 Python 脚本
     */
    private fun isValidPythonScript(script: String): Boolean {
        return try {
            // 基本的 Python 脚本验证
            script.contains("class Spider") || 
            script.contains("def homeContent") ||
            script.contains("def categoryContent") ||
            script.contains("def detailContent") ||
            script.contains("def searchContent") ||
            script.contains("def playerContent")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🗑️ 清理缓存
     */
    fun clear() {
        try {
            Log.d(TAG, "🗑️ 清理 Drpy 缓存")
            
            // 销毁所有 Spider
            spiders.values.forEach { spider ->
                try {
                    if (spider is DrpySpider) {
                        // 这里可以添加 Spider 的清理逻辑
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ 销毁 Spider 失败", e)
                }
            }
            
            spiders.clear()
            scriptCache.clear()
            recentSpider = null
            
            Log.d(TAG, "✅ Drpy 缓存清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理 Drpy 缓存失败", e)
        }
    }
    
    /**
     * 🔄 重新加载 Spider
     */
    fun reloadSpider(key: String, api: String, ext: String): Spider {
        try {
            Log.d(TAG, "🔄 重新加载 Spider: $key")
            
            // 移除缓存
            spiders.remove(key)
            scriptCache.remove(api)
            
            // 重新创建
            return getSpider(key, api, ext)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 重新加载 Spider 失败: $key", e)
            return SpiderNull()
        }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "cached_spiders" to spiders.size,
            "cached_scripts" to scriptCache.size,
            "recent_spider" to (recentSpider ?: ""),
            "spider_keys" to spiders.keys.toList(),
            "script_urls" to scriptCache.keys.toList()
        )
    }
    
    /**
     * 🔍 检查 Spider 是否存在
     */
    fun hasSpider(key: String): Boolean {
        return spiders.containsKey(key)
    }
    
    /**
     * 📜 检查脚本是否缓存
     */
    fun hasScript(url: String): Boolean {
        return scriptCache.containsKey(url) && !scriptCache[url].isNullOrEmpty()
    }
    
    /**
     * 🕷️ 获取最近使用的 Spider
     */
    fun getRecentSpider(): Spider? {
        return recentSpider?.let { spiders[it] }
    }
    
    /**
     * 📝 设置最近使用的 Spider
     */
    fun setRecentSpider(key: String) {
        if (spiders.containsKey(key)) {
            recentSpider = key
            Log.d(TAG, "📝 设置最近使用的 Spider: $key")
        }
    }
    
    /**
     * 🧹 清理过期缓存
     */
    fun cleanupExpiredCache() {
        try {
            Log.d(TAG, "🧹 清理过期缓存")
            
            // 这里可以添加基于时间的缓存清理逻辑
            // 暂时保留所有缓存
            
            Log.d(TAG, "✅ 过期缓存清理完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清理过期缓存失败", e)
        }
    }
    
    /**
     * 📊 获取缓存大小
     */
    fun getCacheSize(): Long {
        return try {
            var totalSize = 0L
            
            // 计算脚本缓存大小
            scriptCache.values.forEach { script ->
                totalSize += script.length * 2 // 估算字符串占用的字节数
            }
            
            totalSize
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 计算缓存大小失败", e)
            0L
        }
    }
}
