package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * VOD 响应数据模型
 *
 * 完全基于 FongMi/TV 的 Result.java 标准
 * 兼容 TVBOX 标准 API 响应
 *
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class VodResponse(
    // 核心响应字段 (对应 FongMi/TV Result.java)
    @SerialName("class") val types: List<VodClass> = emptyList(), // 分类列表
    @SerialName("list") val list: List<VodItem> = emptyList(),    // VOD 列表
    val filters: Map<String, List<VodFilter>> = emptyMap(),       // 筛选条件
    val url: VodUrl? = null,                                      // URL 配置
    val msg: String = "",                                         // 响应消息
    val danmaku: List<String> = emptyList(),                      // 弹幕列表
    val subs: List<VodSubtitle> = emptyList(),                    // 字幕列表
    val header: JsonElement? = null,                              // 请求头 (JsonElement 类型)
    val playUrl: String = "",                                     // 播放地址
    val jxFrom: String = "",                                      // 解析来源
    val flag: String = "",                                        // 播放标识
    val desc: String = "",                                        // 描述信息
    val format: String = "",                                      // 播放格式
    val click: String = "",                                       // 点击地址
    val key: String = "",                                         // 播放密钥
    val pagecount: Int? = null,                                   // 总页数
    val parse: Int? = null,                                       // 是否需要解析 (0=直链, 1=需要解析)
    val code: Int? = null,                                        // 响应码 (1=成功, 0=失败)
    val jx: Int? = null,                                          // 是否需要嗅探 (0=不需要, 1=需要)
    val drm: VodDrm? = null                                       // DRM 配置
) {

    /**
     * 🔍 是否成功 (基于 FongMi/TV 标准)
     */
    fun isSuccess(): Boolean = code == null || code == 1

    /**
     * 🔍 是否为空结果
     */
    fun isEmpty(): Boolean = list.isEmpty()

    /**
     * 🔍 是否有消息
     */
    fun hasMsg(): Boolean = msg.isNotEmpty()

    /**
     * 🔍 是否需要解析
     */
    fun needParse(): Boolean = parse == 1

    /**
     * 🔍 是否需要嗅探
     */
    fun needSniffer(): Boolean = jx == 1

    /**
     * 🔍 获取真实播放地址 (基于 FongMi/TV getRealUrl)
     */
    fun getRealUrl(): String {
        return playUrl + (url?.v() ?: "")
    }

    /**
     * 🔍 获取请求头 (基于 FongMi/TV getHeaders)
     */
    fun getHeaders(): Map<String, String> {
        return try {
            // 这里需要实现 JsonElement 到 Map 的转换
            emptyMap() // 临时实现
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 🔍 根据ID查找VOD
     */
    fun findVod(vodId: String): VodItem? {
        return list.find { it.vodId == vodId }
    }

    /**
     * 🔍 根据ID查找分类
     */
    fun findCategory(typeId: String): VodClass? {
        return types.find { it.typeId == typeId }
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "code" to code,
            "success" to isSuccess(),
            "list_count" to list.size,
            "class_count" to classes.size,
            "filter_count" to filters.size,
            "has_pagination" to (page != null),
            "has_more" to hasMore(),
            "need_parse" to needParse(),
            "need_sniffer" to needSniffer(),
            "site_key" to siteKey,
            "load_time" to loadTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建成功响应
         */
        fun success(
            list: List<VodItem> = emptyList(),
            classes: List<VodClass> = emptyList(),
            filters: Map<String, List<VodFilter>> = emptyMap(),
            siteKey: String = "",
            siteName: String = ""
        ): VodResponse {
            return VodResponse(
                code = 1,
                msg = "success",
                list = list,
                classes = classes,
                filters = filters,
                siteKey = siteKey,
                siteName = siteName
            )
        }
        
        /**
         * 🏭 创建失败响应
         */
        fun failure(
            msg: String,
            siteKey: String = "",
            siteName: String = ""
        ): VodResponse {
            return VodResponse(
                code = 0,
                msg = msg,
                siteKey = siteKey,
                siteName = siteName
            )
        }
        
        /**
         * 🏭 创建空响应
         */
        fun empty(
            siteKey: String = "",
            siteName: String = ""
        ): VodResponse {
            return VodResponse(
                code = 1,
                msg = "empty",
                list = emptyList(),
                siteKey = siteKey,
                siteName = siteName
            )
        }
        
        /**
         * 🏭 创建分页响应
         */
        fun paged(
            list: List<VodItem>,
            page: Int,
            pageCount: Int,
            limit: Int,
            total: Int,
            siteKey: String = "",
            siteName: String = ""
        ): VodResponse {
            return VodResponse(
                code = 1,
                msg = "success",
                list = list,
                page = page,
                pagecount = pageCount,
                limit = limit,
                total = total,
                siteKey = siteKey,
                siteName = siteName
            )
        }
    }
}

/**
 * 首页内容响应 (TVBOX 标准)
 */
typealias VodHomeResponse = VodResponse

/**
 * 内容列表响应 (TVBOX 标准)
 */
typealias VodListResponse = VodResponse

/**
 * 内容详情响应 (TVBOX 标准)
 */
typealias VodDetailResponse = VodResponse

/**
 * 搜索响应 (TVBOX 标准)
 */
typealias VodSearchResponse = VodResponse

/**
 * 解析响应 (TVBOX 标准，复用 VodResponse)
 */
typealias VodParseResponse = VodResponse

/**
 * VOD URL 配置 (基于 FongMi/TV Url.java)
 */
@Serializable
data class VodUrl(
    val url: String = "",                              // URL 地址
    val params: Map<String, String> = emptyMap()       // URL 参数
) {

    /**
     * 获取参数字符串 (基于 FongMi/TV v() 方法)
     */
    fun v(): String {
        return if (params.isEmpty()) {
            ""
        } else {
            "?" + params.map { "${it.key}=${it.value}" }.joinToString("&")
        }
    }

    /**
     * 替换 URL (基于 FongMi/TV replace() 方法)
     */
    fun replace(newUrl: String): VodUrl {
        return copy(url = newUrl)
    }

    companion object {

        /**
         * 创建空 URL
         */
        fun create(): VodUrl {
            return VodUrl()
        }
    }
}

/**
 * VOD 字幕 (基于 FongMi/TV Sub.java)
 */
@Serializable
data class VodSubtitle(
    val name: String = "",                             // 字幕名称
    val url: String = "",                              // 字幕地址
    val lang: String = "",                             // 字幕语言
    val format: String = ""                            // 字幕格式
)

/**
 * VOD DRM 配置 (基于 FongMi/TV Drm.java)
 */
@Serializable
data class VodDrm(
    val type: String = "",                             // DRM 类型
    val licenseUrl: String = "",                       // 许可证地址
    val keyId: String = "",                            // 密钥 ID
    val key: String = "",                              // 密钥
    val headers: Map<String, String> = emptyMap()      // DRM 请求头
)
