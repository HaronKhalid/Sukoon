package com.lu4p.fokuslauncher.lockscreen

import com.lu4p.fokuslauncher.notes.NoteAlignment
import java.net.URLDecoder
import java.net.URLEncoder

enum class LockBackgroundKind { SOLID, GRADIENT, BACKDROP, PHOTO }

/**
 * Everything the lock-screen note designer remembers. Stored on the device as one versioned
 * pipe-delimited string; [decode] never throws and falls back to defaults.
 */
data class LockNoteConfig(
        /** Note to show; [NO_NOTE] uses the first pinned note, else the newest note. */
        val noteId: Long = NO_NOTE,
        /** Registry style id, or empty to use the note's own look. */
        val styleId: String = "minimal",
        /** Font override (stored font value); empty keeps the style font. */
        val fontFamily: String = "",
        val alignment: NoteAlignment = NoteAlignment.CENTER,
        val position: LockPosition = LockPosition.CENTER,
        val background: LockBackgroundKind = LockBackgroundKind.GRADIENT,
        val color1: Int = 0xFF0A0A14.toInt(),
        val color2: Int = 0xFF2A1638.toInt(),
        /** Id of a built-in backdrop (wallpaper.BuiltInWallpaper). */
        val backdropId: String = "dusk",
        /** File name of a picked photo inside the app wallpaper folder. */
        val photoFile: String = "",
        val showClockLine: Boolean = false,
        /** Text size multiplier, 0.5-2. */
        val scale: Float = 1f,
        /** Re-render the wallpaper after a note is saved (still only while the app runs). */
        val autoUpdate: Boolean = false,
        val notificationOn: Boolean = false,
        /** User acknowledged that note text is readable on the lock screen. */
        val notificationOnLockScreen: Boolean = false,
) {
    fun coerced(): LockNoteConfig = copy(scale = scale.coerceIn(0.5f, 2f))

    fun encode(): String =
            listOf(
                            VERSION,
                            noteId,
                            URLEncoder.encode(styleId, "UTF-8"),
                            URLEncoder.encode(fontFamily, "UTF-8"),
                            alignment.ordinal,
                            position.ordinal,
                            background.ordinal,
                            color1,
                            color2,
                            URLEncoder.encode(backdropId, "UTF-8"),
                            URLEncoder.encode(photoFile, "UTF-8"),
                            if (showClockLine) 1 else 0,
                            scale,
                            if (autoUpdate) 1 else 0,
                            if (notificationOn) 1 else 0,
                            if (notificationOnLockScreen) 1 else 0,
                    )
                    .joinToString("|")

    companion object {
        const val VERSION = "l1"
        const val NO_NOTE = -1L
        val DEFAULT = LockNoteConfig()

        fun decode(raw: String?): LockNoteConfig {
            if (raw.isNullOrBlank()) return DEFAULT
            val p = raw.split("|")
            if (p.firstOrNull() != VERSION) return DEFAULT
            return runCatching {
                        fun s(i: Int, d: String) = p.getOrNull(i)?.let { URLDecoder.decode(it, "UTF-8") } ?: d
                        fun n(i: Int, d: Int) = p.getOrNull(i)?.toIntOrNull() ?: d
                        val d = DEFAULT
                        LockNoteConfig(
                                        noteId = p.getOrNull(1)?.toLongOrNull() ?: NO_NOTE,
                                        styleId = s(2, d.styleId),
                                        fontFamily = s(3, d.fontFamily),
                                        alignment = NoteAlignment.entries.getOrElse(n(4, 1)) { d.alignment },
                                        position = LockPosition.entries.getOrElse(n(5, 1)) { d.position },
                                        background =
                                                LockBackgroundKind.entries.getOrElse(n(6, 1)) { d.background },
                                        color1 = n(7, d.color1),
                                        color2 = n(8, d.color2),
                                        backdropId = s(9, d.backdropId),
                                        photoFile = s(10, d.photoFile),
                                        showClockLine = n(11, 0) == 1,
                                        scale = p.getOrNull(12)?.toFloatOrNull()?.takeIf { it.isFinite() } ?: 1f,
                                        autoUpdate = n(13, 0) == 1,
                                        notificationOn = n(14, 0) == 1,
                                        notificationOnLockScreen = n(15, 0) == 1,
                                )
                                .coerced()
                    }
                    .getOrDefault(DEFAULT)
        }
    }
}
