package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// NOVA AI — Dark Futuristic ("Obsidian & Neon Cyan") Raw Palette
val DarkVoidBlack = Color(0xFF04060D)
val DarkDeepSpace = Color(0xFF080D1B)
val DarkSurfaceGlass = Color(0xFF0E162B)
val DarkSurfaceElevated = Color(0xFF15203C)
val DarkBorderGlow = Color(0xFF23355E)

val DarkCyan = Color(0xFF00F0FF)
val DarkElectricBlue = Color(0xFF2979FF)
val DarkPurple = Color(0xFF8A4FFF)
val DarkMagentaGlow = Color(0xFFC850FF)

val DarkEmerald = Color(0xFF00E699)
val DarkAmber = Color(0xFFFFB74D)
val DarkCoralError = Color(0xFFFF5370)
val DarkOfflineSlate = Color(0xFF7B8BA5)

val DarkTextPrimary = Color(0xFFF3F7FF)
val DarkTextSecondary = Color(0xFFA7B7D6)
val DarkTextMuted = Color(0xFF6B7C9E)
val DarkOnAccent = Color(0xFF04060D)
val DarkOnVibrant = Color(0xFFF3F7FF)

// NOVA AI — Light Futuristic ("Aero Daylight & Laser Cyan") Raw Palette
val LightArcticBase = Color(0xFFF3F7FF)
val LightSkyMid = Color(0xFFE6EFFD)
val LightSurfaceGlass = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFEAF2FE)
val LightBorderGlow = Color(0xFFC2D4F4)

val LightCyan = Color(0xFF007A99)
val LightElectricBlue = Color(0xFF1B5BE0)
val LightPurple = Color(0xFF6832DB)
val LightMagentaGlow = Color(0xFFA428D8)

val LightEmerald = Color(0xFF00875A)
val LightAmber = Color(0xFFC26A00)
val LightCoralError = Color(0xFFD32F4F)
val LightOfflineSlate = Color(0xFF556582)

val LightTextPrimary = Color(0xFF0B1328)
val LightTextSecondary = Color(0xFF425478)
val LightTextMuted = Color(0xFF67789B)
val LightOnAccent = Color(0xFFFFFFFF)
val LightOnVibrant = Color(0xFFFFFFFF)

@Immutable
data class NovaColorPalette(
    val isDark: Boolean,
    val voidBlack: Color,
    val deepSpace: Color,
    val surfaceGlass: Color,
    val surfaceElevated: Color,
    val borderGlow: Color,
    val cyan: Color,
    val electricBlue: Color,
    val purple: Color,
    val magentaGlow: Color,
    val emerald: Color,
    val amber: Color,
    val coralError: Color,
    val offlineSlate: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val onAccent: Color,
    val onVibrant: Color
)

val NovaDarkPalette = NovaColorPalette(
    isDark = true,
    voidBlack = DarkVoidBlack,
    deepSpace = DarkDeepSpace,
    surfaceGlass = DarkSurfaceGlass,
    surfaceElevated = DarkSurfaceElevated,
    borderGlow = DarkBorderGlow,
    cyan = DarkCyan,
    electricBlue = DarkElectricBlue,
    purple = DarkPurple,
    magentaGlow = DarkMagentaGlow,
    emerald = DarkEmerald,
    amber = DarkAmber,
    coralError = DarkCoralError,
    offlineSlate = DarkOfflineSlate,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    onAccent = DarkOnAccent,
    onVibrant = DarkOnVibrant
)

val NovaLightPalette = NovaColorPalette(
    isDark = false,
    voidBlack = LightArcticBase,
    deepSpace = LightSkyMid,
    surfaceGlass = LightSurfaceGlass,
    surfaceElevated = LightSurfaceElevated,
    borderGlow = LightBorderGlow,
    cyan = LightCyan,
    electricBlue = LightElectricBlue,
    purple = LightPurple,
    magentaGlow = LightMagentaGlow,
    emerald = LightEmerald,
    amber = LightAmber,
    coralError = LightCoralError,
    offlineSlate = LightOfflineSlate,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    onAccent = LightOnAccent,
    onVibrant = LightOnVibrant
)

val LocalNovaColors = staticCompositionLocalOf { NovaDarkPalette }

// Theme-reactive semantic tokens consumed across all NOVA screens & components
val NovaIsDarkTheme: Boolean
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.isDark

val NovaVoidBlack: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.voidBlack

val NovaDeepSpace: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.deepSpace

val NovaSurfaceGlass: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.surfaceGlass

val NovaSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.surfaceElevated

val NovaBorderGlow: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.borderGlow

val NovaCyan: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.cyan

val NovaElectricBlue: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.electricBlue

val NovaPurple: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.purple

val NovaMagentaGlow: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.magentaGlow

val NovaEmerald: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.emerald

val NovaAmber: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.amber

val NovaCoralError: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.coralError

val NovaOfflineSlate: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.offlineSlate

val NovaTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.textPrimary

val NovaTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.textSecondary

val NovaTextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.textMuted

val NovaOnAccent: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.onAccent

val NovaOnVibrant: Color
    @Composable @ReadOnlyComposable get() = LocalNovaColors.current.onVibrant
