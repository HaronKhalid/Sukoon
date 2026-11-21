package com.lu4p.fokuslauncher.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteStylesTest {
    @Test
    fun registryHasStableIds() {
        val ids = NoteStyles.all.map { it.id }
        assertEquals(
                listOf(
                        "sticky", "minimal", "glass", "quote", "handwritten", "checklist",
                        "ruled", "neon", "outlined", "journal", "poster", "soft",
                ),
                ids,
        )
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.size >= 10)
    }

    @Test
    fun unknownIdFallsBackToMinimal() {
        assertEquals("minimal", NoteStyles.resolve("does-not-exist").id)
        assertEquals("minimal", NoteStyles.resolve(null).id)
    }

    @Test
    fun everyStyleMapsOntoValidEditableFields() {
        NoteStyles.all.forEach { s ->
            val w = NoteStyles.widgetStyleOf(s) { false }
            // Already within clamps: coercing changes nothing, so nothing is lost on edit.
            assertEquals(s.id, w, w.coerced())
            val note = NoteStyles.applyTo(NoteEntity(), s) { false }
            assertEquals(s.id, note.styleId)
            assertEquals(w.appearance, note.appearance())
            assertEquals(w.cardStyle, NoteWidgetStyle.decode(note.cardStyle).cardStyle)
            assertEquals(w.cornerRadiusDp, NoteWidgetStyle.decode(note.cardStyle).cornerRadiusDp)
        }
    }

    @Test
    fun fontsUseRegistryIdsWithSafeFallback() {
        val hand = NoteStyles.resolve("handwritten")
        assertEquals("cursive", NoteStyles.fontFor(hand) { false })
        assertEquals("bundled:caveat", NoteStyles.fontFor(hand) { it == "caveat" })
        assertEquals("bundled:dancing_script", NoteStyles.fontFor(hand) { it == "dancing_script" })
        assertEquals("", NoteStyles.fontFor(NoteStyles.resolve("glass")) { true })
    }

    @Test
    fun decoratedText() {
        assertEquals(
                "☐ milk\n\n☑ eggs\n☐ bread",
                NoteStyles.decorateText(NoteDecoration.CHECKLIST, "milk\n\n- [x] eggs\n- [ ] bread"),
        )
        assertEquals("a\n— 21 Nov", NoteStyles.decorateText(NoteDecoration.TIMESTAMP, "a\n", "21 Nov"))
        assertEquals("a", NoteStyles.decorateText(NoteDecoration.TIMESTAMP, "a", null))
        assertEquals("a", NoteStyles.decorateText(NoteDecoration.RULED, "a", "x"))
    }

    @Test
    fun effectiveUsesGlobalCardUnlessNoteHasItsOwn() {
        val global = NoteWidgetStyle.DEFAULT.copy(cornerRadiusDp = 5, widthScale = 1f)
        val plain = NoteEntity(fontSizeSp = 20f)
        assertEquals(5, NoteStyles.effective(plain, global).cornerRadiusDp)
        assertEquals(20f, NoteStyles.effective(plain, global).appearance.fontSizeSp, 0f)
        val own = NoteStyles.applyTo(plain, NoteStyles.resolve("soft"))
        assertEquals(32, NoteStyles.effective(own, global).cornerRadiusDp)
        assertFalse(NoteStyles.effective(own, global).widthScale == 1f)
    }
}
