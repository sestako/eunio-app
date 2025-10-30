# CI/CD Workflows

This directory contains GitHub Actions workflows for automated building, testing, and validation of the Eunio Health App.

## Workflows

### 1. Build and Test (`build-and-test.yml`)

**Purpose:** Main CI/CD pipeline for building and testing the app on both Android and iOS platforms.

**Triggers:**
- Push to `main`, `develop`, or `upgrade/**` branches
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**Jobs:**
1. **Verify Toolchain** - Validates that all required toolchain versions are correct
   - Kotlin 2.2.20
   - Gradle 8.10+
   - Xcode 26.x (macOS only)

2. **Android Build and Test** - Builds Android app and runs tests
   - Builds shared module
   - Builds Android APK
   - Runs unit tests
   - Uploads build artifacts and test results

3. **iOS Build and Test** - Builds iOS app and runs tests
   - Builds iOS frameworks (arm64 and simulator arm64)
   - Installs CocoaPods dependencies
   - Builds iOS app
   - Runs iOS tests
   - Uploads test results

4. **Shared Module Tests** - Runs tests for the shared Kotlin Multiplatform module
   - Runs all shared module unit tests
   - Uploads test results

5. **Test Summary** - Generates comprehensive test summary
   - Collects all test results
   - Creates summary report
   - Fails if any job failed

**Caching:**
- Gradle dependencies and wrapper
- Kotlin compilation cache
- CocoaPods dependencies (iOS only)

**Environment Variables:**
- `JAVA_VERSION`: 17
- `KOTLIN_VERSION`: 2.2.20
- `XCODE_VERSION`: 26.0

### 2. Comprehensive Cross-Platform Testing (`comprehensive-testing.yml`)

**Purpose:** Extended testing workflow for cross-platform consistency validation.

**Triggers:**
- Push to `main` or `develop`
- Pull requests to `main` or `develop`
- Manual workflow dispatch with test type selection

**Jobs:**
1. **Verify Toolchain** - Validates toolchain versions
2. **Shared Module Tests** - Tests shared code
3. **Android Tests** - Tests Android-specific code
4. **Cross-Platform Consistency Validation** - Validates consistency across platforms
5. **Test Suite Health Check** - Overall health check and reporting

**Test Types (Manual Dispatch):**
- `all` - Run all tests
- `unit` - Run unit tests only
- `integration` - Run integration tests only
- `cross-platform` - Run cross-platform validation only

### 3. Toolchain Verification Test (`toolchain-verification-test.yml`)

**Purpose:** Tests that the toolchain verification script correctly detects version mismatches.

**Triggers:**
- Manual workflow dispatch only

**Test Scenarios:**
1. **Correct Versions** - Verifies that the script passes with correct versions
2. **Wrong Kotlin Version** - Simulates incorrect Kotlin version and verifies script fails
3. **Wrong Gradle Version** - Simulates incorrect Gradle version and verifies script fails

**Usage:**
```bash
# Run from GitHub Actions UI
# Go to Actions > Toolchain Verification Test > Run workflow
# Select test scenario from dropdown
```

## Caching Strategy

All workflows implement aggressive caching to reduce build times:

### Gradle Cache
```yaml
- name: Setup Gradle Cache
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
      ~/.gradle/kotlin
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties', 'gradle/libs.versions.toml') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

**Cache Key Includes:**
- Operating system
- All Gradle build files
- Gradle wrapper properties
- Version catalog (libs.versions.toml)

**Cached Directories:**
- `~/.gradle/caches` - Gradle dependency cache
- `~/.gradle/wrapper` - Gradle wrapper distributions
- `~/.gradle/kotlin` - Kotlin compilation cache

### CocoaPods Cache (iOS only)
```yaml
- name: Setup CocoaPods Cache
  uses: actions/cache@v4
  with:
    path: |
      iosApp/Pods
      ~/Library/Caches/CocoaPods
    key: ${{ runner.os }}-pods-${{ hashFiles('**/Podfile.lock') }}
    restore-keys: |
      ${{ runner.os }}-pods-
```

**Cache Key Includes:**
- Operating system
- Podfile.lock hash

**Cached Directories:**
- `iosApp/Pods` - Installed CocoaPods
- `~/Library/Caches/CocoaPods` - CocoaPods download cache

## Toolchain Verification

All workflows run the toolchain verification script (`scripts/verify-toolchain.sh`) as a pre-check step before building or testing.

**Verified Versions:**
- Kotlin: 2.2.20
- Gradle: 8.10+
- Xcode: 26.x (macOS runners only)

**Behavior:**
- ✅ If all versions are correct, the workflow continues
- ❌ If any version is incorrect, the workflow fails immediately with exit code 1

**Example Output:**
```
🔍 Verifying Toolchain Versions...

Checking Kotlin version...
✓ Kotlin: 2.2.20
Checking Gradle version...
✓ Gradle: 8.10
Checking Xcode version...
✓ Xcode: 26.0

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ All toolchain versions are correct!
```

## Runner Requirements

### Ubuntu Runners (Android/Shared)
- **OS:** ubuntu-latest
- **Java:** 17 (Temurin distribution)
- **Gradle:** 8.10+ (via wrapper)

### macOS Runners (iOS)
- **OS:** macos-14 (latest available)
- **Xcode:** Latest available on runner (should be 26.x when available)
- **Java:** 17 (Temurin distribution)
- **Gradle:** 8.10+ (via wrapper)
- **CocoaPods:** Pre-installed on macOS runners

**Note:** As of the time of writing, GitHub Actions may not yet have macOS 26 (Tahoe) runners available. The workflows use `macos-14` which will be updated to `macos-26` when available. The workflows are designed to work with the latest available Xcode on the runner.

## Artifacts

All workflows upload artifacts for debugging and analysis:

### Build Artifacts
- **android-apk** - Android debug APK (7 days retention)

### Test Results
- **android-test-results** - Android test reports and results (7 days retention)
- **ios-test-results** - iOS test reports and results (7 days retention)
- **shared-test-results** - Shared module test reports (7 days retention)
- **cross-platform-validation-results** - Cross-platform validation reports (7 days retention)

### Summaries
- **test-summary** - Comprehensive test summary (30 days retention)
- **comprehensive-test-report** - Full test report with all results (30 days retention)

## Troubleshooting

### Toolchain Verification Fails

**Problem:** Workflow fails at toolchain verification step.

**Solution:**
1. Check the error message to see which version is incorrect
2. Update the version in the appropriate file:
   - Kotlin: `gradle/libs.versions.toml`
   - Gradle: `gradle/wrapper/gradle-wrapper.properties`
   - Xcode: Update Xcode installation on macOS

### Cache Issues

**Problem:** Builds are slow or dependencies are re-downloaded every time.

**Solution:**
1. Check that cache keys are correct in workflow files
2. Verify that `gradle/libs.versions.toml` is committed
3. Clear cache manually from GitHub Actions UI if corrupted

### iOS Build Fails

**Problem:** iOS build fails with Xcode errors.

**Solution:**
1. Check Xcode version on runner matches requirements
2. Verify CocoaPods dependencies are up to date
3. Check that iOS framework builds successfully in Gradle step
4. Review iOS test results artifact for detailed errors

### Android Build Fails

**Problem:** Android build fails with Gradle errors.

**Solution:**
1. Check Kotlin version matches 2.2.20
2. Verify Gradle version is 8.10+
3. Check for dependency conflicts in build files
4. Review Android test results artifact for detailed errors

## Local Testing

You can test the workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash  # Linux

# Run Android build locally
act -j android-build

# Run toolchain verification
act -j verify-toolchain

# Run all jobs
act
```

**Note:** iOS builds cannot be run locally with act as they require macOS runners.

## Maintenance

### Updating Kotlin Version

When updating Kotlin version:
1. Update `gradle/libs.versions.toml`
2. Update `KOTLIN_VERSION` in workflow files
3. Update `EXPECTED_KOTLIN` in `scripts/verify-toolchain.sh`
4. Test locally before pushing

### Updating Gradle Version

When updating Gradle version:
1. Update `gradle/wrapper/gradle-wrapper.properties`
2. Update `EXPECTED_GRADLE_MAJOR` and `EXPECTED_GRADLE_MINOR` in `scripts/verify-toolchain.sh`
3. Test locally before pushing

### Updating Xcode Version

When updating Xcode version:
1. Wait for GitHub to release new macOS runners with updated Xcode
2. Update `XCODE_VERSION` in workflow files
3. Update `EXPECTED_XCODE_MAJOR` in `scripts/verify-toolchain.sh`
4. Update runner OS version if needed (e.g., `macos-14` to `macos-26`)
5. Test on GitHub Actions

## Best Practices

1. **Always run toolchain verification first** - Catches version issues early
2. **Use caching aggressively** - Reduces build times significantly
3. **Upload artifacts for debugging** - Makes troubleshooting easier
4. **Set appropriate timeouts** - Prevents stuck jobs from consuming resources
5. **Use `continue-on-error` sparingly** - Only for non-critical steps
6. **Keep workflows DRY** - Use reusable workflows for common tasks
7. **Document changes** - Update this README when modifying workflows

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Xcode Build Settings](https://developer.apple.com/documentation/xcode/build-settings-reference)
