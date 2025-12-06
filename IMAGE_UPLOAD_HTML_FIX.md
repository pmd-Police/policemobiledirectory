# Image Upload Returning HTML Instead of JSON - Fix

## Issue
The server is returning HTML instead of JSON when uploading profile images. This typically happens when:
1. The Google Apps Script web app is not deployed correctly
2. The `doPost()` function has an error and Apps Script returns an HTML error page
3. The action parameter isn't being handled correctly

## Root Cause
When Google Apps Script encounters an error or isn't properly configured, it returns an HTML error page instead of the expected JSON response.

## Solutions

### Solution 1: Verify Apps Script Deployment (Most Common Fix)

1. **Go to Google Apps Script**: https://script.google.com
2. **Open your project** (the one with URL: `AKfycbw3BybPar7IpUPm10nEDlT1UEbYMTiMsDvnxQyv9l3sf916Mk9DuDZcc4u_h8DV7vSI9w`)
3. **Deploy → Manage deployments**
4. **Click Edit (pencil icon)** on your existing deployment
5. **Verify these settings**:
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone`
   - Click **Deploy**

### Solution 2: Update doPost Function

Ensure your `doPost()` function properly handles the `uploadImage` action:

```javascript
function doPost(e) {
  try {
    // Get action from query parameter
    const action = e.parameter ? e.parameter.action : null;
    
    if (!action) {
      return ContentService
        .createTextOutput(JSON.stringify({ 
          success: false, 
          error: "Missing action parameter. Use ?action=uploadImage" 
        }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    
    Logger.log('doPost called with action: ' + action);
    
    // Route to appropriate handler
    if (action === "uploadImage") {
      return uploadProfileImage(e);
    }
    
    // Unknown action
    return ContentService
      .createTextOutput(JSON.stringify({ 
        success: false, 
        error: "Unknown action: " + action 
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    Logger.log('Error in doPost: ' + error.toString());
    // ✅ IMPORTANT: Always return JSON, never let Apps Script return HTML error
    return ContentService
      .createTextOutput(JSON.stringify({ 
        success: false, 
        error: error.toString() 
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
```

### Solution 3: Ensure uploadProfileImage Returns JSON

Make sure your `uploadProfileImage()` function always returns JSON:

```javascript
function uploadProfileImage(e) {
  try {
    // Your upload logic here...
    
    // ✅ ALWAYS use ContentService.createTextOutput with JSON
    return ContentService
      .createTextOutput(JSON.stringify({
        success: true,
        url: driveUrl,
        id: fileId
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    Logger.log('Error in uploadProfileImage: ' + error.toString());
    // ✅ Return JSON error, not HTML
    return ContentService
      .createTextOutput(JSON.stringify({
        success: false,
        error: error.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}
```

### Solution 4: Test the Endpoint

Test your endpoint manually:

```bash
# Test with curl
curl -X POST "https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=uploadImage" \
  -H "Content-Type: application/json" \
  -d '{"image":"data:image/jpeg;base64,/9j/4AAQ...","filename":"test.jpg"}'
```

**Expected Response**: JSON like `{"success":true,"url":"..."}`

**If you get HTML**: The deployment or doPost function needs to be fixed.

### Solution 5: Check Apps Script Execution Logs

1. **Open Apps Script editor**
2. **View → Execution log** (or run a test execution)
3. **Look for errors** in the uploadProfileImage function
4. **Fix any errors** found in the logs

## Quick Diagnostic Steps

1. ✅ **Check Deployment Settings** (Solution 1)
2. ✅ **Verify doPost handles action parameter** (Solution 2)
3. ✅ **Test endpoint manually** (Solution 4)
4. ✅ **Check execution logs** (Solution 5)

## Prevention

- Always wrap functions in try-catch
- Always return JSON using `ContentService.createTextOutput().setMimeType(ContentService.MimeType.JSON)`
- Never throw errors that would cause Apps Script to return HTML error pages
- Test deployments after any code changes

## Android App Error Handling

The app already handles HTML responses and shows a helpful error message. However, the fix must be on the server side (Apps Script).
