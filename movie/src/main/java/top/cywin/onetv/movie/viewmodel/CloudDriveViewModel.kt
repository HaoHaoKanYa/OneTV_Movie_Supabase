package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.adapter.ViewModelAdapter
import top.cywin.onetv.movie.event.CloudDriveConfigEvent
import top.cywin.onetv.movie.event.CloudDriveEvent
import top.cywin.onetv.movie.event.ErrorEvent
import top.cywin.onetv.movie.event.NavigationEvent
import top.cywin.onetv.movie.ui.model.CloudDriveConfig
import top.cywin.onetv.movie.ui.model.CloudFile

/**
 * OneTV Movie云盘ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV云盘功能，完整的事件驱动架构
 */
class CloudDriveViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_CLOUD_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(CloudDriveUiState())
    val uiState: StateFlow<CloudDriveUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ CloudDriveViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)

        // ✅ 初始化加载云盘配置
        loadCloudDriveConfigs()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 CloudDriveViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }
    // ===== EventBus事件监听 =====

    /**
     * 监听云盘配置事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCloudDriveConfig(event: CloudDriveConfigEvent) {
        Log.d(TAG, "📡 收到云盘配置事件: success=${event.isSuccess}")

        if (event.isSuccess) {
            val driveConfigs = event.configs.map { config ->
                ViewModelAdapter.convertToCloudDriveConfig(config)
            }.filterNotNull()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                driveConfigs = driveConfigs,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "云盘配置加载失败"
            )
        }
    }

    /**
     * 监听云盘文件事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCloudDriveFiles(event: CloudDriveEvent) {
        Log.d(TAG, "📡 收到云盘文件事件: driveId=${event.driveId}, success=${event.isSuccess}")

        if (event.isSuccess) {
            val cloudFiles = event.files.map { file ->
                ViewModelAdapter.convertToCloudFile(file)
            }.filterNotNull()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentFiles = cloudFiles,
                currentPath = event.path,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "文件列表加载失败"
            )
        }
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 加载云盘配置
     */
    fun loadCloudDriveConfigs() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "☁️ 开始加载云盘配置")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 通过适配器加载云盘配置
                repositoryAdapter.loadCloudDriveConfigs()

            } catch (e: Exception) {
                Log.e(TAG, "💥 云盘配置加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "云盘配置加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择云盘
     */
    fun selectDrive(driveConfig: CloudDriveConfig) {
        Log.d(TAG, "☁️ 选择云盘: ${driveConfig.name}")

        _uiState.value = _uiState.value.copy(
            selectedDrive = driveConfig,
            currentPath = "/"
        )

        // 加载根目录文件
        loadFiles(driveConfig, "/")
    }

    /**
     * 加载文件列表
     */
    fun loadFiles(drive: CloudDriveConfig, path: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📁 加载文件列表: ${drive.name}$path")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // ✅ 通过适配器获取文件列表
                repositoryAdapter.getCloudFiles(drive.id, path)

            } catch (e: Exception) {
                Log.e(TAG, "💥 文件列表加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "文件列表加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 进入文件夹
     */
    fun enterFolder(folder: CloudFile) {
        if (folder.isDirectory) {
            val currentDrive = _uiState.value.selectedDrive ?: return
            loadFiles(currentDrive, folder.path)
        }
    }

    /**
     * 返回上级目录
     */
    fun goBack() {
        val currentPath = _uiState.value.currentPath
        if (currentPath != "/" && currentPath.isNotEmpty()) {
            val parentPath = currentPath.substringBeforeLast("/")
            val finalPath = if (parentPath.isEmpty()) "/" else parentPath

            val currentDrive = _uiState.value.selectedDrive ?: return
            loadFiles(currentDrive, finalPath)
        }
    }

    /**
     * 播放文件
     */
    fun playFile(file: CloudFile) {
        if (!file.isDirectory && file.playUrl != null) {
            Log.d(TAG, "▶️ 播放文件: ${file.name}")

            // 通过EventBus通知播放
            EventBus.getDefault().post(NavigationEvent(
                action = "play_cloud_file",
                params = mapOf(
                    "url" to file.playUrl,
                    "name" to file.name
                )
            ))
        }
    }

    /**
     * 刷新当前目录
     */
    fun refresh() {
        val currentDrive = _uiState.value.selectedDrive ?: return
        val currentPath = _uiState.value.currentPath
        loadFiles(currentDrive, currentPath)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 云盘UI状态数据类
 */
data class CloudDriveUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,

    // 云盘配置
    val driveConfigs: List<CloudDriveConfig> = emptyList(),
    val selectedDrive: CloudDriveConfig? = null,

    // 文件列表
    val currentFiles: List<CloudFile> = emptyList(),
    val currentPath: String = "/",

    // UI控制
    val showDriveSelector: Boolean = false,
    val sortType: FileSortType = FileSortType.NAME,
    val viewType: FileViewType = FileViewType.LIST
)

enum class FileSortType {
    NAME, SIZE, DATE
}

enum class FileViewType {
    LIST, GRID
}
