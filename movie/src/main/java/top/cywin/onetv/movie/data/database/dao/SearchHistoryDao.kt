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

/**
 * 缓存数据DAO
 */
@Dao
interface CacheDataDao {

    /**
     * 根据缓存键获取缓存数据
     */
    @Query("SELECT * FROM cache_data WHERE cacheKey = :cacheKey")
    suspend fun getCacheData(cacheKey: String): CacheDataEntity?

    /**
     * 获取所有未过期的缓存
     */
    @Query("SELECT * FROM cache_data WHERE expireTime > :currentTime ORDER BY lastAccessTime DESC")
    suspend fun getAllValidCache(currentTime: Long = System.currentTimeMillis()): List<CacheDataEntity>

    /**
     * 获取过期的缓存
     */
    @Query("SELECT * FROM cache_data WHERE expireTime <= :currentTime")
    suspend fun getExpiredCache(currentTime: Long = System.currentTimeMillis()): List<CacheDataEntity>

    /**
     * 获取热门缓存（访问次数多的）
     */
    @Query("SELECT * FROM cache_data WHERE accessCount >= 5 ORDER BY accessCount DESC, lastAccessTime DESC")
    suspend fun getHotCache(): List<CacheDataEntity>

    /**
     * 根据数据类型获取缓存
     */
    @Query("SELECT * FROM cache_data WHERE dataType = :dataType AND expireTime > :currentTime ORDER BY lastAccessTime DESC")
    suspend fun getCacheByType(dataType: String, currentTime: Long = System.currentTimeMillis()): List<CacheDataEntity>

    /**
     * 插入或更新缓存数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCache(cache: CacheDataEntity)

    /**
     * 更新访问信息
     */
    @Query("UPDATE cache_data SET accessCount = accessCount + 1, lastAccessTime = :accessTime WHERE cacheKey = :cacheKey")
    suspend fun updateAccessInfo(cacheKey: String, accessTime: Long = System.currentTimeMillis())

    /**
     * 删除缓存
     */
    @Query("DELETE FROM cache_data WHERE cacheKey = :cacheKey")
    suspend fun deleteCache(cacheKey: String)

    /**
     * 删除指定类型的缓存
     */
    @Query("DELETE FROM cache_data WHERE dataType = :dataType")
    suspend fun deleteCacheByType(dataType: String)

    /**
     * 删除过期的缓存
     */
    @Query("DELETE FROM cache_data WHERE expireTime <= :currentTime")
    suspend fun deleteExpiredCache(currentTime: Long = System.currentTimeMillis())

    /**
     * 清空所有缓存
     */
    @Query("DELETE FROM cache_data")
    suspend fun clearAllCache()

    /**
     * 获取缓存总大小
     */
    @Query("SELECT SUM(LENGTH(data)) FROM cache_data")
    suspend fun getTotalCacheSize(): Long?

    /**
     * 获取缓存数量
     */
    @Query("SELECT COUNT(*) FROM cache_data")
    suspend fun getCacheCount(): Int

    /**
     * 获取指定类型的缓存数量
     */
    @Query("SELECT COUNT(*) FROM cache_data WHERE dataType = :dataType")
    suspend fun getCacheCountByType(dataType: String): Int

    /**
     * 获取缓存统计信息
     */
    @Query("""
        SELECT dataType, COUNT(*) as count, SUM(LENGTH(data)) as totalSize 
        FROM cache_data 
        GROUP BY dataType 
        ORDER BY totalSize DESC
    """)
    suspend fun getCacheStats(): List<CacheStats>
}

/**
 * 缓存统计数据类
 */
data class CacheStats(
    val dataType: String,
    val count: Int,
    val totalSize: Long
)
