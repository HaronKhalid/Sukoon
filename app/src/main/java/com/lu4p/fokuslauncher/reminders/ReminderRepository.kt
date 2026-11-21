package com.lu4p.fokuslauncher.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A separate DataStore file (not Room), so reminders never take part in database migrations. */
private val Context.remindersDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

data class ReminderSettings(
        /** Lock screen shows the full reminder text. Default off: the lock screen only says "Reminder". */
        val showFullTextOnLock: Boolean = false,
        /** User opted in to show calendar events (still needs the READ_CALENDAR permission). */
        val calendarEnabled: Boolean = false,
)

@Singleton
class ReminderRepository(private val store: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.remindersDataStore)

    val reminders: Flow<List<Reminder>> =
            store.data.map { ReminderCodec.decode(it[ITEMS]) }.distinctUntilChanged()

    val settings: Flow<ReminderSettings> =
            store.data.map { ReminderSettings(it[SHOW_FULL_TEXT] ?: false, it[CALENDAR] ?: false) }
                    .distinctUntilChanged()

    suspend fun snapshot(): List<Reminder> = reminders.first()

    suspend fun settingsSnapshot(): ReminderSettings = settings.first()

    /** Atomic read-modify-write; the result is capped to [MAX_REMINDERS]. */
    suspend fun update(transform: (List<Reminder>) -> List<Reminder>) {
        store.edit { prefs ->
            val next = ReminderMath.cap(transform(ReminderCodec.decode(prefs[ITEMS])))
            prefs[ITEMS] = ReminderCodec.encode(next)
        }
    }

    suspend fun notifiedUpTo(): Long = store.data.first()[NOTIFIED_UP_TO] ?: 0L

    suspend fun setNotifiedUpTo(ms: Long) {
        store.edit { it[NOTIFIED_UP_TO] = ms }
    }

    suspend fun setShowFullTextOnLock(value: Boolean) {
        store.edit { it[SHOW_FULL_TEXT] = value }
    }

    suspend fun setCalendarEnabled(value: Boolean) {
        store.edit { it[CALENDAR] = value }
    }

    companion object {
        private val ITEMS = stringPreferencesKey("reminders_v1")
        private val NOTIFIED_UP_TO = longPreferencesKey("notified_up_to")
        private val SHOW_FULL_TEXT = booleanPreferencesKey("lock_show_full_text")
        private val CALENDAR = booleanPreferencesKey("calendar_enabled")
    }
}
