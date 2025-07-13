package top.cywin.onetv.film.spider.base

import android.content.Context
import android.util.Log
import top.cywin.onetv.film.spider.xpath.XPathSpider
import top.cywin.onetv.film.spider.appys.AppYsSpider
import top.cywin.onetv.film.spider.javascript.JavaScriptSpider
import top.cywin.onetv.film.spider.drpy.DrpySpider
import top.cywin.onetv.film.spider.custom.YydsAli1Spider
import top.cywin.onetv.film.spider.custom.CokemvSpider
import top.cywin.onetv.film.spider.custom.AueteSpider
import top.cywin.onetv.film.spider.special.ThunderSpider
import top.cywin.onetv.film.spider.special.TvbusSpider
import top.cywin.onetv.film.spider.special.JianpianSpider
import top.cywin.onetv.film.spider.special.ForcetechSpider
import top.cywin.onetv.film.spider.cloud.AliDriveSpider
import top.cywin.onetv.film.spider.cloud.BaiduDriveSpider
import top.cywin.onetv.film.spider.cloud.QuarkDriveSpider
import java.util.concurrent.ConcurrentHashMap

/**
 * Spider 工厂
 * 基于 FongMi/TV 标准实现
 * 
 * 负责创建和管理各种类型的 Spider 实例
 * 
 * 功能：
 * - Spider 类型检测
 * - Spider 实例创建
 * - Spider 缓存管理
 * - 动态加载支持
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object SpiderFactory {
    
    private const val TAG = "ONETV_FILM_SPIDER_FACTORY"
    
    // Spider 实例缓存
    private val spiderCache = ConcurrentHashMap<String, Spider>()
    
    // Spider 类型映射
    private val spiderTypeMap = mapOf(
        // XPath 解析器
        "xpath" to XPathSpider::class.java,
        "csp_XPath" to XPathSpider::class.java,
        
        // AppYs 解析器
        "appys" to AppYsSpider::class.java,
        "csp_AppYs" to AppYsSpider::class.java,
        
        // JavaScript 解析器
        "javascript" to JavaScriptSpider::class.java,
        "csp_JavaScript" to JavaScriptSpider::class.java,
        
        // Drpy 解析器
        "drpy" to DrpySpider::class.java,
        "csp_Drpy" to DrpySpider::class.java,
        
        // 专用解析器
        "csp_YydsAli1" to YydsAli1Spider::class.java,
        "csp_Cokemv" to CokemvSpider::class.java,
        "csp_Auete" to AueteSpider::class.java,
        
        // 特殊解析器
        "csp_Thunder" to ThunderSpider::class.java,
        "csp_Tvbus" to TvbusSpider::class.java,
        "csp_Jianpian" to JianpianSpider::class.java,
        "csp_Forcetech" to ForcetechSpider::class.java,
        
        // 云盘解析器
        "csp_AliDrive" to AliDriveSpider::class.java,
        "csp_BaiduDrive" to BaiduDriveSpider::class.java,
        "csp_QuarkDrive" to QuarkDriveSpider::class.java
    )
    
    /**
     * 🏭 创建 Spider 实例
     * 
     * @param type Spider 类型
     * @param api API 地址
     * @param context Android 上下文
     * @param extend 扩展参数
     * @param useCache 是否使用缓存
     * @return Spider 实例，失败返回 null
     */
    fun createSpider(
        type: String,
        api: String,
        context: Context,
        extend: String = "",
        useCache: Boolean = true
    ): Spider? {
        return try {
            Log.d(TAG, "🏭 创建 Spider: type=$type, api=$api")
            
            val cacheKey = generateCacheKey(type, api, extend)
            
            // 检查缓存
            if (useCache && spiderCache.containsKey(cacheKey)) {
                Log.d(TAG, "📦 从缓存获取 Spider: $type")
                return spiderCache[cacheKey]
            }
            
            // 获取 Spider 类
            val spiderClass = getSpiderClass(type, api)
            if (spiderClass == null) {
                Log.w(TAG, "⚠️ 未找到 Spider 类型: $type")
                return null
            }
            
            // 创建实例
            val spider = spiderClass.getDeclaredConstructor().newInstance()
            
            // 初始化
            spider.init(context, extend)
            
            // 缓存实例
            if (useCache) {
                spiderCache[cacheKey] = spider
            }
            
            Log.d(TAG, "✅ Spider 创建成功: ${spider::class.simpleName}")
            spider
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spider 创建失败: type=$type, api=$api", e)
            null
        }
    }
    
    /**
     * 🔍 获取 Spider 类
     */
    private fun getSpiderClass(type: String, api: String): Class<out Spider>? {
        // 首先检查直接类型映射
        spiderTypeMap[type]?.let { return it }
        
        // 检查 API URL 模式匹配
        return when {
            // JavaScript 文件检测
            api.endsWith(".js") -> JavaScriptSpider::class.java
            
            // AppYs 接口检测
            api.contains("/api.php/provide/vod/") -> AppYsSpider::class.java
            
            // XPath 接口检测
            api.contains("xpath") || type.contains("xpath", ignoreCase = true) -> XPathSpider::class.java
            
            // Drpy 接口检测
            api.contains("drpy") || type.contains("drpy", ignoreCase = true) -> DrpySpider::class.java
            
            // 云盘接口检测
            api.contains("aliyundrive") || api.contains("alipan") -> AliDriveSpider::class.java
            api.contains("baidu") || api.contains("pan.baidu") -> BaiduDriveSpider::class.java
            api.contains("quark") -> QuarkDriveSpider::class.java
            
            // 特殊站点检测
            api.contains("cokemv") -> CokemvSpider::class.java
            api.contains("auete") -> AueteSpider::class.java
            api.contains("yydsys") -> YydsAli1Spider::class.java
            
            else -> {
                Log.w(TAG, "⚠️ 无法识别的 Spider 类型: type=$type, api=$api")
                null
            }
        }
    }
    
    /**
     * 🔧 生成缓存键
     */
    private fun generateCacheKey(type: String, api: String, extend: String): String {
        return "$type|$api|$extend".hashCode().toString()
    }
    
    /**
     * 📋 获取支持的 Spider 类型列表
     */
    fun getSupportedTypes(): List<String> {
        return spiderTypeMap.keys.toList()
    }
    
    /**
     * 🔍 检查是否支持指定类型
     */
    fun isTypeSupported(type: String): Boolean {
        return spiderTypeMap.containsKey(type)
    }
    
    /**
     * 🔍 自动检测 Spider 类型
     */
    fun detectSpiderType(api: String): String? {
        return when {
            api.endsWith(".js") -> "javascript"
            api.contains("/api.php/provide/vod/") -> "appys"
            api.contains("xpath") -> "xpath"
            api.contains("drpy") -> "drpy"
            api.contains("aliyundrive") || api.contains("alipan") -> "csp_AliDrive"
            api.contains("baidu") || api.contains("pan.baidu") -> "csp_BaiduDrive"
            api.contains("quark") -> "csp_QuarkDrive"
            api.contains("cokemv") -> "csp_Cokemv"
            api.contains("auete") -> "csp_Auete"
            api.contains("yydsys") -> "csp_YydsAli1"
            else -> null
        }
    }
    
    /**
     * 🧹 清理缓存
     */
    fun clearCache() {
        spiderCache.clear()
        Log.d(TAG, "🧹 Spider 缓存已清理")
    }
    
    /**
     * 📊 获取缓存统计
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cached_spiders" to spiderCache.size,
            "supported_types" to spiderTypeMap.size,
            "cache_keys" to spiderCache.keys.toList()
        )
    }
    
    /**
     * 🔄 重新加载 Spider
     */
    fun reloadSpider(type: String, api: String, context: Context, extend: String = ""): Spider? {
        val cacheKey = generateCacheKey(type, api, extend)
        spiderCache.remove(cacheKey)
        return createSpider(type, api, context, extend, useCache = true)
    }
}
