package top.cywin.onetv.movie.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.cywin.onetv.movie.MovieApp
import top.cywin.onetv.movie.bean.Vod
import android.util.Log

/**
 * 搜索页面UI状态数据类
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val keyword: String = "",
    val searchResults: List<Vod> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val hotKeywords: List<String> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

/**
 * OneTV Movie搜索页面ViewModel
 * 通过适配器系统调用FongMi_TV解析功能，不参与线路接口解析
 */
class MovieSearchViewModel : ViewModel() {

    // ✅ 通过MovieApp访问适配器系统 - 不参与解析逻辑
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val siteViewModel = movieApp.siteViewModel

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * 执行搜索 - 通过适配器调用FongMi_TV解析系统
     */
    fun search(keyword: String, page: Int = 1) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    keyword = keyword,
                    error = null
                )

                Log.d("ONETV_MOVIE", "🔍 开始搜索: $keyword")

                // ✅ 通过适配器搜索内容 - 解析逻辑在FongMi_TV中
                repositoryAdapter.searchContent(keyword, "")

                // 实际数据通过SiteViewModel观察获取
                Log.d("ONETV_MOVIE", "✅ 搜索请求已发送: $keyword")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentPage = page,
                    error = null
                )

                // 保存搜索历史
                saveSearchHistory(keyword)

            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "搜索失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "搜索失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 加载更多搜索结果
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.hasMore && currentState.keyword.isNotEmpty()) {
            search(currentState.keyword, currentState.currentPage + 1)
        }
    }

    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            keyword = "",
            searchResults = emptyList(),
            currentPage = 1,
            hasMore = false,
            error = null
        )
    }

    /**
     * 保存搜索历史
     */
    private fun saveSearchHistory(keyword: String) {
        try {
            val currentHistory = _uiState.value.searchHistory.toMutableList()

            // 移除重复项
            currentHistory.remove(keyword)

            // 添加到开头
            currentHistory.add(0, keyword)

            // 限制历史记录数量
            if (currentHistory.size > 20) {
                currentHistory.removeAt(currentHistory.size - 1)
            }

            _uiState.value = _uiState.value.copy(
                searchHistory = currentHistory
            )

        } catch (e: Exception) {
            Log.e("ONETV_MOVIE", "保存搜索历史失败", e)
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
