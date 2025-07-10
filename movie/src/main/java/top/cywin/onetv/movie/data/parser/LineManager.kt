package top.cywin.onetv.movie.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.models.VodFlag
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodParse
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 线路管理器 - 完全基于OneMoVie架构的线路切换系统
 * 实现Flag管理、Parse管理、智能线路选择
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class LineManager(
    private val parseManager: ParseManager
) {

    companion object {
        private const val TAG = "LineManager"

        @Volatile
        private var INSTANCE: LineManager? = null

        fun get(): LineManager {
            return INSTANCE ?: throw IllegalStateException("LineManager not initialized")
        }

        fun initialize(instance: LineManager) {
            INSTANCE = instance
        }
    }

    init {
        initialize(this)
    }
    
    /**
     * 线路信息数据类
     */
    data class LineInfo(
        val flag: VodFlag,
        val parse: VodParse?,
        val quality: LineQuality,
        val speed: Int = 0, // 线路速度评分 (0-100)
        val isAvailable: Boolean = true
    )
    
    /**
     * 线路质量枚举
     */
    enum class LineQuality(val displayName: String, val priority: Int) {
        ULTRA_HD("超清", 5),
        HD("高清", 4),
        SD("标清", 3),
        LD("流畅", 2),
        UNKNOWN("未知", 1)
    }
    
    /**
     * 线路选择结果
     */
    data class LineSelectionResult(
        val selectedLine: LineInfo,
        val playUrl: String,
        val allLines: List<LineInfo>
    )
    
    private val lineSpeedCache = mutableMapOf<String, Int>()
    private val lineAvailabilityCache = mutableMapOf<String, Boolean>()
    
    /**
     * 获取所有可用线路
     */
    suspend fun getAvailableLines(
        vodItem: VodItem,
        availableParsers: List<VodParse>
    ): List<LineInfo> {
        return withContext(Dispatchers.Default) {
            val playFlags = vodItem.parseFlags()
            val lines = mutableListOf<LineInfo>()
            
            playFlags.forEach { flag ->
                // 为每个播放源匹配合适的解析器
                val suitableParsers = findSuitableParsers(flag, availableParsers)
                
                suitableParsers.forEach { parse ->
                    val lineKey = "${flag.flag}_${parse?.name ?: "direct"}"
                    val quality = detectLineQuality(flag.flag)
                    val speed = lineSpeedCache[lineKey] ?: 0
                    val isAvailable = lineAvailabilityCache[lineKey] ?: true
                    
                    lines.add(
                        LineInfo(
                            flag = flag,
                            parse = parse,
                            quality = quality,
                            speed = speed,
                            isAvailable = isAvailable
                        )
                    )
                }
            }
            
            // 按优先级排序：质量 > 速度 > 可用性
            lines.sortedWith(compareByDescending<LineInfo> { it.quality.priority }
                .thenByDescending { it.speed }
                .thenByDescending { it.isAvailable })
        }
    }
    
    /**
     * 智能选择最佳线路
     */
    suspend fun selectBestLine(
        vodItem: VodItem,
        availableParsers: List<VodParse>,
        preferredQuality: LineQuality? = null
    ): LineSelectionResult? {
        val availableLines = getAvailableLines(vodItem, availableParsers)
        
        if (availableLines.isEmpty()) {
            return null
        }
        
        // 根据偏好选择线路
        val selectedLine = when {
            preferredQuality != null -> {
                availableLines.find { it.quality == preferredQuality && it.isAvailable }
                    ?: availableLines.first { it.isAvailable }
            }
            else -> availableLines.first { it.isAvailable }
        }
        
        // 解析播放地址
        val episodes = selectedLine.flag.createEpisodes()
        val firstEpisode = episodes.firstOrNull()
        
        if (firstEpisode == null) {
            return null
        }
        
        val playUrl = parseManager.parsePlayUrl(
            url = firstEpisode.url,
            parse = selectedLine.parse,
            flag = selectedLine.flag.flag
        )
        
        return LineSelectionResult(
            selectedLine = selectedLine,
            playUrl = playUrl,
            allLines = availableLines
        )
    }
    
    /**
     * 切换到指定线路
     */
    suspend fun switchToLine(
        lineInfo: LineInfo,
        episodeIndex: Int = 0
    ): String? {
        return try {
            val episodes = lineInfo.flag.createEpisodes()
            val targetEpisode = episodes.getOrNull(episodeIndex) ?: episodes.firstOrNull()
            
            if (targetEpisode != null) {
                parseManager.parsePlayUrl(
                    url = targetEpisode.url,
                    parse = lineInfo.parse,
                    flag = lineInfo.flag.flag
                )
            } else null
            
        } catch (e: Exception) {
            // 标记该线路不可用
            val lineKey = "${lineInfo.flag.flag}_${lineInfo.parse?.name ?: "direct"}"
            lineAvailabilityCache[lineKey] = false
            null
        }
    }
    
    /**
     * 测试线路速度
     */
    suspend fun testLineSpeed(lineInfo: LineInfo): Int {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                // 获取第一集进行测试
                val episodes = lineInfo.flag.createEpisodes()
                val testEpisode = episodes.firstOrNull()
                
                if (testEpisode != null) {
                    val playUrl = parseManager.parsePlayUrl(
                        url = testEpisode.url,
                        parse = lineInfo.parse,
                        flag = lineInfo.flag.flag
                    )
                    
                    val endTime = System.currentTimeMillis()
                    val responseTime = endTime - startTime
                    
                    // 根据响应时间计算速度评分 (0-100)
                    val speed = when {
                        responseTime < 1000 -> 100 // 1秒内
                        responseTime < 3000 -> 80  // 3秒内
                        responseTime < 5000 -> 60  // 5秒内
                        responseTime < 10000 -> 40 // 10秒内
                        else -> 20
                    }
                    
                    // 缓存速度结果
                    val lineKey = "${lineInfo.flag.flag}_${lineInfo.parse?.name ?: "direct"}"
                    lineSpeedCache[lineKey] = speed
                    lineAvailabilityCache[lineKey] = true
                    
                    speed
                } else 0
                
            } catch (e: Exception) {
                // 测试失败，标记为不可用
                val lineKey = "${lineInfo.flag.flag}_${lineInfo.parse?.name ?: "direct"}"
                lineAvailabilityCache[lineKey] = false
                0
            }
        }
    }
    
    /**
     * 查找适合的解析器
     */
    private fun findSuitableParsers(
        flag: VodFlag,
        availableParsers: List<VodParse>
    ): List<VodParse?> {
        val episodes = flag.createEpisodes()
        val sampleUrl = episodes.firstOrNull()?.url ?: return listOf(null)
        
        val suitableParsers = mutableListOf<VodParse?>()
        
        // 检查是否为直链
        if (isDirectPlayUrl(sampleUrl)) {
            suitableParsers.add(null) // null表示直接播放
        }
        
        // 查找匹配的解析器
        availableParsers.forEach { parse ->
            if (isParserSuitable(sampleUrl, parse)) {
                suitableParsers.add(parse)
            }
        }
        
        return suitableParsers.ifEmpty { listOf(null) }
    }
    
    /**
     * 检测线路质量
     */
    private fun detectLineQuality(flagName: String): LineQuality {
        val lowerFlag = flagName.lowercase()
        return when {
            lowerFlag.contains("超清") || lowerFlag.contains("4k") || lowerFlag.contains("uhd") -> LineQuality.ULTRA_HD
            lowerFlag.contains("高清") || lowerFlag.contains("hd") || lowerFlag.contains("1080") -> LineQuality.HD
            lowerFlag.contains("标清") || lowerFlag.contains("sd") || lowerFlag.contains("720") -> LineQuality.SD
            lowerFlag.contains("流畅") || lowerFlag.contains("ld") || lowerFlag.contains("480") -> LineQuality.LD
            else -> LineQuality.UNKNOWN
        }
    }
    
    /**
     * 检查是否为直链播放地址
     */
    private fun isDirectPlayUrl(url: String): Boolean {
        val directExtensions = listOf(".mp4", ".m3u8", ".flv", ".avi", ".mkv", ".ts", ".mov")
        return directExtensions.any { url.contains(it, ignoreCase = true) } ||
               (url.startsWith("http") && !url.contains("url=") && !url.contains("v="))
    }
    
    /**
     * 检查解析器是否适合该URL
     */
    private fun isParserSuitable(url: String, parse: VodParse): Boolean {
        // 根据URL特征和解析器类型判断适合性
        return when (parse.type) {
            0 -> !isDirectPlayUrl(url) // Web解析器适合非直链
            1 -> url.contains("json") || url.contains("api") // JSON解析器适合API地址
            else -> true
        }
    }
    
    /**
     * 获取线路统计信息
     */
    fun getLineStats(): Map<String, Any> {
        return mapOf(
            "total_lines" to lineSpeedCache.size,
            "available_lines" to lineAvailabilityCache.values.count { it },
            "average_speed" to if (lineSpeedCache.isNotEmpty()) {
                lineSpeedCache.values.average().toInt()
            } else 0,
            "cache_size" to lineSpeedCache.size
        )
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        lineSpeedCache.clear()
        lineAvailabilityCache.clear()
    }

    /**
     * 获取最佳解析器 (参考OneMoVie getBestParse)
     */
    fun getBestParseForFlag(flag: VodFlag): VodParse? {
        return try {
            val configManager = VodConfigManager.getInstance()
            val parses = configManager.getParses()

            // 优先选择神解析器
            val godParse = parses.find { it.isGod() }
            if (godParse != null) return godParse

            // 根据Flag名称选择合适的解析器
            when {
                flag.flag.contains("m3u8", ignoreCase = true) -> {
                    parses.find { it.type == 0 } // 嗅探解析
                }
                flag.flag.contains("mp4", ignoreCase = true) -> {
                    parses.find { it.type == 0 } // 嗅探解析
                }
                flag.flag.contains("json", ignoreCase = true) -> {
                    parses.find { it.type == 1 } // JSON解析
                }
                else -> {
                    parses.find { it.type == 1 } ?: parses.firstOrNull() // JSON解析或第一个
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建解析任务 (参考OneMoVie ParseJob)
     */
    fun createParseJob(callback: top.cywin.onetv.movie.data.callback.ParseCallback): VodParseJob {
        return VodParseJob(callback)
    }
}
