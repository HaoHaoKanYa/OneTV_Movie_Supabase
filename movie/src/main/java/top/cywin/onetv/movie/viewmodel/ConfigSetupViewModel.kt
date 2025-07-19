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
import top.cywin.onetv.movie.event.ConfigUpdateEvent
import top.cywin.onetv.movie.event.ConfigLoadEvent
import top.cywin.onetv.movie.event.ApiTestEvent
import top.cywin.onetv.movie.event.ErrorEvent
import top.cywin.onetv.movie.ui.model.SiteInfo

/**
 * OneTV Movie配置设置ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV配置功能，完整的事件驱动架构
 */
class ConfigSetupViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_CONFIG_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(ConfigSetupUiState())
    val uiState: StateFlow<ConfigSetupUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ ConfigSetupViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)

        // ✅ 初始化加载配置状态
        loadConfigStatus()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 ConfigSetupViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }
    // ===== EventBus事件监听 =====

    /**
     * 监听配置更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConfigUpdate(event: ConfigUpdateEvent) {
        Log.d(TAG, "📡 收到配置更新事件: success=${event.isSuccess}")

        if (event.isSuccess && event.config != null) {
            val siteInfos = event.config.sites.map { site ->
                ViewModelAdapter.convertSiteToSiteInfo(site)
            }.filterNotNull()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isConfigured = true,
                currentConfig = event.config,
                availableSites = siteInfos,
                error = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = event.errorMessage ?: "配置更新失败"
            )
        }
    }

    /**
     * 监听配置加载事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConfigLoad(event: ConfigLoadEvent) {
        Log.d(TAG, "📡 收到配置加载事件: loading=${event.isLoading}")

        _uiState.value = _uiState.value.copy(
            isLoading = event.isLoading,
            loadingProgress = event.progress,
            loadingMessage = event.message
        )
    }

    /**
     * 监听API测试事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onApiTest(event: ApiTestEvent) {
        Log.d(TAG, "📡 收到API测试事件: url=${event.url}, success=${event.isSuccess}")

        val currentTests = _uiState.value.testResults.toMutableMap()
        currentTests[event.url] = ApiTestResult(
            url = event.url,
            isSuccess = event.isSuccess,
            responseTime = event.responseTime,
            errorMessage = event.errorMessage
        )

        _uiState.value = _uiState.value.copy(
            testResults = currentTests,
            isTesting = false
        )
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isTesting = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 加载配置状态
     */
    fun loadConfigStatus() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⚙️ 开始加载配置状态")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 检查当前配置状态
                val vodConfig = repositoryAdapter.getVodConfig()
                val isConfigured = vodConfig.sites.isNotEmpty()

                if (isConfigured) {
                    val siteInfos = vodConfig.sites.map { site ->
                        ViewModelAdapter.convertSiteToSiteInfo(site)
                    }.filterNotNull()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConfigured = true,
                        currentConfig = vodConfig,
                        availableSites = siteInfos
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConfigured = false,
                        showConfigInput = true
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 配置状态加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置状态加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 设置配置URL
     */
    fun setConfigUrl(url: String) {
        _uiState.value = _uiState.value.copy(configUrl = url)
    }

    /**
     * 解析配置
     */
    fun parseConfig() {
        val configUrl = _uiState.value.configUrl.trim()
        if (configUrl.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请输入配置URL")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 开始解析配置: $configUrl")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    loadingMessage = "正在解析配置..."
                )

                // ✅ 通过适配器解析配置
                repositoryAdapter.parseConfig(configUrl)

            } catch (e: Exception) {
                Log.e(TAG, "💥 配置解析失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置解析失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 测试配置URL
     */
    fun testConfigUrl(url: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 开始测试配置URL: $url")
                _uiState.value = _uiState.value.copy(
                    isTesting = true,
                    error = null
                )

                // ✅ 通过适配器测试连接
                repositoryAdapter.testConnection(url, "")

            } catch (e: Exception) {
                Log.e(TAG, "💥 配置测试失败", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = "配置测试失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 重置配置
     */
    fun resetConfig() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 重置配置")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 通过适配器清除配置
                repositoryAdapter.clearConfigCache()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isConfigured = false,
                    currentConfig = null,
                    availableSites = emptyList(),
                    configUrl = "",
                    showConfigInput = true
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 配置重置失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置重置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 显示配置输入
     */
    fun showConfigInput() {
        _uiState.value = _uiState.value.copy(showConfigInput = true)
    }

    /**
     * 隐藏配置输入
     */
    fun hideConfigInput() {
        _uiState.value = _uiState.value.copy(showConfigInput = false)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 配置设置UI状态数据类
 */
data class ConfigSetupUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,
    val loadingMessage: String = "",
    val loadingProgress: Float = 0f,

    // 配置状态
    val isConfigured: Boolean = false,
    val currentConfig: top.cywin.onetv.movie.api.config.VodConfig? = null,
    val configUrl: String = "",

    // 站点信息
    val availableSites: List<SiteInfo> = emptyList(),

    // 测试状态
    val isTesting: Boolean = false,
    val testResults: Map<String, ApiTestResult> = emptyMap(),

    // UI控制
    val showConfigInput: Boolean = false,
    val showAdvancedOptions: Boolean = false
)

/**
 * API测试结果数据类
 */
data class ApiTestResult(
    val url: String,
    val isSuccess: Boolean,
    val responseTime: Long = 0,
    val errorMessage: String? = null
)
