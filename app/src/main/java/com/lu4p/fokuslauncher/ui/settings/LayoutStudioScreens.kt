package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.ClockDateStyle
import com.lu4p.fokuslauncher.home.layout.ClockFontWeight
import com.lu4p.fokuslauncher.home.layout.ClockHourFormat
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.ClockStyle
import com.lu4p.fokuslauncher.home.layout.ConfiguredClock
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.ElementSize
import com.lu4p.fokuslauncher.home.layout.ElementSpacing
import com.lu4p.fokuslauncher.home.layout.HomeElementType
import com.lu4p.fokuslauncher.home.layout.HomeLayoutConfig
import com.lu4p.fokuslauncher.home.layout.HomeLayoutViewModel
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.ui.components.FokusTextButton
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDivider
import com.lu4p.fokuslauncher.ui.settings.components.SettingsDropdown
import com.lu4p.fokuslauncher.ui.settings.components.SettingsRow
import com.lu4p.fokuslauncher.ui.settings.components.SettingsToggleRow
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop

// Option labels are derived from enum names (English only); titles and hints are string resources.
private fun Enum<*>.pretty(): String =
        name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Composable
private fun <T : Enum<T>> EnumDropdown(title: String, options: List<T>, selected: T, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SettingsDropdown(
            title = title,
            options = options,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            selectedDisplayText = selected.pretty(),
            itemContent = { Text(it.pretty()) },
            onItemSelected = onSelected,
    )
}

@Composable
fun LayoutStudioScreen(
        viewModel: HomeLayoutViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onOpenClockStudio: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = state.layout
    Column(Modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding().testTag("layout_studio_screen")) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.layout_studio_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                SettingsRow(
                        label = stringResource(R.string.layout_studio_open_clock),
                        onClick = onOpenClockStudio,
                )
                SettingsDivider()
                SettingsRow(
                        label = stringResource(R.string.layout_studio_presets),
                        subtitle = stringResource(R.string.layout_studio_presets_subtitle) + "\n" +
                                stringResource(R.string.layout_studio_current_preset, layout.preset.pretty()),
                )
            }
            items(listOf(LayoutPreset.PURE_MINIMAL, LayoutPreset.AESTHETIC, LayoutPreset.FOCUS, LayoutPreset.INFORMATION)) {
                SettingsRow(label = it.pretty(), onClick = { viewModel.applyPreset(it) })
            }
            item {
                SettingsRow(
                        label = stringResource(R.string.layout_studio_reset),
                        onClick = { viewModel.applyPreset(LayoutPreset.DEFAULT) },
                )
                SettingsDivider()
                SettingsRow(
                        label = stringResource(R.string.layout_studio_elements),
                        subtitle = stringResource(R.string.layout_studio_order_note),
                )
            }
            items(layout.ordered().map { it.type }) { type ->
                ElementEditor(layout, type, viewModel)
                SettingsDivider()
            }
        }
    }
}

@Composable
private fun ElementEditor(layout: HomeLayoutConfig, type: HomeElementType, vm: HomeLayoutViewModel) {
    val e = layout.element(type)
    // Every element is drawn on the home screen now (favourites stay anchored at the bottom).
    val rendered = true
    Column(Modifier.testTag("layout_element_${type.name}")) {
        SettingsToggleRow(
                label = type.pretty(),
                subtitle = if (rendered) null else stringResource(R.string.layout_studio_not_rendered),
                checked = e.visible,
                onCheckedChange = { v -> vm.updateLayout { it.update(type) { c -> c.copy(visible = v) } } },
        )
        if (e.visible) {
            ActionRow(
                    stringResource(R.string.layout_studio_move_up) to { vm.updateLayout { it.move(type, -1) } },
                    stringResource(R.string.layout_studio_move_down) to { vm.updateLayout { it.move(type, 1) } },
            )
            EnumDropdown(stringResource(R.string.layout_studio_alignment), ElementAlignment.entries, e.alignment) { v ->
                vm.updateLayout { it.update(type) { c -> c.copy(alignment = v) } }
            }
            EnumDropdown(stringResource(R.string.layout_studio_size), ElementSize.entries, e.size) { v ->
                vm.updateLayout { it.update(type) { c -> c.copy(size = v) } }
            }
            EnumDropdown(stringResource(R.string.layout_studio_spacing), ElementSpacing.entries, e.spacing) { v ->
                vm.updateLayout { it.update(type) { c -> c.copy(spacing = v) } }
            }
        }
    }
}

@Composable
private fun ActionRow(vararg actions: Pair<String, () -> Unit>) {
    Row(Modifier.padding(horizontal = 12.dp)) {
        actions.forEach { (label, onClick) -> FokusTextButton(onClick = onClick) { Text(label) } }
    }
}

@Composable
fun ClockStudioScreen(
        viewModel: HomeLayoutViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = state.clock
    fun change(block: (ClockSettings) -> ClockSettings) = viewModel.updateClock(block)
    Column(Modifier.fillMaxSize().background(backgroundScrim).navigationBarsPadding().testTag("clock_studio_screen")) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.clock_studio_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                SettingsRow(label = stringResource(R.string.clock_studio_preview))
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ConfiguredClock(
                            settings = c,
                            sizeScale = state.layout.element(HomeElementType.CLOCK).size.scale,
                            onClick = {},
                    )
                }
                EnumDropdown(stringResource(R.string.clock_studio_style), ClockStyle.entries, c.style) { v -> change { it.copy(style = v) } }
                EnumDropdown(stringResource(R.string.clock_studio_hour_format), ClockHourFormat.entries, c.hourFormat) { v -> change { it.copy(hourFormat = v) } }
                SettingsToggleRow(
                        label = stringResource(R.string.clock_studio_show_seconds),
                        subtitle = stringResource(R.string.clock_studio_show_seconds_subtitle),
                        checked = c.showSeconds,
                        onCheckedChange = { v -> change { it.copy(showSeconds = v) } },
                )
                SettingsRow(label = stringResource(
                        R.string.clock_studio_font_size,
                        if (c.fontSizeSp == 0) stringResource(R.string.clock_studio_font_size_theme) else "${c.fontSizeSp}sp",
                ))
                Slider(
                        value = c.fontSizeSp.toFloat(),
                        onValueChange = { v -> change { it.copy(fontSizeSp = if (v < ClockSettings.MIN_FONT_SP) 0 else v.toInt()) } },
                        valueRange = 0f..ClockSettings.MAX_FONT_SP.toFloat(),
                        modifier = Modifier.padding(horizontal = 24.dp),
                )
                EnumDropdown(stringResource(R.string.clock_studio_font_weight), ClockFontWeight.entries, c.weight) { v -> change { it.copy(weight = v) } }
                SettingsRow(label = stringResource(R.string.clock_studio_letter_spacing, c.letterSpacingSp))
                Slider(
                        value = c.letterSpacingSp,
                        onValueChange = { v -> change { it.copy(letterSpacingSp = v) } },
                        valueRange = -2f..12f,
                        modifier = Modifier.padding(horizontal = 24.dp),
                )
                SettingsRow(label = stringResource(R.string.clock_studio_line_height, c.lineHeightMultiplier))
                Slider(
                        value = c.lineHeightMultiplier,
                        onValueChange = { v -> change { it.copy(lineHeightMultiplier = v) } },
                        valueRange = 0.8f..1.6f,
                        modifier = Modifier.padding(horizontal = 24.dp),
                )
                SettingsRow(label = stringResource(R.string.clock_studio_color))
                ActionRow(
                        stringResource(R.string.clock_studio_color_theme) to { change { it.copy(colorArgb = 0) } },
                        stringResource(R.string.clock_studio_color_white) to { change { it.copy(colorArgb = 0xFFFFFFFF.toInt()) } },
                        stringResource(R.string.clock_studio_color_amber) to { change { it.copy(colorArgb = 0xFFFFC857.toInt()) } },
                        stringResource(R.string.clock_studio_color_mint) to { change { it.copy(colorArgb = 0xFF9BE8C4.toInt()) } },
                        stringResource(R.string.clock_studio_color_sky) to { change { it.copy(colorArgb = 0xFF8FC8FF.toInt()) } },
                )
                EnumDropdown(stringResource(R.string.clock_studio_alignment), ElementAlignment.entries, c.alignment) { v -> change { it.copy(alignment = v) } }
                SettingsToggleRow(
                        label = stringResource(R.string.clock_studio_show_date),
                        checked = c.showDate,
                        onCheckedChange = { v -> change { it.copy(showDate = v) } },
                )
                EnumDropdown(stringResource(R.string.clock_studio_date_format), ClockDateStyle.entries, c.dateStyle) { v -> change { it.copy(dateStyle = v) } }
                SettingsRow(label = stringResource(R.string.clock_studio_reset), onClick = { change { ClockSettings() } })
            }
        }
    }
}
