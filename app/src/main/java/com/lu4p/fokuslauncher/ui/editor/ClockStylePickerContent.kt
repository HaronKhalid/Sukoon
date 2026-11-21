package com.lu4p.fokuslauncher.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.rememberClockFaceSpec
import com.lu4p.fokuslauncher.home.layout.rememberClockNow

/**
 * Integration point for the clock-style picker shown in the Home Editor's Clock bottom sheet.
 * The editor calls exactly this function with [onSelect] receiving the chosen clock style id.
 *
 * The fallback below lists the faces of the `clock.faces` registry. When the 40-style grid
 * (package `clock.styles`) lands, replace the body with a call into it (keeping this signature);
 * the editor stores whatever id comes back in `ClockSettings.faceId`.
 */
@Composable
fun ClockStylePickerContent(
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier,
        selectedId: String = "",
) {
    val now by rememberClockNow(false)
    val spec = rememberClockFaceSpec(ClockSettings(alignment = ElementAlignment.CENTER), 0.42f, now, showSeconds = false)
    LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxWidth().testTag("clock_style_picker"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(ClockFaceRegistry.faces, key = { it.id }) { face ->
            val isSelected = face.id == selectedId
            val label = stringResource(face.labelRes)
            val outline =
                    if (isSelected) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(if (isSelected) 2.dp else 1.dp, outline), RoundedCornerShape(16.dp))
                            .clickable { onSelect(face.id) }
                            .semantics { this.selected = isSelected; contentDescription = label }
                            .testTag("clock_style_${face.id}")
                            .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().height(86.dp), contentAlignment = Alignment.Center) {
                    face.Render(spec, Modifier)
                }
                Text(label, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
