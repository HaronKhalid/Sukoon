package com.lu4p.fokuslauncher.home.layout

/**
 * Compact text codec (no JSON dependency, so it is usable from plain unit tests):
 * `v1|PRESET|TYPE:vis:order:align:size:spacing|...`. Decoding returns null for anything invalid,
 * letting callers fall back to the default configuration.
 */
object HomeLayoutCodec {
    fun encode(config: HomeLayoutConfig): String {
        val c = config.normalized()
        return buildString {
            append("v").append(c.version).append('|').append(c.preset.name)
            c.elements.forEach {
                append('|').append(it.type.name).append(':').append(if (it.visible) 1 else 0)
                append(':').append(it.order).append(':').append(it.alignment.name)
                append(':').append(it.size.name).append(':').append(it.spacing.name)
            }
        }
    }

    fun decode(raw: String?): HomeLayoutConfig? {
        if (raw.isNullOrBlank() || raw.length > 4096) return null
        return try {
            val parts = raw.split('|')
            if (parts.size < 3 || parts[0] != "v${HomeLayoutConfig.CURRENT_VERSION}") return null
            val preset = LayoutPreset.valueOf(parts[1])
            val elements = parts.drop(2).map { p ->
                val f = p.split(':')
                if (f.size != 6) return null
                val order = f[2].toInt()
                if (order !in 0..99) return null
                ElementConfig(
                        type = HomeElementType.valueOf(f[0]),
                        visible = when (f[1]) { "1" -> true; "0" -> false; else -> return null },
                        order = order,
                        alignment = ElementAlignment.valueOf(f[3]),
                        size = ElementSize.valueOf(f[4]),
                        spacing = ElementSpacing.valueOf(f[5]),
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
