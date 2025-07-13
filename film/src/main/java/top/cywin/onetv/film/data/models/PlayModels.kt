package top.cywin.onetv.film.data.models

import kotlinx.serialization.Serializable

/**
 * 播放相关数据模型
 * 
 * 基于 FongMi/TV 的播放数据模型定义
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 播放信息
 */
@Serializable
data class PlayInfo(
    val vodId: String,                  // VOD ID
    val vodName: String,                // VOD 名称
    val vodPic: String = "",            // VOD 图片
    val siteKey: String,                // 站点标识
    val siteName: String = "",          // 站点名称
    val flag: String,                   // 播放标识
    val episodeName: String,            // 集数名称
    val episodeUrl: String,             // 集数地址
    val episodeIndex: Int = 0,          // 集数索引
    val totalEpisodes: Int = 0,         // 总集数
    val position: Long = 0L,            // 播放位置（毫秒）
    val duration: Long = 0L,            // 总时长（毫秒）
    val playTime: Long = System.currentTimeMillis(), // 播放时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val isFinished: Boolean = false,    // 是否播放完成
    val playCount: Int = 1,             // 播放次数
    val lastPlayTime: Long = System.currentTimeMillis(), // 最后播放时间
    val playSpeed: Float = 1.0f,        // 播放速度
    val volume: Float = 1.0f,           // 音量
    val brightness: Float = 0.5f,       // 亮度
    val subtitle: String = "",          // 字幕
    val audioTrack: String = "",        // 音轨
    val videoQuality: String = "",      // 视频质量
    val playerType: String = "",        // 播放器类型
    val userAgent: String = "",         // User-Agent
    val referer: String = "",           // Referer
    val headers: Map<String, String> = emptyMap(), // 请求头
    val extra: Map<String, String> = emptyMap()    // 额外信息
) {
    
    /**
     * 📊 获取播放进度百分比
     */
    fun getProgressPercent(): Float {
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 🔍 是否刚开始播放
     */
    fun isJustStarted(): Boolean {
        return position < 30000L // 30秒内
    }
    
    /**
     * 🔍 是否接近结束
     */
    fun isNearEnd(): Boolean {
        return duration > 0 && (duration - position) < 60000L // 1分钟内
    }
    
    /**
     * 🔍 是否可以续播
     */
    fun canResume(): Boolean {
        return position > 30000L && !isFinished && !isNearEnd()
    }
    
    /**
     * 📊 获取播放摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "vod_id" to vodId,
            "vod_name" to vodName,
            "site_key" to siteKey,
            "flag" to flag,
            "episode_name" to episodeName,
            "episode_index" to episodeIndex,
            "total_episodes" to totalEpisodes,
            "progress_percent" to String.format("%.1f%%", getProgressPercent() * 100),
            "position_formatted" to formatTime(position),
            "duration_formatted" to formatTime(duration),
            "is_finished" to isFinished,
            "can_resume" to canResume(),
            "play_count" to playCount,
            "last_play_time" to lastPlayTime
        )
    }
    
    /**
     * ⏰ 格式化时间
     */
    private fun formatTime(timeMs: Long): String {
        val seconds = timeMs / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }
}

/**
 * 播放历史
 */
@Serializable
data class PlayHistory(
    val id: String = "",                // 历史记录ID
    val vodId: String,                  // VOD ID
    val vodName: String,                // VOD 名称
    val vodPic: String = "",            // VOD 图片
    val siteKey: String,                // 站点标识
    val siteName: String = "",          // 站点名称
    val typeId: String = "",            // 分类ID
    val typeName: String = "",          // 分类名称
    val flag: String,                   // 播放标识
    val episodeName: String,            // 集数名称
    val episodeIndex: Int = 0,          // 集数索引
    val totalEpisodes: Int = 0,         // 总集数
    val position: Long = 0L,            // 播放位置（毫秒）
    val duration: Long = 0L,            // 总时长（毫秒）
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val playCount: Int = 1,             // 播放次数
    val isFinished: Boolean = false,    // 是否播放完成
    val isFavorite: Boolean = false,    // 是否收藏
    val rating: Float = 0f,             // 评分
    val tags: List<String> = emptyList(), // 标签
    val notes: String = "",             // 备注
    val deviceId: String = "",          // 设备ID
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 📊 获取播放进度百分比
     */
    fun getProgressPercent(): Float {
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 🔍 是否可以续播
     */
    fun canResume(): Boolean {
        return position > 30000L && !isFinished
    }
    
    /**
     * 📊 获取历史摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "vod_id" to vodId,
            "vod_name" to vodName,
            "site_key" to siteKey,
            "episode_name" to episodeName,
            "progress_percent" to String.format("%.1f%%", getProgressPercent() * 100),
            "is_finished" to isFinished,
            "can_resume" to canResume(),
            "is_favorite" to isFavorite,
            "play_count" to playCount,
            "update_time" to updateTime
        )
    }
}

/**
 * 收藏信息
 */
@Serializable
data class FavoriteInfo(
    val id: String = "",                // 收藏ID
    val vodId: String,                  // VOD ID
    val vodName: String,                // VOD 名称
    val vodPic: String = "",            // VOD 图片
    val siteKey: String,                // 站点标识
    val siteName: String = "",          // 站点名称
    val typeId: String = "",            // 分类ID
    val typeName: String = "",          // 分类名称
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val category: String = "默认",      // 收藏分类
    val rating: Float = 0f,             // 评分
    val tags: List<String> = emptyList(), // 标签
    val notes: String = "",             // 备注
    val isWatched: Boolean = false,     // 是否已观看
    val watchProgress: Float = 0f,      // 观看进度
    val lastWatchTime: Long = 0L,       // 最后观看时间
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 📊 获取收藏摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "vod_id" to vodId,
            "vod_name" to vodName,
            "site_key" to siteKey,
            "type_name" to typeName,
            "category" to category,
            "rating" to rating,
            "is_watched" to isWatched,
            "watch_progress" to String.format("%.1f%%", watchProgress * 100),
            "tags_count" to tags.size,
            "create_time" to createTime
        )
    }
}

/**
 * 下载信息
 */
@Serializable
data class DownloadInfo(
    val id: String = "",                // 下载ID
    val vodId: String,                  // VOD ID
    val vodName: String,                // VOD 名称
    val vodPic: String = "",            // VOD 图片
    val siteKey: String,                // 站点标识
    val siteName: String = "",          // 站点名称
    val flag: String,                   // 下载标识
    val episodeName: String,            // 集数名称
    val episodeUrl: String,             // 集数地址
    val episodeIndex: Int = 0,          // 集数索引
    val localPath: String = "",         // 本地路径
    val fileName: String = "",          // 文件名
    val fileSize: Long = 0L,            // 文件大小
    val downloadedSize: Long = 0L,      // 已下载大小
    val status: DownloadStatus = DownloadStatus.PENDING, // 下载状态
    val progress: Float = 0f,           // 下载进度
    val speed: Long = 0L,               // 下载速度（字节/秒）
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val startTime: Long = 0L,           // 开始时间
    val finishTime: Long = 0L,          // 完成时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val errorMessage: String = "",      // 错误信息
    val retryCount: Int = 0,            // 重试次数
    val maxRetries: Int = 3,            // 最大重试次数
    val priority: Int = 0,              // 优先级
    val headers: Map<String, String> = emptyMap(), // 请求头
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 🔍 是否正在下载
     */
    fun isDownloading(): Boolean = status == DownloadStatus.DOWNLOADING
    
    /**
     * 🔍 是否已完成
     */
    fun isCompleted(): Boolean = status == DownloadStatus.COMPLETED
    
    /**
     * 🔍 是否失败
     */
    fun isFailed(): Boolean = status == DownloadStatus.FAILED
    
    /**
     * 🔍 是否可以重试
     */
    fun canRetry(): Boolean = isFailed() && retryCount < maxRetries
    
    /**
     * 📊 获取下载摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "vod_name" to vodName,
            "episode_name" to episodeName,
            "status" to status.name,
            "progress" to String.format("%.1f%%", progress * 100),
            "file_size_mb" to String.format("%.2f", fileSize / 1024.0 / 1024.0),
            "downloaded_size_mb" to String.format("%.2f", downloadedSize / 1024.0 / 1024.0),
            "speed_kbps" to String.format("%.1f", speed / 1024.0),
            "can_retry" to canRetry(),
            "retry_count" to retryCount,
            "create_time" to createTime
        )
    }
}

/**
 * 下载状态
 */
enum class DownloadStatus {
    PENDING,        // 等待中
    DOWNLOADING,    // 下载中
    PAUSED,         // 已暂停
    COMPLETED,      // 已完成
    FAILED,         // 失败
    CANCELLED       // 已取消
}

/**
 * 播放设置
 */
@Serializable
data class PlaySettings(
    val playSpeed: Float = 1.0f,        // 播放速度
    val volume: Float = 1.0f,           // 音量
    val brightness: Float = 0.5f,       // 亮度
    val autoNext: Boolean = true,       // 自动播放下一集
    val autoSkipOpening: Boolean = false, // 自动跳过片头
    val autoSkipEnding: Boolean = false, // 自动跳过片尾
    val subtitleEnabled: Boolean = true, // 启用字幕
    val subtitleSize: Float = 1.0f,     // 字幕大小
    val subtitleColor: String = "#FFFFFF", // 字幕颜色
    val subtitlePosition: String = "bottom", // 字幕位置
    val playerType: String = "default", // 播放器类型
    val hardwareDecoding: Boolean = true, // 硬件解码
    val backgroundPlay: Boolean = false, // 后台播放
    val gestureControl: Boolean = true, // 手势控制
    val doubleClickFullscreen: Boolean = true, // 双击全屏
    val rememberPosition: Boolean = true, // 记住播放位置
    val resumeThreshold: Long = 30000L, // 续播阈值（毫秒）
    val skipOpeningDuration: Long = 90000L, // 跳过片头时长（毫秒）
    val skipEndingDuration: Long = 60000L,  // 跳过片尾时长（毫秒）
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 📊 获取设置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "play_speed" to playSpeed,
            "volume" to volume,
            "brightness" to brightness,
            "auto_next" to autoNext,
            "auto_skip_opening" to autoSkipOpening,
            "auto_skip_ending" to autoSkipEnding,
            "subtitle_enabled" to subtitleEnabled,
            "player_type" to playerType,
            "hardware_decoding" to hardwareDecoding,
            "background_play" to backgroundPlay,
            "gesture_control" to gestureControl,
            "remember_position" to rememberPosition
        )
    }
}

/**
 * 播放统计
 */
@Serializable
data class PlayStatistics(
    val totalPlayTime: Long = 0L,       // 总播放时长（毫秒）
    val totalPlayCount: Int = 0,        // 总播放次数
    val totalVodCount: Int = 0,         // 总观看影片数
    val favoriteCount: Int = 0,         // 收藏数量
    val downloadCount: Int = 0,         // 下载数量
    val averagePlayTime: Long = 0L,     // 平均播放时长
    val mostWatchedType: String = "",   // 最常观看类型
    val mostUsedSite: String = "",      // 最常用站点
    val lastPlayTime: Long = 0L,        // 最后播放时间
    val todayPlayTime: Long = 0L,       // 今日播放时长
    val weekPlayTime: Long = 0L,        // 本周播放时长
    val monthPlayTime: Long = 0L,       // 本月播放时长
    val userId: String = ""             // 用户ID
) {
    
    /**
     * 📊 获取统计摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "total_play_time_hours" to String.format("%.1f", totalPlayTime / 1000.0 / 3600.0),
            "total_play_count" to totalPlayCount,
            "total_vod_count" to totalVodCount,
            "favorite_count" to favoriteCount,
            "download_count" to downloadCount,
            "average_play_time_minutes" to String.format("%.1f", averagePlayTime / 1000.0 / 60.0),
            "most_watched_type" to mostWatchedType,
            "most_used_site" to mostUsedSite,
            "today_play_time_hours" to String.format("%.1f", todayPlayTime / 1000.0 / 3600.0),
            "week_play_time_hours" to String.format("%.1f", weekPlayTime / 1000.0 / 3600.0),
            "month_play_time_hours" to String.format("%.1f", monthPlayTime / 1000.0 / 3600.0)
        )
    }
}
