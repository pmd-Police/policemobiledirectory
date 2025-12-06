# Image Upload Integration Guide - FINAL VERSION

## ✅ What Was Fixed

Your new clean `EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs` script has been updated to handle image uploads properly.

## 🔧 Changes Needed

### Step 1: Update `handleEmployeeApi()` Function

In your script, find the `handleEmployeeApi()` function and add the image upload routing:

**Find this section:**
```javascript
function handleEmployeeApi(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    
    const action = e.parameter.action;
    Logger.log("handleEmployeeApi called with action: " + action);

    switch (action) {
      case "getEmployees": 
        return jsonResponse(getEmployees());
        
      case "pullDataFromFirebase": 
        return jsonResponse(task_pullDataFromFirebase());
        
      case "pushDataToFirebase": 
        return jsonResponse(task_pushDataToFirebase());
        
      case "dryRunPush": 
        return jsonResponse(task_dryRunPush());
        
      default:
        return jsonResponse({ error: "Invalid action: " + action }, 400);
    }
  } catch(err) {
    Logger.log("handleEmployeeApi ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}
```

**Replace with:**
```javascript
function handleEmployeeApi(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    
    const action = e.parameter.action;
    Logger.log("handleEmployeeApi called with action: " + action);

    switch (action) {
      case "getEmployees": 
        return jsonResponse(getEmployees());
        
      case "pullDataFromFirebase": 
        return jsonResponse(task_pullDataFromFirebase());
        
      case "pushDataToFirebase": 
        return jsonResponse(task_pushDataToFirebase());
        
      case "dryRunPush": 
        return jsonResponse(task_dryRunPush());
      
      // ✅ ADD THIS: Handle image upload actions
      case "uploadImage":
      case "uploadImageEnhanced":
        return uploadImageEnhanced(e);
        
      default:
        return jsonResponse({ error: "Invalid action: " + action }, 400);
    }
  } catch(err) {
    Logger.log("handleEmployeeApi ERROR: " + err.toString());
    // ✅ Always return JSON, never HTML
    return jsonResponse({ error: err.toString() }, 500);
  }
}
```

### Step 2: Verify `uploadImageEnhanced()` Function Exists

Your script already has the `uploadImageEnhanced()` function (from your provided code). Make sure it's present and includes all helper functions:
- `uploadImageEnhanced(e)`
- `findPhotoUrlInSheetByKgid(kgid)`
- `extractFileIdFromDriveUrl(url)`
- `updateSheetPhotoForKgid(kgid, url)`
- `updateFirestorePhotoUrlWithToken(kgid, url)`
- `sidebar_getPhotoPreview(kgid)`

### Step 3: Verify `DRIVE_FOLDER_ID` is Set

Your script already has:
```javascript
const DRIVE_FOLDER_ID = "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs";
```

✅ This is correct! No changes needed.

### Step 4: Deploy the Updated Script

1. **Save** your script
2. **Deploy → Manage deployments**
3. **Edit** your existing deployment
4. **Select "New version"**
5. **Verify settings**:
   - Execute as: `Me`
   - Who has access: `Anyone`
6. **Deploy**

## 🎯 Complete File Available

I've created a complete fixed version in:
- **`FINAL_EMPLOYEE_SYNC_WITH_IMAGE_UPLOAD.gs`** - Complete script with image upload integrated

You can copy this entire file to your Apps Script project, or just copy the `handleEmployeeApi()` function update.

## ✅ What This Fixes

1. **Routes `uploadImage` action** to `uploadImageEnhanced()` function
2. **Always returns JSON** - never HTML error pages
3. **Proper error handling** - all errors return JSON format
4. **Uses your existing `uploadImageEnhanced()`** function with all features:
   - Image resizing
   - Replace old files
   - Update Sheet
   - Update Firestore
   - Drive folder management

## 🔍 Testing

After deploying:
1. Try uploading a profile image from the Android app
2. Check Logcat for success/error messages
3. Check Apps Script Execution logs for any errors

The fix ensures the `handleEmployeeApi()` function properly routes image upload requests and always returns JSON responses.
