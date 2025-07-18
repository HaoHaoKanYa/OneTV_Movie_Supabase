package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import android.util.Log
import top.cywin.onetv.movie.viewmodel.MovieViewModel
import top.cywin.onetv.movie.viewmodel.MovieUiState
import top.cywin.onetv.movie.viewmodel.VodConfigUrl
import top.cywin.onetv.movie.viewmodel.HomeCategorySection
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Class
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.ui.components.MovieCard
import top.cywin.onetv.movie.ui.components.QuickCategoryGrid
import top.cywin.onetv.movie.ui.components.RouteSelector

/**
 * OneTV Movie首页 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHomeScreen(
    navController: NavController,
    viewModel: MovieViewModel = viewModel { MovieViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val siteViewModel = movieApp.siteViewModel
    val uiAdapter = movieApp.uiAdapter

    // ✅ 观察FongMi_TV的数据变化 - 数据来源于FongMi_TV解析系统
    // 注意：这里需要根据实际的FongMi_TV SiteViewModel API进行调整
    // val searchResult by siteViewModel.result.observeAsState()
    // val contentDetail by siteViewModel.detail.observeAsState()
    // val homeContent by siteViewModel.list.observeAsState()

    // ✅ 页面初始化时加载数据
    LaunchedEffect(Unit) {
        Log.d("ONETV_MOVIE", "🏠 MovieHomeScreen 初始化")
        viewModel.loadHomeData()
    }

    // ✅ 处理FongMi_TV数据变化
    // LaunchedEffect(homeContent) {
    //     homeContent?.let { content ->
    //         Log.d("ONETV_MOVIE", "🏠 收到FongMi_TV首页数据: ${content.list.size}条")
    //         // 这里可以进一步处理FongMi_TV返回的数据
    //     }
    // }

    // ✅ UI状态处理
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在加载配置...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error ?: "未知错误",
                onRetry = { viewModel.refresh() },
                onBack = { navController.popBackStack() }
            )
        }
        uiState.isStoreHouseIndex -> {
            // 仓库索引模式
            StoreHouseScreen(
                uiState = uiState,
                onRouteSelect = { route -> viewModel.selectRoute(route) },
                onShowSelector = { viewModel.showRouteSelector() },
                onHideSelector = { viewModel.hideRouteSelector() }
            )
        }
        else -> {
            // 正常首页模式
            HomeContent(
                uiState = uiState,
                // searchResult = searchResult,
                // homeContent = homeContent,
                onRefresh = { viewModel.refresh() },
                onCategoryClick = { category ->
                    navController.navigate("category/${category.typeId}")
                },
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.getVodId()}/${movie.getSite()?.getKey() ?: ""}")
                },
                onSearchClick = {
                    navController.navigate("search")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: MovieUiState,
    // searchResult: Any?, // FongMi_TV的搜索结果
    // homeContent: Any?, // FongMi_TV的首页内容
    onRefresh: () -> Unit,
    onCategoryClick: (Class) -> Unit,
    onMovieClick: (Vod) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("OneTV 影视") },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        )

        // 主要内容区域
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 推荐内容轮播
            if (uiState.recommendMovies.isNotEmpty()) {
                item {
                    RecommendCarousel(
                        movies = uiState.recommendMovies,
                        onMovieClick = onMovieClick
                    )
                }
            }

            // 分类网格
            if (uiState.categories.isNotEmpty()) {
                item {
                    CategoryGrid(
                        categories = uiState.categories,
                        onCategoryClick = onCategoryClick
                    )
                }
            }

            // 各分类内容
            items(uiState.homeCategories) { categorySection ->
                CategorySection(
                    section = categorySection,
                    onMovieClick = onMovieClick,
                    onMoreClick = {
                        onCategoryClick(
                            Class().apply {
                                typeId = categorySection.categoryId
                                typeName = categorySection.categoryName
                            }
                        )
                    }
                )
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
private fun StoreHouseScreen(
    uiState: MovieUiState,
    onRouteSelect: (VodConfigUrl) -> Unit,
    onShowSelector: () -> Unit,
    onHideSelector: () -> Unit
) {
    // 仓库索引界面的实现
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("仓库索引模式 - 待实现")
    }
}

@Composable
private fun RecommendCarousel(
    movies: List<Vod>,
    onMovieClick: (Vod) -> Unit
) {
    Column {
        Text(
            text = "推荐内容",
            style = MaterialTheme.typography.titleLarge,
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

@Composable
private fun CategoryGrid(
    categories: List<Class>,
    onCategoryClick: (Class) -> Unit
) {
    Column {
        Text(
            text = "分类",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                Card(
                    modifier = Modifier.clickable { onCategoryClick(category) }
                ) {
                    Text(
                        text = category.typeName ?: "未知分类",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    section: HomeCategorySection,
    onMovieClick: (Vod) -> Unit,
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
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(onClick = onMoreClick) {
                Text("更多")
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



