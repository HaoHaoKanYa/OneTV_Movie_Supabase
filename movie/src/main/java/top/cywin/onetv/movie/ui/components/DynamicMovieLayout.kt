package top.cywin.onetv.movie.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.ui.base.MovieViewType
import top.cywin.onetv.movie.ui.base.MovieViewConfig

/**
 * 动态电影布局组件 - 支持多种ViewType
 * 与FongMi_TV数据兼容，使用Compose UI实现
 */
@Composable
fun DynamicMovieLayout(
    movies: List<Vod>,
    viewConfig: MovieViewConfig,
    onMovieClick: (Vod) -> Unit,
    onMovieLongClick: (Vod) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    // 动态计算列数
    val adaptiveColumns = viewConfig.getAdaptiveColumns(screenWidthDp)
    
    // 动画切换布局
    AnimatedContent(
        targetState = viewConfig.viewType,
        transitionSpec = {
            if (viewConfig.enableAnimation) {
                fadeIn() togetherWith fadeOut()
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        modifier = modifier,
        label = "ViewTypeTransition"
    ) { targetViewType ->
        when (targetViewType) {
            MovieViewType.RECT -> {
                RectGridLayout(
                    movies = movies,
                    columns = adaptiveColumns,
                    aspectRatio = viewConfig.aspectRatio,
                    showOverlay = viewConfig.showOverlay,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    contentPadding = contentPadding
                )
            }
            
            MovieViewType.OVAL -> {
                OvalGridLayout(
                    movies = movies,
                    columns = adaptiveColumns,
                    showOverlay = viewConfig.showOverlay,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    contentPadding = contentPadding
                )
            }
            
            MovieViewType.LIST -> {
                ListLayout(
                    movies = movies,
                    showOverlay = viewConfig.showOverlay,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    contentPadding = contentPadding
                )
            }
            
            MovieViewType.GRID -> {
                GridLayout(
                    movies = movies,
                    columns = adaptiveColumns,
                    onMovieClick = onMovieClick,
                    onMovieLongClick = onMovieLongClick,
                    contentPadding = contentPadding
                )
            }
        }
    }
}

/**
 * 矩形网格布局 - 标准电影海报
 */
@Composable
private fun RectGridLayout(
    movies: List<Vod>,
    columns: Int,
    aspectRatio: Float,
    showOverlay: Boolean,
    onMovieClick: (Vod) -> Unit,
    onMovieLongClick: (Vod) -> Unit,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(movies) { movie ->
            RectMovieCard(
                movie = movie,
                aspectRatio = aspectRatio,
                showOverlay = showOverlay,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) }
            )
        }
    }
}

/**
 * 圆形网格布局 - 演员头像等
 */
@Composable
private fun OvalGridLayout(
    movies: List<Vod>,
    columns: Int,
    showOverlay: Boolean,
    onMovieClick: (Vod) -> Unit,
    onMovieLongClick: (Vod) -> Unit,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies) { movie ->
            OvalMovieCard(
                movie = movie,
                showOverlay = showOverlay,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) }
            )
        }
    }
}

/**
 * 列表布局 - 紧凑信息显示
 */
@Composable
private fun ListLayout(
    movies: List<Vod>,
    showOverlay: Boolean,
    onMovieClick: (Vod) -> Unit,
    onMovieLongClick: (Vod) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(movies) { movie ->
            ListMovieItem(
                movie = movie,
                showOverlay = showOverlay,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) }
            )
        }
    }
}

/**
 * 密集网格布局
 */
@Composable
private fun GridLayout(
    movies: List<Vod>,
    columns: Int,
    onMovieClick: (Vod) -> Unit,
    onMovieLongClick: (Vod) -> Unit,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns + 1), // 比其他布局更密集
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(movies) { movie ->
            GridMovieCard(
                movie = movie,
                onClick = { onMovieClick(movie) },
                onLongClick = { onMovieLongClick(movie) }
            )
        }
    }
}
