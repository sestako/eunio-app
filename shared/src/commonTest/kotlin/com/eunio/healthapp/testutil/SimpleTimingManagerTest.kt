package com.eunio.healthapp.testutil

import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Simple tests for TestTimingManager to verify basic functionality
 */
class SimpleTimingManagerTest : BaseIntegrationTest() {
    
    @Test
    fun `timing manager initializes correctly`() = runIntegrationTest {
        val config = timingManager.getConfiguration()
        
        assertEquals(5.seconds, config.defaultAsyncTimeout)
        assertEquals(100.milliseconds, config.defaultPollingInterval)
    }
    
    @Test
    fun `virtual time advances correctly`() = runIntegrationTest {
        val initialTime = timingManager.getCurrentTime()
        val advanceBy = 1.seconds
        
        timingManager.advanceTime(advanceBy)
        
        val newTime = timingManager.getCurrentTime()
        val actualAdvance = newTime - initialTime
        
        assertEquals(advanceBy.inWholeMilliseconds, actualAdvance.inWholeMilliseconds)
    }
    
    @Test
    fun `executeWithDelay works correctly`() = runIntegrationTest {
        var operationExecuted = false
        
        timingManager.executeWithDelay("network_fast") {
            operationExecuted = true
        }
        
        assertTrue(operationExecuted, "Operation should have been executed")
    }
    
    @Test
    fun `device registration works correctly`() = runIntegrationTest {
        val deviceId = "test_device"
        val clockSkew = 500.milliseconds
        
        timingManager.registerDevice(deviceId, clockSkew)
        
        val deviceTime = timingManager.getDeviceTime(deviceId)
        assertNotNull(deviceTime, "Device time should be available")
    }
    
    @Test
    fun `reset clears timing state`() = runIntegrationTest {
        // Set up some state
        timingManager.registerDevice("test_device", 100.milliseconds)
        timingManager.advanceTime(1.seconds)
        
        // Reset should clear everything
        timingManager.reset()
        
        val diagnostics = timingManager.getDiagnostics()
        assertTrue(diagnostics.contains("Test Timing Manager Diagnostics:"))
    }
}