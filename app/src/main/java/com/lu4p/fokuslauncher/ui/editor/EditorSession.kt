package com.lu4p.fokuslauncher.ui.editor

import com.lu4p.fokuslauncher.backdrop.Backdrops
import com.lu4p.fokuslauncher.home.layout.ClockSettings
import com.lu4p.fokuslauncher.home.layout.ElementConfig
import com.lu4p.fokuslauncher.home.layout.HomeElementType
import com.lu4p.fokuslauncher.home.layout.HomeLayoutConfig
import com.lu4p.fokuslauncher.home.layout.LayoutPreset
import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ClockFaces
import com.lu4p.fokuslauncher.theme.ThemeColorMath
import com.lu4p.fokuslauncher.theme.ThemeDefinition
import com.lu4p.fokuslauncher.theme.ThemeGradient
import com.lu4p.fokuslauncher.typography.TextElement

/**
 * Everything the Home Editor can change, as one immutable value. [theme] is the untinted theme;
 * the background tint is kept separately (and non-destructively) in [tintArgb] / [tintPct] and
 * folded in by [effectiveTheme], so dragging the tint slider back to 0 restores the exact colours.
 */
data class EditorSnapshot(
        val theme: ThemeDefinition,
        val layout: HomeLayoutConfig,
        val clock: ClockSettings,
        /** Stored launcher font value ("" = system default). */
        val appFont: String = "",
        val elementFonts: Map<TextElement, String> = emptyMap(),
        val tintArgb: Int = 0,
        val tintPct: Int = 0,
) {
    val effectiveTheme: ThemeDefinition
        get() = if (tintArgb == 0 || tintPct <= 0) theme else EditorOps.tinted(theme, tintArgb, tintPct)

    val clockFaceId: String get() = clock.faceId.ifEmpty { theme.style.resolvedClockFaceId }
    val clockScalePct: Int
        get() = layout.elements.firstOrNull { it.type == HomeElementType.CLOCK }?.scalePct ?: 100
}

/**
 * Undo/reset logic for the editor, free of Android so it is unit-tested. Edits that share a
 * [coalesceKey] (a slider drag) collapse into one undo step until [endGesture] is called.
 * Reset returns to the state the editor opened with and is itself undoable.
 */
class EditorSession(initial: EditorSnapshot, private val maxUndo: Int = 50) {
    val baseline: EditorSnapshot = initial
    var current: EditorSnapshot = initial
        private set

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private var openGestureKey: String? = null

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val isModified: Boolean get() = current != baseline

    /** Applies [change]; returns false (and records nothing) when it changes nothing. */
    fun edit(coalesceKey: String? = null, change: (EditorSnapshot) -> EditorSnapshot): Boolean {
        val next = change(current)
        if (next == current) return false
        val continuing = coalesceKey != null && coalesceKey == openGestureKey
        if (!continuing) push(current)
        openGestureKey = coalesceKey
        current = next
        return true
    }

    fun endGesture() {
        openGestureKey = null
    }

    fun undo(): Boolean {
        val prev = undoStack.removeLastOrNull() ?: return false
        current = prev
        openGestureKey = null
        return true
    }

    fun reset(): Boolean {
        if (current == baseline) return false
        push(current)
        current = baseline
        openGestureKey = null
        return true
    }

    private fun push(s: EditorSnapshot) {
        undoStack.addLast(s)
        while (undoStack.size > maxUndo) undoStack.removeFirst()
    }
}

/** Pure edit operations. Operations that could make text unreadable return null instead. */
object EditorOps {
    /** Minimum contrast an editor change must keep between primary text and every background stop. */
    const val MIN_CONTRAST = 4.5

    fun selectTheme(s: EditorSnapshot, theme: ThemeDefinition): EditorSnapshot {
        val preset = LayoutPreset.entries.firstOrNull { it.name == theme.style.layoutPreset } ?: LayoutPreset.DEFAULT
        val face = theme.style.resolvedClockFaceId
        return s.copy(
                theme = theme,
                layout = HomeLayoutConfig.forPreset(preset),
                clock = s.clock.copy(faceId = face, accentArgb = 0),
                tintArgb = 0,
                tintPct = 0,
        )
    }

    /**
     * Sets the backdrop. When its light/dark nature disagrees with the theme the paired built-in
     * theme's surfaces and text colours are adopted (accent is kept) so text stays readable.
     */
    fun setBackdrop(s: EditorSnapshot, id: String): EditorSnapshot {
        val info = Backdrops.info(id)
        var t = s.theme
        if (info != null) {
            val wantDark = info.dark
            val isDark = t.appearance == com.lu4p.fokuslauncher.theme.ThemeAppearance.DARK
            if (wantDark != isDark) {
                val pair = BuiltInThemes.backdropThemes.first { it.style.backdrop == id }
                t = t.copy(
                        appearance = pair.appearance,
                        colors = pair.colors.copy(accent = t.colors.accent),
                        gradient = pair.gradient,
                )
            }
        }
        return s.copy(theme = t.copy(style = t.style.copy(backdrop = id)))
    }

    fun setBackdropIntensity(s: EditorSnapshot, intensity: Float): EditorSnapshot =
            s.copy(theme = s.theme.copy(style = s.theme.style.copy(backdropIntensity = intensity.coerceIn(0.2f, 1f))))

    fun setAccent(s: EditorSnapshot, argb: Int): EditorSnapshot =
            s.copy(theme = s.theme.copy(colors = s.theme.colors.copy(accent = opaque(argb))))

    /** Null when the colour would be hard to read on the (tinted) background. */
    fun setTextColor(s: EditorSnapshot, argb: Int): EditorSnapshot? {
        val c = opaque(argb)
        if (ThemeColorMath.minContrast(c, s.effectiveTheme.backgroundStops) < MIN_CONTRAST) return null
        val secondary = ThemeColorMath.blend(c, s.effectiveTheme.representativeBackground, 0.65)
        return s.copy(theme = s.theme.copy(colors = s.theme.colors.copy(primaryText = c, secondaryText = secondary)))
    }

    /** Null when the tint would make the current text unreadable. */
    fun setTint(s: EditorSnapshot, tintArgb: Int, pct: Int): EditorSnapshot? {
        val next = s.copy(tintArgb = if (pct <= 0) 0 else opaque(tintArgb), tintPct = pct.coerceIn(0, MAX_TINT_PCT))
        val ok = ThemeColorMath.minContrast(next.theme.colors.primaryText, next.effectiveTheme.backgroundStops) >= MIN_CONTRAST
        return if (ok) next else null
    }

    fun setLayoutPreset(s: EditorSnapshot, preset: LayoutPreset): EditorSnapshot =
            s.copy(
                    layout = HomeLayoutConfig.forPreset(preset),
                    clock = s.clock.copy(faceId = HomeLayoutConfig.presetClockFace(preset).ifEmpty { s.clock.faceId }, accentArgb = 0),
            )

    fun setClockFace(s: EditorSnapshot, faceId: String): EditorSnapshot =
            s.copy(clock = s.clock.copy(faceId = faceId.takeIf { FACE_ID.matches(it) } ?: ClockFaces.DEFAULT, accentArgb = 0))

    fun setClockScale(s: EditorSnapshot, pct: Int): EditorSnapshot =
            s.copy(layout = s.layout.copy(
                    preset = LayoutPreset.CUSTOM,
                    elements = s.layout.elements.map {
                        if (it.type == HomeElementType.CLOCK) it.copy(scalePct = ElementConfig.clampPercent(pct)) else it
                    }))

    fun setAppFont(s: EditorSnapshot, value: String): EditorSnapshot = s.copy(appFont = value)

    fun setElementFont(s: EditorSnapshot, element: TextElement, value: String): EditorSnapshot =
            s.copy(elementFonts = if (value.isEmpty()) s.elementFonts - element else s.elementFonts + (element to value))

    const val MAX_TINT_PCT = 60

    /** Any well-formed id: faces registered later (clock styles) are not limited to the theme's known list. */
    private val FACE_ID = Regex("[a-z0-9-]{1,32}")

    internal fun tinted(theme: ThemeDefinition, tint: Int, pct: Int): ThemeDefinition {
        val a = pct.coerceIn(0, MAX_TINT_PCT) / 100.0
        fun mix(c: Int) = ThemeColorMath.blend(tint, c, a)
        return theme.copy(
                colors = theme.colors.copy(background = mix(theme.colors.background), surface = mix(theme.colors.surface)),
                gradient = theme.gradient?.let { ThemeGradient(it.stops.map(::mix), it.angleDegrees) },
        )
    }

    private fun opaque(argb: Int) = 0xFF000000.toInt() or (argb and 0xFFFFFF)
}
