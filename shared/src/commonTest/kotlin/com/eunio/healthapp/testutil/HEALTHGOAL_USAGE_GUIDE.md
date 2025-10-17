# HealthGoal Enum Usage Guide

## Overview

This guide documents the correct usage of the `HealthGoal` enum in the Eunio Health App test infrastructure. This addresses task 2.2 from the test infrastructure fixes specification.

## Valid HealthGoal Values

The `HealthGoal` enum contains exactly four valid values:

```kotlin
enum class HealthGoal {
    CONCEPTION,      // User trying to conceive
    CONTRACEPTION,   // User preventing pregnancy  
    CYCLE_TRACKING,  // General cycle tracking
    GENERAL_HEALTH   // Overall health monitoring
}
```

## ❌ Incorrect Values (Fixed)

The following values were mentioned in the task but **do not exist** and should never be used:

- `FERTILITY_TRACKING` - This was an incorrect reference, use `CONCEPTION` instead
- `fertility_tracking` - Lowercase versions are not valid enum values
- String literals like `"FERTILITY_TRACKING"` - Use proper enum values

## ✅ Correct Usage Patterns

### 1. Creating Users with HealthGoal

```kotlin
// ✅ Correct - Using enum values
val user = User(
    id = "user123",
    email = "test@example.com", 
    name = "Test User",
    onboardingComplete = true,
    primaryGoal = HealthGoal.CONCEPTION,  // ✅ Proper enum usage
    unitSystem = UnitSystem.METRIC,
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now()
)

// ❌ Incorrect - Using string literals
val badUser = User(
    primaryGoal = "CONCEPTION"  // ❌ This won't compile
)
```

### 2. Database Layer Usage

For database operations, the DAO layer correctly converts between enum and string:

```kotlin
// ✅ Correct - Domain model with enum
userDao.insertUser(user)  // Automatically converts enum to string for DB

// ✅ Correct - Raw database insertion (for testing)
userDao.insertUser(
    id = "user1",
    primaryGoal = "CONCEPTION",  // ✅ String is correct for raw DB operations
    // ... other parameters
)
```

### 3. Test Data Creation

Use the provided test utilities for consistent data creation:

```kotlin
// ✅ Using TestDataBuilder
val conceptionUser = TestDataBuilder.createConceptionUser()
val contraceptionUser = TestDataBuilder.createContraceptionUser()
val cycleUser = TestDataBuilder.createCycleTrackingUser()
val healthUser = TestDataBuilder.createGeneralHealthUser()

// ✅ Using HealthGoalTestUtils
val testUser = HealthGoalTestUtils.createTestUser(
    primaryGoal = HealthGoal.CONCEPTION
)

// ✅ Creating users for all goals
val allUsers = TestDataBuilder.createUsersWithAllHealthGoals()
```

## Test Utilities

### HealthGoalTestUtils

Provides utilities for working with HealthGoal enums in tests:

```kotlin
// Validate enum values
HealthGoalTestUtils.isValidHealthGoal(goal)

// Convert to database string
val dbValue = HealthGoalTestUtils.getHealthGoalDatabaseValue(goal)

// Parse from string safely
val goal = HealthGoalTestUtils.parseHealthGoal("CONCEPTION")

// Get all valid goals
val allGoals = HealthGoalTestUtils.ALL_HEALTH_GOALS
```

### TestDataBuilder

Provides factory methods for creating test data:

```kotlin
// Create specific goal users
TestDataBuilder.createConceptionUser()
TestDataBuilder.createContraceptionUser()
TestDataBuilder.createCycleTrackingUser()
TestDataBuilder.createGeneralHealthUser()

// Create custom user
TestDataBuilder.createUser(primaryGoal = HealthGoal.CONCEPTION)
```

## Migration from Incorrect Usage

If you encounter old code using incorrect values:

```kotlin
// ❌ Old incorrect usage
val goal = "FERTILITY_TRACKING"  // This doesn't exist

// ✅ Correct replacement
val goal = HealthGoal.CONCEPTION  // Use this for fertility tracking

// ❌ Old incorrect usage  
primaryGoal = HealthGoal.FERTILITY_TRACKING  // Compilation error

// ✅ Correct replacement
primaryGoal = HealthGoal.CONCEPTION  // Proper enum value
```

## Validation

The `HealthGoalValidationTest` ensures all enum usage is correct:

- Validates all enum values exist
- Tests string conversion
- Verifies test utilities work correctly
- Ensures no invalid values are used

## Best Practices

1. **Always use enum values** in domain models and business logic
2. **Use string values only** for raw database operations
3. **Use test utilities** for consistent test data creation
4. **Validate enum usage** with the provided validation tests
5. **Document goal purposes** clearly in test scenarios

## Goal Descriptions

- **CONCEPTION**: User is trying to conceive and tracks fertility indicators
- **CONTRACEPTION**: User is preventing pregnancy and tracks cycle for contraception
- **CYCLE_TRACKING**: General cycle tracking for menstrual health monitoring  
- **GENERAL_HEALTH**: Overall health monitoring with basic health tracking

## Error Prevention

The test infrastructure now prevents common errors:

1. **Compilation errors** for non-existent enum values
2. **Runtime validation** of enum usage in tests
3. **Consistent test data** through centralized builders
4. **Clear documentation** of correct usage patterns

## Files Updated

- `HealthGoalTestUtils.kt` - New utility for HealthGoal operations
- `TestDataBuilder.kt` - Enhanced with HealthGoal-specific methods
- `HealthGoalValidationTest.kt` - Comprehensive validation tests
- Database tests - Already using correct string/enum patterns

This ensures all HealthGoal usage is consistent, correct, and well-documented across the test infrastructure.