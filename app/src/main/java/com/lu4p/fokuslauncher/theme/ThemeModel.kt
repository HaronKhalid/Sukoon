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

/** Versioned, data-only theme description. Nothing in here is ever interpreted as code. */
data class ThemeDefinition(
        val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val id: String,
        val name: String,
        val appearance: ThemeAppearance,
        val colors: ThemeColors,
        val typography: ThemeTypography = ThemeTypography(),
        val motion: ThemeMotion = ThemeMotion(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
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
