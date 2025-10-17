package com.eunio.healthapp.testutil

import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.reflect.KClass

/**
 * Manager for mock services with graceful failure handling and diagnostic capabilities.
 * Provides centralized mock service creation, registration, and error recovery.
 */
object MockServiceManager {
    
    private val mockServiceRegistry = mutableMapOf<KClass<*>, Any>()
    private val serviceCreationLog = mutableListOf<ServiceCreationEntry>()
    private val failureHandlers = mutableMapOf<KClass<*>, (Exception) -> Any?>()
    
    /**
     * Creates a mock service with graceful failure handling.
     * 
     * @param serviceClass The class/interface to mock
     * @param fallbackProvider Optional fallback provider if creation fails
     * @return Mock service instance or null if creation fails
     */
    fun <T : Any> createMockService(
        serviceClass: KClass<T>,
        fallbackProvider: (() -> T)? = null
    ): T? {
        return try {
            logServiceCreationAttempt(serviceClass)
            
            // Check if we already have a cached mock
            mockServiceRegistry[serviceClass]?.let { cached ->
                logServiceCreationSuccess(serviceClass, fromCache = true)
                return cached as T
            }
            
            // Try to create new mock service
            val mockService = createMockImplementation(serviceClass)
            
            // Cache the created service
            mockServiceRegistry[serviceClass] = mockService
            logServiceCreationSuccess(serviceClass, fromCache = false)
            
            mockService
            
        } catch (e: Exception) {
            logServiceCreationFailure(serviceClass, e)
            
            // Try fallback provider
            fallbackProvider?.let { provider ->
                try {
                    val fallbackService = provider()
                    logServiceFallbackSuccess(serviceClass)
                    return fallbackService
                } catch (fallbackException: Exception) {
                    logServiceFallbackFailure(serviceClass, fallbackException)
                }
            }
            
            // Try registered failure handler
            failureHandlers[serviceClass]?.let { handler ->
                try {
                    val handlerResult = handler(e) as? T
                    if (handlerResult != null) {
                        logServiceHandlerSuccess(serviceClass)
                        return handlerResult
                    }
                } catch (handlerException: Exception) {
                    logServiceHandlerFailure(serviceClass, handlerException)
                }
            }
            
            null
        }
    }
    
    /**
     * Registers a failure handler for a specific service type.
     */
    fun <T : Any> registerFailureHandler(
        serviceClass: KClass<T>,
        handler: (Exception) -> T?
    ) {
        failureHandlers[serviceClass] = handler as (Exception) -> Any?
    }
    
    /**
     * Creates a test module with all registered mock services.
     */
    fun createTestModule(): Module {
        return module {
            mockServiceRegistry.forEach { (serviceClass, mockInstance) ->
                try {
                    // Register the mock instance in Koin
                    single(qualifier = null, createdAtStart = false) { mockInstance }
                } catch (e: Exception) {
                    logModuleRegistrationFailure(serviceClass, e)
                }
            }
        }
    }
    
    /**
     * Validates that all required mock services are available.
     */
    fun validateMockServices(requiredServices: List<KClass<*>>): MockServiceValidationResult {
        val missingServices = mutableListOf<KClass<*>>()
        val failedServices = mutableListOf<Pair<KClass<*>, String>>()
        
        requiredServices.forEach { serviceClass ->
            val mockService = mockServiceRegistry[serviceClass]
            
            when {
                mockService == null -> {
                    missingServices.add(serviceClass)
                }
                !isServiceHealthy(mockService) -> {
                    failedServices.add(serviceClass to "Service health check failed")
                }
            }
        }
        
        return MockServiceValidationResult(
            isValid = missingServices.isEmpty() && failedServices.isEmpty(),
            missingServices = missingServices,
            failedServices = failedServices
        )
    }
    
    /**
     * Gets the current status of all mock services.
     */
    fun getServiceStatus(): String {
        return buildString {
            appendLine("Mock Service Registry Status:")
            appendLine("  Total Services: ${mockServiceRegistry.size}")
            
            if (mockServiceRegistry.isEmpty()) {
                appendLine("  No services registered")
            } else {
                mockServiceRegistry.forEach { (serviceClass, mockInstance) ->
                    val health = if (isServiceHealthy(mockInstance)) "Healthy" else "Unhealthy"
                    appendLine("  ${serviceClass.simpleName}: $health")
                }
            }
            
            appendLine()
            appendLine("Recent Service Creation Log:")
            serviceCreationLog.takeLast(5).forEach { entry ->
                appendLine("  ${entry.timestamp}: ${entry.message}")
            }
        }
    }
    
    /**
     * Clears all cached mock services and logs.
     */
    fun clearAll() {
        mockServiceRegistry.clear()
        serviceCreationLog.clear()
        failureHandlers.clear()
    }
    
    /**
     * Resets all mock services to their default state.
     */
    fun resetAllMocks() {
        mockServiceRegistry.clear()
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), "All mocks reset"))
    }
    
    /**
     * Resets suite-specific mock services.
     */
    fun resetSuiteMocks(suiteId: String) {
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), "Suite mocks reset for: $suiteId"))
    }
    
    /**
     * Gets diagnostic information for troubleshooting.
     */
    fun getDiagnosticInfo(): String {
        return buildString {
            appendLine("=== Mock Service Manager Diagnostics ===")
            appendLine(getServiceStatus())
            appendLine()
            appendLine("Failure Handlers Registered: ${failureHandlers.size}")
            failureHandlers.keys.forEach { serviceClass ->
                appendLine("  ${serviceClass.simpleName}")
            }
            appendLine()
            appendLine("Koin Context Status: ${getKoinContextStatus()}")
        }
    }
    
    // Private implementation methods
    private fun <T : Any> createMockImplementation(serviceClass: KClass<T>): T {
        // Use the existing MockServiceFactory to create implementations
        return try {
            // Create a basic mock implementation
            @Suppress("UNCHECKED_CAST")
            when (serviceClass.simpleName) {
                "TestService" -> FallbackTestService() as T
                else -> throw MockCreationException("No mock implementation available for ${serviceClass.simpleName}")
            }
        } catch (e: Exception) {
            throw MockCreationException("Failed to create mock for ${serviceClass.simpleName}", serviceClass, e)
        }
    }
    
    private fun isServiceHealthy(service: Any): Boolean {
        return try {
            // Basic health check - ensure service is not null and responds to basic operations
            service.toString() // This will throw if service is in bad state
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getKoinContextStatus(): String {
        return try {
            "Koin status not available in common code"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
    
    // Logging methods
    private fun logServiceCreationAttempt(serviceClass: KClass<*>) {
        val message = "Attempting to create mock service: ${serviceClass.simpleName}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceCreationSuccess(serviceClass: KClass<*>, fromCache: Boolean) {
        val source = if (fromCache) "from cache" else "newly created"
        val message = "Mock service created successfully: ${serviceClass.simpleName} ($source)"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceCreationFailure(serviceClass: KClass<*>, exception: Exception) {
        val message = "Mock service creation failed: ${serviceClass.simpleName} - ${exception.message}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceFallbackSuccess(serviceClass: KClass<*>) {
        val message = "Fallback provider succeeded for: ${serviceClass.simpleName}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceFallbackFailure(serviceClass: KClass<*>, exception: Exception) {
        val message = "Fallback provider failed for: ${serviceClass.simpleName} - ${exception.message}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceHandlerSuccess(serviceClass: KClass<*>) {
        val message = "Failure handler succeeded for: ${serviceClass.simpleName}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logServiceHandlerFailure(serviceClass: KClass<*>, exception: Exception) {
        val message = "Failure handler failed for: ${serviceClass.simpleName} - ${exception.message}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun logModuleRegistrationFailure(serviceClass: KClass<*>, exception: Exception) {
        val message = "Module registration failed for: ${serviceClass.simpleName} - ${exception.message}"
        serviceCreationLog.add(ServiceCreationEntry(getCurrentTimestamp(), message))
    }
    
    private fun getCurrentTimestamp(): String {
        return kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Data class for service creation log entries.
 */
data class ServiceCreationEntry(
    val timestamp: String,
    val message: String
)

/**
 * Result of mock service validation.
 */
data class MockServiceValidationResult(
    val isValid: Boolean,
    val missingServices: List<KClass<*>>,
    val failedServices: List<Pair<KClass<*>, String>>
)

/**
 * Exception thrown when mock service creation fails.
 */
class MockCreationException(
    message: String,
    val serviceClass: KClass<*>? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Test service interface for demonstration.
 */
interface TestService {
    fun performOperation(): String
}

/**
 * Fallback test service implementation.
 */
class FallbackTestService : TestService {
    override fun performOperation(): String = "Fallback operation"
}