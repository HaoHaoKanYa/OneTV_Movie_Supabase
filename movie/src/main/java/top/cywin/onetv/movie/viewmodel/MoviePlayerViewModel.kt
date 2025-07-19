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
 * 线路信息 (替代LineManager.LineInfo)
 */
data class LineInfo(
    val flag: String = "",
    val quality: String = "",
    val speed: String = "",
    val isAvailable: Boolean = true
)

/**
 * 播放器UI状态数据类 - 完整版本
 */
data class PlayerUiState(
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
    val currentEpisodeIndex: Int = 0,

    // 播放状态
    val playUrl: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,

    // UI控制
    val showControls: Boolean = true,
    val isFullscreen: Boolean = false,
    val showSpeedSelector: Boolean = false,
    val showQualitySelector: Boolean = false,

    // 线路信息
    val availableLines: List<LineInfo> = emptyList(),
    val currentLineIndex: Int = 0,

    // 播放历史
    val watchHistory: WatchHistory? = null
)

/**
 * OneTV Movie播放器ViewModel - 完整版本
 * 处理播放器状态、播放地址解析、播放历史等完整功能
 */
class MoviePlayerViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_PLAYER_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MoviePlayerViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MoviePlayerViewModel 清理")

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
     * 监听播放地址解析事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayUrlParse(event: PlayUrlParseEvent) {
        Log.d(TAG, "📡 收到播放地址解析事件: url=${event.playUrl}")

        if (!event.playUrl.isNullOrEmpty()) {
            handlePlayUrlParseSuccess(event.playUrl, event.headers)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "播放地址解析失败"
            )
        }
    }

    /**
     * 监听WebView解析事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebViewParseSuccess(event: WebViewParseSuccessEvent) {
        Log.d(TAG, "📡 收到WebView解析成功事件: url=${event.playUrl}")

        handlePlayUrlParseSuccess(event.playUrl, event.headers)
    }

    /**
     * 监听WebView解析错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebViewParseError(event: WebViewParseErrorEvent) {
        Log.e(TAG, "📡 收到WebView解析错误事件: ${event.error}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "WebView解析失败: ${event.error}"
        )
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
     * 初始化播放器
     */
    fun initPlayer(vodId: String, siteKey: String, episodeIndex: Int = 0) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "▶️ 初始化播放器: vodId=$vodId, episodeIndex=$episodeIndex")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    vodId = vodId,
                    siteKey = siteKey,
                    currentEpisodeIndex = episodeIndex
                )

                // ✅ 通过适配器获取影片详情
                repositoryAdapter.getContentDetail(vodId, siteKey)

                // 详情获取后会通过事件处理播放初始化

            } catch (e: Exception) {
                Log.e(TAG, "💥 播放器初始化失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "播放器初始化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 播放指定剧集
     */
    fun playEpisode(episode: Episode, episodeIndex: Int = 0) {
        val currentState = _uiState.value
        val movie = currentState.movie
        val currentFlag = currentState.currentFlag

        if (movie == null || currentFlag == null) {
            _uiState.value = _uiState.value.copy(error = "播放器未初始化")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "📺 播放剧集: ${episode.name}")

                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    currentEpisode = episode,
                    currentEpisodeIndex = episodeIndex
                )

                // ✅ 通过适配器解析播放地址
                repositoryAdapter.parsePlayUrl(episode.url, movie.siteKey, currentFlag.flag)

                // 播放地址将通过事件回调处理

            } catch (e: Exception) {
                Log.e(TAG, "💥 剧集播放失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "剧集播放失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 播放下一集
     */
    fun playNextEpisode() {
        val currentState = _uiState.value
        val episodes = currentState.episodes
        val currentIndex = currentState.currentEpisodeIndex

        if (currentIndex < episodes.size - 1) {
            val nextEpisode = episodes[currentIndex + 1]
            playEpisode(nextEpisode, currentIndex + 1)
            Log.d("ONETV_MOVIE", "⏭️ 播放下一集: ${nextEpisode.name}")
        } else {
            Log.d("ONETV_MOVIE", "⚠️ 已经是最后一集")
        }
    }

    /**
     * 播放上一集
     */
    fun playPreviousEpisode() {
        val currentState = _uiState.value
        val episodes = currentState.episodes
        val currentIndex = currentState.currentEpisodeIndex

        if (currentIndex > 0) {
            val previousEpisode = episodes[currentIndex - 1]
            playEpisode(previousEpisode, currentIndex - 1)
            Log.d("ONETV_MOVIE", "⏮️ 播放上一集: ${previousEpisode.name}")
        } else {
            Log.d("ONETV_MOVIE", "⚠️ 已经是第一集")
        }
    }

    /**
     * 选择播放源
     */
    fun selectFlag(flag: Flag) {
        Log.d("ONETV_MOVIE", "🎬 选择播放源: ${flag.flag}")

        // 解析剧集列表
        val episodes = parseEpisodes(flag.urls)
        val defaultEpisode = episodes.firstOrNull()

        if (defaultEpisode != null) {
            _uiState.value = _uiState.value.copy(
                currentFlag = flag,
                episodes = episodes,
                currentEpisode = defaultEpisode,
                currentEpisodeIndex = 0
            )

            // 播放第一集
            playEpisode(defaultEpisode, 0)
        }
    }

    /**
     * 解析剧集列表
     */
    private fun parseEpisodes(urls: String): List<Episode> {
        return try {
            urls.split("#").mapIndexed { index, episodeData ->
                val parts = episodeData.split("$")
                Episode.create(
                    if (parts.size >= 2) parts[0] else "第${index + 1}集",
                    if (parts.size >= 2) parts[1] else episodeData
                ).apply {
                    setIndex(index)
                }
            }
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "剧集解析失败", e)
            emptyList()
        }
    }

    /**
     * 选择剧集
     */
    fun selectEpisode(episode: Episode) {
        val currentState = _uiState.value
        val episodes = currentState.episodes
        val episodeIndex = episodes.indexOf(episode)

        if (episodeIndex >= 0) {
            playEpisode(episode, episodeIndex)
        }
    }

    /**
     * 更新播放进度
     */
    fun updatePlayProgress(position: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(
            currentPosition = position,
            duration = duration
        )

        // ✅ 通过适配器保存播放历史 - 历史管理在FongMi_TV中
        val currentState = _uiState.value
        val movie = currentState.movie
        val episode = currentState.currentEpisode

        if (movie != null && episode != null) {
            repositoryAdapter.savePlayHistory(
                movie.getVodId(),
                movie.getSite()?.getKey() ?: "",
                episode.getIndex(),
                position
            )
        }
    }


    /**
     * 切换到指定线路
     */
    fun switchToLine(lineInfo: LineInfo) {
        Log.d("ONETV_MOVIE", "🔄 切换线路: ${lineInfo.flag}")

        val currentState = _uiState.value
        val currentEpisode = currentState.currentEpisode

        if (currentEpisode != null) {
            // ✅ 通过适配器切换线路 - 线路管理在FongMi_TV中
            repositoryAdapter.switchLine(lineInfo.flag, currentEpisode.url)

            val lineIndex = currentState.availableLines.indexOf(lineInfo)
            _uiState.value = _uiState.value.copy(
                currentLineIndex = lineIndex.coerceAtLeast(0)
            )
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 设置播放状态
     */
    fun setPlayingState(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    /**
     * 更新播放进度
     */
    fun updatePlaybackPosition(position: Long, duration: Long) {
        _uiState.value = _uiState.value.copy(
            currentPosition = position,
            duration = duration
        )

        // ✅ 保存播放历史
        savePlaybackHistory(position, duration)
    }

    /**
     * WebView解析成功回调
     */
    fun onWebViewParseSuccess(playUrl: String) {
        Log.d(TAG, "✅ WebView解析成功: $playUrl")
        handlePlayUrlParseSuccess(playUrl, null)
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

            // ✅ 根据指定的剧集索引选择剧集
            val currentState = _uiState.value
            val targetEpisodeIndex = currentState.currentEpisodeIndex.coerceIn(0, episodes.size - 1)
            val targetEpisode = episodes.getOrNull(targetEpisodeIndex) ?: episodes.first()

            // ✅ 更新UI状态
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                movie = movieItem,
                playFlags = playFlags,
                currentFlag = defaultFlag,
                episodes = episodes,
                currentEpisode = targetEpisode,
                currentEpisodeIndex = targetEpisodeIndex,
                error = null
            )

            // ✅ 自动开始播放指定剧集
            playEpisode(targetEpisode, targetEpisodeIndex)

        } catch (e: Exception) {
            Log.e(TAG, "💥 详情处理失败", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "详情处理失败: ${e.message}"
            )
        }
    }

    /**
     * 处理播放地址解析成功
     */
    private fun handlePlayUrlParseSuccess(playUrl: String, headers: Map<String, String>?) {
        Log.d(TAG, "✅ 播放地址解析成功: $playUrl")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            playUrl = playUrl,
            error = null
        )
    }

    /**
     * 保存播放历史
     */
    private fun savePlaybackHistory(position: Long, duration: Long) {
        val currentState = _uiState.value
        val movie = currentState.movie
        val episode = currentState.currentEpisode

        if (movie != null && episode != null && duration > 0) {
            viewModelScope.launch {
                try {
                    repositoryAdapter.saveWatchHistory(
                        movie.vodId,
                        movie.vodName,
                        position,
                        duration
                    )
                } catch (e: Exception) {
                    // 历史保存失败不影响播放
                    Log.e(TAG, "💥 保存播放历史失败", e)
                }
            }
        }
    }
}
