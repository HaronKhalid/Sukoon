package com.lu4p.fokuslauncher.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeColorField
import com.lu4p.fokuslauncher.theme.ThemeColorMath
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.ThemeGradient
import com.lu4p.fokuslauncher.theme.backgroundBrush
import com.lu4p.fokuslauncher.theme.ThemeImportResult
import com.lu4p.fokuslauncher.theme.ThemeState
import com.lu4p.fokuslauncher.theme.get
import com.lu4p.fokuslauncher.theme.isReadable
import com.lu4p.fokuslauncher.theme.themedSettingsScrim
import com.lu4p.fokuslauncher.ui.components.FokusAlertDialog
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.settings.components.SectionHeader
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow

private enum class StudioDialog { SAVE, RENAME, DELETE, IMPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioScreen(
        viewModel: ThemeStudioViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = themedSettingsScrim,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state) { viewModel.initFrom(state) }

    var dialog by remember { mutableStateOf<StudioDialog?>(null) }
    val source = viewModel.sourceOf(state, draft)
    val edited = source != draft
    val isCustom = state.customThemes.any { it.id == draft.id }
    val readable = draft.isReadable()

    fun toast(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .background(backgroundScrim)
                            .navigationBarsPadding()
                            .testTag("theme_studio_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.theme_studio_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        HomePreviewCard(
                theme = draft,
                modifier =
                        Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                .fillMaxWidth()
                                .height(230.dp)
                                .testTag("theme_preview"),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (!readable) {
                item {
                    Text(
                            stringResource(R.string.theme_studio_low_contrast),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                ActionRow(
                        stringResource(R.string.theme_studio_apply),
                        enabled = readable && (state.selectedId != draft.id || edited),
                ) {
                    viewModel.apply(state) { ok -> toast(if (ok) R.string.theme_studio_saved else R.string.theme_studio_save_failed) }
                }
            }
            if (!state.isClassic) {
                item { ActionRow(stringResource(R.string.theme_studio_reset)) { viewModel.reset() } }
            }
            item { SettingsDivider() }

            item { SectionHeader(stringResource(R.string.theme_studio_section_themes)) }
            if (state.isClassic) {
                item {
                    Text(
                            stringResource(R.string.theme_studio_none_applied),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }
            items(state.allThemes, key = { it.id }) { t ->
                val badge =
                        when {
                            state.selectedId == t.id -> stringResource(R.string.theme_studio_badge_applied)
                            state.customThemes.any { it.id == t.id } -> stringResource(R.string.theme_studio_badge_custom)
                            else -> null
                        }
                SettingsRow(
                        label = t.name,
                        subtitle = badge,
                        modifier = Modifier.testTag("theme_row_${t.id}"),
                        onClick = { viewModel.preview(t) },
                        leading = { Swatches(t) },
                        labelColor =
                                if (t.id == draft.id) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onBackground,
                )
            }
            item { SettingsDivider() }

            item { SectionHeader(stringResource(R.string.theme_studio_section_colors)) }
            items(
                    ThemeColorField.entries.filter { it != ThemeColorField.BACKGROUND || draft.gradient == null },
                    key = { it.name },
            ) { field ->
                ColorFieldRow(field, draft.colors.get(field)) { viewModel.setColor(field, it) }
            }
            item { SettingsDivider() }

            item { SectionHeader(stringResource(R.string.theme_studio_section_gradient)) }
            item {
                SettingsRow(
                        label = stringResource(R.string.theme_studio_gradient_enable),
                        modifier = Modifier.testTag("theme_gradient_toggle"),
                        onClick = { viewModel.setGradientEnabled(draft.gradient == null) },
                        trailing = {
                            androidx.compose.material3.Switch(
                                    checked = draft.gradient != null,
                                    onCheckedChange = { viewModel.setGradientEnabled(it) },
                            )
                        },
                )
            }
            draft.gradient?.let { g ->
                itemsIndexed(g.stops) { i, stop ->
                    HexRow(
                            label = stringResource(R.string.theme_studio_gradient_stop, i + 1),
                            color = stop,
                            tag = "theme_gradient_stop_$i",
                    ) { viewModel.setGradientStop(i, it) }
                }
                item {
                    ActionRow(
                            stringResource(
                                    if (g.stops.size < ThemeGradient.MAX_STOPS) R.string.theme_studio_gradient_add_stop
                                    else R.string.theme_studio_gradient_remove_stop
                            )
                    ) { viewModel.setThirdStop(g.stops.size < ThemeGradient.MAX_STOPS) }
                }
                item {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        Text(
                                stringResource(R.string.theme_studio_gradient_angle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                        )
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ThemeGradient.ANGLE_PRESETS, key = { it }) { a ->
                                androidx.compose.material3.FilterChip(
                                        selected = g.angleDegrees == a,
                                        onClick = { viewModel.setGradientAngle(a) },
                                        label = { Text(stringResource(R.string.theme_studio_gradient_angle_value, a)) },
                                        modifier = Modifier.testTag("theme_gradient_angle_$a"),
                                )
                            }
                        }
                    }
                }
            }
            if (edited && source != null) {
                item { ActionRow(stringResource(R.string.theme_studio_discard)) { viewModel.discardEdits(state) } }
            }
            item { SettingsDivider() }

            item { SectionHeader(stringResource(R.string.theme_studio_section_actions)) }
            if (isCustom) {
                item {
                    ActionRow(stringResource(R.string.theme_studio_update), enabled = readable && edited) {
                        viewModel.saveAs(state, draft.name) {
                            toast(if (it) R.string.theme_studio_saved else R.string.theme_studio_save_failed)
                        }
                    }
                }
                item { ActionRow(stringResource(R.string.theme_studio_rename)) { dialog = StudioDialog.RENAME } }
                item { ActionRow(stringResource(R.string.theme_studio_delete)) { dialog = StudioDialog.DELETE } }
            }
            item {
                ActionRow(stringResource(R.string.theme_studio_duplicate), enabled = readable) {
                    viewModel.duplicate(state, draft.name) {
                        toast(if (it) R.string.theme_studio_saved else R.string.theme_studio_save_failed)
                    }
                }
            }
            item {
                ActionRow(stringResource(R.string.theme_studio_save), enabled = readable) {
                    dialog = StudioDialog.SAVE
                }
            }
            item {
                ActionRow(stringResource(R.string.theme_studio_export)) {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("theme", viewModel.exportJson()))
                    toast(R.string.theme_studio_exported)
                }
            }
            item { ActionRow(stringResource(R.string.theme_studio_import)) { dialog = StudioDialog.IMPORT } }
        }
    }

    when (dialog) {
        StudioDialog.SAVE, StudioDialog.RENAME ->
                NameDialog(
                        title =
                                stringResource(
                                        if (dialog == StudioDialog.SAVE) R.string.theme_studio_save
                                        else R.string.theme_studio_rename
                                ),
                        initial =
                                if (dialog == StudioDialog.SAVE && !isCustom)
                                    stringResource(R.string.theme_studio_default_name)
                                else draft.name,
                        onDismiss = { dialog = null },
                        onConfirm = { name ->
                            val d = dialog
                            dialog = null
                            if (d == StudioDialog.SAVE) {
                                viewModel.saveAs(state, name) {
                                    toast(if (it) R.string.theme_studio_saved else R.string.theme_studio_save_failed)
                                }
                            } else {
                                viewModel.rename(draft.id, name) {
                                    if (!it) toast(R.string.theme_studio_save_failed)
                                }
                            }
                        },
                )
        StudioDialog.DELETE ->
                FokusAlertDialog(
                        onDismissRequest = { dialog = null },
                        title = { Text(stringResource(R.string.theme_studio_delete_title), color = MaterialTheme.colorScheme.onBackground) },
                        text = {
                            Text(
                                    stringResource(R.string.theme_studio_delete_message, draft.name),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        confirmButton = {
                            FokusTextButton(onClick = {
                                dialog = null
                                viewModel.delete(draft.id)
                            }) { Text(stringResource(R.string.theme_studio_delete), color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            FokusTextButton(onClick = { dialog = null }) {
                                Text(stringResource(R.string.theme_studio_cancel), color = MaterialTheme.colorScheme.onBackground)
                            }
                        },
                )
        StudioDialog.IMPORT ->
                ImportDialog(
                        onDismiss = { dialog = null },
                        onConfirm = { json ->
                            dialog = null
                            viewModel.import(json) { r ->
                                toast(
                                        when (r) {
                                            is ThemeImportResult.Imported -> R.string.theme_studio_import_ok
                                            ThemeImportResult.LimitReached -> R.string.theme_studio_limit_reached
                                            is ThemeImportResult.Rejected -> R.string.theme_studio_import_failed
                                        }
                                )
                            }
                        },
                )
        null -> Unit
    }
}

@Composable
private fun ActionRow(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    SettingsRow(
            label = label,
            labelColor =
                    if (enabled) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
            onClick = if (enabled) onClick else null,
    )
}

@Composable
private fun Swatches(theme: ThemeDefinition) {
    Box(
            Modifier.size(28.dp)
                    .background(theme.backgroundBrush(), RoundedCornerShape(8.dp))
                    .border(2.dp, Color(theme.colors.accent), RoundedCornerShape(8.dp))
    )
}

@Composable
private fun ColorFieldRow(field: ThemeColorField, color: Int, onValid: (Int) -> Unit) {
    val label =
            stringResource(
                    when (field) {
                        ThemeColorField.BACKGROUND -> R.string.theme_studio_color_background
                        ThemeColorField.PRIMARY_TEXT -> R.string.theme_studio_color_primary_text
                        ThemeColorField.SECONDARY_TEXT -> R.string.theme_studio_color_secondary_text
                        ThemeColorField.ACCENT -> R.string.theme_studio_color_accent
                        ThemeColorField.SURFACE -> R.string.theme_studio_color_surface
                    }
            )
    HexRow(label, color, "theme_color_${field.name}", onValid)
}

@Composable
private fun HexRow(label: String, color: Int, tag: String, onValid: (Int) -> Unit) {
    var text by remember(tag) { mutableStateOf(ThemeColorMath.toHex(color)) }
    LaunchedEffect(color) {
        if (ThemeColorMath.parseHex(text) != color) text = ThemeColorMath.toHex(color)
    }
    val parsed = ThemeColorMath.parseHex(text)
    Row24 {
        Box(Modifier.size(32.dp).background(Color(color), RoundedCornerShape(8.dp)))
        OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it.take(7)
                    ThemeColorMath.parseHex(text)?.let(onValid)
                },
                label = { Text(label) },
                isError = parsed == null,
                supportingText = if (parsed == null) ({ Text(stringResource(R.string.theme_studio_color_invalid)) }) else null,
                singleLine = true,
                modifier = Modifier.weight(1f).padding(start = 12.dp).testTag(tag),
        )
    }
}

@Composable
private fun Row24(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
    )
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(ThemeDefinition.MAX_NAME_LENGTH) },
                        label = { Text(stringResource(R.string.theme_studio_name_label)) },
                        singleLine = true,
                )
            },
            confirmButton = {
                FokusTextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.theme_studio_ok), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.theme_studio_cancel), color = MaterialTheme.colorScheme.onBackground)
                }
            },
    )
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var json by remember { mutableStateOf("") }
    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.theme_studio_import_title), color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                        value = json,
                        onValueChange = { json = it.take(65_536) },
                        label = { Text(stringResource(R.string.theme_studio_import_hint)) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.testTag("theme_import_field"),
                )
            },
            confirmButton = {
                FokusTextButton(onClick = { onConfirm(json) }, enabled = json.isNotBlank()) {
                    Text(stringResource(R.string.theme_studio_import), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.theme_studio_cancel), color = MaterialTheme.colorScheme.onBackground)
                }
            },
    )
}
