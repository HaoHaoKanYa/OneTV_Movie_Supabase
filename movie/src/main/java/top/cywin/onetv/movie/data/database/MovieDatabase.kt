package top.cywin.onetv.movie.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import top.cywin.onetv.movie.data.database.entity.*
import top.cywin.onetv.movie.data.database.dao.*

/**
 * Movie模块数据库 (参考OneMoVie数据库设计)
 */
@Database(
    entities = [
        WatchHistoryEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
        CacheDataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MovieDatabase : RoomDatabase() {

    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun cacheDataDao(): CacheDataDao

    companion object {
        private const val DATABASE_NAME = "movie_database"

        @Volatile
        private var INSTANCE: MovieDatabase? = null

        fun getDatabase(context: Context): MovieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2) // 预留迁移
                    .fallbackToDestructiveMigration() // 开发阶段使用，生产环境需要移除
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库迁移示例（从版本1到版本2）
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 示例：添加新字段
                // database.execSQL("ALTER TABLE watch_history ADD COLUMN newField TEXT DEFAULT ''")
            }
        }

        /**
         * 清理数据库实例（用于测试）
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}

/**
 * 数据库初始化和维护工具
 */
class DatabaseManager(private val database: MovieDatabase) {

    /**
     * 清理过期数据
     */
    suspend fun cleanupExpiredData() {
        val expireTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L // 30天前

        // 清理过期的搜索历史
        database.searchHistoryDao().deleteExpiredSearchHistory(expireTime)

        // 清理过期的缓存数据
        database.cacheDataDao().deleteExpiredCache()

        // 可选：清理过期的播放历史（根据需求决定）
        // database.watchHistoryDao().deleteExpiredHistory(expireTime)
    }

    /**
     * 获取数据库统计信息
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return DatabaseStats(
            watchHistoryCount = database.watchHistoryDao().getHistoryCount(),
            favoriteCount = database.favoriteDao().getFavoriteCount(),
            searchHistoryCount = database.searchHistoryDao().getSearchHistoryCount(),
            cacheDataCount = database.cacheDataDao().getCacheCount(),
            totalCacheSize = database.cacheDataDao().getTotalCacheSize() ?: 0L
        )
    }

    /**
     * 优化数据库
     */
    suspend fun optimizeDatabase() {
        // 清理过期数据
        cleanupExpiredData()

        // 执行VACUUM命令优化数据库文件大小
        database.openHelper.writableDatabase.execSQL("VACUUM")
    }

    /**
     * 备份数据库（导出关键数据）
     */
    suspend fun exportData(): DatabaseBackup {
        return DatabaseBackup(
            favorites = database.favoriteDao().getAllFavorites(),
            watchHistory = database.watchHistoryDao().getAllHistory(),
            searchHistory = database.searchHistoryDao().getAllSearchHistory()
        )
    }

    /**
     * 恢复数据库（导入数据）
     */
    suspend fun importData(backup: DatabaseBackup) {
        // 导入收藏数据
        backup.favorites.collect { favorites ->
            database.favoriteDao().addFavorites(favorites)
        }

        // 导入播放历史
        backup.watchHistory.collect { history ->
            database.watchHistoryDao().insertAll(history)
        }

        // 导入搜索历史
        backup.searchHistory.collect { searchHistory ->
            searchHistory.forEach { history ->
                database.searchHistoryDao().addSearchHistory(history)
            }
        }
    }
}

/**
 * 数据库统计信息
 */
data class DatabaseStats(
    val watchHistoryCount: Int,
    val favoriteCount: Int,
    val searchHistoryCount: Int,
    val cacheDataCount: Int,
    val totalCacheSize: Long
)

/**
 * 数据库备份数据
 */
data class DatabaseBackup(
    val favorites: kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>,
    val watchHistory: kotlinx.coroutines.flow.Flow<List<WatchHistoryEntity>>,
    val searchHistory: kotlinx.coroutines.flow.Flow<List<SearchHistoryEntity>>
)
