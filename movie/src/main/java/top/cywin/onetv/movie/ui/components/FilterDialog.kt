package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import top.cywin.onetv.movie.data.models.VodFilter
import top.cywin.onetv.movie.data.models.VodFilterValue

/**
 * 筛选对话框组件
 */
@Composable
fun FilterDialog(
    filters: Map<String, List<VodFilter>>,
    selectedFilters: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var tempFilters by remember { mutableStateOf(selectedFilters.toMutableMap()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = "筛选条件",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 筛选内容
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filters.forEach { (key, filterList) ->
                        item {
                            FilterSection(
                                filter = filterList.firstOrNull(),
                                selectedValue = tempFilters[key] ?: "",
                                onValueChange = { value ->
                                    tempFilters[key] = value
                                }
                            )
                        }
                    }
                }
                
                // 按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempFilters.clear()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("重置")
                    }
                    
                    Button(
                        onClick = { onConfirm(tempFilters.toMap()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

/**
 * 筛选区域
 */
@Composable
private fun FilterSection(
    filter: VodFilter?,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    if (filter != null) {
        Column {
            Text(
                text = filter.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filter.value) { option ->
                    FilterOption(
                        option = option,
                        isSelected = selectedValue == option.v,
                        onSelect = { onValueChange(option.v) }
                    )
                }
            }
        }
    }
}

/**
 * 筛选选项
 */
@Composable
private fun FilterOption(
    option: VodFilterValue,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = Color.Gray
            )
        )
        
        Text(
            text = option.n,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * 快速筛选栏 (用于分类页面顶部)
 */
@Composable
fun QuickFilterBar(
    filters: Map<String, List<VodFilter>>,
    selectedFilters: Map<String, String>,
    onFilterChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filters.isNotEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            filters.forEach { (key, filterList) ->
                item {
                    QuickFilterRow(
                        filter = filterList.firstOrNull(),
                        selectedValue = selectedFilters[key] ?: "",
                        onValueChange = { value ->
                            onFilterChange(key, value)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 快速筛选行
 */
@Composable
private fun QuickFilterRow(
    filter: VodFilter?,
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    if (filter != null) {
        Column {
            Text(
                text = filter.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filter.value.take(8)) { option -> // 只显示前8个选项
                    FilterChip(
                        onClick = { onValueChange(option.v) },
                        label = { 
                            Text(
                                text = option.n,
                                fontSize = 12.sp
                            ) 
                        },
                        selected = selectedValue == option.v,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.Gray,
                            labelColor = Color.White
                        )
                    )
                }
                
                if (filter.value.size > 8) {
                    item {
                        AssistChip(
                            onClick = { /* TODO: 显示完整筛选对话框 */ },
                            label = { 
                                Text(
                                    text = "更多",
                                    fontSize = 12.sp
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.DarkGray,
                                labelColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 筛选结果摘要
 */
@Composable
fun FilterSummary(
    filters: Map<String, List<VodFilter>>,
    selectedFilters: Map<String, String>,
    onClearFilter: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFilters = selectedFilters.filter { it.value.isNotEmpty() }
    
    if (activeFilters.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.DarkGray.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选筛选条件",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "清空全部",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeFilters.toList()) { (key, value) ->
                        val filter = filters[key]?.firstOrNull()
                        val displayName = filter?.getDisplayName(value) ?: value
                        
                        FilterChip(
                            onClick = { onClearFilter(key) },
                            label = { 
                                Text(
                                    text = displayName,
                                    fontSize = 12.sp
                                ) 
                            },
                            selected = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
