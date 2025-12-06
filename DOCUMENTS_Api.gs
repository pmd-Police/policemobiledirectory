/**
 * === DOCUMENTS SCRIPT - API HANDLERS ===
 * Handles Android app requests via doGet/doPost
 * Upload, Edit, Delete operations from mobile app
 */

// =======================================================
// GET Handler (for external API use)
// =======================================================
function doGet(e) {
  try {
    const action = e?.parameter?.action || "";
    if (action === "getDocuments") return getDocuments();
    return jsonResponse([]);
  } catch (err) {
    // App expects ARRAY always → never send object
    return jsonResponse([]);
  }
}

// =======================================================
// POST Handler (for external API use)
// =======================================================
function doPost(e) {
  try {
    if (!e.postData?.contents) throw new Error("Missing POST body");

    const data = JSON.parse(e.postData.contents);
    
    // ✅ FIX: Get user email from request body (Android sends it)
    // Fallback to Session.getActiveUser() for Google Sheet sidebar usage
    let user = data.userEmail || "";
    if (!user) {
      try {
        user = Session.getActiveUser().getEmail();
      } catch (sessionErr) {
        // Session.getActiveUser() fails for HTTP requests
        Logger.log("Session.getActiveUser() failed (expected for HTTP requests): " + sessionErr);
      }
    }
    
    if (!user) throw new Error("Missing user email");
    if (!isAdmin(user)) throw new Error("Unauthorized user: " + user);
    
    // ✅ FIX: Check query parameter first (Android sends action in query)
    // Then fallback to body action
    let action = (e?.parameter?.action || data.action || "").toLowerCase();
    
    // ✅ Map Android action names to script action names
    if (action === "uploaddocument") action = "upload";
    if (action === "editdocument") action = "edit";
    if (action === "deletedocument") action = "delete";

    switch (action) {
      case "upload": return uploadDocument(data, user);
      case "edit": return editDocument(data, user);
      case "delete": return deleteDocument(data, user);
      default: throw new Error("Invalid POST action: " + action);
    }
  } catch (err) {
    Logger.log("doPost ERROR: " + err.message);
    return jsonResponse({ success: false, error: err.message });
  }
}

// =======================================================
// GET ALL DOCUMENTS (for external API)
// =======================================================
function getDocuments() {
  try {
    const sheet = getSheet();
    const rows = sheet.getDataRange().getValues();
    if (rows.length <= 1) return jsonResponse([]);

    const headers = rows.shift();
    const list = [];

    rows.forEach(row => {
      if (row[6] && row[6].toString().toLowerCase() === "deleted") return;
      const item = {};
      headers.forEach((h, i) => item[h] = row[i]);
      list.push(item);
    });

    return jsonResponse(list);
  } catch (err) {
    Logger.log("getDocuments ERROR: " + err.message);
    // ✅ Always return array, even on error
    return jsonResponse([]);
  }
}

// =======================================================
// UPLOAD DOCUMENT (file from app via Base64)
// =======================================================
function uploadDocument(data, user) {
  // ✅ FIX: Accept both fileBase64 (Android) and fileData (legacy)
  let fileBase64Data = data.fileBase64 || data.fileData;
  const { title, mimeType, category, description } = data;
  if (!fileBase64Data) throw new Error("Missing fileData/fileBase64");

  // ✅ Strip data URI prefix if present (e.g., "data:application/pdf;base64,...")
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }

  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(bytes, mimeType || "application/pdf", title || "Untitled");

  const folder = getFolder();
  const file = folder.createFile(blob);

  // ✅ Ensure file is accessible without Google login
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (shareErr) {
    Logger.log("Warning: Failed to set sharing permissions: " + shareErr);
  }

  const date = new Date();
  const sheet = getSheet();

  sheet.appendRow([
    title || "Untitled",
    file.getUrl(),
    category || "",
    user,
    date,
    description || "",
    ""
  ]);

  logAction("UPLOAD_FILE", title, user);
  saveHistory("UPLOAD_FILE", "", title, "", file.getUrl(), user);

  return jsonResponse({ success: true, action: "upload", url: file.getUrl() });
}

// =======================================================
// EDIT DOCUMENT
// =======================================================
function editDocument(data, user) {
  const { oldTitle, newTitle, newFileData, fileBase64, mimeType, category, description } = data;
  if (!oldTitle) throw new Error("Missing oldTitle");

  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;

  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === oldTitle.trim()) {
      found = true;

      const oldUrl = rows[i][1];
      let newUrl = oldUrl;

      // ✅ Accept both fileBase64 (Android) and newFileData (legacy)
      // If new file uploaded → replace it
      let fileBase64Data = fileBase64 || newFileData;
      if (fileBase64Data) {
        // ✅ Strip data URI prefix if present
        if (fileBase64Data.includes(",")) {
          fileBase64Data = fileBase64Data.split(",")[1];
        }
        
        try {
          const oldId = oldUrl.match(/[-\w]{25,}/)?.[0];
          if (oldId) DriveApp.getFileById(oldId).setTrashed(true);
        } catch (_) {}

        const bytes = Utilities.base64Decode(fileBase64Data);
        const blob = Utilities.newBlob(bytes, mimeType || "application/pdf", newTitle || oldTitle);
        const folder = getFolder();
        const newFile = folder.createFile(blob);
        
        // ✅ Ensure new file is accessible without Google login
        try {
          newFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
        } catch (shareErr) {
          Logger.log("Warning: Failed to set sharing permissions (edit): " + shareErr);
        }
        
        newUrl = newFile.getUrl();
      }

      sheet.getRange(i + 1, 1, 1, 7).setValues([[
        newTitle || oldTitle,
        newUrl,
        category || rows[i][2],
        user,
        new Date(),
        description || rows[i][5],
        rows[i][6] || ""
      ]]);

      logAction("EDIT", oldTitle, user);
      saveHistory("EDIT", oldTitle, newTitle, oldUrl, newUrl, user);

      break;
    }
  }

  if (!found) throw new Error("Document not found: " + oldTitle);

  return jsonResponse({ success: true, action: "edit" });
}

// =======================================================
// DELETE DOCUMENT (SOFT DELETE)
// =======================================================
function deleteDocument(data, user) {
  const { title } = data;
  if (!title) throw new Error("Missing title");

  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;

  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === title.trim()) {
      found = true;

      const url = rows[i][1];
      try {
        const fileId = url.match(/[-\w]{25,}/)?.[0];
        if (fileId) DriveApp.getFileById(fileId).setTrashed(true);
      } catch (_) {}

      sheet.getRange(i + 1, 7).setValue("Deleted");
      sheet.getRange(i + 1, 4).setValue(user);

      logAction("DELETE", title, user);
      saveHistory("DELETE", title, title, url, "", user);

      break;
    }
  }

  if (!found) throw new Error("Document not found: " + title);

  return jsonResponse({ success: true, action: "delete" });
}

