package com.lu4p.fokuslauncher.clock.styles

import com.lu4p.fokuslauncher.clock.faces.ClockFaceRegistry

private typealias M = ClockColorMode
private typealias L = ClockStyleLayout
private typealias V = ClockLineVariant

private fun s(
        id: String, name: String, font: String = "", mode: M = M.THEME, w: Int = 600, layout: L = L.INLINE,
        face: String = "classic-digital", case: ClockTextCase = ClockTextCase.AS_IS, italic: Boolean = false,
        outline: ClockOutline = ClockOutline.NONE, shadow: Boolean = false, glow: Boolean = false,
        spacing: Float = 0f, line: V = V.NONE, size: Float = 1f,
) = ClockStylePreset(id, name, face, layout, font, mode, w, case, italic, outline, shadow, glow, spacing, line, size)

/** Curated presets with stable ids. Order is the picker order; ids are never renamed. */
object ClockStyleRegistry {
    val presets: List<ClockStylePreset> = listOf(
            s("clean-sans", "Clean Sans", "inter"),
            s("thin-air", "Thin Air", "outfit", w = 200, spacing = 2f),
            s("montserrat-bold", "Montserrat Bold", "montserrat", w = 700),
            s("montserrat-black", "Montserrat Black", "montserrat", M.ACCENT, w = 900),
            s("poppins-accent", "Poppins Accent", "poppins_bold", M.ACCENT),
            s("poppins-duo", "Poppins Duo", "poppins_bold", M.TWO_TONE),
            s("outfit-gradient", "Outfit Gradient", "outfit", M.GRADIENT, w = 700),
            s("nunito-soft", "Nunito Soft", "nunito", w = 800),
            s("fredoka-chunky", "Fredoka Chunky", "fredoka", w = 700, shadow = true),
            s("fredoka-stack", "Fredoka Stack", "fredoka", M.TWO_TONE, layout = L.STACKED),
            s("bebas-tall", "Bebas Tall", "bebas_neue", w = 400, spacing = 3f, size = 1.1f),
            s("bebas-weekday", "Bebas Weekday", "bebas_neue", w = 400, spacing = 4f, line = V.WEEKDAY,
                    case = ClockTextCase.UPPER),
            s("caveat-hand", "Caveat Hand", "caveat", size = 1.15f),
            s("caveat-accent", "Caveat Accent", "caveat", M.ACCENT, w = 700, line = V.WEEKDAY_DATE, size = 1.1f),
            s("script-italic", "Script Italic", "dancing_script", italic = true),
            s("script-glow", "Script Glow", "dancing_script", M.ACCENT, w = 700, glow = true),
            s("script-gradient", "Script Gradient", "dancing_script", M.GRADIENT, w = 700),
            s("playfair-serif", "Playfair Serif", "playfair_display", w = 500),
            s("playfair-duo", "Playfair Duo", "playfair_display", M.TWO_TONE, w = 700, italic = true),
            s("grotesk-tech", "Grotesk Tech", "space_grotesk", w = 500, spacing = 1f),
            s("grotesk-outline", "Grotesk Outline", "space_grotesk", M.ACCENT, w = 700, outline = ClockOutline.HOLLOW),
            s("righteous-retro", "Righteous Retro", "righteous", M.ACCENT, w = 400),
            s("righteous-shadow", "Righteous Shadow", "righteous", w = 400, shadow = true),
            s("neon-glow", "Neon Glow", "space_grotesk", M.ACCENT, glow = true, spacing = 2f),
            s("hollow-bold", "Hollow Bold", "montserrat", w = 900, outline = ClockOutline.HOLLOW),
            s("outline-fill", "Outline Fill", "poppins_bold", outline = ClockOutline.FILLED),
            s("heavy-shadow", "Heavy Shadow", "montserrat", w = 800, shadow = true),
            s("stacked-bold", "Stacked Bold", "montserrat", w = 800, layout = L.STACKED),
            s("stacked-duo", "Stacked Duo", "poppins_bold", M.TWO_TONE, layout = L.STACKED),
            s("stacked-outline", "Stacked Outline", "montserrat", M.ACCENT, w = 900, layout = L.STACKED,
                    outline = ClockOutline.HOLLOW),
            s("stacked-script", "Stacked Script", "dancing_script", M.TWO_TONE, w = 700, layout = L.STACKED),
            s("stacked-bebas", "Stacked Bebas", "bebas_neue", w = 400, layout = L.STACKED, spacing = 2f, size = 1.05f),
            s("vertical-digits", "Vertical Digits", "outfit", layout = L.VERTICAL, size = 0.7f),
            s("vertical-accent", "Vertical Accent", "montserrat", M.TWO_TONE, w = 800, layout = L.VERTICAL, size = 0.7f),
            s("vertical-bebas", "Vertical Bebas", "bebas_neue", w = 400, layout = L.VERTICAL, size = 0.75f),
            s("tiny-caps", "Tiny Caps", "inter", w = 400, spacing = 6f, size = 0.4f, case = ClockTextCase.UPPER),
            s("tiny-weekday", "Tiny Weekday", "space_grotesk", w = 400, spacing = 5f, size = 0.4f, line = V.WEEKDAY,
                    case = ClockTextCase.UPPER),
            s("weekday-thu", "Weekday Dot", "poppins", M.TWO_TONE, w = 400, line = V.WEEKDAY_DOT),
            s("weekday-script", "Weekday Script", "dancing_script", M.ACCENT, line = V.WEEKDAY),
            s("weekday-date-duo", "Weekday Date Duo", "nunito", M.TWO_TONE, w = 800, line = V.WEEKDAY_DATE),
            s("date-serif", "Date Serif", "playfair_display", line = V.DATE),
            s("analog-accent", "Analog Accent", mode = M.ACCENT, layout = L.FACE, face = "analog-minimal"),
            s("analog-ticks-theme", "Analog Ticks", layout = L.FACE, face = "analog-ticks"),
            s("analog-ticks-accent", "Analog Ticks Accent", "poppins", M.ACCENT, layout = L.FACE, face = "analog-ticks",
                    line = V.WEEKDAY_DOT),
            s("analog-minimal-date", "Analog Minimal Date", "nunito", w = 700, layout = L.FACE, face = "analog-minimal",
                    line = V.WEEKDAY_DATE),
            s("word-serif", "Word Serif", "playfair_display", M.TWO_TONE, w = 500, layout = L.FACE, face = "word-clock"),
            s("word-script", "Word Script", "caveat", M.TWO_TONE, layout = L.FACE, face = "word-clock"),
            s("gradient-chunky", "Gradient Chunky", "fredoka", M.GRADIENT, w = 700, shadow = true),
            s("split-poppins", "Split Poppins", "poppins", M.TWO_TONE, w = 700, spacing = 1f),
            s("italic-sans", "Italic Sans", "outfit", italic = true),
            s("lower-soft", "Lower Soft", "nunito", case = ClockTextCase.LOWER, line = V.WEEKDAY, spacing = 1f),
    )

    val ids: List<String> get() = presets.map { it.id }

    fun isKnown(id: String?): Boolean = presets.any { it.id == id }

    /** Null for empty/unknown ids: the caller then falls back to the clock face (and its own fallback). */
    fun find(id: String?): ClockStylePreset? = presets.firstOrNull { it.id == id }

    /** Face the preset draws with; unknown face ids fall back to classic-digital. */
    fun faceIdFor(preset: ClockStylePreset): String =
            if (ClockFaceRegistry.isKnown(preset.faceId)) preset.faceId else ClockFaceRegistry.DEFAULT_ID
}
