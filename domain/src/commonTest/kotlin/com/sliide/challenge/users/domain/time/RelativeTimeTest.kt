package com.sliide.challenge.users.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val now = 1_750_000_000_000L

    private fun ago(millis: Long) = RelativeTime.format(now, now - millis)

    @Test
    fun `future timestamps clamp to just now`() {
        assertEquals("just now", RelativeTime.format(now, now + 5 * 60_000))
    }

    @Test
    fun `under a minute is just now`() {
        assertEquals("just now", ago(0))
        assertEquals("just now", ago(59_999))
    }

    @Test
    fun `minute boundaries`() {
        assertEquals("1 minute ago", ago(60_000))
        assertEquals("1 minute ago", ago(119_999))
        assertEquals("2 minutes ago", ago(120_000))
        assertEquals("59 minutes ago", ago(60 * 60_000 - 1))
    }

    @Test
    fun `hour boundaries`() {
        assertEquals("1 hour ago", ago(60 * 60_000))
        assertEquals("23 hours ago", ago(24 * 60 * 60_000L - 1))
    }

    @Test
    fun `day and week boundaries`() {
        assertEquals("1 day ago", ago(24 * 60 * 60_000L))
        assertEquals("6 days ago", ago(7 * 24 * 60 * 60_000L - 1))
        assertEquals("1 week ago", ago(7 * 24 * 60 * 60_000L))
        assertEquals("4 weeks ago", ago(30 * 24 * 60 * 60_000L - 1))
    }

    @Test
    fun `month and year boundaries`() {
        assertEquals("1 month ago", ago(30 * 24 * 60 * 60_000L))
        assertEquals("12 months ago", ago(365 * 24 * 60 * 60_000L - 1))
        assertEquals("1 year ago", ago(365 * 24 * 60 * 60_000L))
        assertEquals("2 years ago", ago(2 * 365 * 24 * 60 * 60_000L))
    }
}
