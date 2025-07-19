package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.ConfigSetupViewModel
import top.cywin.onetv.movie.viewmodel.ConfigSetupUiState
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie配置设置页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigSetupScreen(
    navController: NavController,
    viewModel: ConfigSetupViewModel = viewModel {
        ConfigSetupViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ UI内容渲染
    ConfigSetupContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onConfigUrlChange = { url -> viewModel.setConfigUrl(url) },
        onParseConfig = { viewModel.parseConfig() },
        onTestUrl = { url -> viewModel.testConfigUrl(url) },
        onResetConfig = { viewModel.resetConfig() },
        onShowConfigInput = { viewModel.showConfigInput() },
        onHideConfigInput = { viewModel.hideConfigInput() },
        onError = { viewModel.clearError() },
        onNavigateToHome = {
            navController.navigate("home") {
                popUpTo("config_setup") { inclusive = true }
            }
        }
    )
}

@Composable
private fun ConfigSetupContent(
    uiState: ConfigSetupUiState,
    onBack: () -> Unit,
    onConfigUrlChange: (String) -> Unit,
    onParseConfig: () -> Unit,
    onTestUrl: (String) -> Unit,
    onResetConfig: () -> Unit,
    onShowConfigInput: () -> Unit,
    onHideConfigInput: () -> Unit,
    onError: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = "配置设置",
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
            }
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingScreen(
                    message = uiState.loadingMessage.ifEmpty { "正在处理..." },
                    progress = uiState.loadingProgress
                )
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = onResetConfig,
                    onBack = onBack
                )
            }
            !uiState.isConfigured -> {
                ConfigInputScreen(
                    uiState = uiState,
                    onConfigUrlChange = onConfigUrlChange,
                    onParseConfig = onParseConfig,
                    onTestUrl = onTestUrl
                )
            }
            else -> {
                ConfigSuccessScreen(
                    uiState = uiState,
                    onNavigateToHome = onNavigateToHome,
                    onResetConfig = onResetConfig
                )
            }
        }
    }
}

@Composable
private fun ConfigInputScreen(
    uiState: ConfigSetupUiState,
    onConfigUrlChange: (String) -> Unit,
    onParseConfig: () -> Unit,
    onTestUrl: (String) -> Unit
) {
    var configUrl by remember { mutableStateOf(uiState.configUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 配置URL输入
        OutlinedTextField(
            value = configUrl,
            onValueChange = {
                configUrl = it
                onConfigUrlChange(it)
            },
            label = { Text("配置地址") },
            placeholder = { Text("请输入TVBOX配置地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (uiState.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { onTestUrl(configUrl) }) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = "测试")
                    }
                }
            }
        )

        // 测试结果显示
        uiState.testResults.forEach { (url, result) ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isSuccess) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = if (result.isSuccess) "✓ 连接成功" else "✗ 连接失败",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.errorMessage != null) {
                        Text(
                            text = result.errorMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 操作按钮
        Button(
            onClick = onParseConfig,
            enabled = configUrl.isNotEmpty() && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("解析配置")
        }

        // 帮助信息
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "配置说明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 支持标准TVBOX配置格式\n• 支持JSON和TXT格式配置文件\n• 配置将自动验证有效性",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ConfigSuccessScreen(
    uiState: ConfigSetupUiState,
    onNavigateToHome: () -> Unit,
    onResetConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 成功信息
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "✓ 配置解析成功",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "发现 ${uiState.availableSites.size} 个可用站点",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 站点列表
        if (uiState.availableSites.isNotEmpty()) {
            Text(
                text = "可用站点",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            uiState.availableSites.take(5).forEach { site ->
                Card {
                    Text(
                        text = site.name,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.availableSites.size > 5) {
                Text(
                    text = "还有 ${uiState.availableSites.size - 5} 个站点...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 操作按钮
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("进入首页")
            }

            OutlinedButton(
                onClick = onResetConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新配置")
            }
        }
    }
}

// ✅ 按照指南添加必要的辅助Composable函数

@Composable
private fun LoadingScreen(
    message: String,
    progress: Float = 0f
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            } else {
                CircularProgressIndicator()
            }
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
