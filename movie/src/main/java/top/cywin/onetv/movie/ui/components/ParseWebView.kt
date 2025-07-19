package top.cywin.onetv.movie.ui.components

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import top.cywin.onetv.movie.event.WebViewParseRequest
import top.cywin.onetv.movie.utils.Sniffer

/**
 * Compose WebView适配器 - 用于FongMi_TV的WebView解析
 * 只负责UI显示，使用FongMi_TV的解析逻辑
 */
@Composable
fun ParseWebView(
    request: WebViewParseRequest,
    onParseSuccess: (String) -> Unit,
    onParseError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(request.url) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "解析中...",
                style = MaterialTheme.typography.titleMedium
            )
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("取消")
            }
        }
        
        // 加载指示器
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在加载页面...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // URL显示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "URL: $currentUrl",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            webRequest: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = webRequest?.url?.toString() ?: return null
                            
                            // ✅ 使用FongMi_TV的Sniffer检测视频链接
                            if (Sniffer.isVideoFormat(url)) {
                                // ✅ 通过原有的ParseCallback回调
                                request.callback?.onParseSuccess(
                                    webRequest.requestHeaders ?: emptyMap(),
                                    url
                                )
                                onParseSuccess(url)
                                return null
                            }
                            
                            return super.shouldInterceptRequest(view, webRequest)
                        }
                        
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            currentUrl = url ?: request.url
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // 执行点击操作
                            if (request.click.isNotEmpty()) {
                                view?.evaluateJavascript(request.click, null)
                            }
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            errorRequest: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, errorRequest, error)
                            isLoading = false
                            
                            // ✅ 通过原有的ParseCallback回调
                            request.callback?.onParseError()
                            onParseError("WebView加载错误: ${error?.description}")
                        }
                    }
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        // 设置User-Agent
                        if (request.headers.containsKey("User-Agent")) {
                            userAgentString = request.headers["User-Agent"]
                        }
                    }
                    
                    // 设置请求头并加载URL
                    loadUrl(request.url, request.headers)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

/**
 * WebView解析对话框
 */
@Composable
fun ParseWebViewDialog(
    request: WebViewParseRequest,
    onParseSuccess: (String) -> Unit,
    onParseError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("视频解析")
        },
        text = {
            ParseWebView(
                request = request,
                onParseSuccess = onParseSuccess,
                onParseError = onParseError,
                onDismiss = onDismiss
            )
        },
        confirmButton = {},
        dismissButton = {},
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.8f)
    )
}
