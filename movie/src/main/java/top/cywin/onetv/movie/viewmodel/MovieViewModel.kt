package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.bean.Site
import top.cywin.onetv.movie.bean.Class
import top.cywin.onetv.movie.bean.Vod
import android.util.Log

/**
 * UI状态数据类
 */
data class MovieUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStoreHouseIndex: Boolean = false,
    val storeHouseName: String = "",
    val availableRoutes: List<VodConfigUrl> = emptyList(),
    val selectedRoute: VodConfigUrl? = null,
    val showRouteSelector: Boolean = false,
    val currentSite: Site? = null,
    val categories: List<Class> = emptyList(),
    val recommendMovies: List<Vod> = emptyList(),
    val homeCategories: List<HomeCategorySection> = emptyList()
)

/**
 * 配置URL数据类
 */
data class VodConfigUrl(
    val name: String,
    val url: String
)

/**
 * 首页分类区域数据类
 */
data class HomeCategorySection(
    val categoryId: String,
    val categoryName: String,
    val movies: List<Vod>,
    val hasMore: Boolean = true
)

/**
 * OneTV Movie首页ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MovieViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel
    private val vodConfig = movieApp.vodConfig

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    /**
     * 加载首页数据 - 通过适配器调用FongMi_TV解析系统
     */
    fun loadHomeData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                Log.d("ONETV_MOVIE", "开始加载配置文件")

                // ✅ 直接使用FongMi_TV的VodConfig
                repositoryAdapter.loadConfig()

                // 检查VodConfig是否加载成功
                val config = vodConfig
                if (config.sites.isEmpty()) {
                    Log.w("ONETV_MOVIE", "配置加载失败，显示空状态")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置加载失败，请检查网络连接"
                    )
                    return@launch
                }

                Log.d("ONETV_MOVIE", "配置文件加载成功")

                // ✅ 通过适配器确保Repository连接
                repositoryAdapter.reconnectRepositories()

                // ✅ 检查是否为仓库索引文件
                val isStoreHouse = checkIfStoreHouseIndex(config)
                if (isStoreHouse) {
                    handleStoreHouseIndex(config)
                    return@launch
                }

                // ✅ 加载正常配置
                loadNormalConfig(config)

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 检查是否为仓库索引文件
     */
    private fun checkIfStoreHouseIndex(config: top.cywin.onetv.movie.api.config.VodConfig): Boolean {
        return config.sites.any { site ->
            site.name?.contains("仓库") == true ||
            site.api?.contains("index") == true
        }
    }

    /**
     * 处理仓库索引文件
     */
    private suspend fun handleStoreHouseIndex(config: top.cywin.onetv.movie.api.config.VodConfig) {
        Log.d("ONETV_MOVIE", "🏪 检测到仓库索引文件")

        // 获取配置URL列表
        val configUrls = config.sites.map { site ->
            VodConfigUrl(
                name = site.name ?: "未知线路",
                url = site.api ?: ""
            )
        }

        Log.d("ONETV_MOVIE", "📋 可用线路数: ${configUrls.size}")

        // 设置仓库索引状态
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStoreHouseIndex = true,
            storeHouseName = "默认仓库",
            availableRoutes = configUrls,
            showRouteSelector = false,
            error = null
        )

        // 自动选择第一条线路
        if (configUrls.isNotEmpty()) {
            selectRoute(configUrls[0])
        }
    }

    /**
     * 加载正常配置
     */
    private suspend fun loadNormalConfig(config: top.cywin.onetv.movie.api.config.VodConfig) {
        Log.d("ONETV_MOVIE", "📋 加载正常配置，站点数: ${config.sites.size}")

        // ✅ 使用FongMi_TV的SiteViewModel获取内容
        repositoryAdapter.getCategories()
        repositoryAdapter.getRecommendContent()

        // 获取当前站点
        val currentSite = config.home

        // 更新UI状态
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentSite = currentSite,
            categories = emptyList(), // 分类数据通过SiteViewModel观察获取
            error = null
        )
    }

    /**
     * 刷新配置和内容
     */
    fun refresh() {
        Log.d("ONETV_MOVIE", "🔄 用户触发刷新，强制更新配置")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // ✅ 通过适配器强制刷新配置 - 解析逻辑在FongMi_TV中
                repositoryAdapter.refreshConfig()
                Log.d("ONETV_MOVIE", "✅ 配置刷新请求已发送，重新加载首页数据")
                loadHomeData()
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "💥 刷新过程异常", e)
                loadHomeData() // 降级处理
            }
        }
    }

    /**
     * 选择仓库线路
     */
    fun selectRoute(routeUrl: VodConfigUrl) {
        Log.d("ONETV_MOVIE", "🔗 用户选择线路: ${routeUrl.name}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // ✅ 通过适配器解析选中的线路配置 - 解析逻辑在FongMi_TV中
                repositoryAdapter.parseRouteConfig(routeUrl.url)

                // 等待配置加载完成
                delay(1000)

                // 获取当前配置
                val config = vodConfig
                if (config.sites.isEmpty()) {
                    Log.e("ONETV_MOVIE", "线路解析失败")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "线路解析失败",
                        showRouteSelector = false
                    )
                    return@launch
                }

                Log.d("ONETV_MOVIE", "✅ 线路解析成功: 站点=${config.sites.size}个")

                // ✅ 重新加载首页数据
                loadNormalConfig(config)

                _uiState.value = _uiState.value.copy(
                    showRouteSelector = false,
                    selectedRoute = routeUrl
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "线路切换失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "线路切换失败: ${e.message}",
                    showRouteSelector = false
                )
            }
        }
    }

    /**
     * 显示线路选择器
     */
    fun showRouteSelector() {
        _uiState.value = _uiState.value.copy(showRouteSelector = true)
    }

    /**
     * 隐藏线路选择器
     */
    fun hideRouteSelector() {
        _uiState.value = _uiState.value.copy(showRouteSelector = false)
    }

    /**
     * 加载推荐内容
     */
    fun loadRecommendContent() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🌟 加载推荐内容")

                // ✅ 通过适配器获取推荐内容
                repositoryAdapter.getRecommendContent()

                Log.d("ONETV_MOVIE", "✅ 推荐内容请求已发送")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "推荐内容加载失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "推荐内容加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载分类列表
     */
    fun loadCategories() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "📂 加载分类列表")

                // ✅ 通过适配器获取分类列表
                repositoryAdapter.getCategories()

                Log.d("ONETV_MOVIE", "✅ 分类列表请求已发送")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "分类列表加载失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "分类列表加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔄 刷新数据")

                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // ✅ 通过适配器刷新配置
                repositoryAdapter.refreshConfig()

                // 重新加载首页数据
                loadHomeData()

                Log.d("ONETV_MOVIE", "✅ 数据刷新完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "数据刷新失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "数据刷新失败: ${e.message}"
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
                Log.d("ONETV_MOVIE", "🗑️ 清空缓存")

                // ✅ 通过适配器清空缓存
                repositoryAdapter.clearAllCache { progress ->
                    Log.d("ONETV_MOVIE", "缓存清理进度: ${(progress * 100).toInt()}%")
                }

                Log.d("ONETV_MOVIE", "✅ 缓存清空完成")

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "缓存清空失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "缓存清空失败: ${e.message}"
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
}
