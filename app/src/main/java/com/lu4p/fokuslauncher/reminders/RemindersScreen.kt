package com.lu4p.fokuslauncher.reminders

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.rememberClockNow
import com.lu4p.fokuslauncher.reminders.ReminderMath.GroupKey
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private fun hasNotificationPermission(context: android.content.Context) =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: ReminderViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val now by rememberClockNow(false)
    val nowMs = now.toInstant().toEpochMilli()
    val zone = ZoneId.systemDefault()
    val is24h = DateFormat.is24HourFormat(context)
    val groups = remember(reminders, nowMs / 60_000L) { ReminderMath.group(reminders, nowMs, zone) }

    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Reminder?>(null) }
    var pickForDraft by remember { mutableStateOf(false) }
    var showCalendarWhy by remember { mutableStateOf(false) }
    var calendarDenied by remember { mutableStateOf(false) }
    var calendarGranted by remember { mutableStateOf(CalendarReader.hasPermission(context)) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarGranted = granted
        calendarDenied = !granted
        viewModel.setCalendarEnabled(granted)
    }

    fun addAt(ms: Long) {
        val title = draft.trim()
        if (title.isEmpty()) return
        val r = viewModel.newReminder(title, ms)
        viewModel.save(r)
        draft = ""
        if (r.notify && !hasNotificationPermission(context)) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Box(modifier.fillMaxSize().background(backgroundScrim).testTag("reminders_screen")) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            FokusSettingsTopBar(
                    title = { Text(stringResource(R.string.reminders_title)) },
                    onNavigateBack = onNavigateBack,
                    containerColor = Color.Transparent,
            )
            LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it.take(120) },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.reminders_new_hint)) },
                                modifier = Modifier.fillMaxWidth().testTag("reminder_draft"),
                                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val enabled = draft.isNotBlank()
                            AssistChip(onClick = { addAt(ReminderMath.inOneHour(nowMs)) }, enabled = enabled,
                                    label = { Text(stringResource(R.string.reminders_chip_hour)) })
                            AssistChip(onClick = { addAt(ReminderMath.thisEvening(nowMs, zone)) }, enabled = enabled,
                                    label = { Text(stringResource(R.string.reminders_chip_evening)) })
                            AssistChip(onClick = { addAt(ReminderMath.tomorrowAt(nowMs, zone)) }, enabled = enabled,
                                    label = { Text(stringResource(R.string.reminders_chip_tomorrow)) })
                            AssistChip(onClick = { pickForDraft = true }, enabled = enabled,
                                    label = { Text(stringResource(R.string.reminders_chip_pick)) })
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item {
                        Text(
                                stringResource(R.string.reminders_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                groups.forEach { group ->
                    item(key = "h-${group.key}") {
                        Text(
                                groupTitle(group.key),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (group.key == GroupKey.Overdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }
                    items(group.items, key = { "${it.id}-${it.dueEpochMs}-${it.done}" }) { r ->
                        ReminderRow(
                                r, nowMs, zone, is24h,
                                onToggle = { viewModel.toggleDone(r.id) },
                                onClick = { editing = r },
                        )
                    }
                }
                item { PrivacySection(settings.showFullTextOnLock, viewModel::setShowFullTextOnLock) }
                item {
                    CalendarSection(
                            enabled = settings.calendarEnabled,
                            granted = calendarGranted,
                            denied = calendarDenied,
                            onToggle = { on ->
                                if (!on) viewModel.setCalendarEnabled(false)
                                else if (calendarGranted) viewModel.setCalendarEnabled(true)
                                else showCalendarWhy = true
                            },
                            onAskAgain = { showCalendarWhy = true },
                    )
                }
                item { Box(Modifier.size(48.dp)) }
            }
        }
    }

    if (showCalendarWhy) {
        // Explain first; only the Continue button triggers the system permission dialog.
        AlertDialog(
                onDismissRequest = { showCalendarWhy = false },
                title = { Text(stringResource(R.string.reminders_calendar_why_title)) },
                text = { Text(stringResource(R.string.reminders_calendar_why_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        showCalendarWhy = false
                        calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                    }) { Text(stringResource(R.string.reminders_calendar_continue)) }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarWhy = false }) { Text(stringResource(R.string.reminders_cancel)) }
                },
        )
    }

    if (pickForDraft) {
        DateTimePickerFlow(
                initialMs = ReminderMath.inOneHour(nowMs),
                zone = zone,
                is24h = is24h,
                onDismiss = { pickForDraft = false },
                onPicked = { ms -> pickForDraft = false; addAt(ms) },
        )
    }

    editing?.let { current ->
        EditReminderDialog(
                initial = current,
                zone = zone,
                is24h = is24h,
                onDismiss = { editing = null },
                onSave = { viewModel.save(it); editing = null },
                onDelete = { viewModel.delete(current.id); editing = null },
                onNeedNotificationPermission = {
                    if (!hasNotificationPermission(context)) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
        )
    }
    LaunchedEffect(Unit) { calendarGranted = CalendarReader.hasPermission(context) }
}

@Composable
private fun groupTitle(key: GroupKey): String = when (key) {
    GroupKey.Overdue -> stringResource(R.string.reminders_group_overdue)
    GroupKey.Today -> stringResource(R.string.reminders_group_today)
    GroupKey.Tomorrow -> stringResource(R.string.reminders_group_tomorrow)
    GroupKey.Done -> stringResource(R.string.reminders_group_done)
    is GroupKey.Weekday -> key.day.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
    is GroupKey.Date -> java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).format(key.date)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(r: Reminder, nowMs: Long, zone: ZoneId, is24h: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    val dismiss = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it != SwipeToDismissBoxValue.Settled) onToggle()
        false // the list re-keys itself; never leave the row half-swiped
    })
    SwipeToDismissBox(
            state = dismiss,
            backgroundContent = {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.CenterEnd) {
                    Text(
                            stringResource(if (r.done) R.string.reminders_reopen else R.string.reminders_complete),
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 16.dp),
                    )
                }
            },
            modifier = Modifier.testTag("reminder_row"),
    ) {
        Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
                        .clickable(onClick = onClick).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(checked = r.done, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                        r.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (r.done) 0.5f else 1f),
                        textDecoration = if (r.done) TextDecoration.LineThrough else null,
                )
                val time = timeText(r.effectiveDueMs, zone, is24h)
                val repeat = repeatLabel(r.repeat)
                Text(
                        listOfNotNull(time, repeat.takeIf { r.repeat != RepeatRule.NONE }, r.note.takeIf { it.isNotBlank() })
                                .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ReminderMath.isOverdue(r, nowMs)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun repeatLabel(rule: RepeatRule): String = stringResource(
        when (rule) {
            RepeatRule.NONE -> R.string.reminders_repeat_none
            RepeatRule.DAILY -> R.string.reminders_repeat_daily
            RepeatRule.WEEKDAYS -> R.string.reminders_repeat_weekdays
            RepeatRule.WEEKLY -> R.string.reminders_repeat_weekly
            RepeatRule.MONTHLY -> R.string.reminders_repeat_monthly
        },
)

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f))
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun PrivacySection(showFull: Boolean, onChange: (Boolean) -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(stringResource(R.string.reminders_privacy_header), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        SwitchRow(
                stringResource(R.string.reminders_lock_full_text),
                stringResource(R.string.reminders_lock_full_text_subtitle),
                showFull, onChange, "reminders_lock_full_text",
        )
    }
}

@Composable
private fun CalendarSection(enabled: Boolean, granted: Boolean, denied: Boolean, onToggle: (Boolean) -> Unit, onAskAgain: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.padding(top = 16.dp)) {
        Text(stringResource(R.string.reminders_calendar_header), style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        SwitchRow(
                stringResource(R.string.reminders_calendar_toggle),
                stringResource(R.string.reminders_calendar_toggle_subtitle),
                enabled && granted, onToggle, "reminders_calendar_toggle",
        )
        if ((enabled && !granted) || denied) {
            Text(stringResource(R.string.reminders_calendar_denied), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            Row {
                TextButton(onClick = onAskAgain) { Text(stringResource(R.string.reminders_calendar_grant_again)) }
                TextButton(onClick = {
                    context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.reminders_open_app_settings)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerFlow(initialMs: Long, zone: ZoneId, is24h: Boolean, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val initial = Instant.ofEpochMilli(initialMs).atZone(zone)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    if (pickedDate == null) {
        val dateState = rememberDatePickerState(
                initialSelectedDateMillis = initial.toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { pickedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
                    }) { Text(stringResource(R.string.reminders_ok)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminders_cancel)) } },
        ) { DatePicker(state = dateState) }
    } else {
        val timeState = rememberTimePickerState(initial.hour, initial.minute, is24h)
        AlertDialog(
                onDismissRequest = onDismiss,
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(onClick = {
                        val ms = ZonedDateTime.of(pickedDate, LocalTime.of(timeState.hour, timeState.minute), zone)
                                .toInstant().toEpochMilli()
                        onPicked(ms)
                    }) { Text(stringResource(R.string.reminders_ok)) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminders_cancel)) } },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditReminderDialog(
        initial: Reminder,
        zone: ZoneId,
        is24h: Boolean,
        onDismiss: () -> Unit,
        onSave: (Reminder) -> Unit,
        onDelete: () -> Unit,
        onNeedNotificationPermission: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var dueMs by remember { mutableStateOf(initial.dueEpochMs) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var notify by remember { mutableStateOf(initial.notify) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.reminders_edit_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it.take(120) }, label = { Text(stringResource(R.string.reminders_field_title)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(note, { note = it.take(500) }, label = { Text(stringResource(R.string.reminders_field_note)) },
                            modifier = Modifier.fillMaxWidth())
                    val dt = Instant.ofEpochMilli(dueMs).atZone(zone)
                    TextButton(onClick = { picking = true }) {
                        Text(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).format(dt) +
                                "  " + timeText(dueMs, zone, is24h))
                    }
                    Text(stringResource(R.string.reminders_repeat), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RepeatRule.entries.forEach { rule ->
                            FilterChip(selected = repeat == rule, onClick = { repeat = rule }, label = { Text(repeatLabel(rule)) })
                        }
                    }
                    SwitchRow(stringResource(R.string.reminders_notify), null, notify, {
                        notify = it
                        if (it) onNeedNotificationPermission()
                    }, "reminder_notify")
                }
            },
            confirmButton = {
                TextButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onSave(initial.copy(title = title.trim(), note = note.trim(), dueEpochMs = dueMs,
                                    zoneId = zone.id, repeat = repeat, notify = notify, snoozeUntilMs = 0L))
                        },
                ) { Text(stringResource(R.string.reminders_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.reminders_delete), color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.reminders_cancel)) }
                }
            },
    )
    if (picking) {
        DateTimePickerFlow(dueMs, zone, is24h, onDismiss = { picking = false }, onPicked = { dueMs = it; picking = false })
    }
}
