package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.cywin.onetv.movie.data.models.VodClass

/**
 * 分类标签组件
 */
@Composable
fun CategoryTabs(
    categories: List<VodClass>,
    selectedCategory: VodClass?,
    modifier: Modifier = Modifier,
    onCategorySelected: (VodClass) -> Unit = {}
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            CategoryTab(
                category = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

/**
 * 分类标签项
 */
@Composable
private fun CategoryTab(
    category: VodClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = category.typeName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = Color.DarkGray,
            labelColor = Color.Gray
        )
    )
}

/**
 * 快速分类导航 (用于首页)
 */
@Composable
fun QuickCategoryGrid(
    categories: List<VodClass>,
    modifier: Modifier = Modifier,
    onCategoryClick: (VodClass) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "分类导航",
            color = Color.White,
            fontSize = 18.sp,
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
 * 快速分类项
 */
@Composable
private fun QuickCategoryItem(
    category: VodClass,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(80.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.typeName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 垂直分类列表 (用于设置页面等)
 */
@Composable
fun CategoryList(
    categories: List<VodClass>,
    selectedCategory: VodClass?,
    modifier: Modifier = Modifier,
    onCategorySelected: (VodClass) -> Unit = {}
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
 * 分类列表项
 */
@Composable
private fun CategoryListItem(
    category: VodClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                Color.DarkGray
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
                text = category.typeName,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "已选择",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 分类统计信息
 */
@Composable
fun CategoryStats(
    categories: List<VodClass>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "分类统计",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "总分类数: ${categories.size}",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            val enabledCount = categories.count { it.isEnabled() }
            Text(
                text = "可用分类: $enabledCount",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            val withFiltersCount = categories.count { it.hasFilters() }
            Text(
                text = "支持筛选: $withFiltersCount",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
