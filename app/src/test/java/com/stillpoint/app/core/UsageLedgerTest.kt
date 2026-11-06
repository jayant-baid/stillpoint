package com.stillpoint.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageLedgerTest {
    @Test
    fun `adds up time within a day`() {
        val ledger = UsageLedger("2026-11-06")
            .add("insta", 1000, "2026-11-06")
            .add("insta", 500, "2026-11-06")
        assertEquals(1500L, ledger.usageMs["insta"])
    }

    @Test
    fun `new day clears earlier usage`() {
        val ledger = UsageLedger("2026-11-06", mapOf("insta" to 9000L)).add("x", 100, "2026-11-07")
        assertEquals("2026-11-07", ledger.day)
        assertEquals(mapOf("x" to 100L), ledger.usageMs)
    }

    @Test
    fun `reading on a new day resets without any new time`() {
        val ledger = UsageLedger("2026-11-06", mapOf("insta" to 9000L)).forDay("2026-11-07")
        assertTrue(ledger.usageMs.isEmpty())
    }

    @Test
    fun `non positive time is ignored`() {
        val ledger = UsageLedger("2026-11-06").add("insta", -5, "2026-11-06")
        assertTrue(ledger.usageMs.isEmpty())
    }
}
