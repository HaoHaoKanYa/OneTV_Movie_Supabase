package top.cywin.onetv.movie.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp

/**
 * 设置页面UI状态数据类
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val cacheSize: String = "0 MB",
    val configUrl: String = "",
    val autoUpdate: Boolean = true,
    val enableCache: Boolean = true,
    val maxCacheSize: Int = 500, // MB
    val clearCacheProgress: Float = 0f,
    val isClearingCache: Boolean = false
)

/**
 * OneTV Movie设置页面ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MovieSettingsViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * 加载设置数据 - 通过适配器调用FongMi_TV解析系统
     */
    fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                Log.d("ONETV_MOVIE", "⚙️ 加载设置数据")

                // ✅ 通过适配器获取缓存信息 - 缓存管理在FongMi_TV中
                repositoryAdapter.getCacheInfo { cacheSize ->
                    _uiState.value = _uiState.value.copy(
                        cacheSize = formatCacheSize(cacheSize)
                    )
                }

                // ✅ 通过适配器获取配置信息 - 配置管理在FongMi_TV中
                repositoryAdapter.getConfigInfo { configUrl ->
                    _uiState.value = _uiState.value.copy(
                        configUrl = configUrl
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

                Log.d("ONETV_MOVIE", "✅ 设置数据加载完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "设置数据加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "设置加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🗑️ 开始清空缓存")
                _uiState.value = _uiState.value.copy(
                    isClearingCache = true,
                    clearCacheProgress = 0f
                )

                // ✅ 通过适配器清空缓存 - 缓存管理在FongMi_TV中
                repositoryAdapter.clearAllCache { progress ->
                    _uiState.value = _uiState.value.copy(
                        clearCacheProgress = progress
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    clearCacheProgress = 1f,
                    cacheSize = "0 MB"
                )

                Log.d("ONETV_MOVIE", "✅ 缓存清空完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "缓存清空失败", e)
                _uiState.value = _uiState.value.copy(
                    isClearingCache = false,
                    error = "缓存清空失败: ${e.message}"
                )
            }
        }
    }


    /**
     * 更新配置URL
     */
    fun updateConfigUrl(url: String) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔗 更新配置URL: $url")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // ✅ 通过适配器更新配置 - 配置管理在FongMi_TV中
                repositoryAdapter.updateConfigUrl(url)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configUrl = url,
                    error = null
                )

                Log.d("ONETV_MOVIE", "✅ 配置URL更新成功")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置URL更新失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置更新失败: ${e.message}"
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
     * 格式化缓存大小
     */
    private fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024}MB"
            else -> "${bytes / 1024 / 1024 / 1024}GB"
        }
    }
}
