package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// KotlinPoet专业重构 - 移除Hilt import
// import dagger.hilt.onetv.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.data.models.*
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.data.VodConfigManager
// KotlinPoet专业重构 - 移除Inject import
// import javax.inject.Inject

/**
 * 分类页面ViewModel
 * KotlinPoet专业重构 - 使用MovieApp单例管理依赖
 */
// @HiltViewModel
class MovieCategoryViewModel(
    private val configManager: VodConfigManager
) : ViewModel() {

    // 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    /**
     * 初始化分类页面
     */
    fun initCategory(typeId: String, siteKey: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. 获取站点信息
                val site = configManager.getSite(siteKey)
                if (site == null) {
                    throw Exception("未找到站点")
                }

                // 2. 获取所有分类 - 使用FongMi_TV的RepositoryAdapter
                repositoryAdapter.getCategories()

                // 临时处理，实际数据通过SiteViewModel观察获取
                val allCategories = emptyList<VodClass>()

                // 3. 找到当前分类
                val currentCategory = VodClass(typeId, "默认分类")
                if (currentCategory.typeId.isEmpty()) {
                    throw Exception("未找到分类")
                }

                // 4. 获取筛选条件
                val filterList = emptyList<VodFilter>() // 临时空列表
                val filters = emptyMap<String, List<VodFilter>>()

                // 5. 加载分类内容
                loadCategoryContent(typeId, site.key, 1, emptyMap())

                _uiState.value = _uiState.value.copy(
                    currentCategory = currentCategory,
                    availableCategories = allCategories,
                    filters = filters,
                    selectedFilters = emptyMap()
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "分类初始化失败"
                )
            }
        }
    }

    /**
     * 加载分类内容
     */
    private suspend fun loadCategoryContent(
        typeId: String,
        siteKey: String,
        page: Int,
        filters: Map<String, String>
    ) {
        try {
            // 使用FongMi_TV的RepositoryAdapter获取内容列表
            repositoryAdapter.getContentList(typeId, page, filters)

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
            val newMovies = response.list

            val allMovies = if (page == 1) {
                newMovies
            } else {
                _uiState.value.movies + newMovies
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                movies = allMovies,
                currentPage = page,
                totalPages = response.pagecount ?: 1,
                hasMore = page < (response.pagecount ?: 1),
                error = null
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = e.message ?: "内容加载失败"
            )
        }
    }

    /**
     * 加载更多内容
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.hasMore && !currentState.isLoadingMore && currentState.currentCategory != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)

                val site = configManager.getCurrentSite()
                if (site != null) {
                    loadCategoryContent(
                        typeId = currentState.currentCategory.typeId,
                        siteKey = site.key,
                        page = currentState.currentPage + 1,
                        filters = currentState.selectedFilters
                    )
                }
            }
        }
    }

    /**
     * 应用筛选条件
     */
    fun applyFilters(filters: Map<String, String>) {
        val currentCategory = _uiState.value.currentCategory ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selectedFilters = filters,
                movies = emptyList(),
                currentPage = 1
            )

            val site = configManager.getCurrentSite()
            if (site != null) {
                loadCategoryContent(
                    typeId = currentCategory.typeId,
                    siteKey = site.key,
                    page = 1,
                    filters = filters
                )
            }
        }
    }

    /**
     * 清除筛选条件
     */
    fun clearFilters() {
        applyFilters(emptyMap())
    }

    /**
     * 刷新分类内容
     */
    fun refresh() {
        val currentCategory = _uiState.value.currentCategory ?: return
        val site = configManager.getCurrentSite() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                movies = emptyList(),
                currentPage = 1
            )

            loadCategoryContent(
                typeId = currentCategory.typeId,
                siteKey = site.key,
                page = 1,
                filters = _uiState.value.selectedFilters
            )
        }
    }

    /**
     * 切换分类
     */
    fun switchCategory(category: VodClass) {
        val site = configManager.getCurrentSite() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentCategory = category,
                movies = emptyList(),
                currentPage = 1,
                selectedFilters = emptyMap(),
                filters = category.filters.groupBy { it.key }
            )

            loadCategoryContent(
                typeId = category.typeId,
                siteKey = site.key,
                page = 1,
                filters = emptyMap()
            )
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取当前筛选条件摘要
     */
    fun getFilterSummary(): String {
        val selectedFilters = _uiState.value.selectedFilters
        val filters = _uiState.value.filters

        if (selectedFilters.isEmpty()) return ""

        return selectedFilters.entries.mapNotNull { (key, value) ->
            if (value.isNotEmpty()) {
                val filter = filters[key]?.firstOrNull()
                val displayName = filter?.getDisplayName(value) ?: value
                displayName
            } else {
                null
            }
        }.joinToString(", ")
    }
}
