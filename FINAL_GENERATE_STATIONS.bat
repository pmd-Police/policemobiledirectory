@echo off
echo ================================================
echo   Generate Complete Stations CSV
echo ================================================
echo.
echo IMPORTANT: Close GOOGLE_SHEETS_STATIONS_CSV.csv
echo            in your editor before running this!
echo.
pause
echo.

cd /d "%~dp0"
echo Running Python script...
python generate_complete_csv.py

echo.
echo Checking result...
if exist GOOGLE_SHEETS_STATIONS_CSV.csv (
    for %%A in (GOOGLE_SHEETS_STATIONS_CSV.csv) do set size=%%~zA
    echo File size: %size% bytes
    powershell -Command "(Get-Content GOOGLE_SHEETS_STATIONS_CSV.csv | Measure-Object -Line).Lines" 
    echo.
    echo If file size is less than 50000 bytes, the script may have failed.
    echo Check the output above for any error messages.
) else (
    echo ERROR: File was not created!
)

echo.
pause
