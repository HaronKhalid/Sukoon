package com.lu4p.fokuslauncher.home.layout

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.lu4p.fokuslauncher.home.layout.HomeElementType.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private fun time(h: Int, m: Int, s: Int = 0) =
            ZonedDateTime.of(2026, 4, 7, h, m, s, 0, ZoneId.of("UTC"))

    @Test
    fun defaultConfigKeepsExistingElementsVisibleAndNewOnesOff() {
        val d = HomeLayoutConfig.Default
        listOf(CLOCK, DATE, BATTERY, NOTES, FAVOURITES).forEach { assertTrue(d.isVisible(it)) }
        listOf(APP_SEARCH, FOCUS_STATUS, SPACER).forEach { assertFalse(d.isVisible(it)) }
        assertTrue(ClockSettings().isLegacy)
        assertEquals(HomeElementType.entries.size, d.elements.size)
    }

    @Test
    fun presetsHaveExpectedContentAndStayCustomisable() {
        fun visible(p: LayoutPreset) =
                HomeLayoutConfig.forPreset(p).elements.filter { it.visible }.map { it.type }.toSet()
        assertEquals(setOf(CLOCK, FAVOURITES), visible(LayoutPreset.PURE_MINIMAL))
        assertEquals(setOf(SPACER, CLOCK, DATE, FAVOURITES), visible(LayoutPreset.AESTHETIC))
        assertEquals(setOf(CLOCK, FOCUS_STATUS, FAVOURITES), visible(LayoutPreset.FOCUS))
        assertTrue(BATTERY in visible(LayoutPreset.INFORMATION) && NOTES in visible(LayoutPreset.INFORMATION))
        val aesthetic = HomeLayoutConfig.forPreset(LayoutPreset.AESTHETIC)
        assertEquals(ElementAlignment.CENTER, aesthetic.element(CLOCK).alignment)
        val edited = aesthetic.update(DATE) { it.copy(visible = false) }
        assertEquals(LayoutPreset.CUSTOM, edited.preset)
        assertFalse(edited.isVisible(DATE))
    }

    @Test
    fun normalizationRepairsDuplicatesMissingAndOrder() {
        val broken = HomeLayoutConfig(
                elements = listOf(
                        ElementConfig(DATE, order = 5),
                        ElementConfig(DATE, order = 1, visible = false),
                        ElementConfig(CLOCK, order = 5),
                ),
        ).normalized()
        assertEquals(HomeElementType.entries.size, broken.elements.size)
        assertEquals((0 until HomeElementType.entries.size).toList(), broken.elements.map { it.order }.sorted())
        assertTrue(broken.isVisible(DATE))
    }

    @Test
    fun moveReordersDensely() {
        val moved = HomeLayoutConfig.Default.move(NOTES, -1)
        val order = moved.ordered().map { it.type }
        assertTrue(order.indexOf(NOTES) < order.indexOf(BATTERY))
        assertEquals(HomeLayoutConfig.Default, HomeLayoutConfig.Default.move(CLOCK, -1))
    }

    @Test
    fun layoutCodecRoundTripsAndRejectsInvalid() {
        HomeLayoutConfig.forPreset(LayoutPreset.INFORMATION).let {
            assertEquals(it, HomeLayoutCodec.decode(HomeLayoutCodec.encode(it)))
        }
        assertEquals(HomeLayoutConfig.Default, HomeLayoutCodec.decode(HomeLayoutCodec.encode(HomeLayoutConfig.Default)))
        listOf(null, "", "garbage", "v9|DEFAULT|CLOCK:1:0:START:MEDIUM:NORMAL", "v1|NOPE|CLOCK:1:0:START:MEDIUM:NORMAL",
                "v1|DEFAULT|CLOCK:2:0:START:MEDIUM:NORMAL", "v1|DEFAULT|CLOCK:1:x:START:MEDIUM:NORMAL",
                "v1|DEFAULT|CLOCK:1:0:START:MEDIUM:NORMAL|CLOCK:1:1:START:MEDIUM:NORMAL",
                "v1|DEFAULT|CLOCK:1:0:SIDEWAYS:MEDIUM:NORMAL", "v1|DEFAULT|CLOCK:1:500:START:MEDIUM:NORMAL")
                .forEach { assertNull(it, HomeLayoutCodec.decode(it)) }
        assertEquals(HomeLayoutConfig.Default, HomeLayoutCodec.decodeOrDefault("garbage"))
    }

    @Test
    fun clockSettingsValidationClampsAndCodecRoundTrips() {
        val wild = ClockSettings(fontSizeSp = 5000, letterSpacingSp = Float.NaN, lineHeightMultiplier = 99f).validated()
        assertEquals(ClockSettings.MAX_FONT_SP, wild.fontSizeSp)
        assertEquals(0f, wild.letterSpacingSp)
        assertEquals(1.6f, wild.lineHeightMultiplier)
        assertEquals(ClockSettings.MIN_FONT_SP, ClockSettings(fontSizeSp = 3).validated().fontSizeSp)
        val s = ClockSettings(style = ClockStyle.SPLIT, hourFormat = ClockHourFormat.H12, showSeconds = true,
                fontSizeSp = 80, weight = ClockFontWeight.LIGHT, letterSpacingSp = 2.5f, lineHeightMultiplier = 1.2f,
                colorArgb = 0xFFFFC857.toInt(), alignment = ElementAlignment.CENTER, showDate = false,
                dateStyle = ClockDateStyle.LONG)
        assertEquals(s, ClockSettingsCodec.decode(ClockSettingsCodec.encode(s)))
        assertNull(ClockSettingsCodec.decode("c1|bad"))
        assertNull(ClockSettingsCodec.decode("c1|LARGE_DIGITAL|SYSTEM|0|0|SEMI_BOLD|NaN|1.0|0|START|1|SYSTEM"))
        assertEquals(ClockSettings(), ClockSettingsCodec.decodeOrDefault("junk"))
        assertFalse(ClockSettings().showSeconds)
    }

    @Test
    fun clockFormatting() {
        assertEquals(listOf("14:05"), ClockFormatter.format(time(14, 5), ClockStyle.LARGE_DIGITAL, true, false).lines)
        val twelve = ClockFormatter.format(time(14, 5), ClockStyle.LARGE_DIGITAL, false, false)
        assertEquals(listOf("2:05"), twelve.lines)
        assertEquals("PM", twelve.period)
        assertEquals(listOf("12:00"), ClockFormatter.format(time(0, 0), ClockStyle.COMPACT, false, false).lines)
        assertEquals("AM", ClockFormatter.format(time(0, 0), ClockStyle.COMPACT, false, false).period)
        assertEquals(listOf("14:05:09"), ClockFormatter.format(time(14, 5, 9), ClockStyle.LARGE_DIGITAL, true, true).lines)
        assertEquals(listOf("14", "05"), ClockFormatter.format(time(14, 5, 9), ClockStyle.STACKED, true, false).lines)
        assertEquals(listOf("14", "05", "09"), ClockFormatter.format(time(14, 5, 9), ClockStyle.STACKED, true, true).lines)
        assertEquals(listOf("14", "05:09"), ClockFormatter.format(time(14, 5, 9), ClockStyle.SPLIT, true, true).lines)
        assertTrue(ClockFormatter.resolve24h(ClockHourFormat.SYSTEM, true))
        assertFalse(ClockFormatter.resolve24h(ClockHourFormat.H12, true))
        assertTrue(ClockFormatter.resolve24h(ClockHourFormat.H24, false))
    }

    @Test
    fun dateFormattingIsLocaleAware() {
        assertNull(ClockFormatter.formatDate(time(1, 1), Locale.US, ClockDateStyle.SYSTEM))
        val us = ClockFormatter.formatDate(time(1, 1), Locale.US, ClockDateStyle.LONG)
        val de = ClockFormatter.formatDate(time(1, 1), Locale.GERMANY, ClockDateStyle.LONG)
        assertEquals("April 7, 2026", us)
        assertEquals("7. April 2026", de)
        assertNotNull(ClockFormatter.formatDate(time(1, 1), Locale.FRANCE, ClockDateStyle.FULL))
    }

    /** In-memory DataStore: real file-backed DataStore cannot rename its temp file reliably on Windows CI. */
    private class FakeStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(state.value).also { state.value = it }
    }

    @Test
    fun repositoryPersistsAndFallsBackOnInvalidData() = runBlocking {
        val store = FakeStore()
        val repo = HomeLayoutRepository(store)
        assertEquals(HomeLayoutConfig.Default, repo.layoutFlow.first())
        assertEquals(ClockSettings(), repo.clockFlow.first())
        val cfg = HomeLayoutConfig.forPreset(LayoutPreset.FOCUS)
        val clock = ClockSettings(style = ClockStyle.STACKED, showSeconds = true)
        repo.setLayout(cfg)
        repo.setClock(clock)
        assertEquals(cfg, repo.layoutFlow.first())
        assertEquals(clock, repo.clockFlow.first())
        store.edit {
            it[stringPreferencesKey(HomeLayoutRepository.LAYOUT_KEY_NAME)] = "corrupt"
            it[stringPreferencesKey(HomeLayoutRepository.CLOCK_KEY_NAME)] = "corrupt"
        }
        assertEquals(HomeLayoutConfig.Default, repo.layoutFlow.first())
        assertEquals(ClockSettings(), repo.clockFlow.first())
    }
}
