package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * 配置设置UI状态数据类
 */
data class ConfigSetupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val configUrl: String = "",
    val isValidating: Boolean = false,
    val validationResult: String? = null,
    val isConfigValid: Boolean = false
)

/**
 * OneTV Movie配置设置ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class ConfigSetupViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter

    private val _uiState = MutableStateFlow(ConfigSetupUiState())
    val uiState: StateFlow<ConfigSetupUiState> = _uiState.asStateFlow()
    
    /**
     * 验证配置URL - 通过适配器调用FongMi_TV解析系统
     */
    fun validateConfigUrl(configUrl: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isValidating = true,
                    configUrl = configUrl,
                    error = null
                )

                Log.d("ONETV_MOVIE", "🔍 验证配置URL: $configUrl")

                // ✅ 通过适配器验证配置 - 验证逻辑在FongMi_TV中
                repositoryAdapter.validateConfigUrl(configUrl) { isValid, message ->
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        isConfigValid = isValid,
                        validationResult = message
                    )
                }

                Log.d("ONETV_MOVIE", "✅ 配置验证请求已发送")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置验证失败", e)
                _uiState.value = _uiState.value.copy(
                    isValidating = false,
                    isConfigValid = false,
                    validationResult = "验证失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 保存配置
     */
    fun saveConfig(configUrl: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "💾 保存配置: $configUrl")

                // ✅ 通过适配器保存配置 - 配置管理在FongMi_TV中
                repositoryAdapter.saveConfigUrl(configUrl) { success ->
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            configUrl = configUrl,
                            error = null
                        )
                        onSuccess()
                        Log.d("ONETV_MOVIE", "✅ 配置保存成功")
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "配置保存失败"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "配置保存失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置保存失败: ${e.message}"
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
     * 重置配置
     */
    fun resetConfig() {
        _uiState.value = ConfigSetupUiState()
    }
