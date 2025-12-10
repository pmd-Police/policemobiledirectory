/********** MINIMAL TEST - VERIFY DEPLOYMENT WORKS **********/
/** Use this to test if deployment is working **/
/** If this works, then add the upload logic back **/

function doPost(e) {
  Logger.log("=== doPost TEST CALLED ===");
  Logger.log("e exists: " + (e != null));
  
  try {
    const response = {
      success: true,
      message: "doPost is working!",
      action: e && e.parameter ? e.parameter.action : "none",
      timestamp: new Date().toISOString()
    };
    
    Logger.log("Returning: " + JSON.stringify(response));
    
    const output = ContentService.createTextOutput(JSON.stringify(response));
    output.setMimeType(ContentService.MimeType.JSON);
    
    Logger.log("About to return output");
    return output;
    
  } catch (err) {
    Logger.log("ERROR in doPost: " + err.toString());
    const errorResponse = {
      success: false,
      error: err.toString()
    };
    const output = ContentService.createTextOutput(JSON.stringify(errorResponse));
    output.setMimeType(ContentService.MimeType.JSON);
    return output;
  }
}

function doGet(e) {
  return ContentService.createTextOutput(JSON.stringify({
    status: "ok",
    message: "GET endpoint works"
  })).setMimeType(ContentService.MimeType.JSON);
}



