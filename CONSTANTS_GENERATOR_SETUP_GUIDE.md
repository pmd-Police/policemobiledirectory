# 📊 Constants Generator - Complete Setup Guide

## ✅ Overview

This method **automatically generates the `Constants.kt` file** from Google Sheets, which you then copy into your Android project. This is better than runtime fetching because:
- ✅ No network calls needed in the app
- ✅ Faster app performance
- ✅ Works completely offline
- ✅ Type-safe at compile time
- ✅ Auto-syncs districts to stations sheet

---

## 📋 Step 1: Create Google Sheet Structure

Create a Google Sheet with these **exact sheet names** (case-sensitive):

### Sheet 1: "Ranks"
| Rank | Requires Metal Number |
|------|----------------------|
| APC | Yes |
| CPC | Yes |
| WPC | Yes |
| PC | Yes |
| ASI | |
| PSI | |
| ... | |

**Note:** Column B is optional. Put "Yes" for ranks that require metal numbers.

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

**Note:** Multiple rows per district are allowed. The script groups them automatically.

### Sheet 4: "BloodGroups"
| BloodGroup |
|------------|
| A+ |
| A- |
| B+ |
| AB+ |
| AB- |
| O+ |
| O- |
| ?? |
| ... |

---

## 🔧 Step 2: Set Up Apps Script

### 2.1 Open Apps Script
1. Open your Google Sheet
2. Go to **Extensions → Apps Script**
3. Delete any existing code

### 2.2 Paste the Code
1. Copy the entire code from `CONSTANTS_GENERATOR_SCRIPT.gs`
2. Paste it into the Apps Script editor
3. **Update the `SHEET_ID` constant** (line ~18):
   ```javascript
   const SHEET_ID = "YOUR_SHEET_ID_HERE"; // Replace with your actual Sheet ID
   ```
   - Find Sheet ID in your Google Sheet URL: `https://docs.google.com/spreadsheets/d/[SHEET_ID]/edit`

### 2.3 (Optional) Set Drive Folder
If you want the generated file saved to a specific Drive folder:
```javascript
const DRIVE_FOLDER_ID = "your_folder_id_here";
```
Leave empty (`""`) to save to Drive root.

### 2.4 Save the Script
- Click **Save** (💾) or press `Ctrl+S` / `Cmd+S`
- Name your project (e.g., "Constants Generator")

---

## 🔐 Step 3: Enable Drive API & Authorize

### 3.1 First Run Authorization
1. Click **Run** (▶️) button in Apps Script editor
2. Select `testConstants` function from dropdown
3. Click **Run**
4. You'll be prompted to **Authorize access**
5. Click **Review Permissions**
6. Choose your Google account
7. Click **Advanced** → **Go to [Project Name] (unsafe)**
8. Click **Allow**

**Why:** The script needs Drive API access to save the generated file.

### 3.2 Verify Authorization
- After authorization, you should see execution logs
- If you see errors, check that permissions were granted

---

## 🚀 Step 4: Deploy as Web App

### 4.1 Create Deployment
1. Click **Deploy** → **New deployment**
2. Click the **gear icon** (⚙️) next to "Select type"
3. Choose **Web app**

### 4.2 Configure Settings
- **Description:** (optional) "Constants Generator"
- **Execute as:** **Me** (your account)
- **Who has access:** 
  - **Anyone** (if you want public access)
  - **Anyone with link** (more secure, recommended)

### 4.3 Deploy
1. Click **Deploy**
2. **Copy the Web App URL** (you'll need this!)
   - Format: `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec`

### 4.4 Enable onEdit Trigger (Auto-sync Districts)
1. In Apps Script editor, click **Triggers** (⏰) in left sidebar
2. Click **+ Add Trigger** (bottom right)
3. Configure:
   - **Function:** `onEdit`
   - **Event source:** From spreadsheet
   - **Event type:** On edit
4. Click **Save**
5. Authorize if prompted

**What this does:** When you add a new district in the Districts sheet, it automatically adds a placeholder row in the Stations sheet.

---

## 📥 Step 5: Generate Constants.kt File

### Method 1: Download via Browser (Recommended)

1. Open your Web App URL in browser:
   ```
   https://script.google.com/macros/s/[YOUR_DEPLOYMENT_ID]/exec?type=kotlin
   ```

2. You'll see the complete `Constants.kt` file content

3. **Copy all the text** (Ctrl+A, Ctrl+C)

4. In Android Studio:
   - Open `app/src/main/java/com/example/policemobiledirectory/utils/Constants.kt`
   - Select all (Ctrl+A)
   - Paste (Ctrl+V) to replace
   - Save

### Method 2: Download to Google Drive

1. Open this URL:
   ```
   https://script.google.com/macros/s/[YOUR_DEPLOYMENT_ID]/exec?type=download
   ```

2. You'll get a JSON response:
   ```json
   {
     "success": true,
     "fileId": "...",
     "fileUrl": "https://drive.google.com/file/d/...",
     "webViewLink": "https://drive.google.com/file/d/...",
     "message": "Constants.kt generated and saved to Drive"
   }
   ```

3. Click the `fileUrl` to open the file in Drive

4. Download or copy the content to Android Studio

### Method 3: Run from Script Editor

1. In Apps Script editor, select `saveConstantsToDrive` function
2. Click **Run** (▶️)
3. Check execution logs for the Drive file URL
4. Open the file from Drive and copy to Android Studio

---

## 🔄 Step 6: Workflow for Updates

### When you add/remove items in Google Sheets:

1. **Update Google Sheet:**
   - Add/remove ranks, districts, stations, or blood groups
   - The `onEdit` trigger automatically syncs new districts to Stations sheet

2. **Regenerate Constants.kt:**
   - Visit: `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec?type=kotlin`
   - Copy the generated code

3. **Update Android Project:**
   - Paste into `Constants.kt`
   - Rebuild the app

**That's it!** No need to update app code manually.

---

## 📊 Google Sheet Headers Reference

### Exact Headers (Case-Sensitive):

| Sheet Name | Column A Header | Column B Header (Optional) |
|------------|----------------|----------------------------|
| **Ranks** | `Rank` | `Requires Metal Number` |
| **Districts** | `District` | - |
| **Stations** | `District` | `Station` |
| **BloodGroups** | `BloodGroup` | - |

### Example Sheet Structure:

**Ranks Sheet:**
```
Rank                  | Requires Metal Number
---------------------|----------------------
APC                  | Yes
CPC                  | Yes
WPC                  | Yes
PC                   | Yes
ASI                  |
PSI                  |
```

**Districts Sheet:**
```
District
--------
Bagalkot
Ballari
Belagavi City
```

**Stations Sheet:**
```
District        | Station
---------------|------------------
Bagalkot       | Amengad PS
Bagalkot       | Badami PS
Bagalkot       | Control Room Bagalkot
Ballari        | APMC Yard PS
```

**BloodGroups Sheet:**
```
BloodGroup
----------
A+
A-
B+
AB+
O+
??
```

---

## ✅ Benefits of This Method

1. ✅ **No Runtime Fetching** - Constants are compiled into the app
2. ✅ **Faster Performance** - No network delays
3. ✅ **Offline Support** - Works without internet
4. ✅ **Type Safety** - Compile-time checking
5. ✅ **Auto-Sync** - Districts automatically added to Stations sheet
6. ✅ **Easy Updates** - Just regenerate and paste

---

## 🧪 Testing

### Test the Script:
1. In Apps Script editor, select `testConstants` function
2. Click **Run** (▶️)
3. Check execution logs for output

### Test Web App:
1. Visit: `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec?type=kotlin`
2. Verify you see valid Kotlin code
3. Check that all your data is included

### Test onEdit Trigger:
1. Add a new district in Districts sheet
2. Check Stations sheet - should see a new row with that district
3. Check execution logs for trigger activity

---

## 🔧 Troubleshooting

### Issue: "Authorization required"
- Run any function once to trigger authorization
- Make sure you clicked "Allow" when prompted

### Issue: "Sheet not found"
- Verify sheet names match exactly (case-sensitive)
- Check that `SHEET_ID` is correct

### Issue: "Drive API not enabled"
- The script will prompt you on first run
- Make sure you authorized Drive access

### Issue: onEdit trigger not working
- Go to Triggers section in Apps Script
- Verify trigger is set up correctly
- Check execution logs for errors

### Issue: Generated Kotlin has syntax errors
- Check that all data in sheets is valid
- Make sure there are no special characters that break Kotlin strings
- Verify all districts have at least one station

---

## 📝 Quick Reference URLs

Replace `[DEPLOYMENT_ID]` with your actual deployment ID:

- **Generate Kotlin:** `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec?type=kotlin`
- **Save to Drive:** `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec?type=download`
- **Get JSON:** `https://script.google.com/macros/s/[DEPLOYMENT_ID]/exec`

---

## 🎯 Summary

1. ✅ Create Google Sheet with 4 sheets (Ranks, Districts, Stations, BloodGroups)
2. ✅ Paste script code, set `SHEET_ID`
3. ✅ Authorize Drive API access
4. ✅ Deploy as Web App
5. ✅ Set up onEdit trigger
6. ✅ Generate Constants.kt via browser URL
7. ✅ Copy/paste into Android Studio
8. ✅ Rebuild app

**This method is excellent** because it generates the file once, you paste it, and the app uses it directly - no runtime fetching needed! 🚀







