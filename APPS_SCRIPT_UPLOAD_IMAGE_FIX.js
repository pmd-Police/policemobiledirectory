/********** FIXED APPS SCRIPT - UPLOAD IMAGE ALWAYS RETURNS JSON **********/
/** This ensures doPost always returns JSON, never HTML error pages **/

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/

const SHEET_ID = "1g0ex1MgMc6mf9bJUG511M-v6FNq704ocimK3I4j9NzE";
const SHEET_NAME = "Emp Profiles";
const DRIVE_FOLDER_ID = "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs"; 
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";

/** ------------------------------------------------------ **/

/**
 * ✅ CRITICAL: Always returns JSON, never HTML
 */
function jsonResponse(obj, status) {
  try {
    const output = ContentService.createTextOutput(JSON.stringify(obj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  } catch (err) {
    // Even if JSON.stringify fails, return something
    const errorObj = { success: false, error: "Failed to create JSON response: " + err.toString() };
    const output = ContentService.createTextOutput(JSON.stringify(errorObj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

/**
 * ✅ CRITICAL: doPost MUST always return JSON
 * This is the entry point - if it doesn't return, Apps Script shows HTML error
 */
function doPost(e) {
  try {
    // ✅ Always ensure we have parameters
    if (!e) {
      return jsonResponse({ success: false, error: "No event object received" }, 400);
    }
    
    if (!e.parameter) {
      return jsonResponse({ success: false, error: "No parameters. Use ?action=uploadImage" }, 400);
    }

    const action = e.parameter.action;
    
    if (!action) {
      return jsonResponse({ success: false, error: "Missing action parameter. Use ?action=uploadImage" }, 400);
    }
    
    Logger.log("doPost called with action: " + action);

    // ✅ Route to uploadImage handler
    if (action === "uploadImage") {
      // ✅ CRITICAL: Ensure uploadProfileImage returns a value
      const result = uploadProfileImage(e);
      // ✅ Double-check: if result is null/undefined, return error JSON
      if (!result) {
        Logger.log("ERROR: uploadProfileImage returned null/undefined");
        return jsonResponse({ 
          success: false, 
          error: "uploadProfileImage did not return a value. Check script logs." 
        }, 500);
      }
      return result;
    }

    // Unknown action
    return jsonResponse({ success: false, error: "Unknown action: " + action }, 400);

  } catch (err) {
    // ✅ CRITICAL: Always catch errors and return JSON
    Logger.log("doPost ERROR: " + err.toString());
    Logger.log("doPost ERROR stack: " + (err.stack || "no stack"));
    return jsonResponse({ 
      success: false, 
      error: "Server error in doPost: " + err.toString() 
    }, 500);
  }
}

/**
 * ✅ FIXED: uploadProfileImage - Always returns JSON
 * Handles base64 JSON uploads from Android app
 */
function uploadProfileImage(e) {
  const debug = [];
  debug.push("=== START uploadProfileImage ===");
  
  try {
    if (!e) {
      debug.push("ERROR: No event object");
      return jsonResponse({ success: false, error: "No event object", debug: debug }, 400);
    }
    
    // ✅ Get raw body - try multiple methods
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
      debug.push("Error reading body: " + err.toString());
    }
    
    debug.push("Raw body length: " + rawBody.length);
    debug.push("Raw body starts with: " + (rawBody.length > 0 ? rawBody.substring(0, 100) : "empty"));
    
    if (!rawBody || rawBody.length === 0) {
      debug.push("ERROR: No POST data received");
      return jsonResponse({ success: false, error: "No POST data received", debug: debug }, 400);
    }
    
    // ✅ Parse JSON (Android app sends base64 JSON)
    let jsonData = null;
    try {
      const trimmed = rawBody.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        jsonData = JSON.parse(trimmed);
        debug.push("✅ JSON parsed successfully");
        debug.push("JSON keys: " + Object.keys(jsonData).join(", "));
      } else {
        debug.push("ERROR: Body does not start with { or [");
        return jsonResponse({ 
          success: false, 
          error: "Invalid request format. Expected JSON.", 
          debug: debug 
        }, 400);
      }
    } catch (parseErr) {
      debug.push("ERROR: JSON parse failed: " + parseErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Failed to parse JSON: " + parseErr.toString(), 
        debug: debug 
      }, 400);
    }
    
    // ✅ Extract image and filename
    if (!jsonData.image) {
      debug.push("ERROR: No 'image' field in JSON");
      return jsonResponse({ 
        success: false, 
        error: "Missing 'image' field in request", 
        debug: debug 
      }, 400);
    }
    
    let base64 = jsonData.image;
    // Remove data:image/...;base64, prefix if present
    if (typeof base64 === "string" && base64.indexOf(",") >= 0) {
      base64 = base64.split(",")[1];
      debug.push("✅ Removed data URI prefix");
    }
    
    // ✅ Decode base64
    let bytes = [];
    try {
      bytes = Utilities.base64Decode(base64);
      debug.push("✅ Base64 decoded: " + bytes.length + " bytes");
    } catch (decodeErr) {
      debug.push("ERROR: Base64 decode failed: " + decodeErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Invalid base64 data: " + decodeErr.toString(), 
        debug: debug 
      }, 400);
    }
    
    // ✅ Extract filename and kgid
    const fileName = (jsonData.filename && String(jsonData.filename)) || ("upload_" + new Date().getTime() + ".jpg");
    const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
    const kgid = kgidMatch ? kgidMatch[1] : null;
    if (kgid) {
      debug.push("✅ Extracted kgid from filename: " + kgid);
    } else {
      debug.push("WARNING: Could not extract kgid from filename: " + fileName);
    }
    
    // ✅ Validate JPEG signature
    if (bytes.length < 3 || bytes[0] !== 0xFF || bytes[1] !== 0xD8 || bytes[2] !== 0xFF) {
      debug.push("ERROR: Not a valid JPEG (first bytes: " + 
        bytes.slice(0, 5).map(b => "0x" + b.toString(16).toUpperCase()).join(" ") + ")");
      return jsonResponse({ 
        success: false, 
        error: "File is not a valid JPEG image", 
        debug: debug 
      }, 400);
    }
    
    // ✅ Create blob
    const blob = Utilities.newBlob(bytes, "image/jpeg", fileName);
    debug.push("✅ Blob created: " + blob.getBytes().length + " bytes");
    
    // ✅ Save to Drive and update sheet/Firestore
    return handleBlobSave(e, blob, kgid, debug);
    
  } catch (err) {
    debug.push("EXCEPTION: " + err.toString());
    debug.push("Stack: " + (err.stack || "no stack"));
    // ✅ CRITICAL: Always return JSON, never let error propagate
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + err.toString(), 
      debug: debug 
    }, 500);
  }
}

/**
 * ✅ FIXED: handleBlobSave - Always returns JSON
 */
function handleBlobSave(e, blob, kgid, debug) {
  try {
    debug.push("--- handleBlobSave START ---");
    
    if (!blob || blob.getBytes().length === 0) {
      debug.push("ERROR: Empty blob");
      return jsonResponse({ success: false, error: "Empty blob", debug: debug }, 400);
    }
    
    // Get kgid from query if not extracted
    if (!kgid && e && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
      debug.push("Using kgid from query: " + kgid);
    }
    
    // ✅ Get Drive folder
    let folder;
    try {
      folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
      debug.push("✅ Drive folder accessed");
    } catch (folderErr) {
      debug.push("ERROR: Could not access Drive folder: " + folderErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Drive folder access failed: " + folderErr.toString(), 
        debug: debug 
      }, 500);
    }
    
    // ✅ Create file in Drive
    const ts = new Date().getTime();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = (kgid ? ("employee_" + kgid + "_" + ts) : ("employee_" + ts)) + "." + ext;
    
    let file;
    try {
      file = folder.createFile(blob.setName(fname));
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      debug.push("✅ File created in Drive: " + file.getId());
    } catch (fileErr) {
      debug.push("ERROR: Could not create file: " + fileErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "File creation failed: " + fileErr.toString(), 
        debug: debug 
      }, 500);
    }
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    debug.push("✅ Drive URL: " + driveUrl);
    
    // ✅ Update sheet and Firestore if kgid available
    if (kgid) {
      try {
        const sheetUpdated = updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
        debug.push("Sheet update result: " + sheetUpdated);
      } catch (sheetErr) {
        debug.push("WARNING: Sheet update failed: " + sheetErr.toString());
        // Don't fail the whole request if sheet update fails
      }
      
      try {
        const firestoreStatus = updateFirebaseProfileImage(kgid, driveUrl);
        debug.push("Firestore update status: " + firestoreStatus);
      } catch (firestoreErr) {
        debug.push("WARNING: Firestore update failed: " + firestoreErr.toString());
        // Don't fail the whole request if Firestore update fails
      }
    } else {
      debug.push("WARNING: No kgid, skipping sheet/Firestore update");
    }
    
    debug.push("✅ SUCCESS - Returning response");
    // ✅ CRITICAL: Always return JSON response
    return jsonResponse({ 
      success: true, 
      url: driveUrl, 
      id: fileId, 
      error: null, 
      debug: debug 
    });
    
  } catch (err) {
    debug.push("handleBlobSave ERROR: " + err.toString());
    debug.push("Stack: " + (err.stack || "no stack"));
    // ✅ CRITICAL: Always return JSON, never let error propagate
    return jsonResponse({ 
      success: false, 
      error: "Error saving blob: " + err.toString(), 
      debug: debug 
    }, 500);
  }
}

function updateSheetFieldByKgid(kgid, field, value) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const sheet = ss.getSheetByName(SHEET_NAME);
    if (!sheet) return false;
    
    const rows = sheet.getDataRange().getValues();
    const headers = rows[0].map(h => String(h).trim());
    const idx = headers.indexOf(field);
    const kgidIdx = headers.indexOf("kgid");
    
    if (idx < 0 || kgidIdx < 0) return false;
    
    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgidIdx]) === String(kgid)) {
        sheet.getRange(r+1, idx+1).setValue(value);
        return true;
      }
    }
  
    return false;
  } catch (err) {
    Logger.log("updateSheetFieldByKgid ERROR: " + err.toString());
    return false;
  }
}

function updateFirebaseProfileImage(kgid, url) {
  try {
    if (!FIREBASE_PROJECT_ID || !FIREBASE_API_KEY) return null;
    
    const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/employees/" + encodeURIComponent(kgid);
    const firestoreUrl = "https://firestore.googleapis.com/v1/" + docPath + "?updateMask.fieldPaths=photoUrl&key=" + FIREBASE_API_KEY;
    
    const payload = {
      fields: {
        photoUrl: { stringValue: url }
      }
    };
    
    const res = UrlFetchApp.fetch(firestoreUrl, {
      method: "PATCH",
      contentType: "application/json",
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
    
    return res.getResponseCode();
  } catch (err) {
    Logger.log("updateFirebaseProfileImage ERROR: " + err.toString());
    return null;
  }
}



