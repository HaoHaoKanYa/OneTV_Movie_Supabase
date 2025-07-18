package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import top.cywin.onetv.movie.bean.CategoryFilter

/**
 * OneTV Movie筛选对话框组件 - 按照FongMi_TV整合指南重构
 */
@Composable
fun FilterDialog(
    filters: List<CategoryFilter>,
    selectedFilters: Set<CategoryFilter>,
    onDismiss: () -> Unit,
    onConfirm: (Set<CategoryFilter>) -> Unit,
    onClearAll: () -> Unit
) {
    var tempSelectedFilters by remember { mutableStateOf(selectedFilters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选条件") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterItem(
                        filter = filter,
                        isSelected = tempSelectedFilters.contains(filter),
                        onToggle = { isSelected ->
                            tempSelectedFilters = if (isSelected) {
                                tempSelectedFilters + filter
                            } else {
                                tempSelectedFilters - filter
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempSelectedFilters) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

@Composable
private fun FilterItem(
    filter: CategoryFilter,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onToggle
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = filter.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (filter.description.isNotEmpty()) {
                Text(
                    text = filter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 简化的筛选摘要组件 - 重构版本
 */
@Composable
fun FilterSummary(
    selectedFilters: Set<CategoryFilter>,
    onClearFilter: (CategoryFilter) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedFilters.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth()
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    TextButton(onClick = onClearAll) {
                        Text(
                            text = "清空全部",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 显示选中的筛选条件
                selectedFilters.forEach { filter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextButton(onClick = { onClearFilter(filter) }) {
                            Text(
                                text = "移除",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
