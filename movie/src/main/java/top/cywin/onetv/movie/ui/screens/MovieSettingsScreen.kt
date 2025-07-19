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
    viewModel: MovieSettingsViewModel = viewModel {
        MovieSettingsViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ UI内容渲染
    SettingsContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onSettingUpdate = { key, value -> viewModel.updateSetting(key, value) },
        onResetSettings = { viewModel.resetAllSettings() },
        onExportSettings = { viewModel.exportSettings() },
        onImportSettings = { json -> viewModel.importSettings(json) },
        onClearCache = { viewModel.clearCache() },
        onShowExportDialog = { viewModel.showExportDialog() },
        onHideExportDialog = { viewModel.hideExportDialog() },
        onShowImportDialog = { viewModel.showImportDialog() },
        onHideImportDialog = { viewModel.hideImportDialog() },
        onError = { viewModel.clearError() }
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSettingUpdate: (String, Any) -> Unit,
    onResetSettings: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: (String) -> Unit,
    onClearCache: () -> Unit,
    onShowExportDialog: () -> Unit,
    onHideExportDialog: () -> Unit,
    onShowImportDialog: () -> Unit,
    onHideImportDialog: () -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = "设置",
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
                // 更多操作菜单
                var showMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("导出设置") },
                        onClick = {
                            showMenu = false
                            onExportSettings()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("导入设置") },
                        onClick = {
                            showMenu = false
                            onShowImportDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("重置设置") },
                        onClick = {
                            showMenu = false
                            onResetSettings()
                        }
                    )
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingScreen(message = "正在加载设置...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = { /* 重新加载设置 */ },
                    onBack = onError
                )
            }
            else -> {
                SettingsListContent(
                    settingItems = uiState.settingItems,
                    networkState = uiState.networkState,
                    expandedSections = uiState.expandedSections,
                    onSettingUpdate = onSettingUpdate,
                    onClearCache = onClearCache
                )
            }
        }

        // 导出对话框
        if (uiState.showExportDialog) {
            ExportDialog(
                exportData = uiState.exportData,
                onDismiss = onHideExportDialog
            )
        }

        // 导入对话框
        if (uiState.showImportDialog) {
            ImportDialog(
                onImport = onImportSettings,
                onDismiss = onHideImportDialog
            )
        }
    }
}

@Composable
private fun SettingsListContent(
    settingItems: List<Any>, // SettingItem类型
    networkState: Any?, // NetworkState类型
    expandedSections: Set<String>,
    onSettingUpdate: (String, Any) -> Unit,
    onClearCache: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 网络状态
        if (networkState != null) {
            item {
                NetworkStatusCard(networkState = networkState)
            }
        }

        // 设置项列表
        items(settingItems) { item ->
            // 设置项UI实现
            Text(
                text = "设置项: ${item.toString()}",
                modifier = Modifier.padding(8.dp)
            )
        }

        // 缓存清理按钮
        item {
            Button(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清理缓存")
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(networkState: Any) {
    Card {
        Text(
            text = "网络状态: ${networkState.toString()}",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ExportDialog(
    exportData: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出设置") },
        text = {
            Text("设置已导出: $exportData")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

@Composable
private fun ImportDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var importText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入设置") },
        text = {
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("设置JSON") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onImport(importText)
                    onDismiss()
                }
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

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

// ✅ 按照指南添加必要的辅助Composable函数