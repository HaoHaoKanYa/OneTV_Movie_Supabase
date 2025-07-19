package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import top.cywin.onetv.movie.viewmodel.MovieCategoryViewModel
import top.cywin.onetv.movie.viewmodel.CategoryUiState
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Filter
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie分类浏览页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieCategoryScreen(
    typeId: String,
    siteKey: String = "",
    navController: NavController,
    viewModel: MovieCategoryViewModel = viewModel { MovieCategoryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val siteViewModel = movieApp.siteViewModel

    // ✅ 观察FongMi_TV的数据变化
    // val categoryResult by siteViewModel.list.observeAsState()

    // ✅ 页面初始化时加载分类数据
    LaunchedEffect(typeId, siteKey) {
        Log.d("ONETV_MOVIE", "📂 MovieCategoryScreen 初始化: typeId=$typeId")
        viewModel.initCategory(typeId)
    }

    // ✅ UI状态处理
    when {
        uiState.isLoading && uiState.movies.isEmpty() -> {
            LoadingScreen(message = "正在加载分类内容...")
        }
        uiState.error != null && uiState.movies.isEmpty() -> {
            ErrorScreen(
                error = uiState.error ?: "未知错误",
                onRetry = { viewModel.initCategory(typeId) },
                onBack = { navController.popBackStack() }
            )
        }
        else -> {
            CategoryContent(
                uiState = uiState,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.vodId}/$siteKey")
                },
                onLoadMore = { viewModel.loadMore() },
                onFilterChange = { filter -> viewModel.applyFilters(mapOf()) },
                onSortChange = { sort -> /* TODO: 实现排序 */ },
                onRefresh = { viewModel.refresh() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryContent(
    uiState: CategoryUiState,
    onMovieClick: (Vod) -> Unit,
    onLoadMore: () -> Unit,
    onFilterChange: (Filter) -> Unit,
    onSortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text(uiState.categoryName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = { /* 显示筛选 */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "筛选")
                }
            }
        )

        // 筛选器行
        if (uiState.availableFilters.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.availableFilters) { filter ->
                    FilterChip(
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.getName()) },
                        selected = uiState.selectedFilters.containsKey(filter.getKey())
                    )
                }
            }
        }

        // 电影网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }

            // 加载更多指示器
            if (uiState.hasMore && !uiState.isLoadingMore) {
                item {
                    LaunchedEffect(Unit) {
                        onLoadMore()
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(
    movie: Vod,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // 电影海报占位符
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 电影信息
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = movie.vodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.vodRemarks.isNotEmpty()) {
                    Text(
                        text = movie.vodRemarks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
