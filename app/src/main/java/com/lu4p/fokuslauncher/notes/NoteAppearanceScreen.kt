package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import com.lu4p.fokuslauncher.typography.FontFamilyCache
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NoteAppearanceViewModel
@Inject
constructor(
        private val repository: NoteRepository,
        private val preferences: NoteWidgetPreferences,
        customFonts: CustomFontStore,
        val fontCache: FontFamilyCache,
) : ViewModel() {
    /** The pinned note's own look is what the home shows, so it is what we edit and preview. */
    val state: StateFlow<Pair<NoteWidgetStyle, NoteEntity?>> =
            combine(preferences.styleFlow, repository.pinnedNote) { style, pinned ->
                        style.copy(appearance = pinned?.appearance() ?: style.appearance) to pinned
                    }
                    .stateIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5_000),
                            NoteWidgetStyle.DEFAULT to null,
                    )

    val imported: Pair<String, String>? =
            customFonts.readStoredFontDisplayLabel()?.let { label ->
                customFonts.activeFileName()?.let {
                    (LauncherFontPreferences.CUSTOM_FONT_PREFIX + it) to label
                }
            }

    /** Saves the global default and, if a note is pinned, its look too, so the home updates live. */
    fun update(value: NoteWidgetStyle) {
        val v = value.coerced()
        viewModelScope.launch {
            preferences.setStyle(v)
            state.value.second?.let { repository.saveAppearance(it.id, v.appearance) }
        }
    }

    fun applyToAll(value: NoteWidgetStyle) {
        viewModelScope.launch { repository.applyAppearanceToAll(value.coerced().appearance) }
    }

    fun reset() = update(NoteWidgetStyle.DEFAULT)
}

@Composable
fun NoteAppearanceScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: NoteAppearanceViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val (style, pinned) = viewModel.state.collectAsStateWithLifecycle().value
    val a = style.appearance
    val family =
            remember(a.fontFamily, viewModel.imported) {
                viewModel.fontCache.familyFor(a.fontFamily, FontFamily.Default)
            }
    val previewText =
            pinned?.let { listOf(it.title, it.body).filter { s -> s.isNotBlank() }.joinToString("\n") }
                    ?.takeIf { it.isNotBlank() }
                    ?: (stringResource(R.string.notes_sample_title) + "\n" +
                            stringResource(R.string.notes_sample_body))
    Column(
            modifier.fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("note_appearance_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.notes_appearance_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Label(stringResource(R.string.notes_preview))
            NoteCardView(
                    text = previewText,
                    appearance = a,
                    style = style,
                    family = family,
                    modifier = Modifier.padding(vertical = 8.dp).testTag("note_preview"),
            )
            Label(stringResource(R.string.notes_card_style))
            val cardLabels =
                    listOf(
                            stringResource(R.string.notes_card_plain),
                            stringResource(R.string.notes_card_filled),
                            stringResource(R.string.notes_card_outlined),
                            stringResource(R.string.notes_card_sticky),
                    )
            ChipRow(cardLabels, style.cardStyle.ordinal) {
                viewModel.update(style.copy(cardStyle = NoteCardStyle.entries[it]))
            }
            AppearanceControls(a, viewModel.imported) { viewModel.update(style.copy(appearance = it)) }
            Label(stringResource(R.string.notes_bg_opacity, (style.backgroundOpacity * 100).toInt()))
            Slider(
                    value = style.backgroundOpacity,
                    onValueChange = { viewModel.update(style.copy(backgroundOpacity = it)) },
            )
            Label(stringResource(R.string.notes_corner_radius, style.cornerRadiusDp))
            Slider(
                    value = style.cornerRadiusDp.toFloat(),
                    onValueChange = { viewModel.update(style.copy(cornerRadiusDp = it.toInt())) },
                    valueRange = 0f..NoteWidgetStyle.MAX_CORNER_DP.toFloat(),
            )
            Label(stringResource(R.string.notes_border))
            ChipRow(
                    listOf(
                            stringResource(R.string.notes_on_off_off),
                            stringResource(R.string.notes_on_off_on),
                    ),
                    if (style.borderEnabled) 1 else 0,
            ) {
                viewModel.update(style.copy(borderEnabled = it == 1))
            }
            if (style.borderEnabled || style.cardStyle == NoteCardStyle.OUTLINED) {
                Label(stringResource(R.string.notes_border_color))
                SwatchRow(TextSwatches, style.borderColor) {
                    viewModel.update(style.copy(borderColor = it))
                }
            }
            Label(stringResource(R.string.notes_width_scale, (style.widthFraction * 100).toInt()))
            Slider(
                    value = style.widthScale,
                    onValueChange = { viewModel.update(style.copy(widthScale = it)) },
                    valueRange = NoteWidgetStyle.MIN_SCALE..NoteWidgetStyle.MAX_SCALE,
            )
            Label(stringResource(R.string.notes_height_scale, (style.heightScale * 100).toInt()))
            Slider(
                    value = style.heightScale,
                    onValueChange = { viewModel.update(style.copy(heightScale = it)) },
                    valueRange = NoteWidgetStyle.MIN_SCALE..NoteWidgetStyle.MAX_SCALE,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { viewModel.applyToAll(style) }) {
                    Text(stringResource(R.string.notes_apply_all))
                }
                TextButton(onClick = viewModel::reset) {
                    Text(stringResource(R.string.notes_reset_style))
                }
            }
        }
    }
}
