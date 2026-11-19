package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.preferences.NovaThemeMode

private fun buildDarkColorScheme(p: NovaColorPalette): ColorScheme = darkColorScheme(
    primary = p.cyan,
    onPrimary = p.onAccent,
    primaryContainer = p.electricBlue.copy(alpha = 0.25f),
    onPrimaryContainer = p.textPrimary,
    secondary = p.purple,
    onSecondary = p.onVibrant,
    secondaryContainer = p.purple.copy(alpha = 0.22f),
    onSecondaryContainer = p.textPrimary,
    tertiary = p.electricBlue,
    onTertiary = p.onVibrant,
    background = p.voidBlack,
    onBackground = p.textPrimary,
    surface = p.deepSpace,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceGlass,
    onSurfaceVariant = p.textSecondary,
    surfaceContainerLowest = p.voidBlack,
    surfaceContainerLow = p.deepSpace,
    surfaceContainer = p.surfaceGlass,
    surfaceContainerHigh = p.surfaceElevated,
    outline = p.borderGlow,
    outlineVariant = p.borderGlow.copy(alpha = 0.5f),
    error = p.coralError,
    onError = p.onAccent
)

private fun buildLightColorScheme(p: NovaColorPalette): ColorScheme = lightColorScheme(
    primary = p.cyan,
    onPrimary = p.onAccent,
    primaryContainer = p.cyan.copy(alpha = 0.14f),
    onPrimaryContainer = p.textPrimary,
    secondary = p.purple,
    onSecondary = p.onVibrant,
    secondaryContainer = p.purple.copy(alpha = 0.12f),
    onSecondaryContainer = p.textPrimary,
    tertiary = p.electricBlue,
    onTertiary = p.onVibrant,
    background = p.voidBlack,
    onBackground = p.textPrimary,
    surface = p.surfaceGlass,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceElevated,
    onSurfaceVariant = p.textSecondary,
    surfaceContainerLowest = p.surfaceGlass,
    surfaceContainerLow = p.voidBlack,
    surfaceContainer = p.deepSpace,
    surfaceContainerHigh = p.surfaceElevated,
    outline = p.borderGlow,
    outlineVariant = p.borderGlow.copy(alpha = 0.65f),
    error = p.coralError,
    onError = p.onAccent
)

val NovaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
private fun animateThemeColor(target: Color, label: String): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 280),
        label = label
    )
    return color
}

@Composable
fun NovaAiTheme(
    themeMode: NovaThemeMode = NovaThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val target = if (themeMode == NovaThemeMode.DARK) NovaDarkPalette else NovaLightPalette
    val animatedPalette = NovaColorPalette(
        isDark = target.isDark,
        voidBlack = animateThemeColor(target.voidBlack, "voidBlack"),
        deepSpace = animateThemeColor(target.deepSpace, "deepSpace"),
        surfaceGlass = animateThemeColor(target.surfaceGlass, "surfaceGlass"),
        surfaceElevated = animateThemeColor(target.surfaceElevated, "surfaceElevated"),
        borderGlow = animateThemeColor(target.borderGlow, "borderGlow"),
        cyan = animateThemeColor(target.cyan, "cyan"),
        electricBlue = animateThemeColor(target.electricBlue, "electricBlue"),
        purple = animateThemeColor(target.purple, "purple"),
        magentaGlow = animateThemeColor(target.magentaGlow, "magentaGlow"),
        emerald = animateThemeColor(target.emerald, "emerald"),
        amber = animateThemeColor(target.amber, "amber"),
        coralError = animateThemeColor(target.coralError, "coralError"),
        offlineSlate = animateThemeColor(target.offlineSlate, "offlineSlate"),
        textPrimary = animateThemeColor(target.textPrimary, "textPrimary"),
        textSecondary = animateThemeColor(target.textSecondary, "textSecondary"),
        textMuted = animateThemeColor(target.textMuted, "textMuted"),
        onAccent = animateThemeColor(target.onAccent, "onAccent"),
        onVibrant = animateThemeColor(target.onVibrant, "onVibrant")
    )

    val colorScheme = if (target.isDark) {
        buildDarkColorScheme(animatedPalette)
    } else {
        buildLightColorScheme(animatedPalette)
    }

    CompositionLocalProvider(LocalNovaColors provides animatedPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovaTypography,
            shapes = NovaShapes,
            content = content
        )
    }
}
