package com.lu4p.fokuslauncher.clock.styles

import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

enum class ClockColorMode { THEME, ACCENT, TWO_TONE, GRADIENT }

enum class ClockTextCase { AS_IS, UPPER, LOWER }

enum class ClockOutline { NONE, HOLLOW, FILLED }

/** How the digits are arranged. [FACE] hands drawing to a `clock.faces` face (analog, word clock). */
enum class ClockStyleLayout { INLINE, STACKED, VERTICAL, FACE }

/** Optional second line under the time. */
enum class ClockLineVariant { NONE, WEEKDAY, WEEKDAY_DOT, DATE, WEEKDAY_DATE }

/**
 * A curated clock look: which face/layout, font, colour treatment and effects. Pure data;
 * [ClockStyleRenderer] draws it. [id] is persisted in `ClockSettings.styleId` and never renamed.
 */
data class ClockStylePreset(
        val id: String,
        /** Design name, used for accessibility descriptions. */
        val name: String,
        val faceId: String = "classic-digital",
        val layout: ClockStyleLayout = ClockStyleLayout.INLINE,
        /** `FontRegistry` bundled id; empty means the theme/launcher font. */
        val fontId: String = "",
        val colorMode: ClockColorMode = ClockColorMode.THEME,
        val weight: Int = 600,
        val case: ClockTextCase = ClockTextCase.AS_IS,
        val italic: Boolean = false,
        val outline: ClockOutline = ClockOutline.NONE,
        val shadow: Boolean = false,
        val glow: Boolean = false,
        val letterSpacingSp: Float = 0f,
        val line: ClockLineVariant = ClockLineVariant.NONE,
        /** Relative to the face base size (1 = full size). */
        val sizeFactor: Float = 1f,
)

/** Pure text helpers so they can be unit tested without Compose. */
object ClockStyleText {
    fun applyCase(text: String, case: ClockTextCase, locale: Locale = Locale.getDefault()): String = when (case) {
        ClockTextCase.AS_IS -> text
        ClockTextCase.UPPER -> text.uppercase(locale)
        ClockTextCase.LOWER -> text.lowercase(locale)
    }

    /** Second line for [variant], or null for [ClockLineVariant.NONE]. */
    fun line(variant: ClockLineVariant, time: ZonedDateTime, locale: Locale, case: ClockTextCase): String? {
        val full = time.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val short = time.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')
        val date = "${time.dayOfMonth} ${time.month.getDisplayName(TextStyle.SHORT, locale).trimEnd('.')}"
        val raw = when (variant) {
            ClockLineVariant.NONE -> return null
            ClockLineVariant.WEEKDAY -> full
            ClockLineVariant.WEEKDAY_DOT -> "$short."
            ClockLineVariant.DATE -> date
            ClockLineVariant.WEEKDAY_DATE -> "$short, $date"
        }
        return applyCase(raw, case, locale)
    }

    /** One glyph per row, hour digits then minute digits ("1045" -> 1,0,4,5). */
    fun verticalRows(hour: String, minute: String): List<String> = (hour + minute).map { it.toString() }
}
