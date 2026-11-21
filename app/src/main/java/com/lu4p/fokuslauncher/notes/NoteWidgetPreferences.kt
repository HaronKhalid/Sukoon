package com.lu4p.fokuslauncher.notes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noteWidgetDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "note_widget_preferences")

/** Global default note style plus the one-time sample-note flag. Local only. */
@Singleton
class NoteWidgetPreferences
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val styleKey = stringPreferencesKey("note_widget_style")
    private val seededKey = booleanPreferencesKey("sample_seeded")
    private val defaultStyleIdKey = stringPreferencesKey("default_note_style_id")

    val styleFlow: Flow<NoteWidgetStyle> =
            context.noteWidgetDataStore.data.map { NoteWidgetStyle.decode(it[styleKey]) }

    suspend fun style(): NoteWidgetStyle = styleFlow.first()

    suspend fun setStyle(style: NoteWidgetStyle) {
        context.noteWidgetDataStore.edit { it[styleKey] = style.coerced().encode() }
    }

    /** Registry id of the global default note design (empty when styled by hand). */
    val defaultStyleIdFlow: Flow<String> =
            context.noteWidgetDataStore.data.map { it[defaultStyleIdKey].orEmpty() }

    suspend fun setDefaultStyleId(id: String) {
        context.noteWidgetDataStore.edit { it[defaultStyleIdKey] = id }
    }

    suspend fun sampleSeeded(): Boolean = context.noteWidgetDataStore.data.first()[seededKey] == true

    suspend fun markSampleSeeded() {
        context.noteWidgetDataStore.edit { it[seededKey] = true }
    }
}
