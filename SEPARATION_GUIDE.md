# 📦 Code Separation Guide

## ✅ Files Created

I've created two **standalone, fully-featured** Apps Script files:

### 1. `IMAGE_UPLOAD_COMPLETE.gs`
**Purpose**: Handles profile image uploads

**Features**:
- ✅ Image upload to Google Drive
- ✅ JPEG validation with debugging
- ✅ Updates Google Sheets (photoUrl field)
- ✅ Updates Firestore (photoUrl field)
- ✅ Full error handling and logging
- ✅ Standalone deployment ready

**Endpoints**:
- `POST /exec?action=uploadImage` - Upload image

### 2. `EMPLOYEE_SYNC_COMPLETE.gs`
**Purpose**: Handles employee data synchronization

**Features**:
- ✅ Pull data from Firestore → Google Sheets
- ✅ Push data from Google Sheets → Firestore
- ✅ Single employee push
- ✅ Batch sync with progress tracking
- ✅ Sheet validation
- ✅ Menu and sidebar integration
- ✅ Search functionality
- ✅ Full error handling and logging
- ✅ Standalone deployment ready

**Endpoints**:
- `GET/POST /exec?action=getEmployees` - Get all employees
- `GET/POST /exec?action=pullDataFromFirebase` - Pull from Firestore
- `GET/POST /exec?action=pushDataToFirebase` - Push to Firestore
- `GET/POST /exec?action=pushSingleEmployee&kgid=XXX` - Push single employee
- `GET/POST /exec?action=dryRunPush` - Dry run (disabled)

## 🚀 Deployment Options

### Option 1: Separate Deployments (Recommended)

**Benefits**:
- Independent scaling
- Separate permissions
- Easier maintenance
- Can update one without affecting the other

**Steps**:

1. **Deploy Image Upload Service**:
   - Create new Apps Script project
   - Paste `IMAGE_UPLOAD_COMPLETE.gs`
   - Deploy as Web app
   - Copy deployment URL
   - Update Android app `ImageRepository.kt` with new URL

2. **Deploy Employee Sync Service**:
   - Create new Apps Script project (or use existing)
   - Paste `EMPLOYEE_SYNC_COMPLETE.gs`
   - Deploy as Web app
   - Copy deployment URL
   - Update Android app `NetworkModule.kt` with new URL

### Option 2: Combined Deployment (Single Project)

**Benefits**:
- Single URL to manage
- Shared configuration
- Easier for small projects

**Steps**:

1. Create Apps Script project
2. Create two files:
   - `IMAGE_UPLOAD.gs` - Paste `IMAGE_UPLOAD_COMPLETE.gs` content
   - `EMPLOYEE_SYNC.gs` - Paste `EMPLOYEE_SYNC_COMPLETE.gs` content
3. Create `MAIN_ROUTER.gs`:
   ```javascript
   function doGet(e) {
     const action = e.parameter.action;
     if (action === "uploadImage") {
       return uploadProfileImage(e); // From IMAGE_UPLOAD.gs
     }
     return handleEmployeeApi(e); // From EMPLOYEE_SYNC.gs
   }
   
   function doPost(e) {
     const action = e.parameter.action;
     if (action === "uploadImage") {
       return uploadProfileImage(e); // From IMAGE_UPLOAD.gs
     }
     return handleEmployeeApi(e); // From EMPLOYEE_SYNC.gs
   }
   ```
4. Deploy as Web app
5. Use single URL for both services

## ⚙️ Configuration

Both files use **Script Properties** for configuration:

### Image Upload Service Properties:
- `SHEET_ID` - Google Sheet ID
- `SHEET_NAME` - Sheet name (default: "Emp Profiles")
- `DRIVE_FOLDER_ID` - Google Drive folder ID
- `FIREBASE_API_KEY` - Firebase API key
- `FIREBASE_PROJECT_ID` - Firebase project ID

### Employee Sync Service Properties:
- `SHEET_ID` - Google Sheet ID
- `SHEET_NAME` - Sheet name (default: "Emp Profiles")
- `FIREBASE_PROJECT_ID` - Firebase project ID
- `BATCH_SIZE` - Batch size (default: 300)
- `PRIVATE_KEY` - Service account private key
- `CLIENT_EMAIL` - Service account email

**To set properties**:
1. In Apps Script: **Project Settings** → **Script properties**
2. Add each property as key-value pairs

## 📝 Android App Updates

### If Using Separate Deployments:

1. **Update `ImageRepository.kt`**:
   ```kotlin
   .baseUrl("https://script.google.com/macros/s/YOUR_IMAGE_UPLOAD_DEPLOYMENT_ID/")
   ```

2. **Update `NetworkModule.kt`**:
   ```kotlin
   private const val EMPLOYEES_SYNC_BASE_URL =
       "https://script.google.com/macros/s/YOUR_EMPLOYEE_SYNC_DEPLOYMENT_ID/"
   ```

### If Using Combined Deployment:

Both services use the same URL, so no changes needed!

## ✅ Testing

### Test Image Upload:
```bash
POST https://your-deployment-url/exec?action=uploadImage
Content-Type: application/json

{
  "image": "data:image/jpeg;base64,/9j/4AAQ...",
  "filename": "1953036.jpg"
}
```

### Test Employee Sync:
```bash
GET https://your-deployment-url/exec?action=getEmployees
GET https://your-deployment-url/exec?action=pullDataFromFirebase
GET https://your-deployment-url/exec?action=pushDataToFirebase
```

## 🎯 Recommended Approach

**For Production**: Use **Option 1 (Separate Deployments)**
- Better separation of concerns
- Independent scaling
- Easier debugging
- Can update services independently

**For Development**: Use **Option 2 (Combined)**
- Faster setup
- Single URL to manage
- Easier testing

## 📋 Checklist

- [ ] Choose deployment option (separate or combined)
- [ ] Create Apps Script project(s)
- [ ] Paste code into file(s)
- [ ] Set Script Properties
- [ ] Deploy as Web app
- [ ] Set "Execute as: Me"
- [ ] Set "Who has access: Anyone"
- [ ] Copy deployment URL(s)
- [ ] Update Android app with new URL(s)
- [ ] Test image upload
- [ ] Test employee sync
- [ ] Verify logs in Apps Script Executions

Both files are **fully functional** and **ready to deploy**! 🚀



