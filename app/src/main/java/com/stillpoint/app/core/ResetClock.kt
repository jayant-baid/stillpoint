package com.stillpoint.app.core

import java.time.LocalDateTime

object ResetClock {
    /**
     * The usage day that [now] belongs to. With a 6:00 AM reset, 1:00 AM on the 7th still belongs
     * to the 6th, so time used on the 6th keeps counting until 6:00 AM.
     */
    fun dayKey(now: LocalDateTime, resetMinute: Int): String =
        now.minusMinutes(resetMinute.toLong()).toLocalDate().toString()
}
