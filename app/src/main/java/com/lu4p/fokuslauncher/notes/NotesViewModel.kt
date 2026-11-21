package com.lu4p.fokuslauncher.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.typography.FontFamilyCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Holds the editor draft in [SavedStateHandle] so typing survives rotation and process death, and
 * autosaves to Room shortly after each change. A draft restored after process death is written
 * back on start, so the last keystrokes are not lost.
 */
@HiltViewModel
class NotesViewModel
@Inject
constructor(
        private val repository: NoteRepository,
        val fontCache: FontFamilyCache,
        private val widgetPreferences: NoteWidgetPreferences,
        private val savedState: SavedStateHandle,
        private val lockNotes: com.lu4p.fokuslauncher.lockscreen.LockNoteController,
) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> =
            repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Id of the note open in the editor, or [NO_NOTE]. Survives process death. */
    var editingId: Long by mutableStateOf(savedState[KEY_ID] ?: NO_NOTE)
        private set

    var draftTitle: String by mutableStateOf(savedState[KEY_TITLE] ?: "")
        private set

    var draftBody: String by mutableStateOf(savedState[KEY_BODY] ?: "")
        private set

    var appearance: NoteAppearance by mutableStateOf(NoteAppearance.DEFAULT)
        private set

    var pinned: Boolean by mutableStateOf(false)
        private set

    private var autosave: Job? = null

    init {
        if (editingId != NO_NOTE) {
            viewModelScope.launch {
                // Restored draft wins over the stored text: it may hold unsaved keystrokes.
                repository.saveText(editingId, draftTitle, draftBody)
                refreshMeta(editingId)
            }
        }
    }

    private suspend fun refreshMeta(id: Long) {
        val note = repository.get(id) ?: return
        appearance = note.appearance()
        pinned = note.pinned
    }

    fun newNote() {
        viewModelScope.launch {
            val id = repository.create(widgetPreferences.style().appearance)
            open(id)
        }
    }

    fun openNote(id: Long) {
        viewModelScope.launch { open(id) }
    }

    private suspend fun open(id: Long) {
        val note = repository.get(id) ?: return
        draftTitle = note.title
        draftBody = note.body
        appearance = note.appearance()
        pinned = note.pinned
        editingId = id
        savedState[KEY_ID] = id
        savedState[KEY_TITLE] = note.title
        savedState[KEY_BODY] = note.body
    }

    fun onTitleChange(value: String) {
        draftTitle = value
        savedState[KEY_TITLE] = value
        scheduleAutosave()
    }

    fun onBodyChange(value: String) {
        draftBody = value
        savedState[KEY_BODY] = value
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        val id = editingId
        if (id == NO_NOTE) return
        autosave?.cancel()
        autosave =
                viewModelScope.launch {
                    delay(AUTOSAVE_DELAY_MS)
                    repository.saveText(id, draftTitle, draftBody)
                }
    }

    fun onAppearanceChange(value: NoteAppearance) {
        val id = editingId
        if (id == NO_NOTE) return
        appearance = value.coerced()
        viewModelScope.launch { repository.saveAppearance(id, appearance) }
    }

    fun togglePin() {
        val id = editingId
        if (id == NO_NOTE) return
        val target = !pinned
        pinned = target
        viewModelScope.launch {
            if (target) { if (!repository.pinAdd(id)) pinned = false } else repository.unpinOne(id)
        }
    }

    /** Saves now; called when leaving the editor or when the screen stops. */
    fun flush() {
        val id = editingId
        if (id == NO_NOTE) return
        autosave?.cancel()
        viewModelScope.launch { withContext(NonCancellable) { repository.saveText(id, draftTitle, draftBody) }; lockNotes.onNoteSaved(id) }
    }

    fun closeEditor() {
        val id = editingId
        if (id == NO_NOTE) return
        autosave?.cancel()
        editingId = NO_NOTE
        savedState[KEY_ID] = NO_NOTE
        savedState[KEY_TITLE] = ""
        savedState[KEY_BODY] = ""
        val title = draftTitle
        val body = draftBody
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.saveText(id, title, body)
                repository.deleteIfBlank(id)
                lockNotes.onNoteSaved(id)
            }
        }
    }

    fun deleteCurrent() {
        val id = editingId
        if (id == NO_NOTE) return
        autosave?.cancel()
        editingId = NO_NOTE
        savedState[KEY_ID] = NO_NOTE
        savedState[KEY_TITLE] = ""
        savedState[KEY_BODY] = ""
        viewModelScope.launch { withContext(NonCancellable) { repository.delete(id) } }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        const val NO_NOTE = -1L
        const val AUTOSAVE_DELAY_MS = 400L
        private const val KEY_ID = "notes_editing_id"
        private const val KEY_TITLE = "notes_draft_title"
        private const val KEY_BODY = "notes_draft_body"
    }
}
