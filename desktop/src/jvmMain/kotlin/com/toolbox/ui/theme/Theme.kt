package com.toolbox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== 模块标识 ====================
enum class ModuleId { NOTE, AI, CALCULATOR, PASSWORD, SCHEDULE, TEXT, DATA, FILE, DEVELOPER, NETWORK, SYSTEM, IMAGE, PRODUCTIVITY, SECURITY, TESTGEN }

// ==================== 模式调色板 ====================
data class ModePalette(
    val primary: Color,
    val bg: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color
)

data class SchemePalette(
    val label: String,
    val keyword: String,
    val light: ModePalette,
    val dark: ModePalette,
    val accentLight: Map<ModuleId, Color>,
    val accentDark: Map<ModuleId, Color>
)

// ==================== 四套配色方案（theme.md） ====================
enum class ThemeScheme(val label: String, val keyword: String, val palette: SchemePalette) {
    INDIGO(
        label = "靛蓝科技",
        keyword = "专业、AI感、理性",
        palette = SchemePalette(
            label = "靛蓝科技",
            keyword = "专业、AI感、理性",
            light = ModePalette(
                primary = Color(0xFF4F46E5),
                bg = Color(0xFFF7F8FC),
                card = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF1E2030),
                textSecondary = Color(0xFF64748B),
                border = Color(0xFFE2E8F0)
            ),
            dark = ModePalette(
                primary = Color(0xFF6366F1),
                bg = Color(0xFF0B1020),
                card = Color(0xFF141B2D),
                textPrimary = Color(0xFFF1F5F9),
                textSecondary = Color(0xFF94A3B8),
                border = Color(0xFF2A3352)
            ),
            accentLight = mapOf(
                ModuleId.NOTE to Color(0xFF818CF8),
                ModuleId.AI to Color(0xFF4F46E5),
                ModuleId.CALCULATOR to Color(0xFF22D3EE),
                ModuleId.PASSWORD to Color(0xFFA78BFA),
                ModuleId.SCHEDULE to Color(0xFFFBBF24),
                ModuleId.TEXT to Color(0xFF94A3B8),
                ModuleId.DATA to Color(0xFFFB7185),
                ModuleId.FILE to Color(0xFF34D399),
                ModuleId.DEVELOPER to Color(0xFFF97316),
                ModuleId.NETWORK to Color(0xFF0EA5E9),
                ModuleId.SYSTEM to Color(0xFF7C3AED),
                ModuleId.IMAGE to Color(0xFFEC4899),
                ModuleId.PRODUCTIVITY to Color(0xFF14B8A6),
                ModuleId.SECURITY to Color(0xFFDC2626),
                ModuleId.TESTGEN to Color(0xFF65A30D)
                 ),
                 accentDark = mapOf(
                ModuleId.NOTE to Color(0xFFA5B4FC),
                ModuleId.AI to Color(0xFF6366F1),
                ModuleId.CALCULATOR to Color(0xFF67E8F9),
                ModuleId.PASSWORD to Color(0xFFC4B5FD),
                ModuleId.SCHEDULE to Color(0xFFFCD34D),
                ModuleId.TEXT to Color(0xFFCBD5E1),
                ModuleId.DATA to Color(0xFFFDA4AF),
                ModuleId.FILE to Color(0xFF6EE7B7),
                ModuleId.DEVELOPER to Color(0xFFFB923C),
                ModuleId.NETWORK to Color(0xFF38BDF8),
                ModuleId.SYSTEM to Color(0xFFA78BFA),
                ModuleId.IMAGE to Color(0xFFF9A8D4),
                ModuleId.PRODUCTIVITY to Color(0xFF2DD4BF),
                ModuleId.SECURITY to Color(0xFFFCA5A5),
                ModuleId.TESTGEN to Color(0xFFBEF264)
            )
        )
    ),
    TEAL(
        label = "青绿色效率",
        keyword = "清爽、轻办公、低压力",
        palette = SchemePalette(
            label = "青绿色效率",
            keyword = "清爽、轻办公、低压力",
            light = ModePalette(
                primary = Color(0xFF0D9488),
                bg = Color(0xFFF4FAF9),
                card = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF162B2A),
                textSecondary = Color(0xFF5C7A78),
                border = Color(0xFFD7EAE8)
            ),
            dark = ModePalette(
                primary = Color(0xFF14B8A6),
                bg = Color(0xFF081A19),
                card = Color(0xFF0F2A28),
                textPrimary = Color(0xFFECFDFC),
                textSecondary = Color(0xFF99F6E4),
                border = Color(0xFF1E3A38)
            ),
            accentLight = mapOf(
                ModuleId.NOTE to Color(0xFF38BDF8),
                ModuleId.AI to Color(0xFF0D9488),
                ModuleId.CALCULATOR to Color(0xFF22C55E),
                ModuleId.PASSWORD to Color(0xFFA78BFA),
                ModuleId.SCHEDULE to Color(0xFFF59E0B),
                ModuleId.TEXT to Color(0xFF64748B),
                ModuleId.DATA to Color(0xFFF97316),
                ModuleId.FILE to Color(0xFF10B981),
                ModuleId.DEVELOPER to Color(0xFFEF4444),
                ModuleId.NETWORK to Color(0xFF0EA5E9),
                ModuleId.SYSTEM to Color(0xFF7C3AED),
                ModuleId.IMAGE to Color(0xFFEC4899),
                ModuleId.PRODUCTIVITY to Color(0xFF14B8A6),
                ModuleId.SECURITY to Color(0xFFDC2626),
                ModuleId.TESTGEN to Color(0xFF65A30D)
                 ),
                 accentDark = mapOf(
                ModuleId.NOTE to Color(0xFF7DD3FC),
                ModuleId.AI to Color(0xFF14B8A6),
                ModuleId.CALCULATOR to Color(0xFF4ADE80),
                ModuleId.PASSWORD to Color(0xFFC4B5FD),
                ModuleId.SCHEDULE to Color(0xFFFBBF24),
                ModuleId.TEXT to Color(0xFF94A3B8),
                ModuleId.DATA to Color(0xFFFB923C),
                ModuleId.FILE to Color(0xFF34D399),
                ModuleId.DEVELOPER to Color(0xFFF87171),
                ModuleId.NETWORK to Color(0xFF38BDF8),
                ModuleId.SYSTEM to Color(0xFFA78BFA),
                ModuleId.IMAGE to Color(0xFFF9A8D4),
                ModuleId.PRODUCTIVITY to Color(0xFF2DD4BF),
                ModuleId.SECURITY to Color(0xFFFCA5A5),
                ModuleId.TESTGEN to Color(0xFFBEF264)
            )
        )
    ),
    ROSE(
        label = "玫瑰粉紫",
        keyword = "亲和、有记忆点、个人助手",
        palette = SchemePalette(
            label = "玫瑰粉紫",
            keyword = "亲和、有记忆点、个人助手",
            light = ModePalette(
                primary = Color(0xFFDB2777),
                bg = Color(0xFFFFF7FA),
                card = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF2A1220),
                textSecondary = Color(0xFF7A5668),
                border = Color(0xFFF5D7E4)
            ),
            dark = ModePalette(
                primary = Color(0xFFF472B6),
                bg = Color(0xFF1A0B14),
                card = Color(0xFF2A1424),
                textPrimary = Color(0xFFFDF2F8),
                textSecondary = Color(0xFFFBCFE8),
                border = Color(0xFF4B2438)
            ),
            accentLight = mapOf(
                ModuleId.NOTE to Color(0xFF60A5FA),
                ModuleId.AI to Color(0xFFDB2777),
                ModuleId.CALCULATOR to Color(0xFF10B981),
                ModuleId.PASSWORD to Color(0xFF8B5CF6),
                ModuleId.SCHEDULE to Color(0xFFF59E0B),
                ModuleId.TEXT to Color(0xFF64748B),
                ModuleId.DATA to Color(0xFFF97316),
                ModuleId.FILE to Color(0xFF06B6D4),
                ModuleId.DEVELOPER to Color(0xFFEF4444),
                ModuleId.NETWORK to Color(0xFF0EA5E9),
                ModuleId.SYSTEM to Color(0xFF7C3AED),
                ModuleId.IMAGE to Color(0xFFEC4899),
                ModuleId.PRODUCTIVITY to Color(0xFF14B8A6),
                ModuleId.SECURITY to Color(0xFFDC2626),
                ModuleId.TESTGEN to Color(0xFF65A30D)
                 ),
                 accentDark = mapOf(
                ModuleId.NOTE to Color(0xFF93C5FD),
                ModuleId.AI to Color(0xFFF472B6),
                ModuleId.CALCULATOR to Color(0xFF34D399),
                ModuleId.PASSWORD to Color(0xFFA78BFA),
                ModuleId.SCHEDULE to Color(0xFFFBBF24),
                ModuleId.TEXT to Color(0xFF94A3B8),
                ModuleId.DATA to Color(0xFFFB923C),
                ModuleId.FILE to Color(0xFF22D3EE),
                ModuleId.DEVELOPER to Color(0xFFF87171),
                ModuleId.NETWORK to Color(0xFF38BDF8),
                ModuleId.SYSTEM to Color(0xFFA78BFA),
                ModuleId.IMAGE to Color(0xFFF9A8D4),
                ModuleId.PRODUCTIVITY to Color(0xFF2DD4BF),
                ModuleId.SECURITY to Color(0xFFFCA5A5),
                ModuleId.TESTGEN to Color(0xFFBEF264)
            )
        )
    ),
    DEV_ORANGE(
        label = "深灰橙开发者风",
        keyword = "克制、极客、代码感",
        palette = SchemePalette(
            label = "深灰橙开发者风",
            keyword = "克制、极客、代码感",
            light = ModePalette(
                primary = Color(0xFFEA580C),
                bg = Color(0xFFFAF8F5),
                card = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF231F1A),
                textSecondary = Color(0xFF78716C),
                border = Color(0xFFE7E2DC)
            ),
            dark = ModePalette(
                primary = Color(0xFFFB923C),
                bg = Color(0xFF131110),
                card = Color(0xFF1C1917),
                textPrimary = Color(0xFFF5F5F4),
                textSecondary = Color(0xFFA8A29E),
                border = Color(0xFF3F3A37)
            ),
            accentLight = mapOf(
                ModuleId.NOTE to Color(0xFF60A5FA),
                ModuleId.AI to Color(0xFFEA580C),
                ModuleId.CALCULATOR to Color(0xFF22C55E),
                ModuleId.PASSWORD to Color(0xFFA78BFA),
                ModuleId.SCHEDULE to Color(0xFFF59E0B),
                ModuleId.TEXT to Color(0xFF78716C),
                ModuleId.DATA to Color(0xFFEF4444),
                ModuleId.FILE to Color(0xFF10B981),
                ModuleId.DEVELOPER to Color(0xFF6366F1),
                ModuleId.NETWORK to Color(0xFF0EA5E9),
                ModuleId.SYSTEM to Color(0xFF7C3AED),
                ModuleId.IMAGE to Color(0xFFEC4899),
                ModuleId.PRODUCTIVITY to Color(0xFF14B8A6),
                ModuleId.SECURITY to Color(0xFFDC2626),
                ModuleId.TESTGEN to Color(0xFF65A30D)
                 ),
                 accentDark = mapOf(
                ModuleId.NOTE to Color(0xFF93C5FD),
                ModuleId.AI to Color(0xFFFB923C),
                ModuleId.CALCULATOR to Color(0xFF4ADE80),
                ModuleId.PASSWORD to Color(0xFFC4B5FD),
                ModuleId.SCHEDULE to Color(0xFFFBBF24),
                ModuleId.TEXT to Color(0xFFA8A29E),
                ModuleId.DATA to Color(0xFFF87171),
                ModuleId.FILE to Color(0xFF34D399),
                ModuleId.DEVELOPER to Color(0xFF818CF8),
                ModuleId.NETWORK to Color(0xFF38BDF8),
                ModuleId.SYSTEM to Color(0xFFA78BFA),
                ModuleId.IMAGE to Color(0xFFF9A8D4),
                ModuleId.PRODUCTIVITY to Color(0xFF2DD4BF),
                ModuleId.SECURITY to Color(0xFFFCA5A5),
                ModuleId.TESTGEN to Color(0xFFBEF264)
            )
        )
    )
}

// ==================== 主题控制器 ====================
object ThemeController {
    private val prefs: java.util.prefs.Preferences =
        java.util.prefs.Preferences.userRoot().node("ai-toolbox")

    val scheme: MutableState<ThemeScheme> = mutableStateOf(
        runCatching { ThemeScheme.valueOf(prefs.get("scheme", ThemeScheme.INDIGO.name)) }
            .getOrDefault(ThemeScheme.INDIGO)
    )

    val darkMode: MutableState<Boolean> = mutableStateOf(
        prefs.getBoolean("dark", false)
    )

    fun setScheme(value: ThemeScheme) {
        scheme.value = value
        runCatching {
            prefs.put("scheme", value.name)
            prefs.flush()
        }
    }

    fun setDarkMode(value: Boolean) {
        darkMode.value = value
        runCatching {
            prefs.putBoolean("dark", value)
            prefs.flush()
        }
    }
}

fun moduleAccent(id: ModuleId): Color {
    val palette = ThemeController.scheme.value.palette
    return if (ThemeController.darkMode.value) {
        palette.accentDark.getValue(id)
    } else {
        palette.accentLight.getValue(id)
    }
}

// ==================== 颜色方案构建 ====================
private fun ModePalette.toColorScheme(dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = card,
            onPrimaryContainer = textPrimary,
            secondary = primary,
            onSecondary = Color.White,
            secondaryContainer = border,
            onSecondaryContainer = textPrimary,
            background = bg,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = bg,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = Color(0xFFF87171),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFFECDD3)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = card,
            onPrimaryContainer = textPrimary,
            secondary = primary,
            onSecondary = Color.White,
            secondaryContainer = border,
            onSecondaryContainer = textPrimary,
            background = bg,
            onBackground = textPrimary,
            surface = card,
            onSurface = textPrimary,
            surfaceVariant = bg,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = Color(0xFFE65B5B),
            onError = Color.White,
            errorContainer = Color(0xFFFCE9E9),
            onErrorContainer = Color(0xFF8C2B2B)
        )
    }

// ==================== 字体层级 ====================
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.W600,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.W600,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W500,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.W400,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// ==================== 圆角形状 ====================
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

// ==================== 主题入口 ====================
@Composable
fun ToolboxTheme(
    content: @Composable () -> Unit
) {
    val scheme = ThemeController.scheme.value
    val dark = ThemeController.darkMode.value
    val mode = if (dark) scheme.palette.dark else scheme.palette.light

    MaterialTheme(
        colorScheme = mode.toColorScheme(dark),
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
