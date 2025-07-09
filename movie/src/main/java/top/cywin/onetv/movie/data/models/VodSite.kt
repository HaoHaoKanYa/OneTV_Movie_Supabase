package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    val ext: String = "", // 扩展配置
    val jar: String = "", // JAR包地址
    val type: Int = 1, // 站点类型: 0=spider, 1=cms, 3=app, 4=alist
    val searchable: Int = 1, // 是否可搜索
    val changeable: Int = 1, // 是否可切换
    val timeout: Int = 30000, // 超时时间
    val header: Map<String, String> = emptyMap(), // 请求头
    val style: VodStyle? = null, // 显示样式配置
    val categories: List<VodClass> = emptyList() // 动态分类列表 (从API获取)
) {
    /**
     * 是否可搜索
     */
    fun isSearchable(): Boolean = searchable == 1
    
    /**
     * 是否可切换
     */
    fun isChangeable(): Boolean = changeable == 1
    
    /**
     * 获取分类显示名称
     */
    fun getCategoryName(typeId: String): String {
        return categories.find { it.typeId == typeId }?.typeName ?: "未知分类"
    }
    
    /**
     * 获取所有可用分类
     */
    fun getAvailableCategories(): List<VodClass> {
        return categories.filter { it.typeFlag != "0" }
    }
    
    /**
     * 是否为爬虫站点
     */
    fun isSpider(): Boolean = type == 0
    
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
     * 获取站点类型描述
     */
    fun getTypeDescription(): String {
        return when (type) {
            0 -> "爬虫"
            1 -> "CMS"
            3 -> "APP"
            4 -> "Alist"
            else -> "未知"
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
     * 获取User-Agent
     */
    fun getUserAgent(): String {
        return if (ext.isNotEmpty()) {
            // 从ext中提取User-Agent
            try {
                val extJson = kotlinx.serialization.json.Json.parseToJsonElement(ext)
                extJson.jsonObject["User-Agent"]?.jsonPrimitive?.content
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            } catch (e: Exception) {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            }
        } else {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        }
    }
}
