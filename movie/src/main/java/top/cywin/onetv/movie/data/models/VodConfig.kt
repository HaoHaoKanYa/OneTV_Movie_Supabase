package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 点播配置 (参考OneMoVie Config，支持动态分类)
 */
@Serializable
data class VodConfig(
    val id: Int = 0,
    val type: Int = 0, // 0: VOD, 1: Live
    val url: String = "",
    val name: String = "",
    val logo: String = "",
    val home: String = "", // 默认站点key
    val parse: String = "", // 默认解析器
    val json: String = "", // 配置JSON缓存
    val time: Long = 0L, // 更新时间
    val sites: List<VodSite> = emptyList(), // 站点列表
    val parses: List<VodParse> = emptyList(), // 解析器列表
    val spider: String = "", // 爬虫JAR地址
    val wallpaper: String = "", // 壁纸地址
    val notice: String = "" // 公告信息
) {
    companion object {
        fun create(url: String, name: String = ""): VodConfig {
            return VodConfig(
                url = url,
                name = name,
                time = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 获取默认站点
     */
    fun getHomeSite(): VodSite? {
        return if (home.isNotEmpty()) {
            sites.find { it.key == home }
        } else {
            sites.firstOrNull()
        }
    }
    
    /**
     * 获取默认解析器
     */
    fun getDefaultParse(): VodParse? {
        return if (parse.isNotEmpty()) {
            parses.find { it.name == parse }
        } else {
            parses.firstOrNull()
        }
    }
    
    /**
     * 获取所有可用站点
     */
    fun getAvailableSites(): List<VodSite> {
        return sites.filter { it.searchable == 1 || it.changeable == 1 }
    }
    
    /**
     * 根据key获取站点
     */
    fun getSite(key: String): VodSite? {
        return sites.find { it.key == key }
    }
    
    /**
     * 根据名称获取解析器
     */
    fun getParse(name: String): VodParse? {
        return parses.find { it.name == name }
    }
    
    /**
     * 是否为空配置
     */
    fun isEmpty(): Boolean {
        return sites.isEmpty() && parses.isEmpty()
    }
    
    /**
     * 获取配置摘要信息
     */
    fun getSummary(): String {
        return "站点: ${sites.size}, 解析器: ${parses.size}"
    }
}
