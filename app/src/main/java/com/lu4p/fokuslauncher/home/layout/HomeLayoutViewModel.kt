package com.lu4p.fokuslauncher.home.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the home screen reads. The default value reproduces the pre-layout-system home screen. */
@Immutable
data class HomeLayoutState(
        val layout: HomeLayoutConfig = HomeLayoutConfig.Default,
        val clock: ClockSettings = ClockSettings(),
)

val LocalHomeLayout = compositionLocalOf { HomeLayoutState() }

@HiltViewModel
class HomeLayoutViewModel @Inject constructor(private val repository: HomeLayoutRepository) : ViewModel() {
    val state: StateFlow<HomeLayoutState> =
            combine(repository.layoutFlow, repository.clockFlow, ::HomeLayoutState)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeLayoutState())

    fun applyPreset(preset: LayoutPreset) = edit { repository.setLayout(HomeLayoutConfig.forPreset(preset)) }

    fun updateLayout(change: (HomeLayoutConfig) -> HomeLayoutConfig) =
            edit { repository.setLayout(change(state.value.layout)) }

    fun updateClock(change: (ClockSettings) -> ClockSettings) =
            edit { repository.setClock(change(state.value.clock).validated()) }

    private fun edit(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
