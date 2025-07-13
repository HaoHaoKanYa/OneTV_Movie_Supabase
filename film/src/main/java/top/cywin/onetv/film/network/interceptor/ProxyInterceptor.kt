package top.cywin.onetv.film.network.interceptor

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.proxy.ProxyManager
import top.cywin.onetv.film.hook.HookManager
import top.cywin.onetv.film.utils.NetworkUtils
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 代理拦截器
 *
 * 基于 FongMi/TV 的代理处理机制
 * 与本地代理服务器集成，支持智能路由
 *
 * @author OneTV Team
 * @since 2025-07-13
 */
class ProxyInterceptor(
    private val context: Context,
    private val proxyManager: ProxyManager,
    private val hookManager: HookManager
) : Interceptor {

    companion object {
        private const val TAG = "ONETV_PROXY_INTERCEPTOR"
        private const val LOCAL_PROXY_HOST = "127.0.0.1"
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        try {
            Log.d(TAG, "处理代理请求: $url")

            // 1. 检查是否需要使用本地代理
            if (!shouldUseLocalProxy(url)) {
                Log.d(TAG, "直接连接: $url")
                return chain.proceed(originalRequest)
            }

            // 2. 获取本地代理端口
            val proxyPort = getLocalProxyPort(url)
            if (proxyPort == null) {
                Log.d(TAG, "无可用本地代理，直接连接: $url")
                return chain.proceed(originalRequest)
            }

            // 3. 通过本地代理连接
            return connectThroughLocalProxy(chain, originalRequest, proxyPort)

        } catch (e: Exception) {
            Log.e(TAG, "代理处理失败: $url", e)
            // 代理失败时尝试直接连接
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 判断是否需要使用本地代理
     */
    private fun shouldUseLocalProxy(url: String): Boolean {
        return try {
            // 1. 检查代理管理器是否运行
            if (!proxyManager.isRunning()) {
                return false
            }

            // 2. 检查是否有可用的代理服务器
            val status = proxyManager.getStatus()
            if (status.runningServers == 0) {
                return false
            }

            // 3. 检查 URL 是否需要特殊处理
            if (needsProxyProcessing(url)) {
                return true
            }

            // 4. 检查网络连通性
            if (NetworkUtils.isDirectlyAccessible(url)) {
                return false
            }

            // 5. 默认使用本地代理
            true

        } catch (e: Exception) {
            Log.w(TAG, "判断本地代理需求失败", e)
            false
        }
    }

    /**
     * 检查 URL 是否需要代理处理
     */
    private fun needsProxyProcessing(url: String): Boolean {
        return url.contains(".m3u8") ||
               url.contains(".ts") ||
               url.contains("player") ||
               url.contains("parse") ||
               hookManager.needsProcessing(url)
    }
    
    /**
     * 获取本地代理端口
     */
    private fun getLocalProxyPort(url: String): Int? {
        return try {
            val status = proxyManager.getStatus()

            // 根据 URL 类型选择合适的代理服务器
            when {
                url.contains("player") || url.contains(".m3u8") || url.contains(".ts") -> {
                    // 播放器相关请求使用播放器代理
                    status.serverPorts["default_player"]
                }
                else -> {
                    // 其他请求使用 HTTP 代理
                    status.serverPorts["default_http"]
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取本地代理端口失败", e)
            null
        }
    }
    
    /**
     * 通过本地代理连接
     */
    private fun connectThroughLocalProxy(
        chain: Interceptor.Chain,
        request: okhttp3.Request,
        proxyPort: Int
    ): Response {
        return try {
            Log.d(TAG, "通过本地代理连接: $LOCAL_PROXY_HOST:$proxyPort")

            // 创建新的请求构建器
            val requestBuilder = request.newBuilder()

            // 添加本地代理标识头部
            addLocalProxyHeaders(requestBuilder, proxyPort)

            val newRequest = requestBuilder.build()

            // 执行请求（OkHttp 会自动使用系统代理设置）
            val response = chain.proceed(newRequest)

            Log.d(TAG, "本地代理连接成功: ${response.code}")
            response

        } catch (e: Exception) {
            Log.e(TAG, "本地代理连接失败: $LOCAL_PROXY_HOST:$proxyPort", e)
            throw IOException("本地代理连接失败", e)
        }
    }
    
    /**
     * 添加本地代理相关请求头
     */
    private fun addLocalProxyHeaders(builder: okhttp3.Request.Builder, proxyPort: Int) {
        try {
            // 添加本地代理标识
            builder.addHeader("X-Local-Proxy-Used", "$LOCAL_PROXY_HOST:$proxyPort")
            builder.addHeader("X-Proxy-Type", "LOCAL")
            builder.addHeader("X-OneTV-Client", "Film-Module")

            // 添加时间戳用于调试
            builder.addHeader("X-Request-Time", System.currentTimeMillis().toString())

        } catch (e: Exception) {
            Log.w(TAG, "添加本地代理请求头失败", e)
        }
    }

}
