@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ========================================
echo  Generate Stations CSV File
echo ========================================
echo.
python test_and_create.py
echo.
echo ========================================
pause
