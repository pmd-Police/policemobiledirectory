/**
 * === GALLERY SCRIPT ===
 * Police Mobile Directory - Gallery & Documents API
 * 
 * Supports: GET (fetch), POST (upload/edit/delete)
 * Admin-only operations (upload/edit/delete)
 * Includes:
 *  - Gallery Image Management
 *  - Document Management
 *  - Logs
 *  - Version History
 *  - Category Dropdown
 *  - Date Formatting
 *  - Link upload sidebar with validation & file-type detection
 *  - Drive Picker to upload/pick files and auto-fill link
 *
 * Sheet columns (Documents):
 *  A: Title
 *  B: URL
 *  C: Category
 *  D: Uploaded By
 *  E: Uploaded Date
 *  F: Description
 *  G: Delete
 *
 * Sheet columns (Gallery):
 *  A: Title
 *  B: URL (Direct image URL)
 *  C: Category
 *  D: Uploaded By
 *  E: Uploaded Date
 *  F: Description
 *  G: Delete
 */

const SPREADSHEET_ID = "1m8z-ryBbFTUsox-sEoZgFm_lOvbMb1_XjsaUky1VUjY";
const SHEET_NAME = "Documents Url";   // ⚠️ must match your tab name exactly
const GALLERY_SHEET_NAME = "Gallery";   // ⚠️ Gallery sheet name - must match tab name in gallery sheet
const FOLDER_ID = "13qNrVmJQeFgcC_Q90yyD3fhTZq8j0GXK";  // Drive folder for storing documents
const GALLERY_FOLDER_ID = "1SP9Zuy7RWUvDMhhEbHSbb3Ll0uc01VA-";  // Gallery Drive folder

// ⚠️ FILL THESE WITH YOUR REAL VALUES FROM GOOGLE CLOUD CONSOLE (for Drive Picker)
const PICKER_DEVELOPER_KEY = "AIzaSyB8J0s4_GMOBWsCRXhk1TlgQ8kbGWVrMRw";
const PICKER_APP_ID        = "603972083927";  // numeric project ID

const ALLOWED_ADMINS = [
  "ravipolice@gmail.com",
  "noreply.policemobiledirectory@gmail.com"
];

// =======================================================
// Utility
// =======================================================
function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function getSheet() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ss.getSheetByName(SHEET_NAME);
  if (!sheet) throw new Error("Sheet not found: " + SHEET_NAME);
  return sheet;
}

function getFolder() {
  return DriveApp.getFolderById(FOLDER_ID);
}

function isAdmin(user) {
  return user && ALLOWED_ADMINS.includes(user);
}

// OAuth token for Drive Picker
function getOAuthToken() {
  return ScriptApp.getOAuthToken();
}

// =======================================================
// Menu & Sidebar
// =======================================================
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu("Documents")
    .addItem("Upload Link", "openUploadLinkSidebar")
    .addToUi();
}

function openUploadLinkSidebar() {
  const html = HtmlService.createHtmlOutputFromFile("UploadLinkSidebar")
    .setTitle("Upload Document Link")
    .setWidth(400);
  SpreadsheetApp.getUi().showSidebar(html);
}

// =======================================================
// GET Handler (for external API use)
// =======================================================
function doGet(e) {
  try {
    const action = e?.parameter?.action || "";
    if (action === "getDocuments") return getDocuments();
    if (action === "getGallery") return getGallery();
    return jsonResponse([]);
  } catch (err) {
    // App expects ARRAY always → never send object
    return jsonResponse([]);
  }
}


// =======================================================
// POST Handler (for external API use)
// =======================================================
function doPost(e) {
  try {
    if (!e.postData?.contents) throw new Error("Missing POST body");

    const data = JSON.parse(e.postData.contents);
    
    // ✅ FIX: Get user email from request body (Android sends it)
    // Fallback to Session.getActiveUser() for Google Sheet sidebar usage
    let user = data.userEmail || "";
    if (!user) {
      try {
        user = Session.getActiveUser().getEmail();
      } catch (sessionErr) {
        // Session.getActiveUser() fails for HTTP requests
        Logger.log("Session.getActiveUser() failed (expected for HTTP requests): " + sessionErr);
      }
    }
    
    if (!user) throw new Error("Missing user email");
    if (!isAdmin(user)) throw new Error("Unauthorized user: " + user);
    
    // ✅ FIX: Check query parameter first (Android sends action in query)
    // Then fallback to body action
    let action = (e?.parameter?.action || data.action || "").toLowerCase();
    
    // ✅ Map Android action names to script action names
    if (action === "uploaddocument") action = "upload";
    if (action === "editdocument") action = "edit";
    if (action === "deletedocument") action = "delete";
    if (action === "uploadgallery") action = "uploadGallery";
    if (action === "deletegallery") action = "deleteGallery";

    switch (action) {
      case "upload": return uploadDocument(data, user);
      case "edit": return editDocument(data, user);
      case "delete": return deleteDocument(data, user);
      case "uploadGallery": return uploadGallery(data, user);
      case "deleteGallery": return deleteGallery(data, user);
      default: throw new Error("Invalid POST action: " + action);
    }
  } catch (err) {
    Logger.log("doPost ERROR: " + err.message);
    return jsonResponse({ success: false, error: err.message });
  }
}

// =======================================================
// GET ALL DOCUMENTS (for external API)
// =======================================================
function getDocuments() {
  try {
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    if (rows.length <= 1) return jsonResponse([]);

    const headers = rows.shift();
    const list = [];

    rows.forEach(row => {
      if (row[6] && row[6].toString().toLowerCase() === "deleted") return;
      const item = {};
      headers.forEach((h, i) => item[h] = row[i]);
      list.push(item);
    });

    return jsonResponse(list);
  } catch (err) {
    Logger.log("getDocuments ERROR: " + err.message);
    // ✅ Always return array, even on error
    return jsonResponse([]);
  }
}


// =======================================================
// UPLOAD DOCUMENT (file from app via Base64)
// =======================================================
function uploadDocument(data, user) {
  // ✅ FIX: Accept both fileBase64 (Android) and fileData (legacy)
  let fileBase64Data = data.fileBase64 || data.fileData;
  const { title, mimeType, category, description } = data;
  if (!fileBase64Data) throw new Error("Missing fileData/fileBase64");

  // ✅ Strip data URI prefix if present (e.g., "data:application/pdf;base64,...")
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }

  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(bytes, mimeType || "application/pdf", title || "Untitled");

  const folder = getFolder();
  const file = folder.createFile(blob);

  // ✅ Ensure file is accessible without Google login
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (shareErr) {
    Logger.log("Warning: Failed to set sharing permissions: " + shareErr);
  }

  const date = new Date();
  const sheet = getSheet();

  sheet.appendRow([
    title || "Untitled",
    file.getUrl(),
    category || "",
    user,
    date,
    description || "",
    ""
  ]);

  logAction("UPLOAD_FILE", title, user);
  saveHistory("UPLOAD_FILE", "", title, "", file.getUrl(), user);

  return jsonResponse({ success: true, action: "upload", url: file.getUrl() });
}

// =======================================================
// UPLOAD DOCUMENT FROM GOOGLE SHEET (SIDEBAR)
// SAME LOGIC AS APP uploadDocument()
// =======================================================
function uploadFromSheet(data) {
  const user = Session.getActiveUser().getEmail();
  if (!isAdmin(user)) throw new Error("Unauthorized user: " + user);

  let fileBase64Data = data.fileBase64 || data.fileData;
  if (!fileBase64Data) throw new Error("Missing fileBase64/fileData");

  // Strip prefix "data:application/pdf;base64,..."
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }

  const { title, mimeType, category, description } = data;

  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(
    bytes,
    mimeType || "application/pdf",
    title || "Untitled"
  );

  const folder = getFolder();
  const file = folder.createFile(blob);

  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (err) {
    Logger.log("Sharing error: " + err);
  }

  const sheet = getSheet();
  const date = new Date();

  sheet.appendRow([
    title || "Untitled",
    file.getUrl(),
    category || "",
    user,
    date,
    description || "",
    ""
  ]);

  logAction("UPLOAD_FILE_SHEET", title, user);
  saveHistory("UPLOAD_FILE", "", title, "", file.getUrl(), user);

  return { success: true, url: file.getUrl() };
}

// =======================================================
// EDIT DOCUMENT
// =======================================================
function editDocument(data, user) {
  const { oldTitle, newTitle, newFileData, mimeType, category, description } = data;
  if (!oldTitle) throw new Error("Missing oldTitle");

  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;

  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === oldTitle.trim()) {
      found = true;

      const oldUrl = rows[i][1];
      let newUrl = oldUrl;

      // If new file uploaded → replace it
      if (newFileData) {
        try {
          const oldId = oldUrl.match(/[-\w]{25,}/)?.[0];
          if (oldId) DriveApp.getFileById(oldId).setTrashed(true);
        } catch (_) {}

        const bytes = Utilities.base64Decode(newFileData);
        const blob = Utilities.newBlob(bytes, mimeType || "application/pdf", newTitle || oldTitle);
        const folder = getFolder();
        const newFile = folder.createFile(blob);
        
        // ✅ Ensure new file is accessible without Google login
        try {
          newFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        } catch (shareErr) {
          Logger.log("Warning: Failed to set sharing permissions (edit): " + shareErr);
        }
        
        newUrl = newFile.getUrl();
      }

      sheet.getRange(i + 1, 1, 1, 7).setValues([[
        newTitle || oldTitle,
        newUrl,
        category || rows[i][2],
        user,
        new Date(),
        description || rows[i][5],
        rows[i][6] || ""
      ]]);

      logAction("EDIT", oldTitle, user);
      saveHistory("EDIT", oldTitle, newTitle, oldUrl, newUrl, user);

      break;
    }
  }

  if (!found) throw new Error("Document not found: " + oldTitle);

  return jsonResponse({ success: true, action: "edit" });
}

// =======================================================
// DELETE DOCUMENT (SOFT DELETE)
// =======================================================
function deleteDocument(data, user) {
  const { title } = data;
  if (!title) throw new Error("Missing title");

  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;

  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === title.trim()) {
      found = true;

      const url = rows[i][1];
      try {
        const fileId = url.match(/[-\w]{25,}/)?.[0];
        if (fileId) DriveApp.getFileById(fileId).setTrashed(true);
      } catch (_) {}

      sheet.getRange(i + 1, 7).setValue("Deleted");
      sheet.getRange(i + 1, 4).setValue(user);

      logAction("DELETE", title, user);
      saveHistory("DELETE", title, title, url, "", user);

      break;
    }
  }

  if (!found) throw new Error("Document not found: " + title);

  return jsonResponse({ success: true, action: "delete" });
}

// =======================================================
// LOG SYSTEM
// =======================================================
function logAction(action, title, user) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  let logSheet = ss.getSheetByName("Logs");

  if (!logSheet) {
    logSheet = ss.insertSheet("Logs");
    logSheet.appendRow(["Timestamp", "Action", "Title", "User"]);
  }

  logSheet.appendRow([new Date(), action, title, user]);
}

// =======================================================
// HISTORY SYSTEM
// =======================================================
function saveHistory(action, oldTitle, newTitle, oldUrl, newUrl, user) {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  let hist = ss.getSheetByName("DocumentHistory");

  if (!hist) {
    hist = ss.insertSheet("DocumentHistory");
    hist.appendRow(["Timestamp", "Action", "Old Title", "New Title", "Old URL", "New URL", "User"]);
  }

  hist.appendRow([new Date(), action, oldTitle, newTitle, oldUrl, newUrl, user]);
}

// =======================================================
// LINK UPLOAD (from sidebar)
// =======================================================
function detectFileTypeFromUrl(url) {
  if (!url) return "UNKNOWN";
  const lower = url.toLowerCase().split("?")[0]; // remove query

  if (lower.endsWith(".pdf")) return "PDF";
  if (lower.endsWith(".doc")) return "DOC";
  if (lower.endsWith(".docx")) return "DOCX";
  if (lower.endsWith(".xls")) return "XLS";
  if (lower.endsWith(".xlsx")) return "XLSX";
  if (lower.endsWith(".ppt")) return "PPT";
  if (lower.endsWith(".pptx")) return "PPTX";
  if (lower.match(/\.(jpg|jpeg|png|gif|bmp|webp)$/)) return "IMAGE";
  return "OTHER";
}

// Called by sidebar form
function uploadLink(data) {
  const user = Session.getActiveUser().getEmail();
  if (!isAdmin(user)) throw new Error("Unauthorized");

  const title = (data.title || "").trim();
  const url = (data.url || "").trim();
  const category = (data.category || "").trim();
  const description = (data.description || "").trim();

  if (!title) throw new Error("Title is required");
  if (!url) throw new Error("URL is required");
  if (!/^https?:\/\//i.test(url)) {
    throw new Error("URL must start with http:// or https://");
  }

  const fileType = detectFileTypeFromUrl(url);

  const sheet = getSheet();
  sheet.appendRow([
    title,
    url,
    category || "Misc",
    user,
    new Date(),
    description || ("(Link upload, type: " + fileType + ")"),
    ""
  ]);

  logAction("UPLOAD_LINK_" + fileType, title, user);
  saveHistory("UPLOAD_LINK", "", title, "", url, user);

  return {
    success: true,
    fileType: fileType,
    title: title,
    url: url
  };
}

// =======================================================
// DRIVE PICKER SUPPORT
// =======================================================

// Called from sidebar: after selecting file in Drive Picker
// Ensures file is in our folder and returns info for form auto-fill
function handlePickedFile(fileId) {
  const user = Session.getActiveUser().getEmail();
  if (!isAdmin(user)) throw new Error("Unauthorized");

  const file = DriveApp.getFileById(fileId);
  const folder = getFolder();

  // Add file to our folder (do not remove from other parents to avoid confusion)
  folder.addFile(file);

  const title = file.getName();
  const url = file.getUrl();
  const mimeType = file.getMimeType();

  // Try to detect type from filename first, then fallback to mime
  let fileType = detectFileTypeFromUrl(title);
  if (fileType === "OTHER") {
    if (mimeType === "application/pdf") fileType = "PDF";
    else if (mimeType.indexOf("image/") === 0) fileType = "IMAGE";
  }

  return {
    title: title,
    url: url,
    mimeType: mimeType,
    fileType: fileType
  };
}

// Expose Picker configuration to client
function getPickerConfig() {
  return {
    developerKey: PICKER_DEVELOPER_KEY,
    appId: PICKER_APP_ID,
    folderId: FOLDER_ID
  };
}

// =======================================================
// DATA FOR SIDEBAR TABLE
// =======================================================
function listDocumentsForSidebar() {
  const sheet = getSheet();
  const values = sheet.getDataRange().getValues();

  if (values.length <= 1) return [];

  values.shift(); // remove header
  const docs = [];

  values.forEach(row => {
    if (row[6] && row[6].toString().toLowerCase() === "deleted") return;
    docs.push({
      title: row[0],
      url: row[1],
      category: row[2],
      uploadedBy: row[3],
      uploadedDate: row[4],
      description: row[5]
    });
  });

  // Latest first
  docs.reverse();
  return docs;
}

// =======================================================
// ONE-TIME SETUP
// =======================================================
function setup() {
  setupCategoryDropdown();
  formatUploadedDate();
}

function setupCategoryDropdown() {
  const sheet = getSheet();
  const list = [
    "Circular",
    "Notification",
    "Service Rules",
    "Government Orders",
    "Department Orders",
    "Manuals",
    "Misc"
  ];

  const rule = SpreadsheetApp.newDataValidation()
    .requireValueInList(list, true)
    .setAllowInvalid(false)
    .build();

  sheet.getRange("C2:C").setDataValidation(rule);
}

function formatUploadedDate() {
  const sheet = getSheet();
  sheet.getRange("E2:E").setNumberFormat("dd-MM-yyyy HH:mm:ss");
}

// =======================================================
// GALLERY FUNCTIONS
// =======================================================

function getGallerySheet() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ss.getSheetByName(GALLERY_SHEET_NAME);
  if (!sheet) {
    // Create sheet if it doesn't exist
    const newSheet = ss.insertSheet(GALLERY_SHEET_NAME);
    newSheet.appendRow(["Title", "URL", "Category", "Uploaded By", "Uploaded Date", "Description", "Delete"]);
    return newSheet;
  }
  return sheet;
}

function getGalleryFolder() {
  return DriveApp.getFolderById(GALLERY_FOLDER_ID);
}

// =======================================================
// GET ALL GALLERY IMAGES
// =======================================================
function getGallery() {
  try {
    const sheet = getGallerySheet();
    const rows = sheet.getDataRange().getValues();
    if (rows.length <= 1) return jsonResponse([]);

    const headers = rows.shift();
    const list = [];

    rows.forEach(row => {
      if (row[6] && row[6].toString().toLowerCase() === "deleted") return;
      const item = {};
      headers.forEach((h, i) => item[h] = row[i]);
      list.push(item);
    });

    return jsonResponse(list);
  } catch (err) {
    Logger.log("getGallery ERROR: " + err.message);
    return jsonResponse([]);
  }
}

// =======================================================
// UPLOAD GALLERY IMAGE
// =======================================================
function uploadGallery(data, user) {
  // ✅ Accept both fileBase64 (Android) and fileData (legacy)
  let fileBase64Data = data.fileBase64 || data.fileData;
  const { title, mimeType, category, description } = data;
  if (!fileBase64Data) throw new Error("Missing fileData/fileBase64");

  // ✅ Strip data URI prefix if present
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }

  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(bytes, mimeType || "image/jpeg", title || "Gallery Image");

  const folder = getGalleryFolder();
  const file = folder.createFile(blob);

  // ✅ Ensure file is accessible without Google login
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (shareErr) {
    Logger.log("Warning: Failed to set sharing permissions: " + shareErr);
  }

  // ✅ Convert to direct image URL format for gallery display
  const fileId = file.getId();
  const directImageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;

  const date = new Date();
  const sheet = getGallerySheet();

  sheet.appendRow([
    title || "Gallery Image",
    directImageUrl,  // ✅ Store direct image URL instead of view URL
    category || "",
    user,
    date,
    description || "",
    ""
  ]);

  logAction("UPLOAD_GALLERY", title, user);
  saveHistory("UPLOAD_GALLERY", "", title, "", directImageUrl, user);

  return jsonResponse({ success: true, action: "uploadGallery", url: directImageUrl });
}

// =======================================================
// DELETE GALLERY IMAGE
// =======================================================
function deleteGallery(data, user) {
  const { title } = data;
  if (!title) throw new Error("Missing title");

  const sheet = getGallerySheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;

  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === title.trim()) {
      found = true;
      const url = rows[i][1];
      try {
        const fileId = url.match(/[-\w]{25,}/)?.[0];
        if (fileId) DriveApp.getFileById(fileId).setTrashed(true);
      } catch (_) {}

      sheet.getRange(i + 1, 7).setValue("Deleted");
      sheet.getRange(i + 1, 4).setValue(user);
      logAction("DELETE_GALLERY", title, user);
      saveHistory("DELETE_GALLERY", title, title, url, "", user);
      break;
    }
  }

  if (!found) throw new Error("Gallery image not found: " + title);
  return jsonResponse({ success: true, action: "deleteGallery" });
}
