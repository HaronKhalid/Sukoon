package com.lu4p.fokuslauncher.reminders

import java.time.ZoneId

enum class RepeatRule { NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY }

/**
 * A local reminder. [dueEpochMs] plus [zoneId] fix the wall-clock time that repeats keep across
 * DST changes. [snoozeUntilMs] > 0 temporarily overrides the due time (notification "Snooze").
 */
data class Reminder(
        val id: String,
        val title: String,
        val note: String = "",
        val dueEpochMs: Long,
        val zoneId: String = ZoneId.systemDefault().id,
        val repeat: RepeatRule = RepeatRule.NONE,
        val done: Boolean = false,
        val notify: Boolean = true,
        /** 0 means no colour; otherwise an ARGB colour used as a small accent dot. */
        val colorArgb: Int = 0,
        val snoozeUntilMs: Long = 0L,
) {
    /** The time the reminder is currently due, honouring a snooze. */
    val effectiveDueMs: Long get() = if (snoozeUntilMs > 0L) snoozeUntilMs else dueEpochMs
    val zone: ZoneId get() = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
}

/** How many reminders are stored at most. */
const val MAX_REMINDERS = 200
