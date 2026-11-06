package com.stillpoint.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitEngineTest {
    private val minute = 60_000L
    private val social = LimitGroup("g1", "Social", 30, setOf("insta", "x", "reddit"))

    @Test
    fun `app under its own limit is allowed`() {
        val config = Config(appLimits = mapOf("insta" to 10))
        assertNull(LimitEngine.decide("insta", config, mapOf("insta" to 9 * minute)).block)
    }

    @Test
    fun `app is blocked exactly when its limit is reached`() {
        val config = Config(appLimits = mapOf("insta" to 10))
        assertEquals(
            BlockReason.AppLimit(10),
            LimitEngine.decide("insta", config, mapOf("insta" to 10 * minute)).block,
        )
    }

    @Test
    fun `group time is combined across its apps`() {
        val config = Config(groups = listOf(social))
        val usage = mapOf("insta" to 10 * minute, "x" to 10 * minute, "reddit" to 10 * minute)
        assertEquals(30 * minute, LimitEngine.groupUsageMs(social, usage))
        assertEquals(BlockReason.GroupLimit("Social", 30), LimitEngine.decide("insta", config, usage).block)
    }

    @Test
    fun `used up group blocks an app that was not used today`() {
        val config = Config(groups = listOf(social))
        val usage = mapOf("insta" to 30 * minute)
        assertEquals(BlockReason.GroupLimit("Social", 30), LimitEngine.decide("reddit", config, usage).block)
    }

    @Test
    fun `used up group does not affect apps outside it`() {
        val config = Config(groups = listOf(social))
        assertNull(LimitEngine.decide("maps", config, mapOf("insta" to 30 * minute, "maps" to 99 * minute)).block)
    }

    @Test
    fun `usage outside the group does not count towards it`() {
        val config = Config(groups = listOf(social))
        assertNull(LimitEngine.decide("insta", config, mapOf("maps" to 99 * minute)).block)
    }

    @Test
    fun `the tighter of app and group limits wins`() {
        val config = Config(appLimits = mapOf("insta" to 60), groups = listOf(social))
        val usage = mapOf("insta" to 30 * minute)
        assertEquals(BlockReason.GroupLimit("Social", 30), LimitEngine.decide("insta", config, usage).block)
    }

    @Test
    fun `grayscale applies only to listed apps`() {
        val config = Config(grayscaleApps = setOf("insta"))
        assertTrue(LimitEngine.decide("insta", config, emptyMap()).grayscale)
        assertFalse(LimitEngine.decide("maps", config, emptyMap()).grayscale)
    }

    @Test
    fun `master switch pauses grayscale`() {
        val config = Config(grayscaleApps = setOf("insta"), grayscaleEnabled = false)
        assertFalse(LimitEngine.decide("insta", config, emptyMap()).grayscale)
    }

    @Test
    fun `exempt apps are never blocked or gray`() {
        val config = Config(appLimits = mapOf("home" to 0), grayscaleApps = setOf("home"))
        val decision = LimitEngine.decide("home", config, mapOf("home" to minute), exempt = setOf("home"))
        assertNull(decision.block)
        assertFalse(decision.grayscale)
    }

    @Test
    fun `limits stay flexible unless the lock is on`() {
        val config = Config(appLimits = mapOf("insta" to 10), groups = listOf(social))
        val usage = mapOf("insta" to 60 * minute)
        assertFalse(LimitEngine.isAppLocked("insta", config, usage))
        assertFalse(LimitEngine.isGroupLocked(social, config, usage))
        assertFalse(LimitEngine.anyLocked(config, usage))
    }

    @Test
    fun `lock applies only once a limit has run out`() {
        val config = Config(appLimits = mapOf("insta" to 10), lockExhausted = true)
        assertFalse(LimitEngine.isAppLocked("insta", config, mapOf("insta" to 9 * minute)))
        assertTrue(LimitEngine.isAppLocked("insta", config, mapOf("insta" to 10 * minute)))
    }

    @Test
    fun `lock covers a used up group`() {
        val config = Config(groups = listOf(social), lockExhausted = true)
        val usage = mapOf("insta" to 20 * minute, "x" to 10 * minute)
        assertTrue(LimitEngine.isGroupLocked(social, config, usage))
        assertTrue(LimitEngine.anyLocked(config, usage))
    }

    @Test
    fun `no foreground app means no block and no gray`() {
        assertEquals(Decision(null, false), LimitEngine.decide(null, Config(), emptyMap()))
    }
}
