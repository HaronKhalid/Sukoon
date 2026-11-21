package com.lu4p.fokuslauncher.home.widgets

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import java.time.temporal.WeekFields

/** Pure calendar maths (java.time only) behind the month calendar widget. */
object CalendarMath {
    fun weekStart(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    /** Weekdays in display order for a week beginning on [start]. */
    fun weekdays(start: DayOfWeek): List<DayOfWeek> = (0L..6L).map { start.plus(it) }

    /** Rows of 7 cells; leading and trailing cells outside the month are null. */
    fun grid(month: YearMonth, start: DayOfWeek): List<List<Int?>> {
        val lead = (month.atDay(1).dayOfWeek.value - start.value + 7) % 7
        val cells: List<Int?> = List(lead) { null } + (1..month.lengthOfMonth()).toList()
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        return padded.chunked(7)
    }

    /** The seven dates of the week containing [date]. */
    fun weekOf(date: LocalDate, start: DayOfWeek): List<LocalDate> {
        val back = (date.dayOfWeek.value - start.value + 7) % 7
        val first = date.minusDays(back.toLong())
        return (0L..6L).map { first.plusDays(it) }
    }
}

/** Fractions (0..1) for the day and week progress widget. */
object ProgressMath {
    private const val DAY_SECONDS = 86_400f

    fun dayFraction(time: LocalDateTime): Float = (time.toLocalTime().toSecondOfDay() / DAY_SECONDS).coerceIn(0f, 1f)

    fun weekFraction(time: LocalDateTime, start: DayOfWeek): Float {
        val dayIndex = (time.dayOfWeek.value - start.value + 7) % 7
        return ((dayIndex * DAY_SECONDS + time.toLocalTime().toSecondOfDay()) / (7 * DAY_SECONDS)).coerceIn(0f, 1f)
    }
}

enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

/** Time-of-day greeting. English text lives in string resources; Urdu is supplied here. */
object Greeting {
    fun dayPart(hour: Int): DayPart = when (((hour % 24) + 24) % 24) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..20 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

    /** Urdu greetings, written as escapes so source encoding can never corrupt them. */
    fun urdu(part: DayPart): String = when (part) {
        DayPart.MORNING -> "صبح بخیر"
        DayPart.AFTERNOON -> "دوپہر بخیر"
        DayPart.EVENING -> "شام بخیر"
        DayPart.NIGHT -> "شب بخیر"
    }

    fun english(part: DayPart): String = when (part) {
        DayPart.MORNING -> "Good morning"
        DayPart.AFTERNOON -> "Good afternoon"
        DayPart.EVENING -> "Good evening"
        DayPart.NIGHT -> "Good night"
    }

    fun isUrdu(locale: Locale) = locale.language == "ur"
}
