package com.lu4p.fokuslauncher.reminders

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.ConfiguredClock
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.LocalHomeLayout
import com.lu4p.fokuslauncher.home.layout.rememberClockNow
import com.lu4p.fokuslauncher.home.widgets.WidgetScale
import com.lu4p.fokuslauncher.reminders.ReminderMath.AgendaItem
import com.lu4p.fokuslauncher.reminders.ReminderMath.GroupKey
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.typography.withElementFont
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

val LocalOpenReminders = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

@Composable
internal fun dayWord(key: GroupKey): String = when (key) {
    GroupKey.Today, GroupKey.Done, GroupKey.Overdue -> stringResource(R.string.reminders_group_today)
    GroupKey.Tomorrow -> stringResource(R.string.reminders_group_tomorrow)
    is GroupKey.Weekday -> weekdayName(key.day)
    is GroupKey.Date -> key.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}

internal fun weekdayName(day: DayOfWeek): String = day.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())

internal fun timeText(ms: Long, zone: ZoneId, is24h: Boolean): String =
        DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault())
                .format(Instant.ofEpochMilli(ms).atZone(zone))

/** "Today 14:00", "Tomorrow 09:30", "Thu 08:00" or "12 Jun 08:00". */
@Composable
internal fun whenLabel(item: AgendaItem, nowMs: Long, is24h: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
    val key = ReminderMath.dayKey(item.whenMs, nowMs, zone)
    val day = if (item.overdue) stringResource(R.string.reminders_group_overdue) else dayWord(key)
    val time = if (item.allDay) stringResource(R.string.reminders_all_day) else timeText(item.whenMs, zone, is24h)
    return "$day $time"
}

/**
 * Home agenda widget. Styles: 0 beside clock, 1 list, 2 compact line, 3 today card. Colours come
 * from the theme, fonts from the typography registry (date font for time labels, notes font for
 * titles), [scale] is the element size and [alignment] applies to styles 1-3.
 */
@Composable
fun AgendaWidget(
        style: Int,
        alignment: ElementAlignment,
        scale: Float,
        onClockClick: () -> Unit,
        modifier: Modifier = Modifier,
        onOpen: () -> Unit = LocalOpenReminders.current,
        viewModel: ReminderViewModel = hiltViewModel(),
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, settings.calendarEnabled) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_START) viewModel.refreshCalendar(settings.calendarEnabled) }
        lifecycle.addObserver(observer)
        viewModel.refreshCalendar(settings.calendarEnabled)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val now by rememberClockNow(false)
    val nowMs = now.toInstant().toEpochMilli()
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val s = style.coerceIn(0, 3)
    val limit = if (s == 2) 1 else 3
    val items = remember(reminders, calendar, nowMs / 60_000L, limit) {
        ReminderMath.selectAgenda(reminders, calendar, nowMs, if (s == 3) 12 else limit)
    }
    val hiddenCount = remember(reminders, calendar, nowMs / 60_000L) {
        (ReminderMath.selectAgenda(reminders, calendar, nowMs, 99).size - 1).coerceAtLeast(0)
    }

    Box(modifier.fillMaxWidth().testTag("widget_agenda")) {
        when (s) {
            0 -> BesideClock(items, nowMs, is24h, scale, onClockClick, onOpen)
            1 -> WidgetScale(scale) { AgendaList(items, nowMs, is24h, alignment, onOpen) }
            2 -> WidgetScale(scale) { AgendaCompact(items.firstOrNull(), hiddenCount, nowMs, is24h, alignment, onOpen) }
            else -> WidgetScale(scale) { TodayCard(items, nowMs, is24h, alignment, onOpen) }
        }
    }
}

@Composable
private fun BesideClock(
        items: List<AgendaItem>,
        nowMs: Long,
        is24h: Boolean,
        scale: Float,
        onClockClick: () -> Unit,
        onOpen: () -> Unit,
) {
    val layout = LocalHomeLayout.current
    val divider = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        ConfiguredClock(
                settings = layout.clock,
                sizeScale = layout.layout.element(com.lu4p.fokuslauncher.home.layout.HomeElementType.CLOCK).scale,
                onClick = onClockClick,
                modifier = Modifier.weight(1.15f),
        )
        Box(Modifier.padding(horizontal = 12.dp).width(1.dp).fillMaxHeight().background(divider))
        Box(Modifier.weight(1f)) {
            WidgetScale(scale) { AgendaCompactColumn(items, nowMs, is24h, onOpen) }
        }
    }
}

@Composable
private fun AgendaCompactColumn(items: List<AgendaItem>, nowMs: Long, is24h: Boolean, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isEmpty()) {
            EmptyText(TextAlign.Start)
        } else {
            items.take(3).forEach { ItemLine(it, nowMs, is24h, ElementAlignment.START, stacked = true) }
        }
    }
}

@Composable
private fun AgendaList(items: List<AgendaItem>, nowMs: Long, is24h: Boolean, alignment: ElementAlignment, onOpen: () -> Unit) {
    Column(
            Modifier.fillMaxWidth().clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = alignment.horizontal(),
    ) {
        if (items.isEmpty()) EmptyText(alignment.textAlign())
        else items.take(3).forEach { ItemLine(it, nowMs, is24h, alignment, stacked = false) }
    }
}

@Composable
private fun AgendaCompact(
        item: AgendaItem?,
        more: Int,
        nowMs: Long,
        is24h: Boolean,
        alignment: ElementAlignment,
        onOpen: () -> Unit,
) {
    Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpen),
            horizontalArrangement = alignment.arrangement(),
            verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item == null) {
            EmptyText(alignment.textAlign())
        } else {
            Text(
                    whenLabel(item, nowMs, is24h),
                    style = MaterialTheme.typography.labelLarge.withElementFont(TextElement.DATE),
                    color = labelColor(item),
            )
            Text(
                    " · " + item.title,
                    style = MaterialTheme.typography.bodyMedium.withElementFont(TextElement.NOTES),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
            )
            if (more > 0) {
                Text(
                        "  " + stringResource(R.string.agenda_more, more),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun TodayCard(items: List<AgendaItem>, nowMs: Long, is24h: Boolean, alignment: ElementAlignment, onOpen: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val today = items.filter { it.overdue || ReminderMath.dayKey(it.whenMs, nowMs, zone) == GroupKey.Today }.take(4)
    val surface = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(surface).clickable(onClick = onOpen).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = alignment.horizontal(),
    ) {
        Text(
                stringResource(R.string.agenda_today_header),
                style = MaterialTheme.typography.labelLarge.withElementFont(TextElement.DATE),
                color = MaterialTheme.colorScheme.primary,
        )
        if (today.isEmpty()) {
            Text(
                    stringResource(if (items.isEmpty()) R.string.agenda_empty else R.string.agenda_nothing_today),
                    style = MaterialTheme.typography.bodyMedium.withElementFont(TextElement.NOTES),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = alignment.textAlign(),
            )
        } else {
            today.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            if (item.allDay) stringResource(R.string.reminders_all_day) else timeText(item.whenMs, zone, is24h),
                            style = MaterialTheme.typography.labelLarge.withElementFont(TextElement.DATE),
                            color = labelColor(item),
                            modifier = Modifier.width(64.dp),
                    )
                    Text(
                            item.title,
                            style = MaterialTheme.typography.bodyMedium.withElementFont(TextElement.NOTES),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** One agenda entry: a small time label over the title (stacked) or label and title side by side. */
@Composable
private fun ItemLine(item: AgendaItem, nowMs: Long, is24h: Boolean, alignment: ElementAlignment, stacked: Boolean) {
    val label = whenLabel(item, nowMs, is24h)
    val labelStyle = MaterialTheme.typography.labelMedium.withElementFont(TextElement.DATE)
    val titleStyle = MaterialTheme.typography.bodyMedium.withElementFont(TextElement.NOTES)
    val titleColor = MaterialTheme.colorScheme.onBackground
    if (stacked) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = labelStyle, color = labelColor(item), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.title, style = titleStyle, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = alignment.arrangement(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = labelStyle.copy(fontWeight = FontWeight.Medium), color = labelColor(item), maxLines = 1)
            Text(
                    "  " + item.title, style = titleStyle, color = titleColor, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun labelColor(item: AgendaItem): Color =
        if (item.overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

@Composable
private fun EmptyText(align: TextAlign) {
    Text(
            stringResource(R.string.agenda_empty),
            style = MaterialTheme.typography.bodyMedium.withElementFont(TextElement.NOTES),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = align,
    )
}

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

private fun ElementAlignment.textAlign(): TextAlign = when (this) {
    ElementAlignment.START -> TextAlign.Start
    ElementAlignment.CENTER -> TextAlign.Center
    ElementAlignment.END -> TextAlign.End
}
