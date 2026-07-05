package com.sliide.challenge.users.domain.time

/**
 * Injectable clock seam. Production actuals live in :data (platform code);
 * tests pass a fixed or advancing fake.
 */
fun interface TimeProvider {
    fun nowEpochMillis(): Long
}
