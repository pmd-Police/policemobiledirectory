#!/usr/bin/env python3
"""Create stations CSV file"""

# Execute the main script to get data
exec(open('generate_google_sheets_data.py', encoding='utf-8').read())

# The stations CSV should already be created by the script above
# But let's verify and create it explicitly if needed
import os
if not os.path.exists('GOOGLE_SHEETS_STATIONS_CSV.csv'):
    print("WARNING: Stations CSV not found, creating it now...")
    # Re-run the stations generation part
    total_stations = 0
    with open('GOOGLE_SHEETS_STATIONS_CSV.csv', 'w', encoding='utf-8') as f:
        f.write('District,Station\n')
        for district in sorted(districts):
            if district in stations_by_district:
                stations = sorted(list(set(stations_by_district[district])))
                for station in stations:
                    station_escaped = station.replace(',', ';') if ',' in station else station
                    f.write(f'{district},{station_escaped}\n')
                    total_stations += 1
    print(f"✅ Created GOOGLE_SHEETS_STATIONS_CSV.csv with {total_stations} entries")
else:
    print("✅ Stations CSV already exists")
