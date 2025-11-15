# Google Apps Script Setup for Image Upload

## ⚠️ CRITICAL ERROR: "Script function not found: doPost"

**Your Apps Script is missing the `doPost()` function!**

The error message from your server shows:
```
Script function not found: doPost
```

This means your Apps Script web app doesn't have a `doPost()` function to handle POST requests.

## ⚠️ HTML Response Error Fix Guide

If you're getting "Server returned HTML instead of JSON", this means your Apps Script web app needs to be configured correctly.

## 🔧 Required Apps Script Configuration

### 1. **Web App Deployment Settings**

In your Google Apps Script:
1. Click **Deploy** → **Manage deployments**
2. Click **Edit** (pencil icon) on your current deployment
3. Make sure these settings are correct:
   - **Execute as:** `Me (your-email@gmail.com)`
   - **Who has access:** `Anyone` (not "Anyone with Google account")
4. Click **Deploy**

### 2. **Apps Script Code Structure**

**⚠️ YOU MUST CREATE A `doPost()` FUNCTION!**

Your Apps Script **MUST** have a `doPost()` function to handle POST requests. Here's the complete code:

```javascript
/**
 * ✅ REQUIRED: Handle POST requests (multipart/form-data)
 * This function is called when your app sends a POST request
 */
function doPost(e) {
  try {
    // Get the action parameter from query string
    const action = e.parameter.action;
    
    Logger.log('Received POST request with action: ' + action);
    
    if (action === 'uploadImage') {
      return handleImageUpload(e);
    }
    
    // Unknown action
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: 'Unknown action: ' + action
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    Logger.log('Error in doPost: ' + error.toString());
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * ✅ Handle image upload from multipart/form-data
 */
function handleImageUpload(e) {
  try {
    Logger.log('Handling image upload...');
    
    // Get the file from multipart form data
    // The file comes as a blob in e.postData.contents or e.parameter.file
    let blob;
    
    if (e.postData && e.postData.contents) {
      // Parse multipart form data
      const boundary = e.postData.type.split('boundary=')[1];
      const parts = e.postData.contents.split('--' + boundary);
      
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i];
        if (part.includes('name="file"')) {
          // Extract file data (skip headers)
          const fileData = part.split('\r\n\r\n')[1];
          if (fileData) {
            blob = Utilities.newBlob(
              Utilities.base64Decode(fileData.split('--')[0].trim()),
              'image/jpeg',
              'upload.jpg'
            );
            break;
          }
        }
      }
    } else if (e.parameter.file) {
      blob = e.parameter.file;
    }
    
    if (!blob) {
      throw new Error('No file data received');
    }
    
    Logger.log('File received, size: ' + blob.getBytes().length);
    
    // ✅ REPLACE 'YOUR_FOLDER_ID' with your actual Google Drive folder ID
    // To get folder ID: Open folder in Drive → URL shows folder ID
    const folderId = 'YOUR_FOLDER_ID'; // ⚠️ CHANGE THIS!
    const folder = DriveApp.getFolderById(folderId);
    
    // Create file in Google Drive
    const fileName = 'employee_' + new Date().getTime() + '.jpg';
    const file = folder.createFile(blob.setName(fileName));
    
    Logger.log('File created in Drive: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE, DriveApp.Permission.VIEW);
    
    // Get public URL
    const fileId = file.getId();
    const publicUrl = 'https://drive.google.com/uc?export=view&id=' + fileId;
    
    Logger.log('Public URL: ' + publicUrl);
    
    // Return JSON response
    return ContentService.createTextOutput(JSON.stringify({
      success: true,
      url: publicUrl,
      id: fileId,
      error: null
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    Logger.log('Error in handleImageUpload: ' + error.toString());
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
```

**⚠️ IMPORTANT STEPS:**

1. **Open your Google Apps Script project**
2. **Paste the code above** (replace any existing code)
3. **Replace `YOUR_FOLDER_ID`** with your actual Google Drive folder ID:
   - Open Google Drive
   - Create or select a folder for employee photos
   - Open the folder
   - Look at the URL: `https://drive.google.com/drive/folders/FOLDER_ID_HERE`
   - Copy the `FOLDER_ID_HERE` part
4. **Save the script** (Ctrl+S or Cmd+S)
5. **Deploy as Web App** (see step 1 above)
6. **Test the deployment**

### 3. **Important Notes**

⚠️ **Always return JSON with `setMimeType(ContentService.MimeType.JSON)`**

❌ **Don't do this:**
```javascript
return HtmlService.createHtmlOutput("Error message");
```

✅ **Do this instead:**
```javascript
return ContentService.createTextOutput(JSON.stringify({
  success: false,
  error: "Error message"
})).setMimeType(ContentService.MimeType.JSON);
```

### 4. **Testing Your Script**

Test your endpoint manually:
1. Use Postman or curl to send a multipart POST request:
```
POST https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=uploadImage
Content-Type: multipart/form-data
file: [your image file]
```

2. Check the response - it should be JSON, not HTML

### 5. **Common Issues**

- **HTML Response:** Deployment settings wrong or error in script
- **Empty Response:** Script timed out or crashed
- **403 Forbidden:** Access permissions wrong
- **404 Not Found:** URL or action parameter wrong

### 6. **Check Execution Logs**

In Apps Script:
1. Click **Executions** (clock icon) to see execution history
2. Click on failed executions to see error details
3. Fix errors and redeploy

## 📱 Your Script URL

```
https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec
```

**✅ Updated in Android app on:** 2025-11-15

Make sure this URL is deployed with correct settings!

