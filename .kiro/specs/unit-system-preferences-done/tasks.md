# Implementation Plan

- [x] 1. Create core domain models and enums
  - Create UnitSystem enum with locale detection logic
  - Create UserPreferences data class with serialization
  - Add SyncStatus enum for preference synchronization
  - Update existing User model to include unitSystem field
  - _Requirements: 1.3, 2.1, 5.1, 5.2, 5.3_

- [x] 2. Implement unit conversion system
  - Create UnitConverter interface with conversion and formatting methods
  - Implement UnitConverterImpl with round half up precision logic
  - Add weight conversion (kg ↔ lbs) with formula validation
  - Add distance conversion (km ↔ miles) with formula validation
  - Add temperature conversion (°C ↔ °F) with formula validation
  - Implement formatting methods for all measurement types
  - Write comprehensive unit tests for all conversion functions
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 8.1_

- [x] 3. Set up database schema and migrations
  - Add unitSystem column to existing User table via migration
  - Create UserPreferences table with foreign key constraints
  - Add database indexes for performance optimization
  - Update SQLDelight schema files with new table definitions
  - Implement database migration logic in DatabaseManager
  - Write tests for database schema changes and migrations
  - _Requirements: 2.1, 2.2, 7.3_

- [x] 4. Create preference data access layer
  - Create PreferencesLocalDataSource interface and implementation
  - Create PreferencesRemoteDataSource interface and implementation
  - Implement local storage operations using SQLDelight
  - Implement Firestore operations for cloud sync
  - Add methods for pending sync tracking and status management
  - Write unit tests for data source implementations
  - _Requirements: 2.1, 2.2, 2.4, 2.5_

- [x] 5. Implement preferences repository
  - Create PreferencesRepository interface with all required methods
  - Implement PreferencesRepositoryImpl with offline-first strategy
  - Add getUserPreferences() with local-first fallback logic
  - Add saveUserPreferences() with immediate local save and background sync
  - Implement syncPreferences() for batch synchronization
  - Implement clearPreferences() for data cleanup
  - Write unit tests for repository operations and sync logic
  - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 6. Create unit system manager
  - Create UnitSystemManager interface with preference management methods
  - Implement UnitSystemManagerImpl with reactive state management
  - Add getCurrentUnitSystem() with caching for performance
  - Add setUnitSystem() with validation and user model updates
  - Implement initializeFromLocale() with manual preference priority
  - Add observeUnitSystemChanges() using StateFlow for reactive updates
  - Write unit tests for manager operations and state management
  - _Requirements: 1.4, 1.5, 5.1, 5.4, 5.5, 6.4_

- [x] 7. Set up dependency injection
  - Create unitSystemModule for Koin with all component bindings
  - Add UnitConverter as singleton dependency
  - Add UnitSystemManager with repository dependencies
  - Add PreferencesRepository with data source dependencies
  - Add local and remote data sources with proper dependencies
  - Update existing SharedModule to include unit system module
  - Write tests for dependency injection configuration
  - _Requirements: 7.1, 7.2_

- [x] 8. Implement settings UI components
  - Create UnitSystemSettingItem composable with selection interface
  - Create UnitSystemOption composable for individual system selection
  - Add proper state management and user interaction handling
  - Implement immediate visual feedback for preference changes
  - Add confirmation messages (toast/snackbar) for setting changes
  - Apply Eunio design system styling and accessibility features
  - Write UI tests for settings interaction and feedback
  - _Requirements: 1.1, 1.2, 6.1, 6.2, 6.5, 6.6_

- [x] 9. Create measurement display components
  - Create WeightDisplay composable with unit conversion and formatting
  - Create DistanceDisplay composable with unit conversion and formatting
  - Create TemperatureDisplay composable with unit conversion and formatting
  - Implement reactive updates when unit system preference changes
  - Add proper dependency injection for UnitConverter
  - Ensure consistent formatting and precision across all components
  - Write UI tests for measurement display and unit system changes
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 7.4, 7.5_

- [x] 10. Implement locale-based initialization
  - Add platform-specific locale detection for Android and iOS
  - Implement first-time user unit system initialization logic
  - Add logic to respect manual preferences over locale changes
  - Create fallback to metric system when locale detection fails
  - Integrate initialization with existing onboarding flow
  - Write tests for locale detection and initialization scenarios
  - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

- [x] 11. Add error handling and validation
  - Create UnitSystemError sealed class hierarchy
  - Implement UnitSystemErrorHandler for specific error scenarios
  - Add validation for unit conversion inputs and edge cases
  - Implement graceful handling of network failures during sync
  - Add error recovery mechanisms for failed preference operations
  - Write tests for error scenarios and recovery mechanisms
  - _Requirements: 8.5, 2.5_

- [x] 12. Implement offline sync and connectivity handling
  - Add network connectivity monitoring for preference sync
  - Implement background sync service for pending preference changes
  - Add conflict resolution strategy for preference synchronization
  - Create retry logic for failed sync operations
  - Implement graceful degradation when offline
  - Write tests for offline scenarios and sync recovery
  - _Requirements: 2.5, 7.3_

- [x] 13. Create comprehensive test suite
  - Write unit tests for UnitConverter with all conversion scenarios
  - Write unit tests for UnitSystemManager with state management
  - Write unit tests for PreferencesRepository with sync scenarios
  - Write integration tests for end-to-end preference flow
  - Write UI tests for settings interaction and measurement display
  - Create test data factories and mock services for testing
  - Add performance tests for conversion operations
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14. Integrate with existing app screens
  - Update daily logging screens to use new measurement components
  - Update cycle tracking screens with unit-aware displays
  - Update BBT charting to respect temperature unit preferences
  - Update health reports to include unit system information
  - Ensure all existing measurement displays use new components
  - Write integration tests for updated screens
  - _Requirements: 3.4, 3.5, 7.1, 7.2_

- [x] 15. Add platform-specific implementations
  - Implement Android-specific locale detection and storage
  - Implement iOS-specific locale detection and storage
  - Add platform-specific preference storage optimizations
  - Ensure consistent behavior across Android and iOS platforms
  - Test cross-platform data synchronization
  - Write platform-specific tests for native integrations
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 16. Performance optimization and caching
  - Implement in-memory caching for frequently accessed preferences
  - Add conversion result caching for performance optimization
  - Optimize database queries with proper indexing
  - Implement lazy loading for unit system components
  - Add batch operations for multiple measurement conversions
  - Write performance tests and benchmarks
  - _Requirements: 3.4, 7.4_

- [x] 17. Final integration and testing
  - Integrate all unit system components with existing architecture
  - Perform end-to-end testing of complete preference workflow
  - Test preference persistence across app restarts and device changes
  - Validate cross-platform consistency and synchronization
  - Conduct user acceptance testing for settings and measurement display
  - Perform security and privacy compliance validation
  - _Requirements: All requirements final validation_š