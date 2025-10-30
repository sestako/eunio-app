# Implementation Plan

- [x] 1. Preparation and backup
  - Create backup branch `upgrade/kotlin-2.2-ios26`
  - Document current versions in versions.md
  - Review Kotlin 2.2.20 release notes and migration guide
  - Review iOS 26 SDK release notes
  - Check dependency compatibility matrix
  - Create rollback plan document
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 9.1, 9.2_

- [x] 2. Update Kotlin version and compiler plugins
  - Update kotlin version to "2.2.20" in gradle/libs.versions.toml
  - Update Kotlin Serialization plugin to "2.2.20"
  - Update KSP to "2.2.20-1.0.28" (compatible with Kotlin 2.2.20)
  - Update Android Gradle Plugin to "8.7.3"
  - Update Gradle wrapper to 8.10
  - Update kotlinx-coroutines to "1.9.0"
  - Update kotlinx-serialization to "1.7.3"
  - Update kotlinx-datetime to "0.6.1"
  - Sync Gradle files and resolve any immediate conflicts
  - _Requirements: 1.1, 1.2, 1.3, 1.7, 2.1, 2.2, 2.3_

- [x] 3. Update Compose Multiplatform and UI dependencies
  - Update compose-plugin to "1.7.1"
  - Verify Compose compiler plugin is compatible with Kotlin 2.2.20
  - Update Compose UI libraries to compatible versions
  - Update Material3 to latest compatible version
  - Sync and verify no conflicts
  - _Requirements: 2.4, 5.8_

- [x] 4. Update Ktor client to version 3.x
  - Update ktor version to "3.0.1"
  - Review Ktor 3.0 migration guide
  - Update Ktor client configuration if needed
  - Update content negotiation setup if needed
  - Fix any breaking API changes
  - _Requirements: 2.5_

- [x] 5. Update Koin to version 4.x
  - Update koin version to "4.0.0" ✅
  - Review Koin 4.0 migration guide ✅
  - Update Koin module definitions if needed ✅ (No changes required)
  - Update dependency injection setup if needed ✅ (No changes required)
  - Fix any breaking API changes ✅ (Fixed NoBeanDefFoundException → NoDefinitionFoundException)
  - _Requirements: 2.6_
  - **Summary**: Koin 4.0 upgrade complete. All Koin functionality working correctly. See task-5-koin-4-upgrade-summary.md for details.

- [x] 6. Update SQLDelight and other dependencies
  - Update sqlDelight to "2.0.2"
  - Update Firebase BOM to latest version
  - Update any other dependencies to Kotlin 2.2 compatible versions
  - Verify all dependencies resolve without conflicts
  - _Requirements: 2.7, 2.8_

- [x] 7. Update Gradle build configuration and enable strict dependency verification
  - Update gradle.properties with Kotlin 2.2 compiler options
  - Add kotlin.mpp.stability.nowarn=true
  - Add kotlin.mpp.androidSourceSetLayoutVersion=2
  - Add dependencyVerificationMode=strict to prevent version mismatches
  - Update shared/build.gradle.kts with iOS 26 compatibility settings
  - Disable Bitcode in iOS framework (bitcode = Framework.BitcodeEmbeddingMode.DISABLE)
  - Configure support for arm64 and x86_64 simulator architectures
  - Update compiler args for iOS targets
  - _Requirements: 2.9, 2.10, 3.8, 3.9, 5.1, 5.2, 5.3, 5.4, 5.7_

- [x] 8. Fix Kotlin compilation errors
  - Run ./gradlew clean
  - Run ./gradlew :shared:build
  - Fix any compilation errors in shared module
  - Fix deprecated API usage
  - Update code for Kotlin 2.2 changes
  - Verify shared module builds successfully
  - _Requirements: 1.2, 1.5, 5.5_

- [x] 9. Fix Android compilation and build
  - Run ./gradlew :androidApp:assembleDebug
  - Fix any Android-specific compilation errors
  - Update Android-specific code for new dependencies
  - Verify Android app builds successfully
  - _Requirements: 1.3, 5.6_

- [x] 10. Run Android tests
  - Run ./gradlew :androidApp:testDebugUnitTest
  - Fix any failing unit tests
  - Update test code for new dependency versions
  - Verify all Android tests pass
  - _Requirements: 7.1, 7.2_

- [x] 11. Install and configure Xcode 26
  - Download Xcode 26 from Mac App Store
  - Install Xcode 26 to /Applications/Xcode.app
  - Run sudo xcode-select -s /Applications/Xcode.app
  - Verify installation: xcodebuild -version (should show Xcode 26.x)
  - Accept Xcode license if prompted
  - Install additional components if prompted
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 12. Update iOS project settings
  - Open iosApp/iosApp.xcodeproj in Xcode 26
  - Update project settings if Xcode prompts
  - Verify iOS Deployment Target is set to 15.0
  - Verify build settings are correct
  - Update signing certificates if needed
  - _Requirements: 3.4, 4.1, 4.2_

- [x] 13. Migrate to Swift Package Manager and update dependencies
  - Evaluate migrating Firebase from CocoaPods to Swift Package Manager (SPM)
  - If using SPM: Add Firebase packages via Xcode (File > Add Packages)
  - Use latest FirebaseCore and FirebaseFirestore compatible with iOS 26
  - If keeping CocoaPods: Update Podfile platform to ios '15.0'
  - If keeping CocoaPods: Run pod update to get latest compatible versions
  - Verify Firebase integration works with iOS 26 SDK
  - Fix any dependency installation issues
  - _Requirements: 6.7, 6.8, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 14. Build iOS framework with Kotlin 2.2
  - Run ./gradlew :shared:linkDebugFrameworkIosArm64
  - Run ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
  - Verify Kotlin/Native compilation succeeds
  - Verify framework is compatible with Xcode 26
  - _Requirements: 1.5, 1.6, 5.4, 5.5_

- [x] 14.5. Implement typed Swift framework + cinterop bridge (EunioBridgeKit)
  - Create Swift framework EunioBridgeKit with @objc protocol for Firebase operations
  - Define protocol with typed methods (saveDocument, getDocument, etc.) using primitives, NSDictionary/NSArray, and completion blocks
  - Configure framework build settings (Build Libraries for Distribution: YES, Defines Module: YES)
  - Build XCFramework for arm64 and x86_64 simulator architectures
  - Create cinterop definition file (shared/src/iosMain/cinterop/EunioBridgeKit.def)
  - Configure Kotlin/Native cinterop in shared/build.gradle.kts with compilerOpts and linkerOpts
  - Refactor FirebaseNativeBridge.ios.kt to use typed cinterop bindings (remove all asDynamic, performSelector, unsafeCast)
  - Implement concrete Swift class in iosApp that conforms to protocol and uses Firebase SDK
  - Wire up bridge initialization in iOS app startup (inject instance into shared module)
  - Remove all legacy dynamic/runtime hacks and temporary stubs
  - Verify Firebase save/load operations work end-to-end
  - Confirm Android ↔ iOS data parity
  - _Requirements: 1.6, 3.8, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.7_

- [x] 15. Fix iOS compilation errors
  - Build iOS app in Xcode 26 (Cmd+B)
  - Fix any Swift compilation errors
  - Fix any Objective-C bridging errors
  - Update iOS code for iOS 26 SDK changes
  - Fix deprecated API warnings
  - Verify iOS app builds successfully
  - _Requirements: 3.5, 4.5_

- [x] 16. Run iOS tests
  - Run iOS unit tests in Xcode (Cmd+U)
  - Fix any failing tests
  - Update test code for iOS 26 changes
  - Verify all iOS tests pass
  - _Requirements: 7.1, 7.2_

- [x] 17. Test iOS app on simulator
  - Run app on iOS 26 simulator
  - Test app launch and basic navigation
  - Verify UI renders correctly
  - Test all major features
  - Check for crashes or errors
  - _Requirements: 3.6, 4.4, 7.7_

- [ ] 18. Test iOS app on physical device
  - Run app on physical iOS device (iOS 15-26)
  - Test app launch and performance
  - Verify all features work on device
  - Test with different iOS versions if possible
  - _Requirements: 3.7, 4.4, 7.7_

- [x] 19. Test Firebase authentication on both platforms
  - Test sign-up flow on Android
  - Test sign-up flow on iOS
  - Test sign-in flow on Android
  - Test sign-in flow on iOS
  - Test sign-out on both platforms
  - Verify Firebase Auth works correctly
  - _Requirements: 6.1, 6.2, 6.3, 7.5_

- [x] 20. Test Firestore data operations on both platforms
  - Test saving daily log on Android
  - Test saving daily log on iOS
  - Test loading daily log on Android
  - Test loading daily log on iOS
  - Verify data format is correct
  - _Requirements: 6.4, 6.5, 7.3_

- [x] 21. Test cross-platform data sync
  - Save data on Android, verify it appears on iOS
  - Save data on iOS, verify it appears on Android
  - Test data updates sync correctly
  - Test conflict resolution still works
  - Verify sync timestamps are correct
  - _Requirements: 7.4_

- [x] 22. Test offline mode and local persistence
  - Test saving data offline on Android
  - Test saving data offline on iOS
  - Test data persists after app restart
  - Test sync after coming back online
  - Verify offline mode works correctly
  - _Requirements: 6.6, 7.6_

- [x] 23. Test all major app features
  - Test daily logging screen on both platforms
  - Test calendar view on both platforms
  - Test settings screen on both platforms
  - Test insights screen on both platforms
  - Test all navigation flows
  - Verify no regressions in functionality
  - _Requirements: 7.3, 7.4, 7.5, 7.6, 7.7_

- [x] 24. Performance validation
  - Measure app startup time on both platforms
  - Measure build time (Gradle and Xcode)
  - Measure Firebase sync latency
  - Measure memory usage
  - Compare with pre-upgrade metrics
  - Document any performance changes
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 25. Create toolchain verification script
  - Create scripts/verify-toolchain.sh
  - Add Kotlin version check (must be 2.2.20)
  - Add Gradle version check (must be 8.10+)
  - Add Xcode version check (must be 26.x, macOS only)
  - Add colored output for success/failure messages
  - Make script executable (chmod +x)
  - Test script locally
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [x] 26. Update CI/CD pipelines
  - Update GitHub Actions to use macOS 26 (Tahoe) runners
  - Update GitHub Actions to use Xcode 26
  - Update GitHub Actions to use Kotlin 2.2.20
  - Add toolchain verification script as pre-check step
  - Configure Gradle caching in CI
  - Configure CocoaPods caching in CI
  - Test CI builds for Android
  - Test CI builds for iOS
  - Verify all CI tests pass
  - Verify toolchain verification fails build if versions incorrect
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 13.6, 13.7_

- [ ] 27. Update documentation
  - Update README with new version requirements (Kotlin 2.2.20, Xcode 26, macOS 26)
  - Document all version changes in versions.md
  - Document Kotlin compiler plugin updates
  - Document Bitcode deprecation and removal
  - Document Swift Package Manager preference for Firebase
  - Document any breaking changes encountered
  - Document any API changes made
  - Create troubleshooting guide for common issues
  - Update setup instructions for new developers
  - Document toolchain verification script usage
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [ ] 28. Create and test rollback plan
  - Document rollback steps in detail
  - Document how to revert Kotlin and all compiler plugins
  - Document how to revert Xcode version
  - Document how to revert dependency verification mode
  - Test rollback procedure on separate branch
  - Verify rollback restores working state
  - Document time required for rollback
  - Ensure team knows rollback procedure
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 29. Final validation and sign-off
  - Run toolchain verification script
  - Run complete test suite on both platforms
  - Verify all requirements are met (including new Req 13)
  - Test on multiple devices and iOS versions
  - Verify Bitcode is disabled in iOS framework
  - Verify strict dependency verification is enabled
  - Verify CI uses macOS 26 (Tahoe) with caching
  - Get team approval for upgrade
  - Merge upgrade branch to main
  - Tag release with new version numbers
  - _Requirements: All requirements final validation_
