/**
 * ✅ SIMPLE FIX: uploadProfileImage() Function
 * 
 * This version tries multiple methods to get the image data
 * 
 * Replace your current uploadProfileImage() function with this
 */

function uploadProfileImage(e) {
  try {
    Logger.log('=== uploadProfileImage START ===');
    Logger.log('postData exists: ' + (e.postData != null));
    
    if (e.postData) {
      Logger.log('postData.type: ' + (e.postData.type || 'none'));
      Logger.log('postData.contents length: ' + (e.postData.contents ? e.postData.contents.length : 0));
      Logger.log('postData.bytes length: ' + (e.postData.bytes ? e.postData.bytes.length : 0));
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob;
    let kgid = null;
    
    // ✅ METHOD 1: Try e.postData.bytes directly (sometimes works)
    if (e.postData && e.postData.bytes && e.postData.bytes.length > 0) {
      Logger.log('METHOD 1: Using postData.bytes directly');
      
      // Extract filename and kgid from contents if available
      if (e.postData.contents) {
        const filenameMatch = e.postData.contents.match(/filename="([^"]+)"/);
        if (filenameMatch) {
          const fileName = filenameMatch[1];
          Logger.log('Filename: ' + fileName);
          
          // Extract kgid: "98765.jpg" -> "98765"
          const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
          if (kgidMatch) {
            kgid = kgidMatch[1];
            Logger.log('Extracted kgid: ' + kgid);
          }
        }
      }
      
      blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'upload.jpg');
      Logger.log('Blob created from bytes: ' + blob.getBytes().length + ' bytes');
    }
    
    // ✅ METHOD 2: Parse multipart/form-data manually
    if (!blob && e.postData && e.postData.contents) {
      Logger.log('METHOD 2: Parsing multipart/form-data manually');
      
      const contentType = e.postData.type || '';
      if (contentType.indexOf('multipart') >= 0) {
        // Extract boundary
        const boundaryMatch = contentType.match(/boundary=([^;]+)/);
        if (boundaryMatch) {
          const boundary = '--' + boundaryMatch[1].trim();
          Logger.log('Boundary: ' + boundary);
          
          // Split by boundary
          const parts = e.postData.contents.split(boundary);
          Logger.log('Parts found: ' + parts.length);
          
          // Find file part
          for (let i = 0; i < parts.length; i++) {
            const part = parts[i].trim();
            
            if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
              Logger.log('Found file part');
              
              // Extract filename
              const filenameMatch = part.match(/filename="([^"]+)"/);
              if (filenameMatch) {
                const fileName = filenameMatch[1];
                Logger.log('Filename: ' + fileName);
                
                // Extract kgid
                const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
                if (kgidMatch) {
                  kgid = kgidMatch[1];
                }
              }
              
              // Find header/body separator
              const headerEnd = part.indexOf('\r\n\r\n');
              if (headerEnd >= 0) {
                let fileData = part.substring(headerEnd + 4);
                fileData = fileData.replace(/--\r\n?$/g, '').trim();
                
                Logger.log('File data length: ' + fileData.length);
                
                // Convert string to bytes
                const bytes = [];
                for (let j = 0; j < fileData.length; j++) {
                  bytes.push(fileData.charCodeAt(j) & 0xFF);
                }
                
                blob = Utilities.newBlob(bytes, 'image/jpeg', 'upload.jpg');
                Logger.log('Blob created from parsing: ' + blob.getBytes().length + ' bytes');
                break;
              }
            }
          }
        }
      }
    }
    
    // ✅ METHOD 3: Try accessing blob from form data (if available)
    if (!blob && e.parameter && e.parameter.file) {
      Logger.log('METHOD 3: Using e.parameter.file');
      blob = e.parameter.file;
    }
    
    // Validate blob
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('❌ ERROR: No blob created');
      Logger.log('Debug info:');
      Logger.log('  - postData exists: ' + (e.postData != null));
      Logger.log('  - postData.type: ' + (e.postData ? e.postData.type : 'none'));
      Logger.log('  - postData.contents: ' + (e.postData && e.postData.contents ? 'exists' : 'none'));
      Logger.log('  - postData.bytes: ' + (e.postData && e.postData.bytes ? 'exists' : 'none'));
      Logger.log('  - e.parameter.file: ' + (e.parameter && e.parameter.file ? 'exists' : 'none'));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Please check Apps Script execution logs.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob created successfully: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query if not extracted
    if (!kgid) {
      kgid = e.parameter.kgid;
      Logger.log('Using kgid from query: ' + kgid);
    }
    
    // Create file in Drive
    const fileName = kgid ? ('employee_' + kgid + '_' + new Date().getTime() + '.jpg') : ('employee_' + new Date().getTime() + '.jpg');
    const file = folder.createFile(blob.setName(fileName));
    
    Logger.log('✅ File created in Drive: ' + file.getId());
    
    // Make public
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    
    const fileId = file.getId();
    const driveUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
    
    Logger.log('✅ Public URL: ' + driveUrl);
    
    // Update sheet/Firestore
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

