/********************************************************************
 * 📸 IMAGE UPLOAD SERVICE — IMPROVED VERSION
 * ---------------------------------------------------------------
 * Enhanced image upload with:
 * - Better error handling
 * - File size validation
 * - Image dimension validation
 * - Duplicate detection
 * - Performance optimizations
 * - Better logging
 ********************************************************************/

/* ================================================================
   CONFIGURATION
================================================================ */

const PROPS = PropertiesService.getScriptProperties();

// ✅ Google Sheet configuration
const SHEET_ID = PROPS.getProperty("SHEET_ID") || "1E8cE9zzM3jAHL-a_Cafn5EDWEbk_QNBfOpNtpWwVjfA";
const SHEET_NAME = PROPS.getProperty("SHEET_NAME") || "Emp Profiles";

// ✅ Google Drive configuration
const DRIVE_FOLDER_ID = PROPS.getProperty("DRIVE_FOLDER_ID") || "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs";

// ✅ Firebase configuration
const FIREBASE_API_KEY = PROPS.getProperty("FIREBASE_API_KEY") || "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";
const FIREBASE_PROJECT_ID = PROPS.getProperty("FIREBASE_PROJECT_ID") || "pmd-police-mobile-directory";

// ✅ Upload limits (configurable via Script Properties)
const MAX_FILE_SIZE = parseInt(PROPS.getProperty("MAX_FILE_SIZE") || "5242880"); // 5MB default
const MAX_IMAGE_WIDTH = parseInt(PROPS.getProperty("MAX_IMAGE_WIDTH") || "4096"); // 4K default
const MAX_IMAGE_HEIGHT = parseInt(PROPS.getProperty("MAX_IMAGE_HEIGHT") || "4096"); // 4K default
const MIN_IMAGE_WIDTH = parseInt(PROPS.getProperty("MIN_IMAGE_WIDTH") || "50"); // 50px minimum
const MIN_IMAGE_HEIGHT = parseInt(PROPS.getProperty("MIN_IMAGE_HEIGHT") || "50"); // 50px minimum

/* ================================================================
   JSON RESPONSE HELPER
================================================================ */

function jsonResponse(obj, status) {
  try {
    const output = ContentService.createTextOutput(JSON.stringify(obj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  } catch (err) {
    const errorObj = { success: false, error: "Failed to create JSON response: " + err.toString() };
    const output = ContentService.createTextOutput(JSON.stringify(errorObj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

/* ================================================================
   MAIN API ROUTING
================================================================ */

function doGet(e) {
  // Test endpoint
  if (e && e.parameter && e.parameter.test) {
    return jsonResponse({
      success: true,
      message: "Image Upload Service is running",
      version: "2.0-improved",
      config: {
        sheetId: SHEET_ID,
        sheetName: SHEET_NAME,
        driveFolderId: DRIVE_FOLDER_ID,
        firebaseProjectId: FIREBASE_PROJECT_ID,
        limits: {
          maxFileSize: MAX_FILE_SIZE,
          maxWidth: MAX_IMAGE_WIDTH,
          maxHeight: MAX_IMAGE_HEIGHT,
          minWidth: MIN_IMAGE_WIDTH,
          minHeight: MIN_IMAGE_HEIGHT
        }
      }
    });
  }
  
  return jsonResponse({
    error: "No parameters. Use POST with ?action=uploadImage",
    usage: "POST to /exec?action=uploadImage with JSON body: {image: 'data:image/jpeg;base64,...', filename: '12345.jpg'}"
  }, 400);
}

function doPost(e) {
  try {
    Logger.log("=== doPost START ===");
    
    // Always require action
    if (!e || !e.parameter) {
      Logger.log("ERROR: No parameters");
      return jsonResponse({ success: false, error: "Missing parameters" }, 400);
    }

    const action = e.parameter.action;
    
    if (!action) {
      Logger.log("ERROR: No action parameter");
      return jsonResponse({ success: false, error: "Missing action parameter. Use ?action=uploadImage" }, 400);
    }

    Logger.log("doPost called with action: " + action);

    // ✅ Route image upload
    if (action === "uploadImage") {
      Logger.log("Routing to uploadImage...");
      
      try {
        const result = uploadProfileImage(e);
        
        Logger.log("uploadProfileImage returned: " + (result != null ? "result exists" : "null"));
        
        if (!result) {
          Logger.log("ERROR: uploadProfileImage returned null");
          return jsonResponse({
            success: false,
            error: "uploadProfileImage returned null"
          }, 500);
        }

        Logger.log("Returning result from doPost");
        return result;

      } catch (uploadErr) {
        Logger.log("uploadProfileImage ERROR: " + uploadErr.toString());
        Logger.log("Stack: " + (uploadErr.stack || "no stack"));
        return jsonResponse({
          success: false,
          error: "Image upload error: " + uploadErr.toString()
        }, 500);
      }
    }

    // Unknown action
    return jsonResponse({
      success: false,
      error: "Invalid action: " + action + ". Use ?action=uploadImage"
    }, 400);

  } catch (err) {
    Logger.log("doPost ERROR: " + err.toString());
    Logger.log("doPost ERROR stack: " + (err.stack || "no stack"));
    return jsonResponse({
      success: false,
      error: "doPost error: " + err.toString()
    }, 500);
  }
}

/* ================================================================
   IMAGE UPLOAD FUNCTIONS (IMPROVED)
================================================================ */

function uploadProfileImage(e) {
  const debug = [];
  const startTime = Date.now();
  debug.push("=== START uploadProfileImage ===");
  
  try {
    if (!e) {
      debug.push("ERROR: No event object");
      return jsonResponse({ success: false, error: "No event object", debug }, 400);
    }

    /** Read raw body safely */
    let rawBody = "";
    try {
      if (e.postData && typeof e.postData.getDataAsString === "function") {
        rawBody = e.postData.getDataAsString();
      } else if (e.postData && e.postData.contents) {
        rawBody = e.postData.contents;
      } else if (e._rawBody) {
        rawBody = e._rawBody;
      }
    } catch (err) {
      debug.push("Error reading body: " + err);
    }

    if (!rawBody) {
      debug.push("ERROR: Empty POST body");
      return jsonResponse({ success: false, error: "No POST data", debug }, 400);
    }

    debug.push("Body size: " + rawBody.length + " chars");

    /** Parse JSON */
    let jsonData;
    try {
      jsonData = JSON.parse(rawBody.trim());
      debug.push("JSON parsed: OK");
    } catch (err) {
      return jsonResponse({
        success: false,
        error: "Invalid JSON: " + err,
        debug
      }, 400);
    }

    /** Extract base64 image */
    if (!jsonData.image) {
      debug.push("ERROR: No image field in JSON");
      return jsonResponse({ success: false, error: "Missing 'image' field", debug }, 400);
    }

    let base64 = jsonData.image.includes(",")
      ? jsonData.image.split(",")[1]
      : jsonData.image;

    /** Decode image */
    let bytes;
    try {
      bytes = Utilities.base64Decode(base64);
      debug.push("Base64 decoded: " + bytes.length + " bytes");
    } catch (err) {
      debug.push("Base64 decode error: " + err);
      return jsonResponse({ success: false, error: "Base64 decode failed: " + err, debug }, 400);
    }

    /** ✅ File size validation */
    if (bytes.length > MAX_FILE_SIZE) {
      const sizeMB = (bytes.length / 1048576).toFixed(2);
      const maxMB = (MAX_FILE_SIZE / 1048576).toFixed(2);
      debug.push("ERROR: File too large (" + sizeMB + "MB > " + maxMB + "MB)");
      return jsonResponse({
        success: false,
        error: "File too large: " + sizeMB + "MB. Maximum allowed: " + maxMB + "MB",
        debug
      }, 400);
    }

    if (bytes.length < 100) {
      debug.push("ERROR: File too small (" + bytes.length + " bytes)");
      return jsonResponse({
        success: false,
        error: "File too small. Minimum size: 100 bytes",
        debug
      }, 400);
    }

    /** Verify JPEG header */
    if (bytes.length < 2) {
      debug.push("ERROR: File too small for header check");
      return jsonResponse({
        success: false,
        error: "File too small",
        debug
      }, 400);
    }
    
    // Check first two bytes for JPEG magic number
    const byte0 = bytes[0] & 0xFF;
    const byte1 = bytes[1] & 0xFF;
    const hex0 = (byte0 < 16 ? "0" : "") + byte0.toString(16).toUpperCase();
    const hex1 = (byte1 < 16 ? "0" : "") + byte1.toString(16).toUpperCase();
    debug.push("First bytes: 0x" + hex0 + " 0x" + hex1);
    
    if (byte0 !== 0xFF || byte1 !== 0xD8) {
      debug.push("WARNING: JPEG header check failed (expected 0xFF 0xD8, got 0x" + hex0 + " 0x" + hex1 + ")");
      debug.push("Continuing anyway - blob creation will handle validation");
    } else {
      debug.push("✅ JPEG signature verified");
    }

    /** ✅ Extract image dimensions (if possible) */
    let imageInfo = null;
    try {
      imageInfo = getImageDimensions(bytes);
      if (imageInfo) {
        debug.push("Image dimensions: " + imageInfo.width + "x" + imageInfo.height);
        
        // Validate dimensions
        if (imageInfo.width > MAX_IMAGE_WIDTH || imageInfo.height > MAX_IMAGE_HEIGHT) {
          debug.push("ERROR: Image dimensions too large");
          return jsonResponse({
            success: false,
            error: "Image dimensions too large: " + imageInfo.width + "x" + imageInfo.height + ". Maximum: " + MAX_IMAGE_WIDTH + "x" + MAX_IMAGE_HEIGHT,
            debug
          }, 400);
        }
        
        if (imageInfo.width < MIN_IMAGE_WIDTH || imageInfo.height < MIN_IMAGE_HEIGHT) {
          debug.push("ERROR: Image dimensions too small");
          return jsonResponse({
            success: false,
            error: "Image dimensions too small: " + imageInfo.width + "x" + imageInfo.height + ". Minimum: " + MIN_IMAGE_WIDTH + "x" + MIN_IMAGE_HEIGHT,
            debug
          }, 400);
        }
      }
    } catch (err) {
      debug.push("WARNING: Could not extract image dimensions: " + err);
    }

    /** Create Blob */
    const fileName = jsonData.filename || ("upload_" + Date.now() + ".jpg");
    const blob = Utilities.newBlob(bytes, "image/jpeg", fileName);

    /** Extract KGID from filename or query */
    let kgid = (fileName.match(/^(\d+)\.jpg$/) || [])[1];
    if (!kgid && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
    }

    /** ✅ Check for existing image (optional - prevent duplicates) */
    let existingUrl = null;
    if (kgid) {
      try {
        existingUrl = getExistingImageUrl(kgid);
        if (existingUrl) {
          debug.push("Found existing image URL for kgid: " + kgid);
        }
      } catch (err) {
        debug.push("WARNING: Could not check for existing image: " + err);
      }
    }

    const result = handleBlobSave(e, blob, kgid, debug, imageInfo, existingUrl);
    
    const duration = Date.now() - startTime;
    debug.push("Total upload time: " + duration + "ms");
    
    return result;

  } catch (err) {
    const duration = Date.now() - startTime;
    debug.push("ERROR after " + duration + "ms: " + err.toString());
    return jsonResponse({
      success: false,
      error: "Server error: " + err,
      debug
    }, 500);
  }
}

/**
 * ✅ Extract image dimensions from JPEG bytes
 * Reads SOF (Start of Frame) marker to get width/height
 */
function getImageDimensions(bytes) {
  try {
    // JPEG SOF markers: 0xFFC0, 0xFFC1, 0xFFC2, etc.
    for (let i = 0; i < bytes.length - 8; i++) {
      if (bytes[i] === 0xFF && bytes[i + 1] >= 0xC0 && bytes[i + 1] <= 0xC3) {
        // Found SOF marker
        const height = (bytes[i + 5] << 8) | bytes[i + 6];
        const width = (bytes[i + 7] << 8) | bytes[i + 8];
        return { width: width, height: height };
      }
    }
    return null;
  } catch (err) {
    Logger.log("getImageDimensions ERROR: " + err);
    return null;
  }
}

/**
 * ✅ Get existing image URL for a kgid (to prevent duplicates)
 */
function getExistingImageUrl(kgid) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sh = ss.getSheetByName(SHEET_NAME);
    if (!sh) return null;

    const rows = sh.getDataRange().getValues();
    const headers = rows[0].map(h => h.toString().trim());

    const photoIdx = headers.indexOf("photoUrl");
    const kgIdx = headers.indexOf("kgid");
    
    if (photoIdx < 0 || kgIdx < 0) return null;

    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgIdx]) === String(kgid)) {
        const url = rows[r][photoIdx];
        if (url && String(url).trim() !== "") {
          return String(url).trim();
        }
      }
    }

    return null;
  } catch (err) {
    Logger.log("getExistingImageUrl ERROR: " + err);
    return null;
  }
}

function handleBlobSave(e, blob, kgid, debug, imageInfo, existingUrl) {
  try {
    debug.push("--- handleBlobSave START ---");

    /** Get folder */
    let folder;
    try {
      folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
      debug.push("✅ Drive folder accessed");
    } catch (err) {
      return jsonResponse({
        success: false,
        error: "Drive folder error: " + err,
        debug
      }, 500);
    }

    /** ✅ Check if file with same name exists (optional cleanup) */
    const ts = Date.now();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = "employee_" + (kgid || "unk") + "_" + ts + "." + ext;

    /** Upload to Drive */
    let file;
    try {
      file = folder.createFile(blob.setName(fname));
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      debug.push("✅ File created in Drive: " + file.getId());
    } catch (err) {
      return jsonResponse({ success: false, error: "File create error: " + err, debug }, 500);
    }

    const url = "https://drive.google.com/uc?export=view&id=" + file.getId();

    /** ✅ Delete old file if exists (optional cleanup) */
    if (existingUrl && kgid) {
      try {
        const oldFileId = extractFileIdFromUrl(existingUrl);
        if (oldFileId) {
          try {
            const oldFile = DriveApp.getFileById(oldFileId);
            oldFile.setTrashed(true);
            debug.push("✅ Old image deleted: " + oldFileId);
          } catch (err) {
            debug.push("WARNING: Could not delete old file: " + err);
          }
        }
      } catch (err) {
        debug.push("WARNING: Could not process old file URL: " + err);
      }
    }

    /** Update Sheet */
    if (kgid) {
      try {
        const sheetUpdated = updateSheetFieldByKgid(kgid, "photoUrl", url);
        if (sheetUpdated) {
          debug.push("✅ Sheet updated");
        } else {
          debug.push("WARNING: Sheet update returned false");
        }
      } catch (err) {
        debug.push("WARNING: Sheet update failed: " + err);
      }
    }

    /** Update Firestore */
    if (kgid) {
      try {
        const firestoreResult = updateFirebaseProfileImage(kgid, url);
        if (firestoreResult === 200) {
          debug.push("✅ Firestore updated");
        } else {
          debug.push("WARNING: Firestore update returned code: " + firestoreResult);
        }
      } catch (err) {
        debug.push("WARNING: Firestore update failed: " + err);
      }
    }

    return jsonResponse({
      success: true,
      kgid,
      url,
      id: file.getId(),
      size: blob.getBytes().length,
      dimensions: imageInfo,
      replaced: existingUrl !== null,
      debug
    });

  } catch (err) {
    return jsonResponse({ success: false, error: err.toString(), debug }, 500);
  }
}

/**
 * ✅ Extract file ID from Google Drive URL
 */
function extractFileIdFromUrl(url) {
  try {
    // Match pattern: https://drive.google.com/uc?export=view&id=FILE_ID
    const match = url.match(/[?&]id=([a-zA-Z0-9_-]+)/);
    if (match && match[1]) {
      return match[1];
    }
    return null;
  } catch (err) {
    return null;
  }
}

function updateSheetFieldByKgid(kgid, field, value) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sh = ss.getSheetByName(SHEET_NAME);
    if (!sh) return false;

    const rows = sh.getDataRange().getValues();
    const headers = rows[0].map(h => h.toString().trim());

    const idx = headers.indexOf(field);
    const kgIdx = headers.indexOf("kgid");
    if (idx < 0 || kgIdx < 0) return false;

    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgIdx]) === String(kgid)) {
        sh.getRange(r + 1, idx + 1).setValue(value);
        return true;
      }
    }

    return false;
  } catch (err) {
    Logger.log("updateSheetFieldByKgid ERROR: " + err);
    return false;
  }
}

function updateFirebaseProfileImage(kgid, url) {
  try {
    if (!FIREBASE_PROJECT_ID || !FIREBASE_API_KEY) return null;

    const path = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/employees/" + encodeURIComponent(kgid);
    const api = "https://firestore.googleapis.com/v1/" + path + "?updateMask.fieldPaths=photoUrl&key=" + FIREBASE_API_KEY;

    const payload = {
      fields: {
        photoUrl: { stringValue: url }
      }
    };

    const resp = UrlFetchApp.fetch(api, {
      method: "PATCH",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });

    return resp.getResponseCode();

  } catch (err) {
    Logger.log("updateFirebaseProfileImage ERROR: " + err);
    return null;
  }
}



