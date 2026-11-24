package com.metronome.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StudioColors {
    val Background = Color(0xFF0E1116)
    val Surface = Color(0xFF161B22)
    val SurfaceElevated = Color(0xFF1F2630)
    val SurfaceHighlight = Color(0xFF28313E)
    val Outline = Color(0xFF333F50)

    val TextPrimary = Color(0xFFF3F6FA)
    val TextSecondary = Color(0xFF9AA7B8)
    val TextMuted = Color(0xFF667385)

    // Primary Voice Accent Palette
    val AccentStrong = Color(0xFFFF6B3D)      // 珊瑚橙 - 强拍
    val AccentMedium = Color(0xFFFFB74D)      // 琥珀黄 - 次强拍
    val AccentNormal = Color(0xFF5D8AA8)      // 钢青蓝 - 普通拍
    val AccentMute = Color(0xFF2D3644)        // 暗灰 - 静音拍

    // Secondary Polyrhythm Voice Palette
    val PolyStrong = Color(0xFF2CEAA3)        // 翡翠青 - 副声部强拍
    val PolyMedium = Color(0xFF7BE9C3)        // 浅翠 - 副声部次强
    val PolyNormal = Color(0xFF3A7B6D)        // 深翠 - 副声部普通

    val PlayActive = Color(0xFFFF5252)
    val PlayIdle = Color(0xFFFF6B3D)
}

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioColors.AccentStrong,
    onPrimary = Color(0xFF1A0B05),
    primaryContainer = Color(0xFF452014),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = StudioColors.PolyStrong,
    onSecondary = Color(0xFF042217),
    secondaryContainer = Color(0xFF103A2B),
    onSecondaryContainer = Color(0xFFB5F9DD),
    tertiary = StudioColors.AccentMedium,
    onTertiary = Color(0xFF241600),
    background = StudioColors.Background,
    onBackground = StudioColors.TextPrimary,
    surface = StudioColors.Surface,
    onSurface = StudioColors.TextPrimary,
    surfaceVariant = StudioColors.SurfaceElevated,
    onSurfaceVariant = StudioColors.TextSecondary,
    outline = StudioColors.Outline
)

@Composable
fun MetronomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        content = content
    )
}
