package com.lu4p.fokuslauncher.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lu4p.fokuslauncher.data.local.fokusLauncherPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Persisted theme state. [selectedId] null means "nothing chosen yet", which shows Sukoon's own
 * default theme; [BuiltInThemes.ID_CLASSIC] keeps the legacy Fokus look.
 */
data class ThemeState(
        val selectedId: String?,
        val customThemes: List<ThemeDefinition>,
) {
    val allThemes: List<ThemeDefinition>
        get() = BuiltInThemes.all + customThemes

    /** True when the user explicitly chose the legacy look. */
    val isClassic: Boolean get() = selectedId == BuiltInThemes.ID_CLASSIC

    /**
     * The theme to paint: the chosen one, Sukoon's default when nothing (valid) is chosen, or null
     * only for the explicit classic look.
     */
    val applied: ThemeDefinition?
        get() =
                if (isClassic) null
                else selectedId?.let { id -> allThemes.firstOrNull { it.id == id } }
                        ?: BuiltInThemes.SukoonDefault
}

sealed interface ThemeImportResult {
    data class Imported(val theme: ThemeDefinition) : ThemeImportResult

    data class Rejected(val reason: String) : ThemeImportResult

    data object LimitReached : ThemeImportResult
}

/** DataStore persistence for the selected theme and user-created custom themes. */
@Singleton
class ThemeStore internal constructor(private val dataStore: DataStore<Preferences>) {

    @Inject
    constructor(
            @ApplicationContext context: Context
    ) : this(context.fokusLauncherPreferencesDataStore)

    val state: Flow<ThemeState> =
            dataStore.data
                    .map { prefs ->
                        ThemeState(
                                selectedId = prefs[SELECTED_KEY]?.takeIf { it.isNotBlank() },
                                customThemes =
                                        ThemeCodec.decodeList(prefs[CUSTOM_KEY]).filterNot {
                                            BuiltInThemes.isBuiltInId(it.id)
                                        }.distinctBy { it.id },
                        )
                    }
                    .distinctUntilChanged()

    /** Applied theme for the Compose theme; null = unchanged legacy look. */
    val appliedTheme: Flow<ThemeDefinition?> = state.map { it.applied }.distinctUntilChanged()

    suspend fun select(id: String) {
        dataStore.edit { it[SELECTED_KEY] = id }
    }

    /** Returns to the legacy (Fokus-like) launcher look. */
    suspend fun reset() {
        dataStore.edit { it[SELECTED_KEY] = BuiltInThemes.ID_CLASSIC }
    }

    /** Clock face id suggested by the last applied style (read by the clock engine; may be unknown). */
    val clockFaceId: Flow<String> =
            dataStore.data.map { ClockFaces.resolve(it[CLOCK_FACE_KEY]) }.distinctUntilChanged()

    suspend fun setClockFaceId(id: String) {
        dataStore.edit { it[CLOCK_FACE_KEY] = ClockFaces.resolve(id) }
    }

    /** Whether the one-time "Choose your style" sheet was already shown or skipped. */
    val stylePromptSeen: Flow<Boolean> =
            dataStore.data.map { it[STYLE_PROMPT_KEY] == true }.distinctUntilChanged()

    suspend fun markStylePromptSeen() {
        dataStore.edit { it[STYLE_PROMPT_KEY] = true }
    }

    /**
     * Inserts or replaces a custom theme. Returns false (and writes nothing) for built-in ids,
     * invalid themes, or when [MAX_CUSTOM_THEMES] would be exceeded.
     */
    suspend fun saveCustom(theme: ThemeDefinition): Boolean {
        if (BuiltInThemes.isBuiltInId(theme.id) || !ThemeCodec.isValid(theme)) return false
        var ok = true
        dataStore.edit { prefs ->
            val current = ThemeCodec.decodeList(prefs[CUSTOM_KEY])
            val exists = current.any { it.id == theme.id }
            if (!exists && current.size >= MAX_CUSTOM_THEMES) {
                ok = false
            } else {
                val next = if (exists) current.map { if (it.id == theme.id) theme else it } else current + theme
                prefs[CUSTOM_KEY] = ThemeCodec.encodeList(next)
            }
        }
        return ok
    }

    suspend fun rename(id: String, newName: String): Boolean {
        val name = newName.trim()
        var ok = false
        dataStore.edit { prefs ->
            val current = ThemeCodec.decodeList(prefs[CUSTOM_KEY])
            val target = current.firstOrNull { it.id == id } ?: return@edit
            val renamed = target.copy(name = name)
            if (!ThemeCodec.isValid(renamed)) return@edit
            prefs[CUSTOM_KEY] = ThemeCodec.encodeList(current.map { if (it.id == id) renamed else it })
            ok = true
        }
        return ok
    }

    /** Deletes a custom theme; if it was applied the selection is reset. */
    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = ThemeCodec.decodeList(prefs[CUSTOM_KEY])
            prefs[CUSTOM_KEY] = ThemeCodec.encodeList(current.filterNot { it.id == id })
            if (prefs[SELECTED_KEY] == id) prefs.remove(SELECTED_KEY)
        }
    }

    /** Validates [json] and stores it as a new custom theme under a freshly generated id. */
    suspend fun importTheme(json: String): ThemeImportResult {
        val parsed =
                when (val r = ThemeCodec.decode(json)) {
                    is ThemeParseResult.Failure -> return ThemeImportResult.Rejected(r.reason)
                    is ThemeParseResult.Success -> r.theme
                }
        var result: ThemeImportResult = ThemeImportResult.LimitReached
        dataStore.edit { prefs ->
            val current = ThemeCodec.decodeList(prefs[CUSTOM_KEY])
            if (current.size >= MAX_CUSTOM_THEMES) return@edit
            val fresh = parsed.copy(id = newCustomId(current.map { it.id }.toSet()))
            prefs[CUSTOM_KEY] = ThemeCodec.encodeList(current + fresh)
            result = ThemeImportResult.Imported(fresh)
        }
        return result
    }

    companion object {
        const val MAX_CUSTOM_THEMES = 20
        private val SELECTED_KEY = stringPreferencesKey("theme_selected_id")
        const val CLOCK_FACE_KEY_NAME = "clock_face_id"
        private val CLOCK_FACE_KEY = stringPreferencesKey(CLOCK_FACE_KEY_NAME)
        private val STYLE_PROMPT_KEY = booleanPreferencesKey("style_prompt_seen")
        private val CUSTOM_KEY = stringPreferencesKey("theme_custom_themes_v1")

        fun newCustomId(taken: Set<String>): String {
            var n = 1
            while ("custom-$n" in taken) n++
            return "custom-$n"
        }
    }
}
