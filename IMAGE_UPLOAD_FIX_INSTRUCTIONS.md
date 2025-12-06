# Image Upload HTML Error - Fix Instructions

## ✅ Fix Applied
Added `doPost()` and `uploadProfileImage()` functions to `EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs`.

## 🔍 Verify Which Apps Script to Update

The Android app is using this URL for image uploads:
```
https://script.google.com/macros/s/AKfycbw3BybPar7IpUPm10nEDlT1UEbYMTiMsDvnxQyv9l3sf916Mk9DuDZcc4u_h8DV7vSI9w/exec
```

**Check**: Is `EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs` deployed to this URL, or is it a different Apps Script project?

## 📋 Steps to Fix

### Step 1: Identify the Correct Apps Script Project

1. **Open Google Apps Script**: https://script.google.com
2. **Find the project** that matches URL: `AKfycbw3BybPar7IpUPm10nEDlT1UEbYMTiMsDvnxQyv9l3sf916Mk9DuDZcc4u_h8DV7vSI9w`
3. **Check if it has a `doPost()` function** - if not, that's the problem!

### Step 2: Add doPost() Function

If the Apps Script project **doesn't have `doPost()`**, add this:

```javascript
/**
 * ✅ FIXED doPost - Handles POST requests (including image uploads)
 * Always returns JSON (never HTML error pages)
 */
function doPost(e) {
  try {
    // Get action from query parameter
    const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : null;
    
    if (!action) {
      return jsonResponse({ 
        success: false, 
        error: "Missing action parameter. Use ?action=uploadImage" 
      }, 400);
    }
    
    Logger.log('doPost called with action: ' + action);
    
    // Route to appropriate handler
    if (action === "uploadImage") {
      return uploadProfileImage(e);
    }
    
    // Unknown action
    return jsonResponse({ 
      success: false, 
      error: "Unknown action: " + action 
    }, 400);
      
  } catch (error) {
    Logger.log('Error in doPost: ' + error.toString());
    // ✅ IMPORTANT: Always return JSON, never let Apps Script return HTML error
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + error.toString() 
    }, 500);
  }
}
```

### Step 3: Add uploadProfileImage() Function

Add the complete `uploadProfileImage()` function from `EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs` (lines 864-1017).

**OR** copy from `UPLOAD_PROFILE_IMAGE_FIXED.gs` and update `DRIVE_FOLDER_ID`.

### Step 4: Add DRIVE_FOLDER_ID Constant

At the top of your Apps Script file, add:

```javascript
const DRIVE_FOLDER_ID = "YOUR_DRIVE_FOLDER_ID_HERE"; 
// Or leave blank - the function will auto-create "Employee Photos" folder
```

**To find your Drive Folder ID:**
1. Open Google Drive
2. Navigate to or create the folder for employee photos
3. Open the folder
4. Look at the URL: `https://drive.google.com/drive/folders/FOLDER_ID_HERE`
5. Copy the `FOLDER_ID_HERE` part

### Step 5: Add jsonResponse() Helper (if missing)

If your script doesn't have this helper function, add it:

```javascript
function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}
```

### Step 6: Deploy the Updated Script

1. **Save** your script (Ctrl+S)
2. **Deploy → Manage deployments**
3. **Click Edit** (pencil icon) on your existing deployment
4. **Verify settings**:
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone`
5. **Click Deploy**
6. **Select "New version"** when prompted
7. **Click Deploy**

### Step 7: Test the Upload

1. Try uploading a profile image from the Android app
2. Check Logcat for any errors
3. Check Apps Script **Execution log** (View → Execution log) for any errors

## 🎯 Quick Checklist

- [ ] `doPost()` function exists and handles `action=uploadImage`
- [ ] `uploadProfileImage()` function exists and returns JSON
- [ ] `DRIVE_FOLDER_ID` is set (or function will auto-create folder)
- [ ] `jsonResponse()` helper function exists
- [ ] Script is deployed with "Execute as: Me" and "Who has access: Anyone"
- [ ] Deployment is a new version

## 🔧 If Still Getting HTML Response

1. **Check Execution Logs**: View → Execution log in Apps Script
2. **Look for errors** in `doPost()` or `uploadProfileImage()`
3. **Test manually**: Try calling the endpoint with curl or Postman
4. **Verify deployment URL** matches what's in the Android app

## 📝 Files Updated

- ✅ `EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs` - Added `doPost()` and `uploadProfileImage()`
- ✅ `UPLOAD_PROFILE_IMAGE_FIXED.gs` - Standalone fix file (can be copied)

The fix ensures **all responses are JSON**, preventing HTML error pages from Apps Script.
