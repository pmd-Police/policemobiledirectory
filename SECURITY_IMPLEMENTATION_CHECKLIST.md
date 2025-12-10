# ✅ Security Implementation Checklist

## Quick Verification Guide

### Step 1: Verify firebaseServices.gs Exists
**In Google Apps Script Editor:**
1. Open each Apps Script project
2. Check if `firebaseServices.gs` file exists
3. Verify it contains:
   - `verifyAdmin(email)` function
   - `getFirestoreDoc(collection, docId)` function
   - `updateFirestoreDoc(collection, docId, data)` function

### Step 2: Verify helpers.gs Exists
**In Google Apps Script Editor:**
1. Check if `helpers.gs` file exists
2. Verify it contains:
   - `SECRET_TOKEN` constant (with actual token value)
   - `verifyToken(e)` function
   - `isValidEmail(email)` function
   - `validateImage(base64Data, filename)` function

### Step 3: Check Token Usage
**Search in each script for:**
```javascript
verifyToken(e)
```

**Should appear in:**
- ✅ `USEFUL_LINKS_Api.gs` → `doGet()` and `doPost()`
- ✅ `DOCUMENTS_Api.gs` → `doPost()`
- ✅ `GALLERY_Api.gs` → `doPost()`

### Step 4: Check Admin Verification
**Search in each Common file for:**
```javascript
verifyAdmin(user)
// OR
isAdmin(user) // Should call verifyAdmin internally
```

**Should appear in:**
- ✅ `DOCUMENTS_Common.gs` → `isAdmin()` function
- ✅ `GALLERY_Common.gs` → `isAdmin()` function
- ✅ `USEFUL_LINKS_Common.gs` → (if applicable)

### Step 5: Test Security
**Test each endpoint:**

1. **Without Token:**
   ```bash
   curl "https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=getLinks"
   ```
   **Expected:** `{"success":false,"error":"Unauthorized: Invalid or missing token"}`

2. **With Wrong Token:**
   ```bash
   curl "https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=getLinks&token=wrong_token"
   ```
   **Expected:** `{"success":false,"error":"Unauthorized: Invalid or missing token"}`

3. **With Correct Token:**
   ```bash
   curl "https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec?action=getLinks&token=YOUR_SECRET_TOKEN"
   ```
   **Expected:** `{"success":true,"data":[...]}`

---

## Files to Update

### ✅ Already Secure:
- `firestore.rules` - ✅ Uses Firestore admins collection
- `USEFUL_LINKS_SECURE_TEMPLATE.gs` - ✅ Has all security measures

### ⚠️ Needs Updates:
- `USEFUL_LINKS_Api.gs` - Add token verification
- `DOCUMENTS_Api.gs` - Add token verification
- `GALLERY_Api.gs` - Add token verification
- `DOCUMENTS_Common.gs` - Replace hardcoded admins
- `GALLERY_Common.gs` - Replace hardcoded admins

---

## Quick Fix Template

### For API Files (doGet/doPost):
```javascript
function doGet(e) {
  try {
    // ✅ ADD THIS FIRST
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    // ... rest of your code
  } catch (err) {
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}
```

### For Common Files (isAdmin):
```javascript
// ❌ REMOVE:
const ALLOWED_ADMINS = ["email1", "email2"];

function isAdmin(user) {
  return user && ALLOWED_ADMINS.includes(user);
}

// ✅ REPLACE WITH:
function isAdmin(user) {
  if (typeof verifyAdmin === 'function') {
    return verifyAdmin(user);
  }
  // Fallback implementation
  return verifyAdminFromFirestore(user);
}
```

---

**🔐 Complete this checklist to ensure all security measures are in place!**

