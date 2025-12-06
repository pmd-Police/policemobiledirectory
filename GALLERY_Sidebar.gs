/**
 * === GALLERY SCRIPT - SIDEBAR & SHEET UI ===
 * Handles Google Sheets sidebar, menu, picker, and formatting
 * Completely independent from Android API code
 */

// =======================================================
// Menu & Sidebar
// =======================================================
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu("Gallery")
    .addItem("Open Gallery Manager", "openGallerySidebar")
    .addToUi();
}

function openGallerySidebar() {
  const html = HtmlService.createHtmlOutputFromFile("GallerySidebar")
    .setTitle("Gallery Manager")
    .setWidth(400);
  SpreadsheetApp.getUi().showSidebar(html);
}

// =======================================================
// UPLOAD FROM GOOGLE SHEET SIDEBAR
// =======================================================
function uploadFromSheet(data) {
  const user = Session.getActiveUser().getEmail();
  if (!isAdmin(user)) throw new Error("Unauthorized user: " + user);
  
  let fileBase64Data = data.fileBase64 || data.fileData;
  if (!fileBase64Data) throw new Error("Missing fileBase64/fileData");
  
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }
  
  const { title, mimeType, category, description } = data;
  
  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(
    bytes,
    mimeType || "image/jpeg",
    title || "Untitled"
  );
  
  const folder = getFolder();
  const file = folder.createFile(blob);
  
  // ✅ Convert to direct image URL format
  const fileId = file.getId();
  const directImageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
  
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (err) {
    Logger.log("Sharing error: " + err);
  }
  
  const sheet = getSheet();
  const date = new Date();
  
  sheet.appendRow([
    title || "Untitled",
    directImageUrl,  // ✅ Store direct image URL
    category || "Gallery",
    user,
    date,
    description || "",
    ""
  ]);
  
  logAction("UPLOAD_IMAGE_SHEET", title, user);
  saveHistory("UPLOAD_IMAGE", "", title, "", directImageUrl, user);
  
  return { success: true, url: directImageUrl };
}

// =======================================================
// LINK UPLOAD (from sidebar)
// =======================================================
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
    category || "Gallery",
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
function handlePickedFile(fileId) {
  const user = Session.getActiveUser().getEmail();
  if (!isAdmin(user)) throw new Error("Unauthorized");
  
  const file = DriveApp.getFileById(fileId);
  const folder = getFolder();
  folder.addFile(file);
  
  const title = file.getName();
  const url = file.getUrl();
  const mimeType = file.getMimeType();
  
  let fileType = detectFileTypeFromUrl(title);
  if (fileType === "OTHER") {
    if (mimeType.indexOf("image/") === 0) fileType = "IMAGE";
  }
  
  return {
    title: title,
    url: url,
    mimeType: mimeType,
    fileType: fileType
  };
}

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
function listGalleryForSidebar() {
  const sheet = getSheet();
  const values = sheet.getDataRange().getValues();
  
  if (values.length <= 1) return [];
  
  values.shift();
  const items = [];
  
  values.forEach(row => {
    if (row[6] && row[6].toString().toLowerCase() === "deleted") return;
    items.push({
      title: row[0],
      url: row[1],
      category: row[2],
      uploadedBy: row[3],
      uploadedDate: row[4],
      description: row[5]
    });
  });
  
  items.reverse();
  return items;
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
    "Event",
    "Function",
    "Parade",
    "Office",
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

