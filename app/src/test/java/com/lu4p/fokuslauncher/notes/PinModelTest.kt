package com.lu4p.fokuslauncher.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinModelTest {
    @Test
    fun legacySinglePinnedRowIsFirstEntry() {
        // Schema 8 rows: one pinned note, pinOrder defaults to 0 after the migration.
        val rows = listOf(Triple(7L, true, 0), Triple(3L, false, 0), Triple(9L, false, 0))
        assertEquals(listOf(7L), PinModel.orderedIds(rows))
    }

    @Test
    fun ordersByPinOrderThenIdAndCapsAtThree() {
        val rows = listOf(Triple(1L, true, 2), Triple(2L, true, 0), Triple(3L, true, 1), Triple(4L, true, 3))
        assertEquals(listOf(2L, 3L, 1L), PinModel.orderedIds(rows))
        assertEquals(listOf(5L, 6L), PinModel.orderedIds(listOf(Triple(6L, true, 0), Triple(5L, true, 0))))
    }

    @Test
    fun addRules() {
        assertEquals(listOf(1L, 2L), PinModel.add(listOf(1L), 2L))
        assertEquals(listOf(1L, 2L), PinModel.add(listOf(1L, 2L), 2L))
        assertNull(PinModel.add(listOf(1L, 2L, 3L), 4L))
    }

    @Test
    fun replaceAndMove() {
        assertEquals(listOf(1L, 9L, 3L), PinModel.replace(listOf(1L, 2L, 3L), 2L, 9L))
        assertEquals(listOf(1L, 3L), PinModel.replace(listOf(1L, 2L, 3L), 2L, 3L))
        assertEquals(listOf(2L, 1L, 3L), PinModel.move(listOf(1L, 2L, 3L), 2L, -1))
        assertEquals(listOf(1L, 3L, 2L), PinModel.move(listOf(1L, 2L, 3L), 2L, 5))
        assertEquals(listOf(1L, 2L), PinModel.move(listOf(1L, 2L), 99L, 1))
    }

    @Test
    fun migrationOnlyAddsColumnsWithDefaults() {
        val sql = NoteMigrations.SQL_8_TO_9
        assertEquals(3, sql.size)
        sql.forEach {
            assertTrue(it, it.startsWith("ALTER TABLE `notes` ADD COLUMN"))
            assertTrue(it, it.contains("NOT NULL DEFAULT"))
        }
        assertTrue(sql.any { "`pinOrder` INTEGER" in it })
        assertTrue(sql.any { "`styleId` TEXT" in it })
        assertTrue(sql.any { "`cardStyle` TEXT" in it })
        assertEquals(8, NoteMigrations.migration8To9.startVersion)
        assertEquals(9, NoteMigrations.migration8To9.endVersion)
    }

    @Test
    fun legacyEntityDefaultsMatchMigrationDefaults() {
        val e = NoteEntity(pinned = true)
        assertEquals(0, e.pinOrder)
        assertEquals("", e.styleId)
        assertEquals("", e.cardStyle)
        assertEquals(NoteWidgetStyle.DEFAULT.appearance, e.appearance())
    }
}
