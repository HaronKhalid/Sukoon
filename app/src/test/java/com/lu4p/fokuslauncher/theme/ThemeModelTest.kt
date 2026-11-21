package com.lu4p.fokuslauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModelTest {
    @Test
    fun `original eight built-in ids stay stable and ids are unique`() {
        val ids = BuiltInThemes.all.map { it.id }
        assertEquals(
                listOf("amoled-black", "midnight-navy", "cream", "monochrome", "nature", "ocean", "soft-pastel", "custom"),
                ids.take(8),
        )
        assertTrue(ids.size >= 22)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every built-in theme passes strict validation and round-trips`() {
        BuiltInThemes.all.forEach { t ->
            val decoded = ThemeCodec.decode(ThemeCodec.encode(t))
            assertTrue("${t.id}: $decoded", decoded is ThemeParseResult.Success)
            assertEquals(t, (decoded as ThemeParseResult.Success).theme)
            assertEquals(ThemeDefinition.CURRENT_SCHEMA_VERSION, t.schemaVersion)
        }
    }

    @Test
    fun `built-in appearance matches background luminance`() {
        BuiltInThemes.all.forEach { t ->
            val light = ThemeColorMath.luminance(t.colors.background) > 0.4
            assertEquals(t.id, light, t.appearance == ThemeAppearance.LIGHT)
        }
    }

    @Test
    fun `default theme is AMOLED black and lookup by id works`() {
        assertEquals(BuiltInThemes.ID_AMOLED_BLACK, BuiltInThemes.default.id)
        assertNotNull(BuiltInThemes.byId("ocean"))
        assertNull(BuiltInThemes.byId("nope"))
        assertTrue(BuiltInThemes.isBuiltInId("custom"))
    }

    @Test
    fun `hex parsing is strict`() {
        assertEquals(0xFF112233.toInt(), ThemeColorMath.parseHex("#112233"))
        assertEquals(0xFFAABBCC.toInt(), ThemeColorMath.parseHex("#aAbBcC"))
        listOf("112233", "#12345", "#1234567", "#GG0000", "#+12345", "", "#11 233", "#-12345").forEach {
            assertNull(it, ThemeColorMath.parseHex(it))
        }
        assertEquals("#0A0B0C", ThemeColorMath.toHex(0xFF0A0B0C.toInt()))
    }

    @Test
    fun `contrast ratio matches WCAG reference values`() {
        assertEquals(21.0, ThemeColorMath.contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, ThemeColorMath.contrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001)
    }

    @Test
    fun `editing a colour updates it, keeps it opaque and adjusts appearance`() {
        val t = BuiltInThemes.Custom.withColor(ThemeColorField.BACKGROUND, 0x00FFFFFF)
        assertEquals(0xFFFFFFFF.toInt(), t.colors.background)
        assertEquals(ThemeAppearance.LIGHT, t.appearance)
        val unreadable = t.withColor(ThemeColorField.PRIMARY_TEXT, 0xFFF0F0F0.toInt())
        assertFalse(unreadable.isReadable())
        assertEquals(0xFF123456.toInt(), t.withColor(ThemeColorField.ACCENT, 0x123456).colors.get(ThemeColorField.ACCENT))
    }

    @Test
    fun `save id keeps an existing custom id otherwise allocates a fresh one`() {
        val taken = setOf("custom-1", "custom-2")
        assertEquals("custom-2", saveIdFor(BuiltInThemes.Custom.copy(id = "custom-2"), taken))
        assertEquals("custom-3", saveIdFor(BuiltInThemes.Ocean, taken))
        assertEquals("custom-1", ThemeStore.newCustomId(emptySet()))
    }
}
