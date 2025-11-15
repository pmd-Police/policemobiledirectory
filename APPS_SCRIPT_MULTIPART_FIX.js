/**
 * ✅ FIXED: uploadProfileImage() - Proper Multipart Parsing
 * 
 * This version correctly parses multipart/form-data from Android app
 */

function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage called ===');
    Logger.log('postData exists: ' + (e.postData != null));
    Logger.log('postData type: ' + (e.postData ? e.postData.type : 'none'));
    Logger.log('postData contents length: ' + (e.postData && e.postData.contents ? e.postData.contents.length : 0));
    Logger.log('postData bytes length: ' + (e.postData && e.postData.bytes ? e.postData.bytes.length : 0));
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    
    let blob;
    let fileName = '';
    let kgid = null;
    
    // ✅ FIXED: Proper multipart/form-data parsing
    if (e.postData && e.postData.contents) {
      const contentType = e.postData.type || '';
      Logger.log('Content-Type: ' + contentType);
      
      if (contentType.indexOf('multipart/form-data') >= 0) {
        // ✅ Parse multipart form data
        const boundaryMatch = contentType.match(/boundary=([^;]+)/);
        if (!boundaryMatch) {
          Logger.log('ERROR: No boundary found in Content-Type');
          return jsonResponse({ 
            success: false, 
            error: "Invalid multipart data: no boundary",
            url: null,
            id: null
          }, 400);
        }
        
        const boundary = boundaryMatch[1].trim();
        Logger.log('Boundary: ' + boundary);
        
        // Split by boundary
        const parts = e.postData.contents.split('--' + boundary);
        Logger.log('Number of parts: ' + parts.length);
        
        // Find the file part
        for (let i = 0; i < parts.length; i++) {
          const part = parts[i];
          
          // Skip empty parts
          if (!part || part.trim() === '' || part.trim() === '--') continue;
          
          // Check if this is the file part
          if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
            Logger.log('Found file part at index: ' + i);
            
            // Extract filename if present
            const filenameMatch = part.match(/filename="([^"]+)"/);
            if (filenameMatch) {
              fileName = filenameMatch[1];
              Logger.log('Extracted filename: ' + fileName);
              
              // Extract kgid from filename: "98765.jpg" -> "98765"
              const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
              if (kgidMatch) {
                kgid = kgidMatch[1];
                Logger.log('Extracted kgid from filename: ' + kgid);
              }
            }
            
            // Find the double CRLF that separates headers from body
            const headerEnd = part.indexOf('\r\n\r\n');
            if (headerEnd < 0) {
              Logger.log('ERROR: No header-body separator found');
              continue;
            }
            
            // Extract file data (everything after headers)
            let fileData = part.substring(headerEnd + 4);
            
            // Remove trailing boundary markers and whitespace
            fileData = fileData.replace(/--\r\n$/, '').replace(/--$/, '').trim();
            
            Logger.log('File data length: ' + fileData.length);
            
            // ✅ Convert to blob
            // For multipart, the data is binary, so we need to convert properly
            try {
              // Try to get bytes directly if available
              if (e.postData.bytes && e.postData.bytes.length > 0) {
                // Use bytes directly - this is the most reliable
                Logger.log('Using postData.bytes directly');
                blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', fileName || 'profile.jpg');
              } else {
                // Fallback: convert string to bytes
                Logger.log('Converting string to bytes');
                const bytes = [];
                for (let j = 0; j < fileData.length; j++) {
                  bytes.push(fileData.charCodeAt(j) & 0xFF);
                }
                blob = Utilities.newBlob(bytes, 'image/jpeg', fileName || 'profile.jpg');
              }
            } catch (err) {
              Logger.log('ERROR creating blob: ' + err.toString());
              return jsonResponse({ 
                success: false, 
                error: "Failed to create image blob: " + err.toString(),
                url: null,
                id: null
              }, 400);
            }
            
            break; // Found file, exit loop
          }
        }
      } else {
        // Not multipart - try direct bytes or JSON
        Logger.log('Not multipart, trying direct bytes or JSON');
        if (e.postData.bytes && e.postData.bytes.length > 0) {
          blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
        } else if (e.postData.contents) {
          try {
            const data = JSON.parse(e.postData.contents);
            if (data.image) {
              const base64 = data.image.split(',')[1] || data.image;
              blob = Utilities.newBlob(Utilities.base64Decode(base64), 'image/png', 'profile.png');
            }
          } catch (err) {
            Logger.log('Could not parse as JSON: ' + err.toString());
          }
        }
      }
    } else {
      Logger.log('ERROR: No postData or postData.contents');
    }
    
    // ✅ Validate blob
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('ERROR: No blob created or blob is empty');
      Logger.log('postData exists: ' + (e.postData != null));
      Logger.log('postData contents: ' + (e.postData && e.postData.contents ? 'exists (' + e.postData.contents.length + ' chars)' : 'none'));
      Logger.log('postData bytes: ' + (e.postData && e.postData.bytes ? 'exists (' + e.postData.bytes.length + ' bytes)' : 'none'));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Check Apps Script logs for details.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob created successfully, size: ' + blob.getBytes().length + ' bytes');
    
    // ✅ Get kgid from query parameter if not extracted from filename
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
      Logger.log('WARNING: No kgid found, skipping sheet/Firestore update');
    }
    
    // ✅ Return format that matches Android app
    return jsonResponse({ 
      success: true, 
      url: driveUrl,
      id: fileId,
      error: null
    });
    
  } catch (err) {
    Logger.log('❌ ERROR in uploadProfileImage: ' + err.toString());
    Logger.log('Stack: ' + err.stack);
    return jsonResponse({ 
      success: false, 
      error: err.toString(),
      url: null,
      id: null
    }, 500);
  }
}

