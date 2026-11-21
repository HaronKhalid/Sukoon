package com.lu4p.fokuslauncher.ui.editor

import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.home.layout.HomeLayoutRepository
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.ThemeStore
import com.lu4p.fokuslauncher.typography.FontFamilyCache
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.typography.TypographyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class EditorTab { STYLE, CLOCK, FONT, COLOUR, MORE }

/** What the editor screen renders. [snapshot] is null only until the stored settings have loaded. */
data class HomeEditorUi(
        val snapshot: EditorSnapshot? = null,
        val canUndo: Boolean = false,
        val isModified: Boolean = false,
        val tab: EditorTab = EditorTab.STYLE,
        val customThemes: List<ThemeDefinition> = emptyList(),
        /** One-shot message as a string resource id; the screen clears it with [HomeEditorViewModel.messageShown]. */
        val message: Int? = null,
)

/**
 * State holder of the Home Editor. All edits go through [EditorSession] (undo/reset), update the
 * UI state immediately (so the preview never waits on disk) and are written to DataStore after a
 * short debounce, so dragging a slider causes one write, not hundreds.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class HomeEditorViewModel
@Inject
constructor(
        private val themeStore: ThemeStore,
        private val layoutRepository: HomeLayoutRepository,
        private val preferences: PreferencesManager,
        private val typography: TypographyPreferences,
        private val fontCache: FontFamilyCache,
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeEditorUi())
    val ui: StateFlow<HomeEditorUi> = _ui.asStateFlow()

    private var session: EditorSession? = null

    /** Outlives the ViewModel scope on purpose so the last edit is flushed after the screen closes. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistMutex = Mutex()
    private var persistJob: Job? = null
    private var lastPersisted: EditorSnapshot? = null
    private var sessionCustomId: String? = null

    init {
        viewModelScope.launch {
            val state = themeStore.state.first()
            val applied = state.applied
            val layout = layoutRepository.layoutFlow.first()
            val clock = layoutRepository.clockFlow.first()
            val appFont = preferences.launcherFontFamilyFlow.first()
            val elementFonts = typography.allFontsFlow.first().filterValues { it.isNotEmpty() }
            val snap =
                    EditorSnapshot(
                            theme = applied ?: BuiltInThemes.ClassicPreview,
                            layout = layout,
                            clock = clock,
                            appFont = appFont,
                            elementFonts = elementFonts,
                    )
            session = EditorSession(snap)
            lastPersisted = snap
            _ui.value = HomeEditorUi(snapshot = snap, customThemes = state.customThemes)
            startPersistence()
        }
        viewModelScope.launch {
            themeStore.state.map { it.customThemes }.distinctUntilChanged().collect { list ->
                _ui.value = _ui.value.copy(customThemes = list)
            }
        }
    }

    private fun startPersistence() {
        persistJob =
                ioScope.launch {
                    _ui.map { it.snapshot }.distinctUntilChanged().drop(1).debounce(PERSIST_DEBOUNCE_MS).collect { s ->
                        if (s != null) persist(s)
                    }
                }
    }

    // ---- edits -----------------------------------------------------------------------------

    fun selectTab(tab: EditorTab) {
        _ui.value = _ui.value.copy(tab = tab)
    }

    fun selectTheme(theme: ThemeDefinition) = edit { EditorOps.selectTheme(it, theme) }

    fun setBackdrop(id: String) = edit { EditorOps.setBackdrop(it, id) }

    fun setBackdropIntensity(v: Float) = edit("backdrop-intensity") { EditorOps.setBackdropIntensity(it, v) }

    fun setAccent(argb: Int) = edit { EditorOps.setAccent(it, argb) }

    fun setTextColor(argb: Int) {
        val s = session ?: return
        val next = EditorOps.setTextColor(s.current, argb)
        if (next == null) toast(R.string.editor_colour_too_dim) else edit { next }
    }

    fun setTint(argb: Int, pct: Int) {
        val s = session ?: return
        val next = EditorOps.setTint(s.current, argb, pct)
        if (next == null) toast(R.string.editor_colour_too_dim) else edit("tint") { next }
    }

    fun setLayoutPreset(preset: LayoutPreset) = edit { EditorOps.setLayoutPreset(it, preset) }

    fun setClockFace(id: String) = edit { EditorOps.setClockFace(it, id) }

    fun setClockScale(pct: Int) = edit("clock-scale") { EditorOps.setClockScale(it, pct) }

    fun setAppFont(value: String) = edit { EditorOps.setAppFont(it, value) }

    fun setElementFont(element: TextElement, value: String) = edit { EditorOps.setElementFont(it, element, value) }

    /** Call when a slider drag ends so the next drag becomes its own undo step. */
    fun endGesture() {
        session?.endGesture()
    }

    fun undo() {
        val s = session ?: return
        if (s.undo()) publish()
    }

    fun reset() {
        val s = session ?: return
        if (s.reset()) publish()
    }

    fun messageShown() {
        _ui.value = _ui.value.copy(message = null)
    }

    fun fontFamily(storageValue: String): FontFamily = fontCache.familyFor(storageValue, FontFamily.Default)

    private fun edit(coalesceKey: String? = null, change: (EditorSnapshot) -> EditorSnapshot) {
        val s = session ?: return
        if (s.edit(coalesceKey, change)) publish()
    }

    private fun publish() {
        val s = session ?: return
        _ui.value = _ui.value.copy(snapshot = s.current, canUndo = s.canUndo, isModified = s.isModified)
    }

    private fun toast(res: Int) {
        _ui.value = _ui.value.copy(message = res)
    }

    // ---- persistence -----------------------------------------------------------------------

    private suspend fun persist(s: EditorSnapshot) =
            persistMutex.withLock {
                val prev = lastPersisted
                val theme = s.effectiveTheme
                val isClassic = theme.id == BuiltInThemes.ID_CLASSIC
                val builtIn = BuiltInThemes.byId(theme.id)
                val unchangedBuiltIn = isClassic && theme == BuiltInThemes.ClassicPreview || builtIn != null && builtIn == theme
                when {
                    unchangedBuiltIn && isClassic -> themeStore.reset()
                    unchangedBuiltIn -> themeStore.select(theme.id)
                    else -> {
                        val fromBuiltIn = isClassic || builtIn != null
                        val id =
                                if (fromBuiltIn) sessionCustomId
                                        ?: ThemeStore.newCustomId(themeStore.state.first().customThemes.map { it.id }.toSet())
                                else theme.id
                        val toSave = if (fromBuiltIn) theme.copy(id = id, name = "My ${theme.name}".take(ThemeDefinition.MAX_NAME_LENGTH)) else theme
                        if (themeStore.saveCustom(toSave)) {
                            if (fromBuiltIn) sessionCustomId = id
                            themeStore.select(id)
                        } else {
                            toast(R.string.editor_limit_reached)
                            return@withLock
                        }
                    }
                }
                if (prev == null || prev.layout != s.layout) layoutRepository.setLayout(s.layout)
                if (prev == null || prev.clock != s.clock) layoutRepository.setClock(s.clock)
                themeStore.setClockFaceId(s.clockFaceId)
                if (prev == null || prev.appFont != s.appFont) preferences.setLauncherFontFamilyName(s.appFont)
                for (e in TextElement.entries) {
                    val v = s.elementFonts[e].orEmpty()
                    if (prev == null || prev.elementFonts[e].orEmpty() != v) typography.setFont(e, v)
                }
                lastPersisted = s
            }

    override fun onCleared() {
        persistJob?.cancel()
        val latest = _ui.value.snapshot
        if (latest != null && latest != lastPersisted) ioScope.launch { persist(latest) }
        super.onCleared()
    }

    companion object {
        const val PERSIST_DEBOUNCE_MS = 250L
    }
}
