package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.MovieUiState
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.HomeCategorySection
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.ui.components.MovieCard
import top.cywin.onetv.movie.ui.components.QuickCategoryGrid
import top.cywin.onetv.movie.viewmodel.MovieViewModel

/**
 * 点播首页 (参考OneMoVie主界面)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHomeScreen(
    navController: NavController,
    viewModel: MovieViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MovieHomeContent(
        navController = navController,
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onCategoryClick = { category ->
            navController.navigate(MovieRoutes.category(category.typeId, uiState.currentSite?.key))
        },
        onMovieClick = { movie ->
            navController.navigate(MovieRoutes.detail(movie.vodId, movie.siteKey))
        }
    )
}

@Composable
private fun MovieHomeContent(
    navController: NavController,
    uiState: MovieUiState,
    onRefresh: () -> Unit,
    onCategoryClick: (VodClass) -> Unit,
    onMovieClick: (VodItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        MovieTopBar(
            title = "影视点播",
            onSearchClick = {
                navController.navigate(MovieRoutes.SEARCH)
            },
            onSettingsClick = {
                navController.navigate(MovieRoutes.SETTINGS)
            }
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
                onRetry = onRefresh
            )
        } else {
            // 主要内容
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // 推荐内容区域
                if (uiState.recommendMovies.isNotEmpty()) {
                    item {
                        RecommendSection(
                            movies = uiState.recommendMovies,
                            onMovieClick = onMovieClick
                        )
                    }
                }
                
                // 快速分类导航
                if (uiState.quickCategories.isNotEmpty()) {
                    item {
                        QuickCategoryGrid(
                            categories = uiState.quickCategories,
                            onCategoryClick = onCategoryClick
                        )
                    }
                }
                
                // 分类内容区域
                items(uiState.homeCategories) { categorySection ->
                    HomeCategorySection(
                        section = categorySection,
                        onMovieClick = onMovieClick,
                        onMoreClick = {
                            navController.navigate(
                                MovieRoutes.category(categorySection.categoryId, categorySection.siteKey)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieTopBar(
    title: String,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
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
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

/**
 * 推荐内容区域
 */
@Composable
private fun RecommendSection(
    movies: List<VodItem>,
    onMovieClick: (VodItem) -> Unit
) {
    Column {
        Text(
            text = "推荐内容",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}



/**
 * 首页分类区域
 */
@Composable
private fun HomeCategorySection(
    section: HomeCategorySection,
    onMovieClick: (VodItem) -> Unit,
    onMoreClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.categoryName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onMoreClick) {
                Text(
                    text = "更多",
                    color = Color.Gray
                )
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.movies.take(10)) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
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


