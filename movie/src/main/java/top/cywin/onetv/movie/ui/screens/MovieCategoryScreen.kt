package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.CategoryUiState
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodFilter
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.ui.components.MovieGridCard
import top.cywin.onetv.movie.ui.components.FilterDialog

/**
 * 分类页面 (参考OneMoVie分类界面)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieCategoryScreen(
    typeId: String,
    siteKey: String = "",
    navController: NavController,
    uiState: CategoryUiState = CategoryUiState(),
    onLoadMore: () -> Unit = {},
    onFilterChange: (Map<String, String>) -> Unit = {},
    onMovieClick: (VodItem) -> Unit = {}
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        CategoryTopBar(
            title = uiState.currentCategory?.typeName ?: "分类",
            onBackClick = { navController.popBackStack() },
            onFilterClick = { showFilterDialog = true },
            hasFilters = uiState.filters.isNotEmpty()
        )
        
        if (uiState.isLoading && uiState.movies.isEmpty()) {
            // 初始加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (uiState.error != null && uiState.movies.isEmpty()) {
            // 错误状态
            ErrorContent(
                error = uiState.error,
                onRetry = { /* TODO: 重试逻辑 */ }
            )
        } else {
            // 内容网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.movies) { movie ->
                    MovieGridCard(
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
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 筛选对话框
    if (showFilterDialog) {
        FilterDialog(
            filters = uiState.filters,
            selectedFilters = uiState.selectedFilters,
            onDismiss = { showFilterDialog = false },
            onConfirm = { filters ->
                onFilterChange(filters)
                showFilterDialog = false
            }
        )
    }
}

/**
 * 分类页面顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTopBar(
    title: String,
    onBackClick: () -> Unit,
    onFilterClick: () -> Unit,
    hasFilters: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
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
            if (hasFilters) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
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
