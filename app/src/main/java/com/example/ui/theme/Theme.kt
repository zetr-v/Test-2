package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class StudioThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System"),
}

private val StudioDarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmberPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberBright,
    secondary = EmeraldSuccess,
    onSecondary = OnAmberPrimary,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldSuccess,
    background = StudioBackground,
    onBackground = TextPrimaryDark,
    surface = StudioSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioBorderDashed,
    outlineVariant = StudioBorderSubtle,
    error = CoralError,
    errorContainer = CoralContainer,
    onErrorContainer = CoralError,
)

private val StudioLightColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = LightSurface,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = AmberPrimaryLight,
    secondary = EmeraldSuccess,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightBorder,
    outlineVariant = LightSurfaceVariant,
    error = CoralError,
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun WebPStudioTheme(
    themeMode: StudioThemeMode = StudioThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        StudioThemeMode.DARK -> true
        StudioThemeMode.LIGHT -> false
        StudioThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
