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
import top.cywin.onetv.movie.viewmodel.MovieHistoryViewModel
import top.cywin.onetv.movie.viewmodel.HistoryUiState
import top.cywin.onetv.movie.ui.model.WatchHistory
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie历史记录页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHistoryScreen(
    navController: NavController,
    viewModel: MovieHistoryViewModel = viewModel {
        MovieHistoryViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    // ✅ UI内容渲染
    HistoryContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onHistoryClick = { history ->
            navController.navigate("detail/${history.vodId}/${history.siteKey}")
        },
        onHistoryPlay = { history ->
            navController.navigate("player/${history.vodId}/${history.siteKey}/${history.episodeIndex}")
        },
        onHistoryDelete = { history -> viewModel.deleteHistory(history) },
        onClearAllHistory = { viewModel.clearAllHistory() },
        onRefresh = { viewModel.loadHistory() },
        onError = { viewModel.clearError() }
    )
}
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onHistoryClick: (WatchHistory) -> Unit,
    onHistoryPlay: (WatchHistory) -> Unit,
    onHistoryDelete: (WatchHistory) -> Unit,
    onClearAllHistory: () -> Unit,
    onRefresh: () -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = "观看历史",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                // 清空历史按钮
                if (uiState.histories.isNotEmpty()) {
                    IconButton(onClick = onClearAllHistory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "清空历史"
                        )
                    }
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingScreen(message = "正在加载历史记录...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = onRefresh,
                    onBack = onBack
                )
            }
            uiState.histories.isEmpty() -> {
                EmptyHistoryScreen()
            }
            else -> {
                HistoryListScreen(
                    histories = uiState.histories,
                    onHistoryClick = onHistoryClick,
                    onHistoryPlay = onHistoryPlay,
                    onHistoryDelete = onHistoryDelete
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryScreen() {
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
                text = "暂无观看历史",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryListScreen(
    histories: List<WatchHistory>,
    onHistoryClick: (WatchHistory) -> Unit,
    onHistoryPlay: (WatchHistory) -> Unit,
    onHistoryDelete: (WatchHistory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(histories) { history ->
            HistoryItem(
                history = history,
                onClick = { onHistoryClick(history) },
                onPlay = { onHistoryPlay(history) },
                onDelete = { onHistoryDelete(history) }
            )
        }
    }
}

@Composable
private fun HistoryItem(
    history: WatchHistory,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
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
                    text = history.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "观看至: ${history.episodeName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "进度: ${history.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = history.watchTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            Row {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "继续播放")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
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
