package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.Module
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Example demonstrating different ways to use Koin in tests
 */
class KoinTestUsageExample {
    
    /**
     * Example 1: Using BaseKoinTest for automatic setup/teardown
     */
    class ExampleWithBaseKoinTest : BaseKoinTest(), KoinComponent {
        private val userRepository: UserRepository by inject()
        
        @Test
        fun testWithAutomaticKoinSetup() {
            // Koin is automatically set up with testModule
            assertNotNull(userRepository)
        }
        
        // Override to use custom modules
        override fun getTestModules(): List<Module> = listOf(minimalTestModule)
        
        @Test
        fun testWithCustomModules() {
            // This test uses minimalTestModule instead of full testModule
            assertNotNull(userRepository)
        }
    }
    
    /**
     * Example 2: Using KoinTestRule for manual control
     */
    class ExampleWithKoinTestRule : KoinComponent {
        private val koinRule = defaultKoinTestRule()
        private val userRepository: UserRepository by inject()
        
        fun setup() {
            koinRule.setUp()
        }
        
        fun teardown() {
            koinRule.tearDown()
        }
        
        @Test
        fun testWithManualKoinSetup() {
            setup()
            try {
                assertNotNull(userRepository)
            } finally {
                teardown()
            }
        }
    }
    
    /**
     * Example 3: Using KoinTestUtils for one-off tests
     */
    class ExampleWithKoinTestUtils : KoinComponent {
        private val userRepository: UserRepository by inject()
        
        @Test
        fun testWithKoinUtils() {
            KoinTestUtils.withKoin(listOf(testModule)) {
                assertNotNull(userRepository)
            }
        }
    }
    
    /**
     * Example 4: Testing with different module combinations
     */
    class ExampleWithDifferentModules : BaseKoinTest(), KoinComponent {
        private val userRepository: UserRepository by inject()
        
        override fun getTestModules(): List<Module> = listOf(repositoryTestModule)
        
        @Test
        fun testRepositoryFocusedTest() {
            // This test only has repository-related dependencies
            assertNotNull(userRepository)
        }
    }
}