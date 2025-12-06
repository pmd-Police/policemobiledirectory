#!/usr/bin/env python3
"""Simple script to generate stations CSV"""

# Read the main script and execute only the stations generation part
import sys

# Import the data from the main script
exec(open('generate_google_sheets_data.py').read())

# The stations CSV generation code is already in the main script
# This file just ensures it runs
print("Script executed successfully")
