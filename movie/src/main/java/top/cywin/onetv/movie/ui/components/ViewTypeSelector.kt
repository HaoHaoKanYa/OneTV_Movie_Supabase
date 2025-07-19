package top.cywin.onetv.movie.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.cywin.onetv.movie.ui.base.MovieViewType

/**
 * ViewType选择器组件
 * 允许用户在不同的视图类型之间切换
 */
@Composable
fun ViewTypeSelector(
    currentViewType: MovieViewType,
    onViewTypeChange: (MovieViewType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabels: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MovieViewType.values().forEach { viewType ->
                ViewTypeButton(
                    viewType = viewType,
                    isSelected = currentViewType == viewType,
                    onClick = { onViewTypeChange(viewType) },
                    enabled = enabled,
                    showLabel = showLabels
                )
            }
        }
    }
}

/**
 * 单个ViewType按钮
 */
@Composable
private fun ViewTypeButton(
    viewType: MovieViewType,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    showLabel: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "BackgroundColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "ContentColor"
    )
    
    val icon = getViewTypeIcon(viewType)
    val label = getViewTypeLabel(viewType)
    
    if (showLabel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .selectable(
                    selected = isSelected,
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.RadioButton
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor
            )
        }
    }
}

/**
 * 获取ViewType对应的图标
 */
private fun getViewTypeIcon(viewType: MovieViewType): ImageVector = when(viewType) {
    MovieViewType.RECT -> Icons.Default.GridView
    MovieViewType.OVAL -> Icons.Default.AccountCircle
    MovieViewType.LIST -> Icons.Default.List
    MovieViewType.GRID -> Icons.Default.Apps
}

/**
 * 获取ViewType对应的标签
 */
private fun getViewTypeLabel(viewType: MovieViewType): String = when(viewType) {
    MovieViewType.RECT -> "海报"
    MovieViewType.OVAL -> "头像"
    MovieViewType.LIST -> "列表"
    MovieViewType.GRID -> "网格"
}

/**
 * 紧凑型ViewType选择器 - 用于工具栏
 */
@Composable
fun CompactViewTypeSelector(
    currentViewType: MovieViewType,
    onViewTypeChange: (MovieViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            // 循环切换ViewType
            val nextViewType = when(currentViewType) {
                MovieViewType.RECT -> MovieViewType.OVAL
                MovieViewType.OVAL -> MovieViewType.LIST
                MovieViewType.LIST -> MovieViewType.GRID
                MovieViewType.GRID -> MovieViewType.RECT
            }
            onViewTypeChange(nextViewType)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = getViewTypeIcon(currentViewType),
            contentDescription = "切换视图: ${getViewTypeLabel(currentViewType)}"
        )
    }
}

/**
 * 下拉菜单式ViewType选择器
 */
@Composable
fun DropdownViewTypeSelector(
    currentViewType: MovieViewType,
    onViewTypeChange: (MovieViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = getViewTypeIcon(currentViewType),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(getViewTypeLabel(currentViewType))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MovieViewType.values().forEach { viewType ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getViewTypeIcon(viewType),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(getViewTypeLabel(viewType))
                        }
                    },
                    onClick = {
                        onViewTypeChange(viewType)
                        expanded = false
                    },
                    leadingIcon = if (currentViewType == viewType) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}
