package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.viewmodel.CloudDriveViewModel
import top.cywin.onetv.movie.viewmodel.CloudDriveUiState
import top.cywin.onetv.movie.viewmodel.CloudDriveConfig
import top.cywin.onetv.movie.cloudrive.bean.CloudFile
import top.cywin.onetv.movie.event.NavigationEvent
import top.cywin.onetv.movie.ui.model.FileSortType
import top.cywin.onetv.movie.ui.model.FileViewType
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie云盘浏览页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    navController: NavController,
    viewModel: CloudDriveViewModel = viewModel {
        CloudDriveViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 监听导航事件
    LaunchedEffect(Unit) {
        EventBus.getDefault().register(object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onNavigation(event: NavigationEvent) {
                when (event.action) {
                    "play_cloud_file" -> {
                        val url = event.params["url"] ?: ""
                        val name = event.params["name"] ?: ""
                        navController.navigate("player?url=$url&name=$name")
                    }
                }
            }
        })
    }

    // ✅ UI内容渲染
    CloudDriveContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onDriveSelect = { drive -> viewModel.selectDrive(drive) },
        onFolderEnter = { folder -> viewModel.enterFolder(folder) },
        onFilePlay = { file -> viewModel.playFile(file) },
        onGoBack = { viewModel.goBack() },
        onRefresh = { viewModel.refresh() },
        onSortChange = { sortType -> /* 处理排序变更 */ },
        onViewTypeChange = { viewType -> /* 处理视图类型变更 */ },
        onError = { viewModel.clearError() }
    )
}

@Composable
private fun CloudDriveContent(
    uiState: CloudDriveUiState,
    onBack: () -> Unit,
    onDriveSelect: (CloudDriveConfig) -> Unit,
    onFolderEnter: (CloudFile) -> Unit,
    onFilePlay: (CloudFile) -> Unit,
    onGoBack: () -> Unit,
    onRefresh: () -> Unit,
    onSortChange: (FileSortType) -> Unit,
    onViewTypeChange: (FileViewType) -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = {
                Text(
                    text = uiState.selectedDrive?.name ?: "云盘",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                // 排序按钮
                IconButton(onClick = { /* 显示排序选项 */ }) {
                    Icon(Icons.Default.Sort, contentDescription = "排序")
                }

                // 视图切换按钮
                IconButton(onClick = { /* 切换视图类型 */ }) {
                    Icon(Icons.Default.ViewList, contentDescription = "视图")
                }

                // 刷新按钮
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoading -> {
                LoadingScreen(message = "正在加载云盘配置...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = onRefresh,
                    onBack = onBack
                )
            }
            uiState.selectedDrive == null -> {
                DriveSelectionScreen(
                    drives = uiState.availableDrives,
                    onDriveSelect = onDriveSelect
                )
            }
            else -> {
                FileListScreen(
                    uiState = uiState,
                    onFolderEnter = onFolderEnter,
                    onFilePlay = onFilePlay,
                    onGoBack = onGoBack
                )
            }
        }
    }
}

@Composable
private fun DriveSelectionScreen(
    drives: List<CloudDriveConfig>,
    onDriveSelect: (CloudDriveConfig) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "选择云盘",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(drives) { drive ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDriveSelect(drive) }
                ) {
                    Text(
                        text = drive.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListScreen(
    uiState: CloudDriveUiState,
    onFolderEnter: (CloudFile) -> Unit,
    onFilePlay: (CloudFile) -> Unit,
    onGoBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 返回上级目录按钮
        if (uiState.canGoBack) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoBack() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "返回上级")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("返回上级目录")
                    }
                }
            }
        }

        // 文件和目录列表
        items(uiState.currentFiles) { file ->
            CloudFileItem(
                file = file,
                onClick = {
                    if (file.isFolder()) {
                        onFolderEnter(file)
                    } else {
                        onFilePlay(file)
                    }
                }
            )
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

@Composable
private fun CloudFileItem(
    file: CloudFile,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 文件图标
            Icon(
                imageVector = when {
                    file.isFolder() -> Icons.Default.Folder
                    file.isVideoFile() -> Icons.Default.PlayArrow
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isFolder() -> MaterialTheme.colorScheme.primary
                    file.isVideoFile() -> MaterialTheme.colorScheme.secondary
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

                if (!file.isFolder() && file.size > 0) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 操作指示器
            if (file.isVideoFile()) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else if (file.isFolder()) {
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

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}