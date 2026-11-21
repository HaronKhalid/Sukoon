package com.lu4p.fokuslauncher.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the appearance screen shows: global default, pinned notes in order, selection. */
data class NoteAppearanceState(
        val global: NoteWidgetStyle = NoteWidgetStyle.DEFAULT,
        val pinned: List<NoteEntity> = emptyList(),
        val selectedId: Long = -1L,
) {
    val selected: NoteEntity? get() = pinned.firstOrNull { it.id == selectedId } ?: pinned.firstOrNull()

    /** The look being edited and previewed: the selected note own look, else the global default. */
    val editing: NoteWidgetStyle
        get() = selected?.let { NoteStyles.effective(it, global) } ?: global
}

@HiltViewModel
class NoteAppearanceViewModel
@Inject
constructor(
        private val repository: NoteRepository,
        private val preferences: NoteWidgetPreferences,
        customFonts: CustomFontStore,
        val fontCache: FontFamilyCache,
) : ViewModel() {
    private val selection = kotlinx.coroutines.flow.MutableStateFlow(-1L)
    val messages = MutableSharedFlow<Int>(extraBufferCapacity = 2)

    val state: StateFlow<NoteAppearanceState> =
            combine(preferences.styleFlow, repository.pinnedNotes, selection) { g, p, s ->
                        NoteAppearanceState(g, p, s)
                    }
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteAppearanceState())

    val imported: Pair<String, String>? =
            customFonts.readStoredFontDisplayLabel()?.let { label ->
                customFonts.activeFileName()?.let {
                    (LauncherFontPreferences.CUSTOM_FONT_PREFIX + it) to label
                }
            }

    fun select(id: Long) {
        selection.value = id
    }

    /** Edits the selected home note only; with no pinned note it edits the global default. */
    fun update(value: NoteWidgetStyle) {
        val v = value.coerced()
        val target = state.value.selected
        viewModelScope.launch {
            if (target != null) repository.saveLook(target.id, v) else preferences.setStyle(v)
        }
    }

    fun applyStyle(style: NoteStyle) {
        val target = state.value.selected
        viewModelScope.launch {
            if (target != null) repository.applyStyle(target.id, style)
            else {
                preferences.setStyle(NoteStyles.widgetStyleOf(style))
                preferences.setDefaultStyleId(style.id)
            }
        }
    }

    /** Makes the selected note look (or the gallery default) the look of future notes. */
    fun setAsDefault() {
        val sel = state.value.selected
        viewModelScope.launch {
            val look = state.value.editing
            preferences.setStyle(look)
            preferences.setDefaultStyleId(sel?.styleId.orEmpty())
            messages.emit(R.string.lock_default_set)
        }
    }

    fun addNote(title: String) {
        viewModelScope.launch {
            if (state.value.pinned.size >= PinModel.MAX_PINNED) {
                messages.emit(R.string.lock_max_pins)
                return@launch
            }
            val global = preferences.style()
            val style = NoteStyles.find(preferences.defaultStyleIdFlow.first())
            val id = repository.createStyledNote(title, "", style, global.appearance)
            if (style == null) repository.saveLook(id, global)
            selection.value = id
        }
    }

    fun move(id: Long, delta: Int) {
        viewModelScope.launch { repository.movePin(id, delta) }
    }

    fun unpin(id: Long) {
        viewModelScope.launch { repository.unpinOne(id) }
    }

    fun applyToAll(value: NoteWidgetStyle) {
        viewModelScope.launch { repository.applyAppearanceToAll(value.coerced().appearance) }
    }

    fun reset() {
        val target = state.value.selected
        viewModelScope.launch {
            if (target != null) repository.saveLook(target.id, NoteWidgetStyle.DEFAULT, styleId = "")
            else preferences.setStyle(NoteWidgetStyle.DEFAULT)
        }
    }
}

@Composable
fun NoteAppearanceScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: NoteAppearanceViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val context = LocalContext.current
    val ui = viewModel.state.collectAsStateWithLifecycle().value
    val style = ui.editing
    val pinned = ui.selected
    val a = style.appearance
    val openLock = LocalOpenLockNote.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show() }
    }
    val family =
            remember(a.fontFamily, viewModel.imported) {
                viewModel.fontCache.familyFor(a.fontFamily, FontFamily.Default)
            }
    val previewText =
            pinned?.let { listOf(it.title, it.body).filter { s -> s.isNotBlank() }.joinToString("\n") }
                    ?.takeIf { it.isNotBlank() }
                    ?: (stringResource(R.string.notes_sample_title) + "\n" +
                            stringResource(R.string.notes_sample_body))
    val decoration = NoteStyles.find(pinned?.styleId)?.decoration ?: NoteDecoration.NONE
    val newTitle = stringResource(R.string.lock_new_note_title)
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
            Label(stringResource(R.string.lock_pinned_notes))
            if (ui.pinned.isEmpty()) {
                Text(
                        stringResource(R.string.lock_no_pinned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                ChipRow(
                        ui.pinned.map { n ->
                            n.title.ifBlank { n.body.lineSequence().firstOrNull().orEmpty() }
                                    .ifBlank { stringResource(R.string.notes_untitled) }
                                    .take(20)
                        },
                        ui.pinned.indexOfFirst { it.id == pinned?.id },
                ) {
                    viewModel.select(ui.pinned[it].id)
                }
                pinned?.let { sel ->
                    Row {
                        TextButton(onClick = { viewModel.move(sel.id, -1) }) {
                            Text(stringResource(R.string.lock_move_up))
                        }
                        TextButton(onClick = { viewModel.move(sel.id, 1) }) {
                            Text(stringResource(R.string.lock_move_down))
                        }
                        TextButton(onClick = { viewModel.unpin(sel.id) }) {
                            Text(stringResource(R.string.lock_remove_from_home))
                        }
                    }
                }
            }
            Row {
                TextButton(
                        onClick = { viewModel.addNote(newTitle) },
                        modifier = Modifier.testTag("note_add_home"),
                ) {
                    Text(stringResource(R.string.lock_add_note))
                }
                TextButton(onClick = openLock, modifier = Modifier.testTag("note_open_lock")) {
                    Text(stringResource(R.string.lock_open_designer))
                }
            }

            Label(stringResource(R.string.notes_preview))
            NoteCardView(
                    text = previewText,
                    appearance = a,
                    style = style,
                    family = family,
                    modifier = Modifier.padding(vertical = 8.dp).testTag("note_preview"),
                    decoration = decoration,
                    stamp = null,
            )

            Label(stringResource(R.string.lock_gallery_title))
            Text(
                    stringResource(R.string.lock_gallery_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
            )
            StyleGallery(
                    selectedId = pinned?.styleId.orEmpty(),
                    fontCache = viewModel.fontCache,
                    onPick = viewModel::applyStyle,
            )
            TextButton(onClick = viewModel::setAsDefault) { Text(stringResource(R.string.lock_set_default)) }

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

/** Grid (3 per row) of live style previews. */
@Composable
private fun StyleGallery(selectedId: String, fontCache: FontFamilyCache, onPick: (NoteStyle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("style_gallery")) {
        NoteStyles.all.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { st ->
                    val look = remember(st.id) { NoteStyles.widgetStyleOf(st) }
                    val small =
                            look.copy(
                                    appearance =
                                            look.appearance.copy(
                                                    fontSizeSp = look.appearance.fontSizeSp.coerceAtMost(16f),
                                                    paddingDp = look.appearance.paddingDp.coerceAtMost(8),
                                                    maxLines = 3,
                                                    alignment = look.appearance.alignment,
                                            ),
                                    widthScale = NoteWidgetStyle.MAX_SCALE,
                                    heightScale = 1f,
                            )
                    val fam = remember(look.appearance.fontFamily) { fontCache.familyFor(look.appearance.fontFamily, FontFamily.Default) }
                    Column(
                            Modifier.weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                            if (st.id == selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else Color(0x22FFFFFF)
                                    )
                                    .clickable { onPick(st) }
                                    .padding(6.dp)
                                    .testTag("style_${st.id}")
                    ) {
                        Box(Modifier.fillMaxWidth().padding(2.dp)) {
                            NoteCardView(
                                    text = "Aa 123\nاردو نوٹ",
                                    appearance = small.appearance,
                                    style = small,
                                    family = fam,
                                    decoration = st.decoration,
                                                    stamp = "21 Nov",
                            )
                        }
                        Text(
                                stringResource(st.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
