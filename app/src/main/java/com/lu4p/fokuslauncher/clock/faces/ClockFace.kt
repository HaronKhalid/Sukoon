package com.lu4p.fokuslauncher.clock.faces

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.home.layout.ClockStyle
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import java.time.ZonedDateTime

/** Everything a face needs to draw itself. Faces never read time or settings themselves. */
@Immutable
data class ClockFaceSpec(
        val time: ZonedDateTime,
        val is24h: Boolean,
        val color: Color,
        val accent: Color,
        /** Theme typography (with the user's clock font already applied). */
        val baseStyle: TextStyle,
        /** Base font size in sp at scale 1 for the largest text face. */
        val baseSizeSp: Float,
        val showSeconds: Boolean = false,
        val alignment: ElementAlignment = ElementAlignment.START,
        /** Continuous size factor, clamped to [ClockScale.MIN]..[ClockScale.MAX]. */
        val scale: Float = 1f,
        val weight: Int = 600,
) {
    val clampedScale: Float get() = ClockScale.clamp(scale)
    val horizontal: Alignment.Horizontal
        get() = when (alignment) {
            ElementAlignment.START -> Alignment.Start
            ElementAlignment.CENTER -> Alignment.CenterHorizontally
            ElementAlignment.END -> Alignment.End
        }
    val textAlign: TextAlign
        get() = when (alignment) {
            ElementAlignment.START -> TextAlign.Start
            ElementAlignment.CENTER -> TextAlign.Center
            ElementAlignment.END -> TextAlign.End
        }
}

object ClockScale {
    const val MIN = 0.5f
    const val MAX = 2f
    fun clamp(value: Float): Float = if (value.isFinite()) value.coerceIn(MIN, MAX) else 1f
}

interface ClockFace {
    /** Stable id; persisted in settings and referenced by presets. Never rename. */
    val id: String
    @get:StringRes val labelRes: Int

    @Composable fun Render(spec: ClockFaceSpec, modifier: Modifier)
}

object ClockFaceRegistry {
    const val DEFAULT_ID = "classic-digital"

    val faces: List<ClockFace> = listOf(
            ClassicDigitalFace, StackedFace, SplitAccentFace, MinimalTextFace,
            AnalogMinimalFace, AnalogTicksFace, WordClockFace, OutlineBoldFace,
    )

    val ids: List<String> get() = faces.map { it.id }

    fun isKnown(id: String?): Boolean = faces.any { it.id == id }

    /** Unknown, null or empty ids fall back to classic-digital. */
    fun get(id: String?): ClockFace = faces.firstOrNull { it.id == id } ?: ClassicDigitalFace

    /** Face an existing (pre-gallery) clock style maps to. */
    fun idForLegacyStyle(style: ClockStyle): String = when (style) {
        ClockStyle.LARGE_DIGITAL -> "classic-digital"
        ClockStyle.COMPACT -> "minimal-text"
        ClockStyle.STACKED -> "stacked"
        ClockStyle.MINIMAL_TEXT -> "minimal-text"
        ClockStyle.SPLIT -> "split-accent"
    }

    fun resolveId(faceId: String, legacyStyle: ClockStyle): String =
            if (isKnown(faceId)) faceId else idForLegacyStyle(legacyStyle).takeIf { faceId.isEmpty() } ?: DEFAULT_ID
}

/** Text style for a face line: [factor] is relative to the base size, then the user scale applies. */
internal fun ClockFaceSpec.lineStyle(factor: Float, weight: Int = this.weight, spacingSp: Float = 0f): TextStyle {
    val size = (baseSizeSp * factor * clampedScale).sp
    return baseStyle.copy(
            fontSize = size,
            lineHeight = size * 1.1f,
            fontWeight = FontWeight(weight.coerceIn(1, 1000)),
            letterSpacing = spacingSp.sp,
            textAlign = textAlign,
    )
}

@Composable
internal fun FaceColumn(spec: ClockFaceSpec, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = spec.horizontal, content = content)
}
