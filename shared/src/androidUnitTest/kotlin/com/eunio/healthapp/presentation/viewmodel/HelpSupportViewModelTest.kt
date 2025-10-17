package com.eunio.healthapp.presentation.viewmodel

import com.eunio.healthapp.domain.model.support.*
import com.eunio.healthapp.domain.usecase.support.*

import com.eunio.healthapp.presentation.state.HelpSupportUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import io.mockk.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class HelpSupportViewModelTest {

    private lateinit var viewModel: HelpSupportViewModel
    private val testDispatcher = StandardTestDispatcher()

    // Mock use cases
    private val getHelpCategoriesUseCase = mockk<GetHelpCategoriesUseCase>()
    private val searchFAQsUseCase = mockk<SearchFAQsUseCase>()
    private val getTutorialsUseCase = mockk<GetTutorialsUseCase>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
        
        // Setup default mock behaviors
        coEvery { getHelpCategoriesUseCase() } returns Result.success(emptyList())
        coEvery { searchFAQsUseCase(any<String>()) } returns Result.success(emptyList())
        coEvery { getTutorialsUseCase(any()) } returns Result.success(emptyList())
        
        viewModel = HelpSupportViewModel(
            getHelpCategoriesUseCase = getHelpCategoriesUseCase,
            searchFAQsUseCase = searchFAQsUseCase,
            getTutorialsUseCase = getTutorialsUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() = runTest {
        val initialState = viewModel.uiState.first()
        
        assertEquals(HelpSupportUiState(), initialState)
    }

    @Test
    fun `should load help categories on init`() = runTest {
        val mockCategories = listOf(
            createMockHelpCategory("1", "Getting Started"),
            createMockHelpCategory("2", "Cycle Tracking")
        )
        coEvery { getHelpCategoriesUseCase() } returns Result.success(mockCategories)
        
        // Trigger initialization
        viewModel = HelpSupportViewModel(
            getHelpCategoriesUseCase = getHelpCategoriesUseCase,
            searchFAQsUseCase = searchFAQsUseCase,
            getTutorialsUseCase = getTutorialsUseCase
        )
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(mockCategories, state.helpCategories)
        assertFalse(state.isLoading)
    }

    @Test
    fun `updateSearchQuery should update query and trigger search`() = runTest {
        val query = "cycle"
        val mockResults = listOf(
            createMockFAQ("1", "How to track cycle?")
        )
        coEvery { searchFAQsUseCase(query) } returns Result.success(mockResults)
        
        viewModel.updateSearchQuery(query)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(query, state.searchQuery)
        assertEquals(mockResults, state.searchResults)
    }

    @Test
    fun `updateSearchQuery with empty query should clear results`() = runTest {
        // First set a query with results
        viewModel.updateSearchQuery("cycle")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then clear the query
        viewModel.updateSearchQuery("")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals("", state.searchQuery)
        assertEquals(emptyList(), state.searchResults)
    }

    @Test
    fun `selectCategory should update selected category and clear search`() = runTest {
        val category = createMockHelpCategory("1", "Getting Started")
        
        // First set a search query
        viewModel.updateSearchQuery("test")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then select category
        viewModel.selectCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(category, state.selectedCategory)
        assertEquals("", state.searchQuery)
        assertEquals(emptyList(), state.searchResults)
    }

    // Helper methods
    private fun createMockHelpCategory(
        id: String,
        title: String,
        faqs: List<FAQ> = emptyList()
    ): HelpCategory {
        return HelpCategory(
            id = id,
            title = title,
            description = "Test description",
            icon = "test_icon",
            faqs = faqs
        )
    }

    private fun createMockFAQ(
        id: String,
        question: String,
        isExpanded: Boolean = false
    ): FAQ {
        return FAQ(
            id = id,
            question = question,
            answer = "Test answer",
            tags = listOf("test"),
            isExpanded = isExpanded
        )
    }
}