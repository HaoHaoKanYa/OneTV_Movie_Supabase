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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.DetailUiState
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodFlag
import top.cywin.onetv.movie.data.models.VodEpisode
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.viewmodel.MovieDetailViewModel

/**
 * 详情页面 (参考OneMoVie详情界面)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    vodId: String,
    siteKey: String = "",
    navController: NavController,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 加载详情数据
    LaunchedEffect(vodId, siteKey) {
        viewModel.loadMovieDetail(vodId, siteKey)
    }

    MovieDetailContent(
        navController = navController,
        uiState = uiState,
        onPlayClick = { flag, episode ->
            viewModel.selectFlag(flag)
            viewModel.selectEpisode(episode)
            viewModel.startPlay(episode)
            navController.navigate(MovieRoutes.player(vodId, episode.index, siteKey))
        },
        onFavoriteClick = { viewModel.toggleFavorite() },
        onFlagChange = { viewModel.selectFlag(it) }
    )
}

@Composable
private fun MovieDetailContent(
    navController: NavController,
    uiState: DetailUiState,
    onPlayClick: (VodFlag, VodEpisode) -> Unit,
    onFavoriteClick: () -> Unit,
    onFlagChange: (VodFlag) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        DetailTopBar(
            title = uiState.movie?.vodName ?: "详情",
            onBackClick = { navController.popBackStack() },
            isFavorite = uiState.isFavorite,
            onFavoriteClick = onFavoriteClick
        )
        
        if (uiState.isLoading) {
            // 加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (uiState.error != null) {
            // 错误状态
            ErrorContent(
                error = uiState.error,
                onRetry = { /* TODO: 重试逻辑 */ }
            )
        } else if (uiState.movie != null) {
            // 详情内容
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 基本信息
                item {
                    MovieInfoSection(movie = uiState.movie)
                }
                
                // 播放线路选择
                if (uiState.playFlags.isNotEmpty()) {
                    item {
                        PlayFlagSection(
                            flags = uiState.playFlags,
                            currentFlag = uiState.currentFlag,
                            onFlagChange = onFlagChange
                        )
                    }
                }
                
                // 剧集列表
                if (uiState.currentFlag != null) {
                    item {
                        EpisodeSection(
                            flag = uiState.currentFlag,
                            currentEpisode = uiState.currentEpisode,
                            onEpisodeClick = { episode ->
                                onPlayClick(uiState.currentFlag, episode)
                            }
                        )
                    }
                }
                
                // 相关推荐
                if (uiState.relatedMovies.isNotEmpty()) {
                    item {
                        RelatedMoviesSection(
                            movies = uiState.relatedMovies,
                            onMovieClick = { movie ->
                                navController.navigate(
                                    MovieRoutes.detail(movie.vodId, movie.siteKey)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 详情页面顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String,
    onBackClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) Color.Red else Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

/**
 * 电影信息区域
 */
@Composable
private fun MovieInfoSection(movie: VodItem) {
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
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "海报",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        
        // 信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = movie.vodName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (movie.vodRemarks.isNotEmpty()) {
                Text(
                    text = movie.vodRemarks,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            InfoRow("年份", movie.vodYear)
            InfoRow("地区", movie.vodArea)
            InfoRow("导演", movie.vodDirector)
            InfoRow("主演", movie.vodActor)
            
            if (movie.vodScore.isNotEmpty()) {
                InfoRow("评分", movie.vodScore)
            }
        }
    }
    
    // 剧情简介
    if (movie.vodContent.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "剧情简介",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.vodContent,
            color = Color.Gray,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row {
            Text(
                text = "$label: ",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 播放线路区域
 */
@Composable
private fun PlayFlagSection(
    flags: List<VodFlag>,
    currentFlag: VodFlag?,
    onFlagChange: (VodFlag) -> Unit
) {
    Column {
        Text(
            text = "播放线路",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(flags) { flag ->
                FilterChip(
                    onClick = { onFlagChange(flag) },
                    label = { Text(flag.getSummary()) },
                    selected = currentFlag == flag
                )
            }
        }
    }
}

/**
 * 剧集区域
 */
@Composable
private fun EpisodeSection(
    flag: VodFlag,
    currentEpisode: VodEpisode?,
    onEpisodeClick: (VodEpisode) -> Unit
) {
    val episodes = flag.createEpisodes()

    Column {
        Text(
            text = "选集播放",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes) { episode ->
                EpisodeChip(
                    episode = episode,
                    isSelected = currentEpisode == episode,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

/**
 * 剧集芯片
 */
@Composable
private fun EpisodeChip(
    episode: VodEpisode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = episode.getDisplayName(),
                fontSize = 12.sp
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

/**
 * 相关推荐区域
 */
@Composable
private fun RelatedMoviesSection(
    movies: List<VodItem>,
    onMovieClick: (VodItem) -> Unit
) {
    Column {
        Text(
            text = "相关推荐",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                RelatedMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

/**
 * 相关电影卡片
 */
@Composable
private fun RelatedMovieCard(
    movie: VodItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(140.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(8.dp)
        ) {
            // 海报占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "海报",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 标题
            Text(
                text = movie.vodName,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = Color.White,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
