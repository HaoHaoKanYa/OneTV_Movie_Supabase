package top.cywin.onetv.movie.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TVBOX标准API响应 (完全参考OneMoVie Result)
 */
@Serializable
data class VodResponse(
    val code: Int = 1,
    val msg: String = "",
    val list: List<VodItem> = emptyList(),
    @SerialName("class") val classes: List<VodClass> = emptyList(),
    val filters: Map<String, List<VodFilter>> = emptyMap(),

    // CMS站点特有字段 (可选)
    val page: Int? = null,
    val pagecount: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,

    // 解析相关字段 (可选)
    val url: String? = null,
    val playUrl: String? = null,
    val parse: Int? = null,
    val header: Map<String, String>? = null,
    val jx: Int? = null
) {
    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = code == 1

    /**
     * 是否有更多数据 (仅CMS站点)
     */
    fun hasMore(): Boolean = page != null && pagecount != null && page < pagecount

    /**
     * 是否为空结果
     */
    fun isEmpty(): Boolean = list.isEmpty()

    /**
     * 获取下一页页码 (仅CMS站点)
     */
    fun getNextPage(): Int = if (hasMore()) (page ?: 1) + 1 else (page ?: 1)

    /**
     * 获取结果摘要
     */
    fun getSummary(): String {
        return if (page != null && pagecount != null && total != null) {
            "第${page}页/共${pagecount}页，共${total}条结果"
        } else {
            "共${list.size}条结果"
        }
    }



    /**
     * 是否需要解析
     */
    fun needParse(): Boolean = parse == 1

    /**
     * 是否需要嗅探
     */
    fun needSniffer(): Boolean = jx == 1
}

/**
 * 首页内容响应 (TVBOX标准)
 */
typealias VodHomeResponse = VodResponse

/**
 * 内容列表响应 (TVBOX标准)
 */
typealias VodListResponse = VodResponse

/**
 * 内容详情响应 (TVBOX标准)
 */
typealias VodDetailResponse = VodResponse

/**
 * 搜索响应 (TVBOX标准)
 */
typealias VodSearchResponse = VodResponse

/**
 * 解析响应 (TVBOX标准，复用VodResponse)
 */
typealias VodParseResponse = VodResponse

/**
 * 配置响应
 */
@Serializable
data class VodConfigResponse(
    val spider: String = "",
    val wallpaper: String = "",
    val sites: List<VodSite> = emptyList(),
    val parses: List<VodParse> = emptyList(),
    val flags: List<String> = emptyList(),
    val ijk: List<VodIjkOption> = emptyList(),
    val ads: List<String> = emptyList(),
    val notice: String = ""
) {
    /**
     * 是否为空配置
     */
    fun isEmpty(): Boolean = sites.isEmpty() && parses.isEmpty()
    
    /**
     * 获取配置摘要
     */
    fun getSummary(): String {
        return "站点: ${sites.size}, 解析器: ${parses.size}, 标识: ${flags.size}"
    }
}

/**
 * IJK播放器选项
 */
@Serializable
data class VodIjkOption(
    val group: String,
    val options: List<VodIjkParam>
)

/**
 * IJK播放器参数
 */
@Serializable
data class VodIjkParam(
    val category: Int,
    val name: String,
    val value: String
)
