package top.cywin.onetv.movie.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.cywin.onetv.movie.data.database.entity.FavoriteEntity

/**
 * 收藏DAO
 */
@Dao
interface FavoriteDao {

    /**
     * 获取所有收藏（按创建时间倒序）
     */
    @Query("SELECT * FROM favorites ORDER BY createTime DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    /**
     * 获取分页收藏
     */
    @Query("SELECT * FROM favorites ORDER BY createTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getFavoritesPaged(limit: Int, offset: Int): List<FavoriteEntity>

    /**
     * 根据类型获取收藏
     */
    @Query("SELECT * FROM favorites WHERE vodType = :type ORDER BY createTime DESC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>

    /**
     * 根据站点获取收藏
     */
    @Query("SELECT * FROM favorites WHERE siteKey = :siteKey ORDER BY createTime DESC")
    fun getFavoritesBySite(siteKey: String): Flow<List<FavoriteEntity>>

    /**
     * 获取最近收藏（7天内）
     */
    @Query("SELECT * FROM favorites WHERE createTime > :since ORDER BY createTime DESC")
    suspend fun getRecentFavorites(since: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000): List<FavoriteEntity>

    /**
     * 根据vodId和siteKey获取收藏
     */
    @Query("SELECT * FROM favorites WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun getFavorite(vodId: String, siteKey: String): FavoriteEntity?

    /**
     * 检查是否已收藏
     */
    @Query("SELECT COUNT(*) > 0 FROM favorites WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun isFavorite(vodId: String, siteKey: String): Boolean

    /**
     * 添加收藏
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity): Long

    /**
     * 批量添加收藏
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(favorites: List<FavoriteEntity>)

    /**
     * 更新收藏信息
     */
    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    /**
     * 删除收藏
     */
    @Query("DELETE FROM favorites WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun removeFavorite(vodId: String, siteKey: String)

    /**
     * 删除指定站点的所有收藏
     */
    @Query("DELETE FROM favorites WHERE siteKey = :siteKey")
    suspend fun removeFavoritesBySite(siteKey: String)

    /**
     * 删除指定类型的所有收藏
     */
    @Query("DELETE FROM favorites WHERE vodType = :type")
    suspend fun removeFavoritesByType(type: String)

    /**
     * 清空所有收藏
     */
    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    /**
     * 获取收藏总数
     */
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int

    /**
     * 获取指定类型的收藏数量
     */
    @Query("SELECT COUNT(*) FROM favorites WHERE vodType = :type")
    suspend fun getFavoriteCountByType(type: String): Int

    /**
     * 获取指定站点的收藏数量
     */
    @Query("SELECT COUNT(*) FROM favorites WHERE siteKey = :siteKey")
    suspend fun getFavoriteCountBySite(siteKey: String): Int

    /**
     * 搜索收藏
     */
    @Query("""
        SELECT * FROM favorites 
        WHERE vodName LIKE '%' || :keyword || '%' 
           OR vodActor LIKE '%' || :keyword || '%' 
           OR vodDirector LIKE '%' || :keyword || '%'
        ORDER BY createTime DESC
    """)
    suspend fun searchFavorites(keyword: String): List<FavoriteEntity>

    /**
     * 获取收藏统计信息
     */
    @Query("""
        SELECT vodType, COUNT(*) as count 
        FROM favorites 
        GROUP BY vodType 
        ORDER BY count DESC
    """)
    suspend fun getFavoriteStats(): List<FavoriteStats>

    /**
     * 获取收藏的所有类型
     */
    @Query("SELECT DISTINCT vodType FROM favorites ORDER BY vodType")
    suspend fun getAllFavoriteTypes(): List<String>

    /**
     * 获取收藏的所有站点
     */
    @Query("SELECT DISTINCT siteKey, siteName FROM favorites ORDER BY siteName")
    suspend fun getAllFavoriteSites(): List<FavoriteSite>
}

/**
 * 收藏统计数据类
 */
data class FavoriteStats(
    val vodType: String,
    val count: Int
)

/**
 * 收藏站点数据类
 */
data class FavoriteSite(
    val siteKey: String,
    val siteName: String
)
