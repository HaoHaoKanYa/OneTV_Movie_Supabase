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
import top.cywin.onetv.movie.viewmodel.ConfigSetupViewModel
import top.cywin.onetv.movie.viewmodel.ConfigSetupUiState
import top.cywin.onetv.movie.ui.model.SiteInfo
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie配置管理页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieConfigScreen(
    navController: NavController,
    viewModel: ConfigSetupViewModel = viewModel {
        ConfigSetupViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ UI内容渲染
    MovieConfigContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onConfigUrlChange = { url -> viewModel.setConfigUrl(url) },
        onParseConfig = { viewModel.parseConfig() },
        onTestUrl = { url -> viewModel.testConfigUrl(url) },
        onResetConfig = { viewModel.resetConfig() },
        onError = { viewModel.clearError() }
    )
}

@Composable
private fun MovieConfigContent(
    uiState: ConfigSetupUiState,
    onBack: () -> Unit,
    onConfigUrlChange: (String) -> Unit,
    onParseConfig: () -> Unit,
    onTestUrl: (String) -> Unit,
    onResetConfig: () -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = "影视配置",
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
                // 重置按钮
                IconButton(onClick = onResetConfig) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重置配置"
                    )
                }
            }
        )

        // 内容区域
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前配置状态
            item {
                ConfigStatusCard(
                    isConfigured = uiState.isConfigured,
                    currentConfig = uiState.currentConfig,
                    availableSites = uiState.availableSites
                )
            }

            // 配置输入区域
            item {
                ConfigInputCard(
                    configUrl = uiState.configUrl,
                    onConfigUrlChange = onConfigUrlChange,
                    onParseConfig = onParseConfig,
                    onTestUrl = onTestUrl,
                    isLoading = uiState.isLoading,
                    isTesting = uiState.isTesting,
                    testResults = uiState.testResults
                )
            }

            // 错误信息
            if (uiState.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigStatusCard(
    isConfigured: Boolean,
    currentConfig: Any?, // VodConfig类型
    availableSites: List<SiteInfo>
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "配置状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isConfigured) {
                Text(
                    text = "✓ 配置已加载",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "可用站点: ${availableSites.size} 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "✗ 未配置",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "请添加配置URL",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigInputCard(
    configUrl: String,
    onConfigUrlChange: (String) -> Unit,
    onParseConfig: () -> Unit,
    onTestUrl: (String) -> Unit,
    isLoading: Boolean,
    isTesting: Boolean,
    testResults: Map<String, Any> // ApiTestResult类型
) {
    var inputUrl by remember { mutableStateOf(configUrl) }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "配置管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // URL输入
            OutlinedTextField(
                value = inputUrl,
                onValueChange = {
                    inputUrl = it
                    onConfigUrlChange(it)
                },
                label = { Text("配置地址") },
                placeholder = { Text("请输入TVBOX配置地址") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { onTestUrl(inputUrl) }) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "测试")
                        }
                    }
                }
            )

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onParseConfig,
                    enabled = inputUrl.isNotEmpty() && !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("解析配置")
                }
            }

            // 测试结果
            testResults.forEach { (url, result) ->
                // 显示测试结果的UI
                Text(
                    text = "测试结果: $url",
                    style = MaterialTheme.typography.bodySmall
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
