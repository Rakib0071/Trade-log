package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppThemeMode(val label: String) {
    DARK("Obsidian Dark"),
    LIGHT("Daylight"),
    SYSTEM("System")
}

private val ColorOutlineSubtleDark = androidx.compose.ui.graphics.Color(0xFF1A2433)
private val ColorOutlineSubtleLight = androidx.compose.ui.graphics.Color(0xFFE2E8F0)

private val EdgeLogDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = IndigoSecondaryDark,
    onSecondary = IndigoOnSecondaryDark,
    secondaryContainer = IndigoContainerDark,
    onSecondaryContainer = IndigoOnContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = AmberOnTertiaryDark,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = AmberOnContainerDark,
    error = CrimsonErrorDark,
    onError = CrimsonOnErrorDark,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = CrimsonOnContainerDark,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = ColorOutlineSubtleDark
)

private val EdgeLogLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = IndigoSecondaryLight,
    onSecondary = IndigoOnSecondaryLight,
    secondaryContainer = IndigoContainerLight,
    onSecondaryContainer = IndigoOnContainerLight,
    tertiary = AmberTertiaryLight,
    onTertiary = AmberOnTertiaryLight,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnContainerLight,
    error = CrimsonErrorLight,
    onError = CrimsonOnErrorLight,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = CrimsonOnContainerLight,
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceContainer,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = DaylightSurface,
    surfaceContainerLow = DaylightSurfaceLow,
    surfaceContainer = DaylightSurfaceContainer,
    surfaceContainerHigh = DaylightSurfaceHigh,
    surfaceContainerHighest = DaylightSurfaceHighest,
    outline = DaylightBorder,
    outlineVariant = ColorOutlineSubtleLight
)

val EdgeLogShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun EdgeLogTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) EdgeLogDarkColorScheme else EdgeLogLightColorScheme
    val extendedColors = if (isDark) DarkEdgeLogColors else LightEdgeLogColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalEdgeLogColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EdgeLogTypography,
            shapes = EdgeLogShapes,
            content = content
        )
    }
}

object EdgeLogThemeDefaults {
    val colors: EdgeLogExtendedColors
        @Composable get() = LocalEdgeLogColors.current
}
