/**
 * === GALLERY SCRIPT - COMMON UTILITIES ===
 * Shared constants, utilities, and helper functions
 * Used by both API (Android) and Sidebar (Google Sheets UI)
 */

// =======================================================
// CONSTANTS
// =======================================================
const SPREADSHEET_ID = "1m8z-ryBbFTUsox-sEoZgFm_lOvbMb1_XjsaUky1VUjY";
const SHEET_NAME = "Gallery"; // ⚠️ must match tab name in gallery sheet
const FOLDER_ID = "1SP9Zuy7RWUvDMhhEbHSbb3Ll0uc01VA-"; // Gallery Drive folder

// Reuse same Picker keys + admins as Documents
const PICKER_DEVELOPER_KEY = "AIzaSyB8J0s4_GMOBWsCRXhk1TlgQ8kbGWVrMRw";
const PICKER_APP_ID = "603972083927";

const ALLOWED_ADMINS = [
  "ravipolice@gmail.com",
  "noreply.policemobiledirectory@gmail.com"
];

// =======================================================
// UTILITY FUNCTIONS
// =======================================================
function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

// Alias for compatibility
function json(obj) {
  return jsonResponse(obj);
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

function getOAuthToken() {
  return ScriptApp.getOAuthToken();
}

// =======================================================
// FILE TYPE DETECTION
// =======================================================
function detectFileTypeFromUrl(url) {
  if (!url) return "UNKNOWN";
  const lower = url.toLowerCase().split("?")[0];
  
  if (lower.match(/\.(jpg|jpeg|png|gif|bmp|webp)$/)) return "IMAGE";
  if (lower.endsWith(".pdf")) return "PDF";
  return "OTHER";
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
  let hist = ss.getSheetByName("GalleryHistory");
  
  if (!hist) {
    hist = ss.insertSheet("GalleryHistory");
    hist.appendRow([
      "Timestamp",
      "Action",
      "Old Title",
      "New Title",
      "Old URL",
      "New URL",
      "User"
    ]);
  }
  
  hist.appendRow([new Date(), action, oldTitle, newTitle, oldUrl, newUrl, user]);
}

