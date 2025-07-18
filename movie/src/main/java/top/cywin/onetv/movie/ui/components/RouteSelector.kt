package top.cywin.onetv.movie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.cywin.onetv.movie.bean.VodRoute
import top.cywin.onetv.movie.MovieApp

/**
 * OneTV Movie线路选择器组件 - 按照FongMi_TV整合指南重构
 */
@Composable
fun RouteSelector(
    routes: List<VodRoute>,
    selectedRoute: VodRoute?,
    onRouteSelected: (VodRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ 通过MovieApp访问适配器系统
    val movieApp = MovieApp.getInstance()
    val repositoryAdapter = movieApp.repositoryAdapter

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(routes) { route ->
            RouteChip(
                route = route,
                isSelected = selectedRoute == route,
                onClick = { onRouteSelected(route) }
            )
        }
    }
}

@Composable
private fun RouteChip(
    route: VodRoute,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = route.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        selected = isSelected,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

@Composable
fun RouteDropdown(
    routes: List<VodRoute>,
    selectedRoute: VodRoute?,
    onRouteSelected: (VodRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedRoute?.name ?: "选择线路",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = { Text(route.name) },
                    onClick = {
                        onRouteSelected(route)
                        expanded = false
                    },
                    leadingIcon = if (selectedRoute == route) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    } else null
                )
            }
        }
    }
}
