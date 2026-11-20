package com.lu4p.fokuslauncher.notes

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Local-only note store. No network, account or sync. */
@Singleton
class NoteRepository(private val dao: NoteDao, private val clock: () -> Long) {

    @Inject constructor(dao: NoteDao) : this(dao, System::currentTimeMillis)

    val notes: Flow<List<NoteEntity>> = dao.observeAll()

    /** The pinned note, i.e. the one displayed on the home screen. */
    val pinnedNote: Flow<NoteEntity?> = dao.observePinned()

    suspend fun get(id: Long): NoteEntity? = dao.get(id)

    /** Creates an empty note and returns its id. */
    suspend fun create(appearance: NoteAppearance = NoteAppearance.DEFAULT): Long {
        val now = clock()
        return dao.upsert(
                NoteEntity(createdAt = now, updatedAt = now).withAppearance(appearance)
        )
    }

    /**
     * Saves text for [id]. A no-op when nothing changed (keeps the edit date stable) or when the
     * note no longer exists (so a late autosave cannot resurrect a deleted note).
     */
    suspend fun saveText(id: Long, title: String, body: String) {
        val existing = dao.get(id) ?: return
        if (existing.title == title && existing.body == body) return
        dao.upsert(existing.copy(title = title, body = body, updatedAt = clock()))
    }

    suspend fun saveAppearance(id: Long, appearance: NoteAppearance) {
        val existing = dao.get(id) ?: return
        dao.upsert(existing.withAppearance(appearance))
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** Deletes the note when both fields are blank (an abandoned new note). Returns true if deleted. */
    suspend fun deleteIfBlank(id: Long): Boolean {
        val existing = dao.get(id) ?: return false
        if (existing.title.isNotBlank() || existing.body.isNotBlank()) return false
        dao.delete(id)
        return true
    }

    /** Pins [id] to the home screen, unpinning any other note. Unknown ids are ignored. */
    suspend fun pin(id: Long) {
        if (dao.get(id) == null) return
        dao.pinOnly(id)
    }

    suspend fun unpin(id: Long) {
        val existing = dao.get(id) ?: return
        if (existing.pinned) dao.upsert(existing.copy(pinned = false))
    }
}
