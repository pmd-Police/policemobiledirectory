# Google Sheets Import Guide for Districts and Stations

This guide explains how to import the generated CSV files into your Google Sheets for syncing with the Android app.

## Generated Files

1. **GOOGLE_SHEETS_DISTRICTS_CSV.csv** - Contains all 37 districts
2. **GOOGLE_SHEETS_STATIONS_CSV.csv** - Contains all stations organized by district

## Steps to Import into Google Sheets

### 1. Create/Open Your Google Sheet

1. Go to [Google Sheets](https://sheets.google.com)
2. Create a new spreadsheet or open your existing constants spreadsheet
3. Make sure the Sheet ID matches the one in `CONSTANTS_GOOGLE_SHEETS_SCRIPT.gs`:
   ```
   const SHEET_ID = "1gmUXQn1Fp2JEmNWzicNNDJurUYeS-D5XMyc_pym0avI";
   ```

### 2. Import Districts

1. In your Google Sheet, create or select a sheet named **"district"** (lowercase, exactly)
2. Delete all existing data in that sheet (if any)
3. Go to **File → Import**
4. Select **Upload** tab
5. Choose `GOOGLE_SHEETS_DISTRICTS_CSV.csv`
6. Import settings:
   - **Import location**: Replace spreadsheet
   - **Separator type**: Comma
   - **Convert text to numbers, dates, and formulas**: Yes (optional)
7. Click **Import data**

**Expected Result:**
- Row 1: Header "District"
- Rows 2-38: 37 district names (one per row)

### 3. Import Stations

1. In your Google Sheet, create or select a sheet named **"station"** (lowercase, exactly)
2. Delete all existing data in that sheet (if any)
3. Go to **File → Import**
4. Select **Upload** tab
5. Choose `GOOGLE_SHEETS_STATIONS_CSV.csv`
6. Import settings:
   - **Import location**: Replace spreadsheet
   - **Separator type**: Comma
   - **Convert text to numbers, dates, and formulas**: Yes (optional)
7. Click **Import data**

**Expected Result:**
- Row 1: Headers "District" and "Station"
- Rows 2+: All stations with their corresponding district in Column A

### 4. Verify Data

After importing, verify the data:

1. **District Sheet:**
   - Should have exactly 37 rows (1 header + 36 districts)
   - All districts should be listed

2. **Station Sheet:**
   - Should have headers: "District" (Column A), "Station" (Column B)
   - Each row should have a district in Column A and station name in Column B
   - Stations are grouped by district
   - Each district name also appears as a station (as per Constants.kt logic)

### 5. Test the API

1. Deploy/update your Google Apps Script (`CONSTANTS_GOOGLE_SHEETS_SCRIPT.gs`)
2. Run the `testConstants()` function in Apps Script to verify data
3. Check the logs to ensure all stations are being read correctly

### 6. Refresh in Android App

1. Open the Android app
2. Go to **Admin Panel**
3. Tap **"Refresh Constants (Clear Cache)"**
4. Verify that all stations appear in the dropdown lists

## Sheet Structure Requirements

Your Google Sheet must have these exact sheet names (case-sensitive):
- `rank` - Ranks list
- `district` - Districts list (import from GOOGLE_SHEETS_DISTRICTS_CSV.csv)
- `station` - Stations by district (import from GOOGLE_SHEETS_STATIONS_CSV.csv)
- `bloodgroup` - Blood groups list

## Troubleshooting

### Stations not showing in dropdown

1. Check that the sheet name is exactly `"station"` (lowercase)
2. Verify column headers: Column A = "District", Column B = "Station"
3. Ensure there are no empty rows in the middle of data
4. Check Apps Script logs for any errors
5. Clear app cache and refresh again

### District name mismatch

The script now handles case-insensitive matching, but ensure:
- District names in the `station` sheet match exactly with names in the `district` sheet
- No extra spaces before or after district names

### Missing stations

If some stations are missing:
1. Check that the station CSV file was imported completely
2. Verify that the Apps Script is reading all rows (check logs)
3. The updated script uses `getDataRange()` which should read all rows even if there are gaps

## Updating Stations

To add new stations:
1. Edit the Google Sheet directly (add new rows to the `station` sheet)
2. Format: Column A = District name, Column B = Station name
3. Refresh in the app using "Refresh Constants (Clear Cache)"

To regenerate the CSV files:
```bash
python generate_google_sheets_data.py
```

Then re-import the updated CSV files.
