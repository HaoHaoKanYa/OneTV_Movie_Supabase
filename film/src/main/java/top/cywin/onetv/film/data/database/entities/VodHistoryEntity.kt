package top.cywin.onetv.film.data.database.entities

import androidx.room.*

/**
 * VOD 播放历史实体
 * 
 * 基于 FongMi/TV 的播放历史设计
 * 记录用户的观看历史
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Entity(
    tableName = "vod_history",
    indices = [
        Index(value = ["vodId", "siteKey"], unique = true),
        Index(value = ["watchTime"]),
        Index(value = ["siteKey"])
    ]
)
data class VodHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "vodId") val vodId: String,
    @ColumnInfo(name = "vodName") val vodName: String,
    @ColumnInfo(name = "vodPic") val vodPic: String = "",
    @ColumnInfo(name = "vodRemarks") val vodRemarks: String = "",
    @ColumnInfo(name = "siteKey") val siteKey: String,
    @ColumnInfo(name = "siteName") val siteName: String = "",
    @ColumnInfo(name = "typeId") val typeId: String = "",
    @ColumnInfo(name = "typeName") val typeName: String = "",
    
    // 播放信息
    @ColumnInfo(name = "playFlag") val playFlag: String = "",        // 播放线路
    @ColumnInfo(name = "playIndex") val playIndex: Int = 0,          // 播放集数索引
    @ColumnInfo(name = "playPosition") val playPosition: Long = 0L,   // 播放位置 (毫秒)
    @ColumnInfo(name = "playDuration") val playDuration: Long = 0L,   // 总时长 (毫秒)
    @ColumnInfo(name = "playProgress") val playProgress: Float = 0f,  // 播放进度 (0-1)
    
    // 时间信息
    @ColumnInfo(name = "watchTime") val watchTime: Long = System.currentTimeMillis(), // 观看时间
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(), // 创建时间
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis()  // 更新时间
) {
    
    /**
     * 🔍 是否已完成观看
     */
    fun isCompleted(): Boolean = playProgress >= 0.9f
    
    /**
     * 🔍 获取播放进度百分比
     */
    fun getProgressPercent(): Int = (playProgress * 100).toInt()
    
    /**
     * 🔍 获取播放时长描述
     */
    fun getDurationDescription(): String {
        val minutes = playDuration / 60000
        return if (minutes > 0) "${minutes}分钟" else "未知"
    }
    
    /**
     * 🔍 获取播放位置描述
     */
    fun getPositionDescription(): String {
        val minutes = playPosition / 60000
        val seconds = (playPosition % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    companion object {
        
        /**
         * 🏭 创建播放历史
         */
        fun create(
            vodId: String,
            vodName: String,
            vodPic: String = "",
            siteKey: String,
            siteName: String = "",
            playFlag: String = "",
            playIndex: Int = 0,
            playPosition: Long = 0L,
            playDuration: Long = 0L
        ): VodHistoryEntity {
            val progress = if (playDuration > 0) playPosition.toFloat() / playDuration else 0f
            
            return VodHistoryEntity(
                vodId = vodId,
                vodName = vodName,
                vodPic = vodPic,
                siteKey = siteKey,
                siteName = siteName,
                playFlag = playFlag,
                playIndex = playIndex,
                playPosition = playPosition,
                playDuration = playDuration,
                playProgress = progress
            )
        }
    }
}

/**
 * VOD 收藏实体
 */
@Entity(
    tableName = "vod_favorite",
    indices = [
        Index(value = ["vodId", "siteKey"], unique = true),
        Index(value = ["favoriteTime"]),
        Index(value = ["siteKey"])
    ]
)
data class VodFavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "vodId") val vodId: String,
    @ColumnInfo(name = "vodName") val vodName: String,
    @ColumnInfo(name = "vodPic") val vodPic: String = "",
    @ColumnInfo(name = "vodRemarks") val vodRemarks: String = "",
    @ColumnInfo(name = "vodYear") val vodYear: String = "",
    @ColumnInfo(name = "vodArea") val vodArea: String = "",
    @ColumnInfo(name = "vodDirector") val vodDirector: String = "",
    @ColumnInfo(name = "vodActor") val vodActor: String = "",
    @ColumnInfo(name = "vodScore") val vodScore: String = "",
    @ColumnInfo(name = "siteKey") val siteKey: String,
    @ColumnInfo(name = "siteName") val siteName: String = "",
    @ColumnInfo(name = "typeId") val typeId: String = "",
    @ColumnInfo(name = "typeName") val typeName: String = "",
    
    // 收藏信息
    @ColumnInfo(name = "favoriteTime") val favoriteTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis()
)

/**
 * VOD 下载实体
 */
@Entity(
    tableName = "vod_download",
    indices = [
        Index(value = ["vodId", "siteKey", "episodeIndex"], unique = true),
        Index(value = ["downloadStatus"]),
        Index(value = ["createTime"])
    ]
)
data class VodDownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "vodId") val vodId: String,
    @ColumnInfo(name = "vodName") val vodName: String,
    @ColumnInfo(name = "vodPic") val vodPic: String = "",
    @ColumnInfo(name = "siteKey") val siteKey: String,
    @ColumnInfo(name = "siteName") val siteName: String = "",
    
    // 下载信息
    @ColumnInfo(name = "episodeName") val episodeName: String,
    @ColumnInfo(name = "episodeIndex") val episodeIndex: Int,
    @ColumnInfo(name = "episodeUrl") val episodeUrl: String,
    @ColumnInfo(name = "downloadUrl") val downloadUrl: String = "",
    @ColumnInfo(name = "localPath") val localPath: String = "",
    @ColumnInfo(name = "fileSize") val fileSize: Long = 0L,
    @ColumnInfo(name = "downloadedSize") val downloadedSize: Long = 0L,
    @ColumnInfo(name = "downloadProgress") val downloadProgress: Float = 0f,
    @ColumnInfo(name = "downloadStatus") val downloadStatus: Int = 0, // 0=待下载, 1=下载中, 2=已完成, 3=失败, 4=暂停
    @ColumnInfo(name = "downloadSpeed") val downloadSpeed: Long = 0L,
    @ColumnInfo(name = "errorMessage") val errorMessage: String = "",
    
    // 时间信息
    @ColumnInfo(name = "createTime") val createTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updateTime") val updateTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "startTime") val startTime: Long = 0L,
    @ColumnInfo(name = "finishTime") val finishTime: Long = 0L
) {
    
    /**
     * 下载状态枚举
     */
    object Status {
        const val PENDING = 0    // 待下载
        const val DOWNLOADING = 1 // 下载中
        const val COMPLETED = 2   // 已完成
        const val FAILED = 3      // 失败
        const val PAUSED = 4      // 暂停
    }
    
    /**
     * 🔍 是否正在下载
     */
    fun isDownloading(): Boolean = downloadStatus == Status.DOWNLOADING
    
    /**
     * 🔍 是否已完成
     */
    fun isCompleted(): Boolean = downloadStatus == Status.COMPLETED
    
    /**
     * 🔍 是否失败
     */
    fun isFailed(): Boolean = downloadStatus == Status.FAILED
    
    /**
     * 🔍 获取下载进度百分比
     */
    fun getProgressPercent(): Int = (downloadProgress * 100).toInt()
    
    /**
     * 🔍 获取文件大小描述
     */
    fun getFileSizeDescription(): String {
        return formatFileSize(fileSize)
    }
    
    /**
     * 🔍 获取下载速度描述
     */
    fun getSpeedDescription(): String {
        return if (downloadSpeed > 0) "${formatFileSize(downloadSpeed)}/s" else ""
    }
    
    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }
}
