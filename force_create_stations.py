#!/usr/bin/env python3
"""Force create stations CSV with explicit error handling"""

import sys
import traceback

try:
    # Read and execute the main script
    with open('generate_google_sheets_data.py', 'r', encoding='utf-8') as f:
        code = f.read()
    
    # Execute in a namespace
    namespace = {}
    exec(code, namespace)
    
    # Now create stations CSV explicitly
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
    import os
    base_dir = os.path.dirname(os.path.abspath(__file__))
    filename = os.path.join(base_dir, 'GOOGLE_SHEETS_STATIONS_CSV.csv')
    
    print(f"Writing to: {filename}")
    
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
    
    print(f"✅ Successfully created {filename}")
    print(f"   Total stations: {total_stations}")
    print(f"   Across {len(districts)} districts")
    
    # Verify file was created
    import os
    if os.path.exists(filename):
        size = os.path.getsize(filename)
        print(f"   File size: {size} bytes")
    else:
        print("   ERROR: File was not created!")
        
except Exception as e:
    print(f"ERROR: {e}")
    traceback.print_exc()
    sys.exit(1)
