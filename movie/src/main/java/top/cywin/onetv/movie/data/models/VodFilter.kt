package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 筛选条件 (参考OneMoVie Filter)
 */
@Serializable
data class VodFilter(
    val key: String,
    val name: String,
    val value: List<VodFilterValue>
) {
    /**
     * 根据值获取显示名称
     */
    fun getDisplayName(value: String): String {
        return this.value.find { it.v == value }?.n ?: value
    }
    
    /**
     * 获取默认值
     */
    fun getDefaultValue(): VodFilterValue? {
        return value.firstOrNull()
    }
    
    /**
     * 是否有多个选项
     */
    fun hasMultipleOptions(): Boolean {
        return value.size > 1
    }
    
    /**
     * 根据显示名称查找值
     */
    fun findValueByName(name: String): VodFilterValue? {
        return value.find { it.n == name }
    }
}

/**
 * 筛选条件值
 */
@Serializable
data class VodFilterValue(
    val n: String, // 显示名称
    val v: String  // 值
) {
    /**
     * 是否为默认值（通常为空或"全部"）
     */
    fun isDefault(): Boolean {
        return v.isEmpty() || n.contains("全部") || n.contains("不限")
    }
}
