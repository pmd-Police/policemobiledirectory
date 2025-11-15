# Test POST Endpoint for Apps Script

## ✅ Good News: GET Works!

Getting `{"error":"Invalid action"}` means:
- ✅ Script is deployed correctly
- ✅ Accessible via web
- ✅ Returning JSON (not HTML/401)

## ❌ Issue: POST Gets 401

The 401 error only happens with **POST requests**. This is likely a deployment permission issue for POST.

## 🧪 Test POST Endpoint

### Option 1: Use curl (Command Line)

Test the POST endpoint directly:

```bash
curl -X POST \
  "https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec?action=uploadImage" \
  -F "file=@/path/to/test-image.jpg" \
  -v
```

**Expected:** JSON response with `success: true` (not 401)

### Option 2: Use Postman

1. Create new request
2. Method: **POST**
3. URL: `https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec?action=uploadImage`
4. Body → form-data
5. Key: `file`, Type: **File**
6. Select a test image
7. Click **Send**

**Expected:** JSON response with `success: true` (not 401)

### Option 3: Check Apps Script Executions

1. Go to: https://script.google.com
2. Open your project
3. Click **Executions** (clock icon)
4. Look for recent `doPost` executions
5. Check if any show errors or 401

## 🔧 Fix POST 401 Error

The 401 error for POST usually means:

1. **Deployment permissions need to be set to "Anyone"** (not "Anyone with Google account")
2. **Script needs to be redeployed** after code changes
3. **POST endpoint might need explicit permissions**

### Steps to Fix:

1. **In Apps Script:**
   - Click **Deploy** → **Manage deployments**
   - Click **Edit** (pencil) on your deployment
   - Make sure:
     - **Execute as:** `Me`
     - **Who has access:** `Anyone` ← **CRITICAL!**
   - Click **Deploy**

2. **Wait 1-2 minutes** after redeploying

3. **Test again:**
   - Use curl/Postman to test POST
   - Or test from Android app

## 📋 Verification Checklist

After fixing deployment:

- [ ] GET request works: `{"error":"Invalid action"}` ✅ (Already working)
- [ ] POST request works: Returns JSON with `success: true` (should work after fix)
- [ ] No 401 errors in curl/Postman test
- [ ] Apps Script executions log shows successful POST

## ⚠️ Important Notes

1. **"Anyone with Google account"** will cause 401 for unauthenticated POST requests
2. **Must be "Anyone"** for your Android app to work without login
3. **After changing permissions, you may need a NEW deployment** (not just update)

