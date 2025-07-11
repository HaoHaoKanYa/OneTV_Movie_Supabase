package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
// KotlinPoet专业重构 - 移除hiltViewModel import
// import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import android.util.Log
import top.cywin.onetv.movie.data.models.MovieUiState
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.data.models.VodClass
import top.cywin.onetv.movie.data.models.VodSite
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
            Log.d("ONETV_MOVIE", "LaunchedEffect: 开始初始化检查和配置更新")
            // 检查并更新配置 (智能缓存策略)
            viewModel.checkAndUpdateConfig()
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
        },
        onSiteSwitch = { siteKey ->
            viewModel.switchSite(siteKey)
        }
    )
}

@Composable
private fun MovieHomeContent(
    navController: NavController,
    uiState: MovieUiState,
    onRefresh: () -> Unit,
    onCategoryClick: (VodClass) -> Unit,
    onMovieClick: (VodItem) -> Unit,
    onSiteSwitch: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        MovieTopBar(
            title = "影视点播",
            currentSite = uiState.currentSite,
            availableSites = uiState.availableSites,
            onBackToLiveClick = {
                // 返回直播，回到上一次播放的频道
                Log.d("ONETV_MOVIE", "用户点击返回直播按钮")
                try {
                    navController.navigate("main") {
                        // 清除点播页面的回退栈，避免循环导航
                        popUpTo("movie_home") { inclusive = true }
                        // 确保不会重复添加main页面
                        launchSingleTop = true
                    }
                    Log.d("ONETV_MOVIE", "成功导航回直播页面，将恢复上一次播放的频道")
                } catch (e: Exception) {
                    Log.e("ONETV_MOVIE", "返回直播页面失败", e)
                }
            },
            onSearchClick = {
                navController.navigate(MovieRoutes.SEARCH)
            },
            onSettingsClick = {
                navController.navigate(MovieRoutes.SETTINGS)
            },
            onSiteSwitch = onSiteSwitch
        )
        
        if (uiState.error != null) {
            // 错误状态
            ErrorContent(
                error = uiState.error,
                onRetry = onRefresh
            )
        } else {
            // 检查是否有任何内容
            val hasAnyContent = uiState.recommendMovies.isNotEmpty() ||
                               uiState.quickCategories.isNotEmpty() ||
                               uiState.homeCategories.isNotEmpty()

            if (hasAnyContent) {
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
            } else {
                // 显示空状态或默认内容（包括加载状态）
                EmptyStateContent(
                    onRefresh = onRefresh,
                    navController = navController,
                    isLoading = uiState.isLoading
                )
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
    currentSite: VodSite?,
    availableSites: List<VodSite>,
    onBackToLiveClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSiteSwitch: (String) -> Unit
) {
    var showSiteSelector by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                // 显示当前站点信息
                if (currentSite != null) {
                    Text(
                        text = "当前线路: ${currentSite.name}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        },
        navigationIcon = {
            // 返回直播按钮 - 返回上一次播放的频道
            IconButton(
                onClick = onBackToLiveClick,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "返回直播",
                        tint = Color.White
                    )
                    Text(
                        text = "直播",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        actions = {
            // 线路选择按钮 - 始终显示，方便调试
            IconButton(
                onClick = {
                    Log.d("ONETV_MOVIE", "线路选择按钮被点击，可用站点数: ${availableSites.size}")
                    availableSites.forEach { site ->
                        Log.d("ONETV_MOVIE", "站点: ${site.name} (${site.key})")
                    }
                    showSiteSelector = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "选择线路",
                    tint = Color.White
                )
            }

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

    // 线路选择器 - 始终显示当点击时
    if (showSiteSelector) {
        SiteSelector(
            availableSites = availableSites,
            currentSite = currentSite,
            onSiteSelected = { site ->
                onSiteSwitch(site.key)
                showSiteSelector = false
            },
            onDismiss = { showSiteSelector = false }
        )
    }
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
 * 空状态内容组件
 */
@Composable
private fun EmptyStateContent(
    onRefresh: () -> Unit,
    navController: NavController,
    isLoading: Boolean = false
) {
    // 动态加载文本效果
    var loadingText by remember { mutableStateOf("正在加载影视资源") }
    var dotCount by remember { mutableStateOf(0) }

    // 动态更新加载文本（仅在加载时）
    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (isLoading) {
                kotlinx.coroutines.delay(500)
                dotCount = (dotCount + 1) % 4
                loadingText = "正在加载影视资源" + ".".repeat(dotCount)
            }
        } else {
            loadingText = "暂无影视资源"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "欢迎使用OneTV点播",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 加载状态区域
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 在加载时显示小的进度指示器
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 动态加载文本
            Text(
                text = loadingText,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("刷新")
            }

            Button(
                onClick = {
                    navController.navigate(MovieRoutes.SETTINGS)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Text("设置")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 功能说明
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "功能特色",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            val features = listOf(
                "🎬 海量影视资源",
                "🔍 智能搜索推荐",
                "📱 多设备同步",
                "⚡ 高清流畅播放"
            )

            features.forEach { feature ->
                Text(
                    text = feature,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
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

/**
 * 站点选择器
 */
@Composable
private fun SiteSelector(
    availableSites: List<VodSite>,
    currentSite: VodSite?,
    onSiteSelected: (VodSite) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择线路",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (availableSites.isEmpty()) {
                // 没有站点时显示调试信息
                Text(
                    text = "暂无可用线路\n当前站点: ${currentSite?.name ?: "未知"}\n站点数量: ${availableSites.size}",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(availableSites) { site ->
                        SiteItem(
                            site = site,
                            isSelected = site.key == currentSite?.key,
                            onClick = { onSiteSelected(site) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 站点项目
 */
@Composable
private fun SiteItem(
    site: VodSite,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = site.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = "站点类型: ${getSiteTypeText(site.type)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 获取站点类型文本
 */
private fun getSiteTypeText(type: Int): String {
    return when (type) {
        0 -> "爬虫"
        1 -> "CMS"
        3 -> "APP"
        4 -> "Alist"
        else -> "未知"
    }
}

