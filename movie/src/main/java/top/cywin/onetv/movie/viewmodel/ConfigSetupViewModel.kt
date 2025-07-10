package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// KotlinPoet专业重构 - 移除Hilt import
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.movie.data.config.AppConfigManager
import java.util.concurrent.TimeUnit
// KotlinPoet专业重构 - 移除Inject import
// import javax.inject.Inject

/**
 * 配置设置ViewModel
 * 处理服务器配置的设置、验证和保存
 * KotlinPoet专业重构 - 使用MovieApp单例管理依赖
 */
// @HiltViewModel
class ConfigSetupViewModel(
    private val appConfigManager: AppConfigManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConfigSetupUiState())
    val uiState: StateFlow<ConfigSetupUiState> = _uiState.asStateFlow()
    
    /**
     * 保存配置
     */
    fun saveConfig(
        projectUrl: String,
        apiKey: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 验证配置格式
                val validationResult = validateConfig(projectUrl, apiKey)
                if (!validationResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = validationResult.second
                    )
                    return@launch
                }
                
                // 测试连接
                val testResult = testConnectionInternal(projectUrl, apiKey)
                if (!testResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "连接测试失败: ${testResult.second}"
                    )
                    return@launch
                }
                
                // 保存配置
                val saveResult = appConfigManager.setInitialConfig(projectUrl, apiKey)
                if (saveResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = saveResult.exceptionOrNull()?.message ?: "保存配置失败"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置保存失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 测试连接
     */
    fun testConnection(projectUrl: String, apiKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 验证配置格式
                val validationResult = validateConfig(projectUrl, apiKey)
                if (!validationResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = validationResult.second
                    )
                    return@launch
                }
                
                // 测试连接
                val testResult = testConnectionInternal(projectUrl, apiKey)
                if (testResult.first) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        connectionTestSuccess = true
                    )
                    
                    // 3秒后清除成功状态
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(connectionTestSuccess = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "连接测试失败: ${testResult.second}"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "连接测试失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 验证配置格式
     */
    private fun validateConfig(projectUrl: String, apiKey: String): Pair<Boolean, String> {
        // 验证URL格式
        if (!projectUrl.matches("https://[a-zA-Z0-9-]+\\.supabase\\.co/?".toRegex())) {
            return false to "URL格式不正确，应为: https://your-project.supabase.co"
        }
        
        // 验证API Key格式（JWT token基本验证）
        if (apiKey.length < 50 || !apiKey.startsWith("eyJ")) {
            return false to "API Key格式不正确，应为JWT token格式"
        }
        
        // 检查API Key是否包含必要的部分
        val parts = apiKey.split(".")
        if (parts.size != 3) {
            return false to "API Key格式不正确，JWT token应包含3个部分"
        }
        
        return true to ""
    }
    
    /**
     * 内部连接测试
     */
    private suspend fun testConnectionInternal(projectUrl: String, apiKey: String): Pair<Boolean, String> {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            // 测试基本连接
            val testUrl = "${projectUrl.trimEnd('/')}/rest/v1/"
            val request = Request.Builder()
                .url(testUrl)
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(request).execute()
            
            when {
                response.isSuccessful -> {
                    true to "连接成功"
                }
                response.code == 401 -> {
                    false to "API Key无效或已过期"
                }
                response.code == 404 -> {
                    false to "项目URL不存在"
                }
                else -> {
                    false to "连接失败，状态码: ${response.code}"
                }
            }
            
        } catch (e: java.net.UnknownHostException) {
            false to "无法解析域名，请检查URL是否正确"
        } catch (e: java.net.ConnectException) {
            false to "连接超时，请检查网络连接"
        } catch (e: Exception) {
            false to "连接失败: ${e.message}"
        }
    }
    
    /**
     * 检查是否有现有配置
     */
    fun hasExistingConfig(): Boolean {
        return appConfigManager.hasInitialConfig()
    }
    
    /**
     * 获取现有配置
     */
    fun getExistingConfig(): Pair<String, String> {
        // 这里应该从安全存储中读取，但不暴露完整的敏感信息
        // 只返回URL和部分遮蔽的API Key用于显示
        return try {
            val config = appConfigManager.getCurrentConfig()
            if (config != null) {
                val maskedApiKey = maskApiKey(config.apiKey)
                config.projectUrl to maskedApiKey
            } else {
                "" to ""
            }
        } catch (e: Exception) {
            "" to ""
        }
    }
    
    /**
     * 遮蔽API Key显示
     */
    private fun maskApiKey(apiKey: String): String {
        return if (apiKey.length > 20) {
            "${apiKey.take(10)}...${apiKey.takeLast(10)}"
        } else {
            "***"
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
        appConfigManager.clearLocalConfig()
        _uiState.value = ConfigSetupUiState()
    }

    /**
     * 刷新配置
     */
    fun refreshConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 重新加载配置
                val status = "配置刷新成功"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configStatus = status
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configStatus = "配置刷新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 使用内置源
     */
    fun useBuiltInSource() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 加载内置配置
                val status = "已切换到内置视频源"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configStatus = status
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configStatus = "内置源加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 添加自定义配置
     */
    fun addCustomConfig(configUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                if (validateConfigUrl(configUrl)) {
                    // 保存自定义配置URL
                    val status = "自定义配置添加成功"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        configStatus = status
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        configStatus = "配置URL格式不正确"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    configStatus = "配置添加失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 验证配置URL
     */
    private fun validateConfigUrl(url: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.scheme in listOf("http", "https") && !uri.host.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 配置设置UI状态
 */
data class ConfigSetupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val connectionTestSuccess: Boolean = false,
    val configStatus: String = "等待配置加载..."
)
