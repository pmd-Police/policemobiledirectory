/**
 * ✅ DEBUG VERSION - Enhanced Logging
 * 
 * This version has extensive logging to help diagnose the issue
 * Replace your uploadProfileImage() function with this one
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
    Logger.log('postData.contents exists: ' + (e.postData.contents != null));
    Logger.log('postData.contents type: ' + typeof(e.postData.contents));
    Logger.log('postData.contents length: ' + (e.postData.contents ? e.postData.contents.length : 0));
    Logger.log('postData.bytes exists: ' + (e.postData.bytes != null));
    Logger.log('postData.bytes length: ' + (e.postData.bytes ? e.postData.bytes.length : 0));
    
    // Log first 500 chars of contents to see structure
    if (e.postData.contents) {
      Logger.log('First 500 chars of contents: ' + e.postData.contents.substring(0, 500));
    }
    
    const folder = DriveApp.getFolderById(DRIVE_FOLDER_ID);
    let blob;
    let kgid = null;
    
    // ✅ Try multiple approaches to parse multipart
    if (contentType.indexOf('multipart/form-data') >= 0) {
      Logger.log('✅ Detected multipart/form-data');
      
      // Extract boundary
      const boundaryMatch = contentType.match(/boundary=([^;]+)/);
      if (!boundaryMatch) {
        Logger.log('ERROR: No boundary found in Content-Type');
        Logger.log('Full Content-Type: ' + contentType);
        return jsonResponse({ 
          success: false, 
          error: "Invalid multipart: missing boundary",
          url: null,
          id: null
        }, 400);
      }
      
      const boundary = '--' + boundaryMatch[1].trim();
      Logger.log('Extracted boundary: ' + boundary);
      Logger.log('Boundary length: ' + boundary.length);
      
      // Get raw content
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
      
      Logger.log('Raw content total length: ' + rawContent.length);
      
      // Check if boundary exists in content
      const boundaryIndex = rawContent.indexOf(boundary);
      Logger.log('Boundary found at index: ' + boundaryIndex);
      
      if (boundaryIndex < 0) {
        Logger.log('ERROR: Boundary not found in content');
        Logger.log('Trying without -- prefix...');
        const boundaryNoPrefix = boundaryMatch[1].trim();
        const boundaryIndex2 = rawContent.indexOf(boundaryNoPrefix);
        Logger.log('Boundary (no prefix) found at index: ' + boundaryIndex2);
      }
      
      // Split by boundary
      const parts = rawContent.split(boundary);
      Logger.log('Parts found after split: ' + parts.length);
      
      // Log first 200 chars of each part for debugging
      for (let i = 0; i < Math.min(parts.length, 5); i++) {
        Logger.log('Part ' + i + ' length: ' + parts[i].length);
        Logger.log('Part ' + i + ' first 200 chars: ' + parts[i].substring(0, 200));
      }
      
      // Find the file part
      let filePartFound = false;
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i].trim();
        
        // Skip empty parts or closing boundary
        if (!part || part === '' || part === '--') {
          Logger.log('Skipping part ' + i + ' (empty or closing boundary)');
          continue;
        }
        
        Logger.log('Checking part ' + i + ' for file data...');
        Logger.log('Part ' + i + ' contains name="file": ' + (part.indexOf('name="file"') >= 0));
        Logger.log('Part ' + i + ' contains name=\'file\': ' + (part.indexOf("name='file'") >= 0));
        
        // Check if this is the file part
        if (part.indexOf('name="file"') >= 0 || part.indexOf("name='file'") >= 0) {
          Logger.log('✅ Found file part at index ' + i);
          filePartFound = true;
          
          // Extract filename
          const filenameMatch = part.match(/filename="([^"]+)"/);
          if (filenameMatch) {
            const fileName = filenameMatch[1];
            Logger.log('Extracted filename: ' + fileName);
            
            // Extract kgid: "98765.jpg" -> "98765"
            const kgidMatch = fileName.match(/^(\d+)\.jpg$/);
            if (kgidMatch) {
              kgid = kgidMatch[1];
              Logger.log('✅ Extracted kgid: ' + kgid);
            }
          } else {
            Logger.log('WARNING: No filename found in file part');
          }
          
          // Find header/body separator
          let headerEnd = part.indexOf('\r\n\r\n');
          let headerEndOffset = 4;
          
          if (headerEnd < 0) {
            headerEnd = part.indexOf('\n\n');
            headerEndOffset = 2;
            Logger.log('Using \\n\\n as separator');
          } else {
            Logger.log('Using \\r\\n\\r\\n as separator');
          }
          
          if (headerEnd < 0) {
            Logger.log('ERROR: No header-body separator found in part ' + i);
            Logger.log('Part ' + i + ' first 300 chars: ' + part.substring(0, 300));
            continue;
          }
          
          Logger.log('Header ends at position: ' + headerEnd);
          
          // Extract file data (after headers)
          let fileData = part.substring(headerEnd + headerEndOffset);
          
          // Remove trailing boundary markers
          fileData = fileData.replace(/--\r\n?$/g, '').replace(/--$/g, '').trim();
          
          Logger.log('File data length after extraction: ' + fileData.length);
          
          if (fileData.length === 0) {
            Logger.log('ERROR: File data is empty after extraction');
            continue;
          }
          
          // Log first 100 and last 100 chars of file data
          Logger.log('File data first 100 chars: ' + fileData.substring(0, 100));
          Logger.log('File data last 100 chars: ' + fileData.substring(Math.max(0, fileData.length - 100)));
          
          // ✅ Convert string to bytes array
          const bytes = [];
          for (let j = 0; j < fileData.length; j++) {
            const charCode = fileData.charCodeAt(j);
            bytes.push(charCode & 0xFF);
          }
          
          Logger.log('✅ Converted to ' + bytes.length + ' bytes');
          
          // Validate bytes array
          if (bytes.length === 0) {
            Logger.log('ERROR: Bytes array is empty');
            continue;
          }
          
          // Check if it looks like JPEG (starts with FF D8 FF)
          if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
            Logger.log('✅ File data looks like valid JPEG (starts with FF D8 FF)');
          } else {
            Logger.log('WARNING: File data does not start with JPEG signature');
            Logger.log('First 10 bytes: ' + bytes.slice(0, 10).map(b => '0x' + b.toString(16).toUpperCase()).join(' '));
          }
          
          // Create blob from bytes
          const finalFileName = filenameMatch ? filenameMatch[1] : 'upload.jpg';
          blob = Utilities.newBlob(bytes, 'image/jpeg', finalFileName);
          
          Logger.log('✅ Blob created: ' + blob.getBytes().length + ' bytes');
          
          if (blob.getBytes().length === 0) {
            Logger.log('ERROR: Blob is empty after creation');
            continue;
          }
          
          break; // Found file, exit loop
        }
      }
      
      if (!filePartFound) {
        Logger.log('ERROR: File part (name="file") not found in any part');
        Logger.log('Searching for any part with "file" keyword...');
        for (let i = 0; i < parts.length; i++) {
          if (parts[i].indexOf('file') >= 0) {
            Logger.log('Found "file" keyword in part ' + i);
            Logger.log('Part ' + i + ' first 300 chars: ' + parts[i].substring(0, 300));
          }
        }
      }
    } else {
      Logger.log('Not multipart, Content-Type: ' + contentType);
      // Try direct bytes
      if (e.postData.bytes && e.postData.bytes.length > 0) {
        Logger.log('Trying direct bytes: ' + e.postData.bytes.length + ' bytes');
        blob = Utilities.newBlob(e.postData.bytes, 'image/jpeg', 'profile.jpg');
      }
    }
    
    // ✅ Final validation
    if (!blob || blob.getBytes().length === 0) {
      Logger.log('❌ ERROR: No blob created or blob is empty');
      Logger.log('Final debug info:');
      Logger.log('  - Content-Type: ' + contentType);
      Logger.log('  - postData.contents: ' + (e.postData.contents ? 'exists (' + e.postData.contents.length + ' chars)' : 'none'));
      Logger.log('  - postData.bytes: ' + (e.postData.bytes ? 'exists (' + e.postData.bytes.length + ' bytes)' : 'none'));
      
      return jsonResponse({ 
        success: false, 
        error: "No image data received. Check Apps Script execution logs for detailed debugging info.",
        url: null,
        id: null
      }, 400);
    }
    
    Logger.log('✅ Blob validated: ' + blob.getBytes().length + ' bytes');
    
    // Get kgid from query if not extracted
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

