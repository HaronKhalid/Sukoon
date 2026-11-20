package com.lu4p.fokuslauncher.focus

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import com.lu4p.fokuslauncher.data.model.PomodoroMode
import com.lu4p.fokuslauncher.home.layout.HomeLayoutCodec
import com.lu4p.fokuslauncher.home.layout.HomeLayoutConfig
import com.lu4p.fokuslauncher.home.layout.HomeLayoutRepository
import com.lu4p.fokuslauncher.pomodoro.PomodoroRepository
import com.lu4p.fokuslauncher.theme.ThemeStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide read-only view of which apps focus allows, as `profile|package` keys. Null means
 * focus is off. This is the single additive hook the drawer and home favourites consult.
 */
object FocusVisibility {
    private val _allowed = MutableStateFlow<Set<String>?>(null)
    val allowed: StateFlow<Set<String>?> = _allowed.asStateFlow()

    internal fun publish(keys: Set<String>?) {
        _allowed.value = keys?.takeIf { it.isNotEmpty() }
    }
}

data class FocusState(
        val profiles: List<FocusProfile> = FocusDefaults.profiles(),
        val runtime: FocusRuntime = FocusRuntime(),
) {
    val active: FocusProfile? get() = runtime.activeId?.let { id -> profiles.firstOrNull { it.id == id } }
}

/**
 * Owns focus profile persistence and activation. Schedules are only evaluated in [onAppResume];
 * there is no service, timer or alarm. Sessions reuse [PomodoroRepository].
 */
@Singleton
class FocusController
internal constructor(
        private val store: DataStore<Preferences>,
        private val layoutRepository: HomeLayoutRepository,
        private val themeStore: ThemeStore,
        private val preferencesManager: PreferencesManager,
        private val pomodoro: PomodoroRepository,
) {
    @Inject
    constructor(
            @ApplicationContext context: Context,
            layoutRepository: HomeLayoutRepository,
            themeStore: ThemeStore,
            preferencesManager: PreferencesManager,
            pomodoro: PomodoroRepository,
    ) : this(context.fokusLauncherPreferencesDataStore, layoutRepository, themeStore, preferencesManager, pomodoro)

    private val mutex = Mutex()
    private var published = false

    val state: Flow<FocusState> =
            store.data.map { prefs ->
                        FocusState(
                                profiles = FocusCodec.decodeOrDefault(prefs[PROFILES_KEY]),
                                runtime =
                                        FocusRuntime(
                                                activeId = prefs[ACTIVE_KEY]?.takeIf { FocusKind.fromId(it) != null },
                                                bySchedule = prefs[BY_SCHEDULE_KEY] ?: false,
                                                suppressedId = prefs[SUPPRESSED_KEY]?.takeIf { it.isNotBlank() },
                                        ),
                        )
                    }
                    .distinctUntilChanged()

    private suspend fun current(): FocusState = state.first()

    /** Call from the activity's onResume: evaluates schedules lazily and republishes visibility. */
    suspend fun onAppResume(now: Calendar = Calendar.getInstance()) {
        mutex.withLock {
            val s = current()
            val isoDay = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val next = resolveOnResume(s.runtime, s.profiles, isoDay, minute)
            if (next != s.runtime) persistRuntime(next)
            if (next.activeId != s.runtime.activeId || !published) {
                applyEffects(s.profiles, next.activeId)
            } else {
                FocusVisibility.publish(s.active?.visibleApps)
            }
            published = true
        }
    }

    suspend fun activate(id: String) {
        mutex.withLock {
            val s = current()
            if (s.profiles.none { it.id == id }) return
            val next = s.runtime.copy(activeId = id, bySchedule = false)
            persistRuntime(next)
            applyEffects(s.profiles, id)
        }
    }

    suspend fun deactivate() {
        mutex.withLock {
            val s = current()
            // Suppress so a still-open schedule window does not switch it straight back on.
            persistRuntime(FocusRuntime(null, false, s.runtime.activeId ?: s.runtime.suppressedId))
            applyEffects(s.profiles, null)
        }
    }

    suspend fun saveProfile(profile: FocusProfile) {
        mutex.withLock {
            val s = current()
            val sanitized =
                    profile.copy(
                            name = profile.name.trim().take(MAX_FOCUS_NAME).ifEmpty { profile.kind.id },
                            reminder = profile.reminder.take(MAX_FOCUS_REMINDER),
                            sessionMinutes =
                                    profile.sessionMinutes?.coerceIn(
                                            MIN_FOCUS_SESSION_MINUTES,
                                            MAX_FOCUS_SESSION_MINUTES,
                                    ),
                            visibleApps = profile.visibleApps.take(MAX_FOCUS_APPS).toSet(),
                    )
            val updated = s.profiles.map { if (it.kind == sanitized.kind) sanitized else it }
            store.edit { it[PROFILES_KEY] = FocusCodec.encode(updated) }
            if (s.runtime.activeId == sanitized.id) applyEffects(updated, sanitized.id)
        }
    }

    // ---- Session: thin wrapper over the existing Pomodoro ----

    suspend fun startSession() {
        val s = current()
        val minutes = s.active?.sessionMinutes
        if (minutes != null) {
            val cfg = preferencesManager.pomodoroConfigFlow.first()
            if (cfg.focusMinutes != minutes) {
                preferencesManager.setPomodoroConfig(cfg.copy(focusMinutes = minutes))
            }
        }
        preferencesManager.setShowHomePomodoro(true)
        pomodoro.start()
        pomodoro.selectMode(PomodoroMode.FOCUS)
        pomodoro.togglePlayPause()
    }

    /** Pauses when running, resumes when paused. */
    fun pauseOrResumeSession() = pomodoro.togglePlayPause()

    fun endSession() = pomodoro.selectMode(PomodoroMode.FOCUS)

    val sessionState get() = pomodoro.uiState

    // ---- Effects ----

    private suspend fun persistRuntime(r: FocusRuntime) {
        store.edit {
            if (r.activeId == null) it.remove(ACTIVE_KEY) else it[ACTIVE_KEY] = r.activeId
            it[BY_SCHEDULE_KEY] = r.bySchedule
            if (r.suppressedId == null) it.remove(SUPPRESSED_KEY) else it[SUPPRESSED_KEY] = r.suppressedId
        }
    }

    private suspend fun applyEffects(profiles: List<FocusProfile>, activeId: String?) {
        val profile = activeId?.let { id -> profiles.firstOrNull { it.id == id } }
        if (profile == null) {
            restoreSnapshot()
            FocusVisibility.publish(null)
            return
        }
        ensureSnapshot()
        val prefs = store.data.first()
        val baseLayout = prefs[SNAP_LAYOUT_KEY]?.let { HomeLayoutCodec.decodeOrDefault(it) }
        val layout =
                profile.layoutPreset?.let { HomeLayoutConfig.forPreset(it) } ?: baseLayout
        if (layout != null) layoutRepository.setLayout(layout)
        val snapTheme = prefs[SNAP_THEME_KEY]?.takeIf { it != NO_THEME }
        val theme = profile.themeId ?: snapTheme
        if (theme != null && themeStore.state.first().allThemes.any { it.id == theme }) {
            themeStore.select(theme)
        } else if (theme == null) {
            themeStore.reset()
        }
        FocusVisibility.publish(profile.visibleApps)
    }

    private suspend fun ensureSnapshot() {
        if (store.data.first()[SNAP_PRESENT_KEY] == true) return
        val layout = HomeLayoutCodec.encode(layoutRepository.layoutFlow.first())
        val theme = themeStore.state.first().selectedId ?: NO_THEME
        store.edit {
            it[SNAP_LAYOUT_KEY] = layout
            it[SNAP_THEME_KEY] = theme
            it[SNAP_PRESENT_KEY] = true
        }
    }

    private suspend fun restoreSnapshot() {
        val prefs = store.data.first()
        if (prefs[SNAP_PRESENT_KEY] != true) return
        prefs[SNAP_LAYOUT_KEY]?.let { layoutRepository.setLayout(HomeLayoutCodec.decodeOrDefault(it)) }
        val theme = prefs[SNAP_THEME_KEY]
        if (theme == null || theme == NO_THEME) themeStore.reset()
        else if (themeStore.state.first().allThemes.any { it.id == theme }) themeStore.select(theme)
        else themeStore.reset()
        store.edit {
            it.remove(SNAP_LAYOUT_KEY)
            it.remove(SNAP_THEME_KEY)
            it.remove(SNAP_PRESENT_KEY)
        }
    }

    companion object {
        private const val NO_THEME = "-"
        private val PROFILES_KEY = stringPreferencesKey("focus_profiles_v1")
        private val ACTIVE_KEY = stringPreferencesKey("focus_active_profile")
        private val BY_SCHEDULE_KEY = booleanPreferencesKey("focus_active_by_schedule")
        private val SUPPRESSED_KEY = stringPreferencesKey("focus_suppressed_profile")
        private val SNAP_LAYOUT_KEY = stringPreferencesKey("focus_restore_layout")
        private val SNAP_THEME_KEY = stringPreferencesKey("focus_restore_theme")
        private val SNAP_PRESENT_KEY = booleanPreferencesKey("focus_restore_present")
    }
}
