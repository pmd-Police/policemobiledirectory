# ✅ Firestore Collection Updated: "officers" → "employees"

## ✅ Change Made

Updated the Firestore collection name from `"officers"` to `"employees"` in the Apps Script.

---

## 📝 Updated Code

**Before:**
```javascript
const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/officers/" + encodeURIComponent(kgid);
```

**After:**
```javascript
const docPath = "projects/" + FIREBASE_PROJECT_ID + "/databases/(default)/documents/employees/" + encodeURIComponent(kgid);
```

---

## 📍 Location

**File:** `APPS_SCRIPT_FINAL_COMPLETE.js`  
**Function:** `updateFirebaseProfileImage(kgid, url)`  
**Line:** 585

---

## ✅ What This Means

When an image is uploaded:
1. ✅ Image is saved to Google Drive
2. ✅ Google Sheet is updated with photoUrl
3. ✅ **Firestore collection `employees/{kgid}` is updated** (not `officers/{kgid}`)

---

## 🔍 Firestore Structure

**Collection:** `employees`  
**Document ID:** `{kgid}` (e.g., "98765")  
**Field:** `photoUrl`  
**Value:** Google Drive URL

**Example:**
```
employees/
  └── 98765/
      └── photoUrl: "https://drive.google.com/uc?export=view&id=FILE_ID"
```

---

## ✅ Next Steps

1. **Deploy the updated script** to Apps Script
2. **Verify Firestore collection** is named `employees` (not `officers`)
3. **Test image upload** - should update `employees/{kgid}/photoUrl`

---

**Collection name updated successfully!** ✅

Now deploy the script and test! 🚀





