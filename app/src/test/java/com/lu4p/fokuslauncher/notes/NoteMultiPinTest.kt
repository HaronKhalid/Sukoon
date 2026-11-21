package com.lu4p.fokuslauncher.notes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMultiPinTest {
    private var now = 1_000L
    private val dao = FakeNoteDao()
    private val repo = NoteRepository(dao) { now }

    private suspend fun makeNotes(vararg texts: String): List<Long> =
            texts.map {
                val id = repo.create()
                repo.saveText(id, it, "")
                id
            }

    @Test
    fun pinsUpToThreeInOrderAndRejectsTheFourth() = runBlocking {
        val ids = makeNotes("a", "b", "c", "d")
        assertTrue(repo.pinAdd(ids[0]))
        assertTrue(repo.pinAdd(ids[1]))
        assertTrue(repo.pinAdd(ids[2]))
        assertFalse(repo.pinAdd(ids[3]))
        assertEquals(listOf(ids[0], ids[1], ids[2]), repo.pinnedNotes.first().map { it.id })
    }

    @Test
    fun moveReplaceAndUnpinKeepOrder() = runBlocking {
        val ids = makeNotes("a", "b", "c", "d")
        ids.take(3).forEach { repo.pinAdd(it) }
        repo.movePin(ids[2], -2)
        assertEquals(listOf(ids[2], ids[0], ids[1]), repo.pinnedNotes.first().map { it.id })
        repo.replacePin(ids[0], ids[3])
        assertEquals(listOf(ids[2], ids[3], ids[1]), repo.pinnedNotes.first().map { it.id })
        repo.unpinOne(ids[2])
        assertEquals(listOf(ids[3], ids[1]), repo.pinnedNotes.first().map { it.id })
        assertFalse(repo.get(ids[2])!!.pinned)
    }

    @Test
    fun perNoteStylesAreIndependent() = runBlocking {
        val ids = makeNotes("a", "b")
        ids.forEach { repo.pinAdd(it) }
        repo.applyStyle(ids[0], NoteStyles.resolve("neon"))
        repo.applyStyle(ids[1], NoteStyles.resolve("sticky"))
        val global = NoteWidgetStyle.DEFAULT
        val first = NoteStyles.effective(repo.get(ids[0])!!, global)
        val second = NoteStyles.effective(repo.get(ids[1])!!, global)
        assertEquals(NoteCardStyle.OUTLINED, first.cardStyle)
        assertEquals(NoteCardStyle.STICKY, second.cardStyle)
        assertEquals("neon", repo.get(ids[0])!!.styleId)
    }

    @Test
    fun legacySinglePinStillWorks() = runBlocking {
        val ids = makeNotes("a", "b")
        repo.pin(ids[0])
        repo.pin(ids[1]) // old API replaces the pin
        assertEquals(listOf(ids[1]), repo.pinnedNotes.first().map { it.id })
    }

    @Test
    fun createStyledNotePinsWhenThereIsRoom() = runBlocking {
        val id = repo.createStyledNote("T", "B", NoteStyles.resolve("quote"), NoteAppearance.DEFAULT)
        assertEquals(listOf(id), repo.pinnedNotes.first().map { it.id })
        assertEquals("quote", repo.get(id)!!.styleId)
    }
}
