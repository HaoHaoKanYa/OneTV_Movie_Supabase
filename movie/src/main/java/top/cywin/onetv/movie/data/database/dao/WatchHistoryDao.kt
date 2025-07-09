package top.cywin.onetv.movie.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.cywin.onetv.movie.data.database.entity.WatchHistoryEntity

/**
 * 播放历史DAO
 */
@Dao
interface WatchHistoryDao {

    /**
     * 获取所有播放历史（按更新时间倒序）
     */
    @Query("SELECT * FROM watch_history ORDER BY updateTime DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    /**
     * 获取分页播放历史
     */
    @Query("SELECT * FROM watch_history ORDER BY updateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistoryPaged(limit: Int, offset: Int): List<WatchHistoryEntity>

    /**
     * 根据站点获取播放历史
     */
    @Query("SELECT * FROM watch_history WHERE siteKey = :siteKey ORDER BY updateTime DESC")
    fun getHistoryBySite(siteKey: String): Flow<List<WatchHistoryEntity>>

    /**
     * 获取最近播放历史（24小时内）
     */
    @Query("SELECT * FROM watch_history WHERE updateTime > :since ORDER BY updateTime DESC")
    suspend fun getRecentHistory(since: Long = System.currentTimeMillis() - 24 * 60 * 60 * 1000): List<WatchHistoryEntity>

    /**
     * 根据vodId和siteKey获取播放历史
     */
    @Query("SELECT * FROM watch_history WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun getHistory(vodId: String, siteKey: String): WatchHistoryEntity?

    /**
     * 检查是否存在播放历史
     */
    @Query("SELECT COUNT(*) > 0 FROM watch_history WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun hasHistory(vodId: String, siteKey: String): Boolean

    /**
     * 插入或更新播放历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: WatchHistoryEntity): Long

    /**
     * 批量插入播放历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<WatchHistoryEntity>)

    /**
     * 更新播放进度
     */
    @Query("""
        UPDATE watch_history 
        SET position = :position, 
            duration = :duration, 
            progress = :progress, 
            updateTime = :updateTime 
        WHERE vodId = :vodId AND siteKey = :siteKey
    """)
    suspend fun updateProgress(
        vodId: String,
        siteKey: String,
        position: Long,
        duration: Long,
        progress: Float,
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * 更新剧集信息
     */
    @Query("""
        UPDATE watch_history 
        SET episodeIndex = :episodeIndex,
            episodeName = :episodeName,
            episodeUrl = :episodeUrl,
            flagName = :flagName,
            updateTime = :updateTime
        WHERE vodId = :vodId AND siteKey = :siteKey
    """)
    suspend fun updateEpisode(
        vodId: String,
        siteKey: String,
        episodeIndex: Int,
        episodeName: String,
        episodeUrl: String,
        flagName: String,
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * 删除播放历史
     */
    @Query("DELETE FROM watch_history WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun deleteHistory(vodId: String, siteKey: String)

    /**
     * 删除指定站点的所有历史
     */
    @Query("DELETE FROM watch_history WHERE siteKey = :siteKey")
    suspend fun deleteHistoryBySite(siteKey: String)

    /**
     * 清空所有播放历史
     */
    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()

    /**
     * 删除过期的播放历史（超过指定天数）
     */
    @Query("DELETE FROM watch_history WHERE updateTime < :expireTime")
    suspend fun deleteExpiredHistory(expireTime: Long)

    /**
     * 获取播放历史总数
     */
    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getHistoryCount(): Int

    /**
     * 获取指定站点的播放历史数量
     */
    @Query("SELECT COUNT(*) FROM watch_history WHERE siteKey = :siteKey")
    suspend fun getHistoryCountBySite(siteKey: String): Int

    /**
     * 搜索播放历史
     */
    @Query("SELECT * FROM watch_history WHERE vodName LIKE '%' || :keyword || '%' ORDER BY updateTime DESC")
    suspend fun searchHistory(keyword: String): List<WatchHistoryEntity>

    /**
     * 获取播放完成的历史记录
     */
    @Query("SELECT * FROM watch_history WHERE progress >= 0.95 ORDER BY updateTime DESC")
    suspend fun getCompletedHistory(): List<WatchHistoryEntity>

    /**
     * 获取未完成的历史记录
     */
    @Query("SELECT * FROM watch_history WHERE progress < 0.95 AND progress > 0 ORDER BY updateTime DESC")
    suspend fun getIncompleteHistory(): List<WatchHistoryEntity>
}
