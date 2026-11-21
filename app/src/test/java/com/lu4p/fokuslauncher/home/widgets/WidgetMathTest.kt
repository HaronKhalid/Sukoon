package com.lu4p.fokuslauncher.home.widgets

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMathTest {
    private fun days(rows: List<List<Int?>>) = rows.flatten().filterNotNull()

    @Test
    fun leapYearFebruary() {
        val leap = CalendarMath.grid(YearMonth.of(2024, 2), DayOfWeek.MONDAY)
        assertEquals((1..29).toList(), days(leap))
        // 1 Feb 2024 is a Thursday: three leading blanks, the 29th lands on a Thursday in row 5.
        assertEquals(listOf(null, null, null, 1), leap[0].take(4))
        assertEquals(5, leap.size)
        assertEquals(29, leap[4][3])
        assertTrue(leap.all { it.size == 7 })
        assertEquals((1..28).toList(), days(CalendarMath.grid(YearMonth.of(2023, 2), DayOfWeek.MONDAY)))
        assertEquals((1..29).toList(), days(CalendarMath.grid(YearMonth.of(2000, 2), DayOfWeek.SUNDAY)))
        assertEquals((1..28).toList(), days(CalendarMath.grid(YearMonth.of(1900, 2), DayOfWeek.SUNDAY)))
    }

    @Test
    fun weekStartChangesRowCount() {
        // 1 Feb 2026 is a Sunday: four rows with a Sunday start, five with a Monday start.
        assertEquals(4, CalendarMath.grid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY).size)
        val monday = CalendarMath.grid(YearMonth.of(2026, 2), DayOfWeek.MONDAY)
        assertEquals(5, monday.size)
        assertEquals(List(6) { null }, monday[0].take(6))
        assertEquals(1, monday[0][6])
        assertNull(monday[4].last())
    }

    @Test
    fun weekStartFollowsLocale() {
        assertEquals(DayOfWeek.SUNDAY, CalendarMath.weekStart(Locale.US))
        assertEquals(DayOfWeek.MONDAY, CalendarMath.weekStart(Locale.GERMANY))
        assertEquals(DayOfWeek.MONDAY, CalendarMath.weekStart(Locale.FRANCE))
        assertEquals(DayOfWeek.WEDNESDAY, CalendarMath.weekdays(DayOfWeek.MONDAY)[2])
        assertEquals(DayOfWeek.SUNDAY, CalendarMath.weekdays(DayOfWeek.MONDAY)[6])
    }

    @Test
    fun weekOfContainsDate() {
        val week = CalendarMath.weekOf(LocalDate.of(2026, 11, 21), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 11, 16), week.first())
        assertEquals(LocalDate.of(2026, 11, 22), week.last())
        assertTrue(LocalDate.of(2026, 11, 21) in week)
        assertEquals(LocalDate.of(2026, 11, 15), CalendarMath.weekOf(LocalDate.of(2026, 11, 21), DayOfWeek.SUNDAY).first())
    }

    @Test
    fun progressFractions() {
        assertEquals(0f, ProgressMath.dayFraction(LocalDateTime.of(2026, 11, 21, 0, 0)))
        assertEquals(0.5f, ProgressMath.dayFraction(LocalDateTime.of(2026, 11, 21, 12, 0)))
        assertEquals(0f, ProgressMath.weekFraction(LocalDateTime.of(2026, 11, 16, 0, 0), DayOfWeek.MONDAY))
        assertEquals(1f / 7f, ProgressMath.weekFraction(LocalDateTime.of(2026, 11, 17, 0, 0), DayOfWeek.MONDAY), 1e-6f)
        assertTrue(ProgressMath.weekFraction(LocalDateTime.of(2026, 11, 22, 23, 59, 59), DayOfWeek.MONDAY) > 0.99f)
    }

    @Test
    fun greetingParts() {
        assertEquals(DayPart.MORNING, Greeting.dayPart(5))
        assertEquals(DayPart.AFTERNOON, Greeting.dayPart(12))
        assertEquals(DayPart.EVENING, Greeting.dayPart(17))
        assertEquals(DayPart.NIGHT, Greeting.dayPart(21))
        assertEquals(DayPart.NIGHT, Greeting.dayPart(4))
        assertEquals("Good morning", Greeting.english(DayPart.MORNING))
        assertTrue(Greeting.isUrdu(Locale("ur", "PK")))
        assertTrue(Greeting.urdu(DayPart.MORNING).all { it == ' ' || it.code in 0x0600..0x06FF })
    }
}
