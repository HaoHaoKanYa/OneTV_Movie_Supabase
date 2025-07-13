package top.cywin.onetv.film.parser

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.data.models.PlayerResult
import top.cywin.onetv.film.utils.JsonUtils
import top.cywin.onetv.film.utils.StringUtils
import top.cywin.onetv.film.utils.UrlUtils
import top.cywin.onetv.film.cache.CacheManager

/**
 * 增强播放器解析器
 * 
 * 基于 FongMi/TV 的播放器解析系统
 * 负责播放链接的解析和处理
 * 
 * 核心功能：
 * - 播放链接解析
 * - 播放器 Hook 处理
 * - 播放 URL 最终处理
 * - 播放参数优化
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class EnhancedPlayerParser(
    private val context: Context,
    private val spiderManager: SpiderManager,
    private val hookManager: HookManager,
    private val cacheManager: CacheManager
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_ENHANCED_PLAYER_PARSER"
        private const val PLAYER_CACHE_DURATION = 5 * 60 * 1000L // 5分钟
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * 获取播放内容
     * 
     * 这是 FongMi/TV 播放解析的核心入口
     */
    suspend fun getPlayerContent(
        siteKey: String,
        flag: String,
        id: String,
        vipFlags: List<String> = emptyList()
    ): Result<PlayerResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎬 开始解析播放内容: $siteKey, flag=$flag, id=$id")
            
            // 1. 检查播放缓存
            val cacheKey = "player:$siteKey:$flag:${StringUtils.md5(id)}"
            val cachedResult = getCachedPlayerResult(cacheKey)
            if (cachedResult != null) {
                Log.d(TAG, "✅ 使用缓存播放结果")
                return@withContext Result.success(cachedResult)
            }
            
            // 2. 获取 Spider 实例
            val spider = spiderManager.getSpider(siteKey)
            if (spider == null) {
                Log.w(TAG, "⚠️ 未找到 Spider: $siteKey")
                return@withContext Result.failure(Exception("Spider not found: $siteKey"))
            }
            
            // 3. 执行播放内容获取
            val resultJson = spider.playerContent(flag, id, vipFlags)
            val playerResult = parsePlayerResult(resultJson, siteKey, flag, id)
            
            // 4. 应用 Hook 处理
            val hookedResult = applyPlayerHooks(playerResult)
            
            // 5. 缓存结果
            cachePlayerResult(cacheKey, hookedResult)
            
            Log.d(TAG, "✅ 播放内容解析完成: ${hookedResult.url}")
            Result.success(hookedResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 播放内容解析失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析播放结果
     */
    private fun parsePlayerResult(resultJson: String, siteKey: String, flag: String, id: String): PlayerResult {
        return try {
            val jsonElement = json.parseToJsonElement(resultJson)
            val jsonObject = jsonElement.jsonObject
            
            // 解析播放 URL
            val url = jsonObject["url"]?.toString()?.trim('"') ?: ""
            
            // 解析播放头信息
            val header = jsonObject["header"]?.jsonObject?.mapValues { 
                it.value.toString().trim('"') 
            } ?: emptyMap()
            
            // 解析播放类型
            val parse = jsonObject["parse"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            
            // 解析播放格式
            val playType = jsonObject["playType"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            
            // 解析字幕信息
            val subt = jsonObject["subt"]?.toString()?.trim('"') ?: ""
            
            // 解析其他参数
            val jx = jsonObject["jx"]?.toString()?.trim('"')?.toIntOrNull() ?: 0
            val danmaku = jsonObject["danmaku"]?.toString()?.trim('"') ?: ""
            
            PlayerResult(
                url = url,
                header = header,
                parse = parse,
                playType = playType,
                subt = subt,
                jx = jx,
                danmaku = danmaku,
                siteKey = siteKey,
                flag = flag,
                id = id
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 播放结果解析失败", e)
            PlayerResult(
                url = "",
                header = emptyMap(),
                parse = 0,
                playType = 0,
                subt = "",
                jx = 0,
                danmaku = "",
                siteKey = siteKey,
                flag = flag,
                id = id
            )
        }
    }
    
    /**
     * 应用播放器 Hook
     */
    private suspend fun applyPlayerHooks(playerResult: PlayerResult): PlayerResult {
        return try {
            Log.d(TAG, "🪝 应用播放器 Hook: ${playerResult.url}")
            
            // 1. URL Hook 处理
            val hookedUrl = hookManager.processPlayerUrl(
                playerResult.url,
                playerResult.header
            )
            
            // 2. 头信息 Hook 处理
            val hookedHeaders = hookManager.processPlayerHeaders(
                playerResult.header,
                playerResult.url
            )
            
            // 3. 创建处理后的结果
            playerResult.copy(
                url = hookedUrl,
                header = hookedHeaders
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 播放器 Hook 处理失败", e)
            playerResult
        }
    }
    
    /**
     * 验证播放 URL
     */
    private fun validatePlayerUrl(url: String): Boolean {
        return try {
            if (url.isBlank()) {
                return false
            }
            
            // 检查 URL 格式
            if (!StringUtils.isValidUrl(url) && !url.startsWith("data:")) {
                return false
            }
            
            // 检查支持的协议
            val supportedProtocols = listOf("http", "https", "rtmp", "rtsp", "data", "file")
            val protocol = UrlUtils.extractProtocol(url)
            if (protocol !in supportedProtocols) {
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 播放 URL 验证失败: $url", e)
            false
        }
    }
    
    /**
     * 优化播放参数
     */
    private fun optimizePlayerParams(playerResult: PlayerResult): PlayerResult {
        return try {
            val optimizedHeaders = playerResult.header.toMutableMap()
            
            // 添加默认 User-Agent
            if (!optimizedHeaders.containsKey("User-Agent")) {
                optimizedHeaders["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }
            
            // 添加默认 Referer
            if (!optimizedHeaders.containsKey("Referer") && playerResult.url.isNotBlank()) {
                val domain = UrlUtils.extractDomain(playerResult.url)
                if (domain.isNotBlank()) {
                    optimizedHeaders["Referer"] = "https://$domain/"
                }
            }
            
            playerResult.copy(header = optimizedHeaders)
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 播放参数优化失败", e)
            playerResult
        }
    }
    
    /**
     * 处理特殊播放格式
     */
    private fun handleSpecialFormats(playerResult: PlayerResult): PlayerResult {
        return try {
            val url = playerResult.url
            
            when {
                // 处理 m3u8 格式
                url.contains(".m3u8") -> {
                    playerResult.copy(playType = 1) // HLS
                }
                
                // 处理 mp4 格式
                url.contains(".mp4") -> {
                    playerResult.copy(playType = 0) // MP4
                }
                
                // 处理 flv 格式
                url.contains(".flv") -> {
                    playerResult.copy(playType = 2) // FLV
                }
                
                // 处理 RTMP 流
                url.startsWith("rtmp") -> {
                    playerResult.copy(playType = 3) // RTMP
                }
                
                else -> playerResult
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 特殊格式处理失败", e)
            playerResult
        }
    }
    
    /**
     * 获取缓存播放结果
     */
    private suspend fun getCachedPlayerResult(cacheKey: String): PlayerResult? {
        return try {
            cacheManager.get(cacheKey, PlayerResult::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 缓存播放结果
     */
    private suspend fun cachePlayerResult(cacheKey: String, result: PlayerResult) {
        try {
            cacheManager.put(cacheKey, result, PLAYER_CACHE_DURATION)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 缓存播放结果失败", e)
        }
    }
    
    /**
     * 清除播放缓存
     */
    suspend fun clearPlayerCache() {
        try {
            cacheManager.clearByPrefix("player:")
            Log.d(TAG, "✅ 播放缓存已清除")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 清除播放缓存失败", e)
        }
    }
    
    /**
     * 获取播放解析统计信息
     */
    fun getPlayerStats(): Map<String, Any> {
        return mapOf(
            "parser_type" to "EnhancedPlayerParser",
            "cache_enabled" to true,
            "cache_duration_minutes" to (PLAYER_CACHE_DURATION / 60000),
            "supported_protocols" to listOf("HTTP", "HTTPS", "RTMP", "RTSP", "Data", "File"),
            "supported_formats" to listOf("MP4", "M3U8", "FLV", "RTMP"),
            "features" to listOf("Hook处理", "参数优化", "格式检测", "智能缓存")
        )
    }
}
