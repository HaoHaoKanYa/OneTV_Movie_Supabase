package top.cywin.onetv.movie.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 分类信息 (参考OneMoVie Class，支持筛选配置)
 */
@Serializable
data class VodClass(
    @SerialName("type_id") val typeId: String,
    @SerialName("type_name") val typeName: String,
    @SerialName("type_flag") val typeFlag: String = "1",
    val filters: List<VodFilter> = emptyList(), // 筛选条件
    val land: Int = 0, // 横屏显示
    val circle: Int = 0, // 圆形显示
    val ratio: Float = 0f // 宽高比
) {
    /**
     * 是否启用该分类
     */
    fun isEnabled(): Boolean = typeFlag == "1"
    
    /**
     * 获取筛选条件
     */
    fun getFilters(): List<VodFilter> = filters
    
    /**
     * 是否有筛选条件
     */
    fun hasFilters(): Boolean = filters.isNotEmpty()
    
    /**
     * 是否横屏显示
     */
    fun isLandscape(): Boolean = land == 1
    
    /**
     * 是否圆形显示
     */
    fun isCircle(): Boolean = circle == 1
    
    /**
     * 获取显示比例
     */
    fun getDisplayRatio(): Float {
        return if (ratio > 0) ratio else 0.75f // 默认3:4比例
    }
    
    /**
     * 根据key获取筛选器
     */
    fun getFilter(key: String): VodFilter? {
        return filters.find { it.key == key }
    }
    
    /**
     * 获取所有筛选器的key
     */
    fun getFilterKeys(): List<String> {
        return filters.map { it.key }
    }
}


