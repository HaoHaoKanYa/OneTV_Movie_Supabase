package top.cywin.onetv.movie.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import top.cywin.onetv.movie.data.cloud.CloudDriveManager
import top.cywin.onetv.movie.ui.screens.CloudDriveUiState
import javax.inject.Inject

/**
 * 网盘功能ViewModel
 * 管理网盘配置、文件浏览、下载等功能
 */
@HiltViewModel
class CloudDriveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudDriveManager: CloudDriveManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CloudDriveUiState())
    val uiState: StateFlow<CloudDriveUiState> = _uiState.asStateFlow()
    
    private val cloudDriveConfigs = mutableListOf<CloudDriveManager.CloudDriveConfig>()
    private val pathHistory = mutableListOf<String>()
    
    /**
     * 加载网盘配置
     */
    fun loadCloudDrives() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 从本地存储加载网盘配置
                val drives = loadSavedCloudDrives()
                cloudDriveConfigs.clear()
                cloudDriveConfigs.addAll(drives)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cloudDrives = drives,
                    selectedDrive = drives.firstOrNull()
                )
                
                // 如果有网盘配置，加载根目录
                drives.firstOrNull()?.let { drive ->
                    loadFiles(drive, "")
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载网盘配置失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 选择网盘
     */
    fun selectDrive(drive: CloudDriveManager.CloudDriveConfig) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedDrive = drive,
                currentPath = ""
            )
            
            pathHistory.clear()
            loadFiles(drive, "")
        }
    }
    
    /**
     * 加载文件列表
     */
    private fun loadFiles(drive: CloudDriveManager.CloudDriveConfig, path: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = cloudDriveManager.getFileList(drive, "root", path)
                
                if (result.success && result.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        files = result.data,
                        currentPath = path
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error ?: "加载文件列表失败"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载文件列表失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 进入目录
     */
    fun enterDirectory(file: CloudDriveManager.CloudFile) {
        if (file.isDirectory) {
            val currentPath = _uiState.value.currentPath
            pathHistory.add(currentPath)
            
            val newPath = if (currentPath.isEmpty()) {
                file.name
            } else {
                "$currentPath/${file.name}"
            }
            
            _uiState.value.selectedDrive?.let { drive ->
                loadFiles(drive, newPath)
            }
        }
    }
    
    /**
     * 返回上级目录
     */
    fun backToParent() {
        if (pathHistory.isNotEmpty()) {
            val parentPath = pathHistory.removeLastOrNull() ?: ""
            
            _uiState.value.selectedDrive?.let { drive ->
                loadFiles(drive, parentPath)
            }
        }
    }
    
    /**
     * 刷新当前路径
     */
    fun refreshCurrentPath() {
        val currentState = _uiState.value
        currentState.selectedDrive?.let { drive ->
            loadFiles(drive, currentState.currentPath)
        }
    }
    
    /**
     * 获取下载链接
     */
    fun getDownloadUrl(file: CloudDriveManager.CloudFile, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value.selectedDrive?.let { drive ->
                    val result = cloudDriveManager.getDownloadUrl(drive, file)
                    
                    if (result.success && result.data != null) {
                        onResult(result.data)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = result.error ?: "获取下载链接失败"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "获取下载链接失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 添加网盘配置
     */
    fun addCloudDrive(config: CloudDriveManager.CloudDriveConfig) {
        viewModelScope.launch {
            try {
                cloudDriveConfigs.add(config)
                saveCloudDrives(cloudDriveConfigs)
                
                _uiState.value = _uiState.value.copy(
                    cloudDrives = cloudDriveConfigs.toList(),
                    selectedDrive = config
                )
                
                // 加载新添加网盘的根目录
                loadFiles(config, "")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "添加网盘失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 删除网盘配置
     */
    fun removeCloudDrive(config: CloudDriveManager.CloudDriveConfig) {
        viewModelScope.launch {
            try {
                cloudDriveConfigs.remove(config)
                saveCloudDrives(cloudDriveConfigs)
                
                val newSelectedDrive = if (_uiState.value.selectedDrive == config) {
                    cloudDriveConfigs.firstOrNull()
                } else {
                    _uiState.value.selectedDrive
                }
                
                _uiState.value = _uiState.value.copy(
                    cloudDrives = cloudDriveConfigs.toList(),
                    selectedDrive = newSelectedDrive,
                    files = if (newSelectedDrive == null) emptyList() else _uiState.value.files,
                    currentPath = if (newSelectedDrive == null) "" else _uiState.value.currentPath
                )
                
                // 如果删除的是当前选中的网盘，重新加载
                if (_uiState.value.selectedDrive == config) {
                    newSelectedDrive?.let { drive ->
                        loadFiles(drive, "")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除网盘失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 显示添加网盘对话框
     */
    fun showAddDriveDialog() {
        _uiState.value = _uiState.value.copy(showAddDriveDialog = true)
    }
    
    /**
     * 隐藏添加网盘对话框
     */
    fun hideAddDriveDialog() {
        _uiState.value = _uiState.value.copy(showAddDriveDialog = false)
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 从本地存储加载网盘配置
     */
    private fun loadSavedCloudDrives(): List<CloudDriveManager.CloudDriveConfig> {
        return try {
            // 从SharedPreferences加载配置
            val configJson = context.getSharedPreferences("cloud_drives", Context.MODE_PRIVATE)
                .getString("saved_drives", null)

            if (!configJson.isNullOrEmpty()) {
                Json.decodeFromString<List<CloudDriveManager.CloudDriveConfig>>(configJson)
            } else {
                emptyList() // 不提供硬编码的示例配置
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 保存网盘配置到本地存储
     */
    private fun saveCloudDrives(drives: List<CloudDriveManager.CloudDriveConfig>) {
        try {
            val configJson = Json.encodeToString(drives)
            context.getSharedPreferences("cloud_drives", Context.MODE_PRIVATE)
                .edit()
                .putString("saved_drives", configJson)
                .apply()
        } catch (e: Exception) {
            // 保存失败不影响主流程，但应该记录日志
        }
    }
}
