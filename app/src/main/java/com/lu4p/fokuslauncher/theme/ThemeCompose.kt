package com.lu4p.fokuslauncher.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import com.lu4p.fokuslauncher.ui.theme.LightGray
import com.lu4p.fokuslauncher.ui.theme.White

/**
 * Opaque background of the applied theme, or black when no theme is applied. Settings pages
 * paint this instead of a hard-coded black so light themes stay readable.
 */
val LocalThemeBackground = compositionLocalOf { Color.Black }

/** Scrim colour for settings subpages (theme background when a theme is applied, else black). */
val themedSettingsScrim: Color
    @Composable @ReadOnlyComposable get() = LocalThemeBackground.current

/** Overlays the theme's palette onto an existing (Fokus) scheme; surfaces stay transparent. */
fun ColorScheme.withAppTheme(theme: ThemeDefinition): ColorScheme {
    val c = theme.colors
    val primaryText = Color(c.primaryText)
    val secondaryText = Color(c.secondaryText)
    val accent = Color(c.accent)
    val background = Color(c.background)
    return copy(
            primary = primaryText,
            onPrimary = background,
            secondary = secondaryText,
            onSecondary = background,
            tertiary = accent,
            secondaryContainer = accent.copy(alpha = 0.22f).compositeOver(background),
            onSecondaryContainer = primaryText,
            onBackground = primaryText,
            onSurface = primaryText,
            surfaceVariant = Color(c.surface),
            onSurfaceVariant = secondaryText,
            inverseSurface = primaryText,
            inverseOnSurface = background,
    )
}

/** Recolours the launcher's default white / light-grey text styles to the theme's text colours. */
fun Typography.withAppThemeText(theme: ThemeDefinition): Typography {
    val primary = Color(theme.colors.primaryText)
    val secondary = Color(theme.colors.secondaryText)
    fun TextStyle.t(): TextStyle =
            when (color) {
                White -> copy(color = primary)
                LightGray -> copy(color = secondary)
                else -> this
            }
    return copy(
            displayLarge = displayLarge.t(),
            displayMedium = displayMedium.t(),
            displaySmall = displaySmall.t(),
            headlineLarge = headlineLarge.t(),
            headlineMedium = headlineMedium.t(),
            headlineSmall = headlineSmall.t(),
            titleLarge = titleLarge.t(),
            titleMedium = titleMedium.t(),
            titleSmall = titleSmall.t(),
            bodyLarge = bodyLarge.t(),
            bodyMedium = bodyMedium.t(),
            bodySmall = bodySmall.t(),
            labelLarge = labelLarge.t(),
            labelMedium = labelMedium.t(),
            labelSmall = labelSmall.t(),
    )
}
