package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.adapter.ViewModelAdapter
import top.cywin.onetv.movie.event.CategoryContentEvent
import top.cywin.onetv.movie.event.HomeContentEvent
import top.cywin.onetv.movie.event.ErrorEvent
import top.cywin.onetv.movie.ui.model.CategoryInfo
import top.cywin.onetv.movie.ui.model.MovieItem
import top.cywin.onetv.movie.ui.model.FilterOption
import top.cywin.onetv.movie.ui.base.MovieViewType

/**
 * OneTV Movie分类ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV解析功能，完整的事件驱动架构
 */
class MovieCategoryViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_CATEGORY_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MovieCategoryViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MovieCategoryViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }

    // ===== EventBus事件监听 =====

    /**
     * 监听分类内容事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCategoryContent(event: CategoryContentEvent) {
        Log.d(TAG, "📡 收到分类内容事件: typeId=${event.typeId}, count=${event.vods.size}")

        val currentState = _uiState.value
        if (currentState.currentCategory?.typeId == event.typeId) {
            val movieItems = event.vods.map { vod ->
                ViewModelAdapter.convertVodToMovie(vod)
            }.filterNotNull()

            val updatedMovies = if (event.page == 1) {
                movieItems
            } else {
                currentState.movies + movieItems
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                movies = updatedMovies,
                currentPage = event.page,
                hasMore = event.hasMore,
                totalCount = event.total,
                error = null
            )
        }
    }

    /**
     * 监听首页内容事件（获取分类列表）
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHomeContent(event: HomeContentEvent) {
        Log.d(TAG, "📡 收到首页内容事件: success=${event.isSuccess}")

        if (event.isSuccess) {
            val categoryInfos = event.categories.map { clazz ->
                ViewModelAdapter.convertClassToCategory(clazz)
            }.filterNotNull()

            _uiState.value = _uiState.value.copy(
                categories = categoryInfos,
                isLoadingCategories = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoadingCategories = false,
                error = "分类列表加载失败"
            )
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
            isLoadingCategories = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 初始化分类页面
     */
    fun initializeCategory(typeId: String? = null) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 初始化分类页面: typeId=$typeId")

                // ✅ 加载分类列表
                loadCategories()

                // ✅ 如果指定了分类ID，直接加载该分类内容
                if (!typeId.isNullOrEmpty()) {
                    delay(500) // 等待分类列表加载
                    selectCategory(typeId)
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 分类页面初始化失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "分类页面初始化失败: ${e.message}"
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
                Log.d(TAG, "📋 开始加载分类列表")
                _uiState.value = _uiState.value.copy(
                    isLoadingCategories = true,
                    error = null
                )

                // ✅ 通过适配器获取分类列表
                repositoryAdapter.getCategories()

            } catch (e: Exception) {
                Log.e(TAG, "💥 分类列表加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingCategories = false,
                    error = "分类列表加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 选择分类
     */
    fun selectCategory(typeId: String) {
        val category = _uiState.value.categories.find { it.typeId == typeId }
        if (category == null) {
            _uiState.value = _uiState.value.copy(error = "分类不存在")
            return
        }

        Log.d(TAG, "📂 选择分类: ${category.typeName}")

        _uiState.value = _uiState.value.copy(
            currentCategory = category,
            movies = emptyList(),
            currentPage = 1,
            hasMore = true,
            totalCount = 0,
            currentFilters = emptyMap()
        )

        // 加载分类内容
        loadCategoryContent(typeId, 1, emptyMap())
    }

    /**
     * 加载分类内容
     */
    fun loadCategoryContent(
        typeId: String,
        page: Int = 1,
        filters: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎬 加载分类内容: typeId=$typeId, page=$page")

                if (page == 1) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = true
                    )
                }

                // ✅ 通过适配器获取分类内容
                repositoryAdapter.getContentList(typeId, page, filters)

            } catch (e: Exception) {
                Log.e(TAG, "💥 分类内容加载失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "分类内容加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载更多内容
     */
    fun loadMore() {
        val currentState = _uiState.value
        val currentCategory = currentState.currentCategory ?: return

        if (currentState.hasMore && !currentState.isLoadingMore) {
            val nextPage = currentState.currentPage + 1
            loadCategoryContent(
                typeId = currentCategory.typeId,
                page = nextPage,
                filters = currentState.currentFilters
            )
        }
    }

    /**
     * 应用筛选条件
     */
    fun applyFilters(filters: Map<String, String>) {
        val currentCategory = _uiState.value.currentCategory ?: return

        Log.d(TAG, "🔍 应用筛选条件: $filters")

        _uiState.value = _uiState.value.copy(
            currentFilters = filters,
            showFilterDialog = false
        )

        // 重新加载第一页
        loadCategoryContent(
            typeId = currentCategory.typeId,
            page = 1,
            filters = filters
        )
    }

    /**
     * 清除筛选条件
     */
    fun clearFilters() {
        val currentCategory = _uiState.value.currentCategory ?: return

        Log.d(TAG, "🧹 清除筛选条件")

        _uiState.value = _uiState.value.copy(
            currentFilters = emptyMap()
        )

        // 重新加载第一页
        loadCategoryContent(
            typeId = currentCategory.typeId,
            page = 1,
            filters = emptyMap()
        )
    }

    /**
     * 刷新当前分类
     */
    fun refresh() {
        val currentCategory = _uiState.value.currentCategory ?: return
        val currentFilters = _uiState.value.currentFilters

        loadCategoryContent(
            typeId = currentCategory.typeId,
            page = 1,
            filters = currentFilters
        )
    }

    /**
     * 显示筛选对话框
     */
    fun showFilterDialog() {
        _uiState.value = _uiState.value.copy(showFilterDialog = true)
    }

    /**
     * 隐藏筛选对话框
     */
    fun hideFilterDialog() {
        _uiState.value = _uiState.value.copy(showFilterDialog = false)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 分类UI状态数据类
 */
data class CategoryUiState(
    // 基础状态
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val error: String? = null,

    // 分类数据
    val categories: List<CategoryInfo> = emptyList(),
    val currentCategory: CategoryInfo? = null,

    // 内容数据
    val movies: List<MovieItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val totalCount: Int = 0,

    // 筛选状态
    val currentFilters: Map<String, String> = emptyMap(),
    val availableFilters: List<FilterOption> = emptyList(),
    val showFilterDialog: Boolean = false,

    // UI控制
    val viewType: MovieViewType = MovieViewType.RECT,
    val sortType: CategorySortType = CategorySortType.DEFAULT
)

enum class CategorySortType {
    DEFAULT, NAME, YEAR, RATING
}
