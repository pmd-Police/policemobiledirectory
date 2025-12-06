/**
 * ✅ FIXED: Profile Image Upload Handler
 * Always returns JSON (never HTML error pages)
 */

// Set your Drive folder ID here
const DRIVE_FOLDER_ID = "YOUR_DRIVE_FOLDER_ID"; // Replace with your actual folder ID

/**
 * ✅ FIXED doPost - Always returns JSON
 */
function doPost(e) {
  try {
    // Get action from query parameter
    const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : null;
    
    if (!action) {
      return jsonResponse({ 
        success: false, 
        error: "Missing action parameter. Use ?action=uploadImage" 
      }, 400);
    }
    
    Logger.log('doPost called with action: ' + action);
    
    // Route to appropriate handler
    if (action === "uploadImage") {
      return uploadProfileImage(e);
    }
    
    // Unknown action
    return jsonResponse({ 
      success: false, 
      error: "Unknown action: " + action 
    }, 400);
      
  } catch (error) {
    Logger.log('Error in doPost: ' + error.toString());
    // ✅ IMPORTANT: Always return JSON, never let Apps Script return HTML error
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + error.toString() 
    }, 500);
  }
}

/**
 * ✅ FIXED: Upload profile image - Always returns JSON
 */
function uploadProfileImage(e) {
  try {
    Logger.log('uploadProfileImage called');
    
    if (!e || !e.postData) {
      return jsonResponse({ 
        success: false, 
        error: "No POST data received" 
      }, 400);
    }
    
    // Get the request body
    let body = "";
    try {
      body = e.postData.contents || "";
    } catch (err) {
      Logger.log("Error reading postData.contents: " + err);
      body = "";
    }
    
    if (!body || body.trim().length === 0) {
      return jsonResponse({ 
        success: false, 
        error: "Empty request body" 
      }, 400);
    }
    
    Logger.log('Request body length: ' + body.length);
    Logger.log('First 100 chars: ' + body.substring(0, 100));
    
    // Try to parse as JSON
    let jsonData = null;
    try {
      const trimmed = body.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        jsonData = JSON.parse(trimmed);
        Logger.log('✅ JSON parsed successfully');
      } else {
        return jsonResponse({ 
          success: false, 
          error: "Invalid JSON format. Expected JSON object with 'image' field." 
        }, 400);
      }
    } catch (parseErr) {
      Logger.log('JSON parse error: ' + parseErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Failed to parse JSON: " + parseErr.toString() 
      }, 400);
    }
    
    // Extract image data
    if (!jsonData || !jsonData.image) {
      return jsonResponse({ 
        success: false, 
        error: "Missing 'image' field in JSON" 
      }, 400);
    }
    
    const base64Image = jsonData.image;
    const filename = jsonData.filename || ("employee_" + new Date().getTime() + ".jpg");
    const kgid = jsonData.kgid || e.parameter.kgid || null;
    
    // Extract base64 data (remove data:image/jpeg;base64, prefix if present)
    let base64 = base64Image;
    if (base64.indexOf(",") >= 0) {
      base64 = base64.split(",")[1];
    }
    
    // Decode base64 to bytes
    let imageBytes;
    try {
      imageBytes = Utilities.base64Decode(base64);
      Logger.log('Base64 decoded to ' + imageBytes.length + ' bytes');
    } catch (decodeErr) {
      Logger.log('Base64 decode error: ' + decodeErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Invalid base64 image data" 
      }, 400);
    }
    
    if (!imageBytes || imageBytes.length === 0) {
      return jsonResponse({ 
        success: false, 
        error: "Image data is empty after decoding" 
      }, 400);
    }
    
    // Create blob
    const blob = Utilities.newBlob(imageBytes, "image/jpeg", filename);
    
    // Get Drive folder
    let folder;
    try {
      folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    } catch (folderErr) {
      Logger.log('Drive folder error: ' + folderErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Drive folder not found. Check DRIVE_FOLDER_ID." 
      }, 500);
    }
    
    // Create file in Drive
    const file = folder.createFile(blob);
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('File created: ' + fileId);
    Logger.log('Public URL: ' + driveUrl);
    
    // Update sheet/Firestore if kgid provided (add your own functions here)
    if (kgid) {
      Logger.log('Updating sheet/Firestore for kgid: ' + kgid);
      // Uncomment and implement if needed:
      // updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      // updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    // ✅ Return success response
    return jsonResponse({
      success: true,
      url: driveUrl,
      id: fileId,
      message: "Image uploaded successfully"
    }, 200);
    
  } catch (error) {
    Logger.log('Error in uploadProfileImage: ' + error.toString());
    Logger.log('Stack trace: ' + error.stack);
    
    // ✅ Always return JSON error, never HTML
    return jsonResponse({ 
      success: false, 
      error: "Upload failed: " + error.toString() 
    }, 500);
  }
}

/**
 * ✅ Helper function to create JSON response with proper MIME type
 */
function jsonResponse(data, statusCode) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
