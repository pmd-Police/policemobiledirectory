/**
 * ========== OFFICERS APPS SCRIPT - COMPLETE ==========
 * Handles CRUD operations and sync to Firestore
 */

/** ========== CONFIG ============= */
// Option 1: Use Sheet ID (RECOMMENDED - More reliable)
const SHEET_ID = ""; // ⚠️ UPDATE THIS: Your Google Sheet ID (from URL: /spreadsheets/d/SHEET_ID/edit)

// Option 2: Use Sheet Name (if script is bound to spreadsheet)
const SHEET_NAME = "Office Profiles"; // Change if your sheet has a different name

// Firebase Configuration
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";
const STATUS_HEADER = "uploadStatus";

/**
 * Get the spreadsheet (prefer SHEET_ID, fallback to active)
 */
function getSpreadsheet() {
  if (SHEET_ID && SHEET_ID.trim() !== "") {
    return SpreadsheetApp.openById(SHEET_ID);
  }
  return SpreadsheetApp.getActive();
}

/**
 * Get the sheet with error handling
 */
function getSheet() {
  const ss = getSpreadsheet();
  if (!ss) {
    throw new Error("Spreadsheet not found. Please set SHEET_ID in config.");
  }
  
  const sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) {
    // Try to get first sheet if named sheet doesn't exist
    const firstSheet = ss.getSheets()[0];
    if (firstSheet) {
      Logger.log("Warning: Sheet '" + SHEET_NAME + "' not found. Using first sheet: " + firstSheet.getName());
      return firstSheet;
    }
    throw new Error("Sheet '" + SHEET_NAME + "' not found and no sheets available.");
  }
  return sheet;
}

function ensureStatusColumn(sheet) {
  let lastCol = sheet.getLastColumn();
  if (lastCol === 0) {
    sheet.getRange(1, 1).setValue(STATUS_HEADER);
    return 1;
  }
  const headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0];
  let idx = headers.indexOf(STATUS_HEADER);
  if (idx === -1) {
    sheet.getRange(1, lastCol + 1).setValue(STATUS_HEADER);
    return lastCol + 1;
  }
  return idx + 1;
}

/**
 * Auto-generate AGID in format: AGID0001, AGID0002, ...
 */
function generateAgid() {
  const sheet = getSheet();
  const lastRow = sheet.getLastRow();
  
  if (lastRow < 2) {
    return "AGID0001"; 
  }
  
  const lastAgid = sheet.getRange(lastRow, 1).getValue(); // e.g., AGID0007
  if (!lastAgid || lastAgid.toString().trim() === "") {
    return "AGID0001";
  }
  
  const numberPart = parseInt(lastAgid.toString().replace("AGID", ""), 10);
  if (isNaN(numberPart)) {
    return "AGID0001";
  }
  
  const next = numberPart + 1;
  return "AGID" + next.toString().padStart(4, "0");
}

/**
 * ========== JSON RESPONSE HELPER ==========
 */
function jsonResponse(obj, statusCode = 200) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

/**
 * ========== FETCH ALL OFFICERS (GET) ==========
 */
function doGet(e) {
  try {
    // Handle sync action
    if (e && e.parameter && e.parameter.action === "syncOfficersSheetToFirebase") {
      return syncOfficersSheetToFirebase();
    }
    
    // Default: return all officers
    const sheet = getSheet();
    const data = sheet.getDataRange().getValues();
    
    if (data.length < 2) {
      return jsonResponse([]);
    }
    
    const headers = data[0];
    const rows = data.slice(1);
    
    const output = rows
      .filter(row => row[0]) // Filter out empty rows (no AGID)
      .map(r => {
        let obj = {};
        headers.forEach((h, i) => {
          obj[h] = r[i] || "";
        });
        return obj;
      });
    
    return jsonResponse(output);
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

/**
 * ========== ADD OR UPDATE OFFICER (POST) ==========
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({ error: "No data provided" }, 400);
    }
    
    const body = JSON.parse(e.postData.contents);
    
    if (body.action === "update") {
      const result = updateOfficer(body);
      return jsonResponse(result);
    }
    
    // ===== DEFAULT ACTION: ADD NEW OFFICER =====
    const sheet = getSheet();
    
    // Ensure headers exist
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(["agid", "name", "mobile", "landline", "rank", "station", "district", "email", "photoUrl", STATUS_HEADER]);
    }
    var statusCol = ensureStatusColumn(sheet);
    
    const agid = generateAgid();
    const row = [
      agid,
      body.name || "",
      body.mobile || "",
      body.landline || "",
      body.rank || "",
      body.station || "",
      body.district || "",
      body.email || "",
      body.photoUrl || "",
      "Pending Upload"
    ];
    
    sheet.appendRow(row);
    const newRow = sheet.getLastRow();
    
    // Optionally sync to Firestore immediately
    try {
      writeOfficerToFirestore({
        agid: agid,
        name: body.name || "",
        mobile: body.mobile || "",
        landline: body.landline || "",
        rank: body.rank || "",
        station: body.station || "",
        district: body.district || "",
        email: body.email || "",
        photoUrl: body.photoUrl || ""
      });
    } catch (firestoreErr) {
      Logger.log("Firestore sync failed (non-critical): " + firestoreErr.toString());
      sheet.getRange(newRow, statusCol).setValue("Failed: " + firestoreErr.message);
      return jsonResponse({
        status: "added_with_error",
        agid: agid,
        error: firestoreErr.message
      });
    }

    sheet.getRange(newRow, statusCol).setValue("Uploaded at " + new Date().toLocaleString());
    
    return jsonResponse({
      status: "added",
      agid: agid
    });
  } catch (err) {
    Logger.log("doPost ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

/**
 * ========== UPDATE OFFICER BY AGID ==========
 */
function updateOfficer(data) {
  const sheet = getSheet();
  const values = sheet.getDataRange().getValues();
  const statusCol = ensureStatusColumn(sheet);
  
  for (let i = 1; i < values.length; i++) {
    if (values[i][0] === data.agid) {
      sheet.getRange(i + 1, 2).setValue(data.name || "");
      sheet.getRange(i + 1, 3).setValue(data.mobile || "");
      sheet.getRange(i + 1, 4).setValue(data.landline || "");
      sheet.getRange(i + 1, 5).setValue(data.rank || "");
      sheet.getRange(i + 1, 6).setValue(data.station || "");
      sheet.getRange(i + 1, 7).setValue(data.district || "");
      sheet.getRange(i + 1, 8).setValue(data.email || "");
      sheet.getRange(i + 1, 9).setValue(data.photoUrl || "");
      sheet.getRange(i + 1, statusCol).setValue("Pending Upload");
      
      // Sync to Firestore
      try {
        writeOfficerToFirestore({
          agid: data.agid,
          name: data.name || "",
          mobile: data.mobile || "",
          landline: data.landline || "",
          rank: data.rank || "",
          station: data.station || "",
          district: data.district || "",
          email: data.email || "",
          photoUrl: data.photoUrl || ""
        });
        sheet.getRange(i + 1, statusCol).setValue("Uploaded at " + new Date().toLocaleString());
      } catch (firestoreErr) {
        Logger.log("Firestore sync failed (non-critical): " + firestoreErr.toString());
        sheet.getRange(i + 1, statusCol).setValue("Failed: " + firestoreErr.message);
      }
      
      return { status: "updated", agid: data.agid };
    }
  }
  
  return { status: "not_found" };
}

/**
 * ========== SYNC OFFICERS FROM SHEET TO FIRESTORE ==========
 */
function syncOfficersSheetToFirebase() {
  try {
    const sheet = getSheet();
    const data = sheet.getDataRange().getValues();
    
    if (data.length < 2) {
      return jsonResponse({ message: "Sheet is empty", count: 0 });
    }
    
    const headers = data[0];
    const rows = data.slice(1);
    
    let count = 0;
    let errors = [];
    
    // Find column indices
    const agidIdx = headers.indexOf("agid");
    const nameIdx = headers.indexOf("name");
    const mobileIdx = headers.indexOf("mobile");
    const landlineIdx = headers.indexOf("landline");
    const rankIdx = headers.indexOf("rank");
    const stationIdx = headers.indexOf("station");
    const districtIdx = headers.indexOf("district");
    const emailIdx = headers.indexOf("email");
    const photoUrlIdx = headers.indexOf("photoUrl");
    const statusCol = ensureStatusColumn(sheet);
    const statusIdx = statusCol - 1;
    
    // Process each row
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const agid = row[agidIdx];
      
      // Skip rows without AGID
      if (!agid || agid.toString().trim() === "") {
        continue;
      }
      
      try {
        const officer = {
          agid: agid.toString().trim(),
          name: row[nameIdx] ? row[nameIdx].toString().trim() : "",
          mobile: row[mobileIdx] ? row[mobileIdx].toString().trim() : "",
          landline: row[landlineIdx] ? row[landlineIdx].toString().trim() : "",
          rank: row[rankIdx] ? row[rankIdx].toString().trim() : "",
          station: row[stationIdx] ? row[stationIdx].toString().trim() : "",
          district: row[districtIdx] ? row[districtIdx].toString().trim() : "",
          email: row[emailIdx] ? row[emailIdx].toString().trim() : "",
          photoUrl: row[photoUrlIdx] ? row[photoUrlIdx].toString().trim() : ""
        };
        
        writeOfficerToFirestore(officer);
        count++;
        if (statusIdx !== -1) {
          sheet.getRange(i + 2, statusIdx + 1).setValue("Uploaded at " + new Date().toLocaleString());
        }
        
        // Log progress every 50 records
        if (count % 50 === 0) {
          Logger.log("Synced " + count + " officers...");
        }
      } catch (err) {
        errors.push({ agid: agid, error: err.toString() });
        Logger.log("Error syncing officer " + agid + ": " + err.toString());
        if (statusIdx !== -1) {
          sheet.getRange(i + 2, statusIdx + 1).setValue("Failed: " + err.message);
        }
      }
    }
    
    const message = `Synced ${count} officers to Firestore${errors.length > 0 ? ` (${errors.length} errors)` : ""}`;
    Logger.log(message);
    
    return jsonResponse({
      success: true,
      message: message,
      count: count,
      errors: errors.length > 0 ? errors : undefined
    });
    
  } catch (err) {
    Logger.log("syncOfficersSheetToFirebase ERROR: " + err.toString());
    return jsonResponse({ 
      success: false, 
      error: err.toString() 
    }, 500);
  }
}

/**
 * ========== WRITE OFFICER TO FIRESTORE ==========
 */
function writeOfficerToFirestore(officer) {
  if (!officer.agid) {
    throw new Error("AGID is required");
  }
  
  const docPath = "projects/" + FIREBASE_PROJECT_ID + 
                  "/databases/(default)/documents/officers/" + 
                  encodeURIComponent(officer.agid);
  
  const url = "https://firestore.googleapis.com/v1/" + docPath + "?key=" + FIREBASE_API_KEY;
  
  const payload = {
    fields: {
      agid: { stringValue: officer.agid || "" },
      name: { stringValue: officer.name || "" },
      mobile: { stringValue: officer.mobile || "" },
      landline: { stringValue: officer.landline || "" },
      rank: { stringValue: officer.rank || "" },
      station: { stringValue: officer.station || "" },
      district: { stringValue: officer.district || "" },
      email: { stringValue: officer.email || "" },
      photoUrl: { stringValue: officer.photoUrl || "" }
    }
  };
  
  const options = {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json"
    },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  };
  
  const response = UrlFetchApp.fetch(url, options);
  const responseCode = response.getResponseCode();
  const responseText = response.getContentText();
  
  if (responseCode !== 200 && responseCode !== 201) {
    throw new Error("Firestore API error: " + responseCode + " - " + responseText);
  }
  
  return JSON.parse(responseText);
}

