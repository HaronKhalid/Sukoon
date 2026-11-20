package com.lu4p.fokuslauncher.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.typography.FontRegistry
import com.lu4p.fokuslauncher.typography.LocalElementFonts
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop

/** Notes list and editor in one destination; the open note id survives process death. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: NotesViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val editing = viewModel.editingId != NotesViewModel.NO_NOTE
    BackHandler(enabled = editing) { viewModel.closeEditor() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .imePadding()
                            .testTag("notes_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.notes_settings_title),
                onNavigateBack = { if (editing) viewModel.closeEditor() else onNavigateBack() },
                containerColor = MaterialTheme.colorScheme.background,
                actions = {
                    if (editing) {
                        TextButton(onClick = viewModel::togglePin) {
                            Text(
                                    stringResource(
                                            if (viewModel.pinned) R.string.notes_unpin
                                            else R.string.notes_pin
                                    )
                            )
                        }
                    }
                },
        )
        if (editing) NoteEditor(viewModel) else NoteList(viewModel)
    }
}

@Composable
private fun NoteList(viewModel: NotesViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize()) {
        Text(
                stringResource(R.string.notes_privacy_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        TextButton(
                onClick = viewModel::newNote,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("notes_new"),
        ) {
            Text(stringResource(R.string.notes_new))
        }
        if (notes.isEmpty()) {
            Text(
                    stringResource(R.string.notes_empty),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(notes, key = { it.id }) { note ->
                Column(
                        Modifier.fillMaxWidth()
                                .clickable { viewModel.openNote(note.id) }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                note.title.ifBlank { stringResource(R.string.notes_untitled) },
                                style =
                                        MaterialTheme.typography.bodyLarge.copy(
                                                textDirection = TextDirection.Content
                                        ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { pendingDelete = note.id }) {
                            Text(stringResource(R.string.notes_delete))
                        }
                    }
                    if (note.body.isNotBlank()) {
                        Text(
                                note.body,
                                style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                                textDirection = TextDirection.Content
                                        ),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (note.pinned) {
                        Text(
                                stringResource(R.string.notes_pinned_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    pendingDelete?.let { id ->
        AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.notes_delete_confirm_title)) },
                text = { Text(stringResource(R.string.notes_delete_confirm_body)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.delete(id)
                                pendingDelete = null
                            }
                    ) {
                        Text(stringResource(R.string.notes_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.notes_cancel))
                    }
                },
        )
    }
}

@Composable
private fun NoteEditor(viewModel: NotesViewModel) {
    val launcherFamily =
            LocalElementFonts.current[TextElement.NOTES]
                    ?: MaterialTheme.typography.bodyLarge.fontFamily
                    ?: androidx.compose.ui.text.font.FontFamily.Default
    val appearance = viewModel.appearance
    val family =
            remember(appearance.fontFamily, launcherFamily) {
                viewModel.fontCache.familyFor(appearance.fontFamily, launcherFamily)
            }
    val bodyStyle =
            noteTextStyle(
                    MaterialTheme.typography.bodyLarge,
                    appearance,
                    family,
                    MaterialTheme.colorScheme.onBackground,
            )
    val titleStyle =
            bodyStyle.copy(
                    fontSize = (bodyStyle.fontSize.value * 1.25f).sp,
                    lineHeight = (bodyStyle.lineHeight.value * 1.25f).sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val cursor = SolidColor(MaterialTheme.colorScheme.primary)
    val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BasicTextField(
                value = viewModel.draftTitle,
                onValueChange = viewModel::onTitleChange,
                textStyle = titleStyle,
                cursorBrush = cursor,
                singleLine = false,
                modifier = Modifier.fillMaxWidth().testTag("note_title"),
                decorationBox = { inner ->
                    Box {
                        if (viewModel.draftTitle.isEmpty()) {
                            Text(
                                    stringResource(R.string.notes_title_hint),
                                    style = titleStyle.copy(color = hintColor),
                            )
                        }
                        inner()
                    }
                },
        )
        Spacer(Modifier.height(8.dp))
        BasicTextField(
                value = viewModel.draftBody,
                onValueChange = viewModel::onBodyChange,
                textStyle = bodyStyle,
                cursorBrush = cursor,
                modifier = Modifier.fillMaxWidth().height(280.dp).testTag("note_body"),
                decorationBox = { inner ->
                    Box {
                        if (viewModel.draftBody.isEmpty()) {
                            Text(
                                    stringResource(R.string.notes_body_hint),
                                    style = bodyStyle.copy(color = hintColor),
                            )
                        }
                        inner()
                    }
                },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showAppearance = !showAppearance }) {
                Text(stringResource(R.string.notes_appearance))
            }
            TextButton(onClick = { confirmDelete = true }) {
                Text(stringResource(R.string.notes_delete))
            }
        }
        if (showAppearance) {
            AppearanceControls(appearance, viewModel::onAppearanceChange)
        }
    }
    if (confirmDelete) {
        AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(stringResource(R.string.notes_delete_confirm_title)) },
                text = { Text(stringResource(R.string.notes_delete_confirm_body)) },
                confirmButton = {
                    TextButton(
                            onClick = {
                                confirmDelete = false
                                viewModel.deleteCurrent()
                            }
                    ) {
                        Text(stringResource(R.string.notes_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) {
                        Text(stringResource(R.string.notes_cancel))
                    }
                },
        )
    }
}

private data class Swatch(val argb: Int, val labelRes: Int)

private val TextSwatches =
        listOf(
                Swatch(0, R.string.notes_color_default),
                Swatch(0xFFFFFFFF.toInt(), R.string.notes_color_white),
                Swatch(0xFF000000.toInt(), R.string.notes_color_black),
                Swatch(0xFFFFC857.toInt(), R.string.notes_color_amber),
                Swatch(0xFF7FD6A0.toInt(), R.string.notes_color_green),
                Swatch(0xFF7FB6FF.toInt(), R.string.notes_color_blue),
                Swatch(0xFFFF8FA3.toInt(), R.string.notes_color_rose),
        )

private val BackgroundSwatches =
        listOf(
                Swatch(0, R.string.notes_color_transparent),
                Swatch(0x99000000.toInt(), R.string.notes_color_scrim),
                Swatch(0xFF000000.toInt(), R.string.notes_color_black),
                Swatch(0xFFFFFFFF.toInt(), R.string.notes_color_white),
                Swatch(0xFF3A2E12.toInt(), R.string.notes_color_amber),
                Swatch(0xFF12301D.toInt(), R.string.notes_color_green),
                Swatch(0xFF12233A.toInt(), R.string.notes_color_blue),
        )

@Composable
private fun AppearanceControls(a: NoteAppearance, onChange: (NoteAppearance) -> Unit) {
    val fontOptions =
            buildList {
                add("" to stringResource(R.string.typography_follow_launcher))
                FontRegistry.bundledFonts.forEach { add(FontRegistry.storageValueFor(it) to it.label) }
                add("sans-serif" to stringResource(R.string.typography_system_sans))
                add("serif" to stringResource(R.string.typography_system_serif))
                add("monospace" to stringResource(R.string.typography_system_monospace))
            }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(stringResource(R.string.notes_font))
        ChipRow(fontOptions.map { it.second }, fontOptions.indexOfFirst { it.first == a.fontFamily }) {
            onChange(a.copy(fontFamily = fontOptions[it].first))
        }
        Label(stringResource(R.string.notes_font_size, a.fontSizeSp.toInt()))
        Slider(
                value = a.fontSizeSp,
                onValueChange = { onChange(a.copy(fontSizeSp = it)) },
                valueRange = NoteAppearance.MIN_SIZE_SP..NoteAppearance.MAX_SIZE_SP,
        )
        Label(stringResource(R.string.notes_font_weight))
        val weights = listOf(300, 400, 500, 600, 700)
        ChipRow(weights.map { it.toString() }, weights.indexOf(a.fontWeight)) {
            onChange(a.copy(fontWeight = weights[it]))
        }
        Label(stringResource(R.string.notes_text_color))
        SwatchRow(TextSwatches, a.textColor) { onChange(a.copy(textColor = it)) }
        Label(stringResource(R.string.notes_alignment))
        val alignLabels =
                listOf(
                        stringResource(R.string.notes_align_start),
                        stringResource(R.string.notes_align_center),
                        stringResource(R.string.notes_align_end),
                )
        ChipRow(alignLabels, a.alignment.ordinal) {
            onChange(a.copy(alignment = NoteAlignment.entries[it]))
        }
        Label(stringResource(R.string.notes_line_spacing, "%.1f".format(a.lineSpacing)))
        Slider(
                value = a.lineSpacing,
                onValueChange = { onChange(a.copy(lineSpacing = it)) },
                valueRange = NoteAppearance.MIN_LINE_SPACING..NoteAppearance.MAX_LINE_SPACING,
        )
        Label(stringResource(R.string.notes_background))
        SwatchRow(BackgroundSwatches, a.backgroundColor) { onChange(a.copy(backgroundColor = it)) }
        Label(stringResource(R.string.notes_padding, a.paddingDp))
        Slider(
                value = a.paddingDp.toFloat(),
                onValueChange = { onChange(a.copy(paddingDp = it.toInt())) },
                valueRange = 0f..NoteAppearance.MAX_PADDING_DP.toFloat(),
        )
        Label(stringResource(R.string.notes_max_lines, a.maxLines))
        Slider(
                value = a.maxLines.toFloat(),
                onValueChange = { onChange(a.copy(maxLines = it.toInt())) },
                valueRange = 1f..NoteAppearance.MAX_LINES.toFloat(),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            TextButton(onClick = { onSelect(index) }) {
                Text(
                        label,
                        color =
                                if (index == selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun SwatchRow(swatches: List<Swatch>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        swatches.forEach { swatch ->
            val label = stringResource(swatch.labelRes)
            Box(
                    Modifier.size(36.dp)
                            .background(
                                    if (swatch.argb == 0) Color.Transparent else Color(swatch.argb),
                                    CircleShape,
                            )
                            .border(
                                    if (swatch.argb == selected) 3.dp else 1.dp,
                                    if (swatch.argb == selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary,
                                    CircleShape,
                            )
                            .clickable(onClickLabel = label) { onSelect(swatch.argb) },
                    contentAlignment = Alignment.Center,
            ) {
                if (swatch.argb == 0) {
                    Text(label.take(1), style = TextStyle(fontSize = 12.sp), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
