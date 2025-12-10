/********************************************************************
 * 📸 IMAGE UPLOAD SERVICE — STANDALONE
 * ---------------------------------------------------------------
 * Handles profile image uploads to Google Drive
 * Updates Google Sheets and Firestore with image URLs
 * Can be deployed independently
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
      config: {
        sheetId: SHEET_ID,
        sheetName: SHEET_NAME,
        driveFolderId: DRIVE_FOLDER_ID,
        firebaseProjectId: FIREBASE_PROJECT_ID
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
   IMAGE UPLOAD FUNCTIONS
================================================================ */

function uploadProfileImage(e) {
  const debug = [];
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

    /** Verify JPEG header */
    if (bytes.length < 2) {
      debug.push("ERROR: File too small (" + bytes.length + " bytes)");
      return jsonResponse({
        success: false,
        error: "File too small",
        debug
      }, 400);
    }
    
    // Check first two bytes for JPEG magic number
    // Convert to unsigned bytes (0-255) in case they're signed (-128 to 127)
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

    /** Create Blob */
    const fileName = jsonData.filename || ("upload_" + Date.now() + ".jpg");
    const blob = Utilities.newBlob(bytes, "image/jpeg", fileName);

    /** Extract KGID from filename or query */
    let kgid = (fileName.match(/^(\d+)\.jpg$/) || [])[1];
    if (!kgid && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
    }

    return handleBlobSave(e, blob, kgid, debug);

  } catch (err) {
    return jsonResponse({
      success: false,
      error: "Server error: " + err,
      debug
    }, 500);
  }
}

function handleBlobSave(e, blob, kgid, debug) {
  try {
    debug.push("--- handleBlobSave START ---");

    /** Get folder */
    let folder;
    try {
      folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    } catch (err) {
      return jsonResponse({
        success: false,
        error: "Drive folder error: " + err,
        debug
      }, 500);
    }

    /** Upload to Drive */
    const ts = Date.now();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = "employee_" + (kgid || "unk") + "_" + ts + "." + ext;

    let file;
    try {
      file = folder.createFile(blob.setName(fname));
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch (err) {
      return jsonResponse({ success: false, error: "File create error: " + err, debug }, 500);
    }

    const url = "https://drive.google.com/uc?export=view&id=" + file.getId();

    /** Update Sheet */
    if (kgid) {
      try {
        updateSheetFieldByKgid(kgid, "photoUrl", url);
        debug.push("✅ Sheet updated");
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
      debug
    });

  } catch (err) {
    return jsonResponse({ success: false, error: err.toString(), debug }, 500);
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



