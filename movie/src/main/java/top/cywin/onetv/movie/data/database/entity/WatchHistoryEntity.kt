package top.cywin.onetv.movie.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 播放历史实体 (参考OneMoVie History)
 */
@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["vodId", "siteKey"], unique = true),
        Index(value = ["updateTime"])
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val siteKey: String,
    val siteName: String = "",
    
    // 播放信息
    val episodeIndex: Int = 0,
    val episodeName: String = "",
    val episodeUrl: String = "",
    val flagName: String = "",
    
    // 播放进度
    val position: Long = 0L, // 播放位置(毫秒)
    val duration: Long = 0L, // 总时长(毫秒)
    val progress: Float = 0f, // 播放进度百分比
    
    // 时间戳
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    
    // 额外信息
    val vodYear: String = "",
    val vodArea: String = "",
    val vodType: String = "",
    val vodRemarks: String = ""
) {
    /**
     * 转换为VodHistory模型
     */
    fun toVodHistory(): top.cywin.onetv.movie.data.models.VodHistory {
        return top.cywin.onetv.movie.data.models.VodHistory(
            vodId = vodId,
            vodName = vodName,
            vodPic = vodPic,
            siteKey = siteKey,
            episodeIndex = episodeIndex,
            episodeName = episodeName,
            position = position,
            duration = duration,
            createTime = createTime,
            updateTime = updateTime
        )
    }
    
    /**
     * 是否已播放完成
     */
    fun isCompleted(): Boolean {
        return progress >= 0.95f // 播放95%以上认为完成
    }
    
    /**
     * 是否为最近观看（24小时内）
     */
    fun isRecent(): Boolean {
        val now = System.currentTimeMillis()
        return (now - updateTime) < 24 * 60 * 60 * 1000
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
    
    companion object {
        /**
         * 从VodHistory创建Entity
         */
        fun fromVodHistory(history: top.cywin.onetv.movie.data.models.VodHistory): WatchHistoryEntity {
            return WatchHistoryEntity(
                vodId = history.vodId,
                vodName = history.vodName,
                vodPic = history.vodPic,
                siteKey = history.siteKey,
                episodeIndex = history.episodeIndex,
                episodeName = history.episodeName,
                position = history.position,
                duration = history.duration,
                progress = if (history.duration > 0) {
                    (history.position.toFloat() / history.duration.toFloat()).coerceIn(0f, 1f)
                } else 0f,
                createTime = history.createTime,
                updateTime = history.updateTime
            )
        }
    }
}
