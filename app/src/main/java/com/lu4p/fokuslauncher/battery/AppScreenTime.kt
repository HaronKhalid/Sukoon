package com.lu4p.fokuslauncher.battery

import android.app.usage.UsageStatsManager
import android.content.Context
import com.lu4p.fokuslauncher.usage.UsageStatsHelper

data class AppScreenTime(val packageName: String, val label: String, val foregroundMs: Long)

object AppScreenTimeReader {
    /** Pure: keep positive entries, largest first. */
    fun top(byPackage: Map<String, Long>, n: Int): List<Pair<String, Long>> =
            byPackage.filter { it.value > 0 }.toList().sortedByDescending { it.second }.take(n)

    /** Foreground time per app (last 24 h, system-bucketed). Null when usage access is missing. */
    fun query(context: Context, n: Int = 5): List<AppScreenTime>? {
        if (!UsageStatsHelper.hasUsageAccess(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val map =
                usm.queryAndAggregateUsageStats(end - 24 * 60 * 60 * 1000L, end)
                        .mapValues { it.value.totalTimeInForeground }
        val pm = context.packageManager
        return top(map, n).map { (pkg, ms) ->
            val label =
                    try {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    } catch (_: Exception) {
                        pkg
                    }
            AppScreenTime(pkg, label, ms)
        }
    }
}
