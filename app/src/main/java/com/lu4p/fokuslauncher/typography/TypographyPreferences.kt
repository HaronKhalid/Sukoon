package com.lu4p.fokuslauncher.typography

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.typographyDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "typography_preferences")

/** Per-element font choice. An empty value follows the launcher-wide font. Local only. */
@Singleton
class TypographyPreferences
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private fun key(element: TextElement) = stringPreferencesKey("font_${element.key}")

    val allFontsFlow: Flow<Map<TextElement, String>> =
            context.typographyDataStore.data.map { prefs ->
                TextElement.entries.associateWith { prefs[key(it)].orEmpty() }
            }

    suspend fun setFont(element: TextElement, storageValue: String) {
        context.typographyDataStore.edit { prefs ->
            if (storageValue.isEmpty()) prefs.remove(key(element))
            else prefs[key(element)] = storageValue
        }
    }
}
