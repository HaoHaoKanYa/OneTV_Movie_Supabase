package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.bean.Vod
import top.cywin.onetv.movie.bean.Class
import top.cywin.onetv.movie.bean.Filter
import android.util.Log

/**
 * 分类页面UI状态数据类
 */
data class CategoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val typeId: String = "",
    val typeName: String = "",
    val categoryName: String = "",
    val movies: List<Vod> = emptyList(),
    val categories: List<Class> = emptyList(),
    val selectedCategory: Class? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val filters: Map<String, List<String>> = emptyMap(),
    val selectedFilters: Map<String, String> = emptyMap(),
    val availableFilters: List<Filter> = emptyList()
)

/**
 * OneTV Movie分类页面ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MovieCategoryViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    /**
     * 初始化分类页面 - 通过适配器调用FongMi_TV解析系统
     */
    fun initCategory(typeId: String, typeName: String = "") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    typeId = typeId,
                    typeName = typeName,
                    error = null
                )

                Log.d("ONETV_MOVIE", "📂 初始化分类页面: $typeId - $typeName")

                // ✅ 通过适配器获取分类列表 - 解析逻辑在FongMi_TV中
                repositoryAdapter.getCategories()

                // ✅ 通过适配器获取分类内容 - 解析逻辑在FongMi_TV中
                repositoryAdapter.getContentList(typeId, 1, emptyMap())

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 分类内容加载请求已发送")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPage = 1,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "分类初始化失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "分类初始化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载更多内容
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.hasMore && currentState.typeId.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    Log.d("ONETV_MOVIE", "📄 加载更多: ${currentState.typeId}, 页码: ${currentState.currentPage + 1}")

                    // ✅ 通过适配器加载更多内容 - 解析逻辑在FongMi_TV中
                    repositoryAdapter.getContentList(
                        currentState.typeId,
                        currentState.currentPage + 1,
                        currentState.selectedFilters
                    )

                    _uiState.value = _uiState.value.copy(
                        currentPage = currentState.currentPage + 1
                    )

                } catch (e: Exception) {
                    Log.e("ONETV_MOVIE", "加载更多失败", e)
                    _uiState.value = _uiState.value.copy(
                        error = "加载更多失败: ${e.message}"
                    )
                }
            }
        }
    }


    /**
     * 应用筛选条件
     */
    fun applyFilters(filters: Map<String, String>) {
        val currentState = _uiState.value
        if (currentState.typeId.isEmpty()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    selectedFilters = filters,
                    currentPage = 1
                )

                Log.d("ONETV_MOVIE", "🔍 应用筛选条件: $filters")

                // ✅ 通过适配器应用筛选 - 解析逻辑在FongMi_TV中
                repositoryAdapter.getContentList(currentState.typeId, 1, filters)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "筛选失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "筛选失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 刷新分类内容
     */
    fun refresh() {
        val currentState = _uiState.value
        if (currentState.typeId.isNotEmpty()) {
            initCategory(currentState.typeId, currentState.typeName)
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
