/********** 1. CONFIGURATION **********/

const SHEET_ID            = "1E8cE9zzM3jAHL-a_Cafn5EDWEbk_QNBfOpNtpWwVjfA"; // Your Sheet ID
const SHEET_NAME          = "Emp Profiles";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY    = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ"; // Your API Key
const DRIVE_FOLDER_ID     = "YOUR_DRIVE_FOLDER_ID_HERE"; // Replace with your Google Drive folder ID for storing profile images

/********** 2. BATCH CONFIGURATION **********/

const DOWNLOAD_BATCH_SIZE      = 300; 
const UPLOAD_BATCH_SIZE        = 50; 
const TRIGGER_INTERVAL_MINUTES = 1;
const ARCHIVE_SHEET_NAME       = "Deleted Records";

/********** 3. MENU & UI **********/

function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu("Employee Tools")
    .addItem("Employee Panel", "showEmployeeSidebar")
    .addItem("Reset/Stop Stuck Sync", "stopBatchSync")
    .addToUi();
}

function showEmployeeSidebar() {
  const html = HtmlService.createHtmlOutputFromFile("Sidebar")
    .setTitle("Employee Management")
    .setWidth(350);
  SpreadsheetApp.getUi().showSidebar(html);
}

// Crucial helper for the HTML file
function include(filename) {
  return HtmlService.createHtmlOutputFromFile(filename).getContent();
}

/********** 4. HTTP HANDLERS **********/

function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    const action = e.parameter.action;
    
    if (action === "getEmployees") return getEmployees();
    if (action === "syncFirebaseToSheet") return syncFirebaseToSheet();
    if (action === "syncSheetToFirebase") return syncSheetToFirebaseLatest();
    
    return jsonResponse({ error: "Invalid action" }, 400);
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

/**
 * ✅ FIXED doPost - Handles POST requests (including image uploads)
 * Always returns JSON (never HTML error pages)
 */
function doPost(e) {
  try {
    // Get action from query parameter
    const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : null;
    
    if (!action) {
      return jsonResponse({ 
        success: false, 
        error: "Missing action parameter. Use ?action=uploadImage" 
      }, 400);
    }
    
    Logger.log('doPost called with action: ' + action);
    
    // Route to appropriate handler
    if (action === "uploadImage") {
      return uploadProfileImage(e);
    }
    
    // Unknown action
    return jsonResponse({ 
      success: false, 
      error: "Unknown action: " + action 
    }, 400);
      
  } catch (error) {
    Logger.log('Error in doPost: ' + error.toString());
    // ✅ IMPORTANT: Always return JSON, never let Apps Script return HTML error
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + error.toString() 
    }, 500);
  }
}

function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

/********** 5. SIDEBAR HANDLERS **********/

function sidebar_fullSyncFromFirestore() { 
  return syncFirebaseToSheet(); 
}

function sidebar_fullSyncToFirestore() { 
  return syncSheetToFirebaseLatest(); 
}

function sidebar_startTwoWayBatch() {
  const props = PropertiesService.getScriptProperties();
  props.setProperty("SYNC_MODE", "TWO_WAY");
  props.setProperty("FS_PAGE_TOKEN", "");
  props.setProperty("FS_DONE", "false");
  props.setProperty("S2F_ROW", "2");
  props.setProperty("S2F_DONE", "false");
  
  stopBatchSync(false); 
  ScriptApp.newTrigger("processBatchSync").timeBased().everyMinutes(TRIGGER_INTERVAL_MINUTES).create();
  return { success: true, message: "Two-way batch sync started" };
}

function sidebar_getBatchStatus() {
  const props = PropertiesService.getScriptProperties();
  const mode = props.getProperty("SYNC_MODE") || "Idle";
  const fsDone = props.getProperty("FS_DONE");
  const s2fDone = props.getProperty("S2F_DONE");

  // Calculate Progress %
  let pct = 0;
  if (mode === "TWO_WAY") {
    if (fsDone !== "true") pct = 30;       // Phase 1: Downloading
    else if (s2fDone !== "true") pct = 70; // Phase 2: Uploading
    else pct = 100;                        // Phase 3: Done
  }

  // Get Row Statistics
  const sheet = getSheet();
  const lastRow = Math.max(sheet.getLastRow() - 1, 0); // Exclude header
  let active = 0;
  let deleted = 0;
  
  // Quick scan for stats (only if sheet has data)
  if (lastRow > 0) {
    const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
    const delIdx = headers.indexOf("isDeleted");
    
    if (delIdx > -1) {
      // Get only the isDeleted column to be fast
      const delCol = sheet.getRange(2, delIdx + 1, lastRow, 1).getValues();
      delCol.forEach(r => {
        if (String(r[0]).toLowerCase() === "true") deleted++;
        else active++;
      });
    } else {
      active = lastRow;
    }
  }

  return {
    mode: mode,
    progress: pct,
    total: lastRow,
    active: active,
    deleted: deleted,
    lastSync: Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "HH:mm:ss")
  };
}

function sidebar_softDeleteKgid(kgid) {
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  const headers = data[0];
  const kgidIdx = headers.indexOf("kgid");
  const delIdx = headers.indexOf("isDeleted");
  
  if (kgidIdx < 0 || delIdx < 0) throw new Error("Columns missing");
  
  for (let r = 1; r < data.length; r++) {
    if (String(data[r][kgidIdx]) === String(kgid)) {
      sheet.getRange(r + 1, delIdx + 1).setValue("true");
      const updatedAtIdx = headers.indexOf("updatedAt");
      if (updatedAtIdx >= 0) {
        sheet.getRange(r + 1, updatedAtIdx + 1).setValue(new Date().toISOString());
      }
      sheet.getRange(r + 1, 1, 1, sheet.getLastColumn()).setBackground("#ffcccc");
      
      // Sync delete to Firestore immediately
      const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?currentDocument.exists=true&key=${FIREBASE_API_KEY}`;
      UrlFetchApp.fetch(url, {
        method: "PATCH", 
        contentType: "application/json",
        payload: JSON.stringify({ fields: { isDeleted: { booleanValue: true } } }),
        muteHttpExceptions: true
      });
      return true;
    }
  }
  return false;
}

function sidebar_searchEmployees(query) {
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  if (data.length < 2) return [];
  
  const headers = data[0];
  const q = query.toLowerCase();
  
  // Cache indices
  const iName = headers.indexOf("Name");
  const iMob = headers.indexOf("mobile1");
  const iKgid = headers.indexOf("kgid");
  const iDel = headers.indexOf("isDeleted");
  
  return data.slice(1).filter(row => {
    if (iDel > -1 && String(row[iDel]) === "true") return false;
    const txt = (String(row[iName]) + String(row[iMob]) + String(row[iKgid])).toLowerCase();
    return txt.includes(q);
  }).slice(0, 10).map(row => { // Limit to 10 results for speed
    let obj = {};
    headers.forEach((h, i) => obj[h] = row[i]);
    return obj;
  });
}

/********** 6. FULL SYNC FUNCTIONS (One-time complete sync) **********/

/**
 * Sync Firestore → Google Sheets (Full sync)
 * Fetches all employees from Firestore and updates/creates rows in the sheet
 */
function syncFirebaseToSheet() {
  try {
    Logger.log("Starting Firestore → Sheet sync...");
    
    const sheet = getSheet();
    const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(h => String(h).trim());
    
    // Ensure required columns exist
    const requiredCols = ["kgid", "name", "email", "mobile1", "rank", "district", "station"];
    requiredCols.forEach(col => {
      if (headers.indexOf(col) < 0) {
        sheet.getRange(1, sheet.getLastColumn() + 1).setValue(col);
        headers.push(col);
      }
    });
    
    // Fetch all employees from Firestore
    const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(firestoreUrl, { muteHttpExceptions: true });
    const responseCode = response.getResponseCode();
    
    if (responseCode !== 200) {
      const errorText = response.getContentText();
      Logger.log("Firestore API Error: " + errorText);
      return jsonResponse({ 
        success: false, 
        error: "Failed to fetch from Firestore: " + responseCode,
        details: errorText
      }, responseCode);
    }
    
    const data = JSON.parse(response.getContentText());
    const documents = data.documents || [];
    
    Logger.log(`Found ${documents.length} employees in Firestore`);
    
    // Get existing sheet data
    const sheetData = sheet.getDataRange().getValues();
    const existingKgids = new Map();
    if (sheetData.length > 1) {
      const kgidIdx = headers.indexOf("kgid");
      if (kgidIdx >= 0) {
        for (let r = 1; r < sheetData.length; r++) {
          const kgid = String(sheetData[r][kgidIdx] || "").trim();
          if (kgid) existingKgids.set(kgid, r + 1); // Store row number
        }
      }
    }
    
    let updated = 0;
    let created = 0;
    let errors = 0;
    
    // Process each Firestore document
    for (const doc of documents) {
      try {
        const kgid = doc.name.split("/").pop(); // Extract kgid from document path
        const fields = doc.fields || {};
        
        // Skip deleted employees
        if (fields.isDeleted && (fields.isDeleted.booleanValue === true || fields.isDeleted.stringValue === "true")) {
          continue;
        }
        
        // Convert Firestore fields to plain object
        const employee = {};
        headers.forEach(header => {
          if (fields[header]) {
            const field = fields[header];
            if (field.stringValue !== undefined) employee[header] = field.stringValue;
            else if (field.integerValue !== undefined) employee[header] = field.integerValue;
            else if (field.booleanValue !== undefined) employee[header] = field.booleanValue;
            else if (field.doubleValue !== undefined) employee[header] = field.doubleValue;
            else if (field.timestampValue !== undefined) employee[header] = field.timestampValue;
            else employee[header] = null;
          }
        });
        
        // Ensure kgid is set
        employee.kgid = kgid;
        
        // Prepare row data
        const row = headers.map(header => employee[header] || "");
        
        if (existingKgids.has(kgid)) {
          // Update existing row
          const rowIndex = existingKgids.get(kgid);
          sheet.getRange(rowIndex, 1, 1, headers.length).setValues([row]);
          updated++;
        } else {
          // Append new row
          sheet.appendRow(row);
          created++;
        }
        
        // Update timestamp
        const updatedAtIdx = headers.indexOf("updatedAt");
        if (updatedAtIdx >= 0) {
          const targetRow = existingKgids.has(kgid) ? existingKgids.get(kgid) : sheet.getLastRow();
          sheet.getRange(targetRow, updatedAtIdx + 1).setValue(new Date().toISOString());
        }
        
      } catch (err) {
        Logger.log(`Error processing document ${doc.name}: ${err.toString()}`);
        errors++;
      }
    }
    
    Logger.log(`Sync complete: ${created} created, ${updated} updated, ${errors} errors`);
    
    return jsonResponse({
      success: true,
      message: `Sync complete: ${created} created, ${updated} updated, ${errors} errors`,
      created: created,
      updated: updated,
      errors: errors,
      total: documents.length
    });
    
  } catch (err) {
    Logger.log("syncFirebaseToSheet ERROR: " + err.toString());
    return jsonResponse({ 
      success: false, 
      error: err.toString() 
    }, 500);
  }
}

/**
 * Sync Google Sheets → Firestore (Full sync)
 * Reads all rows from the sheet and updates/creates documents in Firestore
 */
function syncSheetToFirebaseLatest() {
  try {
    Logger.log("Starting Sheet → Firestore sync...");
    
    const sheet = getSheet();
    const data = sheet.getDataRange().getValues();
    
    if (data.length < 2) {
      return jsonResponse({ 
        success: false, 
        error: "No data in sheet" 
      }, 400);
    }
    
    const headers = data[0].map(h => String(h).trim());
    const kgidIdx = headers.indexOf("kgid");
    const delIdx = headers.indexOf("isDeleted");
    
    if (kgidIdx < 0) {
      return jsonResponse({ 
        success: false, 
        error: "kgid column not found" 
      }, 400);
    }
    
    let updated = 0;
    let created = 0;
    let deleted = 0;
    let errors = 0;
    
    // Process each row (skip header)
    for (let r = 1; r < data.length; r++) {
      try {
        const row = data[r];
        const kgid = String(row[kgidIdx] || "").trim();
        
        if (!kgid) {
          Logger.log(`Row ${r + 1}: Skipping - no kgid`);
          continue;
        }
        
        // Check if deleted
        const isDeleted = delIdx >= 0 && (row[delIdx] === true || String(row[delIdx]).toLowerCase() === "true");
        
        // Build employee object with header name mapping
        const employee = {};
        headers.forEach((header, idx) => {
          if (header === "kgid") return; // Skip kgid, it's the document ID
          const value = row[idx];
          if (value !== null && value !== undefined && value !== "") {
            // Map header name to Firestore field name
            let firestoreField = header;
            if (header === "mobil") {
              firestoreField = "mobile2"; // Map "mobil" to "mobile2"
            }
            employee[firestoreField] = value;
          }
        });
        
        // Convert to Firestore format
        const firestoreFields = {};
        
        // ✅ CRITICAL FIX: Always include kgid in document fields
        firestoreFields.kgid = { stringValue: String(kgid) };
        
        Object.keys(employee).forEach(key => {
          const value = employee[key];
          // Skip kgid since we already added it above
          if (key === "kgid") return;
          
          // Handle boolean fields
          if (key === "isDeleted" || key === "isApproved" || key === "isAdmin") {
            firestoreFields[key] = { booleanValue: String(value).toLowerCase() === "true" || value === true };
          } else if (typeof value === "string") {
            firestoreFields[key] = { stringValue: value };
          } else if (typeof value === "number") {
            if (Number.isInteger(value)) {
              firestoreFields[key] = { integerValue: String(value) };
            } else {
              firestoreFields[key] = { doubleValue: value };
            }
          } else if (typeof value === "boolean") {
            firestoreFields[key] = { booleanValue: value };
          } else {
            firestoreFields[key] = { stringValue: String(value) };
          }
        });
        
        // Add isDeleted flag if needed
        if (isDeleted) {
          firestoreFields.isDeleted = { booleanValue: true };
        }
        
        // Add/update timestamp
        firestoreFields.updatedAt = { timestampValue: new Date().toISOString() };
        
        // Check if document exists
        const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?key=${FIREBASE_API_KEY}`;
        const checkResponse = UrlFetchApp.fetch(docUrl, { muteHttpExceptions: true });
        const exists = checkResponse.getResponseCode() === 200;
        
        if (exists) {
          // Update existing document
          const updateUrl = `${docUrl}&updateMask.fieldPaths=${Object.keys(firestoreFields).map(encodeURIComponent).join("&updateMask.fieldPaths=")}`;
          const updateResponse = UrlFetchApp.fetch(updateUrl, {
            method: "PATCH",
            contentType: "application/json",
            payload: JSON.stringify({ fields: firestoreFields }),
            muteHttpExceptions: true
          });
          
          if (updateResponse.getResponseCode() === 200) {
            updated++;
          } else {
            Logger.log(`Failed to update ${kgid}: ${updateResponse.getContentText()}`);
            errors++;
          }
        } else {
          // Create new document
          const createResponse = UrlFetchApp.fetch(docUrl, {
            method: "POST",
            contentType: "application/json",
            payload: JSON.stringify({ fields: firestoreFields }),
            muteHttpExceptions: true
          });
          
          if (createResponse.getResponseCode() === 200 || createResponse.getResponseCode() === 201) {
            created++;
          } else {
            Logger.log(`Failed to create ${kgid}: ${createResponse.getContentText()}`);
            errors++;
          }
        }
        
        // If deleted, also mark in Firestore
        if (isDeleted) {
          deleted++;
        }
        
      } catch (err) {
        Logger.log(`Error processing row ${r + 1}: ${err.toString()}`);
        errors++;
      }
    }
    
    Logger.log(`Sync complete: ${created} created, ${updated} updated, ${deleted} deleted, ${errors} errors`);
    
    return jsonResponse({
      success: true,
      message: `Sync complete: ${created} created, ${updated} updated, ${deleted} deleted, ${errors} errors`,
      created: created,
      updated: updated,
      deleted: deleted,
      errors: errors,
      total: data.length - 1
    });
    
  } catch (err) {
    Logger.log("syncSheetToFirebaseLatest ERROR: " + err.toString());
    return jsonResponse({ 
      success: false, 
      error: err.toString() 
    }, 500);
  }
}

/********** 7. BATCH SYNC FUNCTIONS (Incremental processing) **********/

/* --- SERVICE ACCOUNT AUTH --- */
function getServiceAccountToken() {
  const props = PropertiesService.getScriptProperties();
  const privateKey = props.getProperty("PRIVATE_KEY");
  const clientEmail = props.getProperty("CLIENT_EMAIL");
  
  if (!privateKey || !clientEmail) {
    throw new Error("Service Account properties missing. Please set PRIVATE_KEY and CLIENT_EMAIL in Script Properties.");
  }

  const tokenUrl = "https://oauth2.googleapis.com/token";
  const header = { alg: "RS256", typ: "JWT" };
  const claim = {
    iss: clientEmail, 
    scope: "https://www.googleapis.com/auth/datastore",
    aud: tokenUrl, 
    exp: Math.floor(Date.now()/1000) + 3600, 
    iat: Math.floor(Date.now()/1000)
  };
  
  const toBase64 = s => Utilities.base64EncodeWebSafe(s).replace(/=+$/, "");
  const jwt = toBase64(JSON.stringify(header)) + "." + toBase64(JSON.stringify(claim));
  const signature = Utilities.computeRsaSha256Signature(jwt, privateKey);
  const signedJwt = jwt + "." + Utilities.base64EncodeWebSafe(signature).replace(/=+$/, "");
  
  const resp = UrlFetchApp.fetch(tokenUrl, {
    method: "post", 
    payload: { 
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", 
      assertion: signedJwt 
    },
    muteHttpExceptions: true
  });
  
  const responseData = JSON.parse(resp.getContentText());
  if (!responseData.access_token) {
    throw new Error("Failed to get access token: " + resp.getContentText());
  }
  
  return responseData.access_token;
}

/* --- BATCH CONTROL --- */
function stopBatchSync(clearProps = true) {
  // Delete all triggers
  ScriptApp.getProjectTriggers().forEach(t => {
    if (t.getHandlerFunction() === "processBatchSync") {
      ScriptApp.deleteTrigger(t);
    }
  });
  
  if (clearProps) {
    const props = PropertiesService.getScriptProperties();
    props.deleteProperty("SYNC_MODE");
    props.deleteProperty("FS_DONE");
    props.deleteProperty("S2F_DONE");
    props.deleteProperty("FS_PAGE_TOKEN");
    props.deleteProperty("S2F_ROW");
    
    SpreadsheetApp.getUi().alert(
      "Sync Status Reset", 
      "The system lock has been cleared.\n\n" + 
      "If the 'Syncing...' bar is still visible in the sidebar, close and reopen the sidebar.",
      SpreadsheetApp.getUi().ButtonSet.OK
    );
  }
}

/* --- WORKER --- */
function processBatchSync() {
  const props = PropertiesService.getScriptProperties();
  const mode = props.getProperty("SYNC_MODE");
  
  try {
    if (mode === "TWO_WAY") {
      // 1. Download from Firestore
      if (props.getProperty("FS_DONE") !== "true") {
        if (firestoreToSheetMerge()) {
          props.setProperty("FS_DONE", "true");
        }
        return;
      }
      
      // 2. Upload to Firestore
      if (props.getProperty("S2F_DONE") !== "true") {
        if (sheetToFirestoreBatch()) {
          props.setProperty("S2F_DONE", "true");
        }
        return;
      }
      
      // 3. Archive Deleted Rows
      archiveDeletedRows();

      // 4. Finish
      stopBatchSync(true);
    }
  } catch (e) {
    Logger.log("Sync Error: " + e.toString());
    Logger.log("Stack: " + (e.stack || "No stack"));
  }
}

/* --- MERGE LOGIC --- */
function firestoreToSheetMerge() {
  const props = PropertiesService.getScriptProperties();
  const token = getServiceAccountToken();
  const sheet = getSheet();
  const pageToken = props.getProperty("FS_PAGE_TOKEN") || "";

  let url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?pageSize=${DOWNLOAD_BATCH_SIZE}`;
  if (pageToken) url += `&pageToken=${pageToken}`;

  const resp = UrlFetchApp.fetch(url, { 
    headers: { Authorization: "Bearer " + token }, 
    muteHttpExceptions: true 
  });
  
  if (resp.getResponseCode() !== 200) {
    Logger.log("Firestore API Error: " + resp.getContentText());
    return false;
  }
  
  const json = JSON.parse(resp.getContentText());
  const docs = json.documents || [];

  const lastRow = sheet.getLastRow();
  const sheetData = lastRow > 1 ? sheet.getRange(2, 1, lastRow - 1, sheet.getLastColumn()).getValues() : [];
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(h => String(h).trim());
  
  const kgidMap = new Map();
  const kgidColIdx = headers.indexOf("kgid");
  if (kgidColIdx >= 0) {
    sheetData.forEach((row, i) => { 
      if (row[kgidColIdx]) {
        kgidMap.set(String(row[kgidColIdx]), i + 2); // +2 because sheet is 1-indexed and we skip header
      }
    });
  }

  const newRows = [];
  
  docs.forEach(doc => {
    const f = doc.fields || {};
    const kgid = f.kgid?.stringValue || "";
    if (!kgid) return;

    // Skip deleted records
    if (f.isDeleted && (f.isDeleted.booleanValue === true || f.isDeleted.stringValue === "true")) {
      return;
    }

    const rowArray = headers.map(h => {
      const val = f[h];
      if (!val) return "";
      return val.stringValue || val.integerValue || val.booleanValue || val.doubleValue || val.timestampValue || "";
    });

    if (kgidMap.has(kgid)) {
      // Update existing row
      const rowNum = kgidMap.get(kgid);
      sheet.getRange(rowNum, 1, 1, rowArray.length).setValues([rowArray]);
    } else {
      // New row to add
      newRows.push(rowArray);
    }
  });

  if (newRows.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, newRows.length, newRows[0].length).setValues(newRows);
  }

  if (json.nextPageToken) { 
    props.setProperty("FS_PAGE_TOKEN", json.nextPageToken); 
    return false; // Not done yet
  } else { 
    props.deleteProperty("FS_PAGE_TOKEN");
    return true; // Done
  }
}

function sheetToFirestoreBatch() {
  const props = PropertiesService.getScriptProperties();
  const startRow = parseInt(props.getProperty("S2F_ROW") || "2");
  const sheet = getSheet();
  const lastRow = sheet.getLastRow();
  
  if (startRow > lastRow) {
    return true; // Done
  }

  const endRow = Math.min(startRow + UPLOAD_BATCH_SIZE - 1, lastRow);
  const data = sheet.getRange(startRow, 1, endRow - startRow + 1, sheet.getLastColumn()).getValues();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(h => String(h).trim());
  const token = getServiceAccountToken();

  let processed = 0;
  let errors = 0;

  data.forEach(row => {
    try {
      const kgidIdx = headers.indexOf("kgid");
      let kgid = kgidIdx >= 0 ? String(row[kgidIdx] || "").trim() : "";
      
      if (!kgid) return;

      let fields = {};
      
      // ✅ CRITICAL FIX: Always include kgid in document fields
      fields.kgid = { stringValue: String(kgid) };
      
      headers.forEach((h, i) => {
        if (h === "kgid") return; // Skip kgid header, already added above
        
        // Map header name to Firestore field name
        let firestoreField = h;
        if (h === "mobil") {
          firestoreField = "mobile2"; // Map "mobil" to "mobile2"
        }
        
        if (row[i] !== "" && row[i] != null && row[i] !== undefined) {
          let val = row[i];
          if (firestoreField === "isDeleted" || firestoreField === "isApproved" || firestoreField === "isAdmin") {
            fields[firestoreField] = { booleanValue: String(val).toLowerCase() === "true" || val === true };
          } else if (typeof val === "number") {
            if (Number.isInteger(val)) {
              fields[firestoreField] = { integerValue: String(val) };
            } else {
              fields[firestoreField] = { doubleValue: val };
            }
          } else if (typeof val === "boolean") {
            fields[firestoreField] = { booleanValue: val };
          } else {
            fields[firestoreField] = { stringValue: String(val) };
          }
        }
      });

      // Add timestamp
      fields.updatedAt = { timestampValue: new Date().toISOString() };

      const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}`;
      const response = UrlFetchApp.fetch(docUrl, {
        method: "PATCH", 
        contentType: "application/json", 
        headers: { Authorization: "Bearer " + token },
        payload: JSON.stringify({ fields: fields }), 
        muteHttpExceptions: true
      });

      if (response.getResponseCode() === 200 || response.getResponseCode() === 201) {
        processed++;
      } else {
        Logger.log(`Failed to sync ${kgid}: ${response.getContentText()}`);
        errors++;
      }
    } catch (e) {
      Logger.log(`Error processing row: ${e.toString()}`);
      errors++;
    }
  });

  props.setProperty("S2F_ROW", String(endRow + 1));
  
  Logger.log(`Processed ${processed} rows, ${errors} errors. Next start: ${endRow + 1}`);
  
  return endRow >= lastRow; // Return true if done
}

/* --- ARCHIVER FUNCTION --- */
function archiveDeletedRows() {
  try {
    const sheet = getSheet();
    const ss = sheet.getParent();
    let archiveSheet = ss.getSheetByName(ARCHIVE_SHEET_NAME);
    
    if (!archiveSheet) {
      // Auto-create if missing
      archiveSheet = ss.insertSheet(ARCHIVE_SHEET_NAME);
      const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
      archiveSheet.appendRow(headers);
    }

    const lastRow = sheet.getLastRow();
    if (lastRow < 2) return; // No data rows

    const data = sheet.getRange(2, 1, lastRow - 1, sheet.getLastColumn()).getValues();
    const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(h => String(h).trim());
    const delIdx = headers.indexOf("isDeleted");

    if (delIdx === -1) {
      Logger.log("No isDeleted column found, skipping archive");
      return;
    }

    // Identify rows to move
    const rowsToMove = [];
    const rowsToKeep = [];

    data.forEach(row => {
      // Check if isDeleted is "true" (string or boolean)
      if (String(row[delIdx]).toLowerCase() === "true") {
        rowsToMove.push(row);
      } else {
        rowsToKeep.push(row);
      }
    });

    // 1. Move to Archive
    if (rowsToMove.length > 0) {
      archiveSheet.getRange(archiveSheet.getLastRow() + 1, 1, rowsToMove.length, rowsToMove[0].length).setValues(rowsToMove);
      Logger.log(`Archived ${rowsToMove.length} deleted rows`);
    }

    // 2. Clear & Rewrite Main Sheet (Safest way to delete scattered rows)
    // Only if we actually moved something
    if (rowsToMove.length > 0) {
      // Clear all data rows (keep header)
      if (lastRow > 1) {
        sheet.getRange(2, 1, lastRow - 1, sheet.getLastColumn()).clearContent();
      }
      
      // Write back only non-deleted rows
      if (rowsToKeep.length > 0) {
        sheet.getRange(2, 1, rowsToKeep.length, rowsToKeep[0].length).setValues(rowsToKeep);
      }
      
      Logger.log(`Kept ${rowsToKeep.length} active rows in main sheet`);
    }
  } catch (err) {
    Logger.log("archiveDeletedRows ERROR: " + err.toString());
  }
}

/********** 8. UTILITIES **********/

function getSheet() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) throw new Error("Tab '" + SHEET_NAME + "' not found.");
  return sheet;
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

/********** 9. PROFILE IMAGE UPLOAD **********/

/**
 * ✅ FIXED: Upload profile image - Always returns JSON
 * Handles base64 JSON uploads from Android app
 */
function uploadProfileImage(e) {
  try {
    Logger.log('uploadProfileImage called');
    
    if (!e || !e.postData) {
      return jsonResponse({ 
        success: false, 
        error: "No POST data received" 
      }, 400);
    }
    
    // Get the request body
    let body = "";
    try {
      body = e.postData.contents || "";
    } catch (err) {
      Logger.log("Error reading postData.contents: " + err);
      body = "";
    }
    
    if (!body || body.trim().length === 0) {
      return jsonResponse({ 
        success: false, 
        error: "Empty request body" 
      }, 400);
    }
    
    Logger.log('Request body length: ' + body.length);
    Logger.log('First 100 chars: ' + body.substring(0, 100));
    
    // Try to parse as JSON
    let jsonData = null;
    try {
      const trimmed = body.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        jsonData = JSON.parse(trimmed);
        Logger.log('✅ JSON parsed successfully');
      } else {
        return jsonResponse({ 
          success: false, 
          error: "Invalid JSON format. Expected JSON object with 'image' field." 
        }, 400);
      }
    } catch (parseErr) {
      Logger.log('JSON parse error: ' + parseErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Failed to parse JSON: " + parseErr.toString() 
      }, 400);
    }
    
    // Extract image data
    if (!jsonData || !jsonData.image) {
      return jsonResponse({ 
        success: false, 
        error: "Missing 'image' field in JSON" 
      }, 400);
    }
    
    const base64Image = jsonData.image;
    const filename = jsonData.filename || ("employee_" + new Date().getTime() + ".jpg");
    const kgid = jsonData.kgid || e.parameter.kgid || null;
    
    // Extract base64 data (remove data:image/jpeg;base64, prefix if present)
    let base64 = base64Image;
    if (base64.indexOf(",") >= 0) {
      base64 = base64.split(",")[1];
    }
    
    // Decode base64 to bytes
    let imageBytes;
    try {
      imageBytes = Utilities.base64Decode(base64);
      Logger.log('Base64 decoded to ' + imageBytes.length + ' bytes');
    } catch (decodeErr) {
      Logger.log('Base64 decode error: ' + decodeErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Invalid base64 image data" 
      }, 400);
    }
    
    if (!imageBytes || imageBytes.length === 0) {
      return jsonResponse({ 
        success: false, 
        error: "Image data is empty after decoding" 
      }, 400);
    }
    
    // Create blob
    const blob = Utilities.newBlob(imageBytes, "image/jpeg", filename);
    
    // Get Drive folder
    let folder;
    try {
      if (!DRIVE_FOLDER_ID || DRIVE_FOLDER_ID === "YOUR_DRIVE_FOLDER_ID_HERE") {
        // Try to find or create a folder named "Employee Photos"
        const folders = DriveApp.getFoldersByName("Employee Photos");
        if (folders.hasNext()) {
          folder = folders.next();
        } else {
          folder = DriveApp.createFolder("Employee Photos");
          Logger.log('Created new folder: Employee Photos');
        }
      } else {
        folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
      }
    } catch (folderErr) {
      Logger.log('Drive folder error: ' + folderErr.toString());
      return jsonResponse({ 
        success: false, 
        error: "Drive folder not found. Check DRIVE_FOLDER_ID or create 'Employee Photos' folder." 
      }, 500);
    }
    
    // Create file in Drive
    const file = folder.createFile(blob);
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('File created: ' + fileId);
    Logger.log('Public URL: ' + driveUrl);
    
    // Update Firestore if kgid provided
    if (kgid) {
      try {
        Logger.log('Updating Firestore for kgid: ' + kgid);
        const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?key=${FIREBASE_API_KEY}`;
        const updateResponse = UrlFetchApp.fetch(docUrl, {
          method: "PATCH",
          contentType: "application/json",
          payload: JSON.stringify({ 
            fields: { 
              photoUrl: { stringValue: driveUrl },
              updatedAt: { timestampValue: new Date().toISOString() }
            } 
          }),
          muteHttpExceptions: true
        });
        if (updateResponse.getResponseCode() === 200) {
          Logger.log('✅ Firestore updated successfully');
        } else {
          Logger.log('⚠️ Firestore update failed: ' + updateResponse.getContentText());
        }
      } catch (firestoreErr) {
        Logger.log('Firestore update error: ' + firestoreErr.toString());
        // Don't fail the upload if Firestore update fails
      }
    }
    
    // ✅ Return success response
    return jsonResponse({
      success: true,
      url: driveUrl,
      id: fileId,
      message: "Image uploaded successfully"
    }, 200);
    
  } catch (error) {
    Logger.log('Error in uploadProfileImage: ' + error.toString());
    Logger.log('Stack trace: ' + error.stack);
    
    // ✅ Always return JSON error, never HTML
    return jsonResponse({ 
      success: false, 
      error: "Upload failed: " + error.toString() 
    }, 500);
  }
}




