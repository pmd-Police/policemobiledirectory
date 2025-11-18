/********** FINAL APPS SCRIPT (copy & paste) **********/

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/
const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Emp Profiles";                 // exact sheet tab name
const DRIVE_FOLDER_ID = "1sR4NPomjADI5lmum-Bx6MAxvmTk1ydxV";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";
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

/** Guard for editor runs where `e` is undefined **/
function requireParams(e) {
  if (!e) throw new Error("No event parameter (do not run doGet/doPost from editor). Use HTTP requests.");
  return true;
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

    // Optional: verify secret token to avoid public abuse
    const providedToken = e.parameter.token || (e.queryString && parseQueryString(e.queryString).token);
    if (SECRET_TOKEN && providedToken !== SECRET_TOKEN) {
      Logger.log("Invalid or missing token. Provided: " + providedToken);
      return jsonResponse({ error: "Invalid or missing token" }, 401);
    }

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
    Logger.log("postData.contents exists: " + (e.postData && e.postData.contents ? "yes" : "no"));
    Logger.log("postData.bytes exists: " + (e.postData && e.postData.bytes ? e.postData.bytes.length + " bytes" : "no"));

    // 1) If postData.bytes present (Apps Script may populate it in some cases) -> use it directly
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log("Using postData.bytes path");
      const blob = Utilities.newBlob(e.postData.bytes, ct || "image/jpeg", "upload.jpg");
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
          Logger.log("Using JSON base64 path");
          return handleBlobSaveAndSync(e, blob);
        }
      } catch (err) {
        Logger.log("JSON base64 parse failed: " + err.toString());
      }
    }

    // 3) Multipart/form-data raw in e.postData.contents (typical Retrofit multipart)
    if (e.postData && e.postData.contents && ct && ct.indexOf("multipart/form-data") >= 0) {
      Logger.log("Attempting multipart parser");
      const raw = e.postData.contents;
      // extract boundary
      const bMatch = ct.match(/boundary=([^;]+)/);
      let boundary = bMatch ? bMatch[1] : null;
      if (!boundary) {
        // try to detect a boundary string inside content
        const possible = raw.substring(0, 100).split("\r\n")[0];
        boundary = possible.replace(/^--/, "");
        Logger.log("Fallback boundary guessed: " + boundary);
      }
      if (boundary) {
        // normalize boundary tokens used for splitting
        const delim = "--" + boundary;
        const parts = raw.split(delim);
        Logger.log("Multipart parts count: " + parts.length);

        // iterate parts and find part having filename or name="file"
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i];
          if (!part || part.length < 10) continue;
          // check headers
          if (part.indexOf("Content-Disposition") >= 0 && (part.indexOf("filename=") >= 0 || part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0)) {
            Logger.log("Found candidate file part at index: " + i);

            // find header separator (support \r\n\r\n or \n\n)
            let headerEnd = part.indexOf("\r\n\r\n");
            let sepLen = 4;
            if (headerEnd < 0) {
              headerEnd = part.indexOf("\n\n");
              sepLen = 2;
            }
            if (headerEnd < 0) {
              Logger.log("No header-body separator found in part");
              continue;
            }

            // header block
            const headersBlock = part.substring(0, headerEnd);
            Logger.log("Headers block: " + headersBlock.substring(0, 200));

            // extract filename if present
            let filename = "upload.jpg";
            const fnMatch = headersBlock.match(/filename="([^"]+)"/) || headersBlock.match(/filename='([^']+)'/);
            if (fnMatch) {
              filename = fnMatch[1];
              Logger.log("Extracted filename: " + filename);
            }

            // guess mime type
            let mime = "image/jpeg";
            const ctMatch = headersBlock.match(/Content-Type:\s*(.+)/i);
            if (ctMatch) mime = ctMatch[1].trim();

            // extract body portion (everything after headerEnd)
            let body = part.substring(headerEnd + sepLen);

            // remove possible trailing boundary markers or CRLFs
            body = body.replace(/\r\n--$/g, "").replace(/--$/g, "").replace(/^\r\n/, "").replace(/\r\n$/, "");
            body = body.replace(/^\n/, "").replace(/\n$/, "");

            Logger.log("Extracted body length (chars): " + body.length);

            // Try to create blob directly from the string body (Apps Script will accept binary contained in string)
            try {
              const blob = Utilities.newBlob(body, mime, filename);
              // quick sanity check: blob byte length > 0 and JPEG signature if jpeg
              const bBytes = blob.getBytes();
              Logger.log("Created blob from body length (bytes): " + bBytes.length);
              if (bBytes && bBytes.length > 4) {
                // looks like success
                return handleBlobSaveAndSync(e, blob);
              } else {
                Logger.log("Blob from body had zero length");
              }
            } catch (errBlob) {
              Logger.log("Blob creation from body failed: " + errBlob.toString());
            }

            // Fallback: convert char codes -> bytes array (works if raw was preserved)
            try {
              const bytes = [];
              for (let j = 0; j < body.length; j++) {
                bytes.push(body.charCodeAt(j) & 0xFF);
              }
              const blob2 = Utilities.newBlob(bytes, mime, filename);
              Logger.log("Created blob2 from char codes length: " + blob2.getBytes().length);
              if (blob2.getBytes().length > 0) {
                return handleBlobSaveAndSync(e, blob2);
              }
            } catch (err2) {
              Logger.log("Fallback charCode blob creation failed: " + err2.toString());
            }
          }
        } // end parts loop
      }
      // if multipart parsing falls through, we continue to final fallback below
      Logger.log("Multipart parsing finished without producing blob");
    }

    // final fallback: no image found
    Logger.log("No image data available in request");
    return jsonResponse({ success: false, error: "No image data received" }, 400);

  } catch (err) {
    Logger.log("uploadProfileImage EXCEPTION: " + err.toString());
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
    // determine kgid either from filename or from query param
    let kgid = null;
    try {
      const name = blob.getName();
      const m = name && name.match(/^(\d+)(\D.*)?$/);
      if (m) kgid = m[1];
    } catch (err) {
      Logger.log("kgid extract from filename failed: " + err.toString());
    }
    if (!kgid && e && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
      Logger.log("kgid from param: " + kgid);
    }

    // compose file name
    const ts = new Date().getTime();
    const fname = (kgid ? ("employee_" + kgid + "_" + ts) : ("employee_" + ts)) + "." + (blob.getName().split('.').pop() || "jpg");
    const file = folder.createFile(blob.setName(fname));
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;

    Logger.log("File uploaded to Drive id=" + fileId + " url=" + driveUrl);

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

/** Small helper to parse query string (if needed) */
function parseQueryString(qs) {
  const map = {};
  if (!qs) return map;
  const pairs = qs.split("&");
  pairs.forEach(p => {
    const kv = p.split("=");
    try {
      const k = decodeURIComponent(kv[0]);
      const v = kv.length > 1 ? decodeURIComponent(kv.slice(1).join("=")) : "";
      map[k] = v;
    } catch (e) {
      map[kv[0]] = kv.length > 1 ? kv.slice(1).join("=") : "";
    }
  });
  return map;
}
