package com.lu4p.fokuslauncher.theme

/** Style Gallery filters. Derived from theme data, so custom themes filter correctly too. */
enum class StyleFilter { ALL, DARK, LIGHT, COLOURFUL, MINIMAL }

private fun saturation(argb: Int): Double {
    val r = ((argb shr 16) and 0xFF) / 255.0
    val g = ((argb shr 8) and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    val mx = maxOf(r, g, b)
    val mn = minOf(r, g, b)
    return if (mx == 0.0) 0.0 else (mx - mn) / mx
}

fun ThemeDefinition.isColourful(): Boolean =
        gradient != null || saturation(colors.accent) > 0.5

fun ThemeDefinition.isMinimal(): Boolean =
        style.layoutPreset == "PURE_MINIMAL" || (gradient == null && saturation(colors.accent) < 0.45)

fun ThemeDefinition.matches(filter: StyleFilter): Boolean =
        when (filter) {
            StyleFilter.ALL -> true
            StyleFilter.DARK -> appearance == ThemeAppearance.DARK
            StyleFilter.LIGHT -> appearance == ThemeAppearance.LIGHT
            StyleFilter.COLOURFUL -> isColourful()
            StyleFilter.MINIMAL -> isMinimal()
        }
