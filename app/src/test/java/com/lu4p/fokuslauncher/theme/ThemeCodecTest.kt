package com.lu4p.fokuslauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCodecTest {
    private val valid =
            """
            {"schemaVersion":1,"id":"midnight-navy","name":"Midnight Navy","appearance":"dark",
             "colors":{"background":"#080B12","primaryText":"#F5F5F5","secondaryText":"#929BAD","accent":"#A6B6D4"},
             "typography":{"defaultFont":"system","clockFont":"inter","noteFont":"system"},
             "motion":{"animationsEnabled":true,"intensity":"subtle"}}
            """.trimIndent()

    private fun failure(json: String) = (ThemeCodec.decode(json) as? ThemeParseResult.Failure)?.reason
    private fun ok(json: String) = (ThemeCodec.decode(json) as ThemeParseResult.Success).theme

    @Test
    fun `brief example decodes and surface default is derived`() {
        val t = ok(valid)
        assertEquals("midnight-navy", t.id)
        assertEquals(ThemeAppearance.DARK, t.appearance)
        assertEquals("inter", t.typography.clockFont)
        assertEquals(MotionIntensity.SUBTLE, t.motion.intensity)
        assertEquals(0xFF080B12.toInt(), t.colors.background)
        assertTrue(ThemeColorMath.luminance(t.colors.surface) > ThemeColorMath.luminance(t.colors.background))
    }

    @Test
    fun `typography and motion are optional with defaults`() {
        val t = ok(
                """{"schemaVersion":1,"id":"a","name":"A","appearance":"dark",
                "colors":{"background":"#000000","primaryText":"#FFFFFF","secondaryText":"#999999","accent":"#FFFFFF"}}"""
        )
        assertEquals(ThemeTypography(), t.typography)
        assertEquals(ThemeMotion(), t.motion)
    }

    @Test
    fun `unknown keys are ignored never interpreted`() {
        val t = ok(valid.replaceFirst("{", "{\"script\":\"rm -rf /\",\"extra\":{\"a\":[1,2]},"))
        assertEquals("midnight-navy", t.id)
    }

    @Test
    fun `malformed json fails safely`() {
        listOf("", "   ", "nope", "{", "[]", "null", "{\"a\":1} trailing", "{\"a\":1,\"a\":2}", "\"str\"", "{'a':1}")
                .forEach { assertTrue("[$it]", failure(it) != null) }
    }

    @Test
    fun `oversized and deeply nested input is rejected`() {
        assertTrue(failure(" ".repeat(MiniJson.MAX_INPUT_CHARS + 1)) != null)
        assertTrue(failure("[".repeat(500) + "]".repeat(500)) != null)
    }

    @Test
    fun `missing and invalid fields are rejected`() {
        assertTrue(failure(valid.replace("\"schemaVersion\":1,", "")) != null)
        assertTrue(failure(valid.replace("\"id\":\"midnight-navy\"", "\"id\":\"Bad Id!\"")) != null)
        assertTrue(failure(valid.replace("\"id\":\"midnight-navy\"", "\"id\":\"../../x\"")) != null)
        assertTrue(failure(valid.replace("\"name\":\"Midnight Navy\"", "\"name\":\"\"")) != null)
        assertTrue(failure(valid.replace("\"name\":\"Midnight Navy\"", "\"name\":\"${"x".repeat(41)}\"")) != null)
        assertTrue(failure(valid.replace("\"name\":\"Midnight Navy\"", "\"name\":\"a\\u0000b\"")) != null)
        assertTrue(failure(valid.replace("\"dark\"", "\"purple\"")) != null)
        assertTrue(failure(valid.replace("#080B12", "red")) != null)
        assertTrue(failure(valid.replace("#080B12", "#08B12")) != null)
        assertTrue(failure(valid.replace("\"accent\":\"#A6B6D4\"", "\"accent\":12")) != null)
        assertTrue(failure(valid.replace("\"intensity\":\"subtle\"", "\"intensity\":\"wild\"")) != null)
        assertTrue(failure(valid.replace("\"animationsEnabled\":true", "\"animationsEnabled\":\"yes\"")) != null)
        assertTrue(failure(valid.replace("\"clockFont\":\"inter\"", "\"clockFont\":\"/sdcard/evil.ttf\"")) != null)
    }

    @Test
    fun `unreadable contrast is rejected`() {
        assertTrue(failure(valid.replace("#F5F5F5", "#0A0D14")) != null)
    }

    @Test
    fun `future schema versions are rejected, not guessed`() {
        assertTrue(failure(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2"))!!.contains("unsupported"))
        assertTrue(failure(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":-1")) != null)
        assertTrue(failure(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":\"1\"")) != null)
        assertTrue(failure(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1.5")) != null)
    }

    @Test
    fun `v0 pre-release themes migrate to the current schema`() {
        val v0 =
                """{"schemaVersion":0,"id":"old","name":"Old","mode":"light",
                "colors":{"background":"#FFFFFF","text":"#111111","muted":"#666666","accent":"#3366FF"}}"""
        val t = ok(v0)
        assertEquals(ThemeDefinition.CURRENT_SCHEMA_VERSION, t.schemaVersion)
        assertEquals(ThemeAppearance.LIGHT, t.appearance)
        assertEquals(0xFF111111.toInt(), t.colors.primaryText)
        assertEquals(0xFF666666.toInt(), t.colors.secondaryText)
    }

    @Test
    fun `migration chain applies steps in order and fails when a step is missing`() {
        val steps: Map<Int, (Map<String, Any?>) -> Map<String, Any?>> =
                mapOf(
                        1 to { m -> m + ("a" to "one") },
                        2 to { m -> m + ("b" to (m["a"] as String) + "-two") },
                )
        val out = ThemeCodec.migrateToCurrent(mapOf("schemaVersion" to 1L), steps, target = 3)!!
        assertEquals(3L, out["schemaVersion"])
        assertEquals("one-two", out["b"])
        assertNull(ThemeCodec.migrateToCurrent(mapOf("schemaVersion" to 0L), steps, target = 3))
        assertNull(ThemeCodec.migrateToCurrent(emptyMap(), steps, target = 3))
        assertEquals(
                mapOf<String, Any?>("schemaVersion" to 3L),
                ThemeCodec.migrateToCurrent(mapOf("schemaVersion" to 3L), steps, 3),
        )
    }

    @Test
    fun `name with quotes backslash and unicode round-trips`() {
        val t = BuiltInThemes.Custom.copy(id = "u1", name = "He said \"hi\" \\ ٹیسٹ")
        assertEquals(t, ok(ThemeCodec.encode(t)))
    }

    @Test
    fun `stored list drops invalid entries and survives garbage`() {
        val good = ThemeCodec.encode(BuiltInThemes.Ocean.copy(id = "c1", name = "C1"))
        val list = ThemeCodec.decodeList("[$good,{\"schemaVersion\":1},42,null]")
        assertEquals(listOf("c1"), list.map { it.id })
        assertEquals(emptyList<ThemeDefinition>(), ThemeCodec.decodeList("garbage"))
        assertEquals(emptyList<ThemeDefinition>(), ThemeCodec.decodeList(null))
        assertEquals(emptyList<ThemeDefinition>(), ThemeCodec.decodeList("{}"))
    }

    @Test
    fun `isValid reflects import validation`() {
        assertTrue(ThemeCodec.isValid(BuiltInThemes.Cream))
        assertEquals(false, ThemeCodec.isValid(BuiltInThemes.Cream.copy(name = " ")))
    }
}
