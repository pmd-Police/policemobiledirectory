# Fix: Generate Stations CSV

The `GOOGLE_SHEETS_STATIONS_CSV.csv` file is not being generated automatically. 

## Quick Fix

Run this command in your terminal/command prompt:

```bash
cd "c:\Users\ravip\AndroidStudioProjects\PoliceMobileDirectory"
python generate_google_sheets_data.py
```

Then check if the file was created:
```bash
dir GOOGLE_SHEETS_STATIONS_CSV.csv
```

## If Python Script Doesn't Work

If the Python script still doesn't create the file, you can manually create it:

1. Open `generate_google_sheets_data.py` in a text editor
2. The script contains all the station data in the `stations_by_district` dictionary
3. Run it manually and check for any error messages
4. Alternatively, the data can be extracted from `Constants.kt` file

## Expected Output

The `GOOGLE_SHEETS_STATIONS_CSV.csv` file should contain:
- Header row: `District,Station`
- Approximately 1000+ rows with district-station pairs
- One row per station, with the district name in Column A and station name in Column B

## Verification

After generating the file:
1. Check the file size (should be > 50KB)
2. Open in a text editor and verify it starts with `District,Station`
3. Count the lines: `find /c /v "" GOOGLE_SHEETS_STATIONS_CSV.csv` (Windows)

## Alternative: Import Directly to Google Sheets

If you cannot generate the CSV, you can:
1. Manually copy data from `Constants.kt`
2. Create the sheet structure in Google Sheets directly
3. Use the format: Column A = District, Column B = Station
