package com.lu4p.fokuslauncher.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import com.lu4p.fokuslauncher.data.model.LauncherFontScale
import com.lu4p.fokuslauncher.data.model.LauncherVisualStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.lu4p.fokuslauncher.theme.LocalThemeBackground
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.withAppTheme
import com.lu4p.fokuslauncher.theme.withAppThemeText
import com.lu4p.fokuslauncher.ui.util.ProvideSystemClickSound

private val FokusColorSchemeClassic = darkColorScheme(
        primary = White,
        onPrimary = Black,
        secondary = LightGray,
        onSecondary = Black,
        secondaryContainer = ChipBackground,
        onSecondaryContainer = White,
        background = Transparent,
        onBackground = White,
        surface = Transparent,
        onSurface = White,
        surfaceVariant = DarkGray,
        onSurfaceVariant = LightGray,
        surfaceContainerLowest = Transparent,
        surfaceContainerLow = Transparent,
        surfaceContainer = Transparent,
        surfaceContainerHigh = Transparent,
        surfaceContainerHighest = Transparent,
        surfaceBright = Transparent,
        surfaceDim = Transparent,
        inverseSurface = White,
        inverseOnSurface = Black,
        error = DestructiveRed,
        onError = Black,
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD4),
)

fun fokusColorSchemeFor(style: LauncherVisualStyle): ColorScheme {
    val palette = style.neonPalette() ?: return FokusColorSchemeClassic
    val sheetSurface = palette.primary.copy(alpha = 0.09f).compositeOver(Black)
    val segmentSelectedSurface =
            palette.primary.copy(alpha = 0.22f).compositeOver(Black)
    return FokusColorSchemeClassic.copy(
            primary = palette.primary,
            secondary = palette.muted,
            onBackground = palette.primary,
            onSurface = palette.primary,
            secondaryContainer = segmentSelectedSurface,
            onSecondaryContainer = palette.primary,
            surfaceVariant = sheetSurface,
            onSurfaceVariant = palette.muted,
            error = NeonDestructiveRed,
            onError = Black,
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD4),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FokusLauncherTheme(
        fontFamily: FontFamily = FontFamily.Default,
        fontScale: Float = 1f,
        visualStyle: LauncherVisualStyle = LauncherVisualStyle.CLASSIC,
        glowEnabled: Boolean = false,
        /** Applied Theme Studio theme; takes precedence over [visualStyle]. Null = legacy look. */
        appTheme: ThemeDefinition? = null,
        /** Paint the theme background behind content (false over a photo wallpaper). */
        paintThemeBackground: Boolean = true,
        content: @Composable () -> Unit
) {
    val effectiveStyle = if (appTheme != null) LauncherVisualStyle.CLASSIC else visualStyle
    val colorScheme =
            remember(effectiveStyle, appTheme) {
                val base = fokusColorSchemeFor(effectiveStyle)
                if (appTheme != null) base.withAppTheme(appTheme) else base
            }
    val typography =
            remember(fontFamily, fontScale, effectiveStyle, glowEnabled, appTheme) {
                val base =
                        fokusTypographyForLauncher(
                                fontFamily,
                                fontScale,
                                effectiveStyle,
                                glowEnabled,
                        )
                if (appTheme != null) base.withAppThemeText(appTheme) else base
            }
    val resolvedIconScale =
            remember(fontScale) {
                fontScale.coerceIn(LauncherFontScale.MIN, LauncherFontScale.MAX)
            }
    val iconGlow =
            remember(effectiveStyle, glowEnabled, appTheme) {
                if (!glowEnabled) {
                    LauncherIconGlowSpec.None
                } else {
                    val palette = effectiveStyle.neonPalette()
                    val halo =
                            palette?.primary
                                    ?: appTheme?.let { Color(it.colors.primaryText) }
                                    ?: White
                    LauncherIconGlowSpec(enabled = true, haloColor = halo)
                }
            }
    CompositionLocalProvider(
            LocalOverscrollFactory provides null,
            LocalLauncherFontScale provides resolvedIconScale,
            LocalLauncherIconGlow provides iconGlow,
            LocalThemeBackground provides
                    (appTheme?.let { Color(it.colors.background) } ?: Color.Black),
    ) {
        ProvideSystemClickSound {
            MaterialTheme(colorScheme = colorScheme, typography = typography) {
                if (appTheme != null && paintThemeBackground) {
                    Box(Modifier.fillMaxSize().background(Color(appTheme.colors.background))) {
                        content()
                    }
                } else {
                    content()
                }
            }
        }
    }
}
