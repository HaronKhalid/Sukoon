package com.lu4p.fokuslauncher.typography

/** How the extra line height for tall scripts (Nastaliq) is chosen. */
enum class LineBoostMode { AUTO, ON, OFF }

/** Pure font policy helpers (no Android types) so they are covered by plain JVM tests. */
object FontPolicy {
    /** Multiplier applied to every line height when a Nastaliq-like font is active. */
    const val NASTALIQ_BOOST = 1.45f
    const val MAX_FONT_BYTES = 25L * 1024 * 1024

    private val nastaliqHints = listOf("nastaliq", "nastaleeq", "nastaleq", "noori", "jameel", "nafees nastaleeq")

    /** True when a font name (family or file) looks like a Nastaliq face. */
    fun isNastaliqLike(name: String?): Boolean {
        val n = name?.lowercase().orEmpty()
        return n.isNotBlank() && nastaliqHints.any { n.contains(it) }
    }

    /**
     * Line-height multiplier for [storageValue]. [customLabel] is the imported font's family name,
     * needed to recognise Nastaliq among imported fonts.
     */
    fun lineHeightBoost(
            storageValue: String?,
            customLabel: String?,
            mode: LineBoostMode,
    ): Float {
        when (mode) {
            LineBoostMode.OFF -> return 1f
            LineBoostMode.ON -> return NASTALIQ_BOOST
            LineBoostMode.AUTO -> Unit
        }
        return when (val choice = FontRegistry.resolve(storageValue)) {
            is FontChoice.Bundled ->
                    if (choice.font.script == FontScript.URDU_NASTALIQ) NASTALIQ_BOOST else 1f
            is FontChoice.Custom ->
                    if (isNastaliqLike(customLabel) || isNastaliqLike(choice.storageValue)) NASTALIQ_BOOST
                    else 1f
            else -> 1f
        }
    }

    fun boostModeFromStorage(raw: String?): LineBoostMode =
            LineBoostMode.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
                    ?: LineBoostMode.AUTO
}

/** Cheap structural check of a picked file before the platform tries to load it as a Typeface. */
object FontImportValidator {
    enum class Result { OK, EMPTY, TOO_LARGE, NOT_A_FONT }

    fun validate(header: ByteArray, sizeBytes: Long): Result {
        if (sizeBytes <= 0L || header.isEmpty()) return Result.EMPTY
        if (sizeBytes > FontPolicy.MAX_FONT_BYTES) return Result.TOO_LARGE
        if (header.size < 4) return Result.NOT_A_FONT
        val tag = String(header, 0, 4, Charsets.ISO_8859_1)
        val isTrueType = header[0] == 0.toByte() && header[1] == 1.toByte() && header[2] == 0.toByte() && header[3] == 0.toByte()
        val ok = isTrueType || tag == "OTTO" || tag == "true" || tag == "ttcf"
        return if (ok) Result.OK else Result.NOT_A_FONT
    }
}
