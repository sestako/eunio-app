package com.eunio.healthapp.data.local

import com.eunio.healthapp.testutil.IOSTestSupport
import com.eunio.healthapp.testutil.MockNSUserDefaults
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.AfterTest
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * iOS-specific test for NSUserDefaults integration and mocking
 */
class IOSUserDefaultsIntegrationTest {
    
    private lateinit var mockUserDefaults: MockNSUserDefaults
    
    @BeforeTest
    fun setup() {
        mockUserDefaults = IOSTestSupport.createMockUserDefaults()
    }
    
    @AfterTest
    fun teardown() {
        mockUserDefaults.reset()
    }
    
    @Test
    fun `NSUserDefaults string operations work correctly`() = runTest {
        // Test setting and getting strings
        mockUserDefaults.setObject("test_string_value", "string_key")
        assertEquals("test_string_value", mockUserDefaults.stringForKey("string_key"))
        
        // Test getting non-existent string
        assertNull(mockUserDefaults.stringForKey("non_existent_key"))
        
        // Test overwriting string value
        mockUserDefaults.setObject("updated_string_value", "string_key")
        assertEquals("updated_string_value", mockUserDefaults.stringForKey("string_key"))
        
        // Test removing string
        mockUserDefaults.removeObjectForKey("string_key")
        assertNull(mockUserDefaults.stringForKey("string_key"))
    }
    
    @Test
    fun `NSUserDefaults integer operations work correctly`() = runTest {
        // Test setting and getting integers
        mockUserDefaults.setObject(42, "int_key")
        assertEquals(42, mockUserDefaults.integerForKey("int_key"))
        
        // Test getting non-existent integer (should return 0)
        assertEquals(0, mockUserDefaults.integerForKey("non_existent_int"))
        
        // Test negative integers
        mockUserDefaults.setObject(-123, "negative_int_key")
        assertEquals(-123, mockUserDefaults.integerForKey("negative_int_key"))
        
        // Test large integers
        mockUserDefaults.setObject(Int.MAX_VALUE, "max_int_key")
        assertEquals(Int.MAX_VALUE, mockUserDefaults.integerForKey("max_int_key"))
    }
    
    @Test
    fun `NSUserDefaults boolean operations work correctly`() = runTest {
        // Test setting and getting booleans
        mockUserDefaults.setObject(true, "bool_true_key")
        assertTrue(mockUserDefaults.boolForKey("bool_true_key"))
        
        mockUserDefaults.setObject(false, "bool_false_key")
        assertFalse(mockUserDefaults.boolForKey("bool_false_key"))
        
        // Test getting non-existent boolean (should return false)
        assertFalse(mockUserDefaults.boolForKey("non_existent_bool"))
        
        // Test overwriting boolean
        mockUserDefaults.setObject(false, "bool_true_key")
        assertFalse(mockUserDefaults.boolForKey("bool_true_key"))
    }
    
    @Test
    fun `NSUserDefaults float operations work correctly`() = runTest {
        // Test setting and getting floats
        mockUserDefaults.setObject(3.14f, "float_key")
        assertEquals(3.14f, mockUserDefaults.floatForKey("float_key"))
        
        // Test getting non-existent float (should return 0.0f)
        assertEquals(0.0f, mockUserDefaults.floatForKey("non_existent_float"))
        
        // Test negative floats
        mockUserDefaults.setObject(-2.718f, "negative_float_key")
        assertEquals(-2.718f, mockUserDefaults.floatForKey("negative_float_key"))
        
        // Test very small floats
        mockUserDefaults.setObject(Float.MIN_VALUE, "min_float_key")
        assertEquals(Float.MIN_VALUE, mockUserDefaults.floatForKey("min_float_key"))
    }
    
    @Test
    fun `NSUserDefaults double operations work correctly`() = runTest {
        // Test setting and getting doubles
        mockUserDefaults.setObject(2.718281828, "double_key")
        assertEquals(2.718281828, mockUserDefaults.doubleForKey("double_key"))
        
        // Test getting non-existent double (should return 0.0)
        assertEquals(0.0, mockUserDefaults.doubleForKey("non_existent_double"))
        
        // Test negative doubles
        mockUserDefaults.setObject(-1.41421356, "negative_double_key")
        assertEquals(-1.41421356, mockUserDefaults.doubleForKey("negative_double_key"))
        
        // Test very large doubles
        mockUserDefaults.setObject(Double.MAX_VALUE, "max_double_key")
        assertEquals(Double.MAX_VALUE, mockUserDefaults.doubleForKey("max_double_key"))
    }
    
    @Test
    fun `NSUserDefaults array operations work correctly`() = runTest {
        // Test setting and getting arrays
        val testArray = listOf("item1", "item2", "item3")
        mockUserDefaults.setObject(testArray, "array_key")
        assertEquals(testArray, mockUserDefaults.arrayForKey("array_key"))
        
        // Test getting non-existent array
        assertNull(mockUserDefaults.arrayForKey("non_existent_array"))
        
        // Test empty array
        val emptyArray = emptyList<String>()
        mockUserDefaults.setObject(emptyArray, "empty_array_key")
        assertEquals(emptyArray, mockUserDefaults.arrayForKey("empty_array_key"))
        
        // Test mixed type array
        val mixedArray = listOf("string", 42, true, 3.14)
        mockUserDefaults.setObject(mixedArray, "mixed_array_key")
        assertEquals(mixedArray, mockUserDefaults.arrayForKey("mixed_array_key"))
    }
    
    @Test
    fun `NSUserDefaults dictionary operations work correctly`() = runTest {
        // Test setting and getting dictionaries
        val testDict = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
        mockUserDefaults.setObject(testDict, "dict_key")
        assertEquals(testDict, mockUserDefaults.dictionaryForKey("dict_key"))
        
        // Test getting non-existent dictionary
        assertNull(mockUserDefaults.dictionaryForKey("non_existent_dict"))
        
        // Test empty dictionary
        val emptyDict = emptyMap<String, Any>()
        mockUserDefaults.setObject(emptyDict, "empty_dict_key")
        assertEquals(emptyDict, mockUserDefaults.dictionaryForKey("empty_dict_key"))
        
        // Test nested dictionary
        val nestedDict = mapOf(
            "user" to mapOf("name" to "John", "age" to 30),
            "settings" to mapOf("theme" to "dark", "notifications" to true)
        )
        mockUserDefaults.setObject(nestedDict, "nested_dict_key")
        assertEquals(nestedDict, mockUserDefaults.dictionaryForKey("nested_dict_key"))
    }
    
    @Test
    fun `NSUserDefaults object operations work correctly`() = runTest {
        // Test setting and getting generic objects
        val testObject = "any_object_value"
        mockUserDefaults.setObject(testObject, "object_key")
        assertEquals(testObject, mockUserDefaults.objectForKey("object_key"))
        
        // Test getting non-existent object
        assertNull(mockUserDefaults.objectForKey("non_existent_object"))
        
        // Test null object
        mockUserDefaults.setObject(null, "null_object_key")
        assertNull(mockUserDefaults.objectForKey("null_object_key"))
    }
    
    @Test
    fun `NSUserDefaults removal operations work correctly`() = runTest {
        // Set up test data
        mockUserDefaults.setObject("test_value", "test_key")
        mockUserDefaults.setObject(42, "int_key")
        mockUserDefaults.setObject(true, "bool_key")
        
        // Verify data exists
        assertTrue(mockUserDefaults.hasKey("test_key"))
        assertTrue(mockUserDefaults.hasKey("int_key"))
        assertTrue(mockUserDefaults.hasKey("bool_key"))
        assertEquals(3, mockUserDefaults.getDataSize())
        
        // Remove individual keys
        mockUserDefaults.removeObjectForKey("test_key")
        assertFalse(mockUserDefaults.hasKey("test_key"))
        assertEquals(2, mockUserDefaults.getDataSize())
        
        mockUserDefaults.removeObjectForKey("int_key")
        assertFalse(mockUserDefaults.hasKey("int_key"))
        assertEquals(1, mockUserDefaults.getDataSize())
        
        // Remove non-existent key (should not cause error)
        mockUserDefaults.removeObjectForKey("non_existent_key")
        assertEquals(1, mockUserDefaults.getDataSize())
        
        // Remove last key
        mockUserDefaults.removeObjectForKey("bool_key")
        assertFalse(mockUserDefaults.hasKey("bool_key"))
        assertEquals(0, mockUserDefaults.getDataSize())
    }
    
    @Test
    fun `NSUserDefaults synchronization works correctly`() = runTest {
        // Test synchronization always returns true in mock
        assertTrue(mockUserDefaults.synchronize())
        
        // Test synchronization after data operations
        mockUserDefaults.setObject("sync_test", "sync_key")
        assertTrue(mockUserDefaults.synchronize())
        
        mockUserDefaults.removeObjectForKey("sync_key")
        assertTrue(mockUserDefaults.synchronize())
    }
    
    @Test
    fun `NSUserDefaults helper methods work correctly`() = runTest {
        // Test getAllKeys
        mockUserDefaults.setObject("value1", "key1")
        mockUserDefaults.setObject("value2", "key2")
        mockUserDefaults.setObject("value3", "key3")
        
        val allKeys = mockUserDefaults.getAllKeys()
        assertEquals(3, allKeys.size)
        assertTrue(allKeys.contains("key1"))
        assertTrue(allKeys.contains("key2"))
        assertTrue(allKeys.contains("key3"))
        
        // Test hasKey
        assertTrue(mockUserDefaults.hasKey("key1"))
        assertFalse(mockUserDefaults.hasKey("non_existent_key"))
        
        // Test getDataSize
        assertEquals(3, mockUserDefaults.getDataSize())
        
        mockUserDefaults.removeObjectForKey("key1")
        assertEquals(2, mockUserDefaults.getDataSize())
    }
    
    @Test
    fun `NSUserDefaults reset functionality works correctly`() = runTest {
        // Set up test data
        mockUserDefaults.setObject("value1", "key1")
        mockUserDefaults.setObject(42, "key2")
        mockUserDefaults.setObject(true, "key3")
        mockUserDefaults.setObject(listOf("a", "b", "c"), "key4")
        mockUserDefaults.setObject(mapOf("nested" to "value"), "key5")
        
        // Verify data exists
        assertEquals(5, mockUserDefaults.getDataSize())
        assertTrue(mockUserDefaults.hasKey("key1"))
        assertTrue(mockUserDefaults.hasKey("key5"))
        
        // Reset all data
        mockUserDefaults.reset()
        
        // Verify all data is cleared
        assertEquals(0, mockUserDefaults.getDataSize())
        assertFalse(mockUserDefaults.hasKey("key1"))
        assertFalse(mockUserDefaults.hasKey("key2"))
        assertFalse(mockUserDefaults.hasKey("key3"))
        assertFalse(mockUserDefaults.hasKey("key4"))
        assertFalse(mockUserDefaults.hasKey("key5"))
        
        // Verify we can still use UserDefaults after reset
        mockUserDefaults.setObject("post_reset_value", "post_reset_key")
        assertEquals("post_reset_value", mockUserDefaults.stringForKey("post_reset_key"))
        assertEquals(1, mockUserDefaults.getDataSize())
    }
    
    @Test
    fun `NSUserDefaults handles complex iOS app settings correctly`() = runTest {
        // Simulate typical iOS app settings storage
        
        // User preferences
        mockUserDefaults.setObject("dark", "app_theme")
        mockUserDefaults.setObject(true, "notifications_enabled")
        mockUserDefaults.setObject(1.2f, "text_size_scale")
        mockUserDefaults.setObject(false, "high_contrast_mode")
        
        // App state
        mockUserDefaults.setObject("2024-01-15T10:30:00Z", "last_sync_date")
        mockUserDefaults.setObject(42, "unread_notifications_count")
        mockUserDefaults.setObject(true, "onboarding_completed")
        
        // User data
        val userProfile = mapOf(
            "name" to "Test User",
            "email" to "test@example.com",
            "age" to 25,
            "preferences" to mapOf(
                "units" to "metric",
                "language" to "en"
            )
        )
        mockUserDefaults.setObject(userProfile, "user_profile")
        
        // Recent activity
        val recentActivity = listOf(
            mapOf("type" to "log_entry", "date" to "2024-01-15", "data" to "period_flow"),
            mapOf("type" to "insight", "date" to "2024-01-14", "data" to "fertility_window"),
            mapOf("type" to "reminder", "date" to "2024-01-13", "data" to "take_temperature")
        )
        mockUserDefaults.setObject(recentActivity, "recent_activity")
        
        // Verify all settings are stored correctly
        assertEquals("dark", mockUserDefaults.stringForKey("app_theme"))
        assertTrue(mockUserDefaults.boolForKey("notifications_enabled"))
        assertEquals(1.2f, mockUserDefaults.floatForKey("text_size_scale"))
        assertFalse(mockUserDefaults.boolForKey("high_contrast_mode"))
        
        assertEquals("2024-01-15T10:30:00Z", mockUserDefaults.stringForKey("last_sync_date"))
        assertEquals(42, mockUserDefaults.integerForKey("unread_notifications_count"))
        assertTrue(mockUserDefaults.boolForKey("onboarding_completed"))
        
        assertEquals(userProfile, mockUserDefaults.dictionaryForKey("user_profile"))
        assertEquals(recentActivity, mockUserDefaults.arrayForKey("recent_activity"))
        
        // Verify we have all expected keys
        assertEquals(9, mockUserDefaults.getDataSize())
        
        // Test updating settings
        mockUserDefaults.setObject("light", "app_theme")
        mockUserDefaults.setObject(false, "notifications_enabled")
        
        assertEquals("light", mockUserDefaults.stringForKey("app_theme"))
        assertFalse(mockUserDefaults.boolForKey("notifications_enabled"))
        
        // Test removing some settings
        mockUserDefaults.removeObjectForKey("recent_activity")
        mockUserDefaults.removeObjectForKey("unread_notifications_count")
        
        assertEquals(7, mockUserDefaults.getDataSize())
        assertNull(mockUserDefaults.arrayForKey("recent_activity"))
        assertEquals(0, mockUserDefaults.integerForKey("unread_notifications_count"))
    }
}