/**
 * === GALLERY SCRIPT - API HANDLERS ===
 * Handles Android app requests via doGet/doPost
 * Upload, Edit, Delete operations from mobile app
 */

// =======================================================
// GET Handler (for external API use)
// =======================================================
function doGet(e) {
  try {
    const action = e?.parameter?.action || "";
    if (action === "getGallery") return getGallery();
    return jsonResponse([]);
  } catch (err) {
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
    
    // 1️⃣ Get user from body (Android: userEmail), fallback to Session for sidebar
    let user = data.userEmail || "";
    if (!user) {
      try {
        user = Session.getActiveUser().getEmail();
      } catch (sessionErr) {
        Logger.log("Session.getActiveUser() failed: " + sessionErr);
      }
    }
    
    if (!user) throw new Error("Missing user email");
    if (!isAdmin(user)) throw new Error("Unauthorized user: " + user);
    
    // 2️⃣ Action from query ?action= OR body
    let action = (e?.parameter?.action || data.action || "").toLowerCase();
    
    // 3️⃣ Map Android action names to internal
    if (action === "uploadgallery") action = "upload";
    if (action === "editgallery") action = "edit";
    if (action === "deletegallery") action = "delete";
    
    switch (action) {
      case "upload": return uploadGalleryItem(data, user);
      case "edit":   return editGalleryItem(data, user);
      case "delete": return deleteGalleryItem(data, user);
      default: throw new Error("Invalid POST action: " + action);
    }
  } catch (err) {
    Logger.log("doPost ERROR: " + err.message);
    return jsonResponse({ success: false, error: err.message });
  }
}

// =======================================================
// GET ALL GALLERY ITEMS (for external API)
// =======================================================
function getGallery() {
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
    
    // Latest first
    list.reverse();
    return jsonResponse(list);
  } catch (err) {
    Logger.log("getGallery ERROR: " + err.message);
    return jsonResponse([]);
  }
}

// =======================================================
// UPLOAD GALLERY ITEM (file from app via Base64)
// =======================================================
function uploadGalleryItem(data, user) {
  // Accept fileBase64 (Android) or fileData (legacy)
  let fileBase64Data = data.fileBase64 || data.fileData;
  const { title, mimeType, category, description } = data;
  if (!fileBase64Data) throw new Error("Missing fileData/fileBase64");
  
  // Strip data URI prefix if present
  if (fileBase64Data.includes(",")) {
    fileBase64Data = fileBase64Data.split(",")[1];
  }
  
  const bytes = Utilities.base64Decode(fileBase64Data);
  const blob = Utilities.newBlob(
    bytes,
    mimeType || "image/jpeg",
    title || "Untitled"
  );
  
  const folder = getFolder();
  const file = folder.createFile(blob);
  
  // ✅ Convert to direct image URL format for gallery display
  const fileId = file.getId();
  const directImageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
  
  // Make file public by link
  try {
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (shareErr) {
    Logger.log("Warning: Failed to set sharing permissions: " + shareErr);
  }
  
  const date = new Date();
  const sheet = getSheet();
  
  sheet.appendRow([
    title || "Untitled",
    directImageUrl,  // ✅ Store direct image URL instead of view URL
    category || "Gallery",
    user,
    date,
    description || "",
    ""
  ]);
  
  logAction("UPLOAD_IMAGE", title, user);
  saveHistory("UPLOAD_IMAGE", "", title, "", directImageUrl, user);
  
  return jsonResponse({ success: true, action: "upload", url: directImageUrl });
}

// =======================================================
// EDIT GALLERY ITEM
// =======================================================
function editGalleryItem(data, user) {
  const {
    oldTitle,
    newTitle,
    newFileData,     // legacy name from sidebar/app
    fileBase64,      // Android-friendly name
    mimeType,
    category,
    description
  } = data;
  
  if (!oldTitle) throw new Error("Missing oldTitle");
  
  const sheet = getSheet();
  const rows = sheet.getDataRange().getValues();
  let found = false;
  
  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0].toString().trim() === oldTitle.trim()) {
      found = true;
      const oldUrl = rows[i][1];
      let newUrl = oldUrl;
      
      // --- Handle optional new file (from app or sidebar) ---
      // Accept both fileBase64 and newFileData for compatibility
      let fileBase64Data = fileBase64 || newFileData;
      
      if (fileBase64Data) {
        // Strip data URI prefix if present
        if (fileBase64Data.indexOf(",") !== -1) {
          fileBase64Data = fileBase64Data.split(",")[1];
        }
        
        try {
          // Try to trash old file if it's a Drive URL
          const oldId = oldUrl && oldUrl.match(/[-\w]{25,}/)?.[0];
          if (oldId) {
            DriveApp.getFileById(oldId).setTrashed(true);
          }
        } catch (e) {
          Logger.log("EDIT: failed to trash old file: " + e);
        }
        
        const bytes = Utilities.base64Decode(fileBase64Data);
        const blob = Utilities.newBlob(
          bytes,
          mimeType || "image/jpeg",
          (newTitle || oldTitle) || "Gallery Image"
        );
        
        const folder = getFolder();
        const newFile = folder.createFile(blob);
        
        // ✅ Convert to direct image URL
        const fileId = newFile.getId();
        const directImageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
        
        try {
          newFile.setSharing(
            DriveApp.Access.ANYONE_WITH_LINK,
            DriveApp.Permission.VIEW
          );
        } catch (shareErr) {
          Logger.log("Warning: Failed to set sharing permissions (edit): " + shareErr);
        }
        
        newUrl = directImageUrl;
      }
      
      // --- Update row in sheet ---
      sheet.getRange(i + 1, 1, 1, 7).setValues([[
        newTitle || oldTitle,           // Title
        newUrl,                         // URL
        category || rows[i][2],         // Category (keep old if not provided)
        user,                           // Uploaded By (editor)
        new Date(),                     // Uploaded Date (edit time)
        description || rows[i][5],      // Description (keep old if not provided)
        rows[i][6] || ""                // Delete flag (leave as is)
      ]]);
      
      logAction("EDIT_IMAGE", oldTitle, user);
      saveHistory("EDIT_IMAGE", oldTitle, newTitle || oldTitle, oldUrl, newUrl, user);
      
      break;
    }
  }
  
  if (!found) throw new Error("Gallery item not found: " + oldTitle);
  return jsonResponse({ success: true, action: "edit" });
}

// =======================================================
// DELETE GALLERY ITEM (SOFT DELETE)
// =======================================================
function deleteGalleryItem(data, user) {
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
      
      logAction("DELETE_IMAGE", title, user);
      saveHistory("DELETE_IMAGE", title, title, url, "", user);
      
      break;
    }
  }
  
  if (!found) throw new Error("Gallery item not found: " + title);
  return jsonResponse({ success: true, action: "delete" });
}

