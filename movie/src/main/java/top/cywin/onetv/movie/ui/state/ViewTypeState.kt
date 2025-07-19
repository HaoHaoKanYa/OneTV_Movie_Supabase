package top.cywin.onetv.movie.ui.state

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import top.cywin.onetv.movie.bean.Style
import top.cywin.onetv.movie.ui.base.MovieViewType
import top.cywin.onetv.movie.ui.base.MovieViewConfig
import top.cywin.onetv.movie.ui.base.MovieLayoutDirection
import top.cywin.onetv.movie.ui.base.MovieThemeType

/**
 * ViewType状态管理
 * 提供ViewType的状态保存和恢复功能
 */
@Composable
fun rememberViewTypeState(
    initialViewType: MovieViewType = MovieViewType.RECT,
    initialConfig: MovieViewConfig = MovieViewConfig()
): ViewTypeState {
    val context = LocalContext.current
    
    return remember {
        ViewTypeState(
            initialViewType = initialViewType,
            initialConfig = initialConfig,
            context = context
        )
    }
}

/**
 * ViewType状态类
 */
@Stable
class ViewTypeState(
    initialViewType: MovieViewType,
    initialConfig: MovieViewConfig,
    private val context: android.content.Context
) {
    // 当前ViewType
    var currentViewType by mutableStateOf(initialViewType)
        private set
    
    // 当前配置
    var currentConfig by mutableStateOf(initialConfig)
        private set
    
    /**
     * 更新ViewType
     */
    fun updateViewType(viewType: MovieViewType) {
        currentViewType = viewType
        currentConfig = currentConfig.copy(viewType = viewType)
        saveToPreferences()
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(config: MovieViewConfig) {
        currentConfig = config
        currentViewType = config.viewType
        saveToPreferences()
    }
    
    /**
     * 从Style对象更新ViewType
     */
    fun updateFromStyle(style: Style?) {
        if (style != null) {
            val viewType = MovieViewType.fromString(style.composeViewType)
            val aspectRatio = style.ratio
            
            currentConfig = currentConfig.copy(
                viewType = viewType,
                aspectRatio = aspectRatio
            )
            currentViewType = viewType
            saveToPreferences()
        }
    }
    
    /**
     * 切换到下一个ViewType
     */
    fun switchToNext() {
        val nextViewType = when(currentViewType) {
            MovieViewType.RECT -> MovieViewType.OVAL
            MovieViewType.OVAL -> MovieViewType.LIST
            MovieViewType.LIST -> MovieViewType.GRID
            MovieViewType.GRID -> MovieViewType.RECT
        }
        updateViewType(nextViewType)
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        currentConfig = MovieViewConfig()
        currentViewType = MovieViewConfig().viewType
        saveToPreferences()
    }
    
    /**
     * 保存到SharedPreferences
     */
    private fun saveToPreferences() {
        val prefs = context.getSharedPreferences("movie_viewtype", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("viewtype", currentViewType.value)
            putInt("columns", currentConfig.columns)
            putFloat("aspect_ratio", currentConfig.aspectRatio)
            putBoolean("show_overlay", currentConfig.showOverlay)
            putBoolean("enable_animation", currentConfig.enableAnimation)
            putInt("direction", currentConfig.direction.value)
            putInt("theme", currentConfig.theme.value)
            apply()
        }
    }
    
    /**
     * 从SharedPreferences加载
     */
    fun loadFromPreferences() {
        val prefs = context.getSharedPreferences("movie_viewtype", android.content.Context.MODE_PRIVATE)
        
        val viewType = MovieViewType.fromInt(prefs.getInt("viewtype", MovieViewType.RECT.value))
        val columns = prefs.getInt("columns", 3)
        val aspectRatio = prefs.getFloat("aspect_ratio", 0.75f)
        val showOverlay = prefs.getBoolean("show_overlay", true)
        val enableAnimation = prefs.getBoolean("enable_animation", true)
        val direction = MovieLayoutDirection.fromInt(prefs.getInt("direction", MovieLayoutDirection.VERTICAL.value))
        val theme = MovieThemeType.fromInt(prefs.getInt("theme", MovieThemeType.DARK.value))
        
        currentConfig = MovieViewConfig(
            viewType = viewType,
            direction = direction,
            theme = theme,
            columns = columns,
            aspectRatio = aspectRatio,
            showOverlay = showOverlay,
            enableAnimation = enableAnimation
        )
        currentViewType = viewType
    }
}

/**
 * ViewType配置对话框状态
 */
@Composable
fun rememberViewTypeDialogState(): ViewTypeDialogState {
    return remember { ViewTypeDialogState() }
}

@Stable
class ViewTypeDialogState {
    var isVisible by mutableStateOf(false)
        private set
    
    fun show() {
        isVisible = true
    }
    
    fun hide() {
        isVisible = false
    }
    
    fun toggle() {
        isVisible = !isVisible
    }
}

/**
 * ViewType扩展函数 - 与FongMi_TV兼容
 */
fun MovieViewType.toFongMiInt(): Int = this.value

fun Int.toMovieViewType(): MovieViewType = MovieViewType.fromInt(this)

fun String.toMovieViewType(): MovieViewType = MovieViewType.fromString(this)

/**
 * Style扩展函数 - 简化转换
 */
fun Style.toMovieViewType(): MovieViewType = MovieViewType.fromString(this.composeViewType)

fun Style.toMovieViewConfig(): MovieViewConfig = MovieViewConfig(
    viewType = this.toMovieViewType(),
    aspectRatio = this.ratio
)
