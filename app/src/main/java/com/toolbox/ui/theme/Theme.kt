package com.toolbox.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ==================== 颜色方案 ====================
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = LightDivider,
    outlineVariant = LightBorder,
    error = Danger,
    onError = OnPrimary,
    errorContainer = DangerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryLight,
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = DarkDivider,
    outlineVariant = DarkBorder,
    error = DangerDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFECDD3)
)

// ==================== 字体层级 ====================
// 页面大标题：22sp，FontWeight.w600
// 模块标题：16sp，FontWeight.w500
// 正文常规：14sp，FontWeight.w400
// 辅助小字/说明：12sp，FontWeight.w400
// 标签/备注：11sp，FontWeight.w400

val AppTypography = Typography(
    // 页面大标题
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.W600,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 模块标题
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.W600,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // 模块标题（较小）
    titleLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    // 列表标题
    titleMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // 正文常规
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 正文常规（较小）
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp
    ),
    // 辅助小字/说明
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // 标签/备注
    labelLarge = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // 标签/备注（较小）
    labelMedium = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
    // 极小标签
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// ==================== 圆角形状 ====================
// 大卡片、弹窗、输入框：14px
// 按钮、功能Item、标签：12px
// Icon小容器、小圆标：10px

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),      // Icon小容器、小圆标
    medium = RoundedCornerShape(12.dp),     // 按钮、功能Item、标签
    large = RoundedCornerShape(14.dp),      // 大卡片、弹窗、输入框
    extraLarge = RoundedCornerShape(16.dp)  // 特殊大圆角
)

// ==================== 主题配置 ====================
@Composable
fun ToolboxTheme(
    darkMode: Int = 0, // 0: follow system, 1: light, 2: dark
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkMode) {
        1 -> false  // 强制浅色
        2 -> true   // 强制深色
        else -> isSystemInDarkTheme() // 跟随系统
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// ==================== 扩展属性 ====================
// 便捷访问主题颜色
object AppColors {
    @Composable
    fun primary() = MaterialTheme.colorScheme.primary

    @Composable
    fun background() = MaterialTheme.colorScheme.background

    @Composable
    fun surface() = MaterialTheme.colorScheme.surface

    @Composable
    fun card() = MaterialTheme.colorScheme.surface

    @Composable
    fun textPrimary() = MaterialTheme.colorScheme.onBackground

    @Composable
    fun textSecondary() = MaterialTheme.colorScheme.onSurfaceVariant

    @Composable
    fun divider() = MaterialTheme.colorScheme.outline

    @Composable
    fun border() = MaterialTheme.colorScheme.outlineVariant

    @Composable
    fun success() = Success

    @Composable
    fun warning() = Warning

    @Composable
    fun danger() = MaterialTheme.colorScheme.error
}
