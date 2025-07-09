package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 播放线路 (参考OneMoVie Flag + Episode)
 */
@Serializable
data class VodFlag(
    val flag: String, // 线路名称
    val urls: String, // 播放地址字符串
    private var episodes: List<VodEpisode> = emptyList(),
    var activated: Boolean = false,
    var position: Int = -1
) {
    
    /**
     * 创建剧集列表 (参考OneMoVie createEpisode)
     */
    fun createEpisodes(): List<VodEpisode> {
        if (episodes.isNotEmpty()) return episodes
        
        val urlArray = if (urls.contains("#")) urls.split("#") else listOf(urls)
        episodes = urlArray.mapIndexed { index, episodeUrl ->
            val parts = episodeUrl.split("$")
            val number = String.format("%02d", index + 1)
            
            VodEpisode(
                name = if (parts.size > 1 && parts[0].isNotEmpty()) {
                    parts[0].trim()
                } else {
                    "第${number}集"
                },
                url = if (parts.size > 1) parts[1] else episodeUrl,
                index = index
            )
        }.filter { it.url.isNotEmpty() }
        
        return episodes
    }
    
    /**
     * 设置激活状态 (参考OneMoVie setActivated)
     */
    fun setActivated(item: VodFlag) {
        activated = item == this
        if (activated) {
            item.episodes = episodes
        }
    }
    
    /**
     * 获取剧集数量
     */
    fun getEpisodeCount(): Int {
        return createEpisodes().size
    }
    
    /**
     * 根据索引获取剧集
     */
    fun getEpisode(index: Int): VodEpisode? {
        val eps = createEpisodes()
        return if (index in eps.indices) eps[index] else null
    }
    
    /**
     * 根据名称查找剧集
     */
    fun findEpisodeByName(name: String): VodEpisode? {
        return createEpisodes().find { it.name == name }
    }
    
    /**
     * 获取当前播放的剧集
     */
    fun getCurrentEpisode(): VodEpisode? {
        return if (position >= 0) getEpisode(position) else null
    }
    
    /**
     * 设置当前播放位置
     */
    fun setCurrentPosition(pos: Int) {
        val eps = createEpisodes()
        if (pos in eps.indices) {
            position = pos
            // 更新剧集激活状态
            eps.forEachIndexed { index, episode ->
                episode.setActivated(index == pos)
            }
        }
    }
    
    /**
     * 是否有下一集
     */
    fun hasNext(): Boolean {
        return position < getEpisodeCount() - 1
    }
    
    /**
     * 是否有上一集
     */
    fun hasPrevious(): Boolean {
        return position > 0
    }
    
    /**
     * 获取下一集
     */
    fun getNextEpisode(): VodEpisode? {
        return if (hasNext()) getEpisode(position + 1) else null
    }
    
    /**
     * 获取上一集
     */
    fun getPreviousEpisode(): VodEpisode? {
        return if (hasPrevious()) getEpisode(position - 1) else null
    }
    
    /**
     * 是否为单集内容
     */
    fun isSingleEpisode(): Boolean {
        return getEpisodeCount() <= 1
    }
    
    /**
     * 获取线路摘要信息
     */
    fun getSummary(): String {
        val count = getEpisodeCount()
        return if (count > 1) {
            "$flag (共${count}集)"
        } else {
            flag
        }
    }
}

/**
 * 剧集信息 (参考OneMoVie Episode)
 */
@Serializable
data class VodEpisode(
    val name: String,
    val url: String,
    val index: Int = 0,
    val desc: String = "",
    var activated: Boolean = false,
    var selected: Boolean = false
) {
    
    /**
     * 获取剧集编号 (参考OneMoVie getNumber)
     */
    fun getNumber(): Int {
        return Regex("\\d+").find(name)?.value?.toIntOrNull() ?: (index + 1)
    }
    
    /**
     * 设置激活状态
     */
    fun setActivated(activated: Boolean) {
        this.activated = activated
        this.selected = activated
    }
    
    /**
     * 规则匹配 (参考OneMoVie rule1, rule2)
     */
    fun matchByName(targetName: String): Boolean {
        return name.equals(targetName, ignoreCase = true)
    }
    
    fun matchByNumber(targetNumber: Int): Boolean {
        return getNumber() == targetNumber
    }
    
    /**
     * 是否为有效的播放地址
     */
    fun hasValidUrl(): Boolean {
        return url.isNotEmpty() && !url.startsWith("#")
    }
    
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return if (name.isNotEmpty()) name else "第${index + 1}集"
    }
    
    /**
     * 获取剧集类型（根据名称判断）
     */
    fun getEpisodeType(): String {
        return when {
            name.contains("预告") -> "预告"
            name.contains("花絮") -> "花絮"
            name.contains("特辑") -> "特辑"
            name.contains("番外") -> "番外"
            else -> "正片"
        }
    }
}
