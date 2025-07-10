package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// KotlinPoet专业重构 - 移除hiltViewModel import
// import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.cywin.onetv.movie.data.cloud.CloudDriveManager
import top.cywin.onetv.movie.ui.focus.tvFocusable
import top.cywin.onetv.movie.ui.focus.tvListFocusable
import top.cywin.onetv.movie.viewmodel.CloudDriveViewModel

/**
 * 网盘浏览界面 - 支持多种网盘服务和TV遥控器操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    onNavigateBack: () -> Unit,
    onPlayVideo: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: CloudDriveViewModel = viewModel {
        CloudDriveViewModel(
            context = context,
            cloudDriveManager = top.cywin.onetv.movie.MovieApp.cloudDriveManager
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadCloudDrives()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = {
                Text(
                    text = "网盘资源",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.tvFocusable(
                        onClick = onNavigateBack
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                // 添加网盘按钮
                IconButton(
                    onClick = { viewModel.showAddDriveDialog() },
                    modifier = Modifier.tvFocusable(
                        onClick = { viewModel.showAddDriveDialog() }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加网盘"
                    )
                }
                
                // 刷新按钮
                IconButton(
                    onClick = { viewModel.refreshCurrentPath() },
                    modifier = Modifier.tvFocusable(
                        onClick = { viewModel.refreshCurrentPath() }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error ?: "未知错误",
                    onRetry = { viewModel.loadCloudDrives() }
                )
            }
            
            uiState.cloudDrives.isEmpty() -> {
                EmptyContent(
                    onAddDrive = { viewModel.showAddDriveDialog() }
                )
            }
            
            else -> {
                CloudDriveContent(
                    uiState = uiState,
                    onDriveSelected = { drive -> viewModel.selectDrive(drive) },
                    onFileSelected = { file -> 
                        if (file.isVideoFile()) {
                            viewModel.getDownloadUrl(file) { url ->
                                onPlayVideo(url)
                            }
                        } else if (file.isDirectory) {
                            viewModel.enterDirectory(file)
                        }
                    },
                    onBackToParent = { viewModel.backToParent() }
                )
            }
        }
    }
    
    // 添加网盘对话框
    if (uiState.showAddDriveDialog) {
        AddCloudDriveDialog(
            onDismiss = { viewModel.hideAddDriveDialog() },
            onConfirm = { config -> 
                viewModel.addCloudDrive(config)
                viewModel.hideAddDriveDialog()
            }
        )
    }
}

/**
 * 加载中内容
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "加载网盘资源中...",
                style = MaterialTheme.typography.bodyMedium
            )
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.tvFocusable(
                    onClick = onRetry
                )
            ) {
                Text("重试")
            }
        }
    }
}

/**
 * 空内容
 */
@Composable
private fun EmptyContent(
    onAddDrive: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = "暂无网盘配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "添加您的网盘账号开始浏览资源",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Button(
                onClick = onAddDrive,
                modifier = Modifier.tvFocusable(
                    onClick = onAddDrive
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加网盘")
            }
        }
    }
}

/**
 * 网盘内容
 */
@Composable
private fun CloudDriveContent(
    uiState: CloudDriveUiState,
    onDriveSelected: (CloudDriveManager.CloudDriveConfig) -> Unit,
    onFileSelected: (CloudDriveManager.CloudFile) -> Unit,
    onBackToParent: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 网盘选择器
        if (uiState.cloudDrives.size > 1) {
            CloudDriveSelector(
                drives = uiState.cloudDrives,
                selectedDrive = uiState.selectedDrive,
                onDriveSelected = onDriveSelected
            )
        }
        
        // 路径导航
        if (uiState.currentPath.isNotEmpty()) {
            PathNavigation(
                path = uiState.currentPath,
                onBackToParent = onBackToParent
            )
        }
        
        // 文件列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = uiState.files,
                key = { it.id }
            ) { file ->
                CloudFileItem(
                    file = file,
                    onClick = { onFileSelected(file) },
                    modifier = Modifier.tvListFocusable(
                        onClick = { onFileSelected(file) }
                    )
                )
            }
        }
    }
}

/**
 * 网盘选择器
 */
@Composable
private fun CloudDriveSelector(
    drives: List<CloudDriveManager.CloudDriveConfig>,
    selectedDrive: CloudDriveManager.CloudDriveConfig?,
    onDriveSelected: (CloudDriveManager.CloudDriveConfig) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            drives.forEach { drive ->
                FilterChip(
                    selected = drive == selectedDrive,
                    onClick = { onDriveSelected(drive) },
                    label = {
                        Text(
                            text = drive.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = when (drive.type) {
                                CloudDriveManager.CloudDriveType.ALI_DRIVE -> Icons.Default.Cloud
                                CloudDriveManager.CloudDriveType.BAIDU_PAN -> Icons.Default.CloudQueue
                                CloudDriveManager.CloudDriveType.QUARK_PAN -> Icons.Default.CloudDownload
                                else -> Icons.Default.Storage
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.tvFocusable(
                        onClick = { onDriveSelected(drive) }
                    )
                )
            }
        }
    }
}

/**
 * 路径导航
 */
@Composable
private fun PathNavigation(
    path: String,
    onBackToParent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBackToParent,
                modifier = Modifier.tvFocusable(
                    onClick = onBackToParent
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回上级"
                )
            }

            Text(
                text = if (path.isEmpty()) "根目录" else path,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 云文件项目
 */
@Composable
private fun CloudFileItem(
    file: CloudDriveManager.CloudFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 文件图标
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.isVideoFile() -> Icons.Default.PlayArrow
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isDirectory -> Color(0xFF2196F3)
                    file.isVideoFile() -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            // 文件信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!file.isDirectory) {
                        Text(
                            text = file.getFormattedSize(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (file.updateTime > 0) {
                        Text(
                            text = formatFileTime(file.updateTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 操作按钮
            if (file.isVideoFile()) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else if (file.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 添加网盘对话框
 */
@Composable
private fun AddCloudDriveDialog(
    onDismiss: () -> Unit,
    onConfirm: (CloudDriveManager.CloudDriveConfig) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CloudDriveManager.CloudDriveType.ALI_DRIVE) }
    var baseUrl by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("添加网盘")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("网盘名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 网盘类型选择
                Text(
                    text = "网盘类型",
                    style = MaterialTheme.typography.bodyMedium
                )

                CloudDriveManager.CloudDriveType.values().forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.displayName)
                    }
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("服务器地址") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("访问令牌") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = CloudDriveManager.CloudDriveConfig(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        type = selectedType,
                        baseUrl = baseUrl,
                        token = token
                    )
                    onConfirm(config)
                },
                enabled = name.isNotEmpty() && baseUrl.isNotEmpty() && token.isNotEmpty()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 格式化文件时间
 */
private fun formatFileTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * 网盘UI状态数据类 (需要在ViewModel中定义)
 */
data class CloudDriveUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cloudDrives: List<CloudDriveManager.CloudDriveConfig> = emptyList(),
    val selectedDrive: CloudDriveManager.CloudDriveConfig? = null,
    val files: List<CloudDriveManager.CloudFile> = emptyList(),
    val currentPath: String = "",
    val showAddDriveDialog: Boolean = false
)
