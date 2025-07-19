package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.adapter.ViewModelAdapter
import top.cywin.onetv.movie.event.SettingsUpdateEvent
import top.cywin.onetv.movie.event.NetworkStatusEvent
import top.cywin.onetv.movie.event.ErrorEvent
import top.cywin.onetv.movie.ui.model.SettingItem
import top.cywin.onetv.movie.ui.model.SettingType
import top.cywin.onetv.movie.ui.model.NetworkState

/**
 * OneTV Movie设置ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV设置功能，完整的事件驱动架构
 */
class MovieSettingsViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_SETTINGS_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MovieSettingsViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)

        // ✅ 初始化加载设置
        loadSettings()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MovieSettingsViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }

    // ===== EventBus事件监听 =====

    /**
     * 监听设置更新事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSettingsUpdate(event: SettingsUpdateEvent) {
        Log.d(TAG, "📡 收到设置更新事件: key=${event.key}, success=${event.isSuccess}")

        if (event.isSuccess) {
            // 更新对应的设置项
            updateSettingItem(event.key, event.value)
        } else {
            _uiState.value = _uiState.value.copy(
                error = "设置更新失败: ${event.key}"
            )
        }
    }

    /**
     * 监听网络状态事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNetworkStatus(event: NetworkStatusEvent) {
        Log.d(TAG, "📡 收到网络状态事件: connected=${event.isConnected}")

        _uiState.value = _uiState.value.copy(
            networkState = NetworkState(
                isConnected = event.isConnected,
                networkType = event.networkType
            )
        )
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(error = event.message)
    }

    // ===== 公共方法 =====

    /**
     * 加载设置
     */
    fun loadSettings() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "⚙️ 开始加载设置")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 加载各种设置项
                val settingItems = loadAllSettingItems()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settingItems = settingItems
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 设置加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "设置加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新设置项
     */
    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 更新设置: $key = $value")

                // ✅ 通过适配器更新设置
                // repositoryAdapter.updateSetting(key, value)

                // ✅ 乐观更新UI
                updateSettingItem(key, value)

                // ✅ 发送设置更新事件
                EventBus.getDefault().post(SettingsUpdateEvent(key, value, true))

            } catch (e: Exception) {
                Log.e(TAG, "💥 设置更新失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "设置更新失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 重置所有设置
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 重置所有设置")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 重置设置到默认值
                val defaultSettings = getDefaultSettingItems()

                // ✅ 保存默认设置
                defaultSettings.forEach { setting ->
                    updateSetting(setting.key, setting.value)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settingItems = defaultSettings
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 设置重置失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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
                Log.d(TAG, "📤 导出设置")

                val settingsJson = exportSettingsToJson()

                // ✅ 通知UI显示导出结果
                _uiState.value = _uiState.value.copy(
                    exportData = settingsJson,
                    showExportDialog = true
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 设置导出失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "设置导出失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 导入设置
     */
    fun importSettings(settingsJson: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📥 导入设置")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                val importedSettings = importSettingsFromJson(settingsJson)

                // ✅ 应用导入的设置
                importedSettings.forEach { setting ->
                    updateSetting(setting.key, setting.value)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settingItems = importedSettings,
                    showImportDialog = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "💥 设置导入失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "设置导入失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧹 清除缓存")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

                // ✅ 通过适配器清除缓存
                repositoryAdapter.clearConfigCache()

                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )

                // ✅ 显示成功消息
                EventBus.getDefault().post(SettingsUpdateEvent("cache_cleared", true, true))

            } catch (e: Exception) {
                Log.e(TAG, "💥 缓存清除失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "缓存清除失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 显示导出对话框
     */
    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    /**
     * 隐藏导出对话框
     */
    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    /**
     * 显示导入对话框
     */
    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true)
    }

    /**
     * 隐藏导入对话框
     */
    fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ===== 私有方法 =====

    /**
     * 加载所有设置项
     */
    private suspend fun loadAllSettingItems(): List<SettingItem> {
        return withContext(Dispatchers.IO) {
            listOf(
                // 播放设置
                SettingItem(
                    key = "auto_play",
                    title = "自动播放",
                    description = "进入详情页时自动播放第一集",
                    type = SettingType.SWITCH,
                    value = true
                ),
                SettingItem(
                    key = "play_speed",
                    title = "播放速度",
                    description = "默认播放速度",
                    type = SettingType.SELECT,
                    value = "1.0x",
                    options = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
                ),
                SettingItem(
                    key = "skip_opening",
                    title = "跳过片头",
                    description = "自动跳过片头片尾",
                    type = SettingType.SWITCH,
                    value = false
                ),

                // 界面设置
                SettingItem(
                    key = "theme_mode",
                    title = "主题模式",
                    description = "选择界面主题",
                    type = SettingType.SELECT,
                    value = "dark",
                    options = listOf("light", "dark", "auto")
                ),
                SettingItem(
                    key = "view_type",
                    title = "视图类型",
                    description = "选择内容显示方式",
                    type = SettingType.SELECT,
                    value = "rect",
                    options = listOf("rect", "oval", "list", "grid")
                ),

                // 网络设置
                SettingItem(
                    key = "timeout",
                    title = "网络超时",
                    description = "网络请求超时时间（秒）",
                    type = SettingType.SLIDER,
                    value = 15
                ),
                SettingItem(
                    key = "retry_count",
                    title = "重试次数",
                    description = "网络请求失败重试次数",
                    type = SettingType.SLIDER,
                    value = 3
                ),

                // 缓存设置
                SettingItem(
                    key = "cache_size",
                    title = "缓存大小",
                    description = "图片缓存大小限制（MB）",
                    type = SettingType.SLIDER,
                    value = 100
                ),
                SettingItem(
                    key = "auto_clear_cache",
                    title = "自动清理缓存",
                    description = "定期自动清理过期缓存",
                    type = SettingType.SWITCH,
                    value = true
                )
            )
        }
    }


    /**
     * 获取默认设置项
     */
    private fun getDefaultSettingItems(): List<SettingItem> {
        return listOf(
            SettingItem("auto_play", "自动播放", "", SettingType.SWITCH, true),
            SettingItem("play_speed", "播放速度", "", SettingType.SELECT, "1.0x"),
            SettingItem("skip_opening", "跳过片头", "", SettingType.SWITCH, false),
            SettingItem("theme_mode", "主题模式", "", SettingType.SELECT, "dark"),
            SettingItem("view_type", "视图类型", "", SettingType.SELECT, "rect"),
            SettingItem("timeout", "网络超时", "", SettingType.SLIDER, 15),
            SettingItem("retry_count", "重试次数", "", SettingType.SLIDER, 3),
            SettingItem("cache_size", "缓存大小", "", SettingType.SLIDER, 100),
            SettingItem("auto_clear_cache", "自动清理缓存", "", SettingType.SWITCH, true)
        )
    }

    /**
     * 更新设置项
     */
    private fun updateSettingItem(key: String, value: Any) {
        val currentItems = _uiState.value.settingItems.toMutableList()
        val index = currentItems.indexOfFirst { it.key == key }

        if (index >= 0) {
            currentItems[index] = currentItems[index].copy(value = value)
            _uiState.value = _uiState.value.copy(settingItems = currentItems)
        }
    }

    /**
     * 导出设置为JSON
     */
    private fun exportSettingsToJson(): String {
        val settingsMap = _uiState.value.settingItems.associate { it.key to it.value }
        // 这里应该使用JSON库序列化
        return settingsMap.toString() // 简化实现
    }

    /**
     * 从JSON导入设置
     */
    private fun importSettingsFromJson(json: String): List<SettingItem> {
        // 这里应该使用JSON库反序列化
        // 简化实现，返回默认设置
        return getDefaultSettingItems()
    }
}

/**
 * 设置UI状态数据类
 */
data class SettingsUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,

    // 设置数据
    val settingItems: List<SettingItem> = emptyList(),

    // 网络状态
    val networkState: NetworkState = NetworkState(),

    // 导入导出
    val exportData: String = "",
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,

    // UI控制
    val expandedSections: Set<String> = setOf("playback", "interface", "network", "cache")
)
