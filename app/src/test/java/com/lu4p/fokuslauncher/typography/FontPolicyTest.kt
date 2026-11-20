package com.lu4p.fokuslauncher.typography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontPolicyTest {
    @Test
    fun nastaliqNamesAreRecognised() {
        assertTrue(FontPolicy.isNastaliqLike("Jameel Noori Nastaleeq"))
        assertTrue(FontPolicy.isNastaliqLike("Noto Nastaliq Urdu"))
        assertFalse(FontPolicy.isNastaliqLike("Inter"))
        assertFalse(FontPolicy.isNastaliqLike(null))
    }

    @Test
    fun autoBoostOnlyForNastaliq() {
        val auto = LineBoostMode.AUTO
        assertEquals(FontPolicy.NASTALIQ_BOOST, FontPolicy.lineHeightBoost("bundled:noto_nastaliq_urdu", null, auto), 0f)
        assertEquals(1f, FontPolicy.lineHeightBoost("bundled:inter", null, auto), 0f)
        assertEquals(1f, FontPolicy.lineHeightBoost("", null, auto), 0f)
        assertEquals(FontPolicy.NASTALIQ_BOOST, FontPolicy.lineHeightBoost("custom:font-1.ttf", "Jameel Noori Nastaleeq", auto), 0f)
        assertEquals(1f, FontPolicy.lineHeightBoost("custom:font-1.ttf", "Roboto", auto), 0f)
    }

    @Test
    fun explicitModesOverrideAuto() {
        assertEquals(1f, FontPolicy.lineHeightBoost("bundled:noto_nastaliq_urdu", null, LineBoostMode.OFF), 0f)
        assertEquals(FontPolicy.NASTALIQ_BOOST, FontPolicy.lineHeightBoost("bundled:inter", null, LineBoostMode.ON), 0f)
    }

    @Test
    fun unknownStoredValuesFallBackSafely() {
        // Unknown bundled id (newer backup) follows the launcher font and gets no boost.
        assertTrue(FontRegistry.resolve("bundled:does_not_exist") is FontChoice.FollowLauncher)
        assertEquals(1f, FontPolicy.lineHeightBoost("bundled:does_not_exist", null, LineBoostMode.AUTO), 0f)
        assertEquals(LineBoostMode.AUTO, FontPolicy.boostModeFromStorage("garbage"))
        assertEquals(LineBoostMode.ON, FontPolicy.boostModeFromStorage("on"))
        assertTrue(FontRegistry.resolve("custom:x.ttf") is FontChoice.Custom)
        assertTrue(FontRegistry.resolve("serif") is FontChoice.System)
    }

    @Test
    fun importValidationChecksMagicAndSize() {
        val ttf = byteArrayOf(0, 1, 0, 0, 9, 9)
        assertEquals(FontImportValidator.Result.OK, FontImportValidator.validate(ttf, 1000))
        assertEquals(FontImportValidator.Result.OK, FontImportValidator.validate("OTTO".toByteArray(), 1000))
        assertEquals(FontImportValidator.Result.OK, FontImportValidator.validate("ttcf".toByteArray(), 1000))
        assertEquals(FontImportValidator.Result.NOT_A_FONT, FontImportValidator.validate("%PDF".toByteArray(), 1000))
        assertEquals(FontImportValidator.Result.NOT_A_FONT, FontImportValidator.validate(byteArrayOf(0, 1), 2))
        assertEquals(FontImportValidator.Result.EMPTY, FontImportValidator.validate(ByteArray(0), 0))
        assertEquals(FontImportValidator.Result.TOO_LARGE, FontImportValidator.validate(ttf, FontPolicy.MAX_FONT_BYTES + 1))
    }
}
