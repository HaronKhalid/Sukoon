package com.lu4p.fokuslauncher.clock.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hand angles in degrees clockwise from 12 o'clock. Updates are minute ticks, never animated. */
object AnalogMath {
    fun hourDegrees(hour: Int, minute: Int): Float = (hour % 12) * 30f + minute * 0.5f
    fun minuteDegrees(minute: Int): Float = minute * 6f
    fun secondDegrees(second: Int): Float = second * 6f
}

private fun DrawScope.hand(center: Offset, degrees: Float, from: Float, to: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
    val rad = degrees * PI.toFloat() / 180f
    val dx = sin(rad)
    val dy = -cos(rad)
    drawLine(color, Offset(center.x + dx * from, center.y + dy * from),
            Offset(center.x + dx * to, center.y + dy * to), strokeWidth = width, cap = StrokeCap.Round)
}

@Composable
private fun AnalogFace(spec: ClockFaceSpec, modifier: Modifier, ticks: Boolean) {
    FaceColumn(spec, modifier) {
        BoxWithConstraints {
            val d = minOf(160.dp * spec.clampedScale, maxWidth)
            val time = spec.time
            Canvas(Modifier.size(d)) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                val stroke = (r * 0.03f).coerceAtLeast(1.5f)
                drawCircle(spec.color.copy(alpha = if (ticks) 0.18f else 0.45f), r - stroke, style = Stroke(stroke))
                if (ticks) {
                    for (i in 0 until 60) {
                        val major = i % 5 == 0
                        val len = if (major) r * 0.13f else r * 0.05f
                        hand(c, i * 6f, r * 0.93f - len, r * 0.93f,
                                if (major) stroke * 1.4f else stroke * 0.6f,
                                if (major) spec.color else spec.color.copy(alpha = 0.45f))
                    }
                }
                hand(c, AnalogMath.hourDegrees(time.hour, time.minute), 0f, r * 0.5f, r * 0.07f, spec.color)
                hand(c, AnalogMath.minuteDegrees(time.minute), 0f, r * 0.76f, r * 0.045f, spec.color)
                if (spec.showSeconds) {
                    hand(c, AnalogMath.secondDegrees(time.second), -r * 0.12f, r * 0.84f, r * 0.018f, spec.accent)
                }
                drawCircle(spec.accent, r * 0.055f, c)
            }
        }
    }
}

object AnalogMinimalFace : ClockFace {
    override val id = "analog-minimal"
    override val labelRes = R.string.clock_face_analog_minimal

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) = AnalogFace(spec, modifier, ticks = false)
}

object AnalogTicksFace : ClockFace {
    override val id = "analog-ticks"
    override val labelRes = R.string.clock_face_analog_ticks

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) = AnalogFace(spec, modifier, ticks = true)
}
