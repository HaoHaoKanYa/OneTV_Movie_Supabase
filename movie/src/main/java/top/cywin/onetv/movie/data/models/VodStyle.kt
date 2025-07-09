package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 显示样式配置
 */
@Serializable
data class VodStyle(
    val type: Int = 0, // 显示类型: 0=矩形, 1=椭圆, 2=列表
    val ratio: Float = 0.75f, // 宽高比
    val land: Int = 0, // 横屏显示
    val circle: Int = 0 // 圆形显示
) {
    companion object {
        /**
         * 矩形样式
         */
        fun rect(ratio: Float = 0.75f): VodStyle {
            return VodStyle(type = 0, ratio = ratio)
        }
        
        /**
         * 椭圆样式
         */
        fun oval(ratio: Float = 1.0f): VodStyle {
            return VodStyle(type = 1, ratio = ratio, circle = 1)
        }
        
        /**
         * 列表样式
         */
        fun list(): VodStyle {
            return VodStyle(type = 2, ratio = 0.75f)
        }
        
        /**
         * 横屏样式
         */
        fun landscape(ratio: Float = 1.78f): VodStyle {
            return VodStyle(type = 0, ratio = ratio, land = 1)
        }
    }
    
    /**
     * 是否为矩形显示
     */
    fun isRect(): Boolean = type == 0
    
    /**
     * 是否为椭圆显示
     */
    fun isOval(): Boolean = type == 1
    
    /**
     * 是否为列表显示
     */
    fun isList(): Boolean = type == 2
    
    /**
     * 是否为横屏显示
     */
    fun isLandscape(): Boolean = land == 1
    
    /**
     * 是否为圆形显示
     */
    fun isCircle(): Boolean = circle == 1
    
    /**
     * 获取显示比例
     */
    fun getDisplayRatio(): Float {
        return if (ratio > 0) ratio else 0.75f
    }
    
    /**
     * 获取样式描述
     */
    fun getDescription(): String {
        val typeDesc = when (type) {
            0 -> "矩形"
            1 -> "椭圆"
            2 -> "列表"
            else -> "未知"
        }
        
        val orientationDesc = if (isLandscape()) "横屏" else "竖屏"
        val shapeDesc = if (isCircle()) "圆形" else "方形"
        
        return "$typeDesc | $orientationDesc | $shapeDesc | 比例:${String.format("%.2f", ratio)}"
    }
}
