package top.cywin.onetv.movie.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.cywin.onetv.movie.data.api.VodApiService
import top.cywin.onetv.movie.data.cache.MovieCacheManager
import top.cywin.onetv.movie.data.config.AppConfigManager
import top.cywin.onetv.movie.data.database.MovieDatabase
import top.cywin.onetv.movie.data.database.DatabaseManager
import top.cywin.onetv.movie.data.database.dao.*
import top.cywin.onetv.movie.data.parser.LineManager
import top.cywin.onetv.movie.data.parser.ParseManager
import top.cywin.onetv.movie.data.repository.*
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Movie模块的依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object MovieModule {

    /**
     * 提供MovieCacheManager
     */
    @Provides
    @Singleton
    fun provideMovieCacheManager(
        @ApplicationContext context: Context
    ): MovieCacheManager {
        return MovieCacheManager(context)
    }

    /**
     * 提供AppConfigManager
     */
    @Provides
    @Singleton
    fun provideAppConfigManager(
        @ApplicationContext context: Context
    ): AppConfigManager {
        return AppConfigManager(context)
    }

    /**
     * 提供VodConfigManager
     */
    @Provides
    @Singleton
    fun provideVodConfigManager(
        @ApplicationContext context: Context,
        appConfigManager: AppConfigManager
    ): VodConfigManager {
        return VodConfigManager(context, appConfigManager)
    }

    /**
     * 提供配置API服务
     */
    @Provides
    @Singleton
    @ConfigApiService
    fun provideConfigApiService(): VodApiService {
        return VodApiService.createConfigService()
    }

    /**
     * 提供站点API服务
     */
    @Provides
    @Singleton
    @SiteApiService
    fun provideSiteApiService(): VodApiService {
        return VodApiService.createSiteService()
    }

    /**
     * 提供MovieDatabase
     */
    @Provides
    @Singleton
    fun provideMovieDatabase(
        @ApplicationContext context: Context
    ): MovieDatabase {
        return MovieDatabase.getDatabase(context)
    }

    /**
     * 提供WatchHistoryDao
     */
    @Provides
    fun provideWatchHistoryDao(database: MovieDatabase): WatchHistoryDao {
        return database.watchHistoryDao()
    }

    /**
     * 提供FavoriteDao
     */
    @Provides
    fun provideFavoriteDao(database: MovieDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    /**
     * 提供SearchHistoryDao
     */
    @Provides
    fun provideSearchHistoryDao(database: MovieDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    /**
     * 提供CacheDataDao
     */
    @Provides
    fun provideCacheDataDao(database: MovieDatabase): CacheDataDao {
        return database.cacheDataDao()
    }

    /**
     * 提供DatabaseManager
     */
    @Provides
    @Singleton
    fun provideDatabaseManager(database: MovieDatabase): DatabaseManager {
        return DatabaseManager(database)
    }

    /**
     * 提供WatchHistoryRepository
     */
    @Provides
    @Singleton
    fun provideWatchHistoryRepository(
        watchHistoryDao: WatchHistoryDao
    ): WatchHistoryRepository {
        return WatchHistoryRepository(watchHistoryDao)
    }

    /**
     * 提供FavoriteRepository
     */
    @Provides
    @Singleton
    fun provideFavoriteRepository(
        favoriteDao: FavoriteDao
    ): FavoriteRepository {
        return FavoriteRepository(favoriteDao)
    }

    /**
     * 提供ParseManager
     */
    @Provides
    @Singleton
    fun provideParseManager(): ParseManager {
        return ParseManager()
    }

    /**
     * 提供LineManager
     */
    @Provides
    @Singleton
    fun provideLineManager(parseManager: ParseManager): LineManager {
        return LineManager(parseManager)
    }

    /**
     * 提供CloudDriveManager
     */
    @Provides
    @Singleton
    fun provideCloudDriveManager(): top.cywin.onetv.movie.data.cloud.CloudDriveManager {
        return top.cywin.onetv.movie.data.cloud.CloudDriveManager()
    }

    /**
     * 提供VodRepository
     */
    @Provides
    @Singleton
    fun provideVodRepository(
        @ApplicationContext context: Context,
        appConfigManager: AppConfigManager,
        cacheManager: MovieCacheManager,
        configManager: VodConfigManager,
        parseManager: ParseManager
    ): VodRepository {
        return VodRepository(context, appConfigManager, cacheManager, configManager, parseManager)
    }
}

/**
 * 配置API服务限定符
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConfigApiService

/**
 * 站点API服务限定符
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SiteApiService
