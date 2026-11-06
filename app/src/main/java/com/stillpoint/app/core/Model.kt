package com.stillpoint.app.core

/** One limit shared by every app in the group: time in any of them counts against the same total. */
data class LimitGroup(
    val id: String,
    val name: String,
    val minutes: Int,
    val packages: Set<String>,
)

data class Config(
    /** Package name to daily limit in minutes. */
    val appLimits: Map<String, Int> = emptyMap(),
    val groups: List<LimitGroup> = emptyList(),
    /** Apps that show in grayscale while they are in the foreground. */
    val grayscaleApps: Set<String> = emptySet(),
    /** Master switch, so grayscale can be paused without losing the list. */
    val grayscaleEnabled: Boolean = true,
    /** When on, a limit that has run out can't be changed or removed until midnight. */
    val lockExhausted: Boolean = false,
    /** When all limits reset, in minutes after midnight. 360 means 6:00 AM. */
    val resetMinute: Int = 0,
)

/** Why an app is blocked. */
sealed interface BlockReason {
    data class AppLimit(val minutes: Int) : BlockReason
    data class GroupLimit(val groupName: String, val minutes: Int) : BlockReason
}

data class Decision(
    val block: BlockReason?,
    val grayscale: Boolean,
)
