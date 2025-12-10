# 📊 Constants Google Sheets Setup Guide

## ✅ Yes, you can add/remove stations and other list items from Google Sheets!

The app already has infrastructure to sync constants from Google Sheets. Here's how to set it up:

---

## 📋 Step 1: Create Google Sheet Structure

Create a Google Sheet with the following structure:

### Sheet 1: "Ranks"
| Rank |
|------|
| APC |
| CPC |
| WPC |
| ... |

### Sheet 2: "Districts"
| District |
|----------|
| Bagalkot |
| Ballari |
| Belagavi City |
| ... |

### Sheet 3: "Stations"
| District | Station |
|----------|---------|
| Bagalkot | Amengad PS |
| Bagalkot | Badami PS |
| Bagalkot | Bagalkot CEN Crime PS |
| Ballari | APMC Yard PS |
| ... | ... |

**Note:** Each district can have multiple stations. The script will group them automatically.

### Sheet 4: "BloodGroups"
| BloodGroup |
|------------|
| A+ |
| A- |
| B+ |
| ... |

---

## 🔧 Step 2: Deploy Google Apps Script

1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. Delete any existing code
4. Copy and paste the code from `CONSTANTS_GOOGLE_SHEETS_SCRIPT.gs`
5. Update the `SHEET_ID` constant with your Google Sheet ID (found in the URL)
6. Click **Save** (💾)
7. Click **Deploy → New deployment**
8. Select type: **Web app**
9. Settings:
   - **Execute as:** Me
   - **Who has access:** Anyone
10. Click **Deploy**
11. Copy the **Web App URL**

---

## 📱 Step 3: Update Android App

1. Open `app/src/main/java/com/example/policemobiledirectory/repository/ConstantsRepository.kt`
2. Find this line (around line 24):
   ```kotlin
   .baseUrl("https://script.google.com/macros/s/AKfycbzIW69Yz1BzjVbKD83SpOHqy7KecIG9WQP2DqLrsYJOfPWcVCDEIpNQoia997fV_Jzeng/")
   ```
3. Replace the URL with your new Web App URL (keep the trailing `/`)
4. Save the file

---

## 🔄 Step 4: How It Works

### Current Implementation:
- The app uses `ConstantsRepository` which:
  1. **First tries** to fetch from Google Sheets (cached locally)
  2. **Falls back** to hardcoded `Constants` if fetch fails or is empty
  3. **Caches** the fetched data locally for offline use

### To Refresh Constants:
The app automatically refreshes constants when:
- User logs in
- App starts (if cached data is old)

You can also manually trigger refresh by calling:
```kotlin
constantsRepository.refreshConstants()
```

---

## 📝 Usage in Code

### Current Usage (Hardcoded):
```kotlin
Constants.districtsList
Constants.allRanksList
Constants.stationsByDistrictMap
Constants.bloodGroupsList
```

### Recommended Usage (Dynamic from Google Sheets):
```kotlin
constantsRepository.getDistricts()
constantsRepository.getRanks()
constantsRepository.getStationsByDistrict()
constantsRepository.getBloodGroups()
```

**Note:** The repository methods automatically fall back to hardcoded Constants if Google Sheets data is unavailable.

---

## ✅ Benefits

1. **No app update needed** - Just update the Google Sheet
2. **Easy management** - Add/remove items directly in Google Sheets
3. **Offline support** - Data is cached locally
4. **Automatic fallback** - Uses hardcoded values if sheet is unavailable
5. **Real-time updates** - Changes reflect after app refresh

---

## 🧪 Testing

1. After deploying the script, test it by running the `testConstants()` function in Apps Script
2. Or visit the Web App URL directly in a browser to see the JSON response
3. In the app, check logs for "✅ Constants refreshed from Google Sheet."

---

## 📊 Example JSON Response

The script returns data in this format:
```json
{
  "ranks": ["APC", "CPC", "WPC", ...],
  "districts": ["Bagalkot", "Ballari", ...],
  "stationsByDistrict": {
    "Bagalkot": ["Amengad PS", "Badami PS", ...],
    "Ballari": ["APMC Yard PS", ...]
  },
  "bloodGroups": ["A+", "A-", "B+", ...],
  "lastUpdated": "2024-01-15T10:30:00.000Z"
}
```

---

## ⚠️ Important Notes

1. **Sheet Names Must Match Exactly:**
   - "Ranks" (case-sensitive)
   - "Districts" (case-sensitive)
   - "Stations" (case-sensitive)
   - "BloodGroups" (case-sensitive)

2. **Stations Sheet Format:**
   - Column A = District name (must match exactly with Districts sheet)
   - Column B = Station name
   - Multiple rows per district are allowed

3. **Empty Values:**
   - Empty cells are automatically skipped
   - Headers are automatically detected and skipped

4. **Sorting:**
   - All lists are automatically sorted alphabetically

---

## 🔧 Troubleshooting

### Issue: Constants not updating
- Check if the Web App URL is correct
- Verify the script is deployed as "Anyone" can access
- Check Apps Script execution logs for errors

### Issue: Empty data returned
- Verify sheet names match exactly (case-sensitive)
- Check if data exists in the sheets
- Run `testConstants()` function in Apps Script to debug

### Issue: App still using hardcoded values
- Ensure `constantsRepository.refreshConstants()` is called
- Check if cached data exists in SharedPreferences
- Verify network connectivity

---

## 📞 Support

If you encounter issues:
1. Check Apps Script execution logs
2. Verify Google Sheet structure matches the guide
3. Test the Web App URL directly in a browser
4. Check Android logcat for error messages







