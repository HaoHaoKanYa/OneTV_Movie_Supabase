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
import top.cywin.onetv.movie.data.repository.WatchHistoryRepository
import top.cywin.onetv.movie.MovieApp
// KotlinPoet专业重构 - 移除Inject import
// import javax.inject.Inject

/**
 * 播放器ViewModel (参考OneMoVie播放器架构)
 * KotlinPoet专业重构 - 移除Hilt注解，使用标准构造函数
 */
// @HiltViewModel
class MoviePlayerViewModel(
    private val historyRepository: WatchHistoryRepository
) : ViewModel() {

    // 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel
    private val vodConfig = movieApp.vodConfig
    private val uiAdapter = movieApp.uiAdapter

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * 加载播放数据
     */
    fun loadPlayData(vodId: String, episodeIndex: Int, siteKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. 获取影片详情
                val detailResult = vodRepository.getContentDetail(vodId, siteKey)
                val movie = detailResult.getOrThrow()

                // 2. 解析播放源
                val playFlags = movie.parseFlags()
                if (playFlags.isEmpty()) {
                    throw Exception("没有找到播放源")
                }

                // 3. 选择默认播放源和剧集
                val defaultFlag = playFlags.firstOrNull()
                val episodes = defaultFlag?.createEpisodes() ?: emptyList()
                
                if (episodes.isEmpty()) {
                    throw Exception("没有找到剧集")
                }

                val targetEpisode = episodes.getOrNull(episodeIndex) ?: episodes.first()

                // 4. 获取可用线路
                val availableParsers = vodRepository.getAvailableParsers(siteKey)
                val availableLines = lineManager.getAvailableLines(movie, availableParsers)

                // 5. 选择最佳线路
                val lineSelection = lineManager.selectBestLine(movie, availableParsers)
                val playUrl = lineSelection?.playUrl ?: targetEpisode.url

                // 6. 保存播放历史
                if (defaultFlag != null) {
                    historyRepository.saveHistory(movie, defaultFlag, targetEpisode)
                }

                // 7. 更新UI状态
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movie = movie,
                    playFlags = playFlags,
                    currentFlag = defaultFlag,
                    currentEpisode = targetEpisode,
                    episodes = episodes,
                    currentEpisodeIndex = episodeIndex.coerceIn(0, episodes.size - 1),
                    availableLines = availableLines,
                    currentLineIndex = 0,
                    playUrl = playUrl,
                    error = null
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 选择播放源
     */
    fun selectFlag(flag: VodFlag) {
        val currentState = _uiState.value
        val episodes = flag.createEpisodes()
        val defaultEpisode = episodes.firstOrNull()

        if (defaultEpisode != null) {
            viewModelScope.launch {
                try {
                    val playUrl = parsePlayUrl(defaultEpisode.url, currentState.movie?.siteKey ?: "", flag.flag)
                    
                    _uiState.value = _uiState.value.copy(
                        currentFlag = flag,
                        currentEpisode = defaultEpisode,
                        playUrl = playUrl
                    )

                    // 更新播放历史
                    currentState.movie?.let { movie ->
                        historyRepository.updateEpisode(
                            vodId = movie.vodId,
                            siteKey = movie.siteKey,
                            episode = defaultEpisode,
                            flagName = flag.flag
                        )
                    }

                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "播放源切换失败"
                    )
                }
            }
        }
    }

    /**
     * 选择剧集
     */
    fun selectEpisode(episode: VodEpisode) {
        val currentState = _uiState.value
        val currentFlag = currentState.currentFlag

        if (currentFlag != null) {
            viewModelScope.launch {
                try {
                    val playUrl = parsePlayUrl(episode.url, currentState.movie?.siteKey ?: "", currentFlag.flag)
                    
                    _uiState.value = _uiState.value.copy(
                        currentEpisode = episode,
                        playUrl = playUrl
                    )

                    // 更新播放历史
                    currentState.movie?.let { movie ->
                        historyRepository.updateEpisode(
                            vodId = movie.vodId,
                            siteKey = movie.siteKey,
                            episode = episode,
                            flagName = currentFlag.flag
                        )
                    }

                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "剧集切换失败"
                    )
                }
            }
        }
    }

    /**
     * 播放下一集
     */
    fun playNextEpisode() {
        val currentState = _uiState.value
        val currentFlag = currentState.currentFlag
        val currentEpisode = currentState.currentEpisode

        if (currentFlag != null && currentEpisode != null) {
            val episodes = currentFlag.createEpisodes()
            val currentIndex = episodes.indexOf(currentEpisode)
            
            if (currentIndex >= 0 && currentIndex < episodes.size - 1) {
                selectEpisode(episodes[currentIndex + 1])
            }
        }
    }

    /**
     * 播放上一集
     */
    fun playPreviousEpisode() {
        val currentState = _uiState.value
        val currentFlag = currentState.currentFlag
        val currentEpisode = currentState.currentEpisode

        if (currentFlag != null && currentEpisode != null) {
            val episodes = currentFlag.createEpisodes()
            val currentIndex = episodes.indexOf(currentEpisode)
            
            if (currentIndex > 0) {
                selectEpisode(episodes[currentIndex - 1])
            }
        }
    }

    /**
     * 更新播放进度
     */
    fun updatePlayProgress(position: Long, duration: Long) {
        val currentState = _uiState.value
        val movie = currentState.movie

        if (movie != null) {
            viewModelScope.launch {
                historyRepository.updateProgress(
                    vodId = movie.vodId,
                    siteKey = movie.siteKey,
                    position = position,
                    duration = duration
                )
            }
        }
    }

    /**
     * 解析播放地址 (使用真实的解析器系统)
     */
    private suspend fun parsePlayUrl(url: String, siteKey: String, flag: String): String {
        return try {
            val result = vodRepository.parsePlayUrl(url, siteKey, flag)
            result.getOrThrow()
        } catch (e: Exception) {
            throw Exception("播放地址解析失败: ${e.message}")
        }
    }

    /**
     * 切换到指定线路
     */
    fun switchToLine(lineInfo: LineManager.LineInfo) {
        val currentState = _uiState.value
        val currentEpisode = currentState.currentEpisode

        if (currentEpisode != null) {
            viewModelScope.launch {
                try {
                    val playUrl = lineManager.switchToLine(lineInfo, currentEpisode.index)

                    if (playUrl != null) {
                        val lineIndex = currentState.availableLines.indexOf(lineInfo)

                        _uiState.value = _uiState.value.copy(
                            currentLineIndex = lineIndex.coerceAtLeast(0),
                            playUrl = playUrl,
                            error = null
                        )

                        // 更新播放历史中的线路信息
                        currentState.movie?.let { movie ->
                            historyRepository.updateEpisode(
                                vodId = movie.vodId,
                                siteKey = movie.siteKey,
                                episode = currentEpisode,
                                flagName = lineInfo.flag.flag
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "线路切换失败，该线路暂时不可用"
                        )
                    }

                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "线路切换失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 切换剧集 (增强版，支持线路管理)
     */
    fun switchEpisode(episodeIndex: Int) {
        val currentState = _uiState.value
        val episodes = currentState.episodes

        if (episodeIndex in episodes.indices) {
            val targetEpisode = episodes[episodeIndex]
            val currentLine = currentState.availableLines.getOrNull(currentState.currentLineIndex)

            viewModelScope.launch {
                try {
                    val playUrl = if (currentLine != null) {
                        lineManager.switchToLine(currentLine, episodeIndex) ?: targetEpisode.url
                    } else {
                        targetEpisode.url
                    }

                    _uiState.value = _uiState.value.copy(
                        currentEpisode = targetEpisode,
                        currentEpisodeIndex = episodeIndex,
                        playUrl = playUrl,
                        error = null
                    )

                    // 更新播放历史
                    currentState.movie?.let { movie ->
                        historyRepository.updateEpisode(
                            vodId = movie.vodId,
                            siteKey = movie.siteKey,
                            episode = targetEpisode,
                            flagName = currentLine?.flag?.flag ?: ""
                        )
                    }

                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        error = "剧集切换失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 测试线路速度
     */
    fun testLineSpeed(lineInfo: LineManager.LineInfo) {
        viewModelScope.launch {
            try {
                val speed = lineManager.testLineSpeed(lineInfo)
                // 更新线路信息中的速度
                val updatedLines = _uiState.value.availableLines.map { line ->
                    if (line == lineInfo) {
                        line.copy(speed = speed)
                    } else {
                        line
                    }
                }

                _uiState.value = _uiState.value.copy(
                    availableLines = updatedLines
                )

            } catch (e: Exception) {
                // 测试失败不影响主流程
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
     * 获取当前播放信息
     */
    fun getCurrentPlayInfo(): PlayInfo? {
        val currentState = _uiState.value
        return if (currentState.movie != null && currentState.currentEpisode != null) {
            PlayInfo(
                vodId = currentState.movie.vodId,
                vodName = currentState.movie.vodName,
                episodeIndex = currentState.currentEpisode.index,
                episodeName = currentState.currentEpisode.name,
                siteKey = currentState.movie.siteKey,
                playUrl = currentState.playUrl
            )
        } else {
            null
        }
    }
}

/**
 * 播放信息数据类
 */
data class PlayInfo(
    val vodId: String,
    val vodName: String,
    val episodeIndex: Int,
    val episodeName: String,
    val siteKey: String,
    val playUrl: String
)
