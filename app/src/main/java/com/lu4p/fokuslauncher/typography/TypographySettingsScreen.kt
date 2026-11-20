package com.lu4p.fokuslauncher.typography

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.font.CustomFontImportResult
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences
import com.lu4p.fokuslauncher.ui.settings.FokusSettingsTopBar
import com.lu4p.fokuslauncher.ui.theme.FokusBackdrop
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TypographyViewModel
@Inject
constructor(
        private val preferences: TypographyPreferences,
        private val appPreferences: PreferencesManager,
        private val customFonts: CustomFontStore,
        val fontCache: FontFamilyCache,
) : ViewModel() {
    val fonts: StateFlow<Map<TextElement, String>> =
            preferences.allFontsFlow.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    TextElement.entries.associateWith { "" },
            )

    val appFont: StateFlow<String> =
            appPreferences.launcherFontFamilyFlow.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    "",
            )

    val boostMode: StateFlow<LineBoostMode> =
            preferences.lineBoostModeFlow.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    LineBoostMode.AUTO,
            )

    /** Imported font (storage value to family name), or null. Refreshed after import/remove. */
    var imported: Pair<String, String>? by mutableStateOf(readImported())
        private set

    var message: Int? by mutableStateOf(null)
        private set

    var messageArg: String = ""
        private set

    private fun readImported(): Pair<String, String>? {
        val label = customFonts.readStoredFontDisplayLabel() ?: return null
        val file = customFonts.activeFileName() ?: return null
        return (LauncherFontPreferences.CUSTOM_FONT_PREFIX + file) to label
    }

    fun select(element: TextElement, storageValue: String) {
        viewModelScope.launch { preferences.setFont(element, storageValue) }
    }

    fun selectAppFont(storageValue: String) {
        viewModelScope.launch { appPreferences.setLauncherFontFamilyName(storageValue) }
    }

    fun setBoost(mode: LineBoostMode) {
        viewModelScope.launch { preferences.setLineBoostMode(mode) }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            when (val r = customFonts.importFromUri(uri)) {
                is CustomFontImportResult.Success -> {
                    fontCache.invalidateCustomFonts()
                    imported = r.storageValue to r.displayLabel
                    appPreferences.setLauncherFontFamilyName(r.storageValue)
                    messageArg = r.displayLabel
                    message = R.string.fonts_import_ok
                }
                is CustomFontImportResult.Failure -> message = R.string.fonts_import_failed
            }
        }
    }

    fun removeImported() {
        viewModelScope.launch {
            customFonts.deleteStoredFont()
            fontCache.invalidateCustomFonts()
            imported = null
            if (LauncherFontPreferences.isCustomFont(appFont.value)) {
                appPreferences.setLauncherFontFamilyName("")
            }
            TextElement.entries.forEach {
                if (LauncherFontPreferences.isCustomFont(fonts.value[it])) preferences.setFont(it, "")
            }
            messageArg = ""
            message = R.string.fonts_removed
        }
    }

    fun consumeMessage() {
        message = null
    }
}

/** Per-element font families for the home UI; defaults to the launcher font everywhere. */
val LocalElementFonts = compositionLocalOf<Map<TextElement, FontFamily>> { emptyMap() }

@Composable
fun TypographySettingsScreen(
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier,
        viewModel: TypographyViewModel = hiltViewModel(),
        backgroundScrim: Color = FokusBackdrop.ScrimColorWithoutBlur,
) {
    val selected by viewModel.fonts.collectAsStateWithLifecycle()
    val appFont by viewModel.appFont.collectAsStateWithLifecycle()
    val boostMode by viewModel.boostMode.collectAsStateWithLifecycle()
    val imported = viewModel.imported
    val context = LocalContext.current
    val picker =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) viewModel.import(uri)
            }
    viewModel.message?.let { res ->
        val text = stringResource(res, viewModel.messageArg)
        LaunchedEffect(res, text) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }
    val options = fontOptions(imported)
    val appFamily = remember(appFont, imported) { viewModel.fontCache.familyFor(appFont) }
    val boost = FontPolicy.lineHeightBoost(appFont, imported?.second, boostMode)
    Column(
            modifier.fillMaxSize()
                    .background(backgroundScrim)
                    .navigationBarsPadding()
                    .testTag("typography_screen")
    ) {
        FokusSettingsTopBar(
                titleText = stringResource(R.string.typography_settings_title),
                onNavigateBack = onNavigateBack,
                containerColor = MaterialTheme.colorScheme.background,
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                    stringResource(R.string.fonts_honest_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Heading(R.string.fonts_preview_heading)
            Column(Modifier.padding(horizontal = 24.dp)) {
                listOf(R.string.fonts_preview_sample, R.string.fonts_preview_sentence).forEach {
                    Text(
                            stringResource(it),
                            style = previewStyle(appFamily, boost),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            Heading(R.string.fonts_app_font_heading)
            options.forEach { (value, label) ->
                val family = remember(value, imported) { viewModel.fontCache.familyFor(value) }
                val isCurrent = value == appFont
                Column(
                        Modifier.fillMaxWidth()
                                .clickable { viewModel.selectAppFont(value) }
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                            stringResource(R.string.fonts_preview_sample),
                            style =
                                    previewStyle(
                                            family,
                                            FontPolicy.lineHeightBoost(value, imported?.second, boostMode),
                                    ),
                            color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            TextButton(
                    onClick = {
                        picker.launch(
                                arrayOf(
                                        "font/ttf",
                                        "font/otf",
                                        "font/sfnt",
                                        "application/x-font-ttf",
                                        "application/x-font-otf",
                                        "application/octet-stream",
                                )
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(stringResource(R.string.fonts_import_button))
            }
            Text(
                    stringResource(R.string.fonts_import_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (imported != null) {
                Text(
                        stringResource(R.string.fonts_imported_label, imported.second),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                TextButton(
                        onClick = viewModel::removeImported,
                        modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.fonts_remove_imported))
                }
            }
            Heading(R.string.fonts_line_boost_heading)
            Row(Modifier.padding(horizontal = 16.dp)) {
                listOf(
                                LineBoostMode.AUTO to R.string.fonts_line_boost_auto,
                                LineBoostMode.ON to R.string.fonts_line_boost_on,
                                LineBoostMode.OFF to R.string.fonts_line_boost_off,
                        )
                        .forEach { (mode, label) ->
                            TextButton(onClick = { viewModel.setBoost(mode) }) {
                                Text(
                                        stringResource(label),
                                        color =
                                                if (mode == boostMode) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
            }
            Text(
                    stringResource(R.string.fonts_line_boost_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp),
            )
            Heading(R.string.fonts_elements_heading)
            TextElement.entries.forEach { element ->
                Text(
                        stringResource(elementLabel(element)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp),
                )
                val current = selected[element].orEmpty()
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    fontOptions(imported, includeApp = true).forEach { (value, label) ->
                        TextButton(onClick = { viewModel.select(element, value) }) {
                            Text(
                                    label,
                                    color =
                                            if (value == current) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
            Text(
                    stringResource(R.string.typography_licenses_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                    stringResource(R.string.fonts_licence_import_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun Heading(res: Int) {
    Text(
            stringResource(res),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun previewStyle(family: FontFamily, boost: Float): TextStyle {
    val base = MaterialTheme.typography.bodyLarge
    return base.copy(
            fontFamily = family,
            textDirection = TextDirection.Content,
            lineHeight = base.fontSize * 1.4f * boost,
            lineHeightStyle =
                    LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
}

private fun elementLabel(element: TextElement): Int =
        when (element) {
            TextElement.CLOCK -> R.string.typography_element_clock
            TextElement.DATE -> R.string.typography_element_date
            TextElement.APP_LABELS -> R.string.typography_element_app_labels
            TextElement.NOTES -> R.string.typography_element_notes
        }

/** Font choices as (storage value, label). The imported font is listed when present. */
@Composable
fun fontOptions(
        imported: Pair<String, String>? = null,
        includeApp: Boolean = false,
): List<Pair<String, String>> {
    val first =
            stringResource(
                    if (includeApp) R.string.typography_follow_launcher else R.string.fonts_default_font
            )
    val sans = stringResource(R.string.typography_system_sans)
    val serif = stringResource(R.string.typography_system_serif)
    val mono = stringResource(R.string.typography_system_monospace)
    val cursive = stringResource(R.string.typography_system_cursive)
    return buildList {
        add("" to first)
        FontRegistry.bundledFonts.forEach { add(FontRegistry.storageValueFor(it) to it.label) }
        imported?.let { add(it.first to it.second) }
        add("sans-serif" to sans)
        add("serif" to serif)
        add("monospace" to mono)
        add("cursive" to cursive)
    }
}

/** Applies the user's font for [element] on top of this style; no-op when following the launcher font. */
@Composable
fun TextStyle.withElementFont(element: TextElement): TextStyle {
    val family = LocalElementFonts.current[element] ?: return this
    return copy(fontFamily = family)
}

/** Resolves every element's font once per change and exposes it through [LocalElementFonts]. */
@Composable
fun ProvideElementFonts(
        viewModel: TypographyViewModel = hiltViewModel(),
        content: @Composable () -> Unit,
) {
    val selected by viewModel.fonts.collectAsStateWithLifecycle()
    val launcherFamily = MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.Default
    val families =
            remember(selected, launcherFamily) {
                selected
                        .mapNotNull { (element, value) ->
                            if (FontRegistry.resolve(value) is FontChoice.FollowLauncher) null
                            else element to viewModel.fontCache.familyFor(value, launcherFamily)
                        }
                        .toMap()
            }
    CompositionLocalProvider(LocalElementFonts provides families, content = content)
}
