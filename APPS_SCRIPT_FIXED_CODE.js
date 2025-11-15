/**
 * ✅ FIXED uploadProfileImage function
 * 
 * This version properly handles multipart/form-data from the Android app
 */

function uploadProfileImage(e) {
  try {
    Logger.log('uploadProfileImage called');
    Logger.log('postData type: ' + (e.postData ? e.postData.type : 'none'));
    Logger.log('postData contents length: ' + (e.postData && e.postData.contents ? e.postData.contents.length : 0));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    let blob;
    
    // Check if we have postData (multipart form data)
    if (e.postData && e.postData.contents) {
      // Parse multipart/form-data
      const contentType = e.postData.type;
      Logger.log('Content-Type: ' + contentType);
      
      if (contentType && contentType.indexOf('multipart/form-data') >= 0) {
        // Extract boundary from Content-Type
        const boundaryMatch = contentType.match(/boundary=([^;]+)/);
        const boundary = boundaryMatch ? boundaryMatch[1] : '----WebKitFormBoundary';
        
        Logger.log('Boundary: ' + boundary);
        
        // Split by boundary
        const parts = e.postData.contents.split('--' + boundary);
        
        // Find the file part
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i];
          
          // Look for the file field (name="file")
          if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
            Logger.log('Found file part at index: ' + i);
            
            // Split headers and body
            const headerEnd = part.indexOf('\r\n\r\n');
            if (headerEnd < 0) continue;
            
            // Extract file data (skip headers)
            const fileData = part.substring(headerEnd + 4);
            
            // Remove trailing boundary markers
            const cleanData = fileData.replace(/--\r\n$/, '').replace(/--$/, '').trim();
            
            // Convert to blob
            // For multipart, the data might be binary or base64
            // Try to get as bytes directly
            if (e.postData.bytes && e.postData.bytes.length > 0) {
              // Use bytes if available (more reliable)
              blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
            } else {
              // Fallback: try to decode as base64 or use as-is
              try {
                const decoded = Utilities.base64Decode(cleanData);
                blob = Utilities.newBlob(decoded, 'image/jpeg', 'profile.jpg');
              } catch (e) {
                // If base64 decode fails, use raw string as bytes
                blob = Utilities.newBlob(cleanData, 'image/jpeg', 'profile.jpg');
              }
            }
            
            break;
          }
        }
      } else {
        // Not multipart, try to get bytes directly
        if (e.postData.bytes && e.postData.bytes.length > 0) {
          blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
        } else {
          // Try to parse as JSON with base64
          try {
            const data = JSON.parse(e.postData.contents);
            if (data.image) {
              const base64 = data.image.split(',')[1] || data.image;
              const bytes = Utilities.base64Decode(base64);
              blob = Utilities.newBlob(bytes, 'image/png', 'profile.png');
            }
          } catch (e) {
            Logger.log('Could not parse as JSON: ' + e.toString());
          }
        }
      }
    }
    
    if (!blob) {
      Logger.log('No blob created - returning error');
      return jsonResponse({ 
        success: false, 
        error: "No image payload found. Expected multipart/form-data with file field." 
      }, 400);
    }
    
    Logger.log('Blob created, size: ' + blob.getBytes().length);
    
    // Create file in Drive
    const fileName = 'employee_' + new Date().getTime() + '.jpg';
    const file = folder.createFile(blob.setName(fileName));
    
    Logger.log('File created in Drive: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('Public URL: ' + driveUrl);
    
    // Get kgid from query parameter (if provided)
    const kgid = e.parameter.kgid;
    
    // Update sheet if kgid provided
    if (kgid) {
      Logger.log('Updating sheet for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      
      // Also update Firestore
      updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    // ✅ Return format that matches GDriveUploadResponse
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('Error in uploadProfileImage: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString() 
    }, 500);
  }
}

/**
 * ✅ ALTERNATIVE SIMPLER VERSION
 * 
 * If the above doesn't work, try this simpler version that uses e.postData.bytes directly
 */

function uploadProfileImageSimple(e) {
  try {
    Logger.log('uploadProfileImageSimple called');
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    // Try to get bytes directly (most reliable for multipart)
    let blob;
    
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log('Using postData.bytes, length: ' + e.postData.bytes.length);
      blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
    } else if (e.postData && e.postData.contents) {
      Logger.log('Trying to parse postData.contents');
      // Try JSON first
      try {
        const data = JSON.parse(e.postData.contents);
        if (data.image) {
          const base64 = data.image.split(',')[1] || data.image;
          blob = Utilities.newBlob(Utilities.base64Decode(base64), 'image/png', 'profile.png');
        }
      } catch (e) {
        Logger.log('Not JSON, trying as raw string');
        // Might be raw binary string
        blob = Utilities.newBlob(e.postData.contents, 'image/jpeg', 'profile.jpg');
      }
    }
    
    if (!blob || blob.getBytes().length === 0) {
      return jsonResponse({ 
        success: false, 
        error: "No image data received" 
      }, 400);
    }
    
    Logger.log('Blob created, size: ' + blob.getBytes().length);
    
    // Create file
    const fileName = 'employee_' + new Date().getTime() + '.jpg';
    const file = folder.createFile(blob.setName(fileName));
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    // Update sheet/Firestore if kgid provided
    const kgid = e.parameter.kgid;
    if (kgid) {
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('Error: ' + err.toString());
    return jsonResponse({ 
      success: false, 
      error: err.toString() 
    }, 500);
  }
}

