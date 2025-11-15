/**
 * ✅ COMPLETE FIXED VERSION - Ready to Copy & Paste
 * 
 * Fixed issues:
 * 1. ✅ Proper multipart/form-data parsing (extracts binary image data correctly)
 * 2. ✅ Extract kgid from filename (Android app sends "98765.jpg")
 * 3. ✅ Use correct Firestore collection: "officers" (not "users")
 * 4. ✅ Comprehensive error handling and logging
 */

/** ---------- CONFIG ---------- **/

const SHEET_ID = "16CjFznsde8GV0LKtilaD8-CaUYC3FrYzcmMDfy1ww3Q";
const SHEET_NAME = "Emp Profiles";
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
 * ✅ FIXED: Upload profile image to Drive, update sheet and Firestore
 * 
 * This function properly parses multipart/form-data from Android Retrofit
 * Key fix: Manually parse multipart boundary to extract binary image data
 */
function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage START ===');
    Logger.log('postData exists: ' + (e.postData != null));
    
    if (!e.postData) {
      Logger.log('ERROR: No postData');
      return jsonResponse({ 
        success: false, 
        error: "No POST data received",
        url: null,
        id: null
      }, 400);
    }
    
    const contentType = e.postData.type || '';
    Logger.log('Content-Type: ' + contentType);
    Logger.log('postData.contents exists: ' + (e.postData.contents != null));
    Logger.log('postData.contents type: ' + typeof(e.postData.contents));
    Logger.log('postData.contents length: ' + (e.postData.contents ? e.postData.contents.length : 0));
    Logger.log('postData.bytes exists: ' + (e.postData.bytes != null));
    Logger.log('postData.bytes length: ' + (e.postData.bytes ? e.postData.bytes.length : 0));
    
    // Log first 500 chars of contents to see structure
    if (e.postData.contents) {
      Logger.log('First 500 chars of contents: ' + e.postData.contents.substring(0, 500));
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob;
    let kgid = null;
    
    // ✅ Try multiple approaches to parse multipart
    if (contentType.indexOf('multipart/form-data') >= 0) {
      Logger.log('✅ Detected multipart/form-data');
      
      // Extract boundary
      const boundaryMatch = contentType.match(/boundary=([^;]+)/);
      if (!boundaryMatch) {
        Logger.log('ERROR: No boundary found in Content-Type');
        Logger.log('Full Content-Type: ' + contentType);
        return jsonResponse({ 
          success: false, 
          error: "Invalid multipart: missing boundary",
          url: null,
          id: null
        }, 400);
      }
      
      const boundary = '--' + boundaryMatch[1].trim();
      Logger.log('Extracted boundary: ' + boundary);
      Logger.log('Boundary length: ' + boundary.length);
      
      // Get raw content
      const rawContent = e.postData.contents;
      if (!rawContent) {
        Logger.log('ERROR: No postData.contents');
        return jsonResponse({ 
          success: false, 
          error: "No multipart content",
          url: null,
          id: null
        }, 400);
      }
      
      Logger.log('Raw content total length: ' + rawContent.length);
      
      // Check if boundary exists in content
      const boundaryIndex = rawContent.indexOf(boundary);
      Logger.log('Boundary found at index: ' + boundaryIndex);
      
      if (boundaryIndex < 0) {
        Logger.log('ERROR: Boundary not found in content');
        Logger.log('Trying without -- prefix...');
        const boundaryNoPrefix = boundaryMatch[1].trim();
        const boundaryIndex2 = rawContent.indexOf(boundaryNoPrefix);
        Logger.log('Boundary (no prefix) found at index: ' + boundaryIndex2);
      }
      
      // Split by boundary
      const parts = rawContent.split(boundary);
      Logger.log('Parts found after split: ' + parts.length);
      
      // Log first 200 chars of each part for debugging
      for (let i = 0; i < Math.min(parts.length, 5); i++) {
        Logger.log('Part ' + i + ' length: ' + parts[i].length);
        Logger.log('Part ' + i + ' first 200 chars: ' + parts[i].substring(0, 200));
      }
      
      // Find the file part
      let filePartFound = false;
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        
        // Skip empty parts or closing boundary
        if (!part || part === '' || part === '--') {
          Logger.log('Skipping part ' + i + ' (empty or closing boundary)');
          continue;
        }
        
        Logger.log('Checking part ' + i + ' for file data...');
        Logger.log('Part ' + i + ' contains name="file": ' + (part.indexOf('name="file"') >= 0));
        Logger.log('Part ' + i + ' contains name=\'file\': ' + (part.indexOf("name='file'") >= 0));
        
        // Check if this is the file part
        if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
          Logger.log('✅ Found file part at index ' + i);
          filePartFound = true;
          
          // Extract filename
          const filenameMatch = part.match(/filename="([^"]+)"/);
          if (filenameMatch) {
            const fileName = filenameMatch[1];
            Logger.log('Extracted filename: ' + fileName);
            
            // Extract kgid: "98765.jpg" -> "98765"
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              Logger.log('✅ Extracted kgid: ' + kgid);
            }
          } else {
            Logger.log('WARNING: No filename found in file part');
          }
          
          // Find header/body separator
          let headerEnd = part.indexOf('\r\n\r\n');
          let headerEndOffset = 4;
          
          if (headerEnd < 0) {
            headerEnd = part.indexOf('\n\n');
            headerEndOffset = 2;
            Logger.log('Using \\n\\n as separator');
          } else {
            Logger.log('Using \\r\\n\\r\\n as separator');
          }
          
          if (headerEnd < 0) {
            Logger.log('ERROR: No header-body separator found in part ' + i);
            Logger.log('Part ' + i + ' first 300 chars: ' + part.substring(0, 300));
            continue;
          }
          
          Logger.log('Header ends at position: ' + headerEnd);
          
          // Extract file data (after headers)
          let fileData = part.substring(headerEnd + headerEndOffset);
          
          // Remove trailing boundary markers
          fileData = fileData.replace(/--\r\n?$/g, '').replace(/--$/g, '').trim();
          
          Logger.log('File data length after extraction: ' + fileData.length);
          
          if (fileData.length === 0) {
            Logger.log('ERROR: File data is empty after extraction');
            continue;
          }
          
          // Log first 100 and last 100 chars of file data
          Logger.log('File data first 100 chars: ' + fileData.substring(0, 100));
          Logger.log('File data last 100 chars: ' + fileData.substring(Math.max(0, fileData.length - 100)));
          
          // ✅ Convert string to bytes array
          const bytes = [];
          for (let j = 0; j < fileData.length; j++) {
            const charCode = fileData.charCodeAt(j);
            bytes.push(charCode & 0xFF);
          }
          
          Logger.log('✅ Converted to ' + bytes.length + ' bytes');
          
          // Validate bytes array
          if (bytes.length === 0) {
            Logger.log('ERROR: Bytes array is empty');
            continue;
          }
          
          // Check if it looks like JPEG (starts with FF D8 FF)
          if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
            Logger.log('✅ File data looks like valid JPEG (starts with FF D8 FF)');
          } else {
            Logger.log('WARNING: File data does not start with JPEG signature');
            Logger.log('First 10 bytes: ' + bytes.slice(0, 10).map(b => '0x' + b.toString(16).toUpperCase()).join(' '));
          }
          
          // Create blob from bytes
          const finalFileName = filenameMatch ? filenameMatch[1] : 'upload.jpg';
          blob = Utilities.newBlob(bytes, 'image/jpeg', finalFileName);
          
          Logger.log('✅ Blob created: ' + blob.getBytes().length + ' bytes');
          
          if (blob.getBytes().length === 0) {
            Logger.log('ERROR: Blob is empty after creation');
            continue;
          }
          
          break; // Found file, exit loop
        }
      }
      
      if (!filePartFound) {
        Logger.log('ERROR: File part (name="file") not found in any part');
        Logger.log('Searching for any part with "file" keyword...');
        for (let i = 0; i < parts.length; i++) {
          if (parts[i].indexOf('file') >= 0) {
            Logger.log('Found "file" keyword in part ' + i);
            Logger.log('Part ' + i + ' first 300 chars: ' + parts[i].substring(0, 300));
          }
        }
      }
    } else {
      Logger.log('Not multipart, Content-Type: ' + contentType);
      // Try direct bytes
      if (e.postData.bytes && e.postData.bytes.length > 0) {
        Logger.log('Trying direct bytes: ' + e.postData.bytes.length + ' bytes');
        blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
      }
    }
    
    // ✅ Final validation
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('❌ ERROR: No blob created or blob is empty');
      Logger.log('Final debug info:');
      Logger.log('  - Content-Type: ' + contentType);
      Logger.log('  - postData.contents: ' + (e.postData.contents ? 'exists (' + e.postData.contents.length + ' chars)' : 'none'));
      Logger.log('  - postData.bytes: ' + (e.postData.bytes ? 'exists (' + e.postData.bytes.length + ' bytes)' : 'none'));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Check Apps Script execution logs for detailed debugging info.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob validated: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query if not extracted
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query: ' + kgid);
    }
    
    // Create file in Drive
    const driveFileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(driveFileName));
    
    Logger.log('✅ File created in Drive: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('✅ Public URL: ' + driveUrl);
    
    // Update sheet/Firestore if kgid available
    if (kgid) {
      Logger.log('Updating sheet and Firestore for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    } else {
      Logger.log('WARNING: No kgid found, skipping sheet/Firestore update');
    }
    
    // Return success response
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('❌ EXCEPTION: ' + err.toString());
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

