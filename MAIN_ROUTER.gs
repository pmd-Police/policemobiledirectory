/********************************************************************
 * 🔀 MAIN API ROUTER — For Combined Deployment
 * ---------------------------------------------------------------
 * Routes requests between Image Upload and Employee Sync services
 * Use this ONLY if deploying both services in a single Apps Script project
 ********************************************************************/

/* ================================================================
   API ROUTING
================================================================ */

function doGet(e) {
  try {
    if (!e || !e.parameter) {
      return jsonResponse({ 
        error: "No parameters. Use ?action=...",
        availableActions: {
          imageUpload: "uploadImage",
          employeeSync: ["getEmployees", "pullDataFromFirebase", "pushDataToFirebase", "pushSingleEmployee", "dryRunPush"]
        }
      }, 400);
    }

    const action = e.parameter.action;
    
    if (!action) {
      return jsonResponse({ 
        error: "Missing action parameter",
        usage: "Use ?action=uploadImage or ?action=getEmployees, etc."
      }, 400);
    }

    Logger.log("doGet called with action: " + action);

    // ✅ Route image upload
    if (action === "uploadImage") {
      Logger.log("Routing to uploadProfileImage...");
      return uploadProfileImage(e);
    }

    // ✅ Route employee sync operations
    Logger.log("Routing to handleEmployeeApi...");
    return handleEmployeeApi(e);

  } catch (err) {
    Logger.log("doGet ERROR: " + err.toString());
    return jsonResponse({
      success: false,
      error: "Router error: " + err.toString()
    }, 500);
  }
}

function doPost(e) {
  try {
    Logger.log("=== doPost START ===");
    
    if (!e || !e.parameter) {
      Logger.log("ERROR: No parameters");
      return jsonResponse({ 
        success: false, 
        error: "Missing parameters",
        availableActions: {
          imageUpload: "uploadImage",
          employeeSync: ["getEmployees", "pullDataFromFirebase", "pushDataToFirebase", "pushSingleEmployee", "dryRunPush"]
        }
      }, 400);
    }

    const action = e.parameter.action;
    
    if (!action) {
      Logger.log("ERROR: No action parameter");
      return jsonResponse({ 
        success: false, 
        error: "Missing action parameter. Use ?action=uploadImage or ?action=getEmployees, etc." 
      }, 400);
    }

    Logger.log("doPost called with action: " + action);

    // ✅ Route image upload
    if (action === "uploadImage") {
      Logger.log("Routing to uploadImage...");
      
      try {
        const result = uploadProfileImage(e);
        
        Logger.log("uploadProfileImage returned: " + (result != null ? "result exists" : "null"));
        
        if (!result) {
          Logger.log("ERROR: uploadProfileImage returned null");
          return jsonResponse({
            success: false,
            error: "uploadProfileImage returned null"
          }, 500);
        }

        Logger.log("Returning result from doPost");
        return result;

      } catch (uploadErr) {
        Logger.log("uploadProfileImage ERROR: " + uploadErr.toString());
        Logger.log("Stack: " + (uploadErr.stack || "no stack"));
        return jsonResponse({
          success: false,
          error: "Image upload error: " + uploadErr.toString()
        }, 500);
      }
    }

    // ✅ Route employee sync operations
    Logger.log("Routing to handleEmployeeApi...");
    return handleEmployeeApi(e);

  } catch (err) {
    Logger.log("doPost ERROR: " + err.toString());
    Logger.log("doPost ERROR stack: " + (err.stack || "no stack"));
    return jsonResponse({
      success: false,
      error: "Router error: " + err.toString()
    }, 500);
  }
}

/* ================================================================
   JSON RESPONSE HELPER (Shared)
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



