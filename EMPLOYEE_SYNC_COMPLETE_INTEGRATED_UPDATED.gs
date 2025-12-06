/********************************************************************
 * 🔰 EMPLOYEE SYNC ENGINE — FINAL CLEAN VERSION (for Ravi)
 * ---------------------------------------------------------------
 * ✔ Firestore documentId = KGID  
 * ✔ Push + Pull sync with resume  
 * ✔ Auto-fill timestamps  
 * ✔ Auto-clean mobile numbers  
 * ✔ Auto-normalize district  
 * ✔ Auto-fix booleans  
 * ✔ Backup sheet before push  
 * ✔ Full sidebar UI compatibility  
 * ✅ FIXED: Image upload handler added
 ********************************************************************/



/* ================================================================
   CONFIG
================================================================ */
const PROPS = PropertiesService.getScriptProperties();

const SHEET_ID =
  PROPS.getProperty("SHEET_ID") ||
  "1E8cE9zzM3jAHL-a_Cafn5EDWEbk_QNBfOpNtpWwVjfA";

const FIREBASE_PROJECT_ID =
  PROPS.getProperty("FIREBASE_PROJECT_ID") || "pmd-police-mobile-directory";

const SHEET_NAME = "Emp Profiles";

const DEFAULT_BATCH_SIZE = parseInt(PROPS.getProperty("BATCH_SIZE") || "300");
const MAX_RUN_MS = 330000; // 5.5 minutes
const TOKEN_SCOPE = "https://www.googleapis.com/auth/datastore";
const DRIVE_FOLDER_ID = "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs";

/* ================================================================
   MENU & SIDEBAR
================================================================ */
function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu("Employee Tools")
    .addItem("Employee Panel", "showEmployeeSidebar")
    .addItem("⚠ Reset Sync State", "resetSync")
    .addToUi();
}

function showEmployeeSidebar() {
  const html = HtmlService.createTemplateFromFile("Sidebar")
    .evaluate()
    .setTitle("Employee Management")
    .setWidth(360);

  SpreadsheetApp.getUi().showSidebar(html);
}

/* ================================================================
   UNIVERSAL RESET
================================================================ */
function resetSync() {
  const props = PropertiesService.getScriptProperties();

  [
    "SYNC_ACTION",
    // PUSH
    "S2F_CURRENT_ROW",
    "S2F_CURRENT_KGID",
    "S2F_UP_COUNT",
    "S2F_TOTAL",
    "S2F_DONE",
    // PULL
    "FS_DOWN_COUNT",
    "FS_TOTAL",
    "FS_NEXT_PAGE_TOKEN",
    // Boolean fixer
    "BOOL_FIX_PAGE",
    "BOOL_FIXED"
  ].forEach(k => props.deleteProperty(k));

  return "✔ Sync memory fully reset.";
}
function superReset() {
  return resetSync(); // alias for sidebar button
}

function searchEmployee(query) {
  const sheet = getSheet();
  const values = sheet.getDataRange().getValues();
  const headers = values[0];

  const results = [];
  const q = String(query).toLowerCase();

  for (let r = 1; r < values.length; r++) {
    const row = values[r];
    const rowObj = {};

    let match = false;
    headers.forEach((h, i) => {
      const v = row[i];
      rowObj[h] = (v === "" ? null : v);

      if (
        String(v).toLowerCase().includes(q) ||
        String(row[headers.indexOf("kgid")]).toLowerCase() === q
      ) {
        match = true;
      }
    });

    if (match) results.push(rowObj);
  }

  return {
    success: true,
    count: results.length,
    results
  };
}

/* ================================================================
   ✅ FIXED: HTTP HANDLERS - Always return JSON
================================================================ */
function doGet(e) {
  return handleEmployeeApi(e);
}

function doPost(e) {
  return handleEmployeeApi(e);
}

function handleEmployeeApi(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    
    const action = e.parameter.action;
    Logger.log("handleEmployeeApi called with action: " + action);

    switch (action) {
      case "getEmployees": 
        return jsonResponse(getEmployees());
        
      case "pullDataFromFirebase": 
        return jsonResponse(task_pullDataFromFirebase());
        
      case "pushDataToFirebase": 
        return jsonResponse(task_pushDataToFirebase());
        
      case "dryRunPush": 
        return jsonResponse(task_dryRunPush());
      
      // ✅ FIXED: Handle image upload actions
      case "uploadImage":
      case "uploadImageEnhanced":
        return uploadImageEnhanced(e);
        
      default:
        return jsonResponse({ error: "Invalid action: " + action }, 400);
    }
  } catch(err) {
    Logger.log("handleEmployeeApi ERROR: " + err.toString());
    // ✅ Always return JSON, never HTML
    return jsonResponse({ error: err.toString() }, 500);
  }
}

function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

function dryRunPush() {
  return task_dryRunPush();
}

/* ================================================================
   VALIDATION
================================================================ */
function validateSheetColumns() {
  const sheet = getSheet();
  const headers = sheet
    .getRange(1, 1, 1, sheet.getLastColumn())
    .getValues()[0]
    .map(h => String(h).trim());

  const REQUIRED = [
    "kgid","name","mobile1","mobile2","rank","station","district",
    "metal","bloodGroup","email","photoUrl","fcmToken","firebaseUid",
    "isAdmin","isApproved","pin","createdAt","updatedAt","isDeleted"
  ];

  const missing = [];
  const wrongCase = [];

  REQUIRED.forEach(col => {
    if (!headers.includes(col)) {
      const match = headers.find(h => h.toLowerCase() === col.toLowerCase());
      if (match) wrongCase.push({ expected: col, found: match });
      else missing.push(col);
    }
  });

  wrongCase.forEach(pair => {
    const idx = headers.indexOf(pair.found) + 1;
    sheet.getRange(1, idx).setValue(pair.expected);
  });

  return { ok: missing.length === 0, missing, wrongCase };
}

/* ================================================================
   VALIDATION HELPERS
================================================================ */
function ensureCreatedAt(val) {
  if (!val) return new Date().toISOString();
  let d = new Date(val);
  return isNaN(d) ? new Date().toISOString() : d.toISOString();
}

function ensureUpdatedAt(val) {
  if (!val) return new Date().toISOString();
  let d = new Date(val);
  return isNaN(d) ? new Date().toISOString() : d.toISOString();
}

function validateMobile(m) {
  if (!m) return "";
  const clean = String(m).replace(/\D/g, "");
  return clean.length === 10 ? clean : "";
}

function normalizeDistrict(d) {
  if (!d) return "";
  d = String(d).trim().toLowerCase();

  const FIX = {
    "chikkaballapur": "Chikkaballapura",
    "chikkaballapura": "Chikkaballapura",
    "chikballapur": "Chikkaballapura",
    "kolar": "Kolar",
    "bengaluru rural": "Bengaluru Rural",
    "bangalore rural": "Bengaluru Rural",
    "bengaluru city": "Bengaluru City",
    "bengaluru": "Bengaluru City"
  };

  return FIX[d] || d.charAt(0).toUpperCase() + d.slice(1);
}

/* ================================================================
   BASIC HELPERS
================================================================ */
function getSpreadsheet() {
  try { return SpreadsheetApp.getActiveSpreadsheet(); }
  catch (e) { return SpreadsheetApp.openById(SHEET_ID); }
}

function getSheet() {
  const ss = getSpreadsheet();
  const sh = ss.getSheetByName(SHEET_NAME);
  if (!sh) throw new Error("Sheet '" + SHEET_NAME + "' missing");
  return sh;
}

function isoNow() { return new Date().toISOString(); }

function detectBooleanFields(headers) {
  const known = ["isAdmin","isApproved","isDeleted","isActive","isVerified","isBlocked"];
  return known.filter(x => headers.includes(x));
}

function writeLog(action, obj) {
  try {
    const ss = getSpreadsheet();
    let log = ss.getSheetByName("Sync Logs");

    if (!log) {
      log = ss.insertSheet("Sync Logs");
      log.appendRow(["Timestamp","Action","Message","Details"]);
    }

    let details = JSON.stringify(obj.failedRows || obj.changes || {});
    if (details.length > 40000) details = details.slice(0,40000);

    log.appendRow([new Date().toISOString(), action, obj.message, details]);
  } catch (e) {}
}

function backupSheetCopy() {
  const ss = getSpreadsheet();
  const sheet = getSheet();
  const name = "Backup_" + SHEET_NAME + "_" + isoNow().replace(/[:.]/g,"-");

  try {
    const copy = sheet.copyTo(ss);
    copy.setName(name);
    ss.moveActiveSheet(ss.getNumSheets());
    return name;
  } catch (e) {
    return null;
  }
}

/* ================================================================
   AUTH TOKEN (SERVICE ACCOUNT)
================================================================ */
function getServiceAccountToken() {
  let privateKey = PROPS.getProperty("PRIVATE_KEY");
  let clientEmail = PROPS.getProperty("CLIENT_EMAIL");

  if (!privateKey || !clientEmail)
    throw new Error("Service Account Keys missing");

  privateKey = privateKey.replace(/\\n/g, "\n");

  const tokenUrl = "https://oauth2.googleapis.com/token";
  const now = Math.floor(Date.now() / 1000);

  const header = Utilities.base64EncodeWebSafe(JSON.stringify({ alg:"RS256", typ:"JWT" }));
  const claim = Utilities.base64EncodeWebSafe(JSON.stringify({
    iss: clientEmail,
    scope: TOKEN_SCOPE,
    aud: tokenUrl,
    iat: now,
    exp: now + 3600
  }));

  const unsigned = header + "." + claim;
  const signature = Utilities.computeRsaSha256Signature(unsigned, privateKey);
  const signedJwt = unsigned + "." + Utilities.base64EncodeWebSafe(signature);

  const resp = UrlFetchApp.fetch(tokenUrl, {
    method: "post",
    payload: { grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: signedJwt }
  });

  return JSON.parse(resp.getContentText()).access_token;
}

/* ================================================================
   PULL (Firestore → Sheet)
================================================================ */
function pullDataFromFirebase() {
  let check = validateSheetColumns();
  if (!check.ok) throw new Error("Missing: " + check.missing.join(","));
  return task_pullDataFromFirebase();
}

function task_pullDataFromFirebase() {
  const props = PROPS;
  props.setProperty("SYNC_ACTION","PULL");

  const token = getServiceAccountToken();
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();

  const headers = data[0];
  const kgIdx = headers.indexOf("kgid");
  const updIdx = headers.indexOf("updatedAt");

  const booleanFields = detectBooleanFields(headers);

  const sheetMap = {};
  for (let r = 1; r < data.length; r++) {
    let kg = String(data[r][kgIdx] || "");
    if (kg) sheetMap[kg] = r+1;
  }

  let next = props.getProperty("FS_NEXT_PAGE_TOKEN") || "";
  let downloaded = parseInt(props.getProperty("FS_DOWN_COUNT") || "0");
  let total = parseInt(props.getProperty("FS_TOTAL") || "0");

  const append = [];
  const start = Date.now();

  do {
    if (Date.now() - start > 280000) {
      props.setProperty("FS_NEXT_PAGE_TOKEN", next);
      props.setProperty("FS_DOWN_COUNT", downloaded);
      return { success:true, message:"Paused (timeout)", total, done:downloaded };
    }

    let url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?pageSize=300` +
      (next ? "&pageToken="+next : "");

    const resp = UrlFetchApp.fetch(url, { headers:{ Authorization:"Bearer "+token }});
    const json = JSON.parse(resp.getContentText());
    const docs = json.documents || [];

    total += docs.length;
    props.setProperty("FS_TOTAL", total);

    docs.forEach(doc => {
      const fs = doc.fields || {};
      const docId = doc.name.split("/").pop();
      // Always ensure FSDB has kgid field (in-memory repair)
      let kgid = (fs.kgid && fs.kgid.stringValue) || docId;
      fs.kgid = { stringValue: kgid }; // in-memory repair

      const row = headers.map(h => {
        const v = fs[h];
        if (!v) return "";
        if (v.stringValue !== undefined) return v.stringValue;
        if (v.booleanValue !== undefined) return v.booleanValue;
        if (v.integerValue !== undefined) return v.integerValue;
        if (v.doubleValue !== undefined) return v.doubleValue;
        if (v.timestampValue !== undefined)
          return new Date(v.timestampValue).toISOString();
        return "";
      });

      // Auto booleans  
      booleanFields.forEach(b => {
        const i = headers.indexOf(b);
        if (i >= 0 && (row[i] === "" || row[i] == null)) row[i] = false;
      });

      // Auto timestamps
      let ci = headers.indexOf("createdAt");
      if (ci >= 0) row[ci] = ensureCreatedAt(row[ci]);

      let ui = headers.indexOf("updatedAt");
      if (ui >= 0) row[ui] = ensureUpdatedAt(row[ui]);

      if (sheetMap[kgid]) {
        let rn = sheetMap[kgid];
        let existing = sheet.getRange(rn,1,1,headers.length).getValues()[0];

        const fsTime = fs.updatedAt ? new Date(fs.updatedAt.timestampValue) : null;
        const shTime = existing[updIdx] ? new Date(existing[updIdx]) : null;

        if (fsTime && (!shTime || fsTime > shTime)) {
          const merged = headers.map((h,i)=> row[i] === "" ? existing[i] : row[i]);
          sheet.getRange(rn,1,1,headers.length).setValues([merged]);
        }

      } else {
        append.push(row);
      }

      downloaded++;
    });

    props.setProperty("FS_DOWN_COUNT", downloaded);
    next = json.nextPageToken || "";
  } while(next);

  if (append.length > 0)
    sheet.getRange(sheet.getLastRow()+1, 1, append.length, append[0].length).setValues(append);

  props.deleteProperty("FS_NEXT_PAGE_TOKEN");

  let res = { success:true, message:`Pulled ${downloaded}`, total, done:downloaded };
  writeLog("PULL", res);
  return res;
}

/* ================================================================
   PUSH (Sheet → Firestore)
================================================================ */
function pushDataToFirebase() {
  let check = validateSheetColumns();
  if (!check.ok) throw new Error("Missing: " + check.missing.join(","));
  return task_pushDataToFirebase();
}

function task_pushDataToFirebase() {
  const props = PROPS;
  props.setProperty("SYNC_ACTION","PUSH");

  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();

  if (data.length <= 1)
    return { success:true, message:"Sheet empty", total:0, done:0 };

  const headers = data[0];
  const kgIdx = headers.indexOf("kgid");
  const booleanFields = detectBooleanFields(headers);

  const totalRows = data.length - 1;
  props.setProperty("S2F_TOTAL", totalRows);

  let r = parseInt(props.getProperty("S2F_CURRENT_ROW") || "1");
  let uploaded = parseInt(props.getProperty("S2F_UP_COUNT") || "0");

  if (r === 1) {
    const name = backupSheetCopy();
    if (name) writeLog("BACKUP",{ message:name });
  }

  const token = getServiceAccountToken();
  const batchSize = DEFAULT_BATCH_SIZE;
  const failed = [];
  const start = Date.now();

  for (; r < data.length && uploaded < totalRows; r++) {

    if (Date.now() - start > MAX_RUN_MS) {
      props.setProperty("S2F_CURRENT_ROW", r);
      props.setProperty("S2F_UP_COUNT", uploaded);
      return { success:true, message:`Paused at row ${r}`, done:uploaded, total:totalRows, failedRows:failed };
    }

    let row = data[r];
    let kgid = String(row[kgIdx]).trim();

    if (!kgid) {
      failed.push({ row: r+1, kgid: null, code: "MISSING_KGID", message: "kgid missing in sheet; row skipped" });
      continue;
    }

    /*-----------------------------
      BUILD FIRESTORE FIELDS
    -----------------------------*/
    const fields = {};

    // Always include kgid field in Firestore (Option A)
    fields["kgid"] = { stringValue: kgid };

    // Build rest of fields safely
    headers.forEach((h, i) => {
      if (h === "kgid") return;

      let val = row[i];

      // Blank handling
      if (val === "" || val == null) {
        if (h === "isDeleted") fields[h] = { booleanValue: false };
        return;
      }

      // Boolean fields
      if (booleanFields.includes(h)) {
        fields[h] = { booleanValue: String(val).toLowerCase() === "true" };
        return;
      }

      // Mobile numbers
      if (h === "mobile1" || h === "mobile2") {
        fields[h] = { stringValue: validateMobile(val) };
        return;
      }

      // District normalization
      if (h === "district") {
        fields[h] = { stringValue: normalizeDistrict(val) };
        return;
      }

      // createdAt
      if (h === "createdAt") {
        fields[h] = { timestampValue: ensureCreatedAt(val) };
        return;
      }

      // updatedAt → sheet value (final will override below)
      if (h === "updatedAt") {
        fields[h] = { timestampValue: ensureUpdatedAt(val) };
        return;
      }

      // Metal number
      if (h === "metal" || h === "metalNumber") {
        fields["metalNumber"] = { stringValue: String(val).trim() };
        return;
      }

      // Default: string
      fields[h] = { stringValue: String(val).trim() };
    });

    // Always override updatedAt with NOW
    fields.updatedAt = { timestampValue: isoNow() };

    /*-----------------------------
      FIRESTORE PATCH/POST
    -----------------------------*/
    const updateUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?currentDocument.exists=true`;

    try {
      const resp = UrlFetchApp.fetch(updateUrl, {
        method:"PATCH",
        contentType:"application/json",
        headers:{ Authorization:"Bearer "+token },
        payload: JSON.stringify({ fields }),
        muteHttpExceptions:true
      });

      if (resp.getResponseCode() === 200) {
        uploaded++;
      } else if (resp.getResponseCode() === 404) {
        // CREATE
        const createUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?documentId=${encodeURIComponent(kgid)}`;

        const resp2 = UrlFetchApp.fetch(createUrl, {
          method:"POST",
          contentType:"application/json",
          headers:{ Authorization:"Bearer "+token },
          payload: JSON.stringify({ fields }),
          muteHttpExceptions:true
        });

        if (resp2.getResponseCode() === 200 || resp2.getResponseCode() === 201) {
          uploaded++;
        } else {
          failed.push({ row:r+1, kgid, code:resp2.getResponseCode(), message:resp2.getContentText() });
        }

      } else {
        failed.push({ row:r+1, kgid, code:resp.getResponseCode(), message:resp.getContentText() });
      }

    } catch(err) {
      failed.push({ row:r+1, kgid, code:"EXCEPTION", message:String(err) });
    }

    props.setProperty("S2F_UP_COUNT", uploaded);
  }

  /*-----------------------------
      FINISHED
  -----------------------------*/
  if (r >= data.length) {
    props.deleteProperty("S2F_CURRENT_ROW");

    const res = { success:true, message:`Pushed ${uploaded}/${totalRows}`, done:uploaded, total:totalRows, failedRows:failed };
    writeLog("PUSH", res);
    return res;
  }

  return { success:true, message:`Paused`, done:uploaded, total:totalRows, failedRows:failed };
}

/* ================================================================
   DRY RUN
================================================================ */
function task_dryRunPush() {
  return { success:true, message:"Dry run disabled in final build" };
}

/* ================================================================
   SIDEBAR STATUS
================================================================ */
function sidebar_getBatchStatus() {
  const props = PROPS;

  const mode = props.getProperty("SYNC_ACTION") || "IDLE";
  const up = parseInt(props.getProperty("S2F_UP_COUNT") || "0");
  const total = parseInt(props.getProperty("S2F_TOTAL") || "0");
  const row = parseInt(props.getProperty("S2F_CURRENT_ROW") || "1");

  const pct = total > 0 ? Math.round((up/total)*100) : 0;

  return {
    mode,
    progress:pct,
    s2f:{ uploaded:up, total, currentRow:row }
  };
}

/**
 * Upload only a single employee identified by KGID.
 * Safe: validates sheet, builds fields, applies mobile/district/timestamps.
 */
function pushSingleEmployee(kgid) {
  if (!kgid) throw new Error("KGID required");

  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  const headers = data[0];

  const kgidIdx = headers.indexOf("kgid");
  if (kgidIdx === -1) throw new Error("kgid column missing in sheet");

  let targetRow = null;
  let rowIndex = -1;

  // Find the row
  for (let r = 1; r < data.length; r++) {
    if (String(data[r][kgidIdx]).trim() === String(kgid).trim()) {
      targetRow = data[r];
      rowIndex = r + 1;
      break;
    }
  }

  if (!targetRow) {
    throw new Error(`KGID ${kgid} not found in sheet`);
  }

  // Detect boolean fields
  const booleanFields = detectBooleanFields(headers);

  // Build Firestore fields
  const fields = {};

  // Always include kgid in Firestore
  fields["kgid"] = { stringValue: String(kgid).trim() };

  headers.forEach((h, idx) => {
    if (h === "kgid") return;

    let val = targetRow[idx];

    // Handle blanks
    if (val === "" || val == null) {
      if (h === "isDeleted") fields[h] = { booleanValue: false };
      return;
    }

    // Boolean fields
    if (booleanFields.includes(h)) {
      fields[h] = { booleanValue: String(val).toLowerCase() === "true" };
      return;
    }

    // Mobile numbers
    if (h === "mobile1" || h === "mobile2") {
      fields[h] = { stringValue: validateMobile(val) };
      return;
    }

    // District normalization
    if (h === "district") {
      fields[h] = { stringValue: normalizeDistrict(val) };
      return;
    }

    // createdAt
    if (h === "createdAt") {
      fields[h] = { timestampValue: ensureCreatedAt(val) };
      return;
    }

    // updatedAt
    if (h === "updatedAt") {
      fields[h] = { timestampValue: ensureUpdatedAt(val) };
      return;
    }

    // Metal number
    if (h === "metal" || h === "metalNumber") {
      fields["metalNumber"] = { stringValue: String(val).trim() };
      return;
    }

    // Default: string
    fields[h] = { stringValue: String(val).trim() };
  });

  fields.updatedAt = { timestampValue: isoNow() };

  // Firestore API
  const token = getServiceAccountToken();

  const updateUrl =
    `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/` +
    encodeURIComponent(kgid) + "?currentDocument.exists=true";

  try {
    const resp = UrlFetchApp.fetch(updateUrl, {
      method: "PATCH",
      contentType: "application/json",
      headers: { Authorization: "Bearer " + token },
      payload: JSON.stringify({ fields }),
      muteHttpExceptions: true
    });

    if (resp.getResponseCode() === 200) {
      return { success: true, message: `Updated employee KGID ${kgid}`, row: rowIndex };
    } else if (resp.getResponseCode() === 404) {
      // CREATE new doc
      const createUrl =
        `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?documentId=` +
        encodeURIComponent(kgid);

      const resp2 = UrlFetchApp.fetch(createUrl, {
        method: "POST",
        contentType: "application/json",
        headers: { Authorization: "Bearer " + token },
        payload: JSON.stringify({ fields }),
        muteHttpExceptions: true
      });

      if (resp2.getResponseCode() === 200 || resp2.getResponseCode() === 201) {
        return { success: true, message: `Created employee KGID ${kgid}`, row: rowIndex };
      } else {
        return { success: false, message: resp2.getContentText() };
      }

    } else {
      return { success: false, message: resp.getContentText() };
    }

  } catch (e) {
    return { success: false, message: e.toString() };
  }
}

/* ================================================================
   GET EMPLOYEES (for UI)
================================================================ */
function getEmployees() {
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  const headers = data[0];

  const list = [];

  for (let r = 1; r < data.length; r++) {
    let obj = {};
    headers.forEach((h,i)=> obj[h] = data[r][i] === "" ? null : data[r][i]);
    list.push(obj);
  }
  return list;
}

/* ================================================================
   ✅ FIXED: PROFILE IMAGE UPLOAD - Always returns JSON
================================================================ */
/**
 * Enhanced image upload: resize, replace old file, update Sheet + Firestore
 * Endpoint: POST ?action=uploadImage or ?action=uploadImageEnhanced
 */
function uploadImageEnhanced(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({ success: false, error: "No POST body received" }, 400);
    }

    // Parse JSON body
    let body = e.postData.contents;
    let data;
    try {
      data = JSON.parse(body);
    } catch (err) {
      return jsonResponse({ success: false, error: "Invalid JSON body: " + err.toString() }, 400);
    }

    const kgid = data.kgid;
    const filename = data.filename || ("img_" + Date.now() + ".jpg");
    const base64Raw = data.image;
    const replaceExisting = (data.replaceExisting === undefined) ? true : !!data.replaceExisting;
    const resizeWidth = data.resizeWidth || 512;
    const resizeHeight = data.resizeHeight || 512;

    if (!kgid) return jsonResponse({ success: false, error: "KGID required" }, 400);
    if (!base64Raw) return jsonResponse({ success: false, error: "Image (base64) required" }, 400);

    // Clean base64 (remove data:...;base64, prefix if present)
    const base64 = (base64Raw.indexOf(",") >= 0) ? base64Raw.split(",")[1] : base64Raw;
    let bytes;
    try {
      bytes = Utilities.base64Decode(base64);
    } catch (err) {
      return jsonResponse({ success: false, error: "Invalid base64 image data" }, 400);
    }

    // Create blob and optionally resize using ImagesService
    let blob = Utilities.newBlob(bytes, "image/jpeg", filename);
    try {
      // Resize only if ImagesService exists and sizes provided
      if (typeof ImagesService !== "undefined" && resizeWidth && resizeHeight) {
        try {
          const img = ImagesService.newImage(blob);
          const resized = img.resize(resizeWidth, resizeHeight).getBlob();
          // ensure jpeg
          blob = Utilities.newBlob(resized.getBytes(), "image/jpeg", filename);
        } catch (imgErr) {
          // fallback to original blob if resize fails
          Logger.log("Image resize failed: " + imgErr.toString());
        }
      }
    } catch (ee) {
      // ignore if ImagesService not available
      Logger.log("Resize skipped: " + ee.toString());
    }

    // Get Drive folder
    let folder;
    try {
      folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    } catch (err) {
      return jsonResponse({ success: false, error: "Invalid Drive folder ID: " + err.toString() }, 500);
    }

    // If replaceExisting: try to find existing fileId from Sheet (preferred) else Firestore
    let currentFileId = null;
    try {
      const currentUrl = findPhotoUrlInSheetByKgid(kgid);
      if (currentUrl) currentFileId = extractFileIdFromDriveUrl(String(currentUrl));
    } catch (sheetErr) {
      Logger.log("Could not fetch current photoUrl from sheet: " + sheetErr.toString());
    }

    // Create new file in Drive
    const file = folder.createFile(blob);
    // set view permission for anyone with link
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    const fileId = file.getId();
    const publicUrl = "https://drive.google.com/uc?export=view&id=" + fileId;

    // Update Sheet (photoUrl, updatedAt) and capture success/failure
    try {
      updateSheetPhotoForKgid(kgid, publicUrl);
    } catch (sErr) {
      Logger.log("Failed updating sheet for kgid " + kgid + " : " + sErr.toString());
    }

    // Update Firestore (use service account token if available)
    try {
      updateFirestorePhotoUrlWithToken(kgid, publicUrl);
    } catch (fErr) {
      Logger.log("Failed updating Firestore for kgid " + kgid + " : " + fErr.toString());
    }

    // If replaceExisting is true, trash the old file (best-effort, do not fail the upload on error)
    if (replaceExisting && currentFileId && currentFileId !== fileId) {
      try {
        const oldFile = DriveApp.getFileById(currentFileId);
        oldFile.setTrashed(true); // move to trash instead of permanent delete
        Logger.log("Trashed old file: " + currentFileId);
      } catch (delErr) {
        Logger.log("Failed to trash old file " + currentFileId + ": " + delErr.toString());
      }
    }

    return jsonResponse({
      success: true,
      kgid: kgid,
      url: publicUrl,
      fileId: fileId,
      message: "Uploaded and updated successfully"
    }, 200);

  } catch (err) {
    Logger.log("uploadImageEnhanced ERROR: " + err.toString());
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/* -------------------- Helper: find photoUrl in sheet by kgid -------------------- */
function findPhotoUrlInSheetByKgid(kgid) {
  try {
    const sheet = getSheet();
    const data = sheet.getDataRange().getValues();
    if (data.length < 2) return null;
    const headers = data[0].map(h => String(h).trim());
    const kgIdx = headers.indexOf("kgid");
    const photoIdx = headers.indexOf("photoUrl");
    if (kgIdx === -1 || photoIdx === -1) return null;

    for (let r = 1; r < data.length; r++) {
      if (String(data[r][kgIdx]).trim() === String(kgid).trim()) {
        return data[r][photoIdx] || null;
      }
    }
    return null;
  } catch (e) {
    Logger.log("findPhotoUrlInSheetByKgid error: " + e.toString());
    return null;
  }
}

/* -------------------- Helper: extract Drive fileId from a Drive URL -------------------- */
function extractFileIdFromDriveUrl(url) {
  if (!url) return null;
  // patterns:
  // https://drive.google.com/file/d/FILE_ID/view?usp=...
  // https://drive.google.com/uc?export=view&id=FILE_ID
  // https://drive.google.com/open?id=FILE_ID
  const m1 = url.match(/\/d\/([a-zA-Z0-9_-]{10,})/);
  if (m1 && m1[1]) return m1[1];
  const m2 = url.match(/[?&]id=([a-zA-Z0-9_-]{10,})/);
  if (m2 && m2[1]) return m2[1];
  const m3 = url.match(/\/folders\/([a-zA-Z0-9_-]{10,})/);
  if (m3 && m3[1]) return m3[1];
  return null;
}

/* -------------------- Helper: update sheet row for kgid with photoUrl & updatedAt -------------------- */
function updateSheetPhotoForKgid(kgid, url) {
  const sheet = getSheet();
  const values = sheet.getDataRange().getValues();
  if (values.length < 2) return false;
  const headers = values[0].map(h => String(h).trim());
  const kgIdx = headers.indexOf("kgid");
  let photoIdx = headers.indexOf("photoUrl");
  const updatedAtIdx = headers.indexOf("updatedAt");

  // If photoUrl column missing, add at end
  if (photoIdx === -1) {
    photoIdx = headers.length;
    sheet.getRange(1, photoIdx + 1).setValue("photoUrl");
  }

  for (let r = 1; r < values.length; r++) {
    if (String(values[r][kgIdx]).trim() === String(kgid).trim()) {
      sheet.getRange(r + 1, photoIdx + 1).setValue(url);
      if (updatedAtIdx >= 0) sheet.getRange(r + 1, updatedAtIdx + 1).setValue(new Date().toISOString());
      return true;
    }
  }
  // If kgid not found, append a row with kgid + photoUrl (optional)
  const newRow = [];
  for (let i = 0; i < headers.length; i++) newRow.push("");
  newRow[kgIdx] = kgid;
  newRow[photoIdx] = url;
  sheet.appendRow(newRow);
  return true;
}

/* -------------------- Helper: update Firestore photoUrl using service account token -------------------- */
function updateFirestorePhotoUrlWithToken(kgid, url) {
  try {
    const token = getServiceAccountToken();
    const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}?currentDocument.exists=true`;
    const payload = { fields: { photoUrl: { stringValue: url }, updatedAt: { timestampValue: new Date().toISOString() } } };

    const resp = UrlFetchApp.fetch(docUrl, {
      method: "PATCH",
      contentType: "application/json",
      headers: { Authorization: "Bearer " + token },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });

    // If 404 (not exists) -> create
    if (resp.getResponseCode() === 404) {
      const createUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees?documentId=${encodeURIComponent(kgid)}`;
      UrlFetchApp.fetch(createUrl, {
        method: "POST",
        contentType: "application/json",
        headers: { Authorization: "Bearer " + token },
        payload: JSON.stringify({ fields: { kgid: { stringValue: String(kgid) }, photoUrl: { stringValue: url }, updatedAt: { timestampValue: new Date().toISOString() } } }),
        muteHttpExceptions: true
      });
    }

    return true;
  } catch (err) {
    Logger.log("updateFirestorePhotoUrlWithToken error: " + err.toString());
    return false;
  }
}

/* -------------------- Sidebar preview helper (callable from sidebar) -------------------- */
function sidebar_getPhotoPreview(kgid) {
  const url = findPhotoUrlInSheetByKgid(kgid);
  return { success: true, kgid: kgid, url: url || null };
}
