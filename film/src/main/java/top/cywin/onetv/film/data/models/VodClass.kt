package top.cywin.onetv.film.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VOD 分类数据模型
 * 
 * 基于 FongMi/TV 的标准分类格式
 * 兼容 TVBOX 标准分类响应
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
@Serializable
data class VodClass(
    @SerialName("type_id") val typeId: String,         // 分类 ID
    @SerialName("type_name") val typeName: String,     // 分类名称
    @SerialName("type_flag") val typeFlag: String = "1", // 分类标识 (1=启用, 0=禁用)
    val filters: List<VodFilter> = emptyList(),        // 筛选条件
    val land: Int = 0,                                 // 横屏显示 (0=否, 1=是)
    val circle: Int = 0,                               // 圆形显示 (0=否, 1=是)
    val ratio: Float = 0f,                             // 宽高比
    
    // 扩展字段
    val style: VodClassStyle? = null,                  // 样式配置
    val sortOrder: Int = 0,                            // 排序顺序
    val description: String = "",                      // 分类描述
    val icon: String = "",                             // 分类图标
    val createTime: Long = System.currentTimeMillis(), // 创建时间
    val updateTime: Long = System.currentTimeMillis()  // 更新时间
) {
    
    /**
     * 🔍 是否启用该分类
     */
    fun isEnabled(): Boolean = typeFlag == "1"
    
    /**
     * 🔍 是否有筛选条件
     */
    fun hasFilters(): Boolean = filters.isNotEmpty()
    
    /**
     * 🔍 是否横屏显示
     */
    fun isLandscape(): Boolean = land == 1
    
    /**
     * 🔍 是否圆形显示
     */
    fun isCircle(): Boolean = circle == 1
    
    /**
     * 🔍 获取显示比例
     */
    fun getDisplayRatio(): Float {
        return if (ratio > 0) ratio else 0.75f // 默认 3:4 比例
    }
    
    /**
     * 🔍 根据 key 查找筛选条件
     */
    fun findFilter(key: String): VodFilter? {
        return filters.find { it.key == key }
    }
    
    /**
     * 🔍 获取样式配置
     */
    fun getStyle(): VodClassStyle {
        return style ?: VodClassStyle.default()
    }
    
    /**
     * 📊 获取统计信息
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "type_id" to typeId,
            "type_name" to typeName,
            "is_enabled" to isEnabled(),
            "has_filters" to hasFilters(),
            "filter_count" to filters.size,
            "is_landscape" to isLandscape(),
            "is_circle" to isCircle(),
            "display_ratio" to getDisplayRatio(),
            "sort_order" to sortOrder,
            "create_time" to createTime
        )
    }
    
    companion object {
        
        /**
         * 🏭 创建简单分类
         */
        fun simple(
            typeId: String,
            typeName: String,
            typeFlag: String = "1"
        ): VodClass {
            return VodClass(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag
            )
        }
        
        /**
         * 🏭 创建带筛选的分类
         */
        fun withFilters(
            typeId: String,
            typeName: String,
            filters: List<VodFilter>,
            typeFlag: String = "1"
        ): VodClass {
            return VodClass(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag,
                filters = filters
            )
        }
        
        /**
         * 🏭 创建带样式的分类
         */
        fun withStyle(
            typeId: String,
            typeName: String,
            style: VodClassStyle,
            typeFlag: String = "1"
        ): VodClass {
            return VodClass(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag,
                style = style
            )
        }
        
        /**
         * 🏭 创建横屏分类
         */
        fun landscape(
            typeId: String,
            typeName: String,
            ratio: Float = 1.33f,
            typeFlag: String = "1"
        ): VodClass {
            return VodClass(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag,
                land = 1,
                ratio = ratio
            )
        }
        
        /**
         * 🏭 创建圆形分类
         */
        fun circle(
            typeId: String,
            typeName: String,
            typeFlag: String = "1"
        ): VodClass {
            return VodClass(
                typeId = typeId,
                typeName = typeName,
                typeFlag = typeFlag,
                circle = 1,
                ratio = 1.0f
            )
        }
    }
}

/**
 * VOD 分类样式配置
 */
@Serializable
data class VodClassStyle(
    val type: String = "rect",                         // 样式类型 (rect=矩形, oval=椭圆, list=列表)
    val ratio: Float = 0.75f,                          // 宽高比
    val backgroundColor: String = "",                   // 背景颜色
    val textColor: String = "",                        // 文字颜色
    val borderColor: String = "",                      // 边框颜色
    val borderWidth: Float = 0f,                       // 边框宽度
    val cornerRadius: Float = 0f,                      // 圆角半径
    val padding: VodClassPadding? = null,              // 内边距
    val margin: VodClassMargin? = null                 // 外边距
) {
    
    /**
     * 🔍 是否为矩形样式
     */
    fun isRect(): Boolean = type == "rect"
    
    /**
     * 🔍 是否为椭圆样式
     */
    fun isOval(): Boolean = type == "oval"
    
    /**
     * 🔍 是否为列表样式
     */
    fun isList(): Boolean = type == "list"
    
    companion object {
        
        /**
         * 🏭 创建默认样式
         */
        fun default(): VodClassStyle {
            return VodClassStyle(
                type = "rect",
                ratio = 0.75f
            )
        }
        
        /**
         * 🏭 创建矩形样式
         */
        fun rect(ratio: Float = 0.75f): VodClassStyle {
            return VodClassStyle(
                type = "rect",
                ratio = ratio
            )
        }
        
        /**
         * 🏭 创建椭圆样式
         */
        fun oval(ratio: Float = 1.0f): VodClassStyle {
            return VodClassStyle(
                type = "oval",
                ratio = ratio
            )
        }
        
        /**
         * 🏭 创建列表样式
         */
        fun list(): VodClassStyle {
            return VodClassStyle(
                type = "list",
                ratio = 1.0f
            )
        }
    }
}

/**
 * VOD 分类内边距
 */
@Serializable
data class VodClassPadding(
    val top: Float = 0f,                               // 上边距
    val right: Float = 0f,                             // 右边距
    val bottom: Float = 0f,                            // 下边距
    val left: Float = 0f                               // 左边距
)

/**
 * VOD 分类外边距
 */
@Serializable
data class VodClassMargin(
    val top: Float = 0f,                               // 上边距
    val right: Float = 0f,                             // 右边距
    val bottom: Float = 0f,                            // 下边距
    val left: Float = 0f                               // 左边距
)

/**
 * VOD 筛选条件
 */
@Serializable
data class VodFilter(
    val key: String,                                   // 筛选键
    val name: String,                                  // 筛选名称
    val value: List<VodFilterValue> = emptyList()      // 筛选值列表
) {
    
    /**
     * 🔍 是否有筛选值
     */
    fun hasValues(): Boolean = value.isNotEmpty()
    
    /**
     * 🔍 根据值查找筛选项
     */
    fun findValue(v: String): VodFilterValue? {
        return value.find { it.v == v }
    }
    
    companion object {
        
        /**
         * 🏭 创建筛选条件
         */
        fun create(
            key: String,
            name: String,
            values: List<VodFilterValue> = emptyList()
        ): VodFilter {
            return VodFilter(
                key = key,
                name = name,
                value = values
            )
        }
    }
}

/**
 * VOD 筛选值
 */
@Serializable
data class VodFilterValue(
    val n: String,                                     // 显示名称
    val v: String                                      // 实际值
) {
    
    companion object {
        
        /**
         * 🏭 创建筛选值
         */
        fun create(name: String, value: String): VodFilterValue {
            return VodFilterValue(n = name, v = value)
        }
    }
}
