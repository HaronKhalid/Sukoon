package com.lu4p.fokuslauncher.clock.styles

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.clock.faces.ClockFaceSpec
import com.lu4p.fokuslauncher.clock.faces.lineStyle
import com.lu4p.fokuslauncher.home.layout.ClockFormatter
import com.lu4p.fokuslauncher.home.layout.ClockStyle
import com.lu4p.fokuslauncher.typography.FontRegistry
import com.lu4p.fokuslauncher.typography.LocalElementFonts
import com.lu4p.fokuslauncher.typography.TextElement
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Bundled-font families for styles, built once per (font, weight). Failures fall back to null. */
internal object StyleFonts {
    private val cache = ConcurrentHashMap<String, FontFamily>()

    @OptIn(ExperimentalTextApi::class)
    fun family(context: Context, fontId: String, weight: Int): FontFamily? {
        if (fontId.isEmpty()) return null
        val font = FontRegistry.findBundled(fontId) ?: return null
        val w = weight.coerceIn(100, 900)
        val key = "$fontId:$w"
        cache[key]?.let { return it }
        val built = runCatching {
            FontFamily(
                    Font(
                            path = font.assetPath,
                            assetManager = context.assets,
                            weight = FontWeight(w),
                            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
                    )
            )
        }.getOrNull() ?: return null
        return cache.putIfAbsent(key, built) ?: built
    }
}

/** Colours of the hour (segment 0) and minute (segment 1) parts for a colour mode. */
private fun ClockStylePreset.segmentColor(spec: ClockFaceSpec, segment: Int): Color = when (colorMode) {
    ClockColorMode.THEME, ClockColorMode.GRADIENT -> spec.color
    ClockColorMode.ACCENT -> spec.accent
    ClockColorMode.TWO_TONE -> if (segment == 0) spec.color else spec.accent
}

/**
 * Draws a [ClockStylePreset]. The preset's font is used unless [forceThemeFont] (the user chose a
 * custom clock font), in which case the face spec's base font wins. Cheap: no animation, the
 * caller redraws on the minute tick.
 */
@Composable
fun ClockStyleRenderer(
        preset: ClockStylePreset,
        spec: ClockFaceSpec,
        modifier: Modifier = Modifier,
        forceThemeFont: Boolean = LocalElementFonts.current[TextElement.CLOCK] != null,
) {
    val context = LocalContext.current
    val family = if (forceThemeFont) spec.baseStyle.fontFamily
    else StyleFonts.family(context, preset.fontId, preset.weight) ?: spec.baseStyle.fontFamily
    val locale = Locale.getDefault()

    fun style(factor: Float, weight: Int = preset.weight, spacing: Float = preset.letterSpacingSp): TextStyle {
        val base = spec.lineStyle(factor * preset.sizeFactor, weight, spacing)
        return base.copy(
                fontFamily = family,
                fontStyle = if (preset.italic) FontStyle.Italic else FontStyle.Normal,
                shadow = when {
                    preset.glow -> Shadow(spec.accent.copy(alpha = 0.9f), Offset.Zero, 28f)
                    preset.shadow -> Shadow(Color.Black.copy(alpha = 0.5f), Offset(4f, 4f), 8f)
                    else -> null
                },
                brush = if (preset.colorMode == ClockColorMode.GRADIENT && preset.outline != ClockOutline.HOLLOW)
                    Brush.linearGradient(listOf(spec.color, spec.accent)) else null,
        )
    }

    val strokePx = 6f * spec.clampedScale

    @Composable
    fun Styled(text: AnnotatedString, base: TextStyle, hollowColor: Color, fillColor: Color) {
        when (preset.outline) {
            ClockOutline.NONE -> Text(text, style = base, color = fillColor, maxLines = 1)
            ClockOutline.HOLLOW -> Text(text, style = base.copy(drawStyle = Stroke(strokePx), shadow = null),
                    color = hollowColor, maxLines = 1)
            ClockOutline.FILLED -> Box {
                Text(text, style = base, color = fillColor, maxLines = 1)
                Text(text, style = base.copy(drawStyle = Stroke(strokePx), shadow = null, brush = null),
                        color = spec.accent, maxLines = 1)
            }
        }
    }

    fun twoTone(first: String, second: String, sep: String = ""): AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = preset.segmentColor(spec, 0))) { append(first + sep) }
        withStyle(SpanStyle(color = preset.segmentColor(spec, 1))) { append(second) }
    }

    val c0 = preset.segmentColor(spec, 0)
    val c1 = preset.segmentColor(spec, 1)
    val split = ClockFormatter.format(spec.time, ClockStyle.SPLIT, spec.is24h, spec.showSeconds)
    val periodText = split.period?.let { ClockStyleText.applyCase(it, preset.case, locale) }
    val secondLine = ClockStyleText.line(preset.line, spec.time, locale, preset.case)

    Column(modifier.fillMaxWidth(), horizontalAlignment = spec.horizontal) {
        when (preset.layout) {
            ClockStyleLayout.INLINE -> {
                val text = if (preset.colorMode == ClockColorMode.TWO_TONE) twoTone(split.lines[0], split.lines[1], ":")
                else AnnotatedString(split.lines[0] + ":" + split.lines[1])
                Styled(text, style(1f), c0, c0)
                periodText?.let { Text(it, style = style(0.28f, 400, 1f), color = c1) }
            }
            ClockStyleLayout.STACKED -> {
                val stacked = ClockFormatter.format(spec.time, ClockStyle.STACKED, spec.is24h, spec.showSeconds)
                stacked.lines.forEachIndexed { i, line ->
                    val col = if (i == 0) c0 else c1
                    Styled(AnnotatedString(line), style(0.95f), col, col)
                }
                periodText?.let { Text(it, style = style(0.28f, 400, 1f), color = c0) }
            }
            ClockStyleLayout.VERTICAL -> {
                val rows = ClockStyleText.verticalRows(split.lines[0], split.lines[1].take(2))
                val hourCount = split.lines[0].length
                rows.forEachIndexed { i, ch ->
                    val col = if (i < hourCount) c0 else c1
                    Styled(AnnotatedString(ch), style(0.95f), col, col)
                }
            }
            ClockStyleLayout.FACE -> {
                val face = ClockFaceRegistry.get(ClockStyleRegistry.faceIdFor(preset))
                val faceSpec = spec.copy(
                        color = c0,
                                                baseStyle = spec.baseStyle.copy(fontFamily = family),
                        weight = preset.weight,
                )
                face.Render(faceSpec, Modifier)
            }
        }
        secondLine?.let {
            Text(it, style = style(0.3f, 500, maxOf(1f, preset.letterSpacingSp)),
                    color = if (preset.colorMode == ClockColorMode.THEME) spec.color.copy(alpha = 0.75f) else c1,
                    modifier = Modifier.padding(top = 4.dp))
        }
    }
}
