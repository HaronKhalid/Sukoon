package com.lu4p.fokuslauncher.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteAppearanceTest {
    @Test
    fun coercedClampsEveryField() {
        val wild =
                NoteAppearance(
                                fontSizeSp = 500f,
                                fontWeight = 950,
                                lineSpacing = 0f,
                                paddingDp = -4,
                                maxLines = 0,
                        )
                        .coerced()
        assertEquals(NoteAppearance.MAX_SIZE_SP, wild.fontSizeSp)
        assertEquals(900, wild.fontWeight)
        assertEquals(NoteAppearance.MIN_LINE_SPACING, wild.lineSpacing)
        assertEquals(0, wild.paddingDp)
        assertEquals(1, wild.maxLines)
        val low =
                NoteAppearance(fontSizeSp = 1f, fontWeight = 0, paddingDp = 999, maxLines = 999)
                        .coerced()
        assertEquals(NoteAppearance.MIN_SIZE_SP, low.fontSizeSp)
        assertEquals(100, low.fontWeight)
        assertEquals(NoteAppearance.MAX_PADDING_DP, low.paddingDp)
        assertEquals(NoteAppearance.MAX_LINES, low.maxLines)
    }

    @Test
    fun weightSnapsToHundreds() {
        assertEquals(400, NoteAppearance(fontWeight = 450).coerced().fontWeight)
    }

    @Test
    fun entityRoundTripPreservesAppearance() {
        val a =
                NoteAppearance(
                        fontFamily = "bundled:noto_nastaliq_urdu",
                        fontSizeSp = 22f,
                        fontWeight = 500,
                        textColor = 0xFFFFC857.toInt(),
                        alignment = NoteAlignment.END,
                        lineSpacing = 1.9f,
                        backgroundColor = 0x99000000.toInt(),
                        paddingDp = 12,
                        maxLines = 10,
                )
        assertEquals(a, NoteEntity().withAppearance(a).appearance())
    }

    @Test
    fun corruptAlignmentOrdinalFallsBackToStart() {
        assertEquals(NoteAlignment.START, NoteEntity(alignment = 99).appearance().alignment)
        assertTrue(NoteEntity(fontSizeSp = -3f).appearance().fontSizeSp >= NoteAppearance.MIN_SIZE_SP)
    }
}
