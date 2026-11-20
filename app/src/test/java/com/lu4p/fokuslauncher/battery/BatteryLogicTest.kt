package com.lu4p.fokuslauncher.battery

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryLogicTest {
    @Test
    fun missingValuesAreNull() {
        val r = BatteryParser.parse(RawBattery())
        assertNull(r.percent)
        assertNull(r.chargeState)
        assertNull(r.plug)
        assertNull(r.temperatureC)
        assertNull(r.voltageMv)
        assertNull(r.technology)
        assertNull(r.health)
    }

    @Test
    fun parsesFullReading() {
        val r =
                BatteryParser.parse(
                        RawBattery(
                                level = 42, scale = 100,
                                status = BatteryManager.BATTERY_STATUS_CHARGING,
                                plugged = BatteryManager.BATTERY_PLUGGED_USB,
                                temperatureTenthsC = 312, voltageMv = 3950,
                                technology = " Li-poly ",
                                health = BatteryManager.BATTERY_HEALTH_GOOD,
                        )
                )
        assertEquals(42, r.percent)
        assertEquals(ChargeState.CHARGING, r.chargeState)
        assertEquals(PlugType.USB, r.plug)
        assertEquals(31.2f, r.temperatureC!!, 0.001f)
        assertEquals(3950, r.voltageMv)
        assertEquals("Li-poly", r.technology)
        assertEquals(HealthFlag.GOOD, r.health)
    }

    @Test
    fun invalidValuesAreNotInvented() {
        val r =
                BatteryParser.parse(
                        RawBattery(
                                level = -1, scale = 100, voltageMv = 0, technology = "  ",
                                health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
                                status = BatteryManager.BATTERY_STATUS_UNKNOWN,
                        )
                )
        assertNull(r.percent)
        assertNull(r.voltageMv)
        assertNull(r.technology)
        assertNull(r.health)
        assertNull(r.chargeState)
        assertEquals(PlugType.NONE, BatteryParser.parse(RawBattery(plugged = 0)).plug)
    }

    @Test
    fun historyCapKeepsNewest() {
        var h = emptyList<LevelSample>()
        for (i in 1..(BatteryHistory.MAX_SAMPLES + 50)) {
            h = BatteryHistory.append(h, LevelSample(i * 1_000_000L, i % 100, false))
        }
        assertEquals(BatteryHistory.MAX_SAMPLES, h.size)
        assertEquals((BatteryHistory.MAX_SAMPLES + 50) * 1_000_000L, h.last().timestampMs)
    }

    @Test
    fun historyDedupesAndRoundTrips() {
        val a = LevelSample(1_000_000L, 50, false)
        var h = BatteryHistory.append(emptyList(), a)
        h = BatteryHistory.append(h, LevelSample(1_000_100L, 50, false)) // too soon, same
        h = BatteryHistory.append(h, LevelSample(500L, 49, false)) // older
        assertEquals(1, h.size)
        h = BatteryHistory.append(h, LevelSample(1_000_200L, 49, true))
        assertEquals(2, h.size)
        assertEquals(h, BatteryHistory.decode(BatteryHistory.encode(h)))
        assertTrue(BatteryHistory.decode("garbage;1,2;x,y,z;5,200,1").isEmpty())
    }

    private fun reading(pct: Int?, temp: Float? = null, health: HealthFlag? = null, state: ChargeState? = ChargeState.DISCHARGING) =
            BatteryReading(pct, state, null, temp, null, null, health)

    @Test
    fun recommendationsOnlyWhenSupported() {
        assertTrue(BatteryRecommendations.evaluate(reading(null), null).isEmpty())
        assertTrue(BatteryRecommendations.evaluate(reading(50, 30f, HealthFlag.GOOD), false).isEmpty())
        assertEquals(listOf(Recommendation.WARM), BatteryRecommendations.evaluate(reading(50, 41f), null))
        assertEquals(listOf(Recommendation.HOT), BatteryRecommendations.evaluate(reading(50, 46f), null))
        assertEquals(
                listOf(Recommendation.HEALTH_FLAG),
                BatteryRecommendations.evaluate(reading(50, null, HealthFlag.COLD), null),
        )
        assertEquals(listOf(Recommendation.LOW_SAVER_OFF), BatteryRecommendations.evaluate(reading(15), false))
        assertTrue(BatteryRecommendations.evaluate(reading(15), null).isEmpty())
        assertTrue(BatteryRecommendations.evaluate(reading(15), true).isEmpty())
        assertTrue(BatteryRecommendations.evaluate(reading(15, state = ChargeState.CHARGING), false).isEmpty())
    }

    @Test
    fun topAppsSortedAndPositiveOnly() {
        val top = AppScreenTimeReader.top(mapOf("a" to 5L, "b" to 0L, "c" to 9L, "d" to 1L), 2)
        assertEquals(listOf("c" to 9L, "a" to 5L), top)
    }
}
