package com.lu4p.fokuslauncher.clock.styles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.HomeLayoutViewModel
import com.lu4p.fokuslauncher.home.layout.rememberClockFaceSpec
import com.lu4p.fokuslauncher.home.layout.rememberClockNow

/**
 * Two-column grid of live clock style previews (real time, theme colours, one shared minute
 * ticker, no animation). The selected style is highlighted; tapping applies and persists it
 * immediately, then calls [onSelect] with the style id.
 */
@Composable
fun ClockStylePickerContent(onSelect: (String) -> Unit = {}) {
    val viewModel: HomeLayoutViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.clock
    val now by rememberClockNow(false)
    val previewSettings = settings.copy(alignment = ElementAlignment.CENTER, fontSizeSp = 0, showSeconds = false)
    val spec = rememberClockFaceSpec(previewSettings, 0.4f, now, showSeconds = false)
    LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).testTag("clock_style_picker"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ClockStyleRegistry.presets, key = { it.id }) { preset ->
            val selected = preset.id == settings.styleId
            val outline = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
            val description = stringResource(R.string.clock_style_card_desc, preset.name)
            Box(
                    Modifier.fillMaxWidth().height(124.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(if (selected) 2.dp else 1.dp, outline), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.updateClock { it.copy(styleId = preset.id) }
                                onSelect(preset.id)
                            }
                            .semantics { this.selected = selected; contentDescription = description }
                            .testTag("clock_style_${preset.id}"),
                    contentAlignment = Alignment.Center,
            ) {
                ClockStyleRenderer(preset, spec)
            }
        }
    }
}
