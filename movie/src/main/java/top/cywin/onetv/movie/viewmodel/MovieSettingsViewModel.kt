package top.cywin.onetv.movie.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.cywin.onetv.movie.data.models.SettingsUiState
import top.cywin.onetv.movie.data.models.VodConfig
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.data.cache.VodCacheManager

/**
 * VOD设置ViewModel
 * 处理点播系统的设置功能，包括配置管理和缓存清理
 */
class MovieSettingsViewModel(
    private val configManager: VodConfigManager,
    private val cacheManager: VodCacheManager
) : ViewModel() {

    // 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Log.d("ONETV_MOVIE", "MovieSettingsViewModel 初始化")
        loadSettings()
    }

    /**
     * 加载设置数据
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 获取缓存大小
                val cacheSize = cacheManager.getCacheSize()
                
                // 获取当前配置
                val currentConfig = configManager.getCurrentConfig()?.let { config ->
                    VodConfig(
                        url = "current",
                        name = "当前配置",
                        sites = config.sites,
                        parses = config.parses
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cacheSize = cacheSize,
                    currentConfig = currentConfig,
                    configs = if (currentConfig != null) listOf(currentConfig) else emptyList(),
                    error = null
                )
                
                Log.d("ONETV_MOVIE", "设置数据加载完成，缓存大小: ${cacheSize / 1024 / 1024}MB")
                
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
                Log.d("ONETV_MOVIE", "🗑️ 开始清空VOD缓存")
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                withContext(Dispatchers.IO) {
                    // 清空VOD缓存管理器的所有缓存
                    cacheManager.clearAll()
                    Log.d("ONETV_MOVIE", "✅ VOD缓存管理器缓存已清空")
                    
                    // 清空仓库缓存 - 使用FongMi_TV的RepositoryAdapter
                    repositoryAdapter.clearCache()
                    Log.d("ONETV_MOVIE", "✅ VOD仓库缓存已清空")
                    
                    // 清空配置管理器缓存
                    configManager.clear()
                    Log.d("ONETV_MOVIE", "✅ VOD配置管理器缓存已清空")
                }
                
                // 重新加载设置以更新缓存大小
                loadSettings()
                
                Log.d("ONETV_MOVIE", "🎉 VOD缓存清空完成")
                
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "缓存清空失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "缓存清空失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 添加配置
     */
    fun addConfig(url: String) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "添加配置: $url")
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 解析配置URL - 使用FongMi_TV的RepositoryAdapter
                repositoryAdapter.parseRouteConfig(url)

                // 等待配置加载完成
                delay(1000)

                // 检查配置是否加载成功 - 通过适配器系统获取配置
                val config = repositoryAdapter.getVodConfig()
                if (config == null || config.sites.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置解析失败"
                    )
                    return@launch
                }

                // 加载配置
                val loadResult = configManager.load(config)
                if (loadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置加载失败: ${loadResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                Log.d("ONETV_MOVIE", "配置添加成功")
                loadSettings()
                
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "添加配置失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "添加配置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择配置
     */
    fun selectConfig(config: VodConfig) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "选择配置: ${config.name}")
                
                // 这里可以实现配置切换逻辑
                // 目前只是重新加载设置
                loadSettings()
                
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "选择配置失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "选择配置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除配置
     */
    fun deleteConfig(config: VodConfig) {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "删除配置: ${config.name}")
                
                // 清空当前配置
                configManager.clear()
                
                // 重新加载设置
                loadSettings()
                
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "删除配置失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "删除配置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 刷新设置
     */
    fun refresh() {
        loadSettings()
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
    fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / 1024 / 1024}MB"
            else -> "${bytes / 1024 / 1024 / 1024}GB"
        }
    }

    /**
     * 强制重新解析配置（用于TVBOX仓库索引检测）
     */
    fun forceReparseConfig() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔄 用户请求强制重新解析配置")
                _uiState.value = _uiState.value.copy(isLoading = true)

                // 清除缓存并重新解析 - 使用FongMi_TV的RepositoryAdapter
                repositoryAdapter.clearConfigCache()
                Log.d("ONETV_MOVIE", "✅ 缓存清除请求已发送")
                Log.d("ONETV_MOVIE", "✅ 缓存清除成功，配置将在下次访问时重新解析")

                // 重新加载设置
                loadSettings()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "强制重新解析配置失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "操作失败: ${e.message}"
                )
            }
        }
    }
}
