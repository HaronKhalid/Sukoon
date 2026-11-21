package com.lu4p.fokuslauncher.reminders

import com.lu4p.fokuslauncher.reminders.ReminderMath.GroupKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMathTest {
    private val utc = ZoneId.of("UTC")
    private val ny = ZoneId.of("America/New_York")

    private fun ms(z: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
            LocalDateTime.of(y, mo, d, h, mi).atZone(z).toInstant().toEpochMilli()

    private fun rem(id: String = "a", due: Long, z: ZoneId = utc, repeat: RepeatRule = RepeatRule.NONE,
                    done: Boolean = false, notify: Boolean = true) =
            Reminder(id, "t$id", dueEpochMs = due, zoneId = z.id, repeat = repeat, done = done, notify = notify)

    // ---- recurrence ----

    @Test fun noneHasNoNextOccurrence() {
        assertNull(ReminderMath.nextOccurrence(RepeatRule.NONE, 0, utc, 0))
    }

    @Test fun dailyNextIsTomorrowSameTime() {
        val due = ms(utc, 2026, 5, 10, 9)
        assertEquals(ms(utc, 2026, 5, 11, 9), ReminderMath.nextOccurrence(RepeatRule.DAILY, due, utc, due))
    }

    @Test fun dailySkipsToFutureWhenFarBehind() {
        val due = ms(utc, 2026, 1, 1, 9)
        val now = ms(utc, 2026, 6, 15, 12)
        assertEquals(ms(utc, 2026, 6, 16, 9), ReminderMath.nextOccurrence(RepeatRule.DAILY, due, utc, now))
    }

    @Test fun dailyLaterTodayWhenNowBeforeTime() {
        val due = ms(utc, 2026, 1, 1, 9)
        val now = ms(utc, 2026, 6, 15, 8)
        assertEquals(ms(utc, 2026, 6, 15, 9), ReminderMath.nextOccurrence(RepeatRule.DAILY, due, utc, now))
    }

    @Test fun weekdaysSkipWeekend() {
        val friday = ms(utc, 2026, 5, 8, 9) // 2026-05-08 is a Friday
        assertEquals(DayOfWeek.FRIDAY, LocalDate.of(2026, 5, 8).dayOfWeek)
        assertEquals(ms(utc, 2026, 5, 11, 9), ReminderMath.nextOccurrence(RepeatRule.WEEKDAYS, friday, utc, friday))
    }

    @Test fun weeklyAddsSevenDays() {
        val due = ms(utc, 2026, 5, 10, 9)
        assertEquals(ms(utc, 2026, 5, 17, 9), ReminderMath.nextOccurrence(RepeatRule.WEEKLY, due, utc, due))
        val now = ms(utc, 2026, 8, 1, 0)
        val next = ReminderMath.nextOccurrence(RepeatRule.WEEKLY, due, utc, now)!!
        assertTrue(next > now)
        assertEquals(DayOfWeek.SUNDAY, java.time.Instant.ofEpochMilli(next).atZone(utc).dayOfWeek)
    }

    @Test fun monthlyKeepsDayAndClampsToMonthEnd() {
        val jan31 = ms(utc, 2027, 1, 31, 9)
        val feb = ReminderMath.nextOccurrence(RepeatRule.MONTHLY, jan31, utc, jan31)!!
        assertEquals(ms(utc, 2027, 2, 28, 9), feb)
        // After February the day returns to the 31st (no drift).
        assertEquals(ms(utc, 2027, 3, 31, 9), ReminderMath.nextOccurrence(RepeatRule.MONTHLY, jan31, utc, feb))
    }

    @Test fun monthlyLeapYear() {
        val jan31 = ms(utc, 2028, 1, 31, 9)
        assertEquals(ms(utc, 2028, 2, 29, 9), ReminderMath.nextOccurrence(RepeatRule.MONTHLY, jan31, utc, jan31))
    }

    @Test fun dailyKeepsWallClockAcrossSpringForward() {
        // New York DST starts 2026-03-08 at 02:00. A 09:00 reminder stays 09:00 local.
        val due = ms(ny, 2026, 3, 7, 9)
        val next = ReminderMath.nextOccurrence(RepeatRule.DAILY, due, ny, due)!!
        assertEquals(ms(ny, 2026, 3, 8, 9), next)
        assertEquals(23 * 3_600_000L, next - due) // the day is 23h long
    }

    @Test fun dailyKeepsWallClockAcrossFallBack() {
        val due = ms(ny, 2026, 10, 31, 9)
        val next = ReminderMath.nextOccurrence(RepeatRule.DAILY, due, ny, due)!!
        assertEquals(ms(ny, 2026, 11, 1, 9), next)
        assertEquals(25 * 3_600_000L, next - due)
    }

    @Test fun reminderInSpringForwardGapMovesForward() {
        val due = ms(ny, 2026, 3, 7, 2, 30)
        val next = ReminderMath.nextOccurrence(RepeatRule.DAILY, due, ny, due)!!
        val local = java.time.Instant.ofEpochMilli(next).atZone(ny).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 3, 8, 3, 30), local)
    }

    @Test fun completeOneOffMarksDone() {
        val r = ReminderMath.complete(rem(due = 1000), 2000)
        assertTrue(r.done)
    }

    @Test fun completeRepeatingRollsForwardAndStaysActive() {
        val due = ms(utc, 2026, 5, 10, 9)
        val r = ReminderMath.complete(rem(due = due, repeat = RepeatRule.DAILY), due + 60_000)
        assertFalse(r.done)
        assertEquals(ms(utc, 2026, 5, 11, 9), r.dueEpochMs)
    }

    // ---- grouping ----

    @Test fun dayKeys() {
        val now = ms(utc, 2026, 5, 10, 12) // Sunday
        assertEquals(GroupKey.Today, ReminderMath.dayKey(ms(utc, 2026, 5, 10, 23), now, utc))
        assertEquals(GroupKey.Tomorrow, ReminderMath.dayKey(ms(utc, 2026, 5, 11, 1), now, utc))
        assertEquals(GroupKey.Weekday(DayOfWeek.WEDNESDAY), ReminderMath.dayKey(ms(utc, 2026, 5, 13, 1), now, utc))
        assertEquals(GroupKey.Date(LocalDate.of(2026, 5, 17)), ReminderMath.dayKey(ms(utc, 2026, 5, 17, 1), now, utc))
    }

    @Test fun groupingOrdersOverdueDaysDone() {
        val now = ms(utc, 2026, 5, 10, 12)
        val list = listOf(
                rem("late", ms(utc, 2026, 5, 10, 8)),
                rem("tm", ms(utc, 2026, 5, 11, 9)),
                rem("td", ms(utc, 2026, 5, 10, 15)),
                rem("old", ms(utc, 2026, 5, 9, 9), done = true),
        )
        val g = ReminderMath.group(list, now, utc)
        assertEquals(listOf(GroupKey.Overdue, GroupKey.Today, GroupKey.Tomorrow, GroupKey.Done), g.map { it.key })
        assertEquals("td", g[1].items.single().id)
    }

    // ---- quick add ----

    @Test fun inOneHourRoundsDownToMinute() {
        val now = ms(utc, 2026, 5, 10, 12, 0) + 31_000
        assertEquals(ms(utc, 2026, 5, 10, 13, 0), ReminderMath.inOneHour(now))
    }

    @Test fun thisEveningChoices() {
        assertEquals(ms(utc, 2026, 5, 10, 18), ReminderMath.thisEvening(ms(utc, 2026, 5, 10, 9), utc))
        assertEquals(ms(utc, 2026, 5, 10, 21), ReminderMath.thisEvening(ms(utc, 2026, 5, 10, 19), utc))
        assertEquals(ms(utc, 2026, 5, 11, 18), ReminderMath.thisEvening(ms(utc, 2026, 5, 10, 22), utc))
    }

    @Test fun tomorrowNineAcrossMonthEnd() {
        assertEquals(ms(utc, 2026, 6, 1, 9), ReminderMath.tomorrowAt(ms(utc, 2026, 5, 31, 23), utc))
        assertEquals(ms(ny, 2026, 3, 8, 9, 30), ReminderMath.tomorrowAt(ms(ny, 2026, 3, 7, 20), ny, 9, 30))
    }

    // ---- cap ----

    @Test fun capDropsDoneFirstThenOldest() {
        val list = (0 until 205).map { rem("r$it", 1000L + it, done = it < 3) }
        val capped = ReminderMath.cap(list)
        assertEquals(200, capped.size)
        assertTrue(capped.none { it.done })
        assertTrue(capped.none { it.id == "r3" || it.id == "r4" })
        assertTrue(capped.any { it.id == "r5" })
    }

    // ---- agenda ----

    @Test fun agendaExcludesDoneTakesNextNAndOneOverdue() {
        val now = ms(utc, 2026, 5, 10, 12)
        val list = listOf(
                rem("o1", ms(utc, 2026, 5, 9, 9)),
                rem("o2", ms(utc, 2026, 5, 10, 8)),
                rem("d", ms(utc, 2026, 5, 10, 13), done = true),
                rem("n1", ms(utc, 2026, 5, 10, 14)),
                rem("n2", ms(utc, 2026, 5, 11, 9)),
                rem("n3", ms(utc, 2026, 5, 12, 9)),
        )
        val cal = listOf(ReminderMath.AgendaItem("c", "Event", ms(utc, 2026, 5, 10, 15), fromCalendar = true))
        val a = ReminderMath.selectAgenda(list, cal, now, 3)
        assertEquals(listOf("o2", "n1", "c"), a.map { it.id })
        assertTrue(a.first().overdue)
        assertTrue(ReminderMath.selectAgenda(emptyList(), emptyList(), now, 3).isEmpty())
    }

    // ---- notification planning ----

    @Test fun nextToNotifyPicksEarliestFutureNotifyItem() {
        val now = ms(utc, 2026, 5, 10, 12)
        val list = listOf(
                rem("a", now + 3 * 3_600_000L),
                rem("b", now + 3_600_000L),
                rem("quiet", now + 60_000L, notify = false),
                rem("done", now + 120_000L, done = true),
        )
        assertEquals("b", ReminderMath.nextToNotify(list, now, 0)!!.id)
        assertNull(ReminderMath.nextToNotify(emptyList(), now, 0))
    }

    @Test fun dueNowRespectsNotifiedMarkerAndMissedWindow() {
        val now = ms(utc, 2026, 5, 10, 12)
        val list = listOf(
                rem("fresh", now - 60_000L),
                rem("announced", now - 120_000L),
                rem("ancient", now - 24 * 3_600_000L),
                rem("future", now + 60_000L),
        )
        val due = ReminderMath.dueNow(list, now, notifiedUpToMs = now - 90_000L)
        assertEquals(listOf("fresh"), due.map { it.id })
        assertNotNull(ReminderMath.nextToNotify(list, now, now))
        assertEquals("future", ReminderMath.nextToNotify(list, now, now)!!.id)
    }

    @Test fun snoozeOverridesDueAndAlarmTimeIsInTheFuture() {
        val now = ms(utc, 2026, 5, 10, 12)
        val r = rem("s", now - 1000).copy(snoozeUntilMs = now + 600_000L)
        assertEquals(now + 600_000L, r.effectiveDueMs)
        assertEquals(now + 600_000L, ReminderMath.alarmTime(r, now))
        assertEquals(now + 1_000L, ReminderMath.alarmTime(rem("p", now - 5000), now))
    }

    @Test fun afterFiredRollsRepeatingAndClearsSnooze() {
        val due = ms(utc, 2026, 5, 10, 9)
        val fired = ReminderMath.afterFired(rem(due = due, repeat = RepeatRule.DAILY), due)
        assertEquals(ms(utc, 2026, 5, 11, 9), fired.dueEpochMs)
        val oneOff = ReminderMath.afterFired(rem(due = due).copy(snoozeUntilMs = due + 1), due + 1)
        assertEquals(due, oneOff.dueEpochMs)
        assertEquals(0L, oneOff.snoozeUntilMs)
    }
}
