package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MovieDetailViewModel
import top.cywin.onetv.movie.viewmodel.DetailUiState
import top.cywin.onetv.movie.bean.Episode
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Flag
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie详情页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    vodId: String,
    siteKey: String = "",
    navController: NavController,
    viewModel: MovieDetailViewModel = viewModel {
        MovieDetailViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vodId, siteKey) {
        viewModel.loadMovieDetail(vodId, siteKey)
    }

    // ✅ UI内容渲染
    DetailContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onPlay = { episode, episodeIndex ->
            navController.navigate("player/$vodId/$siteKey/$episodeIndex")
        },
        onToggleFavorite = { viewModel.toggleFavorite() },
        onFlagSelect = { flag -> viewModel.selectFlag(flag) },
        onEpisodeSelect = { episode -> viewModel.selectEpisode(episode) },
        onShowFlagSelector = { viewModel.showFlagSelector() },
        onHideFlagSelector = { viewModel.hideFlagSelector() },
        onShowEpisodeSelector = { viewModel.showEpisodeSelector() },
        onHideEpisodeSelector = { viewModel.hideEpisodeSelector() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onPlay: (Episode, Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onFlagSelect: (Flag) -> Unit,
    onEpisodeSelect: (Episode) -> Unit,
    onShowFlagSelector: () -> Unit,
    onHideFlagSelector: () -> Unit,
    onShowEpisodeSelector: () -> Unit,
    onHideEpisodeSelector: () -> Unit
) {
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在加载详情...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error,
                onRetry = { /* 重试逻辑 */ },
                onBack = onBack
            )
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部导航栏
                TopAppBar(
                    title = { Text(uiState.movie?.vodName ?: "详情") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (uiState.isFavorite) "取消收藏" else "收藏"
                            )
                        }
                    }
                )

                // 详情内容
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 基本信息
                    uiState.movie?.let { movie ->
                        item {
                            MovieInfoSection(movie = movie)
                        }
                    }

                    // 播放线路选择
                    if (uiState.flags.isNotEmpty()) {
                        item {
                            PlayFlagSection(
                                flags = uiState.flags,
                                selectedFlag = uiState.selectedFlag,
                                onFlagSelect = onFlagSelect
                            )
                        }
                    }

                    // 剧集列表
                    if (uiState.episodes.isNotEmpty()) {
                        item {
                            EpisodeSection(
                                episodes = uiState.episodes,
                                selectedEpisode = uiState.selectedEpisode,
                                onEpisodeSelect = onEpisodeSelect,
                                onPlay = onPlay
                            )
                        }
                    }
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

@Composable
private fun MovieInfoSection(movie: Vod) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 海报
        Card(
            modifier = Modifier
                .width(120.dp)
                .height(160.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "海报",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = movie.vodName ?: "未知标题",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            movie.vodRemarks?.let { remarks ->
                if (remarks.isNotEmpty()) {
                    Text(
                        text = remarks,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            InfoRow("年份", movie.vodYear)
            InfoRow("地区", movie.vodArea)
            InfoRow("导演", movie.vodDirector)
            InfoRow("主演", movie.vodActor)
        }
    }

    // 剧情简介
    movie.vodContent?.let { content ->
        if (content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "剧情简介",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    value?.let {
        if (it.isNotEmpty()) {
            Row {
                Text(
                    text = "$label: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PlayFlagSection(
    flags: List<Flag>,
    selectedFlag: Flag?,
    onFlagSelect: (Flag) -> Unit
) {
    Column {
        Text(
            text = "播放线路",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(flags) { flag ->
                FilterChip(
                    onClick = { onFlagSelect(flag) },
                    label = { Text(flag.flag ?: "未知线路") },
                    selected = selectedFlag == flag
                )
            }
        }
    }
}

@Composable
private fun EpisodeSection(
    episodes: List<Episode>,
    selectedEpisode: Episode?,
    onEpisodeSelect: (Episode) -> Unit,
    onPlay: (Episode, Int) -> Unit
) {
    Column {
        Text(
            text = "选集播放",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes.withIndex().toList()) { (index, episode) ->
                EpisodeChip(
                    episode = episode,
                    isSelected = selectedEpisode == episode,
                    onClick = {
                        onEpisodeSelect(episode)
                        onPlay(episode, index)
                    }
                )
            }
        }
    }
}

@Composable
private fun EpisodeChip(
    episode: Episode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = episode.name,
                style = MaterialTheme.typography.bodySmall
            )
        },
        selected = isSelected,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}
