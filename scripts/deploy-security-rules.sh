#!/bin/bash

# Deploy Firebase Security Rules
# This script deploys Firestore and Storage security rules to Firebase

set -e

echo "🔒 Deploying Firebase Security Rules..."
echo ""

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "❌ Firebase CLI not found!"
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

# Check if logged in
if ! firebase projects:list &> /dev/null; then
    echo "❌ Not logged in to Firebase!"
    echo "Run: firebase login"
    exit 1
fi

# Deploy Firestore rules
echo "📝 Deploying Firestore rules..."
firebase deploy --only firestore:rules

# Deploy Storage rules
echo "📦 Deploying Storage rules..."
firebase deploy --only storage:rules

echo ""
echo "✅ Security rules deployed successfully!"
echo ""
echo "🔍 Verify rules in Firebase Console:"
echo "   Firestore: https://console.firebase.google.com/project/eunio-c4dde/firestore/rules"
echo "   Storage: https://console.firebase.google.com/project/eunio-c4dde/storage/rules"
echo ""
