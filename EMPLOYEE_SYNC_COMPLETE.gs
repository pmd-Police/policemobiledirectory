/********************************************************************
 * 🔰 EMPLOYEE SYNC ENGINE — STANDALONE
 * ---------------------------------------------------------------
 * Handles bidirectional sync between Google Sheets and Firestore
 * All employee sync features included
 * Can be deployed independently
 ********************************************************************/

/* ================================================================
   CONFIGURATION
================================================================ */

const PROPS = PropertiesService.getScriptProperties();

const SHEET_ID = PROPS.getProperty("SHEET_ID") || "1E8cE9zzM3jAHL-a_Cafn5EDWEbk_QNBfOpNtpWwVjfA";
const SHEET_NAME = PROPS.getProperty("SHEET_NAME") || "Emp Profiles";

const DEFAULT_BATCH_SIZE = parseInt(PROPS.getProperty("BATCH_SIZE") || "300");
const MAX_RUN_MS = 330000; // 5.5 minutes
const TOKEN_SCOPE = "https://www.googleapis.com/auth/datastore";

// Firebase config (for sync operations)
const FIREBASE_PROJECT_ID = PROPS.getProperty("FIREBASE_PROJECT_ID") || "pmd-police-mobile-directory";

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
    "S2F_CURRENT_ROW",
    "S2F_CURRENT_KGID",
    "S2F_UP_COUNT",
    "S2F_TOTAL",
    "S2F_DONE",
    "FS_DOWN_COUNT",
    "FS_TOTAL",
    "FS_NEXT_PAGE_TOKEN",
    "BOOL_FIX_PAGE",
    "BOOL_FIXED"
  ].forEach(k => props.deleteProperty(k));

  return "✔ Sync memory fully reset.";
}

function superReset() {
  return resetSync();
}

/* ================================================================
   SEARCH
================================================================ */

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
   JSON RESPONSE HELPER
================================================================ */

function jsonResponse(obj, status) {
  try {
    const output = ContentService.createTextOutput(JSON.stringify(obj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  } catch (err) {
    const errorObj = { success: false, error: "Failed to create JSON response: " + err.toString() };
    const output = ContentService.createTextOutput(JSON.stringify(errorObj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

/* ================================================================
   API ROUTING
================================================================ */

function doGet(e) {
  return handleEmployeeApi(e);
}

function doPost(e) {
  return handleEmployeeApi(e);
}

function handleEmployeeApi(e) {
  try {
    if (!e || !e.parameter)
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);

    const action = e.parameter.action;

    switch (action) {
      case "getEmployees":
        return jsonResponse(getEmployees());

      case "pullDataFromFirebase":
        return jsonResponse(task_pullDataFromFirebase());

      case "pushDataToFirebase":
        return jsonResponse(task_pushDataToFirebase());

      case "dryRunPush":
        return jsonResponse(task_dryRunPush());

      case "pushSingleEmployee":
        return jsonResponse(pushSingleEmployee(e.parameter.kgid));

      default:
        return jsonResponse({ error: "Invalid action: " + action }, 400);
    }
  } catch (err) {
    Logger.log("handleEmployeeApi ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
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
    "kgid", "name", "mobile1", "mobile2", "rank", "station", "district",
    "metal", "bloodGroup", "email", "photoUrl", "fcmToken", "firebaseUid",
    "isAdmin", "isApproved", "pin", "createdAt", "updatedAt", "isDeleted"
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

    let url =
      "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
      "/databases/(default)/documents/employees?pageSize=300" +
      (next ? "&pageToken="+next : "");

    const resp = UrlFetchApp.fetch(url, {
      headers:{ Authorization:"Bearer "+token }
    });

    const json = JSON.parse(resp.getContentText());
    const docs = json.documents || [];

    total += docs.length;
    props.setProperty("FS_TOTAL", total);

    docs.forEach(doc => {
      const fs = doc.fields || {};
      const docId = doc.name.split("/").pop();

      // ALWAYS ensure field kgid exists
      let kgid = (fs.kgid && fs.kgid.stringValue) || docId;
      fs.kgid = { stringValue: kgid };

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

      // auto booleans
      booleanFields.forEach(b => {
        const i = headers.indexOf(b);
        if (i >= 0 && (row[i] === "" || row[i] == null)) row[i] = false;
      });

      // auto timestamps
      let ci = headers.indexOf("createdAt");
      if (ci >= 0) row[ci] = ensureCreatedAt(row[ci]);

      let ui = headers.indexOf("updatedAt");
      if (ui >= 0) row[ui] = ensureUpdatedAt(row[ui]);

      if (sheetMap[kgid]) {
        let rn = sheetMap[kgid];
        let existing =
          sheet.getRange(rn,1,1,headers.length).getValues()[0];

        const fsTime =
          fs.updatedAt ? new Date(fs.updatedAt.timestampValue) : null;
        const shTime =
          existing[updIdx] ? new Date(existing[updIdx]) : null;

        if (fsTime && (!shTime || fsTime > shTime)) {
          const merged = headers.map((h,i)=>
            row[i] === "" ? existing[i] : row[i]
          );
          sheet.getRange(rn,1,1,headers.length).setValues([merged]);
        }
      } else {
        append.push(row);
      }

      downloaded++;
    });

    props.setProperty("FS_DOWN_COUNT", downloaded);
    next = json.nextPageToken || "";

  } while (next);

  if (append.length > 0)
    sheet
      .getRange(sheet.getLastRow()+1,1,append.length,append[0].length)
      .setValues(append);

  props.deleteProperty("FS_NEXT_PAGE_TOKEN");

  let res = {
    success:true,
    message:"Pulled " + downloaded,
    total,
    done:downloaded
  };
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

  // backup only on first run
  if (r === 1) {
    const name = backupSheetCopy();
    if (name) writeLog("BACKUP",{ message:name });
  }

  const token = getServiceAccountToken();
  const start = Date.now();
  const failed = [];

  for (; r < data.length; r++) {

    if (Date.now() - start > MAX_RUN_MS) {
      props.setProperty("S2F_CURRENT_ROW", r);
      props.setProperty("S2F_UP_COUNT", uploaded);

      return {
        success:true,
        message:"Paused at row " + r,
        done:uploaded,
        total:totalRows,
        failedRows:failed
      };
    }

    let row = data[r];
    let kgid = String(row[kgIdx]).trim();

    if (!kgid) {
      failed.push({
        row:r+1,
        kgid:null,
        code:"MISSING_KGID",
        message:"Missing kgid in sheet; skipped"
      });
      continue;
    }

    /* --- Build Firestore fields --- */
    const fields = {};

    // ALWAYS include kgid in fields
    fields["kgid"] = { stringValue: kgid };

    headers.forEach((h,i) => {
      if (h === "kgid") return;

      let val = row[i];

      if (val === "" || val == null) {
        if (h === "isDeleted")
          fields[h] = { booleanValue:false };
        return;
      }

      if (booleanFields.includes(h)) {
        fields[h] = {
          booleanValue: String(val).toLowerCase() === "true"
        };
        return;
      }

      if (h === "mobile1" || h === "mobile2") {
        fields[h] = { stringValue: validateMobile(val) };
        return;
      }

      if (h === "district") {
        fields[h] = { stringValue: normalizeDistrict(val) };
        return;
      }

      if (h === "createdAt") {
        fields[h] = { timestampValue: ensureCreatedAt(val) };
        return;
      }

      if (h === "updatedAt") {
        fields[h] = { timestampValue: ensureUpdatedAt(val) };
        return;
      }

      fields[h] = { stringValue:String(val).trim() };
    });

    fields.updatedAt = { timestampValue: isoNow() };

    /* --- Send to Firestore --- */
    const updateUrl =
      "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
      "/databases/(default)/documents/employees/" + encodeURIComponent(kgid) +
      "?currentDocument.exists=true";

    try {
      const resp = UrlFetchApp.fetch(updateUrl, {
        method:"PATCH",
        contentType:"application/json",
        headers:{ Authorization:"Bearer "+token },
        payload:JSON.stringify({ fields }),
        muteHttpExceptions:true
      });

      if (resp.getResponseCode() === 200) {
        uploaded++;
        continue;
      }

      if (resp.getResponseCode() === 404) {
        // CREATE new
        const createUrl =
          "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
          "/databases/(default)/documents/employees?documentId=" + encodeURIComponent(kgid);

        const resp2 = UrlFetchApp.fetch(createUrl, {
          method:"POST",
          contentType:"application/json",
          headers:{ Authorization:"Bearer "+token },
          payload:JSON.stringify({ fields }),
          muteHttpExceptions:true
        });

        if (resp2.getResponseCode() === 200 || resp2.getResponseCode() === 201) {
          uploaded++;
        } else {
          failed.push({
            row:r+1,
            kgid,
            code:resp2.getResponseCode(),
            message:resp2.getContentText()
          });
        }

        continue;
      }

      failed.push({
        row:r+1,
        kgid,
        code:resp.getResponseCode(),
        message:resp.getContentText()
      });

    } catch (err) {
      failed.push({
        row:r+1,
        kgid,
        code:"EXCEPTION",
        message:String(err)
      });
    }

    props.setProperty("S2F_UP_COUNT", uploaded);
  }

  /* --- FINISHED --- */
  props.deleteProperty("S2F_CURRENT_ROW");

  const res = {
    success:true,
    message:"Pushed " + uploaded + "/" + totalRows,
    done:uploaded,
    total:totalRows,
    failedRows:failed
  };

  writeLog("PUSH", res);
  return res;
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

  // Sheet rows (minus header)
  let sheetRows = 0;
  try {
    const sh = getSheet();
    sheetRows = Math.max(0, sh.getLastRow() - 1);
  } catch (_) {}

  // PUSH counters
  const uploaded = parseInt(props.getProperty("S2F_UP_COUNT") || "0");
  const totalPush = parseInt(props.getProperty("S2F_TOTAL") || "0");
  const currentRow = parseInt(props.getProperty("S2F_CURRENT_ROW") || "1");

  // PULL counters
  const fsDownloaded = parseInt(props.getProperty("FS_DOWN_COUNT") || "0");
  const fsTotal = parseInt(props.getProperty("FS_TOTAL") || "0");

  // % calculation
  let pct = 0;
  if (mode === "PULL" && fsTotal > 0) {
    pct = Math.round((fsDownloaded / fsTotal) * 100);
  } else if (mode === "PUSH" && totalPush > 0) {
    pct = Math.round((uploaded / totalPush) * 100);
  }

  return {
    mode,

    // sheet row count
    total: sheetRows,

    // Firestore counters (for PULL)
    fs: {
      total: fsTotal,
      downloaded: fsDownloaded
    },

    // Sheet→Firestore counters (for PUSH)
    s2f: {
      uploaded,
      total: totalPush,
      currentRow
    },

    progress: pct
  };
}

/* ================================================================
   PUSH SINGLE EMPLOYEE
================================================================ */

function pushSingleEmployee(kgid) {
  if (!kgid) return { success:false, error:"KGID required" };

  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  const headers = data[0];

  const kgIdx = headers.indexOf("kgid");
  const booleanFields = detectBooleanFields(headers);

  let targetRow = null;
  let rowIndex = -1;

  for (let r = 1; r < data.length; r++) {
    if (String(data[r][kgIdx]).trim() === String(kgid).trim()) {
      targetRow = data[r];
      rowIndex = r+1;
      break;
    }
  }

  if (!targetRow) {
    return { success:false, error:"KGID " + kgid + " not found" };
  }

  const fields = {};
  fields["kgid"] = { stringValue:String(kgid) };

  headers.forEach((h,i) => {
    if (h === "kgid") return;

    let val = targetRow[i];

    if (val === "" || val == null) {
      if (h === "isDeleted")
        fields[h] = { booleanValue:false };
      return;
    }

    if (booleanFields.includes(h)) {
      fields[h] = { booleanValue:String(val).toLowerCase()==="true" };
      return;
    }

    if (h === "mobile1" || h === "mobile2") {
      fields[h] = { stringValue:validateMobile(val) };
      return;
    }

    if (h === "district") {
      fields[h] = { stringValue:normalizeDistrict(val) };
      return;
    }

    if (h === "createdAt") {
      fields[h] = { timestampValue:ensureCreatedAt(val) };
      return;
    }

    if (h === "updatedAt") {
      fields[h] = { timestampValue:ensureUpdatedAt(val) };
      return;
    }

    fields[h] = { stringValue:String(val).trim() };
  });

  fields.updatedAt = { timestampValue: isoNow() };

  const token = getServiceAccountToken();

  const updateUrl =
    "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
    "/databases/(default)/documents/employees/" + encodeURIComponent(kgid) +
    "?currentDocument.exists=true";

  try {
    const resp = UrlFetchApp.fetch(updateUrl, {
      method:"PATCH",
      contentType:"application/json",
      headers:{ Authorization:"Bearer "+token },
      payload:JSON.stringify({ fields }),
      muteHttpExceptions:true
    });

    if (resp.getResponseCode() === 200) {
      return { success:true, message:"Updated "+kgid };
    }

    if (resp.getResponseCode() === 404) {
      const createUrl =
        "https://firestore.googleapis.com/v1/projects/" + FIREBASE_PROJECT_ID +
        "/databases/(default)/documents/employees?documentId=" + encodeURIComponent(kgid);

      const resp2 = UrlFetchApp.fetch(createUrl, {
        method:"POST",
        contentType:"application/json",
        headers:{ Authorization:"Bearer "+token },
        payload:JSON.stringify({ fields }),
        muteHttpExceptions:true
      });

      if (resp2.getResponseCode() === 200 ||
          resp2.getResponseCode() === 201)
      {
        return { success:true, message:"Created "+kgid };
      } else {
        return { success:false, error:resp2.getContentText() };
      }
    }

    return { success:false, error:resp.getContentText() };

  } catch (err) {
    return { success:false, error:err.toString() };
  }
}

/* ================================================================
   GET EMPLOYEES (used by UI)
================================================================ */

function getEmployees() {
  const sheet = getSheet();
  const data = sheet.getDataRange().getValues();
  const headers = data[0];

  const list = [];

  for (let r = 1; r < data.length; r++) {
    let obj = {};
    headers.forEach((h,i)=>
      obj[h] = data[r][i] === "" ? null : data[r][i]
    );
    list.push(obj);
  }

  return list;
}



