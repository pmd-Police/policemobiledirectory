# Employee Sync URL Updated

## ✅ Changes Made

### Updated Employee Sync URL
- **Old URL**: `AKfycbwb9MxCHDnshxEe6Vw_sXvCj3pkNqAIz5emGZiZ6CiJlk-CqeHqUSBIEmjhIeJavPwUkA/`
- **New URL**: `AKfycbxBKIOzMZmCTpgQnmUlr0Hg-mWkZcQjIoryfq7C_jJ0fl5KtbJV0axxv4XnmcUuoqCs3w/`

### Location
- File: `app/src/main/java/com/example/policemobiledirectory/di/NetworkModule.kt`
- Constant: `EMPLOYEES_SYNC_BASE_URL`
- Used by: `SyncApiService` for employee Sheet ↔ Firestore sync

## 📋 Required Script Actions

Your Google Apps Script must support these actions:

1. **`syncSheetToFirebase`** - Syncs data from Google Sheet → Firestore
   - URL: `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=syncSheetToFirebase`
   - Used when: Admin clicks "Sync Sheet to Firebase" in app

2. **`syncFirebaseToSheet`** - Syncs data from Firestore → Google Sheet  
   - URL: `https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=syncFirebaseToSheet`
   - Used when: Admin clicks "Sync Firebase to Sheet" in app

## 🔧 Script Setup Required

Make sure your Google Apps Script has:

```javascript
function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return ContentService.createTextOutput(JSON.stringify({ error: "No parameters. Use ?action=..." }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    const action = e.parameter.action;
    
    switch(action) {
      case "syncSheetToFirebase":
        return ContentService.createTextOutput(JSON.stringify(syncSheetToFirebase()))
          .setMimeType(ContentService.MimeType.JSON);
        
      case "syncFirebaseToSheet":
        return ContentService.createTextOutput(JSON.stringify(syncFirebaseToSheet()))
          .setMimeType(ContentService.MimeType.JSON);
        
      default:
        return ContentService.createTextOutput(JSON.stringify({ 
          error: "Invalid action",
          available: ["syncSheetToFirebase", "syncFirebaseToSheet"]
        }))
          .setMimeType(ContentService.MimeType.JSON);
    }
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return ContentService.createTextOutput(JSON.stringify({ error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
```

## 📝 Field Mapping

Ensure your script maps Google Sheet columns to Firestore fields correctly:

| Google Sheet Column | Firestore Field | Employee.kt Property |
|---------------------|-----------------|---------------------|
| kgid | kgid | kgid |
| Name | name | name |
| email | email | email |
| mobile1 | mobile1 | mobile1 |
| mobil | mobile2 | mobile2 |
| rank | rank | rank |
| metal | metal | metalNumber (with @PropertyName) |
| District | district | district |
| Station | station | station |
| bloodGroup | bloodGroup | bloodGroup |
| photoUrl | photoUrl | photoUrl |
| photoUrlFromGoog | photoUrlFromGoogle | photoUrlFromGoogle |
| fcmToken | fcmToken | fcmToken |
| firebaseUid | firebaseUid | firebaseUid |
| isAdmin | isAdmin | isAdmin |
| isApproved | isApproved | isApproved |
| pin | pin | pin |
| createdAt | createdAt | createdAt |
| updatedAt | updatedAt | updatedAt |

## ✅ Next Steps

1. **Deploy your script** with the required actions
2. **Test the URL** manually: 
   ```
   https://script.google.com/macros/s/AKfycbxBKIOzMZmCTpgQnmUlr0Hg-mWkZcQjIoryfq7C_jJ0fl5KtbJV0axxv4XnmcUuoqCs3w/exec?action=syncSheetToFirebase
   ```
3. **Rebuild the app** to use the new URL
4. **Test sync** from the Admin Panel in the app

## 🔍 Troubleshooting

If you see "Invalid action" error:
- Check that your `doGet(e)` function handles the action parameter
- Verify the script is deployed as a web app with access to "Anyone"
- Ensure the action names match exactly: `syncSheetToFirebase` and `syncFirebaseToSheet`








