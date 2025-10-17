# Firebase Security Rules Fix

## Problem

iOS is getting permission denied when trying to write to Firebase:

```
Write at users/8FzGtzfcIkUjAwZW9qqA6OkbtNL2/dailyLogs/log_2025-10-10_1760201686 failed: 
Missing or insufficient permissions.
```

## Root Cause

Firebase Security Rules are blocking writes to the `users/{userId}/dailyLogs` collection.

## Solution

Update Firebase Security Rules to allow authenticated users to read/write their own data.

### Step 1: Open Firebase Console

1. Go to https://console.firebase.google.com
2. Select your project
3. Click "Firestore Database" in left menu
4. Click "Rules" tab

### Step 2: Update Rules

Replace the current rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Allow users to read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Allow users to read/write their own daily logs
      match /dailyLogs/{logId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Allow users to read/write their own cycles
      match /cycles/{cycleId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Allow users to read/write their own insights
      match /insights/{insightId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Allow users to read/write their own settings
      match /settings/{settingId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
    
    // Sync metadata
    match /syncMetadata/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Health reports
    match /healthReports/{reportId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
    }
  }
}
```

### Step 3: Publish Rules

1. Click "Publish" button
2. Wait for rules to deploy (usually instant)

### Step 4: Test Again

1. Try saving on iOS again
2. Should work now! ✅

## For Development/Testing Only

If you want to allow all reads/writes temporarily for testing:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**⚠️ WARNING:** This allows any authenticated user to read/write ANY document. Only use for testing!

## Verify Rules Work

After updating rules, test:

1. **iOS:** Save log → Should succeed ✅
2. **Android:** Save log → Should succeed ✅
3. **iOS:** Read Android's log → Should succeed ✅
4. **Android:** Read iOS's log → Should succeed ✅

## Common Issues

### Issue: Still getting permission denied

**Solution:** 
- Make sure you're logged in (check `request.auth != null`)
- Make sure user ID matches (`request.auth.uid == userId`)
- Check rules are published

### Issue: Can't read other users' data

**Solution:** This is correct! Users should only access their own data for privacy.

---

**Next Step:** Update Firebase Rules, then test sync again!
