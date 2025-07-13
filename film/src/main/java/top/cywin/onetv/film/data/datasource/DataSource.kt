package top.cywin.onetv.film.data.datasource

import top.cywin.onetv.film.data.models.*

/**
 * 数据源接口定义
 * 
 * 基于 FongMi/TV 的数据源架构设计
 * 定义本地和远程数据源的统一接口
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * VOD 数据源接口
 */
interface VodDataSource {
    
    /**
     * 🏠 获取首页内容
     */
    suspend fun getHomeContent(siteKey: String, filter: Boolean = false): Result<VodHomeResult>
    
    /**
     * 📂 获取分类内容
     */
    suspend fun getCategoryContent(
        siteKey: String,
        tid: String,
        page: Int = 1,
        filter: Boolean = false,
        extend: Map<String, String> = emptyMap()
    ): Result<VodCategoryResult>
    
    /**
     * 📄 获取详情内容
     */
    suspend fun getDetailContent(siteKey: String, ids: List<String>): Result<List<VodInfo>>
    
    /**
     * 🔍 搜索内容
     */
    suspend fun searchContent(
        siteKey: String,
        keyword: String,
        page: Int = 1,
        quick: Boolean = false
    ): Result<VodSearchResult>
    
    /**
     * ▶️ 获取播放内容
     */
    suspend fun getPlayContent(
        siteKey: String,
        flag: String,
        id: String,
        vipUrl: List<String> = emptyList()
    ): Result<String>
    
    /**
     * 📋 获取站点列表
     */
    suspend fun getSites(): Result<List<VodSite>>
    
    /**
     * 🔧 获取站点配置
     */
    suspend fun getSiteConfig(siteKey: String): Result<VodSite?>
    
    /**
     * 💾 保存站点配置
     */
    suspend fun saveSiteConfig(site: VodSite): Result<Boolean>
    
    /**
     * 🗑️ 删除站点配置
     */
    suspend fun deleteSiteConfig(siteKey: String): Result<Boolean>
    
    /**
     * 🔄 更新站点配置
     */
    suspend fun updateSiteConfig(site: VodSite): Result<Boolean>
}

/**
 * 播放历史数据源接口
 */
interface PlayHistoryDataSource {
    
    /**
     * 📋 获取播放历史列表
     */
    suspend fun getPlayHistories(
        userId: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PlayHistory>>
    
    /**
     * 📄 获取播放历史详情
     */
    suspend fun getPlayHistory(vodId: String, siteKey: String, userId: String = ""): Result<PlayHistory?>
    
    /**
     * 💾 保存播放历史
     */
    suspend fun savePlayHistory(history: PlayHistory): Result<Boolean>
    
    /**
     * 🔄 更新播放历史
     */
    suspend fun updatePlayHistory(history: PlayHistory): Result<Boolean>
    
    /**
     * 🗑️ 删除播放历史
     */
    suspend fun deletePlayHistory(id: String): Result<Boolean>
    
    /**
     * 🧹 清空播放历史
     */
    suspend fun clearPlayHistories(userId: String = ""): Result<Boolean>
    
    /**
     * 🔍 搜索播放历史
     */
    suspend fun searchPlayHistories(
        keyword: String,
        userId: String = "",
        limit: Int = 20
    ): Result<List<PlayHistory>>
}

/**
 * 收藏数据源接口
 */
interface FavoriteDataSource {
    
    /**
     * 📋 获取收藏列表
     */
    suspend fun getFavorites(
        userId: String = "",
        category: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<FavoriteInfo>>
    
    /**
     * 📄 获取收藏详情
     */
    suspend fun getFavorite(vodId: String, siteKey: String, userId: String = ""): Result<FavoriteInfo?>
    
    /**
     * 💾 添加收藏
     */
    suspend fun addFavorite(favorite: FavoriteInfo): Result<Boolean>
    
    /**
     * 🔄 更新收藏
     */
    suspend fun updateFavorite(favorite: FavoriteInfo): Result<Boolean>
    
    /**
     * 🗑️删除收藏
     */
    suspend fun deleteFavorite(id: String): Result<Boolean>
    
    /**
     * 🧹 清空收藏
     */
    suspend fun clearFavorites(userId: String = "", category: String = ""): Result<Boolean>
    
    /**
     * 🔍 搜索收藏
     */
    suspend fun searchFavorites(
        keyword: String,
        userId: String = "",
        limit: Int = 20
    ): Result<List<FavoriteInfo>>
    
    /**
     * 📂 获取收藏分类
     */
    suspend fun getFavoriteCategories(userId: String = ""): Result<List<String>>
}

/**
 * 下载数据源接口
 */
interface DownloadDataSource {
    
    /**
     * 📋 获取下载列表
     */
    suspend fun getDownloads(
        userId: String = "",
        status: DownloadStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<DownloadInfo>>
    
    /**
     * 📄 获取下载详情
     */
    suspend fun getDownload(id: String): Result<DownloadInfo?>
    
    /**
     * 💾 添加下载任务
     */
    suspend fun addDownload(download: DownloadInfo): Result<Boolean>
    
    /**
     * 🔄 更新下载任务
     */
    suspend fun updateDownload(download: DownloadInfo): Result<Boolean>
    
    /**
     * 🗑️ 删除下载任务
     */
    suspend fun deleteDownload(id: String): Result<Boolean>
    
    /**
     * 🧹 清空下载任务
     */
    suspend fun clearDownloads(userId: String = "", status: DownloadStatus? = null): Result<Boolean>
    
    /**
     * 🔍 搜索下载任务
     */
    suspend fun searchDownloads(
        keyword: String,
        userId: String = "",
        limit: Int = 20
    ): Result<List<DownloadInfo>>
}

/**
 * 配置数据源接口
 */
interface ConfigDataSource {
    
    /**
     * 🔧 获取应用配置
     */
    suspend fun getAppConfig(): Result<AppConfig>
    
    /**
     * 💾 保存应用配置
     */
    suspend fun saveAppConfig(config: AppConfig): Result<Boolean>
    
    /**
     * 👤 获取用户配置
     */
    suspend fun getUserConfig(userId: String): Result<UserConfig?>
    
    /**
     * 💾 保存用户配置
     */
    suspend fun saveUserConfig(config: UserConfig): Result<Boolean>
    
    /**
     * 📋 获取站点配置列表
     */
    suspend fun getSiteConfigs(): Result<List<SiteConfig>>
    
    /**
     * 📄 获取站点配置详情
     */
    suspend fun getSiteConfig(configId: String): Result<SiteConfig?>
    
    /**
     * 💾 保存站点配置
     */
    suspend fun saveSiteConfig(config: SiteConfig): Result<Boolean>
    
    /**
     * 🗑️ 删除站点配置
     */
    suspend fun deleteSiteConfig(configId: String): Result<Boolean>
    
    /**
     * 🔄 更新站点配置
     */
    suspend fun updateSiteConfig(config: SiteConfig): Result<Boolean>
    
    /**
     * 📱 获取设备信息
     */
    suspend fun getDeviceInfo(): Result<DeviceInfo>
    
    /**
     * 💾 保存设备信息
     */
    suspend fun saveDeviceInfo(deviceInfo: DeviceInfo): Result<Boolean>
}

/**
 * 播放设置数据源接口
 */
interface PlaySettingsDataSource {
    
    /**
     * 🔧 获取播放设置
     */
    suspend fun getPlaySettings(userId: String = ""): Result<PlaySettings>
    
    /**
     * 💾 保存播放设置
     */
    suspend fun savePlaySettings(settings: PlaySettings): Result<Boolean>
    
    /**
     * 📊 获取播放统计
     */
    suspend fun getPlayStatistics(userId: String = ""): Result<PlayStatistics>
    
    /**
     * 🔄 更新播放统计
     */
    suspend fun updatePlayStatistics(statistics: PlayStatistics): Result<Boolean>
    
    /**
     * 📈 记录播放事件
     */
    suspend fun recordPlayEvent(
        vodId: String,
        siteKey: String,
        episodeName: String,
        playTime: Long,
        userId: String = ""
    ): Result<Boolean>
}

/**
 * 备份数据源接口
 */
interface BackupDataSource {
    
    /**
     * 📋 获取备份列表
     */
    suspend fun getBackups(userId: String = ""): Result<List<BackupInfo>>
    
    /**
     * 📄 获取备份详情
     */
    suspend fun getBackup(backupId: String): Result<BackupInfo?>
    
    /**
     * 💾 创建备份
     */
    suspend fun createBackup(backup: BackupInfo): Result<Boolean>
    
    /**
     * 📥 恢复备份
     */
    suspend fun restoreBackup(backupId: String): Result<Boolean>
    
    /**
     * 🗑️ 删除备份
     */
    suspend fun deleteBackup(backupId: String): Result<Boolean>
    
    /**
     * 🔄 更新备份信息
     */
    suspend fun updateBackup(backup: BackupInfo): Result<Boolean>
    
    /**
     * 📤 导出备份
     */
    suspend fun exportBackup(backupId: String, filePath: String): Result<Boolean>
    
    /**
     * 📥 导入备份
     */
    suspend fun importBackup(filePath: String): Result<BackupInfo>
}

/**
 * 远程数据源接口
 */
interface RemoteDataSource {
    
    /**
     * 🌐 获取远程配置
     */
    suspend fun getRemoteConfig(url: String): Result<SiteConfig>
    
    /**
     * 🔄 检查更新
     */
    suspend fun checkUpdate(): Result<AppConfig>
    
    /**
     * 📥 下载更新
     */
    suspend fun downloadUpdate(url: String, filePath: String): Result<Boolean>
    
    /**
     * 📊 上传统计数据
     */
    suspend fun uploadStatistics(data: Map<String, Any>): Result<Boolean>
    
    /**
     * 💬 提交反馈
     */
    suspend fun submitFeedback(feedback: Map<String, Any>): Result<Boolean>
    
    /**
     * 🔍 搜索在线资源
     */
    suspend fun searchOnlineResources(keyword: String): Result<List<VodInfo>>
}

/**
 * 本地数据源接口
 */
interface LocalDataSource {
    
    /**
     * 💾 保存到本地存储
     */
    suspend fun saveToLocal(key: String, data: Any): Result<Boolean>
    
    /**
     * 📦 从本地存储获取
     */
    suspend fun <T> getFromLocal(key: String, clazz: Class<T>): Result<T?>
    
    /**
     * 🗑️ 从本地存储删除
     */
    suspend fun deleteFromLocal(key: String): Result<Boolean>
    
    /**
     * 🧹 清空本地存储
     */
    suspend fun clearLocal(): Result<Boolean>
    
    /**
     * 📋 获取本地存储键列表
     */
    suspend fun getLocalKeys(): Result<List<String>>
    
    /**
     * 📊 获取本地存储统计
     */
    suspend fun getLocalStats(): Result<Map<String, Any>>
}
