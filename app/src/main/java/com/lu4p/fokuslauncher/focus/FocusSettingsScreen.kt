package com.lu4p.fokuslauncher.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.PomodoroPhase
import com.lu4p.fokuslauncher.data.model.drawerOpenCountKey
import com.lu4p.fokuslauncher.data.repository.AppRepository
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.pomodoro.PomodoroUiState
import com.lu4p.fokuslauncher.theme.ThemeStore
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FocusAppOption(val key: String, val label: String)

@HiltViewModel
class FocusViewModel
@Inject
constructor(
        private val controller: FocusController,
        private val appRepository: AppRepository,
        themeStore: ThemeStore,
) : ViewModel() {
    val state: StateFlow<FocusState> =
            controller.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusState())
    val session: StateFlow<PomodoroUiState> = controller.sessionState
    val themes: StateFlow<List<Pair<String, String>>> =
            kotlinx.coroutines.flow.flow {
                        themeStore.state.collect { s -> emit(s.allThemes.map { it.id to it.name }) }
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _apps = MutableStateFlow<List<FocusAppOption>>(emptyList())
    val apps: StateFlow<List<FocusAppOption>> = _apps

    fun loadApps() {
        viewModelScope.launch {
            _apps.value =
                    withContext(Dispatchers.Default) {
                        appRepository.getInstalledApps()
                                .filter { it.launcherShortcutId == null }
                                .map {
                                    FocusAppOption(
                                            drawerOpenCountKey(it.packageName, it.userHandle),
                                            it.label,
                                    )
                                }
                                .distinctBy { it.key }
                                .sortedBy { it.label.lowercase() }
                    }
        }
    }

    fun activate(id: String) = viewModelScope.launch { controller.activate(id) }
    fun deactivate() = viewModelScope.launch { controller.deactivate() }
    fun save(p: FocusProfile) = viewModelScope.launch { controller.saveProfile(p) }
    fun startSession() = viewModelScope.launch { controller.startSession() }
    fun pauseOrResume() = controller.pauseOrResumeSession()
    fun endSession() = controller.endSession()
}

private val DAY_LABELS = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

private fun fmt(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

@Composable
fun FocusSettingsScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: FocusViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val themes by viewModel.themes.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var editingId by remember { mutableStateOf<String?>(null) }
    var showApps by remember { mutableStateOf(false) }
    val editing = state.profiles.firstOrNull { it.id == editingId }
    val active = state.active

    Column(modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding().testTag("focus_screen")) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.focus_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                    stringResource(R.string.focus_honest_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("focus_notice"),
            )
            SettingsToggleRow(
                    label = stringResource(R.string.focus_enabled),
                    subtitle =
                            active?.let { stringResource(R.string.focus_active_profile, it.name) }
                                    ?: stringResource(R.string.focus_off_subtitle),
                    checked = active != null,
                    onCheckedChange = { on ->
                        if (on) viewModel.activate((editing ?: state.profiles.first()).id) else viewModel.deactivate()
                    },
            )
            if (active != null && active.reminder.isNotBlank()) {
                Text(
                        active.reminder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }

            SectionHeader(stringResource(R.string.focus_section_profiles))
            state.profiles.forEach { p ->
                SettingsRow(
                        label = p.name,
                        subtitle =
                                when {
                                    p.id == active?.id -> stringResource(R.string.focus_profile_active)
                                    p.id == editingId -> stringResource(R.string.focus_profile_editing)
                                    else -> null
                                },
                        onClick = { editingId = if (editingId == p.id) null else p.id },
                )
            }

            if (editing != null) {
                ProfileEditor(
                        profile = editing,
                        themes = themes,
                        isActive = editing.id == active?.id,
                        onSave = viewModel::save,
                        onActivate = { viewModel.activate(editing.id) },
                        onChooseApps = {
                            viewModel.loadApps()
                            showApps = !showApps
                        },
                )
                if (showApps) {
                    Text(
                            stringResource(R.string.focus_apps_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                    apps.forEach { app ->
                        val checked = app.key in editing.visibleApps
                        Row(
                                Modifier.fillMaxWidth()
                                        .clickable {
                                            val next =
                                                    if (checked) editing.visibleApps - app.key
                                                    else editing.visibleApps + app.key
                                            viewModel.save(editing.copy(visibleApps = next))
                                        }
                                        .padding(horizontal = 24.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(
                                    app.label,
                                    modifier = Modifier.padding(start = 12.dp),
                                    color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.focus_section_session))
            val running = session.phase != PomodoroPhase.IDLE
            Text(
                    if (running) session.remainingText else stringResource(R.string.focus_session_idle),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!running) {
                    TextButton(onClick = viewModel::startSession) { Text(stringResource(R.string.focus_session_start)) }
                } else {
                    TextButton(onClick = viewModel::pauseOrResume) {
                        Text(
                                stringResource(
                                        if (session.isRunning) R.string.focus_session_pause
                                        else R.string.focus_session_resume
                                )
                        )
                    }
                    TextButton(onClick = viewModel::endSession) { Text(stringResource(R.string.focus_session_end)) }
                }
            }
            Text(
                    stringResource(R.string.focus_session_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun ProfileEditor(
        profile: FocusProfile,
        themes: List<Pair<String, String>>,
        isActive: Boolean,
        onSave: (FocusProfile) -> Unit,
        onActivate: () -> Unit,
        onChooseApps: () -> Unit,
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var reminder by remember(profile.id) { mutableStateOf(profile.reminder) }
    val sched = profile.schedule
    SectionHeader(stringResource(R.string.focus_section_edit, profile.name))
    if (!isActive) {
        SettingsRow(label = stringResource(R.string.focus_activate), onClick = onActivate)
    }
    OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(MAX_FOCUS_NAME)
                if (name.isNotBlank()) onSave(profile.copy(name = name))
            },
            label = { Text(stringResource(R.string.focus_field_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
    )
    OutlinedTextField(
            value = reminder,
            onValueChange = {
                reminder = it.take(MAX_FOCUS_REMINDER)
                onSave(profile.copy(reminder = reminder))
            },
            label = { Text(stringResource(R.string.focus_field_reminder)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    )
    SettingsRow(
            label = stringResource(R.string.focus_choose_apps),
            subtitle =
                    if (profile.visibleApps.isEmpty()) stringResource(R.string.focus_apps_all)
                    else stringResource(R.string.focus_apps_count, profile.visibleApps.size),
            onClick = onChooseApps,
    )
    val presets = listOf<LayoutPreset?>(null) + LayoutPreset.entries.filter { it != LayoutPreset.CUSTOM }
    SettingsRow(
            label = stringResource(R.string.focus_layout),
            subtitle = profile.layoutPreset?.name ?: stringResource(R.string.focus_unchanged),
            onClick = {
                val i = presets.indexOf(profile.layoutPreset)
                onSave(profile.copy(layoutPreset = presets[(i + 1) % presets.size]))
            },
    )
    val themeIds = listOf<String?>(null) + themes.map { it.first }
    SettingsRow(
            label = stringResource(R.string.focus_theme),
            subtitle =
                    profile.themeId?.let { id -> themes.firstOrNull { it.first == id }?.second ?: id }
                            ?: stringResource(R.string.focus_unchanged),
            onClick = {
                val i = themeIds.indexOf(profile.themeId).coerceAtLeast(0)
                onSave(profile.copy(themeId = themeIds[(i + 1) % themeIds.size]))
            },
    )
    val sessionOptions = listOf<Int?>(null, 15, 25, 45, 60, 90)
    SettingsRow(
            label = stringResource(R.string.focus_session_length),
            subtitle =
                    profile.sessionMinutes?.let { stringResource(R.string.focus_minutes, it) }
                            ?: stringResource(R.string.focus_unchanged),
            onClick = {
                val i = sessionOptions.indexOf(profile.sessionMinutes).let { if (it < 0) 0 else it }
                onSave(profile.copy(sessionMinutes = sessionOptions[(i + 1) % sessionOptions.size]))
            },
    )
    SettingsToggleRow(
            label = stringResource(R.string.focus_schedule),
            subtitle = stringResource(R.string.focus_schedule_hint),
            checked = sched.enabled,
            onCheckedChange = { onSave(profile.copy(schedule = sched.copy(enabled = it))) },
    )
    if (sched.enabled) {
        Row(Modifier.padding(horizontal = 16.dp)) {
            DAY_LABELS.forEach { (d, label) ->
                val on = d in sched.days
                TextButton(
                        onClick = {
                            val days = if (on) sched.days - d else sched.days + d
                            onSave(profile.copy(schedule = sched.copy(days = days)))
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                            label,
                            color =
                                    if (on) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        SettingsRow(
                label = stringResource(R.string.focus_start_time),
                subtitle = fmt(sched.startMinute) + "  " + stringResource(R.string.focus_tap_step),
                onClick = {
                    onSave(profile.copy(schedule = sched.copy(startMinute = (sched.startMinute + 30) % 1440)))
                },
        )
        SettingsRow(
                label = stringResource(R.string.focus_end_time),
                subtitle = fmt(sched.endMinute) + "  " + stringResource(R.string.focus_tap_step),
                onClick = {
                    onSave(profile.copy(schedule = sched.copy(endMinute = (sched.endMinute + 30) % 1440)))
                },
        )
    }
}
