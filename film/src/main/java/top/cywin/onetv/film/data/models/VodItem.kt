package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VOD 项目数据模型
 * 
 * 基于 FongMi/TV 的标准 VOD 项目格式
 * 兼容 TVBOX 标准 API 响应
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class VodItem(
    @SerialName("vod_id") val vodId: String,                    // VOD ID
    @SerialName("vod_name") val vodName: String,                // VOD 名称
    @SerialName("vod_pic") val vodPic: String = "",             // VOD 图片
    @SerialName("vod_remarks") val vodRemarks: String = "",     // VOD 备注
    @SerialName("vod_year") val vodYear: String = "",           // 年份
    @SerialName("vod_area") val vodArea: String = "",           // 地区
    @SerialName("vod_director") val vodDirector: String = "",   // 导演
    @SerialName("vod_actor") val vodActor: String = "",         // 演员
    @SerialName("vod_lang") val vodLang: String = "",           // 语言
    @SerialName("vod_content") val vodContent: String = "",     // 内容简介
    @SerialName("vod_play_from") val vodPlayFrom: String = "",  // 播放来源
    @SerialName("vod_play_url") val vodPlayUrl: String = "",    // 播放地址
    @SerialName("vod_download_from") val vodDownloadFrom: String = "", // 下载来源
    @SerialName("vod_download_url") val vodDownloadUrl: String = "",   // 下载地址
    @SerialName("vod_tag") val vodTag: String = "",             // 标签
    @SerialName("vod_class") val vodClass: String = "",         // 分类
    @SerialName("vod_score") val vodScore: String = "",         // 评分
    @SerialName("vod_score_all") val vodScoreAll: String = "",  // 总评分
    @SerialName("vod_score_num") val vodScoreNum: String = "",  // 评分人数
    @SerialName("vod_time") val vodTime: String = "",           // 时长
    @SerialName("vod_time_add") val vodTimeAdd: String = "",    // 添加时间
    @SerialName("vod_time_hits") val vodTimeHits: String = "",  // 点击时间
    @SerialName("vod_time_hits_day") val vodTimeHitsDay: String = "",   // 日点击
    @SerialName("vod_time_hits_week") val vodTimeHitsWeek: String = "", // 周点击
    @SerialName("vod_time_hits_month") val vodTimeHitsMonth: String = "", // 月点击
    @SerialName("vod_hits") val vodHits: String = "",           // 总点击
    @SerialName("vod_hits_day") val vodHitsDay: String = "",    // 日点击数
    @SerialName("vod_hits_week") val vodHitsWeek: String = "",  // 周点击数
    @SerialName("vod_hits_month") val vodHitsMonth: String = "", // 月点击数
    @SerialName("vod_up") val vodUp: String = "",               // 顶
    @SerialName("vod_down") val vodDown: String = "",           // 踩
    @SerialName("vod_level") val vodLevel: String = "",         // 等级
    @SerialName("vod_lock") val vodLock: String = "",           // 锁定
    @SerialName("vod_points") val vodPoints: String = "",       // 积分
    @SerialName("vod_points_play") val vodPointsPlay: String = "", // 播放积分
    @SerialName("vod_points_down") val vodPointsDown: String = "", // 下载积分
    @SerialName("vod_isend") val vodIsend: String = "",         // 是否完结
    @SerialName("vod_copyright") val vodCopyright: String = "", // 版权
    @SerialName("vod_jumpurl") val vodJumpurl: String = "",     // 跳转地址
    @SerialName("vod_tpl") val vodTpl: String = "",             // 模板
    @SerialName("vod_tpl_play") val vodTplPlay: String = "",    // 播放模板
    @SerialName("vod_tpl_down") val vodTplDown: String = "",    // 下载模板
    @SerialName("vod_isunion") val vodIsunion: String = "",     // 是否联盟
    @SerialName("vod_trailer") val vodTrailer: String = "",     // 预告片
    @SerialName("vod_serial") val vodSerial: String = "",       // 连载
    @SerialName("vod_tv") val vodTv: String = "",               // 电视台
    @SerialName("vod_weekday") val vodWeekday: String = "",     // 星期
    @SerialName("vod_release") val vodRelease: String = "",     // 发布
    @SerialName("vod_douban") val vodDouban: String = "",       // 豆瓣
    @SerialName("vod_imdb") val vodImdb: String = "",           // IMDB
    @SerialName("vod_tvs") val vodTvs: String = "",             // 电视台列表
    @SerialName("vod_version") val vodVersion: String = "",     // 版本
    @SerialName("vod_season_count") val vodSeasonCount: String = "", // 季数
    @SerialName("vod_episode_count") val vodEpisodeCount: String = "", // 集数
    @SerialName("vod_duration") val vodDuration: String = "",   // 时长
    @SerialName("vod_status") val vodStatus: String = "",       // 状态
    @SerialName("vod_subtitle") val vodSubtitle: String = "",   // 字幕
    @SerialName("vod_blurb") val vodBlurb: String = "",         // 简介
    @SerialName("vod_pic_thumb") val vodPicThumb: String = "",  // 缩略图
    @SerialName("vod_pic_slide") val vodPicSlide: String = "",  // 轮播图
    @SerialName("vod_pic_screenshot") val vodPicScreenshot: String = "", // 截图
    @SerialName("vod_pic_logo") val vodPicLogo: String = "",    // Logo
    @SerialName("type_id") val typeId: String = "",             // 分类ID
    @SerialName("type_name") val typeName: String = "",         // 分类名称
    
    // 扩展字段
    val siteKey: String = "",                                   // 站点标识
    val siteName: String = "",                                  // 站点名称
    val createTime: Long = System.currentTimeMillis(),         // 创建时间
    val updateTime: Long = System.currentTimeMillis()          // 更新时间
) {
    
    /**
     * 🎬 获取播放列表
     */
    fun getPlayList(): List<VodPlayGroup> {
        if (vodPlayFrom.isEmpty() || vodPlayUrl.isEmpty()) {
            return emptyList()
        }
        
        val fromList = vodPlayFrom.split("$$$")
        val urlList = vodPlayUrl.split("$$$")
        
        return fromList.mapIndexed { index, from ->
            val urls = if (index < urlList.size) urlList[index] else ""
            val episodes = urls.split("#").mapNotNull { episode ->
                val parts = episode.split("$")
                if (parts.size >= 2) {
                    VodPlayEpisode(parts[0], parts[1])
                } else {
                    null
                }
            }
            VodPlayGroup(from, episodes)
        }
    }
    
    /**
     * 📥 获取下载列表
     */
    fun getDownloadList(): List<VodDownloadGroup> {
        if (vodDownloadFrom.isEmpty() || vodDownloadUrl.isEmpty()) {
            return emptyList()
        }
        
        val fromList = vodDownloadFrom.split("$$$")
        val urlList = vodDownloadUrl.split("$$$")
        
        return fromList.mapIndexed { index, from ->
            val urls = if (index < urlList.size) urlList[index] else ""
            val episodes = urls.split("#").mapNotNull { episode ->
                val parts = episode.split("$")
                if (parts.size >= 2) {
                    VodDownloadEpisode(parts[0], parts[1])
                } else {
                    null
                }
            }
            VodDownloadGroup(from, episodes)
        }
    }
    
    /**
     * 🔍 是否完结
     */
    fun isCompleted(): Boolean = vodIsend == "1"
    
    /**
     * 🔍 是否有播放源
     */
    fun hasPlaySource(): Boolean = vodPlayFrom.isNotEmpty() && vodPlayUrl.isNotEmpty()
    
    /**
     * 🔍 是否有下载源
     */
    fun hasDownloadSource(): Boolean = vodDownloadFrom.isNotEmpty() && vodDownloadUrl.isNotEmpty()
    
    /**
     * 🔍 获取评分数值
     */
    fun getScoreValue(): Float {
        return try {
            vodScore.toFloatOrNull() ?: 0.0f
        } catch (e: Exception) {
            0.0f
        }
    }
    
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
     * 📊 获取VOD摘要
     */
    fun getSummary(): Map<String, Any> {
        return mapOf(
            "vod_id" to vodId,
            "vod_name" to vodName,
            "vod_pic" to vodPic,
            "vod_remarks" to vodRemarks,
            "vod_year" to vodYear,
            "vod_area" to vodArea,
            "vod_director" to vodDirector,
            "vod_actor" to vodActor,
            "vod_score" to vodScore,
            "type_name" to typeName,
            "site_name" to siteName,
            "play_groups" to getPlayList().size,
            "download_groups" to getDownloadList().size,
            "has_content" to vodContent.isNotEmpty(),
            "is_completed" to isCompleted(),
            "has_play_source" to hasPlaySource(),
            "has_download_source" to hasDownloadSource(),
            "create_time" to createTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建简单的VOD项目
         */
        fun simple(
            vodId: String,
            vodName: String,
            vodPic: String = "",
            vodRemarks: String = "",
            siteKey: String = ""
        ): VodItem {
            return VodItem(
                vodId = vodId,
                vodName = vodName,
                vodPic = vodPic,
                vodRemarks = vodRemarks,
                siteKey = siteKey
            )
        }
        
        /**
         * 🏭 创建详细的VOD项目
         */
        fun detailed(
            vodId: String,
            vodName: String,
            vodPic: String = "",
            vodContent: String = "",
            vodPlayFrom: String = "",
            vodPlayUrl: String = "",
            typeId: String = "",
            typeName: String = "",
            siteKey: String = ""
        ): VodItem {
            return VodItem(
                vodId = vodId,
                vodName = vodName,
                vodPic = vodPic,
                vodContent = vodContent,
                vodPlayFrom = vodPlayFrom,
                vodPlayUrl = vodPlayUrl,
                typeId = typeId,
                typeName = typeName,
                siteKey = siteKey
            )
        }
    }
}

/**
 * VOD 播放组
 */
@Serializable
data class VodPlayGroup(
    val name: String,                           // 播放源名称
    val episodes: List<VodPlayEpisode>          // 播放集数列表
)

/**
 * VOD 播放集数
 */
@Serializable
data class VodPlayEpisode(
    val name: String,                           // 集数名称
    val url: String                             // 播放地址
)

/**
 * VOD 下载组
 */
@Serializable
data class VodDownloadGroup(
    val name: String,                           // 下载源名称
    val episodes: List<VodDownloadEpisode>      // 下载集数列表
)

/**
 * VOD 下载集数
 */
@Serializable
data class VodDownloadEpisode(
    val name: String,                           // 集数名称
    val url: String                             // 下载地址
)
