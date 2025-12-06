"""Test and create stations CSV - with verbose output"""

import os
import sys

# Ensure we're in the right directory
script_dir = os.path.dirname(os.path.abspath(__file__))
os.chdir(script_dir)
print(f"Current directory: {os.getcwd()}\n")

# Check if source file exists
if not os.path.exists('generate_google_sheets_data.py'):
    print("ERROR: generate_google_sheets_data.py not found!")
    print(f"Files in directory: {os.listdir('.')}")
    sys.exit(1)

print("✓ Found generate_google_sheets_data.py\n")

# Read and execute
print("Reading and executing generate_google_sheets_data.py...")
try:
    with open('generate_google_sheets_data.py', 'r', encoding='utf-8') as f:
        code = f.read()
    
    namespace = {'__name__': '__main__'}
    exec(code, namespace)
    
    districts = namespace.get('districts', [])
    stations_by_district = namespace.get('stations_by_district', {})
    
    print(f"\n✓ Loaded {len(districts)} districts")
    print(f"✓ Loaded {len(stations_by_district)} district station mappings\n")
    
    # Add district names
    for district in districts:
        if district in stations_by_district and district not in stations_by_district[district]:
            stations_by_district[district].append(district)
    
    # Generate CSV
    filename = 'GOOGLE_SHEETS_STATIONS_CSV.csv'
    total_stations = 0
    
    print(f"Writing to {filename}...")
    with open(filename, 'w', encoding='utf-8', newline='') as f:
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
    
    print(f"\n\n✓ Success! Created {filename}")
    print(f"  Total stations: {total_stations:,}")
    print(f"  Districts: {len(districts)}")
    
    # Verify
    if os.path.exists(filename):
        size = os.path.getsize(filename)
        with open(filename, 'r', encoding='utf-8') as f:
            lines = len(f.readlines())
        print(f"  File size: {size:,} bytes")
        print(f"  Total lines: {lines:,}")
        print(f"\n✓ File ready for Google Sheets import!")
    else:
        print("\n✗ ERROR: File was not created!")
        
except Exception as e:
    print(f"\n✗ ERROR: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
