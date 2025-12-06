# 📸 Image Upload Flow - Complete Process

## ✅ Current Flow (As Implemented)

```
┌─────────────┐
│ Android App │
│  (User)     │
└──────┬──────┘
       │
       │ 1️⃣ Upload image (base64 JSON)
       │    POST /exec?action=uploadImage
       │    Body: {"filename": "98765.jpg", "image": "data:image/jpeg;base64,..."}
       ▼
┌──────────────────┐
│  Apps Script     │
│  (Backend)       │
└──────┬───────────┘
       │
       │ 2️⃣ Parse JSON, decode base64
       │    Create image blob
       ▼
┌──────────────────┐
│  Google Drive    │
│  (Storage)       │
└──────┬───────────┘
       │
       │ 3️⃣ Upload image file
       │    Get public URL: https://drive.google.com/uc?export=view&id=FILE_ID
       │
       │ 4️⃣ Update Google Sheet
       │    Find row by kgid → Update "photoUrl" column
       │
       │ 5️⃣ Update Firebase Firestore
       │    Update document: employees/{kgid} → photoUrl field
       │
       │ 6️⃣ Return success to Android App
       │    {"success": true, "url": "https://drive.google.com/...", "id": "FILE_ID"}
       ▼
┌─────────────┐
│ Android App │
│  (Display)  │
└─────────────┘
```

---

## 📋 Step-by-Step Details

### 1️⃣ **Android App** → **Apps Script**
- **File**: `ImageRepository.kt`
- **Action**: Converts image to base64, sends JSON POST request
- **Endpoint**: `exec?action=uploadImage`
- **Payload**: 
  ```json
  {
    "filename": "98765.jpg",
    "image": "data:image/jpeg;base64,/9j/4QCCRXhpZgAATU0AKgAAAAg..."
  }
  ```

### 2️⃣ **Apps Script** → **Process Image**
- **File**: `APPS_SCRIPT_FINAL_COMPLETE.js`
- **Function**: `uploadProfileImage(e)`
- **Actions**:
  - Parse JSON
  - Extract base64 string
  - Decode to bytes
  - Validate JPEG signature
  - Create blob

### 3️⃣ **Apps Script** → **Google Drive**
- **Function**: `handleBlobSave(e, blob, kgid, debug)`
- **Actions**:
  - Create file in Drive folder (ID: `DRIVE_FOLDER_ID`)
  - Set sharing: `ANYONE_WITH_LINK`
  - Generate public URL: `https://drive.google.com/uc?export=view&id={fileId}`
  - **File naming**: `employee_{kgid}_{timestamp}.jpg`

### 4️⃣ **Apps Script** → **Google Sheet**
- **Function**: `updateSheetFieldByKgid(kgid, "photoUrl", driveUrl)`
- **Actions**:
  - Open sheet: `SHEET_ID` → `SHEET_NAME` ("Emp Profiles")
  - Find row where `kgid` column matches
  - Update `photoUrl` column with Drive URL

### 5️⃣ **Apps Script** → **Firebase Firestore**
- **Function**: `updateFirebaseProfileImage(kgid, driveUrl)`
- **Actions**:
  - Update document: `officers/{kgid}`
  - Set field: `photoUrl = driveUrl`
  - Uses Firestore REST API: `PATCH /v1/projects/{project}/databases/(default)/documents/officers/{kgid}`

### 6️⃣ **Apps Script** → **Android App**
- **Response**:
  ```json
  {
    "success": true,
    "url": "https://drive.google.com/uc?export=view&id=FILE_ID",
    "id": "FILE_ID",
    "error": null,
    "debug": [...]
  }
  ```

---

## ✅ Summary

**Flow**: `Android App` → `Apps Script` → `Google Drive` (store) → `Google Sheet` (update) → `Firebase Firestore` (update) → `Android App` (display)

**All updates happen automatically in Apps Script** - the Android app just sends the image and receives the URL back!

---

## 🔧 Configuration (in Apps Script)

```javascript
const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Emp Profiles";
const DRIVE_FOLDER_ID = "1sR4NPomjADI5lmum-Bx6MAxvmTk1ydxV";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";
```

---

**Yes, your flow is correct!** ✅

1. **App** sends image
2. **Apps Script** processes it
3. **Google Drive** stores it
4. **Google Sheet** gets URL
5. **Firebase Firestore** gets URL
6. **App** displays it

All in one seamless flow! 🚀





