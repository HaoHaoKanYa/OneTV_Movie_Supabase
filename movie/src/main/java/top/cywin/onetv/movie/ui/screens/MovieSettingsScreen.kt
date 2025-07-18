package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MovieSettingsViewModel
import top.cywin.onetv.movie.viewmodel.SettingsUiState
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie设置页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSettingsScreen(
    navController: NavController,
    viewModel: MovieSettingsViewModel = viewModel { MovieSettingsViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val repositoryAdapter = movieApp.repositoryAdapter

    // ✅ 页面初始化时加载设置
    LaunchedEffect(Unit) {
        Log.d("ONETV_MOVIE", "⚙️ MovieSettingsScreen 初始化")
        viewModel.loadSettings()
    }

    SettingsContent(
        uiState = uiState,
        onConfigManagement = {
            navController.navigate("config_management")
        },
        onCacheManagement = {
            navController.navigate("cache_management")
        },
        onHistoryManagement = {
            navController.navigate("history")
        },
        onAbout = {
            navController.navigate("about")
        },
        onClearCache = { viewModel.clearCache() },
        onResetSettings = { viewModel.resetSettings() },
        onExportSettings = { viewModel.exportSettings() },
        onImportSettings = { viewModel.importSettings() },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onConfigManagement: () -> Unit,
    onCacheManagement: () -> Unit,
    onHistoryManagement: () -> Unit,
    onAbout: () -> Unit,
    onClearCache: () -> Unit,
    onResetSettings: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 设置列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 配置管理
            item {
                SettingsGroup(title = "配置管理") {
                    SettingsItem(
                        title = "配置管理",
                        subtitle = "管理视频源配置",
                        icon = Icons.Default.Settings,
                        onClick = onConfigManagement
                    )
                }
            }

            // 缓存管理
            item {
                SettingsGroup(title = "存储管理") {
                    SettingsItem(
                        title = "缓存管理",
                        subtitle = "清理缓存文件",
                        icon = Icons.Default.Storage,
                        onClick = onCacheManagement
                    )
                    SettingsItem(
                        title = "观看历史",
                        subtitle = "管理观看记录",
                        icon = Icons.Default.History,
                        onClick = onHistoryManagement
                    )
                }
            }

            // 数据管理
            item {
                SettingsGroup(title = "数据管理") {
                    SettingsItem(
                        title = "清空缓存",
                        subtitle = "清空所有缓存数据",
                        icon = Icons.Default.Delete,
                        onClick = onClearCache
                    )
                    SettingsItem(
                        title = "重置设置",
                        subtitle = "恢复默认设置",
                        icon = Icons.Default.RestartAlt,
                        onClick = onResetSettings
                    )
                }
            }

            // 备份与恢复
            item {
                SettingsGroup(title = "备份与恢复") {
                    SettingsItem(
                        title = "导出设置",
                        subtitle = "导出配置和设置",
                        icon = Icons.Default.Upload,
                        onClick = onExportSettings
                    )
                    SettingsItem(
                        title = "导入设置",
                        subtitle = "导入配置和设置",
                        icon = Icons.Default.Download,
                        onClick = onImportSettings
                    )
                }
            }

            // 关于
            item {
                SettingsGroup(title = "关于") {
                    SettingsItem(
                        title = "关于应用",
                        subtitle = "版本信息和帮助",
                        icon = Icons.Default.Info,
                        onClick = onAbout
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}