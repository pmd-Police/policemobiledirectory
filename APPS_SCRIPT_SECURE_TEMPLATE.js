/********** SECURE APPS SCRIPT TEMPLATE **********/
/**
 * ⚠️ SECURITY: This template includes authentication and validation
 * Replace all instances of YOUR_SECRET_TOKEN with a strong random string
 * Generate using: openssl rand -hex 32
 */

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/

const SHEET_ID = "YOUR_SHEET_ID_HERE";
const SHEET_NAME = "Emp Profiles";
const DRIVE_FOLDER_ID = "YOUR_DRIVE_FOLDER_ID_HERE";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "YOUR_FIREBASE_API_KEY_HERE";

// ⚠️ CRITICAL: Generate a strong secret token (32+ characters)
// Use: openssl rand -hex 32
const SECRET_TOKEN = "YOUR_SECRET_TOKEN_HERE";

// Maximum file size (5MB)
const MAX_FILE_SIZE = 5 * 1024 * 1024;

// Rate limiting: max uploads per hour per IP/user
const MAX_UPLOADS_PER_HOUR = 10;

/** ------------------------------------------------------ **/

/** Utility: safe json response **/
function jsonResponse(obj, statusCode = 200) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

/** Utility: verify secret token **/
function verifyToken(e) {
  const token = e.parameter.token || (e.postData && JSON.parse(e.postData.contents).token);
  if (!token || token !== SECRET_TOKEN) {
    return jsonResponse({ 
      success: false, 
      error: "Unauthorized: Invalid or missing token" 
    }, 401);
  }
  return null; // Token is valid
}

/** Utility: verify admin status in Firestore **/
function verifyAdmin(email) {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/admins/${encodeURIComponent(email)}?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(url);
    if (response.getResponseCode() === 200) {
      const data = JSON.parse(response.getContentText());
      return data.fields && data.fields.isActive && data.fields.isActive.booleanValue === true;
    }
  } catch (e) {
    Logger.log("Admin verification failed: " + e);
  }
  return false;
}

/** Utility: rate limiting check **/
function checkRateLimit(identifier) {
  const cache = CacheService.getScriptCache();
  const key = `rate_limit_${identifier}`;
  const count = parseInt(cache.get(key) || "0");
  
  if (count >= MAX_UPLOADS_PER_HOUR) {
    return false;
  }
  
  cache.put(key, (count + 1).toString(), 3600); // 1 hour
  return true;
}

/** Utility: validate image **/
function validateImage(base64Data, filename) {
  // Check file size
  const sizeBytes = Utilities.base64Decode(base64Data).length;
  if (sizeBytes > MAX_FILE_SIZE) {
    return { valid: false, error: "File size exceeds 5MB limit" };
  }
  
  // Check file extension
  const ext = filename.toLowerCase().split('.').pop();
  if (!['jpg', 'jpeg', 'png'].includes(ext)) {
    return { valid: false, error: "Invalid file type. Only JPEG/PNG allowed" };
  }
  
  // Verify JPEG/PNG header
  const bytes = Utilities.base64Decode(base64Data);
  if (bytes.length < 2) {
    return { valid: false, error: "Invalid image file" };
  }
  
  // JPEG: FF D8
  // PNG: 89 50 4E 47
  const isJpeg = bytes[0] === 0xFF && bytes[1] === 0xD8;
  const isPng = bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47;
  
  if (!isJpeg && !isPng) {
    return { valid: false, error: "Invalid image format. Only JPEG/PNG allowed" };
  }
  
  return { valid: true };
}

/** Main POST handler **/
function doPost(e) {
  try {
    // 1. Verify token
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    const action = e.parameter.action;
    if (!action) {
      return jsonResponse({ success: false, error: "Missing action parameter" }, 400);
    }
    
    // 2. Route to appropriate handler
    switch (action) {
      case "uploadImage":
        return handleImageUpload(e);
      case "adminAction":
        return handleAdminAction(e);
      default:
        return jsonResponse({ success: false, error: "Invalid action" }, 400);
    }
  } catch (err) {
    Logger.log("doPost error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + err.toString() 
    }, 500);
  }
}

/** Handle image upload with validation **/
function handleImageUpload(e) {
  try {
    // Parse request
    const postData = e.postData ? JSON.parse(e.postData.contents) : {};
    const imageData = postData.image || "";
    const filename = postData.filename || "";
    const kgid = postData.kgid || "";
    const userEmail = postData.userEmail || "";
    
    // Rate limiting (by email or IP)
    const rateLimitId = userEmail || e.parameter.ip || "unknown";
    if (!checkRateLimit(rateLimitId)) {
      return jsonResponse({ 
        success: false, 
        error: "Rate limit exceeded. Maximum 10 uploads per hour." 
      }, 429);
    }
    
    // Extract base64
    let base64 = imageData.includes(",") ? imageData.split(",")[1] : imageData;
    
    // Validate image
    const validation = validateImage(base64, filename);
    if (!validation.valid) {
      return jsonResponse({ success: false, error: validation.error }, 400);
    }
    
    // Verify user owns the KGID (optional but recommended)
    // This would require additional Firestore lookup
    
    // Process upload (your existing logic)
    const bytes = Utilities.base64Decode(base64);
    const blob = Utilities.newBlob(bytes, "image/jpeg", filename);
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    const file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const imageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    // Update Sheet and Firestore (your existing logic)
    // ... rest of upload logic ...
    
    return jsonResponse({ 
      success: true, 
      url: imageUrl, 
      id: fileId 
    });
    
  } catch (err) {
    Logger.log("Image upload error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Upload failed: " + err.toString() 
    }, 500);
  }
}

/** Handle admin actions with verification **/
function handleAdminAction(e) {
  try {
    const postData = e.postData ? JSON.parse(e.postData.contents) : {};
    const userEmail = postData.userEmail || e.parameter.email;
    
    if (!userEmail) {
      return jsonResponse({ success: false, error: "Missing user email" }, 400);
    }
    
    // Verify admin status in Firestore
    if (!verifyAdmin(userEmail)) {
      return jsonResponse({ 
        success: false, 
        error: "Unauthorized: Admin access required" 
      }, 403);
    }
    
    // Process admin action
    // ... your admin action logic ...
    
    return jsonResponse({ success: true });
    
  } catch (err) {
    Logger.log("Admin action error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Action failed: " + err.toString() 
    }, 500);
  }
}

/** GET handler (for testing) **/
function doGet(e) {
  return jsonResponse({ 
    error: "Use POST method with action parameter",
    availableActions: ["uploadImage", "adminAction"]
  });
}

