package com.lu4p.fokuslauncher.notes

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Schema 8 -> 9: pin order and per-note style columns. Purely additive; existing rows keep data. */
object NoteMigrations {
    /** Exposed so a plain JVM test can check exactly what runs (no SQLite needed). */
    val SQL_8_TO_9: List<String> =
            listOf(
                    "ALTER TABLE `notes` ADD COLUMN `pinOrder` INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE `notes` ADD COLUMN `styleId` TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE `notes` ADD COLUMN `cardStyle` TEXT NOT NULL DEFAULT ''",
            )

    val migration8To9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    SQL_8_TO_9.forEach(db::execSQL)
                }
            }
}

/**
 * Pure rules for the pinned-notes list shown on the home screen (at most [MAX_PINNED]).
 * Legacy databases had one pinned note with pinOrder 0, which is simply the first entry.
 */
object PinModel {
    const val MAX_PINNED = 3

    /** Pinned ids in display order from raw rows (id, pinned, pinOrder). */
    fun orderedIds(rows: List<Triple<Long, Boolean, Int>>): List<Long> =
            rows.filter { it.second }.sortedWith(compareBy({ it.third }, { it.first })).map { it.first }
                    .take(MAX_PINNED)

    /** Adds [id] at the end; null when it would exceed [MAX_PINNED]. Already pinned is a no-op. */
    fun add(order: List<Long>, id: Long): List<Long>? =
            when {
                id in order -> order
                order.size >= MAX_PINNED -> null
                else -> order + id
            }

    fun remove(order: List<Long>, id: Long): List<Long> = order - id

    /** Replaces [old] with [new] in the same slot. If [new] is already pinned only [old] is removed. */
    fun replace(order: List<Long>, old: Long, new: Long): List<Long> =
            if (new in order) order - old else order.map { if (it == old) new else it }

    /** Moves [id] by [delta] (-1 up, +1 down), clamped. */
    fun move(order: List<Long>, id: Long, delta: Int): List<Long> {
        val i = order.indexOf(id)
        if (i < 0) return order
        val j = (i + delta).coerceIn(0, order.lastIndex)
        if (i == j) return order
        val m = order.toMutableList()
        m.removeAt(i)
        m.add(j, id)
        return m
    }
}
