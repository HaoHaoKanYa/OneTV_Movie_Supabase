package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MovieSettingsViewModel
import top.cywin.onetv.movie.viewmodel.SettingsUiState
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.History
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie历史记录页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHistoryScreen(
    navController: NavController,
    viewModel: MovieSettingsViewModel = viewModel { MovieSettingsViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val repositoryAdapter = movieApp.repositoryAdapter

    // ✅ 页面初始化时加载历史记录
    LaunchedEffect(Unit) {
        Log.d("ONETV_MOVIE", "📖 MovieHistoryScreen 初始化")
        viewModel.loadWatchHistory()
    }

    // ✅ UI状态处理
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在加载历史记录...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error ?: "未知错误",
                onRetry = { viewModel.loadWatchHistory() },
                onBack = { navController.popBackStack() }
            )
        }
        else -> {
            HistoryContent(
                uiState = uiState,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.vodId}/${movie.siteKey}")
                },
                onContinuePlay = { history ->
                    navController.navigate("player/${history.getVodId()}/${history.getEpisode().getIndex()}/${history.getSiteKey()}")
                },
                onDeleteHistory = { history -> viewModel.deleteWatchHistory(history) },
                onClearAllHistory = { viewModel.clearAllHistory() },
                onToggleFavorite = { movie -> viewModel.toggleFavorite(movie) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
@Composable
private fun HistoryContent(
    uiState: SettingsUiState,
    onMovieClick: (Vod) -> Unit,
    onContinuePlay: (History) -> Unit,
    onDeleteHistory: (History) -> Unit,
    onClearAllHistory: () -> Unit,
    onToggleFavorite: (Vod) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("历史记录") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = onClearAllHistory) {
                    Icon(Icons.Default.Delete, contentDescription = "清空历史")
                }
            }
        )

        // 标签页
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("观看历史") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("我的收藏") }
            )
        }

        // 内容区域
        when (selectedTab) {
            0 -> {
                // 观看历史
                if (uiState.watchHistory.isEmpty()) {
                    EmptyContent("暂无观看历史")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.watchHistory) { history ->
                            HistoryItem(
                                history = history,
                                onContinuePlay = { onContinuePlay(history) },
                                onDelete = { onDeleteHistory(history) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // 收藏列表
                if (uiState.favoriteMovies.isEmpty()) {
                    EmptyContent("暂无收藏内容")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.favoriteMovies) { movie ->
                            FavoriteItem(
                                movie = movie,
                                onClick = { onMovieClick(movie) },
                                onToggleFavorite = { onToggleFavorite(movie) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    history: History,
    onContinuePlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinuePlay() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 电影海报占位符
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 电影信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = history.getVodName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "观看到: ${history.getVodRemarks()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "进度: ${formatProgress(history.getPosition(), history.getDuration())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatWatchTime(history.getCreateTime()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    movie: Vod,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 电影海报占位符
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 电影信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = movie.getVodName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.getVodRemarks().isNotEmpty()) {
                    Text(
                        text = movie.getVodRemarks(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (movie.getVodYear().isNotEmpty()) {
                    Text(
                        text = "${movie.getVodYear()}年",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 取消收藏按钮
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "取消收藏",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ✅ 按照指南添加必要的辅助函数

private fun formatProgress(position: Long, duration: Long): String {
    if (duration <= 0) return "未知"
    val progress = (position * 100 / duration).coerceIn(0, 100)
    return "${progress}%"
}

private fun formatWatchTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "刚刚观看"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        else -> "${diff / (24 * 60 * 60 * 1000)}天前"
    }
}

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
