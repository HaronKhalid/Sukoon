package com.lu4p.fokuslauncher.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound

/**
 * Renders note [text] as a card using [appearance] (text look) and [style] (card options). Used
 * by the home screen and, identically, by the live preview in the appearance screen.
 * [decoration] is the optional ornament of a [NoteStyle] (ruled lines, quote mark, ...).
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
        decoration: NoteDecoration = NoteDecoration.NONE,
        stamp: String? = null,
) {
    val a = appearance.coerced()
    val s = style.coerced()
    val resolved = NoteCard.resolve(s, a)
    val defaultColor =
            resolved.defaultTextArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onBackground
    var textStyle = noteTextStyle(MaterialTheme.typography.bodyLarge, a, family, defaultColor)
    if (decoration == NoteDecoration.GLOW) {
        textStyle =
                textStyle.copy(shadow = Shadow(textStyle.color.copy(alpha = 0.9f), Offset.Zero, 18f))
    }
    val shape = RoundedCornerShape(s.cornerRadiusDp.dp)
    val borderColor =
            if (s.borderColor == 0) MaterialTheme.colorScheme.secondary else Color(s.borderColor)
    val boxAlignment =
            when (a.alignment) {
                NoteAlignment.START -> Alignment.TopStart
                NoteAlignment.CENTER -> Alignment.TopCenter
                NoteAlignment.END -> Alignment.TopEnd
            }
    val density = LocalDensity.current
    val shown = NoteStyles.decorateText(decoration, text, stamp)
    var card =
            Modifier.fillMaxWidth(s.widthFraction)
                    .heightIn(min = (40f * s.heightScale).dp)
    if (resolved.elevated) card = card.shadow(4.dp, shape)
    card = card.clip(shape)
    if (resolved.backgroundArgb != 0) card = card.background(Color(resolved.backgroundArgb))
    if (decoration == NoteDecoration.RULED) {
        val lh = with(density) { (a.fontSizeSp * a.lineSpacing * lineBoost(a)).sp.toPx() }
        val top = with(density) { a.paddingDp.dp.toPx() }
        val lineColor = textStyle.color.copy(alpha = 0.18f)
        card =
                card.drawBehind {
                    var y = top + lh * 0.95f
                    while (y < size.height) {
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.5f)
                        y += lh
                    }
                }
    }
    if (resolved.showBorder) card = card.border(1.5.dp, borderColor, shape)
    if (onClick != null || onLongClick != null) {
        card =
                card.combinedClickableWithSystemSound(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                )
    }
    Box(modifier.fillMaxWidth(), contentAlignment = boxAlignment) {
        Column(card.padding(a.paddingDp.dp)) {
            if (decoration == NoteDecoration.QUOTE_MARK) {
                Text(
                        text = "“",
                        style =
                                textStyle.copy(
                                        fontSize = (a.fontSizeSp * 3f).sp,
                                        lineHeight = (a.fontSizeSp * 2.4f).sp,
                                        color = textStyle.color.copy(alpha = 0.45f),
                                ),
                        modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                    text = shown,
                    style = textStyle,
                    maxLines = a.maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
