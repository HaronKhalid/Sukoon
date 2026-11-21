package com.lu4p.fokuslauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import com.lu4p.fokuslauncher.clock.faces.ClockFaceGallery
import com.lu4p.fokuslauncher.clock.styles.ClockStylePickerContent
import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.home.layout.ElementConfig
import com.lu4p.fokuslauncher.home.widgets.HomeWidget
import com.lu4p.fokuslauncher.home.widgets.WidgetCatalog
import com.lu4p.fokuslauncher.home.widgets.WidgetScale
import com.lu4p.fokuslauncher.home.widgets.rememberBatteryInfo
import com.lu4p.fokuslauncher.pomodoro.PomodoroUiState
import kotlin.math.roundToInt
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
            items(layout.ordered().map { it.type }.filter { !it.optionalWidget || layout.isVisible(it) }) { type ->
                ElementEditor(layout, type, viewModel)
                SettingsDivider()
            }
            item { AddWidgetGallery(layout, viewModel) }
        }
    }
}

@Composable
private fun ElementEditor(layout: HomeLayoutConfig, type: HomeElementType, vm: HomeLayoutViewModel) {
    val e = layout.element(type)
    // Every element is drawn on the home screen now (favourites stay anchored at the bottom).
    val rendered = true
    Column(Modifier.testTag("layout_element_${type.name}")) {
        if (type.optionalWidget) {
            SettingsRow(label = stringResource(WidgetCatalog.title(type)))
            WidgetPreview(type, e)
            StyleChooser(type, e.style) { v -> vm.updateLayout { it.update(type) { c -> c.copy(style = v) } } }
        } else {
            SettingsToggleRow(
                    label = type.pretty(),
                    subtitle = if (rendered) null else stringResource(R.string.layout_studio_not_rendered),
                    checked = e.visible,
                    onCheckedChange = { v -> vm.updateLayout { it.update(type) { c -> c.copy(visible = v) } } },
            )
        }
        if (e.visible) {
            val up = stringResource(R.string.layout_studio_move_up)
            val down = stringResource(R.string.layout_studio_move_down)
            val remove = stringResource(R.string.layout_studio_remove_widget)
            val actions = buildList<Pair<String, () -> Unit>> {
                add(up to { vm.updateLayout { it.move(type, -1) } })
                add(down to { vm.updateLayout { it.move(type, 1) } })
                if (type.optionalWidget) {
                    add(remove to { vm.updateLayout { it.update(type) { c -> c.copy(visible = false) } } })
                }
            }
            ActionRow(*actions.toTypedArray())
            EnumDropdown(stringResource(R.string.layout_studio_alignment), ElementAlignment.entries, e.alignment) { v ->
                vm.updateLayout { it.update(type) { c -> c.copy(alignment = v) } }
            }
            SizeSlider(e.scalePct) { v -> vm.updateLayout { it.update(type) { c -> c.copy(scalePct = v) } } }
            EnumDropdown(stringResource(R.string.layout_studio_spacing), ElementSpacing.entries, e.spacing) { v ->
                vm.updateLayout { it.update(type) { c -> c.copy(spacing = v) } }
            }
        }
    }
}

/** Parses `RRGGBB` or `#RRGGBB` into an opaque ARGB int, or null when invalid. */
internal fun parseAccentHex(input: String): Int? {
    val hex = input.trim().removePrefix("#")
    if (hex.length != 6 || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return (0xFF000000L or hex.toLong(16)).toInt()
}

@Composable
private fun AccentPicker(accentArgb: Int, onChange: (Int) -> Unit) {
    var text by remember(accentArgb) {
        mutableStateOf(if (accentArgb == 0) "" else "%06X".format(accentArgb and 0xFFFFFF))
    }
    val parsed = parseAccentHex(text)
    SettingsRow(label = stringResource(R.string.clock_studio_accent))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        FilterChip(
                selected = accentArgb == 0,
                onClick = { onChange(0) },
                label = { Text(stringResource(R.string.clock_studio_accent_theme)) },
                modifier = Modifier.padding(horizontal = 4.dp),
        )
        listOf(0xFFFFC857, 0xFF9BE8C4, 0xFF8FC8FF, 0xFFFF8FA3, 0xFFC3A6FF).forEach { argb ->
            val v = argb.toInt()
            Box(Modifier.padding(horizontal = 6.dp).size(32.dp).background(Color(v), CircleShape)
                    .border(if (accentArgb == v) 3.dp else 1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = if (accentArgb == v) 1f else 0.3f), CircleShape)
                    .clickable { onChange(v) })
        }
    }
    OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(7) },
            label = { Text(stringResource(R.string.clock_studio_accent_custom_hex)) },
            singleLine = true,
            isError = text.isNotEmpty() && parsed == null,
            supportingText = { if (text.isNotEmpty() && parsed == null) Text(stringResource(R.string.clock_studio_accent_invalid)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("accent_hex"),
    )
    FokusTextButton(onClick = { parsed?.let(onChange) }, enabled = parsed != null) {
        Text(stringResource(R.string.clock_studio_accent_apply))
    }
}

@Composable
private fun SizeSlider(percent: Int, onChange: (Int) -> Unit) {
    SettingsRow(label = stringResource(R.string.layout_studio_size_percent, percent))
    Slider(
            value = percent.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(ElementConfig.MIN_PERCENT, ElementConfig.MAX_PERCENT)) },
            valueRange = ElementConfig.MIN_PERCENT.toFloat()..ElementConfig.MAX_PERCENT.toFloat(),
            modifier = Modifier.padding(horizontal = 24.dp).testTag("size_slider"),
    )
}

@Composable
private fun WidgetPreview(type: HomeElementType, e: ElementConfig) {
    val battery = rememberBatteryInfo()
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        WidgetScale(e.scale) {
            HomeWidget(type, e.style, e.alignment, battery, PREVIEW_POMODORO, {})
        }
    }
}

private val PREVIEW_POMODORO = PomodoroUiState(enabled = true, remainingText = "17:42", progress = 0.3f)

@Composable
private fun StyleChooser(type: HomeElementType, selected: Int, onSelect: (Int) -> Unit) {
    Text(stringResource(R.string.layout_studio_widget_style), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        WidgetCatalog.styleLabels(type).forEachIndexed { i, label ->
            FilterChip(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Gallery of widgets not yet on the home screen, each with a live preview and an Add button. */
@Composable
private fun AddWidgetGallery(layout: HomeLayoutConfig, vm: HomeLayoutViewModel) {
    val battery = rememberBatteryInfo()
    val missing = WidgetCatalog.addable.filter { !layout.isVisible(it) }
    Column(Modifier.testTag("widget_gallery")) {
        SettingsRow(
                label = stringResource(R.string.layout_studio_add_widget),
                subtitle = stringResource(
                        if (missing.isEmpty()) R.string.layout_studio_all_added else R.string.layout_studio_add_widget_hint,
                ),
        )
        missing.forEach { type ->
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("widget_card_${type.name}")) {
                Text(stringResource(WidgetCatalog.title(type)), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground)
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    WidgetScale(0.8f) {
                        HomeWidget(type, 0, ElementAlignment.START, battery, PREVIEW_POMODORO, {})
                    }
                }
                FokusTextButton(onClick = {
                    vm.updateLayout { it.update(type) { c -> c.copy(visible = true) } }
                }) { Text(stringResource(R.string.layout_studio_widget_add)) }
            }
            SettingsDivider()
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
                            sizeScale = state.layout.element(HomeElementType.CLOCK).scale,
                            onClick = {},
                    )
                }
                SettingsRow(label = stringResource(R.string.clock_studio_faces))
                ClockFaceGallery(
                        settings = c,
                        selectedId = ClockFaceRegistry.resolveId(c.faceId, c.style),
                        onSelect = { id -> change { it.copy(faceId = id, styleId = "") } },
                )
                SettingsRow(label = stringResource(R.string.clock_styles_title))
                ClockStylePickerContent()
                SizeSlider(state.layout.element(HomeElementType.CLOCK).scalePct) { v ->
                    viewModel.updateLayout { it.update(HomeElementType.CLOCK) { e -> e.copy(scalePct = v) } }
                }
                AccentPicker(c.accentArgb) { argb -> change { it.copy(accentArgb = argb) } }
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
