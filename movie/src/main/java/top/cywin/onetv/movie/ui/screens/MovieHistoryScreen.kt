package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.HistoryUiState
import top.cywin.onetv.movie.data.models.VodHistory
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.ui.components.MovieListCard

/**
 * 历史记录页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieHistoryScreen(
    navController: NavController,
    uiState: HistoryUiState = HistoryUiState(),
    onTabChange: (Int) -> Unit = {},
    onHistoryClick: (VodHistory) -> Unit = {},
    onFavoriteClick: (VodItem) -> Unit = {},
    onDeleteHistory: (VodHistory) -> Unit = {},
    onClearAllHistory: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        HistoryTopBar(
            onBackClick = { navController.popBackStack() },
            onClearAllClick = onClearAllHistory
        )
        
        // 标签页
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = Color.Black,
            contentColor = Color.White
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { onTabChange(0) },
                text = { Text("观看历史") }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { onTabChange(1) },
                text = { Text("我的收藏") }
            )
        }
        
        if (uiState.isLoading) {
            // 加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            when (uiState.selectedTab) {
                0 -> {
                    // 观看历史
                    if (uiState.watchHistory.isEmpty()) {
                        EmptyContent("暂无观看历史")
                    } else {
                        HistoryList(
                            histories = uiState.watchHistory,
                            onHistoryClick = onHistoryClick,
                            onDeleteClick = onDeleteHistory
                        )
                    }
                }
                1 -> {
                    // 收藏列表
                    if (uiState.favorites.isEmpty()) {
                        EmptyContent("暂无收藏内容")
                    } else {
                        FavoriteList(
                            favorites = uiState.favorites,
                            onFavoriteClick = onFavoriteClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * 历史页面顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    onBackClick: () -> Unit,
    onClearAllClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "历史记录",
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
            IconButton(onClick = onClearAllClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "清空",
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
 * 历史记录列表
 */
@Composable
private fun HistoryList(
    histories: List<VodHistory>,
    onHistoryClick: (VodHistory) -> Unit,
    onDeleteClick: (VodHistory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(histories) { history ->
            HistoryItem(
                history = history,
                onClick = { onHistoryClick(history) },
                onDeleteClick = { onDeleteClick(history) }
            )
        }
    }
}

/**
 * 历史记录项
 */
@Composable
private fun HistoryItem(
    history: VodHistory,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 海报
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(80.dp)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "海报",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            
            // 信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = history.vodName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "观看到: ${history.getEpisodeDisplayText()}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                Text(
                    text = "进度: ${history.getProgressText()}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                Text(
                    text = history.getWatchTimeDescription(),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            // 删除按钮
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * 收藏列表
 */
@Composable
private fun FavoriteList(
    favorites: List<VodItem>,
    onFavoriteClick: (VodItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(favorites) { favorite ->
            MovieListCard(
                movie = favorite,
                onClick = { onFavoriteClick(favorite) }
            )
        }
    }
}



/**
 * 空内容
 */
@Composable
private fun EmptyContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}
