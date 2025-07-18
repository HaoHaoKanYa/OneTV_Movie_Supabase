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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MovieSettingsViewModel
import top.cywin.onetv.movie.viewmodel.SettingsUiState
import top.cywin.onetv.movie.viewmodel.VodConfigUrl
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie配置管理页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieConfigScreen(
    navController: NavController,
    viewModel: MovieSettingsViewModel = viewModel { MovieSettingsViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val repositoryAdapter = movieApp.repositoryAdapter

    // ✅ 页面初始化时加载配置列表
    LaunchedEffect(Unit) {
        Log.d("ONETV_MOVIE", "📋 MovieConfigScreen 初始化")
        viewModel.loadConfigList()
    }

    // ✅ UI状态处理
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在加载配置列表...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error ?: "未知错误",
                onRetry = { viewModel.loadConfigList() },
                onBack = { navController.popBackStack() }
            )
        }
        else -> {
            ConfigManagementContent(
                uiState = uiState,
                onConfigSelect = { config -> viewModel.selectConfig(config) },
                onConfigAdd = { url -> viewModel.addCustomConfig(url) },
                onConfigDelete = { config -> viewModel.deleteConfig(config) },
                onConfigTest = { config -> viewModel.testConfig(config) },
                onRefresh = { viewModel.refreshConfigs() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun ConfigManagementContent(
    uiState: SettingsUiState,
    onConfigSelect: (VodConfigUrl) -> Unit,
    onConfigAdd: (String) -> Unit,
    onConfigDelete: (VodConfigUrl) -> Unit,
    onConfigTest: (VodConfigUrl) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newConfigUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "配置管理",
                style = MaterialTheme.typography.titleLarge
            )
            Row {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加配置")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 配置列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.configList) { config ->
                ConfigItem(
                    config = config,
                    isSelected = config == uiState.selectedConfig,
                    onSelect = { onConfigSelect(config) },
                    onTest = { onConfigTest(config) },
                    onDelete = { onConfigDelete(config) }
                )
            }
        }
    }

    // 添加配置对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加配置") },
            text = {
                OutlinedTextField(
                    value = newConfigUrl,
                    onValueChange = { newConfigUrl = it },
                    label = { Text("配置地址") },
                    placeholder = { Text("请输入TVBOX配置地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newConfigUrl.isNotBlank()) {
                            onConfigAdd(newConfigUrl)
                            newConfigUrl = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ConfigItem(
    config: VodConfigUrl,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = config.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onTest) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = "测试")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "当前使用的配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
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
