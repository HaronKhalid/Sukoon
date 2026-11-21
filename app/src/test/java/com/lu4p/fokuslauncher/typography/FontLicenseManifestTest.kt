package com.lu4p.fokuslauncher.typography

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontLicenseManifestTest {
    private val fontsDir = File("src/main/assets/fonts")

    private val fontFiles
        get() = fontsDir.listFiles { f -> f.extension.lowercase() in setOf("ttf", "otf") }.orEmpty().toList()

    @Test
    fun everyFontFileIsRegisteredWithAnOflLicenceFile() {
        assertTrue(fontFiles.size >= 15)
        val registered = FontRegistry.bundledFonts.associateBy { File("src/main/assets/${it.assetPath}").name }
        fontFiles.forEach { file ->
            val font = registered[file.name]
            assertTrue("${file.name} is bundled but not in FontRegistry", font != null)
            val licence = File("src/main/assets/${font!!.licenseAssetPath}")
            assertTrue("no licence for ${file.name}", licence.isFile)
            assertTrue(licence.readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
            assertTrue(licence.readText().startsWith("Copyright"))
        }
    }

    @Test
    fun registryHasNoMissingFilesAndUniqueIds() {
        FontRegistry.bundledFonts.forEach { assertTrue(it.assetPath, File("src/main/assets/${it.assetPath}").isFile) }
        assertEquals(FontRegistry.bundledFonts.size, FontRegistry.bundledFonts.map { it.id }.toSet().size)
    }

    @Test
    fun jameelNooriIsNotBundled() {
        assertFalse(fontFiles.any { it.name.contains("jameel", ignoreCase = true) })
        assertFalse(FontRegistry.bundledFonts.any { it.id.contains("jameel", ignoreCase = true) })
    }

    @Test
    fun licenceFilesAreAccountedFor() {
        val used = FontRegistry.bundledFonts.map { File(it.licenseAssetPath).name }.toSet()
        File(fontsDir, "licenses").listFiles().orEmpty().forEach {
            assertTrue("orphan licence ${it.name}", it.name in used)
        }
    }
}
