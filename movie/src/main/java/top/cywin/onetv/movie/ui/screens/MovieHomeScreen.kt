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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// KotlinPoet专业重构 - 移除hiltViewModel import
// import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
import android.util.Log

/**
 * 点播首页 (参考OneMoVie主界面)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHomeScreen(
    navController: NavController,
    viewModel: MovieViewModel = viewModel {
        MovieViewModel(
            repository = top.cywin.onetv.movie.MovieApp.vodRepository,
            configManager = top.cywin.onetv.movie.MovieApp.vodConfigManager
        )
    }
) {
    Log.d("ONETV_MOVIE", "MovieHomeScreen 开始初始化")
    Log.d("ONETV_MOVIE", "开始获取ViewModel")

    // 使用LaunchedEffect来捕获初始化错误
    var initError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            Log.d("ONETV_MOVIE", "LaunchedEffect: 开始初始化检查")
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "初始化检查失败", e)
            initError = e.message
        }
    }

    // 如果有初始化错误，显示错误信息
    if (initError != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "点播功能初始化失败: $initError",
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Log.d("ONETV_MOVIE", "ViewModel获取成功，开始收集UI状态")

    Log.d("ONETV_MOVIE", "UI状态: isLoading=${uiState.isLoading}, error=${uiState.error}")

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


