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

/**
 * 筛选条件
 */
@Serializable
data class VodFilter(
    val key: String, // 筛选key
    val name: String, // 筛选名称
    val value: List<VodFilterValue> = emptyList() // 筛选值列表
) {
    /**
     * 根据值获取筛选项
     */
    fun getValue(v: String): VodFilterValue? {
        return value.find { it.v == v }
    }
    
    /**
     * 获取默认值
     */
    fun getDefaultValue(): VodFilterValue? {
        return value.firstOrNull()
    }
    
    /**
     * 是否有筛选值
     */
    fun hasValues(): Boolean = value.isNotEmpty()
}

/**
 * 筛选值
 */
@Serializable
data class VodFilterValue(
    val n: String, // 显示名称
    val v: String  // 实际值
) {
    /**
     * 是否为默认值（通常空值或"全部"）
     */
    fun isDefault(): Boolean = v.isEmpty() || v == "0" || n.contains("全部")
}
