package top.cywin.onetv.movie.data.models

/**
 * Movie UI状态数据类
 */

/**
 * 首页分类区域 (动态配置)
 */
data class HomeCategorySection(
    val categoryId: String, // 分类ID
    val categoryName: String, // 分类名称 (从配置获取)
    val movies: List<VodItem>, // 该分类下的内容
    val siteKey: String = "" // 所属站点
)

/**
 * 首页UI状态 (支持动态分类)
 */
data class MovieUiState(
    val isLoading: Boolean = false,
    val recommendMovies: List<VodItem> = emptyList(), // 推荐内容
    val quickCategories: List<VodClass> = emptyList(), // 快速导航分类
    val homeCategories: List<HomeCategorySection> = emptyList(), // 首页分类区域
    val currentSite: VodSite? = null, // 当前站点
    val availableSites: List<VodSite> = emptyList(), // 可用站点列表
    val error: String? = null
)

/**
 * 分类页面UI状态
 */
data class CategoryUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val movies: List<VodItem> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val currentCategory: VodClass? = null,
    val availableCategories: List<VodClass> = emptyList(),
    val filters: Map<String, List<VodFilter>> = emptyMap(),
    val selectedFilters: Map<String, String> = emptyMap(),
    val error: String? = null
)

/**
 * 详情页面UI状态
 */
data class DetailUiState(
    val isLoading: Boolean = false,
    val movie: VodItem? = null,
    val playFlags: List<VodFlag> = emptyList(),
    val currentFlag: VodFlag? = null,
    val currentEpisode: VodEpisode? = null,
    val isFavorite: Boolean = false,
    val watchHistory: VodHistory? = null,
    val relatedMovies: List<VodItem> = emptyList(),
    val error: String? = null
)

/**
 * 搜索页面UI状态
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val keyword: String = "",
    val searchResults: List<VodItem> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val hotKeywords: List<String> = emptyList(),
    val error: String? = null
)

/**
 * 历史记录页面UI状态
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val watchHistory: List<VodHistory> = emptyList(),
    val favorites: List<VodItem> = emptyList(),
    val selectedTab: Int = 0, // 0: 观看历史, 1: 收藏
    val error: String? = null
)

/**
 * 播放器UI状态
 */
data class PlayerUiState(
    val isLoading: Boolean = false,
    val movie: VodItem? = null,
    val playFlags: List<VodFlag> = emptyList(),
    val currentFlag: VodFlag? = null,
    val currentEpisode: VodEpisode? = null,
    val playUrl: String = "",
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isControlsVisible: Boolean = true,
    val isFullscreen: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null
)

/**
 * 设置页面UI状态
 */
data class SettingsUiState(
    val configs: List<VodConfig> = emptyList(),
    val currentConfig: VodConfig? = null,
    val isLoading: Boolean = false,
    val cacheSize: Long = 0L,
    val playbackSettings: PlaybackSettings = PlaybackSettings(),
    val error: String? = null
)

/**
 * 播放设置
 */
data class PlaybackSettings(
    val autoPlay: Boolean = true,
    val autoNext: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val skipIntro: Boolean = false,
    val skipOutro: Boolean = false,
    val rememberPosition: Boolean = true,
    val hardwareDecoding: Boolean = true
)
