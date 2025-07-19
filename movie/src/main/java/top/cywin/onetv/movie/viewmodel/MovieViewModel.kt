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

// ✅ 添加EventBus支持
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.event.*
import top.cywin.onetv.movie.ui.model.*
import top.cywin.onetv.movie.adapter.ViewModelAdapter

/**
 * 首页UI状态数据类 - 完整版本
 */
data class MovieUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val error: String? = null,
    val loadingMessage: String = "",
    val loadingProgress: Float = 0f,

    // 配置相关
    val isStoreHouseIndex: Boolean = false,
    val storeHouseName: String = "",
    val availableRoutes: List<VodConfigUrl> = emptyList(),
    val selectedRoute: VodConfigUrl? = null,
    val showRouteSelector: Boolean = false,
    val showConfigSetup: Boolean = false,

    // 站点相关
    val currentSite: top.cywin.onetv.movie.bean.Site? = null,
    val siteList: List<SiteInfo> = emptyList(),

    // 内容相关
    val categories: List<CategoryInfo> = emptyList(),
    val recommendMovies: List<MovieItem> = emptyList(),
    val homeCategories: List<HomeCategorySection> = emptyList(),
    val hotMovies: List<MovieItem> = emptyList(),
    val newMovies: List<MovieItem> = emptyList(),

    // UI控制
    val showSearch: Boolean = false,
    val showSettings: Boolean = false,
    val refreshing: Boolean = false,

    // 网络状态
    val networkState: NetworkState = NetworkState(),

    // 其他状态
    val lastUpdateTime: Long = 0
)

/**
 * OneTV Movie首页ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV解析功能，完整的事件驱动架构
 */
class MovieViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter
    private val vodConfig = movieApp.vodConfig

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MovieViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)

        // ✅ 初始化加载首页数据
        loadHomeData()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MovieViewModel 清理")

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
            Log.d(TAG, "✅ 配置更新成功，重新加载首页")
            handleConfigUpdateSuccess(event.config)
        } else {
            Log.e(TAG, "❌ 配置更新失败: ${event.errorMessage}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = event.errorMessage ?: "配置更新失败"
            )
        }
    }

    /**
     * 监听首页内容事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHomeContent(event: HomeContentEvent) {
        Log.d(TAG, "📡 收到首页内容事件: success=${event.isSuccess}")

        if (event.isSuccess) {
            handleHomeContentSuccess(event.categories, event.recommendVods)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "首页内容加载失败"
            )
        }
    }

    /**
     * 监听分类内容事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCategoryContent(event: CategoryContentEvent) {
        Log.d(TAG, "📡 收到分类内容: typeId=${event.typeId}, count=${event.vods.size}")

        handleCategoryContentUpdate(event)
    }

    /**
     * 监听搜索结果事件（用于推荐内容）
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSearchResult(event: SearchResultEvent) {
        Log.d(TAG, "📡 收到搜索结果: keyword=${event.keyword}, count=${event.results.size}")

        // 如果是首页推荐搜索（空关键词或特定关键词）
        if (event.keyword.isEmpty() || event.keyword == "推荐") {
            val movieItems = event.results.map { vod ->
                ViewModelAdapter.convertVodToMovie(vod)
            }.filterNotNull()

            _uiState.value = _uiState.value.copy(
                recommendMovies = movieItems
            )
        }
    }

    /**
     * 监听站点变更事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChange(event: SiteChangeEvent) {
        Log.d(TAG, "📡 收到站点变更事件: success=${event.isSuccess}")

        if (event.isSuccess && event.site != null) {
            _uiState.value = _uiState.value.copy(
                currentSite = event.site
            )

            // 重新加载首页内容
            loadHomeData()
        }
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 加载首页数据 - 通过适配器调用FongMi_TV解析系统
     */
    fun loadHomeData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 开始加载首页数据")
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    loadingMessage = "正在加载配置..."
                )

                // ✅ 通过适配器加载配置
                repositoryAdapter.loadConfig()

                // 检查VodConfig是否加载成功
                val config = vodConfig
                if (config.sites.isEmpty()) {
                    Log.w(TAG, "配置加载失败，显示空状态")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置加载失败，请检查网络连接"
                    )
                    return@launch
                }

                Log.d(TAG, "✅ 配置文件加载成功，站点数: ${config.sites.size}")

                // ✅ 检查是否为仓库索引文件
                val isStoreHouse = checkIfStoreHouseIndex(config)
                if (isStoreHouse) {
                    handleStoreHouseIndex(config)
                    return@launch
                }

                // ✅ 加载正常配置
                loadNormalConfig(config)

            } catch (e: Exception) {
                Log.e(TAG, "💥 首页数据加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "首页数据加载失败: ${e.message}"
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

    // ===== 私有方法 =====

    /**
     * 处理配置更新成功
     */
    private fun handleConfigUpdateSuccess(config: top.cywin.onetv.movie.api.config.VodConfig) {
        Log.d(TAG, "✅ 处理配置更新成功")

        // 更新当前站点
        val currentSite = config.home
        _uiState.value = _uiState.value.copy(
            currentSite = currentSite,
            isLoading = false,
            error = null
        )

        // 加载首页内容
        repositoryAdapter.getHomeContent()
    }

    /**
     * 处理首页内容成功
     */
    private fun handleHomeContentSuccess(
        categories: List<top.cywin.onetv.movie.bean.Class>,
        recommendVods: List<top.cywin.onetv.movie.bean.Vod>
    ) {
        Log.d(TAG, "✅ 处理首页内容成功: 分类${categories.size}个, 推荐${recommendVods.size}个")

        // 转换分类数据
        val categoryInfos = categories.map { clazz ->
            ViewModelAdapter.convertClassToCategory(clazz)
        }.filterNotNull()

        // 转换推荐电影数据
        val recommendMovies = recommendVods.map { vod ->
            ViewModelAdapter.convertVodToMovie(vod)
        }.filterNotNull()

        _uiState.value = _uiState.value.copy(
            categories = categoryInfos,
            recommendMovies = recommendMovies,
            isLoading = false,
            error = null
        )

        // 加载各分类的内容
        loadHomeCategoryContent(categories.take(6)) // 取前6个分类
    }

    /**
     * 处理分类内容更新
     */
    private fun handleCategoryContentUpdate(event: CategoryContentEvent) {
        val currentCategories = _uiState.value.homeCategories.toMutableList()
        val existingIndex = currentCategories.indexOfFirst { it.categoryId == event.typeId }

        // 转换电影数据
        val movieItems = event.vods.map { vod ->
            ViewModelAdapter.convertVodToMovie(vod)
        }.filterNotNull()

        if (existingIndex >= 0) {
            // 更新现有分类
            val existingCategory = currentCategories[existingIndex]
            currentCategories[existingIndex] = existingCategory.copy(
                movies = if (event.page == 1) movieItems else existingCategory.movies + movieItems,
                hasMore = event.hasMore,
                isLoading = false
            )
        } else {
            // 添加新分类
            val categoryName = vodConfig.getClass(event.typeId)?.typeName ?: "未知分类"
            currentCategories.add(
                HomeCategorySection(
                    categoryId = event.typeId,
                    categoryName = categoryName,
                    movies = movieItems,
                    hasMore = event.hasMore,
                    isLoading = false
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            homeCategories = currentCategories
        )
    }

    /**
     * 检查是否为仓库索引文件
     */
    private fun checkIfStoreHouseIndex(config: top.cywin.onetv.movie.api.config.VodConfig): Boolean {
        // 检查配置是否包含多个仓库链接
        // 这里需要根据实际的FongMi_TV仓库索引格式进行判断
        return false // 暂时返回false，需要根据实际情况实现
    }

    /**
     * 处理仓库索引
     */
    private fun handleStoreHouseIndex(config: top.cywin.onetv.movie.api.config.VodConfig) {
        Log.d(TAG, "📦 处理仓库索引")

        // 解析仓库路由
        val routes = parseStoreHouseRoutes(config)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStoreHouseIndex = true,
            storeHouseName = config.name ?: "仓库索引",
            availableRoutes = routes,
            selectedRoute = null,
            showRouteSelector = routes.isNotEmpty()
        )
    }

    /**
     * 解析仓库路由
     */
    private fun parseStoreHouseRoutes(config: top.cywin.onetv.movie.api.config.VodConfig): List<VodConfigUrl> {
        val routes = mutableListOf<VodConfigUrl>()

        // 这里需要根据实际的FongMi_TV仓库索引格式进行解析
        // 暂时返回空列表

        return routes
    }

    /**
     * 加载正常配置
     */
    private suspend fun loadNormalConfig(config: top.cywin.onetv.movie.api.config.VodConfig) {
        Log.d(TAG, "📋 加载正常配置，站点数: ${config.sites.size}")

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            loadingMessage = "正在加载内容...",
            isStoreHouseIndex = false,
            currentSite = config.home
        )

        // ✅ 获取首页内容
        repositoryAdapter.getHomeContent()

        // ✅ 获取推荐内容
        repositoryAdapter.getRecommendContent()

        // ✅ 加载首页各分类内容
        val categories = config.classes.take(6) // 取前6个分类
        if (categories.isNotEmpty()) {
            loadHomeCategoryContent(categories)
        }
    }

    /**
     * 加载首页分类内容
     */
    private fun loadHomeCategoryContent(categories: List<top.cywin.onetv.movie.bean.Class>) {
        viewModelScope.launch {
            try {
                categories.forEach { category ->
                    Log.d(TAG, "🔄 加载分类内容: ${category.typeName}")

                    // 标记分类为加载中
                    val currentCategories = _uiState.value.homeCategories.toMutableList()
                    val existingIndex = currentCategories.indexOfFirst { it.categoryId == category.typeId }

                    if (existingIndex >= 0) {
                        currentCategories[existingIndex] = currentCategories[existingIndex].copy(isLoading = true)
                    } else {
                        currentCategories.add(
                            HomeCategorySection(
                                categoryId = category.typeId,
                                categoryName = category.typeName,
                                movies = emptyList(),
                                hasMore = true,
                                isLoading = true
                            )
                        )
                    }

                    _uiState.value = _uiState.value.copy(homeCategories = currentCategories)

                    // 获取分类内容
                    repositoryAdapter.getContentList(category.typeId, 1, emptyMap())

                    // 延迟避免请求过快
                    delay(500)
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 首页分类内容加载失败", e)
            }
        }
    }
}
