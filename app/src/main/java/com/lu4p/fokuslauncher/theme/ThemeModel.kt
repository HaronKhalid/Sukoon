package com.lu4p.fokuslauncher.theme

/** Light/dark hint. Used for system-bar icon tint and as a contrast sanity check. */
enum class ThemeAppearance(val wire: String) {
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromWire(value: String): ThemeAppearance? = entries.firstOrNull { it.wire == value }
    }
}

enum class MotionIntensity(val wire: String) {
    NONE("none"),
    SUBTLE("subtle"),
    NORMAL("normal");

    companion object {
        fun fromWire(value: String): MotionIntensity? = entries.firstOrNull { it.wire == value }
    }
}

/** Colours as opaque 0xFFRRGGBB ints so the model stays free of Compose types and is unit-testable. */
data class ThemeColors(
        val background: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val accent: Int,
        val surface: Int,
)

/**
 * Font *references* (registry keys such as "system" or "inter"), never font data or paths.
 * Resolution against the installed font set belongs to the typography engine.
 */
data class ThemeTypography(
        val defaultFont: String = FONT_SYSTEM,
        val clockFont: String = FONT_SYSTEM,
        val noteFont: String = FONT_SYSTEM,
) {
    companion object {
        const val FONT_SYSTEM = "system"
    }
}

data class ThemeMotion(
        val animationsEnabled: Boolean = true,
        val intensity: MotionIntensity = MotionIntensity.SUBTLE,
)

/**
 * Optional smooth background gradient of 2-3 [stops] (opaque ARGB). [angleDegrees] follows the CSS
 * convention: 180 = top to bottom, 90 = left to right, 135 = top-left to bottom-right.
 */
data class ThemeGradient(val stops: List<Int>, val angleDegrees: Int = 180) {
    companion object {
        const val MIN_STOPS = 2
        const val MAX_STOPS = 3
        val ANGLE_PRESETS = listOf(180, 135, 90, 45, 0)
    }
}

/**
 * "Style bundle": what a theme suggests beyond colours. [clockFaceId] is resolved leniently (unknown
 * ids fall back to the default digital clock), [layoutPreset] is a LayoutPreset name, [fontWeight]
 * (0 = leave unchanged, else 300..700) and [letterSpacingEm] only tune the launcher typography and
 * never replace the user's chosen font family.
 */
data class ThemeStyle(
        val clockFaceId: String = ClockFaces.DEFAULT,
        val layoutPreset: String = "DEFAULT",
        val fontWeight: Int = 0,
        val letterSpacingEm: Float = 0f,
) {
    /** [clockFaceId] if it is a known face, otherwise the default digital clock. */
    val resolvedClockFaceId: String get() = ClockFaces.resolve(clockFaceId)

    companion object {
        const val MAX_LETTER_SPACING_EM = 0.2f
    }
}

object ClockFaces {
    const val DEFAULT = "classic-digital"
    val KNOWN =
            listOf(
                    "classic-digital", "stacked", "split-accent", "minimal-text",
                    "analog-minimal", "analog-ticks", "word-clock", "outline-bold",
            )

    fun resolve(id: String?): String = if (id != null && id in KNOWN) id else DEFAULT
}

/** Versioned, data-only theme description. Nothing in here is ever interpreted as code. */
data class ThemeDefinition(
        val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val id: String,
        val name: String,
        val appearance: ThemeAppearance,
        val colors: ThemeColors,
        val typography: ThemeTypography = ThemeTypography(),
        val motion: ThemeMotion = ThemeMotion(),
        /** When set, painted instead of the solid [ThemeColors.background] (which stays the fallback). */
        val gradient: ThemeGradient? = null,
        val style: ThemeStyle = ThemeStyle(),
) {
    /** Every background colour the text can land on. */
    val backgroundStops: List<Int> get() = gradient?.stops ?: listOf(colors.background)

    /** One solid colour representing the background (the middle stop) for scrims and system bars. */
    val representativeBackground: Int
        get() = gradient?.stops?.let { it[it.size / 2] } ?: colors.background

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        const val MAX_NAME_LENGTH = 40
        const val MAX_ID_LENGTH = 48
        /** Minimum WCAG contrast ratio between background and primary text. */
        const val MIN_TEXT_CONTRAST = 3.0
    }
}

object ThemeColorMath {
    private fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }

    fun luminance(argb: Int): Double {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    fun contrastRatio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Worst-case contrast of [text] against every colour in [backgrounds]. */
    fun minContrast(text: Int, backgrounds: List<Int>): Double =
            backgrounds.minOf { contrastRatio(text, it) }

    fun toHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** Strict `#RRGGBB`; returns null for anything else. */
    fun parseHex(text: String): Int? {
        if (text.length != 7 || text[0] != '#') return null
        val v = text.substring(1).toIntOrNull(16) ?: return null
        if (text.substring(1).any { it !in "0123456789abcdefABCDEF" }) return null
        return 0xFF000000.toInt() or v
    }

    /** Linear blend of [top] over [bottom] with [alpha] in 0..1. */
    fun blend(top: Int, bottom: Int, alpha: Double): Int {
        fun mix(shift: Int): Int {
            val t = (top shr shift) and 0xFF
            val b = (bottom shr shift) and 0xFF
            return Math.round(t * alpha + b * (1 - alpha)).toInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
