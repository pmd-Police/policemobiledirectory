/**
 * === DOCUMENTS SCRIPT - SIDEBAR UI ===
 * Functions for Google Sheets sidebar UI
 * Allows manual management of documents from the sheet
 */

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
// LINK UPLOAD (from sidebar)
// =======================================================
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
























