package com.lu4p.fokuslauncher.focus

import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLogicTest {
    private val weekdays = setOf(1, 2, 3, 4, 5)

    @Test
    fun sameDayRange() {
        val s = FocusSchedule(true, weekdays, 9 * 60, 17 * 60)
        assertTrue(s.isActiveAt(1, 9 * 60))
        assertTrue(s.isActiveAt(5, 16 * 60 + 59))
        assertFalse(s.isActiveAt(1, 17 * 60))
        assertFalse(s.isActiveAt(1, 8 * 60 + 59))
        assertFalse(s.isActiveAt(6, 12 * 60))
    }

    @Test
    fun overnightRangeBelongsToStartDay() {
        val s = FocusSchedule(true, setOf(5), 22 * 60, 6 * 60) // Friday night
        assertTrue(s.isActiveAt(5, 23 * 60))
        assertTrue(s.isActiveAt(6, 5 * 60 + 59)) // Saturday early morning
        assertFalse(s.isActiveAt(6, 6 * 60))
        assertFalse(s.isActiveAt(5, 5 * 60)) // Friday early morning is Thursday night
        assertFalse(s.isActiveAt(6, 23 * 60))
    }

    @Test
    fun overnightWrapsSundayToMonday() {
        val s = FocusSchedule(true, setOf(7), 23 * 60, 1 * 60)
        assertTrue(s.isActiveAt(1, 30))
        assertFalse(s.isActiveAt(2, 30))
    }

    @Test
    fun disabledEmptyOrZeroLengthNeverActive() {
        assertFalse(FocusSchedule(false, weekdays).isActiveAt(1, 600))
        assertFalse(FocusSchedule(true, emptySet()).isActiveAt(1, 600))
        assertFalse(FocusSchedule(true, weekdays, 600, 600).isActiveAt(1, 600))
    }

    @Test
    fun filterHook() {
        val items = listOf("0|a", "0|b", "10|c")
        assertEquals(items, filterForFocus(items, null) { it })
        assertEquals(items, filterForFocus(items, emptySet()) { it })
        assertEquals(listOf("0|b", "10|c"), filterForFocus(items, setOf("0|b", "10|c")) { it })
        assertEquals(listOf("0|a"), filterForFocus(items, setOf("0|a")) { it })
        assertEquals(emptyList<String>(), filterForFocus(items, setOf("0|zzz")) { it })
    }

    private fun profiles(vararg edits: FocusProfile) =
            FocusDefaults.profiles().map { d -> edits.firstOrNull { it.kind == d.kind } ?: d }

    @Test
    fun scheduleActivatesAndDeactivatesOnResume() {
        val work =
                FocusProfile(FocusKind.WORK, "Work", schedule = FocusSchedule(true, weekdays, 9 * 60, 17 * 60))
        val ps = profiles(work)
        val on = resolveOnResume(FocusRuntime(), ps, 2, 10 * 60)
        assertEquals(FocusRuntime("work", true, null), on)
        val off = resolveOnResume(on, ps, 2, 18 * 60)
        assertEquals(FocusRuntime(null, false, null), off)
    }

    @Test
    fun manualProfileIsNotOverriddenOrClearedBySchedule() {
        val work =
                FocusProfile(FocusKind.WORK, "Work", schedule = FocusSchedule(true, weekdays, 9 * 60, 17 * 60))
        val ps = profiles(work)
        val manual = FocusRuntime("study", false, null)
        assertEquals(manual, resolveOnResume(manual, ps, 2, 10 * 60))
        assertEquals(manual, resolveOnResume(manual, ps, 2, 20 * 60))
    }

    @Test
    fun userSwitchOffIsRespectedUntilWindowEnds() {
        val work =
                FocusProfile(FocusKind.WORK, "Work", schedule = FocusSchedule(true, weekdays, 9 * 60, 17 * 60))
        val ps = profiles(work)
        val off = FocusRuntime(null, false, "work")
        assertEquals(off, resolveOnResume(off, ps, 2, 11 * 60))
        assertNull(resolveOnResume(off, ps, 2, 18 * 60).suppressedId)
    }

    @Test
    fun codecRoundTrip() {
        val custom =
                FocusProfile(
                        FocusKind.CUSTOM,
                        "My \"deep\" focus\n",
                        visibleApps = setOf("0|com.a", "10|com.b"),
                        layoutPreset = LayoutPreset.PURE_MINIMAL,
                        themeId = "amoled-black",
                        schedule = FocusSchedule(true, setOf(1, 7), 22 * 60, 6 * 60),
                        sessionMinutes = 30,
                        reminder = "Phone away",
                )
        val ps = profiles(custom)
        val decoded = FocusCodec.decodeOrDefault(FocusCodec.encode(ps))
        assertEquals(ps.map { it.copy(name = it.name.trim()) }, decoded)
    }

    @Test
    fun codecFailsSafeToDefaults() {
        val defaults = FocusDefaults.profiles()
        assertEquals(defaults, FocusCodec.decodeOrDefault(null))
        assertEquals(defaults, FocusCodec.decodeOrDefault(""))
        assertEquals(defaults, FocusCodec.decodeOrDefault("not json"))
        assertEquals(defaults, FocusCodec.decodeOrDefault("{\"schemaVersion\":2,\"profiles\":[]}"))
        assertEquals(defaults, FocusCodec.decodeOrDefault("{\"profiles\":[]}"))
        assertEquals(defaults, FocusCodec.decodeOrDefault("x".repeat(100_000)))
    }

    @Test
    fun codecRejectsInvalidProfileButKeepsOthers() {
        val good = FocusProfile(FocusKind.WORK, "Deep work", visibleApps = setOf("0|com.a"))
        val json = FocusCodec.encode(listOf(good))
        val badStart = json.replace("\"start\":540", "\"start\":9999")
        val decodedBad = FocusCodec.decodeOrDefault(badStart)
        assertEquals(FocusDefaults.profiles(), decodedBad)
        val badApp = json.replace("0|com.a", "nopipe")
        assertEquals(FocusDefaults.profiles(), FocusCodec.decodeOrDefault(badApp))
        val badLayout = FocusCodec.encode(listOf(good.copy(layoutPreset = LayoutPreset.FOCUS)))
                .replace("FOCUS", "BOGUS")
        assertEquals(FocusDefaults.profiles(), FocusCodec.decodeOrDefault(badLayout))
        assertEquals(good, FocusCodec.decodeOrDefault(json).first { it.kind == FocusKind.WORK })
    }

    @Test
    fun unknownKeysIgnoredAndUnknownIdDropped() {
        val json =
                "{\"schemaVersion\":1,\"extra\":1,\"profiles\":[{\"id\":\"evil\",\"name\":\"x\"}]}"
        assertEquals(FocusDefaults.profiles(), FocusCodec.decodeOrDefault(json))
    }
}
