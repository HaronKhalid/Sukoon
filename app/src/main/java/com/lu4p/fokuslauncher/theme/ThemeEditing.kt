package com.lu4p.fokuslauncher.theme

enum class ThemeColorField {
    BACKGROUND,
    PRIMARY_TEXT,
    SECONDARY_TEXT,
    ACCENT,
    SURFACE,
}

fun ThemeColors.get(field: ThemeColorField): Int =
        when (field) {
            ThemeColorField.BACKGROUND -> background
            ThemeColorField.PRIMARY_TEXT -> primaryText
            ThemeColorField.SECONDARY_TEXT -> secondaryText
            ThemeColorField.ACCENT -> accent
            ThemeColorField.SURFACE -> surface
        }

fun ThemeDefinition.withColor(field: ThemeColorField, argb: Int): ThemeDefinition {
    val v = argb or 0xFF000000.toInt()
    val c =
            when (field) {
                ThemeColorField.BACKGROUND -> colors.copy(background = v)
                ThemeColorField.PRIMARY_TEXT -> colors.copy(primaryText = v)
                ThemeColorField.SECONDARY_TEXT -> colors.copy(secondaryText = v)
                ThemeColorField.ACCENT -> colors.copy(accent = v)
                ThemeColorField.SURFACE -> colors.copy(surface = v)
            }
    // Keep the appearance hint consistent with the background so system icons stay legible.
    val appearance =
            if (ThemeColorMath.luminance(c.background) > 0.4) ThemeAppearance.LIGHT else ThemeAppearance.DARK
    return copy(colors = c, appearance = appearance)
}

/** Text is readable enough on the background to be saved/applied. */
fun ThemeDefinition.isReadable(): Boolean =
        ThemeColorMath.contrastRatio(colors.background, colors.primaryText) >= ThemeDefinition.MIN_TEXT_CONTRAST

/** Id to save [draft] under: keeps an existing custom id, otherwise allocates a fresh one. */
fun saveIdFor(draft: ThemeDefinition, existingCustomIds: Set<String>): String =
        if (draft.id in existingCustomIds) draft.id else ThemeStore.newCustomId(existingCustomIds)
