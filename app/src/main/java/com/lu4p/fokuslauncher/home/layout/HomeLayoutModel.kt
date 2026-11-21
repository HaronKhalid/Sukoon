package com.lu4p.fokuslauncher.home.layout

import com.lu4p.fokuslauncher.home.layout.HomeElementType.*

/**
 * Home-screen elements that can be configured. Each appears exactly once in a [HomeLayoutConfig].
 * Declaration order is persisted (default element order), so new entries are only ever appended.
 * [styleCount] is the number of style variants; [optionalWidget] marks elements added through the
 * widget gallery.
 */
enum class HomeElementType(
        val defaultVisible: Boolean = true,
        val styleCount: Int = 1,
        val optionalWidget: Boolean = false,
) {
    CLOCK, DATE, BATTERY, NOTES, FAVOURITES,
    // App search, focus status and the spacer are off by default so the default configuration
    // reproduces the existing home screen.
    APP_SEARCH(false), FOCUS_STATUS(false), SPACER(false),
    DATE_CARD(false, 3, true), BATTERY_RING(false, 3, true), MONTH_CALENDAR(false, 2, true),
    FOCUS_RING(false, 2, true), GREETING(false, 3, true), PROGRESS_BAR(false, 3, true),
}

enum class ElementAlignment { START, CENTER, END }

/** Legacy size steps (layout v1). Only used to read old data; sizes are now a continuous percent. */
enum class ElementSize(val percent: Int) { SMALL(85), MEDIUM(100), LARGE(120) }

enum class ElementSpacing(val dp: Int) { NONE(0), COMPACT(4), NORMAL(8), RELAXED(16) }

/** Presets are ordinary configurations; applying one just replaces the config, which stays editable. */
enum class LayoutPreset { DEFAULT, PURE_MINIMAL, AESTHETIC, FOCUS, INFORMATION, CUSTOM }

data class ElementConfig(
        val type: HomeElementType,
        val visible: Boolean = true,
        val order: Int = type.ordinal,
        val alignment: ElementAlignment = ElementAlignment.START,
        /** Continuous size in percent, [MIN_PERCENT]..[MAX_PERCENT]. */
        val scalePct: Int = 100,
        val spacing: ElementSpacing = ElementSpacing.NORMAL,
        /** Style variant index, 0 until [HomeElementType.styleCount]. */
        val style: Int = 0,
) {
    val scale: Float get() = clampPercent(scalePct) / 100f

    companion object {
        const val MIN_PERCENT = 50
        const val MAX_PERCENT = 200
        fun clampPercent(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

        fun defaultFor(type: HomeElementType) = ElementConfig(type, visible = type.defaultVisible)
    }
}

data class HomeLayoutConfig(
        val version: Int = CURRENT_VERSION,
        val preset: LayoutPreset = LayoutPreset.DEFAULT,
        val elements: List<ElementConfig> = defaultElements(),
) {
    fun element(type: HomeElementType): ElementConfig =
            elements.firstOrNull { it.type == type } ?: ElementConfig.defaultFor(type)

    fun isVisible(type: HomeElementType): Boolean = element(type).visible

    /** Elements sorted by order, ties broken by declaration order. */
    fun ordered(): List<ElementConfig> = elements.sortedWith(compareBy({ it.order }, { it.type.ordinal }))

    /** Edits one element; the config then counts as [LayoutPreset.CUSTOM]. */
    fun update(type: HomeElementType, change: (ElementConfig) -> ElementConfig): HomeLayoutConfig =
            copy(
                    preset = LayoutPreset.CUSTOM,
                    elements = elements.map { if (it.type == type) change(it) else it },
            ).normalized()

    /** Moves [type] one step earlier (-1) or later (+1) in the order. */
    fun move(type: HomeElementType, delta: Int): HomeLayoutConfig {
        val list = ordered().toMutableList()
        val from = list.indexOfFirst { it.type == type }
        if (from < 0) return this
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return this
        list.add(to, list.removeAt(from))
        return copy(
                preset = LayoutPreset.CUSTOM,
                elements = list.mapIndexed { i, e -> e.copy(order = i) },
        ).normalized()
    }

    /** Guarantees one entry per type, dense unique orders and in-range scale/style. Never throws. */
    fun normalized(): HomeLayoutConfig {
        val byType = elements.groupBy { it.type }
        val full = HomeElementType.entries.map { type ->
            (byType[type]?.first() ?: ElementConfig.defaultFor(type)).let { e ->
                e.copy(
                        scalePct = ElementConfig.clampPercent(e.scalePct),
                        style = e.style.coerceIn(0, type.styleCount - 1),
                )
            }
        }
        val ranked = full.sortedWith(compareBy({ it.order }, { it.type.ordinal }))
                .mapIndexed { i, e -> e.copy(order = i) }
        return copy(version = CURRENT_VERSION, elements = ranked.sortedBy { it.type.ordinal })
    }

    companion object {
        /** 2 adds continuous size percent, style variants and the widget-gallery element types. */
        const val CURRENT_VERSION = 2

        fun defaultElements(): List<ElementConfig> = HomeElementType.entries.map { ElementConfig.defaultFor(it) }

        val Default = HomeLayoutConfig()

        /** Clock face id a preset shows; empty means "keep the legacy clock style". */
        fun presetClockFace(preset: LayoutPreset): String = when (preset) {
            LayoutPreset.PURE_MINIMAL -> "minimal-text"
            LayoutPreset.AESTHETIC -> "analog-minimal"
            LayoutPreset.FOCUS -> "split-accent"
            LayoutPreset.INFORMATION -> "classic-digital"
            LayoutPreset.DEFAULT, LayoutPreset.CUSTOM -> ""
        }

        fun forPreset(preset: LayoutPreset): HomeLayoutConfig {
            fun cfg(
                    order: List<HomeElementType>,
                    align: ElementAlignment = ElementAlignment.START,
                    percents: Map<HomeElementType, Int> = emptyMap(),
                    spacing: ElementSpacing = ElementSpacing.NORMAL,
            ): HomeLayoutConfig {
                val all = order + HomeElementType.entries.filter { it !in order }
                return HomeLayoutConfig(
                        preset = preset,
                        elements = all.mapIndexed { i, t ->
                            ElementConfig(
                                    type = t,
                                    visible = t in order,
                                    order = i,
                                    alignment = align,
                                    scalePct = percents[t] ?: 100,
                                    spacing = spacing,
                            )
                        },
                ).normalized()
            }
            return when (preset) {
                LayoutPreset.DEFAULT, LayoutPreset.CUSTOM -> Default.copy(preset = preset)
                LayoutPreset.PURE_MINIMAL ->
                    cfg(listOf(CLOCK, FAVOURITES), percents = mapOf(CLOCK to 90),
                            spacing = ElementSpacing.RELAXED)
                LayoutPreset.AESTHETIC ->
                    cfg(listOf(SPACER, CLOCK, DATE_CARD, FAVOURITES), align = ElementAlignment.CENTER,
                            percents = mapOf(CLOCK to 120), spacing = ElementSpacing.RELAXED)
                LayoutPreset.FOCUS ->
                    cfg(listOf(CLOCK, FOCUS_RING, FAVOURITES), spacing = ElementSpacing.COMPACT)
                LayoutPreset.INFORMATION ->
                    cfg(listOf(CLOCK, DATE, BATTERY_RING, MONTH_CALENDAR, NOTES, FAVOURITES, APP_SEARCH),
                            percents = mapOf(BATTERY_RING to 80, MONTH_CALENDAR to 90),
                            spacing = ElementSpacing.COMPACT)
            }
        }
    }
}
