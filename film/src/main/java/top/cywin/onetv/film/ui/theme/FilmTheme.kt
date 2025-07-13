package top.cywin.onetv.film.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Film 模块主题
 * 
 * 基于 OneTV 整体设计风格的 Film 模块专用主题
 * 与项目整体保持一致的视觉风格
 * 
 * @author OneTV Team
 * @since 2025-07-12
 */

// Film 专用颜色配置 - 与 OneTV 主题保持一致
object FilmColors {
    // 主色调 - 蓝色系，与 OneTV 保持一致
    val Primary = Color(0xFF0B57D0)
    val PrimaryVariant = Color(0xFF0842A0)
    val Secondary = Color(0xFF00639B)
    val SecondaryVariant = Color(0xFF004A77)
    
    // 背景色 - 深色主题为主
    val Background = Color(0xFF111111)
    val Surface = Color(0xFF1E1E1E)
    val SurfaceVariant = Color(0xFF2C2C2C)
    val SurfaceContainer = Color(0xFF191919)
    
    // 文字颜色
    val OnPrimary = Color.White
    val OnSecondary = Color.White
    val OnBackground = Color(0xFFE3E3E3)
    val OnSurface = Color(0xFFE3E3E3)
    val OnSurfaceVariant = Color(0xFFB3B3B3)
    
    // 状态颜色
    val Success = Color(0xFF6DD58C)
    val Warning = Color(0xFFCC9C00)
    val Error = Color(0xFFFA5151)
    val Info = Color(0xFF7FCFFF)
    
    // 特殊颜色
    val Accent = Color(0xFF03DAC6)
    val Outline = Color(0xFF404040)
    val OutlineVariant = Color(0xFF666666)
    
    // 透明度变体
    val SurfaceAlpha80 = Surface.copy(alpha = 0.8f)
    val BackgroundAlpha80 = Background.copy(alpha = 0.8f)
}

// 深色主题配色方案
private val DarkColorScheme = darkColorScheme(
    primary = FilmColors.Primary,
    onPrimary = FilmColors.OnPrimary,
    primaryContainer = FilmColors.PrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = FilmColors.Secondary,
    onSecondary = FilmColors.OnSecondary,
    secondaryContainer = FilmColors.SecondaryVariant,
    onSecondaryContainer = Color.White,
    tertiary = FilmColors.Accent,
    onTertiary = Color.Black,
    background = FilmColors.Background,
    onBackground = FilmColors.OnBackground,
    surface = FilmColors.Surface,
    onSurface = FilmColors.OnSurface,
    surfaceVariant = FilmColors.SurfaceVariant,
    onSurfaceVariant = FilmColors.OnSurfaceVariant,
    error = FilmColors.Error,
    onError = Color.White,
    outline = FilmColors.Outline,
    outlineVariant = FilmColors.OutlineVariant
)

// 浅色主题配色方案（备用）
private val LightColorScheme = lightColorScheme(
    primary = FilmColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = FilmColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2E7FF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = FilmColors.Accent,
    onTertiary = Color.Black,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFFAFAFA),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF666666),
    error = FilmColors.Error,
    onError = Color.White,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0)
)

// 字体配置 - 使用 HarmonyOS Sans
object FilmTypography {
    val displayLarge = TextStyle(
        fontFamily = FontFamily.Default, // 这里应该使用 HarmonyOSSans
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
    
    val displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    
    val displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    val headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )
    
    val headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )
    
    val headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    val titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    
    val titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )
    
    val bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    
    val bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    val labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    
    val labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    val labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
}

// 尺寸配置
object FilmDimens {
    // 间距
    val spacingXSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingXLarge = 32.dp
    val spacingXXLarge = 48.dp
    
    // 影视卡片尺寸
    val movieCardWidth = 160.dp
    val movieCardHeight = 240.dp
    val movieCardImageHeight = 180.dp
    val movieCardRadius = 8.dp
    
    // 按钮尺寸
    val buttonHeight = 48.dp
    val buttonMinWidth = 120.dp
    val iconButtonSize = 40.dp
    val fabSize = 56.dp
    
    // 列表项高度
    val listItemHeight = 72.dp
    val categoryItemHeight = 56.dp
    val siteItemHeight = 64.dp
    
    // 播放器控制栏
    val playerControlHeight = 80.dp
    val playerSeekBarHeight = 4.dp
    
    // 搜索栏
    val searchBarHeight = 56.dp
    val searchBarRadius = 28.dp
    
    // 底部导航
    val bottomNavHeight = 80.dp
    
    // 顶部应用栏
    val topAppBarHeight = 64.dp
    
    // 圆角半径
    val radiusSmall = 4.dp
    val radiusMedium = 8.dp
    val radiusLarge = 16.dp
    val radiusXLarge = 24.dp
    
    // 阴影高度
    val elevationSmall = 2.dp
    val elevationMedium = 4.dp
    val elevationLarge = 8.dp
}

/**
 * Film 主题组合函数
 */
@Composable
fun FilmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    // 创建自定义 Typography
    val typography = Typography(
        displayLarge = FilmTypography.displayLarge,
        displayMedium = FilmTypography.displayMedium,
        displaySmall = FilmTypography.displaySmall,
        headlineLarge = FilmTypography.headlineLarge,
        headlineMedium = FilmTypography.headlineMedium,
        headlineSmall = FilmTypography.headlineSmall,
        titleLarge = FilmTypography.titleLarge,
        titleMedium = FilmTypography.titleMedium,
        titleSmall = FilmTypography.titleSmall,
        bodyLarge = FilmTypography.bodyLarge,
        bodyMedium = FilmTypography.bodyMedium,
        bodySmall = FilmTypography.bodySmall,
        labelLarge = FilmTypography.labelLarge,
        labelMedium = FilmTypography.labelMedium,
        labelSmall = FilmTypography.labelSmall
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
