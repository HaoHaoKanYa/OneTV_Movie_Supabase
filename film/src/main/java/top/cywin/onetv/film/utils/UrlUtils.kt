package top.cywin.onetv.film.utils

import android.util.Log
import java.net.URI
import java.net.URLEncoder
import java.net.URLDecoder

/**
 * URL 处理工具类
 * 
 * 基于 FongMi/TV 的 URL 工具实现
 * 提供完整的 URL 处理功能
 * 
 * 功能：
 * - URL 解析和构建
 * - 相对路径解析
 * - 参数处理
 * - 编码解码
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
object UrlUtils {
    
    private const val TAG = "ONETV_FILM_URL_UTILS"
    
    /**
     * 🔗 解析相对 URL
     */
    fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            if (relativeUrl.isEmpty()) return baseUrl
            if (isAbsoluteUrl(relativeUrl)) return relativeUrl
            
            val base = URI(baseUrl)
            val resolved = base.resolve(relativeUrl)
            
            Log.d(TAG, "🔗 URL 解析: $baseUrl + $relativeUrl = ${resolved}")
            resolved.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 解析失败: base=$baseUrl, relative=$relativeUrl", e)
            if (relativeUrl.startsWith("/")) {
                extractDomain(baseUrl) + relativeUrl
            } else {
                "$baseUrl/$relativeUrl"
            }
        }
    }
    
    /**
     * 🔗 构建 URL
     */
    fun buildUrl(baseUrl: String, path: String): String {
        return try {
            val cleanBase = baseUrl.trimEnd('/')
            val cleanPath = path.trimStart('/')
            
            val result = if (path.startsWith("http")) {
                path
            } else {
                "$cleanBase/$cleanPath"
            }
            
            Log.d(TAG, "🔗 URL 构建: $baseUrl + $path = $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 构建失败: base=$baseUrl, path=$path", e)
            "$baseUrl/$path"
        }
    }
    
    /**
     * 🌐 提取域名
     */
    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: return url
            val port = if (uri.port != -1) ":${uri.port}" else ""
            
            val domain = "$scheme://$host$port"
            Log.d(TAG, "🌐 域名提取: $url -> $domain")
            domain
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 域名提取失败: $url", e)
            url.substringBefore("/", url)
        }
    }
    
    /**
     * 🏠 提取主机名
     */
    fun extractHost(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
            
            Log.d(TAG, "🏠 主机名提取: $url -> $host")
            host
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 主机名提取失败: $url", e)
            url.substringAfter("://").substringBefore("/").substringBefore(":")
        }
    }
    
    /**
     * 🛤️ 提取路径
     */
    fun extractPath(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: "/"
            
            Log.d(TAG, "🛤️ 路径提取: $url -> $path")
            path
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 路径提取失败: $url", e)
            val afterHost = url.substringAfter("://").substringAfter("/")
            if (afterHost == url) "/" else "/$afterHost"
        }
    }
    
    /**
     * ❓ 提取查询参数
     */
    fun extractQuery(url: String): String {
        return try {
            val uri = URI(url)
            val query = uri.query ?: ""
            
            Log.d(TAG, "❓ 查询参数提取: $url -> $query")
            query
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 查询参数提取失败: $url", e)
            url.substringAfter("?", "")
        }
    }
    
    /**
     * 📋 解析查询参数
     */
    fun parseQueryParams(url: String): Map<String, String> {
        return try {
            val query = extractQuery(url)
            if (query.isEmpty()) return emptyMap()
            
            val params = mutableMapOf<String, String>()
            query.split("&").forEach { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], "UTF-8")
                    val value = URLDecoder.decode(parts[1], "UTF-8")
                    params[key] = value
                }
            }
            
            Log.d(TAG, "📋 查询参数解析: $url -> ${params.size} 个参数")
            params
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 查询参数解析失败: $url", e)
            emptyMap()
        }
    }
    
    /**
     * ➕ 添加查询参数
     */
    fun addParams(url: String, params: Map<String, String>): String {
        return try {
            if (params.isEmpty()) return url
            
            val separator = if (url.contains("?")) "&" else "?"
            val paramString = params.map { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }.joinToString("&")
            
            val result = "$url$separator$paramString"
            Log.d(TAG, "➕ 参数添加: $url + ${params.size} 个参数 = $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 参数添加失败: $url", e)
            url
        }
    }
    
    /**
     * ➕ 添加单个查询参数
     */
    fun addParam(url: String, key: String, value: String): String {
        return addParams(url, mapOf(key to value))
    }
    
    /**
     * 🔍 检查是否为绝对 URL
     */
    fun isAbsoluteUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val isAbsolute = uri.isAbsolute
            
            Log.d(TAG, "🔍 绝对 URL 检查: $url -> $isAbsolute")
            isAbsolute
            
        } catch (e: Exception) {
            Log.d(TAG, "🔍 绝对 URL 检查失败，判断为相对 URL: $url")
            false
        }
    }
    
    /**
     * 🔍 检查是否为有效 URL
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val isValid = uri.scheme != null && uri.host != null
            
            Log.d(TAG, "🔍 URL 有效性检查: $url -> $isValid")
            isValid
            
        } catch (e: Exception) {
            Log.d(TAG, "🔍 URL 有效性检查失败: $url")
            false
        }
    }
    
    /**
     * 🔒 检查是否为 HTTPS
     */
    fun isHttps(url: String): Boolean {
        return try {
            val uri = URI(url)
            val isHttps = "https".equals(uri.scheme, ignoreCase = true)
            
            Log.d(TAG, "🔒 HTTPS 检查: $url -> $isHttps")
            isHttps
            
        } catch (e: Exception) {
            Log.d(TAG, "🔒 HTTPS 检查失败: $url")
            false
        }
    }
    
    /**
     * 🔄 转换为 HTTPS
     */
    fun toHttps(url: String): String {
        return try {
            if (isHttps(url)) return url
            
            val result = url.replaceFirst("http://", "https://")
            Log.d(TAG, "🔄 转换为 HTTPS: $url -> $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTPS 转换失败: $url", e)
            url
        }
    }
    
    /**
     * 🧹 清理 URL
     */
    fun cleanUrl(url: String): String {
        return try {
            val cleaned = url.trim()
                .replace("\\", "/")
                .replace("//+".toRegex(), "//")
                .let { if (it.startsWith("//")) "http:$it" else it }
            
            Log.d(TAG, "🧹 URL 清理: $url -> $cleaned")
            cleaned
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ URL 清理失败: $url", e)
            url
        }
    }
    
    /**
     * 📏 获取文件扩展名
     */
    fun getFileExtension(url: String): String {
        return try {
            val path = extractPath(url)
            val fileName = path.substringAfterLast("/")
            val extension = fileName.substringAfterLast(".", "")
            
            Log.d(TAG, "📏 文件扩展名: $url -> $extension")
            extension
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 文件扩展名获取失败: $url", e)
            ""
        }
    }
    
    /**
     * 📄 获取文件名
     */
    fun getFileName(url: String): String {
        return try {
            val path = extractPath(url)
            val fileName = path.substringAfterLast("/")
            
            Log.d(TAG, "📄 文件名: $url -> $fileName")
            fileName
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 文件名获取失败: $url", e)
            ""
        }
    }
    
    /**
     * 🔗 连接 URL 路径
     */
    fun joinPaths(vararg paths: String): String {
        return try {
            val result = paths.filter { it.isNotEmpty() }
                .joinToString("/") { it.trim('/') }
                .replace("//+".toRegex(), "/")
            
            Log.d(TAG, "🔗 路径连接: ${paths.joinToString(" + ")} = $result")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 路径连接失败: ${paths.joinToString(", ")}", e)
            paths.joinToString("/")
        }
    }
}
