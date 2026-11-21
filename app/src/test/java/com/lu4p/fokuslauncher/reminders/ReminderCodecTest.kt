package com.lu4p.fokuslauncher.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uses Robolectric only because org.json is a stub in plain JVM unit tests. */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [28])
class ReminderCodecTest {
    @Test fun roundTripKeepsEveryField() {
        val r = Reminder(
                id = "x", title = "Design review \"quoted\"", note = "line1\nline2", dueEpochMs = 1_700_000_000_000L,
                zoneId = "Asia/Karachi", repeat = RepeatRule.WEEKDAYS, done = true, notify = false,
                colorArgb = 0xFF336699.toInt(), snoozeUntilMs = 1_700_000_600_000L,
        )
        assertEquals(listOf(r), ReminderCodec.decode(ReminderCodec.encode(listOf(r))))
    }

    @Test fun encodeAndDecodeAreCappedAt200() {
        val many = (0 until 250).map { Reminder("r$it", "t", dueEpochMs = 1000L + it, zoneId = "UTC") }
        val json = ReminderCodec.encode(many)
        assertEquals(MAX_REMINDERS, ReminderCodec.decode(json).size)
    }

    @Test fun garbageAndEmptyDecodeToEmpty() {
        assertTrue(ReminderCodec.decode(null).isEmpty())
        assertTrue(ReminderCodec.decode("").isEmpty())
        assertTrue(ReminderCodec.decode("not json").isEmpty())
        assertTrue(ReminderCodec.decode("{\"v\":1}").isEmpty())
    }

    @Test fun badEntriesAreSkippedAndUnknownRepeatFallsBack() {
        val raw = """{"v":1,"items":[{"id":"","due":1},{"id":"a","due":5,"r":"BOGUS"},{"id":"a","due":6},{"x":1}]}"""
        val out = ReminderCodec.decode(raw)
        assertEquals(1, out.size)
        assertEquals(RepeatRule.NONE, out[0].repeat)
        assertTrue(out[0].notify)
    }

    @Test fun futureVersionStillReadsKnownFields() {
        val raw = """{"v":9,"extra":true,"items":[{"id":"a","t":"hi","due":42,"z":"UTC","new":1}]}"""
        assertEquals("hi", ReminderCodec.decode(raw).single().title)
    }
}
