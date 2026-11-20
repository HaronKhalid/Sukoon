package com.lu4p.fokuslauncher.battery

enum class Recommendation { WARM, HOT, HEALTH_FLAG, LOW_SAVER_OFF }

/** Conditional tips; each is returned only when the supplied data supports it. No savings claims. */
object BatteryRecommendations {
    const val WARM_C = 40f
    const val HOT_C = 45f
    const val LOW_PERCENT = 20

    /** [batterySaverOn] null = unknown, which never triggers the Battery Saver tip. */
    fun evaluate(reading: BatteryReading, batterySaverOn: Boolean?): List<Recommendation> {
        val out = mutableListOf<Recommendation>()
        val t = reading.temperatureC
        if (t != null && t >= HOT_C) out += Recommendation.HOT
        else if (t != null && t >= WARM_C) out += Recommendation.WARM
        val h = reading.health
        if (h != null && h != HealthFlag.GOOD) out += Recommendation.HEALTH_FLAG
        val p = reading.percent
        val charging =
                reading.chargeState == ChargeState.CHARGING || reading.chargeState == ChargeState.FULL
        if (p != null && p < LOW_PERCENT && batterySaverOn == false && !charging) {
            out += Recommendation.LOW_SAVER_OFF
        }
        return out
    }
}
