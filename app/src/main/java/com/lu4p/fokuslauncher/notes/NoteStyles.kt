package com.lu4p.fokuslauncher.notes

import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.typography.FontRegistry

/** Extra ornament a style draws on top of the card; everything else maps to normal fields. */
enum class NoteDecoration { NONE, RULED, QUOTE_MARK, CHECKLIST, TIMESTAMP, GLOW }

/**
 * An original note design. [look] uses only the existing editable fields (font, size, weight,
 * colours, card style, radius, border, opacity, padding), so after applying a style every value
 * stays editable. [decoration] is an optional ornament chosen by the style id.
 * Fonts are listed by registry id ([preferredFontIds], first present wins) with a system
 * [fallbackFamily], so styles degrade safely when a font is not bundled.
 */
data class NoteStyle(
        val id: String,
        val labelRes: Int,
        val look: NoteWidgetStyle,
        val decoration: NoteDecoration = NoteDecoration.NONE,
        val preferredFontIds: List<String> = emptyList(),
        val fallbackFamily: String = "",
)

object NoteStyles {
    const val DEFAULT_ID = "minimal"
    private const val CHECK_OFF = "☐"
    private const val CHECK_ON = "☑"

    private fun look(
            fontSp: Float = 16f,
            weight: Int = 400,
            text: Int = 0,
            align: NoteAlignment = NoteAlignment.START,
            spacing: Float = 1.4f,
            bg: Int = 0,
            padding: Int = 12,
            lines: Int = 6,
            card: NoteCardStyle = NoteCardStyle.PLAIN,
            opacity: Float = 1f,
            radius: Int = 16,
            border: Boolean = false,
            borderColor: Int = 0,
    ) =
            NoteWidgetStyle(
                    appearance =
                            NoteAppearance(
                                    fontSizeSp = fontSp,
                                    fontWeight = weight,
                                    textColor = text,
                                    alignment = align,
                                    lineSpacing = spacing,
                                    backgroundColor = bg,
                                    paddingDp = padding,
                                    maxLines = lines,
                            ),
                    cardStyle = card,
                    backgroundOpacity = opacity,
                    cornerRadiusDp = radius,
                    borderEnabled = border,
                    borderColor = borderColor,
            )

    val all: List<NoteStyle> =
            listOf(
                    NoteStyle(
                            "sticky",
                            R.string.lock_style_sticky,
                            look(
                                    fontSp = 17f,
                                    text = NoteCard.STICKY_TEXT,
                                    card = NoteCardStyle.STICKY,
                                    radius = 6,
                                    padding = 16,
                            ),
                            preferredFontIds = listOf("caveat"),
                    ),
                    NoteStyle("minimal", R.string.lock_style_minimal, look(fontSp = 18f, weight = 300, padding = 8)),
                    NoteStyle(
                            "glass",
                            R.string.lock_style_glass,
                            look(
                                    bg = 0xFFFFFFFF.toInt(),
                                    text = 0xFFFFFFFF.toInt(),
                                    card = NoteCardStyle.FILLED,
                                    opacity = 0.16f,
                                    radius = 28,
                                    border = true,
                                    borderColor = 0x55FFFFFF,
                                    padding = 18,
                            ),
                    ),
                    NoteStyle(
                            "quote",
                            R.string.lock_style_quote,
                            look(
                                    fontSp = 22f,
                                    align = NoteAlignment.CENTER,
                                    spacing = 1.5f,
                                    padding = 20,
                                    lines = 8,
                            ),
                            NoteDecoration.QUOTE_MARK,
                            listOf("playfair_display", "lora", "merriweather"),
                            "serif",
                    ),
                    NoteStyle(
                            "handwritten",
                            R.string.lock_style_handwritten,
                            look(fontSp = 24f, spacing = 1.3f, padding = 12),
                            preferredFontIds = listOf("caveat", "dancing_script"),
                            fallbackFamily = "cursive",
                    ),
                    NoteStyle(
                            "checklist",
                            R.string.lock_style_checklist,
                            look(
                                    bg = 0xFF1E1E1E.toInt(),
                                    text = 0xFFFFFFFF.toInt(),
                                    card = NoteCardStyle.FILLED,
                                    radius = 14,
                                    padding = 16,
                                    spacing = 1.6f,
                            ),
                            NoteDecoration.CHECKLIST,
                    ),
                    NoteStyle(
                            "ruled",
                            R.string.lock_style_ruled,
                            look(
                                    fontSp = 17f,
                                    bg = 0xFFFAF6E9.toInt(),
                                    text = 0xFF23324A.toInt(),
                                    card = NoteCardStyle.FILLED,
                                    radius = 4,
                                    padding = 16,
                                    spacing = 1.8f,
                            ),
                            NoteDecoration.RULED,
                            listOf("caveat"),
                    ),
                    NoteStyle(
                            "neon",
                            R.string.lock_style_neon,
                            look(
                                    fontSp = 22f,
                                    weight = 600,
                                    text = 0xFF39FFB6.toInt(),
                                    bg = 0xFF050A08.toInt(),
                                    card = NoteCardStyle.OUTLINED,
                                    radius = 18,
                                    borderColor = 0xFF39FFB6.toInt(),
                                    padding = 18,
                                    align = NoteAlignment.CENTER,
                            ),
                            NoteDecoration.GLOW,
                    ),
                    NoteStyle(
                            "outlined",
                            R.string.lock_style_outlined,
                            look(card = NoteCardStyle.OUTLINED, radius = 12, padding = 16),
                    ),
                    NoteStyle(
                            "journal",
                            R.string.lock_style_journal,
                            look(fontSp = 15f, spacing = 1.5f, padding = 12),
                            NoteDecoration.TIMESTAMP,
                            listOf("jetbrains_mono"),
                            "monospace",
                    ),
                    NoteStyle(
                            "poster",
                            R.string.lock_style_poster,
                            look(fontSp = 34f, weight = 700, align = NoteAlignment.CENTER, spacing = 1.15f, lines = 5),
                    ),
                    NoteStyle(
                            "soft",
                            R.string.lock_style_soft,
                            look(
                                    text = 0xFF1B1B1B.toInt(),
                                    bg = 0xFFDCE8F5.toInt(),
                                    card = NoteCardStyle.FILLED,
                                    radius = 32,
                                    padding = 20,
                            ),
                    ),
            )

    fun find(id: String?): NoteStyle? = all.firstOrNull { it.id == id }

    /** Unknown or empty ids fall back to the minimal style. */
    fun resolve(id: String?): NoteStyle = find(id) ?: all.first { it.id == DEFAULT_ID }

    /** First preferred bundled font present in the registry, else the system fallback, else "". */
    fun fontFor(style: NoteStyle, exists: (String) -> Boolean = { FontRegistry.findBundled(it) != null }): String =
            style.preferredFontIds.firstOrNull(exists)?.let { FontRegistry.BUNDLED_PREFIX + it }
                    ?: style.fallbackFamily

    /** The editable look this style produces, with its font resolved against the registry. */
    fun widgetStyleOf(
            style: NoteStyle,
            exists: (String) -> Boolean = { FontRegistry.findBundled(it) != null },
    ): NoteWidgetStyle {
        val l = style.look
        return l.copy(appearance = l.appearance.copy(fontFamily = fontFor(style, exists))).coerced()
    }

    /** Text as displayed for [decoration]: checklist boxes or a journal timestamp line. */
    fun decorateText(decoration: NoteDecoration, text: String, stamp: String? = null): String =
            when (decoration) {
                NoteDecoration.CHECKLIST ->
                        text.lines().joinToString("\n") { line ->
                            val t = line.trimStart()
                            when {
                                t.isBlank() -> line
                                t.startsWith(CHECK_OFF) || t.startsWith(CHECK_ON) -> t
                                t.startsWith("- [ ]") -> CHECK_OFF + " " + t.removePrefix("- [ ]").trimStart()
                                t.startsWith("- [x]", ignoreCase = true) -> CHECK_ON + " " + t.drop(5).trimStart()
                                else -> "$CHECK_OFF $t"
                            }
                        }
                NoteDecoration.TIMESTAMP ->
                        if (stamp.isNullOrBlank()) text else text.trimEnd() + "\n— " + stamp
                else -> text
            }

    /** Per-note effective look: stored per-note card options, or the global one, plus own text look. */
    fun effective(note: NoteEntity, global: NoteWidgetStyle): NoteWidgetStyle {
        val base = if (note.cardStyle.isBlank()) global else NoteWidgetStyle.decode(note.cardStyle)
        return base.copy(appearance = note.appearance()).coerced()
    }

    /** Applies [style] to a note: text look, card options and the style id (all stay editable). */
    fun applyTo(
            note: NoteEntity,
            style: NoteStyle,
            exists: (String) -> Boolean = { FontRegistry.findBundled(it) != null },
    ): NoteEntity {
        val w = widgetStyleOf(style, exists)
        return note.withAppearance(w.appearance).copy(styleId = style.id, cardStyle = w.encode())
    }
}
