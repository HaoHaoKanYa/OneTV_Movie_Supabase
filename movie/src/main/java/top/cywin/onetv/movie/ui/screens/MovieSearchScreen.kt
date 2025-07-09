package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.data.models.SearchUiState
import top.cywin.onetv.movie.data.models.VodItem
import top.cywin.onetv.movie.navigation.MovieRoutes
import top.cywin.onetv.movie.ui.components.MovieListCard
import top.cywin.onetv.movie.ui.components.SearchSuggestions
import top.cywin.onetv.movie.viewmodel.MovieSearchViewModel

/**
 * 搜索页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    initialKeyword: String = "",
    navController: NavController,
    viewModel: MovieSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MovieSearchContent(
        initialKeyword = initialKeyword,
        navController = navController,
        uiState = uiState,
        onSearch = { viewModel.search(it) },
        onLoadMore = { viewModel.loadMore() },
        onMovieClick = { movie ->
            navController.navigate(MovieRoutes.detail(movie.vodId, movie.siteKey))
        }
    )
}

@Composable
private fun MovieSearchContent(
    initialKeyword: String,
    navController: NavController,
    uiState: SearchUiState,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onMovieClick: (VodItem) -> Unit
) {
    var searchText by remember { mutableStateOf(initialKeyword) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    LaunchedEffect(initialKeyword) {
        if (initialKeyword.isNotEmpty()) {
            searchText = initialKeyword
            onSearch(initialKeyword)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 搜索栏
        SearchTopBar(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onSearch = {
                keyboardController?.hide()
                onSearch(searchText)
            },
            onBackClick = { navController.popBackStack() },
            onClearClick = { searchText = "" }
        )
        
        when {
            uiState.isLoading && uiState.searchResults.isEmpty() -> {
                // 搜索加载状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            
            uiState.error != null -> {
                // 错误状态
                ErrorContent(
                    error = uiState.error,
                    onRetry = { onSearch(searchText) }
                )
            }
            
            uiState.searchResults.isEmpty() && uiState.keyword.isNotEmpty() -> {
                // 无搜索结果
                NoResultContent(keyword = uiState.keyword)
            }
            
            uiState.searchResults.isNotEmpty() -> {
                // 搜索结果
                SearchResultContent(
                    results = uiState.searchResults,
                    hasMore = uiState.hasMore,
                    isLoadingMore = uiState.isLoadingMore,
                    onMovieClick = onMovieClick,
                    onLoadMore = onLoadMore
                )
            }
            
            else -> {
                // 默认状态：搜索历史和热门关键词
                SearchSuggestions(
                    searchHistory = uiState.searchHistory,
                    hotKeywords = uiState.hotKeywords,
                    onKeywordClick = { keyword ->
                        searchText = keyword
                        onSearch(keyword)
                    }
                )
            }
        }
    }
}

/**
 * 搜索顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                placeholder = { Text("搜索影片...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black
        )
    )
}

/**
 * 搜索结果内容
 */
@Composable
private fun SearchResultContent(
    results: List<VodItem>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onMovieClick: (VodItem) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results) { movie ->
            MovieListCard(
                movie = movie,
                onClick = { onMovieClick(movie) }
            )
        }
        
        // 加载更多
        if (hasMore && !isLoadingMore) {
            item {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}





/**
 * 无结果内容
 */
@Composable
private fun NoResultContent(keyword: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "未找到 \"$keyword\" 的相关结果",
            color = Color.White,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请尝试其他关键词",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = Color.White,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}
