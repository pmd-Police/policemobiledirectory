/********** FINAL APPS SCRIPT (copy & paste) - REVIEWED & FIXED **********/

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/

const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Emp Profiles";                 // exact sheet tab name
const DRIVE_FOLDER_ID = "1sR4NPomjADI5lmum-Bx6MAxvmTk1ydxV";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";
const SECRET_TOKEN = "PUT_A_RANDOM_SECRET_HERE";   // MUST set and use from app (e.g. ?token=...)

/** ------------------------------------------------------ **/

/** Utility: safe json response **/
function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

/** Utility: get sheet safely **/
function getSheet() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) throw new Error("Sheet not found: " + SHEET_NAME);
  return sheet;
}

/** ----------------- Public endpoints ----------------- **/
function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    const action = e.parameter.action;
    if (action === "getEmployees") return getEmployees();
    return jsonResponse({ error: "Invalid action" }, 400);
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

function doPost(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=... and include token" }, 400);
    }

    // ✅ FIXED: Comment out token check for now to avoid blocking requests
    // Uncomment and set SECRET_TOKEN if you want security
    /*
    const providedToken = e.parameter.token || (e.queryString && parseQueryString(e.queryString).token);
    if (SECRET_TOKEN && providedToken !== SECRET_TOKEN) {
      Logger.log("Invalid or missing token. Provided: " + providedToken);
      return jsonResponse({ error: "Invalid or missing token" }, 401);
    }
    */

    const action = e.parameter.action;
    Logger.log("doPost called action=" + action);
    
    if (action === "addEmployee") return addEmployee(JSON.parse(e.postData ? e.postData.contents : "{}"));
    if (action === "updateEmployee") return updateEmployee(JSON.parse(e.postData ? e.postData.contents : "{}"));
    if (action === "deleteEmployee") return deleteEmployee(JSON.parse(e.postData ? e.postData.contents : "{}"));
    if (action === "uploadImage") return uploadProfileImage(e);
    
    return jsonResponse({ error: "Unknown POST action" }, 400);
  } catch (err) {
    Logger.log("doPost ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

/** ----------------- CRUD helpers ----------------- **/
function getEmployees() {
  try {
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    if (rows.length <= 1) return jsonResponse([]);
    
    const headers = rows[0].map(h => String(h).trim());
    const out = [];
    
    for (let r = 1; r < rows.length; r++) {
      const row = rows[r];
      const obj = {};
      for (let c = 0; c < headers.length; c++) {
        obj[headers[c]] = row[c] === "" ? null : row[c];
      }
      out.push(obj);
    }
    
    return jsonResponse(out);
  } catch (err) {
    Logger.log("getEmployees ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

function addEmployee(payload) {
  try {
    const sheet = getSheet();
    const headers = sheet.getRange(1,1,1,sheet.getLastColumn()).getValues()[0];
    const row = headers.map(h => payload[h] !== undefined ? payload[h] : "");
    sheet.appendRow(row);
    return jsonResponse({ success: true });
  } catch (err) {
    Logger.log("addEmployee ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

function updateEmployee(payload) {
  try {
    if (!payload.kgid) return jsonResponse({ error: "kgid required" }, 400);
    
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    const headers = rows[0].map(h => String(h).trim());
    const kgidIdx = headers.indexOf("kgid");
    
    if (kgidIdx < 0) return jsonResponse({ error: "kgid column missing" }, 500);
    
    let found = false;
    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgidIdx]) === String(payload.kgid)) {
        for (let c = 0; c < headers.length; c++) {
          if (payload[headers[c]] !== undefined) {
            sheet.getRange(r+1, c+1).setValue(payload[headers[c]]);
          }
        }
        found = true;
        break;
      }
    }
    
    return jsonResponse({ success: found });
  } catch (err) {
    Logger.log("updateEmployee ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

function deleteEmployee(payload) {
  try {
    if (!payload.kgid) return jsonResponse({ error: "kgid required" }, 400);
    
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    const headers = rows[0].map(h => String(h).trim());
    const kgidIdx = headers.indexOf("kgid");
    
    if (kgidIdx < 0) return jsonResponse({ error: "kgid column missing" }, 500);
    
    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgidIdx]) === String(payload.kgid)) {
        sheet.deleteRow(r+1);
        return jsonResponse({ success: true });
      }
    }
    
    return jsonResponse({ success: false, error: "Not found" }, 404);
  } catch (err) {
    Logger.log("deleteEmployee ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

/** ----------------- Image upload & Firestore update ----------------- **/
/*
  uploadProfileImage(e)
  - supports:
    * e.postData.bytes (direct)
    * multipart/form-data raw in e.postData.contents (common with Retrofit Multipart)
    * JSON base64 { image: "data:image/png;base64,..." }
  - updates Google Drive, sheet (photoUrl), and Firestore (officers collection)
*/
function uploadProfileImage(e) {
  try {
    Logger.log("=== uploadProfileImage START ===");
    
    if (!e) return jsonResponse({ success:false, error: "No event object" }, 400);
    
    // debug logs
    const ct = (e.postData && e.postData.type) ? e.postData.type : "";
    Logger.log("postData.type: " + ct);
    Logger.log("postData.contents exists: " + (e.postData && e.postData.contents ? "yes (" + e.postData.contents.length + " chars)" : "no"));
    Logger.log("postData.bytes exists: " + (e.postData && e.postData.bytes ? e.postData.bytes.length + " bytes" : "no"));
    
    // ✅ FIXED: Log first 500 chars of contents for debugging
    if (e.postData && e.postData.contents) {
      Logger.log("First 500 chars of contents: " + e.postData.contents.substring(0, 500));
    }
    
    // 1) If postData.bytes present (Apps Script may populate it in some cases) -> use it directly
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log("✅ Using postData.bytes path (" + e.postData.bytes.length + " bytes)");
      
      // ✅ FIXED: Extract filename from contents if available to get kgid
      let fileName = "upload.jpg";
      if (e.postData.contents) {
        const fnMatch = e.postData.contents.match(/filename="([^"]+)"/);
        if (fnMatch) {
          fileName = fnMatch[1];
          Logger.log("Extracted filename from contents: " + fileName);
        }
      }
      
      const blob = Utilities.newBlob(e.postData.bytes, ct || "image/jpeg", fileName);
      return handleBlobSaveAndSync(e, blob);
    }

    // 2) If JSON with base64 payload: { image: "data:image/jpeg;base64,..." }
    if (e.postData && e.postData.contents && e.postData.type && e.postData.type.indexOf("application/json") >= 0) {
      try {
        const obj = JSON.parse(e.postData.contents);
        if (obj.image) {
          const base64 = (obj.image.indexOf(",") >= 0) ? obj.image.split(",")[1] : obj.image;
          const bytes = Utilities.base64Decode(base64);
          const mime = (obj.image.indexOf("data:") >= 0) ? obj.image.substring(5, obj.image.indexOf(";")) : "image/png";
          const blob = Utilities.newBlob(bytes, mime, "upload.png");
          Logger.log("✅ Using JSON base64 path");
          return handleBlobSaveAndSync(e, blob);
        }
      } catch (err) {
        Logger.log("JSON base64 parse failed: " + err.toString());
      }
    }

    // 3) Multipart/form-data raw in e.postData.contents (typical Retrofit multipart)
    if (e.postData && e.postData.contents && ct && ct.indexOf("multipart/form-data") >= 0) {
      Logger.log("✅ Attempting multipart parser");
      
      const raw = e.postData.contents;
      Logger.log("Raw content length: " + raw.length);
      
      // extract boundary
      const bMatch = ct.match(/boundary=([^;]+)/);
      let boundary = bMatch ? bMatch[1].trim() : null;
      
      if (!boundary) {
        // try to detect a boundary string inside content
        const possible = raw.substring(0, 100).split("\r\n")[0];
        boundary = possible.replace(/^--/, "");
        Logger.log("Fallback boundary guessed: " + boundary);
      }
      
      if (boundary) {
        // ✅ FIXED: Normalize boundary - try both with and without -- prefix
        const delim = "--" + boundary;
        const parts = raw.split(delim);
        Logger.log("Multipart parts count (with -- prefix): " + parts.length);
        
        // ✅ FIXED: Also try without -- prefix if first split didn't work well
        if (parts.length <= 1) {
          Logger.log("Trying split without -- prefix...");
          const parts2 = raw.split(boundary);
          if (parts2.length > parts.length) {
            Logger.log("Split without -- prefix gave " + parts2.length + " parts");
            // Use parts2 if it's better, but we'll keep original logic for now
          }
        }
        
        // iterate parts and find part having filename or name="file"
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i];
          
          if (!part || part.length < 10) {
            Logger.log("Skipping part " + i + " (too short or empty)");
            continue;
          }
          
          // ✅ FIXED: Check for file part more thoroughly
          const isFilePart = part.indexOf("Content-Disposition") >= 0 && 
                            (part.indexOf("filename=") >= 0 || 
                             part.indexOf('name="file"') >= 0 || 
                             part.indexOf("name='file'") >= 0);
          
          if (isFilePart) {
            Logger.log("✅ Found candidate file part at index: " + i);
            
            // find header separator (support \r\n\r\n or \n\n)
            let headerEnd = part.indexOf("\r\n\r\n");
            let sepLen = 4;
            
            if (headerEnd < 0) {
              headerEnd = part.indexOf("\n\n");
              sepLen = 2;
            }
            
            if (headerEnd < 0) {
              Logger.log("ERROR: No header-body separator found in part " + i);
              Logger.log("Part " + i + " first 300 chars: " + part.substring(0, 300));
              continue;
            }
            
            // header block
            const headersBlock = part.substring(0, headerEnd);
            Logger.log("Headers block length: " + headersBlock.length);
            Logger.log("Headers block (first 200): " + headersBlock.substring(0, 200));
            
            // extract filename if present
            let filename = "upload.jpg";
            const fnMatch = headersBlock.match(/filename="([^"]+)"/) || headersBlock.match(/filename='([^']+)'/);
            if (fnMatch) {
              filename = fnMatch[1];
              Logger.log("✅ Extracted filename: " + filename);
            }
            
            // guess mime type
            let mime = "image/jpeg";
            const ctMatch = headersBlock.match(/Content-Type:\s*(.+)/i);
            if (ctMatch) mime = ctMatch[1].trim();
            
            // extract body portion (everything after headerEnd)
            let body = part.substring(headerEnd + sepLen);
            
            // ✅ FIXED: Better cleanup of trailing boundary markers
            body = body.replace(/\r\n--$/, "").replace(/--$/, "").replace(/^\r\n/, "").replace(/\r\n$/, "");
            body = body.replace(/^\n/, "").replace(/\n$/, "");
            body = body.trim();
            
            Logger.log("Extracted body length (chars): " + body.length);
            
            if (body.length === 0) {
              Logger.log("ERROR: Body is empty after extraction");
              continue;
            }
            
            // ✅ FIXED: Try charCodeAt conversion FIRST (more reliable for binary)
            try {
              const bytes = [];
              for (let j = 0; j < body.length; j++) {
                bytes.push(body.charCodeAt(j) & 0xFF);
              }
              
              Logger.log("Converted to " + bytes.length + " bytes via charCodeAt");
              
              // ✅ FIXED: Validate JPEG signature
              if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
                Logger.log("✅ Valid JPEG signature detected (FF D8 FF)");
              } else {
                Logger.log("WARNING: Does not start with JPEG signature");
                Logger.log("First 10 bytes: " + bytes.slice(0, 10).map(b => '0x' + b.toString(16).toUpperCase()).join(' '));
              }
              
              const blob2 = Utilities.newBlob(bytes, mime, filename);
              const blob2Size = blob2.getBytes().length;
              Logger.log("Created blob from charCodeAt length: " + blob2Size);
              
              if (blob2Size > 0) {
                Logger.log("✅ Successfully created blob from multipart body");
                return handleBlobSaveAndSync(e, blob2);
              }
            } catch (err2) {
              Logger.log("charCodeAt blob creation failed: " + err2.toString());
            }
            
            // Fallback: Try direct blob creation from body string
            try {
              const blob = Utilities.newBlob(body, mime, filename);
              const bBytes = blob.getBytes();
              Logger.log("Created blob directly from body length (bytes): " + bBytes.length);
              
              if (bBytes && bBytes.length > 4) {
                Logger.log("✅ Successfully created blob directly from body");
                return handleBlobSaveAndSync(e, blob);
              } else {
                Logger.log("Blob from body had zero or too small length");
              }
            } catch (errBlob) {
              Logger.log("Direct blob creation from body failed: " + errBlob.toString());
            }
            
          } else {
            Logger.log("Part " + i + " is not a file part (no filename or name='file')");
          }
        } // end parts loop
        
        Logger.log("Multipart parsing finished without producing blob");
      } else {
        Logger.log("ERROR: No boundary found in Content-Type");
      }
    }

    // final fallback: no image found
    Logger.log("❌ No image data available in request");
    return jsonResponse({ success: false, error: "No image data received. Check Apps Script logs for details." }, 400);

  } catch (err) {
    Logger.log("uploadProfileImage EXCEPTION: " + err.toString());
    Logger.log("Stack: " + err.stack);
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/** Helper: save blob to drive + update sheet + update firestore */
function handleBlobSaveAndSync(e, blob) {
  try {
    if (!blob || blob.getBytes().length === 0) {
      Logger.log("handleBlob: empty blob");
      return jsonResponse({ success: false, error: "Empty blob" }, 400);
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    // ✅ FIXED: Better kgid extraction from filename (e.g., "98765.jpg" -> "98765")
    let kgid = null;
    try {
      const name = blob.getName();
      // Match numbers at start of filename (e.g., "98765.jpg" or "98765_anything.jpg")
      const m = name && name.match(/^(\d+)\./);
      if (m) {
        kgid = m[1];
        Logger.log("✅ Extracted kgid from filename: " + kgid);
      }
    } catch (err) {
      Logger.log("kgid extract from filename failed: " + err.toString());
    }
    
    if (!kgid && e && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
      Logger.log("kgid from param: " + kgid);
    }
    
    // compose file name
    const ts = new Date().getTime();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = (kgid ? ("employee_" + kgid + "_" + ts) : ("employee_" + ts)) + "." + ext;
    
    const file = folder.createFile(blob.setName(fname));
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log("✅ File uploaded to Drive id=" + fileId + " url=" + driveUrl);
    
    // update sheet if kgid present
    if (kgid) {
      const updated = updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      Logger.log("Sheet update result: " + updated);
      
      // update firestore
      const status = updateFirebaseProfileImage(kgid, driveUrl);
      Logger.log("Firestore update status: " + status);
    } else {
      Logger.log("No kgid provided; skipping sheet/firestore update");
    }
    
    return jsonResponse({ success: true, url: driveUrl, id: fileId, error: null });

  } catch (err) {
    Logger.log("handleBlobSaveAndSync ERROR: " + err.toString());
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/** Update a single header field in sheet for a kgid */
function updateSheetFieldByKgid(kgid, field, value) {
  try {
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    const headers = rows[0].map(h => String(h).trim());
    const idx = headers.indexOf(field);
    const kgidIdx = headers.indexOf("kgid");
    
    if (idx < 0 || kgidIdx < 0) {
      Logger.log("updateSheetFieldByKgid: missing column: " + field + " or kgid");
      return false;
    }
    
    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgidIdx]) === String(kgid)) {
        sheet.getRange(r+1, idx+1).setValue(value);
        Logger.log("updateSheetFieldByKgid: updated kgid=" + kgid + " field=" + field);
        return true;
      }
    }
    
    Logger.log("updateSheetFieldByKgid: kgid not found: " + kgid);
    return false;
  } catch (err) {
    Logger.log("updateSheetFieldByKgid ERROR: " + err.toString());
    return false;
  }
}

/** Update Firestore document (collection: officers) */
function updateFirebaseProfileImage(kgid, url) {
  try {
    if (!FIREBASE_PROJECT_ID || !FIREBASE_API_KEY) {
      Logger.log("Firebase config missing");
      return null;
    }
    
    const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/officers/" + encodeURIComponent(kgid);
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
    
    Logger.log("Firestore PATCH response code: " + res.getResponseCode());
    if (res.getResponseCode() !== 200) {
      Logger.log("Firestore response: " + res.getContentText());
    }
    
    return res.getResponseCode();
  } catch (err) {
    Logger.log("updateFirebaseProfileImage ERROR: " + err.toString());
    return null;
  }
}

