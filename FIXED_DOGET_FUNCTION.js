/**
 * ============================================
 * REPLACE YOUR CURRENT doGet() FUNCTION WITH THIS
 * ============================================
 * 
 * This version handles BOTH:
 * 1. API requests (with ?action= or ?module= parameters) → Returns JSON
 * 2. UI requests (no parameters) → Returns HTML Sidebar
 */

// --- WEB APP ENTRY POINT (FIXED VERSION) ---
function doGet(e) {
  try {
    // If parameters exist, treat as API request
    if (e && e.parameter && (e.parameter.action || e.parameter.module)) {
      return handleApiRequest(e);
    }
    
    // Otherwise, return the HTML sidebar UI
    return HtmlService.createTemplateFromFile("Sidebar")
      .evaluate()
      .setTitle("Employee Management")
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
      
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return ContentService
      .createTextOutput(JSON.stringify({ error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

// =======================================================
// API REQUEST HANDLER
// =======================================================
function handleApiRequest(e) {
  try {
    const module = (e.parameter.module || "").toLowerCase();
    const action = e.parameter.action || "";
    
    // Route based on module parameter
    if (module) {
      switch (module) {
        case "employees":
          return handleEmployeesApi(e, action);
        case "documents":
          return handleDocumentsApi(e, action);
        case "gallery":
          return handleGalleryApi(e, action);
        case "usefullinks":
        case "links":
          return handleUsefulLinksApi(e);
        default:
          return jsonResponse({ 
            error: "Invalid module. Use: employees, documents, gallery, or usefullinks" 
          }, 400);
      }
    }
    
    // Legacy format: ?action=... (assumes employees module)
    if (action) {
      return handleEmployeesApi(e, action);
    }
    
    return jsonResponse({ 
      error: "No action specified. Use ?action=getEmployees or ?module=employees&action=getEmployees" 
    }, 400);
    
  } catch (err) {
    Logger.log("handleApiRequest ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

// =======================================================
// EMPLOYEES API HANDLER
// =======================================================
function handleEmployeesApi(e, action) {
  try {
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
          error: "Invalid action for employees module",
          available: ["getEmployees", "syncFirebaseToSheet", "syncSheetToFirebase", "pullDataFromFirebase", "pushDataToFirebase"]
        }, 400);
    }
  } catch (err) {
    Logger.log("handleEmployeesApi ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}

// =======================================================
// DOCUMENTS API HANDLER
// =======================================================
function handleDocumentsApi(e, action) {
  try {
    // Check if getDocuments function exists (from other files)
    if (typeof getDocuments === 'function') {
      if (action === "getDocuments") {
        return getDocuments();
      }
      return jsonResponse([]);
    }
    return jsonResponse({ error: "Documents module not available" }, 404);
  } catch (err) {
    Logger.log("handleDocumentsApi ERROR: " + err.toString());
    return jsonResponse([]);
  }
}

// =======================================================
// GALLERY API HANDLER
// =======================================================
function handleGalleryApi(e, action) {
  try {
    // Check if getGallery function exists (from other files)
    if (typeof getGallery === 'function') {
      if (action === "getGallery") {
        return getGallery();
      }
      return jsonResponse([]);
    }
    return jsonResponse({ error: "Gallery module not available" }, 404);
  } catch (err) {
    Logger.log("handleGalleryApi ERROR: " + err.toString());
    return jsonResponse([]);
  }
}

// =======================================================
// USEFUL LINKS API HANDLER
// =======================================================
function handleUsefulLinksApi(e) {
  try {
    // Check if getUsefulLinksSheet function exists (from other files)
    if (typeof getUsefulLinksSheet === 'function') {
      const sheet = getUsefulLinksSheet();
      const data = sheet.getDataRange().getValues();
      const result = [];
      
      for (let i = 1; i < data.length; i++) {
        if (!data[i][0]) continue;
        result.push({
          name: data[i][0],
          playStoreUrl: data[i][1],
          apkUrl: data[i][4],
          category: data[i][3],
        });
      }
      
      return ContentService
        .createTextOutput(JSON.stringify(result))
        .setMimeType(ContentService.MimeType.JSON);
    }
    return jsonResponse({ error: "Useful Links module not available" }, 404);
  } catch (err) {
    Logger.log("handleUsefulLinksApi ERROR: " + err.toString());
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









