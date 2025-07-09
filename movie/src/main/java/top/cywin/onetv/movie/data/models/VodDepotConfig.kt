package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 配置仓库项 (参考OneMoVie Depot架构)
 */
@Serializable
data class VodDepotConfig(
    val name: String,
    val url: String,
    val desc: String = ""
)

/**
 * 配置仓库 (参考OneMoVie Depot)
 */
@Serializable
data class VodDepot(
    val name: String,
    val url: String,
    val configs: List<VodDepotConfig> = emptyList()
)
