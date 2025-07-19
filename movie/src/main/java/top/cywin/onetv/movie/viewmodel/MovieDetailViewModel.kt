package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Flag
import top.cywin.onetv.movie.bean.Episode
import android.util.Log

// ✅ 添加EventBus支持
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.event.*
import top.cywin.onetv.movie.ui.model.*
import top.cywin.onetv.movie.adapter.ViewModelAdapter

/**
 * 详情页UI状态数据类 - 完整版本
 */
data class DetailUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,
    val vodId: String = "",
    val siteKey: String = "",

    // 影片信息
    val movie: MovieItem? = null,

    // 播放相关
    val playFlags: List<PlayFlag> = emptyList(),
    val currentFlag: PlayFlag? = null,
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,

    // 用户状态
    val isFavorite: Boolean = false,
    val watchHistory: WatchHistory? = null,

    // UI控制
    val showFlagSelector: Boolean = false,
    val showEpisodeSelector: Boolean = false,
    val showMoreInfo: Boolean = false,

    // 相关推荐
    val relatedMovies: List<MovieItem> = emptyList(),
    val isLoadingRelated: Boolean = false
)

/**
 * OneTV Movie详情页ViewModel - 完整版本
 * 处理影片详情、播放源、剧集等完整功能
 */
class MovieDetailViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_DETAIL_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MovieDetailViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MovieDetailViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }

    // ===== EventBus事件监听 =====

    /**
     * 监听内容详情事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onContentDetail(event: ContentDetailEvent) {
        Log.d(TAG, "📡 收到内容详情事件: success=${event.isSuccess}")

        if (event.vod != null && event.isSuccess) {
            handleContentDetailSuccess(event.vod)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = event.errorMessage ?: "获取详情失败"
            )
        }
    }

    /**
     * 监听收藏更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFavoriteUpdate(event: FavoriteUpdateEvent) {
        Log.d(TAG, "📡 收到收藏更新事件: vodId=${event.vodId}, favorite=${event.isFavorite}")

        if (event.isSuccess) {
            _uiState.value = _uiState.value.copy(
                isFavorite = event.isFavorite
            )
        }
    }

    /**
     * 监听历史更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHistoryUpdate(event: HistoryUpdateEvent) {
        Log.d(TAG, "📡 收到历史更新事件: vodId=${event.vodId}")

        if (event.isSuccess) {
            val watchHistory = WatchHistory(
                vodId = event.vodId,
                vodName = _uiState.value.movie?.vodName ?: "",
                siteKey = _uiState.value.movie?.siteKey ?: "",
                episodeName = _uiState.value.currentEpisode?.name ?: "",
                position = event.position,
                duration = event.duration,
                watchTime = System.currentTimeMillis(),
                isCompleted = event.position >= event.duration * 0.9
            )

            _uiState.value = _uiState.value.copy(
                watchHistory = watchHistory
            )
        }
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 加载影片详情
     */
    fun loadMovieDetail(vodId: String, siteKey: String = "") {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎬 开始加载影片详情: vodId=$vodId, siteKey=$siteKey")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    vodId = vodId,
                    siteKey = siteKey
                )

                // ✅ 通过适配器获取影片详情
                repositoryAdapter.getContentDetail(vodId, siteKey)

                // ✅ 同时检查收藏状态和观看历史
                checkFavoriteAndHistory(vodId, siteKey)

            } catch (e: Exception) {
                Log.e(TAG, "💥 影片详情加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "详情加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val currentState = _uiState.value
        val movie = currentState.movie ?: return

        viewModelScope.launch {
            try {
                val newFavoriteState = !currentState.isFavorite

                // ✅ 乐观更新UI
                _uiState.value = _uiState.value.copy(isFavorite = newFavoriteState)

                // ✅ 通过适配器更新收藏状态
                if (newFavoriteState) {
                    // 需要转换为FongMi_TV的Vod对象
                    val fongmiVod = convertToFongMiVod(movie)
                    repositoryAdapter.addToFavorites(fongmiVod)
                } else {
                    repositoryAdapter.removeFromFavorites(movie.vodId, movie.siteKey)
                }

                Log.d(TAG, "✅ 收藏状态更新: ${if (newFavoriteState) "已收藏" else "已取消收藏"}")

            } catch (e: Exception) {
                Log.e(TAG, "💥 收藏操作失败", e)
                // 回滚UI状态
                _uiState.value = _uiState.value.copy(isFavorite = currentState.isFavorite)
            }
        }
    }

    /**
     * 选择播放线路
     */
    fun selectFlag(flag: PlayFlag) {
        Log.d(TAG, "🔄 选择播放线路: ${flag.flag}")

        val episodes = ViewModelAdapter.convertVodEpisodes(flag.urls)
        val defaultEpisode = episodes.firstOrNull()

        _uiState.value = _uiState.value.copy(
            currentFlag = flag,
            episodes = episodes,
            currentEpisode = defaultEpisode,
            showFlagSelector = false
        )
    }

    /**
     * 选择剧集
     */
    fun selectEpisode(episode: Episode) {
        Log.d(TAG, "🔄 选择剧集: ${episode.name}")

        _uiState.value = _uiState.value.copy(
            currentEpisode = episode,
            showEpisodeSelector = false
        )
    }

    /**
     * 显示线路选择器
     */
    fun showFlagSelector() {
        _uiState.value = _uiState.value.copy(showFlagSelector = true)
    }

    /**
     * 隐藏线路选择器
     */
    fun hideFlagSelector() {
        _uiState.value = _uiState.value.copy(showFlagSelector = false)
    }

    /**
     * 显示剧集选择器
     */
    fun showEpisodeSelector() {
        _uiState.value = _uiState.value.copy(showEpisodeSelector = true)
    }

    /**
     * 隐藏剧集选择器
     */
    fun hideEpisodeSelector() {
        _uiState.value = _uiState.value.copy(showEpisodeSelector = false)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ===== 私有方法 =====

    /**
     * 处理内容详情成功
     */
    private fun handleContentDetailSuccess(vod: top.cywin.onetv.movie.bean.Vod) {
        Log.d(TAG, "✅ 处理内容详情成功: ${vod.vodName}")

        try {
            // ✅ 转换为UI模型
            val movieItem = ViewModelAdapter.convertVodToMovie(vod)
            if (movieItem == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "数据转换失败"
                )
                return
            }

            // ✅ 解析播放源
            val playFlags = ViewModelAdapter.convertVodFlags(vod)
            if (playFlags.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "没有找到播放源"
                )
                return
            }

            // ✅ 选择默认播放源和剧集
            val defaultFlag = playFlags.firstOrNull()
            val episodes = if (defaultFlag != null) {
                ViewModelAdapter.convertVodEpisodes(defaultFlag.urls)
            } else {
                emptyList()
            }

            if (episodes.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "没有找到可播放的剧集"
                )
                return
            }

            val targetEpisode = episodes.firstOrNull()
            if (targetEpisode == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "剧集信息无效"
                )
                return
            }

            // ✅ 更新UI状态
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                movie = movieItem,
                playFlags = playFlags,
                currentFlag = defaultFlag,
                episodes = episodes,
                currentEpisode = targetEpisode,
                error = null
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 详情处理失败", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "详情处理失败: ${e.message}"
            )
        }
    }

    /**
     * 检查收藏状态和观看历史
     */
    private suspend fun checkFavoriteAndHistory(vodId: String, siteKey: String) {
        try {
            // ✅ 检查收藏状态
            val isFavorite = repositoryAdapter.isFavorite(vodId, siteKey)

            // ✅ 获取观看历史
            val history = repositoryAdapter.getWatchHistory(vodId, siteKey)
            val watchHistory = if (history != null) {
                WatchHistory(
                    vodId = history.vodId,
                    vodName = history.vodName,
                    siteKey = siteKey,
                    episodeName = history.vodRemarks ?: "",
                    position = history.position,
                    duration = history.duration,
                    watchTime = history.createTime,
                    isCompleted = history.position >= history.duration * 0.9
                )
            } else null

            // ✅ 更新UI状态
            _uiState.value = _uiState.value.copy(
                isFavorite = isFavorite,
                watchHistory = watchHistory
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 检查收藏和历史失败", e)
        }
    }

    /**
     * 转换为FongMi_TV的Vod对象
     */
    private fun convertToFongMiVod(movie: MovieItem): top.cywin.onetv.movie.bean.Vod {
        val vod = top.cywin.onetv.movie.bean.Vod()
        vod.vodId = movie.vodId
        vod.vodName = movie.vodName
        vod.vodPic = movie.vodPic
        vod.vodRemarks = movie.vodRemarks
        vod.vodYear = movie.vodYear
        vod.vodArea = movie.vodArea
        vod.vodDirector = movie.vodDirector
        vod.vodActor = movie.vodActor
        vod.vodContent = movie.vodContent

        // 设置站点信息
        val site = repositoryAdapter.getCurrentSite()
        vod.site = site

        return vod
    }

    /**
     * 检查收藏状态 - 辅助方法
     */
    private suspend fun checkFavoriteStatus(vodId: String, siteKey: String): Boolean {
        return try {
            repositoryAdapter.isFavorite(vodId, siteKey)
        } catch (e: Exception) {
            Log.e(TAG, "💥 检查收藏状态失败", e)
            false
        }
    }

    /**
     * 获取观看历史 - 辅助方法
     */
    private suspend fun getWatchHistory(vodId: String, siteKey: String): WatchHistory? {
        return try {
            val history = repositoryAdapter.getWatchHistory(vodId, siteKey)
            if (history != null) {
                WatchHistory(
                    vodId = history.vodId,
                    vodName = history.vodName,
                    siteKey = siteKey,
                    episodeName = history.vodRemarks ?: "",
                    position = history.position,
                    duration = history.duration,
                    watchTime = history.createTime,
                    isCompleted = history.position >= history.duration * 0.9
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "💥 获取观看历史失败", e)
            null
        }
    }
}
