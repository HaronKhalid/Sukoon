package com.lu4p.fokuslauncher.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/** Scales every line height by [factor] (no-op for 1) and stops trimming tall glyphs. */
fun Typography.withLineHeightBoost(factor: Float): Typography {
    if (factor <= 1f) return this
    fun TextStyle.boosted(): TextStyle {
        val lh: TextUnit =
                when {
                    lineHeight == TextUnit.Unspecified -> (fontSize.value.takeIf { !it.isNaN() } ?: 16f).times(1.2f * factor).sp
                    lineHeight.type == TextUnitType.Sp -> (lineHeight.value * factor).sp
                    else -> lineHeight
                }
        return copy(
                lineHeight = lh,
                lineHeightStyle =
                        LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None,
                        ),
        )
    }
    return copy(
            displayLarge = displayLarge.boosted(), displayMedium = displayMedium.boosted(),
            displaySmall = displaySmall.boosted(), headlineLarge = headlineLarge.boosted(),
            headlineMedium = headlineMedium.boosted(), headlineSmall = headlineSmall.boosted(),
            titleLarge = titleLarge.boosted(), titleMedium = titleMedium.boosted(),
            titleSmall = titleSmall.boosted(), bodyLarge = bodyLarge.boosted(),
            bodyMedium = bodyMedium.boosted(), bodySmall = bodySmall.boosted(),
            labelLarge = labelLarge.boosted(), labelMedium = labelMedium.boosted(),
            labelSmall = labelSmall.boosted(),
    )
}
