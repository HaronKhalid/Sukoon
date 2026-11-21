package com.lu4p.fokuslauncher.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory DataStore: the real file-backed one renames over an existing file, which fails on
 * Windows JVMs (unrelated to launcher behaviour), so persistence logic is tested against this.
 */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

class ThemeStoreTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val store = ThemeStore(dataStore)

    private fun custom(id: String, name: String = id) = BuiltInThemes.Ocean.copy(id = id, name = name)

    @Test
    fun `fresh store applies the Sukoon default theme`() = runBlocking {
        val s = store.state.first()
        assertNull(s.selectedId)
        assertEquals(BuiltInThemes.SukoonDefault, s.applied)
        assertEquals(BuiltInThemes.all, s.allThemes)
    }

    @Test
    fun `selecting a built-in persists and resolves`() = runBlocking {
        store.select("cream")
        assertEquals(BuiltInThemes.Cream, store.appliedTheme.first())
        store.reset()
        assertNull(store.appliedTheme.first())
        assertTrue(store.state.first().isClassic)
    }

    @Test
    fun `unknown selected id fails safe to the default theme`() = runBlocking {
        store.select("deleted-long-ago")
        assertEquals(BuiltInThemes.SukoonDefault, store.appliedTheme.first())
    }

    @Test
    fun `custom themes persist and can be applied`() = runBlocking {
        assertTrue(store.saveCustom(custom("custom-1", "Mine")))
        store.select("custom-1")
        val s = store.state.first()
        assertEquals(listOf("Mine"), s.customThemes.map { it.name })
        assertEquals("custom-1", s.applied?.id)
    }

    @Test
    fun `saving built-in ids or invalid themes is refused`() = runBlocking {
        assertFalse(store.saveCustom(BuiltInThemes.Cream))
        assertFalse(store.saveCustom(custom("bad", " ")))
        assertTrue(store.state.first().customThemes.isEmpty())
    }

    @Test
    fun `saving the same id updates rather than duplicates`() = runBlocking {
        store.saveCustom(custom("custom-1", "One"))
        store.saveCustom(custom("custom-1", "Uno"))
        assertEquals(listOf("Uno"), store.state.first().customThemes.map { it.name })
    }

    @Test
    fun `rename works for custom and ignores unknown or invalid`() = runBlocking {
        store.saveCustom(custom("custom-1", "One"))
        assertTrue(store.rename("custom-1", "  Renamed "))
        assertEquals("Renamed", store.state.first().customThemes.single().name)
        assertFalse(store.rename("custom-1", ""))
        assertFalse(store.rename("missing", "x"))
        assertFalse(store.rename("cream", "x"))
    }

    @Test
    fun `deleting the applied custom theme resets the selection`() = runBlocking {
        store.saveCustom(custom("custom-1"))
        store.saveCustom(custom("custom-2"))
        store.select("custom-1")
        store.delete("custom-1")
        val s = store.state.first()
        assertNull(s.selectedId)
        assertEquals(listOf("custom-2"), s.customThemes.map { it.id })
    }

    @Test
    fun `deleting another theme keeps the selection`() = runBlocking {
        store.saveCustom(custom("custom-1"))
        store.saveCustom(custom("custom-2"))
        store.select("custom-2")
        store.delete("custom-1")
        assertEquals("custom-2", store.state.first().selectedId)
    }

    @Test
    fun `custom theme count is capped`() = runBlocking {
        repeat(ThemeStore.MAX_CUSTOM_THEMES) { assertTrue(store.saveCustom(custom("custom-$it"))) }
        assertFalse(store.saveCustom(custom("custom-extra")))
        assertEquals(ThemeImportResult.LimitReached, store.importTheme(ThemeCodec.encode(custom("x"))))
        assertTrue(store.saveCustom(custom("custom-0", "renamed")))
    }

    @Test
    fun `import assigns a fresh id and never overwrites built-ins`() = runBlocking {
        val json = ThemeCodec.encode(BuiltInThemes.Cream)
        val r1 = store.importTheme(json) as ThemeImportResult.Imported
        val r2 = store.importTheme(json) as ThemeImportResult.Imported
        assertEquals("custom-1", r1.theme.id)
        assertEquals("custom-2", r2.theme.id)
        val s = store.state.first()
        assertEquals(BuiltInThemes.Cream, s.allThemes.first { it.id == "cream" })
        assertEquals(2, s.customThemes.size)
    }

    @Test
    fun `invalid import changes nothing`() = runBlocking {
        val r = store.importTheme("{\"schemaVersion\":1,\"id\":\"x\"}")
        assertTrue(r is ThemeImportResult.Rejected)
        assertTrue(store.state.first().customThemes.isEmpty())
    }

    @Test
    fun `corrupt stored data fails safe`() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("theme_custom_themes_v1")] = "{not json"
            it[stringPreferencesKey("theme_selected_id")] = "custom-9"
        }
        val s = store.state.first()
        assertTrue(s.customThemes.isEmpty())
        assertEquals(BuiltInThemes.SukoonDefault, s.applied)
    }

    @Test
    fun `stored custom themes that shadow built-in ids are ignored`() = runBlocking {
        dataStore.edit {
            it[stringPreferencesKey("theme_custom_themes_v1")] =
                    ThemeCodec.encodeList(listOf(BuiltInThemes.Cream.copy(name = "Hijack"), custom("custom-1")))
        }
        assertEquals(listOf("custom-1"), store.state.first().customThemes.map { it.id })
    }
}
