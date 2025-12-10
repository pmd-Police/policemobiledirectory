# 🔧 Image Upload Troubleshooting Guide

## Problem: "The script completed but did not return anything"

This error means the Apps Script `doPost()` function is not returning a value. Here's how to fix it:

## ✅ Step-by-Step Fix

### Step 1: Verify File Structure in Apps Script

Your Apps Script project **MUST** have both files:

```
📁 Your Apps Script Project
  ├── EMPLOYEE_SYNC_FINAL.gs    (main script with doPost routing)
  └── IMAGE_UPLOAD.gs            (image upload module)
```

**To check:**
1. Go to https://script.google.com
2. Open your project
3. Check the file list on the left
4. **Both files must be present**

### Step 2: Verify Function Exists

1. In Apps Script editor, click on `IMAGE_UPLOAD.gs`
2. Press `Ctrl+F` (or `Cmd+F` on Mac)
3. Search for `function uploadProfileImage`
4. **The function must exist**

### Step 3: Check Configuration

In `EMPLOYEE_SYNC_FINAL.gs`, verify these constants are defined:

```javascript
const DRIVE_FOLDER_ID = PROPS.getProperty("DRIVE_FOLDER_ID") || "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs";
const FIREBASE_API_KEY = PROPS.getProperty("FIREBASE_API_KEY") || "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";
const FIREBASE_PROJECT_ID = PROPS.getProperty("FIREBASE_PROJECT_ID") || "pmd-police-mobile-directory";
```

### Step 4: Test the Function Directly

1. In Apps Script editor, select `uploadProfileImage` from the function dropdown
2. Click **Run**
3. If it shows an error, fix it before deploying

### Step 5: Deploy as Web App (CRITICAL)

1. Click **Deploy** → **New deployment**
2. Click the gear icon ⚙️
3. Choose **Web app**
4. Configure:
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone` ⚠️ **MUST BE "Anyone"**
5. Click **Deploy**
6. **Copy the new Web app URL**

### Step 6: Update Android App URL

1. Open `app/src/main/java/com/example/policemobiledirectory/data/remote/GDriveUploadService.kt`
2. Verify the base URL matches your deployed script URL
3. The URL should be: `https://script.google.com/macros/s/YOUR_SCRIPT_ID/`

## 🔍 Common Issues

### Issue 1: Function Not Found

**Error**: "uploadProfileImage is not defined"

**Fix**: 
- Make sure `IMAGE_UPLOAD.gs` is in the same Apps Script project
- Save all files before deploying
- Check for typos in function name

### Issue 2: Missing Constants

**Error**: "DRIVE_FOLDER_ID is not defined"

**Fix**:
- Set constants in `EMPLOYEE_SYNC_FINAL.gs` or PropertiesService
- Use the exact constant names (case-sensitive)

### Issue 3: Wrong Deployment Settings

**Error**: HTML error page instead of JSON

**Fix**:
- Must deploy as **Web app** (not API executable)
- **Who has access** must be **"Anyone"**
- **Execute as** must be **"Me"**

### Issue 4: Script Not Saved

**Error**: Changes not reflected

**Fix**:
- Save all files (Ctrl+S / Cmd+S)
- Create a **new deployment** (don't just update existing one)
- Test with the new deployment URL

## 🧪 Testing

### Test 1: Direct Function Test

```javascript
// In Apps Script editor, run this test function:
function testUploadImage() {
  const testEvent = {
    parameter: { action: "uploadImage" },
    postData: {
      contents: JSON.stringify({
        image: "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        filename: "12345.jpg"
      })
    }
  };
  
  const result = uploadProfileImage(
    testEvent,
    "YOUR_DRIVE_FOLDER_ID",
    "YOUR_SHEET_ID",
    "Emp Profiles",
    "pmd-police-mobile-directory",
    "YOUR_API_KEY"
  );
  
  Logger.log(result.getContent());
}
```

### Test 2: Check Logs

1. In Apps Script editor, click **Executions** (clock icon)
2. Find your latest execution
3. Check for errors in the logs
4. Look for "doPost called with action: uploadImage"

## 📋 Checklist

Before deploying, verify:

- [ ] `IMAGE_UPLOAD.gs` file exists in project
- [ ] `uploadProfileImage` function exists in `IMAGE_UPLOAD.gs`
- [ ] `EMPLOYEE_SYNC_FINAL.gs` has correct constants
- [ ] All files are saved
- [ ] Deployed as **Web app** (not API executable)
- [ ] **Who has access** = **"Anyone"**
- [ ] **Execute as** = **"Me"**
- [ ] Android app URL matches deployment URL
- [ ] Tested with direct function call

## 🚨 Still Not Working?

1. **Check Execution Logs**:
   - View → Executions
   - Look for error messages
   - Check if `doPost` is being called

2. **Verify Function Signature**:
   - `uploadProfileImage` must accept exactly 6 parameters
   - Check parameter order matches the call

3. **Test with Simple Response**:
   ```javascript
   function doPost(e) {
     return jsonResponse({ test: "working" });
   }
   ```
   If this works, the issue is in `uploadProfileImage`

4. **Check Drive Folder Permissions**:
   - Verify `DRIVE_FOLDER_ID` is correct
   - Ensure the script has access to the folder

5. **Verify Firebase API Key**:
   - Check `FIREBASE_API_KEY` is valid
   - Ensure it has Firestore write permissions

## 📝 Notes

- Always create a **new deployment** after making changes
- The deployment URL changes when you create a new deployment
- Old deployments continue to work with old code
- Test with the **newest deployment URL**



