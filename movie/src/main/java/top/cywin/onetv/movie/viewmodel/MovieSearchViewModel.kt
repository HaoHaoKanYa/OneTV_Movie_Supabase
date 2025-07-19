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
import top.cywin.onetv.movie.event.SearchStartEvent
import top.cywin.onetv.movie.event.SearchResultEvent
import top.cywin.onetv.movie.event.SearchErrorEvent
import top.cywin.onetv.movie.event.ErrorEvent
import top.cywin.onetv.movie.ui.model.MovieItem
import top.cywin.onetv.movie.ui.base.MovieViewType

/**
 * OneTV Movie搜索ViewModel - 完整版本
 * 通过适配器系统调用FongMi_TV解析功能，完整的事件驱动架构
 */
class MovieSearchViewModel : ViewModel() {

    companion object {
        private const val TAG = "ONETV_MOVIE_SEARCH_VM"
    }

    // ✅ 通过MovieApp访问适配器系统
    private val movieApp = MovieApp.getInstance()
    private val repositoryAdapter = movieApp.repositoryAdapter
    private val viewModelAdapter = movieApp.viewModelAdapter

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "🏗️ MovieSearchViewModel 初始化")

        // ✅ 注册EventBus监听FongMi_TV事件
        EventBus.getDefault().register(this)

        // ✅ 加载搜索历史
        loadSearchHistory()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹 MovieSearchViewModel 清理")

        // ✅ 取消EventBus注册
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
            Log.e(TAG, "EventBus取消注册失败", e)
        }
    }

    // ===== EventBus事件监听 =====

    /**
     * 监听搜索开始事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSearchStart(event: SearchStartEvent) {
        Log.d(TAG, "📡 收到搜索开始事件: keyword=${event.keyword}")

        _uiState.value = _uiState.value.copy(
            isSearching = true,
            currentKeyword = event.keyword,
            error = null
        )
    }

    /**
     * 监听搜索结果事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSearchResult(event: SearchResultEvent) {
        Log.d(TAG, "📡 收到搜索结果事件: keyword=${event.keyword}, count=${event.results.size}")

        val movieItems = event.results.map { vod ->
            ViewModelAdapter.convertVodToMovie(vod)
        }.filterNotNull()

        _uiState.value = _uiState.value.copy(
            isSearching = false,
            searchResults = movieItems,
            hasMore = event.hasMore,
            currentPage = event.page,
            totalCount = event.total,
            error = null
        )

        // 保存搜索历史
        if (event.keyword.isNotEmpty() && movieItems.isNotEmpty()) {
            saveSearchHistory(event.keyword)
        }
    }

    /**
     * 监听搜索错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSearchError(event: SearchErrorEvent) {
        Log.e(TAG, "📡 收到搜索错误事件: keyword=${event.keyword}, error=${event.error}")

        _uiState.value = _uiState.value.copy(
            isSearching = false,
            error = event.error
        )
    }

    /**
     * 监听错误事件
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onError(event: ErrorEvent) {
        Log.e(TAG, "📡 收到错误事件: ${event.message}")

        _uiState.value = _uiState.value.copy(
            isSearching = false,
            error = event.message
        )
    }

    // ===== 公共方法 =====

    /**
     * 搜索内容
     */
    fun search(keyword: String, siteKey: String = "") {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请输入搜索关键词")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔍 开始搜索: keyword=$trimmedKeyword, siteKey=$siteKey")

                _uiState.value = _uiState.value.copy(
                    isSearching = true,
                    currentKeyword = trimmedKeyword,
                    searchResults = emptyList(),
                    currentPage = 1,
                    hasMore = false,
                    totalCount = 0,
                    error = null
                )

                // ✅ 通过适配器搜索内容
                repositoryAdapter.searchContent(trimmedKeyword, siteKey)

            } catch (e: Exception) {
                Log.e(TAG, "💥 搜索失败", e)
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "搜索失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新搜索关键词
     */
    fun updateKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(searchKeyword = keyword)

        // 实时搜索建议
        if (keyword.length >= 2) {
            updateSearchSuggestions(keyword)
        } else {
            _uiState.value = _uiState.value.copy(searchSuggestions = emptyList())
        }
    }

    /**
     * 清除搜索内容
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchKeyword = "",
            searchResults = emptyList(),
            currentKeyword = "",
            searchSuggestions = emptyList(),
            error = null
        )
    }

    /**
     * 从历史记录搜索
     */
    fun searchFromHistory(keyword: String) {
        _uiState.value = _uiState.value.copy(searchKeyword = keyword)
        search(keyword)
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            try {
                // 清除本地存储的搜索历史
                clearLocalSearchHistory()

                _uiState.value = _uiState.value.copy(searchHistory = emptyList())

            } catch (e: Exception) {
                Log.e(TAG, "清除搜索历史失败", e)
            }
        }
    }

    /**
     * 删除单个搜索历史
     */
    fun removeSearchHistory(keyword: String) {
        viewModelScope.launch {
            try {
                val updatedHistory = _uiState.value.searchHistory.filter { it != keyword }
                _uiState.value = _uiState.value.copy(searchHistory = updatedHistory)

                // 更新本地存储
                saveLocalSearchHistory(updatedHistory)

            } catch (e: Exception) {
                Log.e(TAG, "删除搜索历史失败", e)
            }
        }
    }

    /**
     * 显示搜索建议
     */
    fun showSearchSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = true)
    }

    /**
     * 隐藏搜索建议
     */
    fun hideSearchSuggestions() {
        _uiState.value = _uiState.value.copy(showSuggestions = false)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ===== 私有方法 =====

    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            try {
                val history = getLocalSearchHistory()
                _uiState.value = _uiState.value.copy(searchHistory = history)
            } catch (e: Exception) {
                Log.e(TAG, "加载搜索历史失败", e)
            }
        }
    }

    /**
     * 保存搜索历史
     */
    private fun saveSearchHistory(keyword: String) {
        viewModelScope.launch {
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

                _uiState.value = _uiState.value.copy(searchHistory = currentHistory)

                // 保存到本地存储
                saveLocalSearchHistory(currentHistory)

            } catch (e: Exception) {
                Log.e(TAG, "保存搜索历史失败", e)
            }
        }
    }

    /**
     * 更新搜索建议
     */
    private fun updateSearchSuggestions(keyword: String) {
        viewModelScope.launch {
            try {
                // 从搜索历史中筛选建议
                val suggestions = _uiState.value.searchHistory
                    .filter { it.contains(keyword, ignoreCase = true) }
                    .take(5)

                _uiState.value = _uiState.value.copy(searchSuggestions = suggestions)

            } catch (e: Exception) {
                Log.e(TAG, "更新搜索建议失败", e)
            }
        }
    }

    /**
     * 获取本地搜索历史
     */
    private suspend fun getLocalSearchHistory(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 这里应该从SharedPreferences或数据库读取
                // 暂时返回空列表
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "读取本地搜索历史失败", e)
                emptyList()
            }
        }
    }

    /**
     * 保存本地搜索历史
     */
    private suspend fun saveLocalSearchHistory(history: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                // 这里应该保存到SharedPreferences或数据库
                // 暂时不实现
            } catch (e: Exception) {
                Log.e(TAG, "保存本地搜索历史失败", e)
            }
        }
    }

    /**
     * 清除本地搜索历史
     */
    private suspend fun clearLocalSearchHistory() {
        withContext(Dispatchers.IO) {
            try {
                // 这里应该清除SharedPreferences或数据库中的搜索历史
                // 暂时不实现
            } catch (e: Exception) {
                Log.e(TAG, "清除本地搜索历史失败", e)
            }
        }
    }
}

/**
 * 搜索UI状态数据类
 */
data class SearchUiState(
    // 基础状态
    val isSearching: Boolean = false,
    val error: String? = null,

    // 搜索数据
    val searchKeyword: String = "",
    val currentKeyword: String = "",
    val searchResults: List<MovieItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val totalCount: Int = 0,

    // 搜索历史和建议
    val searchHistory: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val showSuggestions: Boolean = false,

    // UI控制
    val viewType: MovieViewType = MovieViewType.RECT,
    val showSearchHistory: Boolean = true
)
