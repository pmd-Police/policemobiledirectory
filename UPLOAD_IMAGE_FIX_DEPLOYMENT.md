# 🔧 Fix: Apps Script Returning HTML Instead of JSON

## Problem
The Apps Script is returning HTML error page instead of JSON:
```
The script completed but did not return anything.
```

## Root Cause
This happens when:
1. The script is not deployed correctly (not as web app)
2. The `doPost()` function doesn't return a value in all code paths
3. An uncaught error occurs

## ✅ Solution

### Step 1: Copy the Fixed Script
1. Open `APPS_SCRIPT_UPLOAD_IMAGE_FIX.js` in this project
2. Copy the entire contents
3. Go to your Google Apps Script project: https://script.google.com
4. Replace the entire contents of your script with the fixed version

### Step 2: Deploy as Web App (CRITICAL)

1. In Apps Script editor, click **Deploy** → **New deployment**
2. Click the gear icon ⚙️ next to "Select type"
3. Choose **Web app**
4. Configure:
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone` (IMPORTANT!)
5. Click **Deploy**
6. Copy the **Web app URL** (it will look like: `https://script.google.com/macros/s/AKfycbw.../exec`)

### Step 3: Verify Deployment

Test the endpoint:
```bash
curl -X POST "YOUR_WEB_APP_URL?action=uploadImage" \
  -H "Content-Type: application/json" \
  -d '{"image":"data:image/jpeg;base64,/9j/4AAQ...","filename":"12345.jpg"}'
```

You should get JSON response, NOT HTML.

### Step 4: Update Android App (if needed)

The Android app should already be using the correct URL. Verify in:
- `app/src/main/java/com/example/policemobiledirectory/data/remote/GDriveUploadService.kt`
- `app/src/main/java/com/example/policemobiledirectory/repository/ImageRepository.kt`

The URL should be:
```
https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=uploadImage
```

## 🔍 Key Fixes in the New Script

1. **Always returns JSON**: Every function has try-catch and always returns `jsonResponse()`
2. **Better error handling**: Errors are caught and returned as JSON, never as HTML
3. **Simplified logic**: Focuses on base64 JSON uploads (what Android app sends)
4. **Debug array**: Includes debug information in responses for troubleshooting

## 📋 Checklist

- [ ] Copied fixed script to Apps Script editor
- [ ] Deployed as Web app with "Execute as: Me" and "Who has access: Anyone"
- [ ] Tested endpoint returns JSON (not HTML)
- [ ] Verified Android app URL matches deployed script URL
- [ ] Tested image upload from Android app

## 🐛 If Still Getting HTML

1. **Check deployment settings**: Must be "Execute as: Me" and "Anyone"
2. **Check script logs**: View → Executions in Apps Script editor
3. **Verify script saved**: Make sure you saved the script before deploying
4. **Try new deployment**: Delete old deployment and create a new one

## 📝 Notes

- The fixed script (`APPS_SCRIPT_UPLOAD_IMAGE_FIX.js`) is a simplified, focused version that handles base64 JSON uploads
- It always returns JSON, even on errors
- Debug information is included in responses to help troubleshoot issues



