package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.HomeLayoutConfig
import com.lu4p.fokuslauncher.home.layout.HomeLayoutRepository
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.ClockFaces
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.ThemeState
import com.lu4p.fokuslauncher.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StyleGalleryViewModel
@Inject
constructor(
        private val store: ThemeStore,
        private val layoutRepository: HomeLayoutRepository,
) : ViewModel() {

    val state: StateFlow<ThemeState> =
            store.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeState(null, emptyList()))

    /** null until DataStore has answered, so the first-run sheet never flashes. */
    val stylePromptSeen: StateFlow<Boolean?> =
            store.stylePromptSeen
                    .map<Boolean, Boolean?> { it }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Applies the theme plus its style bundle (layout preset and clock face) in one go. */
    fun apply(theme: ThemeDefinition, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            store.select(theme.id)
            val preset =
                    LayoutPreset.entries.firstOrNull { it.name == theme.style.layoutPreset } ?: LayoutPreset.DEFAULT
            layoutRepository.setLayout(HomeLayoutConfig.forPreset(preset))
            store.setClockFaceId(theme.style.resolvedClockFaceId)
            // The home clock reads its face from ClockSettings, so set it there too.
            val clock = layoutRepository.clockFlow.first()
            layoutRepository.setClock(clock.copy(faceId = theme.style.resolvedClockFaceId, accentArgb = 0))
            onDone()
        }
    }

    /** Keeps the legacy Fokus look and its original layout. */
    fun applyClassic(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            store.reset()
            layoutRepository.setLayout(HomeLayoutConfig.forPreset(LayoutPreset.DEFAULT))
            store.setClockFaceId(ClockFaces.DEFAULT)
            layoutRepository.setClock(ClockSettings()) // back to the original Fokus clock
            onDone()
        }
    }

    fun markPromptSeen() {
        viewModelScope.launch { store.markStylePromptSeen() }
    }
}
