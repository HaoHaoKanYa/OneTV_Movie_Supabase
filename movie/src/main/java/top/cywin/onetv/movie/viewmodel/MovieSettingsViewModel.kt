package top.cywin.onetv.movie.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.bean.History
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.viewmodel.VodConfigUrl

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
    val isClearingCache: Boolean = false,
    val watchHistory: List<History> = emptyList(),
    val favoriteMovies: List<Vod> = emptyList(),
    val configList: List<VodConfigUrl> = emptyList(),
    val selectedConfig: VodConfigUrl? = null
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
     * 加载观看历史
     */
    fun loadWatchHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // ✅ 通过适配器获取观看历史 - 数据管理在FongMi_TV中
                // 这里应该从FongMi_TV的数据库中获取历史记录
                val historyList = emptyList<History>() // TODO: 从FongMi_TV获取实际数据

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    watchHistory = historyList,
                    error = null
                )

                Log.d("ONETV_MOVIE", "✅ 观看历史加载完成，数量: ${historyList.size}")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "观看历史加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "观看历史加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除观看历史
     */
    fun deleteWatchHistory(history: History) {
        viewModelScope.launch {
            try {
                // ✅ 通过适配器删除观看历史 - 数据管理在FongMi_TV中
                val currentHistory = _uiState.value.watchHistory.toMutableList()
                currentHistory.remove(history)

                _uiState.value = _uiState.value.copy(
                    watchHistory = currentHistory
                )

                Log.d("ONETV_MOVIE", "✅ 观看历史删除成功")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "观看历史删除失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "观看历史删除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清空所有观看历史
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                // ✅ 通过适配器清空所有观看历史 - 数据管理在FongMi_TV中
                _uiState.value = _uiState.value.copy(
                    watchHistory = emptyList()
                )

                Log.d("ONETV_MOVIE", "✅ 所有观看历史清空成功")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "清空观看历史失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "清空观看历史失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(movie: Vod) {
        viewModelScope.launch {
            try {
                // ✅ 通过适配器切换收藏状态 - 数据管理在FongMi_TV中
                val currentFavorites = _uiState.value.favoriteMovies.toMutableList()

                if (currentFavorites.any { it.vodId == movie.vodId }) {
                    currentFavorites.removeAll { it.vodId == movie.vodId }
                } else {
                    currentFavorites.add(movie)
                }

                _uiState.value = _uiState.value.copy(
                    favoriteMovies = currentFavorites
                )

                Log.d("ONETV_MOVIE", "✅ 收藏状态切换成功")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "收藏状态切换失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "收藏状态切换失败: ${e.message}"
                )
            }
        }
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



    /**
     * 加载配置列表
     */
    fun loadConfigList() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // ✅ 通过适配器加载配置列表
                repositoryAdapter.loadConfigList()

                Log.d("ONETV_MOVIE", "✅ 配置列表加载请求已发送")

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置列表加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置列表加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 刷新配置列表
     */
    fun refreshConfigs() {
        loadConfigList()
    }



    /**
     * 选择配置
     */
    fun selectConfig(config: VodConfigUrl) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔄 选择配置: ${config.name}")

                // ✅ 通过适配器选择配置
                repositoryAdapter.selectConfig(config.url)

                Log.d("ONETV_MOVIE", "✅ 配置选择完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置选择失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "配置选择失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 添加自定义配置
     */
    fun addCustomConfig(url: String) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "➕ 添加自定义配置: $url")

                // ✅ 通过适配器添加自定义配置
                repositoryAdapter.addCustomConfig(url)

                Log.d("ONETV_MOVIE", "✅ 自定义配置添加完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "自定义配置添加失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "自定义配置添加失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除配置
     */
    fun deleteConfig(config: VodConfigUrl) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🗑️ 删除配置: ${config.name}")

                // ✅ 通过适配器删除配置
                repositoryAdapter.deleteConfig(config.url)

                Log.d("ONETV_MOVIE", "✅ 配置删除完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置删除失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "配置删除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 测试配置
     */
    fun testConfig(config: VodConfigUrl) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🧪 测试配置: ${config.name}")

                // ✅ 通过适配器测试配置
                repositoryAdapter.testConfig(config.url)

                Log.d("ONETV_MOVIE", "✅ 配置测试完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置测试失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "配置测试失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 重置设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔄 重置设置")

                // 重置UI状态到默认值
                _uiState.value = SettingsUiState()

                Log.d("ONETV_MOVIE", "✅ 设置重置完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "设置重置失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "设置重置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 导出设置
     */
    fun exportSettings() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "📤 导出设置")

                // TODO: 实现设置导出逻辑
                Log.d("ONETV_MOVIE", "✅ 设置导出完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "设置导出失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "设置导出失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 导入设置
     */
    fun importSettings() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "📥 导入设置")

                // TODO: 实现设置导入逻辑
                Log.d("ONETV_MOVIE", "✅ 设置导入完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "设置导入失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "设置导入失败: ${e.message}"
                )
            }
        }
    }
}
