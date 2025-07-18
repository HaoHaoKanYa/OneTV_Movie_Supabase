package top.cywin.onetv.tv.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.cywin.onetv.core.data.entities.channel.ChannelGroupList
import top.cywin.onetv.tv.ui.material.CircularProgressIndicator
import top.cywin.onetv.tv.ui.material.Visible
import top.cywin.onetv.tv.ui.rememberChildPadding
import top.cywin.onetv.tv.ui.screens.main.components.MainContent
import top.cywin.onetv.tv.ui.screens.quickop.components.QuickOpBtnList
import top.cywin.onetv.tv.ui.screens.settings.SettingsViewModel
import top.cywin.onetv.tv.ui.theme.MyTVTheme
import top.cywin.onetv.tv.ui.tooling.PreviewWithLayoutGrids
import top.cywin.onetv.tv.ui.utils.captureBackKey
import top.cywin.onetv.tv.ui.utils.focusOnLaunched
import top.cywin.onetv.tv.ui.utils.handleKeyEvents
import androidx.compose.runtime.remember // 修复 1: 添加 remember 导入
import androidx.compose.runtime.setValue
import top.cywin.onetv.tv.ui.screens.settings.SettingsScreen


@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToMovie: () -> Unit = {} // 新增：点播导航回调
) {
    val uiState by viewModel.uiState.collectAsState()

    //【步骤1】启动时自动进行缓存清理（自动清理仅对VIP用户生效，普通用户跳过）
    LaunchedEffect(Unit) {
        viewModel.clearAllCache(clearUserCache = false)
    }

    when (val s = uiState) {
        is MainUiState.Ready -> MainContent(
            modifier = modifier,
            channelGroupListProvider = { s.channelGroupList },
            filteredChannelGroupListProvider = {
                ChannelGroupList(s.channelGroupList.filter { it.name !in settingsViewModel.iptvChannelGroupHiddenList })
            },
            epgListProvider = { s.epgList },
            onBackPressed = onBackPressed,
            onNavigateToMovie = onNavigateToMovie, // 新增：传递点播导航回调
        )

        is MainUiState.Loading -> MainScreenSettingsWrapper(onBackPressed = onBackPressed) {
            MainScreenLoading(messageProvider = { s.message })
        }

        is MainUiState.Error -> MainScreenSettingsWrapper(onBackPressed = onBackPressed) {
            MainScreenError(messageProvider = { s.message })
        }
    }

    // 已删除 QuickOpBtnList 代码块
}

@Composable
fun MainScreenMessage(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    messageProvider: () -> String? = { null }
) {
    val childPadding = rememberChildPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = childPadding.start, bottom = childPadding.bottom),
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleLarge
            ) {
                title()
            }

            val message = messageProvider()
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(0.8f),
                    modifier = Modifier.sizeIn(maxWidth = 556.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MainScreenLoading(
    modifier: Modifier = Modifier,
    messageProvider: () -> String? = { null }
) {
    MainScreenMessage(
        modifier = modifier,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("加载中")
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 3.dp,
                )
            }
        },
        messageProvider = messageProvider
    )
}

@Preview(device = "spec:width=1280dp,height=720dp,dpi=213,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun MainScreenLoadingPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            MainScreenLoading(
                messageProvider = { "获取远程直播源(4/10)".repeat(10) }
            )
        }
    }
}

@Composable
private fun MainScreenError(
    modifier: Modifier = Modifier,
    messageProvider: () -> String? = { null }
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.error
    ) {
        MainScreenMessage(
            modifier = modifier,
            title = { Text("加载失败") },
            messageProvider = messageProvider
        )
    }
}

@Preview(device = "spec:width=1280dp,height=720dp,dpi=213,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun MainScreenErrorPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            MainScreenError(
                messageProvider = {
                    "Caused by: androidx.media3.datasource.HttpDataSource\$HttpDataSourceException:" + " java.io.IOException: unexpected end of stream on com.onetv.okhttp.Address@2f10c24d"
                }
            )
        }
    }
}

@Composable
private fun MainScreenSettingsWrapper(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .focusOnLaunched()
            .focusable()
            .captureBackKey { onBackPressed() }
            .handleKeyEvents(
                onSettings = { showSettings = true },
                onLongSelect = { showSettings = true },
            ),
    ) {
        content()

        Visible({ showSettings }) { SettingsScreen() }
    }
}