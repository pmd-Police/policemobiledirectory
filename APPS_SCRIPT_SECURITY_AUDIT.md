# 🔐 Apps Script Security Audit Report

## Current Security Status

### ✅ **What's Working:**
1. **Admin Checks:** All scripts use `isAdmin()` function
2. **JSON Responses:** Proper `jsonResponse()` helpers in place
3. **Error Handling:** Try-catch blocks in API handlers

### ⚠️ **Security Gaps Found:**

#### 1. **Hardcoded Admin Lists** ❌
**Files Affected:**
- `DOCUMENTS_Common.gs` (lines 17-20)
- `GALLERY_Common.gs` (lines 18-21)

**Current Implementation:**
```javascript
const ALLOWED_ADMINS = [
  "ravipolice@gmail.com",
  "noreply.policemobiledirectory@gmail.com"
];

function isAdmin(user) {
  return user && ALLOWED_ADMINS.includes(user);
}
```

**Issue:** Hardcoded emails require script redeployment to add/remove admins.

**Recommendation:** Use Firestore-based admin verification (as in `USEFUL_LINKS_SECURE_TEMPLATE.gs`)

---

#### 2. **Missing Token Authentication** ❌
**Files Affected:**
- `USEFUL_LINKS_Api.gs` - No token verification
- `DOCUMENTS_Api.gs` - No token verification  
- `GALLERY_Api.gs` - No token verification

**Current Implementation:**
- `doGet()` and `doPost()` handlers don't verify secret tokens
- Anyone with the URL can call the API

**Recommendation:** Add `verifyToken()` function to all API handlers

---

#### 3. **Missing firebaseServices.gs Integration** ❓
**Status:** Not found in repository

**Expected Functions:**
- `verifyAdmin(email)` - Check Firestore admins collection
- `verifyToken(e)` - Verify secret token
- `getFirestoreDoc(collection, docId)` - Firestore read helper
- `updateFirestoreDoc(collection, docId, data)` - Firestore write helper

**Action Required:** Verify these functions exist in your Google Apps Script project

---

#### 4. **Missing helpers.gs Integration** ❓
**Status:** Not found in repository

**Expected Functions:**
- `jsonResponse(obj, statusCode)` - Enhanced JSON response
- `isValidEmail(email)` - Email validation
- `validateImage(base64Data, filename)` - Image validation
- `checkRateLimit(identifier)` - Rate limiting

**Action Required:** Verify these functions exist in your Google Apps Script project

---

## 🔧 Required Updates

### **Priority 1: Add Token Authentication**

#### Update `USEFUL_LINKS_Api.gs`:
```javascript
function doGet(e) {
  try {
    // ✅ ADD THIS: Verify token
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    const sheet = getUsefulLinksSheet();
    // ... rest of code
  } catch (err) {
    return jsonResponse({ success: false, error: err.toString() }, 500);
  }
}
```

#### Update `DOCUMENTS_Api.gs`:
```javascript
function doPost(e) {
  try {
    // ✅ ADD THIS: Verify token first
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    if (!e.postData?.contents) throw new Error("Missing POST body");
    // ... rest of code
  } catch (err) {
    return jsonResponse({ success: false, error: err.message });
  }
}
```

#### Update `GALLERY_Api.gs`:
```javascript
function doPost(e) {
  try {
    // ✅ ADD THIS: Verify token first
    const tokenError = verifyToken(e);
    if (tokenError) return tokenError;
    
    if (!e.postData?.contents) throw new Error("Missing POST body");
    // ... rest of code
  } catch (err) {
    return jsonResponse({ success: false, error: err.message });
  }
}
```

---

### **Priority 2: Replace Hardcoded Admins with Firestore**

#### Update `DOCUMENTS_Common.gs`:
```javascript
// ❌ REMOVE THIS:
const ALLOWED_ADMINS = [
  "ravipolice@gmail.com",
  "noreply.policemobiledirectory@gmail.com"
];

function isAdmin(user) {
  return user && ALLOWED_ADMINS.includes(user);
}

// ✅ REPLACE WITH:
function isAdmin(user) {
  // Use firebaseServices.verifyAdmin() if available
  if (typeof verifyAdmin === 'function') {
    return verifyAdmin(user);
  }
  // Fallback to Firestore check
  return verifyAdminFromFirestore(user);
}

function verifyAdminFromFirestore(email) {
  try {
    const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
    const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/admins/${encodeURIComponent(email)}?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(url);
    if (response.getResponseCode() === 200) {
      const data = JSON.parse(response.getContentText());
      return data.fields && data.fields.isActive && data.fields.isActive.booleanValue === true;
    }
  } catch (e) {
    Logger.log("Admin verification failed: " + e);
  }
  return false;
}
```

#### Update `GALLERY_Common.gs`:
Same changes as above.

---

### **Priority 3: Add firebaseServices.gs Functions**

If you've added `firebaseServices.gs`, ensure it includes:

```javascript
// firebaseServices.gs
const FIREBASE_PROJECT_ID = "pmd-police-mobile-directory";
const FIREBASE_API_KEY = "AIzaSyB_d5ueTul9vKeNw3pmEtCmbF9w1BVkrAQ";

function verifyAdmin(email) {
  try {
    const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/admins/${encodeURIComponent(email)}?key=${FIREBASE_API_KEY}`;
    const response = UrlFetchApp.fetch(url);
    if (response.getResponseCode() === 200) {
      const data = JSON.parse(response.getContentText());
      return data.fields && data.fields.isActive && data.fields.isActive.booleanValue === true;
    }
  } catch (e) {
    Logger.log("Admin verification failed: " + e);
  }
  return false;
}

function getFirestoreDoc(collection, docId) {
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}/${docId}?key=${FIREBASE_API_KEY}`;
  const response = UrlFetchApp.fetch(url);
  if (response.getResponseCode() === 200) {
    return JSON.parse(response.getContentText());
  }
  return null;
}

function updateFirestoreDoc(collection, docId, data) {
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}/${docId}?key=${FIREBASE_API_KEY}`;
  const options = {
    method: 'patch',
    contentType: 'application/json',
    payload: JSON.stringify({ fields: data })
  };
  const response = UrlFetchApp.fetch(url, options);
  return response.getResponseCode() === 200;
}
```

---

### **Priority 4: Add helpers.gs Functions**

If you've added `helpers.gs`, ensure it includes:

```javascript
// helpers.gs
const SECRET_TOKEN = "YOUR_SECRET_TOKEN_HERE"; // Set this!

function verifyToken(e) {
  const token = e.parameter.token || (e.postData && JSON.parse(e.postData.contents).token);
  if (!token || token !== SECRET_TOKEN) {
    return jsonResponse({ 
      success: false, 
      error: "Unauthorized: Invalid or missing token" 
    }, 401);
  }
  return null; // Token is valid
}

function isValidEmail(email) {
  return email && email.match(/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/);
}

function validateImage(base64Data, filename) {
  // Check size (5MB max)
  const sizeBytes = Utilities.base64Decode(base64Data).length;
  if (sizeBytes > 5 * 1024 * 1024) {
    return { valid: false, error: "File size exceeds 5MB limit" };
  }
  
  // Check extension
  const ext = filename.toLowerCase().split('.').pop();
  if (!['jpg', 'jpeg', 'png'].includes(ext)) {
    return { valid: false, error: "Invalid file type. Only JPEG/PNG allowed" };
  }
  
  // Verify header
  const bytes = Utilities.base64Decode(base64Data);
  const isJpeg = bytes[0] === 0xFF && bytes[1] === 0xD8;
  const isPng = bytes[0] === 0x89 && bytes[1] === 0x50;
  
  return { valid: isJpeg || isPng };
}
```

---

## 📋 Verification Checklist

- [ ] `firebaseServices.gs` exists in all Apps Script projects
- [ ] `helpers.gs` exists in all Apps Script projects
- [ ] `SECRET_TOKEN` constant is set in `helpers.gs`
- [ ] All `doGet()` handlers verify tokens
- [ ] All `doPost()` handlers verify tokens
- [ ] `isAdmin()` uses Firestore verification (not hardcoded)
- [ ] `verifyAdmin()` function works correctly
- [ ] `verifyToken()` function works correctly
- [ ] Image uploads validate file type and size
- [ ] Rate limiting is implemented (if needed)

---

## 🚨 Critical Actions Required

1. **Add Token Verification** to all API endpoints
2. **Replace Hardcoded Admins** with Firestore-based verification
3. **Verify firebaseServices.gs** is included in all projects
4. **Verify helpers.gs** is included in all projects
5. **Set SECRET_TOKEN** in helpers.gs
6. **Test all endpoints** with and without tokens

---

## 📝 Next Steps

1. **Confirm:** Do `firebaseServices.gs` and `helpers.gs` exist in your Google Apps Script projects?
2. **Update:** Add token verification to all API handlers
3. **Replace:** Hardcoded admin lists with Firestore verification
4. **Test:** Verify all security measures work correctly
5. **Deploy:** Redeploy all Apps Script web apps

---

**🔐 Security is critical for a police department app. Please implement these changes before production deployment!**

