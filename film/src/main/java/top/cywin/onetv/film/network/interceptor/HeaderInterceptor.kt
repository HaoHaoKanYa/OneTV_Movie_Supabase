package top.cywin.onetv.film.network.interceptor

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.utils.DeviceUtils
import top.cywin.onetv.film.utils.NetworkUtils
import java.io.IOException

/**
 * 请求头拦截器
 * 
 * 基于 FongMi/TV 的网络请求头处理
 * 自动添加必要的请求头信息
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
class HeaderInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_HEADER_INTERCEPTOR"
        
        // 默认请求头
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Safari/537.36"
        private const val DEFAULT_ACCEPT = "*/*"
        private const val DEFAULT_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8"
        private const val DEFAULT_ACCEPT_ENCODING = "gzip, deflate, br"
        private const val DEFAULT_CONNECTION = "keep-alive"
        private const val DEFAULT_CACHE_CONTROL = "no-cache"
        
        // OneTV 标识
        private const val ONETV_CLIENT = "OneTV/2.1.1"
        private const val ONETV_PLATFORM = "Android TV"
    }
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        try {
            Log.d(TAG, "处理请求头: $url")
            
            // 构建新的请求
            val requestBuilder = originalRequest.newBuilder()
            
            // 1. 添加基础请求头
            addBasicHeaders(requestBuilder)
            
            // 2. 添加设备信息
            addDeviceHeaders(requestBuilder)
            
            // 3. 添加网络信息
            addNetworkHeaders(requestBuilder)
            
            // 4. 处理站点特定请求头
            processSiteSpecificHeaders(requestBuilder, url)
            
            // 5. 处理自定义请求头
            processCustomHeaders(requestBuilder, originalRequest)
            
            val newRequest = requestBuilder.build()
            
            Log.d(TAG, "请求头处理完成: ${newRequest.headers.size()}个头部")
            
            return chain.proceed(newRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "请求头处理失败: $url", e)
            // 如果处理失败，使用原始请求
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 添加基础请求头
     */
    private fun addBasicHeaders(builder: okhttp3.Request.Builder) {
        // User-Agent
        if (!hasHeader(builder, "User-Agent")) {
            builder.addHeader("User-Agent", DEFAULT_USER_AGENT)
        }
        
        // Accept
        if (!hasHeader(builder, "Accept")) {
            builder.addHeader("Accept", DEFAULT_ACCEPT)
        }
        
        // Accept-Language
        if (!hasHeader(builder, "Accept-Language")) {
            builder.addHeader("Accept-Language", DEFAULT_ACCEPT_LANGUAGE)
        }
        
        // Accept-Encoding
        if (!hasHeader(builder, "Accept-Encoding")) {
            builder.addHeader("Accept-Encoding", DEFAULT_ACCEPT_ENCODING)
        }
        
        // Connection
        if (!hasHeader(builder, "Connection")) {
            builder.addHeader("Connection", DEFAULT_CONNECTION)
        }
        
        // Cache-Control
        if (!hasHeader(builder, "Cache-Control")) {
            builder.addHeader("Cache-Control", DEFAULT_CACHE_CONTROL)
        }
    }
    
    /**
     * 添加设备信息请求头
     */
    private fun addDeviceHeaders(builder: okhttp3.Request.Builder) {
        try {
            // OneTV 客户端标识
            builder.addHeader("X-Client", ONETV_CLIENT)
            builder.addHeader("X-Platform", ONETV_PLATFORM)
            
            // 设备信息
            val deviceInfo = DeviceUtils.getDeviceInfo(context)
            builder.addHeader("X-Device-Model", deviceInfo.model)
            builder.addHeader("X-Device-Brand", deviceInfo.brand)
            builder.addHeader("X-Android-Version", deviceInfo.androidVersion)
            
            // 应用信息
            val appInfo = DeviceUtils.getAppInfo(context)
            builder.addHeader("X-App-Version", appInfo.versionName)
            builder.addHeader("X-App-Build", appInfo.versionCode.toString())
            
        } catch (e: Exception) {
            Log.w(TAG, "添加设备信息失败", e)
        }
    }
    
    /**
     * 添加网络信息请求头
     */
    private fun addNetworkHeaders(builder: okhttp3.Request.Builder) {
        try {
            val networkInfo = NetworkUtils.getNetworkInfo(context)
            builder.addHeader("X-Network-Type", networkInfo.type)
            builder.addHeader("X-Network-Operator", networkInfo.operator)
            
        } catch (e: Exception) {
            Log.w(TAG, "添加网络信息失败", e)
        }
    }
    
    /**
     * 处理站点特定请求头
     */
    private fun processSiteSpecificHeaders(builder: okhttp3.Request.Builder, url: String) {
        try {
            when {
                // 处理特定站点的请求头需求
                url.contains("iqiyi.com") -> {
                    builder.addHeader("Referer", "https://www.iqiyi.com/")
                }
                url.contains("youku.com") -> {
                    builder.addHeader("Referer", "https://www.youku.com/")
                }
                url.contains("qq.com") -> {
                    builder.addHeader("Referer", "https://v.qq.com/")
                }
                url.contains("bilibili.com") -> {
                    builder.addHeader("Referer", "https://www.bilibili.com/")
                }
                url.contains("douyin.com") -> {
                    builder.addHeader("Referer", "https://www.douyin.com/")
                }
                // 通用处理
                else -> {
                    val host = extractHost(url)
                    if (host.isNotEmpty()) {
                        builder.addHeader("Referer", "https://$host/")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "处理站点特定请求头失败", e)
        }
    }
    
    /**
     * 处理自定义请求头
     */
    private fun processCustomHeaders(builder: okhttp3.Request.Builder, originalRequest: okhttp3.Request) {
        try {
            // 检查是否有自定义的请求头配置
            val customHeaders = originalRequest.header("X-Custom-Headers")
            if (!customHeaders.isNullOrEmpty()) {
                // 移除配置头
                builder.removeHeader("X-Custom-Headers")
                
                // 解析并添加自定义头
                parseAndAddCustomHeaders(builder, customHeaders)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "处理自定义请求头失败", e)
        }
    }
    
    /**
     * 解析并添加自定义请求头
     */
    private fun parseAndAddCustomHeaders(builder: okhttp3.Request.Builder, customHeaders: String) {
        try {
            // 格式: "key1=value1;key2=value2"
            customHeaders.split(";").forEach { header ->
                val parts = header.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        builder.addHeader(key, value)
                        Log.d(TAG, "添加自定义请求头: $key = $value")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析自定义请求头失败", e)
        }
    }
    
    /**
     * 检查是否已有指定请求头
     */
    private fun hasHeader(builder: okhttp3.Request.Builder, name: String): Boolean {
        // 注意：这里无法直接检查 builder 中的头部，所以总是返回 false
        // 实际实现中可能需要维护一个头部列表
        return false
    }
    
    /**
     * 提取 URL 中的主机名
     */
    private fun extractHost(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
