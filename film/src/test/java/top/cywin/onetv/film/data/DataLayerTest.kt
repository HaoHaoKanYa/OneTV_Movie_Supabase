package top.cywin.onetv.film.data

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.data.datasource.*
import top.cywin.onetv.film.data.repository.FilmRepository

/**
 * 数据层完整测试
 * 
 * 测试第十阶段实现的所有数据层功能
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class DataLayerTest {
    
    private lateinit var context: Context
    private lateinit var localDataSource: LocalDataSourceImpl
    private lateinit var filmRepository: FilmRepository
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
        localDataSource = LocalDataSourceImpl(context)
    }
    
    @Test
    fun testVodSite() {
        val site = VodSite(
            key = "test_site",
            name = "测试站点",
            type = 1,
            api = "https://api.test.com",
            searchable = 1,
            quickSearch = 1,
            filterable = 1,
            enabled = true,
            group = "测试组"
        )
        
        assertEquals("test_site", site.key)
        assertEquals("测试站点", site.name)
        assertEquals(1, site.type)
        assertTrue(site.isJarSite())
        assertFalse(site.isJsonSite())
        assertFalse(site.isXmlSite())
        assertTrue(site.isSearchable())
        assertTrue(site.isQuickSearchable())
        assertTrue(site.isFilterable())
        
        val summary = site.getSummary()
        assertEquals("test_site", summary["key"])
        assertEquals("测试站点", summary["name"])
        assertEquals("JAR", summary["type_name"])
        assertTrue(summary["searchable"] as Boolean)
        assertTrue(summary["enabled"] as Boolean)
    }
    
    @Test
    fun testVodCategory() {
        val category = VodCategory(
            typeId = "1",
            typeName = "电影",
            typeFlag = "movie",
            land = 1,
            ratio = 1.78,
            pic = "https://example.com/pic.jpg"
        )
        
        assertEquals("1", category.typeId)
        assertEquals("电影", category.typeName)
        assertEquals("movie", category.typeFlag)
        assertTrue(category.isLandscape())
        assertEquals(1.78, category.ratio, 0.01)
        assertFalse(category.hasFilters())
    }
    
    @Test
    fun testVodInfo() {
        val vodInfo = VodInfo(
            vodId = "123",
            vodName = "测试电影",
            vodPic = "https://example.com/pic.jpg",
            vodRemarks = "高清",
            vodYear = "2023",
            vodArea = "中国",
            vodDirector = "张导演",
            vodActor = "李演员,王演员",
            vodScore = "8.5",
            vodPlayFrom = "线路1$$$线路2",
            vodPlayUrl = "第1集$url1#第2集$url2$$$第1集$url3#第2集$url4",
            siteKey = "test_site",
            siteName = "测试站点"
        )
        
        assertEquals("123", vodInfo.vodId)
        assertEquals("测试电影", vodInfo.vodName)
        assertEquals("test_site", vodInfo.siteKey)
        
        val playList = vodInfo.getPlayList()
        assertEquals(2, playList.size)
        assertEquals("线路1", playList[0].from)
        assertEquals("线路2", playList[1].from)
        assertEquals(2, playList[0].episodes.size)
        assertEquals("第1集", playList[0].episodes[0].name)
        assertEquals("url1", playList[0].episodes[0].url)
        
        val summary = vodInfo.getSummary()
        assertEquals("123", summary["vod_id"])
        assertEquals("测试电影", summary["vod_name"])
        assertEquals(2, summary["play_groups"])
    }
    
    @Test
    fun testVodSearchResult() {
        val vodList = listOf(
            VodInfo("1", "电影1"),
            VodInfo("2", "电影2"),
            VodInfo("3", "电影3")
        )
        
        val searchResult = VodSearchResult(
            list = vodList,
            page = 1,
            pageCount = 5,
            limit = 20,
            total = 100,
            siteKey = "test_site",
            keyword = "测试"
        )
        
        assertEquals(3, searchResult.list.size)
        assertEquals(1, searchResult.page)
        assertEquals(5, searchResult.pageCount)
        assertEquals(100, searchResult.total)
        assertTrue(searchResult.hasMore())
        assertFalse(searchResult.isEmpty())
        
        val summary = searchResult.getSummary()
        assertEquals("测试", summary["keyword"])
        assertEquals("test_site", summary["site_key"])
        assertEquals(100, summary["total"])
        assertTrue(summary["has_more"] as Boolean)
    }
    
    @Test
    fun testPlayInfo() {
        val playInfo = PlayInfo(
            vodId = "123",
            vodName = "测试电影",
            siteKey = "test_site",
            flag = "线路1",
            episodeName = "第1集",
            episodeUrl = "https://play.url",
            position = 300000L, // 5分钟
            duration = 3600000L, // 1小时
            playCount = 3
        )
        
        assertEquals("123", playInfo.vodId)
        assertEquals("测试电影", playInfo.vodName)
        assertEquals(0.083f, playInfo.getProgressPercent(), 0.01f)
        assertFalse(playInfo.isJustStarted())
        assertFalse(playInfo.isNearEnd())
        assertTrue(playInfo.canResume())
        
        val summary = playInfo.getSummary()
        assertEquals("123", summary["vod_id"])
        assertEquals("8.3%", summary["progress_percent"])
        assertTrue(summary["can_resume"] as Boolean)
        assertEquals(3, summary["play_count"])
    }
    
    @Test
    fun testPlayHistory() {
        val history = PlayHistory(
            id = "hist_123",
            vodId = "123",
            vodName = "测试电影",
            siteKey = "test_site",
            flag = "线路1",
            episodeName = "第1集",
            position = 1800000L, // 30分钟
            duration = 3600000L, // 1小时
            isFinished = false,
            playCount = 2
        )
        
        assertEquals("hist_123", history.id)
        assertEquals("123", history.vodId)
        assertEquals(0.5f, history.getProgressPercent(), 0.01f)
        assertTrue(history.canResume())
        
        val summary = history.getSummary()
        assertEquals("hist_123", summary["id"])
        assertEquals("50.0%", summary["progress_percent"])
        assertTrue(summary["can_resume"] as Boolean)
        assertEquals(2, summary["play_count"])
    }
    
    @Test
    fun testFavoriteInfo() {
        val favorite = FavoriteInfo(
            id = "fav_123",
            vodId = "123",
            vodName = "测试电影",
            siteKey = "test_site",
            category = "电影",
            rating = 4.5f,
            tags = listOf("动作", "科幻"),
            isWatched = true,
            watchProgress = 0.8f
        )
        
        assertEquals("fav_123", favorite.id)
        assertEquals("123", favorite.vodId)
        assertEquals("电影", favorite.category)
        assertEquals(4.5f, favorite.rating, 0.01f)
        assertEquals(2, favorite.tags.size)
        assertTrue(favorite.isWatched)
        
        val summary = favorite.getSummary()
        assertEquals("fav_123", summary["id"])
        assertEquals("电影", summary["category"])
        assertEquals(4.5f, summary["rating"])
        assertEquals("80.0%", summary["watch_progress"])
        assertEquals(2, summary["tags_count"])
    }
    
    @Test
    fun testDownloadInfo() {
        val download = DownloadInfo(
            id = "dl_123",
            vodId = "123",
            vodName = "测试电影",
            siteKey = "test_site",
            flag = "线路1",
            episodeName = "第1集",
            episodeUrl = "https://download.url",
            fileSize = 1024 * 1024 * 1024L, // 1GB
            downloadedSize = 512 * 1024 * 1024L, // 512MB
            status = DownloadStatus.DOWNLOADING,
            progress = 0.5f,
            speed = 1024 * 1024L, // 1MB/s
            retryCount = 1,
            maxRetries = 3
        )
        
        assertEquals("dl_123", download.id)
        assertEquals("123", download.vodId)
        assertTrue(download.isDownloading())
        assertFalse(download.isCompleted())
        assertFalse(download.isFailed())
        assertFalse(download.canRetry())
        
        val summary = download.getSummary()
        assertEquals("dl_123", summary["id"])
        assertEquals("DOWNLOADING", summary["status"])
        assertEquals("50.0%", summary["progress"])
        assertEquals("1024.00", summary["file_size_mb"])
        assertEquals("1024.0", summary["speed_kbps"])
    }
    
    @Test
    fun testDownloadStatus() {
        assertEquals(6, DownloadStatus.values().size)
        assertTrue(DownloadStatus.values().contains(DownloadStatus.PENDING))
        assertTrue(DownloadStatus.values().contains(DownloadStatus.DOWNLOADING))
        assertTrue(DownloadStatus.values().contains(DownloadStatus.PAUSED))
        assertTrue(DownloadStatus.values().contains(DownloadStatus.COMPLETED))
        assertTrue(DownloadStatus.values().contains(DownloadStatus.FAILED))
        assertTrue(DownloadStatus.values().contains(DownloadStatus.CANCELLED))
    }
    
    @Test
    fun testPlaySettings() {
        val settings = PlaySettings(
            playSpeed = 1.5f,
            volume = 0.8f,
            brightness = 0.6f,
            autoNext = true,
            subtitleEnabled = true,
            playerType = "exoplayer",
            hardwareDecoding = true
        )
        
        assertEquals(1.5f, settings.playSpeed, 0.01f)
        assertEquals(0.8f, settings.volume, 0.01f)
        assertEquals(0.6f, settings.brightness, 0.01f)
        assertTrue(settings.autoNext)
        assertTrue(settings.subtitleEnabled)
        assertEquals("exoplayer", settings.playerType)
        
        val summary = settings.getSummary()
        assertEquals(1.5f, summary["play_speed"])
        assertTrue(summary["auto_next"] as Boolean)
        assertTrue(summary["subtitle_enabled"] as Boolean)
        assertTrue(summary["hardware_decoding"] as Boolean)
    }
    
    @Test
    fun testAppConfig() {
        val config = AppConfig(
            version = "2.0.0",
            buildNumber = 100,
            theme = "dark",
            language = "zh-CN",
            enableAnalytics = true,
            maxCacheSize = 1024 * 1024 * 1024L, // 1GB
            networkTimeout = 30000L
        )
        
        assertEquals("2.0.0", config.version)
        assertEquals(100, config.buildNumber)
        assertEquals("dark", config.theme)
        assertEquals("zh-CN", config.language)
        assertTrue(config.enableAnalytics)
        assertFalse(config.isDebugMode())
        
        val summary = config.getSummary()
        assertEquals("2.0.0", summary["version"])
        assertEquals("dark", summary["theme"])
        assertEquals(1024L, summary["max_cache_size_mb"])
        assertEquals(30000L, summary["network_timeout_ms"])
    }
    
    @Test
    fun testUserConfig() {
        val config = UserConfig(
            userId = "user_123",
            username = "testuser",
            nickname = "测试用户",
            theme = "light",
            autoPlay = true,
            parentalControl = false,
            loginCount = 10
        )
        
        assertEquals("user_123", config.userId)
        assertEquals("testuser", config.username)
        assertEquals("测试用户", config.nickname)
        assertEquals("light", config.theme)
        assertTrue(config.autoPlay)
        assertFalse(config.isNewUser())
        assertFalse(config.isParentalControlEnabled())
        
        val summary = config.getSummary()
        assertEquals("user_123", summary["user_id"])
        assertEquals("testuser", summary["username"])
        assertEquals(10, summary["login_count"])
        assertFalse(summary["is_new_user"] as Boolean)
    }
    
    @Test
    fun testSiteConfig() {
        val sites = listOf(
            VodSite("site1", "站点1", 1, "api1"),
            VodSite("site2", "站点2", 3, "api2")
        )
        
        val config = SiteConfig(
            configId = "config_123",
            name = "测试配置",
            version = "1.0.0",
            author = "测试作者",
            sites = sites,
            isDefault = true,
            isEnabled = true,
            priority = 1,
            tags = listOf("官方", "推荐")
        )
        
        assertEquals("config_123", config.configId)
        assertEquals("测试配置", config.name)
        assertEquals("1.0.0", config.version)
        assertEquals(2, config.sites.size)
        assertTrue(config.hasSites())
        assertFalse(config.hasLives())
        assertFalse(config.hasParses())
        
        val summary = config.getSummary()
        assertEquals("config_123", summary["config_id"])
        assertEquals("测试配置", summary["name"])
        assertEquals(2, summary["sites_count"])
        assertTrue(summary["is_default"] as Boolean)
        assertEquals(2, summary["tags_count"])
    }
    
    @Test
    fun testBackupInfo() {
        val backup = BackupInfo(
            backupId = "backup_123",
            name = "完整备份",
            type = BackupType.FULL,
            fileSize = 50 * 1024 * 1024L, // 50MB
            itemCount = 1000,
            isEncrypted = true,
            isCompressed = true,
            categories = listOf("设置", "收藏", "历史"),
            appVersion = "2.0.0"
        )
        
        assertEquals("backup_123", backup.backupId)
        assertEquals("完整备份", backup.name)
        assertEquals(BackupType.FULL, backup.type)
        assertEquals(1000, backup.itemCount)
        assertTrue(backup.isEncrypted)
        assertTrue(backup.isCompressed)
        assertEquals(3, backup.categories.size)
        
        val summary = backup.getSummary()
        assertEquals("backup_123", summary["backup_id"])
        assertEquals("FULL", summary["type"])
        assertEquals("50.00", summary["file_size_mb"])
        assertEquals(1000, summary["item_count"])
        assertTrue(summary["is_encrypted"] as Boolean)
    }
    
    @Test
    fun testBackupType() {
        assertEquals(6, BackupType.values().size)
        assertTrue(BackupType.values().contains(BackupType.FULL))
        assertTrue(BackupType.values().contains(BackupType.SETTINGS))
        assertTrue(BackupType.values().contains(BackupType.FAVORITES))
        assertTrue(BackupType.values().contains(BackupType.HISTORY))
        assertTrue(BackupType.values().contains(BackupType.SITES))
        assertTrue(BackupType.values().contains(BackupType.CUSTOM))
    }
    
    @Test
    fun testLocalDataSourceBasicFunctionality() = runTest {
        // 测试本地数据源基本功能
        assertNotNull(localDataSource)
        
        // 测试站点配置
        val site = VodSite(
            key = "test_site",
            name = "测试站点",
            type = 1,
            api = "https://api.test.com"
        )
        
        val saveResult = localDataSource.saveSiteConfig(site)
        assertTrue(saveResult.isSuccess)
        
        val getResult = localDataSource.getSiteConfig("test_site")
        assertTrue(getResult.isSuccess)
        assertNotNull(getResult.getOrNull())
        assertEquals("test_site", getResult.getOrNull()?.key)
        
        // 测试应用配置
        val appConfig = AppConfig(version = "2.0.0")
        val saveConfigResult = localDataSource.saveAppConfig(appConfig)
        assertTrue(saveConfigResult.isSuccess)
        
        val getConfigResult = localDataSource.getAppConfig()
        assertTrue(getConfigResult.isSuccess)
        assertEquals("2.0.0", getConfigResult.getOrNull()?.version)
    }
}
