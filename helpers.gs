/**
 * PMD HELPERS — SECURITY UPGRADED VERSION
 * Includes:
 *  - Firestore field formatter
 *  - Token validation
 *  - Admin verification (Firestore-based)
 *  - Utility functions
 */

// 🔥 Set same token in all 6 Apps Scripts & Android
const SECRET_TOKEN = "Ravi@PMD_2025_Secure_Token";
const FIREBASE_PROJECT_ID = PropertiesService.getScriptProperties().getProperty("PROJECT_ID");

/**
 * 🔐 TOKEN VALIDATION
 */
function verifyToken(e) {
  let token = e?.parameter?.token;
  // POST body token
  if (!token && e?.postData?.contents) {
    try {
      const body = JSON.parse(e.postData.contents);
      token = body.token;
    } catch (_) {
      // ignore parse errors
    }
  }
  if (!token || token !== SECRET_TOKEN) {
    return jsonResponse({ success: false, error: "Unauthorized: Invalid token" }, 401);
  }
  return null; // OK
}

/**
 * 🔐 ADMIN VALIDATION USING FIRESTORE
 * Firestore path: /admins/{email}
 * Document must contain → isActive: true
 */
function isAdmin(email) {
  if (!email) return false;
  return verifyAdminFromFirestore(email);
}

function verifyAdminFromFirestore(email) {
  try {
    const url =
      `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}` +
      `/databases/(default)/documents/admins/${encodeURIComponent(email)}`;
    const response = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
    if (response.getResponseCode() !== 200) return false;
    const data = JSON.parse(response.getContentText());
    return data.fields?.isActive?.booleanValue === true;
  } catch (e) {
    Logger.log("Admin check failed: " + e);
    return false;
  }
}

/**
 * JSON Response Wrapper
 */
function jsonResponse(obj, statusCode = 200) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

/*******************************************************
 * Firestore Formatters (required for Service Account)
 *******************************************************/
function formatFirestoreData(obj) {
  const result = {};
  Object.keys(obj).forEach(key => {
    const value = obj[key];
    if (value === null || value === undefined) {
      result[key] = { nullValue: null };
    } else if (typeof value === "string") {
      result[key] = { stringValue: value };
    } else if (typeof value === "number") {
      result[key] = Number.isInteger(value)
        ? { integerValue: value }
        : { doubleValue: value };
    } else if (typeof value === "boolean") {
      result[key] = { booleanValue: value };
    } else if (value instanceof Date) {
      result[key] = { timestampValue: value.toISOString() };
    } else if (Array.isArray(value)) {
      result[key] = {
        arrayValue: {
          values: value.map(v => formatFirestoreData({ temp: v }).temp)
        }
      };
    } else if (typeof value === "object") {
      result[key] = {
        mapValue: { fields: formatFirestoreData(value) }
      };
    } else {
      result[key] = { stringValue: String(value) };
    }
  });
  return result;
}

function nowSeconds() {
  return Math.floor(Date.now() / 1000);
}

function randomOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

