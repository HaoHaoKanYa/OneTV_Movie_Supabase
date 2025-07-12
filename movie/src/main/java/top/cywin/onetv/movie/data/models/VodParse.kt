package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray

/**
 * 解析器配置 (参考OneMoVie Parse)
 */
@Serializable
data class VodParse(
    val name: String, // 解析器名称
    val type: Int, // 解析器类型: 0=嗅探, 1=JSON, 2=WebView, 3=自定义
    val url: String, // 解析地址
    val ext: Map<String, JsonElement> = emptyMap(), // 扩展配置 - 使用JsonElement支持TVBOX各种数据类型
    val header: Map<String, String> = emptyMap(), // 请求头
    val flag: List<String> = emptyList(), // 支持的播放标识 (TVBOX标准字段)
    private var runtimeHeaders: Map<String, String> = emptyMap(), // 运行时请求头
    private var clickUrl: String = "" // 点击地址
) {
    companion object {
        /**
         * 创建神解析器 (参考OneMoVie Parse.god())
         */
        fun god(): VodParse {
            return VodParse(
                name = "神解析",
                type = 0,
                url = ""
            )
        }
        
        /**
         * 根据类型和URL创建解析器 (参考OneMoVie Parse.get)
         */
        fun get(type: Int, url: String, name: String = ""): VodParse {
            return VodParse(
                name = name.ifEmpty { getTypeName(type) },
                type = type,
                url = url
            )
        }

        /**
         * 根据类型和URL创建解析器
         */
        fun create(type: Int, url: String, name: String = ""): VodParse {
            return get(type, url, name)
        }
        
        /**
         * 获取类型名称
         */
        private fun getTypeName(type: Int): String {
            return when (type) {
                0 -> "嗅探解析"
                1 -> "JSON解析"
                2 -> "WebView解析"
                3 -> "自定义解析"
                else -> "未知解析"
            }
        }
    }
    
    /**
     * 是否为嗅探解析
     */
    fun isSniffer(): Boolean = type == 0
    
    /**
     * 是否为JSON解析
     */
    fun isJson(): Boolean = type == 1
    
    /**
     * 是否为WebView解析
     */
    fun isWebView(): Boolean = type == 2
    
    /**
     * 是否为自定义解析
     */
    fun isCustom(): Boolean = type == 3
    
    /**
     * 获取解析器类型描述
     */
    fun getTypeDescription(): String {
        return when (type) {
            0 -> "嗅探解析"
            1 -> "JSON解析"
            2 -> "WebView解析"
            3 -> "自定义解析"
            else -> "未知解析"
        }
    }
    
    /**
     * 是否为神解析器
     */
    fun isGod(): Boolean = name == "神解析" && type == 0 && url.isEmpty()
    
    /**
     * 获取完整的解析URL
     */
    fun getParseUrl(playUrl: String): String {
        return if (url.contains("{url}")) {
            url.replace("{url}", playUrl)
        } else {
            "$url$playUrl"
        }
    }
    
    /**
     * 获取请求头字符串
     */
    fun getHeaderString(): String {
        return header.entries.joinToString("; ") { "${it.key}: ${it.value}" }
    }
    
    /**
     * 是否有扩展配置
     */
    fun hasExtension(): Boolean = ext.isNotEmpty()
    
    /**
     * 获取扩展配置值 - 支持TVBOX标准的各种数据类型
     */
    fun getExtValue(key: String): String? {
        val element = ext[key] ?: return null
        return when {
            element is JsonPrimitive && element.isString -> element.content
            element is JsonPrimitive -> element.toString().trim('"')
            element is JsonArray -> element.joinToString(",") {
                if (it is JsonPrimitive) it.content else it.toString()
            }
            else -> element.toString()
        }
    }

    /**
     * 获取扩展配置数组值 - 专门处理TVBOX的flag等数组字段
     */
    fun getExtArrayValue(key: String): List<String> {
        val element = ext[key] ?: return emptyList()
        return when {
            element is JsonArray -> element.map {
                if (it is JsonPrimitive) it.content else it.toString()
            }
            element is JsonPrimitive -> listOf(element.content)
            else -> listOf(element.toString())
        }
    }

    /**
     * 获取flag数组 - 从ext.flag或直接从flag字段获取
     */
    fun getFlagList(): List<String> {
        // 优先从ext.flag获取（TVBOX标准）
        val extFlags = getExtArrayValue("flag")
        return if (extFlags.isNotEmpty()) extFlags else flag
    }
    
    /**
     * 获取解析器摘要信息
     */
    fun getSummary(): String {
        val typeDesc = getTypeDescription()
        val urlDesc = if (url.isNotEmpty()) url else "无地址"
        return "$typeDesc | $urlDesc"
    }

    /**
     * 设置运行时请求头 (参考OneMoVie setHeader)
     */
    fun setHeader(headers: Map<String, String>) {
        runtimeHeaders = headers
    }

    /**
     * 设置点击地址 (参考OneMoVie setClick)
     */
    fun setClick(clickUrl: String) {
        this.clickUrl = clickUrl
    }

    /**
     * 获取所有请求头 (合并配置和运行时请求头)
     */
    fun getAllHeaders(): Map<String, String> {
        return header + runtimeHeaders
    }

    /**
     * 获取点击地址
     */
    fun getClickUrl(): String = clickUrl

    /**
     * 是否有点击地址
     */
    fun hasClickUrl(): Boolean = clickUrl.isNotEmpty()
}
