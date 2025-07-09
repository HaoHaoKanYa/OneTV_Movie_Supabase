package top.cywin.onetv.movie.ui.theme

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
 * Movie模块主题配置
 * 统一点播功能的UI风格，与现有直播主题保持一致
 */

// 点播专用颜色配置
object MovieColors {
    // 主色调 - 与OneTV主题保持一致
    val Primary = Color(0xFF1976D2)
    val PrimaryVariant = Color(0xFF1565C0)
    val Secondary = Color(0xFF03DAC6)
    val SecondaryVariant = Color(0xFF018786)
    
    // 背景色
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val SurfaceVariant = Color(0xFF2C2C2C)
    
    // 文字颜色
    val OnPrimary = Color.White
    val OnSecondary = Color.Black
    val OnBackground = Color.White
    val OnSurface = Color.White
    val OnSurfaceVariant = Color(0xFFB3B3B3)
    
    // 状态颜色
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
    
    // 点播特有颜色
    val MovieCardBackground = Color(0xFF1A1A1A)
    val MovieCardBorder = Color(0xFF333333)
    val PlayButtonBackground = Color(0xFF1976D2)
    val FavoriteColor = Color(0xFFE91E63)
    val HistoryColor = Color(0xFF9C27B0)
    val CategoryColor = Color(0xFF607D8B)
}

// 深色主题配色方案
private val DarkColorScheme = darkColorScheme(
    primary = MovieColors.Primary,
    onPrimary = MovieColors.OnPrimary,
    primaryContainer = MovieColors.PrimaryVariant,
    onPrimaryContainer = MovieColors.OnPrimary,
    secondary = MovieColors.Secondary,
    onSecondary = MovieColors.OnSecondary,
    secondaryContainer = MovieColors.SecondaryVariant,
    onSecondaryContainer = MovieColors.OnSecondary,
    tertiary = MovieColors.Info,
    onTertiary = Color.White,
    background = MovieColors.Background,
    onBackground = MovieColors.OnBackground,
    surface = MovieColors.Surface,
    onSurface = MovieColors.OnSurface,
    surfaceVariant = MovieColors.SurfaceVariant,
    onSurfaceVariant = MovieColors.OnSurfaceVariant,
    error = MovieColors.Error,
    onError = Color.White,
    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF666666)
)

// 浅色主题配色方案（TV应用通常使用深色主题）
private val LightColorScheme = lightColorScheme(
    primary = MovieColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = MovieColors.Secondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = MovieColors.Info,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFFAFAFA),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF666666),
    error = MovieColors.Error,
    onError = Color.White,
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0)
)

// 字体配置
object MovieTypography {
    val displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
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
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    
    val bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )
    
    val bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    
    val labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )
    
    val labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    
    val labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 8.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
}

// 形状配置
object MovieShapes {
    val small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    val extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    
    // 点播特有形状
    val movieCard = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val playButton = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
    val categoryChip = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
}

// 尺寸配置
object MovieDimens {
    // 间距
    val spacingXSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingXLarge = 32.dp
    
    // 电影卡片尺寸
    val movieCardWidth = 160.dp
    val movieCardHeight = 240.dp
    val movieCardImageHeight = 180.dp
    
    // 按钮尺寸
    val buttonHeight = 48.dp
    val buttonMinWidth = 120.dp
    val iconButtonSize = 40.dp
    
    // 列表项高度
    val listItemHeight = 72.dp
    val categoryItemHeight = 56.dp
    
    // 播放器控制栏
    val playerControlHeight = 80.dp
    val playerSeekBarHeight = 4.dp
}

/**
 * Movie主题组合函数
 */
@Composable
fun MovieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    
    // 创建自定义Typography
    val typography = Typography(
        displayLarge = MovieTypography.displayLarge,
        displayMedium = MovieTypography.displayMedium,
        displaySmall = MovieTypography.displaySmall,
        headlineLarge = MovieTypography.headlineLarge,
        headlineMedium = MovieTypography.headlineMedium,
        headlineSmall = MovieTypography.headlineSmall,
        titleLarge = MovieTypography.titleLarge,
        titleMedium = MovieTypography.titleMedium,
        titleSmall = MovieTypography.titleSmall,
        bodyLarge = MovieTypography.bodyLarge,
        bodyMedium = MovieTypography.bodyMedium,
        bodySmall = MovieTypography.bodySmall,
        labelLarge = MovieTypography.labelLarge,
        labelMedium = MovieTypography.labelMedium,
        labelSmall = MovieTypography.labelSmall
    )
    
    // 创建自定义Shapes
    val shapes = Shapes(
        small = MovieShapes.small,
        medium = MovieShapes.medium,
        large = MovieShapes.large,
        extraLarge = MovieShapes.extraLarge
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
