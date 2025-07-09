package top.cywin.onetv.movie.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 搜索历史实体
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["keyword"], unique = true),
        Index(value = ["searchTime"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val keyword: String,
    val searchTime: Long = System.currentTimeMillis(),
    val searchCount: Int = 1, // 搜索次数
    val resultCount: Int = 0  // 搜索结果数量
) {
    /**
     * 是否为最近搜索（7天内）
     */
    fun isRecent(): Boolean {
        val now = System.currentTimeMillis()
        return (now - searchTime) < 7 * 24 * 60 * 60 * 1000
    }
    
    /**
     * 获取搜索时间描述
     */
    fun getSearchTimeDescription(): String {
        val now = System.currentTimeMillis()
        val diff = now - searchTime
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> {
                val date = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(searchTime))
                date
            }
        }
    }
    
    /**
     * 是否为热门搜索（搜索次数>=3）
     */
    fun isHot(): Boolean {
        return searchCount >= 3
    }
}

/**
 * 缓存数据实体
 */
@Entity(
    tableName = "cache_data",
    indices = [
        Index(value = ["cacheKey"], unique = true),
        Index(value = ["expireTime"])
    ]
)
data class CacheDataEntity(
    @PrimaryKey
    val cacheKey: String,
    
    val data: String, // JSON数据
    val dataType: String, // 数据类型
    val createTime: Long = System.currentTimeMillis(),
    val expireTime: Long, // 过期时间
    val accessCount: Int = 0, // 访问次数
    val lastAccessTime: Long = System.currentTimeMillis()
) {
    /**
     * 是否已过期
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expireTime
    }
    
    /**
     * 是否为热门缓存（访问次数>=5）
     */
    fun isHot(): Boolean {
        return accessCount >= 5
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getSize(): Long {
        return data.toByteArray().size.toLong()
    }
}
