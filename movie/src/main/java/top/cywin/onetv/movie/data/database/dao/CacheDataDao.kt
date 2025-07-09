package top.cywin.onetv.movie.data.database.dao

import androidx.room.*
import top.cywin.onetv.movie.data.database.entity.CacheDataEntity

/**
 * 缓存数据DAO (参考OneMoVie设计)
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
}
