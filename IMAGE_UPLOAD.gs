/********** IMAGE UPLOAD MODULE - COMPLETE & SELF-CONTAINED **********/

/**
 * Always return JSON response
 * Note: This is also defined in MAIN_SCRIPT.gs, but included here for self-containment
 */
function jsonResponse(obj, status) {
  try {
    const out = ContentService.createTextOutput(JSON.stringify(obj));
    out.setMimeType(ContentService.MimeType.JSON);
    return out;
  } catch (err) {
    // Fallback if JSON.stringify fails
    const errorObj = { success: false, error: "Failed to create JSON response: " + err.toString() };
    const out = ContentService.createTextOutput(JSON.stringify(errorObj));
    out.setMimeType(ContentService.MimeType.JSON);
    return out;
  }
}

/**
 * Main image upload handler (Android sends base64 JSON)
 */
function uploadProfileImage(e, DRIVE_FOLDER_ID, SHEET_ID, SHEET_NAME, FIREBASE_PROJECT_ID, FIREBASE_API_KEY) {
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
    } catch (err) {
      return jsonResponse({ success: false, error: "Base64 decode failed: " + err, debug }, 400);
    }

    /** Verify JPEG header */
    if (bytes.length < 2 || bytes[0] !== 0xFF || bytes[1] !== 0xD8) {
      return jsonResponse({
        success: false,
        error: "Invalid JPEG file",
        debug
      }, 400);
    }

    /** Create Blob */
    const fileName = jsonData.filename || ("upload_" + Date.now() + ".jpg");
    const blob = Utilities.newBlob(bytes, "image/jpeg", fileName);

    /** Extract KGID from filename or query */
    let kgid = (fileName.match(/^(\d+)\.jpg$/) || [])[1];
    if (!kgid && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
    }

    return handleBlobSave(
      e,
      blob,
      kgid,
      debug,
      DRIVE_FOLDER_ID,
      SHEET_ID,
      SHEET_NAME,
      FIREBASE_PROJECT_ID,
      FIREBASE_API_KEY
    );

  } catch (err) {
    return jsonResponse({
      success: false,
      error: "Server error: " + err,
      debug
    }, 500);
  }
}

/**
 * Save Blob → Drive → Update Sheet + Firestore
 */
function handleBlobSave(e, blob, kgid, debug, DRIVE_FOLDER_ID, SHEET_ID, SHEET_NAME, FIREBASE_PROJECT_ID, FIREBASE_API_KEY) {
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
        updateSheetFieldByKgid(kgid, "photoUrl", url, SHEET_ID, SHEET_NAME);
      } catch (_) {}
    }

    /** Update Firestore */
    if (kgid) {
      try {
        updateFirebaseProfileImage(kgid, url, FIREBASE_PROJECT_ID, FIREBASE_API_KEY);
      } catch (_) {}
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

/**
 * Update Google Sheet photoUrl by KGID
 */
function updateSheetFieldByKgid(kgid, field, value, SHEET_ID, SHEET_NAME) {
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

/**
 * Update Firestore photoUrl field
 */
function updateFirebaseProfileImage(kgid, url, FIREBASE_PROJECT_ID, FIREBASE_API_KEY) {
  try {
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



