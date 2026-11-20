package com.lu4p.fokuslauncher.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lu4p.fokuslauncher.R
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
        val fontCache: FontFamilyCache,
) : ViewModel() {
    val fonts: StateFlow<Map<TextElement, String>> =
            preferences.allFontsFlow.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    TextElement.entries.associateWith { "" },
            )

    fun select(element: TextElement, storageValue: String) {
        viewModelScope.launch { preferences.setFont(element, storageValue) }
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
    val launcherFamily = MaterialTheme.typography.bodyLarge.fontFamily ?: FontFamily.Default
    val options = fontOptions()
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
                    stringResource(R.string.typography_scope_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            TextElement.entries.forEach { element ->
                Text(
                        stringResource(elementLabel(element)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                val current = selected[element].orEmpty()
                options.forEach { (value, label) ->
                    val family =
                            remember(value, launcherFamily) {
                                viewModel.fontCache.familyFor(value, launcherFamily)
                            }
                    val isCurrent = value == current
                    Column(
                            Modifier.fillMaxWidth()
                                    .clickable { viewModel.select(element, value) }
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                                stringResource(R.string.typography_preview_sample),
                                style =
                                        MaterialTheme.typography.bodyLarge.copy(
                                                fontFamily = family,
                                                textDirection = TextDirection.Content,
                                                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.8f,
                                        ),
                                color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
            Text(
                    stringResource(R.string.typography_licenses_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(24.dp),
            )
        }
    }
}

private fun elementLabel(element: TextElement): Int =
        when (element) {
            TextElement.CLOCK -> R.string.typography_element_clock
            TextElement.DATE -> R.string.typography_element_date
            TextElement.APP_LABELS -> R.string.typography_element_app_labels
            TextElement.NOTES -> R.string.typography_element_notes
        }

@Composable
private fun fontOptions(): List<Pair<String, String>> =
        buildList {
            add("" to stringResource(R.string.typography_follow_launcher))
            FontRegistry.bundledFonts.forEach { add(FontRegistry.storageValueFor(it) to it.label) }
            add("sans-serif" to stringResource(R.string.typography_system_sans))
            add("serif" to stringResource(R.string.typography_system_serif))
            add("monospace" to stringResource(R.string.typography_system_monospace))
            add("cursive" to stringResource(R.string.typography_system_cursive))
        }

/** Applies the user's font for [element] on top of this style; no-op when following the launcher font. */
@Composable
fun androidx.compose.ui.text.TextStyle.withElementFont(element: TextElement): androidx.compose.ui.text.TextStyle {
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
                selected.mapNotNull { (element, value) ->
                    if (FontRegistry.resolve(value) is FontChoice.FollowLauncher) null
                    else element to viewModel.fontCache.familyFor(value, launcherFamily)
                }.toMap()
            }
    androidx.compose.runtime.CompositionLocalProvider(LocalElementFonts provides families, content = content)
}
