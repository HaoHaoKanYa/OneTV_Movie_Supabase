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
 * 详情页UI状态数据类
 */
data class DetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val movie: Vod? = null,
    val flags: List<Flag> = emptyList(),
    val selectedFlag: Flag? = null,
    val episodes: List<Episode> = emptyList(),
    val selectedEpisode: Episode? = null,
    val isFavorite: Boolean = false,
    val showFlagSelector: Boolean = false,
    val showEpisodeSelector: Boolean = false
)

/**
 * OneTV Movie详情页ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MovieDetailViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /**
     * 加载电影详情 - 通过适配器调用FongMi_TV解析系统
     */
    fun loadMovieDetail(vodId: String, siteKey: String = "") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "📺 开始加载详情: vodId=$vodId")

                // ✅ 通过适配器获取详情 - 解析逻辑在FongMi_TV中
                repositoryAdapter.getContentDetail(vodId, siteKey)

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 详情请求已发送")

                // 检查收藏状态
                val isFavorite = repositoryAdapter.isFavorite(vodId, siteKey)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isFavorite = isFavorite,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "详情加载失败", e)
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
        viewModelScope.launch {
            val currentState = _uiState.value
            val vodId = currentState.movie?.vodId ?: return@launch
            val siteKey = currentState.movie?.getSite()?.getKey() ?: ""

            try {
                if (currentState.isFavorite) {
                    // ✅ 通过适配器移除收藏 - 收藏逻辑在FongMi_TV中
                    repositoryAdapter.removeFromFavorites(vodId, siteKey)
                    Log.d("ONETV_MOVIE", "✅ 移除收藏请求已发送")
                } else {
                    // ✅ 通过适配器添加收藏 - 收藏逻辑在FongMi_TV中
                    repositoryAdapter.addToFavorites(vodId, siteKey)
                    Log.d("ONETV_MOVIE", "✅ 添加收藏请求已发送")
                }

                _uiState.value = _uiState.value.copy(
                    isFavorite = !currentState.isFavorite
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "收藏操作失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "收藏操作失败: ${e.message}"
                )
            }
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

        _uiState.value = _uiState.value.copy(
            selectedFlag = flag,
            episodes = episodes,
            selectedEpisode = defaultEpisode,
            showFlagSelector = false
        )
    }

    /**
     * 选择剧集
     */
    fun selectEpisode(episode: Episode) {
        Log.d("ONETV_MOVIE", "📺 选择剧集: ${episode.name}")
        _uiState.value = _uiState.value.copy(
            selectedEpisode = episode,
            showEpisodeSelector = false
        )
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
     * 显示播放源选择器
     */
    fun showFlagSelector() {
        _uiState.value = _uiState.value.copy(showFlagSelector = true)
    }

    /**
     * 隐藏播放源选择器
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


}
