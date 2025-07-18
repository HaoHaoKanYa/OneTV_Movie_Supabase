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
    viewModel: ConfigSetupViewModel = viewModel { ConfigSetupViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val repositoryAdapter = movieApp.repositoryAdapter

    // ✅ UI状态处理
    when {
        uiState.isLoading -> {
            LoadingScreen(message = "正在验证配置...")
        }
        uiState.error != null -> {
            ErrorScreen(
                error = uiState.error,
                onRetry = { viewModel.resetConfig() },
                onBack = { navController.popBackStack() }
            )
        }
        else -> {
            ConfigSetupContent(
                uiState = uiState,
                onConfigUrlChange = { url -> viewModel.validateConfigUrl(url) },
                onSaveConfig = { url ->
                    viewModel.saveConfig(url) {
                        navController.popBackStack()
                    }
                },
                onUseBuiltInConfig = {
                    // 使用内置配置
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun ConfigSetupContent(
    uiState: ConfigSetupUiState,
    onConfigUrlChange: (String) -> Unit,
    onSaveConfig: (String) -> Unit,
    onUseBuiltInConfig: () -> Unit,
    onBack: () -> Unit
) {
    var configUrl by remember { mutableStateOf(uiState.configUrl) }

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
                text = "配置设置",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

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
            isError = uiState.validationResult?.contains("失败") == true
        )

        // 验证结果显示
        if (uiState.validationResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.validationResult,
                color = if (uiState.isConfigValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 操作按钮
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onSaveConfig(configUrl) },
                enabled = uiState.isConfigValid && !uiState.isValidating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("保存配置")
            }

            OutlinedButton(
                onClick = onUseBuiltInConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("使用内置配置")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 帮助信息
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
