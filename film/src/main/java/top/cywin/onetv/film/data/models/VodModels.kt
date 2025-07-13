package top.cywin.onetv.film.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * VOD 数据模型
 *
 * 基于 FongMi/TV 的数据模型定义
 * 兼容 CatVod 标准接口
 *
 * @author OneTV Team
 * @since 2025-07-12
 */

/**
 * 站点状态枚举
 */
@Serializable
enum class SiteStatus {
    UNKNOWN,        // 未知
    ONLINE,         // 在线
    OFFLINE,        // 离线
    MAINTENANCE,    // 维护中
    ERROR,          // 错误
    TIMEOUT,        // 超时
    BLOCKED,        // 被屏蔽
    DEPRECATED      // 已废弃
}

/**
 * 验证状态枚举
 */
@Serializable
enum class VerifyStatus {
    UNVERIFIED,     // 未验证
    VERIFIED,       // 已验证
    REJECTED,       // 已拒绝
    PENDING,        // 待审核
    EXPIRED         // 已过期
}

/**
 * VOD 站点配置
 */
@Serializable
data class VodSite(
    val key: String,                    // 站点唯一标识
    val name: String,                   // 站点名称
    val type: Int,                      // 站点类型 (0=内置, 1=jar, 3=json, 4=xml)
    val api: String,                    // API 地址
    val searchable: Int = 1,            // 是否可搜索 (0=否, 1=是)
    val quickSearch: Int = 0,           // 是否支持快速搜索
    val filterable: Int = 0,            // 是否支持筛选
    val playUrl: String = "",           // 播放地址
    val categories: List<String> = emptyList(), // 分类列表
    val ext: String = "",               // 扩展配置
    val jar: String = "",               // JAR 文件地址
    val timeout: Long = 15000L,         // 超时时间
    val headers: Map<String, String> = emptyMap(), // 请求头
    val enabled: Boolean = true,        // 是否启用
    val weight: Int = 0,                // 权重 (用于排序)
    val group: String = "默认",         // 分组
    val description: String = "",       // 描述
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis(), // 更新时间

    // FongMi/TV 标准字段
    val playerUrl: String = "",         // 播放器地址
    val recordable: Int = 0,            // 是否支持录制 (0=否, 1=是)
    val changeable: Int = 1,            // 是否可更改 (0=否, 1=是)
    val reverseOrder: Int = 0,          // 是否倒序 (0=否, 1=是)
    val style: Map<String, String> = emptyMap(), // 样式配置
    val click: String = "",             // 点击事件
    val ads: List<String> = emptyList(), // 广告配置
    val parse: Int = 0,                 // 解析模式 (0=直接播放, 1=需要解析)
    val jx: Int = 0,                    // 解析接口 (0=不使用, 1=使用)
    val header: String = "",            // 请求头字符串
    val ua: String = "",                // User-Agent
    val referer: String = "",           // Referer
    val origin: String = "",            // Origin
    val cookie: String = "",            // Cookie
    val proxy: String = "",             // 代理配置
    val dns: String = "",               // DNS 配置
    val spider: String = "",            // Spider 类名
    val playerType: Int = 0,            // 播放器类型
    val ijk: String = "",               // IJK 播放器配置
    val exo: String = "",               // ExoPlayer 配置
    val mxPlayer: String = "",          // MX Player 配置
    val relaunch: String = "",          // 重启配置
    val boot: String = "",              // 启动配置
    val logo: String = "",              // 站点图标
    val epg: String = "",               // EPG 配置
    val playerConfig: Map<String, Any> = emptyMap(), // 播放器配置
    val siteConfig: Map<String, Any> = emptyMap(),   // 站点配置
    val filterConfig: Map<String, Any> = emptyMap(), // 筛选配置
    val searchConfig: Map<String, Any> = emptyMap(), // 搜索配置
    val parseConfig: Map<String, Any> = emptyMap(),  // 解析配置
    val hookConfig: Map<String, Any> = emptyMap(),   // Hook 配置
    val proxyConfig: Map<String, Any> = emptyMap(),  // 代理配置
    val cacheConfig: Map<String, Any> = emptyMap(),  // 缓存配置
    val retryConfig: Map<String, Any> = emptyMap(),  // 重试配置
    val limitConfig: Map<String, Any> = emptyMap(),  // 限制配置
    val customConfig: Map<String, Any> = emptyMap(), // 自定义配置

    // 统计和状态字段
    val accessCount: Long = 0L,         // 访问次数
    val successCount: Long = 0L,        // 成功次数
    val errorCount: Long = 0L,          // 错误次数
    val lastAccessTime: Long = 0L,      // 最后访问时间
    val lastSuccessTime: Long = 0L,     // 最后成功时间
    val lastErrorTime: Long = 0L,       // 最后错误时间
    val lastErrorMessage: String = "",  // 最后错误信息
    val averageResponseTime: Long = 0L, // 平均响应时间
    val status: SiteStatus = SiteStatus.UNKNOWN, // 站点状态
    val version: String = "1.0.0",      // 版本号
    val author: String = "",            // 作者
    val contact: String = "",           // 联系方式
    val homepage: String = "",          // 主页地址
    val license: String = "",           // 许可证
    val tags: List<String> = emptyList(), // 标签
    val language: String = "zh-CN",     // 语言
    val region: String = "CN",          // 地区
    val rating: Float = 0.0f,           // 评分
    val ratingCount: Int = 0,           // 评分人数
    val downloadCount: Long = 0L,       // 下载次数
    val favoriteCount: Long = 0L,       // 收藏次数
    val reportCount: Long = 0L,         // 举报次数
    val verifyStatus: VerifyStatus = VerifyStatus.UNVERIFIED, // 验证状态
    val verifyTime: Long = 0L,          // 验证时间
    val verifyMessage: String = "",     // 验证信息
    val maintainer: String = "",        // 维护者
    val maintainTime: Long = 0L,        // 维护时间
    val deprecated: Boolean = false,    // 是否已废弃
    val deprecatedReason: String = "",  // 废弃原因
    val replacement: String = "",       // 替代站点
    val backup: List<String> = emptyList(), // 备用地址
    val mirror: List<String> = emptyList(), // 镜像地址
    val cdn: List<String> = emptyList(),    // CDN 地址
    val loadBalancer: String = "",      // 负载均衡配置
    val healthCheck: String = "",       // 健康检查配置
    val monitoring: Map<String, Any> = emptyMap(), // 监控配置
    val alerting: Map<String, Any> = emptyMap(),   // 告警配置
    val logging: Map<String, Any> = emptyMap(),    // 日志配置
    val security: Map<String, Any> = emptyMap(),   // 安全配置
    val performance: Map<String, Any> = emptyMap(), // 性能配置
    val metadata: Map<String, Any> = emptyMap()    // 元数据
) {
    
    /**
     * 🔍 是否为 JAR 类型站点
     */
    fun isJarSite(): Boolean = type == 1
    
    /**
     * 🔍 是否为 JSON 类型站点
     */
    fun isJsonSite(): Boolean = type == 3
    
    /**
     * 🔍 是否为 XML 类型站点
     */
    fun isXmlSite(): Boolean = type == 4
    
    /**
     * 🔍 是否支持搜索
     */
    fun isSearchable(): Boolean = searchable == 1
    
    /**
     * 🔍 是否支持快速搜索
     */
    fun isQuickSearchable(): Boolean = quickSearch == 1
    
    /**
     * 🔍 是否支持筛选
     */
    fun isFilterable(): Boolean = filterable == 1
    
    /**
     * 📊 获取站点摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "key" to key,
            "name" to name,
            "type" to type,
            "type_name" to when (type) {
                0 -> "内置"
                1 -> "JAR"
                3 -> "JSON"
                4 -> "XML"
                else -> "未知"
            },
            "searchable" to isSearchable(),
            "quick_search" to isQuickSearchable(),
            "filterable" to isFilterable(),
            "enabled" to enabled,
            "group" to group,
            "categories_count" to categories.size
        )
    }
}



/**
 * VOD 筛选条件
 */
@Serializable
data class VodFilter(
    val key: String,                    // 筛选键
    val name: String,                   // 筛选名称
    val value: List<VodFilterValue> = emptyList() // 筛选值列表
)

/**
 * VOD 筛选值
 */
@Serializable
data class VodFilterValue(
    val n: String,                      // 显示名称
    val v: String                       // 实际值
)





/**
 * VOD 播放组
 */
@Serializable
data class VodPlayGroup(
    val from: String,                   // 播放来源
    val episodes: List<VodPlayEpisode>  // 播放集数列表
) {
    
    /**
     * 🔍 根据名称查找集数
     */
    fun findEpisode(name: String): VodPlayEpisode? {
        return episodes.find { it.name == name }
    }
    
    /**
     * 🔍 根据索引获取集数
     */
    fun getEpisode(index: Int): VodPlayEpisode? {
        return episodes.getOrNull(index)
    }
}

/**
 * VOD 播放集数
 */
@Serializable
data class VodPlayEpisode(
    val name: String,                   // 集数名称
    val url: String                     // 播放地址
)

/**
 * VOD 下载组
 */
@Serializable
data class VodDownloadGroup(
    val from: String,                   // 下载来源
    val episodes: List<VodDownloadEpisode> // 下载集数列表
)

/**
 * VOD 下载集数
 */
@Serializable
data class VodDownloadEpisode(
    val name: String,                   // 集数名称
    val url: String                     // 下载地址
)

/**
 * VOD 搜索结果
 */
@Serializable
data class VodSearchResult(
    val list: List<VodItem> = emptyList(), // VOD 列表
    val page: Int = 1,                  // 当前页码
    val pageCount: Int = 1,             // 总页数
    val limit: Int = 20,                // 每页数量
    val total: Int = 0,                 // 总数量
    val siteKey: String = "",           // 站点标识
    val keyword: String = "",           // 搜索关键词
    val searchTime: Long = System.currentTimeMillis() // 搜索时间
) {
    
    /**
     * 🔍 是否有更多页
     */
    fun hasMore(): Boolean = page < pageCount
    
    /**
     * 🔍 是否为空结果
     */
    fun isEmpty(): Boolean = list.isEmpty()
    
    /**
     * 📊 获取搜索摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "keyword" to keyword,
            "site_key" to siteKey,
            "total" to total,
            "page" to page,
            "page_count" to pageCount,
            "current_count" to list.size,
            "has_more" to hasMore(),
            "is_empty" to isEmpty(),
            "search_time" to searchTime
        )
    }
}

/**
 * VOD 分类结果
 */
@Serializable
data class VodCategoryResult(
    val list: List<VodItem> = emptyList(), // VOD 列表
    val page: Int = 1,                  // 当前页码
    val pageCount: Int = 1,             // 总页数
    val limit: Int = 20,                // 每页数量
    val total: Int = 0,                 // 总数量
    val siteKey: String = "",           // 站点标识
    val typeId: String = "",            // 分类ID
    val typeName: String = "",          // 分类名称
    val filters: Map<String, String> = emptyMap(), // 筛选条件
    val loadTime: Long = System.currentTimeMillis() // 加载时间
) {
    
    /**
     * 🔍 是否有更多页
     */
    fun hasMore(): Boolean = page < pageCount
    
    /**
     * 🔍 是否为空结果
     */
    fun isEmpty(): Boolean = list.isEmpty()
    
    /**
     * 📊 获取分类摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "type_id" to typeId,
            "type_name" to typeName,
            "site_key" to siteKey,
            "total" to total,
            "page" to page,
            "page_count" to pageCount,
            "current_count" to list.size,
            "has_more" to hasMore(),
            "is_empty" to isEmpty(),
            "filters_count" to filters.size,
            "load_time" to loadTime
        )
    }
}

/**
 * VOD 首页结果
 */
@Serializable
data class VodHomeResult(
    val categories: List<VodClass> = emptyList(), // 分类列表
    val filters: Map<String, List<VodFilter>> = emptyMap(), // 筛选条件
    val banners: List<VodBanner> = emptyList(), // 轮播图
    val recommendations: List<VodItem> = emptyList(), // 推荐内容
    val siteKey: String = "",           // 站点标识
    val siteName: String = "",          // 站点名称
    val loadTime: Long = System.currentTimeMillis() // 加载时间
) {
    
    /**
     * 🔍 根据ID查找分类
     */
    fun findCategory(typeId: String): VodClass? {
        return categories.find { it.typeId == typeId }
    }
    
    /**
     * 📊 获取首页摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "site_key" to siteKey,
            "site_name" to siteName,
            "categories_count" to categories.size,
            "filters_count" to filters.size,
            "banners_count" to banners.size,
            "recommendations_count" to recommendations.size,
            "load_time" to loadTime
        )
    }
}

/**
 * VOD 轮播图
 */
@Serializable
data class VodBanner(
    val title: String,                  // 标题
    val pic: String,                    // 图片
    val url: String = "",               // 链接地址
    val vodId: String = "",             // VOD ID
    val description: String = ""        // 描述
)

/**
 * 播放器结果
 */
@Serializable
data class PlayerResult(
    val url: String = "",
    val header: Map<String, String> = emptyMap(),
    val parse: Int = 0,
    val playType: Int = 0,
    val subt: String = "",
    val jx: Int = 0,
    val danmaku: String = "",
    val siteKey: String = "",
    val flag: String = "",
    val id: String = ""
)

/**
 * 搜索结果
 */
@Serializable
data class SearchResult(
    val keyword: String,
    val results: List<SiteSearchResult>,
    val totalResults: Int,
    val searchType: SearchType,
    val searchTime: Long,
    val suggestions: List<String>
)

/**
 * 站点搜索结果
 */
@Serializable
data class SiteSearchResult(
    val site: VodSite,
    val vodResponse: VodResponse,
    val success: Boolean
)

/**
 * 搜索类型枚举
 */
@Serializable
enum class SearchType {
    QUICK,          // 快速搜索
    COMPREHENSIVE,  // 综合搜索
    PRECISE,        // 精确搜索
    FUZZY          // 模糊搜索
}
