package com.lu4p.fokuslauncher.notes

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local-only note store. No network, account or sync. */
@Singleton
class NoteRepository(internal val dao: NoteDao, private val clock: () -> Long) {

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

    suspend fun count(): Int = dao.getAll().size

    /** Applies [appearance] to every note's text look (the pinned one included). */
    suspend fun applyAppearanceToAll(appearance: NoteAppearance) {
        dao.getAll().forEach { dao.upsert(it.withAppearance(appearance)) }
    }

    /** Creates the editable sample note, pinned to home. Returns its id. */
    suspend fun createPinned(title: String, body: String, appearance: NoteAppearance): Long {
        val id = create(appearance)
        saveText(id, title, body)
        pin(id)
        return id
    }
}

/** Multi-pin and per-note style operations (additive; the single-pin API above is unchanged). */
val NoteRepository.pinnedNotes: Flow<List<NoteEntity>>
    get() = notes.map { all -> pinnedInOrder(all) }

internal fun pinnedInOrder(all: List<NoteEntity>): List<NoteEntity> {
    val byId = all.associateBy { it.id }
    return PinModel.orderedIds(all.map { Triple(it.id, it.pinned, it.pinOrder) }).mapNotNull(byId::get)
}

private suspend fun NoteRepository.writePins(order: List<Long>) {
    val all = dao.getAll()
    all.forEach { n ->
        val idx = order.indexOf(n.id)
        val want = if (idx >= 0) n.copy(pinned = true, pinOrder = idx) else n.copy(pinned = false, pinOrder = 0)
        if (want != n) dao.upsert(want)
    }
}

private fun NoteRepository.currentOrder(all: List<NoteEntity>): List<Long> = pinnedInOrder(all).map { it.id }

/** Pins [id] as an additional home note. Returns false when [PinModel.MAX_PINNED] are already pinned. */
suspend fun NoteRepository.pinAdd(id: Long): Boolean {
    if (dao.get(id) == null) return false
    val next = PinModel.add(currentOrder(dao.getAll()), id) ?: return false
    writePins(next)
    return true
}

suspend fun NoteRepository.unpinOne(id: Long) {
    writePins(PinModel.remove(currentOrder(dao.getAll()), id))
}

suspend fun NoteRepository.replacePin(old: Long, new: Long) {
    if (dao.get(new) == null) return
    writePins(PinModel.replace(currentOrder(dao.getAll()), old, new))
}

suspend fun NoteRepository.movePin(id: Long, delta: Int) {
    writePins(PinModel.move(currentOrder(dao.getAll()), id, delta))
}

/** Saves a note's own text look and card options together. */
suspend fun NoteRepository.saveLook(id: Long, look: NoteWidgetStyle, styleId: String? = null) {
    val existing = dao.get(id) ?: return
    val v = look.coerced()
    val updated = existing.withAppearance(v.appearance).copy(cardStyle = v.encode())
    dao.upsert(if (styleId != null) updated.copy(styleId = styleId) else updated)
}

/** Applies a registry style to one note. */
suspend fun NoteRepository.applyStyle(id: Long, style: NoteStyle) {
    val existing = dao.get(id) ?: return
    dao.upsert(NoteStyles.applyTo(existing, style))
}

/** Creates a note with [style] (or the global [fallback] look) and pins it when there is room. */
suspend fun NoteRepository.createStyledNote(
        title: String,
        body: String,
        style: NoteStyle?,
        fallback: NoteAppearance,
        pin: Boolean = true,
): Long {
    val id = create(fallback)
    saveText(id, title, body)
    style?.let { applyStyle(id, it) }
    if (pin) pinAdd(id)
    return id
}
