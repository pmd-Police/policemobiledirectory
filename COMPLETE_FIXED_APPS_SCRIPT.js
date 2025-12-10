/**
 * ============================================
 * COMPLETE FIXED APPS SCRIPT - EMPLOYEE SYNC
 * ============================================
 * 
 * This version handles BOTH:
 * 1. API requests (with ?action= parameter) → Returns JSON
 * 2. UI requests (no parameters) → Returns HTML Sidebar
 */

// --- WEB APP ENTRY POINT (FIXED VERSION) ---
function doGet(e) {
  try {
    // If action parameter exists, treat as API request
    if (e && e.parameter && e.parameter.action) {
      return handleEmployeeApi(e);
    }
    
    // Otherwise, return the HTML sidebar UI
    return HtmlService.createTemplateFromFile("Sidebar")
      .evaluate()
      .setTitle("Employee Management")
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
      
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

// =======================================================
// EMPLOYEE API HANDLER
// =======================================================
function handleEmployeeApi(e) {
  try {
    const action = e.parameter.action || "";
    
    switch (action) {
      case "getEmployees":
        return getEmployees();
        
      case "syncFirebaseToSheet":
      case "pullDataFromFirebase":
        const pullResult = pullDataFromFirebase();
        return jsonResponse(pullResult);
        
      case "syncSheetToFirebase":
      case "pushDataToFirebase":
        const pushResult = pushDataToFirebase();
        return jsonResponse(pushResult);
        
      default:
        return jsonResponse({ 
          error: "Invalid action",
          available: ["getEmployees", "syncFirebaseToSheet", "syncSheetToFirebase", "pullDataFromFirebase", "pushDataToFirebase"]
        }, 400);
    }
  } catch (err) {
    Logger.log("handleEmployeeApi ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

// =======================================================
// GET EMPLOYEES (for API)
// =======================================================
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

// =======================================================
// HELPER: JSON Response
// =======================================================
function jsonResponse(obj, status) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}









