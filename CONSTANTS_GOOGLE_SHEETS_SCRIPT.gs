/**
 * ✅ Constants Google Sheets Sync Script
 * 
 * This script reads constants (ranks, districts, stations, blood groups) from Google Sheets
 * and serves them to the Android app via a web app endpoint.
 * 
 * SETUP INSTRUCTIONS:
 * 1. Create a Google Sheet with the following structure:
 *    - Sheet 1: "rank" - Column A: Rank names (one per row, header in row 1)
 *    - Sheet 2: "district" - Column A: District names (one per row, header in row 1)
 *    - Sheet 3: "station" - Column A: District name, Column B: Station name (header in row 1)
 *    - Sheet 4: "bloodgroup" - Column A: Blood group names (one per row, header in row 1)
 * 
 * 2. Update the SHEET_ID below with your Google Sheet ID
 * 3. Deploy as a Web App with "Execute as: Me" and "Who has access: Anyone"
 * 4. Copy the Web App URL and update NetworkModule.kt CONSTANTS_BASE_URL with the new URL
 */

const SHEET_ID = "1gmUXQn1Fp2JEmNWzicNNDJurUYeS-D5XMyc_pym0avI";

function doGet(e) {
  try {
    const ss = SpreadsheetApp.openById(SHEET_ID);
    const constants = getAllConstants(ss);
    return ContentService
      .createTextOutput(JSON.stringify({ success: true, data: constants }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    Logger.log("Error in doGet: " + err.toString());
    return ContentService
      .createTextOutput(JSON.stringify({ 
        success: false, 
        error: err.toString(),
        data: {
          ranks: [],
          districts: [],
          stationsbydistrict: {},
          bloodgroups: [],
          lastupdated: new Date().toISOString(),
          version: 1
        }
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function getAllConstants(ss) {
  try {
    const districts = getDistricts(ss);
    const stations = getStationsByDistrict(ss, districts);
    return {
      ranks: getRanks(ss),
      districts: districts,
      stationsbydistrict: stations,
      bloodgroups: getBloodGroups(ss),
      lastupdated: new Date().toISOString(),
      version: 1  // Version number - increment when constants structure changes
    };
  } catch (err) {
    Logger.log("Error in getAllConstants: " + err.toString());
    // Return empty structure on error
    return {
      ranks: [],
      districts: [],
      stationsbydistrict: {},
      bloodgroups: [],
      lastupdated: new Date().toISOString(),
      version: 1
    };
  }
}

// -------------------------------
// RANK
// -------------------------------
function getRanks(ss) {
  try {
    const sheet = ss.getSheetByName("rank");
    if (!sheet) {
      Logger.log("Rank sheet not found");
      return [];
    }
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return []; // Only header or empty
    
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values
      .map(r => r[0])
      .filter(v => v && v.toString().trim() !== "")
      .map(v => v.toString().trim())
      .sort();
  } catch (err) {
    Logger.log("Error getting ranks: " + err.toString());
    return [];
  }
}

// -------------------------------
// DISTRICT
// -------------------------------
function getDistricts(ss) {
  try {
    const sheet = ss.getSheetByName("district");
    if (!sheet) {
      Logger.log("District sheet not found");
      return [];
    }
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return []; // Only header or empty
    
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values
      .map(r => r[0])
      .filter(v => v && v.toString().trim() !== "")
      .map(v => v.toString().trim())
      .sort();
  } catch (err) {
    Logger.log("Error getting districts: " + err.toString());
    return [];
  }
}

// -------------------------------
// STATION (SAFE VERSION - FIXED TO READ ALL ROWS)
// -------------------------------
function getStationsByDistrict(ss, districts) {
  try {
    const sheet = ss.getSheetByName("station");
    if (!sheet) {
      Logger.log("Station sheet not found");
      return {};
    }
    
    // Initialize all districts with empty list (case-insensitive map for matching)
    const map = {};
    const districtLookup = {}; // Case-insensitive lookup: lowercase -> actual district name
    districts.forEach(d => {
      map[d] = [];
      districtLookup[d.toLowerCase().trim()] = d;
    });
    
    // Use getDataRange() to get ALL data, then filter out header and empty rows
    const dataRange = sheet.getDataRange();
    if (!dataRange || dataRange.getNumRows() <= 1) {
      Logger.log("Station sheet has no data rows (only header or empty)");
      return map;
    }
    
    const allValues = dataRange.getValues();
    Logger.log("Total rows in station sheet (including header): " + allValues.length);
    
    // Process all rows except header (skip row 0)
    let processedCount = 0;
    let skippedEmptyCount = 0;
    
    for (let i = 1; i < allValues.length; i++) {
      const row = allValues[i];
      const districtRaw = row[0];
      const stationRaw = row[1];
      
      // Skip rows where district is empty
      if (!districtRaw || districtRaw.toString().trim() === "") {
        skippedEmptyCount++;
        continue;
      }
      
      const districtStr = districtRaw.toString().trim();
      const districtLower = districtStr.toLowerCase();
      
      // Find matching district (case-insensitive)
      let matchedDistrict = districtLookup[districtLower];
      
      // If no exact match found, try to find a district that contains this name or vice versa
      if (!matchedDistrict) {
        // Try fuzzy matching: check if any district name contains this string or vice versa
        for (const dist of districts) {
          if (dist.toLowerCase().trim() === districtLower || 
              dist.toLowerCase().trim().includes(districtLower) ||
              districtLower.includes(dist.toLowerCase().trim())) {
            matchedDistrict = dist;
            districtLookup[districtLower] = dist; // Cache for future lookups
            break;
          }
        }
      }
      
      // If still no match, create entry for this district anyway (might be a new district)
      if (!matchedDistrict) {
        matchedDistrict = districtStr;
        map[matchedDistrict] = map[matchedDistrict] || [];
        districtLookup[districtLower] = matchedDistrict;
        Logger.log("Found new district in stations sheet: " + districtStr);
      }
      
      // Add station if it exists and is not empty
      if (stationRaw && stationRaw.toString().trim() !== "") {
        const stationStr = stationRaw.toString().trim();
        // Avoid duplicates (case-insensitive)
        const stationLower = stationStr.toLowerCase();
        const exists = map[matchedDistrict].some(s => s.toLowerCase() === stationLower);
        if (!exists) {
          map[matchedDistrict].push(stationStr);
          processedCount++;
        }
      }
    }
    
    Logger.log("Processed " + processedCount + " stations, skipped " + skippedEmptyCount + " empty rows");
    Logger.log("Districts with stations: " + Object.keys(map).filter(d => map[d].length > 0).length);
    
    // Sort each station list
    Object.keys(map).forEach(d => {
      map[d] = map[d].sort();
    });
    
    return map;
  } catch (err) {
    Logger.log("Error getting stations: " + err.toString());
    // Return empty map with all districts initialized
    const map = {};
    districts.forEach(d => { map[d] = []; });
    return map;
  }
}

// -------------------------------
// BLOODGROUP
// -------------------------------
function getBloodGroups(ss) {
  try {
    const sheet = ss.getSheetByName("bloodgroup");
    if (!sheet) {
      Logger.log("Bloodgroup sheet not found");
      return [];
    }
    const lastRow = sheet.getLastRow();
    if (lastRow <= 1) return []; // Only header or empty
    
    const values = sheet.getRange(2, 1, lastRow - 1, 1).getValues();
    return values
      .map(r => r[0])
      .filter(v => v && v.toString().trim() !== "")
      .map(v => v.toString().trim())
      .sort();
  } catch (err) {
    Logger.log("Error getting blood groups: " + err.toString());
    return [];
  }
}

/**
 * Test function - Run this to verify the script works
 */
function testConstants() {
  const ss = SpreadsheetApp.openById(SHEET_ID);
  const constants = getAllConstants(ss);
  Logger.log("Constants: " + JSON.stringify(constants, null, 2));
}
