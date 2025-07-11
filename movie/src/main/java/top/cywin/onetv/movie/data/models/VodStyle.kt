package top.cywin.onetv.movie.data.models

import kotlinx.serialization.Serializable

/**
 * 显示样式配置 (完全兼容TVBOX标准)
 */
@Serializable
data class VodStyle(
    val type: String = "rect", // 显示类型: "rect"=矩形, "list"=列表 (TVBOX标准)
    val ratio: Float = 0.75f, // 宽高比
    val land: Int = 0, // 横屏显示
    val circle: Int = 0 // 圆形显示
) {
    companion object {
        /**
         * 矩形样式 (TVBOX标准)
         */
        fun rect(ratio: Float = 0.75f): VodStyle {
            return VodStyle(type = "rect", ratio = ratio)
        }

        /**
         * 椭圆样式
         */
        fun oval(ratio: Float = 1.0f): VodStyle {
            return VodStyle(type = "rect", ratio = ratio, circle = 1)
        }

        /**
         * 列表样式 (TVBOX标准)
         */
        fun list(): VodStyle {
            return VodStyle(type = "list", ratio = 0.75f)
        }

        /**
         * 横屏样式
         */
        fun landscape(ratio: Float = 1.78f): VodStyle {
            return VodStyle(type = "rect", ratio = ratio, land = 1)
        }
    }
    
    /**
     * 是否为矩形显示 (TVBOX标准)
     */
    fun isRect(): Boolean = type == "rect"

    /**
     * 是否为椭圆显示
     */
    fun isOval(): Boolean = type == "rect" && circle == 1

    /**
     * 是否为列表显示 (TVBOX标准)
     */
    fun isList(): Boolean = type == "list"
    
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
            "rect" -> if (isOval()) "椭圆" else "矩形"
            "list" -> "列表"
            else -> "未知($type)"
        }

        val orientationDesc = if (isLandscape()) "横屏" else "竖屏"
        val shapeDesc = if (isCircle()) "圆形" else "方形"

        return "$typeDesc | $orientationDesc | $shapeDesc | 比例:${String.format("%.2f", ratio)}"
    }
}
