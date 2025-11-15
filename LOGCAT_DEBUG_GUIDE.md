# Logcat Debugging Guide

## ⚠️ If NO logs are appearing:

### 1. **Check Package Name**
Your app package is: `com.example.policemobiledirectory`

**NOT:** `com.policemobiledirectory` ❌

### 2. **Logcat Filter Settings**

In Android Studio Logcat:

1. **Clear ALL filters** - Click the "X" on any active filters
2. **Set Log Level to "Verbose"** (shows all logs)
3. **Select your device/emulator** from the dropdown
4. **Search for:** `TEST_LOG` or `ImageRepository` or `CommonEmployeeForm`

### 3. **Verify Logging Works**

When you start the app, you should IMMEDIATELY see:
```
TEST_LOG: ═══════════════════════════════════════
TEST_LOG: 🚀🚀🚀 MAINACTIVITY ONCREATE CALLED 🚀🚀🚀
```

**If you DON'T see this:**
- The app isn't running the new code
- You need to **Clean & Rebuild**:
  - Build → Clean Project
  - Build → Rebuild Project
  - Run the app again

### 4. **Check Logcat Window**

Make sure Logcat is visible:
- View → Tool Windows → Logcat
- Or press `Alt + 6` (Windows) / `Cmd + 6` (Mac)

### 5. **Try Different Log Levels**

In Logcat, try filtering by:
- **Tag:** `TEST_LOG` (should show immediately on app start)
- **Tag:** `ImageRepository` (upload logs)
- **Tag:** `CommonEmployeeForm` (button click logs)
- **No filter** (show all logs)

### 6. **Alternative: Use System.out.println**

If Log.d() doesn't work, try:
```kotlin
System.out.println("DEBUG: Your message here")
```

### 7. **Check if App is Running**

- Make sure the app is actually running on your device/emulator
- Check the device selector in Logcat shows your device
- Try uninstalling and reinstalling the app

### 8. **Expected Log Sequence When Uploading**

When you click "Submit update for approval", you should see:

```
TEST_LOG: 🚀🚀🚀 MAINACTIVITY ONCREATE CALLED 🚀🚀🚀
CommonEmployeeForm: 🔵🔵🔵 BUTTON CLICKED 🔵🔵🔵
CommonEmployeeForm: ✅ Validation passed
MyProfileScreen: 📤 onSubmit called!
AddEditViewModel: 🎯 saveEmployee() CALLED
AddEditViewModel: 📸 Photo provided, starting upload...
ImageRepository: ═══════════════════════════════════════
ImageRepository: 📤 Starting upload for userId: [kgid]
```

### 9. **If Still No Logs**

1. **Check build.gradle.kts** - Make sure `isMinifyEnabled = false` in debug build
2. **Check ProGuard** - If enabled, it might be removing logs
3. **Try a different device/emulator**
4. **Restart Android Studio**
5. **Check Android Studio Logcat settings** - File → Settings → Editor → General → Console

### 10. **Quick Test**

Add this to any composable function:
```kotlin
LaunchedEffect(Unit) {
    android.util.Log.e("QUICK_TEST", "This composable is being rendered!")
}
```

If you see this log, logging works. If not, there's a configuration issue.

