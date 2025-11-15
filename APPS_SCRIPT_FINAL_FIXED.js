/**
 * ✅ FINAL FIXED VERSION - Ready to Use
 * 
 * Fixed issues:
 * 1. Extract kgid from filename (Android app sends "98765.jpg")
 * 2. Use correct Firestore collection: "officers" (not "users")
 * 3. Better multipart parsing and error handling
 */

/** ---------- CONFIG ---------- **/

const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Sheet1";
const DRIVE_FOLDER_ID = "1sR4NPomjADI5lmum-Bx6MAxvmTk1ydxV";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";

/** ---------------------------- **/

function doGet(e) {
  const action = e.parameter.action;
  if (action === "getEmployees") return getEmployees();
  return jsonResponse({ error: "Invalid action" }, 400);
}

function doPost(e) {
  const action = e.parameter.action;
  Logger.log('doPost called with action: ' + action);
  
  if (action === "addEmployee") return addEmployee(JSON.parse(e.postData.contents));
  if (action === "updateEmployee") return updateEmployee(JSON.parse(e.postData.contents));
  if (action === "deleteEmployee") return deleteEmployee(JSON.parse(e.postData.contents));
  if (action === "uploadImage") return uploadProfileImage(e);
  
  return jsonResponse({ error: "Unknown POST action" }, 400);
}

/** Utilities **/

function getSheet() {
  return SpreadsheetApp.openById(SHEET_ID).getSheetByName(SHEET_NAME);
}

function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

/** Read all employees and return JSON array */
function getEmployees() {
  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  if (rows.length <= 1) return jsonResponse([]);
  
  const headers = rows[0].map(h => String(h).trim());
  const data = [];
  
  for (let r = 1; r < rows.length; r++) {
    const row = rows[r];
    const obj = {};
    for (let c = 0; c < headers.length; c++) {
      obj[headers[c]] = row[c] === "" ? null : row[c];
    }
    data.push(obj);
  }
  
  return jsonResponse(data);
}

/** Add employee */
function addEmployee(payload) {
  const sheet = getSheet();
  const headers = sheet.getRange(1,1,1,sheet.getLastColumn()).getValues()[0];
  const row = headers.map(h => payload[h] !== undefined ? payload[h] : "");
  sheet.appendRow(row);
  return jsonResponse({ success: true });
}

/** Update employee by kgid */
function updateEmployee(payload) {
  if (!payload.kgid) return jsonResponse({ error: "kgid required" }, 400);
  
  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  const headers = rows[0].map(h => String(h).trim());
  let found = false;
  
  for (let r = 1; r < rows.length; r++) {
    if (String(rows[r][headers.indexOf("kgid")]) === String(payload.kgid)) {
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
}

/** Delete employee by kgid */
function deleteEmployee(payload) {
  if (!payload.kgid) return jsonResponse({ error: "kgid required" }, 400);
  
  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  const headers = rows[0].map(h => String(h).trim());
  
  for (let r = 1; r < rows.length; r++) {
    if (String(rows[r][headers.indexOf("kgid")]) === String(payload.kgid)) {
      sheet.deleteRow(r+1);
      return jsonResponse({ success: true });
    }
  }
  
  return jsonResponse({ success: false, error: "Not found" }, 404);
}

/** 
 * ✅ FIXED: Upload profile image
 * 
 * Key fixes:
 * 1. Extract kgid from filename (e.g., "98765.jpg" -> "98765")
 * 2. Use correct Firestore collection: "officers" (not "users")
 */
function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage called ===');
    Logger.log('postData type: ' + (e.postData ? e.postData.type : 'none'));
    Logger.log('postData bytes: ' + (e.postData && e.postData.bytes ? e.postData.bytes.length : 'none'));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    let blob;
    let fileName = '';
    let kgid = null;
    
    // ✅ Handle multipart/form-data
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log('Using postData.bytes, length: ' + e.postData.bytes.length);
      
      // ✅ FIXED: Extract filename from multipart to get kgid
      if (e.postData.contents) {
        const contentStr = e.postData.contents.toString();
        const filenameMatch = contentStr.match(/filename="([^"]+)"/);
        if (filenameMatch) {
          fileName = filenameMatch[1];
          Logger.log('Extracted filename: ' + fileName);
          
          // ✅ Extract kgid from filename: "98765.jpg" -> "98765"
          const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
          if (kgidMatch) {
            kgid = kgidMatch[1];
            Logger.log('Extracted kgid from filename: ' + kgid);
          }
        }
      }
      
      // Use bytes directly - Apps Script handles multipart
      blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', fileName || 'profile.jpg');
      
    } else if (e.postData && e.postData.contents) {
      // Fallback: try JSON (for base64 uploads)
      try {
        const data = JSON.parse(e.postData.contents);
        if (data.image) {
          const base64 = data.image.split(',')[1] || data.image;
          blob = Utilities.newBlob(Utilities.base64Decode(base64), 'image/png', 'profile.png');
        }
      } catch (e) {
        Logger.log('Could not parse as JSON: ' + e.toString());
      }
    }
    
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('ERROR: No blob created');
      return jsonResponse({ 
        success: false, 
        error: "No image data received",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('Blob created, size: ' + blob.getBytes().length);
    
    // ✅ FIXED: Fallback to query parameter if not in filename
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query parameter: ' + kgid);
    }
    
    // Create file in Drive
    const driveFileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(driveFileName));
    
    Logger.log('File created in Drive: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('Public URL: ' + driveUrl);
    
    // ✅ FIXED: Update sheet/Firestore if kgid available
    if (kgid) {
      Logger.log('Updating sheet and Firestore for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl); // ✅ Uses "officers" collection
    } else {
      Logger.log('WARNING: No kgid found, skipping sheet/Firestore update');
    }
    
    // ✅ CORRECT: Response format matches Android app
    return jsonResponse({ 
      success: true, 
      url: driveUrl,    // ✅ Must be "url"
      id: fileId,       // ✅ Must include "id"
      error: null       // ✅ Must include "error"
    });
    
  } catch (err) {
    Logger.log('ERROR in uploadProfileImage: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString(),
      url: null,
      id: null
    }, 500);
  }
}

/** Update a single header field in sheet for a kgid */
function updateSheetFieldByKgid(kgid, field, value) {
  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  const headers = rows[0].map(h => String(h).trim());
  const idx = headers.indexOf(field);
  const kgidIdx = headers.indexOf("kgid");
  
  if (idx < 0 || kgidIdx < 0) {
    Logger.log('ERROR: Field or kgid column not found');
    return false;
  }
  
  for (let r = 1; r < rows.length; r++) {
    if (String(rows[r][kgidIdx]) === String(kgid)) {
      sheet.getRange(r+1, idx+1).setValue(value);
      Logger.log('Updated sheet: kgid=' + kgid + ', field=' + field);
      return true;
    }
  }
  
  Logger.log('WARNING: kgid not found in sheet: ' + kgid);
  return false;
}

/** 
 * ✅ FIXED: Update Firestore - uses "officers" collection (not "users")
 * 
 * Your Android app uses: firestore.collection("officers")
 */
function updateFirebaseProfileImage(kgid, url) {
  if (!FIREBASE_PROJECT_ID || !FIREBASE_API_KEY) {
    Logger.log('WARNING: Firebase config missing');
    return null;
  }
  
  try {
    // ✅ FIXED: Use "officers" collection to match Android app
    const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/officers/" + encodeURIComponent(kgid);
    const firestoreUrl = "https://firestore.googleapis.com/v1/" + docPath + "?updateMask.fieldPaths=photoUrl&key=" + FIREBASE_API_KEY;
    
    Logger.log('Firestore URL: ' + firestoreUrl);
    
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
    
    const statusCode = res.getResponseCode();
    Logger.log('Firestore update response: ' + statusCode);
    
    if (statusCode !== 200) {
      Logger.log('Firestore error: ' + res.getContentText());
    } else {
      Logger.log('Firestore updated successfully for kgid: ' + kgid);
    }
    
    return statusCode;
  } catch (err) {
    Logger.log('ERROR updating Firestore: ' + err.toString());
    return null;
  }
}

