package com.lu4p.fokuslauncher.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.batteryDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "battery_preferences")

enum class PowerProfile { BALANCED, POWER_SAVER, EXTREME_FOCUS }

/** Reads ACTION_BATTERY_CHANGED. Registering with a null receiver returns the sticky intent only. */
object BatteryReader {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    fun readSticky(context: Context): BatteryReading? =
            context.registerReceiver(null, filter)?.let { parse(it) }

    fun parse(i: Intent): BatteryReading {
        fun int(name: String) =
                if (i.hasExtra(name)) i.getIntExtra(name, RawBattery.MISSING) else RawBattery.MISSING
        return BatteryParser.parse(
                RawBattery(
                        level = int(BatteryManager.EXTRA_LEVEL),
                        scale = int(BatteryManager.EXTRA_SCALE),
                        status = int(BatteryManager.EXTRA_STATUS),
                        plugged = int(BatteryManager.EXTRA_PLUGGED),
                        temperatureTenthsC = int(BatteryManager.EXTRA_TEMPERATURE),
                        voltageMv = int(BatteryManager.EXTRA_VOLTAGE),
                        technology = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY),
                        health = int(BatteryManager.EXTRA_HEALTH),
                )
        )
    }

    fun isBatterySaverOn(context: Context): Boolean? =
            (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode
}

/** Local-only battery level history (capped) and the chosen launcher power profile. */
@Singleton
class BatteryRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val historyKey = stringPreferencesKey("level_history_v1")
    private val profileKey = stringPreferencesKey("power_profile")

    val history: Flow<List<LevelSample>> =
            context.batteryDataStore.data.map { BatteryHistory.decode(it[historyKey]) }

    val profile: Flow<PowerProfile> =
            context.batteryDataStore.data.map { p ->
                PowerProfile.entries.firstOrNull { it.name == p[profileKey] } ?: PowerProfile.BALANCED
            }

    suspend fun record(sample: LevelSample) {
        context.batteryDataStore.edit { p ->
            val next = BatteryHistory.append(BatteryHistory.decode(p[historyKey]), sample)
            p[historyKey] = BatteryHistory.encode(next)
        }
    }

    suspend fun clearHistory() {
        context.batteryDataStore.edit { it.remove(historyKey) }
    }

    suspend fun setProfile(profile: PowerProfile) {
        context.batteryDataStore.edit { it[profileKey] = profile.name }
    }
}
