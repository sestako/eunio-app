# Rollback Plan: Kotlin 2.2.20 + Xcode 26 + iOS 26 SDK Upgrade

## Overview

This document provides a detailed rollback procedure in case critical issues are discovered during or after the upgrade to Kotlin 2.2.20, Xcode 26, and iOS 26 SDK.

**Estimated Rollback Time**: 30-60 minutes

## Pre-Rollback Checklist

Before initiating rollback, verify:
- [ ] Critical issue has been identified and documented
- [ ] Issue cannot be quickly resolved with a hotfix
- [ ] Team has been notified of rollback decision
- [ ] Backup branch exists and is accessible

## Rollback Procedures

### Option 1: Git Branch Revert (Recommended)

This is the fastest and safest rollback method if the upgrade was done on a separate branch.

#### Steps:

1. **Switch to main branch**
   ```bash
   git checkout main
   ```

2. **Verify current state**
   ```bash
   # Check Kotlin version
   grep 'kotlin = ' gradle/libs.versions.toml
   
   # Should show: kotlin = "1.9.21"
   ```

3. **Clean build artifacts**
   ```bash
   ./gradlew clean
   rm -rf build
   rm -rf .gradle
   rm -rf androidApp/build
   rm -rf shared/build
   ```

4. **Rebuild project**
   ```bash
   ./gradlew build
   ```

5. **Verify rollback**
   - Run tests: `./gradlew test`
   - Build Android app: `./gradlew :androidApp:assembleDebug`
   - Test on device/emulator

**Time Required**: 15-30 minutes

---

### Option 2: Git Commit Revert

If changes were merged to main, revert specific commits.

#### Steps:

1. **Identify upgrade commits**
   ```bash
   git log --oneline --grep="upgrade" --grep="kotlin" --grep="xcode" -i
   ```

2. **Revert commits (newest first)**
   ```bash
   # Revert each commit in reverse order
   git revert <commit-hash-1>
   git revert <commit-hash-2>
   git revert <commit-hash-3>
   ```

3. **Resolve any conflicts**
   ```bash
   # If conflicts occur, resolve manually
   git status
   # Edit conflicting files
   git add .
   git revert --continue
   ```

4. **Clean and rebuild**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

5. **Push reverted changes**
   ```bash
   git push origin main
   ```

**Time Required**: 30-45 minutes

---

### Option 3: Manual File Restoration

If git history is unavailable or corrupted.

#### Steps:

1. **Restore gradle/libs.versions.toml**
   ```toml
   [versions]
   kotlin = "1.9.21"
   kotlinx-coroutines = "1.7.3"
   kotlinx-serialization = "1.6.2"
   kotlinx-datetime = "0.5.0"
   compose-plugin = "1.5.11"
   ktor = "2.3.7"
   koin = "3.5.3"
   sqlDelight = "2.0.1"
   agp = "8.2.2"
   ```

2. **Restore gradle.properties**
   - Remove any Kotlin 2.2-specific properties
   - Remove `dependencyVerificationMode=strict` if added

3. **Restore gradle-wrapper.properties**
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
   ```

4. **Restore shared/build.gradle.kts**
   - Revert iOS framework configuration changes
   - Restore Bitcode settings if changed
   - Revert compiler args

5. **Clean and rebuild**
   ```bash
   ./gradlew clean
   rm -rf .gradle
   ./gradlew build
   ```

**Time Required**: 45-60 minutes

---

## iOS/Xcode Rollback

### If Xcode 26 was installed:

1. **Keep Xcode 26 installed** (recommended)
   - Xcode 26 can still build for older iOS versions
   - No need to uninstall unless disk space is critical

2. **If multiple Xcode versions exist**
   ```bash
   # List installed Xcode versions
   ls /Applications/ | grep Xcode
   
   # Switch to older version if needed
   sudo xcode-select -s /Applications/Xcode_15.4.app
   
   # Verify
   xcodebuild -version
   ```

3. **Revert iOS project settings**
   - Open `iosApp/iosApp.xcodeproj` in Xcode
   - Verify iOS Deployment Target is still 15.0
   - No other changes should be needed

4. **Revert CocoaPods if updated**
   ```bash
   cd iosApp
   # Restore Podfile from git
   git checkout main -- Podfile
   pod install
   ```

**Time Required**: 10-15 minutes

---

## Dependency Rollback

### Ktor (if upgraded to 3.x)

1. **Revert version in gradle/libs.versions.toml**
   ```toml
   ktor = "2.3.7"
   ```

2. **Revert any code changes**
   - Restore Ktor client configuration
   - Restore content negotiation setup

### Koin (if upgraded to 4.x)

1. **Revert version in gradle/libs.versions.toml**
   ```toml
   koin = "3.5.3"
   ```

2. **Revert any code changes**
   - Restore Koin module definitions
   - Restore dependency injection setup

---

## Verification Steps

After rollback, verify the following:

### 1. Build Verification
```bash
# Clean build
./gradlew clean

# Build shared module
./gradlew :shared:build

# Build Android app
./gradlew :androidApp:assembleDebug
```

### 2. Test Verification
```bash
# Run all tests
./gradlew test

# Run Android tests
./gradlew :androidApp:testDebugUnitTest
```

### 3. Functional Verification
- [ ] Android app launches successfully
- [ ] iOS app launches successfully (if Xcode available)
- [ ] Firebase authentication works
- [ ] Data sync works
- [ ] All major features functional

### 4. Version Verification
```bash
# Verify Kotlin version
grep 'kotlin = ' gradle/libs.versions.toml

# Verify Gradle version
./gradlew --version

# Verify Xcode version (if applicable)
xcodebuild -version
```

---

## CI/CD Rollback

If CI/CD was updated:

1. **Revert GitHub Actions workflow**
   ```bash
   git checkout main -- .github/workflows/
   ```

2. **Verify CI builds**
   - Push a test commit
   - Monitor CI build status
   - Verify both Android and iOS builds succeed

---

## Post-Rollback Actions

1. **Document the issue**
   - Create detailed issue report
   - Include error messages, logs, screenshots
   - Document steps to reproduce

2. **Notify team**
   - Inform team of rollback completion
   - Share issue documentation
   - Discuss resolution strategy

3. **Plan next steps**
   - Analyze root cause
   - Determine if upgrade should be reattempted
   - Create action plan for resolution

4. **Update documentation**
   - Document what went wrong
   - Update rollback plan with lessons learned
   - Add troubleshooting steps for future attempts

---

## Emergency Contacts

- **Team Lead**: [Name/Contact]
- **DevOps**: [Name/Contact]
- **On-Call Engineer**: [Name/Contact]

---

## Rollback Decision Criteria

Initiate rollback if:
- ✅ Critical functionality is broken
- ✅ App crashes on launch
- ✅ Data loss or corruption occurs
- ✅ Firebase integration fails completely
- ✅ Build process fails consistently
- ✅ Performance degrades significantly (>50%)
- ✅ Security vulnerability introduced

Do NOT rollback if:
- ❌ Minor UI glitches (can be fixed with hotfix)
- ❌ Non-critical warnings
- ❌ Single test failure (can be fixed quickly)
- ❌ Documentation issues

---

## Rollback Success Criteria

Rollback is successful when:
- [ ] All builds complete successfully
- [ ] All tests pass
- [ ] App launches on both platforms
- [ ] Firebase integration works
- [ ] No critical errors in logs
- [ ] Team can continue development
- [ ] CI/CD pipelines are green

---

## Lessons Learned Template

After rollback, document:

### What Went Wrong
[Detailed description of the issue]

### Root Cause
[Analysis of why the issue occurred]

### What We Learned
[Key takeaways and insights]

### Prevention Measures
[Steps to prevent similar issues in future]

### Next Steps
[Plan for addressing the issue and reattempting upgrade]

---

## Version History

- **v1.0** - Initial rollback plan created
- **Date**: [Current date]
- **Author**: [Your name]

---

## Notes

- This rollback plan assumes the upgrade was done on a separate branch (`upgrade/kotlin-2.2-ios26`)
- Always test rollback procedure in a safe environment before production
- Keep this document updated as the project evolves
- Rollback should be a last resort - attempt fixes first when possible
