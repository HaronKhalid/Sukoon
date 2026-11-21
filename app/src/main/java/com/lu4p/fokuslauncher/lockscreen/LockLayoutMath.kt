package com.lu4p.fokuslauncher.lockscreen

import com.lu4p.fokuslauncher.notes.BidiText
import com.lu4p.fokuslauncher.notes.NoteAlignment
import com.lu4p.fokuslauncher.typography.FontPolicy
import com.lu4p.fokuslauncher.typography.LineBoostMode

enum class LockPosition { TOP, CENTER, BOTTOM }

/** Result of [LockLayoutMath.wrap]. */
data class WrappedText(val lines: List<String>, val truncated: Boolean)

/** Paragraph direction summary for a text. */
enum class TextDir { LTR, RTL, MIXED_LTR_FIRST, MIXED_RTL_FIRST, NEUTRAL }

/**
 * Pure layout maths for the lock-screen note bitmap (no Android types), so wrapping, line limits,
 * RTL handling and placement are covered by plain JVM tests. All sizes scale with the canvas width
 * against a 360 dp reference so the in-app preview and the real wallpaper bitmap look identical.
 */
object LockLayoutMath {
    const val REF_WIDTH_DP = 360f
    const val NATURAL_LINE = 1.2f
    const val URDU_BOOST = 1.15f
    const val ELLIPSIS = "…"

    fun pxPerDp(canvasWidth: Int): Float = canvasWidth / REF_WIDTH_DP

    fun textSizePx(sp: Float, canvasWidth: Int, scale: Float): Float =
            sp * pxPerDp(canvasWidth) * scale.coerceIn(0.5f, 2f)

    fun sideMarginPx(canvasWidth: Int): Float = canvasWidth * 0.08f

    /** Card width in px: the free width times the style width fraction. */
    fun cardWidthPx(canvasWidth: Int, widthFraction: Float): Float =
            ((canvasWidth - 2 * sideMarginPx(canvasWidth)) * widthFraction.coerceIn(0.25f, 1f))

    fun textWidthPx(cardWidth: Float, paddingPx: Float): Float = (cardWidth - 2 * paddingPx).coerceAtLeast(1f)

    /** Line height in px for a font size, line spacing and script boost. */
    fun lineHeightPx(sizePx: Float, lineSpacing: Float, boost: Float): Float = sizePx * lineSpacing * boost

    /** StaticLayout spacing multiplier that yields [lineHeightPx] from the font natural height. */
    fun spacingMultiplier(lineSpacing: Float, boost: Float): Float =
            ((lineSpacing * boost) / NATURAL_LINE).coerceIn(0.8f, 4f)

    /** Extra height for tall scripts: Nastaliq fonts get the policy boost, other Arabic-script text a small one. */
    fun lineBoost(fontStorageValue: String?, text: String): Float {
        val font = FontPolicy.lineHeightBoost(fontStorageValue, null, LineBoostMode.AUTO)
        val script = if (BidiText.containsArabicScript(text)) URDU_BOOST else 1f
        return maxOf(font, script)
    }

    fun maxLinesThatFit(availableHeightPx: Float, lineHeightPx: Float, wanted: Int): Int =
            if (lineHeightPx <= 0f) 1
            else minOf(wanted.coerceAtLeast(1), (availableHeightPx / lineHeightPx).toInt().coerceAtLeast(1))

    /** Y of the block top. TOP and BOTTOM keep clear of the real status area and gestures. */
    fun blockTop(position: LockPosition, canvasHeight: Int, blockHeight: Float): Float {
        val h = canvasHeight.toFloat()
        val raw =
                when (position) {
                    LockPosition.TOP -> h * 0.12f
                    LockPosition.CENTER -> (h - blockHeight) / 2f
                    LockPosition.BOTTOM -> h - h * 0.18f - blockHeight
                }
        return raw.coerceIn(0f, (h - blockHeight).coerceAtLeast(0f))
    }

    fun direction(text: String): TextDir {
        val first = BidiText.firstStrongIsRtl(text) ?: return TextDir.NEUTRAL
        val mixed = BidiText.isMixedDirection(text)
        return when {
            !mixed -> if (first) TextDir.RTL else TextDir.LTR
            first -> TextDir.MIXED_RTL_FIRST
            else -> TextDir.MIXED_LTR_FIRST
        }
    }

    /** True when the first paragraph reads right to left (so logical START is the physical right). */
    fun isRtlParagraph(text: String): Boolean = BidiText.firstStrongIsRtl(text) == true

    /** Physical edge for a logical alignment: -1 left, 0 centre, 1 right. */
    fun physicalAlign(alignment: NoteAlignment, rtl: Boolean): Int =
            when (alignment) {
                NoteAlignment.CENTER -> 0
                NoteAlignment.START -> if (rtl) 1 else -1
                NoteAlignment.END -> if (rtl) -1 else 1
            }

    /** Readable default text colour for a background of the given relative luminance (0..1). */
    fun defaultTextColor(backgroundLuma: Float): Int =
            if (backgroundLuma > 0.55f) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()

    fun luma(argb: Int): Float {
        fun ch(v: Int): Float {
            val c = v / 255f
            return if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * ch((argb shr 16) and 0xFF) + 0.7152f * ch((argb shr 8) and 0xFF) +
                0.0722f * ch(argb and 0xFF)
    }

    /**
     * Greedy word wrap using [measure] (width in px of a string). Paragraphs split on newlines,
     * over-long words are broken by code point, and when more than [maxLines] would be needed the
     * last line is ellipsised to fit. Never throws; blank text gives no lines.
     */
    fun wrap(text: String, maxWidth: Float, maxLines: Int, measure: (String) -> Float): WrappedText {
        val limit = maxLines.coerceAtLeast(1)
        val out = ArrayList<String>()
        for (para in text.replace("\r\n", "\n").split('\n')) {
            if (para.isBlank()) {
                out.add("")
                continue
            }
            var line = ""
            for (word in para.trim().split(' ').filter { it.isNotEmpty() }) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (measure(candidate) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) {
                    out.add(line)
                    line = ""
                }
                var rest = word
                while (measure(rest) > maxWidth && rest.codePointCount(0, rest.length) > 1) {
                    val cut = fitPrefix(rest, maxWidth, measure)
                    out.add(rest.substring(0, cut))
                    rest = rest.substring(cut)
                }
                line = rest
            }
            if (line.isNotEmpty()) out.add(line)
        }
        while (out.isNotEmpty() && out.first().isEmpty()) out.removeAt(0)
        while (out.isNotEmpty() && out.last().isEmpty()) out.removeAt(out.lastIndex)
        val more = out.size > limit
        val kept = out.take(limit).toMutableList()
        if (more && kept.isNotEmpty()) kept[kept.lastIndex] = ellipsize(kept.last(), maxWidth, measure)
        return WrappedText(kept, more)
    }

    /** Length (in chars, on a code point boundary) of the longest prefix that fits; at least one code point. */
    private fun fitPrefix(s: String, maxWidth: Float, measure: (String) -> Float): Int {
        var end = s.offsetByCodePoints(0, 1)
        while (end < s.length) {
            val next = s.offsetByCodePoints(end, 1)
            if (measure(s.substring(0, next)) > maxWidth) break
            end = next
        }
        return end
    }

    fun ellipsize(line: String, maxWidth: Float, measure: (String) -> Float): String {
        var s = line.trimEnd()
        if (measure(s + ELLIPSIS) <= maxWidth) return s + ELLIPSIS
        while (s.isNotEmpty() && measure(s + ELLIPSIS) > maxWidth) {
            s = s.substring(0, s.offsetByCodePoints(s.length, -1)).trimEnd()
        }
        return s + ELLIPSIS
    }
}

/** Text for the optional pinned notification. Pure so truncation is unit tested. */
object NotificationText {
    const val MAX_CHARS = 600
    const val MAX_LINES = 10
    const val SHORT_CHARS = 90

    /** Trimmed, blank lines collapsed, capped by lines and characters, never splitting a surrogate pair. */
    fun body(text: String, maxChars: Int = MAX_CHARS, maxLines: Int = MAX_LINES): String {
        val lines = text.replace("\r\n", "\n").lines().map { it.trim() }.filter { it.isNotEmpty() }
        var joined = lines.take(maxLines).joinToString("\n")
        var cut = lines.size > maxLines
        if (joined.length > maxChars) {
            var end = maxChars
            if (end > 0 && Character.isHighSurrogate(joined[end - 1])) end--
            joined = joined.substring(0, end).trimEnd()
            cut = true
        }
        return if (cut) joined + LockLayoutMath.ELLIPSIS else joined
    }

    /** One-line collapsed preview for the notification content text. */
    fun short(text: String, max: Int = SHORT_CHARS): String {
        val flat = text.replace(Regex("\\s+"), " ").trim()
        if (flat.length <= max) return flat
        var end = max
        if (end > 0 && Character.isHighSurrogate(flat[end - 1])) end--
        return flat.substring(0, end).trimEnd() + LockLayoutMath.ELLIPSIS
    }
}
