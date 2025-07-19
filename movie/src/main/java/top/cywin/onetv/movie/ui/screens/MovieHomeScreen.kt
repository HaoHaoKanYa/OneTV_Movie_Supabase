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
import androidx.compose.material.icons.filled.SwapHoriz
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
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.viewmodel.MovieViewModel
import top.cywin.onetv.movie.viewmodel.MovieUiState
import top.cywin.onetv.movie.viewmodel.VodConfigUrl
import top.cywin.onetv.movie.viewmodel.HomeCategorySection
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Class
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.event.NavigationEvent
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
    viewModel: MovieViewModel = viewModel {
        MovieViewModel(
            configManager = MovieApp.getInstance().vodConfigManager
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 监听导航事件
    LaunchedEffect(Unit) {
        EventBus.getDefault().register(object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onNavigation(event: NavigationEvent) {
                when (event.action) {
                    "live_boot" -> {
                        // 导航到直播页面
                        navController.navigate("live")
                    }
                    "movie_detail" -> {
                        val vodId = event.params["vodId"] ?: ""
                        val siteKey = event.params["siteKey"] ?: ""
                        navController.navigate("detail/$vodId/$siteKey")
                    }
                }
            }
        })
    }

    // ✅ UI内容渲染
    HomeContent(
        uiState = uiState,
        onRefresh = { viewModel.refresh() },
        onMovieClick = { movie ->
            navController.navigate("detail/${movie.vodId}/${movie.site?.key ?: ""}")
        },
        onCategoryClick = { category ->
            navController.navigate("category/${category.typeId}")
        },
        onSearchClick = {
            navController.navigate("search")
        },
        onSettingsClick = {
            navController.navigate("settings")
        },
        onRouteSelect = { route ->
            viewModel.selectRoute(route)
        },
        onShowRouteSelector = {
            viewModel.showRouteSelector()
        },
        onHideRouteSelector = {
            viewModel.hideRouteSelector()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: MovieUiState,
    onRefresh: () -> Unit,
    onMovieClick: (Vod) -> Unit,
    onCategoryClick: (Class) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRouteSelect: (VodConfigUrl) -> Unit,
    onShowRouteSelector: () -> Unit,
    onHideRouteSelector: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = if (uiState.isStoreHouseIndex) uiState.storeHouseName else "OneTV 影视",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            actions = {
                // 线路选择按钮
                if (uiState.isStoreHouseIndex && uiState.availableRoutes.isNotEmpty()) {
                    IconButton(onClick = onShowRouteSelector) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "切换线路"
                        )
                    }
                }

                // 搜索按钮
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                }

                // 设置按钮
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置"
                    )
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingScreen(message = "正在加载配置...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = onRefresh,
                    onBack = { /* 首页不需要返回 */ }
                )
            }
            uiState.isStoreHouseIndex && uiState.selectedRoute == null -> {
                RouteSelectionScreen(
                    routes = uiState.availableRoutes,
                    onRouteSelect = onRouteSelect
                )
            }
            else -> {
                HomeContentScreen(
                    uiState = uiState,
                    onRefresh = onRefresh,
                    onMovieClick = onMovieClick,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // 线路选择对话框
        if (uiState.showRouteSelector) {
            RouteSelector(
                routes = uiState.availableRoutes,
                selectedRoute = uiState.selectedRoute,
                onRouteSelect = { route ->
                    onRouteSelect(route)
                    onHideRouteSelector()
                },
                onDismiss = onHideRouteSelector
            )
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
private fun RouteSelectionScreen(
    routes: List<VodConfigUrl>,
    onRouteSelect: (VodConfigUrl) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "选择线路",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(routes) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRouteSelect(route) }
                ) {
                    Text(
                        text = route.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContentScreen(
    uiState: MovieUiState,
    onRefresh: () -> Unit,
    onMovieClick: (Vod) -> Unit,
    onCategoryClick: (Class) -> Unit
) {
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



