package com.lu4p.fokuslauncher.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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

/** Linear gradient at a CSS-style angle (180 = top to bottom) that always spans the whole box. */
private class AngledGradientBrush(private val colors: List<Color>, private val angleDegrees: Int) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val a = Math.toRadians(angleDegrees.toDouble())
        val dx = Math.sin(a).toFloat()
        val dy = (-Math.cos(a)).toFloat()
        val half = (Math.abs(size.width * dx) + Math.abs(size.height * dy)) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
                from = Offset(c.x - dx * half, c.y - dy * half),
                to = Offset(c.x + dx * half, c.y + dy * half),
                colors = colors,
        )
    }
}

/** Brush for the theme background: its gradient when present, otherwise the solid colour. */
fun ThemeDefinition.backgroundBrush(): Brush {
    val g = gradient ?: return SolidColor(Color(colors.background))
    return AngledGradientBrush(g.stops.map { Color(it) }, g.angleDegrees)
}

/**
 * Applies the theme's weight / letter-spacing "feel" to display, headline and title styles. Only
 * weight and spacing change; the font family (the user's choice) is never touched.
 */
fun Typography.withThemeFeel(style: ThemeStyle): Typography {
    if (style.fontWeight == 0 && style.letterSpacingEm == 0f) return this
    fun TextStyle.f(): TextStyle {
        val extra = style.letterSpacingEm
        val spacing =
                when {
                    extra == 0f -> letterSpacing
                    letterSpacing.isSp && fontSize.isSp -> (letterSpacing.value + extra * fontSize.value).sp
                    else -> extra.em
                }
        return copy(
                fontWeight = if (style.fontWeight == 0) fontWeight else FontWeight(style.fontWeight),
                letterSpacing = spacing,
        )
    }
    return copy(
            displayLarge = displayLarge.f(),
            displayMedium = displayMedium.f(),
            displaySmall = displaySmall.f(),
            headlineLarge = headlineLarge.f(),
            headlineMedium = headlineMedium.f(),
            headlineSmall = headlineSmall.f(),
            titleLarge = titleLarge.f(),
            titleMedium = titleMedium.f(),
            titleSmall = titleSmall.f(),
    )
}
