/**
 * === USEFUL LINKS SCRIPT - SECURE TEMPLATE ===
 * ⚠️ SECURITY: This template includes authentication and validation
 * 
 * URL: https://script.google.com/macros/s/AKfycbyut8D5xNsytdL7m0IDiK5fH2z0s7Kc9eO8bT5IDqCpHworWvaTBMzB0MUcJmszlT1v/exec
 */

// =======================================================
// Constants (from USEFUL_LINKS_Common.gs)
// =======================================================
const SHEET_NAME = "playstore links";
const COMMON_FOLDER_ID = "1EPbT7hlXRRRSwgJ7V_UrfSVv6ZsLT_SF";
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";

// ⚠️ CRITICAL: Generate a strong secret token (32+ characters)
// Use: openssl rand -hex 32
const SECRET_TOKEN = "YOUR_SECRET_TOKEN_HERE";

const FIRESTORE_COLLECTION = "useful_links";
const FIRESTORE_URL = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${FIRESTORE_COLLECTION}`;

// =======================================================
// Security Functions
// =======================================================

/**
 * Verify secret token
 */
function verifyToken(e) {
  const token = e.parameter.token || (e.postData && JSON.parse(e.postData.contents).token);
  if (!token || token !== SECRET_TOKEN) {
    return jsonResponse({ 
      success: false, 
      error: "Unauthorized: Invalid or missing token" 
    }, 401);
  }
  return null; // Token is valid
}

/**
 * Verify admin status in Firestore
 */
function verifyAdmin(email) {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/admins/${encodeURIComponent(email)}?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(url);
    if (response.getResponseCode() === 200) {
      const data = JSON.parse(response.getContentText());
      return data.fields && data.fields.isActive && data.fields.isActive.booleanValue === true;
    }
  } catch (e) {
    Logger.log("Admin verification failed: " + e);
  }
  return false;
}

/**
 * JSON response helper
 */
function jsonResponse(obj, statusCode = 200) {
  const output = ContentService.createTextOutput(JSON.stringify(obj));
  output.setMimeType(ContentService.MimeType.JSON);
  return output;
}

// =======================================================
// Sheet Helpers (from USEFUL_LINKS_Common.gs)
// =======================================================
function getUsefulLinksSheet() {
  return SpreadsheetApp.getActive().getSheetByName(SHEET_NAME) || SpreadsheetApp.getActiveSheet();
}

function getCommonFolder() {
  return DriveApp.getFolderById(COMMON_FOLDER_ID);
}

// =======================================================
// API Handlers
// =======================================================

/**
 * GET Handler - Get all useful links
 */
function doGet(e) {
  try {
    // Verify token
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    const action = e.parameter.action;
    
    if (action === "getLinks" || !action) {
      return getLinks();
    }
    
    return jsonResponse({ 
      success: false, 
      error: "Invalid action. Use ?action=getLinks" 
    }, 400);
    
  } catch (err) {
    Logger.log("doGet error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + err.toString() 
    }, 500);
  }
}

/**
 * POST Handler - Add/Delete links
 */
function doPost(e) {
  try {
    // Verify token
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    const action = e.parameter.action;
    const postData = e.postData ? JSON.parse(e.postData.contents) : {};
    const userEmail = postData.userEmail || e.parameter.email;
    
    switch (action) {
      case "addLink":
        // Verify admin for add operation
        if (!userEmail || !verifyAdmin(userEmail)) {
          return jsonResponse({ 
            success: false, 
            error: "Unauthorized: Admin access required" 
          }, 403);
        }
        return addLink(postData);
        
      case "deleteLink":
        // Verify admin for delete operation
        if (!userEmail || !verifyAdmin(userEmail)) {
          return jsonResponse({ 
            success: false, 
            error: "Unauthorized: Admin access required" 
          }, 403);
        }
        return deleteLink(e.parameter.linkId || postData.linkId);
        
      default:
        return jsonResponse({ 
          success: false, 
          error: "Invalid action. Use ?action=addLink or ?action=deleteLink" 
        }, 400);
    }
    
  } catch (err) {
    Logger.log("doPost error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Server error: " + err.toString() 
    }, 500);
  }
}

/**
 * Get all links from Firestore
 */
function getLinks() {
  try {
    const url = `${FIRESTORE_URL}?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(url);
    
    if (response.getResponseCode() === 200) {
      const data = JSON.parse(response.getContentText());
      const links = (data.documents || []).map(doc => {
        const fields = doc.fields || {};
        return {
          id: doc.name.split('/').pop(),
          name: fields.name?.stringValue || '',
          playStoreUrl: fields.playStoreUrl?.stringValue || null,
          apkUrl: fields.apkUrl?.stringValue || null,
          iconUrl: fields.iconUrl?.stringValue || null,
          category: fields.category?.stringValue || null
        };
      });
      
      return jsonResponse({
        success: true,
        data: links
      });
    }
    
    return jsonResponse({ 
      success: false, 
      error: "Failed to fetch links" 
    }, 500);
    
  } catch (err) {
    Logger.log("getLinks error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Error fetching links: " + err.toString() 
    }, 500);
  }
}

/**
 * Add a new link (Admin only)
 */
function addLink(data) {
  try {
    const { name, playStoreUrl, apkUrl, iconUrl, category } = data;
    
    if (!name) {
      return jsonResponse({ 
        success: false, 
        error: "Missing required field: name" 
      }, 400);
    }
    
    // Create Firestore document
    const document = {
      fields: {
        name: { stringValue: name },
        playStoreUrl: playStoreUrl ? { stringValue: playStoreUrl } : null,
        apkUrl: apkUrl ? { stringValue: apkUrl } : null,
        iconUrl: iconUrl ? { stringValue: iconUrl } : null,
        category: category ? { stringValue: category } : null
      }
    };
    
    // Remove null fields
    Object.keys(document.fields).forEach(key => {
      if (document.fields[key] === null) {
        delete document.fields[key];
      }
    });
    
    const url = `${FIRESTORE_URL}?key=${FIREBASE_API_KEY}`;
    const options = {
      method: 'post',
      contentType: 'application/json',
      payload: JSON.stringify(document)
    };
    
    const response = UrlFetchApp.fetch(url, options);
    
    if (response.getResponseCode() === 200 || response.getResponseCode() === 201) {
      return jsonResponse({ 
        success: true,
        message: "Link added successfully"
      });
    }
    
    return jsonResponse({ 
      success: false, 
      error: "Failed to add link" 
    }, 500);
    
  } catch (err) {
    Logger.log("addLink error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Error adding link: " + err.toString() 
    }, 500);
  }
}

/**
 * Delete a link (Admin only)
 */
function deleteLink(linkId) {
  try {
    if (!linkId) {
      return jsonResponse({ 
        success: false, 
        error: "Missing linkId" 
      }, 400);
    }
    
    const url = `${FIRESTORE_URL}/${linkId}?key=${FIREBASE_API_KEY}`;
    const options = {
      method: 'delete'
    };
    
    const response = UrlFetchApp.fetch(url, options);
    
    if (response.getResponseCode() === 200 || response.getResponseCode() === 204) {
      return jsonResponse({ 
        success: true,
        message: "Link deleted successfully"
      });
    }
    
    return jsonResponse({ 
      success: false, 
      error: "Failed to delete link" 
    }, 500);
    
  } catch (err) {
    Logger.log("deleteLink error: " + err);
    return jsonResponse({ 
      success: false, 
      error: "Error deleting link: " + err.toString() 
    }, 500);
  }
}

