package top.cywin.onetv.movie.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 缓存数据实体 (参考OneMoVie设计)
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
    companion object {
        /**
         * 创建缓存数据实体
         */
        fun create(cacheKey: String, data: String, dataType: String, expireTime: Long): CacheDataEntity {
            return CacheDataEntity(
                cacheKey = cacheKey,
                data = data,
                dataType = dataType,
                expireTime = expireTime,
                createTime = System.currentTimeMillis(),
                accessCount = 0,
                lastAccessTime = System.currentTimeMillis()
            )
        }
    }

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
