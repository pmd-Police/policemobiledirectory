/**
 * ✅ REPLACE THIS FUNCTION in your Apps Script
 * 
 * This version properly parses multipart/form-data from Android app
 * 
 * The Android app sends:
 * - MultipartBody.Part with name="file" and filename="$userId.jpg"
 * - We need to extract the binary data from the multipart structure
 */

function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage START ===');
    Logger.log('postData exists: ' + (e.postData != null));
    
    if (!e.postData) {
      Logger.log('ERROR: No postData');
      return jsonResponse({ 
        success: false, 
        error: "No POST data received",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('postData.type: ' + (e.postData.type || 'none'));
    Logger.log('postData.contents length: ' + (e.postData.contents ? e.postData.contents.length : 0));
    Logger.log('postData.bytes length: ' + (e.postData.bytes ? e.postData.bytes.length : 0));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob;
    let kgid = null;
    
    // ✅ Parse multipart/form-data
    const contentType = e.postData.type || '';
    Logger.log('Content-Type: ' + contentType);
    
    if (contentType.indexOf('multipart/form-data') >= 0) {
      Logger.log('✅ Detected multipart/form-data');
      
      // Extract boundary from Content-Type header
      const boundaryMatch = contentType.match(/boundary=([^;]+)/);
      if (!boundaryMatch) {
        Logger.log('ERROR: No boundary found');
        return jsonResponse({ 
          success: false, 
          error: "Invalid multipart data: missing boundary",
          url: null,
          id: null
        }, 400);
      }
      
      const boundary = '--' + boundaryMatch[1].trim();
      Logger.log('Boundary: ' + boundary);
      
      // Get raw content (as string for parsing)
      const rawContent = e.postData.contents;
      if (!rawContent) {
        Logger.log('ERROR: No postData.contents');
        return jsonResponse({ 
          success: false, 
          error: "No multipart content received",
          url: null,
          id: null
        }, 400);
      }
      
      Logger.log('Raw content length: ' + rawContent.length);
      
      // Split by boundary to get parts
      const parts = rawContent.split(boundary);
      Logger.log('Parts found: ' + parts.length);
      
      // Find the file part (contains name="file")
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        
        // Skip empty parts or closing boundary
        if (!part || part === '' || part === '--') continue;
        
        // Check if this is the file part
        if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
          Logger.log('✅ Found file part at index ' + i);
          
          // Extract filename
          const filenameMatch = part.match(/filename="([^"]+)"/);
          if (filenameMatch) {
            const fileName = filenameMatch[1];
            Logger.log('Filename: ' + fileName);
            
            // Extract kgid from filename: "98765.jpg" -> "98765"
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              Logger.log('✅ Extracted kgid: ' + kgid);
            }
          }
          
          // Find where headers end (double CRLF or double LF)
          let headerEnd = part.indexOf('\r\n\r\n');
          if (headerEnd < 0) {
            headerEnd = part.indexOf('\n\n');
          }
          
          if (headerEnd < 0) {
            Logger.log('ERROR: No header-body separator found');
            continue;
          }
          
          // Extract file data (everything after headers)
          let fileData = part.substring(headerEnd + (part.indexOf('\r\n\r\n') >= 0 ? 4 : 2));
          
          // Remove trailing boundary markers and whitespace
          fileData = fileData.replace(/--\r\n?$/g, '').replace(/--$/g, '').trim();
          
          Logger.log('File data length: ' + fileData.length);
          
          if (fileData.length === 0) {
            Logger.log('ERROR: File data is empty');
            continue;
          }
          
          // ✅ Convert string to bytes array
          // For binary data in multipart, we need to convert each character to byte
          const bytes = [];
          for (let j = 0; j < fileData.length; j++) {
            const charCode = fileData.charCodeAt(j);
            // For binary data, preserve byte values (0-255)
            bytes.push(charCode & 0xFF);
          }
          
          Logger.log('✅ Converted to ' + bytes.length + ' bytes');
          
          // Create blob from bytes
          blob = Utilities.newBlob(bytes, 'image/jpeg', filenameMatch ? filenameMatch[1] : 'upload.jpg');
          Logger.log('✅ Blob created: ' + blob.getBytes().length + ' bytes');
          
          break; // Found file, exit loop
        }
      }
    } else {
      // Not multipart - try direct bytes
      Logger.log('Not multipart, trying direct bytes');
      if (e.postData.bytes && e.postData.bytes.length > 0) {
        Logger.log('Using postData.bytes directly');
        blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
      }
    }
    
    // Validate blob
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('❌ ERROR: No blob created or blob is empty');
      Logger.log('Debug info:');
      Logger.log('  - postData exists: ' + (e.postData != null));
      Logger.log('  - postData.type: ' + (e.postData ? e.postData.type : 'none'));
      Logger.log('  - postData.contents: ' + (e.postData && e.postData.contents ? 'exists (' + e.postData.contents.length + ' chars)' : 'none'));
      Logger.log('  - postData.bytes: ' + (e.postData && e.postData.bytes ? 'exists (' + e.postData.bytes.length + ' bytes)' : 'none'));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Please check Apps Script execution logs for details.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob validated: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query parameter if not extracted from filename
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query parameter: ' + kgid);
    }
    
    // Create file in Drive
    const driveFileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(driveFileName));
    
    Logger.log('✅ File created in Drive: ' + file.getId());
    
    // Make file publicly viewable
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('✅ Public URL: ' + driveUrl);
    
    // Update sheet/Firestore if kgid available
    if (kgid) {
      Logger.log('Updating sheet and Firestore for kgid: ' + kgid);
      updateSheetFieldByKgid(kgid, "photoUrl", driveUrl);
      updateFirebaseProfileImage(kgid, driveUrl);
    } else {
      Logger.log('⚠️ WARNING: No kgid found, skipping sheet/Firestore update');
    }
    
    // Return success response
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('❌ EXCEPTION: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString(),
      url: null,
      id: null
    }, 500);
  }
}

