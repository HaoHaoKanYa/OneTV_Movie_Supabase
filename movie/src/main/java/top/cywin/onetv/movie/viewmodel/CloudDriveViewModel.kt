package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.cloudrive.bean.CloudFile
import android.util.Log

/**
 * 网盘功能UI状态数据类
 */
data class CloudDriveUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cloudDrives: List<CloudDriveConfig> = emptyList(),
    val availableDrives: List<CloudDriveConfig> = emptyList(),
    val currentDrive: CloudDriveConfig? = null,
    val selectedDrive: CloudDriveConfig? = null,
    val currentPath: String = "/",
    val files: List<CloudFile> = emptyList(),
    val currentFiles: List<CloudFile> = emptyList(),
    val canGoBack: Boolean = false,
    val isConnected: Boolean = false
)

/**
 * 网盘配置数据类 - 简化版本
 */
data class CloudDriveConfig(
    val id: String,
    val name: String,
    val type: String,
    val config: Map<String, String>
)

/**
 * OneTV Movie网盘功能ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class CloudDriveViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter

    private val _uiState = MutableStateFlow(CloudDriveUiState())
    val uiState: StateFlow<CloudDriveUiState> = _uiState.asStateFlow()

    private val pathHistory = mutableListOf<String>()
    
    /**
     * 加载网盘配置 - 通过适配器调用FongMi_TV解析系统
     */
    fun loadCloudDrives() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "☁️ 开始加载网盘配置")

                // ✅ 通过适配器获取网盘配置 - 配置管理在FongMi_TV中
                repositoryAdapter.loadCloudDriveConfigs()

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 网盘配置加载请求已发送")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "网盘配置加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网盘配置加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择网盘
     */
    fun selectDrive(drive: CloudDriveConfig) {
        Log.d("ONETV_MOVIE", "☁️ 选择网盘: ${drive.name}")

        _uiState.value = _uiState.value.copy(
            currentDrive = drive,
            currentPath = "/"
        )

        pathHistory.clear()
        loadFiles(drive, "/")
    }

    /**
     * 加载文件列表
     */
    private fun loadFiles(drive: CloudDriveConfig, path: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "📁 加载文件列表: ${drive.name}$path")

                // ✅ 通过适配器获取文件列表 - 文件管理在FongMi_TV中
                repositoryAdapter.getCloudFiles(drive.id, path)

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 文件列表加载请求已发送")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPath = path,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "文件列表加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "文件列表加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 进入目录
     */
    fun enterDirectory(file: CloudFile) {
        if (file.isFolder()) {
            val currentPath = _uiState.value.currentPath
            pathHistory.add(currentPath)

            val newPath = "${currentPath.trimEnd('/')}/${file.name}"

            _uiState.value.currentDrive?.let { drive ->
                loadFiles(drive, newPath)
            }
        }
    }

    /**
     * 返回上级目录
     */
    fun backToParent() {
        if (pathHistory.isNotEmpty()) {
            val parentPath = pathHistory.removeLastOrNull() ?: "/"

            _uiState.value.currentDrive?.let { drive ->
                loadFiles(drive, parentPath)
            }
        }
    }

    /**
     * 获取下载链接
     */
    fun getDownloadUrl(file: CloudFile, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value.currentDrive?.let { drive ->
                    // ✅ 通过适配器获取下载链接 - 下载管理在FongMi_TV中
                    repositoryAdapter.getCloudDownloadUrl(drive.id, file.path) { url ->
                        if (url.isNotEmpty()) {
                            onResult(url)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                error = "获取下载链接失败"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "获取下载链接失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "获取下载链接失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 刷新当前路径
     */
    fun refreshCurrentPath() {
        val currentState = _uiState.value
        currentState.currentDrive?.let { drive ->
            loadFiles(drive, currentState.currentPath)
        }
    }

    /**
     * 刷新当前目录
     */
    fun refreshCurrentDirectory() {
        refreshCurrentPath()
    }

    /**
     * 播放文件
     */
    fun playFile(file: CloudFile) {
        Log.d("ONETV_MOVIE", "▶️ 播放文件: ${file.name}")
        // 这里应该调用播放器逻辑
    }
}
