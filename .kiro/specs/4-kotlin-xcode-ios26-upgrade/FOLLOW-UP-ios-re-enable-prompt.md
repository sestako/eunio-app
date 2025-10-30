# Follow-Up Prompt: Re-enable iOS Targets

Use this prompt when you're ready to re-enable iOS builds in CI.

---

## Prompt for Kiro

```
Re-enable iOS targets in the Eunio Kotlin Multiplatform project after packaging EunioBridgeKit framework.

Context:
- iOS targets were temporarily disabled in shared/build.gradle.kts to unblock CI
- The EunioBridgeKit.xcframework is now available in the repository at:
  shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework
- The iOS code is production-ready and uses proper cinterop bindings
- No code changes needed, just uncomment the iOS target configuration

Tasks:
1. Open shared/build.gradle.kts
2. Uncomment the iOS target declarations:
   - Remove /* */ around the listOf(iosX64(), iosArm64(), iosSimulatorArm64()) block
   - Remove the TODO comment about re-enabling
3. Uncomment the iosMain.dependencies block
4. Verify the build file syntax is correct
5. Test locally:
   - Run: ./gradlew :shared:linkDebugFrameworkIosArm64
   - Run: ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
6. Commit with message: "chore: re-enable iOS targets after framework packaging"
7. Push and verify CI passes

Expected outcome:
- iOS frameworks build successfully
- CI builds both Android and iOS
- All tests pass
```

---

## Alternative: If Framework Needs to be Added First

```
Package the EunioBridgeKit framework for CI and re-enable iOS targets.

Context:
- iOS targets are disabled because EunioBridgeKit.xcframework is not in the repository
- The framework exists locally at: shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework
- Need to add it to git and then re-enable iOS targets

Tasks:
1. Check if framework exists locally:
   - Run: ls -la shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework
   
2. If framework exists, add it to git:
   - Check .gitignore doesn't exclude it
   - Run: git add shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework
   - Run: git status (verify framework is staged)
   
3. Re-enable iOS targets in shared/build.gradle.kts:
   - Uncomment the iOS target configuration
   - Remove TODO comments
   
4. Commit both changes:
   - Message: "chore: add EunioBridgeKit framework and re-enable iOS targets"
   
5. Push and verify CI passes

If framework doesn't exist locally:
- Document that framework needs to be built first
- Provide instructions for building EunioBridgeKit
- Keep iOS targets disabled until framework is available
```

---

## Alternative: Build Framework in CI

```
Configure CI to build EunioBridgeKit framework before compiling iOS targets.

Context:
- iOS targets are disabled because framework is not available in CI
- Want to build framework as part of CI instead of packaging it
- This keeps repository lean and framework always up to date

Tasks:
1. Create a script to build EunioBridgeKit framework:
   - Location: scripts/build-euniobridgekit.sh
   - Should build xcframework for both device and simulator
   - Output to: shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework

2. Update .github/workflows/build-and-test.yml:
   - Add step before iOS build to run the framework build script
   - Only on macOS runners
   - Cache the built framework

3. Re-enable iOS targets in shared/build.gradle.kts

4. Test the full CI workflow

5. Commit with message: "feat: build EunioBridgeKit in CI and re-enable iOS targets"

Expected outcome:
- CI builds framework automatically
- iOS targets compile successfully
- Framework is cached for faster subsequent builds
```

---

## Verification Checklist

After re-enabling iOS targets, verify:

### Local Build
- [ ] `./gradlew clean` succeeds
- [ ] `./gradlew :shared:linkDebugFrameworkIosArm64` succeeds
- [ ] `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` succeeds
- [ ] `./gradlew :shared:linkDebugFrameworkIosX64` succeeds (if needed)
- [ ] iOS app builds in Xcode
- [ ] iOS app runs in simulator

### CI Build
- [ ] CI workflow triggers
- [ ] Toolchain verification passes
- [ ] Android build passes
- [ ] iOS build passes
- [ ] Shared module tests pass
- [ ] All artifacts uploaded
- [ ] No errors in logs

### Code Quality
- [ ] No compilation warnings
- [ ] No deprecation warnings
- [ ] Build file syntax correct
- [ ] All TODO comments addressed

---

## Troubleshooting

### If iOS build fails with "framework not found"
```bash
# Check framework exists
ls -la shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework

# Check framework structure
ls -la shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework/ios-arm64/
ls -la shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework/ios-arm64_x86_64-simulator/

# Verify Info.plist exists
cat shared/src/iosMain/c_interop/libs/EunioBridgeKit.xcframework/Info.plist
```

### If cinterop fails
```bash
# Check .def file
cat shared/src/iosMain/c_interop/EunioBridgeKit.def

# Verify it points to correct framework location
# Should contain:
# language = Objective-C
# headers = EunioBridgeKit.h
# package = com.eunio.healthapp.bridge
```

### If CI fails but local works
- Framework might not be in git
- Check .gitignore
- Verify framework is committed and pushed
- Check CI logs for specific error

---

## Success Criteria

✅ iOS targets compile without errors  
✅ iOS frameworks generated successfully  
✅ CI builds pass for both Android and iOS  
✅ No asDynamic or dynamic call errors  
✅ All tests pass  
✅ iOS app runs on simulator  
✅ Firebase bridge works on iOS  

---

## Related Files

- `shared/build.gradle.kts` - iOS target configuration
- `shared/src/iosMain/c_interop/EunioBridgeKit.def` - cinterop definition
- `shared/src/iosMain/kotlin/com/eunio/healthapp/data/remote/firebase/FirebaseNativeBridge.ios.kt` - iOS bridge implementation
- `.github/workflows/build-and-test.yml` - CI configuration
- `.kiro/specs/4-kotlin-xcode-ios26-upgrade/ios-targets-temporary-disable.md` - This document

---

**Ready to use when:** EunioBridgeKit.xcframework is available in repository or CI can build it
