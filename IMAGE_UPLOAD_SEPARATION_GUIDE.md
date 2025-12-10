# 📁 Image Upload Logic Separation Guide

## Overview

The image upload functionality has been separated into a dedicated module for better code organization and maintainability.

## File Structure

### 1. `MAIN_SCRIPT.gs`
- **Purpose**: Main entry point, routing, and configuration
- **Contains**:
  - Configuration constants (SHEET_ID, DRIVE_FOLDER_ID, etc.)
  - `doPost()` and `doGet()` functions (routing)
  - `jsonResponse()` helper function
  - Routes requests to appropriate handlers

### 2. `IMAGE_UPLOAD.gs`
- **Purpose**: All image upload related logic
- **Contains**:
  - `uploadProfileImage()` - Main upload handler
  - `handleBlobSave()` - Saves blob to Drive and updates sheet/Firestore
  - `updateSheetFieldByKgid()` - Updates Google Sheet
  - `updateFirebaseProfileImage()` - Updates Firestore

## 📋 Setup Instructions

### Step 1: Create Files in Apps Script Project

1. Go to your Google Apps Script project: https://script.google.com
2. In the Apps Script editor, you'll see a file (usually `Code.gs`)
3. Click the **+** button to add a new file
4. Name it `MAIN_SCRIPT.gs` and paste the contents from `MAIN_SCRIPT.gs`
5. Click **+** again to add another file
6. Name it `IMAGE_UPLOAD.gs` and paste the contents from `IMAGE_UPLOAD.gs`
7. Delete or rename the old `Code.gs` file (or replace its contents)

### Step 2: Verify File Structure

Your Apps Script project should now have:
```
📁 Your Project
  ├── MAIN_SCRIPT.gs    (routing & config)
  └── IMAGE_UPLOAD.gs   (image upload logic)
```

### Step 3: Update Configuration

1. Open `MAIN_SCRIPT.gs`
2. Update the configuration constants at the top:
   ```javascript
   const SHEET_ID = "your-sheet-id";
   const SHEET_NAME = "your-sheet-name";
   const DRIVE_FOLDER_ID = "your-drive-folder-id";
   const FIREBASE_PROJECT_ID = "your-firebase-project-id";
   const FIREBASE_API_KEY = "your-firebase-api-key";
   ```

### Step 4: Deploy

1. Click **Deploy** → **New deployment**
2. Click the gear icon ⚙️ next to "Select type"
3. Choose **Web app**
4. Configure:
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone`
5. Click **Deploy**
6. Copy the **Web app URL**

## 🔍 How It Works

### Request Flow

```
Android App
    ↓
POST /exec?action=uploadImage
    ↓
MAIN_SCRIPT.gs → doPost()
    ↓
Routes to uploadImage action
    ↓
IMAGE_UPLOAD.gs → uploadProfileImage()
    ↓
handleBlobSave()
    ↓
Updates Drive, Sheet, Firestore
    ↓
Returns JSON response
```

### Function Dependencies

- `MAIN_SCRIPT.gs` calls `uploadProfileImage()` from `IMAGE_UPLOAD.gs`
- `IMAGE_UPLOAD.gs` uses `jsonResponse()` from `MAIN_SCRIPT.gs`
- All functions in the same Apps Script project can access each other

## ✅ Benefits of Separation

1. **Better Organization**: Image upload logic is isolated
2. **Easier Maintenance**: Update image upload without touching routing
3. **Reusability**: Image upload functions can be used by other actions
4. **Cleaner Code**: Main script focuses on routing, upload script focuses on uploads
5. **Easier Testing**: Can test image upload functions independently

## 🔧 Adding More Features

### To Add a New Action (e.g., `uploadDocument`):

1. Create a new file `DOCUMENT_UPLOAD.gs` with document upload functions
2. In `MAIN_SCRIPT.gs`, add routing:
   ```javascript
   if (action === "uploadDocument") {
     return uploadDocument(e, DRIVE_FOLDER_ID, ...);
   }
   ```

### To Add More Image Upload Methods:

1. Add new functions to `IMAGE_UPLOAD.gs`
2. Update `uploadProfileImage()` to handle new methods
3. No changes needed in `MAIN_SCRIPT.gs`

## 📝 Notes

- All files in the same Apps Script project share the same global scope
- Functions from one file can call functions from another file
- Configuration is centralized in `MAIN_SCRIPT.gs`
- Each module can have its own helper functions

## 🐛 Troubleshooting

### Error: "uploadProfileImage is not defined"
- **Cause**: `IMAGE_UPLOAD.gs` file not added to project
- **Fix**: Add `IMAGE_UPLOAD.gs` file to your Apps Script project

### Error: "jsonResponse is not defined"
- **Cause**: `MAIN_SCRIPT.gs` not loaded or function name mismatch
- **Fix**: Ensure `MAIN_SCRIPT.gs` contains `jsonResponse()` function

### Functions Not Found After Deployment
- **Cause**: Script not saved before deployment
- **Fix**: Save all files (Ctrl+S / Cmd+S) before deploying



