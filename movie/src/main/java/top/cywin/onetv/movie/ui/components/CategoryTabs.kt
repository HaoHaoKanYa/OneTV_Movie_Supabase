package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.cywin.onetv.movie.viewmodel.CategoryUiState
import top.cywin.onetv.movie.bean.Class
import top.cywin.onetv.movie.MovieApp
import android.util.Log

/**
 * OneTV Movie分类标签组件 - 按照FongMi_TV整合指南重构
 */
@Composable
fun CategoryTabs(
    categories: List<Class>,
    selectedCategory: Class?,
    onCategorySelected: (Class) -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val siteViewModel = movieApp.siteViewModel

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                onClick = { onCategorySelected(category) },
                label = { Text(category.getTypeName()) },
                selected = selectedCategory == category,
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@Composable
fun CategoryTabsWithLoading(
    uiState: CategoryUiState,
    onCategorySelected: (Class) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.categories.isEmpty() -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        uiState.categories.isNotEmpty() -> {
            CategoryTabs(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = modifier
            )
        }
    }
}

/**
 * 快速分类导航 (用于首页) - 重构版本
 */
@Composable
fun QuickCategoryGrid(
    categories: List<Class>,
    modifier: Modifier = Modifier,
    onCategoryClick: (Class) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "分类导航",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(categories) { category ->
                QuickCategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}

/**
 * 快速分类项 - 重构版本
 */
@Composable
private fun QuickCategoryItem(
    category: Class,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.getTypeName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 垂直分类列表 (用于设置页面等) - 重构版本
 */
@Composable
fun CategoryList(
    categories: List<Class>,
    selectedCategory: Class?,
    modifier: Modifier = Modifier,
    onCategorySelected: (Class) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { category ->
            CategoryListItem(
                category = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * 分类列表项 - 重构版本
 */
@Composable
private fun CategoryListItem(
    category: Class,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.getTypeName(),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "已选择",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
