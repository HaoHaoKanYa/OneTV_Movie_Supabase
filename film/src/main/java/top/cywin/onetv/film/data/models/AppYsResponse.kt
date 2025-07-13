package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AppYs API 响应数据模型
 * 
 * 基于 FongMi/TV 的 AppYs 解析器标准
 * 兼容 AppYs 站点 API 响应格式
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class AppYsResponse(
    val code: Int = 1,                                 // 响应码 (1=成功, 0=失败)
    val msg: String = "",                              // 响应消息
    val data: AppYsData? = null,                       // 响应数据
    
    // 扩展字段
    val siteKey: String = "",                          // 站点标识
    val requestTime: Long = System.currentTimeMillis() // 请求时间
) {
    
    /**
     * 🔍 是否成功
     */
    fun isSuccess(): Boolean = code == 1
    
    /**
     * 🔍 是否有数据
     */
    fun hasData(): Boolean = data != null
    
    /**
     * 🔍 获取视频列表
     */
    fun getVideoList(): List<AppYsVideoItem> {
        return data?.list ?: emptyList()
    }
    
    /**
     * 🔍 获取分类列表
     */
    fun getCategoryList(): List<AppYsCategory> {
        return data?.type ?: emptyList()
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "code" to code,
            "success" to isSuccess(),
            "has_data" to hasData(),
            "video_count" to getVideoList().size,
            "category_count" to getCategoryList().size,
            "site_key" to siteKey,
            "request_time" to requestTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建成功响应
         */
        fun success(
            data: AppYsData,
            siteKey: String = ""
        ): AppYsResponse {
            return AppYsResponse(
                code = 1,
                msg = "success",
                data = data,
                siteKey = siteKey
            )
        }
        
        /**
         * 🏭 创建失败响应
         */
        fun failure(
            msg: String,
            siteKey: String = ""
        ): AppYsResponse {
            return AppYsResponse(
                code = 0,
                msg = msg,
                data = null,
                siteKey = siteKey
            )
        }
        
        /**
         * 🏭 创建空响应
         */
        fun empty(siteKey: String = ""): AppYsResponse {
            return AppYsResponse(
                code = 1,
                msg = "empty",
                data = AppYsData.empty(),
                siteKey = siteKey
            )
        }
    }
}

/**
 * AppYs 响应数据
 */
@Serializable
data class AppYsData(
    val list: List<AppYsVideoItem> = emptyList(),      // 视频列表
    val type: List<AppYsCategory> = emptyList(),       // 分类列表
    val page: Int = 1,                                 // 当前页码
    val pagecount: Int = 1,                            // 总页数
    val limit: Int = 20,                               // 每页数量
    val total: Int = 0                                 // 总数量
) {
    
    /**
     * 🔍 是否有更多页
     */
    fun hasMore(): Boolean = page < pagecount
    
    /**
     * 🔍 是否为空结果
     */
    fun isEmpty(): Boolean = list.isEmpty()
    
    /**
     * 🔍 获取下一页页码
     */
    fun getNextPage(): Int = if (hasMore()) page + 1 else page
    
    companion object {
        
        /**
         * 🏭 创建空数据
         */
        fun empty(): AppYsData {
            return AppYsData()
        }
        
        /**
         * 🏭 创建视频列表数据
         */
        fun videoList(
            list: List<AppYsVideoItem>,
            page: Int = 1,
            pageCount: Int = 1,
            limit: Int = 20,
            total: Int = list.size
        ): AppYsData {
            return AppYsData(
                list = list,
                page = page,
                pagecount = pageCount,
                limit = limit,
                total = total
            )
        }
        
        /**
         * 🏭 创建分类列表数据
         */
        fun categoryList(categories: List<AppYsCategory>): AppYsData {
            return AppYsData(
                type = categories
            )
        }
    }
}

/**
 * AppYs 视频项目
 */
@Serializable
data class AppYsVideoItem(
    @SerialName("vod_id") val vodId: String,           // 视频 ID
    @SerialName("vod_name") val vodName: String,       // 视频名称
    @SerialName("vod_pic") val vodPic: String = "",    // 视频图片
    @SerialName("vod_remarks") val vodRemarks: String = "", // 视频备注
    @SerialName("vod_year") val vodYear: String = "",  // 年份
    @SerialName("vod_area") val vodArea: String = "",  // 地区
    @SerialName("vod_director") val vodDirector: String = "", // 导演
    @SerialName("vod_actor") val vodActor: String = "", // 演员
    @SerialName("vod_content") val vodContent: String = "", // 内容简介
    @SerialName("vod_play_from") val vodPlayFrom: String = "", // 播放来源
    @SerialName("vod_play_url") val vodPlayUrl: String = "", // 播放地址
    @SerialName("type_id") val typeId: String = "",    // 分类 ID
    @SerialName("type_name") val typeName: String = "" // 分类名称
) {
    
    /**
     * 🔍 是否有播放源
     */
    fun hasPlaySource(): Boolean = vodPlayFrom.isNotEmpty() && vodPlayUrl.isNotEmpty()
    
    /**
     * 🔍 获取年份数值
     */
    fun getYearValue(): Int {
        return try {
            vodYear.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 🎬 转换为标准 VodItem
     */
    fun toVodItem(siteKey: String = ""): VodItem {
        return VodItem(
            vodId = vodId,
            vodName = vodName,
            vodPic = vodPic,
            vodRemarks = vodRemarks,
            vodYear = vodYear,
            vodArea = vodArea,
            vodDirector = vodDirector,
            vodActor = vodActor,
            vodContent = vodContent,
            vodPlayFrom = vodPlayFrom,
            vodPlayUrl = vodPlayUrl,
            typeId = typeId,
            typeName = typeName,
            siteKey = siteKey
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建简单视频项目
         */
        fun simple(
            vodId: String,
            vodName: String,
            vodPic: String = "",
            vodRemarks: String = ""
        ): AppYsVideoItem {
            return AppYsVideoItem(
                vodId = vodId,
                vodName = vodName,
                vodPic = vodPic,
                vodRemarks = vodRemarks
            )
        }
        
        /**
         * 🏭 从标准 VodItem 创建
         */
        fun fromVodItem(vodItem: VodItem): AppYsVideoItem {
            return AppYsVideoItem(
                vodId = vodItem.vodId,
                vodName = vodItem.vodName,
                vodPic = vodItem.vodPic,
                vodRemarks = vodItem.vodRemarks,
                vodYear = vodItem.vodYear,
                vodArea = vodItem.vodArea,
                vodDirector = vodItem.vodDirector,
                vodActor = vodItem.vodActor,
                vodContent = vodItem.vodContent,
                vodPlayFrom = vodItem.vodPlayFrom,
                vodPlayUrl = vodItem.vodPlayUrl,
                typeId = vodItem.typeId,
                typeName = vodItem.typeName
            )
        }
    }
}

/**
 * AppYs 分类
 */
@Serializable
data class AppYsCategory(
    @SerialName("type_id") val typeId: String,         // 分类 ID
    @SerialName("type_name") val typeName: String,     // 分类名称
    @SerialName("type_flag") val typeFlag: String = "1" // 分类标识
) {
    
    /**
     * 🔍 是否启用
     */
    fun isEnabled(): Boolean = typeFlag == "1"
    
    /**
     * 🎬 转换为标准 VodClass
     */
    fun toVodClass(): VodClass {
        return VodClass(
            typeId = typeId,
            typeName = typeName,
            typeFlag = typeFlag
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建分类
         */
        fun create(
            typeId: String,
            typeName: String,
            typeFlag: String = "1"
        ): AppYsCategory {
            return AppYsCategory(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag
            )
        }
        
        /**
         * 🏭 从标准 VodClass 创建
         */
        fun fromVodClass(vodClass: VodClass): AppYsCategory {
            return AppYsCategory(
                typeId = vodClass.typeId,
                typeName = vodClass.typeName,
                typeFlag = vodClass.typeFlag
            )
        }
    }
}
