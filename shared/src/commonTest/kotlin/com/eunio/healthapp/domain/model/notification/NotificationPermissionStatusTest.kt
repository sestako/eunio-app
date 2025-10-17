package com.eunio.healthapp.domain.model.notification

import kotlin.test.*

class NotificationPermissionStatusTest {
    
    @Test
    fun `canShowNotifications returns true only for GRANTED status`() {
        assertTrue(NotificationPermissionStatus.GRANTED.canShowNotifications())
        
        assertFalse(NotificationPermissionStatus.DENIED.canShowNotifications())
        assertFalse(NotificationPermissionStatus.NOT_REQUESTED.canShowNotifications())
        assertFalse(NotificationPermissionStatus.PERMANENTLY_DENIED.canShowNotifications())
        assertFalse(NotificationPermissionStatus.UNKNOWN.canShowNotifications())
    }
    
    @Test
    fun `canRequestPermission returns true only for NOT_REQUESTED`() {
        assertTrue(NotificationPermissionStatus.NOT_REQUESTED.canRequestPermission())
        
        assertFalse(NotificationPermissionStatus.DENIED.canRequestPermission())
        assertFalse(NotificationPermissionStatus.GRANTED.canRequestPermission())
        assertFalse(NotificationPermissionStatus.PERMANENTLY_DENIED.canRequestPermission())
        assertFalse(NotificationPermissionStatus.UNKNOWN.canRequestPermission())
    }
    
    @Test
    fun `all permission statuses are covered`() {
        val allStatuses = NotificationPermissionStatus.values()
        
        // Verify we have all expected statuses
        assertTrue(allStatuses.contains(NotificationPermissionStatus.GRANTED))
        assertTrue(allStatuses.contains(NotificationPermissionStatus.DENIED))
        assertTrue(allStatuses.contains(NotificationPermissionStatus.NOT_REQUESTED))
        assertTrue(allStatuses.contains(NotificationPermissionStatus.PERMANENTLY_DENIED))
        assertTrue(allStatuses.contains(NotificationPermissionStatus.UNKNOWN))
        
        // Verify each status has consistent behavior
        allStatuses.forEach { status ->
            val canShow = status.canShowNotifications()
            val canRequest = status.canRequestPermission()
            
            // GRANTED should be able to show but not request
            if (status == NotificationPermissionStatus.GRANTED) {
                assertTrue(canShow, "GRANTED should be able to show notifications")
                assertFalse(canRequest, "GRANTED should not need to request permission")
            }
            
            // NOT_REQUESTED should be able to request but not show
            if (status == NotificationPermissionStatus.NOT_REQUESTED) {
                assertFalse(canShow, "$status should not be able to show notifications")
                assertTrue(canRequest, "$status should be able to request permission")
            }
            
            // DENIED should not be able to show or request (needs manual permission)
            if (status == NotificationPermissionStatus.DENIED) {
                assertFalse(canShow, "$status should not be able to show notifications")
                assertFalse(canRequest, "$status should not be able to request permission")
            }
            
            // PERMANENTLY_DENIED should not be able to show or request
            if (status == NotificationPermissionStatus.PERMANENTLY_DENIED) {
                assertFalse(canShow, "PERMANENTLY_DENIED should not be able to show notifications")
                assertFalse(canRequest, "PERMANENTLY_DENIED should not be able to request permission")
            }
            
            // UNKNOWN should not be able to show or request
            if (status == NotificationPermissionStatus.UNKNOWN) {
                assertFalse(canShow, "UNKNOWN should not be able to show notifications")
                assertFalse(canRequest, "UNKNOWN should not be able to request permission")
            }
        }
    }
}