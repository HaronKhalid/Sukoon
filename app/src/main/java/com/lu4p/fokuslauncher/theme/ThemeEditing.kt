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
    // Picking a solid background replaces any gradient.
    val cleared = if (field == ThemeColorField.BACKGROUND) null else gradient
    return copy(colors = c, appearance = appearance, gradient = cleared)
}

private fun ThemeDefinition.withGradientState(g: ThemeGradient?): ThemeDefinition {
    val bg = g?.stops?.let { it[it.size / 2] } ?: colors.background
    val appearance = if (ThemeColorMath.luminance(bg) > 0.4) ThemeAppearance.LIGHT else ThemeAppearance.DARK
    return copy(gradient = g, colors = colors.copy(background = bg), appearance = appearance)
}

/** Turns the gradient on (two stops derived from the background and accent) or off. */
fun ThemeDefinition.withGradientEnabled(enabled: Boolean): ThemeDefinition =
        if (!enabled) withGradientState(null)
        else if (gradient != null) this
        else
                withGradientState(
                        ThemeGradient(
                                listOf(colors.background, ThemeColorMath.blend(colors.accent, colors.background, 0.18)),
                                180,
                        )
                )

fun ThemeDefinition.withGradientStop(index: Int, argb: Int): ThemeDefinition {
    val g = gradient ?: return this
    if (index !in g.stops.indices) return this
    return withGradientState(g.copy(stops = g.stops.mapIndexed { i, c -> if (i == index) argb or 0xFF000000.toInt() else c }))
}

fun ThemeDefinition.withGradientAngle(angle: Int): ThemeDefinition {
    val g = gradient ?: return this
    return withGradientState(g.copy(angleDegrees = angle.coerceIn(0, 360)))
}

/** Adds a third stop (average of the first two) or removes it again. */
fun ThemeDefinition.withThirdStop(enabled: Boolean): ThemeDefinition {
    val g = gradient ?: return this
    return when {
        enabled && g.stops.size == 2 ->
                withGradientState(g.copy(stops = listOf(g.stops[0], ThemeColorMath.blend(g.stops[0], g.stops[1], 0.5), g.stops[1])))
        !enabled && g.stops.size == 3 -> withGradientState(g.copy(stops = listOf(g.stops[0], g.stops[2])))
        else -> this
    }
}

/** A user-editable copy (any built-in or custom theme) that gets a fresh id when saved. */
fun ThemeDefinition.duplicated(newId: String, newName: String): ThemeDefinition =
        copy(id = newId, name = newName.take(ThemeDefinition.MAX_NAME_LENGTH))

/** Text is readable enough on the background to be saved/applied. */
fun ThemeDefinition.isReadable(): Boolean =
        ThemeColorMath.minContrast(colors.primaryText, backgroundStops) >= ThemeDefinition.MIN_TEXT_CONTRAST

/** Id to save [draft] under: keeps an existing custom id, otherwise allocates a fresh one. */
fun saveIdFor(draft: ThemeDefinition, existingCustomIds: Set<String>): String =
        if (draft.id in existingCustomIds) draft.id else ThemeStore.newCustomId(existingCustomIds)
