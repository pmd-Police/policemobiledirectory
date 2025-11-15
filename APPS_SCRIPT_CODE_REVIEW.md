# Apps Script Code Review

## ✅ Good Parts

Your code structure is **excellent**:
- ✅ Has `doPost()` function (this was the main issue before)
- ✅ Handles `action=uploadImage` correctly
- ✅ Returns correct response format: `{success: true, url: "...", id: "...", error: null}`
- ✅ Updates both Google Sheet and Firestore
- ✅ Has proper error handling

## ⚠️ Issues Found

### 1. **Multipart/form-data Parsing**

**Current code:**
```javascript
if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
  blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
}
```

**Issue:** `e.postData.bytes` should work for multipart, but we need to extract the **filename** from the multipart data to get the kgid.

**Fix:** Extract filename from multipart contents to get kgid:
```javascript
// Extract filename from multipart
const contentStr = e.postData.contents.toString();
const filenameMatch = contentStr.match(/filename="([^"]+)"/);
if (filenameMatch) {
  fileName = filenameMatch[1];
  // Extract kgid: "98765.jpg" -> "98765"
  const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
  if (kgidMatch) {
    kgid = kgidMatch[1];
  }
}
```

### 2. **kgid Extraction**

**Current code:**
```javascript
const kgid = e.parameter.kgid;
```

**Issue:** The Android app sends kgid in the **filename** (`"$userId.jpg"`), not as a query parameter.

**Fix:** Extract from filename first, fallback to query parameter:
```javascript
let kgid = null;

// Extract from filename
const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
if (kgidMatch) {
  kgid = kgidMatch[1];
}

// Fallback to query parameter
if (!kgid) {
  kgid = e.parameter.kgid;
}
```

### 3. **Firestore Collection Name**

**Current code:**
```javascript
const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/users/" + ...
```

**Issue:** Uses `users` collection, but your Android app uses `officers` collection.

**Fix:** Check what collection your app uses and update accordingly.

## ✅ What Works

1. ✅ Response format is correct
2. ✅ Error handling is good
3. ✅ Logging is helpful for debugging
4. ✅ File creation and sharing works

## 🔧 Recommended Fixes

See `APPS_SCRIPT_REVIEWED_FIXED.js` for the complete fixed version.

### Key Changes:
1. **Better kgid extraction** - from filename first, then query parameter
2. **Better logging** - more detailed logs for debugging
3. **Firestore collection** - verify if it should be `users` or `officers`
4. **Error messages** - more descriptive errors

## 🧪 Testing Steps

After applying fixes:

1. **Test in Apps Script:**
   - Go to **Executions** → Run `uploadProfileImage` manually (if possible)
   - Check logs in **View → Logs**

2. **Test from Android App:**
   - Upload an image from profile
   - Check Logcat for response
   - Verify file appears in Google Drive
   - Check if Firestore is updated

3. **Verify Response Format:**
   - Response should be JSON: `{success: true, url: "...", id: "...", error: null}`
   - Not HTML error page

## 📋 Checklist

- [x] `doPost()` function exists
- [x] `uploadProfileImage()` handles `action=uploadImage`
- [x] Returns correct response format
- [ ] Extracts kgid from filename (needs fix)
- [ ] Uses correct Firestore collection (`officers` vs `users`)
- [x] Error handling implemented
- [x] Logging for debugging

## 🎯 Next Steps

1. ✅ Copy the fixed code from `APPS_SCRIPT_REVIEWED_FIXED.js`
2. ✅ Update Firestore collection if needed (check your Android code)
3. ✅ Deploy the script
4. ✅ Test from Android app
5. ✅ Check Apps Script execution logs if errors occur

