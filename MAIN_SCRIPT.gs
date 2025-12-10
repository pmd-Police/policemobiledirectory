/********** MAIN APPS SCRIPT - ROUTING & CONFIG **********/
/** This is the main entry point for the Apps Script web app **/
/** Import IMAGE_UPLOAD.gs into the same project **/

/** ---------- CONFIG - EDIT THESE BEFORE DEPLOY ---------- **/

const SHEET_ID = "1g0ex1MgMc6mf9bJUG511M-v6FNq704ocimK3I4j9NzE";
const SHEET_NAME = "Emp Profiles";
const DRIVE_FOLDER_ID = "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs"; 
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";

/** ------------------------------------------------------ **/

/**
 * ✅ CRITICAL: Always returns JSON, never HTML
 * Helper function for creating JSON responses
 * 
 * @param {Object} obj - Response object
 * @param {number} status - HTTP status code (optional, not used but kept for compatibility)
 * @returns {TextOutput} JSON formatted response
 */
function jsonResponse(obj, status) {
  try {
    const output = ContentService.createTextOutput(JSON.stringify(obj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  } catch (err) {
    // Even if JSON.stringify fails, return something
    const errorObj = { success: false, error: "Failed to create JSON response: " + err.toString() };
    const output = ContentService.createTextOutput(JSON.stringify(errorObj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

/**
 * ✅ CRITICAL: doPost MUST always return JSON
 * This is the entry point - if it doesn't return, Apps Script shows HTML error
 * Routes requests to appropriate handlers based on action parameter
 * 
 * @param {Object} e - Event object from Apps Script
 * @returns {TextOutput} JSON response
 */
function doPost(e) {
  try {
    // ✅ Always ensure we have parameters
    if (!e) {
      return jsonResponse({ success: false, error: "No event object received" }, 400);
    }
    
    if (!e.parameter) {
      return jsonResponse({ success: false, error: "No parameters. Use ?action=uploadImage" }, 400);
    }

    const action = e.parameter.action;
    
    if (!action) {
      return jsonResponse({ success: false, error: "Missing action parameter. Use ?action=uploadImage" }, 400);
    }
    
    Logger.log("doPost called with action: " + action);

    // ✅ Route to uploadImage handler (from IMAGE_UPLOAD.gs)
    if (action === "uploadImage") {
      // ✅ CRITICAL: Ensure uploadProfileImage returns a value
      // Pass all required config parameters
      const result = uploadProfileImage(
        e, 
        DRIVE_FOLDER_ID, 
        SHEET_ID, 
        SHEET_NAME, 
        FIREBASE_PROJECT_ID, 
        FIREBASE_API_KEY
      );
      
      // ✅ Double-check: if result is null/undefined, return error JSON
      if (!result) {
        Logger.log("ERROR: uploadProfileImage returned null/undefined");
        return jsonResponse({ 
          success: false, 
          error: "uploadProfileImage did not return a value. Check script logs." 
        }, 500);
      }
      return result;
    }

    // Unknown action
    return jsonResponse({ success: false, error: "Unknown action: " + action }, 400);

  } catch (err) {
    // ✅ CRITICAL: Always catch errors and return JSON
    Logger.log("doPost ERROR: " + err.toString());
    Logger.log("doPost ERROR stack: " + (err.stack || "no stack"));
    return jsonResponse({ 
      success: false, 
      error: "Server error in doPost: " + err.toString() 
    }, 500);
  }
}

/**
 * Handle GET requests (if needed)
 * 
 * @param {Object} e - Event object from Apps Script
 * @returns {TextOutput} JSON response
 */
function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ error: "No parameters. Use ?action=..." }, 400);
    }
    
    const action = e.parameter.action;
    
    if (action === "health") {
      return jsonResponse({ 
        status: "ok", 
        message: "Apps Script is running",
        timestamp: new Date().toISOString()
      });
    }
    
    return jsonResponse({ error: "Invalid action" }, 400);
    
  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({ error: err.toString() }, 500);
  }
}



