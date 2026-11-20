package com.lu4p.fokuslauncher.notes

/** Pure helpers for mixed Urdu/English text; Compose does the actual bidi shaping and layout. */
object BidiText {
    private fun isStrongRtl(cp: Int): Boolean =
            when (Character.getDirectionality(cp)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> true
                else -> false
            }

    private fun isStrongLtr(cp: Int): Boolean =
            Character.getDirectionality(cp) == Character.DIRECTIONALITY_LEFT_TO_RIGHT

    /**
     * Paragraph direction by the first strong character (the Unicode default rule). Digits and
     * punctuation are neutral. Returns null when no strong character exists.
     */
    fun firstStrongIsRtl(text: String): Boolean? {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (isStrongRtl(cp)) return true
            if (isStrongLtr(cp)) return false
            i += Character.charCount(cp)
        }
        return null
    }

    /** True when the text has both RTL and LTR letters. */
    fun isMixedDirection(text: String): Boolean {
        var rtl = false
        var ltr = false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            if (isStrongRtl(cp)) rtl = true else if (isStrongLtr(cp)) ltr = true
            if (rtl && ltr) return true
            i += Character.charCount(cp)
        }
        return false
    }

    /** Whether the text contains Arabic-script letters (Urdu uses this script). */
    fun containsArabicScript(text: String): Boolean =
            text.codePoints().anyMatch { Character.UnicodeScript.of(it) == Character.UnicodeScript.ARABIC }
}
