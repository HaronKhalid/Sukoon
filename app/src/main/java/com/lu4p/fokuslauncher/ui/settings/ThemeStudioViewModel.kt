package com.lu4p.fokuslauncher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeCodec
import com.lu4p.fokuslauncher.theme.ThemeColorField
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.ThemeImportResult
import com.lu4p.fokuslauncher.theme.ThemeState
import com.lu4p.fokuslauncher.theme.ThemeStore
import com.lu4p.fokuslauncher.theme.isReadable
import com.lu4p.fokuslauncher.theme.saveIdFor
import com.lu4p.fokuslauncher.theme.duplicated
import com.lu4p.fokuslauncher.theme.withColor
import com.lu4p.fokuslauncher.theme.withGradientAngle
import com.lu4p.fokuslauncher.theme.withGradientEnabled
import com.lu4p.fokuslauncher.theme.withGradientStop
import com.lu4p.fokuslauncher.theme.withThirdStop
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeStudioViewModel @Inject constructor(private val store: ThemeStore) : ViewModel() {

    val state: StateFlow<ThemeState> =
            store.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeState(null, emptyList()))

    /** Theme being previewed/edited. Not applied until [apply]. */
    private val _draft = MutableStateFlow(BuiltInThemes.default)
    val draft: StateFlow<ThemeDefinition> = _draft

    private var initialised = false

    fun initFrom(state: ThemeState) {
        if (initialised) return
        initialised = true
        _draft.value = state.applied ?: BuiltInThemes.default
    }

    /** The stored theme the draft started from, used to detect unsaved edits. */
    fun sourceOf(state: ThemeState, draft: ThemeDefinition): ThemeDefinition? =
            state.allThemes.firstOrNull { it.id == draft.id }

    fun preview(theme: ThemeDefinition) {
        _draft.value = theme
    }

    fun discardEdits(state: ThemeState) {
        sourceOf(state, _draft.value)?.let { _draft.value = it }
    }

    fun setColor(field: ThemeColorField, argb: Int) {
        _draft.update { it.withColor(field, argb) }
    }

    fun setGradientEnabled(on: Boolean) { _draft.update { it.withGradientEnabled(on) } }

    fun setGradientStop(index: Int, argb: Int) { _draft.update { it.withGradientStop(index, argb) } }

    fun setGradientAngle(angle: Int) { _draft.update { it.withGradientAngle(angle) } }

    fun setThirdStop(on: Boolean) { _draft.update { it.withThirdStop(on) } }

    /** Saves a copy of the draft (built-in or custom) as a brand-new custom theme. */
    fun duplicate(state: ThemeState, copyName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val id = ThemeStore.newCustomId(state.customThemes.map { it.id }.toSet())
            val copy = _draft.value.duplicated(id, copyName.trim().ifEmpty { _draft.value.name })
            if (store.saveCustom(copy)) {
                _draft.value = copy
                onDone(true)
            } else onDone(false)
        }
    }

    fun apply(state: ThemeState, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val d = _draft.value
            if (!d.isReadable()) return@launch onDone(false)
            // Applying unsaved edits to a built-in or custom theme stores them as a custom theme.
            val stored = sourceOf(state, d)
            if (stored == d) {
                store.select(d.id)
                onDone(true)
            } else {
                val saved = save(state, d.name)
                if (saved != null) {
                    store.select(saved.id)
                    onDone(true)
                } else onDone(false)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            store.reset()
            _draft.value = BuiltInThemes.default
        }
    }

    /** Saves the draft as a custom theme (new id unless already a custom id). */
    fun saveAs(state: ThemeState, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(save(state, name) != null) }
    }

    private suspend fun save(state: ThemeState, name: String): ThemeDefinition? {
        val d = _draft.value
        val id = saveIdFor(d, state.customThemes.map { it.id }.toSet())
        val trimmed = name.trim().ifEmpty { d.name }.take(ThemeDefinition.MAX_NAME_LENGTH)
        val toSave = d.copy(id = id, name = trimmed)
        return if (store.saveCustom(toSave)) toSave.also { _draft.value = it } else null
    }

    fun rename(id: String, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = store.rename(id, name)
            if (ok && _draft.value.id == id) _draft.update { it.copy(name = name.trim()) }
            onDone(ok)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            store.delete(id)
            if (_draft.value.id == id) _draft.value = BuiltInThemes.default
        }
    }

    fun exportJson(): String = ThemeCodec.encode(_draft.value)

    fun import(json: String, onDone: (ThemeImportResult) -> Unit) {
        viewModelScope.launch {
            val r = store.importTheme(json)
            if (r is ThemeImportResult.Imported) _draft.value = r.theme
            onDone(r)
        }
    }
}
