package com.lu4p.fokuslauncher.notes

enum class NoteAlignment { START, CENTER, END }

/** Per-note look. [coerced] clamps every field so persisted or restored values cannot break layout. */
data class NoteAppearance(
        /** Stored font value; empty follows the launcher font (see typography.FontRegistry). */
        val fontFamily: String = "",
        val fontSizeSp: Float = 16f,
        val fontWeight: Int = 400,
        /** ARGB; 0 means the theme's on-background colour. */
        val textColor: Int = 0,
        val alignment: NoteAlignment = NoteAlignment.START,
        /** Line height as a multiple of the font size. */
        val lineSpacing: Float = 1.4f,
        /** ARGB; 0 means transparent. */
        val backgroundColor: Int = 0,
        val paddingDp: Int = 8,
        /** Maximum lines shown on the home screen; the full note opens in the editor. */
        val maxLines: Int = 6,
) {
    fun coerced(): NoteAppearance =
            copy(
                    fontSizeSp = fontSizeSp.coerceIn(MIN_SIZE_SP, MAX_SIZE_SP),
                    fontWeight = (fontWeight.coerceIn(100, 900) / 100) * 100,
                    lineSpacing = lineSpacing.coerceIn(MIN_LINE_SPACING, MAX_LINE_SPACING),
                    paddingDp = paddingDp.coerceIn(0, MAX_PADDING_DP),
                    maxLines = maxLines.coerceIn(1, MAX_LINES),
            )

    companion object {
        val DEFAULT = NoteAppearance()
        const val MIN_SIZE_SP = 10f
        const val MAX_SIZE_SP = 40f
        const val MIN_LINE_SPACING = 1.0f
        const val MAX_LINE_SPACING = 2.4f
        const val MAX_PADDING_DP = 48
        const val MAX_LINES = 30
    }
}

fun NoteEntity.appearance(): NoteAppearance =
        NoteAppearance(
                fontFamily = fontFamily,
                fontSizeSp = fontSizeSp,
                fontWeight = fontWeight,
                textColor = textColor,
                alignment = NoteAlignment.entries.getOrElse(alignment) { NoteAlignment.START },
                lineSpacing = lineSpacing,
                backgroundColor = backgroundColor,
                paddingDp = paddingDp,
                maxLines = maxLines,
        ).coerced()

fun NoteEntity.withAppearance(appearance: NoteAppearance): NoteEntity {
    val a = appearance.coerced()
    return copy(
            fontFamily = a.fontFamily,
            fontSizeSp = a.fontSizeSp,
            fontWeight = a.fontWeight,
            textColor = a.textColor,
            alignment = a.alignment.ordinal,
            lineSpacing = a.lineSpacing,
            backgroundColor = a.backgroundColor,
            paddingDp = a.paddingDp,
            maxLines = a.maxLines,
    )
}
