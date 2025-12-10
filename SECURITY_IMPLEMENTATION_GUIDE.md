# 🔐 Security Implementation Guide

## Critical Security Hardening for Police Department App

This guide documents all security measures implemented to protect the Police Mobile Directory app.

---

## ✅ Security Measures Implemented

### 1. **Firestore Security Rules** ✅

**File:** `firestore.rules`

**Improvements:**
- ✅ Uses `admins` collection instead of hardcoded emails
- ✅ Rate limiting for OTP codes (10-minute expiration)
- ✅ Strict validation for all write operations
- ✅ Admin verification from Firestore collection
- ✅ Field-level restrictions (prevent privilege escalation)
- ✅ Size limits for string fields
- ✅ Email format validation

**Key Changes:**
```javascript
// Before: Hardcoded emails
function isAdmin() {
  return request.auth.token.email in ["email1", "email2"];
}

// After: Dynamic from Firestore
function isAdmin() {
  return request.auth != null
      && exists(/databases/$(database)/documents/admins/$(request.auth.token.email))
      && get(...).data.isActive == true;
}
```

---

### 2. **API Authentication with Secret Tokens** ✅

**Files:**
- `app/src/main/java/com/example/policemobiledirectory/utils/SecurityConfig.kt` (NEW)
- `APPS_SCRIPT_SECURE_TEMPLATE.js` (NEW)

**Implementation:**
- Secret token stored in BuildConfig (from gradle.properties or CI/CD)
- Token verified in all Apps Script endpoints
- Returns 401 if token is missing/invalid

**Setup:**
1. Generate secret token:
   ```bash
   openssl rand -hex 32
   ```

2. Add to `gradle.properties`:
   ```properties
   APPS_SCRIPT_SECRET_TOKEN=your_generated_token_here
   ```

3. Add to Apps Script:
   ```javascript
   const SECRET_TOKEN = "your_generated_token_here";
   ```

4. Verify token in all endpoints:
   ```javascript
   function verifyToken(e) {
     const token = e.parameter.token || JSON.parse(e.postData.contents).token;
     if (token !== SECRET_TOKEN) {
       return jsonResponse({ error: "Unauthorized" }, 401);
     }
   }
   ```

---

### 3. **Server-Side Admin Verification** ✅

**Implementation:**
- Admin status verified in Firestore before allowing admin actions
- Client-side checks are NOT trusted
- Apps Script verifies admin via Firestore API

**Code:**
```javascript
function verifyAdmin(email) {
  const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/admins/${email}?key=${API_KEY}`;
  const response = UrlFetchApp.fetch(url);
  if (response.getResponseCode() === 200) {
    const data = JSON.parse(response.getContentText());
    return data.fields.isActive.booleanValue === true;
  }
  return false;
}
```

---

### 4. **Image Upload Security** ✅

**Validations:**
- ✅ File size limit: 5MB maximum
- ✅ File type validation: JPEG/PNG only
- ✅ File header verification (prevents fake extensions)
- ✅ Rate limiting: 10 uploads per hour per user
- ✅ Authentication token required
- ✅ User ownership verification (optional but recommended)

**Implementation:**
```javascript
function validateImage(base64Data, filename) {
  // Check size
  if (Utilities.base64Decode(base64Data).length > 5 * 1024 * 1024) {
    return { valid: false, error: "File too large" };
  }
  
  // Check type
  const ext = filename.toLowerCase().split('.').pop();
  if (!['jpg', 'jpeg', 'png'].includes(ext)) {
    return { valid: false, error: "Invalid file type" };
  }
  
  // Verify header
  const bytes = Utilities.base64Decode(base64Data);
  const isJpeg = bytes[0] === 0xFF && bytes[1] === 0xD8;
  const isPng = bytes[0] === 0x89 && bytes[1] === 0x50;
  
  return { valid: isJpeg || isPng };
}
```

---

### 5. **App Signature Verification** ✅

**File:** `app/src/main/java/com/example/policemobiledirectory/utils/AppSignatureVerifier.kt` (NEW)

**Purpose:**
- Prevents app tampering/repackaging
- Verifies app hasn't been modified
- Runs at app startup

**Setup:**
1. Get current signature hash:
   ```kotlin
   val hash = signatureVerifier.getCurrentSignatureHash()
   Log.d("Security", "Signature hash: $hash")
   ```

2. Add to `gradle.properties`:
   ```properties
   EXPECTED_SIGNATURE_HASH=your_signature_hash_here
   ```

3. Verification runs automatically at app startup

---

## 📋 Deployment Checklist

### Before Deploying:

- [ ] **Generate Secret Tokens**
  ```bash
  openssl rand -hex 32  # For Apps Script
  ```

- [ ] **Update Firestore Rules**
  - Deploy new rules to Firebase Console
  - Test with admin and non-admin users
  - Verify OTP rate limiting works

- [ ] **Create Admins Collection**
  - Create `admins` collection in Firestore
  - Add admin documents:
    ```json
    {
      "email": "admin@example.com",
      "isActive": true
    }
    ```

- [ ] **Update Apps Script**
  - Replace `APPS_SCRIPT_SECURE_TEMPLATE.js` content
  - Set `SECRET_TOKEN` constant
  - Deploy as web app

- [ ] **Update Android App**
  - Add `APPS_SCRIPT_SECRET_TOKEN` to `gradle.properties`
  - Rebuild app
  - Test all API calls

- [ ] **Get App Signature Hash**
  - Run app in debug mode
  - Check logs for signature hash
  - Add to `gradle.properties` as `EXPECTED_SIGNATURE_HASH`

- [ ] **Test Security**
  - [ ] Try API call without token → Should fail
  - [ ] Try API call with wrong token → Should fail
  - [ ] Try admin action as non-admin → Should fail
  - [ ] Try uploading >5MB image → Should fail
  - [ ] Try uploading non-image file → Should fail
  - [ ] Try uploading 11th time in an hour → Should fail

---

## 🔒 Security Best Practices

### 1. **Never Commit Secrets**
- Add to `.gitignore`:
  ```
  gradle.properties
  local.properties
  *.keystore
  ```

### 2. **Use CI/CD Secrets**
- Store tokens in GitHub Secrets / CI/CD variables
- Inject at build time
- Never hardcode in source code

### 3. **Rotate Tokens Regularly**
- Change secret tokens every 90 days
- Update both Apps Script and Android app simultaneously

### 4. **Monitor for Attacks**
- Log all failed authentication attempts
- Set up alerts for suspicious activity
- Review Firestore access logs regularly

### 5. **Keep Dependencies Updated**
- Regularly update Firebase, Room, and other libraries
- Patch security vulnerabilities immediately

---

## 🚨 Critical Security Notes

1. **Firestore Rules are the Last Line of Defense**
   - Never trust client-side validation alone
   - Always verify server-side

2. **Secret Tokens Must Be Strong**
   - Minimum 32 characters
   - Use cryptographically secure random generation
   - Never reuse tokens across environments

3. **Admin Actions Require Double Verification**
   - Client-side check (UX)
   - Server-side verification (Security)

4. **Image Uploads are High Risk**
   - Validate everything: size, type, content
   - Rate limit aggressively
   - Scan for malware (if possible)

5. **App Signature Verification**
   - Prevents repackaging attacks
   - Warns if app is tampered with
   - Consider blocking app if signature fails

---

## 📞 Security Incident Response

If you suspect a security breach:

1. **Immediately:**
   - Rotate all secret tokens
   - Review Firestore access logs
   - Check for unauthorized data access

2. **Within 24 Hours:**
   - Update Firestore rules if needed
   - Revoke compromised admin access
   - Notify affected users

3. **Post-Incident:**
   - Conduct security audit
   - Update security measures
   - Document lessons learned

---

## ✅ Files Modified/Created

1. ✅ `firestore.rules` - Hardened security rules
2. ✅ `app/src/main/java/com/example/policemobiledirectory/utils/SecurityConfig.kt` - NEW
3. ✅ `app/src/main/java/com/example/policemobiledirectory/utils/AppSignatureVerifier.kt` - NEW
4. ✅ `app/src/main/java/com/example/policemobiledirectory/PoliceMobileDirectoryApp.kt` - Added signature verification
5. ✅ `app/build.gradle.kts` - Added BuildConfig fields
6. ✅ `APPS_SCRIPT_SECURE_TEMPLATE.js` - NEW secure template
7. ✅ `SECURITY_HARDENING.md` - Security documentation
8. ✅ `SECURITY_IMPLEMENTATION_GUIDE.md` - This file

---

**🔐 Security is an ongoing process. Regularly review and update these measures!**

