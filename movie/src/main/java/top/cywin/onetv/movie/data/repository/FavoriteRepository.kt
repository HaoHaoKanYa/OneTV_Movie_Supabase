package top.cywin.onetv.movie.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.database.dao.FavoriteDao
import top.cywin.onetv.movie.data.database.entity.FavoriteEntity
import top.cywin.onetv.movie.data.models.VodItem
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 收藏Repository (参考OneMoVie Favorite管理)
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class FavoriteRepository(
    private val favoriteDao: FavoriteDao
) {

    /**
     * 获取所有收藏
     */
    fun getAllFavorites(): Flow<List<VodItem>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toVodItem() }
        }
    }

    /**
     * 获取分页收藏
     */
    suspend fun getFavoritesPaged(page: Int, pageSize: Int = 20): List<VodItem> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * pageSize
        favoriteDao.getFavoritesPaged(pageSize, offset).map { it.toVodItem() }
    }

    /**
     * 根据类型获取收藏
     */
    fun getFavoritesByType(type: String): Flow<List<VodItem>> {
        return favoriteDao.getFavoritesByType(type).map { entities ->
            entities.map { it.toVodItem() }
        }
    }

    /**
     * 根据站点获取收藏
     */
    fun getFavoritesBySite(siteKey: String): Flow<List<VodItem>> {
        return favoriteDao.getFavoritesBySite(siteKey).map { entities ->
            entities.map { it.toVodItem() }
        }
    }

    /**
     * 获取最近收藏
     */
    suspend fun getRecentFavorites(): List<VodItem> = withContext(Dispatchers.IO) {
        favoriteDao.getRecentFavorites().map { it.toVodItem() }
    }

    /**
     * 检查是否已收藏
     */
    suspend fun isFavorite(vodId: String, siteKey: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(vodId, siteKey)
    }

    /**
     * 添加收藏
     */
    suspend fun addFavorite(movie: VodItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val favoriteEntity = FavoriteEntity.fromVodItem(movie)
            favoriteDao.addFavorite(favoriteEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 移除收藏
     */
    suspend fun removeFavorite(vodId: String, siteKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.removeFavorite(vodId, siteKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(movie: VodItem): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val isCurrentlyFavorite = favoriteDao.isFavorite(movie.vodId, movie.siteKey)
            
            if (isCurrentlyFavorite) {
                favoriteDao.removeFavorite(movie.vodId, movie.siteKey)
                Result.success(false)
            } else {
                val favoriteEntity = FavoriteEntity.fromVodItem(movie)
                favoriteDao.addFavorite(favoriteEntity)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除指定站点的所有收藏
     */
    suspend fun removeFavoritesBySite(siteKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.removeFavoritesBySite(siteKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除指定类型的所有收藏
     */
    suspend fun removeFavoritesByType(type: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.removeFavoritesByType(type)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清空所有收藏
     */
    suspend fun clearAllFavorites(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            favoriteDao.clearAllFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 搜索收藏
     */
    suspend fun searchFavorites(keyword: String): List<VodItem> = withContext(Dispatchers.IO) {
        favoriteDao.searchFavorites(keyword).map { it.toVodItem() }
    }

    /**
     * 获取收藏统计信息
     */
    suspend fun getFavoriteStats(): FavoriteStats = withContext(Dispatchers.IO) {
        val totalCount = favoriteDao.getFavoriteCount()
        val recentCount = favoriteDao.getRecentFavorites().size
        val typeStats = favoriteDao.getFavoriteStats()
        val siteStats = favoriteDao.getAllFavoriteSites()

        FavoriteStats(
            totalCount = totalCount,
            recentCount = recentCount,
            typeStats = typeStats.associate { it.vodType to it.count },
            siteCount = siteStats.size
        )
    }

    /**
     * 获取所有收藏类型
     */
    suspend fun getAllFavoriteTypes(): List<String> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavoriteTypes()
    }

    /**
     * 获取所有收藏站点
     */
    suspend fun getAllFavoriteSites(): List<FavoriteSiteInfo> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavoriteSites().map { 
            FavoriteSiteInfo(it.siteKey, it.siteName)
        }
    }

    /**
     * 批量添加收藏
     */
    suspend fun addFavorites(movies: List<VodItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = movies.map { FavoriteEntity.fromVodItem(it) }
            favoriteDao.addFavorites(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出收藏数据
     */
    suspend fun exportFavorites(): List<VodItem> = withContext(Dispatchers.IO) {
        favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toVodItem() }
        }.let { flow ->
            // 由于这是suspend函数，我们需要收集Flow的值
            var result = emptyList<VodItem>()
            flow.collect { result = it }
            result
        }
    }

    /**
     * 导入收藏数据
     */
    suspend fun importFavorites(favorites: List<VodItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = favorites.map { FavoriteEntity.fromVodItem(it) }
            favoriteDao.addFavorites(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 同步收藏状态（用于多设备同步）
     */
    suspend fun syncFavorites(remoteFavorites: List<VodItem>): Result<SyncResult> = withContext(Dispatchers.IO) {
        try {
            val localFavorites = exportFavorites()
            val localMap = localFavorites.associateBy { "${it.vodId}_${it.siteKey}" }
            val remoteMap = remoteFavorites.associateBy { "${it.vodId}_${it.siteKey}" }

            // 找出需要添加的收藏（远程有，本地没有）
            val toAdd = remoteFavorites.filter { movie ->
                "${movie.vodId}_${movie.siteKey}" !in localMap
            }

            // 找出需要删除的收藏（本地有，远程没有）
            val toRemove = localFavorites.filter { movie ->
                "${movie.vodId}_${movie.siteKey}" !in remoteMap
            }

            // 执行同步
            if (toAdd.isNotEmpty()) {
                addFavorites(toAdd)
            }

            toRemove.forEach { movie ->
                removeFavorite(movie.vodId, movie.siteKey)
            }

            Result.success(SyncResult(toAdd.size, toRemove.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 收藏统计信息
 */
data class FavoriteStats(
    val totalCount: Int,
    val recentCount: Int,
    val typeStats: Map<String, Int>,
    val siteCount: Int
)

/**
 * 收藏站点信息
 */
data class FavoriteSiteInfo(
    val siteKey: String,
    val siteName: String
)

/**
 * 同步结果
 */
data class SyncResult(
    val addedCount: Int,
    val removedCount: Int
)
