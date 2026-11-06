package com.stillpoint.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `formats minutes`() {
        assertEquals("0m", formatMinutes(0))
        assertEquals("45m", formatMinutes(45))
        assertEquals("2h", formatMinutes(120))
        assertEquals("1h 5m", formatMinutes(65))
    }

    @Test
    fun `formats usage`() {
        assertEquals("0m", formatUsage(0))
        assertEquals("<1m", formatUsage(59_999))
        assertEquals("1m", formatUsage(60_000))
        assertEquals("1h 1m", formatUsage(3_660_000))
    }
}
