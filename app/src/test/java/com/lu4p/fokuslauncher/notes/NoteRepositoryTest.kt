package com.lu4p.fokuslauncher.notes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory DAO so repository rules run on the plain JVM (no SQLite native library needed). */
private class FakeNoteDao : NoteDao() {
    private val rows = MutableStateFlow<Map<Long, NoteEntity>>(emptyMap())
    private var nextId = 1L

    override fun observeAll(): Flow<List<NoteEntity>> =
            rows.map { m ->
                m.values.sortedWith(
                        compareByDescending<NoteEntity> { it.pinned }
                                .thenByDescending { it.updatedAt }
                                .thenByDescending { it.id }
                )
            }

    override fun observePinned(): Flow<NoteEntity?> =
            rows.map { m -> m.values.firstOrNull { it.pinned } }

    override suspend fun get(id: Long): NoteEntity? = rows.value[id]

    override suspend fun upsert(note: NoteEntity): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        rows.value = rows.value + (id to note.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        rows.value = rows.value - id
    }

    override suspend fun clearPins() {
        rows.value = rows.value.mapValues { it.value.copy(pinned = false) }
    }

    override suspend fun markPinned(id: Long) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(pinned = true)) }
    }

    override suspend fun pinOnly(id: Long) {
        clearPins()
        markPinned(id)
    }
}

class NoteRepositoryTest {
    private var now = 1_000L
    private val dao = FakeNoteDao()
    private val repo = NoteRepository(dao) { now }

    @Test
    fun createSaveAndReadBack() = runBlocking {
        val id = repo.create()
        now = 2_000L
        repo.saveText(id, "عنوان", "متن with English")
        val note = repo.get(id)!!
        assertEquals("عنوان", note.title)
        assertEquals("متن with English", note.body)
        assertEquals(1_000L, note.createdAt)
        assertEquals(2_000L, note.updatedAt)
    }

    @Test
    fun unchangedSaveKeepsEditDate() = runBlocking {
        val id = repo.create()
        repo.saveText(id, "t", "b")
        now = 9_000L
        repo.saveText(id, "t", "b")
        assertEquals(1_000L, repo.get(id)!!.updatedAt)
    }

    @Test
    fun lateAutosaveDoesNotResurrectDeletedNote() = runBlocking {
        val id = repo.create()
        repo.delete(id)
        repo.saveText(id, "ghost", "ghost")
        repo.saveAppearance(id, NoteAppearance(fontSizeSp = 30f))
        assertNull(repo.get(id))
        assertTrue(repo.notes.first().isEmpty())
    }

    @Test
    fun onlyOneNoteIsPinnedAtATime() = runBlocking {
        val a = repo.create()
        val b = repo.create()
        repo.pin(a)
        repo.pin(b)
        assertFalse(repo.get(a)!!.pinned)
        assertTrue(repo.get(b)!!.pinned)
        assertEquals(b, repo.pinnedNote.first()?.id)
        assertEquals(1, repo.notes.first().count { it.pinned })
    }

    @Test
    fun pinUnknownIdKeepsExistingPin() = runBlocking {
        val a = repo.create()
        repo.pin(a)
        repo.pin(999L)
        assertEquals(a, repo.pinnedNote.first()?.id)
    }

    @Test
    fun unpinClearsHomeNote() = runBlocking {
        val a = repo.create()
        repo.pin(a)
        repo.unpin(a)
        assertNull(repo.pinnedNote.first())
    }

    @Test
    fun deletingPinnedNoteLeavesNothingOnHome() = runBlocking {
        val a = repo.create()
        repo.pin(a)
        repo.delete(a)
        assertNull(repo.pinnedNote.first())
    }

    @Test
    fun blankNewNoteIsDiscardedButTextNoteIsKept() = runBlocking {
        val blank = repo.create()
        val kept = repo.create()
        repo.saveText(kept, "", "  hello ")
        assertTrue(repo.deleteIfBlank(blank))
        assertFalse(repo.deleteIfBlank(kept))
        assertNull(repo.get(blank))
        assertNotNull(repo.get(kept))
    }

    @Test
    fun appearanceIsPerNoteAndClamped() = runBlocking {
        val a = repo.create()
        val b = repo.create()
        repo.saveAppearance(a, NoteAppearance(fontFamily = "bundled:inter", fontSizeSp = 999f))
        assertEquals("bundled:inter", repo.get(a)!!.appearance().fontFamily)
        assertEquals(NoteAppearance.MAX_SIZE_SP, repo.get(a)!!.appearance().fontSizeSp)
        assertEquals("", repo.get(b)!!.appearance().fontFamily)
    }

    @Test
    fun listOrdersPinnedFirstThenMostRecentlyEdited() = runBlocking {
        val a = repo.create()
        now = 2_000
        val b = repo.create()
        now = 3_000
        val c = repo.create()
        now = 4_000
        repo.saveText(a, "a", "a")
        repo.pin(b)
        assertEquals(listOf(b, a, c), repo.notes.first().map { it.id })
    }
}
