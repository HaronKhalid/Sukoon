package com.lu4p.fokuslauncher.home.layout

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Persists layout and clock configuration in the launcher DataStore. Invalid data falls back to defaults. */
@Singleton
class HomeLayoutRepository(private val store: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.fokusLauncherPreferencesDataStore)

    val layoutFlow: Flow<HomeLayoutConfig> =
            store.data.map { HomeLayoutCodec.decodeOrDefault(it[LAYOUT_KEY]) }.distinctUntilChanged()

    val clockFlow: Flow<ClockSettings> =
            store.data.map { ClockSettingsCodec.decodeOrDefault(it[CLOCK_KEY]) }.distinctUntilChanged()

    suspend fun setLayout(config: HomeLayoutConfig) {
        store.edit { it[LAYOUT_KEY] = HomeLayoutCodec.encode(config) }
    }

    suspend fun setClock(settings: ClockSettings) {
        store.edit { it[CLOCK_KEY] = ClockSettingsCodec.encode(settings) }
    }

    companion object {
        /** Names are also listed in ConfigurationBackup so they travel with backups. */
        const val LAYOUT_KEY_NAME = "home_layout_config_v1"
        const val CLOCK_KEY_NAME = "clock_studio_config_v1"
        private val LAYOUT_KEY = stringPreferencesKey(LAYOUT_KEY_NAME)
        private val CLOCK_KEY = stringPreferencesKey(CLOCK_KEY_NAME)
    }
}
