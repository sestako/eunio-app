package com.eunio.healthapp.domain.model.notification

import kotlin.test.*

class RepeatIntervalTest {
    
    @Test
    fun `all repeat intervals have correct display names`() {
        assertEquals("No repeat", RepeatInterval.NONE.displayName)
        assertEquals("Daily", RepeatInterval.DAILY.displayName)
        assertEquals("Weekly", RepeatInterval.WEEKLY.displayName)
        assertEquals("Monthly", RepeatInterval.MONTHLY.displayName)
    }
    
    @Test
    fun `all repeat intervals have correct intervals in milliseconds`() {
        assertEquals(0L, RepeatInterval.NONE.intervalInMillis)
        assertEquals(24 * 60 * 60 * 1000L, RepeatInterval.DAILY.intervalInMillis)
        assertEquals(7 * 24 * 60 * 60 * 1000L, RepeatInterval.WEEKLY.intervalInMillis)
        assertEquals(30 * 24 * 60 * 60 * 1000L, RepeatInterval.MONTHLY.intervalInMillis)
    }
    
    @Test
    fun `fromDisplayName returns correct repeat interval`() {
        RepeatInterval.values().forEach { interval ->
            val found = RepeatInterval.fromDisplayName(interval.displayName)
            assertEquals(interval, found, "fromDisplayName should return the correct interval for ${interval.displayName}")
        }
    }
    
    @Test
    fun `fromDisplayName returns null for unknown display name`() {
        val result = RepeatInterval.fromDisplayName("Unknown Interval")
        assertNull(result, "fromDisplayName should return null for unknown display names")
    }
    
    @Test
    fun `daily interval is 24 hours`() {
        val expectedMillis = 24 * 60 * 60 * 1000L
        assertEquals(expectedMillis, RepeatInterval.DAILY.intervalInMillis)
    }
    
    @Test
    fun `weekly interval is 7 days`() {
        val expectedMillis = 7 * 24 * 60 * 60 * 1000L
        assertEquals(expectedMillis, RepeatInterval.WEEKLY.intervalInMillis)
    }
    
    @Test
    fun `monthly interval is 30 days`() {
        val expectedMillis = 30 * 24 * 60 * 60 * 1000L
        assertEquals(expectedMillis, RepeatInterval.MONTHLY.intervalInMillis)
    }
}