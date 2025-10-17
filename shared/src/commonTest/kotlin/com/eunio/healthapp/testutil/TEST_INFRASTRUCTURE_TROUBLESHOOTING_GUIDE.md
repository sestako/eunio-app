# Test Infrastructure Troubleshooting Guide

## Overview

This guide provides comprehensive troubleshooting information for common issues encountered with the test infrastructure. It covers compilation errors, runtime failures, performance issues, and platform-specific problems.

## Quick Diagnosis Checklist

When encountering test issues, check these common causes first:

- [ ] Are you extending `BaseKoinTest` for dependency injection?
- [ ] Are you using `TestDataBuilder` for test data creation?
- [ ] Are mock services properly configured through Koin?
- [ ] Are test methods properly annotated with `@Test`?
- [ ] Are setup and teardown methods called correctly?
- [ ] Are you cleaning up resources in `@AfterTest`?

## Compilation Errors

### Unresolved Reference Errors

**Symptoms:**
```
Unresolved reference: MockNetworkConnectivity
Unresolved reference: TestDataBuilder
Unresolved reference: BaseKoinTest
```

**Causes and Solutions:**

1. **Missing Imports**
   ```kotlin
   // Add missing imports
   import com.eunio.healthapp.testutil.MockServices.*
   import com.eunio.healthapp.testutil.TestDataBuilder
   import com.eunio.healthapp.testutil.BaseKoinTest
   ```

2. **Wrong Package Structure**
   ```kotlin
   // Ensure test files are in correct package
   package com.eunio.healthapp.domain.usecase // Correct package
   
   // Not in androidTest or iosTest unless platform-specific
   ```

3. **Missing Test Dependencies**
   ```kotlin
   // Check build.gradle.kts has test dependencies
   commonTest {
       dependencies {
           implementation(kotlin("test"))
           implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
           implementation("io.insert-koin:koin-test")
       }
   }
   ```

### Type Mismatch Errors

**Symptoms:**
```
Type mismatch: inferred type is MockUserRepository but UserRepository was expected
Constructor parameter 'theme' has changed from String to ThemeType
```

**Solutions:**

1. **Cast Mock Services Correctly**
   ```kotlin
   // ✅ Correct casting
   val mockRepo = get<UserRepository>() as MockUserRepository
   
   // ❌ Wrong - direct instantiation
   val mockRepo = MockUserRepository()
   ```

2. **Update Data Model Usage**
   ```kotlin
   // ✅ Use current constructor parameters
   val preferences = TestDataBuilder.createDisplayPreferences(
       theme = ThemeType.DARK, // Enum, not String
       language = LanguageCode.EN
   )
   
   // ❌ Old constructor parameters
   val preferences = DisplayPreferences(
       theme = "dark", // String - wrong type
       language = "en"
   )
   ```

### Abstract Class Implementation Errors

**Symptoms:**
```
Class 'MockUserRepository' is not abstract and does not implement abstract member 'getLastModifiedTimestamp'
```

**Solutions:**

1. **Implement Missing Abstract Members**
   ```kotlin
   class MockUserRepository : UserRepository {
       // Implement all abstract methods
       override suspend fun getLastModifiedTimestamp(): Instant? = 
           Instant.now()
       
       override suspend fun markAsSynced(userId: String): Result<Unit> = 
           Result.success(Unit)
   }
   ```

2. **Check Interface Changes**
   ```kotlin
   // If interface changed, update all implementations
   // Check the actual interface definition for current methods
   ```

## Runtime Errors

### Koin Dependency Injection Failures

**Symptoms:**
```
org.koin.core.error.NoBeanDefFoundException: No definition found for class 'UserRepository'
```

**Diagnosis:**
```kotlin
// Check if test module is properly configured
@Test
fun `debug koin configuration`() {
    val koin = getKoin()
    println("Registered modules: ${koin.instanceRegistry}")
    
    // Verify specific service registration
    val hasUserRepo = koin.getOrNull<UserRepository>() != null
    println("UserRepository registered: $hasUserRepo")
}
```

**Solutions:**

1. **Ensure BaseKoinTest is Extended**
   ```kotlin
   class MyTest : BaseKoinTest() { // Must extend BaseKoinTest
       @Test
       fun `my test`() {
           val service = get<UserRepository>() // Will work
       }
   }
   ```

2. **Check Test Module Configuration**
   ```kotlin
   // Verify TestModule.kt has all required services
   val testModule = module {
       single<UserRepository> { MockUserRepository() }
       single<NetworkConnectivity> { MockNetworkConnectivity() }
       // Add missing services here
   }
   ```

3. **Custom Module Override**
   ```kotlin
   class SpecialTest : BaseKoinTest() {
       override fun getTestModules(): List<Module> {
           return super.getTestModules() + module {
               single<SpecialService> { MockSpecialService() }
           }
       }
   }
   ```

### Mock Service Configuration Issues

**Symptoms:**
```
MockNetworkConnectivity returned null when isConnected() was called
Mock service behavior not working as expected
```

**Diagnosis:**
```kotlin
@Test
fun `debug mock service state`() {
    val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
    
    println("Mock state:")
    println("  isConnected: ${mockNetwork.isConnected}")
    println("  isStable: ${mockNetwork.isStable}")
    println("  connectionType: ${mockNetwork.connectionType}")
    println("  shouldThrowException: ${mockNetwork.shouldThrowException}")
}
```

**Solutions:**

1. **Proper Mock Configuration**
   ```kotlin
   @Test
   fun `test with properly configured mock`() {
       val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
       
       // Configure BEFORE using
       mockNetwork.apply {
           isConnected = true
           isStable = true
           connectionType = ConnectionType.WIFI
       }
       
       // Now use the service
       val service = get<SyncService>()
       val result = service.syncData()
   }
   ```

2. **Reset Mock State**
   ```kotlin
   @BeforeTest
   fun setup() {
       super.setup()
       
       // Reset all mocks to known state
       val mockNetwork = get<NetworkConnectivity>() as MockNetworkConnectivity
       mockNetwork.reset() // If reset method available
   }
   ```

### Test Data Creation Failures

**Symptoms:**
```
IllegalArgumentException: Invalid user ID format
NullPointerException in TestDataBuilder.createUser()
```

**Diagnosis:**
```kotlin
@Test
fun `debug test data creation`() {
    try {
        val user = TestDataBuilder.createUser()
        println("Created user: $user")
    } catch (e: Exception) {
        println("Failed to create user: ${e.message}")
        e.printStackTrace()
    }
}
```

**Solutions:**

1. **Use Correct TestDataBuilder Methods**
   ```kotlin
   // ✅ Correct usage
   val user = TestDataBuilder.createUser(
       id = TestDataBuilder.generateUniqueUserId(),
       email = "test@example.com"
   )
   
   // ❌ Wrong - manual construction
   val user = User(
       id = "invalid-id-format",
       email = null // Missing required field
   )
   ```

2. **Check Required Parameters**
   ```kotlin
   // If TestDataBuilder fails, check what parameters are required
   val user = TestDataBuilder.createUser(
       // Provide all required parameters explicitly
       id = "user-${UUID.randomUUID()}",
       email = "test@example.com",
       profile = TestDataBuilder.createUserProfile()
   )
   ```

## Performance Issues

### Slow Test Execution

**Symptoms:**
- Tests take longer than 5 minutes to complete
- Individual tests timeout
- Memory usage grows continuously

**Diagnosis:**
```kotlin
@Test
fun `measure test performance`() {
    val startTime = TimeSource.Monotonic.markNow()
    val startMemory = getMemoryUsage()
    
    // Your test logic here
    performTestOperation()
    
    val duration = startTime.elapsedNow()
    val memoryUsed = getMemoryUsage() - startMemory
    
    println("Test duration: $duration")
    println("Memory used: $memoryUsed")
    
    assertTrue(duration < 30.seconds, "Test too slow: $duration")
    assertTrue(memoryUsed < 50.MB, "Memory usage too high: $memoryUsed")
}
```

**Solutions:**

1. **Use Performance-Optimized Base Class**
   ```kotlin
   class MyPerformanceTest : PerformanceOptimizedBaseTest() {
       @Test
       fun `fast test`() {
           // Automatically gets performance optimizations
       }
   }
   ```

2. **Optimize Test Data Creation**
   ```kotlin
   // ✅ Use cached test data for large datasets
   val largeDataset = TestDataBuilder.createLargeDatasetOptimized(
       size = 10000,
       useCache = true
   )
   
   // ❌ Create large dataset from scratch each time
   val largeDataset = (1..10000).map { 
       TestDataBuilder.createUser() 
   }
   ```

3. **Proper Resource Cleanup**
   ```kotlin
   @AfterTest
   fun cleanup() {
       super.teardown()
       
       // Force cleanup of large objects
       ResourceCleanupManager.forceCleanup()
       
       // Clear caches
       TestDataBuilder.clearCache()
   }
   ```

### Memory Leaks

**Symptoms:**
- Memory usage increases with each test
- OutOfMemoryError during test execution
- Tests become progressively slower

**Diagnosis:**
```kotlin
class MemoryLeakDetectionTest : BaseKoinTest() {
    
    @Test
    fun `detect memory leaks`() {
        val initialMemory = getMemoryUsage()
        
        repeat(100) {
            // Perform operation that might leak
            val service = get<MyService>()
            service.performOperation()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = getMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue(
            memoryIncrease < 10.MB, 
            "Potential memory leak: ${memoryIncrease} increase"
        )
    }
}
```

**Solutions:**

1. **Use ResourceCleanupManager**
   ```kotlin
   @AfterTest
   fun cleanup() {
       super.teardown()
       ResourceCleanupManager.cleanup()
   }
   ```

2. **Avoid Static References**
   ```kotlin
   // ❌ Static references cause memory leaks
   companion object {
       val sharedService = MyService()
   }
   
   // ✅ Create fresh instances
   @BeforeTest
   fun setup() {
       // Get fresh instance from Koin
       val service = get<MyService>()
   }
   ```

## Platform-Specific Issues

### Android Test Issues

**Symptoms:**
```
java.lang.RuntimeException: Method getMainLooper in android.os.Looper not mocked
Context is null in Android tests
```

**Solutions:**

1. **Use AndroidTestContext**
   ```kotlin
   class AndroidSpecificTest : BaseKoinTest() {
       
       @BeforeTest
       fun setup() {
           super.setup()
           
           // Set up Android test environment
           val mockContext = AndroidTestContext.createMockContext()
           // Use mockContext in tests
       }
   }
   ```

2. **Mock Android Dependencies**
   ```kotlin
   // Add to test module
   val androidTestModule = module {
       single<Context> { AndroidTestContext.createMockContext() }
       single<SharedPreferences> { 
           AndroidTestContext.createMockSharedPreferences() 
       }
   }
   ```

### iOS Test Issues

**Symptoms:**
```
Platform.NSUserDefaults not available in test environment
iOS-specific services not working in CI/CD
```

**Solutions:**

1. **Use IOSTestSupport**
   ```kotlin
   class IOSSpecificTest : BaseKoinTest() {
       
       @BeforeTest
       fun setup() {
           super.setup()
           IOSTestSupport.setupIOSTestEnvironment()
       }
   }
   ```

2. **Check CI/CD Configuration**
   ```kotlin
   // Verify iOS test compatibility
   @Test
   fun `verify ios test environment`() {
       val validator = IOSTestCompatibilityValidator()
       val result = validator.validateEnvironment()
       
       assertTrue(result.isValid, "iOS test environment issues: ${result.issues}")
   }
   ```

## Cross-Platform Consistency Issues

**Symptoms:**
- Tests pass on one platform but fail on another
- Different behavior between Android and iOS tests
- Inconsistent test results

**Diagnosis:**
```kotlin
@Test
fun `check cross platform consistency`() {
    val runner = CrossPlatformTestRunner()
    val result = runner.runConsistencyCheck()
    
    if (!result.isConsistent) {
        println("Platform differences found:")
        result.differences.forEach { diff ->
            println("  ${diff.platform}: ${diff.issue}")
        }
    }
    
    assertTrue(result.isConsistent, "Cross-platform inconsistency detected")
}
```

**Solutions:**

1. **Use Platform-Agnostic Test Code**
   ```kotlin
   // ✅ Platform-agnostic
   @Test
   fun `test works on all platforms`() {
       val service = get<UserService>()
       val result = service.getUser("test-id")
       assertTrue(result.isSuccess)
   }
   
   // ❌ Platform-specific code in common test
   @Test
   fun `android specific test in common`() {
       val context = getAndroidContext() // Won't work on iOS
   }
   ```

2. **Use Expect/Actual for Platform Differences**
   ```kotlin
   // In commonTest
   expect fun getPlatformSpecificTestData(): TestData
   
   // In androidUnitTest
   actual fun getPlatformSpecificTestData(): TestData = 
       AndroidTestData()
   
   // In iosTest  
   actual fun getPlatformSpecificTestData(): TestData = 
       IOSTestData()
   ```

## CI/CD Integration Issues

### Build Failures in CI/CD

**Symptoms:**
- Tests pass locally but fail in CI/CD
- Timeout errors in automated builds
- Platform-specific build failures

**Diagnosis:**
```kotlin
// Add CI/CD environment detection
@Test
fun `detect ci environment`() {
    val isCI = System.getenv("CI") != null
    val platform = Platform.current
    
    println("Running in CI: $isCI")
    println("Platform: $platform")
    
    if (isCI) {
        // Adjust test behavior for CI environment
        TestConfiguration.setCIMode(true)
    }
}
```

**Solutions:**

1. **CI-Specific Configuration**
   ```kotlin
   class CIOptimizedTest : BaseKoinTest() {
       
       @BeforeTest
       fun setup() {
           super.setup()
           
           if (TestConfiguration.isCI()) {
               // Reduce timeouts, disable animations, etc.
               TestConfiguration.optimizeForCI()
           }
       }
   }
   ```

2. **Deterministic Test Execution**
   ```kotlin
   @Test
   fun `deterministic test for ci`() {
       DeterministicTestExecution.withFixedSeed(12345) {
           // Test logic that produces consistent results
       }
   }
   ```

## Error Recovery and Resilience

### Automatic Error Recovery

```kotlin
abstract class ResilientTest : BaseKoinTest() {
    
    protected inline fun <T> withRetry(
        maxAttempts: Int = 3,
        delay: Duration = 1.seconds,
        operation: () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(delay.inWholeMilliseconds)
                }
            }
        }
        
        throw lastException ?: RuntimeException("Operation failed after $maxAttempts attempts")
    }
}

class MyResilientTest : ResilientTest() {
    
    @Test
    fun `test with automatic retry`() {
        val result = withRetry {
            // Operation that might fail intermittently
            unstableService.performOperation()
        }
        
        assertNotNull(result)
    }
}
```

### Graceful Degradation

```kotlin
@Test
fun `test with graceful degradation`() {
    try {
        // Try optimal path
        val result = performOptimalOperation()
        assertEquals(expectedResult, result)
    } catch (e: ServiceUnavailableException) {
        // Fall back to alternative approach
        val fallbackResult = performFallbackOperation()
        assertNotNull(fallbackResult)
        println("Used fallback due to: ${e.message}")
    }
}
```

## Debugging Tools and Utilities

### Test Diagnostics

```kotlin
@Test
fun `run comprehensive diagnostics`() {
    val diagnostics = TestDiagnostics.runFullDiagnostics()
    
    println("=== Test Environment Diagnostics ===")
    println("Koin Status: ${diagnostics.koinStatus}")
    println("Mock Services: ${diagnostics.mockServiceStatus}")
    println("Test Data: ${diagnostics.testDataStatus}")
    println("Platform: ${diagnostics.platformStatus}")
    println("Performance: ${diagnostics.performanceStatus}")
    
    if (!diagnostics.allHealthy) {
        println("\n=== Issues Found ===")
        diagnostics.issues.forEach { issue ->
            println("${issue.severity}: ${issue.description}")
            println("  Solution: ${issue.suggestedSolution}")
        }
    }
    
    assertTrue(diagnostics.allHealthy, "Test environment issues detected")
}
```

### Mock Service Inspector

```kotlin
@Test
fun `inspect mock service configuration`() {
    val inspector = MockServiceInspector()
    val report = inspector.inspectAllMockServices()
    
    report.services.forEach { service ->
        println("${service.name}:")
        println("  Configured: ${service.isConfigured}")
        println("  State: ${service.currentState}")
        println("  Call Count: ${service.callCount}")
        
        if (service.hasIssues) {
            println("  Issues: ${service.issues}")
        }
    }
}
```

## Prevention Strategies

### Code Review Checklist

When reviewing test code, check for:

- [ ] **Proper inheritance**: Extends BaseKoinTest or appropriate base class
- [ ] **Test data creation**: Uses TestDataBuilder consistently
- [ ] **Mock configuration**: Mocks configured through Koin DI
- [ ] **Resource cleanup**: Proper @AfterTest cleanup
- [ ] **Error handling**: Tests both success and failure cases
- [ ] **Performance**: No obvious performance issues
- [ ] **Platform compatibility**: Works on target platforms
- [ ] **Deterministic behavior**: No random or time-dependent behavior

### Automated Validation

```kotlin
// Add to CI/CD pipeline
class TestInfrastructureValidation {
    
    @Test
    fun `validate test infrastructure health`() {
        val validator = TestValidationPipeline.Builder()
            .addValidation("compilation") { validateCompilation() }
            .addValidation("koin_setup") { validateKoinSetup() }
            .addValidation("mock_services") { validateMockServices() }
            .addValidation("test_data") { validateTestDataBuilder() }
            .addValidation("performance") { validatePerformance() }
            .build()
        
        val result = validator.execute()
        
        assertTrue(result.allPassed, "Infrastructure validation failed: ${result.failures}")
    }
}
```

## Getting Help

### Internal Resources

1. **Documentation Files**:
   - `TEST_WRITING_GUIDELINES.md` - Comprehensive test writing guide
   - `MOCK_SERVICE_USAGE_PATTERNS.md` - Mock service patterns
   - `TEST_DATA_CREATION_EXAMPLES.md` - Test data examples

2. **Utility Classes**:
   - `TestDiagnostics` - Diagnostic utilities
   - `TestValidationPipeline` - Validation framework
   - `CrossPlatformTestRunner` - Cross-platform testing

3. **Example Tests**:
   - Look at existing working tests for patterns
   - Check `*Test.kt` files for examples

### External Resources

1. **Kotlin Multiplatform Testing**: [Official Documentation](https://kotlinlang.org/docs/multiplatform-run-tests.html)
2. **Koin Testing**: [Koin Test Documentation](https://insert-koin.io/docs/reference/koin-test/testing)
3. **Coroutines Testing**: [Coroutines Test Documentation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)

### Escalation Path

If you can't resolve an issue:

1. **Check this troubleshooting guide** for similar issues
2. **Run diagnostic utilities** to identify the problem
3. **Review recent changes** that might have caused the issue
4. **Check CI/CD logs** for additional error information
5. **Create a minimal reproduction case** to isolate the problem
6. **Document the issue** with steps to reproduce and expected vs actual behavior

Remember: Most test infrastructure issues are caused by configuration problems, not bugs in the test framework itself. Start with the basics and work your way up to more complex scenarios.