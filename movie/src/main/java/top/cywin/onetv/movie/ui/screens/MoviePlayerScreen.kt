@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MoviePlayerViewModel
import top.cywin.onetv.movie.viewmodel.PlayerUiState
import top.cywin.onetv.movie.bean.Episode
import top.cywin.onetv.movie.bean.Flag
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie播放器页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePlayerScreen(
    vodId: String,
    siteKey: String = "",
    episodeIndex: Int = 0,
    navController: NavController,
    viewModel: MoviePlayerViewModel = viewModel {
        MoviePlayerViewModel(
            historyRepository = MovieApp.getInstance().watchHistoryRepository
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vodId, siteKey, episodeIndex) {
        viewModel.initPlayer(vodId, siteKey, episodeIndex)
    }

    // ✅ UI内容渲染
    PlayerContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onPlayPause = { viewModel.togglePlayPause() },
        onSeek = { position -> viewModel.seekTo(position) },
        onNextEpisode = { viewModel.playNextEpisode() },
        onPreviousEpisode = { viewModel.playPreviousEpisode() },
        onEpisodeSelect = { episode, index ->
            viewModel.playEpisode(episode, index)
        },
        onLineSwitch = { lineInfo ->
            viewModel.switchToLine(lineInfo)
        }
    )

    // ✅ WebView解析器
    if (uiState.showWebViewParser && uiState.webViewParseRequest != null) {
        ParseWebView(
            request = uiState.webViewParseRequest,
            onParseSuccess = { url -> viewModel.onWebViewParseSuccess(url) },
            onParseError = { error -> viewModel.onWebViewParseError(error) }
        )
    }
}

@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onEpisodeSelect: (Episode, Int) -> Unit,
    onLineSwitch: (Any) -> Unit
) {
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在解析播放地址...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error,
                onRetry = { /* 重试逻辑 */ },
                onBack = onBack
            )
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // 播放器区域
                VideoPlayerView(
                    playUrl = uiState.playUrl,
                    isPlaying = uiState.isPlaying,
                    onPlayPause = onPlayPause
                )

                // 播放器控制界面
                PlayerControls(
                    uiState = uiState,
                    onBack = onBack,
                    onSeek = onSeek,
                    onNextEpisode = onNextEpisode,
                    onPreviousEpisode = onPreviousEpisode,
                    onEpisodeSelect = onEpisodeSelect,
                    onLineSwitch = onLineSwitch
                )
            }
        }
    }
}

// ✅ 按照指南添加必要的辅助Composable函数

@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message)
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("重试")
                }
                OutlinedButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}
@Composable
private fun VideoPlayerView(
    playUrl: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (playUrl.isNotEmpty()) {
            // 这里应该集成实际的视频播放器组件
            // 例如 ExoPlayer 或其他播放器
            Text(
                text = "视频播放器\n播放地址: $playUrl",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "等待播放地址解析...",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // 播放/暂停按钮
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun PlayerControls(
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onSeek: (Long) -> Unit,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onEpisodeSelect: (Episode, Int) -> Unit,
    onLineSwitch: (Any) -> Unit
) {
    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
    ) {
        if (showControls) {
            // 顶部控制栏
            TopAppBar(
                title = {
                    Text(
                        text = uiState.movie?.vodName ?: "播放中",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 底部控制栏
            BottomPlayerControls(
                uiState = uiState,
                onEpisodeSelect = onEpisodeSelect,
                onLineSwitch = onLineSwitch,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BottomPlayerControls(
    uiState: PlayerUiState,
    onEpisodeSelect: (Episode, Int) -> Unit,
    onLineSwitch: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 线路选择
            if (uiState.playFlags.isNotEmpty()) {
                Text(
                    text = "播放线路",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.playFlags) { flag ->
                        FilterChip(
                            onClick = { onLineSwitch(flag) },
                            label = { Text(flag.getFlag() ?: "未知线路") },
                            selected = uiState.currentFlag == flag
                        )
                    }
                }
            }

            // 剧集选择
            if (uiState.episodes.isNotEmpty()) {
                Text(
                    text = "选集",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.episodes.withIndex().toList()) { (index, episode) ->
                        FilterChip(
                            onClick = { onEpisodeSelect(episode, index) },
                            label = { Text(episode.name) },
                            selected = uiState.currentEpisode == episode
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParseWebView(
    request: Any?, // WebView解析请求
    onParseSuccess: (String) -> Unit,
    onParseError: (String) -> Unit
) {
    // WebView解析器实现
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在解析播放地址...")
        }
    }
}