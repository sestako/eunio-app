# Scripts

## Testing
- `test-android-to-ios-sync.sh` - Test Android → iOS data sync
- `test-ios-to-android-sync.sh` - Test iOS → Android data sync
- `comprehensive_test_suite.sh` - Run full test suite
- `test_suite_validation.sh` - Validate test suite
- `test-commands.sh` - Common test commands

## Building
- `rebuild-and-test.sh` - Clean rebuild and test

## Debugging
- `watch-my-app-logs.sh` - Watch app logs in real-time
- `check-logs.sh` - Check logs for errors

## Firebase
- `deploy-security-rules.sh` - Deploy Firebase security rules
- `verify-firebase-bridge.sh` - Verify Firebase connection

## iOS
- `fix-ios-simulator-keyboard.sh` - Fix iOS simulator keyboard issues
- `upload-dsyms.sh` - Upload debug symbols to Firebase

## Validation
- `validate_cross_platform_dependencies.sh` - Validate dependencies
- `verify-logging-migration.sh` - Verify logging setup

## Usage

Make scripts executable:
```bash
chmod +x scripts/*.sh
```

Run a script:
```bash
./scripts/watch-my-app-logs.sh
```
