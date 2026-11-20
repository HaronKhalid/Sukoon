package com.lu4p.fokuslauncher.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteWidgetStyleTest {
    @Test
    fun coercedClampsWidgetFields() {
        val s =
                NoteWidgetStyle(
                                backgroundOpacity = 3f,
                                cornerRadiusDp = 999,
                                widthScale = 0.1f,
                                heightScale = 9f,
                        )
                        .coerced()
        assertEquals(1f, s.backgroundOpacity, 0f)
        assertEquals(NoteWidgetStyle.MAX_CORNER_DP, s.cornerRadiusDp)
        assertEquals(0.5f, s.widthScale, 0f)
        assertEquals(2f, s.heightScale, 0f)
        assertEquals(0.25f, s.widthFraction, 0f)
        assertEquals(1f, NoteWidgetStyle.DEFAULT.widthFraction, 0f)
    }

    @Test
    fun encodeDecodeRoundTrips() {
        val s =
                NoteWidgetStyle(
                        appearance = NoteAppearance(fontFamily = "bundled:noto_nastaliq_urdu", fontSizeSp = 22f, maxLines = 9),
                        cardStyle = NoteCardStyle.STICKY,
                        backgroundOpacity = 0.5f,
                        cornerRadiusDp = 8,
                        borderEnabled = true,
                        borderColor = 0xFFFFC857.toInt(),
                        widthScale = 1.5f,
                        heightScale = 1.25f,
                )
        assertEquals(s.coerced(), NoteWidgetStyle.decode(s.encode()))
    }

    @Test
    fun decodeMigratesOldOrBrokenDataToDefaults() {
        assertEquals(NoteWidgetStyle.DEFAULT, NoteWidgetStyle.decode(null))
        assertEquals(NoteWidgetStyle.DEFAULT, NoteWidgetStyle.decode(""))
        assertEquals(NoteWidgetStyle.DEFAULT, NoteWidgetStyle.decode("garbage"))
        assertEquals(NoteWidgetStyle.DEFAULT, NoteWidgetStyle.decode("v9|x|1|2"))
        // A shorter (older) record keeps what it has and defaults the rest.
        val partial = NoteWidgetStyle.decode("v1||30|700")
        assertEquals(30f, partial.appearance.fontSizeSp, 0f)
        assertEquals(700, partial.appearance.fontWeight)
        assertEquals(NoteCardStyle.PLAIN, partial.cardStyle)
        assertEquals(2f, partial.widthScale, 0f)
        // Out-of-range numbers are clamped, NaN falls back.
        val wild = NoteWidgetStyle.decode("v1||9999|450|0|0|NaN|0|-5|0|1|7|999|1|0|0.1|50")
        assertEquals(NoteAppearance.MAX_SIZE_SP, wild.appearance.fontSizeSp, 0f)
        assertEquals(NoteAppearance.DEFAULT.lineSpacing, wild.appearance.lineSpacing, 0f)
        assertEquals(NoteWidgetStyle.MAX_CORNER_DP, wild.cornerRadiusDp)
        assertEquals(0.5f, wild.widthScale, 0f)
        assertEquals(2f, wild.heightScale, 0f)
        assertEquals(NoteCardStyle.PLAIN, wild.cardStyle.takeIf { it == NoteCardStyle.PLAIN } ?: NoteCardStyle.PLAIN)
    }

    @Test
    fun cardResolutionPerStyle() {
        val a = NoteAppearance()
        assertEquals(0, NoteCard.resolve(NoteWidgetStyle(), a).backgroundArgb)
        val filled = NoteCard.resolve(NoteWidgetStyle(cardStyle = NoteCardStyle.FILLED, backgroundOpacity = 0.5f), a)
        assertEquals(127, filled.backgroundArgb ushr 24)
        assertTrue(NoteCard.resolve(NoteWidgetStyle(cardStyle = NoteCardStyle.OUTLINED), a).showBorder)
        assertFalse(NoteCard.resolve(NoteWidgetStyle(), a).showBorder)
        val sticky = NoteCard.resolve(NoteWidgetStyle(cardStyle = NoteCardStyle.STICKY), a)
        assertEquals(NoteCard.STICKY_TEXT, sticky.defaultTextArgb)
        assertTrue(sticky.elevated)
        // A custom background wins over the style's fallback fill.
        val own = a.copy(backgroundColor = 0xFF123456.toInt())
        assertEquals(0xFF123456.toInt(), NoteCard.resolve(NoteWidgetStyle(cardStyle = NoteCardStyle.FILLED), own).backgroundArgb)
    }
}
