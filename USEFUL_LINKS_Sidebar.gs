/**
 * === USEFUL LINKS SCRIPT - SIDEBAR UI ===
 * Functions for Google Sheets sidebar UI and menu
 * Allows manual management of useful links from the sheet
 */

// =======================================================
// Menu & Sidebar
// =======================================================
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu("📱 Useful Links")
    .addItem("Open Control Panel", "openSidebar")
    .addToUi();
}

function openSidebar() {
  const html = HtmlService.createHtmlOutput(`
    <div style="font-family: Arial, sans-serif; padding: 20px;">
      
      <h2 style="color:#1a73e8; margin-bottom:20px;">
        📱 Useful Links Control
      </h2>
      <!-- SYNC BUTTON -->
      <button onclick="google.script.run.uploadUsefulLinks();" 
        style="
          background:#1a73e8;
          color:white;
          border:none;
          padding:15px 25px;
          font-size:18px;
          border-radius:8px;
          cursor:pointer;
          width:100%;
          margin-bottom:15px;
        ">
        🔄 SYNC NOW
      </button>
      <!-- MARK PENDING BUTTON -->
      <button onclick="google.script.run.markPendingUploads();" 
        style="
          background:#fbbc04;
          color:black;
          border:none;
          padding:15px 25px;
          font-size:18px;
          border-radius:8px;
          cursor:pointer;
          width:100%;
          margin-bottom:15px;
        ">
        🟡 MARK PENDING
      </button>
      <!-- DELETE BUTTON -->
      <button onclick="google.script.run.deleteSelectedRows();" 
        style="
          background:#d93025;
          color:white;
          border:none;
          padding:15px 25px;
          font-size:18px;
          border-radius:8px;
          cursor:pointer;
          width:100%;
          margin-bottom:15px;
        ">
        ❌ DELETE SELECTED
      </button>
      <!-- HELP -->
      <button onclick="google.script.run.showHelpDialog();" 
        style="
          background:#34a853;
          color:white;
          border:none;
          padding:15px 25px;
          font-size:18px;
          border-radius:8px;
          cursor:pointer;
          width:100%;
        ">
        ℹ️ HELP
      </button>
    </div>
  `).setTitle("Useful Links Control");
  SpreadsheetApp.getUi().showSidebar(html);
}

// =======================================================
// Edit Handler
// =======================================================
function onEdit(e) {
  const range = e.range;
  const sheet = e.source.getActiveSheet();
  const row = range.getRow();
  const col = range.getColumn();
  
  if (row <= 1) return; // Ignore header
  
  // Column B = PlayStoreURL → Mark as Pending
  if (col === 2) {
    sheet.getRange(row, 6).setValue("Pending");
  }
  
  // Column G = Delete checkbox
  if (col === 7) {
    sheet.getRange(row, 6).setValue("Pending Delete");
  }
}

// =======================================================
// Sidebar Functions
// =======================================================
function markPendingUploads() {
  const sheet = getUsefulLinksSheet();
  const data = sheet.getDataRange().getValues();
  
  for (let i = 1; i < data.length; i++) {
    if (!data[i][5]) { // Status col F empty
      sheet.getRange(i + 1, 6).setValue("Pending");
    }
  }
}

function uploadUsefulLinks() {
  const sheet = getUsefulLinksSheet();
  const dataRange = sheet.getDataRange();
  const data = dataRange.getValues();
  
  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    const appName    = row[0];  // Col A
    const playUrl    = row[1];  // Col B
    const apkFileId  = row[2];  // Col C
    const category   = row[3];  // Col D
    const storageUrl = row[4];  // Col E
    const status     = row[5];  // Col F
    const deleteFlag = row[6];  // Col G
    
    if (!appName || !playUrl) continue;
    
    /***************************
     * Handle Deletion
     ***************************/
    if (deleteFlag === true || deleteFlag === "TRUE" || status === "Pending Delete") {
      const deletedCount = deleteFirestoreEntry(appName, playUrl);
      sheet.getRange(i + 1, 6).setValue(deletedCount > 0 ? "Deleted" : "Not Found");
      // Clear delete flag so next run doesn't re-delete
      sheet.getRange(i + 1, 7).clearContent();
      continue;
    }
    
    /***************************
     * Upload APK if exists
     ***************************/
    let newStorageUrl = storageUrl;
    if (apkFileId && (!storageUrl || storageUrl === "")) {
      newStorageUrl = uploadApkToFirebase(apkFileId, appName);
      sheet.getRange(i + 1, 5).setValue(newStorageUrl);
    }
    
    /***************************
     * Upload to Firestore (create or update)
     ***************************/
    const ok = uploadToFirestore(appName, playUrl, newStorageUrl, category);
    sheet.getRange(i + 1, 6).setValue(ok ? "Updated" : "Error");
  }
}

function deleteSelectedRows() {
  const sheet = getUsefulLinksSheet();
  const data = sheet.getDataRange().getValues();
  
  for (let i = 1; i < data.length; i++) {
    const deleteFlag = data[i][6]; // Column G
    if (deleteFlag === true || deleteFlag === "TRUE" || deleteFlag === "Yes") {
      sheet.getRange(i + 1, 6).setValue("Pending Delete"); // Column F
    }
  }
  
  SpreadsheetApp.getUi().alert("Rows marked for deletion. Click 'Sync Useful Links' to complete deletion.");
}

function showHelpDialog() {
  SpreadsheetApp.getUi().alert(
    "Useful Links Manager\n\n" +
    "• Sync Useful Links: Uploads data to Firebase\n" +
    "• Mark Pending Rows: Marks rows needing sync\n" +
    "• Delete Selected: Marks delete rows\n" +
    "• Column E = APK Firebase URL\n\n" +
    "Paste Play Store URL in Column B → Sync."
  );
}

// =======================================================
// Combined Upload Entry (Sidebar -> Apps Script)
// =======================================================

function processCombinedUpload(data) {
  try {
    const sheet = getUsefulLinksSheet();
    const row = Number(data.rowIndex);
    if (!row || row < 2) {
      throw new Error("Invalid row index");
    }

    const appName = (data.appName || "").trim();
    const category = (data.category || "").trim();
    const playUrl = (data.playStoreUrl || "").trim();
    const base64Apk = data.base64Apk;
    const base64Logo = data.base64Logo;

    let iconUrl = data.iconUrl || "";
    let iconFileId = data.iconFileId || "";
    let apkUrl = data.apkUrl || "";
    let apkFileId = data.apkFileId || "";

    if (playUrl) {
      const fetchedIcon = fetchPlayStoreIcon(playUrl);
      if (fetchedIcon) {
        const iconObj = uploadImageToDriveFromUrl(fetchedIcon, `${appName || "app"}_icon.png`);
        iconUrl = iconObj.url || iconUrl;
        iconFileId = iconObj.id || iconFileId;
        if (iconFileId) sheet.getRange(row, 6).setValue(iconFileId);
        if (iconUrl) sheet.getRange(row, 7).setValue(iconUrl);
      }
    }

    if (base64Apk) {
      const apkObj = uploadBinaryToDrive(
        base64Apk,
        `${appName || "app"}.apk`,
        "application/vnd.android.package-archive"
      );
      apkUrl = apkObj.url || apkUrl;
      apkFileId = apkObj.id || apkFileId;
      if (apkUrl) sheet.getRange(row, 4).setValue(apkUrl);
      if (apkFileId) sheet.getRange(row, 5).setValue(apkFileId);
    }

    if (base64Logo) {
      const logoObj = uploadBinaryToDrive(
        base64Logo,
        `${appName || "app"}_logo.png`,
        "image/png"
      );
      iconUrl = logoObj.url || iconUrl;
      iconFileId = logoObj.id || iconFileId;
      if (iconFileId) sheet.getRange(row, 6).setValue(iconFileId);
      if (iconUrl) sheet.getRange(row, 7).setValue(iconUrl);
    }

    const success = uploadToFirestoreCombined({
      appName,
      category,
      playUrl,
      apkUrl,
      iconUrl,
    });

    sheet.getRange(row, 8).setValue(success ? "Uploaded" : "Error");
    return success ? "Success" : "Failed";
  } catch (e) {
    Logger.log("processCombinedUpload error: " + e);
    return "Failed";
  }
}

