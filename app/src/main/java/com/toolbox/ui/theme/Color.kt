package com.toolbox.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 主色 ====================
val Primary = Color(0xFF2563EB)
val PrimaryLight = Color(0xFF93C5FD)
val PrimaryDark = Color(0xFF1D4ED8)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDBEAFE)
val OnPrimaryContainer = Color(0xFF1E3A8A)

// ==================== 浅色模式 ====================
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1F2937)
val LightTextSecondary = Color(0xFF6B7280)
val LightDivider = Color(0xFFE5E7EB)
val LightBorder = Color(0xFFE5E7EB)
val LightOnBackground = Color(0xFF1F2937)
val LightOnSurface = Color(0xFF1F2937)

// ==================== 深色模式 ====================
val DarkBackground = Color(0xFF12141A)
val DarkSurface = Color(0xFF1E212B)
val DarkCard = Color(0xFF1E212B)
val DarkTextPrimary = Color(0xFFF3F4F6)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkDivider = Color(0xFF333847)
val DarkBorder = Color(0xFF333847)
val DarkOnBackground = Color(0xFFF3F4F6)
val DarkOnSurface = Color(0xFFF3F4F6)

// ==================== 状态色 ====================
val Success = Color(0xFF10B981)
val SuccessLight = Color(0xFFD1FAE5)
val SuccessDark = Color(0xFF34D399)

val Warning = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val WarningDark = Color(0xFFFBBF24)

val Danger = Color(0xFFEF4444)
val DangerLight = Color(0xFFFEE2E2)
val DangerDark = Color(0xFFF87171)

// ==================== 辅助色 ====================
val Secondary = Color(0xFF64748B)
val SecondaryLight = Color(0xFF94A3B8)
val SecondaryDark = Color(0xFF475569)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFF1F5F9)
val OnSecondaryContainer = Color(0xFF334155)

// ==================== Surface 变体 ====================
val SurfaceVariantLight = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF272934)
val OnSurfaceVariantLight = Color(0xFF64748B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// ==================== 向后兼容（保留旧定义，标记废弃）====================
@Deprecated("使用 Primary", ReplaceWith("Primary"))
val Blue200 = PrimaryLight

@Deprecated("使用 Primary", ReplaceWith("Primary"))
val Blue500 = Primary

@Deprecated("使用 PrimaryDark", ReplaceWith("PrimaryDark"))
val Blue700 = PrimaryDark

@Deprecated("使用 Secondary", ReplaceWith("Secondary"))
val Teal200 = SecondaryLight

@Deprecated("使用 Secondary", ReplaceWith("Secondary"))
val Teal500 = Secondary

@Deprecated("使用 SecondaryDark", ReplaceWith("SecondaryDark"))
val Teal700 = SecondaryDark

@Deprecated("使用 LightBackground", ReplaceWith("LightBackground"))
val BackgroundLight = LightBackground

@Deprecated("使用 DarkBackground", ReplaceWith("DarkBackground"))
val BackgroundDark = DarkBackground

@Deprecated("使用 LightSurface", ReplaceWith("LightSurface"))
val SurfaceLight = LightSurface

@Deprecated("使用 DarkSurface", ReplaceWith("DarkSurface"))
val SurfaceDark = DarkSurface

@Deprecated("使用 Danger", ReplaceWith("Danger"))
val ErrorRed = Danger

@Deprecated("使用 LightOnBackground", ReplaceWith("LightOnBackground"))
val OnBackgroundLight = LightOnBackground

@Deprecated("使用 DarkOnBackground", ReplaceWith("DarkOnBackground"))
val OnBackgroundDark = DarkOnBackground

@Deprecated("使用 LightOnSurface", ReplaceWith("LightOnSurface"))
val OnSurfaceLight = LightOnSurface

@Deprecated("使用 DarkOnSurface", ReplaceWith("DarkOnSurface"))
val OnSurfaceDark = DarkOnSurface

// ==================== 分类颜色（保留） ====================
val CategoryBlue = Color(0xFF3B82F6)
val CategoryGreen = Color(0xFF22C55E)
val CategoryOrange = Color(0xFFF97316)
val CategoryRed = Color(0xFFEF4444)
val CategoryPurple = Color(0xFFA855F7)
val CategoryPink = Color(0xFFEC4899)
val CategoryTeal = Color(0xFF14B8A6)
val CategoryBrown = Color(0xFFA16207)
