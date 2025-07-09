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
 * 搜索页面ViewModel
 */
@HiltViewModel
class MovieSearchViewModel @Inject constructor(
    private val repository: VodRepository,
    private val configManager: VodConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadSearchData()
    }

    /**
     * 加载搜索页面数据
     */
    private fun loadSearchData() {
        viewModelScope.launch {
            try {
                // 加载搜索历史
                val searchHistory = getSearchHistory()
                
                // 加载热门关键词
                val hotKeywords = getHotKeywords()

                _uiState.value = _uiState.value.copy(
                    searchHistory = searchHistory,
                    hotKeywords = hotKeywords
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "数据加载失败"
                )
            }
        }
    }

    /**
     * 执行搜索
     */
    fun search(keyword: String, page: Int = 1) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            try {
                if (page == 1) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        keyword = keyword,
                        searchResults = emptyList(),
                        currentPage = 1,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = true)
                }

                // 获取当前站点
                val currentSite = configManager.getCurrentSite()
                if (currentSite == null) {
                    throw Exception("未找到可用站点")
                }

                // 执行搜索
                val result = repository.searchContent(keyword, page, currentSite.key)
                val response = result.getOrThrow()

                val newResults = response.list ?: emptyList()
                val allResults = if (page == 1) {
                    newResults
                } else {
                    _uiState.value.searchResults + newResults
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    searchResults = allResults,
                    currentPage = page,
                    totalPages = response.pagecount ?: 1,
                    hasMore = page < (response.pagecount ?: 1),
                    error = null
                )

                // 保存搜索历史
                saveSearchHistory(keyword)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }

    /**
     * 加载更多搜索结果
     */
    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.hasMore && !currentState.isLoadingMore && currentState.keyword.isNotEmpty()) {
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
            totalPages = 1,
            hasMore = false,
            error = null
        )
    }

    /**
     * 获取搜索历史
     */
    private suspend fun getSearchHistory(): List<String> {
        return try {
            // TODO: 从本地存储获取搜索历史
            listOf("复仇者联盟", "钢铁侠", "蜘蛛侠", "黑寡妇", "雷神")
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取热门关键词
     */
    private suspend fun getHotKeywords(): List<String> {
        return try {
            val currentSite = configManager.getCurrentSite()
            if (currentSite != null) {
                // TODO: 从API获取热门关键词
                listOf("漫威", "DC", "动作", "科幻", "喜剧", "爱情", "悬疑", "恐怖")
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            listOf("热门电影", "最新电视剧", "经典动漫", "纪录片")
        }
    }

    /**
     * 保存搜索历史
     */
    private suspend fun saveSearchHistory(keyword: String) {
        try {
            // TODO: 保存搜索历史到本地存储
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
            e.printStackTrace()
        }
    }

    /**
     * 删除搜索历史项
     */
    fun removeSearchHistory(keyword: String) {
        val currentHistory = _uiState.value.searchHistory.toMutableList()
        currentHistory.remove(keyword)
        
        _uiState.value = _uiState.value.copy(
            searchHistory = currentHistory
        )
        
        // TODO: 同步到本地存储
    }

    /**
     * 清空搜索历史
     */
    fun clearSearchHistory() {
        _uiState.value = _uiState.value.copy(
            searchHistory = emptyList()
        )
        
        // TODO: 清空本地存储
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 获取搜索建议
     */
    fun getSearchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        
        val history = _uiState.value.searchHistory
        val hotKeywords = _uiState.value.hotKeywords
        
        return (history + hotKeywords)
            .filter { it.contains(query, ignoreCase = true) }
            .distinct()
            .take(5)
    }
}
