package com.lu4p.fokuslauncher.theme

sealed interface ThemeParseResult {
    data class Success(val theme: ThemeDefinition) : ThemeParseResult

    /** [reason] is a developer-facing English diagnostic, not shown verbatim to users. */
    data class Failure(val reason: String) : ThemeParseResult
}

/**
 * Strict import/export of themes. Decoding never throws: any malformed, oversized, unsupported or
 * unreadable (low-contrast) input becomes [ThemeParseResult.Failure], and callers fall back to the
 * default theme. Unknown JSON keys are ignored, never interpreted.
 */
object ThemeCodec {
    private val ID_REGEX = Regex("^[a-z0-9][a-z0-9_-]{0,${ThemeDefinition.MAX_ID_LENGTH - 1}}$")
    private val FONT_REF_REGEX = Regex("^[A-Za-z0-9][A-Za-z0-9 ._-]{0,63}$")

    // ---- migration -------------------------------------------------------------------------

    /** Steps keyed by the version they upgrade FROM; each yields a map at version + 1. */
    internal val migrations: Map<Int, (Map<String, Any?>) -> Map<String, Any?>> =
            mapOf(0 to ::migrateV0ToV1, 1 to ::migrateV1ToV2)

    /**
     * v0 was the pre-release layout: `mode` instead of `appearance`, and `colors.text` /
     * `colors.muted` instead of `primaryText` / `secondaryText`; no typography or motion.
     */
    private fun migrateV0ToV1(m: Map<String, Any?>): Map<String, Any?> {
        val out = LinkedHashMap(m)
        out.remove("mode")?.let { out.putIfAbsent("appearance", it) }
        @Suppress("UNCHECKED_CAST")
        (out["colors"] as? Map<String, Any?>)?.let { c ->
            val nc = LinkedHashMap(c)
            nc.remove("text")?.let { nc.putIfAbsent("primaryText", it) }
            nc.remove("muted")?.let { nc.putIfAbsent("secondaryText", it) }
            out["colors"] = nc
        }
        out["schemaVersion"] = 1L
        return out
    }

    /** v2 only adds the optional `gradient` and `style` objects, so v1 data carries over unchanged. */
    private fun migrateV1ToV2(m: Map<String, Any?>): Map<String, Any?> = m + ("schemaVersion" to 2L)

    internal fun migrateToCurrent(
            map: Map<String, Any?>,
            steps: Map<Int, (Map<String, Any?>) -> Map<String, Any?>> = migrations,
            target: Int = ThemeDefinition.CURRENT_SCHEMA_VERSION,
    ): Map<String, Any?>? {
        var version = (map["schemaVersion"] as? Long)?.toInt() ?: return null
        var cur = map
        while (version < target) {
            val step = steps[version] ?: return null
            cur = step(cur)
            version++
            if (cur["schemaVersion"] != version.toLong()) cur = cur + ("schemaVersion" to version.toLong())
        }
        return cur
    }

    // ---- decode ----------------------------------------------------------------------------

    fun decode(json: String): ThemeParseResult {
        val root =
                try {
                    MiniJson.parse(json)
                } catch (e: MiniJson.JsonException) {
                    return ThemeParseResult.Failure("invalid JSON: ${e.message}")
                } catch (e: RuntimeException) {
                    return ThemeParseResult.Failure("invalid JSON")
                }
        return decodeValue(root)
    }

    internal fun decodeValue(root: Any?): ThemeParseResult {
        @Suppress("UNCHECKED_CAST")
        val raw = root as? Map<String, Any?> ?: return ThemeParseResult.Failure("theme must be an object")
        val version = raw["schemaVersion"] as? Long ?: return ThemeParseResult.Failure("missing schemaVersion")
        if (version < 0) return ThemeParseResult.Failure("invalid schemaVersion")
        if (version > ThemeDefinition.CURRENT_SCHEMA_VERSION) {
            return ThemeParseResult.Failure("unsupported schemaVersion $version")
        }
        val m = migrateToCurrent(raw) ?: return ThemeParseResult.Failure("cannot migrate schemaVersion $version")
        return validate(m)
    }

    private fun validate(m: Map<String, Any?>): ThemeParseResult {
        fun fail(r: String) = ThemeParseResult.Failure(r)

        val id = m["id"] as? String ?: return fail("missing id")
        if (!ID_REGEX.matches(id)) return fail("invalid id")

        val name = (m["name"] as? String)?.trim() ?: return fail("missing name")
        if (name.isEmpty() || name.length > ThemeDefinition.MAX_NAME_LENGTH) return fail("invalid name length")
        if (name.any { it.isISOControl() }) return fail("invalid name characters")

        val appearance =
                ThemeAppearance.fromWire(m["appearance"] as? String ?: return fail("missing appearance"))
                        ?: return fail("invalid appearance")

        @Suppress("UNCHECKED_CAST")
        val c = m["colors"] as? Map<String, Any?> ?: return fail("missing colors")
        fun color(key: String): Int? = (c[key] as? String)?.let(ThemeColorMath::parseHex)
        val background = color("background") ?: return fail("invalid colors.background")
        val primary = color("primaryText") ?: return fail("invalid colors.primaryText")
        val secondary = color("secondaryText") ?: return fail("invalid colors.secondaryText")
        val accent = color("accent") ?: return fail("invalid colors.accent")
        val surface =
                if (c["surface"] == null) ThemeColorMath.blend(primary, background, 0.08)
                else color("surface") ?: return fail("invalid colors.surface")

        val gradient =
                if (m["gradient"] == null) null
                else parseGradient(m["gradient"]) ?: return fail("invalid gradient")
        val style =
                if (m["style"] == null) ThemeStyle()
                else parseStyle(m["style"]) ?: return fail("invalid style")
        val stops = gradient?.stops ?: listOf(background)
        if (ThemeColorMath.minContrast(primary, stops) < ThemeDefinition.MIN_TEXT_CONTRAST) {
            return fail("primaryText has too little contrast with background")
        }

        @Suppress("UNCHECKED_CAST")
        val t = m["typography"] as? Map<String, Any?>
        if (m["typography"] != null && t == null) return fail("invalid typography")
        fun font(key: String): String? {
            val v = t?.get(key) ?: return ThemeTypography.FONT_SYSTEM
            return (v as? String)?.takeIf { FONT_REF_REGEX.matches(it) }
        }
        val typography =
                ThemeTypography(
                        defaultFont = font("defaultFont") ?: return fail("invalid typography.defaultFont"),
                        clockFont = font("clockFont") ?: return fail("invalid typography.clockFont"),
                        noteFont = font("noteFont") ?: return fail("invalid typography.noteFont"),
                )

        @Suppress("UNCHECKED_CAST")
        val mo = m["motion"] as? Map<String, Any?>
        if (m["motion"] != null && mo == null) return fail("invalid motion")
        val animations =
                if (mo?.get("animationsEnabled") == null) true
                else mo["animationsEnabled"] as? Boolean ?: return fail("invalid motion.animationsEnabled")
        val intensity =
                if (mo?.get("intensity") == null) MotionIntensity.SUBTLE
                else MotionIntensity.fromWire(mo["intensity"] as? String ?: "")
                        ?: return fail("invalid motion.intensity")

        return ThemeParseResult.Success(
                ThemeDefinition(
                        schemaVersion = ThemeDefinition.CURRENT_SCHEMA_VERSION,
                        id = id,
                        name = name,
                        appearance = appearance,
                        colors = ThemeColors(background, primary, secondary, accent, surface),
                        typography = typography,
                        motion = ThemeMotion(animations, intensity),
                        gradient = gradient,
                        style = style,
                )
        )
    }

    private val CLOCK_FACE_REGEX = Regex("^[a-z0-9][a-z0-9_-]{0,47}$")

    private fun parseGradient(v: Any?): ThemeGradient? {
        @Suppress("UNCHECKED_CAST")
        val g = v as? Map<String, Any?> ?: return null
        val raw = g["stops"] as? List<*> ?: return null
        if (raw.size !in ThemeGradient.MIN_STOPS..ThemeGradient.MAX_STOPS) return null
        val stops = raw.map { (it as? String)?.let(ThemeColorMath::parseHex) ?: return null }
        val angle =
                when (val a = g["angle"]) {
                    null -> 180
                    is Long -> if (a in 0L..360L) a.toInt() else return null
                    else -> return null
                }
        return ThemeGradient(stops, angle)
    }

    /** Lenient on the clock face id (any well-formed id; unknown faces fall back at use), strict on shape. */
    private fun parseStyle(v: Any?): ThemeStyle? {
        @Suppress("UNCHECKED_CAST")
        val s = v as? Map<String, Any?> ?: return null
        val face =
                when (val f = s["clockFaceId"]) {
                    null -> ClockFaces.DEFAULT
                    is String -> if (CLOCK_FACE_REGEX.matches(f)) f else return null
                    else -> return null
                }
        val preset =
                when (val p = s["layoutPreset"]) {
                    null -> "DEFAULT"
                    is String ->
                            if (p.length in 1..24 && p.all { it in 'A'..'Z' || it == '_' }) p
                            else return null
                    else -> return null
                }
        val weight =
                when (val w = s["fontWeight"]) {
                    null -> 0
                    is Long -> if (w == 0L || w in 300L..700L) w.toInt() else return null
                    else -> return null
                }
        val spacing =
                when (val l = s["letterSpacingEm"]) {
                    null -> 0f
                    is Long -> l.toFloat()
                    is Double -> l.toFloat()
                    else -> return null
                }
        if (spacing.isNaN() || spacing < -0.1f || spacing > ThemeStyle.MAX_LETTER_SPACING_EM) return null
        return ThemeStyle(face, preset, weight, spacing)
    }

    /** True when [theme] would pass import validation (used by the editor before saving). */
    fun isValid(theme: ThemeDefinition): Boolean = decode(encode(theme)) is ThemeParseResult.Success

    // ---- encode ----------------------------------------------------------------------------

    fun encode(theme: ThemeDefinition): String =
            buildString {
                val q = MiniJson::quote
                append("{\n")
                append("  \"schemaVersion\": ${theme.schemaVersion},\n")
                append("  \"id\": ${q(theme.id)},\n")
                append("  \"name\": ${q(theme.name)},\n")
                append("  \"appearance\": ${q(theme.appearance.wire)},\n")
                append("  \"colors\": {\n")
                append("    \"background\": ${q(ThemeColorMath.toHex(theme.colors.background))},\n")
                append("    \"primaryText\": ${q(ThemeColorMath.toHex(theme.colors.primaryText))},\n")
                append("    \"secondaryText\": ${q(ThemeColorMath.toHex(theme.colors.secondaryText))},\n")
                append("    \"accent\": ${q(ThemeColorMath.toHex(theme.colors.accent))},\n")
                append("    \"surface\": ${q(ThemeColorMath.toHex(theme.colors.surface))}\n")
                append("  },\n")
                append("  \"typography\": {\n")
                append("    \"defaultFont\": ${q(theme.typography.defaultFont)},\n")
                append("    \"clockFont\": ${q(theme.typography.clockFont)},\n")
                append("    \"noteFont\": ${q(theme.typography.noteFont)}\n")
                append("  },\n")
                append("  \"motion\": {\n")
                append("    \"animationsEnabled\": ${theme.motion.animationsEnabled},\n")
                append("    \"intensity\": ${q(theme.motion.intensity.wire)}\n")
                append("  },\n")
                theme.gradient?.let { g ->
                    append("  \"gradient\": {\"stops\": ")
                    append(g.stops.joinToString(prefix = "[", postfix = "]") { q(ThemeColorMath.toHex(it)) })
                    append(", \"angle\": ${g.angleDegrees}},\n")
                }
                append("  \"style\": {\n")
                append("    \"clockFaceId\": ${q(theme.style.clockFaceId)},\n")
                append("    \"layoutPreset\": ${q(theme.style.layoutPreset)},\n")
                append("    \"fontWeight\": ${theme.style.fontWeight},\n")
                append("    \"letterSpacingEm\": ${String.format(java.util.Locale.ROOT, "%.3f", theme.style.letterSpacingEm)}\n")
                append("  }\n")
                append("}")
            }

    fun encodeList(themes: List<ThemeDefinition>): String =
            themes.joinToString(prefix = "[", postfix = "]", separator = ",") { encode(it) }

    /** Decodes a stored array; invalid entries are dropped, never fatal. */
    fun decodeList(json: String?): List<ThemeDefinition> {
        if (json.isNullOrBlank()) return emptyList()
        val root =
                try {
                    MiniJson.parse(json)
                } catch (e: Exception) {
                    return emptyList()
                }
        val list = root as? List<*> ?: return emptyList()
        return list.mapNotNull { (decodeValue(it) as? ThemeParseResult.Success)?.theme }
    }
}
