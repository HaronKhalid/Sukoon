package com.lu4p.fokuslauncher.reminders

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Optional, read-only calendar access. Only called after the user enabled "Also show calendar
 * events" and granted READ_CALENDAR. Events are mapped to transient agenda items and never stored.
 */
object CalendarReader {
    const val DAYS_AHEAD = 3
    private const val MAX_EVENTS = 20

    fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** All-day events are stored as UTC midnight; show them at local midnight of the same date. */
    fun allDayToLocalMs(utcMidnightMs: Long, zone: ZoneId): Long =
            Instant.ofEpochMilli(utcMidnightMs).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun upcoming(context: Context, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): List<ReminderMath.AgendaItem> {
        if (!hasPermission(context)) return emptyList()
        val end = nowMs + DAYS_AHEAD * 24L * 3_600_000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, nowMs)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
        )
        val out = ArrayList<ReminderMath.AgendaItem>()
        try {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext() && out.size < MAX_EVENTS) {
                    val allDay = c.getInt(4) == 1
                    val begin = c.getLong(2)
                    val finish = c.getLong(3)
                    val whenMs = if (allDay) allDayToLocalMs(begin, zone) else begin
                    if (!allDay && finish < nowMs) continue
                    val title = c.getString(1).orEmpty().ifBlank { "(no title)" }
                    out += ReminderMath.AgendaItem(
                            id = "cal:${c.getLong(0)}:$begin", title = title, whenMs = whenMs,
                            fromCalendar = true, allDay = allDay,
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: RuntimeException) {
            return emptyList()
        }
        return out
    }
}
