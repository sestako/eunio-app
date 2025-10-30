# Fix Duplicate Swift Files Causing .stringsdata Errors

## Root Cause
The "Multiple commands produce .stringsdata" errors are caused by duplicate Swift files being compiled twice. Xcode is trying to build both copies, creating conflicting build artifacts.

## Duplicate Files Identified

### Files to REMOVE (duplicates in UI/Auth/):
- ❌ `iosApp/iosApp/UI/Auth/AuthViewModel.swift`
- ❌ `iosApp/iosApp/UI/Auth/SignInView.swift`
- ❌ `iosApp/iosApp/UI/Auth/SignUpView.swift`
- ❌ `iosApp/iosApp/ProfileTestView.swift` (root level duplicate)

### Files to KEEP (canonical versions):
- ✅ `iosApp/iosApp/ViewModels/AuthViewModel.swift`
- ✅ `iosApp/iosApp/Views/Authentication/SignInView.swift`
- ✅ `iosApp/iosApp/Views/Authentication/SignUpView.swift`
- ✅ `iosApp/iosApp/Views/ProfileTestView.swift`

## Step-by-Step Fix (In Xcode UI)

### Step 1: Open Xcode
```bash
open iosApp/iosApp.xcodeproj
```

### Step 2: Remove Duplicate Files

**For each duplicate file:**

1. **Find `UI/Auth/AuthViewModel.swift`** in the Project Navigator
   - Right-click on the file
   - Select **Delete**
   - In the dialog, choose **Move to Trash** (not just "Remove Reference")
   - This ensures the file is deleted from disk

2. **Find `UI/Auth/SignInView.swift`**
   - Right-click → Delete → **Move to Trash**

3. **Find `UI/Auth/SignUpView.swift`**
   - Right-click → Delete → **Move to Trash**

4. **Find `ProfileTestView.swift`** (the one at root level, NOT in Views/)
   - Right-click → Delete → **Move to Trash**

5. **Delete the empty `UI/Auth/` folder** if it exists
   - Right-click on `UI/Auth` folder → Delete → **Move to Trash**

### Step 3: Verify Canonical Files Exist

Make sure these files are still in the project:

- ✅ `ViewModels/AuthViewModel.swift`
- ✅ `Views/Authentication/SignInView.swift`
- ✅ `Views/Authentication/SignUpView.swift`
- ✅ `Views/ProfileTestView.swift`

### Step 4: Verify Target Membership

For each of the KEPT files:

1. Select the file in Project Navigator
2. Open **File Inspector** (right panel, first tab)
3. Under **Target Membership**, verify:
   - ✅ `iosApp` is checked
   - No other targets are checked

### Step 5: Check Build Phases

1. Select **iosApp project** → **iosApp target**
2. Go to **Build Phases** tab
3. Expand **Compile Sources**
   - Verify each of the 4 kept files appears ONCE
   - Verify the deleted files do NOT appear
4. Expand **Copy Bundle Resources**
   - Verify NO `.swift` files are listed here
   - Only assets, plists, and resources should be here

### Step 6: Clean Build Folder

1. **Product → Clean Build Folder** (hold Option key)
2. Wait for completion

### Step 7: Delete DerivedData

Close Xcode, then:
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData/*
```

Reopen Xcode:
```bash
open iosApp/iosApp.xcodeproj
```

### Step 8: Rebuild

1. **Product → Build** (Cmd+B)
2. Verify build succeeds
3. Check that NO `.stringsdata` errors appear
4. Check that NO "Multiple commands produce" errors appear

## Expected Result

After this fix:
- ✅ No "Multiple commands produce" errors
- ✅ No `.stringsdata` errors
- ✅ Clean folder structure (Views/ and ViewModels/)
- ✅ No duplicate files
- ✅ Build succeeds

## Folder Structure (After Cleanup)

```
iosApp/iosApp/
├── ViewModels/
│   └── AuthViewModel.swift          ✅ KEEP
├── Views/
│   ├── Authentication/
│   │   ├── SignInView.swift         ✅ KEEP
│   │   └── SignUpView.swift         ✅ KEEP
│   └── ProfileTestView.swift        ✅ KEEP
├── Services/
├── Models/
├── Core/
└── ...
```

## Prevention

Going forward:
- Use **Views/** for all SwiftUI screens
- Use **ViewModels/** for all view models
- Avoid creating parallel folders like `UI/Auth/`
- Always check for duplicates before adding files

## Troubleshooting

### If files don't appear in Xcode:
- The files might be in the filesystem but not in the project
- Use Finder to verify they exist on disk
- If missing from Xcode, right-click folder → Add Files

### If "Move to Trash" is grayed out:
- The file might be a reference only
- Choose "Remove Reference" instead
- Then manually delete from Finder

### If errors persist after cleanup:
- Verify all 4 duplicates are truly deleted (check Finder)
- Verify Compile Sources phase has no duplicates
- Delete DerivedData again
- Restart Xcode

## Verification Commands

After cleanup, verify from terminal:

```bash
# Should find exactly 4 files (no duplicates)
find iosApp/iosApp -name "AuthViewModel.swift" -o -name "SignInView.swift" -o -name "SignUpView.swift" -o -name "ProfileTestView.swift"

# Expected output:
# iosApp/iosApp/ViewModels/AuthViewModel.swift
# iosApp/iosApp/Views/Authentication/SignInView.swift
# iosApp/iosApp/Views/Authentication/SignUpView.swift
# iosApp/iosApp/Views/ProfileTestView.swift
```

If you see more than 4 files, duplicates still exist!

## Next Steps

Once the build succeeds:
1. Continue with Task 16 (Run iOS tests)
2. Test the app on simulator
3. Verify all features work correctly
