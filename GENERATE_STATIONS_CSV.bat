@echo off
echo Generating Stations CSV file...
echo.

cd /d "%~dp0"
python create_stations_csv_simple.py

echo.
echo Done! Check GOOGLE_SHEETS_STATIONS_CSV.csv
pause
