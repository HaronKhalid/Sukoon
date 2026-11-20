package com.lu4p.fokuslauncher.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * Builds the Compose style for a note. Direction follows the content of each paragraph
 * ([TextDirection.Content]) so Urdu and English notes both align naturally, and
 * [NoteAlignment.START]/[NoteAlignment.END] are logical (they flip with the text direction).
 * Line height is a multiple of the font size with no trimming, so tall Nastaliq glyphs are not
 * clipped.
 */
fun noteTextStyle(
        base: TextStyle,
        appearance: NoteAppearance,
        fontFamily: FontFamily,
        defaultColor: Color,
): TextStyle {
    val a = appearance.coerced()
    return base.copy(
            fontFamily = fontFamily,
            fontSize = a.fontSizeSp.sp,
            fontWeight = FontWeight(a.fontWeight),
            color = if (a.textColor == 0) defaultColor else Color(a.textColor),
            lineHeight = (a.fontSizeSp * a.lineSpacing).sp,
            textAlign =
                    when (a.alignment) {
                        NoteAlignment.START -> TextAlign.Start
                        NoteAlignment.CENTER -> TextAlign.Center
                        NoteAlignment.END -> TextAlign.End
                    },
            textDirection = TextDirection.Content,
            lineHeightStyle =
                    LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                    ),
    )
}
