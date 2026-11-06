package com.stillpoint.app.core

fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/** A reset time in words: "midnight" or "6:00 AM". */
fun formatResetTime(minuteOfDay: Int): String {
    if (minuteOfDay == 0) return "midnight"
    val hour = minuteOfDay / 60
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    val minute = (minuteOfDay % 60).toString().padStart(2, '0')
    return "$hour12:$minute ${if (hour < 12) "AM" else "PM"}"
}

/** Rounds down, so the display never claims more time was used than really was. */
fun formatUsage(ms: Long): String =
    if (ms in 1 until 60_000L) "<1m" else formatMinutes((ms / 60_000L).toInt())
