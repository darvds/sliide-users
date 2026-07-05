package com.sliide.challenge.users.domain.time

/**
 * Locale-free relative time ("5 minutes ago"), computed entirely in shared
 * logic per the brief. Pure function of two instants — trivially testable.
 *
 * Buckets are deliberately coarse above days (weeks/months/years are
 * approximations); the feed's purpose is recency at a glance, not precision.
 */
object RelativeTime {

    private const val SECOND = 1_000L
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val WEEK = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR = 365 * DAY

    fun format(nowEpochMillis: Long, thenEpochMillis: Long): String {
        val delta = nowEpochMillis - thenEpochMillis
        return when {
            // Clock skew / freshly created rows can put `then` in the future.
            delta < MINUTE -> "just now"
            delta < HOUR -> plural(delta / MINUTE, "minute")
            delta < DAY -> plural(delta / HOUR, "hour")
            delta < WEEK -> plural(delta / DAY, "day")
            delta < MONTH -> plural(delta / WEEK, "week")
            delta < YEAR -> plural(delta / MONTH, "month")
            else -> plural(delta / YEAR, "year")
        }
    }

    private fun plural(n: Long, unit: String): String =
        if (n == 1L) "1 $unit ago" else "$n ${unit}s ago"
}
