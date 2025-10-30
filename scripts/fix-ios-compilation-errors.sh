#!/bin/bash

# Script to fix common iOS compilation errors after Kotlin 2.2 / iOS 26 upgrade

set -e

echo "🔧 Fixing iOS compilation errors..."

# 1. Fix Clock.System.now() -> Kotlinx_datetimeInstant.companion.now()
echo "📅 Fixing Clock.System.now() calls..."
find iosApp/iosApp -name "*.swift" -type f -exec sed -i '' 's/Clock\.System\.now()/Kotlinx_datetimeInstant.companion.now()/g' {} \;

# 2. Fix Result type conflicts (Swift.Result vs Kotlin Result)
echo "🔄 Fixing Result type conflicts..."
find iosApp/iosApp -name "*.swift" -type f -exec sed -i '' 's/-> Result</-> Swift.Result</g; s/return Result\./return Swift.Result./g' {} \;

echo "✅ Common patterns fixed!"
echo ""
echo "Next steps:"
echo "1. Create SharedTypeAliases.swift with HealthAppError typealias"
echo "2. Create environment keys for settingsManager and notificationManager"
echo "3. Fix remaining file-specific issues"
