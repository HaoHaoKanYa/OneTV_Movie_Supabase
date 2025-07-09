package top.cywin.onetv.movie.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 点播内容项 (参考OneMoVie Vod)
 */
@Serializable
data class VodItem(
    @SerialName("vod_id") val vodId: String,
    @SerialName("vod_name") val vodName: String,
    @SerialName("vod_pic") val vodPic: String = "",
    @SerialName("vod_remarks") val vodRemarks: String = "",
    @SerialName("vod_year") val vodYear: String = "",
    @SerialName("vod_area") val vodArea: String = "",
    @SerialName("vod_director") val vodDirector: String = "",
    @SerialName("vod_actor") val vodActor: String = "",
    @SerialName("vod_content") val vodContent: String = "",
    @SerialName("vod_play_from") val vodPlayFrom: String = "",
    @SerialName("vod_play_url") val vodPlayUrl: String = "",
    @SerialName("type_id") val typeId: String = "",
    @SerialName("type_name") val typeName: String = "",
    @SerialName("vod_time") val vodTime: String = "",
    @SerialName("vod_score") val vodScore: String = "",
    @SerialName("vod_tag") val vodTag: String = "",
    @SerialName("vod_class") val vodClass: String = "",
    @SerialName("vod_lang") val vodLang: String = "",
    @SerialName("vod_douban_id") val vodDoubanId: String = "",
    @SerialName("vod_douban_score") val vodDoubanScore: String = "",
    val siteKey: String = "" // 所属站点key
) {
    /**
     * 获取年份整数
     */
    fun getYearInt(): Int {
        return vodYear.toIntOrNull() ?: 0
    }
    
    /**
     * 获取导演列表
     */
    fun getDirectorList(): List<String> {
        return vodDirector.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * 获取演员列表
     */
    fun getActorList(): List<String> {
        return vodActor.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * 获取地区列表
     */
    fun getAreaList(): List<String> {
        return vodArea.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * 解析播放线路
     */
    fun parseFlags(): List<VodFlag> {
        if (vodPlayFrom.isEmpty() || vodPlayUrl.isEmpty()) return emptyList()
        
        val fromArray = vodPlayFrom.split("$$$")
        val urlArray = vodPlayUrl.split("$$$")
        
        val flags = mutableListOf<VodFlag>()
        
        for (i in fromArray.indices) {
            if (i < urlArray.size) {
                flags.add(
                    VodFlag(
                        flag = fromArray[i].trim(),
                        urls = urlArray[i].trim()
                    )
                )
            }
        }
        
        return flags
    }
    
    /**
     * 是否有播放地址
     */
    fun hasPlayUrl(): Boolean = vodPlayUrl.isNotEmpty()
    
    /**
     * 是否有多个播放线路
     */
    fun hasMultipleFlags(): Boolean {
        return vodPlayFrom.contains("$$$") || vodPlayUrl.contains("$$$")
    }
    
    /**
     * 获取第一个播放线路
     */
    fun getFirstFlag(): VodFlag? {
        return parseFlags().firstOrNull()
    }
    
    /**
     * 是否为电影类型
     */
    fun isMovie(): Boolean {
        return typeId == "1" || typeName.contains("电影")
    }
    
    /**
     * 是否为电视剧类型
     */
    fun isTvSeries(): Boolean {
        return typeId == "2" || typeName.contains("电视剧") || typeName.contains("剧集")
    }
    
    /**
     * 是否为综艺类型
     */
    fun isVariety(): Boolean {
        return typeId == "3" || typeName.contains("综艺")
    }
    
    /**
     * 是否为动漫类型
     */
    fun isAnime(): Boolean {
        return typeId == "4" || typeName.contains("动漫") || typeName.contains("动画")
    }
    
    /**
     * 获取内容摘要
     */
    fun getContentSummary(maxLength: Int = 100): String {
        return if (vodContent.length > maxLength) {
            "${vodContent.take(maxLength)}..."
        } else {
            vodContent
        }
    }
    
    /**
     * 获取显示标题（包含年份和地区）
     */
    fun getDisplayTitle(): String {
        val parts = mutableListOf<String>()
        parts.add(vodName)
        
        if (vodYear.isNotEmpty()) {
            parts.add("($vodYear)")
        }
        
        if (vodArea.isNotEmpty()) {
            parts.add("[$vodArea]")
        }
        
        return parts.joinToString(" ")
    }

    /**
     * 获取分类列表
     */
    fun getCategories(): List<String> {
        return vodClass.split(",").filter { it.isNotEmpty() }
    }

    /**
     * 获取标签列表
     */
    fun getTagList(): List<String> {
        return vodTag.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 获取语言列表
     */
    fun getLanguageList(): List<String> {
        return vodLang.split(",", "，", "/", "、")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * 获取评分浮点数
     */
    fun getScoreFloat(): Float {
        return vodScore.toFloatOrNull() ?: 0f
    }

    /**
     * 获取豆瓣评分浮点数
     */
    fun getDoubanScoreFloat(): Float {
        return vodDoubanScore.toFloatOrNull() ?: 0f
    }

    /**
     * 获取播放源列表
     */
    fun getPlaySources(): List<VodFlag> {
        return parseFlags()
    }
}
