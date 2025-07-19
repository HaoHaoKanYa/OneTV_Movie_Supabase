package top.cywin.onetv.movie.ui.model

/**
 * Compose UI数据模型 - 与FongMi_TV数据模型解耦的UI专用模型
 */

// ===== 电影相关模型 =====
data class MovieItem(
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val vodRemarks: String,
    val vodYear: String,
    val vodArea: String,
    val vodDirector: String,
    val vodActor: String,
    val vodContent: String,
    val siteKey: String,
    val isCollected: Boolean = false,
    val watchProgress: Float = 0f,
    val lastWatchTime: Long = 0
)

// ===== 站点相关模型 =====
data class SiteInfo(
    val key: String,
    val name: String,
    val api: String,
    val searchable: Boolean,
    val playable: Boolean,
    val isActive: Boolean = false
)

// ===== 分类相关模型 =====
data class CategoryInfo(
    val typeId: String,
    val typeName: String,
    val typeFlag: String,
    val isSelected: Boolean = false
)

// ===== 搜索相关模型 =====
data class SearchResult(
    val movies: List<MovieItem>,
    val page: Int,
    val pageCount: Int,
    val total: Int
)

// ===== 播放相关模型 =====
data class PlayFlag(
    val flag: String,
    val urls: String,
    val isSelected: Boolean = false
)

data class Episode(
    val index: Int,
    val name: String,
    val url: String,
    val isWatched: Boolean = false,
    val progress: Float = 0f
)

data class PlayInfo(
    val vodId: String,
    val vodName: String,
    val siteKey: String,
    val flags: List<PlayFlag>,
    val episodes: List<Episode>,
    val currentFlag: PlayFlag? = null,
    val currentEpisode: Episode? = null
)

// ===== 历史记录模型 =====
data class WatchHistory(
    val vodId: String,
    val vodName: String,
    val siteKey: String,
    val episodeName: String,
    val position: Long,
    val duration: Long,
    val watchTime: Long,
    val isCompleted: Boolean
)

// ===== 收藏模型 =====
data class FavoriteItem(
    val vodId: String,
    val vodName: String,
    val vodPic: String,
    val siteKey: String,
    val siteName: String,
    val addTime: Long
)

// ===== 配置相关模型 =====
data class VodConfigUrl(
    val name: String,
    val url: String,
    val isSelected: Boolean = false
)

data class ConfigInfo(
    val name: String,
    val url: String,
    val sites: List<SiteInfo>,
    val isStoreHouse: Boolean = false
)

// ===== 云盘相关模型 =====
data class CloudDriveConfig(
    val id: String,
    val name: String,
    val type: String, // "alist", "webdav", etc.
    val server: String,
    val username: String,
    val password: String,
    val isActive: Boolean = false
)

data class CloudFile(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val playUrl: String? = null
)

// ===== UI状态模型 =====
data class LoadingState(
    val isLoading: Boolean = false,
    val message: String = "",
    val progress: Float = 0f
)

data class ErrorState(
    val hasError: Boolean = false,
    val message: String = "",
    val canRetry: Boolean = true
)

// ===== 首页相关模型 =====
data class HomeCategorySection(
    val categoryId: String,
    val categoryName: String,
    val movies: List<MovieItem>,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
)

data class HomeRecommendSection(
    val title: String,
    val movies: List<MovieItem>,
    val type: String // "hot", "new", "recommend"
)

// ===== 播放器相关模型 =====
data class PlayerState(
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val bufferedPosition: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isFullscreen: Boolean = false,
    val showControls: Boolean = true
)

data class VideoQuality(
    val name: String,
    val url: String,
    val isSelected: Boolean = false
)

// ===== 筛选相关模型 =====
data class FilterOption(
    val key: String,
    val name: String,
    val values: List<FilterValue>
)

data class FilterValue(
    val value: String,
    val name: String,
    val isSelected: Boolean = false
)

// ===== 设置相关模型 =====
data class SettingItem(
    val key: String,
    val title: String,
    val description: String,
    val type: SettingType,
    val value: Any,
    val options: List<String> = emptyList()
)

enum class SettingType {
    SWITCH,
    SELECT,
    INPUT,
    SLIDER,
    BUTTON
}

// ===== 网络状态模型 =====
data class NetworkState(
    val isConnected: Boolean = false,
    val networkType: String = "unknown",
    val isMetered: Boolean = false
)

// ===== 解析状态模型 =====
data class ParseState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val currentStep: String = "",
    val result: String? = null,
    val error: String? = null
)
