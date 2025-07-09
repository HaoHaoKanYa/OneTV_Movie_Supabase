package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
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

/**
 * 搜索栏组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索影片...",
    enabled: Boolean = true
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除"
                    )
                }
            }
        },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch(query)
            }
        ),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * 搜索建议组件
 */
@Composable
fun SearchSuggestions(
    searchHistory: List<String>,
    hotKeywords: List<String>,
    modifier: Modifier = Modifier,
    onKeywordClick: (String) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        // 搜索历史
        if (searchHistory.isNotEmpty()) {
            item {
                SearchSection(
                    title = "搜索历史",
                    icon = Icons.Default.History,
                    keywords = searchHistory,
                    onKeywordClick = onKeywordClick,
                    onClearClick = onClearHistory
                )
            }
        }
        
        // 热门搜索
        if (hotKeywords.isNotEmpty()) {
            item {
                SearchSection(
                    title = "热门搜索",
                    icon = Icons.Default.TrendingUp,
                    keywords = hotKeywords,
                    onKeywordClick = onKeywordClick
                )
            }
        }
    }
}

/**
 * 搜索区域
 */
@Composable
private fun SearchSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keywords: List<String>,
    onKeywordClick: (String) -> Unit,
    onClearClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (onClearClick != null) {
                TextButton(onClick = onClearClick) {
                    Text(
                        text = "清空",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 关键词标签
        KeywordTags(
            keywords = keywords,
            onKeywordClick = onKeywordClick
        )
    }
}

/**
 * 关键词标签
 */
@Composable
private fun KeywordTags(
    keywords: List<String>,
    onKeywordClick: (String) -> Unit
) {
    // 简化的网格布局
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(200.dp)
    ) {
        items(keywords) { keyword ->
            KeywordChip(
                keyword = keyword,
                onClick = { onKeywordClick(keyword) }
            )
        }
    }
}

/**
 * 关键词芯片
 */
@Composable
private fun KeywordChip(
    keyword: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = keyword,
                fontSize = 12.sp
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.DarkGray,
            labelColor = Color.White
        )
    )
}

/**
 * 搜索结果为空的提示
 */
@Composable
fun SearchEmptyState(
    query: String,
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "未找到 \"$query\" 的相关结果",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请尝试其他关键词或检查网络连接",
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetryClick) {
            Text("重新搜索")
        }
    }
}

/**
 * 搜索加载状态
 */
@Composable
fun SearchLoadingState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "正在搜索 \"$query\"...",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
