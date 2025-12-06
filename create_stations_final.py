#!/usr/bin/env python3
"""Create complete stations CSV - writes to temp file first to avoid lock issues"""

import os
import sys
import shutil

script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)

print("=" * 70)
print("Creating Complete Stations CSV File")
print("=" * 70)
print()

# Load data
print("Loading station data...")
with open('generate_google_sheets_data.py', 'r', encoding='utf-8') as f:
    source_code = f.read()

globs = {'__builtins__': __builtins__}
exec(source_code, globs)

districts = globs.get('districts', [])
stations_by_district = globs.get('stations_by_district', {})

print(f"  ✓ Loaded {len(districts)} districts")
print(f"  ✓ Loaded stations for {len(stations_by_district)} districts")
print()

# Add district names
for district in districts:
    if district in stations_by_district:
        if district not in stations_by_district[district]:
            stations_by_district[district].append(district)

# Write to temporary file first
temp_filename = 'GOOGLE_SHEETS_STATIONS_CSV_TEMP.csv'
final_filename = 'GOOGLE_SHEETS_STATIONS_CSV.csv'

print(f"Writing to temporary file: {temp_filename}...")
total_stations = 0

try:
    with open(temp_filename, 'w', encoding='utf-8', newline='') as f:
        f.write('District,Station\n')
        
        for district in sorted(districts):
            if district in stations_by_district:
                stations = sorted(list(set(stations_by_district[district])))
                for station in stations:
                    station_escaped = station.replace(',', ';') if ',' in station else station
                    f.write(f'{district},{station_escaped}\n')
                    total_stations += 1
                    if total_stations % 100 == 0:
                        print(f"  Written {total_stations} stations...", end='\r')
    
    print(f"\n  ✓ Written {total_stations} stations")
    print()
    
    # Verify temp file
    if os.path.exists(temp_filename):
        size = os.path.getsize(temp_filename)
        with open(temp_filename, 'r', encoding='utf-8') as f:
            lines = len(f.readlines())
            f.seek(0)
            all_lines = f.readlines()
            first_dist = all_lines[1].split(',')[0] if len(all_lines) > 1 else 'N/A'
            last_dist = all_lines[-1].split(',')[0] if len(all_lines) > 1 else 'N/A'
        
        print("=" * 70)
        print("✓ Temporary file created successfully!")
        print("=" * 70)
        print(f"  File: {temp_filename}")
        print(f"  Total stations: {total_stations:,}")
        print(f"  Districts: {len(districts)}")
        print(f"  File size: {size:,} bytes")
        print(f"  Total lines: {lines:,}")
        print(f"  First district: {first_dist}")
        print(f"  Last district: {last_dist}")
        print()
        
        # Try to replace the final file
        try:
            if os.path.exists(final_filename):
                os.remove(final_filename)
            shutil.move(temp_filename, final_filename)
            print(f"✓ Successfully created {final_filename}")
        except Exception as e:
            print(f"⚠ Warning: Could not replace {final_filename}")
            print(f"  Error: {e}")
            print(f"  The complete file is available as: {temp_filename}")
            print(f"  Please close {final_filename} in your editor and rename:")
            print(f"  {temp_filename} -> {final_filename}")
    else:
        print("\n✗ ERROR: Temporary file was not created!")
        sys.exit(1)
        
except Exception as e:
    print(f"\n✗ ERROR: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

print()
print("=" * 70)
print("✅ COMPLETE!")
print("=" * 70)
