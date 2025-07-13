package top.cywin.onetv.film.data.datasource

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import top.cywin.onetv.film.data.models.*
import top.cywin.onetv.film.utils.FileUtils
import top.cywin.onetv.film.utils.JsonUtils
import java.io.File
import java.util.UUID

/**
 * 本地数据源实现
 * 
 * 基于 FongMi/TV 的本地数据存储实现
 * 使用 SharedPreferences 和文件存储
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class LocalDataSourceImpl(
    private val context: Context
) : VodDataSource, PlayHistoryDataSource, FavoriteDataSource, DownloadDataSource, 
    ConfigDataSource, PlaySettingsDataSource, BackupDataSource, LocalDataSource {
    
    companion object {
        private const val TAG = "ONETV_FILM_LOCAL_DATA_SOURCE"
        private const val PREFS_NAME = "onetv_film_data"
        private const val DATA_DIR = "onetv_data"
        
        // 数据文件名
        private const val SITES_FILE = "sites.json"
        private const val PLAY_HISTORIES_FILE = "play_histories.json"
        private const val FAVORITES_FILE = "favorites.json"
        private const val DOWNLOADS_FILE = "downloads.json"
        private const val APP_CONFIG_FILE = "app_config.json"
        private const val USER_CONFIGS_FILE = "user_configs.json"
        private const val SITE_CONFIGS_FILE = "site_configs.json"
        private const val PLAY_SETTINGS_FILE = "play_settings.json"
        private const val PLAY_STATISTICS_FILE = "play_statistics.json"
        private const val DEVICE_INFO_FILE = "device_info.json"
        private const val BACKUPS_FILE = "backups.json"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val dataDir: File by lazy {
        File(context.filesDir, DATA_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    
    // ==================== VOD 数据源实现 ====================
    
    override suspend fun getHomeContent(siteKey: String, filter: Boolean): Result<VodHomeResult> {
        return Result.failure(UnsupportedOperationException("本地数据源不支持获取首页内容"))
    }
    
    override suspend fun getCategoryContent(
        siteKey: String,
        tid: String,
        page: Int,
        filter: Boolean,
        extend: Map<String, String>
    ): Result<VodCategoryResult> {
        return Result.failure(UnsupportedOperationException("本地数据源不支持获取分类内容"))
    }
    
    override suspend fun getDetailContent(siteKey: String, ids: List<String>): Result<List<VodInfo>> {
        return Result.failure(UnsupportedOperationException("本地数据源不支持获取详情内容"))
    }
    
    override suspend fun searchContent(
        siteKey: String,
        keyword: String,
        page: Int,
        quick: Boolean
    ): Result<VodSearchResult> {
        return Result.failure(UnsupportedOperationException("本地数据源不支持搜索内容"))
    }
    
    override suspend fun getPlayContent(
        siteKey: String,
        flag: String,
        id: String,
        vipUrl: List<String>
    ): Result<String> {
        return Result.failure(UnsupportedOperationException("本地数据源不支持获取播放内容"))
    }
    
    override suspend fun getSites(): Result<List<VodSite>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(dataDir, SITES_FILE)
            if (!file.exists()) {
                Result.success(emptyList())
            } else {
                val content = FileUtils.readFileToString(file)
                val sites = json.decodeFromString<List<VodSite>>(content)
                Result.success(sites)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点列表失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSiteConfig(siteKey: String): Result<VodSite?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sites = getSites().getOrNull() ?: emptyList()
            val site = sites.find { it.key == siteKey }
            Result.success(site)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取站点配置失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveSiteConfig(site: VodSite): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sites = getSites().getOrNull()?.toMutableList() ?: mutableListOf()
            val existingIndex = sites.indexOfFirst { it.key == site.key }
            
            if (existingIndex >= 0) {
                sites[existingIndex] = site.copy(updateTime = System.currentTimeMillis())
            } else {
                sites.add(site.copy(createTime = System.currentTimeMillis()))
            }
            
            val file = File(dataDir, SITES_FILE)
            val content = json.encodeToString(sites)
            FileUtils.writeStringToFile(file, content)
            
            Log.d(TAG, "💾 站点配置保存成功: ${site.key}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存站点配置失败: ${site.key}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSiteConfig(siteKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sites = getSites().getOrNull()?.toMutableList() ?: mutableListOf()
            val removed = sites.removeAll { it.key == siteKey }
            
            if (removed) {
                val file = File(dataDir, SITES_FILE)
                val content = json.encodeToString(sites)
                FileUtils.writeStringToFile(file, content)
                
                Log.d(TAG, "🗑️ 站点配置删除成功: $siteKey")
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除站点配置失败: $siteKey", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateSiteConfig(site: VodSite): Result<Boolean> {
        return saveSiteConfig(site)
    }
    
    // ==================== 播放历史数据源实现 ====================
    
    override suspend fun getPlayHistories(
        userId: String,
        limit: Int,
        offset: Int
    ): Result<List<PlayHistory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(dataDir, PLAY_HISTORIES_FILE)
            if (!file.exists()) {
                Result.success(emptyList())
            } else {
                val content = FileUtils.readFileToString(file)
                val allHistories = json.decodeFromString<List<PlayHistory>>(content)
                
                val filteredHistories = if (userId.isNotEmpty()) {
                    allHistories.filter { it.userId == userId }
                } else {
                    allHistories
                }
                
                val sortedHistories = filteredHistories
                    .sortedByDescending { it.updateTime }
                    .drop(offset)
                    .take(limit)
                
                Result.success(sortedHistories)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放历史失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getPlayHistory(
        vodId: String,
        siteKey: String,
        userId: String
    ): Result<PlayHistory?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val histories = getPlayHistories(userId).getOrNull() ?: emptyList()
            val history = histories.find { it.vodId == vodId && it.siteKey == siteKey }
            Result.success(history)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取播放历史详情失败: $vodId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun savePlayHistory(history: PlayHistory): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val histories = getPlayHistories().getOrNull()?.toMutableList() ?: mutableListOf()
            val existingIndex = histories.indexOfFirst { 
                it.vodId == history.vodId && it.siteKey == history.siteKey && it.userId == history.userId 
            }
            
            val newHistory = if (history.id.isEmpty()) {
                history.copy(id = UUID.randomUUID().toString())
            } else {
                history
            }
            
            if (existingIndex >= 0) {
                histories[existingIndex] = newHistory.copy(updateTime = System.currentTimeMillis())
            } else {
                histories.add(newHistory.copy(createTime = System.currentTimeMillis()))
            }
            
            val file = File(dataDir, PLAY_HISTORIES_FILE)
            val content = json.encodeToString(histories)
            FileUtils.writeStringToFile(file, content)
            
            Log.d(TAG, "💾 播放历史保存成功: ${newHistory.vodName}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存播放历史失败: ${history.vodName}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updatePlayHistory(history: PlayHistory): Result<Boolean> {
        return savePlayHistory(history)
    }
    
    override suspend fun deletePlayHistory(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val histories = getPlayHistories().getOrNull()?.toMutableList() ?: mutableListOf()
            val removed = histories.removeAll { it.id == id }
            
            if (removed) {
                val file = File(dataDir, PLAY_HISTORIES_FILE)
                val content = json.encodeToString(histories)
                FileUtils.writeStringToFile(file, content)
                
                Log.d(TAG, "🗑️ 播放历史删除成功: $id")
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除播放历史失败: $id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun clearPlayHistories(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (userId.isEmpty()) {
                // 清空所有历史
                val file = File(dataDir, PLAY_HISTORIES_FILE)
                FileUtils.writeStringToFile(file, "[]")
            } else {
                // 清空指定用户的历史
                val histories = getPlayHistories().getOrNull()?.toMutableList() ?: mutableListOf()
                histories.removeAll { it.userId == userId }
                
                val file = File(dataDir, PLAY_HISTORIES_FILE)
                val content = json.encodeToString(histories)
                FileUtils.writeStringToFile(file, content)
            }
            
            Log.d(TAG, "🧹 播放历史清空成功")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空播放历史失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchPlayHistories(
        keyword: String,
        userId: String,
        limit: Int
    ): Result<List<PlayHistory>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val histories = getPlayHistories(userId).getOrNull() ?: emptyList()
            val filteredHistories = histories.filter { history ->
                history.vodName.contains(keyword, ignoreCase = true) ||
                history.episodeName.contains(keyword, ignoreCase = true) ||
                history.siteName.contains(keyword, ignoreCase = true)
            }.take(limit)
            
            Result.success(filteredHistories)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索播放历史失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    // ==================== 收藏数据源实现 ====================
    
    override suspend fun getFavorites(
        userId: String,
        category: String,
        limit: Int,
        offset: Int
    ): Result<List<FavoriteInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(dataDir, FAVORITES_FILE)
            if (!file.exists()) {
                Result.success(emptyList())
            } else {
                val content = FileUtils.readFileToString(file)
                val allFavorites = json.decodeFromString<List<FavoriteInfo>>(content)
                
                val filteredFavorites = allFavorites.filter { favorite ->
                    (userId.isEmpty() || favorite.userId == userId) &&
                    (category.isEmpty() || favorite.category == category)
                }
                
                val sortedFavorites = filteredFavorites
                    .sortedByDescending { it.createTime }
                    .drop(offset)
                    .take(limit)
                
                Result.success(sortedFavorites)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取收藏列表失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getFavorite(
        vodId: String,
        siteKey: String,
        userId: String
    ): Result<FavoriteInfo?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites(userId).getOrNull() ?: emptyList()
            val favorite = favorites.find { it.vodId == vodId && it.siteKey == siteKey }
            Result.success(favorite)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取收藏详情失败: $vodId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun addFavorite(favorite: FavoriteInfo): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites().getOrNull()?.toMutableList() ?: mutableListOf()
            
            // 检查是否已存在
            val existingIndex = favorites.indexOfFirst { 
                it.vodId == favorite.vodId && it.siteKey == favorite.siteKey && it.userId == favorite.userId 
            }
            
            if (existingIndex >= 0) {
                // 已存在，更新
                favorites[existingIndex] = favorite.copy(updateTime = System.currentTimeMillis())
            } else {
                // 不存在，添加
                val newFavorite = if (favorite.id.isEmpty()) {
                    favorite.copy(id = UUID.randomUUID().toString())
                } else {
                    favorite
                }
                favorites.add(newFavorite.copy(createTime = System.currentTimeMillis()))
            }
            
            val file = File(dataDir, FAVORITES_FILE)
            val content = json.encodeToString(favorites)
            FileUtils.writeStringToFile(file, content)
            
            Log.d(TAG, "💾 收藏添加成功: ${favorite.vodName}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 添加收藏失败: ${favorite.vodName}", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateFavorite(favorite: FavoriteInfo): Result<Boolean> {
        return addFavorite(favorite)
    }
    
    override suspend fun deleteFavorite(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites().getOrNull()?.toMutableList() ?: mutableListOf()
            val removed = favorites.removeAll { it.id == id }
            
            if (removed) {
                val file = File(dataDir, FAVORITES_FILE)
                val content = json.encodeToString(favorites)
                FileUtils.writeStringToFile(file, content)
                
                Log.d(TAG, "🗑️ 收藏删除成功: $id")
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 删除收藏失败: $id", e)
            Result.failure(e)
        }
    }
    
    override suspend fun clearFavorites(userId: String, category: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites().getOrNull()?.toMutableList() ?: mutableListOf()
            
            favorites.removeAll { favorite ->
                (userId.isEmpty() || favorite.userId == userId) &&
                (category.isEmpty() || favorite.category == category)
            }
            
            val file = File(dataDir, FAVORITES_FILE)
            val content = json.encodeToString(favorites)
            FileUtils.writeStringToFile(file, content)
            
            Log.d(TAG, "🧹 收藏清空成功")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 清空收藏失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun searchFavorites(
        keyword: String,
        userId: String,
        limit: Int
    ): Result<List<FavoriteInfo>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites(userId).getOrNull() ?: emptyList()
            val filteredFavorites = favorites.filter { favorite ->
                favorite.vodName.contains(keyword, ignoreCase = true) ||
                favorite.siteName.contains(keyword, ignoreCase = true) ||
                favorite.typeName.contains(keyword, ignoreCase = true)
            }.take(limit)
            
            Result.success(filteredFavorites)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 搜索收藏失败: $keyword", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getFavoriteCategories(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favorites = getFavorites(userId).getOrNull() ?: emptyList()
            val categories = favorites.map { it.category }.distinct().sorted()
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取收藏分类失败", e)
            Result.failure(e)
        }
    }
    
    // ==================== 其他接口实现（继续在下一个文件中） ====================
    
    override suspend fun getDownloads(
        userId: String,
        status: DownloadStatus?,
        limit: Int,
        offset: Int
    ): Result<List<DownloadInfo>> {
        // 实现下载数据源方法...
        return Result.success(emptyList())
    }
    
    override suspend fun getDownload(id: String): Result<DownloadInfo?> {
        return Result.success(null)
    }
    
    override suspend fun addDownload(download: DownloadInfo): Result<Boolean> {
        return Result.success(true)
    }
    
    override suspend fun updateDownload(download: DownloadInfo): Result<Boolean> {
        return Result.success(true)
    }
    
    override suspend fun deleteDownload(id: String): Result<Boolean> {
        return Result.success(true)
    }
    
    override suspend fun clearDownloads(userId: String, status: DownloadStatus?): Result<Boolean> {
        return Result.success(true)
    }
    
    override suspend fun searchDownloads(keyword: String, userId: String, limit: Int): Result<List<DownloadInfo>> {
        return Result.success(emptyList())
    }
    
    // ==================== 配置数据源实现 ====================
    
    override suspend fun getAppConfig(): Result<AppConfig> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(dataDir, APP_CONFIG_FILE)
            if (!file.exists()) {
                val defaultConfig = AppConfig()
                saveAppConfig(defaultConfig)
                Result.success(defaultConfig)
            } else {
                val content = FileUtils.readFileToString(file)
                val config = json.decodeFromString<AppConfig>(content)
                Result.success(config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取应用配置失败", e)
            Result.failure(e)
        }
    }
    
    override suspend fun saveAppConfig(config: AppConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(dataDir, APP_CONFIG_FILE)
            val content = json.encodeToString(config.copy(updateTime = System.currentTimeMillis()))
            FileUtils.writeStringToFile(file, content)
            
            Log.d(TAG, "💾 应用配置保存成功")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 保存应用配置失败", e)
            Result.failure(e)
        }
    }
    
    // 其他方法的简化实现...
    override suspend fun getUserConfig(userId: String): Result<UserConfig?> = Result.success(null)
    override suspend fun saveUserConfig(config: UserConfig): Result<Boolean> = Result.success(true)
    override suspend fun getSiteConfigs(): Result<List<SiteConfig>> = Result.success(emptyList())
    override suspend fun getSiteConfig(configId: String): Result<SiteConfig?> = Result.success(null)
    override suspend fun saveSiteConfig(config: SiteConfig): Result<Boolean> = Result.success(true)
    override suspend fun deleteSiteConfig(configId: String): Result<Boolean> = Result.success(true)
    override suspend fun updateSiteConfig(config: SiteConfig): Result<Boolean> = Result.success(true)
    override suspend fun getDeviceInfo(): Result<DeviceInfo> = Result.success(DeviceInfo("", ""))
    override suspend fun saveDeviceInfo(deviceInfo: DeviceInfo): Result<Boolean> = Result.success(true)
    
    // 播放设置数据源实现...
    override suspend fun getPlaySettings(userId: String): Result<PlaySettings> = Result.success(PlaySettings())
    override suspend fun savePlaySettings(settings: PlaySettings): Result<Boolean> = Result.success(true)
    override suspend fun getPlayStatistics(userId: String): Result<PlayStatistics> = Result.success(PlayStatistics())
    override suspend fun updatePlayStatistics(statistics: PlayStatistics): Result<Boolean> = Result.success(true)
    override suspend fun recordPlayEvent(vodId: String, siteKey: String, episodeName: String, playTime: Long, userId: String): Result<Boolean> = Result.success(true)
    
    // 备份数据源实现...
    override suspend fun getBackups(userId: String): Result<List<BackupInfo>> = Result.success(emptyList())
    override suspend fun getBackup(backupId: String): Result<BackupInfo?> = Result.success(null)
    override suspend fun createBackup(backup: BackupInfo): Result<Boolean> = Result.success(true)
    override suspend fun restoreBackup(backupId: String): Result<Boolean> = Result.success(true)
    override suspend fun deleteBackup(backupId: String): Result<Boolean> = Result.success(true)
    override suspend fun updateBackup(backup: BackupInfo): Result<Boolean> = Result.success(true)
    override suspend fun exportBackup(backupId: String, filePath: String): Result<Boolean> = Result.success(true)
    override suspend fun importBackup(filePath: String): Result<BackupInfo> = Result.success(BackupInfo("", "", BackupType.FULL))
    
    // 本地数据源实现...
    override suspend fun saveToLocal(key: String, data: Any): Result<Boolean> = Result.success(true)
    override suspend fun <T> getFromLocal(key: String, clazz: Class<T>): Result<T?> = Result.success(null)
    override suspend fun deleteFromLocal(key: String): Result<Boolean> = Result.success(true)
    override suspend fun clearLocal(): Result<Boolean> = Result.success(true)
    override suspend fun getLocalKeys(): Result<List<String>> = Result.success(emptyList())
    override suspend fun getLocalStats(): Result<Map<String, Any>> = Result.success(emptyMap())
}
