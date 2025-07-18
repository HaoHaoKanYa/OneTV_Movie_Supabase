package top.cywin.onetv.tv.ui.screens.quickop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import top.cywin.onetv.core.data.entities.channel.Channel
import top.cywin.onetv.core.data.entities.channel.ChannelList
import top.cywin.onetv.core.data.entities.epg.EpgList
import top.cywin.onetv.core.data.entities.epg.EpgList.Companion.recentProgramme
import top.cywin.onetv.core.data.entities.epg.EpgProgramme
import top.cywin.onetv.core.data.entities.iptvsource.IptvSourceList
import top.cywin.onetv.core.data.repositories.iptv.IptvRepository
import top.cywin.onetv.core.data.repositories.user.SessionManager
import top.cywin.onetv.core.data.repositories.user.SessionManager.getIptvRepository
import top.cywin.onetv.tv.ui.material.LocalPopupManager
import top.cywin.onetv.tv.ui.material.SimplePopup
import top.cywin.onetv.tv.ui.rememberChildPadding
import top.cywin.onetv.tv.ui.screens.channel.components.ChannelInfo
import top.cywin.onetv.tv.ui.screens.channel.components.ChannelNumber
import top.cywin.onetv.tv.ui.screens.components.rememberScreenAutoCloseState
import top.cywin.onetv.tv.ui.screens.datetime.components.DateTimeDetail
import top.cywin.onetv.tv.ui.screens.iptvsource.IptvSourceScreen
import top.cywin.onetv.tv.ui.screens.main.MainViewModel
import top.cywin.onetv.tv.ui.screens.quickop.components.QuickOpBtnList
import top.cywin.onetv.tv.ui.screens.settings.SettingsViewModel
import top.cywin.onetv.tv.ui.screens.videoplayer.player.VideoPlayer
import top.cywin.onetv.tv.ui.theme.MyTVTheme
import top.cywin.onetv.tv.ui.tooling.PreviewWithLayoutGrids
import top.cywin.onetv.tv.ui.utils.captureBackKey
import top.cywin.onetv.tv.ui.utils.handleKeyEvents
// 新增导入
import android.content.Context
import androidx.compose.ui.platform.LocalContext



@Composable
fun QuickOpScreen(
    modifier: Modifier = Modifier,
    currentChannelProvider: () -> Channel = { Channel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    currentChannelNumberProvider: () -> String = { "" },
    showChannelLogoProvider: () -> Boolean = { false },
    epgListProvider: () -> EpgList = { EpgList() },
    isInTimeShiftProvider: () -> Boolean = { false },
    currentPlaybackEpgProgrammeProvider: () -> EpgProgramme? = { null },
    videoPlayerMetadataProvider: () -> VideoPlayer.Metadata = { VideoPlayer.Metadata() },
    onShowEpg: () -> Unit = {},
    onShowChannelUrl: () -> Unit = {},
    onShowVideoPlayerController: () -> Unit = {},
    onShowVideoPlayerDisplayMode: () -> Unit = {},
    onShowMoreSettings: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)
    // 新增：获取上下文和 sessionId
    val context = LocalContext.current  // 确保已正确获取上下文
    Box(
        modifier = modifier
            .captureBackKey { onClose() }
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
    ) {
        QuickOpScreenTop(
            channelNumberProvider = {
                currentChannelNumberProvider().padStart(2, '0')
            },
        )

        QuickOpScreenBottom(
            currentChannelProvider = currentChannelProvider,
            currentChannelUrlIdxProvider = currentChannelUrlIdxProvider,
            showChannelLogoProvider = showChannelLogoProvider,
            epgListProvider = epgListProvider,
            isInTimeShiftProvider = isInTimeShiftProvider,
            currentPlaybackEpgProgrammeProvider = currentPlaybackEpgProgrammeProvider,
            videoPlayerMetadataProvider = videoPlayerMetadataProvider,
            onShowEpg = onShowEpg,
            onShowChannelUrl = onShowChannelUrl,
            onShowVideoPlayerController = onShowVideoPlayerController,
            onShowVideoPlayerDisplayMode = onShowVideoPlayerDisplayMode,
            onShowMoreSettings = onShowMoreSettings,
            onClearCache = onClearCache,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Composable
private fun QuickOpScreenTop(
    modifier: Modifier = Modifier,
    channelNumberProvider: () -> String = { "" },
) {
    val childPadding = rememberChildPadding()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(childPadding.paddingValues),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.titleLarge
        ) {
            QuickOpScreeIptvSource()
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChannelNumber(channelNumberProvider = channelNumberProvider)

            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                Spacer(
                    modifier = Modifier
                        .background(Color.White)
                        .width(2.dp)
                        .height(30.dp),
                )
            }

            DateTimeDetail()
        }
    }
}

@Composable
fun QuickOpScreeIptvSource(
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val currentIptvSource = settingsViewModel.iptvSourceCurrent

    var isIptvSourceScreenVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val popupManager = LocalPopupManager.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            while (true) {
                alpha.animateTo(0.2f, tween(durationMillis = 1000))
                alpha.animateTo(1f, tween(durationMillis = 1000))
            }
        } else {
            alpha.animateTo(1f)
        }
    }

    Surface(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
            .focusRequester(focusRequester)
            .handleKeyEvents(
                onSelect = {
                    popupManager.push(focusRequester, true)
                    isIptvSourceScreenVisible = true
                },
            )
            .alpha(alpha.value),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        onClick = {
            popupManager.push(focusRequester, true)
            isIptvSourceScreenVisible = true
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(currentIptvSource.name)
            if (isFocused) Icon(Icons.Default.SyncAlt, contentDescription = null)
        }
    }

    SimplePopup(
        visibleProvider = { isIptvSourceScreenVisible },
        onDismissRequest = { isIptvSourceScreenVisible = false },
    ) {
        IptvSourceScreen(
            iptvSourceListProvider = { settingsViewModel.iptvSourceList },
            currentIptvSourceProvider = { settingsViewModel.iptvSourceCurrent },
            onIptvSourceSelected = {
                isIptvSourceScreenVisible = false
                if (settingsViewModel.iptvSourceCurrent != it) {
                    settingsViewModel.iptvSourceCurrent = it
                    settingsViewModel.iptvLastChannelIdx = 0
                    settingsViewModel.iptvChannelGroupHiddenList = emptySet()
                    // 修改 clearCache 调用处：
                    coroutineScope.launch {

                        getIptvRepository(context, settingsViewModel.iptvSourceCurrent).clearCache()  // ✅ 明确传递参数
                        mainViewModel.init()
                    }
                    mainViewModel.init()
                }
            },
            onIptvSourceDeleted = {
                settingsViewModel.iptvSourceList =
                    IptvSourceList(settingsViewModel.iptvSourceList - it)
            },
        )
    }
}

@Composable
private fun QuickOpScreenBottom(
    modifier: Modifier = Modifier,
    currentChannelProvider: () -> Channel = { Channel() },
    currentChannelUrlIdxProvider: () -> Int = { 0 },
    showChannelLogoProvider: () -> Boolean = { false },
    epgListProvider: () -> EpgList = { EpgList() },
    isInTimeShiftProvider: () -> Boolean = { false },
    currentPlaybackEpgProgrammeProvider: () -> EpgProgramme? = { null },
    videoPlayerMetadataProvider: () -> VideoPlayer.Metadata = { VideoPlayer.Metadata() },
    onShowEpg: () -> Unit = {},
    onShowChannelUrl: () -> Unit = {},
    onShowVideoPlayerController: () -> Unit = {},
    onShowVideoPlayerDisplayMode: () -> Unit = {},
    onShowMoreSettings: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onUserAction: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = childPadding.bottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ChannelInfo(
                modifier = Modifier.padding(start = childPadding.start, end = childPadding.end),
                channelProvider = currentChannelProvider,
                channelUrlIdxProvider = currentChannelUrlIdxProvider,
                recentEpgProgrammeProvider = {
                    epgListProvider().recentProgramme(currentChannelProvider())
                },
                isInTimeShiftProvider = isInTimeShiftProvider,
                currentPlaybackEpgProgrammeProvider = currentPlaybackEpgProgrammeProvider,
                videoPlayerMetadataProvider = videoPlayerMetadataProvider,
                showChannelLogoProvider = showChannelLogoProvider,
            )

            QuickOpBtnList(
                onShowEpg = onShowEpg,
                onShowChannelUrl = onShowChannelUrl,
                onShowVideoPlayerController = onShowVideoPlayerController,
                onShowVideoPlayerDisplayMode = onShowVideoPlayerDisplayMode,
                onShowMoreSettings = onShowMoreSettings,
                onClearCache = onClearCache,
                onUserAction = onUserAction,
            )
        }
    }
}

@Preview(device = "spec:width=1280dp,height=720dp,dpi=213,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
private fun QuickOpScreenPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            QuickOpScreen(
                currentChannelProvider = { Channel.EXAMPLE },
                currentChannelNumberProvider = { "1" },
                epgListProvider = {
                    EpgList.example(ChannelList(listOf(Channel.EXAMPLE)))
                },
            )
        }
    }
}