/**
 * Set isApproved = true for ALL employees in Firestore
 * Run this once after uploading employees to Firestore
 */

function setAllEmployeesApproved() {
  const token = getServiceAccountToken();
  let processed = 0;
  let errors = 0;
  
  try {
    // Get all employees from Firestore
    let nextPageToken = null;
    
    do {
      let firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents:runQuery`;
      
      const query = {
        structuredQuery: {
          from: [{ collectionId: "employees" }],
          limit: 500
        }
      };
      
      if (nextPageToken) {
        query.structuredQuery.offset = processed;
      }
      
      const response = UrlFetchApp.fetch(firestoreUrl, {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + token,
          "Content-Type": "application/json"
        },
        payload: JSON.stringify(query),
        muteHttpExceptions: true
      });
      
      if (response.getResponseCode() !== 200) {
        Logger.log("Error fetching employees: " + response.getContentText());
        break;
      }
      
      const result = JSON.parse(response.getContentText());
      
      if (!result || result.length === 0) {
        break;
      }
      
      // Process each document
      result.forEach(item => {
        if (item.document) {
          try {
            const docPath = item.document.name;
            const kgid = docPath.split("/").pop();
            const fields = item.document.fields || {};
            
            // Skip if already approved
            if (fields.isApproved && fields.isApproved.booleanValue === true) {
              return;
            }
            
            // Update isApproved to true
            const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${encodeURIComponent(kgid)}`;
            
            const updateResponse = UrlFetchApp.fetch(docUrl, {
              method: "PATCH",
              headers: {
                "Authorization": "Bearer " + token,
                "Content-Type": "application/json"
              },
              payload: JSON.stringify({
                fields: {
                  isApproved: { booleanValue: true },
                  updatedAt: { timestampValue: new Date().toISOString() }
                }
              }),
              muteHttpExceptions: true
            });
            
            if (updateResponse.getResponseCode() === 200) {
              processed++;
              Logger.log(`✅ Approved employee: ${kgid}`);
            } else {
              errors++;
              Logger.log(`❌ Failed to approve ${kgid}: ${updateResponse.getContentText()}`);
            }
          } catch (e) {
            errors++;
            Logger.log(`Error processing document: ${e.toString()}`);
          }
        }
      });
      
      // Check for next page
      nextPageToken = result.nextPageToken;
      
      // Add small delay to avoid rate limits
      Utilities.sleep(500);
      
    } while (nextPageToken);
    
    Logger.log(`✅ Completed! Processed: ${processed}, Errors: ${errors}`);
    return {
      success: true,
      processed: processed,
      errors: errors,
      message: `Successfully approved ${processed} employees`
    };
    
  } catch (e) {
    Logger.log("Error in setAllEmployeesApproved: " + e.toString());
    return {
      success: false,
      error: e.toString()
    };
  }
}

/**
 * Helper function - Make sure this exists in your script
 * If not, copy the getServiceAccountToken() function from your main sync script
 */
function getServiceAccountToken() {
  // This should match your existing implementation
  // Return the OAuth token for Firebase API access
  // (Copy from EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs)
}








