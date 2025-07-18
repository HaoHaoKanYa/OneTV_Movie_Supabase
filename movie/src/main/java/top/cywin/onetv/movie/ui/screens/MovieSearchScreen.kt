package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import top.cywin.onetv.movie.viewmodel.MovieSearchViewModel
import top.cywin.onetv.movie.viewmodel.SearchUiState
import top.cywin.onetv.movie.bean.Movie
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie搜索页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    initialKeyword: String = "",
    navController: NavController,
    viewModel: MovieSearchViewModel = viewModel { MovieSearchViewModel() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val siteViewModel = movieApp.siteViewModel

    // ✅ 观察FongMi_TV的搜索数据变化
    // val searchResult by siteViewModel.result.observeAsState()

    // ✅ 页面初始化
    LaunchedEffect(initialKeyword) {
        Log.d("ONETV_MOVIE", "🔍 MovieSearchScreen 初始化: keyword=$initialKeyword")
        if (initialKeyword.isNotEmpty()) {
            viewModel.search(initialKeyword)
        }
    }

    // ✅ UI状态处理
    SearchContent(
        uiState = uiState,
        // searchResult = searchResult,
        onSearch = { keyword -> viewModel.search(keyword) },
        onMovieClick = { movie ->
            navController.navigate("detail/${movie.vodId}/${movie.siteKey}")
        },
        onHistoryClick = { keyword -> viewModel.search(keyword) },
        onClearHistory = { viewModel.clearSearchHistory() },
        onLoadMore = { viewModel.loadMoreResults() },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    // searchResult: Any?, // FongMi_TV的搜索结果
    onSearch: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit
) {
    var searchText by remember { mutableStateOf(uiState.currentKeyword) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 搜索栏
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("搜索电影、电视剧...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearch(searchText)
                        }
                    ),
                    trailingIcon = {
                        Row {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空")
                                }
                            }
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onSearch(searchText)
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                        }
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        // 内容区域
        when {
            uiState.isLoading && uiState.searchResults.isEmpty() -> {
                LoadingScreen(message = "正在搜索...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = { onSearch(searchText) },
                    onBack = onBack
                )
            }
            uiState.searchResults.isEmpty() && uiState.currentKeyword.isNotEmpty() -> {
                NoResultContent(keyword = uiState.currentKeyword)
            }
            uiState.searchResults.isNotEmpty() -> {
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
                SearchSuggestionsContent(
                    searchHistory = uiState.searchHistory,
                    hotKeywords = uiState.hotKeywords,
                    onKeywordClick = onHistoryClick,
                    onClearHistory = onClearHistory
                )
            }
        }
    }
}

@Composable
private fun SearchResultContent(
    results: List<Movie>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onMovieClick: (Movie) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(results) { movie ->
            MovieCard(
                movie = movie,
                onClick = { onMovieClick(movie) }
            )
        }

        // 加载更多指示器
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}





@Composable
private fun MovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            // 电影海报占位符
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 电影信息
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = movie.vodName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.vodRemarks.isNotEmpty()) {
                    Text(
                        text = movie.vodRemarks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionsContent(
    searchHistory: List<String>,
    hotKeywords: List<String>,
    onKeywordClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 搜索历史
        if (searchHistory.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "搜索历史",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onClearHistory) {
                            Text("清空")
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchHistory) { keyword ->
                            FilterChip(
                                onClick = { onKeywordClick(keyword) },
                                label = { Text(keyword) },
                                selected = false
                            )
                        }
                    }
                }
            }
        }

        // 热门搜索
        if (hotKeywords.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "热门搜索",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hotKeywords) { keyword ->
                            FilterChip(
                                onClick = { onKeywordClick(keyword) },
                                label = { Text(keyword) },
                                selected = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoResultContent(keyword: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "未找到 \"$keyword\" 的相关结果",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请尝试其他关键词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ✅ 按照指南添加必要的辅助Composable函数

@Composable
private fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message)
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRetry) {
                    Text("重试")
                }
                OutlinedButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}
