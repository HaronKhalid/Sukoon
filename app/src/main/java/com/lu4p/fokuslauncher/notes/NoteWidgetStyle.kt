package com.lu4p.fokuslauncher.notes

import java.net.URLDecoder
import java.net.URLEncoder

enum class NoteCardStyle { PLAIN, FILLED, OUTLINED, STICKY }

/**
 * Global default look of the home note: the text appearance used for new notes and the
 * widget-level card options (card style, opacity, corners, border, width, height).
 * Everything is clamped by [coerced]; [decode] never throws and returns defaults for anything
 * unreadable or from a newer version.
 */
data class NoteWidgetStyle(
        val appearance: NoteAppearance = NoteAppearance.DEFAULT,
        val cardStyle: NoteCardStyle = NoteCardStyle.PLAIN,
        /** Multiplies the alpha of the background colour. */
        val backgroundOpacity: Float = 1f,
        val cornerRadiusDp: Int = 16,
        val borderEnabled: Boolean = false,
        /** ARGB; 0 means the theme's secondary colour. */
        val borderColor: Int = 0,
        /** 0.5-2.0. Width is scale/2 of the available width, so 2.0 (default) is full width. */
        val widthScale: Float = MAX_SCALE,
        /** 0.5-2.0 multiplier of the minimum card height. */
        val heightScale: Float = 1f,
) {
    fun coerced(): NoteWidgetStyle =
            copy(
                    appearance = appearance.coerced(),
                    backgroundOpacity = backgroundOpacity.coerceIn(0f, 1f),
                    cornerRadiusDp = cornerRadiusDp.coerceIn(0, MAX_CORNER_DP),
                    widthScale = widthScale.coerceIn(MIN_SCALE, MAX_SCALE),
                    heightScale = heightScale.coerceIn(MIN_SCALE, MAX_SCALE),
            )

    /** Fraction of the available width the card takes (0.25-1.0). */
    val widthFraction: Float
        get() = widthScale.coerceIn(MIN_SCALE, MAX_SCALE) / 2f

    fun encode(): String {
        val a = appearance
        return listOf(
                        VERSION,
                        URLEncoder.encode(a.fontFamily, "UTF-8"),
                        a.fontSizeSp,
                        a.fontWeight,
                        a.textColor,
                        a.alignment.ordinal,
                        a.lineSpacing,
                        a.backgroundColor,
                        a.paddingDp,
                        a.maxLines,
                        cardStyle.ordinal,
                        backgroundOpacity,
                        cornerRadiusDp,
                        if (borderEnabled) 1 else 0,
                        borderColor,
                        widthScale,
                        heightScale,
                )
                .joinToString("|")
    }

    companion object {
        const val VERSION = "v1"
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2.0f
        const val MAX_CORNER_DP = 48
        val DEFAULT = NoteWidgetStyle()

        fun decode(raw: String?): NoteWidgetStyle {
            if (raw.isNullOrBlank()) return DEFAULT
            val p = raw.split("|")
            if (p.firstOrNull() != VERSION) return DEFAULT
            return runCatching {
                        val d = NoteAppearance.DEFAULT
                        fun f(i: Int, def: Float) =
                                p.getOrNull(i)?.toFloatOrNull()?.takeIf { it.isFinite() } ?: def
                        fun n(i: Int, def: Int) = p.getOrNull(i)?.toIntOrNull() ?: def
                        NoteWidgetStyle(
                                        appearance =
                                                NoteAppearance(
                                                        fontFamily =
                                                                p.getOrNull(1)?.let {
                                                                    URLDecoder.decode(it, "UTF-8")
                                                                } ?: d.fontFamily,
                                                        fontSizeSp = f(2, d.fontSizeSp),
                                                        fontWeight = n(3, d.fontWeight),
                                                        textColor = n(4, d.textColor),
                                                        alignment =
                                                                NoteAlignment.entries.getOrElse(n(5, 0)) {
                                                                    NoteAlignment.START
                                                                },
                                                        lineSpacing = f(6, d.lineSpacing),
                                                        backgroundColor = n(7, d.backgroundColor),
                                                        paddingDp = n(8, d.paddingDp),
                                                        maxLines = n(9, d.maxLines),
                                                ),
                                        cardStyle =
                                                NoteCardStyle.entries.getOrElse(n(10, 0)) {
                                                    NoteCardStyle.PLAIN
                                                },
                                        backgroundOpacity = f(11, 1f),
                                        cornerRadiusDp = n(12, 16),
                                        borderEnabled = n(13, 0) == 1,
                                        borderColor = n(14, 0),
                                        widthScale = f(15, MAX_SCALE),
                                        heightScale = f(16, 1f),
                                )
                                .coerced()
                    }
                    .getOrDefault(DEFAULT)
        }
    }
}

/** Colours and flags a card needs, resolved from the style and appearance (pure, testable). */
data class ResolvedCard(
        /** ARGB with the opacity already applied; 0 alpha means no fill. */
        val backgroundArgb: Int,
        val showBorder: Boolean,
        /** ARGB text colour to use when the note has none of its own; null = theme colour. */
        val defaultTextArgb: Int?,
        val elevated: Boolean,
)

object NoteCard {
    const val FILLED_FALLBACK = 0xFF1E1E1E.toInt()
    const val STICKY_FILL = 0xFFFFF3A8.toInt()
    const val STICKY_TEXT = 0xFF2B2B2B.toInt()

    fun withOpacity(argb: Int, opacity: Float): Int {
        val alpha = ((argb ushr 24) * opacity.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (alpha shl 24) or (argb and 0x00FFFFFF)
    }

    fun resolve(style: NoteWidgetStyle, appearance: NoteAppearance): ResolvedCard {
        val own = appearance.backgroundColor
        val fill =
                when (style.cardStyle) {
                    NoteCardStyle.PLAIN, NoteCardStyle.OUTLINED -> own
                    NoteCardStyle.FILLED -> if (own != 0) own else FILLED_FALLBACK
                    NoteCardStyle.STICKY -> if (own != 0) own else STICKY_FILL
                }
        return ResolvedCard(
                backgroundArgb = if (fill == 0) 0 else withOpacity(fill, style.backgroundOpacity),
                showBorder = style.borderEnabled || style.cardStyle == NoteCardStyle.OUTLINED,
                defaultTextArgb =
                        if (style.cardStyle == NoteCardStyle.STICKY && own == 0) STICKY_TEXT else null,
                elevated = style.cardStyle == NoteCardStyle.STICKY,
        )
    }
}
