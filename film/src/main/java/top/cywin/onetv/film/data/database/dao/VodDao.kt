package top.cywin.onetv.film.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import top.cywin.onetv.film.data.database.entities.VodEntity

/**
 * VOD 数据访问对象
 * 
 * 基于 FongMi/TV 的 DAO 设计
 * 提供 VOD 数据的 CRUD 操作
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Dao
interface VodDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 🔍 根据 ID 查询 VOD
     */
    @Query("SELECT * FROM vod WHERE vodId = :vodId AND siteKey = :siteKey LIMIT 1")
    suspend fun getVod(vodId: String, siteKey: String): VodEntity?
    
    /**
     * 🔍 根据 ID 查询 VOD (Flow)
     */
    @Query("SELECT * FROM vod WHERE vodId = :vodId AND siteKey = :siteKey LIMIT 1")
    fun getVodFlow(vodId: String, siteKey: String): Flow<VodEntity?>
    
    /**
     * 🔍 根据站点查询所有 VOD
     */
    @Query("SELECT * FROM vod WHERE siteKey = :siteKey ORDER BY updateTime DESC")
    suspend fun getVodsBySite(siteKey: String): List<VodEntity>
    
    /**
     * 🔍 根据分类查询 VOD
     */
    @Query("SELECT * FROM vod WHERE siteKey = :siteKey AND typeId = :typeId ORDER BY updateTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getVodsByCategory(siteKey: String, typeId: String, limit: Int, offset: Int): List<VodEntity>
    
    /**
     * 🔍 搜索 VOD
     */
    @Query("SELECT * FROM vod WHERE vodName LIKE '%' || :keyword || '%' ORDER BY updateTime DESC LIMIT :limit")
    suspend fun searchVods(keyword: String, limit: Int = 50): List<VodEntity>
    
    /**
     * 🔍 根据站点搜索 VOD
     */
    @Query("SELECT * FROM vod WHERE siteKey = :siteKey AND vodName LIKE '%' || :keyword || '%' ORDER BY updateTime DESC LIMIT :limit")
    suspend fun searchVodsBySite(siteKey: String, keyword: String, limit: Int = 50): List<VodEntity>
    
    /**
     * 🔍 获取最近更新的 VOD
     */
    @Query("SELECT * FROM vod ORDER BY updateTime DESC LIMIT :limit")
    suspend fun getRecentVods(limit: Int = 20): List<VodEntity>
    
    /**
     * 🔍 获取最近更新的 VOD (Flow)
     */
    @Query("SELECT * FROM vod ORDER BY updateTime DESC LIMIT :limit")
    fun getRecentVodsFlow(limit: Int = 20): Flow<List<VodEntity>>
    
    /**
     * 🔍 根据年份查询 VOD
     */
    @Query("SELECT * FROM vod WHERE vodYear = :year ORDER BY updateTime DESC LIMIT :limit")
    suspend fun getVodsByYear(year: String, limit: Int = 50): List<VodEntity>
    
    /**
     * 🔍 根据地区查询 VOD
     */
    @Query("SELECT * FROM vod WHERE vodArea = :area ORDER BY updateTime DESC LIMIT :limit")
    suspend fun getVodsByArea(area: String, limit: Int = 50): List<VodEntity>
    
    /**
     * 🔍 根据评分查询 VOD
     */
    @Query("SELECT * FROM vod WHERE CAST(vodScore AS REAL) >= :minScore ORDER BY CAST(vodScore AS REAL) DESC LIMIT :limit")
    suspend fun getVodsByScore(minScore: Float, limit: Int = 50): List<VodEntity>
    
    /**
     * 🔍 检查 VOD 是否存在
     */
    @Query("SELECT COUNT(*) FROM vod WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun exists(vodId: String, siteKey: String): Int
    
    /**
     * 🔍 获取 VOD 总数
     */
    @Query("SELECT COUNT(*) FROM vod")
    suspend fun getCount(): Int
    
    /**
     * 🔍 根据站点获取 VOD 总数
     */
    @Query("SELECT COUNT(*) FROM vod WHERE siteKey = :siteKey")
    suspend fun getCountBySite(siteKey: String): Int
    
    // ==================== 插入操作 ====================
    
    /**
     * ➕ 插入 VOD
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vod: VodEntity): Long
    
    /**
     * ➕ 批量插入 VOD
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vods: List<VodEntity>): List<Long>
    
    // ==================== 更新操作 ====================
    
    /**
     * 🔄 更新 VOD
     */
    @Update
    suspend fun update(vod: VodEntity): Int
    
    /**
     * 🔄 批量更新 VOD
     */
    @Update
    suspend fun updateAll(vods: List<VodEntity>): Int
    
    /**
     * 🔄 更新 VOD 播放信息
     */
    @Query("UPDATE vod SET vodPlayFrom = :playFrom, vodPlayUrl = :playUrl, updateTime = :updateTime WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun updatePlayInfo(vodId: String, siteKey: String, playFrom: String, playUrl: String, updateTime: Long = System.currentTimeMillis()): Int
    
    /**
     * 🔄 更新 VOD 内容
     */
    @Query("UPDATE vod SET vodContent = :content, updateTime = :updateTime WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun updateContent(vodId: String, siteKey: String, content: String, updateTime: Long = System.currentTimeMillis()): Int
    
    // ==================== 删除操作 ====================
    
    /**
     * ❌ 删除 VOD
     */
    @Delete
    suspend fun delete(vod: VodEntity): Int
    
    /**
     * ❌ 根据 ID 删除 VOD
     */
    @Query("DELETE FROM vod WHERE vodId = :vodId AND siteKey = :siteKey")
    suspend fun deleteById(vodId: String, siteKey: String): Int
    
    /**
     * ❌ 根据站点删除所有 VOD
     */
    @Query("DELETE FROM vod WHERE siteKey = :siteKey")
    suspend fun deleteBySite(siteKey: String): Int
    
    /**
     * ❌ 删除过期的 VOD (超过指定天数)
     */
    @Query("DELETE FROM vod WHERE updateTime < :expireTime")
    suspend fun deleteExpired(expireTime: Long): Int
    
    /**
     * ❌ 清空所有 VOD
     */
    @Query("DELETE FROM vod")
    suspend fun deleteAll(): Int
    
    // ==================== 统计操作 ====================
    
    /**
     * 📊 获取站点统计信息
     */
    @Query("SELECT siteKey, COUNT(*) as count FROM vod GROUP BY siteKey")
    suspend fun getSiteStats(): List<SiteStats>
    
    /**
     * 📊 获取分类统计信息
     */
    @Query("SELECT typeId, typeName, COUNT(*) as count FROM vod WHERE siteKey = :siteKey GROUP BY typeId, typeName")
    suspend fun getCategoryStats(siteKey: String): List<CategoryStats>
    
    /**
     * 📊 获取年份统计信息
     */
    @Query("SELECT vodYear, COUNT(*) as count FROM vod WHERE vodYear != '' GROUP BY vodYear ORDER BY vodYear DESC")
    suspend fun getYearStats(): List<YearStats>
    
    /**
     * 📊 获取地区统计信息
     */
    @Query("SELECT vodArea, COUNT(*) as count FROM vod WHERE vodArea != '' GROUP BY vodArea ORDER BY count DESC")
    suspend fun getAreaStats(): List<AreaStats>
}

/**
 * 站点统计数据
 */
data class SiteStats(
    val siteKey: String,
    val count: Int
)

/**
 * 分类统计数据
 */
data class CategoryStats(
    val typeId: String,
    val typeName: String,
    val count: Int
)

/**
 * 年份统计数据
 */
data class YearStats(
    val vodYear: String,
    val count: Int
)

/**
 * 地区统计数据
 */
data class AreaStats(
    val vodArea: String,
    val count: Int
)
