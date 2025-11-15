/**
 * ✅ FIXED: uploadProfileImage() Function
 * 
 * This version correctly handles multipart/form-data from Android Retrofit
 * 
 * The key issue: e.postData.bytes contains the ENTIRE multipart body including
 * boundaries and headers. We need to extract just the binary file data.
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
    
    const contentType = e.postData.type || '';
    Logger.log('Content-Type: ' + contentType);
    Logger.log('postData.contents length: ' + (e.postData.contents ? e.postData.contents.length : 0));
    Logger.log('postData.bytes length: ' + (e.postData.bytes ? e.postData.bytes.length : 0));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob;
    let kgid = null;
    
    // ✅ CRITICAL FIX: For multipart/form-data, we need to parse manually
    if (contentType.indexOf('multipart/form-data') >= 0) {
      Logger.log('✅ Detected multipart/form-data');
      
      // Extract boundary
      const boundaryMatch = contentType.match(/boundary=([^;]+)/);
      if (!boundaryMatch) {
        Logger.log('ERROR: No boundary found');
        return jsonResponse({ 
          success: false, 
          error: "Invalid multipart: missing boundary",
          url: null,
          id: null
        }, 400);
      }
      
      const boundary = '--' + boundaryMatch[1].trim();
      Logger.log('Boundary: ' + boundary);
      
      // ✅ Convert bytes array to string for parsing (safe for boundaries/headers)
      // We'll extract binary data separately
      const rawContent = e.postData.contents;
      if (!rawContent) {
        Logger.log('ERROR: No postData.contents');
        return jsonResponse({ 
          success: false, 
          error: "No multipart content",
          url: null,
          id: null
        }, 400);
      }
      
      Logger.log('Raw content length: ' + rawContent.length);
      
      // Split by boundary to get parts
      const parts = rawContent.split(boundary);
      Logger.log('Parts found: ' + parts.length);
      
      // Find the file part
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        
        // Skip empty parts or closing boundary
        if (!part || part === '' || part === '--') continue;
        
        // Check if this is the file part (contains name="file")
        if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
          Logger.log('✅ Found file part at index ' + i);
          
          // Extract filename
          const filenameMatch = part.match(/filename="([^"]+)"/);
          if (filenameMatch) {
            const fileName = filenameMatch[1];
            Logger.log('Filename: ' + fileName);
            
            // Extract kgid: "98765.jpg" -> "98765"
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              Logger.log('✅ Extracted kgid: ' + kgid);
            }
          }
          
          // Find header/body separator (double CRLF or double LF)
          let headerEnd = part.indexOf('\r\n\r\n');
          let headerEndOffset = 4;
          
          if (headerEnd < 0) {
            headerEnd = part.indexOf('\n\n');
            headerEndOffset = 2;
          }
          
          if (headerEnd < 0) {
            Logger.log('ERROR: No header-body separator');
            continue;
          }
          
          Logger.log('Header ends at position: ' + headerEnd);
          
          // Extract file data (after headers)
          let fileData = part.substring(headerEnd + headerEndOffset);
          
          // Remove trailing boundary markers
          fileData = fileData.replace(/--\r\n?$/g, '').replace(/--$/g, '').trim();
          
          Logger.log('File data length: ' + fileData.length);
          
          if (fileData.length === 0) {
            Logger.log('ERROR: File data is empty after parsing');
            continue;
          }
          
          // ✅ Convert string to bytes array (preserve binary data)
          // Each character code is converted to a byte (0-255)
          const bytes = [];
          for (let j = 0; j < fileData.length; j++) {
            const charCode = fileData.charCodeAt(j);
            // Preserve byte value (0-255)
            bytes.push(charCode & 0xFF);
          }
          
          Logger.log('✅ Converted to ' + bytes.length + ' bytes');
          
          // Create blob from bytes
          const finalFileName = filenameMatch ? filenameMatch[1] : 'upload.jpg';
          blob = Utilities.newBlob(bytes, 'image/jpeg', finalFileName);
          
          Logger.log('✅ Blob created: ' + blob.getBytes().length + ' bytes');
          
          // Validate blob size matches expectation
          if (blob.getBytes().length === 0) {
            Logger.log('ERROR: Blob is empty');
            continue;
          }
          
          break; // Found file, exit loop
        }
      }
    } else {
      // Not multipart - try direct bytes
      Logger.log('Not multipart, trying direct bytes');
      if (e.postData.bytes && e.postData.bytes.length > 0) {
        Logger.log('Using postData.bytes directly: ' + e.postData.bytes.length + ' bytes');
        blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
      }
    }
    
    // ✅ Final validation
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('❌ ERROR: No blob created or blob is empty');
      Logger.log('Debug info:');
      Logger.log('  - Content-Type: ' + contentType);
      Logger.log('  - postData.contents: ' + (e.postData.contents ? 'exists (' + e.postData.contents.length + ' chars)' : 'none'));
      Logger.log('  - postData.bytes: ' + (e.postData.bytes ? 'exists (' + e.postData.bytes.length + ' bytes)' : 'none'));
      
      // Log first 200 chars of contents for debugging
      if (e.postData.contents) {
        Logger.log('  - First 200 chars: ' + e.postData.contents.substring(0, 200));
      }
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Check Apps Script execution logs for details.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob validated: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query if not extracted
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query: ' + kgid);
    }
    
    // Create file in Drive
    const driveFileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(driveFileName));
    
    Logger.log('✅ File created in Drive: ' + file.getId());
    
    // Make public
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('✅ Public URL: ' + driveUrl);
    
    // Update sheet/Firestore
    if (kgid) {
      Logger.log('Updating for kgid: ' + kgid);
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

