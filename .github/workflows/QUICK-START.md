# CI/CD Quick Start Guide

## Overview

This project uses GitHub Actions for automated building and testing. All workflows include toolchain verification to ensure consistent development environments.

## Workflows at a Glance

| Workflow | Purpose | When It Runs |
|----------|---------|--------------|
| **Build and Test** | Main CI/CD pipeline | Push to main/develop, PRs |
| **Comprehensive Testing** | Extended cross-platform tests | Push to main/develop, PRs |
| **Toolchain Verification Test** | Tests verification script | Manual only |

## Quick Actions

### View Workflow Runs
1. Go to your repository on GitHub
2. Click the **Actions** tab
3. Select a workflow from the left sidebar
4. Click on a specific run to see details

### Manually Trigger a Workflow
1. Go to **Actions** tab
2. Select workflow from left sidebar
3. Click **Run workflow** button
4. Select branch and options (if any)
5. Click **Run workflow**

### Download Build Artifacts
1. Go to a completed workflow run
2. Scroll to **Artifacts** section at bottom
3. Click artifact name to download

## What Gets Tested

### Every Push/PR:
- ✅ Toolchain versions (Kotlin 2.2.20, Gradle 8.10+, Xcode 26)
- ✅ Android app builds
- ✅ iOS app builds
- ✅ Shared module tests
- ✅ Android unit tests
- ✅ iOS unit tests
- ✅ Cross-platform consistency

### Build Artifacts:
- Android APK (debug)
- Test results and reports
- Test summaries

## Understanding Results

### ✅ Green Check = Success
All tests passed, build succeeded.

### ❌ Red X = Failure
Something failed. Click the run to see which job failed.

### 🟡 Yellow Dot = In Progress
Workflow is currently running.

### Common Failure Reasons:

1. **Toolchain Verification Failed**
   - Wrong Kotlin version in `gradle/libs.versions.toml`
   - Wrong Gradle version in `gradle/wrapper/gradle-wrapper.properties`
   - Solution: Update to required versions

2. **Build Failed**
   - Compilation errors in code
   - Missing dependencies
   - Solution: Fix code errors, check build locally first

3. **Tests Failed**
   - Unit test failures
   - Solution: Run tests locally, fix failing tests

4. **Timeout**
   - Job took too long
   - Solution: Check for infinite loops or performance issues

## Caching

Workflows use caching to speed up builds:

- **Gradle cache**: Dependencies, wrapper, Kotlin compilation
- **CocoaPods cache**: iOS dependencies

**First run:** Slower (builds cache)  
**Subsequent runs:** Faster (uses cache)

## Required Versions

| Tool | Version |
|------|---------|
| Kotlin | 2.2.20 |
| Gradle | 8.10+ |
| Xcode | 26.x |
| Java | 17 |

## Troubleshooting

### "Toolchain verification failed"
Check error message for which version is wrong, then update:
- Kotlin: `gradle/libs.versions.toml`
- Gradle: `gradle/wrapper/gradle-wrapper.properties`

### "Build failed" on iOS
- Check Xcode version on runner
- Verify CocoaPods dependencies are correct
- Check iOS framework builds in Gradle step

### "Build failed" on Android
- Check Kotlin version matches 2.2.20
- Verify Gradle version is 8.10+
- Check for dependency conflicts

### Cache not working
- Check workflow logs for cache hit/miss
- Verify cache keys in workflow files
- Clear cache from GitHub Actions UI if corrupted

## Testing Locally

Before pushing, test locally:

```bash
# Verify toolchain
./scripts/verify-toolchain.sh

# Build Android
./gradlew :androidApp:assembleDebug

# Run Android tests
./gradlew :androidApp:testDebugUnitTest

# Build iOS frameworks
./gradlew :shared:linkDebugFrameworkIosArm64

# Run shared tests
./gradlew :shared:testDebugUnitTest
```

## Getting Help

1. Check workflow logs in GitHub Actions
2. Review [README.md](.github/workflows/README.md) for detailed documentation
3. Check [task completion summary](../.kiro/specs/4-kotlin-xcode-ios26-upgrade/task-26-completion-summary.md)
4. Ask team for help with specific errors

## Best Practices

✅ **DO:**
- Run toolchain verification before pushing
- Test builds locally first
- Keep dependencies up to date
- Review workflow logs when failures occur

❌ **DON'T:**
- Push without testing locally
- Ignore toolchain version warnings
- Skip reviewing test failures
- Commit with known failing tests

## Status Badges

Add to your README.md:

```markdown
![Build and Test](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Build%20and%20Test/badge.svg)
![Comprehensive Testing](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Comprehensive%20Cross-Platform%20Testing/badge.svg)
```

Replace `YOUR_USERNAME` and `YOUR_REPO` with your actual values.

## Next Steps

1. Push code to trigger workflows
2. Monitor first run in Actions tab
3. Verify all jobs complete successfully
4. Check that artifacts are uploaded
5. Review test results

For detailed information, see [README.md](README.md).
