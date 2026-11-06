package com.stillpoint.app.service

import com.stillpoint.app.core.BlockReason
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the stop screen should show. It lives in memory rather than in an Intent, so the screen
 * accepts no outside input and nothing else on the phone can make it appear.
 */
object BlockGate {
    data class Info(val appLabel: String, val reason: BlockReason, val resetLabel: String)

    val current = MutableStateFlow<Info?>(null)

    /** True from the moment the screen is requested until it is destroyed. */
    var screenAlive = false
}
