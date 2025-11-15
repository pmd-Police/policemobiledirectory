/**
 * ✅ CORRECTED uploadProfileImage function
 * 
 * Fixed to properly handle multipart/form-data and return correct response format
 */

function uploadProfileImage(e) {
  try {
    Logger.log('uploadProfileImage called');
    Logger.log('postData type: ' + (e.postData ? e.postData.type : 'none'));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    let blob;
    
    // ✅ Handle multipart/form-data (what Android app sends)
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      // Use bytes directly - Apps Script handles multipart parsing
      Logger.log('Using postData.bytes, length: ' + e.postData.bytes.length);
      blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
    } else if (e.postData && e.postData.contents) {
      // Fallback: try to parse as JSON (for base64 uploads)
      try {
        const data = JSON.parse(e.postData.contents);
        if (data.image) {
          const base64 = data.image.split(',')[1] || data.image;
          blob = Utilities.newBlob(Utilities.base64Decode(base64), 'image/png', 'profile.png');
        }
      } catch (e) {
        Logger.log('Could not parse as JSON');
      }
    }
    
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('No blob created');
      return jsonResponse({ 
        success: false, 
        error: "No image data received",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('Blob created, size: ' + blob.getBytes().length);
    
    // Create file in Drive
    const fileName = 'employee_' + new Date().getTime() + '.jpg';
    const file = folder.createFile(blob.setName(fileName));
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('File created: ' + fileId);
    Logger.log('Public URL: ' + driveUrl);
    
    // Get kgid from query parameter (if provided)
    // Note: The Android app doesn't send kgid in query, it's in the file name
    // But we can still try to update sheet/Firestore if kgid is provided
    const kgid = e.parameter.kgid;
    
    if (kgid) {
      Logger.log('Updating sheet for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    // ✅ FIXED: Return format that matches GDriveUploadResponse
    // The Android app expects: {success: true, url: "...", id: "...", error: null}
    return jsonResponse({ 
      success: true, 
      url: driveUrl,  // ✅ Changed from imageUrl to url
      id: fileId,     // ✅ Added id field
      error: null     // ✅ Added error field (null for success)
    });
    
  } catch (err) {
    Logger.log('Error in uploadProfileImage: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString(),
      url: null,
      id: null
    }, 500);
  }
}

