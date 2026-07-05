package com.sliide.challenge.users.data.time

/** Wall-clock now. Actuals: System.currentTimeMillis / NSDate. */
internal expect fun currentEpochMillis(): Long
