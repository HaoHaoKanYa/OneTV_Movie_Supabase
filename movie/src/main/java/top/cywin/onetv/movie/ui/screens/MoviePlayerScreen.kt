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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.PlayerUiState
import top.cywin.onetv.movie.data.models.VodEpisode
import top.cywin.onetv.movie.data.models.VodFlag
import top.cywin.onetv.movie.data.parser.LineManager
import top.cywin.onetv.movie.ui.focus.tvFocusable
import top.cywin.onetv.movie.ui.focus.tvPlayerControlFocusable
import top.cywin.onetv.movie.viewmodel.MoviePlayerViewModel
import top.cywin.onetv.tv.ui.screens.videoplayer.VideoPlayerScreen
import top.cywin.onetv.tv.ui.screens.videoplayer.rememberVideoPlayerState

/**
 * 点播播放器页面 (集成ExoPlayer)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePlayerScreen(
    vodId: String,
    episodeIndex: Int,
    siteKey: String = "",
    navController: NavController,
    viewModel: MoviePlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 创建播放器状态，禁用直播观看历史，使用点播历史
    val videoPlayerState = rememberVideoPlayerState(
        enableWatchHistory = false // 禁用直播历史，使用点播历史
    )

    // 加载播放数据
    LaunchedEffect(vodId, episodeIndex, siteKey) {
        viewModel.loadPlayData(vodId, episodeIndex, siteKey)
    }

    // 监听播放URL变化
    LaunchedEffect(uiState.playUrl) {
        if (uiState.playUrl.isNotEmpty()) {
            videoPlayerState.prepare(uiState.playUrl)
        }
    }

    // 监听播放进度，保存历史
    LaunchedEffect(videoPlayerState.currentPosition, videoPlayerState.duration) {
        if (videoPlayerState.currentPosition > 0 && videoPlayerState.duration > 0) {
            viewModel.updatePlayProgress(
                position = videoPlayerState.currentPosition,
                duration = videoPlayerState.duration
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // 使用现有的VideoPlayerScreen作为基础
        VideoPlayerScreen(
            state = videoPlayerState,
            showMetadataProvider = {
                uiState.movie != null && uiState.currentEpisode != null
            }
        )

        // 点播特有的控制界面 - 支持线路切换和TV遥控器
        EnhancedMoviePlayerControls(
            uiState = uiState,
            videoPlayerState = videoPlayerState,
            onBackClick = { navController.popBackStack() },
            onFlagChange = { viewModel.selectFlag(it) },
            onEpisodeChange = { viewModel.selectEpisode(it) },
            onPreviousEpisode = { viewModel.playPreviousEpisode() },
            onNextEpisode = { viewModel.playNextEpisode() },
            onLineSwitch = { lineInfo -> viewModel.switchToLine(lineInfo) }
        )
    }
}

/**
 * 增强的点播播放器控制界面 - 支持线路切换和TV遥控器
 */
@Composable
private fun EnhancedMoviePlayerControls(
    uiState: PlayerUiState,
    videoPlayerState: top.cywin.onetv.tv.ui.screens.videoplayer.VideoPlayerState,
    onBackClick: () -> Unit,
    onFlagChange: (VodFlag) -> Unit,
    onEpisodeChange: (VodEpisode) -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onLineSwitch: (LineManager.LineInfo) -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var showLineSelector by remember { mutableStateOf(false) }
    var showEpisodeSelector by remember { mutableStateOf(false) }

    val backButtonFocusRequester = remember { FocusRequester() }

    // 点击屏幕显示/隐藏控制界面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showControls = !showControls }
            .background(Color.Transparent)
    ) {
        // 顶部控制栏
        if (showControls) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.movie?.vodName ?: "",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.currentEpisode != null) {
                            Text(
                                text = uiState.currentEpisode.getDisplayName(),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .focusRequester(backButtonFocusRequester)
                            .tvPlayerControlFocusable(
                                onSelect = onBackClick
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // 线路切换按钮
                    IconButton(
                        onClick = { showLineSelector = true },
                        modifier = Modifier.tvPlayerControlFocusable(
                            onSelect = { showLineSelector = true }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "切换线路",
                            tint = Color.White
                        )
                    }

                    // 剧集选择按钮
                    IconButton(
                        onClick = { showEpisodeSelector = true },
                        modifier = Modifier.tvPlayerControlFocusable(
                            onSelect = { showEpisodeSelector = true }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "选择剧集",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // 底部控制栏
        if (showControls) {
            BottomPlayerControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                uiState = uiState,
                onPreviousEpisode = onPreviousEpisode,
                onNextEpisode = onNextEpisode
            )
        }

        // 线路选择器
        if (showLineSelector) {
            LineSelector(
                modifier = Modifier.align(Alignment.CenterEnd),
                availableLines = uiState.availableLines,
                currentLineIndex = uiState.currentLineIndex,
                onLineSelected = { lineInfo ->
                    onLineSwitch(lineInfo)
                    showLineSelector = false
                },
                onDismiss = { showLineSelector = false }
            )
        }

        // 剧集选择器
        if (showEpisodeSelector) {
            EpisodeSelector(
                modifier = Modifier.align(Alignment.CenterStart),
                episodes = uiState.episodes,
                currentEpisodeIndex = uiState.currentEpisodeIndex,
                onEpisodeSelected = { episode ->
                    onEpisodeChange(episode)
                    showEpisodeSelector = false
                },
                onDismiss = { showEpisodeSelector = false }
            )
        }

        // 自动隐藏控制界面
        LaunchedEffect(showControls) {
            if (showControls) {
                kotlinx.coroutines.delay(5000) // 5秒后自动隐藏
                showControls = false
            }
        }
    }

    // 错误处理
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { /* 不允许取消 */ },
            title = { Text("播放错误") },
            text = { Text(uiState.error) },
            confirmButton = {
                TextButton(onClick = onBackClick) {
                    Text("返回")
                }
            }
        )
    }

    // 请求初始焦点
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        backButtonFocusRequester.requestFocus()
    }
}

/**
 * 底部播放控制栏
 */
@Composable
private fun BottomPlayerControls(
    modifier: Modifier = Modifier,
    uiState: PlayerUiState,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一集按钮
            IconButton(
                onClick = onPreviousEpisode,
                enabled = uiState.currentEpisodeIndex > 0,
                modifier = Modifier.tvPlayerControlFocusable(
                    onSelect = onPreviousEpisode
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "上一集",
                    tint = if (uiState.currentEpisodeIndex > 0) Color.White else Color.Gray
                )
            }

            // 当前剧集信息
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "第${uiState.currentEpisodeIndex + 1}集",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "共${uiState.episodes.size}集",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // 下一集按钮
            IconButton(
                onClick = onNextEpisode,
                enabled = uiState.currentEpisodeIndex < uiState.episodes.size - 1,
                modifier = Modifier.tvPlayerControlFocusable(
                    onSelect = onNextEpisode
                )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "下一集",
                    tint = if (uiState.currentEpisodeIndex < uiState.episodes.size - 1) Color.White else Color.Gray
                )
            }
        }
    }
}

/**
 * 线路选择器
 */
@Composable
private fun LineSelector(
    modifier: Modifier = Modifier,
    availableLines: List<LineManager.LineInfo>,
    currentLineIndex: Int,
    onLineSelected: (LineManager.LineInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .heightIn(max = 400.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择线路",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.tvPlayerControlFocusable(
                        onSelect = onDismiss
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                itemsIndexed(availableLines) { index, lineInfo ->
                    LineItem(
                        lineInfo = lineInfo,
                        isSelected = index == currentLineIndex,
                        onClick = { onLineSelected(lineInfo) },
                        modifier = Modifier.tvFocusable(
                            onClick = { onLineSelected(lineInfo) }
                        )
                    )
                }
            }
        }
    }
}

/**
 * 线路项目
 */
@Composable
private fun LineItem(
    lineInfo: LineManager.LineInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lineInfo.flag.flag,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 质量标签
                    Text(
                        text = lineInfo.quality.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 速度标签
                    if (lineInfo.speed > 0) {
                        Text(
                            text = "${lineInfo.speed}分",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                lineInfo.speed >= 80 -> Color.Green
                                lineInfo.speed >= 60 -> Color.Yellow
                                else -> Color.Red
                            }
                        )
                    }

                    // 可用性标签
                    if (!lineInfo.isAvailable) {
                        Text(
                            text = "不可用",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 剧集选择器
 */
@Composable
private fun EpisodeSelector(
    modifier: Modifier = Modifier,
    episodes: List<VodEpisode>,
    currentEpisodeIndex: Int,
    onEpisodeSelected: (VodEpisode) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .width(300.dp)
            .heightIn(max = 500.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择剧集",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.tvPlayerControlFocusable(
                        onSelect = onDismiss
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                itemsIndexed(episodes) { index, episode ->
                    EpisodeItem(
                        episode = episode,
                        episodeIndex = index,
                        isSelected = index == currentEpisodeIndex,
                        onClick = { onEpisodeSelected(episode) },
                        modifier = Modifier.tvFocusable(
                            onClick = { onEpisodeSelected(episode) }
                        )
                    )
                }
            }
        }
    }
}

/**
 * 剧集项目
 */
@Composable
private fun EpisodeItem(
    episode: VodEpisode,
    episodeIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = episode.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "第${episodeIndex + 1}集",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "正在播放",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
