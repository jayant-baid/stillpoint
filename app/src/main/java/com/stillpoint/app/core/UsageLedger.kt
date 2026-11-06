package com.stillpoint.app.core

/** Today's foreground time per app. Starting a new day clears it, which resets every limit. */
data class UsageLedger(
    val day: String,
    val usageMs: Map<String, Long> = emptyMap(),
) {
    fun forDay(today: String): UsageLedger = if (today == day) this else UsageLedger(today)

    /**
     * Keeps today's time when the reset time changes and the day's label changes with it.
     * Changing the setting must never hand back time that has already been used.
     */
    fun relabelled(oldToday: String, newToday: String): UsageLedger = forDay(oldToday).copy(day = newToday)

    fun add(pkg: String, ms: Long, today: String): UsageLedger {
        val current = forDay(today)
        if (ms <= 0) return current
        return current.copy(usageMs = current.usageMs + (pkg to (current.usageMs[pkg] ?: 0L) + ms))
    }
}
