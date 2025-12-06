# ✅ Import Stations CSV to Google Sheets - Step by Step

Your `GOOGLE_SHEETS_STATIONS_CSV.csv` file is now complete with **1,549 stations** across **37 districts**!

## Step-by-Step Import Instructions

### 1. Open Your Google Sheet
1. Go to [Google Sheets](https://sheets.google.com)
2. Open your constants spreadsheet
3. Verify the Sheet ID matches: `1gmUXQn1Fp2JEmNWzicNNDJurUYeS-D5XMyc_pym0avI`

### 2. Import Districts (if not already done)
1. Create/select sheet named **"district"** (lowercase, exactly)
2. **File → Import**
3. Upload `GOOGLE_SHEETS_DISTRICTS_CSV.csv`
4. Settings: Replace spreadsheet, Comma separator
5. Click **Import data**

**Expected:** 37 districts (1 header + 36 districts)

### 3. Import Stations ✅ (NEW - Complete File)
1. Create/select sheet named **"station"** (lowercase, exactly)
2. **File → Import**
3. Upload `GOOGLE_SHEETS_STATIONS_CSV.csv`
4. Settings: 
   - **Import location**: Replace spreadsheet
   - **Separator type**: Comma
5. Click **Import data**

**Expected:** 1,550 rows (1 header + 1,549 station entries)

### 4. Verify the Import
After importing, check:

**District Sheet:**
- Row 1: Header "District"
- Rows 2-38: 37 district names
- Total: 38 rows

**Station Sheet:**
- Row 1: Headers "District,Station"
- Row 2: First station (should be "Bagalkot,Amengad PS")
- Last row: Should be "Yadgir,Yadgiri Women PS" or similar
- Column A: District names
- Column B: Station names
- Total: 1,550 rows

### 5. Update Google Apps Script
1. Open [Google Apps Script](https://script.google.com)
2. Open your project with the constants script
3. Copy the updated `CONSTANTS_GOOGLE_SHEETS_SCRIPT.gs` code
4. **Deploy → Manage deployments**
5. Click **Edit** on your existing deployment
6. Update to **New version** and **Deploy**

### 6. Test the API
1. In Apps Script, run the `testConstants()` function
2. Check the Execution log to verify:
   - All districts are loaded
   - All stations are being read correctly
   - Station counts per district

### 7. Refresh in Android App
1. Open your Android app
2. Go to **Admin Panel**
3. Tap **"Refresh Constants (Clear Cache)"**
4. Wait for success message
5. Check the dropdown - all stations should now appear!

## Verification Checklist

- [ ] Districts CSV imported (37 districts)
- [ ] Stations CSV imported (1,549 stations)
- [ ] Google Apps Script updated with new code
- [ ] Script deployed as Web App
- [ ] App cache cleared and refreshed
- [ ] Stations dropdown shows all stations for each district

## Troubleshooting

### Stations still incomplete in dropdown?
1. Check Apps Script logs - are all stations being read?
2. Verify district names match exactly between sheets
3. Clear app cache again: Admin Panel → "Refresh Constants (Clear Cache)"
4. Check that the Apps Script Web App URL is correct in `NetworkModule.kt`

### Import errors?
- Make sure sheet names are exactly: `district` and `station` (lowercase)
- Check that CSV files have correct headers
- Verify no special characters are causing issues

## Current File Status

✅ **GOOGLE_SHEETS_DISTRICTS_CSV.csv** - 37 districts (ready)
✅ **GOOGLE_SHEETS_STATIONS_CSV.csv** - 1,549 stations across 37 districts (ready)

Both files are ready to import to Google Sheets!
