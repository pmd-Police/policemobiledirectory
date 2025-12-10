# Complete Google Sheet Headers Reference

## 📋 Your Google Sheet Header Structure (Row 1)

Based on your sheet image, here are the exact headers in order:

```
Column A:  kgid
Column B:  name
Column C:  mobile1
Column D:  mobile2        ← Direct mapping (no transformation needed)
Column E:  rank
Column F:  station
Column G:  district
Column H:  metal          ← Maps to "metal" in Firestore (not "metalNumber")
Column I:  bloodGroup
Column J:  email
Column K:  photoUrl
Column L:  photoUrlFromGoogle
Column M:  firebaseUid
Column N:  fcmToken
Column O:  isAdmin        ← Boolean (TRUE/FALSE)
Column P:  isApproved     ← Boolean (TRUE/FALSE) - Must be TRUE to show
Column Q:  pin
Column R:  createdAt
Column S:  updatedAt
Column T:  isDeleted      ← Boolean (TRUE/FALSE)
```

## ✅ Sync Script Updates Applied

### 1. Header Name Mapping
- ✅ `mobile2` (Column D) → `mobile2` in Firestore (direct match)
- ✅ `metal` (Column H) → `metal` in Firestore
- ✅ All other headers map directly (perfect alignment)

### 2. Boolean Field Handling
- ✅ `isAdmin` - Converted to boolean automatically
- ✅ `isApproved` - Converted to boolean automatically  
- ✅ `isDeleted` - Converted to boolean automatically

### 3. Field Mapping Summary

| Sheet Header | Firestore Field | Type | Notes |
|--------------|----------------|------|-------|
| kgid | kgid | String | Document ID |
| name | name | String | |
| mobile1 | mobile1 | String | |
| mobile2 | mobile2 | String | Direct mapping |
| rank | rank | String | |
| station | station | String | |
| district | district | String | |
| **metal** | **metal** | String | Maps via @PropertyName |
| bloodGroup | bloodGroup | String | |
| email | email | String | |
| photoUrl | photoUrl | String | |
| photoUrlFromGoogle | photoUrlFromGoogle | String | |
| firebaseUid | firebaseUid | String | |
| fcmToken | fcmToken | String | |
| isAdmin | isAdmin | Boolean | TRUE/FALSE |
| isApproved | isApproved | Boolean | TRUE/FALSE - **Critical** |
| pin | pin | String | |
| createdAt | createdAt | Timestamp | |
| updatedAt | updatedAt | Timestamp | Auto-updated |
| isDeleted | isDeleted | Boolean | TRUE/FALSE |

## 🚨 Critical Requirements

### For Employees to Show in App:
1. ✅ `kgid` must be present (required)
2. ✅ `isApproved` must be `TRUE` (not FALSE or empty)
3. ✅ `isDeleted` must be `FALSE` or empty

### Before Syncing:
1. Ensure all headers are in Row 1
2. Set `isApproved = TRUE` for all employees you want to display
3. Set `isDeleted = FALSE` for active employees
4. Ensure `kgid` is filled in for all rows

## ✅ Status

- ✅ Sync URL updated to your script
- ✅ Header mapping configured (direct `mobile2` mapping, with legacy `mobil` support)
- ✅ Boolean fields handled correctly
- ✅ `metal` field mapping confirmed
- ✅ Auto-refresh after sync enabled








