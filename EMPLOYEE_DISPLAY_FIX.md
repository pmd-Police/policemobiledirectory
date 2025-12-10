# Employee Cards Not Showing - Fix Guide

## Issues Identified

1. **Employees may not have `isApproved = true` set** in Firestore, causing them to be filtered out
2. **District/Station/Rank filters** may be excluding employees with null/empty values
3. **Employees might not be loading** from Firestore properly

## Changes Made

### 1. Removed isApproved Filtering (Temporary)
- Modified `allContacts` in `EmployeeViewModel.kt` to show ALL employees temporarily
- This allows you to see if employees are loading from Firestore

### 2. Fixed District/Station/Rank Filtering
- Updated filtering logic to properly handle null/empty values
- Employees with null district/station/rank will now show when filters are set to "All"

### 3. Fixed refreshEmployees() Function
- Updated `refreshEmployees()` to actually force reload from Firestore
- It now clears cache AND fetches fresh data from Firestore
- Skips deleted employees (isDeleted = true)

## Next Steps to Fix

### Step 1: Verify Employees Are Loading
1. Open the app
2. Check if employees now appear (they should, since filtering is disabled)
3. If they still don't appear, the issue is with loading from Firestore

### Step 2: Set isApproved for All Employees
Once employees are showing, you need to set `isApproved = true` for all employee records in Firestore:

**Option A: Via Firebase Console**
1. Go to Firebase Console → Firestore → employees collection
2. For each employee document, add/update the `isApproved` field to `true`

**Option B: Via Google Apps Script**
Add this function to your sync script:
```javascript
function setAllEmployeesApproved() {
  const firestoreUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents:runQuery`;
  const token = getServiceAccountToken();
  
  // Get all employees
  const query = {
    structuredQuery: {
      from: [{ collectionId: "employees" }],
      limit: 1000
    }
  };
  
  const response = UrlFetchApp.fetch(firestoreUrl, {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + token,
      "Content-Type": "application/json"
    },
    payload: JSON.stringify(query),
    muteHttpExceptions: true
  });
  
  const result = JSON.parse(response.getContentText());
  
  result.forEach(item => {
    if (item.document) {
      const kgid = item.document.name.split("/").pop();
      const docUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/employees/${kgid}`;
      
      UrlFetchApp.fetch(docUrl, {
        method: "PATCH",
        headers: {
          "Authorization": "Bearer " + token,
          "Content-Type": "application/json"
        },
        payload: JSON.stringify({
          fields: {
            isApproved: { booleanValue: true }
          }
        }),
        muteHttpExceptions: true
      });
    }
  });
}
```

### Step 3: Re-enable isApproved Filtering
Once all employees have `isApproved = true`, update `EmployeeViewModel.kt`:

```kotlin
val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
    val filteredEmployees = if (isAdmin) {
        employees
    } else {
        employees.filter { it.isApproved }
    }
    val employeeContacts = filteredEmployees.map { Contact(employee = it) }
    val officerContacts = officers.map { Contact(officer = it) }
    employeeContacts + officerContacts
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

### Step 4: Ensure District/Station/Rank Are Set
Make sure all employee records have:
- `district` field set (even if empty, should be "")
- `station` field set
- `rank` field set

Or set filters to "All" to show all employees regardless of these fields.

## Debugging

If employees still don't show:

1. **Check Logcat** for errors:
   ```
   adb logcat | grep -i "employee\|error\|exception"
   ```

2. **Verify Firestore Connection**:
   - Check if employees collection exists
   - Verify employees have kgid field set

3. **Clear App Data**:
   - Settings → Apps → Your App → Clear Data
   - This forces a fresh load from Firestore

## Current Status

✅ Filtering temporarily disabled - all employees should show  
✅ District/Station/Rank filtering improved  
✅ refreshEmployees() now forces reload from Firestore  
⏳ Need to set isApproved = true for all employees in Firestore  
⏳ Need to re-enable isApproved filtering after setting values  

## Important Notes

1. **After syncing employees to Firestore**, you MUST:
   - Run `setAllEmployeesApproved()` function to set `isApproved = true` for all
   - Clear app data or use the refresh button in the app to reload

2. **Verify kgid field**: Make sure each employee document has:
   - Document ID = kgid (e.g., "2010804")
   - `kgid` field in the document fields as well

3. **Employee record structure**: Each employee should have:
   - `kgid` (string) - Document ID
   - `name` (string)
   - `email` (string)
   - `isApproved` (boolean) - Set to `true`
   - `isDeleted` (boolean) - Set to `false`
   - `district`, `station`, `rank` (strings) - Can be null/empty  








