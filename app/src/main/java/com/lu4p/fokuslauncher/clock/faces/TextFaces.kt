package com.lu4p.fokuslauncher.clock.faces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.ClockFormatter
import com.lu4p.fokuslauncher.home.layout.ClockStyle
import java.util.Locale

private fun ClockFaceSpec.digits(style: ClockStyle) = ClockFormatter.format(time, style, is24h, showSeconds)

private fun ClockFaceSpec.horizontalArrangement() = when (horizontal) {
    Alignment.CenterHorizontally -> Arrangement.Center
    Alignment.End -> Arrangement.End
    else -> Arrangement.Start
}

/** Large single-line digits, the closest match to the original home clock. */
object ClassicDigitalFace : ClockFace {
    override val id = "classic-digital"
    override val labelRes = R.string.clock_face_classic_digital

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val t = spec.digits(ClockStyle.LARGE_DIGITAL)
        FaceColumn(spec, modifier) {
            Text(t.lines.first(), style = spec.lineStyle(1f), color = spec.color, maxLines = 1,
                    overflow = TextOverflow.Clip)
            t.period?.let { Text(it, style = spec.lineStyle(0.3f, 400, 1f), color = spec.accent) }
        }
    }
}

/** Hour over minute, both large; minutes are drawn in the accent colour. */
object StackedFace : ClockFace {
    override val id = "stacked"
    override val labelRes = R.string.clock_face_stacked

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val t = spec.digits(ClockStyle.STACKED)
        FaceColumn(spec, modifier) {
            t.lines.forEachIndexed { i, line ->
                Text(line, style = spec.lineStyle(if (i == 0) 0.95f else 0.95f), maxLines = 1,
                        color = if (i == 0) spec.color else spec.accent)
            }
            t.period?.let { Text(it, style = spec.lineStyle(0.28f, 400, 1f), color = spec.color) }
        }
    }
}

/** One row: bold hour, accent minute, and a short accent bar underneath. */
object SplitAccentFace : ClockFace {
    override val id = "split-accent"
    override val labelRes = R.string.clock_face_split_accent

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val t = spec.digits(ClockStyle.SPLIT)
        FaceColumn(spec, modifier) {
            Row(horizontalArrangement = spec.horizontalArrangement(), verticalAlignment = Alignment.Bottom) {
                Text(t.lines[0], style = spec.lineStyle(1f, 700), color = spec.color, maxLines = 1)
                Text(t.lines[1], style = spec.lineStyle(1f, 300), color = spec.accent, maxLines = 1,
                        modifier = Modifier.padding(start = (6 * spec.clampedScale).dp))
            }
            Box(Modifier.padding(top = (4 * spec.clampedScale).dp).width((56 * spec.clampedScale).dp)
                    .height((3 * spec.clampedScale).dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(spec.accent))
            t.period?.let { Text(it, style = spec.lineStyle(0.28f, 400, 1f), color = spec.color) }
        }
    }
}

/** Small, light, widely spaced digits for a quiet home screen. */
object MinimalTextFace : ClockFace {
    override val id = "minimal-text"
    override val labelRes = R.string.clock_face_minimal_text

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val t = spec.digits(ClockStyle.MINIMAL_TEXT)
        FaceColumn(spec, modifier) {
            Text(t.joined, style = spec.lineStyle(0.45f, 300, 4f), color = spec.color, maxLines = 1)
        }
    }
}

/** Heavy outlined digits drawn as a stroke in the accent colour. */
object OutlineBoldFace : ClockFace {
    override val id = "outline-bold"
    override val labelRes = R.string.clock_face_outline_bold

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val t = spec.digits(ClockStyle.LARGE_DIGITAL)
        val style = spec.lineStyle(1.1f, 900).copy(
                drawStyle = Stroke(width = 3f * spec.clampedScale * 2f),
        )
        FaceColumn(spec, modifier) {
            Text(t.lines.first(), style = style, color = spec.accent, maxLines = 1, overflow = TextOverflow.Clip)
            t.period?.let { Text(it, style = spec.lineStyle(0.3f, 400, 1f), color = spec.accent) }
        }
    }
}

/** Time in English words ("half past ten"); the hour word uses the accent colour. */
object WordClockFace : ClockFace {
    override val id = "word-clock"
    override val labelRes = R.string.clock_face_word_clock

    @Composable
    override fun Render(spec: ClockFaceSpec, modifier: Modifier) {
        val lines = WordClock.lines(spec.time.hour, spec.time.minute)
        FaceColumn(spec, modifier) {
            lines.forEachIndexed { i, line ->
                val first = i == 0
                Text(line.replaceFirstChar { it.titlecase(Locale.ENGLISH) },
                        style = spec.lineStyle(0.5f, if (first) 300 else 700),
                        color = if (first) spec.color else spec.accent, maxLines = 2)
            }
        }
    }
}

/** Pure English time-to-words; the lead line is the minutes phrase and the last line the hour. */
object WordClock {
    private val ones = listOf(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven",
            "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )

    /** 1..59 spelled out ("twenty five"). */
    fun number(n: Int): String {
        require(n in 0..59)
        return if (n < 20) ones[n] else "twenty" + if (n % 10 == 0) "" else " " + ones[n % 10]
    }

    fun hourWord(hour24: Int): String = ones[if (hour24 % 12 == 0) 12 else hour24 % 12]

    private fun minutes(n: Int) = number(n) + if (n % 5 == 0) "" else if (n == 1) " minute" else " minutes"

    fun lines(hour24: Int, minute: Int): List<String> {
        val h = ((hour24 % 24) + 24) % 24
        val m = minute.coerceIn(0, 59)
        return when {
            m == 0 -> listOf(hourWord(h), "o'clock")
            m == 30 -> listOf("half past", hourWord(h))
            m == 15 -> listOf("quarter past", hourWord(h))
            m < 30 -> listOf(minutes(m) + " past", hourWord(h))
            m == 45 -> listOf("quarter to", hourWord(h + 1))
            else -> listOf(minutes(60 - m) + " to", hourWord(h + 1))
        }
    }

    fun text(hour24: Int, minute: Int): String = lines(hour24, minute).joinToString(" ")
}
