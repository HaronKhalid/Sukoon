package com.lu4p.fokuslauncher.reminders

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Pure recurrence, grouping, agenda and scheduling logic. No Android types, fully unit-tested. */
object ReminderMath {
    /**
     * The first occurrence of a repeating reminder strictly after both [afterMs] and [baseDueMs],
     * keeping the wall-clock time of the base in [zone] (so a 09:00 reminder stays at 09:00 across
     * DST; a time inside a spring-forward gap moves forward). Monthly repeats keep the original day
     * of month, clamped to short months (31st -> 28/29/30). Returns null for [RepeatRule.NONE].
     */
    fun nextOccurrence(repeat: RepeatRule, baseDueMs: Long, zone: ZoneId, afterMs: Long): Long? {
        if (repeat == RepeatRule.NONE) return null
        val base = Instant.ofEpochMilli(baseDueMs).atZone(zone)
        val time: LocalTime = base.toLocalTime()
        val d0: LocalDate = base.toLocalDate()
        fun at(d: LocalDate): Long = ZonedDateTime.of(d, time, zone).toInstant().toEpochMilli()
        val floor = maxOf(afterMs, baseDueMs)
        val afterDate = Instant.ofEpochMilli(floor).atZone(zone).toLocalDate()
        val gapDays = ChronoUnit.DAYS.between(d0, afterDate).coerceAtLeast(0)
        return when (repeat) {
            RepeatRule.NONE -> null
            RepeatRule.DAILY -> {
                var d = d0.plusDays(maxOf(1L, gapDays - 1))
                while (at(d) <= floor) d = d.plusDays(1)
                at(d)
            }
            RepeatRule.WEEKDAYS -> {
                var d = d0.plusDays(maxOf(1L, gapDays - 1))
                while (isWeekend(d) || at(d) <= floor) d = d.plusDays(1)
                at(d)
            }
            RepeatRule.WEEKLY -> {
                var d = d0.plusWeeks(maxOf(1L, gapDays / 7 - 1))
                while (at(d) <= floor) d = d.plusWeeks(1)
                at(d)
            }
            RepeatRule.MONTHLY -> {
                var k = maxOf(1L, ChronoUnit.MONTHS.between(d0, afterDate) - 1)
                // plusMonths always works from d0, so the day of month never drifts.
                while (at(d0.plusMonths(k)) <= floor) k++
                at(d0.plusMonths(k))
            }
        }
    }

    private fun isWeekend(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY

    /** Completing: one-off reminders become done; repeating ones roll to the next future occurrence. */
    fun complete(r: Reminder, nowMs: Long): Reminder {
        val next = nextOccurrence(r.repeat, r.dueEpochMs, r.zone, maxOf(r.effectiveDueMs, nowMs))
        return if (next == null) r.copy(done = true, snoozeUntilMs = 0L)
        else r.copy(dueEpochMs = next, snoozeUntilMs = 0L, done = false)
    }

    fun isOverdue(r: Reminder, nowMs: Long): Boolean = !r.done && r.effectiveDueMs < nowMs

    /** Drops done reminders first (oldest due first), then the oldest by due time, down to [max]. */
    fun cap(list: List<Reminder>, max: Int = MAX_REMINDERS): List<Reminder> {
        if (list.size <= max) return list
        val drop = list.size - max
        val victims = list.sortedWith(compareBy<Reminder>({ !it.done }, { it.effectiveDueMs }))
                .take(drop).map { it.id }.toSet()
        return list.filterNot { it.id in victims }
    }

    // ---- grouping -----------------------------------------------------------------------

    sealed interface GroupKey {
        data object Overdue : GroupKey
        data object Today : GroupKey
        data object Tomorrow : GroupKey
        data class Weekday(val day: DayOfWeek) : GroupKey
        data class Date(val date: LocalDate) : GroupKey
        data object Done : GroupKey
    }

    data class Group(val key: GroupKey, val items: List<Reminder>)

    /** Label key for a due time: Today, Tomorrow, a weekday within a week, else a date. */
    fun dayKey(dueMs: Long, nowMs: Long, zone: ZoneId): GroupKey {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val day = Instant.ofEpochMilli(dueMs).atZone(zone).toLocalDate()
        return when (ChronoUnit.DAYS.between(today, day)) {
            0L -> GroupKey.Today
            1L -> GroupKey.Tomorrow
            in 2L..6L -> GroupKey.Weekday(day.dayOfWeek)
            else -> GroupKey.Date(day)
        }
    }

    /** Overdue first, then each day ascending, then Done (newest first). */
    fun group(list: List<Reminder>, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): List<Group> {
        val active = list.filter { !it.done }.sortedBy { it.effectiveDueMs }
        val out = mutableListOf<Group>()
        val overdue = active.filter { it.effectiveDueMs < nowMs }
        if (overdue.isNotEmpty()) out += Group(GroupKey.Overdue, overdue)
        active.filter { it.effectiveDueMs >= nowMs }
                .groupBy { Instant.ofEpochMilli(it.effectiveDueMs).atZone(zone).toLocalDate() }
                .toSortedMap()
                .forEach { (_, items) -> out += Group(dayKey(items.first().effectiveDueMs, nowMs, zone), items) }
        val done = list.filter { it.done }.sortedByDescending { it.effectiveDueMs }
        if (done.isNotEmpty()) out += Group(GroupKey.Done, done)
        return out
    }

    // ---- quick-add times ----------------------------------------------------------------

    fun inOneHour(nowMs: Long): Long = (nowMs / 60_000L) * 60_000L + 3_600_000L

    /** 18:00 today; 21:00 if it is already past 18:00; 18:00 tomorrow after 20:30. */
    fun thisEvening(nowMs: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val today = now.toLocalDate()
        val t = now.toLocalTime()
        val target = when {
            t < LocalTime.of(18, 0) -> ZonedDateTime.of(today, LocalTime.of(18, 0), zone)
            t < LocalTime.of(20, 30) -> ZonedDateTime.of(today, LocalTime.of(21, 0), zone)
            else -> ZonedDateTime.of(today.plusDays(1), LocalTime.of(18, 0), zone)
        }
        return target.toInstant().toEpochMilli()
    }

    fun tomorrowAt(nowMs: Long, zone: ZoneId, hour: Int = 9, minute: Int = 0): Long {
        val tomorrow = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().plusDays(1)
        return ZonedDateTime.of(tomorrow, LocalTime.of(hour, minute), zone).toInstant().toEpochMilli()
    }

    // ---- agenda -------------------------------------------------------------------------

    /** Not stored: calendar items are mapped from a live query. */
    data class AgendaItem(
            val id: String,
            val title: String,
            val whenMs: Long,
            val overdue: Boolean = false,
            val fromCalendar: Boolean = false,
            val allDay: Boolean = false,
            val colorArgb: Int = 0,
    )

    /**
     * Next [limit] items: done reminders are excluded, at most one overdue reminder (the most
     * recently due) is shown first, then upcoming reminders and calendar events by time.
     */
    fun selectAgenda(
            reminders: List<Reminder>,
            calendar: List<AgendaItem>,
            nowMs: Long,
            limit: Int,
    ): List<AgendaItem> {
        if (limit <= 0) return emptyList()
        val active = reminders.filter { !it.done }
        val overdue = active.filter { it.effectiveDueMs < nowMs }.maxByOrNull { it.effectiveDueMs }
                ?.let { listOf(it.toAgenda(true)) }.orEmpty()
        val upcoming = active.filter { it.effectiveDueMs >= nowMs }.map { it.toAgenda(false) } +
                calendar.filter { it.whenMs >= nowMs || it.allDay }
        return (overdue + upcoming.sortedBy { it.whenMs }).take(limit)
    }

    private fun Reminder.toAgenda(overdue: Boolean) =
            AgendaItem(id, title, effectiveDueMs, overdue = overdue, colorArgb = colorArgb)

    // ---- notification planning ------------------------------------------------------------

    /** Reminders missed by more than this (e.g. phone off for a day) are not announced late. */
    const val MISSED_WINDOW_MS = 6 * 3_600_000L
    const val MIN_ALARM_DELAY_MS = 1_000L

    private fun lowerBound(nowMs: Long, notifiedUpToMs: Long) = maxOf(notifiedUpToMs, nowMs - MISSED_WINDOW_MS)

    /** Reminders that are due and not announced yet. */
    fun dueNow(list: List<Reminder>, nowMs: Long, notifiedUpToMs: Long): List<Reminder> {
        val lo = lowerBound(nowMs, notifiedUpToMs)
        return list.filter { it.notify && !it.done && it.effectiveDueMs <= nowMs && it.effectiveDueMs > lo }
                .sortedBy { it.effectiveDueMs }
    }

    /** The single reminder the next alarm should be set for, or null. */
    fun nextToNotify(list: List<Reminder>, nowMs: Long, notifiedUpToMs: Long): Reminder? {
        val lo = lowerBound(nowMs, notifiedUpToMs)
        return list.filter { it.notify && !it.done && it.effectiveDueMs > lo }.minByOrNull { it.effectiveDueMs }
    }

    fun alarmTime(r: Reminder, nowMs: Long): Long = maxOf(r.effectiveDueMs, nowMs + MIN_ALARM_DELAY_MS)

    /** After announcing: snooze cleared; repeating reminders roll to their next occurrence. */
    fun afterFired(r: Reminder, nowMs: Long): Reminder {
        val next = nextOccurrence(r.repeat, r.dueEpochMs, r.zone, maxOf(r.effectiveDueMs, nowMs))
        return r.copy(snoozeUntilMs = 0L, dueEpochMs = next ?: r.dueEpochMs)
    }
}
