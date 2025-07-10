package top.cywin.onetv.movie.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.database.dao.WatchHistoryDao
import top.cywin.onetv.movie.data.database.entity.WatchHistoryEntity
import top.cywin.onetv.movie.data.models.VodHistory
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodFlag
import top.cywin.onetv.movie.data.models.VodEpisode
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 播放历史Repository (参考OneMoVie History管理)
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class WatchHistoryRepository(
    private val watchHistoryDao: WatchHistoryDao
) {

    /**
     * 获取所有播放历史
     */
    fun getAllHistory(): Flow<List<VodHistory>> {
        return watchHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toVodHistory() }
        }
    }

    /**
     * 获取分页播放历史
     */
    suspend fun getHistoryPaged(page: Int, pageSize: Int = 20): List<VodHistory> = withContext(Dispatchers.IO) {
        val offset = (page - 1) * pageSize
        watchHistoryDao.getHistoryPaged(pageSize, offset).map { it.toVodHistory() }
    }

    /**
     * 根据站点获取播放历史
     */
    fun getHistoryBySite(siteKey: String): Flow<List<VodHistory>> {
        return watchHistoryDao.getHistoryBySite(siteKey).map { entities ->
            entities.map { it.toVodHistory() }
        }
    }

    /**
     * 获取最近播放历史
     */
    suspend fun getRecentHistory(): List<VodHistory> = withContext(Dispatchers.IO) {
        watchHistoryDao.getRecentHistory().map { it.toVodHistory() }
    }

    /**
     * 获取指定影片的播放历史
     */
    suspend fun getMovieHistory(vodId: String, siteKey: String): VodHistory? = withContext(Dispatchers.IO) {
        watchHistoryDao.getHistory(vodId, siteKey)?.toVodHistory()
    }

    /**
     * 检查是否有播放历史
     */
    suspend fun hasHistory(vodId: String, siteKey: String): Boolean = withContext(Dispatchers.IO) {
        watchHistoryDao.hasHistory(vodId, siteKey)
    }

    /**
     * 保存播放历史
     */
    suspend fun saveHistory(
        movie: VodItem,
        flag: VodFlag,
        episode: VodEpisode,
        position: Long = 0L,
        duration: Long = 0L
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val progress = if (duration > 0) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            val historyEntity = WatchHistoryEntity(
                vodId = movie.vodId,
                vodName = movie.vodName,
                vodPic = movie.vodPic,
                siteKey = movie.siteKey,
                siteName = "", // TODO: 从配置获取站点名称
                episodeIndex = episode.index,
                episodeName = episode.name,
                episodeUrl = episode.url,
                flagName = flag.flag,
                position = position,
                duration = duration,
                progress = progress,
                vodYear = movie.vodYear,
                vodArea = movie.vodArea,
                vodType = movie.typeId.toString(),
                vodRemarks = movie.vodRemarks
            )

            watchHistoryDao.insertOrUpdate(historyEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新播放进度
     */
    suspend fun updateProgress(
        vodId: String,
        siteKey: String,
        position: Long,
        duration: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val progress = if (duration > 0) {
                (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            watchHistoryDao.updateProgress(vodId, siteKey, position, duration, progress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新剧集信息
     */
    suspend fun updateEpisode(
        vodId: String,
        siteKey: String,
        episode: VodEpisode,
        flagName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            watchHistoryDao.updateEpisode(
                vodId = vodId,
                siteKey = siteKey,
                episodeIndex = episode.index,
                episodeName = episode.name,
                episodeUrl = episode.url,
                flagName = flagName
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除播放历史
     */
    suspend fun deleteHistory(vodId: String, siteKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            watchHistoryDao.deleteHistory(vodId, siteKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除指定站点的所有历史
     */
    suspend fun deleteHistoryBySite(siteKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            watchHistoryDao.deleteHistoryBySite(siteKey)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清空所有播放历史
     */
    suspend fun clearAllHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            watchHistoryDao.clearAllHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清理过期历史（超过指定天数）
     */
    suspend fun cleanupExpiredHistory(days: Int = 30): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val expireTime = System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L
            watchHistoryDao.deleteExpiredHistory(expireTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 搜索播放历史
     */
    suspend fun searchHistory(keyword: String): List<VodHistory> = withContext(Dispatchers.IO) {
        watchHistoryDao.searchHistory(keyword).map { it.toVodHistory() }
    }

    /**
     * 获取播放完成的历史记录
     */
    suspend fun getCompletedHistory(): List<VodHistory> = withContext(Dispatchers.IO) {
        watchHistoryDao.getCompletedHistory().map { it.toVodHistory() }
    }

    /**
     * 获取未完成的历史记录
     */
    suspend fun getIncompleteHistory(): List<VodHistory> = withContext(Dispatchers.IO) {
        watchHistoryDao.getIncompleteHistory().map { it.toVodHistory() }
    }

    /**
     * 获取播放历史统计信息
     */
    suspend fun getHistoryStats(): HistoryStats = withContext(Dispatchers.IO) {
        val totalCount = watchHistoryDao.getHistoryCount()
        val recentCount = watchHistoryDao.getRecentHistory().size
        val completedCount = watchHistoryDao.getCompletedHistory().size
        val incompleteCount = watchHistoryDao.getIncompleteHistory().size

        HistoryStats(
            totalCount = totalCount,
            recentCount = recentCount,
            completedCount = completedCount,
            incompleteCount = incompleteCount
        )
    }

    /**
     * 批量导入播放历史
     */
    suspend fun importHistory(histories: List<VodHistory>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = histories.map { WatchHistoryEntity.fromVodHistory(it) }
            watchHistoryDao.insertAll(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * 播放历史统计信息
 */
data class HistoryStats(
    val totalCount: Int,
    val recentCount: Int,
    val completedCount: Int,
    val incompleteCount: Int
)
