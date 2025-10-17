package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random

/**
 * Enhanced Mock API Service for realistic network behavior simulation.
 * Provides configurable network delays, timeout handling, retry mechanisms,
 * and comprehensive error response generation for different scenarios.
 */
class MockApiService {
    
    // Network behavior configuration
    private var networkDelay: Duration = 100.milliseconds
    private var timeoutDuration: Duration = 30.seconds
    private var shouldTimeout: Boolean = false
    private var shouldFailWithNetworkError: Boolean = false
    private var shouldFailWithServerError: Boolean = false
    private var shouldFailWithAuthError: Boolean = false
    private var shouldFailWithValidationError: Boolean = false
    
    // Retry mechanism configuration
    private var maxRetries: Int = 3
    private var retryDelay: Duration = 500.milliseconds
    private var useExponentialBackoff: Boolean = true
    private var currentFailureCount: Int = 0
    private var maxFailuresBeforeSuccess: Int = 0
    
    // Network condition simulation
    private var networkCondition: NetworkCondition = NetworkCondition.GOOD
    private var connectionStability: Double = 1.0 // 1.0 = stable, 0.0 = completely unstable
    private var packetLossRate: Double = 0.0 // 0.0 = no loss, 1.0 = 100% loss
    
    // Request tracking
    private val requestHistory = mutableListOf<ApiRequest>()
    private var requestCounter: Int = 0
    
    enum class NetworkCondition {
        EXCELLENT,  // < 50ms delay
        GOOD,       // 50-200ms delay
        FAIR,       // 200-500ms delay
        POOR,       // 500-1000ms delay
        VERY_POOR,  // 1000-3000ms delay
        OFFLINE     // No connection
    }
    
    enum class ErrorType {
        NETWORK_ERROR,
        SERVER_ERROR,
        AUTHENTICATION_ERROR,
        VALIDATION_ERROR,
        TIMEOUT_ERROR,
        RATE_LIMIT_ERROR,
        SERVICE_UNAVAILABLE,
        CONFLICT_ERROR
    }
    
    data class ApiRequest(
        val id: Int,
        val timestamp: kotlinx.datetime.Instant,
        val operation: String,
        val duration: Duration,
        val success: Boolean,
        val errorType: ErrorType? = null,
        val retryCount: Int = 0
    )
    
    /**
     * Executes an API request with realistic network behavior simulation.
     * 
     * @param operation Name of the operation for tracking
     * @param request The actual request operation to execute
     * @return Result containing the response or error
     */
    suspend fun <T> executeRequest(
        operation: String,
        request: suspend () -> T
    ): Result<T> {
        val requestId = ++requestCounter
        val startTime = Clock.System.now()
        
        return try {
            // Simulate network condition delays
            val actualDelay = calculateNetworkDelay()
            if (actualDelay > Duration.ZERO) {
                delay(actualDelay.inWholeMilliseconds)
            }
            
            // Check for timeout before processing
            if (shouldTimeout || actualDelay >= timeoutDuration) {
                recordRequest(requestId, operation, actualDelay, false, ErrorType.TIMEOUT_ERROR)
                return Result.error(AppError.NetworkError("Request timeout after ${timeoutDuration.inWholeSeconds}s"))
            }
            
            // Simulate packet loss
            if (packetLossRate > 0.0 && Random.nextDouble() < packetLossRate) {
                recordRequest(requestId, operation, actualDelay, false, ErrorType.NETWORK_ERROR)
                return Result.error(AppError.NetworkError("Packet loss occurred"))
            }
            
            // Check for simulated failures
            val errorResult = checkForSimulatedErrors<T>(requestId, operation, actualDelay)
            if (errorResult != null) {
                return errorResult
            }
            
            // Execute the actual request
            val result = request()
            val totalDuration = Clock.System.now() - startTime
            recordRequest(requestId, operation, totalDuration, true)
            
            Result.success(result)
            
        } catch (e: Exception) {
            val totalDuration = Clock.System.now() - startTime
            recordRequest(requestId, operation, totalDuration, false, ErrorType.NETWORK_ERROR)
            Result.error(AppError.NetworkError("Request execution failed: ${e.message}"))
        }
    }
    
    /**
     * Executes an API request with built-in retry mechanism.
     * 
     * @param operation Name of the operation for tracking
     * @param request The actual request operation to execute
     * @return Result containing the response or error after retries
     */
    suspend fun <T> executeRequestWithRetry(
        operation: String,
        request: suspend () -> T
    ): Result<T> {
        var lastError: AppError? = null
        var retryCount = 0
        
        for (attempt in 0..maxRetries) {
            val result = executeRequest("$operation (attempt ${attempt + 1})", request)
            
            if (result.isSuccess) {
                return result
            }
            
            lastError = result.errorOrNull() as? AppError
            retryCount = attempt
            
            // Don't retry on certain error types
            if (lastError is AppError.ValidationError || 
                lastError is AppError.AuthenticationError) {
                break
            }
            
            // Don't retry on the last attempt
            if (attempt < maxRetries) {
                val delayDuration = if (useExponentialBackoff) {
                    retryDelay * (1 shl attempt) // Exponential backoff: 500ms, 1s, 2s, 4s...
                } else {
                    retryDelay
                }
                delay(delayDuration.inWholeMilliseconds)
            }
        }
        
        return Result.error(lastError ?: AppError.NetworkError("Request failed after $retryCount retries"))
    }
    
    /**
     * Simulates a batch API operation with partial failure handling.
     * 
     * @param operations List of operations to execute
     * @param allowPartialFailure Whether to continue on individual failures
     * @return Result containing success count and any errors
     */
    suspend fun <T> executeBatchRequest(
        operations: List<Pair<String, suspend () -> T>>,
        allowPartialFailure: Boolean = true
    ): Result<BatchResult<T>> {
        val results = mutableListOf<Result<T>>()
        val successfulResults = mutableListOf<T>()
        val errors = mutableListOf<AppError>()
        
        for ((operationName, operation) in operations) {
            val result = executeRequest(operationName, operation)
            results.add(result)
            
            if (result.isSuccess) {
                result.getOrNull()?.let { successfulResults.add(it) }
            } else {
                val error = result.errorOrNull() as? AppError
                if (error != null) {
                    errors.add(error)
                }
                
                // Stop on first failure if partial failure is not allowed
                if (!allowPartialFailure) {
                    break
                }
            }
        }
        
        val batchResult = BatchResult(
            totalOperations = operations.size,
            successfulOperations = successfulResults.size,
            failedOperations = errors.size,
            results = successfulResults,
            errors = errors
        )
        
        return Result.success(batchResult)
    }
    
    // Configuration methods
    
    /**
     * Sets the base network delay for all requests.
     */
    fun setNetworkDelay(delay: Duration) {
        this.networkDelay = delay
    }
    
    /**
     * Sets the timeout duration for requests.
     */
    fun setTimeoutDuration(timeout: Duration) {
        this.timeoutDuration = timeout
    }
    
    /**
     * Configures the network condition to simulate different connection qualities.
     */
    fun setNetworkCondition(condition: NetworkCondition) {
        this.networkCondition = condition
        // Adjust base delay based on condition
        this.networkDelay = when (condition) {
            NetworkCondition.EXCELLENT -> 25.milliseconds
            NetworkCondition.GOOD -> 100.milliseconds
            NetworkCondition.FAIR -> 300.milliseconds
            NetworkCondition.POOR -> 750.milliseconds
            NetworkCondition.VERY_POOR -> 2.seconds
            NetworkCondition.OFFLINE -> Duration.ZERO // Will be handled as timeout
        }
    }
    
    /**
     * Sets connection stability (affects delay variance).
     * @param stability 1.0 = perfectly stable, 0.0 = highly unstable
     */
    fun setConnectionStability(stability: Double) {
        this.connectionStability = stability.coerceIn(0.0, 1.0)
    }
    
    /**
     * Sets packet loss rate for network simulation.
     * @param lossRate 0.0 = no loss, 1.0 = 100% loss
     */
    fun setPacketLossRate(lossRate: Double) {
        this.packetLossRate = lossRate.coerceIn(0.0, 1.0)
    }
    
    /**
     * Configures retry behavior.
     */
    fun setRetryConfiguration(
        maxRetries: Int,
        retryDelay: Duration,
        useExponentialBackoff: Boolean = true
    ) {
        this.maxRetries = maxRetries
        this.retryDelay = retryDelay
        this.useExponentialBackoff = useExponentialBackoff
    }
    
    // Error simulation methods
    
    /**
     * Simulates network failures for the next N requests.
     */
    fun simulateNetworkFailure(maxFailures: Int = 1) {
        this.shouldFailWithNetworkError = true
        this.maxFailuresBeforeSuccess = maxFailures
        this.currentFailureCount = 0
    }
    
    /**
     * Simulates server errors (5xx) for the next N requests.
     */
    fun simulateServerError(maxFailures: Int = 1) {
        this.shouldFailWithServerError = true
        this.maxFailuresBeforeSuccess = maxFailures
        this.currentFailureCount = 0
    }
    
    /**
     * Simulates authentication errors for the next N requests.
     */
    fun simulateAuthenticationError(maxFailures: Int = 1) {
        this.shouldFailWithAuthError = true
        this.maxFailuresBeforeSuccess = maxFailures
        this.currentFailureCount = 0
    }
    
    /**
     * Simulates validation errors for the next N requests.
     */
    fun simulateValidationError(maxFailures: Int = 1) {
        this.shouldFailWithValidationError = true
        this.maxFailuresBeforeSuccess = maxFailures
        this.currentFailureCount = 0
    }
    
    /**
     * Simulates request timeouts.
     */
    fun simulateTimeout(enable: Boolean = true) {
        this.shouldTimeout = enable
    }
    
    /**
     * Simulates slow network conditions with high delays.
     */
    fun simulateSlowNetwork(delay: Duration = 5.seconds) {
        setNetworkDelay(delay)
        setConnectionStability(0.3) // Unstable connection
        setPacketLossRate(0.1) // 10% packet loss
    }
    
    /**
     * Simulates intermittent connectivity issues.
     */
    fun simulateIntermittentConnectivity(failureRate: Double = 0.3) {
        setConnectionStability(1.0 - failureRate)
        setPacketLossRate(failureRate * 0.5) // Half the failure rate as packet loss
    }
    
    // State management and monitoring
    
    /**
     * Resets all simulation settings to defaults.
     */
    fun reset() {
        networkDelay = 100.milliseconds
        timeoutDuration = 30.seconds
        shouldTimeout = false
        shouldFailWithNetworkError = false
        shouldFailWithServerError = false
        shouldFailWithAuthError = false
        shouldFailWithValidationError = false
        maxRetries = 3
        retryDelay = 500.milliseconds
        useExponentialBackoff = true
        currentFailureCount = 0
        maxFailuresBeforeSuccess = 0
        networkCondition = NetworkCondition.GOOD
        connectionStability = 1.0
        packetLossRate = 0.0
        requestHistory.clear()
        requestCounter = 0
    }
    
    /**
     * Gets request statistics for analysis.
     */
    fun getRequestStatistics(): RequestStatistics {
        val totalRequests = requestHistory.size
        val successfulRequests = requestHistory.count { it.success }
        val failedRequests = totalRequests - successfulRequests
        val averageLatency = if (totalRequests > 0) {
            requestHistory.map { it.duration.inWholeMilliseconds }.average()
        } else 0.0
        
        val errorsByType = requestHistory
            .filter { !it.success }
            .groupBy { it.errorType }
            .mapValues { it.value.size }
        
        return RequestStatistics(
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0,
            averageLatencyMs = averageLatency,
            errorsByType = errorsByType
        )
    }
    
    /**
     * Gets the request history for detailed analysis.
     */
    fun getRequestHistory(): List<ApiRequest> = requestHistory.toList()
    
    /**
     * Clears request history while keeping configuration.
     */
    fun clearHistory() {
        requestHistory.clear()
        requestCounter = 0
    }
    
    // Private helper methods
    
    private fun calculateNetworkDelay(): Duration {
        if (networkCondition == NetworkCondition.OFFLINE) {
            return timeoutDuration + 1.seconds // Force timeout
        }
        
        val baseDelay = networkDelay.inWholeMilliseconds.toDouble()
        
        // Add variance based on connection stability
        // Ensure variance doesn't reduce delay below 80% of base delay for predictable testing
        val variance = (1.0 - connectionStability) * baseDelay * 0.3 // Reduced variance factor
        val randomVariance = (Random.nextDouble() - 0.5) * 2 * variance
        
        val finalDelay = (baseDelay.toDouble() + randomVariance).coerceAtLeast(baseDelay.toDouble() * 0.8)
        return finalDelay.toLong().milliseconds
    }
    
    private suspend fun <T> checkForSimulatedErrors(
        requestId: Int,
        operation: String,
        delay: Duration
    ): Result<T>? {
        // Check if we should fail with network error
        if (shouldFailWithNetworkError && currentFailureCount < maxFailuresBeforeSuccess) {
            currentFailureCount++
            recordRequest(requestId, operation, delay, false, ErrorType.NETWORK_ERROR)
            return Result.error(generateNetworkError())
        }
        
        // Check if we should fail with server error
        if (shouldFailWithServerError && currentFailureCount < maxFailuresBeforeSuccess) {
            currentFailureCount++
            recordRequest(requestId, operation, delay, false, ErrorType.SERVER_ERROR)
            return Result.error(generateServerError())
        }
        
        // Check if we should fail with auth error
        if (shouldFailWithAuthError && currentFailureCount < maxFailuresBeforeSuccess) {
            currentFailureCount++
            recordRequest(requestId, operation, delay, false, ErrorType.AUTHENTICATION_ERROR)
            return Result.error(generateAuthenticationError())
        }
        
        // Check if we should fail with validation error
        if (shouldFailWithValidationError && currentFailureCount < maxFailuresBeforeSuccess) {
            currentFailureCount++
            recordRequest(requestId, operation, delay, false, ErrorType.VALIDATION_ERROR)
            return Result.error(generateValidationError())
        }
        
        // Reset failure flags if we've reached the max failures
        if (currentFailureCount >= maxFailuresBeforeSuccess) {
            shouldFailWithNetworkError = false
            shouldFailWithServerError = false
            shouldFailWithAuthError = false
            shouldFailWithValidationError = false
            currentFailureCount = 0
            maxFailuresBeforeSuccess = 0
        }
        
        return null
    }
    
    private fun generateNetworkError(): AppError {
        val networkErrors = listOf(
            "Connection timeout",
            "Network unreachable",
            "Connection refused",
            "DNS resolution failed",
            "SSL handshake failed",
            "Connection reset by peer"
        )
        return AppError.NetworkError(networkErrors.random())
    }
    
    private fun generateServerError(): AppError {
        val serverErrors = listOf(
            "Internal server error (500)",
            "Service unavailable (503)",
            "Bad gateway (502)",
            "Gateway timeout (504)",
            "Insufficient storage (507)"
        )
        return AppError.NetworkError(serverErrors.random())
    }
    
    private fun generateAuthenticationError(): AppError {
        val authErrors = listOf(
            "Authentication token expired",
            "Invalid credentials",
            "Access denied",
            "Token refresh required",
            "Session expired"
        )
        return AppError.AuthenticationError(authErrors.random())
    }
    
    private fun generateValidationError(): AppError {
        val validationErrors = listOf(
            "Invalid request format",
            "Missing required field",
            "Invalid field value",
            "Request too large",
            "Invalid content type"
        )
        return AppError.ValidationError(validationErrors.random())
    }
    
    private fun recordRequest(
        id: Int,
        operation: String,
        duration: Duration,
        success: Boolean,
        errorType: ErrorType? = null,
        retryCount: Int = 0
    ) {
        val request = ApiRequest(
            id = id,
            timestamp = Clock.System.now(),
            operation = operation,
            duration = duration,
            success = success,
            errorType = errorType,
            retryCount = retryCount
        )
        requestHistory.add(request)
    }
    
    // Data classes for results and statistics
    
    data class BatchResult<T>(
        val totalOperations: Int,
        val successfulOperations: Int,
        val failedOperations: Int,
        val results: List<T>,
        val errors: List<AppError>
    )
    
    data class RequestStatistics(
        val totalRequests: Int,
        val successfulRequests: Int,
        val failedRequests: Int,
        val successRate: Double,
        val averageLatencyMs: Double,
        val errorsByType: Map<ErrorType?, Int>
    )
}