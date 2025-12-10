# Google Sheet Headers Reference for Employee Sync

## 📊 Exact Column Headers (Based on Your Sheet)

Your Google Sheet has these headers in Row 1:

| Column | Header Name | Firestore Field | Employee.kt Property | Notes |
|--------|-------------|-----------------|---------------------|-------|
| A | `kgid` | kgid | kgid | Document ID - **REQUIRED** |
| B | `name` | name | name | Employee name |
| C | `mobile1` | mobile1 | mobile1 | Primary mobile |
| D | `mobil` | mobile2 | mobile2 | Secondary mobile (header says "mobil") |
| E | `rank` | rank | rank | Employee rank |
| F | `station` | station | station | Station name |
| G | `district` | district | district | District name |
| H | `metal` | metal | metalNumber | Metal number (uses @PropertyName) |
| I | `bloodGroup` | bloodGroup | bloodGroup | Blood group |
| J | `email` | email | email | Email address |
| K | `photoUrl` | photoUrl | photoUrl | Photo URL |
| L | `photoUrlFromGoogle` | photoUrlFromGoogle | photoUrlFromGoogle | Google photo URL |
| M | `firebaseUid` | firebaseUid | firebaseUid | Firebase user ID |
| N | `fcmToken` | fcmToken | fcmToken | FCM token |
| O | `isAdmin` | isAdmin | isAdmin | Admin flag (Boolean) |
| P | `isApproved` | isApproved | isApproved | Approval flag (Boolean) - **Must be TRUE** |
| Q | `pin` | pin | pin | PIN hash |
| R | `createdAt` | createdAt | createdAt | Creation timestamp |
| S | `updatedAt` | updatedAt | updatedAt | Update timestamp |
| T | `isDeleted` | isDeleted | isDeleted | Deletion flag (Boolean) |

## ⚠️ Important Header Mappings

### 1. Column D: `mobil` → `mobile2`
- **Sheet header**: `mobil`
- **Firestore field**: `mobile2`
- **Action needed**: The sync script should map `mobil` column to `mobile2` field in Firestore

### 2. Column H: `metal` → `metal`
- **Sheet header**: `metal`
- **Firestore field**: `metal`
- **Employee.kt property**: `metalNumber` (with `@PropertyName("metal")`)
- ✅ Already correctly mapped

### 3. Boolean Fields
These must contain `TRUE`/`FALSE` or `true`/`false`:
- `isAdmin` (Column O)
- `isApproved` (Column P) - **⚠️ Set to TRUE for employees to show**
- `isDeleted` (Column T) - Should be FALSE for active employees

## 🔧 Sync Script Behavior

The current sync script (`EMPLOYEE_SYNC_COMPLETE_INTEGRATED.gs`):
- ✅ Reads headers dynamically from Row 1
- ✅ Maps header names directly to Firestore field names
- ✅ Handles boolean conversion automatically (isAdmin, isApproved, isDeleted)
- ✅ Maps `mobil` → `mobile2` automatically
- ✅ Maps `metal` → `metal` correctly

## 📝 Required Updates

### Update Sync Script to Handle Header Variations

The script should map:
- `mobil` (sheet header) → `mobile2` (Firestore field)
- `metal` (sheet header) → `metal` (Firestore field) ✅ Already correct

## ✅ Verification Checklist

Before syncing, verify your sheet has:
- [x] All 20 headers in Row 1
- [x] `kgid` in Column A (required)
- [x] `metal` (not `metalNumber`) in Column H
- [x] `isApproved` column with TRUE/FALSE values
- [x] No extra spaces in header names
- [x] Headers are exactly as listed above








