package com.sliide.challenge.users.data.time

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
