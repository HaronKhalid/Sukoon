package com.lu4p.fokuslauncher.battery

data class LevelSample(val timestampMs: Long, val level: Int, val charging: Boolean)

/** Capped, locally stored level history. Encoded as "ts,level,c;..." (c = 0/1). */
object BatteryHistory {
    const val MAX_SAMPLES = 500
    const val MIN_INTERVAL_MS = 5 * 60 * 1000L

    fun encode(samples: List<LevelSample>): String =
            samples.joinToString(";") { "${it.timestampMs},${it.level},${if (it.charging) 1 else 0}" }

    fun decode(text: String?): List<LevelSample> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(';').mapNotNull { part ->
            val f = part.split(',')
            if (f.size != 3) return@mapNotNull null
            val ts = f[0].toLongOrNull() ?: return@mapNotNull null
            val lvl = f[1].toIntOrNull()?.takeIf { it in 0..100 } ?: return@mapNotNull null
            val c = f[2].toIntOrNull()?.takeIf { it == 0 || it == 1 } ?: return@mapNotNull null
            LevelSample(ts, lvl, c == 1)
        }.takeLast(MAX_SAMPLES)
    }

    /**
     * Appends [sample] unless redundant (not later than the last sample, or same level and
     * charging state within [MIN_INTERVAL_MS]). Keeps the newest [MAX_SAMPLES].
     */
    fun append(history: List<LevelSample>, sample: LevelSample): List<LevelSample> {
        val last = history.lastOrNull()
        if (last != null) {
            if (sample.timestampMs <= last.timestampMs) return history
            val same = last.level == sample.level && last.charging == sample.charging
            if (same && sample.timestampMs - last.timestampMs < MIN_INTERVAL_MS) return history
        }
        return (history + sample).takeLast(MAX_SAMPLES)
    }
}
