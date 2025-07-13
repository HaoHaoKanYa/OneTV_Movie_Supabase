package top.cywin.onetv.film.proxy

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 代理连接处理器
 * 
 * 基于 FongMi/TV 的代理连接处理实现
 * 负责处理单个客户端连接的 HTTP 请求和响应
 * 
 * 功能：
 * - HTTP 请求解析
 * - 响应生成和发送
 * - 连接管理
 * - 错误处理
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ProxyConnection(
    private val connectionId: String,
    private val clientSocket: Socket,
    private val proxyServer: ProxyServer
) {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_CONNECTION"
        private const val BUFFER_SIZE = 8192
        private const val HTTP_VERSION = "HTTP/1.1"
    }
    
    private val isClosed = AtomicBoolean(false)
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var bufferedReader: BufferedReader? = null
    private var printWriter: PrintWriter? = null
    
    init {
        try {
            inputStream = clientSocket.getInputStream()
            outputStream = clientSocket.getOutputStream()
            bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            printWriter = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 初始化连接失败: $connectionId", e)
            close()
        }
    }
    
    /**
     * 🔗 处理连接
     */
    suspend fun handle() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔗 处理连接: $connectionId")
            
            while (!isClosed.get() && !clientSocket.isClosed) {
                val request = parseHttpRequest()
                if (request != null) {
                    val response = processRequest(request)
                    sendResponse(response)
                } else {
                    break
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 连接处理异常: $connectionId", e)
        } finally {
            close()
        }
    }
    
    /**
     * 📥 解析 HTTP 请求
     */
    private suspend fun parseHttpRequest(): ProxyRequest? = withContext(Dispatchers.IO) {
        try {
            val requestLine = bufferedReader?.readLine()
            if (requestLine.isNullOrEmpty()) {
                return@withContext null
            }
            
            Log.d(TAG, "📥 解析请求行: $requestLine")
            
            // 解析请求行：METHOD PATH HTTP/1.1
            val parts = requestLine.split(" ")
            if (parts.size < 3) {
                Log.w(TAG, "⚠️ 无效的请求行: $requestLine")
                return@withContext null
            }
            
            val method = parts[0]
            val path = parts[1]
            val version = parts[2]
            
            // 解析请求头
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (bufferedReader?.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                
                val colonIndex = line!!.indexOf(':')
                if (colonIndex > 0) {
                    val key = line!!.substring(0, colonIndex).trim()
                    val value = line!!.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }
            
            // 解析请求体
            val body = if (method.equals("POST", ignoreCase = true)) {
                val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val bodyChars = CharArray(contentLength)
                    bufferedReader?.read(bodyChars, 0, contentLength)
                    String(bodyChars)
                } else {
                    ""
                }
            } else {
                ""
            }
            
            // 解析 URL
            val url = parseUrl(path, headers)
            
            ProxyRequest(
                method = method,
                url = url,
                headers = headers,
                body = body
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析 HTTP 请求失败: $connectionId", e)
            null
        }
    }
    
    /**
     * 🔗 解析 URL
     */
    private fun parseUrl(path: String, headers: Map<String, String>): String {
        return try {
            when {
                // 代理请求：/proxy?url=...
                path.startsWith("/proxy") -> {
                    val queryStart = path.indexOf('?')
                    if (queryStart > 0) {
                        val query = path.substring(queryStart + 1)
                        val params = parseQueryParams(query)
                        URLDecoder.decode(params["url"] ?: "", "UTF-8")
                    } else {
                        ""
                    }
                }
                
                // 播放器请求：/player?url=...
                path.startsWith("/player") -> {
                    val queryStart = path.indexOf('?')
                    if (queryStart > 0) {
                        val query = path.substring(queryStart + 1)
                        val params = parseQueryParams(query)
                        URLDecoder.decode(params["url"] ?: "", "UTF-8")
                    } else {
                        ""
                    }
                }
                
                // 直接 URL
                path.startsWith("http") -> path
                
                // 相对路径
                else -> {
                    val host = headers["Host"] ?: "localhost"
                    "http://$host$path"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 解析失败: $path", e)
            path
        }
    }
    
    /**
     * 🔍 解析查询参数
     */
    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        query.split("&").forEach { param ->
            val equalIndex = param.indexOf('=')
            if (equalIndex > 0) {
                val key = param.substring(0, equalIndex)
                val value = param.substring(equalIndex + 1)
                params[key] = value
            }
        }
        
        return params
    }
    
    /**
     * 🚀 处理请求
     */
    private suspend fun processRequest(request: ProxyRequest): ProxyResponse {
        return try {
            Log.d(TAG, "🚀 处理请求: ${request.method} ${request.url}")
            
            when {
                // 代理请求
                request.url.startsWith("http") -> {
                    proxyServer.handleHttpRequest(request)
                }
                
                // 播放器请求
                request.url.contains("/player") -> {
                    val url = extractUrlFromPath(request.url)
                    proxyServer.handlePlayerRequest(url, request.headers)
                }
                
                // 资源请求
                request.url.startsWith("/") -> {
                    proxyServer.handleResourceRequest(request.url)
                }
                
                // 其他请求
                else -> {
                    ProxyResponse(
                        statusCode = 400,
                        body = "无效的请求: ${request.url}"
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 请求处理失败: $connectionId", e)
            ProxyResponse(
                statusCode = 500,
                body = "服务器内部错误: ${e.message}"
            )
        }
    }
    
    /**
     * 🔗 从路径提取 URL
     */
    private fun extractUrlFromPath(path: String): String {
        val queryStart = path.indexOf('?')
        if (queryStart > 0) {
            val query = path.substring(queryStart + 1)
            val params = parseQueryParams(query)
            return URLDecoder.decode(params["url"] ?: "", "UTF-8")
        }
        return ""
    }
    
    /**
     * 📤 发送响应
     */
    private suspend fun sendResponse(response: ProxyResponse) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 发送响应: ${response.statusCode}")
            
            // 发送状态行
            printWriter?.println("$HTTP_VERSION ${response.statusCode} ${getStatusText(response.statusCode)}")
            
            // 发送响应头
            response.headers.forEach { (key, value) ->
                printWriter?.println("$key: $value")
            }
            
            // 添加默认响应头
            if (!response.headers.containsKey("Content-Length")) {
                printWriter?.println("Content-Length: ${response.body.toByteArray(Charsets.UTF_8).size}")
            }
            
            if (!response.headers.containsKey("Content-Type")) {
                printWriter?.println("Content-Type: text/plain; charset=utf-8")
            }
            
            printWriter?.println("Connection: close")
            printWriter?.println() // 空行分隔头部和正文
            
            // 发送响应体
            if (response.body.isNotEmpty()) {
                outputStream?.write(response.body.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 发送响应失败: $connectionId", e)
        }
    }
    
    /**
     * 📋 获取状态文本
     */
    private fun getStatusText(statusCode: Int): String {
        return when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> "Unknown"
        }
    }
    
    /**
     * 🔒 关闭连接
     */
    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                Log.d(TAG, "🔒 关闭连接: $connectionId")
                
                bufferedReader?.close()
                printWriter?.close()
                inputStream?.close()
                outputStream?.close()
                clientSocket.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 关闭连接失败: $connectionId", e)
            }
        }
    }
    
    /**
     * 🔍 检查连接是否已关闭
     */
    fun isClosed(): Boolean {
        return isClosed.get() || clientSocket.isClosed
    }
}
