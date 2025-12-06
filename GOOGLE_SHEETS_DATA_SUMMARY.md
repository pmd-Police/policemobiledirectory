# Google Sheets Data Files Summary

## Generated Files

### ✅ Districts CSV
- **File:** `GOOGLE_SHEETS_DISTRICTS_CSV.csv`
- **Content:** 37 districts (one per row)
- **Format:** Header row "District", followed by district names
- **Status:** ✅ Generated successfully

### ⚠️ Stations CSV  
- **File:** `GOOGLE_SHEETS_STATIONS_CSV.csv`
- **Content:** All stations with their corresponding districts
- **Format:** Header row "District,Station", followed by district-station pairs
- **Status:** Run `python generate_google_sheets_data.py` to generate

## How to Generate Stations CSV

If the stations CSV was not generated automatically, run:

```bash
python generate_google_sheets_data.py
```

This will create `GOOGLE_SHEETS_STATIONS_CSV.csv` with all stations.

## Manual Alternative

If the Python script doesn't work, you can manually create the stations CSV:

1. Create a file named `GOOGLE_SHEETS_STATIONS_CSV.csv`
2. First line: `District,Station`
3. For each district in the districts list, add rows with:
   - Column A: District name
   - Column B: Station name
4. Include all stations from `Constants.kt` for each district
5. Also include the district name itself as a station (as per Constants.kt logic)

## Expected Station Counts by District

Based on Constants.kt, approximate station counts:

- **Bagalkot**: ~35 stations
- **Ballari**: ~22 stations  
- **Belagavi City**: ~17 stations
- **Belagavi Dist**: ~35 stations
- **Bengaluru City**: ~170+ stations (largest)
- **Bengaluru Dist**: ~29 stations
- **Bidar**: ~29 stations
- **Chamarajanagar**: ~19 stations
- **Chikkaballapura**: ~21 stations
- **Chikkamagaluru**: ~27 stations
- **Chitradurga**: ~25 stations
- **Dakshina Kannada**: ~19 stations
- **Davanagere**: ~24 stations
- **Dharwad**: ~13 stations
- **Gadag**: ~16 stations
- **Hassan**: ~29 stations
- **Haveri**: ~24 stations
- **Hubballi Dharwad City**: ~23 stations
- **K.G.F**: ~13 stations
- **Kalaburagi**: ~24 stations
- **Kalaburagi City**: ~16 stations
- **Kodagu**: ~19 stations
- **Kolar**: ~16 stations
- **Koppal**: ~19 stations
- **Mandya**: ~36 stations
- **Mangaluru City**: ~22 stations
- **Mysuru City**: ~27 stations
- **Mysuru Dist**: ~26 stations
- **Raichur**: ~27 stations
- **Ramanagara**: ~24 stations
- **Shivamogga**: ~28 stations
- **Tumakuru**: ~39 stations
- **Udupi**: ~23 stations
- **Uttara Kannada**: ~31 stations
- **Vijayanagara**: ~23 stations
- **Vijayapur**: ~27 stations
- **Yadgir**: ~16 stations

**Total:** Approximately 1,000+ station entries across 37 districts

Each district also includes common units like:
- Control Room [District]
- DPO [District]
- Computer Sec [District]
- DAR [District]
- FPB [District]
- MCU [District]
- DCRB [District]
- DSB [District]
- SMMC [District]
- State INT [District]
- DCRE [District]
- Lokayukta [District]
- ESCOM [District]

Plus the district name itself is included as a station option.

## Quick Import Instructions

1. **Import Districts:**
   - Open Google Sheets
   - Create/select sheet named "district" (lowercase)
   - File → Import → Upload `GOOGLE_SHEETS_DISTRICTS_CSV.csv`
   - Replace spreadsheet

2. **Import Stations:**
   - Create/select sheet named "station" (lowercase)  
   - File → Import → Upload `GOOGLE_SHEETS_STATIONS_CSV.csv`
   - Replace spreadsheet
   - Ensure Column A = District, Column B = Station

3. **Verify:**
   - Check that all districts appear in the district sheet
   - Check that stations are properly grouped by district
   - Test the Apps Script API endpoint

4. **Update App:**
   - Use "Refresh Constants (Clear Cache)" in Admin Panel
   - Verify all stations appear in dropdowns
