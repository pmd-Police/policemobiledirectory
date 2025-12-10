/**
 * ✅ Constants Generator Script - Auto-generates Constants.kt from Google Sheets
 * 
 * This script:
 * 1. Reads constants from Google Sheets (Ranks, Districts, Stations, BloodGroups)
 * 2. Generates a complete Constants.kt Kotlin file
 * 3. Saves it to Google Drive
 * 4. Provides download endpoints
 * 5. Auto-syncs districts to stations sheet when new district is added
 * 
 * SETUP:
 * 1. Create Google Sheet with sheets: "Ranks", "Districts", "Stations", "BloodGroups"
 * 2. Update SHEET_ID below
 * 3. Deploy as Web App
 * 4. Enable Drive API (will prompt on first run)
 */

// ⚙️ CONFIGURATION
const SHEET_ID = "YOUR_SHEET_ID_HERE"; // Replace with your Google Sheet ID
const DRIVE_FOLDER_ID = ""; // Optional: Folder ID where Constants.kt will be saved (leave empty for root)
const RANKS_SHEET = "Ranks";
const DISTRICTS_SHEET = "Districts";
const STATIONS_SHEET = "Stations";
const BLOOD_GROUPS_SHEET = "BloodGroups";
const OUTPUT_FILE_NAME = "Constants.kt";

/**
 * Main GET handler - Returns Kotlin file or JSON
 */
function doGet(e) {
  const type = e.parameter.type;
  
  if (type === "kotlin") {
    return generateKotlinFile();
  } else if (type === "download") {
    return downloadToDrive();
  } else {
    // Default: return JSON data
    return getConstantsJson();
  }
}

/**
 * Generate Constants.kt file content
 */
function generateKotlinFile() {
  try {
    const sheet = SpreadsheetApp.openById(SHEET_ID);
    
    const ranks = getRanks(sheet);
    const districts = getDistricts(sheet);
    const stationsByDistrict = getStationsByDistrict(sheet);
    const bloodGroups = getBloodGroups(sheet);
    const ranksRequiringMetalNumber = getRanksRequiringMetalNumber(sheet, ranks);
    
    const kotlinCode = buildKotlinFile(ranks, districts, stationsByDistrict, bloodGroups, ranksRequiringMetalNumber);
    
    return ContentService
      .createTextOutput(kotlinCode)
      .setMimeType(ContentService.MimeType.TEXT);
      
  } catch (error) {
    return ContentService
      .createTextOutput("// Error: " + error.toString())
      .setMimeType(ContentService.MimeType.TEXT);
  }
}

/**
 * Download generated file to Drive and return JSON with URL
 */
function downloadToDrive() {
  try {
    const kotlinContent = generateKotlinFile().getContent();
    const folder = DRIVE_FOLDER_ID ? DriveApp.getFolderById(DRIVE_FOLDER_ID) : DriveApp.getRootFolder();
    
    // Delete existing file if it exists
    const existingFiles = folder.getFilesByName(OUTPUT_FILE_NAME);
    while (existingFiles.hasNext()) {
      existingFiles.next().setTrashed(true);
    }
    
    // Create new file
    const file = folder.createFile(OUTPUT_FILE_NAME, kotlinContent, MimeType.PLAIN_TEXT);
    
    return ContentService
      .createTextOutput(JSON.stringify({
        success: true,
        fileId: file.getId(),
        fileUrl: file.getUrl(),
        webViewLink: file.getUrl(),
        message: "Constants.kt generated and saved to Drive"
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    return ContentService
      .createTextOutput(JSON.stringify({
        success: false,
        error: error.toString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Save Constants.kt to Drive (can be called from Script Editor)
 */
function saveConstantsToDrive() {
  const result = JSON.parse(downloadToDrive().getContent());
  Logger.log("✅ Constants.kt saved to Drive: " + result.fileUrl);
  return result;
}

/**
 * Get constants as JSON (for runtime fetching)
 */
function getConstantsJson() {
  try {
    const sheet = SpreadsheetApp.openById(SHEET_ID);
    
    return ContentService
      .createTextOutput(JSON.stringify({
        ranks: getRanks(sheet),
        districts: getDistricts(sheet),
        stationsByDistrict: getStationsByDistrict(sheet),
        bloodGroups: getBloodGroups(sheet),
        lastUpdated: new Date().toISOString()
      }))
      .setMimeType(ContentService.MimeType.JSON);
      
  } catch (error) {
    return ContentService
      .createTextOutput(JSON.stringify({ error: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Build complete Kotlin file content
 */
function buildKotlinFile(ranks, districts, stationsByDistrict, bloodGroups, ranksRequiringMetalNumber) {
  let code = "package com.example.policemobiledirectory.utils\n\n";
  code += "object Constants {\n\n";
  
  // Ranks
  code += "    // Updated list of all ranks in the desired order for dropdowns\n";
  code += "    val allRanksList = listOf(\n";
  code += "        \"" + ranks.join("\", \"") + "\"\n";
  code += "    ).sorted()\n\n";
  
  // Ranks requiring metal number
  code += "    // Set of ranks that require a metal number\n";
  code += "    val ranksRequiringMetalNumber = setOf(\n";
  code += "        \"" + ranksRequiringMetalNumber.join("\", \"") + "\"\n";
  code += "    ).sorted()\n\n";
  
  // Blood Groups
  code += "    val bloodGroupsList = listOf(\n";
  code += "        \"" + bloodGroups.join("\", \"") + "\"\n";
  code += "    ).sorted()\n\n";
  
  // Districts
  code += "    val districtsList = listOf(\n";
  code += "        \"" + districts.join("\", \"") + "\"\n";
  code += "    ).sorted()\n\n";
  
  // Stations by District Map
  code += "    // This map contains station lists for ALL districts\n";
  code += "    // All stations (including common units) are hardcoded in each district's list\n";
  code += "    val stationsByDistrictMap: Map<String, List<String>> = districtsList.associateWith { districtName ->\n";
  code += "        val specificStations = when (districtName) {\n";
  
  // Generate when cases for each district
  districts.forEach(function(district) {
    const stations = stationsByDistrict[district] || [];
    if (stations.length > 0) {
      code += "            \"" + district + "\" -> listOf(\n";
      stations.forEach(function(station, index) {
        code += "                \"" + station + "\"";
        if (index < stations.length - 1) code += ",";
        code += "\n";
      });
      code += "            )\n";
    }
  });
  
  code += "            else -> emptyList()\n";
  code += "        }\n\n";
  code += "        // Add district name itself and sort\n";
  code += "        (specificStations + districtName).distinct().sorted()\n";
  code += "    }\n";
  code += "}\n";
  
  return code;
}

/**
 * Fetch ranks from the Ranks sheet
 */
function getRanks(sheet) {
  try {
    const ranksSheet = sheet.getSheetByName(RANKS_SHEET);
    if (!ranksSheet) return [];
    
    const data = ranksSheet.getDataRange().getValues();
    const ranks = [];
    const startRow = data.length > 0 && data[0][0].toString().toLowerCase() === "rank" ? 1 : 0;
    
    for (let i = startRow; i < data.length; i++) {
      const rank = String(data[i][0] || "").trim();
      if (rank && rank.length > 0) {
        ranks.push(rank);
      }
    }
    
    return ranks.sort();
  } catch (error) {
    Logger.log("Error getting ranks: " + error.toString());
    return [];
  }
}

/**
 * Fetch districts from the Districts sheet
 */
function getDistricts(sheet) {
  try {
    const districtsSheet = sheet.getSheetByName(DISTRICTS_SHEET);
    if (!districtsSheet) return [];
    
    const data = districtsSheet.getDataRange().getValues();
    const districts = [];
    const startRow = data.length > 0 && data[0][0].toString().toLowerCase() === "district" ? 1 : 0;
    
    for (let i = startRow; i < data.length; i++) {
      const district = String(data[i][0] || "").trim();
      if (district && district.length > 0) {
        districts.push(district);
      }
    }
    
    return districts.sort();
  } catch (error) {
    Logger.log("Error getting districts: " + error.toString());
    return [];
  }
}

/**
 * Fetch stations grouped by district from the Stations sheet
 */
function getStationsByDistrict(sheet) {
  try {
    const stationsSheet = sheet.getSheetByName(STATIONS_SHEET);
    if (!stationsSheet) return {};
    
    const data = stationsSheet.getDataRange().getValues();
    const stationsMap = {};
    const startRow = data.length > 0 && 
                     (data[0][0].toString().toLowerCase() === "district" || 
                      data[0][1].toString().toLowerCase() === "station") ? 1 : 0;
    
    for (let i = startRow; i < data.length; i++) {
      const district = String(data[i][0] || "").trim();
      const station = String(data[i][1] || "").trim();
      
      if (district && station && district.length > 0 && station.length > 0) {
        if (!stationsMap[district]) {
          stationsMap[district] = [];
        }
        if (stationsMap[district].indexOf(station) === -1) {
          stationsMap[district].push(station);
        }
      }
    }
    
    // Sort stations within each district
    for (const district in stationsMap) {
      stationsMap[district].sort();
    }
    
    return stationsMap;
  } catch (error) {
    Logger.log("Error getting stations: " + error.toString());
    return {};
  }
}

/**
 * Fetch blood groups from the BloodGroups sheet
 */
function getBloodGroups(sheet) {
  try {
    const bloodGroupsSheet = sheet.getSheetByName(BLOOD_GROUPS_SHEET);
    if (!bloodGroupsSheet) return [];
    
    const data = bloodGroupsSheet.getDataRange().getValues();
    const bloodGroups = [];
    const startRow = data.length > 0 && data[0][0].toString().toLowerCase() === "bloodgroup" ? 1 : 0;
    
    for (let i = startRow; i < data.length; i++) {
      const bloodGroup = String(data[i][0] || "").trim();
      if (bloodGroup && bloodGroup.length > 0) {
        bloodGroups.push(bloodGroup);
      }
    }
    
    return bloodGroups.sort();
  } catch (error) {
    Logger.log("Error getting blood groups: " + error.toString());
    return [];
  }
}

/**
 * Get ranks that require metal number (from optional column B in Ranks sheet)
 */
function getRanksRequiringMetalNumber(sheet, allRanks) {
  try {
    const ranksSheet = sheet.getSheetByName(RANKS_SHEET);
    if (!ranksSheet) {
      // Default: return lower ranks if no sheet
      return ["APC", "CPC", "WPC", "PCW", "PC", "AHC", "CHC", "WHC", "HCW", "HC"];
    }
    
    const data = ranksSheet.getDataRange().getValues();
    const ranksRequiringMetal = [];
    const startRow = data.length > 0 && data[0][0].toString().toLowerCase() === "rank" ? 1 : 0;
    
    for (let i = startRow; i < data.length; i++) {
      const rank = String(data[i][0] || "").trim();
      const requiresMetal = String(data[i][1] || "").trim().toLowerCase();
      
      if (rank && (requiresMetal === "yes" || requiresMetal === "true" || requiresMetal === "1")) {
        ranksRequiringMetal.push(rank);
      }
    }
    
    // If no column B data, use default
    if (ranksRequiringMetal.length === 0) {
      return ["APC", "CPC", "WPC", "PCW", "PC", "AHC", "CHC", "WHC", "HCW", "HC"];
    }
    
    return ranksRequiringMetal.sort();
  } catch (error) {
    Logger.log("Error getting ranks requiring metal number: " + error.toString());
    return ["APC", "CPC", "WPC", "PCW", "PC", "AHC", "CHC", "WHC", "HCW", "HC"];
  }
}

/**
 * onEdit trigger - Auto-adds district row to Stations sheet when new district is added
 */
function onEdit(e) {
  try {
    const sheet = e.source.getActiveSheet();
    const sheetName = sheet.getName();
    
    // Only process if editing Districts sheet
    if (sheetName !== DISTRICTS_SHEET) return;
    
    const range = e.range;
    const row = range.getRow();
    const col = range.getColumn();
    
    // Only process if editing column A (District column)
    if (col !== 1) return;
    
    // Skip header row
    if (row === 1) return;
    
    const districtValue = String(range.getValue() || "").trim();
    
    // If a new district was added (non-empty value)
    if (districtValue && districtValue.length > 0) {
      const stationsSheet = e.source.getSheetByName(STATIONS_SHEET);
      if (!stationsSheet) return;
      
      // Check if district already exists in Stations sheet
      const stationsData = stationsSheet.getDataRange().getValues();
      let districtExists = false;
      
      for (let i = 0; i < stationsData.length; i++) {
        if (String(stationsData[i][0] || "").trim() === districtValue) {
          districtExists = true;
          break;
        }
      }
      
      // If district doesn't exist in Stations sheet, add a placeholder row
      if (!districtExists) {
        const lastRow = stationsSheet.getLastRow();
        stationsSheet.getRange(lastRow + 1, 1).setValue(districtValue);
        stationsSheet.getRange(lastRow + 1, 2).setValue(""); // Empty station placeholder
        Logger.log("✅ Added placeholder row for district: " + districtValue);
      }
    }
  } catch (error) {
    Logger.log("Error in onEdit trigger: " + error.toString());
  }
}

/**
 * Test function - Run this to verify the script works
 */
function testConstants() {
  const sheet = SpreadsheetApp.openById(SHEET_ID);
  
  Logger.log("Ranks: " + JSON.stringify(getRanks(sheet)));
  Logger.log("Districts: " + JSON.stringify(getDistricts(sheet)));
  Logger.log("Stations: " + JSON.stringify(getStationsByDistrict(sheet)));
  Logger.log("Blood Groups: " + JSON.stringify(getBloodGroups(sheet)));
  
  const kotlin = generateKotlinFile().getContent();
  Logger.log("\n=== Generated Kotlin Code (first 500 chars) ===\n" + kotlin.substring(0, 500));
}







