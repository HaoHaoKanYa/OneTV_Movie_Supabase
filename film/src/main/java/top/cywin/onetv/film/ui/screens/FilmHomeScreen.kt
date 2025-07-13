package top.cywin.onetv.film.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import top.cywin.onetv.film.catvod.SpiderManager
import top.cywin.onetv.film.data.datasource.RealDataSourceManager
import top.cywin.onetv.film.navigation.FilmRoutes
import top.cywin.onetv.film.ui.theme.FilmTheme
import top.cywin.onetv.film.ui.theme.FilmColors
import top.cywin.onetv.film.ui.theme.FilmDimens

/**
 * Film 首页界面
 *
 * 基于 OneTV 设计风格的现代化影视模块主界面
 * 使用真实数据源，无模拟数据
 *
 * @author OneTV Team
 * @since 2025-07-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmHomeScreen(navController: NavController) {
    Log.d("ONETV_FILM", "FilmHomeScreen 开始渲染")

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var spiderCount by remember { mutableStateOf(0) }
    var siteCount by remember { mutableStateOf(0) }
    var systemStats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // 初始化真实数据源和系统组件
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                Log.d("ONETV_FILM", "开始初始化真实数据源和系统组件")

                // 初始化真实数据源管理器
                val realDataSourceManager = RealDataSourceManager.getInstance()
                realDataSourceManager.initialize(context)

                // 获取真实站点数据
                val realSites = realDataSourceManager.getRealDataSources()
                siteCount = realSites.size

                // 初始化 SpiderManager
                val spiderManager = SpiderManager.getInstance()
                val spiderStats = spiderManager.getStats()
                spiderCount = spiderStats["registered_spiders"] as? Int ?: 0

                // 获取真实数据源统计
                val dataSourceStats = realDataSourceManager.getStats()

                // 构建真实系统统计
                systemStats = mapOf(
                    "spider_stats" to mapOf(
                        "total_spiders" to spiderCount,
                        "registered_spiders" to spiderCount
                    ),
                    "data_source_stats" to dataSourceStats,
                    "site_stats" to mapOf(
                        "total_sites" to siteCount,
                        "active_sites" to realSites.count { it.enabled }
                    ),
                    "system_info" to mapOf(
                        "version" to "2.1.1",
                        "uptime" to System.currentTimeMillis(),
                        "data_source" to "OneTV Official API"
                    )
                )

                isLoading = false

                Log.d("ONETV_FILM", "真实数据初始化完成 - Spider: $spiderCount, Sites: $siteCount")

            } catch (e: Exception) {
                Log.e("ONETV_FILM", "真实数据初始化失败", e)
                errorMessage = "初始化失败: ${e.message}"
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { 
                Text(
                    text = "OneTV 影视解析系统",
                    color = Color.White
                ) 
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        Log.d("ONETV_FILM", "返回直播按钮被点击")
                        try {
                            navController.navigate("main") {
                                popUpTo("film_home") { inclusive = true }
                                launchSingleTop = true
                            }
                            Log.d("ONETV_FILM", "成功导航回直播页面")
                        } catch (e: Exception) {
                            Log.e("ONETV_FILM", "返回直播页面失败", e)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回直播",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate(FilmRoutes.SEARCH)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        navController.navigate(FilmRoutes.SETTINGS)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "设置",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )
        
                // 加载状态或内容区域
                if (isLoading) {
                    item {
                        LoadingSection()
                    }
                } else if (errorMessage != null) {
                    item {
                        ErrorSection(errorMessage = errorMessage!!)
                    }
                } else {
                    // 推荐内容区域
                    item {
                        RecommendedSection()
                    }

                    // 最近观看区域
                    item {
                        RecentWatchSection()
                    }

                    // 系统状态区域
                    item {
                        SystemStatusSection(
                            spiderCount = spiderCount,
                            siteCount = siteCount,
                            systemStats = systemStats
                        )
                    }
                }
            }
        }
    }
}

/**
 * 顶部标题栏
 */
@Composable
private fun FilmTopBar(
    onReturnToLiveTV: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmDimens.topAppBarHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回直播按钮
        OutlinedButton(
            onClick = onReturnToLiveTV,
            modifier = Modifier.height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = FilmColors.Primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.LiveTv,
                contentDescription = "返回直播",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(FilmDimens.spacingSmall))
            Text(
                text = "返回直播",
                style = MaterialTheme.typography.labelMedium
            )
        }

        // 标题
        Text(
            text = "OneTV 影视",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = FilmColors.Primary
            )
        )

        // 搜索按钮
        IconButton(
            onClick = onNavigateToSearch,
            modifier = Modifier.size(FilmDimens.iconButtonSize)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = FilmColors.Primary
            )
        }
    }
}

/**
 * 欢迎区域
 */
@Composable
private fun WelcomeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = FilmColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = FilmDimens.elevationMedium
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FilmColors.Primary.copy(alpha = 0.1f),
                            FilmColors.Secondary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(FilmDimens.spacingLarge),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "欢迎使用 OneTV 影视",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = FilmColors.OnSurface
                    )
                )
                Spacer(modifier = Modifier.height(FilmDimens.spacingSmall))
                Text(
                    text = "基于 FongMi/TV 的完整解析系统，使用真实数据源",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = FilmColors.OnSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * 快速操作区域
 */
@Composable
private fun QuickActionsSection(
    onNavigateToSites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column {
        Text(
            text = "快速操作",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = FilmColors.OnBackground
            ),
            modifier = Modifier.padding(bottom = FilmDimens.spacingMedium)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(FilmDimens.spacingMedium)
        ) {
            items(getQuickActions(onNavigateToSites, onNavigateToHistory, onNavigateToFavorites, onNavigateToSettings)) { action ->
                QuickActionCard(action = action)
            }
        }
    }
}

/**
 * 快速操作卡片
 */
@Composable
private fun QuickActionCard(action: QuickAction) {
    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable { action.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = FilmColors.SurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = FilmDimens.elevationSmall
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FilmDimens.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.title,
                tint = FilmColors.Primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(FilmDimens.spacingSmall))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelSmall,
                color = FilmColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 加载区域
 */
@Composable
private fun LoadingSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FilmColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FilmDimens.spacingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = FilmColors.Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(FilmDimens.spacingMedium))
            Text(
                text = "正在初始化影视解析系统...",
                style = MaterialTheme.typography.bodyLarge,
                color = FilmColors.OnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(FilmDimens.spacingSmall))
            Text(
                text = "正在加载真实数据源",
                style = MaterialTheme.typography.bodyMedium,
                color = FilmColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 错误区域
 */
@Composable
private fun ErrorSection(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FilmColors.Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FilmDimens.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "错误",
                tint = FilmColors.Error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(FilmDimens.spacingMedium))
            Text(
                text = "初始化失败",
                style = MaterialTheme.typography.headlineSmall,
                color = FilmColors.Error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(FilmDimens.spacingSmall))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = FilmColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 推荐内容区域
 */
@Composable
private fun RecommendedSection() {
    Column {
        Text(
            text = "推荐内容",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = FilmColors.OnBackground
            ),
            modifier = Modifier.padding(bottom = FilmDimens.spacingMedium)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = FilmColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(FilmDimens.spacingMedium)
            ) {
                Text(
                    text = "暂无推荐内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FilmColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 最近观看区域
 */
@Composable
private fun RecentWatchSection() {
    Column {
        Text(
            text = "最近观看",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = FilmColors.OnBackground
            ),
            modifier = Modifier.padding(bottom = FilmDimens.spacingMedium)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = FilmColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(FilmDimens.spacingMedium)
            ) {
                Text(
                    text = "暂无观看记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FilmColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 系统状态区域
 */
@Composable
private fun SystemStatusSection(
    spiderCount: Int,
    siteCount: Int,
    systemStats: Map<String, Any>
) {
    Column {
        Text(
            text = "系统状态",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = FilmColors.OnBackground
            ),
            modifier = Modifier.padding(bottom = FilmDimens.spacingMedium)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = FilmColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(FilmDimens.spacingMedium)
            ) {
                SystemStatusItem("解析器数量", "$spiderCount 个")
                SystemStatusItem("影视源数量", "$siteCount 个")
                SystemStatusItem("数据源", "OneTV 官方 API")
                SystemStatusItem("系统版本", "2.1.1")
                SystemStatusItem("运行状态", "正常")
            }
        }
    }
}

/**
 * 系统状态项
 */
@Composable
private fun SystemStatusItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FilmDimens.spacingSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FilmColors.OnSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = FilmColors.OnSurface
        )
    }
}

/**
 * 快速操作数据类
 */
private data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * 获取快速操作列表
 */
private fun getQuickActions(
    onNavigateToSites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
): List<QuickAction> {
    return listOf(
        QuickAction("影视源", Icons.Default.Source, onNavigateToSites),
        QuickAction("观看历史", Icons.Default.History, onNavigateToHistory),
        QuickAction("我的收藏", Icons.Default.Favorite, onNavigateToFavorites),
        QuickAction("设置", Icons.Default.Settings, onNavigateToSettings)
    )
}
