package top.cywin.onetv.movie.ui.base

/**
 * Movie模块专用的ViewType系统 - Compose UI版本
 * 与FongMi_TV数据层兼容，避免与主TV应用UI冲突
 */
enum class MovieViewType(val value: Int) {
    RECT(0),    // 矩形海报布局 - 标准电影海报
    OVAL(1),    // 圆形头像布局 - 演员头像等
    LIST(2),    // 列表布局 - 紧凑信息显示
    GRID(3);    // 网格布局 - 密集网格显示
    
    companion object {
        /**
         * 从整数值创建ViewType - 兼容FongMi_TV
         */
        fun fromInt(value: Int): MovieViewType = when(value) {
            0 -> RECT
            1 -> OVAL
            2 -> LIST
            3 -> GRID
            else -> RECT // 默认值
        }
        
        /**
         * 转换为整数值 - 兼容FongMi_TV数据层
         */
        fun toInt(viewType: MovieViewType): Int = viewType.value
        
        /**
         * 从字符串类型创建ViewType - 兼容Style.java
         */
        fun fromString(type: String): MovieViewType = when(type.lowercase()) {
            "oval" -> OVAL
            "list" -> LIST
            "grid" -> GRID
            "rect" -> RECT
            else -> RECT // 默认值
        }
    }
}

/**
 * 布局方向枚举
 */
enum class MovieLayoutDirection(val value: Int) {
    HORIZONTAL(0),  // 水平方向
    VERTICAL(1);    // 垂直方向
    
    companion object {
        fun fromInt(value: Int): MovieLayoutDirection = when(value) {
            0 -> HORIZONTAL
            1 -> VERTICAL
            else -> HORIZONTAL
        }
    }
}

/**
 * 主题类型枚举
 */
enum class MovieThemeType(val value: Int) {
    DARK(0),    // 深色主题
    LIGHT(1);   // 浅色主题
    
    companion object {
        fun fromInt(value: Int): MovieThemeType = when(value) {
            0 -> DARK
            1 -> LIGHT
            else -> DARK
        }
    }
}

/**
 * ViewType配置数据类
 */
data class MovieViewConfig(
    val viewType: MovieViewType = MovieViewType.RECT,
    val direction: MovieLayoutDirection = MovieLayoutDirection.VERTICAL,
    val theme: MovieThemeType = MovieThemeType.DARK,
    val columns: Int = 3,
    val aspectRatio: Float = 0.75f,
    val showOverlay: Boolean = true,
    val enableAnimation: Boolean = true
) {
    /**
     * 获取适配的列数
     */
    fun getAdaptiveColumns(screenWidthDp: Int): Int = when {
        screenWidthDp < 600 -> 2  // 手机
        screenWidthDp < 840 -> 3  // 平板
        else -> columns           // TV/桌面
    }
    
    /**
     * 是否为列表布局
     */
    fun isList(): Boolean = viewType == MovieViewType.LIST
    
    /**
     * 是否为网格布局
     */
    fun isGrid(): Boolean = viewType == MovieViewType.GRID
    
    /**
     * 是否为矩形布局
     */
    fun isRect(): Boolean = viewType == MovieViewType.RECT
    
    /**
     * 是否为圆形布局
     */
    fun isOval(): Boolean = viewType == MovieViewType.OVAL
}
