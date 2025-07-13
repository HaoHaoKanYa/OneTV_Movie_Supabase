package top.cywin.onetv.film.data.models

import kotlinx.serialization.Serializable

/**
 * XPath 配置数据模型
 * 
 * 基于 FongMi/TV 的 XPath 解析规则配置
 * 定义了网页解析所需的所有 XPath 规则
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */
@Serializable
data class XPathConfig(
    // ========== 分类相关规则 ==========
    
    /**
     * 分类列表规则
     * 用于选择分类元素列表
     */
    val categoryListRule: String = "",
    
    /**
     * 分类ID规则
     * 用于提取分类ID
     */
    val categoryIdRule: String = "",
    
    /**
     * 分类名称规则
     * 用于提取分类名称
     */
    val categoryNameRule: String = "",
    
    // ========== 内容列表相关规则 ==========
    
    /**
     * 内容列表规则
     * 用于选择内容项元素列表
     */
    val vodListRule: String = "",
    
    /**
     * 内容ID规则
     * 用于提取内容ID或链接
     */
    val vodIdRule: String = "",
    
    /**
     * 内容名称规则
     * 用于提取内容标题
     */
    val vodNameRule: String = "",
    
    /**
     * 内容图片规则
     * 用于提取封面图片链接
     */
    val vodPicRule: String = "",
    
    /**
     * 内容备注规则
     * 用于提取备注信息（如更新状态）
     */
    val vodRemarksRule: String = "",
    
    // ========== 详情页相关规则 ==========
    
    /**
     * 详情名称规则
     */
    val detailNameRule: String = "",
    
    /**
     * 详情图片规则
     */
    val detailPicRule: String = "",
    
    /**
     * 详情内容规则
     */
    val detailContentRule: String = "",
    
    /**
     * 详情年份规则
     */
    val detailYearRule: String = "",
    
    /**
     * 详情地区规则
     */
    val detailAreaRule: String = "",
    
    /**
     * 详情演员规则
     */
    val detailActorRule: String = "",
    
    /**
     * 详情导演规则
     */
    val detailDirectorRule: String = "",
    
    /**
     * 播放源规则
     */
    val playFromRule: String = "",
    
    /**
     * 播放列表规则
     */
    val playListRule: String = "",
    
    /**
     * 播放链接规则
     */
    val playUrlRule: String = "",
    
    // ========== 搜索相关规则 ==========
    
    /**
     * 搜索结果列表规则
     */
    val searchListRule: String = "",
    
    /**
     * 搜索结果ID规则
     */
    val searchIdRule: String = "",
    
    /**
     * 搜索结果名称规则
     */
    val searchNameRule: String = "",
    
    /**
     * 搜索结果图片规则
     */
    val searchPicRule: String = "",
    
    /**
     * 搜索结果备注规则
     */
    val searchRemarksRule: String = "",
    
    // ========== 分页相关规则 ==========
    
    /**
     * 当前页码规则
     */
    val pageCurrentRule: String = "",
    
    /**
     * 总页数规则
     */
    val pageTotalRule: String = "",
    
    /**
     * 下一页链接规则
     */
    val pageNextRule: String = "",
    
    // ========== 过滤相关规则 ==========
    
    /**
     * 过滤器规则
     */
    val filterRule: String = "",
    
    /**
     * 过滤器名称规则
     */
    val filterNameRule: String = "",
    
    /**
     * 过滤器值规则
     */
    val filterValueRule: String = "",
    
    // ========== URL 模板 ==========
    
    /**
     * 分类页面 URL 模板
     */
    val categoryUrlTemplate: String = "",
    
    /**
     * 详情页面 URL 模板
     */
    val detailUrlTemplate: String = "",
    
    /**
     * 搜索页面 URL 模板
     */
    val searchUrlTemplate: String = "",
    
    /**
     * 播放页面 URL 模板
     */
    val playUrlTemplate: String = "",
    
    // ========== 其他配置 ==========
    
    /**
     * 是否启用调试模式
     */
    val debug: Boolean = false,
    
    /**
     * 请求间隔（毫秒）
     */
    val requestInterval: Long = 1000,
    
    /**
     * 重试次数
     */
    val retryCount: Int = 3,
    
    /**
     * 超时时间（秒）
     */
    val timeout: Int = 15
) {
    
    /**
     * 🔍 检查配置是否有效
     */
    fun isValid(): Boolean {
        return categoryListRule.isNotEmpty() && 
               vodListRule.isNotEmpty() && 
               vodIdRule.isNotEmpty() && 
               vodNameRule.isNotEmpty()
    }
    
    /**
     * 🔧 获取配置摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "has_category_rules" to (categoryListRule.isNotEmpty() && categoryIdRule.isNotEmpty()),
            "has_vod_rules" to (vodListRule.isNotEmpty() && vodIdRule.isNotEmpty()),
            "has_detail_rules" to (detailNameRule.isNotEmpty()),
            "has_search_rules" to (searchListRule.isNotEmpty()),
            "has_play_rules" to (playUrlRule.isNotEmpty()),
            "has_filter_rules" to (filterRule.isNotEmpty()),
            "debug_enabled" to debug,
            "timeout" to timeout
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建默认配置
         */
        fun createDefault(): XPathConfig {
            return XPathConfig(
                categoryListRule = ".nav-item",
                categoryIdRule = "a@href",
                categoryNameRule = "a@text",
                vodListRule = ".video-item",
                vodIdRule = "a@href",
                vodNameRule = ".title@text",
                vodPicRule = "img@src",
                vodRemarksRule = ".remarks@text",
                detailNameRule = ".video-title@text",
                detailPicRule = ".video-pic img@src",
                detailContentRule = ".video-desc@text",
                playUrlRule = ".play-url@href",
                searchListRule = ".search-item",
                searchIdRule = "a@href",
                searchNameRule = ".title@text",
                searchPicRule = "img@src"
            )
        }
        
        /**
         * 🏭 创建通用配置
         */
        fun createGeneric(): XPathConfig {
            return XPathConfig(
                categoryListRule = "ul.nav li, .category-list li, .nav-item",
                categoryIdRule = "a@href",
                categoryNameRule = "a@text",
                vodListRule = ".video-list li, .movie-item, .video-item",
                vodIdRule = "a@href",
                vodNameRule = ".title@text, .name@text, h3@text",
                vodPicRule = "img@src, .pic img@src",
                vodRemarksRule = ".remarks@text, .status@text, .update@text",
                detailNameRule = ".video-title@text, .movie-title@text, h1@text",
                detailPicRule = ".video-pic img@src, .movie-pic img@src",
                detailContentRule = ".video-desc@text, .movie-desc@text, .content@text",
                detailYearRule = ".year@text, .date@text",
                detailAreaRule = ".area@text, .region@text",
                detailActorRule = ".actor@text, .cast@text",
                detailDirectorRule = ".director@text",
                playFromRule = ".play-source@text, .source@text",
                playListRule = ".play-list a, .episode-list a",
                playUrlRule = ".play-url@href, a@href",
                searchListRule = ".search-list li, .search-item",
                searchIdRule = "a@href",
                searchNameRule = ".title@text, .name@text",
                searchPicRule = "img@src"
            )
        }
    }
}

/**
 * 分类项数据模型
 */
@Serializable
data class CategoryItem(
    val typeId: String,
    val typeName: String
)

/**
 * 内容项数据模型
 */
@Serializable
data class VodItem(
    val vodId: String,
    val vodName: String,
    val vodPic: String = "",
    val vodRemarks: String = ""
)

/**
 * 视频详情数据模型
 */
@Serializable
data class VodDetail(
    val vodId: String,
    val vodName: String,
    val vodPic: String = "",
    val vodContent: String = "",
    val vodYear: String = "",
    val vodArea: String = "",
    val vodActor: String = "",
    val vodDirector: String = "",
    val vodPlayFrom: String = "",
    val vodPlayUrl: String = ""
)

/**
 * 分页信息数据模型
 */
@Serializable
data class PageInfo(
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val totalCount: Int
)
