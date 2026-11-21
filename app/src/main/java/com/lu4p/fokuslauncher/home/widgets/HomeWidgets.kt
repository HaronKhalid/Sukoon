package com.lu4p.fokuslauncher.home.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.HomeElementType
import com.lu4p.fokuslauncher.home.layout.rememberClockNow
import com.lu4p.fokuslauncher.pomodoro.PomodoroUiState
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.typography.withElementFont
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.YearMonth
import java.util.Locale

/** Battery reading used by the battery ring. Home passes its existing values; Studio reads live. */
data class BatteryInfo(val percent: Int, val charging: Boolean)

/** Live battery from the sticky ACTION_BATTERY_CHANGED broadcast (no permission needed). */
@Composable
fun rememberBatteryInfo(): BatteryInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(readBattery(context.registerSticky(null))) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                info = readBattery(intent)
            }
        }
        ContextCompat.registerReceiver(
                context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return info
}

private fun Context.registerSticky(receiver: BroadcastReceiver?): Intent? =
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

private fun readBattery(intent: Intent?): BatteryInfo {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryInfo(percent, charging)
}

/** Titles and style names for the gallery and editors. */
object WidgetCatalog {
    val addable: List<HomeElementType> = HomeElementType.entries.filter { it.optionalWidget }

    @StringRes
    fun title(type: HomeElementType): Int = when (type) {
        HomeElementType.DATE_CARD -> R.string.widget_date_card
        HomeElementType.BATTERY_RING -> R.string.widget_battery_ring
        HomeElementType.MONTH_CALENDAR -> R.string.widget_month_calendar
        HomeElementType.FOCUS_RING -> R.string.widget_focus_ring
        HomeElementType.GREETING -> R.string.widget_greeting
        HomeElementType.PROGRESS_BAR -> R.string.widget_progress_bar
        else -> R.string.widget_generic
    }

    fun styleLabels(type: HomeElementType): List<Int> = when (type) {
        HomeElementType.DATE_CARD -> listOf(R.string.widget_style_card, R.string.widget_style_inline, R.string.widget_style_outline)
        HomeElementType.BATTERY_RING -> listOf(R.string.widget_style_ring, R.string.widget_style_ring_label, R.string.widget_style_bar)
        HomeElementType.MONTH_CALENDAR -> listOf(R.string.widget_style_month, R.string.widget_style_week)
        HomeElementType.FOCUS_RING -> listOf(R.string.widget_style_ring, R.string.widget_style_bar)
        HomeElementType.GREETING -> listOf(R.string.widget_style_plain, R.string.widget_style_large, R.string.widget_style_with_date)
        HomeElementType.PROGRESS_BAR -> listOf(R.string.widget_style_day, R.string.widget_style_week_progress, R.string.widget_style_both)
        else -> listOf(R.string.widget_style_plain)
    }
}

/** Scales sp-based text and sp-derived sizes inside [content]; used for the continuous size scale. */
@Composable
fun WidgetScale(scale: Float, content: @Composable () -> Unit) {
    if (scale == 1f) {
        content()
    } else {
        val density = LocalDensity.current
        CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale * scale),
                content = content,
        )
    }
}

@Composable
private fun spDp(value: Float): Dp = with(LocalDensity.current) { value.sp.toDp() }

private fun ElementAlignment.horizontal(): Alignment.Horizontal = when (this) {
    ElementAlignment.START -> Alignment.Start
    ElementAlignment.CENTER -> Alignment.CenterHorizontally
    ElementAlignment.END -> Alignment.End
}

private fun ElementAlignment.arrangement(): Arrangement.Horizontal = when (this) {
    ElementAlignment.START -> Arrangement.Start
    ElementAlignment.CENTER -> Arrangement.Center
    ElementAlignment.END -> Arrangement.End
}

/** Renders one gallery widget. Sizes follow the ambient font scale, so wrap in [WidgetScale]. */
@Composable
fun HomeWidget(
        type: HomeElementType,
        style: Int,
        alignment: ElementAlignment,
        battery: BatteryInfo,
        pomodoro: PomodoroUiState,
        onFocusClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val s = style.coerceIn(0, type.styleCount - 1)
    when (type) {
        HomeElementType.DATE_CARD -> DateCardWidget(s, alignment, modifier)
        HomeElementType.BATTERY_RING -> BatteryRingWidget(battery, s, alignment, modifier)
        HomeElementType.MONTH_CALENDAR -> MonthCalendarWidget(s, alignment, modifier)
        HomeElementType.FOCUS_RING -> FocusRingWidget(pomodoro, s, alignment, onFocusClick, modifier)
        HomeElementType.GREETING -> GreetingWidget(s, alignment, modifier)
        HomeElementType.PROGRESS_BAR -> ProgressWidget(s, alignment, modifier)
        else -> Unit
    }
}

@Composable
fun DateCardWidget(style: Int, alignment: ElementAlignment, modifier: Modifier = Modifier) {
    val now by rememberClockNow(false)
    val locale = Locale.getDefault()
    val weekday = now.dayOfWeek.getDisplayName(DateTextStyle.FULL, locale)
    val month = now.month.getDisplayName(DateTextStyle.FULL, locale)
    val color = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val day = now.dayOfMonth.toString()
    val numberStyle = MaterialTheme.typography.displayLarge.withElementFont(TextElement.DATE)
    when (style) {
        1 -> Row(modifier.fillMaxWidth().testTag("widget_date_card"), horizontalArrangement = alignment.arrangement(),
                verticalAlignment = Alignment.CenterVertically) {
            Text(day, style = numberStyle.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold), color = accent)
            Column(Modifier.padding(start = 10.dp)) {
                Text(weekday, style = MaterialTheme.typography.titleMedium, color = color)
                Text(month, style = MaterialTheme.typography.bodyMedium, color = color.copy(alpha = 0.7f))
            }
        }
        2 -> Column(modifier.fillMaxWidth().testTag("widget_date_card"), horizontalAlignment = alignment.horizontal()) {
            Text(day, style = numberStyle.copy(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.Black,
                    drawStyle = Stroke(width = 4f)), color = accent)
            Text("$weekday · $month", style = MaterialTheme.typography.labelLarge, color = color.copy(alpha = 0.8f))
        }
        else -> Column(modifier.fillMaxWidth().testTag("widget_date_card"), horizontalAlignment = alignment.horizontal()) {
            Text(weekday.uppercase(locale), style = MaterialTheme.typography.labelLarge, color = accent,
                    letterSpacing = 2.sp)
            Text(day, style = numberStyle.copy(fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Light), color = color)
            Text(month, style = MaterialTheme.typography.titleMedium, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun Text(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, letterSpacing: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) =
        androidx.compose.material3.Text(text, style = style.copy(letterSpacing = letterSpacing), color = color, modifier = modifier)

@Composable
fun BatteryRingWidget(battery: BatteryInfo, style: Int, alignment: ElementAlignment, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    val low = battery.percent <= 15 && !battery.charging
    val arc = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val label = "${battery.percent}%"
    val fraction = battery.percent / 100f
    val state = stringResource(if (battery.charging) R.string.widget_battery_charging else R.string.widget_battery)
    Row(modifier.fillMaxWidth().testTag("widget_battery_ring"), horizontalArrangement = alignment.arrangement(),
            verticalAlignment = Alignment.CenterVertically) {
        when (style) {
            2 -> Column(Modifier.widthIn(max = spDp(220f))) {
                Text("$state $label", style = MaterialTheme.typography.labelLarge, color = color)
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(spDp(8f)).clip(CircleShape)
                        .background(color.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0.02f, 1f)).clip(CircleShape).background(arc))
                }
            }
            else -> {
                Ring(fraction, arc, color.copy(alpha = 0.18f), spDp(if (style == 0) 72f else 40f), spDp(if (style == 0) 7f else 4f)) {
                    if (style == 0) Text(label, style = MaterialTheme.typography.titleMedium, color = color)
                }
                if (style == 1) {
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(label, style = MaterialTheme.typography.titleLarge, color = color)
                        Text(state, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Ring(fraction: Float, color: Color, track: Color, size: Dp, stroke: Dp, center: @Composable () -> Unit = {}) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = stroke.toPx()
            val inset = w / 2f
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize,
                    style = Stroke(w, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
fun MonthCalendarWidget(style: Int, alignment: ElementAlignment, modifier: Modifier = Modifier) {
    val now by rememberClockNow(false)
    val locale = Locale.getDefault()
    val start = CalendarMath.weekStart(locale)
    val today = now.toLocalDate()
    val color = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val cell = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center)
    val title = DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(today)

    @Composable
    fun Cell(text: String, today: Boolean, dim: Boolean, modifier: Modifier) {
        Box(modifier.height(spDp(28f)), contentAlignment = Alignment.Center) {
            val chip = if (today) Modifier.size(spDp(26f)).background(accent, CircleShape) else Modifier
            Box(chip, contentAlignment = Alignment.Center) {
                Text(text, style = cell, maxLines = 1,
                        color = if (today) MaterialTheme.colorScheme.onPrimary else color.copy(alpha = if (dim) 0.5f else 0.9f))
            }
        }
    }

    Column(modifier.fillMaxWidth().testTag("widget_month_calendar"), horizontalAlignment = alignment.horizontal()) {
        Column(Modifier.widthIn(max = spDp(280f)).fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = color,
                    modifier = Modifier.padding(bottom = 4.dp))
            Row(Modifier.fillMaxWidth()) {
                CalendarMath.weekdays(start).forEach {
                    Cell(it.getDisplayName(DateTextStyle.NARROW, locale), false, true, Modifier.weight(1f))
                }
            }
            if (style == 1) {
                Row(Modifier.fillMaxWidth()) {
                    CalendarMath.weekOf(today, start).forEach {
                        Cell(it.dayOfMonth.toString(), it == today, it.month != today.month, Modifier.weight(1f))
                    }
                }
            } else {
                CalendarMath.grid(YearMonth.from(today), start).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Cell(d?.toString().orEmpty(), d == today.dayOfMonth, false, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusRingWidget(
        pomodoro: PomodoroUiState,
        style: Int,
        alignment: ElementAlignment,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onBackground
    val arc = MaterialTheme.colorScheme.primary
    val fraction = pomodoro.progress.coerceIn(0f, 1f)
    Row(modifier.fillMaxWidth().clickable(onClick = onClick).testTag("widget_focus_ring"),
            horizontalArrangement = alignment.arrangement(), verticalAlignment = Alignment.CenterVertically) {
        if (style == 1) {
            Column(Modifier.widthIn(max = spDp(240f))) {
                Text(stringResource(R.string.widget_focus_label, pomodoro.remainingText),
                        style = MaterialTheme.typography.titleMedium, color = color)
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(spDp(8f)).clip(CircleShape)
                        .background(color.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.02f)).clip(CircleShape).background(arc))
                }
            }
        } else {
            Ring(fraction, arc, color.copy(alpha = 0.18f), spDp(96f), spDp(8f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pomodoro.remainingText, style = MaterialTheme.typography.titleLarge, color = color)
                    Text(stringResource(if (pomodoro.isRunning) R.string.widget_focus_running else R.string.widget_focus_idle),
                            style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun GreetingWidget(style: Int, alignment: ElementAlignment, modifier: Modifier = Modifier) {
    val now by rememberClockNow(false)
    val locale = Locale.getDefault()
    val part = Greeting.dayPart(now.hour)
    val text = if (Greeting.isUrdu(locale)) Greeting.urdu(part) else stringResource(when (part) {
        DayPart.MORNING -> R.string.widget_greeting_morning
        DayPart.AFTERNOON -> R.string.widget_greeting_afternoon
        DayPart.EVENING -> R.string.widget_greeting_evening
        DayPart.NIGHT -> R.string.widget_greeting_night
    })
    val color = MaterialTheme.colorScheme.onBackground
    val align = when (alignment) {
        ElementAlignment.START -> TextAlign.Start
        ElementAlignment.CENTER -> TextAlign.Center
        ElementAlignment.END -> TextAlign.End
    }
    Column(modifier.fillMaxWidth().testTag("widget_greeting"), horizontalAlignment = alignment.horizontal()) {
        when (style) {
            1 -> Text(text, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light, textAlign = align),
                    color = MaterialTheme.colorScheme.primary)
            else -> Text(text, style = MaterialTheme.typography.headlineSmall.copy(textAlign = align), color = color)
        }
        if (style == 2) {
            Text(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(locale).format(now),
                    style = MaterialTheme.typography.bodyMedium.copy(textAlign = align), color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ProgressWidget(style: Int, alignment: ElementAlignment, modifier: Modifier = Modifier) {
    val now by rememberClockNow(false)
    val start = CalendarMath.weekStart(Locale.getDefault())
    val local = now.toLocalDateTime()
    val color = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary

    @Composable
    fun Bar(label: String, fraction: Float) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("$label ${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(spDp(6f)).clip(CircleShape)
                    .background(color.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceAtLeast(0.01f)).clip(RoundedCornerShape(50)).background(accent))
            }
        }
    }
    Column(modifier.fillMaxWidth().testTag("widget_progress_bar"), horizontalAlignment = alignment.horizontal()) {
        Column(Modifier.widthIn(max = spDp(280f)).fillMaxWidth()) {
            if (style == 0 || style == 2) Bar(stringResource(R.string.widget_progress_day), ProgressMath.dayFraction(local))
            if (style == 1 || style == 2) Bar(stringResource(R.string.widget_progress_week), ProgressMath.weekFraction(local, start))
        }
    }
}
