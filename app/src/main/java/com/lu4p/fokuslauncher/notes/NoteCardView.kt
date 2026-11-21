package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound

/**
 * Renders note [text] as a card using [appearance] (text look) and [style] (card options). Used
 * by the home screen and, identically, by the live preview in the appearance screen.
 */
@Composable
fun NoteCardView(
        text: String,
        appearance: NoteAppearance,
        style: NoteWidgetStyle,
        family: FontFamily,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
) {
    val a = appearance.coerced()
    val s = style.coerced()
    val resolved = NoteCard.resolve(s, a)
    val defaultColor =
            resolved.defaultTextArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onBackground
    val textStyle = noteTextStyle(MaterialTheme.typography.bodyLarge, a, family, defaultColor)
    val shape = RoundedCornerShape(s.cornerRadiusDp.dp)
    val borderColor =
            if (s.borderColor == 0) MaterialTheme.colorScheme.secondary else Color(s.borderColor)
    val boxAlignment =
            when (a.alignment) {
                NoteAlignment.START -> Alignment.TopStart
                NoteAlignment.CENTER -> Alignment.TopCenter
                NoteAlignment.END -> Alignment.TopEnd
            }
    var card =
            Modifier.fillMaxWidth(s.widthFraction)
                    .heightIn(min = (40f * s.heightScale).dp)
    if (resolved.elevated) card = card.shadow(4.dp, shape)
    card = card.clip(shape)
    if (resolved.backgroundArgb != 0) card = card.background(Color(resolved.backgroundArgb))
    if (resolved.showBorder) card = card.border(1.5.dp, borderColor, shape)
    if (onClick != null || onLongClick != null) {
        card =
                card.combinedClickableWithSystemSound(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                )
    }
    Box(modifier.fillMaxWidth(), contentAlignment = boxAlignment) {
        Text(
                text = text,
                style = textStyle,
                maxLines = a.maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = card.padding(a.paddingDp.dp),
        )
    }
}
