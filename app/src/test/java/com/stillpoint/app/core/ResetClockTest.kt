package com.stillpoint.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ResetClockTest {
    private val sixAm = 6 * 60
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun `default reset is at midnight`() {
        assertEquals("2026-11-06", ResetClock.dayKey(at(6, 23, 59), 0))
        assertEquals("2026-11-07", ResetClock.dayKey(at(7, 0, 0), 0))
    }

    @Test
    fun `after midnight still counts as the previous day until the reset time`() {
        assertEquals("2026-11-06", ResetClock.dayKey(at(7, 0, 0), sixAm))
        assertEquals("2026-11-06", ResetClock.dayKey(at(7, 1, 0), sixAm))
        assertEquals("2026-11-06", ResetClock.dayKey(at(7, 5, 59), sixAm))
    }

    @Test
    fun `new day starts exactly at the reset time`() {
        assertEquals("2026-11-07", ResetClock.dayKey(at(7, 6, 0), sixAm))
    }

    @Test
    fun `a limit used up before midnight stays used up until the reset time`() {
        val config = Config(appLimits = mapOf("insta" to 10), resetMinute = sixAm)
        val ledger = UsageLedger("2026-11-06").add("insta", 10 * 60_000L, "2026-11-06")

        fun blockedAt(now: LocalDateTime): Boolean {
            val usage = ledger.forDay(ResetClock.dayKey(now, config.resetMinute)).usageMs
            return LimitEngine.decide("insta", config, usage).block != null
        }

        assertEquals(true, blockedAt(at(6, 23, 0)))
        assertEquals(true, blockedAt(at(7, 1, 0)))
        assertEquals(true, blockedAt(at(7, 5, 59)))
        assertEquals(false, blockedAt(at(7, 6, 0)))
    }

    @Test
    fun `changing the reset time keeps time already used`() {
        val ledger = UsageLedger("2026-11-07", mapOf("insta" to 9000L))
        val moved = ledger.relabelled(oldToday = "2026-11-07", newToday = "2026-11-06")
        assertEquals("2026-11-06", moved.day)
        assertEquals(mapOf("insta" to 9000L), moved.usageMs)
    }

    @Test
    fun `changing the reset time does not bring back an old day`() {
        val stale = UsageLedger("2026-11-01", mapOf("insta" to 9000L))
        assertTrue(stale.relabelled("2026-11-07", "2026-11-06").usageMs.isEmpty())
    }

    @Test
    fun `reset setting survives saving and older settings default to midnight`() {
        val config = Config(resetMinute = sixAm)
        assertEquals(config, Codec.decodeConfig(Codec.encodeConfig(config)))
        val old = """{"appLimits":{},"groups":[],"grayscaleApps":[],"grayscaleEnabled":true}"""
        assertEquals(0, Codec.decodeConfig(old).resetMinute)
    }

    @Test
    fun `reset times read naturally`() {
        assertEquals("midnight", formatResetTime(0))
        assertEquals("4:00 AM", formatResetTime(240))
        assertEquals("6:30 AM", formatResetTime(390))
        assertEquals("12:00 PM", formatResetTime(720))
        assertEquals("11:05 PM", formatResetTime(1385))
        assertEquals("12:15 AM", formatResetTime(15))
    }
}
