package com.laffy.caffeine

/** Pure session logic: no Android types, unit-tested on the JVM. */

const val EXTEND_MINUTES = 5
val CUSTOM_RANGE = 1..120
val PRESET_MINUTES = listOf(10, 15, 20)

private const val MINUTE_MS = 60_000L
private const val LOW_BATTERY_PERCENT = 15
private const val INFINITE_RAW = -1

sealed interface Duration {
    data class Minutes(val value: Int) : Duration
    data object Infinite : Duration
}

/** [endAt] == null means no limit (∞). */
data class Session(val startAt: Long, val endAt: Long?, val duration: Duration)

fun Duration.toSession(now: Long): Session = when (this) {
    is Duration.Minutes -> Session(now, now + value * MINUTE_MS, this)
    Duration.Infinite -> Session(now, null, this)
}

fun Session.isActive(now: Long): Boolean = endAt == null || endAt > now

/** Milliseconds left (never negative), or null for an unlimited session. */
fun Session.remainingMs(now: Long): Long? = endAt?.let { (it - now).coerceAtLeast(0L) }

/** Adds [minutes] to the end time. Unlimited sessions are unchanged; expired ones return null. */
fun Session.extendedBy(minutes: Int, now: Long): Session? {
    val end = endAt ?: return this
    if (end <= now) return null
    return copy(endAt = end + minutes * MINUTE_MS)
}

fun Duration.encode(): Int = when (this) {
    is Duration.Minutes -> value
    Duration.Infinite -> INFINITE_RAW
}

/** Anything that is not -1 or within [CUSTOM_RANGE] (corrupted prefs) falls back to 10 minutes. */
fun decodeDuration(raw: Int): Duration = when {
    raw == INFINITE_RAW -> Duration.Infinite
    raw in CUSTOM_RANGE -> Duration.Minutes(raw)
    else -> Duration.Minutes(PRESET_MINUTES.first())
}

/** Quick Settings tile cycle: Off → 10 → 15 → 20 → ∞ → Off. A duration outside the cycle goes Off. */
object TileCycle {
    val STEPS: List<Duration> = PRESET_MINUTES.map { Duration.Minutes(it) } + Duration.Infinite

    /** Next duration to start, or null to turn off. [current] is null when nothing is running. */
    fun next(current: Duration?): Duration? {
        if (current == null) return STEPS.first()
        val index = STEPS.indexOf(current)
        if (index < 0) return null // e.g. a 35-minute custom session started from the app
        return STEPS.getOrNull(index + 1) // null after ∞ → off
    }
}

/**
 * True when the battery is low and the device is not charging. [lowFlag] is the system's
 * `EXTRA_BATTERY_LOW` (API 28+, null before); without it fall back to 15%.
 */
fun isBatteryLow(level: Int, scale: Int, plugged: Int, lowFlag: Boolean?): Boolean {
    if (plugged != 0) return false
    return lowFlag ?: (scale > 0 && level * 100 / scale <= LOW_BATTERY_PERCENT)
}
