/**
 * === DOCUMENTS SCRIPT - COMMON UTILITIES ===
 * Shared constants and utility functions for Documents API
 */

// =======================================================
// Constants
// =======================================================
const SPREADSHEET_ID = "1QKR1gHCTM53MhANbRjid7VBCkuJjyLUV_1nY60sCVXE";
const SHEET_NAME = "Documents Url";   // ⚠️ must match your tab name exactly
const FOLDER_ID = "13qNrVmJQeFgcC_Q90yyD3fhTZq8j0GXK";  // Drive folder for storing files

// ⚠️ FILL THESE WITH YOUR REAL VALUES FROM GOOGLE CLOUD CONSOLE (for Drive Picker)
const PICKER_DEVELOPER_KEY = "AIzaSyB8J0s4_GMOBWsCRXhk1TlgQ8kbGWVrMRw";
const PICKER_APP_ID        = "603972083927";  // numeric project ID

const ALLOWED_ADMINS = [
  "ravipolice@gmail.com",
  "noreply.policemobiledirectory@gmail.com"
];

// =======================================================
// Utility Functions
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

// OAuth token for Drive Picker
function getOAuthToken() {
  return ScriptApp.getOAuthToken();
}

// =======================================================
// File Type Detection
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

// =======================================================
// Logging System
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
// History System
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
























