/**
 * === USEFUL LINKS SCRIPT - COMMON UTILITIES ===
 * Shared constants and utility functions for Useful Links API
 */

// =======================================================
// Constants
// =======================================================
const SHEET_NAME = "playstore links";
const COMMON_FOLDER_ID = "1EPbT7hlXRRRSwgJ7V_UrfSVv6ZsLT_SF";

const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";
const API_KEY = FIREBASE_API_KEY; // Backwards compatibility

// Firestore REST API URL
const FIRESTORE_COLLECTION = "useful_links";

const FIRESTORE_URL =
  `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}` +
  `/databases/(default)/documents/${FIRESTORE_COLLECTION}`;

// Firebase Storage upload URL
const STORAGE_UPLOAD_URL =
  `https://firebasestorage.googleapis.com/v0/b/${FIREBASE_PROJECT_ID}.appspot.com/o`;

// =======================================================
// Sheet / Drive Helpers
// =======================================================

function getUsefulLinksSheet() {
  return SpreadsheetApp.getActive().getSheetByName(SHEET_NAME) || SpreadsheetApp.getActiveSheet();
}

function getCommonFolder() {
  return DriveApp.getFolderById(COMMON_FOLDER_ID);
}

function buildDriveDirectUrl(fileId) {
  return `https://drive.google.com/uc?export=download&id=${fileId}`;
}

// =======================================================
// Utility Functions
// =======================================================

/**
 * Normalize Play Store URL by extracting id=packageName and returning canonical form
 */
function normalizePlayUrl(url) {
  try {
    if (!url) return null;
    const u = url.toString();
    const id = u.includes("id=") ? u.split("id=")[1].split("&")[0] : u;
    return id.trim();
  } catch (e) {
    return url;
  }
}

/**
 * Find existing Firestore document by playStoreUrl (preferred) or by name.
 * Returns docId if found, otherwise null.
 */
function findExistingFirestoreDoc(appName, playUrl) {
  try {
    const pageSize = 200;
    let nextPageToken = null;
    do {
      const url = `${FIRESTORE_URL}?pageSize=${pageSize}` + (nextPageToken ? `&pageToken=${nextPageToken}` : "");
      const resp = UrlFetchApp.fetch(url);
      const json = JSON.parse(resp.getContentText());
      const docs = json.documents || [];
      
      for (const doc of docs) {
        const fields = doc.fields || {};
        const nameField = fields.name && fields.name.stringValue ? fields.name.stringValue : null;
        const playField = fields.playStoreUrl && fields.playStoreUrl.stringValue ? fields.playStoreUrl.stringValue : null;
        
        // 1) Prefer exact playStoreUrl match (strip utm/query parts for reliable match)
        if (playUrl && playField) {
          const normA = normalizePlayUrl(playUrl);
          const normB = normalizePlayUrl(playField);
          if (normA === normB) {
            return doc.name.split("/").pop();
          }
        }
        
        // 2) Fallback to exact name match
        if (nameField && appName && nameField === appName) {
          return doc.name.split("/").pop();
        }
      }
      
      nextPageToken = json.nextPageToken || null;
    } while (nextPageToken);
  } catch (e) {
    Logger.log("Find Doc Error: " + e);
  }
  return null;
}

/**
 * Upload APK file to Firebase Storage
 */
function uploadApkToFirebase(fileId, appName) {
  try {
    const file = DriveApp.getFileById(fileId);
    const blob = file.getBlob();
    const uploadUrl =
      `${STORAGE_UPLOAD_URL}/${encodeURIComponent(appName + ".apk")}` +
      `?uploadType=media&name=${encodeURIComponent(appName + ".apk")}`;
    
    const options = {
      method: "post",
      contentType: "application/vnd.android.package-archive",
      payload: blob.getBytes(),
      muteHttpExceptions: true,
    };
    
    const response = UrlFetchApp.fetch(uploadUrl, options);
    const json = JSON.parse(response.getContentText());
    
    return `https://firebasestorage.googleapis.com/v0/b/${FIREBASE_PROJECT_ID}.appspot.com/o/` +
           `${encodeURIComponent(appName + ".apk")}?alt=media&token=${json.downloadTokens}`;
  } catch (e) {
    Logger.log("APK Upload Error: " + e);
    return "";
  }
}

/**
 * Upload or update Firestore document.
 * Uses playStoreUrl matching first, then name matching.
 */
function uploadToFirestore(appName, playUrl, storageUrl, category) {
  try {
    // Try to find existing doc by playStoreUrl or name
    const docId = findExistingFirestoreDoc(appName, playUrl);
    
    // Build body with fields we want to set/update.
    const body = {
      fields: {
        name:          { stringValue: appName },
        playStoreUrl:  { stringValue: playUrl || "" },
        apkUrl:        { stringValue: storageUrl || "" },
        category:      { stringValue: category || "" },
        // Do not overwrite iconUrl if already present in Firestore (preserve)
        iconUrl:       { stringValue: "" }
      }
    };
    
    if (docId) {
      // PATCH (update) - preserve existing fields if necessary by reading existing doc first
      // Read existing doc to preserve iconUrl if present
      const existingResp = UrlFetchApp.fetch(`${FIRESTORE_URL}/${docId}`);
      if (existingResp.getResponseCode() === 200) {
        try {
          const existingJson = JSON.parse(existingResp.getContentText());
          const existingIcon = existingJson.fields && existingJson.fields.iconUrl && existingJson.fields.iconUrl.stringValue;
          if (existingIcon) {
            body.fields.iconUrl.stringValue = existingIcon;
          }
        } catch (e) {
          // ignore parse errors - continue with empty icon
        }
      }
      
      const updateUrl = `${FIRESTORE_URL}/${docId}`;
      const params = {
        method: "patch",
        contentType: "application/json",
        payload: JSON.stringify(body),
        muteHttpExceptions: true,
      };
      const res = UrlFetchApp.fetch(updateUrl, params);
      return res.getResponseCode() === 200;
    } else {
      // Create new document
      const params = {
        method: "post",
        contentType: "application/json",
        payload: JSON.stringify(body),
        muteHttpExceptions: true,
      };
      const res = UrlFetchApp.fetch(FIRESTORE_URL, params);
      return res.getResponseCode() === 200;
    }
  } catch (e) {
    Logger.log("Firestore Error: " + e);
    return false;
  }
}

/**
 * Delete Firestore document(s) that match appName or playUrl.
 * Optionally returns number of deleted docs.
 */
function deleteFirestoreEntry(appName, playUrl) {
  try {
    const pageSize = 200;
    let nextPageToken = null;
    let deleted = 0;
    
    do {
      const url = `${FIRESTORE_URL}?pageSize=${pageSize}` + (nextPageToken ? `&pageToken=${nextPageToken}` : "");
      const resp = UrlFetchApp.fetch(url);
      const json = JSON.parse(resp.getContentText());
      const docs = json.documents || [];
      
      for (const doc of docs) {
        const fields = doc.fields || {};
        const nameField = fields.name && fields.name.stringValue ? fields.name.stringValue : null;
        const playField = fields.playStoreUrl && fields.playStoreUrl.stringValue ? fields.playStoreUrl.stringValue : null;
        
        // Match either by normalized playUrl or by exact name
        const matchesPlay = playUrl && playField && normalizePlayUrl(playUrl) === normalizePlayUrl(playField);
        const matchesName = appName && nameField && nameField === appName;
        
        if (matchesPlay || matchesName) {
          const id = doc.name.split("/").pop();
          UrlFetchApp.fetch(`${FIRESTORE_URL}/${id}`, { method: "delete" });
          deleted++;
        }
      }
      
      nextPageToken = json.nextPageToken || null;
    } while (nextPageToken);
    
    return deleted;
  } catch (e) {
    Logger.log("Delete Error: " + e);
    return 0;
  }
}

// =======================================================
// Combined Upload Helpers
// =======================================================

function fetchPlayStoreIcon(playUrl) {
  try {
    if (!playUrl) return null;
    const resp = UrlFetchApp.fetch(playUrl, { muteHttpExceptions: true });
    if (resp.getResponseCode() !== 200) return null;
    const html = resp.getContentText();
    const match = html.match(/<meta[^>]*property="og:image"[^>]*content="([^"]+)"/i);
    return match && match[1] ? match[1] : null;
  } catch (e) {
    Logger.log("fetchPlayStoreIcon error: " + e);
    return null;
  }
}

function uploadImageToDriveFromUrl(imageUrl, fileName) {
  try {
    if (!imageUrl) return { id: "", url: "" };
    const response = UrlFetchApp.fetch(imageUrl, { muteHttpExceptions: true });
    if (response.getResponseCode() !== 200) {
      return { id: "", url: "" };
    }
    const contentType = response.getHeaders()["Content-Type"] || "image/png";
    const blob = Utilities.newBlob(response.getContent(), contentType, fileName);
    const file = getCommonFolder().createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    return { id: file.getId(), url: buildDriveDirectUrl(file.getId()) };
  } catch (e) {
    Logger.log("uploadImageToDriveFromUrl error: " + e);
    return { id: "", url: "" };
  }
}

function uploadBinaryToDrive(base64Data, fileName, mimeType) {
  try {
    if (!base64Data) return { id: "", url: "" };
    const cleanData = base64Data.includes(",") ? base64Data.split(",")[1] : base64Data;
    const bytes = Utilities.base64Decode(cleanData);
    const blob = Utilities.newBlob(bytes, mimeType, fileName);
    const file = getCommonFolder().createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    return { id: file.getId(), url: buildDriveDirectUrl(file.getId()) };
  } catch (e) {
    Logger.log("uploadBinaryToDrive error: " + e);
    return { id: "", url: "" };
  }
}

function uploadToFirestoreCombined(payload) {
  try {
    const { appName, playUrl, apkUrl, category, iconUrl } = payload;
    const docId = findExistingFirestoreDoc(appName, playUrl);

    const body = {
      fields: {
        name:         { stringValue: appName || "" },
        playStoreUrl: { stringValue: playUrl || "" },
        apkUrl:       { stringValue: apkUrl || "" },
        category:     { stringValue: category || "" },
        iconUrl:      { stringValue: iconUrl || "" },
      },
    };

    if (docId) {
      // Preserve existing icon if not provided this time
      if (!iconUrl) {
        const existingResp = UrlFetchApp.fetch(`${FIRESTORE_URL}/${docId}`);
        if (existingResp.getResponseCode() === 200) {
          try {
            const existingJson = JSON.parse(existingResp.getContentText());
            const existingIcon = existingJson.fields && existingJson.fields.iconUrl && existingJson.fields.iconUrl.stringValue;
            if (existingIcon) {
              body.fields.iconUrl.stringValue = existingIcon;
            }
          } catch (_) {}
        }
      }
      const res = UrlFetchApp.fetch(`${FIRESTORE_URL}/${docId}`, {
        method: "patch",
        contentType: "application/json",
        payload: JSON.stringify(body),
        muteHttpExceptions: true,
      });
      return res.getResponseCode() === 200;
    } else {
      const res = UrlFetchApp.fetch(FIRESTORE_URL, {
        method: "post",
        contentType: "application/json",
        payload: JSON.stringify(body),
        muteHttpExceptions: true,
      });
      return res.getResponseCode() === 200;
    }
  } catch (e) {
    Logger.log("uploadToFirestoreCombined error: " + e);
    return false;
  }
}

