package com.lu4p.fokuslauncher.home.layout

import com.lu4p.fokuslauncher.home.layout.HomeElementType.*

/** Home-screen elements that can be configured. Each appears exactly once in a [HomeLayoutConfig]. */
enum class HomeElementType { CLOCK, DATE, BATTERY, NOTES, FAVOURITES, APP_SEARCH, FOCUS_STATUS, SPACER }

enum class ElementAlignment { START, CENTER, END }

enum class ElementSize(val scale: Float) { SMALL(0.85f), MEDIUM(1f), LARGE(1.2f) }

enum class ElementSpacing(val dp: Int) { NONE(0), COMPACT(4), NORMAL(8), RELAXED(16) }

/** Presets are ordinary configurations; applying one just replaces the config, which stays editable. */
enum class LayoutPreset { DEFAULT, PURE_MINIMAL, AESTHETIC, FOCUS, INFORMATION, CUSTOM }

data class ElementConfig(
        val type: HomeElementType,
        val visible: Boolean = true,
        val order: Int = type.ordinal,
        val alignment: ElementAlignment = ElementAlignment.START,
        val size: ElementSize = ElementSize.MEDIUM,
        val spacing: ElementSpacing = ElementSpacing.NORMAL,
)

data class HomeLayoutConfig(
        val version: Int = CURRENT_VERSION,
        val preset: LayoutPreset = LayoutPreset.DEFAULT,
        val elements: List<ElementConfig> = defaultElements(),
) {
    fun element(type: HomeElementType): ElementConfig =
            elements.firstOrNull { it.type == type } ?: ElementConfig(type)

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

    /** Guarantees one entry per type and dense, unique orders. Never throws. */
    fun normalized(): HomeLayoutConfig {
        val byType = elements.groupBy { it.type }
        val full = HomeElementType.entries.map { byType[it]?.first() ?: ElementConfig(it) }
        val ranked = full.sortedWith(compareBy({ it.order }, { it.type.ordinal }))
                .mapIndexed { i, e -> e.copy(order = i) }
        return copy(version = CURRENT_VERSION, elements = ranked.sortedBy { it.type.ordinal })
    }

    companion object {
        const val CURRENT_VERSION = 1

        fun defaultElements(): List<ElementConfig> =
                HomeElementType.entries.map {
                    // App search, focus status and the spacer are new and off by default so the
                    // default configuration reproduces the existing home screen.
                    val off = it == HomeElementType.APP_SEARCH || it == HomeElementType.FOCUS_STATUS ||
                            it == HomeElementType.SPACER
                    ElementConfig(it, visible = !off)
                }

        val Default = HomeLayoutConfig()

        fun forPreset(preset: LayoutPreset): HomeLayoutConfig {
            fun cfg(
                    order: List<HomeElementType>,
                    visible: Set<HomeElementType>,
                    align: ElementAlignment = ElementAlignment.START,
                    clockSize: ElementSize = ElementSize.MEDIUM,
                    spacing: ElementSpacing = ElementSpacing.NORMAL,
            ): HomeLayoutConfig {
                val all = order + HomeElementType.entries.filter { it !in order }
                return HomeLayoutConfig(
                        preset = preset,
                        elements = all.mapIndexed { i, t ->
                            ElementConfig(
                                    type = t,
                                    visible = t in visible,
                                    order = i,
                                    alignment = align,
                                    size = if (t == HomeElementType.CLOCK) clockSize else ElementSize.MEDIUM,
                                    spacing = spacing,
                            )
                        },
                ).normalized()
            }
            return when (preset) {
                LayoutPreset.DEFAULT, LayoutPreset.CUSTOM -> Default.copy(preset = preset)
                LayoutPreset.PURE_MINIMAL ->
                    cfg(listOf(CLOCK, FAVOURITES), setOf(CLOCK, FAVOURITES),
                            clockSize = ElementSize.SMALL, spacing = ElementSpacing.RELAXED)
                LayoutPreset.AESTHETIC ->
                    cfg(listOf(SPACER, CLOCK, DATE, FAVOURITES),
                            setOf(SPACER, CLOCK, DATE, FAVOURITES),
                            align = ElementAlignment.CENTER, clockSize = ElementSize.LARGE,
                            spacing = ElementSpacing.RELAXED)
                LayoutPreset.FOCUS ->
                    cfg(listOf(CLOCK, FOCUS_STATUS, FAVOURITES), setOf(CLOCK, FOCUS_STATUS, FAVOURITES),
                            spacing = ElementSpacing.COMPACT)
                LayoutPreset.INFORMATION ->
                    cfg(listOf(CLOCK, DATE, BATTERY, NOTES, FAVOURITES, APP_SEARCH),
                            setOf(CLOCK, DATE, BATTERY, NOTES, FAVOURITES, APP_SEARCH),
                            spacing = ElementSpacing.COMPACT)
            }
        }
    }
}
