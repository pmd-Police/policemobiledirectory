/********** SIMPLE WORKING VERSION - Try This First **********/

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/

const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Emp Profiles";
const DRIVE_FOLDER_ID = "1sR4NPomjADI5lmum-Bx6MAxvmTk1ydxV";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";

/** ------------------------------------------------------ **/

function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

function getSheet() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) throw new Error("Sheet not found: " + SHEET_NAME);
  return sheet;
}

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
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
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

/** 
 * ✅ SIMPLIFIED: Upload profile image
 * 
 * This version tries the simplest approaches first:
 * 1. Use e.postData.bytes directly (if Apps Script parsed multipart automatically)
 * 2. Parse multipart manually from e.postData.contents
 */
function uploadProfileImage(e) {
  try {
    Logger.log("=== uploadProfileImage START ===");
    
    if (!e || !e.postData) {
      Logger.log("ERROR: No postData");
      return jsonResponse({ success: false, error: "No POST data received" }, 400);
    }
    
    const contentType = e.postData.type || "";
    Logger.log("Content-Type: " + contentType);
    Logger.log("postData.contents: " + (e.postData.contents ? e.postData.contents.length + " chars" : "none"));
    Logger.log("postData.bytes: " + (e.postData.bytes ? e.postData.bytes.length + " bytes" : "none"));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob = null;
    let kgid = null;
    
    // ✅ METHOD 1: Try e.postData.bytes directly (simplest - might work if Apps Script auto-parses)
    if (e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log("Trying METHOD 1: postData.bytes directly (" + e.postData.bytes.length + " bytes)");
      
      // Extract filename from contents if available
      let fileName = "upload.jpg";
      if (e.postData.contents) {
        const fnMatch = e.postData.contents.match(/filename="([^"]+)"/);
        if (fnMatch) {
          fileName = fnMatch[1];
          Logger.log("Extracted filename: " + fileName);
          
          // Extract kgid: "98765.jpg" -> "98765"
          const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
          if (kgidMatch) {
            kgid = kgidMatch[1];
            Logger.log("Extracted kgid: " + kgid);
          }
        }
      }
      
      try {
        blob = Utilities.newBlob(e.postData.bytes, "image/jpeg", fileName);
        Logger.log("✅ METHOD 1 SUCCESS: Blob created from bytes (" + blob.getBytes().length + " bytes)");
        
        // Validate it's actually an image (check JPEG signature)
        const bytes = blob.getBytes();
        if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
          Logger.log("✅ Valid JPEG signature (FF D8 FF)");
          return handleBlobSaveAndSync(e, blob, kgid);
        } else {
          Logger.log("WARNING: Doesn't look like JPEG (first bytes: " + bytes.slice(0, 5).map(b => "0x" + b.toString(16).toUpperCase()).join(" ") + ")");
          Logger.log("Trying METHOD 2: multipart parsing...");
          blob = null; // Reset and try next method
        }
      } catch (err) {
        Logger.log("METHOD 1 failed: " + err.toString());
        blob = null;
      }
    }
    
    // ✅ METHOD 2: Parse multipart manually from e.postData.contents
    if (!blob && e.postData.contents && contentType.indexOf("multipart/form-data") >= 0) {
      Logger.log("Trying METHOD 2: multipart parsing");
      
      const raw = e.postData.contents;
      Logger.log("Raw content length: " + raw.length);
      Logger.log("First 500 chars: " + raw.substring(0, 500));
      
      // Extract boundary
      const bMatch = contentType.match(/boundary=([^;]+)/);
      let boundary = bMatch ? bMatch[1].trim() : null;
      
      if (!boundary) {
        Logger.log("ERROR: No boundary found");
        return jsonResponse({ success: false, error: "Invalid multipart: no boundary" }, 400);
      }
      
      Logger.log("Boundary: " + boundary);
      
      // Split by boundary (with -- prefix)
      const delim = "--" + boundary;
      const parts = raw.split(delim);
      Logger.log("Parts found: " + parts.length);
      
      // Find file part (contains name="file" or filename=)
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        
        if (!part || part === "" || part === "--") continue;
        
        Logger.log("Checking part " + i + " (length: " + part.length + ")");
        
        // Check if this is the file part
        if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0 || part.indexOf("filename=") >= 0) {
          Logger.log("✅ Found file part at index " + i);
          
          // Extract filename
          const fnMatch = part.match(/filename="([^"]+)"/) || part.match(/filename='([^']+)'/);
          if (fnMatch) {
            const fileName = fnMatch[1];
            Logger.log("Filename: " + fileName);
            
            // Extract kgid: "98765.jpg" -> "98765"
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              Logger.log("Extracted kgid: " + kgid);
            }
          }
          
          // Find header/body separator
          let headerEnd = part.indexOf("\r\n\r\n");
          let sepLen = 4;
          
          if (headerEnd < 0) {
            headerEnd = part.indexOf("\n\n");
            sepLen = 2;
          }
          
          if (headerEnd < 0) {
            Logger.log("ERROR: No header/body separator in part " + i);
            continue;
          }
          
          Logger.log("Header ends at position: " + headerEnd);
          
          // Extract body (file data)
          let body = part.substring(headerEnd + sepLen);
          
          // Clean up trailing boundary markers
          body = body.replace(/\r\n--$/g, "").replace(/--$/g, "").replace(/^\r\n/, "").replace(/\r\n$/, "").trim();
          
          Logger.log("Body length: " + body.length);
          
          if (body.length === 0) {
            Logger.log("ERROR: Body is empty");
            continue;
          }
          
          // Convert string to bytes array
          try {
            const bytes = [];
            for (let j = 0; j < body.length; j++) {
              bytes.push(body.charCodeAt(j) & 0xFF);
            }
            
            Logger.log("Converted to " + bytes.length + " bytes");
            
            // Check JPEG signature
            if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
              Logger.log("✅ Valid JPEG signature");
              
              const fileName = fnMatch ? fnMatch[1] : "upload.jpg";
              blob = Utilities.newBlob(bytes, "image/jpeg", fileName);
              Logger.log("✅ METHOD 2 SUCCESS: Blob created (" + blob.getBytes().length + " bytes)");
              
              return handleBlobSaveAndSync(e, blob, kgid);
            } else {
              Logger.log("WARNING: Doesn't look like JPEG");
              Logger.log("First 10 bytes: " + bytes.slice(0, 10).map(b => "0x" + b.toString(16).toUpperCase()).join(" "));
            }
          } catch (err) {
            Logger.log("Failed to convert body to bytes: " + err.toString());
          }
        }
      }
      
      Logger.log("METHOD 2 failed: File part not found or parsing failed");
    }
    
    // Final fallback
    Logger.log("❌ ERROR: All methods failed");
    Logger.log("Debug info:");
    Logger.log("  - Content-Type: " + contentType);
    Logger.log("  - postData.contents: " + (e.postData.contents ? "exists" : "none"));
    Logger.log("  - postData.bytes: " + (e.postData.bytes ? "exists" : "none"));
    
    return jsonResponse({ success: false, error: "No image data received. Check Apps Script logs." }, 400);

  } catch (err) {
    Logger.log("EXCEPTION: " + err.toString());
    Logger.log("Stack: " + err.stack);
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}

function handleBlobSaveAndSync(e, blob, kgid) {
  try {
    if (!blob || blob.getBytes().length === 0) {
      Logger.log("ERROR: Empty blob");
      return jsonResponse({ success: false, error: "Empty blob" }, 400);
    }
    
    // Get kgid from query if not extracted
    if (!kgid && e && e.parameter && e.parameter.kgid) {
      kgid = e.parameter.kgid;
      Logger.log("Using kgid from query: " + kgid);
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    // Create file in Drive
    const ts = new Date().getTime();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = (kgid ? ("employee_" + kgid + "_" + ts) : ("employee_" + ts)) + "." + ext;
    
    const file = folder.createFile(blob.setName(fname));
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log("✅ File uploaded: " + driveUrl);
    
    // Update sheet and Firestore if kgid available
    if (kgid) {
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    return jsonResponse({ success: true, url: driveUrl, id: fileId, error: null });

  } catch (err) {
    Logger.log("handleBlobSaveAndSync ERROR: " + err.toString());
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}

function updateSheetFieldByKgid(kgid, field, value) {
  try {
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    const headers = rows[0].map(h => String(h).trim());
    const idx = headers.indexOf(field);
    const kgidIdx = headers.indexOf("kgid");
    
    if (idx < 0 || kgidIdx < 0) return false;
    
    for (let r = 1; r < rows.length; r++) {
      if (String(rows[r][kgidIdx]) === String(kgid)) {
        sheet.getRange(r+1, idx+1).setValue(value);
        Logger.log("Updated sheet: kgid=" + kgid + ", " + field + "=" + value);
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
    
    Logger.log("Firestore update: " + res.getResponseCode());
    return res.getResponseCode();
  } catch (err) {
    Logger.log("updateFirebaseProfileImage ERROR: " + err.toString());
    return null;
  }
}

