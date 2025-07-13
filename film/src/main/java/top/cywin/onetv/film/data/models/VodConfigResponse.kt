package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * VOD 配置响应数据模型
 * 
 * 基于 FongMi/TV 的标准配置格式
 * 兼容 TVBOX 标准配置文件
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class VodConfigResponse(
    val spider: String = "",                           // 爬虫 JAR 地址
    val wallpaper: String = "",                        // 壁纸地址
    val logo: String = "",                             // 应用 Logo
    val warningText: String = "",                      // 警告文本
    val storeHouse: List<VodStoreHouse> = emptyList(), // 仓库配置
    val urls: List<VodConfigUrl> = emptyList(),        // 配置文件链接列表
    val doh: List<VodDohConfig> = emptyList(),         // DNS over HTTPS 配置
    val rules: List<VodProxyRule> = emptyList(),       // 代理规则配置
    val lives: List<JsonElement> = emptyList(),        // 直播源配置
    val sites: List<VodSite> = emptyList(),            // 站点列表
    val parses: List<VodParse> = emptyList(),          // 解析器列表
    val flags: List<String> = emptyList(),             // 播放标识列表
    val ijk: List<VodIjkOption> = emptyList(),         // IJK 播放器配置
    val ads: List<String> = emptyList(),               // 广告配置
    val notice: String = "",                           // 公告信息
    
    // 扩展字段
    val version: String = "",                          // 配置版本
    val updateTime: Long = System.currentTimeMillis(), // 更新时间
    val source: String = ""                            // 配置来源
) {
    
    /**
     * 🔍 检查是否为仓库索引文件
     */
    fun isStoreHouseIndex(): Boolean {
        return sites.isEmpty() && urls.isNotEmpty()
    }
    
    /**
     * 🔍 获取仓库名称
     */
    fun getStoreHouseName(): String {
        return storeHouse.firstOrNull()?.sourceName ?: "影视仓库"
    }
    
    /**
     * 🔍 是否为空配置
     */
    fun isEmpty(): Boolean = sites.isEmpty() && parses.isEmpty()
    
    /**
     * 🔍 是否有站点
     */
    fun hasSites(): Boolean = sites.isNotEmpty()
    
    /**
     * 🔍 是否有解析器
     */
    fun hasParses(): Boolean = parses.isNotEmpty()
    
    /**
     * 🔍 是否有直播源
     */
    fun hasLives(): Boolean = lives.isNotEmpty()
    
    /**
     * 🔍 根据 key 查找站点
     */
    fun findSite(key: String): VodSite? {
        return sites.find { it.key == key }
    }
    
    /**
     * 🔍 根据名称查找解析器
     */
    fun findParse(name: String): VodParse? {
        return parses.find { it.name == name }
    }
    
    /**
     * 🔍 获取可搜索的站点
     */
    fun getSearchableSites(): List<VodSite> {
        return sites.filter { it.searchable == 1 }
    }
    
    /**
     * 🔍 获取启用的站点
     */
    fun getEnabledSites(): List<VodSite> {
        return sites.filter { !it.isHidden() }
    }
    
    /**
     * 🔍 获取启用的解析器
     */
    fun getEnabledParses(): List<VodParse> {
        return parses.filter { it.type != null && it.type >= 0 }
    }
    
    /**
     * 📊 获取配置摘要
     */
    fun getSummary(): String {
        return "站点: ${sites.size}, 解析器: ${parses.size}, 标识: ${flags.size}"
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "sites_count" to sites.size,
            "parses_count" to parses.size,
            "flags_count" to flags.size,
            "ijk_count" to ijk.size,
            "ads_count" to ads.size,
            "lives_count" to lives.size,
            "searchable_sites" to getSearchableSites().size,
            "enabled_sites" to getEnabledSites().size,
            "enabled_parses" to getEnabledParses().size,
            "is_storehouse" to isStoreHouseIndex(),
            "has_spider" to spider.isNotEmpty(),
            "has_wallpaper" to wallpaper.isNotEmpty(),
            "update_time" to updateTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建空配置
         */
        fun empty(): VodConfigResponse {
            return VodConfigResponse()
        }
        
        /**
         * 🏭 创建仓库索引配置
         */
        fun storeHouse(
            urls: List<VodConfigUrl>,
            storeHouse: List<VodStoreHouse> = emptyList()
        ): VodConfigResponse {
            return VodConfigResponse(
                urls = urls,
                storeHouse = storeHouse
            )
        }
        
        /**
         * 🏭 创建标准配置
         */
        fun standard(
            sites: List<VodSite>,
            parses: List<VodParse>,
            spider: String = "",
            wallpaper: String = ""
        ): VodConfigResponse {
            return VodConfigResponse(
                sites = sites,
                parses = parses,
                spider = spider,
                wallpaper = wallpaper
            )
        }
    }
}

/**
 * TVBOX 配置文件链接
 */
@Serializable
data class VodConfigUrl(
    val url: String = "",                              // 配置文件 URL
    val name: String = ""                              // 配置文件名称
)

/**
 * DNS over HTTPS 配置
 */
@Serializable
data class VodDohConfig(
    val name: String = "",                             // DNS 名称
    val url: String = "",                              // DNS 地址
    val ips: List<String> = emptyList()                // IP 列表
)

/**
 * 代理规则配置
 */
@Serializable
data class VodProxyRule(
    val name: String = "",                             // 规则名称
    val hosts: List<String> = emptyList()              // 主机列表
)

/**
 * 仓库配置
 */
@Serializable
data class VodStoreHouse(
    val sourceName: String = "",                       // 仓库名称
    val sourceUrl: String = ""                         // 仓库地址
)

/**
 * IJK 播放器选项
 */
@Serializable
data class VodIjkOption(
    val group: String,                                 // 选项组
    val options: List<VodIjkParam>                     // 选项列表
)

/**
 * IJK 播放器参数
 */
@Serializable
data class VodIjkParam(
    val category: Int,                                 // 参数类别
    val name: String,                                  // 参数名称
    val value: String                                  // 参数值
)

/**
 * VOD 解析器
 */
@Serializable
data class VodParse(
    val name: String,                                  // 解析器名称
    val type: Int? = 0,                                // 解析器类型: 0=嗅探, 1=JSON, 2=WebView, 3=自定义, 4=神解析
    val url: String = "",                              // 解析地址
    val ext: VodParseExt? = null,                      // 扩展配置
    
    // 运行时字段
    var activated: Boolean = false,                    // 是否激活
    var click: String = ""                             // 点击地址
) {
    
    /**
     * 🔍 是否为空解析器
     */
    fun isEmpty(): Boolean = type == 0 && url.isEmpty()
    
    /**
     * 🔍 是否为神解析器
     */
    fun isGod(): Boolean = type == 4
    
    /**
     * 🔍 获取请求头
     */
    fun getHeaders(): Map<String, String> {
        return ext?.header ?: emptyMap()
    }
    
    /**
     * 🔍 获取支持的播放标识
     */
    fun getFlags(): List<String> {
        return ext?.flag ?: emptyList()
    }
    
    companion object {
        
        /**
         * 🏭 创建神解析器
         */
        fun god(): VodParse {
            return VodParse(
                name = "神解析",
                type = 4,
                url = ""
            )
        }
        
        /**
         * 🏭 创建简单解析器
         */
        fun simple(name: String, url: String, type: Int = 0): VodParse {
            return VodParse(
                name = name,
                type = type,
                url = url
            )
        }
    }
}

/**
 * VOD 解析器扩展配置
 */
@Serializable
data class VodParseExt(
    val flag: List<String> = emptyList(),              // 支持的播放标识
    val header: Map<String, String> = emptyMap()       // 请求头
) {
    
    /**
     * 🔍 是否为空配置
     */
    fun isEmpty(): Boolean = flag.isEmpty() && header.isEmpty()
}
