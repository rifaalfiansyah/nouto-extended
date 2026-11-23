package org.kaorun.nouto.ui.utils

/** Pure attempt counter so lockout timing can be tested without Android framework dependencies. */
internal class PinAttemptLockout(
    var failedAttempts: Int = 0,
    var lockedUntilMs: Long = 0L
) {
    fun remainingMs(nowMs: Long): Long = (lockedUntilMs - nowMs).coerceAtLeast(0L)

    fun recordFailure(nowMs: Long): Long {
        failedAttempts += 1
        if (failedAttempts >= LOCKOUT_THRESHOLD) {
            val exponent = (failedAttempts - LOCKOUT_THRESHOLD).coerceAtMost(MAX_EXPONENT)
            val durationMs = (INITIAL_LOCKOUT_MS * (1L shl exponent))
                .coerceAtMost(MAX_LOCKOUT_MS)
            lockedUntilMs = nowMs + durationMs
        }
        return remainingMs(nowMs)
    }

    fun reset() {
        failedAttempts = 0
        lockedUntilMs = 0L
    }

    companion object {
        const val LOCKOUT_THRESHOLD = 5
        const val INITIAL_LOCKOUT_MS = 30_000L
        const val MAX_LOCKOUT_MS = 15 * 60_000L
        private const val MAX_EXPONENT = 10
    }
}
