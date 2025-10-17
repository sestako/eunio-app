# Migration Guide: From Manual Instantiation to Dependency Injection

This guide helps you migrate from manual instantiation patterns to dependency injection using Koin.

## Overview

The application is transitioning from manual instantiation to dependency injection to improve:
- Testability
- Code maintainability
- Separation of concerns
- Configuration management

## Backward Compatibility

All existing manual instantiation patterns are preserved with deprecation warnings. Your existing code will continue to work, but you'll see deprecation warnings encouraging migration to DI.

## Migration Patterns

### 1. ViewModel Migration

#### Before (Manual Instantiation)
```kotlin
// Old pattern - deprecated
val viewModel = OnboardingViewModel(
    getCurrentUserUseCase = createGetCurrentUserUseCase(),
    completeOnboardingUseCase = createCompleteOnboardingUseCase()
)
```

#### After (Dependency Injection)
```kotlin
// New pattern - recommended
class MyKoinComponent : KoinComponent {
    private val viewModel: OnboardingViewModel by inject()
    
    // Or for immediate resolution
    fun getViewModel(): OnboardingViewModel = get<OnboardingViewModel>()
}
```

#### Transition Pattern (Backward Compatible)
```kotlin
// Transition pattern - uses DI when available, falls back to manual
val viewModel = BackwardCompatibilitySupport.createOnboardingViewModel()
```

### 2. Service Migration

#### Before (Manual Instantiation)
```kotlin
// Old pattern - deprecated
val settingsManager = AndroidSettingsManager(context, settingsRepository)
```

#### After (Dependency Injection)
```kotlin
// New pattern - recommended
class MyKoinComponent : KoinComponent {
    private val settingsManager: SettingsManager by inject()
}
```

#### Transition Pattern (Backward Compatible)
```kotlin
// Transition pattern - uses DI when available, falls back to manual
val settingsManager = BackwardCompatibilitySupport.createSettingsManager()
```

### 3. iOS Swift Integration

#### Before (IOSKoinHelper)
```swift
// Old pattern - deprecated
let viewModel = IOSKoinHelper.shared.getDailyLoggingViewModel()
```

#### After (SwiftUI Environment)
```swift
// New pattern - recommended
struct MyView: View {
    @Environment(\.viewModelFactory) private var viewModelFactory
    @StateObject private var viewModel: ModernObservableDailyLoggingViewModel
    
    init() {
        _viewModel = StateObject(wrappedValue: viewModelFactory.createDailyLoggingViewModel())
    }
}
```

### 4. Test Migration

#### Before (Manual Test Setup)
```kotlin
// Old pattern - deprecated
@Test
fun testViewModel() {
    val viewModel = OnboardingViewModel(
        getCurrentUserUseCase = mockGetCurrentUserUseCase,
        completeOnboardingUseCase = mockCompleteOnboardingUseCase
    )
    // test code
}
```

#### After (Koin Test Setup)
```kotlin
// New pattern - recommended
class ViewModelTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
    }
    
    @Test
    fun testViewModel() {
        val viewModel: OnboardingViewModel = get()
        // test code
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
    }
}
```

## Step-by-Step Migration Process

### Phase 1: Enable Dependency Injection (No Code Changes Required)
1. Koin initialization is already set up in both iOS and Android
2. All ViewModels and services are registered in Koin modules
3. Existing code continues to work with backward compatibility

### Phase 2: Migrate Components Gradually
1. **Start with new code**: Use DI for all new components
2. **Migrate tests**: Convert test setup to use Koin test modules
3. **Migrate ViewModels**: Replace manual instantiation with `get<T>()`
4. **Migrate services**: Replace manual instantiation with `get<T>()`

### Phase 3: Remove Deprecated Code
1. Remove calls to deprecated factory methods
2. Remove backward compatibility wrappers
3. Clean up unused manual instantiation code

## Common Migration Scenarios

### Scenario 1: Android Activity/Fragment
```kotlin
// Before
class MainActivity : AppCompatActivity() {
    private val viewModel = OnboardingViewModel(...)
}

// After
class MainActivity : AppCompatActivity(), KoinComponent {
    private val viewModel: OnboardingViewModel by inject()
}
```

### Scenario 2: iOS SwiftUI View
```swift
// Before
struct OnboardingView: View {
    @StateObject private var viewModel = OnboardingViewModel()
}

// After
struct OnboardingView: View {
    @Environment(\.viewModelFactory) private var viewModelFactory
    @StateObject private var viewModel: ModernObservableOnboardingViewModel
    
    init() {
        _viewModel = StateObject(wrappedValue: viewModelFactory.createOnboardingViewModel())
    }
}
```

### Scenario 3: Service Classes
```kotlin
// Before
class DataService {
    private val settingsManager = AndroidSettingsManager(...)
}

// After
class DataService : KoinComponent {
    private val settingsManager: SettingsManager by inject()
}
```

## Testing Migration

### Unit Tests
```kotlin
// Before
class ServiceTest {
    private val service = MyService(mockDependency1, mockDependency2)
}

// After
class ServiceTest : KoinTest {
    private val testModule = module {
        single<Dependency1> { mockDependency1 }
        single<Dependency2> { mockDependency2 }
        single { MyService(get(), get()) }
    }
    
    @BeforeTest
    fun setup() {
        startKoin { modules(testModule) }
    }
    
    @Test
    fun testService() {
        val service: MyService = get()
        // test code
    }
}
```

### Integration Tests
```kotlin
// Use existing test modules that provide mock implementations
class IntegrationTest : KoinTest {
    @BeforeTest
    fun setup() {
        startKoin {
            modules(
                testModule,
                mockServiceModule,
                viewModelModule
            )
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. "No definition found for class X"
**Cause**: The class is not registered in any Koin module.
**Solution**: Add the class to the appropriate module or use fallback factory.

```kotlin
// Quick fix - use backward compatibility
val instance = BackwardCompatibilitySupport.createMyService()

// Proper fix - add to module
val myModule = module {
    single<MyService> { MyServiceImpl(get()) }
}
```

#### 2. Circular Dependencies
**Cause**: Two classes depend on each other directly.
**Solution**: Use lazy injection or refactor dependencies.

```kotlin
// Use lazy injection
class ServiceA : KoinComponent {
    private val serviceB: ServiceB by inject()
}

class ServiceB : KoinComponent {
    private val serviceA: ServiceA by lazy { get<ServiceA>() }
}
```

#### 3. Platform-Specific Dependencies
**Cause**: Trying to inject platform-specific implementations in common code.
**Solution**: Use interfaces in common code, implementations in platform modules.

```kotlin
// Common code
expect class PlatformService

// Platform modules register implementations
val androidModule = module {
    single<PlatformService> { AndroidPlatformService() }
}
```

### Debugging Tips

1. **Enable Koin logging**: Set log level to DEBUG to see dependency resolution
2. **Check module registration**: Ensure all required modules are included in startKoin
3. **Verify scope**: Make sure you're requesting dependencies from the correct scope
4. **Use fallback factories**: Temporarily use backward compatibility support while debugging

## Best Practices

### 1. Prefer Constructor Injection
```kotlin
// Good
class MyService(
    private val dependency1: Dependency1,
    private val dependency2: Dependency2
) {
    // implementation
}

// Register in module
single { MyService(get(), get()) }
```

### 2. Use Interfaces for Dependencies
```kotlin
// Good
class MyService(
    private val repository: UserRepository // interface
) {
    // implementation
}
```

### 3. Keep Modules Focused
```kotlin
// Good - focused modules
val repositoryModule = module {
    // only repositories
}

val useCaseModule = module {
    // only use cases
}

val viewModelModule = module {
    // only view models
}
```

### 4. Use Scopes Appropriately
```kotlin
// Singleton for stateless services
single<ApiService> { ApiServiceImpl() }

// Factory for stateful components
factory<ViewModel> { MyViewModel(get()) }
```

## Timeline and Deprecation Schedule

### Current Phase (Backward Compatibility)
- All existing code works without changes
- Deprecation warnings guide migration
- New code should use DI patterns

### Next Phase (6 months)
- Increase deprecation level to ERROR
- Remove some backward compatibility wrappers
- Require DI for new features

### Final Phase (12 months)
- Remove all deprecated code
- Complete migration to DI
- Clean up fallback mechanisms

## Support and Resources

### Documentation
- [Koin Documentation](https://insert-koin.io/)
- [Kotlin Multiplatform DI Guide](https://kotlinlang.org/docs/multiplatform.html)

### Code Examples
- See `ModernViewModelWrapper.swift` for iOS patterns
- See test modules for testing patterns
- See existing ViewModels for DI usage examples

### Getting Help
- Check existing implementations in the codebase
- Use backward compatibility support during transition
- Consult team for complex migration scenarios