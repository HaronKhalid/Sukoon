package com.lu4p.fokuslauncher.typography

import com.lu4p.fokuslauncher.data.model.LauncherFontPreferences

/** Launcher text elements that each carry their own, independent font choice. */
enum class TextElement(val key: String) {
    CLOCK("clock"),
    DATE("date"),
    APP_LABELS("app_labels"),
    NOTES("notes"),
}

/** Script coverage hint shown in the picker; not used for shaping. */
enum class FontScript { LATIN, ARABIC_NASKH, URDU_NASTALIQ }

/** A font file shipped in `assets/fonts`. All bundled fonts are SIL OFL 1.1 and unmodified. */
data class BundledFont(
        val id: String,
        val label: String,
        val assetPath: String,
        val script: FontScript,
        val licenseAssetPath: String,
)

/** How a stored font value is resolved. Parsing is pure so it is unit-testable. */
sealed class FontChoice {
    /** Empty storage: follow the launcher-wide font. */
    data object FollowLauncher : FontChoice()
    data class System(val familyName: String) : FontChoice()
    data class Bundled(val font: BundledFont) : FontChoice()
    data class Custom(val storageValue: String) : FontChoice()
}

object FontRegistry {
    const val BUNDLED_PREFIX = "bundled:"

    val bundledFonts: List<BundledFont> =
            listOf(
                    BundledFont(
                            id = "inter",
                            label = "Inter",
                            assetPath = "fonts/Inter-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Inter-OFL.txt",
                    ),
                    BundledFont(
                            id = "noto_naskh_arabic",
                            label = "Noto Naskh Arabic",
                            assetPath = "fonts/NotoNaskhArabic-Variable.ttf",
                            script = FontScript.ARABIC_NASKH,
                            licenseAssetPath = "fonts/licenses/NotoNaskhArabic-OFL.txt",
                    ),
                    BundledFont(
                            id = "noto_nastaliq_urdu",
                            label = "Noto Nastaliq Urdu",
                            assetPath = "fonts/NotoNastaliqUrdu-Variable.ttf",
                            script = FontScript.URDU_NASTALIQ,
                            licenseAssetPath = "fonts/licenses/NotoNastaliqUrdu-OFL.txt",
                    ),
            )

    /** Generic Android families offered without bundling anything. */
    val systemFamilies: List<String> = listOf("sans-serif", "serif", "monospace", "cursive")

    fun storageValueFor(font: BundledFont): String = BUNDLED_PREFIX + font.id

    fun findBundled(id: String): BundledFont? = bundledFonts.firstOrNull { it.id == id }

    /**
     * Resolves a stored value. Unknown bundled ids (for example from a newer backup) degrade to
     * [FontChoice.FollowLauncher] instead of failing.
     */
    fun resolve(storageValue: String?): FontChoice {
        val value = LauncherFontPreferences.normalizeFontFamilyFromStorage(storageValue)
        return when {
            value.isEmpty() -> FontChoice.FollowLauncher
            value.startsWith(BUNDLED_PREFIX) ->
                    findBundled(value.removePrefix(BUNDLED_PREFIX))?.let { FontChoice.Bundled(it) }
                            ?: FontChoice.FollowLauncher
            LauncherFontPreferences.isCustomFont(value) -> FontChoice.Custom(value)
            else -> FontChoice.System(value)
        }
    }
}
