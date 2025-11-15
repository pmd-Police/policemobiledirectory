/**
 * ✅ COMPLETE FIXED uploadProfileImage() Function
 * 
 * This function properly handles multipart/form-data from Android app
 * 
 * Replace your current uploadProfileImage() function with this one
 */

function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage called ===');
    Logger.log('postData exists: ' + (e.postData != null));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    let blob;
    let fileName = '';
    let kgid = null;
    
    // ✅ Handle multipart/form-data (what Android app sends)
    if (e.postData && e.postData.contents) {
      const contentType = e.postData.type || '';
      Logger.log('Content-Type: ' + contentType);
      
      // Check if it's multipart
      if (contentType.indexOf('multipart/form-data') >= 0) {
        Logger.log('Processing multipart/form-data');
        
        // Extract boundary from Content-Type
        const boundaryMatch = contentType.match(/boundary=([^;]+)/);
        if (!boundaryMatch) {
          Logger.log('ERROR: No boundary found');
          return jsonResponse({ 
            success: false, 
            error: "Invalid multipart data: no boundary",
            url: null,
            id: null
          }, 400);
        }
        
        const boundary = '--' + boundaryMatch[1].trim();
        Logger.log('Boundary: ' + boundary);
        
        // Split by boundary to get parts
        const rawContent = e.postData.contents;
        const parts = rawContent.split(boundary);
        Logger.log('Number of parts: ' + parts.length);
        
        // Find the file part
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i].trim();
          
          // Skip empty parts
          if (!part || part === '' || part === '--') continue;
          
          // Check if this part contains the file
          if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
            Logger.log('Found file part at index: ' + i);
            
            // Extract filename if present
            const filenameMatch = part.match(/filename="([^"]+)"/);
            if (filenameMatch) {
              fileName = filenameMatch[1];
              Logger.log('Filename: ' + fileName);
              
              // Extract kgid from filename: "98765.jpg" -> "98765"
              const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
              if (kgidMatch) {
                kgid = kgidMatch[1];
                Logger.log('Extracted kgid: ' + kgid);
              }
            }
            
            // Find where headers end (double CRLF)
            const headerEnd = part.indexOf('\r\n\r\n');
            if (headerEnd >= 0) {
              // Extract file data (after headers)
              let fileData = part.substring(headerEnd + 4);
              
              // Remove trailing boundary markers
              fileData = fileData.replace(/--\r\n?$/g, '').trim();
              
              Logger.log('File data length: ' + fileData.length);
              
              // Convert string to bytes array
              const bytes = [];
              for (let j = 0; j < fileData.length; j++) {
                bytes.push(fileData.charCodeAt(j) & 0xFF);
              }
              
              Logger.log('Converted to ' + bytes.length + ' bytes');
              
              // Create blob from bytes
              blob = Utilities.newBlob(bytes, 'image/jpeg', fileName || 'upload.jpg');
              
              break; // Found file, exit loop
            }
          }
        }
      } else {
        // Not multipart - try direct bytes
        Logger.log('Not multipart, trying direct bytes');
        if (e.postData.bytes && e.postData.bytes.length > 0) {
          Logger.log('Using postData.bytes: ' + e.postData.bytes.length + ' bytes');
          blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
        }
      }
    }
    
    // ✅ Fallback: Try bytes directly (sometimes works for multipart)
    if (!blob && e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log('Fallback: Using postData.bytes directly');
      blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
    }
    
    // Validate blob
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('ERROR: No blob created');
      Logger.log('postData exists: ' + (e.postData != null));
      Logger.log('postData type: ' + (e.postData ? e.postData.type : 'none'));
      Logger.log('postData contents length: ' + (e.postData && e.postData.contents ? e.postData.contents.length : 0));
      Logger.log('postData bytes length: ' + (e.postData && e.postData.bytes ? e.postData.bytes.length : 0));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Check Apps Script logs for debugging details.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob created, size: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query parameter if not extracted from filename
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query parameter: ' + kgid);
    }
    
    // Create file in Drive
    const driveFileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(driveFileName));
    
    Logger.log('✅ File created: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('✅ Public URL: ' + driveUrl);
    
    // Update sheet/Firestore if kgid available
    if (kgid) {
      Logger.log('Updating for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    }
    
    // Return success response
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('❌ ERROR: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString(),
      url: null,
      id: null
    }, 500);
  }
}

