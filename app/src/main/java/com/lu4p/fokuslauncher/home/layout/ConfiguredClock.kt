package com.lu4p.fokuslauncher.home.layout

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/**
 * Current time that updates once per minute, or once per second only while [showSeconds] is true.
 * With seconds hidden no per-second timer runs.
 */
@Composable
internal fun rememberClockNow(showSeconds: Boolean): State<ZonedDateTime> =
        produceState(ZonedDateTime.now(), showSeconds) {
            while (true) {
                val now = ZonedDateTime.now(ZoneId.systemDefault())
                value = now
                val step = if (showSeconds) 1_000L else 60_000L
                val into = System.currentTimeMillis() % step
                delay(step - into + 5)
            }
        }

@Composable
fun ConfiguredClock(
        settings: ClockSettings,
        sizeScale: Float,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val now by rememberClockNow(settings.showSeconds)
    val context = LocalContext.current
    val is24h = ClockFormatter.resolve24h(settings.hourFormat, DateFormat.is24HourFormat(context))
    val text = remember(now, settings.style, is24h, settings.showSeconds) {
        ClockFormatter.format(now, settings.style, is24h, settings.showSeconds)
    }
    val base = MaterialTheme.typography.displayLarge
    val baseSize = if (settings.fontSizeSp > 0) settings.fontSizeSp.toFloat() else base.fontSize.value
    val styleScale = when (settings.style) {
        ClockStyle.COMPACT -> 0.6f
        ClockStyle.MINIMAL_TEXT -> 0.45f
        ClockStyle.STACKED, ClockStyle.SPLIT -> 0.9f
        ClockStyle.LARGE_DIGITAL -> 1f
    }
    val size: TextUnit = (baseSize * styleScale * sizeScale).sp
    val weight = if (settings.style == ClockStyle.MINIMAL_TEXT) ClockFontWeight.LIGHT.value else settings.weight.value
    val textStyle = base.copy(
            fontSize = size,
            lineHeight = size * settings.lineHeightMultiplier * (base.lineHeight.value / base.fontSize.value),
            fontWeight = FontWeight(weight),
            letterSpacing = settings.letterSpacingSp.sp,
            textAlign = when (settings.alignment) {
                ElementAlignment.START -> TextAlign.Start
                ElementAlignment.CENTER -> TextAlign.Center
                ElementAlignment.END -> TextAlign.End
            },
    )
    val color = if (settings.colorArgb != 0) Color(settings.colorArgb) else MaterialTheme.colorScheme.onBackground
    val horizontal = when (settings.alignment) {
        ElementAlignment.START -> Alignment.Start
        ElementAlignment.CENTER -> Alignment.CenterHorizontally
        ElementAlignment.END -> Alignment.End
    }
    Column(
            horizontalAlignment = horizontal,
            modifier = modifier.fillMaxWidth()
                    .clickableNoRippleWithSystemSound(onClick = onClick)
                    .semantics { contentDescription = text.joined },
    ) {
        if (settings.style == ClockStyle.SPLIT || settings.style == ClockStyle.STACKED) {
            text.lines.forEach { Text(it, style = textStyle, color = color, maxLines = 1) }
        } else {
            Text(text.lines.first(), style = textStyle, color = color, maxLines = 1)
        }
        text.period?.let {
            Text(it, style = textStyle.copy(fontSize = size * 0.3f, fontWeight = FontWeight.Normal), color = color)
        }
    }
}

/** The home date line: the existing formatted [default] unless a locale-aware style is chosen. */
@Composable
fun configuredDate(default: String, style: ClockDateStyle): String {
    if (style == ClockDateStyle.SYSTEM) return default
    val now by rememberClockNow(false)
    return ClockFormatter.formatDate(now, java.util.Locale.getDefault(), style) ?: default
}
