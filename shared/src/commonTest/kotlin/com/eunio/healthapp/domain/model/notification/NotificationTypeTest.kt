package com.eunio.healthapp.domain.model.notification

import kotlin.test.*

class NotificationTypeTest {
    
    @Test
    fun `all notification types have unique IDs`() {
        val ids = NotificationType.values().map { it.id }
        val uniqueIds = ids.toSet()
        
        assertEquals(ids.size, uniqueIds.size, "All notification type IDs should be unique")
    }
    
    @Test
    fun `all notification types have non-empty titles and bodies`() {
        NotificationType.values().forEach { type ->
            assertTrue(type.title.isNotBlank(), "Notification type ${type.name} should have a non-empty title")
            assertTrue(type.body.isNotBlank(), "Notification type ${type.name} should have a non-empty body")
            assertTrue(type.category.isNotBlank(), "Notification type ${type.name} should have a non-empty category")
        }
    }
    
    @Test
    fun `fromId returns correct notification type`() {
        NotificationType.values().forEach { type ->
            val found = NotificationType.fromId(type.id)
            assertEquals(type, found, "fromId should return the correct notification type for ${type.id}")
        }
    }
    
    @Test
    fun `fromId returns null for unknown ID`() {
        val result = NotificationType.fromId("unknown_id")
        assertNull(result, "fromId should return null for unknown IDs")
    }
    
    @Test
    fun `getByCategory returns correct notifications`() {
        val healthTrackingTypes = NotificationType.getByCategory("health_tracking")
        assertTrue(healthTrackingTypes.contains(NotificationType.DAILY_LOGGING))
        
        val cycleTrackingTypes = NotificationType.getByCategory("cycle_tracking")
        assertTrue(cycleTrackingTypes.contains(NotificationType.PERIOD_PREDICTION))
        assertTrue(cycleTrackingTypes.contains(NotificationType.OVULATION_ALERT))
        
        val insightTypes = NotificationType.getByCategory("insights")
        assertTrue(insightTypes.contains(NotificationType.INSIGHTS))
    }
    
    @Test
    fun `getByCategory returns empty list for unknown category`() {
        val result = NotificationType.getByCategory("unknown_category")
        assertTrue(result.isEmpty(), "getByCategory should return empty list for unknown categories")
    }
    
    @Test
    fun `notification type properties are correct`() {
        // Test specific notification types
        with(NotificationType.DAILY_LOGGING) {
            assertEquals("daily_logging_reminder", id)
            assertEquals("Daily Health Log", title)
            assertEquals("health_tracking", category)
        }
        
        with(NotificationType.PERIOD_PREDICTION) {
            assertEquals("period_prediction_alert", id)
            assertEquals("Period Reminder", title)
            assertEquals("cycle_tracking", category)
        }
        
        with(NotificationType.OVULATION_ALERT) {
            assertEquals("ovulation_alert", id)
            assertEquals("Ovulation Window", title)
            assertEquals("cycle_tracking", category)
        }
        
        with(NotificationType.INSIGHTS) {
            assertEquals("insight_notifications", id)
            assertEquals("Health Insights", title)
            assertEquals("insights", category)
        }
    }
}