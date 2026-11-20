package com.lu4p.fokuslauncher.typography

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontRegistryTest {

    private fun assetFile(path: String) = File("src/main/assets/$path")

    @Test
    fun emptyAndUnknownBundledFollowLauncherFont() {
        assertEquals(FontChoice.FollowLauncher, FontRegistry.resolve(""))
        assertEquals(FontChoice.FollowLauncher, FontRegistry.resolve(null))
        assertEquals(FontChoice.FollowLauncher, FontRegistry.resolve("bundled:does_not_exist"))
    }

    @Test
    fun bundledSystemAndCustomValuesResolveToTheirKind() {
        val inter = FontRegistry.resolve("bundled:inter")
        assertTrue(inter is FontChoice.Bundled && inter.font.id == "inter")
        assertEquals(FontChoice.System("serif"), FontRegistry.resolve("serif"))
        assertEquals(FontChoice.Custom("custom:font-1.ttf"), FontRegistry.resolve("custom:font-1.ttf"))
        // Legacy enum-style value keeps working.
        assertEquals(FontChoice.System("monospace"), FontRegistry.resolve("MONOSPACE"))
    }

    @Test
    fun storageValueRoundTrips() {
        FontRegistry.bundledFonts.forEach {
            val choice = FontRegistry.resolve(FontRegistry.storageValueFor(it))
            assertEquals(FontChoice.Bundled(it), choice)
        }
    }

    @Test
    fun bundledIdsAreUnique() {
        val ids = FontRegistry.bundledFonts.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyBundledFontShipsItsFileAndOflLicence() {
        FontRegistry.bundledFonts.forEach { font ->
            assertTrue("missing ${font.assetPath}", assetFile(font.assetPath).isFile)
            val licence = assetFile(font.licenseAssetPath)
            assertTrue("missing ${font.licenseAssetPath}", licence.isFile)
            assertTrue(licence.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
        }
    }

    @Test
    fun jameelNooriIsNotBundled() {
        assertTrue(FontRegistry.bundledFonts.none { it.label.contains("Jameel", ignoreCase = true) })
        val fonts = assetFile("fonts").listFiles().orEmpty()
        assertTrue(fonts.none { it.name.contains("Jameel", ignoreCase = true) })
    }

    @Test
    fun elementsHaveDistinctKeys() {
        val keys = TextElement.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }
}
