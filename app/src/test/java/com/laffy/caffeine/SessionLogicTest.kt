package com.laffy.caffeine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOW = 1_000_000L
private const val MIN = 60_000L

class SessionLogicTest {

    @Test fun minutesToSession() =
        assertEquals(Session(NOW, NOW + 15 * MIN, Duration.Minutes(15)), Duration.Minutes(15).toSession(NOW))

    @Test fun infiniteToSession() =
        assertEquals(Session(NOW, null, Duration.Infinite), Duration.Infinite.toSession(NOW))

    @Test fun infiniteAlwaysActive() =
        assertTrue(Duration.Infinite.toSession(NOW).isActive(NOW + 1_000 * MIN))

    @Test fun remainingClampsToZero() =
        assertEquals(0L, Duration.Minutes(1).toSession(NOW).remainingMs(NOW + 2 * MIN))

    @Test fun extendAddsFiveFromEnd() =
        assertEquals(
            NOW + 15 * MIN,
            Duration.Minutes(10).toSession(NOW).extendedBy(EXTEND_MINUTES, NOW + MIN)!!.endAt
        )

    @Test fun extendExpiredReturnsNull() =
        assertNull(Duration.Minutes(1).toSession(NOW).extendedBy(5, NOW + 2 * MIN))

    @Test fun extendInfiniteUnchanged() {
        val s = Duration.Infinite.toSession(NOW)
        assertEquals(s, s.extendedBy(5, NOW))
    }

    @Test fun encodeRoundTrip() {
        listOf(Duration.Minutes(1), Duration.Minutes(120), Duration.Infinite).forEach {
            assertEquals(it, decodeDuration(it.encode()))
        }
    }

    @Test fun decodeInvalidFallsBack() {
        listOf(0, 121, 999, -7).forEach { assertEquals(Duration.Minutes(10), decodeDuration(it)) }
    }

    @Test fun tileCycleOrder() {
        assertEquals(Duration.Minutes(10), TileCycle.next(null))
        assertEquals(Duration.Minutes(15), TileCycle.next(Duration.Minutes(10)))
        assertEquals(Duration.Minutes(20), TileCycle.next(Duration.Minutes(15)))
        assertEquals(Duration.Infinite, TileCycle.next(Duration.Minutes(20)))
        assertNull(TileCycle.next(Duration.Infinite))
    }

    @Test fun tileCycleCustomGoesOff() = assertNull(TileCycle.next(Duration.Minutes(35)))

    @Test fun batteryLowFlagWins() =
        assertTrue(isBatteryLow(level = 50, scale = 100, plugged = 0, lowFlag = true))

    @Test fun batteryLowFallback() {
        assertTrue(isBatteryLow(15, 100, 0, null))
        assertFalse(isBatteryLow(16, 100, 0, null))
    }

    @Test fun batteryLowIgnoredWhenPlugged() =
        assertFalse(isBatteryLow(5, 100, plugged = 1, lowFlag = true))
}
