package com.eunio.healthapp.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for UserNotificationService to verify non-blocking user messaging functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserNotificationServiceTest {
    
    init {
        // Set up test dispatcher for all tests
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }
    
    @Test
    fun `showError should emit error message`() = runTest {
        UserNotificationService.showError("Test error message")
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals("Test error message", message.text)
        assertEquals(MessageType.ERROR, message.type)
        assertNull(message.actionLabel)
        assertNull(message.action)
    }
    
    @Test
    fun `showError should emit error message with action`() = runTest {
        var actionCalled = false
        val action = { actionCalled = true }
        
        UserNotificationService.showError(
            message = "Test error message",
            actionLabel = "Retry",
            action = action
        )
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals("Test error message", message.text)
        assertEquals(MessageType.ERROR, message.type)
        assertEquals("Retry", message.actionLabel)
        
        message.action?.invoke()
        assertEquals(true, actionCalled)
    }
    
    @Test
    fun `showWarning should emit warning message`() = runTest {
        UserNotificationService.showWarning("Test warning message")
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals("Test warning message", message.text)
        assertEquals(MessageType.WARNING, message.type)
    }
    
    @Test
    fun `showInfo should emit info message`() = runTest {
        UserNotificationService.showInfo("Test info message")
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals("Test info message", message.text)
        assertEquals(MessageType.INFO, message.type)
    }
    
    @Test
    fun `showSuccess should emit success message`() = runTest {
        UserNotificationService.showSuccess("Test success message")
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals("Test success message", message.text)
        assertEquals(MessageType.SUCCESS, message.type)
    }
    
    @Test
    fun `showServiceDegraded should create appropriate message`() = runTest {
        UserNotificationService.showServiceDegraded(
            serviceName = "TestService",
            availableFeatures = listOf("read", "list"),
            unavailableFeatures = listOf("write", "delete")
        )
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals(MessageType.WARNING, message.type)
        
        val text = message.text
        assertEquals(true, text.contains("TestService"))
        assertEquals(true, text.contains("limited functionality"))
        assertEquals(true, text.contains("Available: read, list"))
        assertEquals(true, text.contains("Temporarily unavailable: write, delete"))
    }
    
    @Test
    fun `showServiceDegraded should handle empty lists`() = runTest {
        UserNotificationService.showServiceDegraded(
            serviceName = "TestService",
            availableFeatures = emptyList(),
            unavailableFeatures = emptyList()
        )
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals(MessageType.WARNING, message.type)
        
        val text = message.text
        assertEquals(true, text.contains("TestService"))
        assertEquals(true, text.contains("limited functionality"))
    }
    
    @Test
    fun `showServiceUnavailable should create appropriate message`() = runTest {
        UserNotificationService.showServiceUnavailable(
            serviceName = "TestService",
            fallbackMessage = "Using cached data instead."
        )
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals(MessageType.ERROR, message.type)
        
        val text = message.text
        assertEquals(true, text.contains("TestService"))
        assertEquals(true, text.contains("temporarily unavailable"))
        assertEquals(true, text.contains("Using cached data instead"))
    }
    
    @Test
    fun `showServiceUnavailable should work without fallback message`() = runTest {
        UserNotificationService.showServiceUnavailable("TestService")
        
        testScheduler.advanceUntilIdle()
        
        val message = UserNotificationService.messages.first()
        assertEquals(MessageType.ERROR, message.type)
        
        val text = message.text
        assertEquals(true, text.contains("TestService"))
        assertEquals(true, text.contains("temporarily unavailable"))
    }
}