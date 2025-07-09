package top.cywin.onetv.movie.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 收藏实体 (参考OneMoVie Favorite)
 */
@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["vodId", "siteKey"], unique = true),
        Index(value = ["createTime"]),
        Index(value = ["vodType"])
    ]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 基本信息
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val siteKey: String,
    val siteName: String = "",
    
    // 详细信息
    val vodYear: String = "",
    val vodArea: String = "",
    val vodType: String = "",
    val vodActor: String = "",
    val vodDirector: String = "",
    val vodContent: String = "",
    val vodRemarks: String = "",
    val vodScore: String = "",
    
    // 播放信息
    val vodPlayFrom: String = "",
    val vodPlayUrl: String = "",
    val totalEpisodes: Int = 0,
    
    // 时间戳
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    /**
     * 转换为VodItem模型
     */
    fun toVodItem(): top.cywin.onetv.movie.data.models.VodItem {
        return top.cywin.onetv.movie.data.models.VodItem(
            vodId = vodId,
            vodName = vodName,
            vodPic = vodPic,
            vodYear = vodYear,
            vodArea = vodArea,
            vodActor = vodActor,
            vodDirector = vodDirector,
            vodContent = vodContent,
            vodRemarks = vodRemarks,
            vodScore = vodScore,
            vodPlayFrom = vodPlayFrom,
            vodPlayUrl = vodPlayUrl,
            siteKey = siteKey
        )
    }
    
    /**
     * 是否为最近收藏（7天内）
     */
    fun isRecentlyAdded(): Boolean {
        val now = System.currentTimeMillis()
        return (now - createTime) < 7 * 24 * 60 * 60 * 1000
    }
    
    /**
     * 获取收藏时间描述
     */
    fun getCreateTimeDescription(): String {
        val now = System.currentTimeMillis()
        val diff = now - createTime
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> {
                val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(createTime))
                date
            }
        }
    }
    
    /**
     * 获取类型显示名称
     */
    fun getTypeDisplayName(): String {
        return when (vodType) {
            "1" -> "电影"
            "2" -> "电视剧"
            "3" -> "综艺"
            "4" -> "动漫"
            else -> vodType.ifEmpty { "未知" }
        }
    }
    
    /**
     * 获取演员列表
     */
    fun getActorList(): List<String> {
        return vodActor.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * 获取导演列表
     */
    fun getDirectorList(): List<String> {
        return vodDirector.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    companion object {
        /**
         * 从VodItem创建Entity
         */
        fun fromVodItem(vodItem: top.cywin.onetv.movie.data.models.VodItem): FavoriteEntity {
            return FavoriteEntity(
                vodId = vodItem.vodId,
                vodName = vodItem.vodName,
                vodPic = vodItem.vodPic,
                siteKey = vodItem.siteKey,
                vodYear = vodItem.vodYear,
                vodArea = vodItem.vodArea,
                vodType = vodItem.typeId.toString(),
                vodActor = vodItem.vodActor,
                vodDirector = vodItem.vodDirector,
                vodContent = vodItem.vodContent,
                vodRemarks = vodItem.vodRemarks,
                vodScore = vodItem.vodScore,
                vodPlayFrom = vodItem.vodPlayFrom,
                vodPlayUrl = vodItem.vodPlayUrl,
                totalEpisodes = vodItem.parseFlags().sumOf { it.createEpisodes().size }
            )
        }
    }
}
