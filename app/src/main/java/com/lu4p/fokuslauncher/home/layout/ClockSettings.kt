package com.lu4p.fokuslauncher.home.layout

enum class ClockStyle { LARGE_DIGITAL, COMPACT, STACKED, MINIMAL_TEXT, SPLIT }

enum class ClockHourFormat { SYSTEM, H12, H24 }

/** Date formats resolved through java.time locale data. SYSTEM keeps the existing home date format. */
enum class ClockDateStyle { SYSTEM, FULL, LONG, MEDIUM, SHORT }

enum class ClockFontWeight(val value: Int) { LIGHT(300), NORMAL(400), MEDIUM(500), SEMI_BOLD(600), BOLD(700) }

/**
 * Clock Studio settings. Defaults reproduce the current clock (displayLarge, SemiBold, theme
 * colour). [fontSizeSp] of 0 means "theme default"; [colorArgb] of 0 means "theme colour".
 */
data class ClockSettings(
        val style: ClockStyle = ClockStyle.LARGE_DIGITAL,
        val hourFormat: ClockHourFormat = ClockHourFormat.SYSTEM,
        val showSeconds: Boolean = false,
        val fontSizeSp: Int = 0,
        val weight: ClockFontWeight = ClockFontWeight.SEMI_BOLD,
        val letterSpacingSp: Float = 0f,
        val lineHeightMultiplier: Float = 1f,
        val colorArgb: Int = 0,
        val alignment: ElementAlignment = ElementAlignment.START,
        val showDate: Boolean = true,
        val dateStyle: ClockDateStyle = ClockDateStyle.SYSTEM,
) {
    /** True when nothing differs from the legacy clock, so the legacy composable can be used. */
    val isLegacy: Boolean get() = this == ClockSettings()

    /** Always returns a copy with every numeric field inside its supported range. */
    fun validated(): ClockSettings = copy(
            fontSizeSp = if (fontSizeSp == 0) 0 else fontSizeSp.coerceIn(MIN_FONT_SP, MAX_FONT_SP),
            letterSpacingSp = letterSpacingSp.finiteOr(0f).coerceIn(-2f, 12f),
            lineHeightMultiplier = lineHeightMultiplier.finiteOr(1f).coerceIn(0.8f, 1.6f),
    )

    private fun Float.finiteOr(default: Float) = if (isFinite()) this else default

    companion object {
        const val MIN_FONT_SP = 24
        const val MAX_FONT_SP = 160
    }
}

object ClockSettingsCodec {
    private const val VERSION = "c1"

    fun encode(settings: ClockSettings): String = settings.validated().let {
        listOf(VERSION, it.style.name, it.hourFormat.name, if (it.showSeconds) 1 else 0, it.fontSizeSp,
                it.weight.name, it.letterSpacingSp, it.lineHeightMultiplier, it.colorArgb, it.alignment.name,
                if (it.showDate) 1 else 0, it.dateStyle.name).joinToString("|")
    }

    fun decode(raw: String?): ClockSettings? {
        if (raw.isNullOrBlank() || raw.length > 512) return null
        return try {
            val f = raw.split('|')
            if (f.size != 12 || f[0] != VERSION) return null
            fun bool(v: String) = when (v) { "1" -> true; "0" -> false; else -> throw IllegalArgumentException() }
            val letter = f[6].toFloat()
            val line = f[7].toFloat()
            if (!letter.isFinite() || !line.isFinite()) return null
            ClockSettings(
                    style = ClockStyle.valueOf(f[1]), hourFormat = ClockHourFormat.valueOf(f[2]),
                    showSeconds = bool(f[3]), fontSizeSp = f[4].toInt(), weight = ClockFontWeight.valueOf(f[5]),
                    letterSpacingSp = letter, lineHeightMultiplier = line, colorArgb = f[8].toInt(),
                    alignment = ElementAlignment.valueOf(f[9]), showDate = bool(f[10]),
                    dateStyle = ClockDateStyle.valueOf(f[11]),
            ).validated()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun decodeOrDefault(raw: String?): ClockSettings = decode(raw) ?: ClockSettings()
}
