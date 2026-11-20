package com.lu4p.fokuslauncher.wallpaper

/** Pure helpers for wallpaper cropping, colour extraction and the recent-picks list. */
data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
}

object WallpaperMath {
    /** Largest centred rectangle of the source that has the target aspect ratio. */
    fun centerCrop(srcW: Int, srcH: Int, targetW: Int, targetH: Int): CropRect {
        require(srcW > 0 && srcH > 0 && targetW > 0 && targetH > 0) { "sizes must be positive" }
        // Compare srcW/srcH with targetW/targetH via cross-multiplication (no float error).
        val srcWide = srcW.toLong() * targetH > targetW.toLong() * srcH
        return if (srcWide) {
            val w = ((srcH.toLong() * targetW) / targetH).toInt().coerceIn(1, srcW)
            val left = (srcW - w) / 2
            CropRect(left, 0, left + w, srcH)
        } else {
            val h = ((srcW.toLong() * targetH) / targetW).toInt().coerceIn(1, srcH)
            val top = (srcH - h) / 2
            CropRect(0, top, srcW, top + h)
        }
    }

    /** Power-of-two sample size so the decoded image is still at least [maxEdge] on its long side. */
    fun sampleSize(srcW: Int, srcH: Int, maxEdge: Int): Int {
        var s = 1
        val longest = maxOf(srcW, srcH)
        while (longest / (s * 2) >= maxEdge) s *= 2
        return s
    }

    /** Average opaque ARGB colour (channel-wise mean); null when nothing is opaque. */
    fun averageColor(pixels: IntArray): Int? {
        var n = 0L
        var r = 0L
        var g = 0L
        var b = 0L
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            n++
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        if (n == 0L) return null
        return (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
    }

    /** Most common colour after quantising each channel to 4 levels; ties pick the lowest key. */
    fun dominantColor(pixels: IntArray): Int? {
        val counts = HashMap<Int, Int>()
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val key = (((p shr 22) and 3) shl 4) or (((p shr 14) and 3) shl 2) or ((p shr 6) and 3)
            counts[key] = (counts[key] ?: 0) + 1
        }
        val best =
            counts.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .firstOrNull() ?: return null
        val r = ((best.key shr 4) and 3) * 64 + 32
        val g = ((best.key shr 2) and 3) * 64 + 32
        val b = (best.key and 3) * 64 + 32
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** New recents (newest first, de-duplicated, capped) and the entries that fell off. */
    fun pushRecent(existing: List<String>, added: String, max: Int): Pair<List<String>, List<String>> {
        val all = listOf(added) + existing.filter { it != added }
        return all.take(max) to all.drop(max)
    }
}
