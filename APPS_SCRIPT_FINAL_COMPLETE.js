/********** COMPLETE APPS SCRIPT - READY TO USE **********/
/** This is a complete rewrite with better multipart handling **/

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
 * ✅ REWRITTEN: Upload profile image with comprehensive debugging
 * 
 * This version:
 * 1. Includes debug info in JSON response
 * 2. Tries multiple methods to parse multipart
 * 3. Validates JPEG signature
 * 4. Extracts kgid from filename
 */
function uploadProfileImage(e) {
  // ✅ ALWAYS initialize debug array first
  const debug = [];
  debug.push("=== START uploadProfileImage ===");
  debug.push("e exists: " + (e != null));
  
  try {
    if (!e) {
      debug.push("ERROR: No event object");
      return jsonResponse({ success: false, error: "No event object", debug: debug }, 400);
    }
    
    debug.push("e.parameter exists: " + (e.parameter != null));
    debug.push("e.parameter.action: " + (e.parameter ? e.parameter.action : "none"));
    debug.push("e.keys: " + Object.keys(e).join(", "));
    
    if (!e.postData) {
      debug.push("ERROR: No postData");
      return jsonResponse({ success: false, error: "No POST data received", debug: debug }, 400);
    }
    
    const ct = e.postData.type || "";
    debug.push("Content-Type: " + ct);
    
    const hasContents = !!(e.postData.contents);
    const contentsLen = hasContents ? e.postData.contents.length : 0;
    const hasBytes = !!(e.postData.bytes);
    const bytesLen = hasBytes ? e.postData.bytes.length : 0;
    
    debug.push("postData.contents: " + (hasContents ? "exists (" + contentsLen + " chars)" : "none"));
    debug.push("postData.bytes: " + (hasBytes ? "exists (" + bytesLen + " bytes)" : "none"));
    debug.push("postData.keys: " + Object.keys(e.postData).join(", "));
    
    // Log first 300 chars of contents if available
    if (hasContents && contentsLen > 0) {
      debug.push("First 300 chars: " + e.postData.contents.substring(0, 300));
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob = null;
    let kgid = null;
    
    // ✅ METHOD 1: Use e.postData.bytes directly (if Apps Script auto-parsed multipart)
    if (hasBytes && bytesLen > 0) {
      debug.push("--- METHOD 1: Trying postData.bytes ---");
      
      try {
        let fileName = "upload.jpg";
        
        // Try to extract filename and kgid from contents
        if (hasContents) {
          const fnMatch = e.postData.contents.match(/filename="([^"]+)"/);
          if (fnMatch) {
            fileName = fnMatch[1];
            debug.push("Extracted filename: " + fileName);
            
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              debug.push("Extracted kgid: " + kgid);
            }
          }
        }
        
        blob = Utilities.newBlob(e.postData.bytes, "image/jpeg", fileName);
        const blobBytes = blob.getBytes();
        
        debug.push("Blob created: " + blobBytes.length + " bytes");
        
        // Validate JPEG signature
        if (blobBytes.length >= 3 && blobBytes[0] === 0xFF && blobBytes[1] === 0xD8 && blobBytes[2] === 0xFF) {
          debug.push("✅ METHOD 1 SUCCESS: Valid JPEG");
          return handleBlobSave(e, blob, kgid, debug);
        } else {
          debug.push("METHOD 1 FAILED: Not a JPEG (first bytes: " + 
            blobBytes.slice(0, 5).map(b => "0x" + b.toString(16).toUpperCase()).join(" ") + ")");
          blob = null;
        }
      } catch (err) {
        debug.push("METHOD 1 ERROR: " + err.toString());
        blob = null;
      }
    } else {
      debug.push("METHOD 1 SKIPPED: No postData.bytes");
    }
    
    // ✅ METHOD 2: Parse JSON base64 (NEW - simpler and more reliable)
    // Check if it's JSON by Content-Type OR by checking if contents starts with {
    const ctIsJson = ct.indexOf("application/json") >= 0;
    const contentsIsJson = hasContents && e.postData.contents.trim().startsWith("{");
    const isJson = ctIsJson || contentsIsJson;
    
    debug.push("METHOD 2 CHECK: ctIsJson=" + ctIsJson + ", contentsIsJson=" + contentsIsJson + ", isJson=" + isJson + ", blob=" + (blob != null) + ", hasContents=" + hasContents);
    
    if (!blob && hasContents && isJson) {
      debug.push("--- METHOD 2: Parsing JSON base64 ---");
      debug.push("Content-Type check: " + (ct.indexOf("application/json") >= 0));
      debug.push("Starts with { check: " + (e.postData.contents.trim().startsWith("{")));
      
      try {
        const jsonData = JSON.parse(e.postData.contents);
        debug.push("✅ JSON parsed successfully");
        debug.push("JSON keys: " + Object.keys(jsonData).join(", "));
        
        if (jsonData.image) {
          debug.push("✅ Found 'image' field in JSON");
          
          // Extract base64 string
          let base64 = jsonData.image;
          if (base64.indexOf(",") >= 0) {
            base64 = base64.split(",")[1];
            debug.push("Extracted base64 after comma");
          }
          
          debug.push("Base64 length: " + base64.length);
          
          // Decode base64
          const bytes = Utilities.base64Decode(base64);
          debug.push("Decoded to " + bytes.length + " bytes");
          
          // Validate JPEG signature
          if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
            debug.push("✅ Valid JPEG signature (FF D8 FF)");
          } else {
            debug.push("WARNING: Doesn't look like JPEG");
          }
          
          // Get filename and extract kgid
          let fileName = jsonData.filename || "upload.jpg";
          debug.push("Filename: " + fileName);
          
          const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
          if (kgidMatch) {
            kgid = kgidMatch[1];
            debug.push("Extracted kgid: " + kgid);
          }
          
          // Create blob
          blob = Utilities.newBlob(bytes, "image/jpeg", fileName);
          const blobSize = blob.getBytes().length;
          debug.push("✅ METHOD 2 SUCCESS: Blob created (" + blobSize + " bytes)");
          
          return handleBlobSave(e, blob, kgid, debug);
        } else {
          debug.push("ERROR: No 'image' field in JSON");
          debug.push("JSON keys found: " + Object.keys(jsonData).join(", "));
          debug.push("JSON sample: " + JSON.stringify(jsonData).substring(0, 200));
        }
      } catch (err) {
        debug.push("METHOD 2 ERROR: " + err.toString());
        debug.push("Error stack: " + (err.stack || "no stack"));
      }
    } else if (!blob && hasContents) {
      debug.push("METHOD 2 SKIPPED: isJson=" + isJson + ", Content-Type: " + ct);
      debug.push("Contents starts with: " + e.postData.contents.substring(0, 50));
    }
    
    // ✅ METHOD 3: Parse multipart/form-data manually (fallback)
    if (!blob && hasContents && ct.indexOf("multipart/form-data") >= 0) {
      debug.push("--- METHOD 3: Parsing multipart/form-data ---");
      
      try {
        const raw = e.postData.contents;
        debug.push("Raw content length: " + raw.length);
        
        // Extract boundary
        const bMatch = ct.match(/boundary=([^;\s]+)/);
        let boundary = bMatch ? bMatch[1].trim() : null;
        
        if (!boundary) {
          debug.push("ERROR: No boundary in Content-Type");
          debug.push("Content-Type: " + ct);
          return jsonResponse({ success: false, error: "Invalid multipart: no boundary", debug: debug }, 400);
        }
        
        debug.push("Boundary: " + boundary);
        
        // Try splitting with -- prefix first
        const delim1 = "--" + boundary;
        let parts = raw.split(delim1);
        debug.push("Split with '--" + boundary + "': " + parts.length + " parts");
        
        // If that didn't work, try without -- prefix
        if (parts.length <= 1) {
          parts = raw.split(boundary);
          debug.push("Split with '" + boundary + "': " + parts.length + " parts");
        }
        
        if (parts.length <= 1) {
          debug.push("ERROR: Could not split content by boundary");
          debug.push("First 200 chars of raw: " + raw.substring(0, 200));
          return jsonResponse({ success: false, error: "Could not parse multipart", debug: debug }, 400);
        }
        
        // Find the file part
        let filePart = null;
        let filePartIndex = -1;
        
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i].trim();
          
          if (!part || part.length < 10) continue;
          
          const isFilePart = (part.indexOf('name="file"') >= 0 || 
                             part.indexOf("name='file'") >= 0 || 
                             part.indexOf("filename=") >= 0);
          
          debug.push("Part " + i + ": length=" + part.length + ", isFilePart=" + isFilePart);
          
          if (isFilePart) {
            filePart = part;
            filePartIndex = i;
            debug.push("✅ Found file part at index " + i);
            break;
          }
        }
        
        if (!filePart) {
          debug.push("ERROR: File part not found");
          debug.push("Searching all parts for 'file' keyword...");
          for (let i = 0; i < Math.min(parts.length, 5); i++) {
            if (parts[i].indexOf("file") >= 0) {
              debug.push("Part " + i + " contains 'file': " + parts[i].substring(0, 200));
            }
          }
          return jsonResponse({ success: false, error: "File part not found", debug: debug }, 400);
        }
        
        // Extract filename
        const fnMatch = filePart.match(/filename="([^"]+)"/) || filePart.match(/filename='([^']+)'/);
        let fileName = "upload.jpg";
        if (fnMatch) {
          fileName = fnMatch[1];
          debug.push("Filename: " + fileName);
          
          const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
          if (kgidMatch) {
            kgid = kgidMatch[1];
            debug.push("Extracted kgid: " + kgid);
          }
        }
        
        // Find header/body separator
        let headerEnd = filePart.indexOf("\r\n\r\n");
        let sepLen = 4;
        
        if (headerEnd < 0) {
          headerEnd = filePart.indexOf("\n\n");
          sepLen = 2;
        }
        
        if (headerEnd < 0) {
          debug.push("ERROR: No header/body separator in file part");
          debug.push("File part first 300 chars: " + filePart.substring(0, 300));
          return jsonResponse({ success: false, error: "Could not find file data", debug: debug }, 400);
        }
        
        debug.push("Header ends at position: " + headerEnd);
        
        // Extract body (file data)
        let body = filePart.substring(headerEnd + sepLen);
        
        // Clean up trailing boundary markers
        body = body.replace(/\r\n--$/g, "").replace(/--$/g, "");
        body = body.replace(/^\r\n/, "").replace(/\r\n$/, "");
        body = body.trim();
        
        debug.push("Body length after cleanup: " + body.length);
        
        if (body.length === 0) {
          debug.push("ERROR: Body is empty");
          return jsonResponse({ success: false, error: "File data is empty", debug: debug }, 400);
        }
        
        // Convert string to bytes array
        const bytes = [];
        for (let j = 0; j < body.length; j++) {
          bytes.push(body.charCodeAt(j) & 0xFF);
        }
        
        debug.push("Converted to " + bytes.length + " bytes");
        
        // Validate JPEG signature
        if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
          debug.push("✅ Valid JPEG signature");
          
          blob = Utilities.newBlob(bytes, "image/jpeg", fileName);
          const blobSize = blob.getBytes().length;
          debug.push("✅ METHOD 3 SUCCESS: Blob created (" + blobSize + " bytes)");
          
          return handleBlobSave(e, blob, kgid, debug);
        } else {
          debug.push("ERROR: Not a JPEG");
          debug.push("First 10 bytes: " + bytes.slice(0, 10).map(b => "0x" + b.toString(16).toUpperCase()).join(" "));
          return jsonResponse({ success: false, error: "File is not a valid JPEG", debug: debug }, 400);
        }
        
      } catch (err) {
        debug.push("METHOD 3 ERROR: " + err.toString());
        debug.push("Stack: " + (err.stack || "no stack"));
        return jsonResponse({ success: false, error: err.toString(), debug: debug }, 500);
      }
    } else {
      if (!hasContents) {
        debug.push("METHOD 3 SKIPPED: No postData.contents");
      } else {
        debug.push("METHOD 3 SKIPPED: Not multipart (Content-Type: " + ct + ")");
      }
    }
    
    // Final error
    debug.push("❌ ALL METHODS FAILED");
    return jsonResponse({ 
      success: false, 
      error: "No image data received. Check debug array for details.",
      debug: debug,
      url: null,
      id: null
    }, 400);

  } catch (err) {
    debug.push("EXCEPTION: " + err.toString());
    debug.push("Stack: " + (err.stack || "no stack"));
    return jsonResponse({ success: false, error: err.toString(), debug: debug }, 500);
  }
}

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
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    // Create file in Drive
    const ts = new Date().getTime();
    const ext = blob.getName().split('.').pop() || "jpg";
    const fname = (kgid ? ("employee_" + kgid + "_" + ts) : ("employee_" + ts)) + "." + ext;
    
    const file = folder.createFile(blob.setName(fname));
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    debug.push("✅ File uploaded: " + driveUrl);
    
    // Update sheet and Firestore if kgid available
    if (kgid) {
      const sheetUpdated = updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      debug.push("Sheet update: " + sheetUpdated);
      
      const firestoreStatus = updateFirebaseProfileImage(kgid, driveUrl);
      debug.push("Firestore update: " + firestoreStatus);
    } else {
      debug.push("WARNING: No kgid, skipping sheet/Firestore update");
    }
    
    debug.push("✅ SUCCESS");
    return jsonResponse({ success: true, url: driveUrl, id: fileId, error: null, debug: debug });

  } catch (err) {
    debug.push("handleBlobSave ERROR: " + err.toString());
    return jsonResponse({ success: false, error: err.toString(), debug: debug }, 500);
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
    
    return res.getResponseCode();
  } catch (err) {
    Logger.log("updateFirebaseProfileImage ERROR: " + err.toString());
    return null;
  }
}

