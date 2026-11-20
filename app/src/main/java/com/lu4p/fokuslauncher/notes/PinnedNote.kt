package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.typography.FontFamilyCache
import com.lu4p.fokuslauncher.typography.LocalElementFonts
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Lets the nav graph supply navigation to the notes screen without threading it through Home. */
val LocalOpenNotes = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

@HiltViewModel
class PinnedNoteViewModel
@Inject
constructor(repository: NoteRepository, val fontCache: FontFamilyCache) : ViewModel() {
    val pinned: StateFlow<NoteEntity?> =
            repository.pinnedNote.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Home-screen slot for the pinned in-app note. When no note is pinned this renders [fallback]
 * (the existing markdown home note), so the SAF note feature keeps working unchanged. Long
 * press opens the notes screen via [onOpen]; text is clamped to the note's maximum lines.
 */
@Composable
fun HomePinnedNoteOr(
        modifier: Modifier = Modifier,
        viewModel: PinnedNoteViewModel = hiltViewModel(),
        fallback: @Composable () -> Unit,
) {
    val onOpen = LocalOpenNotes.current
    val note by viewModel.pinned.collectAsStateWithLifecycle()
    val pinned = note
    if (pinned == null) {
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
    val style =
            noteTextStyle(
                    MaterialTheme.typography.bodyLarge,
                    appearance,
                    family,
                    MaterialTheme.colorScheme.onBackground,
            )
    val text =
            remember(pinned.title, pinned.body) {
                listOf(pinned.title, pinned.body).filter { it.isNotBlank() }.joinToString("\n")
            }
    Text(
            text = text,
            style = style,
            maxLines = appearance.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier =
                    modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                    if (appearance.backgroundColor == 0) Color.Transparent
                                    else Color(appearance.backgroundColor)
                            )
                            .combinedClickableWithSystemSound(
                                    onClick = onOpen,
                                    onLongClick = onOpen,
                            )
                            .padding(appearance.paddingDp.dp)
                            .testTag("pinned_note"),
    )
}
