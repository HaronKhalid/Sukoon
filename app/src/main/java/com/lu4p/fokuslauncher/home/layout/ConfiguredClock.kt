package com.lu4p.fokuslauncher.home.layout

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.clock.faces.ClockFaceSpec
import com.lu4p.fokuslauncher.clock.faces.WordClock
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.typography.withElementFont
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

/**
 * Builds the face inputs from Clock Studio settings: theme colours (custom text colour and accent
 * override them), the user's clock font, 12/24h, alignment and the continuous [sizeScale].
 */
@Composable
fun rememberClockFaceSpec(
        settings: ClockSettings,
        sizeScale: Float,
        now: ZonedDateTime,
        showSeconds: Boolean = settings.showSeconds,
): ClockFaceSpec {
    val context = LocalContext.current
    val is24h = ClockFormatter.resolve24h(settings.hourFormat, DateFormat.is24HourFormat(context))
    val base = MaterialTheme.typography.displayLarge.withElementFont(TextElement.CLOCK)
    val color = if (settings.colorArgb != 0) Color(settings.colorArgb) else MaterialTheme.colorScheme.onBackground
    val accent = if (settings.accentArgb != 0) Color(settings.accentArgb) else MaterialTheme.colorScheme.primary
    val baseSize = if (settings.fontSizeSp > 0) settings.fontSizeSp.toFloat() else base.fontSize.value
    return ClockFaceSpec(
            time = now, is24h = is24h, color = color, accent = accent, baseStyle = base,
            baseSizeSp = baseSize, showSeconds = showSeconds, alignment = settings.alignment,
            scale = sizeScale, weight = settings.weight.value,
    )
}

@Composable
fun ConfiguredClock(
        settings: ClockSettings,
        sizeScale: Float,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val now by rememberClockNow(settings.showSeconds)
    val face = ClockFaceRegistry.get(ClockFaceRegistry.resolveId(settings.faceId, settings.style))
    val spec = rememberClockFaceSpec(settings, sizeScale, now)
    val description = remember(now, spec.is24h, face.id) {
        if (face.id == "word-clock") WordClock.text(now.hour, now.minute)
        else ClockFormatter.format(now, ClockStyle.LARGE_DIGITAL, spec.is24h, false).joined
    }
    Box(
            modifier.fillMaxWidth()
                    .clickableNoRippleWithSystemSound(onClick = onClick)
                    .semantics { contentDescription = description },
    ) {
        face.Render(spec, Modifier)
    }
}

/** The home date line: the existing formatted [default] unless a locale-aware style is chosen. */
@Composable
fun configuredDate(default: String, style: ClockDateStyle): String {
    if (style == ClockDateStyle.SYSTEM) return default
    val now by rememberClockNow(false)
    return ClockFormatter.formatDate(now, java.util.Locale.getDefault(), style) ?: default
}
