package top.cywin.onetv.movie.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.config.DefaultConfigProvider
import top.cywin.onetv.movie.viewmodel.ConfigSetupViewModel

/**
 * 点播配置界面
 * 支持内置源和外置源配置管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieConfigScreen(
    navController: NavController,
    viewModel: ConfigSetupViewModel = viewModel {
        ConfigSetupViewModel(
            appConfigManager = top.cywin.onetv.movie.MovieApp.getInstance().appConfigManager
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showAddConfigDialog by remember { mutableStateOf(false) }
    var customConfigUrl by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "点播配置管理",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配置状态卡片
        ConfigStatusCard(
            isLoading = uiState.isLoading,
            configStatus = uiState.configStatus,
            onRefresh = { viewModel.refreshConfig() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 内置源配置
        BuiltInSourceCard(
            onUseBuiltInSource = { viewModel.useBuiltInSource() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 外置源配置
        ExternalSourceCard(
            onAddCustomConfig = { showAddConfigDialog = true },
            exampleUrls = emptyList() // 生产环境不提供示例URL
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配置历史
        ConfigHistoryCard()
    }
    
    // 添加自定义配置对话框
    if (showAddConfigDialog) {
        AddConfigDialog(
            configUrl = customConfigUrl,
            onConfigUrlChange = { customConfigUrl = it },
            onConfirm = { 
                viewModel.addCustomConfig(customConfigUrl)
                showAddConfigDialog = false
                customConfigUrl = ""
            },
            onDismiss = { 
                showAddConfigDialog = false
                customConfigUrl = ""
            }
        )
    }
}

/**
 * 配置状态卡片
 */
@Composable
private fun ConfigStatusCard(
    isLoading: Boolean,
    configStatus: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = "配置状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "刷新配置"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正在加载配置...")
                }
            } else {
                Text(
                    text = configStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 内置源配置卡片
 */
@Composable
private fun BuiltInSourceCard(
    onUseBuiltInSource: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "内置源",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "内置视频源",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "使用存储在Supabase存储桶中的官方配置文件，包含精选的高质量视频源。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onUseBuiltInSource,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.GetApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("使用内置源")
            }
        }
    }
}

/**
 * 外置源配置卡片
 */
@Composable
private fun ExternalSourceCard(
    onAddCustomConfig: () -> Unit,
    exampleUrls: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "外置源",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "外置视频源",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "添加自定义的TVBOX兼容配置文件，支持GitHub、Gitee等托管平台。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onAddCustomConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加自定义配置")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "示例配置地址：",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            exampleUrls.take(2).forEach { url ->
                Text(
                    text = "• ${url.take(50)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }
}

/**
 * 配置历史卡片
 */
@Composable
private fun ConfigHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "配置历史",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "配置历史",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "暂无配置历史记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 添加配置对话框
 */
@Composable
private fun AddConfigDialog(
    configUrl: String,
    onConfigUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加自定义配置")
        },
        text = {
            Column {
                Text("请输入TVBOX兼容的配置文件URL：")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = configUrl,
                    onValueChange = onConfigUrlChange,
                    label = { Text("配置URL") },
                    placeholder = { Text("https://example.com/config.json") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = configUrl.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
