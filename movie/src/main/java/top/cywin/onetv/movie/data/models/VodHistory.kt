package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 观看历史 (参考OneMoVie History)
 */
@Serializable
data class VodHistory(
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val siteKey: String, // 站点标识
    val episodeIndex: Int = 0, // 当前集数
    val episodeName: String = "", // 当前集名称
    val position: Long = 0L, // 播放位置(毫秒)
    val duration: Long = 0L, // 总时长(毫秒)
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取播放进度百分比
     */
    fun getProgressPercent(): Float {
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat() * 100).coerceIn(0f, 100f)
        } else {
            0f
        }
    }
    
    /**
     * 是否已播放完成
     */
    fun isCompleted(): Boolean {
        return getProgressPercent() >= 95f // 播放95%以上认为完成
    }
    
    /**
     * 获取播放时长文本
     */
    fun getPositionText(): String {
        return formatTime(position)
    }
    
    /**
     * 获取总时长文本
     */
    fun getDurationText(): String {
        return formatTime(duration)
    }
    
    /**
     * 获取播放进度文本
     */
    fun getProgressText(): String {
        return "${getPositionText()} / ${getDurationText()}"
    }
    
    /**
     * 格式化时间
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
    
    /**
     * 获取显示的剧集信息
     */
    fun getEpisodeDisplayText(): String {
        return if (episodeName.isNotEmpty()) {
            episodeName
        } else {
            "第${episodeIndex + 1}集"
        }
    }
    
    /**
     * 是否为最近观看（24小时内）
     */
    fun isRecent(): Boolean {
        val now = System.currentTimeMillis()
        return (now - updateTime) < 24 * 60 * 60 * 1000 // 24小时
    }
    
    /**
     * 获取观看时间描述
     */
    fun getWatchTimeDescription(): String {
        val now = System.currentTimeMillis()
        val diff = now - updateTime
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> {
                val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(updateTime))
                date
            }
        }
    }
}
