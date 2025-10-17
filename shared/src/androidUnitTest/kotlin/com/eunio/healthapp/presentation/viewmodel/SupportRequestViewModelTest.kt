package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.domain.repository.HelpSupportRepository
import com.eunio.healthapp.domain.usecase.support.SubmitSupportRequestUseCase

import com.eunio.healthapp.presentation.state.SupportRequestUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import io.mockk.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SupportRequestViewModelTest {

    private lateinit var viewModel: SupportRequestViewModel
    private val testDispatcher = StandardTestDispatcher()

    // Mock implementations
    private val mockSubmitSupportRequestUseCase = mockk<SubmitSupportRequestUseCase>()
    private val mockHelpSupportRepository = mockk<HelpSupportRepository>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        
        // Setup default mock behaviors
        coEvery { mockSubmitSupportRequestUseCase(any<String>(), any<SupportRequestType>(), any<String>(), any<String>(), any<Boolean>()) } returns Result.success("request123")
        coEvery { mockHelpSupportRepository.getDeviceInfo() } returns DeviceInfo("Test", "1.0", "TestDevice", "100x100", "en")
        coEvery { mockHelpSupportRepository.getAppInfo() } returns AppInfo("1.0.0", "1", null, null)
        
        viewModel = SupportRequestViewModel(
            submitSupportRequestUseCase = mockSubmitSupportRequestUseCase,
            helpSupportRepository = mockHelpSupportRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        // Advance dispatcher to complete async initialization
        testDispatcher.scheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.first()
        
        // Check that the state has the expected default values (after device info is loaded)
        assertEquals(SupportRequestType.GENERAL_INQUIRY, initialState.requestType)
        assertEquals("", initialState.subject)
        assertEquals("", initialState.description)
        assertFalse(initialState.attachLogs)
        assertFalse(initialState.isSubmitted)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
        assertTrue(initialState.validationErrors.isEmpty())
        // Device and app info should be loaded
        assertNotNull(initialState.deviceInfo)
        assertNotNull(initialState.appInfo)
    }

    @Test
    fun `should load device and app info on init`() = runTest {
        val mockDeviceInfo = DeviceInfo("iOS", "15.0", "iPhone", "375x812", "en_US")
        val mockAppInfo = AppInfo("1.0.0", "100", null, null)
        
        coEvery { mockHelpSupportRepository.getDeviceInfo() } returns mockDeviceInfo
        coEvery { mockHelpSupportRepository.getAppInfo() } returns mockAppInfo
        
        viewModel = SupportRequestViewModel(
            submitSupportRequestUseCase = mockSubmitSupportRequestUseCase,
            helpSupportRepository = mockHelpSupportRepository
        )
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(mockDeviceInfo, state.deviceInfo)
        assertEquals(mockAppInfo, state.appInfo)
    }

    @Test
    fun `updateRequestType should update request type`() = runTest {
        val newType = SupportRequestType.BUG_REPORT
        
        viewModel.updateRequestType(newType)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(newType, state.requestType)
    }

    @Test
    fun `updateSubject should update subject and clear validation error`() = runTest {
        val subject = "Test subject"
        
        viewModel.updateSubject(subject)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(subject, state.subject)
        assertFalse(state.validationErrors.containsKey("subject"))
    }

    @Test
    fun `submitRequest should validate required fields`() = runTest {
        // Submit with empty fields
        viewModel.submitRequest("user123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.validationErrors.containsKey("subject"))
        assertTrue(state.validationErrors.containsKey("description"))
        assertFalse(state.isSubmitted)
    }

    @Test
    fun `submitRequest should succeed with valid input`() = runTest {
        coEvery { mockSubmitSupportRequestUseCase(any<String>(), any<SupportRequestType>(), any<String>(), any<String>(), any<Boolean>()) } returns Result.success("request123")
        
        viewModel.updateSubject("Valid subject")
        viewModel.updateDescription("This is a valid description that is long enough to pass validation")
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.submitRequest("user123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.isSubmitted)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun `resetForm should reset all form fields`() = runTest {
        // Set some values first
        viewModel.updateSubject("Test subject")
        viewModel.updateDescription("Test description")
        viewModel.toggleAttachLogs()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Reset form
        viewModel.resetForm()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(SupportRequestType.GENERAL_INQUIRY, state.requestType)
        assertEquals("", state.subject)
        assertEquals("", state.description)
        assertFalse(state.attachLogs)
        assertFalse(state.isSubmitted)
        assertNull(state.errorMessage)
        assertTrue(state.validationErrors.isEmpty())
    }
}