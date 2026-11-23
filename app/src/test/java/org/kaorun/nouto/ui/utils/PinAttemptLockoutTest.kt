package org.kaorun.nouto.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PinAttemptLockoutTest {
    @Test
    fun doesNotLockBeforeFifthFailure() {
        val lockout = PinAttemptLockout()

        repeat(4) { index ->
            assertEquals(0L, lockout.recordFailure(index.toLong()))
        }
        assertEquals(0L, lockout.remainingMs(3L))
    }

    @Test
    fun firstLockoutStartsAfterFifthFailureAndExpires() {
        val lockout = PinAttemptLockout()
        repeat(4) { lockout.recordFailure(0L) }

        assertEquals(30_000L, lockout.recordFailure(100L))
        assertEquals(29_500L, lockout.remainingMs(600L))
        assertEquals(0L, lockout.remainingMs(30_100L))
    }

    @Test
    fun lockoutDurationIncreasesAndIsCapped() {
        val lockout = PinAttemptLockout()
        repeat(5) { lockout.recordFailure(0L) }

        assertEquals(60_000L, lockout.recordFailure(30_000L))
        repeat(20) { lockout.recordFailure(100_000L) }
        assertEquals(PinAttemptLockout.MAX_LOCKOUT_MS, lockout.remainingMs(100_000L))
    }

    @Test
    fun resetClearsAttemptsAndLockout() {
        val lockout = PinAttemptLockout()
        repeat(5) { lockout.recordFailure(10L) }

        lockout.reset()

        assertEquals(0, lockout.failedAttempts)
        assertEquals(0L, lockout.remainingMs(10L))
        assertEquals(0L, lockout.recordFailure(10L))
    }
}
