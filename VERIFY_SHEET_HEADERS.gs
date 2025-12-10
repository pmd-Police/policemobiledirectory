/**
 * Helper function to verify and fix Google Sheet headers
 * Run this to ensure all required headers are present and correctly named
 */

function verifyAndFixHeaders() {
  const sheet = getSheet();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0].map(h => String(h).trim());
  
  // Expected headers in order (based on your Google Sheet structure)
  const expectedHeaders = [
    "kgid",
    "name", 
    "mobile1",
    "mobile2",    // Column D might be "mobil" - will handle mapping
    "rank",
    "station",
    "district",
    "metal",      // Must be "metal" not "metalNumber"
    "bloodGroup",
    "email",
    "photoUrl",
    "photoUrlFromGoogle",
    "firebaseUid",
    "fcmToken",
    "isAdmin",
    "isApproved",
    "pin",
    "createdAt",
    "updatedAt",
    "isDeleted"
  ];
  
  const missingHeaders = [];
  const headerMap = new Map();
  
  // Check each expected header
  expectedHeaders.forEach(header => {
    const idx = headers.indexOf(header);
    if (idx < 0) {
      // Check for common variations
      if (header === "mobile2") {
        const mobilIdx = headers.indexOf("mobil");
        if (mobilIdx >= 0) {
          Logger.log(`Found "mobil" at column ${mobilIdx + 1}, mapping to "mobile2"`);
          headerMap.set(header, mobilIdx);
          return;
        }
      }
      missingHeaders.push(header);
    } else {
      headerMap.set(header, idx);
    }
  });
  
  if (missingHeaders.length > 0) {
    Logger.log("Missing headers: " + missingHeaders.join(", "));
    Logger.log("Please add these headers to Row 1 of your sheet");
    return {
      success: false,
      missing: missingHeaders,
      message: `Missing headers: ${missingHeaders.join(", ")}`
    };
  }
  
  Logger.log("✅ All required headers are present!");
  return {
    success: true,
    message: "All headers verified",
    headers: Array.from(headerMap.keys())
  };
}

/**
 * Get the correct column index for a header, handling variations
 */
function getHeaderIndex(headers, headerName) {
  let idx = headers.indexOf(headerName);
  
  // Handle variations
  if (idx < 0 && headerName === "mobile2") {
    idx = headers.indexOf("mobil");
  }
  
  return idx;
}








