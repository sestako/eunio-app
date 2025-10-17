package com.eunio.healthapp.testutil

import com.eunio.healthapp.domain.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Common test extensions and utilities for consistent testing patterns
 */

// Result testing extensions
/**
 * Asserts that a Result is Success and returns the value
 */
fun <T> Result<T>.assertSuccess(): T {
    return when (this) {
        is Result.Success -> this.data
        is Result.Error -> fail("Expected Success but got Error: ${this.error.message}")
    }
}

/**
 * Asserts that a Result is Error and returns the exception
 */
fun <T> Result<T>.assertError(): com.eunio.healthapp.domain.error.AppError {
    return when (this) {
        is Result.Success -> fail("Expected Error but got Success: ${this.data}")
        is Result.Error -> this.error
    }
}

/**
 * Asserts that a Result is Success with a specific value
 */
fun <T> Result<T>.assertSuccessEquals(expected: T) {
    val actual = assertSuccess()
    assertEquals(expected, actual)
}

/**
 * Asserts that a Result is Error with a specific message
 */
fun <T> Result<T>.assertErrorMessage(expectedMessage: String) {
    val error = assertError()
    assertTrue(
        error.message?.contains(expectedMessage) == true,
        "Expected error message to contain '$expectedMessage' but was '${error.message}'"
    )
}

// Async testing extensions
/**
 * Waits for a condition to become true within a timeout
 */
suspend fun waitForCondition(
    timeout: Duration = 5.seconds,
    checkInterval: Duration = 100.seconds,
    condition: suspend () -> Boolean
) {
    withTimeout(timeout) {
        while (!condition()) {
            delay(checkInterval)
        }
    }
}

/**
 * Waits for a condition to become true and returns a result
 */
suspend fun <T> waitForResult(
    timeout: Duration = 5.seconds,
    checkInterval: Duration = 100.seconds,
    operation: suspend () -> T?
): T {
    return withTimeout(timeout) {
        var result: T? = null
        while (result == null) {
            result = operation()
            if (result == null) {
                delay(checkInterval)
            }
        }
        result
    }
}

/**
 * Executes a suspend function and measures execution time
 */
suspend fun <T> measureExecutionTime(operation: suspend () -> T): Pair<T, Duration> {
    val startTimeMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val result = operation()
    val endTimeMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val durationMillis = endTimeMillis - startTimeMillis
    val duration = kotlin.time.Duration.parse("PT${durationMillis / 1000.0}S")
    return Pair(result, duration)
}

// Date and time testing extensions
/**
 * Creates a test date for consistent testing
 */
fun createTestDate(year: Int = 2024, month: Int = 1, day: Int = 15): LocalDate {
    return LocalDate(year, month, day)
}

/**
 * Creates a test instant for consistent testing
 */
fun createTestInstant(year: Int = 2024, month: Int = 1, day: Int = 15, hour: Int = 12): Instant {
    return LocalDateTime(year, month, day, hour, 0, 0, 0)
        .toInstant(TimeZone.UTC)
}

/**
 * Checks if two instants are within a certain tolerance
 */
fun Instant.isWithinTolerance(other: Instant, tolerance: Duration = 1.seconds): Boolean {
    val diff = if (this > other) this - other else other - this
    return diff <= tolerance
}

/**
 * Asserts that two instants are within a certain tolerance
 */
fun assertInstantsWithinTolerance(
    expected: Instant,
    actual: Instant,
    tolerance: Duration = 1.seconds,
    message: String? = null
) {
    assertTrue(
        expected.isWithinTolerance(actual, tolerance),
        message ?: "Expected $expected and $actual to be within $tolerance of each other"
    )
}

// Collection testing extensions
/**
 * Asserts that a collection contains exactly the expected items in any order
 */
fun <T> Collection<T>.assertContainsExactly(vararg expected: T) {
    assertEquals(expected.size, this.size, "Collection size mismatch")
    expected.forEach { item ->
        assertTrue(
            this.contains(item),
            "Collection should contain $item but was $this"
        )
    }
}

/**
 * Asserts that a collection contains all expected items (may contain others)
 */
fun <T> Collection<T>.assertContainsAll(vararg expected: T) {
    expected.forEach { item ->
        assertTrue(
            this.contains(item),
            "Collection should contain $item but was $this"
        )
    }
}

/**
 * Asserts that a collection is empty
 */
fun <T> Collection<T>.assertEmpty() {
    assertTrue(this.isEmpty(), "Collection should be empty but was $this")
}

/**
 * Asserts that a collection is not empty
 */
fun <T> Collection<T>.assertNotEmpty() {
    assertTrue(this.isNotEmpty(), "Collection should not be empty")
}

/**
 * Asserts that a collection has a specific size
 */
fun <T> Collection<T>.assertSize(expectedSize: Int) {
    assertEquals(expectedSize, this.size, "Collection size mismatch")
}

// String testing extensions
/**
 * Asserts that a string contains a substring (case-insensitive)
 */
fun String.assertContainsIgnoreCase(substring: String) {
    assertTrue(
        this.contains(substring, ignoreCase = true),
        "String '$this' should contain '$substring' (case-insensitive)"
    )
}

/**
 * Asserts that a string matches a regex pattern
 */
fun String.assertMatches(pattern: Regex) {
    assertTrue(
        pattern.matches(this),
        "String '$this' should match pattern '$pattern'"
    )
}

/**
 * Asserts that a string is not blank
 */
fun String?.assertNotBlank() {
    assertNotNull(this, "String should not be null")
    assertTrue(this.isNotBlank(), "String should not be blank but was '$this'")
}

// Mock service testing extensions
/**
 * Resets all services in a MockServiceSet
 */
fun MockServiceSet.resetAndVerify() {
    this.resetAll()
    
    // Verify all services are reset to clean state
    assertTrue(platformManager.getDeepLinkHistory().isEmpty())
    assertTrue(hapticFeedbackManager.getHapticHistory().isEmpty())
    assertTrue(themeManager.getThemeHistory().isEmpty())
    assertTrue(notificationService.getScheduledNotifications().isEmpty())
    assertTrue(lifecycleManager.getLifecycleEvents().isEmpty())
    assertTrue(navigationManager.getNavigationHistory().isEmpty())
}

/**
 * Verifies that a mock service has recorded specific events
 */
fun MockHapticFeedbackManager.assertHapticPerformed(method: String, count: Int = 1) {
    val events = getHapticHistory().filter { it.method == method }
    assertEquals(count, events.size, "Expected $count '$method' haptic events but found ${events.size}")
}

/**
 * Verifies that a mock theme manager has applied theme changes
 */
fun MockThemeManager.assertThemeEventOccurred(method: String, count: Int = 1) {
    val events = getThemeHistory().filter { it.method == method }
    assertEquals(count, events.size, "Expected $count '$method' theme events but found ${events.size}")
}

/**
 * Verifies that a mock lifecycle manager has recorded specific lifecycle events
 */
fun MockPlatformLifecycleManager.assertLifecycleEventOccurred(eventType: String, count: Int = 1) {
    val events = getLifecycleEvents().filter { it.eventType == eventType }
    assertEquals(count, events.size, "Expected $count '$eventType' lifecycle events but found ${events.size}")
}

// Test data validation extensions
/**
 * Validates that a user ID follows the expected format
 */
fun String.assertValidUserId() {
    assertNotBlank()
    assertTrue(
        this.matches(Regex("^[a-zA-Z0-9_-]+$")),
        "User ID '$this' should only contain alphanumeric characters, underscores, and hyphens"
    )
}

/**
 * Validates that an email follows basic email format
 */
fun String.assertValidEmail() {
    assertNotBlank()
    assertTrue(
        this.contains("@") && this.contains("."),
        "Email '$this' should contain @ and . characters"
    )
    assertTrue(
        this.matches(Regex("^[^@]+@[^@]+\\.[^@]+$")),
        "Email '$this' should follow basic email format"
    )
}

/**
 * Validates that a password meets basic requirements
 */
fun String.assertValidPassword() {
    assertNotBlank()
    assertTrue(
        this.length >= 8,
        "Password should be at least 8 characters long but was ${this.length}"
    )
}

// Performance testing extensions
/**
 * Asserts that an operation completes within a specified time
 */
suspend fun <T> assertCompletesWithin(
    timeout: Duration,
    operation: suspend () -> T
): T {
    val (result, duration) = measureExecutionTime(operation)
    assertTrue(
        duration <= timeout,
        "Operation should complete within $timeout but took $duration"
    )
    return result
}

/**
 * Asserts that an operation takes at least a minimum amount of time
 */
suspend fun <T> assertTakesAtLeast(
    minimumTime: Duration,
    operation: suspend () -> T
): T {
    val (result, duration) = measureExecutionTime(operation)
    assertTrue(
        duration >= minimumTime,
        "Operation should take at least $minimumTime but took $duration"
    )
    return result
}

// Error testing extensions
/**
 * Asserts that a suspend function throws a specific exception type
 */
suspend inline fun <reified T : Exception> assertThrows(
    message: String? = null,
    noinline operation: suspend () -> Unit
): T {
    try {
        operation()
        fail(message ?: "Expected ${T::class.simpleName} to be thrown")
    } catch (e: Exception) {
        if (e is T) {
            return e
        } else {
            fail(message ?: "Expected ${T::class.simpleName} but got ${e::class.simpleName}: ${e.message}")
        }
    }
}

/**
 * Asserts that a suspend function does not throw any exception
 */
suspend fun assertDoesNotThrow(
    message: String? = null,
    operation: suspend () -> Unit
) {
    try {
        operation()
    } catch (e: Exception) {
        fail(message ?: "Expected no exception but got ${e::class.simpleName}: ${e.message}")
    }
}

// Test isolation extensions
/**
 * Runs a test with automatic cleanup
 */
suspend fun <T> runTestWithCleanup(
    setup: suspend () -> Unit = {},
    cleanup: suspend () -> Unit = {},
    test: suspend () -> T
): T {
    try {
        setup()
        return test()
    } finally {
        cleanup()
    }
}

/**
 * Runs a test with mock service isolation
 */
suspend fun <T> runTestWithMockServices(
    mockServices: MockServiceSet = MockServiceFactory.createMockServiceSet(),
    test: suspend (MockServiceSet) -> T
): T {
    return runTestWithCleanup(
        setup = { mockServices.resetAll() },
        cleanup = { mockServices.resetAll() },
        test = { test(mockServices) }
    )
}

// Debugging extensions
/**
 * Prints debug information about a Result
 */
fun <T> Result<T>.debug(label: String = "Result"): Result<T> {
    when (this) {
        is Result.Success -> println("$label: Success($data)")
        is Result.Error -> println("$label: Error(${error.message})")
    }
    return this
}

/**
 * Prints debug information about a collection
 */
fun <T> Collection<T>.debug(label: String = "Collection"): Collection<T> {
    println("$label: size=${this.size}, items=$this")
    return this
}

/**
 * Prints debug information about execution time
 */
suspend fun <T> debugExecutionTime(
    label: String = "Operation",
    operation: suspend () -> T
): T {
    val (result, duration) = measureExecutionTime(operation)
    println("$label took $duration")
    return result
}