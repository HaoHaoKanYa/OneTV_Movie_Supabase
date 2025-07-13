package top.cywin.onetv.film.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * VOD 站点数据模型
 * 
 * 完全按照 FongMi/TV 的站点配置模型实现
 * 支持所有 TVBOX 标准字段和 FongMi/TV 扩展字段
 * 
 * 基础字段：
 * - key: 站点唯一标识
 * - name: 站点名称
 * - type: 站点类型（0=普通, 1=AppYs, 3=自定义Spider）
 * - api: API地址或Spider类名
 * 
 * TVBOX 标准字段：
 * - searchable: 是否支持搜索
 * - quickSearch: 是否支持快速搜索
 * - filterable: 是否支持筛选
 * - changeable: 是否可切换
 * - timeout: 超时时间
 * - jar: JAR包地址
 * 
 * FongMi/TV 扩展字段：
 * - ext: 扩展配置
 * - header: 请求头
 * - proxy: 代理配置
 * - hosts: Hosts重定向
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
@Serializable
data class VodSite(
    // ========== 基础字段 ==========
    
    /**
     * 站点唯一标识
     */
    val key: String,
    
    /**
     * 站点名称
     */
    val name: String,
    
    /**
     * 站点类型
     * - 0: 普通站点
     * - 1: AppYs 接口
     * - 3: 自定义 Spider
     */
    val type: Int,
    
    /**
     * API 地址或 Spider 类名
     */
    val api: String,
    
    // ========== TVBOX 标准字段 ==========
    
    /**
     * 是否支持搜索
     * - 0: 不支持
     * - 1: 支持
     */
    val searchable: Int = 1,
    
    /**
     * 是否支持快速搜索
     * - 0: 不支持
     * - 1: 支持
     */
    val quickSearch: Int = 1,
    
    /**
     * 是否支持筛选
     * - 0: 不支持
     * - 1: 支持
     */
    val filterable: Int = 1,
    
    /**
     * 是否可切换
     * - 0: 不可切换
     * - 1: 可切换
     */
    val changeable: Int = 1,
    
    /**
     * 索引排序
     */
    val indexs: Int = 0,
    
    /**
     * 超时时间（秒）
     */
    val timeout: Int = 15,
    
    /**
     * 播放器类型
     * - 0: 默认播放器
     * - 1: 系统播放器
     * - 2: 外部播放器
     */
    val playerType: Int = 0,
    
    /**
     * 播放链接
     */
    val playUrl: String = "",
    
    /**
     * 分类列表
     */
    val categories: List<String> = emptyList(),
    
    /**
     * JAR 包地址
     */
    val jar: String = "",
    
    /**
     * 点击事件
     */
    val click: String = "",
    
    /**
     * 样式配置
     */
    val style: Map<String, Any> = emptyMap(),
    
    // ========== FongMi/TV 扩展字段 ==========
    
    /**
     * 扩展配置
     * 可以是字符串、对象或数组
     */
    val ext: JsonElement = JsonPrimitive(""),
    
    /**
     * 请求头配置
     */
    val header: Map<String, String> = emptyMap(),
    
    /**
     * 代理配置列表
     */
    val proxy: List<String> = emptyList(),
    
    /**
     * Hosts 重定向配置
     */
    val hosts: List<String> = emptyList(),
    
    /**
     * User-Agent
     */
    val ua: String = "",
    
    /**
     * Referer
     */
    val referer: String = "",
    
    /**
     * Origin
     */
    val origin: String = "",
    
    /**
     * Cookie
     */
    val cookie: String = "",
    
    // ========== 高级配置 ==========
    
    /**
     * 重试次数
     */
    val retry: Int = 3,
    
    /**
     * 是否支持并发
     */
    val concurrent: Boolean = true,
    
    /**
     * 是否启用缓存
     */
    val cache: Boolean = true,
    
    /**
     * 是否启用调试
     */
    val debug: Boolean = false
) {
    
    /**
     * 🔍 检查站点是否有效
     */
    fun isValid(): Boolean {
        return key.isNotEmpty() && 
               name.isNotEmpty() && 
               api.isNotEmpty() &&
               type in 0..3
    }
    
    /**
     * 🔍 检查是否支持搜索
     */
    fun isSearchable(): Boolean = searchable == 1
    
    /**
     * 🔍 检查是否支持快速搜索
     */
    fun isQuickSearchable(): Boolean = quickSearch == 1
    
    /**
     * 🔍 检查是否支持筛选
     */
    fun isFilterable(): Boolean = filterable == 1
    
    /**
     * 🔍 检查是否可切换
     */
    fun isChangeable(): Boolean = changeable == 1
    
    /**
     * 🔧 获取站点类型描述
     */
    fun getTypeDescription(): String {
        return when (type) {
            0 -> "普通站点"
            1 -> "AppYs 接口"
            3 -> "自定义 Spider"
            else -> "未知类型"
        }
    }
    
    /**
     * 🔧 获取播放器类型描述
     */
    fun getPlayerTypeDescription(): String {
        return when (playerType) {
            0 -> "默认播放器"
            1 -> "系统播放器"
            2 -> "外部播放器"
            else -> "未知播放器"
        }
    }
    
    /**
     * 🌐 获取完整的请求头
     */
    fun getFullHeaders(): Map<String, String> {
        val fullHeaders = mutableMapOf<String, String>()
        
        // 添加基础请求头
        if (ua.isNotEmpty()) {
            fullHeaders["User-Agent"] = ua
        }
        if (referer.isNotEmpty()) {
            fullHeaders["Referer"] = referer
        }
        if (origin.isNotEmpty()) {
            fullHeaders["Origin"] = origin
        }
        if (cookie.isNotEmpty()) {
            fullHeaders["Cookie"] = cookie
        }
        
        // 添加自定义请求头
        fullHeaders.putAll(header)
        
        return fullHeaders
    }
    
    /**
     * 🔧 获取站点信息摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "key" to key,
            "name" to name,
            "type" to getTypeDescription(),
            "api" to api,
            "searchable" to isSearchable(),
            "quickSearch" to isQuickSearchable(),
            "filterable" to isFilterable(),
            "timeout" to timeout,
            "hasJar" to jar.isNotEmpty(),
            "hasHeaders" to header.isNotEmpty(),
            "hasProxy" to proxy.isNotEmpty(),
            "hasHosts" to hosts.isNotEmpty()
        )
    }
    
    /**
     * 🔧 创建站点副本（用于修改配置）
     */
    fun copy(
        newKey: String = this.key,
        newName: String = this.name,
        newType: Int = this.type,
        newApi: String = this.api,
        newTimeout: Int = this.timeout,
        newHeaders: Map<String, String> = this.header
    ): VodSite {
        return this.copy(
            key = newKey,
            name = newName,
            type = newType,
            api = newApi,
            timeout = newTimeout,
            header = newHeaders
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建默认站点
         */
        fun createDefault(key: String, name: String, api: String, type: Int = 3): VodSite {
            return VodSite(
                key = key,
                name = name,
                type = type,
                api = api,
                searchable = 1,
                quickSearch = 1,
                filterable = 1,
                timeout = 15
            )
        }
        
        /**
         * 🏭 创建 AppYs 站点
         */
        fun createAppYs(key: String, name: String, api: String): VodSite {
            return VodSite(
                key = key,
                name = name,
                type = 1,
                api = api,
                searchable = 1,
                quickSearch = 1,
                filterable = 1,
                timeout = 15
            )
        }
        
        /**
         * 🏭 创建 JavaScript 站点
         */
        fun createJavaScript(key: String, name: String, api: String): VodSite {
            return VodSite(
                key = key,
                name = name,
                type = 3,
                api = api,
                searchable = 1,
                quickSearch = 1,
                filterable = 1,
                timeout = 15
            )
        }
    }
}
