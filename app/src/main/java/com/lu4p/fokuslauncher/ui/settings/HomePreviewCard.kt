package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.backgroundBrush
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reusable miniature of the whole home screen: the theme background (solid or gradient), a clock in
 * the chosen face, the date and a few app labels, all drawn with the theme's colours, weight and
 * letter spacing and the launcher's current font family. Used by the Style Gallery, the first-run
 * sheet and the Theme Studio live preview. [clockFaceId] and [layoutPreset] default to the theme's
 * style bundle; unknown clock ids fall back to the classic digital clock.
 */
@Composable
fun HomePreviewCard(
        theme: ThemeDefinition,
        modifier: Modifier = Modifier,
        clockFaceId: String = theme.style.clockFaceId,
        layoutPreset: String = theme.style.layoutPreset,
        selected: Boolean = false,
        cornerRadius: Dp = 22.dp,
) {
    val c = theme.colors
    val primary = Color(c.primaryText)
    val secondary = Color(c.secondaryText)
    val accent = Color(c.accent)
    val face = com.lu4p.fokuslauncher.theme.ClockFaces.resolve(clockFaceId)
    val family = MaterialTheme.typography.bodyMedium.fontFamily
    val weight = if (theme.style.fontWeight == 0) FontWeight.Normal else FontWeight(theme.style.fontWeight)
    val spacing = theme.style.letterSpacingEm.em
    fun ts(size: Int, w: FontWeight = weight, color: Color = primary) =
            TextStyle(fontFamily = family, fontWeight = w, letterSpacing = spacing, fontSize = size.sp, color = color)

    val centered = layoutPreset == "AESTHETIC"
    val hAlign = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start
    val shape = RoundedCornerShape(cornerRadius)

    Box(
            modifier =
                    modifier.clip(shape)
                            .background(theme.backgroundBrush())
                            .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) accent else secondary.copy(alpha = 0.35f),
                                    shape,
                            )
    ) {
        Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = hAlign,
        ) {
            if (layoutPreset == "AESTHETIC") Spacer(Modifier.height(34.dp))
            PreviewClock(face, ts(1), accent, primary, textAlign, if (layoutPreset == "PURE_MINIMAL") 0.8f else 1f)
            if (layoutPreset != "PURE_MINIMAL") {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.style_preview_date), style = ts(11, color = secondary), textAlign = textAlign)
            }
            if (layoutPreset == "FOCUS") {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.style_preview_focus), style = ts(10, color = accent))
                }
            }
            if (layoutPreset == "INFORMATION") {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.style_preview_battery), style = ts(10, color = secondary))
            }
            Spacer(Modifier.weight(1f))
            val apps =
                    buildList {
                        add(R.string.style_preview_app_phone)
                        add(R.string.style_preview_app_messages)
                        if (layoutPreset != "PURE_MINIMAL") add(R.string.style_preview_app_camera)
                        if (layoutPreset == "INFORMATION" || layoutPreset == "DEFAULT") add(R.string.style_preview_app_notes)
                        if (layoutPreset == "INFORMATION") add(R.string.style_preview_app_music)
                    }
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = hAlign,
                    verticalArrangement = Arrangement.spacedBy(if (layoutPreset == "PURE_MINIMAL" || centered) 12.dp else 8.dp),
            ) {
                apps.forEachIndexed { i, res ->
                    Text(
                            stringResource(res),
                            style = ts(if (centered) 16 else 15, color = if (i == 0) accent else primary),
                            textAlign = textAlign,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewClock(
        face: String,
        base: TextStyle,
        accent: Color,
        primary: Color,
        align: TextAlign,
        scale: Float,
) {
    val big = base.copy(fontSize = (46 * scale).sp, lineHeight = (48 * scale).sp)
    when (face) {
        "stacked" -> {
            Text("09", style = big.copy(color = primary), textAlign = align)
            Text("41", style = big.copy(color = accent), textAlign = align)
        }
        "split-accent" -> {
            Row2 {
                Text("09", style = big.copy(color = primary))
                Text(":41", style = big.copy(color = accent))
            }
        }
        "minimal-text" ->
                Text(
                        stringResource(R.string.style_preview_minimal_text),
                        style = base.copy(fontSize = (22 * scale).sp, color = primary),
                        textAlign = align,
                )
        "word-clock" ->
                Text(
                        stringResource(R.string.style_preview_word_clock),
                        style = base.copy(fontSize = (20 * scale).sp, lineHeight = (26 * scale).sp, color = primary),
                        textAlign = align,
                )
        "analog-minimal", "analog-ticks" -> AnalogPreview(face == "analog-ticks", primary, accent, (84 * scale).dp)
        "outline-bold" ->
                Text(
                        "09:41",
                        style =
                                big.copy(
                                        color = primary,
                                        fontWeight = FontWeight.Black,
                                        drawStyle = Stroke(width = 3f),
                                ),
                        textAlign = align,
                )
        else -> Text("09:41", style = big.copy(color = primary), textAlign = align)
    }
}

@Composable
private fun Row2(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun AnalogPreview(ticks: Boolean, primary: Color, accent: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(primary.copy(alpha = 0.5f), radius = r - 2f, center = center, style = Stroke(2f))
        if (ticks) {
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0)
                val inner = r * (if (i % 3 == 0) 0.78f else 0.88f)
                drawLine(
                        primary,
                        Offset(center.x + (sin(a) * inner).toFloat(), center.y - (cos(a) * inner).toFloat()),
                        Offset(center.x + (sin(a) * (r - 4f)).toFloat(), center.y - (cos(a) * (r - 4f)).toFloat()),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                )
            }
        }
        fun hand(angleDeg: Double, length: Float, color: Color, width: Float) {
            val a = Math.toRadians(angleDeg)
            drawLine(
                    color,
                    center,
                    Offset(center.x + (sin(a) * length).toFloat(), center.y - (cos(a) * length).toFloat()),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
            )
        }
        hand(9 * 30 + 41 * 0.5, r * 0.5f, primary, 5f)
        hand(41 * 6.0, r * 0.75f, accent, 3.5f)
    }
}
