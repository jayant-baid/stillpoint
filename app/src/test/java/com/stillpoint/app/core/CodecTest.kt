package com.stillpoint.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CodecTest {
    @Test
    fun `config survives a round trip`() {
        val config = Config(
            appLimits = mapOf("insta" to 15, "com.x.android" to 45),
            groups = listOf(LimitGroup("g1", "Social \"apps\"", 30, setOf("insta", "x"))),
            grayscaleApps = setOf("insta"),
            grayscaleEnabled = false,
        )
        assertEquals(config, Codec.decodeConfig(Codec.encodeConfig(config)))
    }

    @Test
    fun `lock setting survives a round trip`() {
        val config = Config(lockExhausted = true)
        assertEquals(config, Codec.decodeConfig(Codec.encodeConfig(config)))
    }

    @Test
    fun `settings saved before the lock existed still load`() {
        val old = """{"appLimits":{},"groups":[],"grayscaleApps":["brave"],"grayscaleEnabled":true}"""
        assertEquals(Config(grayscaleApps = setOf("brave")), Codec.decodeConfig(old))
    }

    @Test
    fun `empty config survives a round trip`() {
        assertEquals(Config(), Codec.decodeConfig(Codec.encodeConfig(Config())))
    }

    @Test
    fun `ledger survives a round trip`() {
        val ledger = UsageLedger("2026-11-06", mapOf("com.x.android" to 5_000_000_000L))
        assertEquals(ledger, Codec.decodeLedger(Codec.encodeLedger(ledger)))
    }
}
