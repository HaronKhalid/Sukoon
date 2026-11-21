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
                    BundledFont(
                            id = "montserrat",
                            label = "Montserrat",
                            assetPath = "fonts/Montserrat-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Montserrat-OFL.txt",
                    ),
                    BundledFont(
                            id = "poppins",
                            label = "Poppins",
                            assetPath = "fonts/Poppins-Regular.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Poppins-OFL.txt",
                    ),
                    BundledFont(
                            id = "poppins_bold",
                            label = "Poppins Bold",
                            assetPath = "fonts/Poppins-Bold.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Poppins-OFL.txt",
                    ),
                    BundledFont(
                            id = "outfit",
                            label = "Outfit",
                            assetPath = "fonts/Outfit-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Outfit-OFL.txt",
                    ),
                    BundledFont(
                            id = "nunito",
                            label = "Nunito",
                            assetPath = "fonts/Nunito-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Nunito-OFL.txt",
                    ),
                    BundledFont(
                            id = "fredoka",
                            label = "Fredoka",
                            assetPath = "fonts/Fredoka-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Fredoka-OFL.txt",
                    ),
                    BundledFont(
                            id = "bebas_neue",
                            label = "Bebas Neue",
                            assetPath = "fonts/BebasNeue-Regular.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/BebasNeue-OFL.txt",
                    ),
                    BundledFont(
                            id = "caveat",
                            label = "Caveat",
                            assetPath = "fonts/Caveat-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Caveat-OFL.txt",
                    ),
                    BundledFont(
                            id = "dancing_script",
                            label = "Dancing Script",
                            assetPath = "fonts/DancingScript-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/DancingScript-OFL.txt",
                    ),
                    BundledFont(
                            id = "playfair_display",
                            label = "Playfair Display",
                            assetPath = "fonts/PlayfairDisplay-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/PlayfairDisplay-OFL.txt",
                    ),
                    BundledFont(
                            id = "space_grotesk",
                            label = "Space Grotesk",
                            assetPath = "fonts/SpaceGrotesk-Variable.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/SpaceGrotesk-OFL.txt",
                    ),
                    BundledFont(
                            id = "righteous",
                            label = "Righteous",
                            assetPath = "fonts/Righteous-Regular.ttf",
                            script = FontScript.LATIN,
                            licenseAssetPath = "fonts/licenses/Righteous-OFL.txt",
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
