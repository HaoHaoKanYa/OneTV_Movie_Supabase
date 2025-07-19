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
import top.cywin.onetv.movie.ui.model.MovieItem
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie搜索页面 - 按照FongMi_TV整合指南重构
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchScreen(
    navController: NavController,
    viewModel: MovieSearchViewModel = viewModel {
        MovieSearchViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ✅ UI内容渲染
    SearchContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onKeywordChange = { keyword -> viewModel.updateKeyword(keyword) },
        onSearch = { keyword -> viewModel.search(keyword) },
        onMovieClick = { movie ->
            navController.navigate("detail/${movie.vodId}/${movie.siteKey}")
        },
        onHistoryClick = { keyword -> viewModel.searchFromHistory(keyword) },
        onHistoryDelete = { keyword -> viewModel.removeSearchHistory(keyword) },
        onClearHistory = { viewModel.clearSearchHistory() },
        onClearSearch = { viewModel.clearSearch() },
        onShowSuggestions = { viewModel.showSearchSuggestions() },
        onHideSuggestions = { viewModel.hideSearchSuggestions() },
        onError = { viewModel.clearError() }
    )
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    onBack: () -> Unit,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onMovieClick: (MovieItem) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearSearch: () -> Unit,
    onShowSuggestions: () -> Unit,
    onHideSuggestions: () -> Unit,
    onError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 搜索栏
        SearchBar(
            keyword = uiState.searchKeyword,
            isSearching = uiState.isSearching,
            onKeywordChange = onKeywordChange,
            onSearch = onSearch,
            onClear = onClearSearch,
            onBack = onBack,
            onFocusChange = { focused ->
                if (focused) onShowSuggestions() else onHideSuggestions()
            }
        )

        // 内容区域
        when {
            uiState.isSearching -> {
                LoadingScreen(message = "正在搜索...")
            }
            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error,
                    onRetry = { onSearch(uiState.currentKeyword) },
                    onBack = onError
                )
            }
            uiState.searchKeyword.isEmpty() -> {
                SearchHomeContent(
                    searchHistory = uiState.searchHistory,
                    onHistoryClick = onHistoryClick,
                    onHistoryDelete = onHistoryDelete,
                    onClearHistory = onClearHistory
                )
            }
            uiState.searchResults.isEmpty() && uiState.currentKeyword.isNotEmpty() -> {
                NoResultContent(keyword = uiState.currentKeyword)
            }
            else -> {
                SearchResultContent(
                    results = uiState.searchResults,
                    hasMore = uiState.hasMore,
                    onMovieClick = onMovieClick
                )
            }
        }

        // 搜索建议
        if (uiState.showSuggestions && uiState.searchSuggestions.isNotEmpty()) {
            SearchSuggestionsOverlay(
                suggestions = uiState.searchSuggestions,
                onSuggestionClick = { suggestion ->
                    onKeywordChange(suggestion)
                    onSearch(suggestion)
                    onHideSuggestions()
                }
            )
        }
    }
}
}

@Composable
private fun SearchBar(
    keyword: String,
    isSearching: Boolean,
    onKeywordChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    TopAppBar(
        title = {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                placeholder = { Text("搜索电影、电视剧...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearch(keyword)
                    }
                ),
                trailingIcon = {
                    Row {
                        if (keyword.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.Clear, contentDescription = "清空")
                            }
                        }
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                onSearch(keyword)
                            },
                            enabled = !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
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
}

@Composable
private fun SearchHomeContent(
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
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
                                onClick = { onHistoryClick(keyword) },
                                label = { Text(keyword) },
                                selected = false,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onHistoryDelete(keyword) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "删除",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultContent(
    results: List<MovieItem>,
    hasMore: Boolean,
    onMovieClick: (MovieItem) -> Unit
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
    }
}

@Composable
private fun SearchSuggestionsOverlay(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn {
            items(suggestions) { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}





@Composable
private fun MovieCard(
    movie: MovieItem,
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
                    text = movie.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (movie.subtitle.isNotEmpty()) {
                    Text(
                        text = movie.subtitle,
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

// ✅ 按照指南添加必要的辅助Composable函数

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
