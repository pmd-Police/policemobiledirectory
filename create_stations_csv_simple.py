#!/usr/bin/env python3
"""Simple script to create complete stations CSV"""

import os
import sys

# Change to script directory
script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)

print(f"Working directory: {os.getcwd()}")
print(f"Looking for generate_google_sheets_data.py...")

if not os.path.exists('generate_google_sheets_data.py'):
    print("ERROR: generate_google_sheets_data.py not found!")
    sys.exit(1)

# Read and execute the main script
print("Reading generate_google_sheets_data.py...")
with open('generate_google_sheets_data.py', 'r', encoding='utf-8') as f:
    code = f.read()

# Execute in a namespace
print("Executing script...")
namespace = {}
exec(code, namespace)

# Get data
districts = namespace.get('districts', [])
stations_by_district = namespace.get('stations_by_district', {})

print(f"Found {len(districts)} districts")
print(f"Found {len(stations_by_district)} district entries in stations dict")

# Add district name to each station list
for district in districts:
    if district in stations_by_district:
        if district not in stations_by_district[district]:
            stations_by_district[district].append(district)

# Generate Stations CSV
total_stations = 0
filename = os.path.join(script_dir, 'GOOGLE_SHEETS_STATIONS_CSV.csv')

print(f"\nGenerating {filename}...")

with open(filename, 'w', encoding='utf-8', newline='') as f:
    f.write('District,Station\n')
    for district in sorted(districts):
        if district in stations_by_district:
            # Remove duplicates and sort
            stations = sorted(list(set(stations_by_district[district])))
            for station in stations:
                # Escape commas in station names by replacing with semicolon
                station_escaped = station.replace(',', ';') if ',' in station else station
                f.write(f'{district},{station_escaped}\n')
                total_stations += 1

print(f"\n✅ Successfully created {filename}")
print(f"   Total stations: {total_stations}")
print(f"   Across {len(districts)} districts")

# Verify file was created
if os.path.exists(filename):
    size = os.path.getsize(filename)
    lines = sum(1 for _ in open(filename, 'r', encoding='utf-8'))
    print(f"   File size: {size:,} bytes")
    print(f"   Total lines: {lines:,}")
    print(f"\n✅ DONE! File is ready to import to Google Sheets.")
else:
    print("   ❌ ERROR: File was not created!")
