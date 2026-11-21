package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.layout.Box
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

@HiltViewModel
class PinnedNoteViewModel
@Inject
constructor(
        private val repository: NoteRepository,
        private val widgetPreferences: NoteWidgetPreferences,
        val fontCache: FontFamilyCache,
) : ViewModel() {
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

    fun unpin(id: Long) {
        viewModelScope.launch { repository.unpin(id) }
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
 * Home-screen slot for the pinned in-app note. When no note is pinned this renders [fallback]
 * (the existing markdown home note), so the SAF note feature keeps working unchanged. Tap or
 * long-press opens a small menu: edit, switch the pinned note, appearance, remove from home.
 * When the slot is empty ([fallbackIsEmpty]) a tasteful sample note is created once.
 */
@Composable
fun HomePinnedNoteOr(
        modifier: Modifier = Modifier,
        fallbackIsEmpty: Boolean = false,
        viewModel: PinnedNoteViewModel = hiltViewModel(),
        fallback: @Composable () -> Unit,
) {
    val onOpen = LocalOpenNotes.current
    val onAppearance = LocalOpenNoteAppearance.current
    val note by viewModel.pinned.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val pinned = note
    if (pinned == null) {
        val sampleTitle = stringResource(R.string.notes_sample_title)
        val sampleBody = stringResource(R.string.notes_sample_body)
        LaunchedEffect(fallbackIsEmpty) {
            if (fallbackIsEmpty) viewModel.seedSampleOnce(sampleTitle, sampleBody)
        }
        fallback()
        return
    }
    val appearance = remember(pinned) { pinned.appearance() }
    val launcherFamily =
            LocalElementFonts.current[TextElement.NOTES]
                    ?: MaterialTheme.typography.bodyLarge.fontFamily
                    ?: FontFamily.Default
    val family =
            remember(appearance.fontFamily, launcherFamily) {
                viewModel.fontCache.familyFor(appearance.fontFamily, launcherFamily)
            }
    val text =
            remember(pinned.title, pinned.body) {
                listOf(pinned.title, pinned.body).filter { it.isNotBlank() }.joinToString("\n")
            }
    var menuOpen by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    Box(modifier) {
        NoteCardView(
                text = text,
                appearance = appearance,
                style = style,
                family = family,
                onClick = { menuOpen = true },
                onLongClick = { menuOpen = true },
                modifier = Modifier.testTag("pinned_note"),
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
                others.forEach { n ->
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
                                viewModel.pin(n.id)
                            },
                    )
                }
            }
        }
    }
}
