package com.lu4p.fokuslauncher.home.layout

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Pieces of a rendered clock; [lines] are laid out top to bottom by the composable. */
data class ClockText(val lines: List<String>, val period: String? = null) {
    val joined: String get() = lines.joinToString(" ") + (period?.let { " $it" } ?: "")
}

/** Pure clock/date formatting, independent of Android so it is unit-testable. */
object ClockFormatter {
    fun resolve24h(format: ClockHourFormat, systemIs24h: Boolean): Boolean = when (format) {
        ClockHourFormat.SYSTEM -> systemIs24h
        ClockHourFormat.H12 -> false
        ClockHourFormat.H24 -> true
    }

    fun format(time: ZonedDateTime, style: ClockStyle, is24h: Boolean, showSeconds: Boolean): ClockText {
        val hour = if (is24h) "%02d".format(time.hour)
        else (if (time.hour % 12 == 0) 12 else time.hour % 12).toString()
        val minute = "%02d".format(time.minute)
        val second = "%02d".format(time.second)
        val period = if (is24h) null else if (time.hour < 12) "AM" else "PM"
        val sec = if (showSeconds) ":$second" else ""
        return when (style) {
            ClockStyle.LARGE_DIGITAL, ClockStyle.COMPACT, ClockStyle.MINIMAL_TEXT ->
                ClockText(listOf("$hour:$minute$sec"), period)
            ClockStyle.STACKED -> ClockText(listOfNotNull(hour, minute, second.takeIf { showSeconds }), period)
            ClockStyle.SPLIT -> ClockText(listOf(hour, minute + sec), period)
        }
    }

    /** Locale-aware date, or null for [ClockDateStyle.SYSTEM] (caller keeps the existing home date). */
    fun formatDate(time: ZonedDateTime, locale: Locale, style: ClockDateStyle): String? {
        val fs = when (style) {
            ClockDateStyle.SYSTEM -> return null
            ClockDateStyle.FULL -> FormatStyle.FULL
            ClockDateStyle.LONG -> FormatStyle.LONG
            ClockDateStyle.MEDIUM -> FormatStyle.MEDIUM
            ClockDateStyle.SHORT -> FormatStyle.SHORT
        }
        return DateTimeFormatter.ofLocalizedDate(fs).withLocale(locale).format(time)
    }
}
