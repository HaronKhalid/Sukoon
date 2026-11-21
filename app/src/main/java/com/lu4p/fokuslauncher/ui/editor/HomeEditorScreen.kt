package com.lu4p.fokuslauncher.ui.editor

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.backdrop.BackdropLayer
import com.lu4p.fokuslauncher.backdrop.Backdrops
import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeColorMath
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.backgroundBrush
import com.lu4p.fokuslauncher.theme.withAppTheme
import com.lu4p.fokuslauncher.typography.FontRegistry
import com.lu4p.fokuslauncher.typography.TextElement
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.settings.HomePreviewCard

/** Fixed so switching tabs never changes the preview size (no layout jumps). */
private val PANEL_HEIGHT = 204.dp

/**
 * One tidy place to edit the home screen: a big live phone preview, a fixed-height tool panel and a
 * bottom toolbar with Style, Clock, Font, Colour and More. Edits apply to the preview instantly and
 * are saved (debounced) as you go; Undo and Reset live in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEditorScreen(
        viewModel: HomeEditorViewModel = hiltViewModel(),
        onNavigateBack: () -> Unit = {},
        onOpenLayoutStudio: () -> Unit = {},
        onOpenNoteAppearance: () -> Unit = {},
        onOpenWallpaper: () -> Unit = {},
        onOpenThemeStudio: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snapshot = ui.snapshot
    var showClockSheet by rememberSaveable { mutableStateOf(false) }

    ui.message?.let { res ->
        LaunchedEffect(res) {
            Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    if (snapshot == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("home_editor_loading"))
        return
    }
    val theme = snapshot.effectiveTheme
    // The chrome follows the draft instantly, not the (debounced) saved theme.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.withAppTheme(theme)) {
        val scheme = MaterialTheme.colorScheme
        Column(
                Modifier.fillMaxSize()
                        .background(Color(theme.representativeBackground))
                        .testTag("home_editor_screen"),
        ) {
            FokusSettingsTopBar(
                    titleText = stringResource(R.string.editor_title),
                    onNavigateBack = onNavigateBack,
                    containerColor = Color.Transparent,
                    actions = {
                        IconButton(onClick = viewModel::undo, enabled = ui.canUndo, modifier = Modifier.testTag("editor_undo")) {
                            Icon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.editor_undo),
                                    tint = scheme.onBackground.copy(alpha = if (ui.canUndo) 1f else 0.35f))
                        }
                        IconButton(onClick = viewModel::reset, enabled = ui.isModified, modifier = Modifier.testTag("editor_reset")) {
                            Icon(Icons.Outlined.RestartAlt, stringResource(R.string.editor_reset),
                                    tint = scheme.onBackground.copy(alpha = if (ui.isModified) 1f else 0.35f))
                        }
                    },
            )

            val appFamily: FontFamily? =
                    remember(snapshot.appFont) { snapshot.appFont.takeIf { it.isNotEmpty() }?.let(viewModel::fontFamily) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center) {
                val frameWidth = minOf(maxWidth, maxHeight * 0.5f)
                PhoneFrame(
                        modifier = Modifier.width(frameWidth).height(frameWidth * 2f),
                        outline = scheme.onBackground.copy(alpha = 0.45f),
                ) {
                    HomePreviewCard(
                            theme = theme,
                            modifier = Modifier.fillMaxSize(),
                            clockFaceId = snapshot.clockFaceId,
                            layoutPreset = snapshot.layout.preset.name,
                            cornerRadius = 22.dp,
                            fontFamily = appFamily,
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(PANEL_HEIGHT).padding(horizontal = 16.dp)) {
                when (ui.tab) {
                    EditorTab.STYLE -> StylePanel(snapshot, ui.customThemes, viewModel)
                    EditorTab.CLOCK -> ClockPanel(snapshot, viewModel) { showClockSheet = true }
                    EditorTab.FONT -> FontPanel(snapshot, viewModel)
                    EditorTab.COLOUR -> ColourPanel(snapshot, viewModel)
                    EditorTab.MORE ->
                        MorePanel(snapshot, viewModel, onOpenLayoutStudio, onOpenNoteAppearance, onOpenWallpaper, onOpenThemeStudio)
                }
            }

            Box(Modifier.navigationBarsPadding()) {
                NavigationBar(containerColor = scheme.surfaceVariant.copy(alpha = 0.6f), windowInsets = WindowInsets(0)) {
                    for ((tab, icon, label) in TABS) {
                        NavigationBarItem(
                                selected = ui.tab == tab,
                                onClick = {
                                    viewModel.selectTab(tab)
                                    if (tab == EditorTab.CLOCK) showClockSheet = true
                                },
                                icon = { Icon(icon, null) },
                                label = { Text(stringResource(label)) },
                                colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = scheme.onBackground,
                                        selectedTextColor = scheme.tertiary,
                                        indicatorColor = scheme.tertiary.copy(alpha = 0.28f),
                                        unselectedIconColor = scheme.secondary,
                                        unselectedTextColor = scheme.secondary,
                                ),
                                modifier = Modifier.testTag("editor_tab_${tab.name}"),
                        )
                    }
                }
            }
        }

        if (showClockSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                    onDismissRequest = { showClockSheet = false },
                    sheetState = sheetState,
                    containerColor = Color(theme.representativeBackground),
            ) {
                Text(stringResource(R.string.editor_clock_style), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
                // The real 50+ style picker applies and saves the choice itself.
                com.lu4p.fokuslauncher.clock.styles.ClockStylePickerContent(
                        onSelect = { showClockSheet = false },
                )
            }
        }
    }
}

private data class TabSpec(val tab: EditorTab, val icon: ImageVector, val label: Int)

private operator fun TabSpec.component1() = tab
private operator fun TabSpec.component2() = icon
private operator fun TabSpec.component3() = label

private val TABS =
        listOf(
                TabSpec(EditorTab.STYLE, Icons.Outlined.Style, R.string.editor_tab_style),
                TabSpec(EditorTab.CLOCK, Icons.Outlined.Schedule, R.string.editor_tab_clock),
                TabSpec(EditorTab.FONT, Icons.Outlined.TextFields, R.string.editor_tab_font),
                TabSpec(EditorTab.COLOUR, Icons.Outlined.Palette, R.string.editor_tab_colour),
                TabSpec(EditorTab.MORE, Icons.Outlined.Tune, R.string.editor_tab_more),
        )

@Composable
private fun PhoneFrame(modifier: Modifier, outline: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.clip(shape).border(3.dp, outline, shape).padding(3.dp).clip(RoundedCornerShape(27.dp))) { content() }
}

// ---- Style ---------------------------------------------------------------------------------

@Composable
private fun StylePanel(s: EditorSnapshot, custom: List<ThemeDefinition>, vm: HomeEditorViewModel) {
    val themes = remember(custom) { BuiltInThemes.gallery + custom }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PanelLabel(R.string.editor_themes)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(themes, key = { it.id }) { t ->
                ThemeThumb(t, selected = t.id == s.theme.id && s.theme.colors == t.colors) { vm.selectTheme(t) }
            }
        }
        PanelLabel(R.string.editor_backdrop)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
            item(key = "none") {
                BackdropDot(null, selected = s.theme.style.backdrop.isEmpty(), onClick = { vm.setBackdrop("") })
            }
            items(Backdrops.all, key = { it.id }) { b ->
                BackdropDot(b.id, selected = s.theme.style.backdrop == b.id, onClick = { vm.setBackdrop(b.id) })
            }
        }
        val hasBackdrop = s.theme.style.backdrop.isNotEmpty()
        Row(Modifier.fillMaxWidth().alpha(if (hasBackdrop) 1f else 0.4f), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.editor_backdrop_strength), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(112.dp))
            Slider(
                    value = s.theme.style.backdropIntensity,
                    onValueChange = vm::setBackdropIntensity,
                    onValueChangeFinished = vm::endGesture,
                    valueRange = 0.2f..1f,
                    enabled = hasBackdrop,
                    colors = accentSliderColors(),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("editor_backdrop_intensity"),
            )
        }
    }
}

@Composable
private fun ThemeThumb(theme: ThemeDefinition, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.width(56.dp).clickable(onClick = onClick).testTag("editor_theme_${theme.id}"),
            horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp, 70.dp).clip(shape).background(theme.backgroundBrush())
                .border(if (selected) 2.dp else 1.dp, if (selected) Color(theme.colors.accent) else Color(theme.colors.secondaryText).copy(alpha = 0.4f), shape)) {
            if (theme.style.backdrop.isNotEmpty()) BackdropLayer(theme.style.backdrop, intensity = theme.style.backdropIntensity)
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(24.dp, 5.dp).clip(CircleShape).background(Color(theme.colors.primaryText)))
                Box(Modifier.size(14.dp, 3.dp).clip(CircleShape).background(Color(theme.colors.accent)))
            }
        }
        Text(theme.name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun BackdropDot(id: String?, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    Box(Modifier.size(34.dp).clip(CircleShape).border(if (selected) 2.dp else 1.dp, ring, CircleShape)
            .clickable(onClick = onClick).testTag("editor_backdrop_${id ?: "none"}"),
            contentAlignment = Alignment.Center) {
        if (id != null) BackdropLayer(id) else Text("—", color = MaterialTheme.colorScheme.secondary)
    }
}

// ---- Clock ---------------------------------------------------------------------------------

@Composable
private fun ClockPanel(s: EditorSnapshot, vm: HomeEditorViewModel, openSheet: () -> Unit) {
    val face = ClockFaceRegistry.get(s.clockFaceId)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PanelLabel(R.string.editor_clock_style)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(face.labelRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            OutlinedButton(onClick = openSheet, border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.testTag("editor_clock_change")) {
                Text(stringResource(R.string.editor_clock_change), color = MaterialTheme.colorScheme.tertiary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.editor_clock_size), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(112.dp))
            Slider(
                    value = s.clockScalePct.toFloat(),
                    onValueChange = { vm.setClockScale(it.toInt()) },
                    onValueChangeFinished = vm::endGesture,
                    valueRange = 50f..200f,
                    colors = accentSliderColors(),
                    modifier = Modifier.weight(1f).testTag("editor_clock_size"),
            )
        }
    }
}

// ---- Font ----------------------------------------------------------------------------------

private data class FontOption(val value: String, val label: String?, val labelRes: Int?)

@Composable
private fun FontPanel(s: EditorSnapshot, vm: HomeEditorViewModel) {
    // null target = the app-wide font, otherwise one element.
    var target by rememberSaveable { mutableStateOf<String>("app") }
    val element = TextElement.entries.firstOrNull { it.key == target }
    val current = if (element == null) s.appFont else s.elementFonts[element].orEmpty()
    val options =
            remember(current, element) {
                buildList {
                    add(if (element == null) FontOption("", null, R.string.editor_font_default) else FontOption("", null, R.string.editor_font_same))
                    FontRegistry.bundledFonts.forEach { add(FontOption(FontRegistry.storageValueFor(it), it.label, null)) }
                    FontRegistry.systemFamilies.forEach { add(FontOption(it, it.replaceFirstChar(Char::uppercase), null)) }
                    if (current.startsWith("custom:") && none { it.value == current }) add(FontOption(current, null, R.string.editor_font_custom))
                }
            }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "app") {
                FilterChip(selected = target == "app", onClick = { target = "app" },
                        label = { Text(stringResource(R.string.editor_font_app)) })
            }
            items(TextElement.entries.toList(), key = { it.key }) { e ->
                FilterChip(selected = target == e.key, onClick = { target = e.key },
                        label = { Text(stringResource(elementLabel(e))) }, modifier = Modifier.testTag("editor_font_target_${e.key}"))
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items(options, key = { it.value }) { o ->
                val selected = o.value == current
                val family = remember(o.value) { if (o.value.isEmpty()) FontFamily.Default else vm.fontFamily(o.value) }
                Column(
                        Modifier.width(76.dp).clickable {
                            if (element == null) vm.setAppFont(o.value) else vm.setElementFont(element, o.value)
                        }.testTag("editor_font_${o.value.ifEmpty { "default" }}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val shape = RoundedCornerShape(14.dp)
                    val ring = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    Box(Modifier.size(76.dp, 64.dp).clip(shape).border(if (selected) 2.dp else 1.dp, ring, shape),
                            contentAlignment = Alignment.Center) {
                        Text("Aa", fontFamily = family, fontSize = 26.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(o.label ?: stringResource(o.labelRes!!), maxLines = 1, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private fun elementLabel(e: TextElement): Int =
        when (e) {
            TextElement.CLOCK -> R.string.editor_font_clock
            TextElement.DATE -> R.string.editor_font_date
            TextElement.APP_LABELS -> R.string.editor_font_labels
            TextElement.NOTES -> R.string.editor_font_notes
        }

// ---- Colour --------------------------------------------------------------------------------

private val ACCENTS = listOf("#FF8FC0", "#FFB067", "#FFD27A", "#9BD3A8", "#6EE7C8", "#4FB3D9", "#7FB0FF", "#C3A6FF",
        "#E5484D", "#C13566", "#17815D", "#9A4A12")
private val TEXTS = listOf("#FFFFFF", "#F2F2F2", "#FFF1E6", "#E8F6FC", "#1F1E1B", "#2B2620", "#3A1F2A", "#10303C")
private val TINTS = listOf("#204080", "#6A2C70", "#1F6F5C", "#8A4B1F", "#7A1F3A", "#303030")

@Composable
private fun ColourPanel(s: EditorSnapshot, vm: HomeEditorViewModel) {
    var hexFor by rememberSaveable { mutableStateOf<String?>(null) }
    var tintColor by rememberSaveable { mutableStateOf(ThemeColorMath.parseHex(TINTS[0])!!) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PanelLabel(R.string.editor_colour_accent)
        SwatchRow(ACCENTS, s.theme.colors.accent, "editor_accent", { vm.setAccent(it) }, onCustom = { hexFor = "accent" })
        PanelLabel(R.string.editor_colour_text)
        SwatchRow(TEXTS, s.theme.colors.primaryText, "editor_text", { vm.setTextColor(it) }, onCustom = { hexFor = "text" })
        PanelLabel(R.string.editor_colour_tint)
        SwatchRow(TINTS, if (s.tintPct > 0) s.tintArgb else 0, "editor_tint",
                { tintColor = it; vm.setTint(it, maxOf(s.tintPct, 25)); vm.endGesture() }, onCustom = null)
        Row(Modifier.fillMaxWidth().alpha(if (s.tintPct > 0) 1f else 0.7f), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.editor_colour_tint_amount), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.width(112.dp))
            Slider(
                    value = s.tintPct.toFloat(),
                    onValueChange = { vm.setTint(if (s.tintArgb != 0) s.tintArgb else tintColor, it.toInt()) },
                    onValueChangeFinished = vm::endGesture,
                    valueRange = 0f..EditorOps.MAX_TINT_PCT.toFloat(),
                    colors = accentSliderColors(),
                    modifier = Modifier.weight(1f).height(28.dp).testTag("editor_tint_amount"),
            )
        }
    }
    hexFor?.let { which ->
        HexDialog(onDismiss = { hexFor = null }) { argb ->
            if (which == "accent") vm.setAccent(argb) else vm.setTextColor(argb)
            hexFor = null
        }
    }
}

@Composable
private fun SwatchRow(hexes: List<String>, selectedArgb: Int, tag: String, onPick: (Int) -> Unit, onCustom: (() -> Unit)?) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp)) {
        items(hexes, key = { it }) { hex ->
            val argb = ThemeColorMath.parseHex(hex)!!
            val selected = (selectedArgb and 0xFFFFFF) == (argb and 0xFFFFFF) && selectedArgb != 0
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(argb))
                    .border(if (selected) 3.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
                    .clickable { onPick(argb) }.testTag("${tag}_$hex"))
        }
        if (onCustom != null) {
            item(key = "custom") {
                Box(Modifier.size(32.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                        .clickable(onClick = onCustom).testTag("${tag}_custom"), contentAlignment = Alignment.Center) {
                    Text("#", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
private fun HexDialog(onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("#") }
    val parsed = ThemeColorMath.parseHex(text.trim())
    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.editor_colour_hex)) },
            text = {
                OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(7) },
                        singleLine = true,
                        isError = parsed == null && text.length > 1,
                        placeholder = { Text(stringResource(R.string.editor_colour_hex_hint)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.testTag("editor_hex_field"),
                )
            },
            confirmButton = { TextButton(onClick = { parsed?.let(onApply) }, enabled = parsed != null) { Text(stringResource(R.string.editor_colour_use)) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// ---- More ----------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MorePanel(
        s: EditorSnapshot,
        vm: HomeEditorViewModel,
        onOpenLayoutStudio: () -> Unit,
        onOpenNoteAppearance: () -> Unit,
        onOpenWallpaper: () -> Unit,
        onOpenThemeStudio: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelLabel(R.string.editor_more_layout)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LayoutPreset.entries.toList(), key = { it.name }) { p ->
                FilterChip(
                        selected = s.layout.preset == p,
                        onClick = { if (p != LayoutPreset.CUSTOM) vm.setLayoutPreset(p) },
                        enabled = p != LayoutPreset.CUSTOM || s.layout.preset == LayoutPreset.CUSTOM,
                        label = { Text(stringResource(presetLabel(p))) },
                        modifier = Modifier.testTag("editor_preset_${p.name}"),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onOpenLayoutStudio, label = { Text(stringResource(R.string.editor_more_widgets)) })
            AssistChip(onClick = onOpenNoteAppearance, label = { Text(stringResource(R.string.editor_more_notes)) })
            AssistChip(onClick = onOpenWallpaper, label = { Text(stringResource(R.string.editor_more_wallpaper)) })
            AssistChip(onClick = onOpenThemeStudio, label = { Text(stringResource(R.string.editor_more_advanced)) })
        }
    }
}

private fun presetLabel(p: LayoutPreset): Int =
        when (p) {
            LayoutPreset.DEFAULT -> R.string.editor_preset_default
            LayoutPreset.PURE_MINIMAL -> R.string.editor_preset_minimal
            LayoutPreset.AESTHETIC -> R.string.editor_preset_aesthetic
            LayoutPreset.FOCUS -> R.string.editor_preset_focus
            LayoutPreset.INFORMATION -> R.string.editor_preset_information
            LayoutPreset.CUSTOM -> R.string.editor_preset_custom
        }

// ---- shared bits ---------------------------------------------------------------------------

@Composable
private fun PanelLabel(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun accentSliderColors() =
        SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
        )
