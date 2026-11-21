package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.typography.FontFamilyCache
import com.lu4p.fokuslauncher.typography.LocalElementFonts
import com.lu4p.fokuslauncher.typography.TextElement
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lets the nav graph supply navigation to the notes screen without threading it through Home. */
val LocalOpenNotes = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/** Opens the home-note appearance screen. */
val LocalOpenNoteAppearance = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

/** Opens the lock-screen note designer. */
val LocalOpenLockNote = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

@HiltViewModel
class PinnedNoteViewModel
@Inject
constructor(
        private val repository: NoteRepository,
        private val widgetPreferences: NoteWidgetPreferences,
        val fontCache: FontFamilyCache,
) : ViewModel() {
    val pinnedList: StateFlow<List<NoteEntity>> =
            repository.pinnedNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinned: StateFlow<NoteEntity?> =
            repository.pinnedNote.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allNotes: StateFlow<List<NoteEntity>> =
            repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val style: StateFlow<NoteWidgetStyle> =
            widgetPreferences.styleFlow.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    NoteWidgetStyle.DEFAULT,
            )

    fun pin(id: Long) {
        viewModelScope.launch { repository.pin(id) }
    }

    fun replacePinned(old: Long, new: Long) {
        viewModelScope.launch { repository.replacePin(old, new) }
    }

    fun unpin(id: Long) {
        viewModelScope.launch { repository.unpinOne(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /**
     * Seeds one editable, pinned sample note the first time the home note slot is empty and the
     * user has no notes. Runs at most once; deleting it later does not bring it back.
     */
    fun seedSampleOnce(title: String, body: String) {
        viewModelScope.launch {
            if (widgetPreferences.sampleSeeded()) return@launch
            widgetPreferences.markSampleSeeded()
            if (repository.count() == 0) {
                val look = widgetPreferences.style().appearance
                repository.createPinned(title, body, look)
            }
        }
    }
}

/**
 * Home-screen slot for pinned in-app notes (up to [PinModel.MAX_PINNED], each with its own style,
 * order and size). When no note is pinned this renders [fallback] (the existing markdown home
 * note), so the SAF note feature keeps working unchanged. Tap or long-press on a note opens a
 * small menu. When the slot is empty ([fallbackIsEmpty]) a sample note is created once.
 */
@Composable
fun HomePinnedNoteOr(
        modifier: Modifier = Modifier,
        fallbackIsEmpty: Boolean = false,
        viewModel: PinnedNoteViewModel = hiltViewModel(),
        fallback: @Composable () -> Unit,
) {
    val notes by viewModel.pinnedList.collectAsStateWithLifecycle()
    val global by viewModel.style.collectAsStateWithLifecycle()
    if (notes.isEmpty()) {
        val sampleTitle = stringResource(R.string.notes_sample_title)
        val sampleBody = stringResource(R.string.notes_sample_body)
        LaunchedEffect(fallbackIsEmpty) {
            if (fallbackIsEmpty) viewModel.seedSampleOnce(sampleTitle, sampleBody)
        }
        fallback()
        return
    }
    Column(modifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        notes.forEachIndexed { index, pinned ->
            PinnedNoteCard(pinned, global, viewModel, tag = if (index == 0) "pinned_note" else "pinned_note_$index")
        }
    }
}

@Composable
private fun PinnedNoteCard(
        pinned: NoteEntity,
        global: NoteWidgetStyle,
        viewModel: PinnedNoteViewModel,
        tag: String,
) {
    val onOpen = LocalOpenNotes.current
    val onAppearance = LocalOpenNoteAppearance.current
    val onLock = LocalOpenLockNote.current
    val style = remember(pinned, global) { NoteStyles.effective(pinned, global) }
    val appearance = style.appearance
    val launcherFamily =
            LocalElementFonts.current[TextElement.NOTES]
                    ?: MaterialTheme.typography.bodyLarge.fontFamily
                    ?: FontFamily.Default
    val family =
            remember(appearance.fontFamily, launcherFamily) {
                viewModel.fontCache.familyFor(appearance.fontFamily, launcherFamily)
            }
    val decoration = remember(pinned.styleId) { NoteStyles.find(pinned.styleId)?.decoration ?: NoteDecoration.NONE }
    val text =
            remember(pinned.title, pinned.body) {
                listOf(pinned.title, pinned.body).filter { it.isNotBlank() }.joinToString("\n")
            }
    val stamp =
            remember(pinned.updatedAt) {
                if (pinned.updatedAt <= 0) null
                else java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                        .format(java.util.Date(pinned.updatedAt))
            }
    var menuOpen by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    Box {
        NoteCardView(
                text = text,
                appearance = appearance,
                style = style,
                family = family,
                onClick = { menuOpen = true },
                onLongClick = { menuOpen = true },
                modifier = Modifier.testTag(tag),
                decoration = decoration,
                stamp = stamp,
        )
        DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {
                    menuOpen = false
                    switching = false
                },
        ) {
            if (!switching) {
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_home_edit)) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_home_switch)) },
                        onClick = { switching = true },
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_home_appearance)) },
                        onClick = {
                            menuOpen = false
                            onAppearance()
                        },
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.lock_home_menu)) },
                        onClick = {
                            menuOpen = false
                            onLock()
                        },
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_home_unpin)) },
                        onClick = {
                            menuOpen = false
                            viewModel.unpin(pinned.id)
                        },
                )
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.notes_delete)) },
                        onClick = {
                            menuOpen = false
                            viewModel.delete(pinned.id)
                        },
                )
            } else {
                val others by viewModel.allNotes.collectAsStateWithLifecycle()
                others.filter { !it.pinned }.forEach { n ->
                    DropdownMenuItem(
                            text = {
                                Text(
                                        n.title.ifBlank { n.body.lineSequence().firstOrNull().orEmpty() }
                                                .ifBlank { stringResource(R.string.notes_untitled) }
                                                .take(40)
                                )
                            },
                            onClick = {
                                menuOpen = false
                                switching = false
                                viewModel.replacePinned(pinned.id, n.id)
                            },
                    )
                }
            }
        }
    }
}
