# 📍 Where to Paste the Code

## ⚠️ Important: This is for Google Apps Script, NOT your local project!

The code goes into **Google Apps Script** (online), not your Android Studio project.

## ✅ Step-by-Step: Where to Paste

### Step 1: Go to Google Apps Script

1. Open your web browser
2. Go to: **https://script.google.com**
3. Sign in with your Google account (the same account that has access to your Google Sheet)

### Step 2: Open or Create Your Project

**Option A: If you already have a project:**
1. Click on your existing project (or create a new one)
2. You'll see the Apps Script editor

**Option B: If you need to create a new project:**
1. Click **"New project"** (or the **+** button)
2. A new project opens with a file called `Code.gs`

### Step 3: Paste the Code

**For the embedded version (recommended - single file):**

1. In the Apps Script editor, you'll see a file (usually `Code.gs`)
2. **Select ALL the text** in that file (Ctrl+A / Cmd+A)
3. **Delete it** (Delete key)
4. Open `EMPLOYEE_SYNC_WITH_EMBEDDED_UPLOAD.gs` from your local project
5. **Copy the ENTIRE contents** (Ctrl+A, then Ctrl+C)
6. **Paste into the Apps Script editor** (Ctrl+V)
7. **Save** (Ctrl+S or click the save icon 💾)

### Step 4: Verify the File

After pasting, you should see:
- One file in the Apps Script project (usually `Code.gs`)
- The file contains all the functions including `doPost`, `uploadProfileImage`, etc.

### Step 5: Check Function Exists

1. Click the function dropdown (top right, says "Select function")
2. Type "uploadProfileImage"
3. If it appears in the list → ✅ Function exists
4. If it doesn't appear → ❌ Check the file was saved

## 📁 File Structure in Apps Script

After pasting, your Apps Script project should look like:

```
📁 Your Apps Script Project
  └── Code.gs  (or whatever you named it)
      ├── All configuration constants
      ├── doPost() function
      ├── uploadProfileImage() function
      ├── handleBlobSave() function
      ├── All employee sync functions
      └── All helper functions
```

**Everything is in ONE file** - that's the embedded version.

## 🆚 Two Options

### Option 1: Single File (Easier - Recommended)
- Use: `EMPLOYEE_SYNC_WITH_EMBEDDED_UPLOAD.gs`
- Paste into: One file in Apps Script (e.g., `Code.gs`)
- ✅ **Easier, less chance of errors**

### Option 2: Two Files (Separated)
- Use: `EMPLOYEE_SYNC_FINAL.gs` + `IMAGE_UPLOAD.gs`
- Paste into: Two separate files in Apps Script
- ⚠️ More steps, but better organization

## 📝 Quick Checklist

- [ ] Opened https://script.google.com
- [ ] Opened/Created Apps Script project
- [ ] Selected all text in `Code.gs` and deleted it
- [ ] Copied entire contents of `EMPLOYEE_SYNC_WITH_EMBEDDED_UPLOAD.gs`
- [ ] Pasted into Apps Script editor
- [ ] Saved the file (Ctrl+S)
- [ ] Verified `uploadProfileImage` appears in function dropdown
- [ ] Ready to deploy!

## 🎯 Visual Guide

```
Your Computer (Android Studio Project)
  └── EMPLOYEE_SYNC_WITH_EMBEDDED_UPLOAD.gs  ← Copy from here
                    ↓
              (Copy contents)
                    ↓
Google Apps Script (script.google.com)
  └── Code.gs  ← Paste here
```

## ⚠️ Common Mistakes

1. **Pasting into Android Studio** ❌
   - The code goes in Apps Script, not Android Studio

2. **Not deleting old code** ❌
   - Make sure to delete old code before pasting new code

3. **Not saving** ❌
   - Always save (Ctrl+S) after pasting

4. **Pasting into wrong file** ❌
   - Paste into the main file (usually `Code.gs`)

## 🚀 After Pasting

Once you've pasted and saved:
1. Deploy as Web app (Deploy → New deployment)
2. Copy the deployment URL
3. Update your Android app with the new URL



