package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// KotlinPoet专业重构 - 移除Hilt import
// import dagger.hilt.onetv.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.VodConfigManager
import top.cywin.onetv.movie.MovieApp
// KotlinPoet专业重构 - 移除Inject import
// import javax.inject.Inject
import android.util.Log

/**
 * 点播首页ViewModel (参考OneMoVie架构)
 */
// KotlinPoet专业重构 - 移除Hilt注解，使用标准构造函数
// @HiltViewModel
class MovieViewModel(
    private val configManager: VodConfigManager
) : ViewModel() {

    // 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel
    private val vodConfig = movieApp.vodConfig

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    init {
        Log.d("ONETV_MOVIE", "MovieViewModel 初始化开始")
        Log.d("ONETV_MOVIE", "RepositoryAdapter: $repositoryAdapter")
        Log.d("ONETV_MOVIE", "ConfigManager: $configManager")
        try {
            loadHomeData()
            Log.d("ONETV_MOVIE", "loadHomeData调用成功")
        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "MovieViewModel初始化失败", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "初始化失败: ${e.message}"
            )
        }
    }

    /**
     * 加载首页数据 (动态分类驱动)
     */
    private fun loadHomeData() {
        Log.d("ONETV_MOVIE", "开始加载首页数据")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                Log.d("ONETV_MOVIE", "开始加载配置文件")
                // 1. 加载配置文件 - 使用FongMi_TV的VodConfig
                repositoryAdapter.loadConfig()

                // 检查VodConfig是否加载成功
                val config = vodConfig
                if (config.sites.isEmpty()) {
                    Log.w("ONETV_MOVIE", "配置加载失败，显示空状态")
                    // 配置加载失败时，显示空状态而不是错误
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null // 不显示错误，而是显示空状态
                    )
                    return@launch
                }
                Log.d("ONETV_MOVIE", "配置文件加载成功")

                // 2. 检查是否为仓库索引文件
                val isStoreHouse = config.spider.contains("index") || config.sites.any { it.name?.contains("仓库") == true }
                if (isStoreHouse) {
                    Log.d("ONETV_MOVIE", "🏪 检测到仓库索引文件: 默认仓库")

                    // 获取配置URL列表
                    val configUrls = config.sites.map { site ->
                        mapOf("name" to (site.name ?: ""), "url" to (site.api ?: ""))
                    }

                    Log.d("ONETV_MOVIE", "📋 可用线路数: ${configUrls.size}")

                    // 显示所有可用线路
                    configUrls.forEachIndexed { index, urlConfig ->
                        Log.d("ONETV_MOVIE", "🔗 线路${index + 1}: ${urlConfig["name"]}")
                    }

                    // TVBOX标准：自动选择第一条线路，但保留线路选择器供用户切换
                    if (configUrls.isNotEmpty()) {
                        Log.d("ONETV_MOVIE", "🎯 自动选择第一条线路: ${configUrls[0]["name"]}")

                        // 设置仓库索引状态，但不显示选择器（自动选择第一条）
                        _uiState.value = _uiState.value.copy(
                            isStoreHouseIndex = true,
                            storeHouseName = "默认仓库",
                            availableRoutes = configUrls.map { VodConfigUrl(it["name"] ?: "", it["url"] ?: "") },
                            showRouteSelector = false, // 不立即显示选择器
                            error = null
                        )

                        // 自动选择第一条线路
                        if (configUrls.isNotEmpty()) {
                            selectRoute(VodConfigUrl(configUrls[0]["name"] ?: "", configUrls[0]["url"] ?: ""))
                        }
                    } else {
                        Log.w("ONETV_MOVIE", "⚠️ 仓库索引文件中没有可用线路")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "仓库索引文件中没有可用线路"
                        )
                    }
                    return@launch
                }

                // 2. 获取当前站点和分类
                Log.d("ONETV_MOVIE", "获取当前站点")
                val currentSite = configManager.getCurrentSite()
                if (currentSite == null) {
                    Log.w("ONETV_MOVIE", "未找到可用站点，显示空状态")
                    // 没有站点时，显示空状态而不是错误
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null // 不显示错误，而是显示空状态
                    )
                    return@launch
                }
                Log.d("ONETV_MOVIE", "当前站点: ${currentSite.name}")

                // 3. 获取站点分类 (动态获取，不硬编码) - 使用FongMi_TV的SiteViewModel
                repositoryAdapter.getCategories()

                // 等待SiteViewModel的结果
                val categories = emptyList<VodClass>() // 临时空列表，实际数据通过SiteViewModel观察获取

                // 4. 加载首页内容
                loadHomeContent(currentSite, categories)

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "首页数据加载失败", e)
                // 只有在真正的网络错误或其他严重错误时才显示错误信息
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网络连接失败，请检查网络设置"
                )
            }
        }
    }

    /**
     * 加载首页内容 (基于动态分类，不再硬编码分类名称)
     */
    private suspend fun loadHomeContent(site: VodSite, categories: List<VodClass>) {
        try {
            Log.d("ONETV_MOVIE", "🏠 开始加载首页内容，站点: ${site.name}")

            // 1. 加载推荐内容 - 使用FongMi_TV的SiteViewModel
            Log.d("ONETV_MOVIE", "🔍 获取推荐内容...")
            repositoryAdapter.getRecommendContent()

            // 临时空列表，实际数据通过SiteViewModel观察获取
            val recommendMovies = emptyList<VodItem>()
            Log.d("ONETV_MOVIE", "✅ 推荐内容请求已发送，等待SiteViewModel响应")

            // 2. 设置快速导航分类 (取前5个启用的分类)
            val quickCategories = categories.filter { it.isEnabled() }.take(5)
            Log.d("ONETV_MOVIE", "📂 快速导航分类: ${quickCategories.size}个")

            // 3. 为每个分类动态加载内容
            val homeCategorySections = mutableListOf<HomeCategorySection>()

            categories.filter { it.isEnabled() }.take(6).forEach { category ->
                // 使用FongMi_TV的SiteViewModel获取内容
                repositoryAdapter.getContentList(category.typeId, 1, emptyMap())

                // 临时处理，实际数据通过SiteViewModel观察获取
                val response = VodListResponse(
                    code = 1,
                    msg = "",
                    list = emptyList(),
                    classes = emptyList(),
                    filters = emptyMap(),
                    page = 1,
                    pagecount = 1,
                    limit = 20,
                    total = 0
                )
                if (response.list.isNotEmpty()) {
                        homeCategorySections.add(
                            HomeCategorySection(
                                categoryId = category.typeId,
                                categoryName = category.typeName, // 从配置获取的分类名称
                                movies = response.list.take(10), // 每个分类显示10个
                                siteKey = site.key
                            )
                        )
                    }
                }

            // 4. 更新UI状态 (不再提供硬编码的默认分类)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                recommendMovies = recommendMovies,
                quickCategories = quickCategories,
                homeCategories = homeCategorySections,
                currentSite = site,
                availableSites = configManager.getAllSites(),
                error = null
            )

            // 5. 记录加载结果
            if (recommendMovies.isEmpty() && quickCategories.isEmpty() && homeCategorySections.isEmpty()) {
                Log.d("ONETV_MOVIE", "📭 没有加载到任何内容，显示空状态界面")
            } else {
                Log.d("ONETV_MOVIE", "🎉 内容加载成功: 推荐=${recommendMovies.size}, 分类=${quickCategories.size}, 区域=${homeCategorySections.size}")
            }

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "内容加载失败", e)
            // 网络错误时也显示空状态而不是错误
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = null // 显示空状态而不是错误
            )
        }
    }

    /**
     * 刷新首页数据 (强制刷新配置)
     */
    fun refresh() {
        Log.d("ONETV_MOVIE", "🔄 用户触发刷新，强制更新配置")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 强制刷新配置 - 使用FongMi_TV的RepositoryAdapter
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
     * 检查并更新配置 (应用启动时调用)
     */
    fun checkAndUpdateConfig() {
        viewModelScope.launch {
            try {
                val needUpdate = repositoryAdapter.isConfigUpdateNeeded()
                if (needUpdate) {
                    Log.d("ONETV_MOVIE", "🔄 检测到配置需要更新，自动刷新")
                    refresh()
                } else {
                    Log.d("ONETV_MOVIE", "✅ 配置缓存有效，直接加载")
                    loadHomeData()
                }
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "❌ 配置检查失败，直接加载", e)
                loadHomeData()
            }
        }
    }

    /**
     * 切换站点 (允许用户自由切换)
     */
    fun switchSite(siteKey: String) {
        viewModelScope.launch {
            Log.d("ONETV_MOVIE", "🔄 用户切换站点: $siteKey")
            val site = configManager.getSite(siteKey)
            if (site != null) {
                configManager.setCurrentSite(site)
                loadHomeData()
            } else {
                Log.w("ONETV_MOVIE", "⚠️ 未找到站点: $siteKey")
            }
        }
    }

    /**
     * 选择仓库线路
     */
    fun selectRoute(routeUrl: VodConfigUrl) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showRouteSelector = false,
                error = null
            )

            try {
                Log.d("ONETV_MOVIE", "🔗 用户选择线路: ${routeUrl.name}")
                Log.d("ONETV_MOVIE", "🌐 线路URL: ${routeUrl.url}")

                // 解析选中的线路配置 - 使用FongMi_TV的RepositoryAdapter
                repositoryAdapter.parseRouteConfig(routeUrl.url)

                // 等待配置加载完成
                delay(1000) // 给适配器一些时间处理

                // 获取当前配置
                val config = vodConfig
                if (config.sites.isEmpty()) {
                    Log.e("ONETV_MOVIE", "线路解析失败")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "线路解析失败",
                        showRouteSelector = false // 不自动弹出选择器，用户可通过右上角按钮手动选择
                    )
                    return@launch
                }

                Log.d("ONETV_MOVIE", "✅ 线路解析成功: 站点=${config.sites.size}个")

                // 加载配置到管理器 - 转换VodConfig到VodConfigResponse
                val configResponse = VodConfigResponse(
                    spider = config.spider,
                    sites = config.sites.map { site ->
                        VodSite(
                            key = site.key,
                            name = site.name ?: "",
                            api = site.api ?: "",
                            type = site.type
                        )
                    }
                )
                val loadResult = configManager.load(configResponse)
                if (loadResult.isFailure) {
                    Log.e("ONETV_MOVIE", "配置加载失败", loadResult.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置加载失败: ${loadResult.exceptionOrNull()?.message}",
                        showRouteSelector = false // 不自动弹出选择器，用户可通过右上角按钮手动选择
                    )
                    return@launch
                }

                Log.d("ONETV_MOVIE", "✅ 线路配置加载成功，按TVBOX标准直接使用默认配置")

                // 保持仓库索引状态，更新当前站点信息（TVBOX标准：保留线路切换功能）
                val currentSite = configManager.getCurrentSite()
                _uiState.value = _uiState.value.copy(
                    // 保持仓库索引状态，以便右上角显示线路选择器
                    isStoreHouseIndex = true,
                    currentSite = currentSite,
                    availableSites = configManager.getAllSites(),
                    showRouteSelector = false,
                    isLoading = false
                )

                Log.d("ONETV_MOVIE", "🎯 UI状态更新: isStoreHouseIndex=true, 当前站点=${currentSite?.name}")

                // 直接加载首页内容，不再提供站点选择
                loadHomeDataDirectly()

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "线路选择失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "线路选择失败: ${e.message}",
                    showRouteSelector = false // 不自动弹出选择器，用户可通过右上角按钮手动选择
                )
            }
        }
    }

    /**
     * 显示线路选择器（用于右上角按钮）
     */
    fun showRouteSelector() {
        if (_uiState.value.isStoreHouseIndex) {
            Log.d("ONETV_MOVIE", "🔗 显示仓库线路选择器，可用线路: ${_uiState.value.availableRoutes.size}")
            _uiState.value = _uiState.value.copy(showRouteSelector = true)
        } else {
            Log.d("ONETV_MOVIE", "⚠️ 非仓库索引状态，无法显示线路选择器")
        }
    }

    /**
     * 隐藏线路选择器
     */
    fun hideRouteSelector() {
        _uiState.value = _uiState.value.copy(showRouteSelector = false)
    }

    /**
     * 直接加载首页数据（TVBOX标准：选择子仓库后直接使用，无需再选择站点）
     */
    private fun loadHomeDataDirectly() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🎯 按TVBOX标准直接加载内容，使用默认站点")

                // 获取默认站点（第一个可用站点）
                val defaultSite = configManager.getCurrentSite()
                if (defaultSite == null) {
                    Log.w("ONETV_MOVIE", "未找到默认站点，显示空状态")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    return@launch
                }

                Log.d("ONETV_MOVIE", "✅ 使用默认站点: ${defaultSite.name}")

                // 获取站点分类 - 使用FongMi_TV的RepositoryAdapter
                Log.d("ONETV_MOVIE", "🔍 开始获取站点分类: ${defaultSite.key}")
                repositoryAdapter.getCategories()

                // 临时空列表，实际数据通过SiteViewModel观察获取
                val categories = emptyList<VodClass>()
                Log.d("ONETV_MOVIE", "✅ 分类获取请求已发送，等待SiteViewModel响应")
                categories.forEach { category ->
                    Log.d("ONETV_MOVIE", "📂 分类: ${category.typeName} (${category.typeId})")
                }

                // 直接加载首页内容，不显示站点选择器
                loadHomeContent(defaultSite, categories)

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "直接加载首页数据失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取分类内容
     */
    fun loadCategoryContent(categoryId: String, page: Int = 1) {
        viewModelScope.launch {
            try {
                val currentSite = configManager.getCurrentSite()
                if (currentSite == null) {
                    _uiState.value = _uiState.value.copy(error = "未找到可用站点")
                    return@launch
                }

                // 使用FongMi_TV的RepositoryAdapter获取内容
                repositoryAdapter.getContentList(categoryId, page, emptyMap())

                // 临时处理，实际数据通过SiteViewModel观察获取
                val response = VodListResponse(
                    code = 1,
                    msg = "",
                    list = emptyList(),
                    classes = emptyList(),
                    filters = emptyMap(),
                    page = page,
                    pagecount = 1,
                    limit = 20,
                    total = 0
                )

                // 更新对应分类的内容
                val updatedSections = _uiState.value.homeCategories.map { section ->
                    if (section.categoryId == categoryId) {
                        section.copy(movies = response.list)
                    } else {
                        section
                    }
                }

                _uiState.value = _uiState.value.copy(
                    homeCategories = updatedSections
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "分类内容加载失败"
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
     * 获取配置统计信息
     */
    fun getConfigStats(): Map<String, Any> {
        return configManager.getConfigStats()
    }

    /**
     * 检查是否有可用站点
     */
    fun hasAvailableSites(): Boolean {
        return configManager.getAllSites().isNotEmpty()
    }

    /**
     * 获取当前站点信息
     */
    fun getCurrentSiteInfo(): VodSite? {
        return configManager.getCurrentSite()
    }

    /**
     * 清除缓存并重新加载（用于TVBOX仓库索引检测）
     */
    fun clearCacheAndReload() {
        viewModelScope.launch {
            try {
                Log.d("ONETV_MOVIE", "🔄 用户请求清除缓存并重新加载")

                // 清除缓存 - 使用FongMi_TV的RepositoryAdapter
                repositoryAdapter.clearConfigCache()
                Log.d("ONETV_MOVIE", "✅ 缓存清除请求已发送，重新加载配置")

                // 重新加载配置
                loadHomeData()

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "清除缓存并重新加载失败", e)
                _uiState.value = _uiState.value.copy(
                    error = "操作失败: ${e.message}"
                )
            }
        }
    }
}
