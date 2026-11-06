package com.stillpoint.app.core

object LimitEngine {
    private const val MS_PER_MINUTE = 60_000L

    /** Total time used today across all the group's apps. */
    fun groupUsageMs(group: LimitGroup, usageMs: Map<String, Long>): Long =
        group.packages.sumOf { usageMs[it] ?: 0L }

    /**
     * What should happen while [pkg] is in the foreground. A null [pkg] means no app is known to
     * be open. [exempt] apps (this app, the home screen) can never be blocked or turned gray.
     */
    fun decide(
        pkg: String?,
        config: Config,
        usageMs: Map<String, Long>,
        exempt: Set<String> = emptySet(),
    ): Decision {
        if (pkg == null || pkg in exempt) return Decision(block = null, grayscale = false)
        return Decision(
            block = blockReason(pkg, config, usageMs),
            grayscale = config.grayscaleEnabled && pkg in config.grayscaleApps,
        )
    }

    fun isAppLocked(pkg: String, config: Config, usageMs: Map<String, Long>): Boolean {
        val minutes = config.appLimits[pkg] ?: return false
        return config.lockExhausted && (usageMs[pkg] ?: 0L) >= minutes * MS_PER_MINUTE
    }

    fun isGroupLocked(group: LimitGroup, config: Config, usageMs: Map<String, Long>): Boolean =
        config.lockExhausted && groupUsageMs(group, usageMs) >= group.minutes * MS_PER_MINUTE

    /** While true, the lock setting itself stays on, or switching it off would undo every lock. */
    fun anyLocked(config: Config, usageMs: Map<String, Long>): Boolean =
        config.appLimits.keys.any { isAppLocked(it, config, usageMs) } ||
            config.groups.any { isGroupLocked(it, config, usageMs) }

    private fun blockReason(pkg: String, config: Config, usageMs: Map<String, Long>): BlockReason? {
        config.appLimits[pkg]?.let { minutes ->
            if ((usageMs[pkg] ?: 0L) >= minutes * MS_PER_MINUTE) return BlockReason.AppLimit(minutes)
        }
        return config.groups
            .firstOrNull { pkg in it.packages && groupUsageMs(it, usageMs) >= it.minutes * MS_PER_MINUTE }
            ?.let { BlockReason.GroupLimit(it.name, it.minutes) }
    }
}
