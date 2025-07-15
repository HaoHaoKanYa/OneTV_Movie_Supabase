package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// KotlinPoet专业重构 - 移除Hilt import
// import dagger.hilt.onetv.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.repository.VodRepository
// KotlinPoet专业重构 - 移除Inject import
// import javax.inject.Inject

/**
 * 详情页面ViewModel (参考OneMoVie架构)
 * KotlinPoet专业重构 - 使用MovieApp单例管理依赖
 */
// @HiltViewModel
class MovieDetailViewModel(
    private val repository: VodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /**
     * 加载电影详情
     */
    fun loadMovieDetail(vodId: String, siteKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. 获取详情信息
                val detailResult = repository.getContentDetail(vodId, siteKey)
                val movie = detailResult.getOrThrow()

                // 2. 解析播放源
                val playFlags = movie.parseFlags()

                // 3. 设置默认播放源和剧集
                val defaultFlag = playFlags.firstOrNull()
                val defaultEpisode = defaultFlag?.createEpisodes()?.firstOrNull()

                // 4. 加载相关推荐 (同类型内容)
                val relatedMovies = loadRelatedMovies(movie, siteKey)

                // 5. 检查收藏状态
                val isFavorite = checkFavoriteStatus(vodId, siteKey)

                // 6. 获取观看历史
                val watchHistory = getWatchHistory(vodId, siteKey)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movie = movie,
                    playFlags = playFlags,
                    currentFlag = defaultFlag,
                    currentEpisode = defaultEpisode,
                    isFavorite = isFavorite,
                    watchHistory = watchHistory,
                    relatedMovies = relatedMovies,
                    error = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "详情加载失败"
                )
            }
        }
    }

    /**
     * 选择播放源
     */
    fun selectFlag(flag: VodFlag) {
        val episodes = flag.createEpisodes()
        val defaultEpisode = episodes.firstOrNull()

        _uiState.value = _uiState.value.copy(
            currentFlag = flag,
            currentEpisode = defaultEpisode
        )
    }

    /**
     * 选择剧集
     */
    fun selectEpisode(episode: VodEpisode) {
        _uiState.value = _uiState.value.copy(
            currentEpisode = episode
        )
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            val currentMovie = _uiState.value.movie ?: return@launch
            val currentFavorite = _uiState.value.isFavorite

            try {
                if (currentFavorite) {
                    // 取消收藏
                    removeFavorite(currentMovie.vodId, currentMovie.siteKey)
                } else {
                    // 添加收藏
                    addFavorite(currentMovie)
                }

                _uiState.value = _uiState.value.copy(
                    isFavorite = !currentFavorite
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "收藏操作失败"
                )
            }
        }
    }

    /**
     * 开始播放
     */
    fun startPlay(episode: VodEpisode? = null) {
        val targetEpisode = episode ?: _uiState.value.currentEpisode
        val currentFlag = _uiState.value.currentFlag
        val currentMovie = _uiState.value.movie

        if (targetEpisode != null && currentFlag != null && currentMovie != null) {
            // 记录播放历史
            savePlayHistory(currentMovie, currentFlag, targetEpisode)
            
            // 更新当前剧集
            _uiState.value = _uiState.value.copy(
                currentEpisode = targetEpisode
            )
        }
    }

    /**
     * 加载相关推荐
     */
    private suspend fun loadRelatedMovies(movie: VodItem, siteKey: String): List<VodItem> {
        return try {
            // 根据类型获取相关内容
            val typeId = movie.typeId.toString()
            val result = repository.getContentList(
                typeId = typeId,
                page = 1,
                siteKey = siteKey
            )
            
            result.getOrNull()?.list
                ?.filter { it.vodId != movie.vodId } // 排除当前电影
                ?.take(10) // 取前10个
                ?: emptyList()
                
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 检查收藏状态
     */
    private suspend fun checkFavoriteStatus(vodId: String, siteKey: String): Boolean {
        return try {
            // TODO: 实现收藏状态检查
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取观看历史
     */
    private suspend fun getWatchHistory(vodId: String, siteKey: String): VodHistory? {
        return try {
            // TODO: 实现观看历史获取
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 添加收藏
     */
    private suspend fun addFavorite(movie: VodItem) {
        // TODO: 实现添加收藏
    }

    /**
     * 移除收藏
     */
    private suspend fun removeFavorite(vodId: String, siteKey: String) {
        // TODO: 实现移除收藏
    }

    /**
     * 保存播放历史
     */
    private fun savePlayHistory(movie: VodItem, flag: VodFlag, episode: VodEpisode) {
        viewModelScope.launch {
            try {
                // TODO: 实现播放历史保存
                val history = VodHistory(
                    vodId = movie.vodId,
                    vodName = movie.vodName,
                    vodPic = movie.vodPic,
                    siteKey = movie.siteKey,
                    episodeIndex = episode.index,
                    episodeName = episode.name,
                    position = 0L,
                    duration = 0L
                )
                
                // 保存到数据库
                // historyRepository.saveHistory(history)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取播放地址
     */
    fun getPlayUrl(): String? {
        val currentEpisode = _uiState.value.currentEpisode
        return currentEpisode?.url
    }

    /**
     * 获取下一集
     */
    fun getNextEpisode(): VodEpisode? {
        val currentFlag = _uiState.value.currentFlag ?: return null
        val currentEpisode = _uiState.value.currentEpisode ?: return null
        val episodes = currentFlag.createEpisodes()
        
        val currentIndex = episodes.indexOf(currentEpisode)
        return if (currentIndex >= 0 && currentIndex < episodes.size - 1) {
            episodes[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * 获取上一集
     */
    fun getPreviousEpisode(): VodEpisode? {
        val currentFlag = _uiState.value.currentFlag ?: return null
        val currentEpisode = _uiState.value.currentEpisode ?: return null
        val episodes = currentFlag.createEpisodes()
        
        val currentIndex = episodes.indexOf(currentEpisode)
        return if (currentIndex > 0) {
            episodes[currentIndex - 1]
        } else {
            null
        }
    }
}
