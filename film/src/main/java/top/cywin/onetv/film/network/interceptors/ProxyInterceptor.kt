package top.cywin.onetv.film.network.interceptors

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import top.cywin.onetv.film.proxy.ProxyRuleManager
import top.cywin.onetv.film.proxy.ProxyType

/**
 * 代理拦截器
 * 
 * 基于 FongMi/TV 的代理处理机制实现
 * 负责根据代理规则处理网络请求
 * 
 * 核心功能：
 * - 代理规则匹配
 * - 请求重定向
 * - URL 重写
 * - 请求阻止
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
class ProxyInterceptor(
    private val proxyRuleManager: ProxyRuleManager
) : Interceptor {
    
    companion object {
        private const val TAG = "ONETV_FILM_PROXY_INTERCEPTOR"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url.toString()
        
        try {
            Log.d(TAG, "🔍 检查代理规则: $originalUrl")
            
            // 1. 查找匹配的代理规则
            val matchingRule = proxyRuleManager.findMatchingRule(originalUrl)
            
            if (matchingRule == null) {
                Log.d(TAG, "📝 无匹配的代理规则，直接处理")
                return chain.proceed(originalRequest)
            }
            
            Log.d(TAG, "✅ 找到匹配规则: ${matchingRule.name}")
            
            // 2. 根据代理类型处理请求
            return when (matchingRule.proxyType) {
                ProxyType.REDIRECT -> handleRedirect(chain, originalRequest, matchingRule)
                ProxyType.REPLACE -> handleReplace(chain, originalRequest, matchingRule)
                ProxyType.PROXY -> handleProxy(chain, originalRequest, matchingRule)
                ProxyType.BLOCK -> handleBlock(originalRequest, matchingRule)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理处理失败: $originalUrl", e)
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 处理重定向
     */
    private fun handleRedirect(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        rule: top.cywin.onetv.film.proxy.ProxyRule
    ): Response {
        try {
            Log.d(TAG, "🔄 执行重定向: ${originalRequest.url} -> ${rule.target}")
            
            // 应用代理规则
            val modifiedRequest = rule.apply(originalRequest)
            
            return chain.proceed(modifiedRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 重定向处理失败", e)
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 处理 URL 替换
     */
    private fun handleReplace(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        rule: top.cywin.onetv.film.proxy.ProxyRule
    ): Response {
        try {
            Log.d(TAG, "🔧 执行 URL 替换: ${rule.name}")
            
            // 应用代理规则
            val modifiedRequest = rule.apply(originalRequest)
            
            return chain.proceed(modifiedRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 替换处理失败", e)
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 处理代理服务器
     */
    private fun handleProxy(
        chain: Interceptor.Chain,
        originalRequest: okhttp3.Request,
        rule: top.cywin.onetv.film.proxy.ProxyRule
    ): Response {
        try {
            Log.d(TAG, "🌐 通过代理服务器: ${rule.target}")
            
            // 添加代理相关的请求头
            val modifiedRequest = rule.apply(originalRequest)
            
            // 注意：实际的代理服务器配置需要在 OkHttpClient 级别设置
            // 这里主要是添加代理相关的请求头和标识
            
            return chain.proceed(modifiedRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 代理服务器处理失败", e)
            return chain.proceed(originalRequest)
        }
    }
    
    /**
     * 处理请求阻止
     */
    private fun handleBlock(
        originalRequest: okhttp3.Request,
        rule: top.cywin.onetv.film.proxy.ProxyRule
    ): Response {
        Log.d(TAG, "🚫 阻止请求: ${originalRequest.url}")
        
        // 创建一个阻止响应
        return createBlockedResponse(originalRequest, rule)
    }
    
    /**
     * 创建阻止响应
     */
    private fun createBlockedResponse(
        request: okhttp3.Request,
        rule: top.cywin.onetv.film.proxy.ProxyRule
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(204) // No Content
            .message("Blocked by proxy rule: ${rule.name}")
            .body(okhttp3.ResponseBody.create(null, ""))
            .build()
    }
}
