package top.cywin.onetv.tv.ui.screens.channelurl.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import top.cywin.onetv.core.data.utils.ChannelUtil
import top.cywin.onetv.core.util.utils.isIPv6
import top.cywin.onetv.tv.ui.material.Tag
import top.cywin.onetv.tv.ui.theme.MyTVTheme
import top.cywin.onetv.tv.ui.utils.focusOnLaunchedSaveable
import top.cywin.onetv.tv.ui.utils.handleKeyEvents
import top.cywin.onetv.tv.ui.utils.ifElse
import java.io.IOException
import kotlin.system.measureTimeMillis

@Composable
fun ChannelUrlItem(
    modifier: Modifier = Modifier,
    urlProvider: () -> String = { "" },
    urlIdxProvider: () -> Int = { 0 },
    isSelectedProvider: () -> Boolean = { false },
    onSelected: () -> Unit = {},
) {
    val url = urlProvider()
    val urlIdx = urlIdxProvider()
    val isSelected = isSelectedProvider()

    val urlDelay = rememberUrlDelay(url)

    ListItem(
        modifier = modifier
            .ifElse(isSelected, Modifier.focusOnLaunchedSaveable())
            .handleKeyEvents(onSelect = onSelected),
        selected = false,
        onClick = {},
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), // 比列表背景(0.5f)更深的透明度
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), // 比列表背景(0.5f)更深的透明度
        ),
        headlineContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("线路${urlIdx + 1}")

                if (ChannelUtil.isHybridWebViewUrl(url)) {
                    Tag("混合")
                    Tag(ChannelUtil.getHybridWebViewUrlProvider(url))
                } else {
                    if (ChannelUtil.urlSupportPlayback(url)) Tag("回放")
                    Tag(if (url.isIPv6()) "IPV6" else "IPV4")
                    if (urlDelay != 0L) Tag("$urlDelay ms")
                }
            }
        },
        supportingContent = null, // 隐藏URL显示
        trailingContent = {
            RadioButton(selected = isSelected, onClick = {})
        },
    )
}

@Composable
private fun rememberUrlDelay(url: String): Long {
    var elapsedTime by remember { mutableLongStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()

                elapsedTime = measureTimeMillis {
                    try {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        }
                    } catch (_: IOException) {
                        hasError = true
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    return if (hasError) 0 else elapsedTime
}

@Preview
@Composable
private fun ChannelUrlItemPreview() {
    MyTVTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ChannelUrlItem(
                urlProvider = { "http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226231/index.m3u8" },
                urlIdxProvider = { 0 },
                isSelectedProvider = { true },
            )

            ChannelUrlItem(
                urlProvider = { "http://[2409:8087:5e01:34::20]:6610/ZTE_CMS/00000001000000060000000000000131/index.m3u8?IAS" },
                urlIdxProvider = { 0 },
            )

            ChannelUrlItem(
                urlProvider = { ChannelUtil.getHybridWebViewUrl("cctv1")!!.first() },
                urlIdxProvider = { 0 },
                isSelectedProvider = { true },
            )
        }
    }
}