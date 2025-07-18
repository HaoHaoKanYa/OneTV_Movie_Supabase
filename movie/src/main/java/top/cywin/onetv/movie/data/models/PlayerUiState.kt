package top.cywin.onetv.movie.data.models

// import top.cywin.onetv.movie.data.parser.LineManager

/**
 * 线路信息 (替代LineManager.LineInfo)
 */
data class LineInfo(
    val flag: String = "",
    val quality: String = "",
    val speed: String = "",
    val isAvailable: Boolean = true
)

/**
 * 播放器UI状态 - 支持线路切换和完整播放功能
 */
data class PlayerUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // 影片信息
    val movie: VodItem? = null,
    
    // 播放源信息
    val playFlags: List<VodFlag> = emptyList(),
    val currentFlag: VodFlag? = null,
    
    // 剧集信息
    val episodes: List<VodEpisode> = emptyList(),
    val currentEpisode: VodEpisode? = null,
    val currentEpisodeIndex: Int = 0,
    
    // 线路管理
    val availableLines: List<LineInfo> = emptyList(),
    val currentLineIndex: Int = 0,
    
    // 播放信息
    val playUrl: String = "",
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    
    // 播放器控制
    val showControls: Boolean = true,
    val volume: Float = 1.0f,
    val playbackSpeed: Float = 1.0f,
    
    // 字幕信息
    val subtitles: List<SubtitleTrack> = emptyList(),
    val currentSubtitleIndex: Int = -1,
    
    // 音轨信息
    val audioTracks: List<AudioTrack> = emptyList(),
    val currentAudioTrackIndex: Int = 0
) {
    /**
     * 获取播放进度百分比
     */
    fun getProgressPercentage(): Float {
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 获取缓冲进度百分比
     */
    fun getBufferedPercentage(): Float {
        return if (duration > 0) {
            (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * 是否有上一集
     */
    fun hasPreviousEpisode(): Boolean {
        return currentEpisodeIndex > 0
    }
    
    /**
     * 是否有下一集
     */
    fun hasNextEpisode(): Boolean {
        return currentEpisodeIndex < episodes.size - 1
    }
    
    /**
     * 获取当前线路信息
     */
    fun getCurrentLine(): LineInfo? {
        return availableLines.getOrNull(currentLineIndex)
    }
    
    /**
     * 格式化播放时间
     */
    fun getFormattedPosition(): String {
        return formatTime(position)
    }
    
    /**
     * 格式化总时长
     */
    fun getFormattedDuration(): String {
        return formatTime(duration)
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

/**
 * 字幕轨道信息
 */
data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
    val url: String = "",
    val isDefault: Boolean = false
)

/**
 * 音轨信息
 */
data class AudioTrack(
    val id: String,
    val language: String,
    val label: String,
    val isDefault: Boolean = false
)

/**
 * 播放器事件
 */
sealed class PlayerEvent {
    object Play : PlayerEvent()
    object Pause : PlayerEvent()
    object Stop : PlayerEvent()
    data class Seek(val position: Long) : PlayerEvent()
    data class SetVolume(val volume: Float) : PlayerEvent()
    data class SetPlaybackSpeed(val speed: Float) : PlayerEvent()
    data class SelectSubtitle(val trackIndex: Int) : PlayerEvent()
    data class SelectAudioTrack(val trackIndex: Int) : PlayerEvent()
    data class SwitchLine(val lineIndex: Int) : PlayerEvent()
    data class SwitchEpisode(val episodeIndex: Int) : PlayerEvent()
    object ShowControls : PlayerEvent()
    object HideControls : PlayerEvent()
    object ToggleControls : PlayerEvent()
}

/**
 * 播放器错误类型
 */
sealed class PlayerError(val message: String) {
    object NetworkError : PlayerError("网络连接失败")
    object ParseError : PlayerError("播放地址解析失败")
    object UnsupportedFormat : PlayerError("不支持的视频格式")
    object PermissionDenied : PlayerError("权限不足")
    data class UnknownError(val errorMessage: String) : PlayerError(errorMessage)
}

/**
 * 播放质量枚举
 */
enum class PlaybackQuality(val displayName: String, val value: String) {
    AUTO("自动", "auto"),
    LOW("流畅", "low"),
    MEDIUM("标清", "medium"),
    HIGH("高清", "high"),
    ULTRA("超清", "ultra")
}

/**
 * 播放器状态枚举
 */
enum class PlayerState {
    IDLE,           // 空闲
    PREPARING,      // 准备中
    READY,          // 准备就绪
    BUFFERING,      // 缓冲中
    PLAYING,        // 播放中
    PAUSED,         // 暂停
    ENDED,          // 播放结束
    ERROR           // 错误
}
