package com.lu4p.fokuslauncher.notes

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * An in-app note. Appearance columns are stored per note so fonts and sizes never couple to the
 * clock or app-label fonts. Colours are ARGB ints; 0 in [textColor] means "theme default" and
 * 0 in [backgroundColor] means transparent.
 */
@Entity(tableName = "notes")
data class NoteEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String = "",
        val body: String = "",
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
        /** At most one note is pinned; the pinned note is the one shown on the home screen. */
        val pinned: Boolean = false,
        val fontFamily: String = NoteAppearance.DEFAULT.fontFamily,
        val fontSizeSp: Float = NoteAppearance.DEFAULT.fontSizeSp,
        val fontWeight: Int = NoteAppearance.DEFAULT.fontWeight,
        val textColor: Int = NoteAppearance.DEFAULT.textColor,
        val alignment: Int = NoteAppearance.DEFAULT.alignment.ordinal,
        val lineSpacing: Float = NoteAppearance.DEFAULT.lineSpacing,
        val backgroundColor: Int = NoteAppearance.DEFAULT.backgroundColor,
        val paddingDp: Int = NoteAppearance.DEFAULT.paddingDp,
        val maxLines: Int = NoteAppearance.DEFAULT.maxLines,
)

@Dao
abstract class NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC, id DESC")
    abstract fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE pinned = 1 LIMIT 1")
    abstract fun observePinned(): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun get(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("UPDATE notes SET pinned = 0 WHERE pinned = 1")
    abstract suspend fun clearPins()

    @Query("UPDATE notes SET pinned = 1 WHERE id = :id")
    abstract suspend fun markPinned(id: Long)

    /** Atomically makes [id] the only pinned note. */
    @Transaction
    open suspend fun pinOnly(id: Long) {
        clearPins()
        markPinned(id)
    }
}
