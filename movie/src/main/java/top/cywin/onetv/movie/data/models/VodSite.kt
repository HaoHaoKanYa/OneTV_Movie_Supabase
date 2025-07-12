package top.cywin.onetv.movie.data.models

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive



/**
 * 点播站点 (参考OneMoVie Site，支持动态分类配置)
 */
@Serializable
data class VodSite(
    val key: String, // 站点唯一标识
    val name: String, // 站点名称
    val api: String, // API地址
    val ext: JsonElement = JsonPrimitive(""), // 扩展配置 (TVBOX标准支持字符串或JSON对象)
    val jar: String = "", // JAR包地址
    val type: Int = 1, // 站点类型: 0=spider, 1=cms, 3=app, 4=alist
    val searchable: Int = 1, // 是否可搜索
    val quickSearch: Int = 1, // 是否支持快速搜索 (TVBOX标准字段)
    val filterable: Int = 1, // 是否支持筛选 (TVBOX标准字段)
    val playerType: Int = 1, // 播放器类型 (TVBOX标准字段)
    val changeable: Int = 1, // 是否可切换 (TVBOX标准字段)
    val click: String = "", // 点击事件配置 (TVBOX扩展字段)
    val timeout: Int = 15000, // 超时时间 (15秒)
    val header: JsonElement? = null, // 请求头 (TVBOX标准JsonElement)
    val style: VodStyle? = null, // 显示样式配置
    val categories: List<String> = emptyList() // 分类列表 (TVBOX标准字符串数组)
) {
    /**
     * 是否可搜索
     */
    fun isSearchable(): Boolean = searchable == 1

    /**
     * 是否支持快速搜索
     */
    fun isQuickSearchable(): Boolean = quickSearch == 1

    /**
     * 是否支持筛选
     */
    fun isFilterable(): Boolean = filterable == 1

    /**
     * 是否可切换
     */
    fun isChangeable(): Boolean = changeable == 1

    /**
     * 获取扩展配置字符串 (TVBOX标准支持字符串或JSON对象)
     */
    fun getExtString(): String {
        return when (ext) {
            is JsonPrimitive -> ext.content
            else -> ext.toString()
        }
    }

    /**
     * 获取播放器类型名称
     */
    fun getPlayerTypeName(): String {
        return when (playerType) {
            0 -> "系统播放器"
            1 -> "IJK播放器"
            2 -> "EXO播放器"
            else -> "未知播放器"
        }
    }
    
    /**
     * 获取分类显示名称 (TVBOX标准字符串数组)
     */
    fun getCategoryName(categoryName: String): String {
        return if (categories.contains(categoryName)) categoryName else "未知分类"
    }
    
    /**
     * 获取所有可用分类 (TVBOX标准字符串数组)
     */
    fun getAvailableCategories(): List<String> {
        return categories.filter { it.isNotEmpty() }
    }
    
    /**
     * 是否为爬虫站点 (TVBOX智能识别机制)
     */
    fun isSpider(): Boolean {
        // 1. 标准Spider类型
        if (type == 0) return true

        // 2. TVBOX智能识别：根据API URL特征判断实际类型
        return isJavaScriptApi() || hasJarReference() || isSpiderKeyword()
    }

    /**
     * 检测是否为JavaScript API
     */
    private fun isJavaScriptApi(): Boolean {
        val jsPatterns = listOf(
            ".js", ".min.js", "drpy", "hipy", "spider.js",
            "libs/", "/js/", "javascript", "drpy2"
        )
        val isJs = jsPatterns.any { pattern ->
            api.lowercase().contains(pattern.lowercase())
        }

        if (isJs) {
            Log.d("ONETV_MOVIE", "🔧 TVBOX智能识别: 站点 $name (type=$type) 检测到JavaScript API，使用Spider处理器")
        }
        return isJs
    }

    /**
     * 检测是否有JAR包引用
     */
    private fun hasJarReference(): Boolean {
        val hasJar = jar.isNotBlank() || api.lowercase().contains(".jar")
        if (hasJar && type != 0) {
            Log.d("ONETV_MOVIE", "🔧 TVBOX智能识别: 站点 $name (type=$type) 检测到JAR包引用，使用Spider处理器")
        }
        return hasJar
    }

    /**
     * 检测是否包含Spider关键词
     */
    private fun isSpiderKeyword(): Boolean {
        val spiderKeywords = listOf("csp_", "spider", "爬虫", "drpy", "hipy")
        val hasKeyword = spiderKeywords.any { keyword ->
            api.lowercase().contains(keyword.lowercase()) ||
            name.lowercase().contains(keyword.lowercase())
        }

        if (hasKeyword && type != 0) {
            Log.d("ONETV_MOVIE", "🔧 TVBOX智能识别: 站点 $name (type=$type) 检测到Spider关键词，使用Spider处理器")
        }
        return hasKeyword
    }

    /**
     * 获取站点类型描述（用于调试）
     */
    fun getTypeDescription(): String {
        val actualType = when {
            isSpider() -> "Spider"
            isCms() -> "CMS"
            isApp() -> "APP"
            isAlist() -> "Alist"
            else -> "Unknown"
        }

        val configType = when(type) {
            0 -> "Spider"
            1 -> "CMS"
            3 -> "APP"
            4 -> "Alist"
            else -> "Unknown($type)"
        }

        return if (actualType != configType) {
            "$configType→$actualType(智能识别)"
        } else {
            actualType
        }
    }
    
    /**
     * 是否为CMS站点
     */
    fun isCms(): Boolean = type == 1
    
    /**
     * 是否为APP站点
     */
    fun isApp(): Boolean = type == 3
    
    /**
     * 是否为Alist站点
     */
    fun isAlist(): Boolean = type == 4
    

    
    /**
     * 获取请求头字符串 (TVBOX标准)
     */
    fun getHeaderString(): String {
        return "" // header现在是JsonElement，暂时返回空字符串
    }

    /**
     * 获取请求头Map (用于HTTP请求)
     */
    fun getHeaderMap(): Map<String, String> {
        return try {
            when (header) {
                is kotlinx.serialization.json.JsonObject -> {
                    header.mapValues { (_, value) ->
                        when (value) {
                            is kotlinx.serialization.json.JsonPrimitive -> value.content
                            else -> value.toString()
                        }
                    }
                }
                is kotlinx.serialization.json.JsonPrimitive -> {
                    // 如果是字符串，尝试解析为键值对
                    parseHeaderString(header.content)
                }
                else -> emptyMap()
            }
        } catch (e: Exception) {
            Log.w("ONETV_MOVIE", "解析请求头失败: ${e.message}")
            emptyMap()
        }
    }

    /**
     * 解析请求头字符串
     */
    private fun parseHeaderString(headerString: String): Map<String, String> {
        return try {
            if (headerString.isBlank()) return emptyMap()

            headerString.split("\n", ";", "&").mapNotNull { line ->
                val parts = line.split(":", "=", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * 是否有扩展配置 (TVBOX标准支持字符串或JSON对象)
     */
    fun hasExtension(): Boolean = getExtString().isNotEmpty()
    
    /**
     * 是否有JAR包
     */
    fun hasJar(): Boolean = jar.isNotEmpty()
    
    /**
     * 获取站点摘要信息
     */
    fun getSummary(): String {
        val searchText = if (isSearchable()) "可搜索" else "不可搜索"
        val changeText = if (isChangeable()) "可切换" else "不可切换"
        return "${getTypeDescription()} | $searchText | $changeText | 分类: ${categories.size}"
    }

    /**
     * 获取User-Agent (TVBOX标准支持从ext中获取)
     */
    fun getUserAgent(): String {
        return try {
            when (ext) {
                is JsonPrimitive -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                else -> {
                    ext.jsonObject["User-Agent"]?.jsonPrimitive?.content
                        ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                }
            }
        } catch (e: Exception) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        }
    }
}
