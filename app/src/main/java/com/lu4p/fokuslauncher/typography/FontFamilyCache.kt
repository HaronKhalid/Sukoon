package com.lu4p.fokuslauncher.typography

import android.content.Context
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.ui.theme.composeFontFamilyFromStoredName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide cache of Compose [FontFamily] instances keyed by stored font value, so font files
 * are opened once rather than on every recomposition. Any failure falls back to the supplied
 * launcher family (system default), never throws.
 */
@Singleton
class FontFamilyCache
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val customFontStore: CustomFontStore,
) {
    private val cache = ConcurrentHashMap<String, FontFamily>()

    /** Family for [storageValue], or [launcherFamily] when empty/unknown/unloadable. */
    fun familyFor(storageValue: String?, launcherFamily: FontFamily = FontFamily.Default): FontFamily {
        val choice = FontRegistry.resolve(storageValue)
        if (choice is FontChoice.FollowLauncher) return launcherFamily
        val key = storageValue.orEmpty().trim()
        cache[key]?.let { return it }
        val built = runCatching { build(choice) }.getOrNull() ?: return launcherFamily
        return cache.putIfAbsent(key, built) ?: built
    }

    /** Drops custom-font entries after an import or deletion replaced the file on disk. */
    fun invalidateCustomFonts() {
        cache.keys.filter { it.startsWith("custom:") }.forEach(cache::remove)
    }

    private fun build(choice: FontChoice): FontFamily? =
            when (choice) {
                FontChoice.FollowLauncher -> null
                is FontChoice.System -> composeFontFamilyFromStoredName(choice.familyName)
                is FontChoice.Custom ->
                        composeFontFamilyFromStoredName(choice.storageValue) {
                            customFontStore.resolveFile(it)
                        }.takeIf { it != FontFamily.Default }
                is FontChoice.Bundled -> bundledFamily(choice.font)
            }

    @OptIn(ExperimentalTextApi::class)
    private fun bundledFamily(font: BundledFont): FontFamily {
        val assets = context.assets
        // Fail early if the asset is missing so the caller can fall back to the launcher font.
        assets.open(font.assetPath).close()
        return FontFamily(
                BUNDLED_WEIGHTS.map { weight ->
                    Font(
                            path = font.assetPath,
                            assetManager = assets,
                            weight = FontWeight(weight),
                            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                    )
                }
        )
    }

    companion object {
        val BUNDLED_WEIGHTS = listOf(300, 400, 500, 600, 700)
    }
}
