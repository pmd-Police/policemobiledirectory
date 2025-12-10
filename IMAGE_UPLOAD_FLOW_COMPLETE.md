# 📸 Image Upload Flow - Complete Diagram

## Current Flow (What Should Happen)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Android App (MyProfileScreen)                            │
│    - User selects/captures image                            │
│    - Image is cropped (UCrop)                               │
│    - URI: content://.../ucrop_xxx.jpg                       │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. ImageRepository.uploadPhoto()                            │
│    - Compresses image (800KB → 196KB)                       │
│    - Converts to base64 (261,508 chars)                     │
│    - Creates JSON: {image: "data:image/jpeg;base64,...",    │
│                    filename: "1953036.jpg"}                 │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. HTTP POST Request                                        │
│    URL: https://script.google.com/.../exec?action=uploadImage│
│    Method: POST                                             │
│    Body: JSON (base64 image)                                │
│    Content-Type: application/json                           │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Google Apps Script - doPost(e)                           │
│    ✅ Should log: "=== doPost START ==="                     │
│    ✅ Should log: "doPost called with action: uploadImage"  │
│    ✅ Should log: "Routing to uploadImage..."              │
│    ✅ Calls: uploadProfileImage(e)                         │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. uploadProfileImage(e)                                    │
│    ✅ Should log: "=== START uploadProfileImage ==="       │
│    - Reads raw body from e.postData                         │
│    - Parses JSON                                            │
│    - Extracts base64 image                                  │
│    - Decodes base64 → bytes                                 │
│    - Validates JPEG signature                               │
│    - Creates Blob                                           │
│    - Extracts KGID from filename (1953036)                  │
│    - Calls: handleBlobSave(e, blob, kgid, debug)            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. handleBlobSave(e, blob, kgid, debug)                     │
│    ✅ Should log: "--- handleBlobSave START ---"            │
│    - Gets Drive folder by ID                                │
│    - Creates file: employee_1953036_1234567890.jpg         │
│    - Sets sharing: Anyone with link                        │
│    - Gets Drive URL: https://drive.google.com/uc?export=... │
│    - Updates Google Sheet (photoUrl column)                │
│    - Updates Firestore (employees/{kgid}/photoUrl)         │
│    - Returns: {success: true, url: "...", id: "...", debug}│
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Response to Android App                                  │
│    ✅ Should return: JSON with success: true               │
│    ❌ Currently returning: HTML error page                 │
└─────────────────────────────────────────────────────────────┘
```

## ❌ Current Problem

**Step 7 is failing** - Apps Script is returning HTML instead of JSON.

This means one of these is happening:
1. `doPost()` is not being called (wrong URL or deployment)
2. `doPost()` is called but not returning a value
3. `uploadProfileImage()` is not returning a value
4. There's an error that's not being caught

## ✅ How to Fix

### Step 1: Check Execution Logs (MOST IMPORTANT)

1. Go to https://script.google.com
2. Open your project
3. Click **Executions** (clock icon)
4. Find the latest execution (from 16:36:13)
5. Click on it to see logs

**What to look for:**
- ✅ `"=== doPost START ==="` → doPost is called
- ✅ `"doPost called with action: uploadImage"` → Routing works
- ✅ `"Routing to uploadImage..."` → About to call function
- ✅ `"=== START uploadProfileImage ==="` → Function is called
- ❌ **No logs at all** → Deployment issue

### Step 2: Based on Logs

#### If NO LOGS appear:
**Problem**: Using old deployment or wrong URL

**Fix**:
1. In Apps Script: **Deploy** → **Manage deployments**
2. **Delete old deployment**
3. Create **NEW deployment**
4. Copy **NEW URL**
5. Update Android app

#### If logs show errors:
**Problem**: Code error

**Fix**: Share the error message from logs

#### If logs show function called but no return:
**Problem**: Function not returning value

**Fix**: Check that all code paths return `jsonResponse()`

## 🔍 Debug Checklist

Check these in order:

1. **Execution Logs** (script.google.com → Executions)
   - [ ] Are there any logs?
   - [ ] What messages appear?
   - [ ] Any error messages?

2. **Deployment**
   - [ ] Is it deployed as **Web app**?
   - [ ] **Who has access** = **"Anyone"**?
   - [ ] **Execute as** = **"Me"**?
   - [ ] Is it a **NEW deployment** (not old one)?

3. **Code**
   - [ ] Is `doPost()` function in the file?
   - [ ] Is `uploadProfileImage()` function in the file?
   - [ ] Are all functions returning values?

4. **URL**
   - [ ] Does Android app URL match deployment URL?
   - [ ] Is it the latest deployment URL?

## 📊 Expected Log Sequence

When working correctly, you should see this in execution logs:

```
=== doPost START ===
doPost called with action: uploadImage
Routing to uploadImage...
=== START uploadProfileImage ===
JSON parsed: OK
Base64 decoded: 196130 bytes
Blob created: 196130 bytes
--- handleBlobSave START ---
✅ Drive folder accessed
✅ File created in Drive: [FILE_ID]
✅ Drive URL: https://drive.google.com/uc?export=view&id=...
uploadProfileImage returned: result exists
Returning result from doPost
```

## 🎯 Next Action

**Check the execution logs first** - they will tell you exactly where the flow is breaking!

Share what you see in the logs, and I can help fix the specific issue.



