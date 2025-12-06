#!/usr/bin/env python3
"""Generate complete stations CSV - standalone version"""

import os
import sys

# Set working directory
os.chdir(os.path.dirname(os.path.abspath(__file__)))

print("=" * 60)
print("Generating Complete Stations CSV File")
print("=" * 60)
print()

# Import the data directly (we'll execute the file)
print("Step 1: Loading station data...")
with open('generate_google_sheets_data.py', 'r', encoding='utf-8') as f:
    source_code = f.read()

# Create a clean namespace
globs = {'__builtins__': __builtins__}
exec(source_code, globs)

districts = globs.get('districts', [])
stations_by_district = globs.get('stations_by_district', {})

print(f"  ✓ Loaded {len(districts)} districts")
print(f"  ✓ Loaded stations data for {len(stations_by_district)} districts")
print()

# Add district name to each station list
print("Step 2: Processing stations...")
for district in districts:
    if district in stations_by_district:
        if district not in stations_by_district[district]:
            stations_by_district[district].append(district)

# Generate CSV
print("Step 3: Writing CSV file...")
filename = 'GOOGLE_SHEETS_STATIONS_CSV.csv'
total_stations = 0

try:
    # Delete existing file first to ensure clean write
    if os.path.exists(filename):
        os.remove(filename)
        print(f"  ✓ Removed old file")
    
    with open(filename, 'w', encoding='utf-8', newline='') as f:
        f.write('District,Station\n')
        
        for district in sorted(districts):
            if district in stations_by_district:
                stations = sorted(list(set(stations_by_district[district])))
                for station in stations:
                    station_escaped = station.replace(',', ';') if ',' in station else station
                    f.write(f'{district},{station_escaped}\n')
                    total_stations += 1
                    
                    # Progress indicator
                    if total_stations % 50 == 0:
                        print(f"  Written {total_stations} stations...", end='\r')
    
    print(f"\n  ✓ Written {total_stations} stations to file")
    print()
    
    # Verify
    if os.path.exists(filename):
        size = os.path.getsize(filename)
        with open(filename, 'r', encoding='utf-8') as f:
            lines = len(f.readlines())
        
        # Check first few and last few lines
        with open(filename, 'r', encoding='utf-8') as f:
            all_lines = f.readlines()
            first_district = all_lines[1].split(',')[0] if len(all_lines) > 1 else 'N/A'
            last_district = all_lines[-1].split(',')[0] if len(all_lines) > 1 else 'N/A'
        
        print("=" * 60)
        print("✓ SUCCESS! File generated successfully")
        print("=" * 60)
        print(f"  File: {filename}")
        print(f"  Total stations: {total_stations:,}")
        print(f"  Districts: {len(districts)}")
        print(f"  File size: {size:,} bytes")
        print(f"  Total lines: {lines:,}")
        print(f"  First district: {first_district}")
        print(f"  Last district: {last_district}")
        print()
        print("✅ File is ready to import to Google Sheets!")
        print("=" * 60)
    else:
        print("\n✗ ERROR: File was not created!")
        sys.exit(1)
        
except Exception as e:
    print(f"\n✗ ERROR: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
