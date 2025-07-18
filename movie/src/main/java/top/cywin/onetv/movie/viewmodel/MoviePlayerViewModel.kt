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
 * 播放器UI状态数据类
 */
data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val movie: Vod? = null,
    val playFlags: List<Flag> = emptyList(),
    val currentFlag: Flag? = null,
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val currentEpisodeIndex: Int = 0,
    val availableLines: List<LineInfo> = emptyList(),
    val currentLineIndex: Int = 0,
    val playUrl: String = "",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
)

/**
 * OneTV Movie播放器ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MoviePlayerViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * 初始化播放器 - 通过适配器调用FongMi_TV解析系统
     */
    fun initPlayer(vodId: String, siteKey: String, episodeIndex: Int = 0) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "▶️ 初始化播放器: vodId=$vodId, episodeIndex=$episodeIndex")

                // ✅ 通过适配器获取影片详情 - 解析逻辑在FongMi_TV中
                repositoryAdapter.getContentDetail(vodId, siteKey)

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 详情请求已发送，等待SiteViewModel响应")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentEpisodeIndex = episodeIndex,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "播放器初始化失败", e)
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
    fun playEpisode(episode: Episode, episodeIndex: Int) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "📺 播放剧集: ${episode.name}")

                val currentState = _uiState.value
                val movie = currentState.movie
                val currentFlag = currentState.currentFlag

                if (movie == null || currentFlag == null) {
                    _uiState.value = _uiState.value.copy(error = "播放器未初始化")
                    return@launch
                }

                // ✅ 通过适配器解析播放地址 - 解析逻辑在FongMi_TV中
                repositoryAdapter.parsePlayUrl(episode.url, movie.getSite()?.getKey() ?: "")

                _uiState.value = _uiState.value.copy(
                    currentEpisode = episode,
                    currentEpisodeIndex = episodeIndex,
                    error = null
                )

                Log.d("ONETV_MOVIE", "✅ 播放地址解析请求已发送")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "剧集播放失败", e)
                _uiState.value = _uiState.value.copy(
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
}
