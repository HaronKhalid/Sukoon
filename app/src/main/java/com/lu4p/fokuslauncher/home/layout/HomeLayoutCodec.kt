package com.lu4p.fokuslauncher.home.layout

/**
 * Compact text codec (no JSON dependency, so it is usable from plain unit tests):
 * `v2|PRESET|TYPE:vis:order:align:percent:spacing:style|...`. Version 1
 * (`TYPE:vis:order:align:SIZE_ENUM:spacing`) is still decoded and migrated: the size enum becomes
 * a percent, style 0, and element types added later fall back to their defaults. Decoding returns
 * null for anything invalid, letting callers fall back to the default configuration.
 */
object HomeLayoutCodec {
    fun encode(config: HomeLayoutConfig): String {
        val c = config.normalized()
        return buildString {
            append("v").append(c.version).append('|').append(c.preset.name)
            c.elements.forEach {
                append('|').append(it.type.name).append(':').append(if (it.visible) 1 else 0)
                append(':').append(it.order).append(':').append(it.alignment.name)
                append(':').append(it.scalePct).append(':').append(it.spacing.name)
                append(':').append(it.style)
            }
        }
    }

    fun decode(raw: String?): HomeLayoutConfig? {
        if (raw.isNullOrBlank() || raw.length > 4096) return null
        return try {
            val parts = raw.split('|')
            val version = when (parts[0]) { "v1" -> 1; "v2" -> 2; else -> return null }
            if (parts.size < 3) return null
            val fieldCount = if (version == 1) 6 else 7
            val preset = LayoutPreset.valueOf(parts[1])
            val elements = parts.drop(2).map { p ->
                val f = p.split(':')
                if (f.size != fieldCount) return null
                val order = f[2].toInt()
                if (order !in 0..99) return null
                ElementConfig(
                        type = HomeElementType.valueOf(f[0]),
                        visible = when (f[1]) { "1" -> true; "0" -> false; else -> return null },
                        order = order,
                        alignment = ElementAlignment.valueOf(f[3]),
                        scalePct = if (version == 1) ElementSize.valueOf(f[4]).percent
                        else ElementConfig.clampPercent(f[4].toInt()),
                        spacing = ElementSpacing.valueOf(f[5]),
                        style = if (version == 1) 0 else f[6].toInt().coerceAtLeast(0),
                )
            }
            if (elements.map { it.type }.distinct().size != elements.size) return null
            HomeLayoutConfig(preset = preset, elements = elements).normalized()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun decodeOrDefault(raw: String?): HomeLayoutConfig = decode(raw) ?: HomeLayoutConfig.Default
}
