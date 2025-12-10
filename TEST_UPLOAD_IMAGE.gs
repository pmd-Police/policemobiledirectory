/********** TEST UPLOAD IMAGE - MINIMAL VERSION FOR DEBUGGING **********/
/** Use this to test if the basic setup works **/

function doPost(e) {
  try {
    Logger.log("=== doPost called ===");
    Logger.log("e exists: " + (e != null));
    Logger.log("e.parameter exists: " + (e && e.parameter != null));
    
    if (!e || !e.parameter) {
      Logger.log("ERROR: No parameters");
      return jsonResponse({ success: false, error: "Missing parameters" }, 400);
    }

    const action = e.parameter.action;
    Logger.log("Action: " + action);

    if (action === "uploadImage") {
      Logger.log("Routing to uploadImage...");
      
      // Check if function exists
      if (typeof uploadProfileImage !== "function") {
        Logger.log("ERROR: uploadProfileImage function not found!");
        return jsonResponse({
          success: false,
          error: "uploadProfileImage function not found. Make sure IMAGE_UPLOAD.gs is in the project."
        }, 500);
      }

      Logger.log("Calling uploadProfileImage...");
      
      // Use hardcoded values for testing
      const DRIVE_FOLDER_ID = "1uzEl9e2uZIarXZhBPb3zlOXJAioeTNFs";
      const SHEET_ID = "1E8cE9zzM3jAHL-a_Cafn5EDWEbk_QNBfOpNtpWwVjfA";
      const SHEET_NAME = "Emp Profiles";
      const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
      const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3EtCmbF9w1BVkrAQ";

      try {
        const result = uploadProfileImage(
          e,
          DRIVE_FOLDER_ID,
          SHEET_ID,
          SHEET_NAME,
          FIREBASE_PROJECT_ID,
          FIREBASE_API_KEY
        );

        Logger.log("uploadProfileImage returned: " + (result != null ? "result exists" : "null"));

        if (!result) {
          Logger.log("ERROR: uploadProfileImage returned null");
          return jsonResponse({
            success: false,
            error: "uploadProfileImage returned null"
          }, 500);
        }

        Logger.log("Returning result...");
        return result;

      } catch (uploadErr) {
        Logger.log("uploadProfileImage threw error: " + uploadErr.toString());
        Logger.log("Stack: " + (uploadErr.stack || "no stack"));
        return jsonResponse({
          success: false,
          error: "uploadProfileImage error: " + uploadErr.toString()
        }, 500);
      }
    }

    Logger.log("Unknown action: " + action);
    return jsonResponse({ success: false, error: "Unknown action: " + action }, 400);

  } catch (err) {
    Logger.log("doPost exception: " + err.toString());
    Logger.log("Stack: " + (err.stack || "no stack"));
    return jsonResponse({
      success: false,
      error: "doPost exception: " + err.toString()
    }, 500);
  }
}

function jsonResponse(obj, status) {
  try {
    const output = ContentService.createTextOutput(JSON.stringify(obj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  } catch (err) {
    const errorObj = { success: false, error: "Failed to create JSON: " + err.toString() };
    const output = ContentService.createTextOutput(JSON.stringify(errorObj));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

// Simple test function
function testDoPost() {
  const testEvent = {
    parameter: { action: "uploadImage" },
    postData: {
      contents: JSON.stringify({
        image: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/",
        filename: "12345.jpg"
      })
    }
  };

  const result = doPost(testEvent);
  Logger.log("Test result: " + result.getContent());
}



