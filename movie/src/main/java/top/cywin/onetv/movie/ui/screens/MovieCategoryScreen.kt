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
import top.cywin.onetv.movie.ui.model.CategoryInfo
import top.cywin.onetv.movie.ui.model.MovieItem
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie分类浏览页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieCategoryScreen(
    typeId: String? = null,
    navController: NavController,
    viewModel: MovieCategoryViewModel = viewModel {
        MovieCategoryViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(typeId) {
        viewModel.initializeCategory(typeId)
    }

    // ✅ UI内容渲染
    CategoryContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onCategorySelect = { category -> viewModel.selectCategory(category.typeId) },
        onMovieClick = { movie ->
            navController.navigate("detail/${movie.vodId}/${movie.siteKey}")
        },
        onLoadMore = { viewModel.loadMore() },
        onRefresh = { viewModel.refresh() },
        onShowFilter = { viewModel.showFilterDialog() },
        onHideFilter = { viewModel.hideFilterDialog() },
        onApplyFilters = { filters -> viewModel.applyFilters(filters) },
        onClearFilters = { viewModel.clearFilters() },
        onError = { viewModel.clearError() }
    )
}

@Composable
private fun CategoryContent(
    uiState: CategoryUiState,
    onBack: () -> Unit,
    onCategorySelect: (CategoryInfo) -> Unit,
    onMovieClick: (MovieItem) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onShowFilter: () -> Unit,
    onHideFilter: () -> Unit,
    onApplyFilters: (Map<String, String>) -> Unit,
    onClearFilters: () -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = uiState.currentCategory?.typeName ?: "分类",
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
                // 筛选按钮
                IconButton(onClick = onShowFilter) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选"
                    )
                }

                // 刷新按钮
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新"
                    )
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoadingCategories -> {
                LoadingScreen(message = "正在加载分类...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = onRefresh,
                    onBack = onBack
                )
            }
            uiState.currentCategory == null -> {
                CategorySelectionScreen(
                    categories = uiState.categories,
                    onCategorySelect = onCategorySelect
                )
            }
            else -> {
                MovieListScreen(
                    uiState = uiState,
                    onMovieClick = onMovieClick,
                    onLoadMore = onLoadMore
                )
            }
        }

        // 筛选对话框
        if (uiState.showFilterDialog) {
            FilterDialog(
                filters = uiState.availableFilters,
                currentFilters = uiState.currentFilters,
                onApplyFilters = { filters ->
                    onApplyFilters(filters)
                    onHideFilter()
                },
                onClearFilters = {
                    onClearFilters()
                    onHideFilter()
                },
                onDismiss = onHideFilter
            )
        }
    }
}

@Composable
private fun CategorySelectionScreen(
    categories: List<CategoryInfo>,
    onCategorySelect: (CategoryInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(categories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelect(category) }
            ) {
                Text(
                    text = category.typeName,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun MovieListScreen(
    uiState: CategoryUiState,
    onMovieClick: (MovieItem) -> Unit,
    onLoadMore: () -> Unit
) {
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

@Composable
private fun FilterDialog(
    filters: List<Any>, // FilterOption类型
    currentFilters: Map<String, String>,
    onApplyFilters: (Map<String, String>) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件") },
        text = {
            // 筛选选项实现
            Text("筛选功能待实现")
        },
        confirmButton = {
            TextButton(onClick = { onApplyFilters(emptyMap()) }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onClearFilters) {
                Text("清除")
            }
        }
    )
}

@Composable
private fun MovieCard(
    movie: MovieItem,
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
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.subtitle.isNotEmpty()) {
                    Text(
                        text = movie.subtitle,
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
