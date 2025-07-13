package top.cywin.onetv.film.hook

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.utils.RegexUtils
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

/**
 * 播放器拦截 Hook
 * 基于 FongMi/TV 标准实现
 * 
 * 提供播放器链接的拦截和处理功能
 * 
 * 功能：
 * - 播放链接解析
 * - 视频源优化
 * - 播放参数调整
 * - 防盗链处理
 * - 多线路支持
 * - 播放质量选择
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class PlayerHook : Hook, PlayerHook {
    
    companion object {
        private const val TAG = "ONETV_FILM_PLAYER_HOOK"
    }
    
    override val name: String = "PlayerHook"
    override val description: String = "播放器链接拦截和处理"
    override val priority: Int = 70
    
    // 统计信息
    private val stats = HookStats(name)
    private val processedCount = AtomicLong(0)
    private val resolvedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    
    // 播放器规则
    private val playerRules = mutableMapOf<String, PlayerRule>()
    
    // 支持的视频格式
    private val supportedFormats = setOf(
        "mp4", "m3u8", "flv", "avi", "mkv", "mov", "wmv", "webm", "ts"
    )
    
    // 默认播放器规则
    init {
        addDefaultPlayerRules()
    }
    
    override fun matches(context: HookContext): Boolean {
        return context is PlayerHookContext
    }
    
    override suspend fun processPlayerUrl(playerUrl: HookPlayerUrl): HookPlayerUrl = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "▶️ 处理播放器链接: ${playerUrl.originalUrl}")
            processedCount.incrementAndGet()
            
            val modifiedPlayerUrl = playerUrl.copy()
            var isResolved = false
            
            // 1. 检查是否为直链
            if (isDirectVideoUrl(playerUrl.originalUrl)) {
                Log.d(TAG, "🎯 检测到直链视频")
                modifiedPlayerUrl.processedUrl = playerUrl.originalUrl
                isResolved = true
            } else {
                // 2. 尝试解析播放链接
                val resolvedUrl = resolvePlayerUrl(playerUrl.originalUrl, playerUrl.flag)
                if (resolvedUrl != null) {
                    modifiedPlayerUrl.processedUrl = resolvedUrl
                    isResolved = true
                    Log.d(TAG, "✅ 播放链接解析成功: $resolvedUrl")
                }
            }
            
            // 3. 优化播放链接
            val optimizedUrl = optimizePlayerUrl(modifiedPlayerUrl.processedUrl)
            if (optimizedUrl != modifiedPlayerUrl.processedUrl) {
                modifiedPlayerUrl.processedUrl = optimizedUrl
                Log.d(TAG, "🔧 播放链接已优化: $optimizedUrl")
            }
            
            // 4. 设置播放器请求头
            setPlayerHeaders(modifiedPlayerUrl)
            
            // 5. 设置播放参数
            setPlayerParams(modifiedPlayerUrl)
            
            // 6. 添加元数据
            modifiedPlayerUrl.setMetadata("processed_by", name)
            modifiedPlayerUrl.setMetadata("processed_time", System.currentTimeMillis())
            modifiedPlayerUrl.setMetadata("is_resolved", isResolved)
            
            if (isResolved) {
                resolvedCount.incrementAndGet()
            } else {
                failedCount.incrementAndGet()
                Log.w(TAG, "⚠️ 播放链接解析失败: ${playerUrl.originalUrl}")
            }
            
            // 记录统计
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.success(modifiedPlayerUrl), duration)
            
            modifiedPlayerUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 播放器处理失败: ${playerUrl.originalUrl}", e)
            failedCount.incrementAndGet()
            val duration = System.currentTimeMillis() - startTime
            stats.recordExecution(HookResult.failure(e.message ?: "播放器处理失败", e), duration)
            throw e
        }
    }
    
    /**
     * 🎯 检查是否为直链视频
     */
    private fun isDirectVideoUrl(url: String): Boolean {
        return try {
            val urlObj = URL(url)
            val path = urlObj.path.lowercase()
            
            // 检查文件扩展名
            supportedFormats.any { format ->
                path.endsWith(".$format") || path.contains(".$format?")
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔧 解析播放链接
     */
    private suspend fun resolvePlayerUrl(url: String, flag: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 根据flag选择解析规则
            val rule = playerRules[flag] ?: getDefaultRule(url)
            if (rule != null) {
                return@withContext rule.resolve(url)
            }
            
            // 2. 通用解析逻辑
            return@withContext when {
                url.contains("m3u8") -> extractM3u8Url(url)
                url.contains("mp4") -> extractMp4Url(url)
                url.contains("flv") -> extractFlvUrl(url)
                url.contains("jx.") || url.contains("parse") -> parseJxUrl(url)
                else -> null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 播放链接解析异常: $url", e)
            null
        }
    }
    
    /**
     * 🔧 优化播放链接
     */
    private fun optimizePlayerUrl(url: String): String {
        var optimizedUrl = url
        
        try {
            // 1. 移除无用参数
            optimizedUrl = removeUselessParams(optimizedUrl)
            
            // 2. 协议优化
            if (optimizedUrl.startsWith("http://") && shouldUpgradeToHttps(optimizedUrl)) {
                optimizedUrl = optimizedUrl.replace("http://", "https://")
            }
            
            // 3. 域名优化
            optimizedUrl = optimizeDomain(optimizedUrl)
            
            // 4. 路径优化
            optimizedUrl = optimizePath(optimizedUrl)
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 播放链接优化失败: $url", e)
        }
        
        return optimizedUrl
    }
    
    /**
     * 🔧 设置播放器请求头
     */
    private fun setPlayerHeaders(playerUrl: HookPlayerUrl) {
        val url = playerUrl.processedUrl
        
        // 基础请求头
        playerUrl.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        playerUrl.addHeader("Accept", "*/*")
        playerUrl.addHeader("Accept-Encoding", "identity")
        playerUrl.addHeader("Connection", "keep-alive")
        
        // 根据URL设置特殊请求头
        when {
            url.contains("youku.com") -> {
                playerUrl.addHeader("Referer", "https://www.youku.com/")
            }
            
            url.contains("iqiyi.com") -> {
                playerUrl.addHeader("Referer", "https://www.iqiyi.com/")
            }
            
            url.contains("qq.com") -> {
                playerUrl.addHeader("Referer", "https://v.qq.com/")
            }
            
            url.contains("bilibili.com") -> {
                playerUrl.addHeader("Referer", "https://www.bilibili.com/")
                playerUrl.addHeader("Origin", "https://www.bilibili.com")
            }
            
            url.contains("m3u8") -> {
                playerUrl.addHeader("Accept", "application/vnd.apple.mpegurl")
            }
        }
    }
    
    /**
     * 🔧 设置播放参数
     */
    private fun setPlayerParams(playerUrl: HookPlayerUrl) {
        val url = playerUrl.processedUrl
        
        // 根据视频格式设置解析参数
        when {
            url.contains("m3u8") -> {
                playerUrl.parse = 0 // 直接播放
            }
            
            url.contains("mp4") -> {
                playerUrl.parse = 0 // 直接播放
            }
            
            url.contains("flv") -> {
                playerUrl.parse = 0 // 直接播放
            }
            
            url.contains("jx.") || url.contains("parse") -> {
                playerUrl.parse = 1 // 需要解析
            }
            
            else -> {
                playerUrl.parse = 1 // 默认需要解析
            }
        }
    }
    
    /**
     * 🔧 提取M3U8链接
     */
    private suspend fun extractM3u8Url(url: String): String? = withContext(Dispatchers.IO) {
        try {
            // 简单的M3U8链接提取逻辑
            val m3u8Pattern = "https?://[^\\s\"']+\\.m3u8[^\\s\"']*".toRegex()
            val matches = m3u8Pattern.findAll(url)
            return@withContext matches.firstOrNull()?.value
        } catch (e: Exception) {
            Log.e(TAG, "❌ M3U8链接提取失败: $url", e)
            null
        }
    }
    
    /**
     * 🔧 提取MP4链接
     */
    private suspend fun extractMp4Url(url: String): String? = withContext(Dispatchers.IO) {
        try {
            // 简单的MP4链接提取逻辑
            val mp4Pattern = "https?://[^\\s\"']+\\.mp4[^\\s\"']*".toRegex()
            val matches = mp4Pattern.findAll(url)
            return@withContext matches.firstOrNull()?.value
        } catch (e: Exception) {
            Log.e(TAG, "❌ MP4链接提取失败: $url", e)
            null
        }
    }
    
    /**
     * 🔧 提取FLV链接
     */
    private suspend fun extractFlvUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            // 简单的FLV链接提取逻辑
            val flvPattern = "https?://[^\\s\"']+\\.flv[^\\s\"']*".toRegex()
            val matches = flvPattern.findAll(url)
            return@withContext matches.firstOrNull()?.value
        } catch (e: Exception) {
            Log.e(TAG, "❌ FLV链接提取失败: $url", e)
            null
        }
    }
    
    /**
     * 🔧 解析JX链接
     */
    private suspend fun parseJxUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            // 这里应该实现具体的JX解析逻辑
            // 暂时返回原链接
            Log.d(TAG, "🔧 JX链接解析: $url")
            return@withContext url
        } catch (e: Exception) {
            Log.e(TAG, "❌ JX链接解析失败: $url", e)
            null
        }
    }
    
    /**
     * 🔧 获取默认规则
     */
    private fun getDefaultRule(url: String): PlayerRule? {
        return when {
            url.contains("youku") -> playerRules["youku"]
            url.contains("iqiyi") -> playerRules["iqiyi"]
            url.contains("qq.com") -> playerRules["tencent"]
            url.contains("bilibili") -> playerRules["bilibili"]
            else -> null
        }
    }
    
    /**
     * 🔧 移除无用参数
     */
    private fun removeUselessParams(url: String): String {
        val uselessParams = setOf(
            "t", "timestamp", "time", "r", "random", "cache", "_"
        )
        
        return try {
            val urlObj = URL(url)
            val query = urlObj.query ?: return url
            
            val cleanParams = query.split("&")
                .filter { param ->
                    val key = param.split("=")[0]
                    !uselessParams.contains(key)
                }
                .joinToString("&")
            
            if (cleanParams.isEmpty()) {
                "${urlObj.protocol}://${urlObj.host}${urlObj.path}"
            } else {
                "${urlObj.protocol}://${urlObj.host}${urlObj.path}?$cleanParams"
            }
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * 🔒 是否应该升级到HTTPS
     */
    private fun shouldUpgradeToHttps(url: String): Boolean {
        return try {
            val host = URL(url).host
            // CDN和知名视频站点通常支持HTTPS
            host.contains("cdn") || host.contains("video") || 
            host.contains("youku") || host.contains("iqiyi") || 
            host.contains("qq.com") || host.contains("bilibili")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔧 优化域名
     */
    private fun optimizeDomain(url: String): String {
        // 域名优化逻辑（如CDN选择等）
        return url
    }
    
    /**
     * 🔧 优化路径
     */
    private fun optimizePath(url: String): String {
        // 路径优化逻辑
        return url
    }
    
    /**
     * 🔧 添加默认播放器规则
     */
    private fun addDefaultPlayerRules() {
        // 优酷规则
        playerRules["youku"] = PlayerRule("youku") { url ->
            // 优酷解析逻辑
            url
        }
        
        // 爱奇艺规则
        playerRules["iqiyi"] = PlayerRule("iqiyi") { url ->
            // 爱奇艺解析逻辑
            url
        }
        
        // 腾讯视频规则
        playerRules["tencent"] = PlayerRule("tencent") { url ->
            // 腾讯视频解析逻辑
            url
        }
        
        // B站规则
        playerRules["bilibili"] = PlayerRule("bilibili") { url ->
            // B站解析逻辑
            url
        }
    }
    
    /**
     * 🔧 添加播放器规则
     */
    fun addPlayerRule(name: String, rule: PlayerRule) {
        playerRules[name] = rule
        Log.d(TAG, "🔧 添加播放器规则: $name")
    }
    
    /**
     * 🗑️ 移除播放器规则
     */
    fun removePlayerRule(name: String) {
        playerRules.remove(name)
        Log.d(TAG, "🗑️ 移除播放器规则: $name")
    }
    
    override fun getStats(): Map<String, Any> {
        return stats.getReport() + mapOf(
            "processed_count" to processedCount.get(),
            "resolved_count" to resolvedCount.get(),
            "failed_count" to failedCount.get(),
            "success_rate" to if (processedCount.get() > 0) {
                (resolvedCount.get().toDouble() / processedCount.get() * 100)
            } else 0.0,
            "player_rules" to playerRules.size
        )
    }
    
    override fun cleanup() {
        playerRules.clear()
        Log.d(TAG, "🧹 PlayerHook 资源已清理")
    }
    
    /**
     * 播放器规则数据类
     */
    data class PlayerRule(
        val name: String,
        val resolver: suspend (String) -> String?
    ) {
        suspend fun resolve(url: String): String? {
            return resolver(url)
        }
    }
}
