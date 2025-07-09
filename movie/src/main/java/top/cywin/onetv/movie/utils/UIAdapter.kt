package top.cywin.onetv.movie.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * UI适配工具类
 * 处理不同屏幕尺寸和密度的适配问题
 */
object UIAdapter {
    
    // 设备类型枚举
    enum class DeviceType {
        PHONE,      // 手机
        TABLET,     // 平板
        TV,         // 电视
        DESKTOP     // 桌面
    }
    
    // 屏幕尺寸分类
    enum class ScreenSize {
        COMPACT,    // 紧凑型 (< 600dp)
        MEDIUM,     // 中等型 (600dp - 840dp)
        EXPANDED    // 扩展型 (> 840dp)
    }
    
    // 方向枚举
    enum class Orientation {
        PORTRAIT,   // 竖屏
        LANDSCAPE   // 横屏
    }
    
    /**
     * 获取设备类型
     */
    fun getDeviceType(context: Context): DeviceType {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        
        return when {
            configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE -> DeviceType.TABLET
            else -> DeviceType.PHONE
        }
    }
    
    /**
     * 获取屏幕尺寸分类
     */
    @Composable
    fun getScreenSize(): ScreenSize {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        
        return when {
            screenWidthDp < 600 -> ScreenSize.COMPACT
            screenWidthDp < 840 -> ScreenSize.MEDIUM
            else -> ScreenSize.EXPANDED
        }
    }
    
    /**
     * 获取屏幕方向
     */
    @Composable
    fun getOrientation(): Orientation {
        val configuration = LocalConfiguration.current
        return if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Orientation.LANDSCAPE
        } else {
            Orientation.PORTRAIT
        }
    }
    
    /**
     * 根据屏幕尺寸调整列数
     */
    @Composable
    fun getAdaptiveColumns(
        compactColumns: Int = 2,
        mediumColumns: Int = 3,
        expandedColumns: Int = 4
    ): Int {
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> compactColumns
            ScreenSize.MEDIUM -> mediumColumns
            ScreenSize.EXPANDED -> expandedColumns
        }
    }
    
    /**
     * 根据屏幕尺寸调整间距
     */
    @Composable
    fun getAdaptiveSpacing(
        compactSpacing: Dp = 8.dp,
        mediumSpacing: Dp = 12.dp,
        expandedSpacing: Dp = 16.dp
    ): Dp {
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> compactSpacing
            ScreenSize.MEDIUM -> mediumSpacing
            ScreenSize.EXPANDED -> expandedSpacing
        }
    }
    
    /**
     * 根据屏幕尺寸调整内容宽度
     */
    @Composable
    fun getAdaptiveContentWidth(): Dp {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> screenWidthDp
            ScreenSize.MEDIUM -> minOf(screenWidthDp, 600.dp)
            ScreenSize.EXPANDED -> minOf(screenWidthDp, 840.dp)
        }
    }
    
    /**
     * 电影卡片自适应尺寸
     */
    @Composable
    fun getMovieCardSize(): Pair<Dp, Dp> {
        val screenSize = getScreenSize()
        val orientation = getOrientation()
        
        return when {
            screenSize == ScreenSize.COMPACT && orientation == Orientation.PORTRAIT -> 120.dp to 180.dp
            screenSize == ScreenSize.COMPACT && orientation == Orientation.LANDSCAPE -> 100.dp to 150.dp
            screenSize == ScreenSize.MEDIUM -> 140.dp to 210.dp
            screenSize == ScreenSize.EXPANDED -> 160.dp to 240.dp
            else -> 120.dp to 180.dp
        }
    }
    
    /**
     * 自适应字体大小
     */
    @Composable
    fun getAdaptiveFontScale(): Float {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        
        // 根据屏幕密度和尺寸调整字体缩放
        val baseScale = configuration.fontScale
        val densityScale = when {
            density.density < 1.5f -> 0.9f
            density.density > 3.0f -> 1.1f
            else -> 1.0f
        }
        
        return baseScale * densityScale
    }
    
    /**
     * 自适应布局修饰符
     */
    @Composable
    fun Modifier.adaptivePadding(
        compactPadding: Dp = 8.dp,
        mediumPadding: Dp = 16.dp,
        expandedPadding: Dp = 24.dp
    ): Modifier {
        val padding = when (getScreenSize()) {
            ScreenSize.COMPACT -> compactPadding
            ScreenSize.MEDIUM -> mediumPadding
            ScreenSize.EXPANDED -> expandedPadding
        }
        return this.padding(padding)
    }
    
    /**
     * 自适应填充修饰符
     */
    @Composable
    fun Modifier.adaptiveFillMaxWidth(
        compactFraction: Float = 1.0f,
        mediumFraction: Float = 0.8f,
        expandedFraction: Float = 0.6f
    ): Modifier {
        val fraction = when (getScreenSize()) {
            ScreenSize.COMPACT -> compactFraction
            ScreenSize.MEDIUM -> mediumFraction
            ScreenSize.EXPANDED -> expandedFraction
        }
        return this.fillMaxWidth(fraction)
    }
    
    /**
     * 响应式网格布局
     */
    @Composable
    fun ResponsiveGrid(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val columns = getAdaptiveColumns()
        val spacing = getAdaptiveSpacing()
        
        // 使用LazyVerticalGrid实现响应式网格
        // 这里简化为Box，实际使用时需要LazyVerticalGrid
        Box(
            modifier = modifier
                .fillMaxWidth()
                .adaptivePadding()
        ) {
            content()
        }
    }
    
    /**
     * 自适应导航布局
     */
    @Composable
    fun AdaptiveNavigation(
        compactContent: @Composable () -> Unit,
        expandedContent: @Composable () -> Unit
    ) {
        when (getScreenSize()) {
            ScreenSize.COMPACT -> compactContent()
            ScreenSize.MEDIUM, ScreenSize.EXPANDED -> expandedContent()
        }
    }
    
    /**
     * 计算网格项目尺寸
     */
    @Composable
    fun calculateGridItemSize(
        totalWidth: Dp,
        columns: Int,
        spacing: Dp
    ): Dp {
        val totalSpacing = spacing * (columns - 1)
        return (totalWidth - totalSpacing) / columns
    }
    
    /**
     * 获取安全区域内边距
     */
    @Composable
    fun getSafeAreaPadding(): PaddingValues {
        // 在实际应用中，这里应该使用WindowInsets
        // 这里提供默认值
        return PaddingValues(
            start = 16.dp,
            top = 24.dp,
            end = 16.dp,
            bottom = 16.dp
        )
    }
    
    /**
     * 自适应播放器控制栏高度
     */
    @Composable
    fun getPlayerControlHeight(): Dp {
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> 60.dp
            ScreenSize.MEDIUM -> 70.dp
            ScreenSize.EXPANDED -> 80.dp
        }
    }
    
    /**
     * 自适应侧边栏宽度
     */
    @Composable
    fun getSidebarWidth(): Dp {
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> 0.dp // 不显示侧边栏
            ScreenSize.MEDIUM -> 240.dp
            ScreenSize.EXPANDED -> 280.dp
        }
    }
    
    /**
     * 检查是否应该显示侧边栏
     */
    @Composable
    fun shouldShowSidebar(): Boolean {
        return getScreenSize() != ScreenSize.COMPACT
    }
    
    /**
     * 自适应对话框宽度
     */
    @Composable
    fun getDialogWidth(): Dp {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        
        return when (getScreenSize()) {
            ScreenSize.COMPACT -> screenWidth * 0.9f
            ScreenSize.MEDIUM -> minOf(screenWidth * 0.7f, 400.dp)
            ScreenSize.EXPANDED -> minOf(screenWidth * 0.5f, 500.dp)
        }
    }
    
    /**
     * 获取屏幕信息
     */
    @Composable
    fun getScreenInfo(): ScreenInfo {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        return ScreenInfo(
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
            density = density.density,
            fontScale = configuration.fontScale,
            orientation = getOrientation(),
            screenSize = getScreenSize()
        )
    }
}

/**
 * 屏幕信息数据类
 */
data class ScreenInfo(
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val fontScale: Float,
    val orientation: UIAdapter.Orientation,
    val screenSize: UIAdapter.ScreenSize
)
