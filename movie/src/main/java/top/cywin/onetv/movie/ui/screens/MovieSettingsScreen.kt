package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import top.cywin.onetv.movie.data.models.SettingsUiState
import top.cywin.onetv.movie.data.models.VodConfig

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSettingsScreen(
    navController: NavController,
    uiState: SettingsUiState = SettingsUiState(),
    onAddConfig: (String) -> Unit = {},
    onSelectConfig: (VodConfig) -> Unit = {},
    onDeleteConfig: (VodConfig) -> Unit = {},
    onClearCache: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 顶部导航栏
        SettingsTopBar(
            onBackClick = { navController.popBackStack() },
            onAddClick = { showAddDialog = true }
        )
        
        if (uiState.isLoading) {
            // 加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 配置管理
                item {
                    SettingsSection(title = "配置管理") {
                        ConfigList(
                            configs = uiState.configs,
                            currentConfig = uiState.currentConfig,
                            onSelectConfig = onSelectConfig,
                            onDeleteConfig = onDeleteConfig
                        )
                    }
                }
                
                // 缓存管理
                item {
                    SettingsSection(title = "缓存管理") {
                        CacheManagement(
                            cacheSize = uiState.cacheSize,
                            onClearCache = onClearCache
                        )
                    }
                }
                
                // 播放设置
                item {
                    SettingsSection(title = "播放设置") {
                        PlaybackSettings(
                            settings = uiState.playbackSettings
                        )
                    }
                }
                
                // 关于信息
                item {
                    SettingsSection(title = "关于") {
                        AboutInfo()
                    }
                }
            }
        }
    }
    
    // 添加配置对话框
    if (showAddDialog) {
        AddConfigDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { url ->
                onAddConfig(url)
                showAddDialog = false
            }
        )
    }
}

/**
 * 设置页面顶部导航栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "设置",
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
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加配置",
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
 * 设置区域
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 配置列表
 */
@Composable
private fun ConfigList(
    configs: List<VodConfig>,
    currentConfig: VodConfig?,
    onSelectConfig: (VodConfig) -> Unit,
    onDeleteConfig: (VodConfig) -> Unit
) {
    if (configs.isEmpty()) {
        Text(
            text = "暂无配置，请添加配置源",
            color = Color.Gray,
            fontSize = 14.sp
        )
    } else {
        configs.forEach { config ->
            ConfigItem(
                config = config,
                isSelected = currentConfig == config,
                onSelect = { onSelectConfig(config) },
                onDelete = { onDeleteConfig(config) }
            )
            
            if (config != configs.last()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 配置项
 */
@Composable
private fun ConfigItem(
    config: VodConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name.ifEmpty { "未命名配置" },
                color = if (isSelected) Color.Green else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            Text(
                text = config.getSummary(),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        Row {
            if (!isSelected) {
                TextButton(onClick = onSelect) {
                    Text("选择")
                }
            }
            
            IconButton(onClick = onDelete) {
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
 * 缓存管理
 */
@Composable
private fun CacheManagement(
    cacheSize: Long,
    onClearCache: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "缓存大小",
                color = Color.White,
                fontSize = 16.sp
            )
            
            Text(
                text = formatFileSize(cacheSize),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Button(onClick = onClearCache) {
            Text("清空缓存")
        }
    }
}

/**
 * 播放设置
 */
@Composable
private fun PlaybackSettings(
    settings: top.cywin.onetv.movie.data.models.PlaybackSettings
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingSwitch(
            title = "自动播放",
            description = "进入播放页面时自动开始播放",
            checked = settings.autoPlay,
            onCheckedChange = { /* TODO */ }
        )
        
        SettingSwitch(
            title = "自动播放下一集",
            description = "当前集播放完成后自动播放下一集",
            checked = settings.autoNext,
            onCheckedChange = { /* TODO */ }
        )
        
        SettingSwitch(
            title = "记住播放位置",
            description = "记住上次播放的位置",
            checked = settings.rememberPosition,
            onCheckedChange = { /* TODO */ }
        )
        
        SettingSwitch(
            title = "硬件解码",
            description = "使用硬件解码提升播放性能",
            checked = settings.hardwareDecoding,
            onCheckedChange = { /* TODO */ }
        )
    }
}

/**
 * 设置开关
 */
@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 关于信息
 */
@Composable
private fun AboutInfo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "OneTV 点播模块",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "版本: 1.0.0",
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        Text(
            text = "基于TVBOX标准实现的点播功能",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

/**
 * 添加配置对话框
 */
@Composable
private fun AddConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var configUrl by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加配置源") },
        text = {
            OutlinedTextField(
                value = configUrl,
                onValueChange = { configUrl = it },
                label = { Text("配置地址") },
                placeholder = { Text("请输入配置源URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(configUrl) },
                enabled = configUrl.isNotEmpty()
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
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}
