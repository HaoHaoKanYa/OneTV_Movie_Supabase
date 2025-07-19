package top.cywin.onetv.movie.event

/**
 * UI事件系统 - 用于FongMi_TV解析引擎与Compose UI的通信
 * 完整的事件定义，支持所有UI交互场景
 */

// ===== 导航事件 =====
data class NavigationEvent(
    val action: String,
    val params: Map<String, String> = emptyMap()
)

// ===== 错误事件 =====
data class ErrorEvent(
    val message: String,
    val throwable: Throwable? = null,
    val errorCode: String? = null
)

// ===== 配置相关事件 =====
data class ConfigUpdateEvent(
    val config: top.cywin.onetv.movie.api.config.VodConfig?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class ConfigLoadEvent(
    val isLoading: Boolean,
    val progress: Float = 0f,
    val message: String = ""
)

// ===== 搜索相关事件 =====
data class SearchResultEvent(
    val results: List<top.cywin.onetv.movie.bean.Vod>,
    val keyword: String,
    val hasMore: Boolean,
    val page: Int = 1,
    val total: Int = 0
)

data class SearchStartEvent(
    val keyword: String,
    val siteKey: String? = null
)

data class SearchErrorEvent(
    val keyword: String,
    val error: String
)

// ===== 内容相关事件 =====
data class ContentDetailEvent(
    val vod: top.cywin.onetv.movie.bean.Vod?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class CategoryContentEvent(
    val vods: List<top.cywin.onetv.movie.bean.Vod>,
    val typeId: String,
    val page: Int,
    val hasMore: Boolean,
    val total: Int = 0
)

data class HomeContentEvent(
    val categories: List<top.cywin.onetv.movie.bean.Class>,
    val recommendVods: List<top.cywin.onetv.movie.bean.Vod>,
    val isSuccess: Boolean
)

// ===== 播放相关事件 =====
data class PlayUrlParseEvent(
    val playUrl: String?,
    val headers: Map<String, String>?,
    val vodId: String? = null,
    val episodeIndex: Int = 0,
    val flag: String? = null
)

data class PlayUrlParseStartEvent(
    val vodId: String,
    val episodeUrl: String,
    val flag: String
)

data class PlayUrlParseErrorEvent(
    val vodId: String,
    val error: String
)

// ===== WebView解析事件 =====
data class WebViewParseEvent(
    val request: WebViewParseRequest
)

data class WebViewParseRequest(
    val key: String,
    val from: String,
    val headers: Map<String, String>,
    val url: String,
    val click: String,
    val callback: top.cywin.onetv.movie.impl.ParseCallback?,
    val isPlayerUrl: Boolean = false
)

data class WebViewParseSuccessEvent(
    val playUrl: String,
    val headers: Map<String, String>? = null
)

data class WebViewParseErrorEvent(
    val error: String
)

// ===== 收藏和历史事件 =====
data class FavoriteUpdateEvent(
    val vodId: String,
    val isFavorite: Boolean,
    val isSuccess: Boolean
)

data class HistoryUpdateEvent(
    val vodId: String,
    val position: Long,
    val duration: Long,
    val isSuccess: Boolean
)

data class HistoryLoadEvent(
    val histories: List<Any>, // History对象列表
    val isSuccess: Boolean
)

// ===== 站点相关事件 =====
data class SiteChangeEvent(
    val site: top.cywin.onetv.movie.bean.Site?,
    val isSuccess: Boolean
)

data class SiteListUpdateEvent(
    val sites: List<top.cywin.onetv.movie.bean.Site>,
    val currentSite: top.cywin.onetv.movie.bean.Site?
)

// ===== 云盘相关事件 =====
data class CloudDriveEvent(
    val driveId: String,
    val files: List<Any>, // CloudFile对象列表
    val path: String,
    val isSuccess: Boolean
)

data class CloudDriveConfigEvent(
    val configs: List<Any>, // CloudDriveConfig对象列表
    val isSuccess: Boolean
)

// ===== 直播相关事件 =====
data class LiveChannelEvent(
    val channels: List<Any>, // Live对象列表
    val group: String,
    val isSuccess: Boolean
)

data class LivePlayEvent(
    val playUrl: String,
    val channelName: String,
    val isSuccess: Boolean
)

// ===== 设置相关事件 =====
data class SettingsUpdateEvent(
    val key: String,
    val value: Any,
    val isSuccess: Boolean
)

// ===== 网络相关事件 =====
data class NetworkStatusEvent(
    val isConnected: Boolean,
    val networkType: String
)

data class ApiTestEvent(
    val url: String,
    val isSuccess: Boolean,
    val responseTime: Long = 0,
    val errorMessage: String? = null
)

// ===== 解析相关事件 =====
data class ParseProgressEvent(
    val progress: Float,
    val message: String,
    val currentStep: String
)

data class ParseCompleteEvent(
    val isSuccess: Boolean,
    val result: Any? = null,
    val errorMessage: String? = null
)

// ===== UI状态事件 =====
data class LoadingStateEvent(
    val isLoading: Boolean,
    val message: String = "",
    val progress: Float = 0f
)

data class RefreshEvent(
    val type: String, // "home", "category", "search", "detail", "player"
    val params: Map<String, String> = emptyMap()
)

// ===== 系统事件 =====
data class SystemEvent(
    val type: String, // "memory_low", "storage_low", "network_changed"
    val data: Map<String, Any> = emptyMap()
)
