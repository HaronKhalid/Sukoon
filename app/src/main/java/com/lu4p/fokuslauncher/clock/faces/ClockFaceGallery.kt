package com.lu4p.fokuslauncher.clock.faces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.ElementAlignment
import com.lu4p.fokuslauncher.home.layout.rememberClockFaceSpec
import com.lu4p.fokuslauncher.home.layout.rememberClockNow

/**
 * Horizontal gallery of every face drawn live with the user's current settings (colour, accent,
 * font, 12/24h). Tiles share one minute ticker.
 */
@Composable
fun ClockFaceGallery(
        settings: ClockSettings,
        selectedId: String,
        onSelect: (String) -> Unit,
        modifier: Modifier = Modifier,
) {
    val now by rememberClockNow(false)
    val tileSettings = settings.copy(alignment = ElementAlignment.CENTER)
    val spec = rememberClockFaceSpec(tileSettings, 0.42f, now, showSeconds = false)
    LazyRow(
            modifier.fillMaxWidth().testTag("clock_face_gallery"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    ) {
        items(ClockFaceRegistry.faces, key = { it.id }) { face ->
            val selected = face.id == selectedId
            val label = stringResource(face.labelRes)
            val outline = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            Column(
                    Modifier.width(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(if (selected) 2.dp else 1.dp, outline), RoundedCornerShape(16.dp))
                            .clickable { onSelect(face.id) }
                            .semantics { this.selected = selected; contentDescription = label }
                            .testTag("clock_face_${face.id}")
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
