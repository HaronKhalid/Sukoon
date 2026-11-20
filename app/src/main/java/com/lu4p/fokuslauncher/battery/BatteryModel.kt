package com.lu4p.fokuslauncher.battery

import android.os.BatteryManager

enum class PlugType { NONE, AC, USB, WIRELESS, DOCK, OTHER }

enum class ChargeState { CHARGING, DISCHARGING, FULL, NOT_CHARGING }

enum class HealthFlag { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, FAILURE, COLD }

/** Raw values as read from the ACTION_BATTERY_CHANGED sticky intent; MISSING = extra absent. */
data class RawBattery(
        val level: Int = MISSING,
        val scale: Int = MISSING,
        val status: Int = MISSING,
        val plugged: Int = MISSING,
        val temperatureTenthsC: Int = MISSING,
        val voltageMv: Int = MISSING,
        val technology: String? = null,
        val health: Int = MISSING,
) {
    companion object {
        const val MISSING = Int.MIN_VALUE
    }
}

/** Parsed reading. A null field means Android did not report it (shown as Not available). */
data class BatteryReading(
        val percent: Int?,
        val chargeState: ChargeState?,
        val plug: PlugType?,
        val temperatureC: Float?,
        val voltageMv: Int?,
        val technology: String?,
        val health: HealthFlag?,
)

object BatteryParser {
    private const val PLUGGED_DOCK = 8 // BATTERY_PLUGGED_DOCK (API 33)

    fun parse(raw: RawBattery): BatteryReading {
        val percent =
                if (raw.level >= 0 && raw.scale > 0) {
                    (raw.level * 100f / raw.scale).toInt().coerceIn(0, 100)
                } else null
        val state =
                when (raw.status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> ChargeState.CHARGING
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeState.DISCHARGING
                    BatteryManager.BATTERY_STATUS_FULL -> ChargeState.FULL
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeState.NOT_CHARGING
                    else -> null
                }
        val plug =
                when {
                    raw.plugged == RawBattery.MISSING || raw.plugged < 0 -> null
                    raw.plugged == 0 -> PlugType.NONE
                    raw.plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> PlugType.AC
                    raw.plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> PlugType.USB
                    raw.plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> PlugType.WIRELESS
                    raw.plugged and PLUGGED_DOCK != 0 -> PlugType.DOCK
                    else -> PlugType.OTHER
                }
        val temp =
                if (raw.temperatureTenthsC != RawBattery.MISSING) raw.temperatureTenthsC / 10f
                else null
        val voltage = raw.voltageMv.takeIf { it != RawBattery.MISSING && it > 0 }
        val tech = raw.technology?.trim()?.takeIf { it.isNotEmpty() }
        val health =
                when (raw.health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> HealthFlag.GOOD
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> HealthFlag.OVERHEAT
                    BatteryManager.BATTERY_HEALTH_DEAD -> HealthFlag.DEAD
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> HealthFlag.OVER_VOLTAGE
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> HealthFlag.FAILURE
                    BatteryManager.BATTERY_HEALTH_COLD -> HealthFlag.COLD
                    else -> null // UNKNOWN or absent
                }
        return BatteryReading(percent, state, plug, temp, voltage, tech, health)
    }
}
