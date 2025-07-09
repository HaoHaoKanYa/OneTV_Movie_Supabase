package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.data.repository.VodRepository
import top.cywin.onetv.movie.data.VodConfigManager
import javax.inject.Inject

/**
 * 点播首页ViewModel (参考OneMoVie架构)
 */
@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: VodRepository,
    private val configManager: VodConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * 加载首页数据 (动态分类驱动)
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. 加载配置文件
                val configResult = repository.loadConfig()
                if (configResult.isFailure) {
                    throw configResult.exceptionOrNull() ?: Exception("配置加载失败")
                }

                // 2. 获取当前站点和分类
                val currentSite = configManager.getCurrentSite()
                if (currentSite == null) {
                    throw Exception("未找到可用站点")
                }

                // 3. 获取站点分类 (动态获取，不硬编码)
                val categoriesResult = repository.getCategories(currentSite.key)
                val categories = categoriesResult.getOrNull() ?: emptyList()

                // 4. 加载首页内容
                loadHomeContent(currentSite, categories)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }

    /**
     * 加载首页内容 (基于动态分类，不再硬编码分类名称)
     */
    private suspend fun loadHomeContent(site: VodSite, categories: List<VodClass>) {
        try {
            // 1. 加载推荐内容
            val recommendResult = repository.getRecommendContent(site.key)
            val recommendMovies = recommendResult.getOrNull() ?: emptyList()

            // 2. 设置快速导航分类 (取前5个启用的分类)
            val quickCategories = categories.filter { it.isEnabled() }.take(5)

            // 3. 为每个分类动态加载内容
            val homeCategorySections = mutableListOf<HomeCategorySection>()

            categories.filter { it.isEnabled() }.take(6).forEach { category ->
                val contentResult = repository.getContentList(
                    typeId = category.typeId,
                    page = 1,
                    siteKey = site.key
                )

                contentResult.getOrNull()?.let { response ->
                    if (!response.list.isNullOrEmpty()) {
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
            }

            // 4. 更新UI状态
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                recommendMovies = recommendMovies,
                quickCategories = quickCategories,
                homeCategories = homeCategorySections,
                currentSite = site,
                availableSites = configManager.getAllSites(),
                error = null
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "内容加载失败"
            )
        }
    }

    /**
     * 刷新首页数据
     */
    fun refresh() {
        loadHomeData()
    }

    /**
     * 切换站点
     */
    fun switchSite(siteKey: String) {
        viewModelScope.launch {
            val site = configManager.getSite(siteKey)
            if (site != null) {
                configManager.setCurrentSite(site)
            }
            loadHomeData()
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

                val result = repository.getContentList(
                    typeId = categoryId,
                    page = page,
                    siteKey = currentSite.key
                )

                result.getOrNull()?.let { response ->
                    // 更新对应分类的内容
                    val updatedSections = _uiState.value.homeCategories.map { section ->
                        if (section.categoryId == categoryId) {
                            section.copy(movies = response.list ?: emptyList())
                        } else {
                            section
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        homeCategories = updatedSections
                    )
                }

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
}
