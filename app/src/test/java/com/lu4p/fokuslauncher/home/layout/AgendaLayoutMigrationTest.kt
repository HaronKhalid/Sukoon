package com.lu4p.fokuslauncher.home.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaLayoutMigrationTest {
    @Test fun agendaIsAppendedAfterAllExistingTypes() {
        val entries = HomeElementType.entries
        assertEquals(HomeElementType.AGENDA, entries.last())
        assertEquals(entries.size - 1, HomeElementType.AGENDA.ordinal)
        assertEquals(13, HomeElementType.PROGRESS_BAR.ordinal)
        assertEquals(14, HomeElementType.AGENDA.ordinal)
    }

    @Test fun agendaIsHiddenByDefaultSoTheDefaultLayoutIsUnchanged() {
        assertFalse(HomeLayoutConfig.Default.isVisible(HomeElementType.AGENDA))
        assertEquals(4, HomeElementType.AGENDA.styleCount)
    }

    @Test fun v2DataStillDecodesAndGainsHiddenAgenda() {
        val v2 = "v2|CUSTOM|CLOCK:1:0:CENTER:120:NORMAL:0|DATE:1:1:START:100:NORMAL:0|FAVOURITES:1:2:START:100:NORMAL:0"
        val c = HomeLayoutCodec.decode(v2)!!
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, c.version)
        assertEquals(120, c.element(HomeElementType.CLOCK).scalePct)
        assertEquals(ElementAlignment.CENTER, c.element(HomeElementType.CLOCK).alignment)
        assertFalse(c.isVisible(HomeElementType.AGENDA))
        assertNotNull(c.elements.firstOrNull { it.type == HomeElementType.AGENDA })
    }

    @Test fun v1DataStillDecodesToCurrentVersion() {
        val v1 = "v1|CUSTOM|CLOCK:1:0:START:LARGE:NORMAL|FAVOURITES:1:1:START:MEDIUM:NORMAL"
        val c = HomeLayoutCodec.decode(v1)!!
        assertEquals(120, c.element(HomeElementType.CLOCK).scalePct)
        assertFalse(c.isVisible(HomeElementType.AGENDA))
    }

    @Test fun v3RoundTripKeepsAgendaVisibilityStyleAndOrder() {
        val base = HomeLayoutConfig.Default
        val c = base.update(HomeElementType.AGENDA) { it.copy(visible = true, style = 2, scalePct = 90) }
                .move(HomeElementType.AGENDA, -3)
        val encoded = HomeLayoutCodec.encode(c)
        assertTrue(encoded.startsWith("v3|"))
        val back = HomeLayoutCodec.decode(encoded)!!
        assertTrue(back.isVisible(HomeElementType.AGENDA))
        assertEquals(2, back.element(HomeElementType.AGENDA).style)
        assertEquals(90, back.element(HomeElementType.AGENDA).scalePct)
        assertEquals(c.element(HomeElementType.AGENDA).order, back.element(HomeElementType.AGENDA).order)
    }

    @Test fun outOfRangeAgendaStyleIsClamped() {
        val c = HomeLayoutCodec.decode("v3|CUSTOM|AGENDA:1:0:START:100:NORMAL:9")!!
        assertEquals(3, c.element(HomeElementType.AGENDA).style)
    }
}
