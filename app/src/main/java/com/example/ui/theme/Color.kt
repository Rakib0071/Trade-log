package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Obsidian Dark Palette — Institutional Quant Terminal
val ObsidianBackground = Color(0xFF0A0E14)
val ObsidianSurface = Color(0xFF0F141D)
val ObsidianSurfaceLow = Color(0xFF121822)
val ObsidianSurfaceContainer = Color(0xFF161E2B)
val ObsidianSurfaceHigh = Color(0xFF1E2838)
val ObsidianSurfaceHighest = Color(0xFF263347)
val ObsidianBorder = Color(0xFF243144)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

// Semantic Accents (Dark)
val EmeraldPrimaryDark = Color(0xFF00D084)
val EmeraldOnPrimaryDark = Color(0xFF022C1C)
val EmeraldContainerDark = Color(0xFF063826)
val EmeraldOnContainerDark = Color(0xFF6EE7B7)

val IndigoSecondaryDark = Color(0xFF818CF8)
val IndigoOnSecondaryDark = Color(0xFF11153D)
val IndigoContainerDark = Color(0xFF1E2452)
val IndigoOnContainerDark = Color(0xFFC7D2FE)

val AmberTertiaryDark = Color(0xFFFBBF24)
val AmberOnTertiaryDark = Color(0xFF332100)
val AmberContainerDark = Color(0xFF3B2A06)
val AmberOnContainerDark = Color(0xFFFDE68A)

val CrimsonErrorDark = Color(0xFFFF4D6A)
val CrimsonOnErrorDark = Color(0xFF3D0612)
val CrimsonContainerDark = Color(0xFF3D121E)
val CrimsonOnContainerDark = Color(0xFFFFB3C1)

// Daylight Editorial Palette — Clean High-Contrast Quant Light
val DaylightBackground = Color(0xFFF4F7FA)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightSurfaceLow = Color(0xFFF8FAFC)
val DaylightSurfaceContainer = Color(0xFFEEF2F7)
val DaylightSurfaceHigh = Color(0xFFE2E8F0)
val DaylightSurfaceHighest = Color(0xFFD5DFEC)
val DaylightBorder = Color(0xFFCBD5E1)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF64748B)

val EmeraldPrimaryLight = Color(0xFF048A5A)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val EmeraldOnContainerLight = Color(0xFF064E3B)

val IndigoSecondaryLight = Color(0xFF4F46E5)
val IndigoOnSecondaryLight = Color(0xFFFFFFFF)
val IndigoContainerLight = Color(0xFFE0E7FF)
val IndigoOnContainerLight = Color(0xFF1E1B4B)

val AmberTertiaryLight = Color(0xFFB45309)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val AmberOnContainerLight = Color(0xFF78350F)

val CrimsonErrorLight = Color(0xFFDC2626)
val CrimsonOnErrorLight = Color(0xFFFFFFFF)
val CrimsonContainerLight = Color(0xFFFEE2E2)
val CrimsonOnContainerLight = Color(0xFF7F1D1D)

// Playbook Swatch Palette (used as selectable strategy accent colors)
val PlaybookColorSwatches = listOf(
    0xFF00D084L, // Emerald
    0xFF6366F1L, // Indigo
    0xFF38BDF8L, // Sky
    0xFFF59E0BL, // Amber
    0xFFEC4899L, // Rose
    0xFFA855F7L, // Violet
    0xFF14B8A6L  // Teal
)

@Immutable
data class EdgeLogExtendedColors(
    val profit: Color,
    val profitContainer: Color,
    val onProfitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val onLossContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val accentIndigo: Color,
    val accentIndigoContainer: Color,
    val cardBorder: Color,
    val chartGrid: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val DarkEdgeLogColors = EdgeLogExtendedColors(
    profit = EmeraldPrimaryDark,
    profitContainer = EmeraldContainerDark,
    onProfitContainer = EmeraldOnContainerDark,
    loss = CrimsonErrorDark,
    lossContainer = CrimsonContainerDark,
    onLossContainer = CrimsonOnContainerDark,
    warning = AmberTertiaryDark,
    warningContainer = AmberContainerDark,
    accentIndigo = IndigoSecondaryDark,
    accentIndigoContainer = IndigoContainerDark,
    cardBorder = ObsidianBorder,
    chartGrid = Color(0xFF1F2B3D),
    textMuted = TextMutedDark,
    isDark = true
)

val LightEdgeLogColors = EdgeLogExtendedColors(
    profit = EmeraldPrimaryLight,
    profitContainer = EmeraldContainerLight,
    onProfitContainer = EmeraldOnContainerLight,
    loss = CrimsonErrorLight,
    lossContainer = CrimsonContainerLight,
    onLossContainer = CrimsonOnContainerLight,
    warning = AmberTertiaryLight,
    warningContainer = AmberContainerLight,
    accentIndigo = IndigoSecondaryLight,
    accentIndigoContainer = IndigoContainerLight,
    cardBorder = DaylightBorder,
    chartGrid = Color(0xFFE2E8F0),
    textMuted = TextMutedLight,
    isDark = false
)

val LocalEdgeLogColors = staticCompositionLocalOf { DarkEdgeLogColors }
