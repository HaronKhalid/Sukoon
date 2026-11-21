package com.lu4p.fokuslauncher.lockscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.notes.NoteAppearance
import com.lu4p.fokuslauncher.notes.NoteDecoration
import com.lu4p.fokuslauncher.notes.NoteEntity
import com.lu4p.fokuslauncher.notes.NoteRepository
import com.lu4p.fokuslauncher.notes.NoteStyles
import com.lu4p.fokuslauncher.notes.NoteWidgetPreferences
import com.lu4p.fokuslauncher.notes.NoteWidgetStyle
import com.lu4p.fokuslauncher.notes.pinnedNotes
import com.lu4p.fokuslauncher.wallpaper.BuiltInWallpaper
import com.lu4p.fokuslauncher.wallpaper.WallpaperRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.lockNoteDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "lock_note_preferences")

enum class LockApplyResult { APPLIED, NO_NOTE, NOT_ALLOWED, FAILED }

/** Designer settings, stored locally only. */
@Singleton
class LockNotePreferences @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("lock_note_config")
    val flow: Flow<LockNoteConfig> = context.lockNoteDataStore.data.map { LockNoteConfig.decode(it[key]) }

    suspend fun get(): LockNoteConfig = flow.first()

    suspend fun set(config: LockNoteConfig) {
        context.lockNoteDataStore.edit { it[key] = config.coerced().encode() }
    }
}

/**
 * Renders the chosen note and, only when the user asks, sets it as the LOCK-screen wallpaper
 * (static snapshot) or posts an optional ongoing notification. A launcher cannot draw on the real
 * lock screen, so nothing here intercepts, overlays or fakes it.
 */
@Singleton
class LockNoteController
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val notes: NoteRepository,
        private val widgetPrefs: NoteWidgetPreferences,
        private val prefs: LockNotePreferences,
        private val typefaces: LockTypefaces,
        private val wallpapers: WallpaperRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pending: Job? = null

    /** The note the config points at, else the first pinned, else the newest note. */
    suspend fun resolveNote(config: LockNoteConfig): NoteEntity? {
        if (config.noteId != LockNoteConfig.NO_NOTE) notes.get(config.noteId)?.let { return it }
        return notes.pinnedNotes.first().firstOrNull() ?: notes.notes.first().firstOrNull()
    }

    private fun noteText(n: NoteEntity): String =
            listOf(n.title, n.body).filter { it.isNotBlank() }.joinToString("\n")

    /** Effective look for the designer: registry style (or the note own look) plus overrides. */
    fun lookFor(config: LockNoteConfig, note: NoteEntity, global: NoteWidgetStyle): Pair<NoteWidgetStyle, NoteDecoration> {
        val style = NoteStyles.find(config.styleId)
        val base = if (style == null) NoteStyles.effective(note, global) else NoteStyles.widgetStyleOf(style)
        var a = base.appearance.copy(alignment = config.alignment)
        if (config.fontFamily.isNotBlank()) a = a.copy(fontFamily = config.fontFamily)
        return base.copy(appearance = a).coerced() to
                (style?.decoration ?: NoteStyles.find(note.styleId)?.decoration ?: NoteDecoration.NONE)
    }

    private suspend fun backdropFor(config: LockNoteConfig): LockBackdrop =
            when (config.background) {
                LockBackgroundKind.SOLID -> LockBackdrop.Solid(config.color1)
                LockBackgroundKind.GRADIENT -> LockBackdrop.Gradient(config.color1, config.color2)
                LockBackgroundKind.BACKDROP ->
                        (BuiltInWallpaper.entries.firstOrNull { it.id == config.backdropId }
                                        ?: BuiltInWallpaper.DUSK)
                                .let { LockBackdrop.Gradient(it.top, it.bottom) }
                LockBackgroundKind.PHOTO ->
                        config.photoFile
                                .takeIf { it.isNotBlank() && !it.contains('/') && !it.contains('\\') }
                                ?.let { name ->
                                    wallpapers.recents().firstOrNull { it.name == name }
                                            ?.let { f: File -> wallpapers.decodeFile(f, 2048) }
                                }
                                ?.let { LockBackdrop.Photo(it) }
                        ?: LockBackdrop.Gradient(config.color1, config.color2)
            }

    /** Renders at [width] x [height]; null when there is no note to show. Caller owns the bitmap. */
    suspend fun render(config: LockNoteConfig, width: Int, height: Int): Bitmap? =
            withContext(Dispatchers.Default) {
                val note = resolveNote(config) ?: return@withContext null
                val global = widgetPrefs.style()
                val (look, decoration) = lookFor(config, note, global)
                val backdrop = backdropFor(config)
                val now = System.currentTimeMillis()
                val clock =
                        if (config.showClockLine)
                            LockClockLine(
                                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now)),
                                    DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)),
                            )
                        else null
                val stamp =
                        if (note.updatedAt > 0)
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(note.updatedAt))
                        else null
                val bmp =
                        NoteBitmapRenderer.render(
                                LockRenderSpec(
                                        width = width,
                                        height = height,
                                        text = noteText(note).ifBlank { context.getString(R.string.lock_empty_note) },
                                        look = look,
                                        decoration = decoration,
                                        stamp = stamp,
                                        backdrop = backdrop,
                                        position = config.position,
                                        scale = config.scale,
                                        clock = clock,
                                        typeface = typefaces.resolve(look.appearance.fontFamily, look.appearance.fontWeight),
                                )
                        )
                (backdrop as? LockBackdrop.Photo)?.bitmap?.recycle()
                bmp
            }

    /** Sets the snapshot as the LOCK wallpaper only (FLAG_LOCK). Some OEMs ignore it. */
    suspend fun applyToLockScreen(config: LockNoteConfig): LockApplyResult =
            withContext(Dispatchers.IO) {
                val m = context.resources.displayMetrics
                val bmp = render(config, m.widthPixels, m.heightPixels) ?: return@withContext LockApplyResult.NO_NOTE
                try {
                    val wm = WallpaperManager.getInstance(context)
                    if (!wm.isWallpaperSupported || !wm.isSetWallpaperAllowed) return@withContext LockApplyResult.NOT_ALLOWED
                    val id = wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
                    if (id > 0) LockApplyResult.APPLIED else LockApplyResult.FAILED
                } catch (_: SecurityException) {
                    LockApplyResult.NOT_ALLOWED
                } catch (_: Exception) {
                    LockApplyResult.FAILED
                } finally {
                    bmp.recycle()
                }
            }

    // ---- optional pinned notification (no foreground service) ----

    private fun ensureChannels(manager: NotificationManager) {
        fun make(id: String, name: Int, visibility: Int) {
            if (manager.getNotificationChannel(id) != null) return
            manager.createNotificationChannel(
                    NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_LOW).apply {
                        description = context.getString(R.string.lock_notif_channel_desc)
                        lockscreenVisibility = visibility
                        setShowBadge(false)
                    }
            )
        }
        make(CHANNEL_PRIVATE, R.string.lock_notif_channel_private, android.app.Notification.VISIBILITY_PRIVATE)
        make(CHANNEL_PUBLIC, R.string.lock_notif_channel_public, android.app.Notification.VISIBILITY_PUBLIC)
    }

    /** Posts or updates the ongoing note notification. Returns false if notifications are blocked. */
    suspend fun postNotification(config: LockNoteConfig): Boolean {
        val note = resolveNote(config) ?: return false
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        ensureChannels(nm)
        val publicOnLock = config.notificationOnLockScreen
        val full = NotificationText.body(noteText(note).ifBlank { context.getString(R.string.lock_empty_note) })
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi =
                launch?.let {
                    PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                }
        val n =
                NotificationCompat.Builder(context, if (publicOnLock) CHANNEL_PUBLIC else CHANNEL_PRIVATE)
                        .setSmallIcon(R.drawable.ic_launcher_mono)
                        .setContentTitle(note.title.ifBlank { context.getString(R.string.lock_notif_title) })
                        .setContentText(NotificationText.short(noteText(note)))
                        .setStyle(NotificationCompat.BigTextStyle().bigText(full))
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setSilent(true)
                        .setShowWhen(false)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                        .setVisibility(if (publicOnLock) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
                        .apply {
                            if (!publicOnLock) {
                                setPublicVersion(
                                        NotificationCompat.Builder(context, CHANNEL_PRIVATE)
                                                .setSmallIcon(R.drawable.ic_launcher_mono)
                                                .setContentTitle(context.getString(R.string.lock_notif_hidden))
                                                .build()
                                )
                            }
                            pi?.let { setContentIntent(it) }
                        }
                        .build()
        return runCatching {
                    // A different channel needs the old post removed first so visibility really changes.
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
                    true
                }
                .getOrDefault(false)
    }

    fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Called after a note is saved: refreshes the notification and (if enabled) the wallpaper. */
    fun onNoteSaved(id: Long) {
        pending?.cancel()
        pending =
                scope.launch {
                    delay(DEBOUNCE_MS)
                    val config = prefs.get()
                    if (!config.autoUpdate && !config.notificationOn) return@launch
                    val shown = resolveNote(config)?.id
                    if (shown != id) return@launch
                    if (config.notificationOn) postNotification(config)
                    if (config.autoUpdate) applyToLockScreen(config)
                }
    }

    companion object {
        const val CHANNEL_PRIVATE = "lock_note_private"
        const val CHANNEL_PUBLIC = "lock_note_public"
        const val NOTIFICATION_ID = 4231
        const val DEBOUNCE_MS = 2_000L
    }
}
