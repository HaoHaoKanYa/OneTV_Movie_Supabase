package top.cywin.onetv.movie.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.cywin.onetv.movie.data.database.entity.SearchHistoryEntity
import top.cywin.onetv.movie.data.database.entity.CacheDataEntity

/**
 * 搜索历史DAO
 */
@Dao
interface SearchHistoryDao {

    /**
     * 获取所有搜索历史（按搜索时间倒序）
     */
    @Query("SELECT * FROM search_history ORDER BY searchTime DESC")
    fun getAllSearchHistory(): Flow<List<SearchHistoryEntity>>

    /**
     * 获取最近搜索历史（限制数量）
     */
    @Query("SELECT * FROM search_history ORDER BY searchTime DESC LIMIT :limit")
    suspend fun getRecentSearchHistory(limit: Int = 20): List<SearchHistoryEntity>

    /**
     * 获取热门搜索关键词
     */
    @Query("SELECT * FROM search_history WHERE searchCount >= 3 ORDER BY searchCount DESC, searchTime DESC LIMIT :limit")
    suspend fun getHotKeywords(limit: Int = 10): List<SearchHistoryEntity>

    /**
     * 根据关键词获取搜索历史
     */
    @Query("SELECT * FROM search_history WHERE keyword = :keyword")
    suspend fun getSearchHistory(keyword: String): SearchHistoryEntity?

    /**
     * 搜索关键词（模糊匹配）
     */
    @Query("SELECT * FROM search_history WHERE keyword LIKE '%' || :query || '%' ORDER BY searchCount DESC, searchTime DESC LIMIT :limit")
    suspend fun searchKeywords(query: String, limit: Int = 5): List<SearchHistoryEntity>

    /**
     * 添加或更新搜索历史
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSearchHistory(history: SearchHistoryEntity): Long

    /**
     * 更新搜索次数
     */
    @Query("UPDATE search_history SET searchCount = searchCount + 1, searchTime = :searchTime WHERE keyword = :keyword")
    suspend fun updateSearchCount(keyword: String, searchTime: Long = System.currentTimeMillis())

    /**
     * 删除搜索历史
     */
    @Query("DELETE FROM search_history WHERE keyword = :keyword")
    suspend fun deleteSearchHistory(keyword: String)

    /**
     * 清空所有搜索历史
     */
    @Query("DELETE FROM search_history")
    suspend fun clearAllSearchHistory()

    /**
     * 删除过期的搜索历史（超过指定天数）
     */
    @Query("DELETE FROM search_history WHERE searchTime < :expireTime")
    suspend fun deleteExpiredSearchHistory(expireTime: Long)

    /**
     * 获取搜索历史总数
     */
    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getSearchHistoryCount(): Int
}
