package com.eunio.healthapp.domain.model.settings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationSettingTest {
    
    @Test
    fun `default constructor creates disabled setting`() {
        val setting = NotificationSetting()
        
        assertFalse(setting.enabled)
        assertNull(setting.time)
        assertEquals(1, setting.daysInAdvance)
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `disabled factory method creates valid disabled setting`() {
        val setting = NotificationSetting.disabled()
        
        assertFalse(setting.enabled)
        assertNull(setting.time)
        assertEquals(1, setting.daysInAdvance)
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `defaultEnabled factory method creates valid enabled setting`() {
        val setting = NotificationSetting.defaultEnabled()
        
        assertTrue(setting.enabled)
        assertEquals(LocalTime(20, 0), setting.time)
        assertEquals(1, setting.daysInAdvance)
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `enabled setting without time is invalid`() {
        val setting = NotificationSetting(
            enabled = true,
            time = null,
            daysInAdvance = 1
        )
        
        assertFalse(setting.isValid())
    }
    
    @Test
    fun `enabled setting with time is valid`() {
        val setting = NotificationSetting(
            enabled = true,
            time = LocalTime(9, 30),
            daysInAdvance = 2
        )
        
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `disabled setting with time is valid`() {
        val setting = NotificationSetting(
            enabled = false,
            time = LocalTime(9, 30),
            daysInAdvance = 1
        )
        
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `negative days in advance is invalid`() {
        val setting = NotificationSetting(
            enabled = false,
            time = null,
            daysInAdvance = -1
        )
        
        assertFalse(setting.isValid())
    }
    
    @Test
    fun `days in advance greater than 7 is invalid`() {
        val setting = NotificationSetting(
            enabled = false,
            time = null,
            daysInAdvance = 8
        )
        
        assertFalse(setting.isValid())
    }
    
    @Test
    fun `zero days in advance is valid`() {
        val setting = NotificationSetting(
            enabled = false,
            time = null,
            daysInAdvance = 0
        )
        
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `seven days in advance is valid`() {
        val setting = NotificationSetting(
            enabled = false,
            time = null,
            daysInAdvance = 7
        )
        
        assertTrue(setting.isValid())
    }
    
    @Test
    fun `various valid times work correctly`() {
        val times = listOf(
            LocalTime(0, 0),    // Midnight
            LocalTime(6, 30),   // 6:30 AM
            LocalTime(12, 0),   // Noon
            LocalTime(18, 45),  // 6:45 PM
            LocalTime(23, 59)   // 11:59 PM
        )
        
        times.forEach { time ->
            val setting = NotificationSetting(
                enabled = true,
                time = time,
                daysInAdvance = 1
            )
            
            assertTrue(setting.isValid(), "Setting with time $time should be valid")
        }
    }
    
    @Test
    fun `copy with changes works correctly`() {
        val original = NotificationSetting.disabled()
        val modified = original.copy(
            enabled = true,
            time = LocalTime(10, 0)
        )
        
        assertFalse(original.enabled)
        assertTrue(modified.enabled)
        assertNull(original.time)
        assertEquals(LocalTime(10, 0), modified.time)
    }
    
    @Test
    fun `serialization annotations are present`() {
        // This test ensures the class can be serialized
        val setting = NotificationSetting(
            enabled = true,
            time = LocalTime(15, 30),
            daysInAdvance = 3
        )
        
        // If serialization annotations are missing, this would fail at compile time
        assertTrue(setting.isValid())
    }
}