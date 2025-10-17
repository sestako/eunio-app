#!/bin/bash

# Script to upload dSYMs to Firebase Crashlytics

echo "🔍 Finding upload-symbols script..."
UPLOAD_SCRIPT=$(find ~/Library/Developer/Xcode/DerivedData -name "upload-symbols" -path "*/firebase-ios-sdk/*" 2>/dev/null | head -1)

if [ -z "$UPLOAD_SCRIPT" ]; then
    echo "❌ Error: Could not find upload-symbols script"
    echo "Make sure you've built the iOS app at least once"
    exit 1
fi

echo "✅ Found upload script at: $UPLOAD_SCRIPT"

echo ""
echo "🔍 Finding dSYM files..."
DSYM_DIR=$(find ~/Library/Developer/Xcode/DerivedData -path "*/iosApp*/Build/Products/*/*.app.dSYM" -type d 2>/dev/null | head -1)

if [ -z "$DSYM_DIR" ]; then
    echo "⚠️  No dSYM files found in Debug build"
    echo ""
    echo "To generate dSYMs, you need to:"
    echo "1. Open Xcode"
    echo "2. Product → Archive (or build for a real device)"
    echo "3. Then run this script again"
    echo ""
    echo "Alternatively, check ~/Library/Developer/Xcode/Archives for archived dSYMs"
    exit 1
fi

echo "✅ Found dSYM at: $DSYM_DIR"

echo ""
echo "📤 Uploading dSYMs to Firebase..."
"$UPLOAD_SCRIPT" -gsp "$(pwd)/iosApp/iosApp/GoogleService-Info.plist" -p ios "$DSYM_DIR"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ dSYM upload completed successfully!"
    echo "Check Firebase Console in a few minutes to see symbolicated crashes"
else
    echo ""
    echo "❌ Upload failed. Check the error messages above."
fi
