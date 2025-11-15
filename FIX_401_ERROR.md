# Fix HTTP 401 Unauthorized Error

## ❌ Current Error

```
Upload response code: 401, isSuccessful: false
Raw response: <!DOCTYPE html><html>...<title>Page not found</title>...
```

**HTTP 401** = **Unauthorized** - The server is rejecting your request because of permissions.

## 🔍 Root Cause

This usually means:
1. ❌ **Apps Script deployment is not accessible** to "Anyone"
2. ❌ **Script needs to be redeployed** after code changes
3. ❌ **Deployment is expired or inactive**
4. ❌ **Wrong URL** (though this looks correct based on your setup)

## ✅ Solution Steps

### 1. **Check Your Apps Script Deployment**

Go to your Apps Script project:
1. Open: https://script.google.com
2. Find your project
3. Click **Deploy** → **Manage deployments**

### 2. **Verify Deployment Settings**

Your deployment **MUST** have:
- ✅ **Execute as:** `Me (your-email@gmail.com)`
- ✅ **Who has access:** `Anyone` (not "Anyone with Google account")

**Important:** If it says "Anyone with Google account", change it to **"Anyone"**

### 3. **Create New Deployment (Recommended)**

If the deployment settings are wrong:

1. Click **Deploy** → **New deployment**
2. Click the **gear icon** (⚙️) next to "Type"
3. Select **Web app**
4. Configure:
   - **Execute as:** `Me`
   - **Who has access:** `Anyone` ← **CRITICAL!**
5. Click **Deploy**
6. **Copy the new deployment URL** - it will have a different ID!

### 4. **Update Android App with New URL (if needed)**

If you created a new deployment with a different URL:
1. Copy the new script ID from the deployment URL
2. Update `ImageRepository.kt` baseUrl
3. Rebuild and test

### 5. **Test the Deployment**

In your browser, test the GET endpoint:
```
https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec?action=getEmployees
```

**Expected:** JSON array of employees (not 401 error)

## 🔧 Alternative: Check Script Permissions

1. In Apps Script, click **Overview** (left sidebar)
2. Check **"Requires authorization"** section
3. Make sure **"Anyone"** is listed under **"Execute as"**

## 📋 Quick Checklist

- [ ] Deployment exists and is active
- [ ] "Who has access" = **"Anyone"** (not "Anyone with Google account")
- [ ] "Execute as" = **"Me"**
- [ ] Script ID in Android app matches deployment URL
- [ ] Test GET endpoint in browser (should return JSON, not 401)
- [ ] Script has `doPost()` function
- [ ] Script has `uploadProfileImage()` function

## ⚠️ Important Notes

1. **Every code change requires redeployment** if you want it to take effect
2. **"Anyone with Google account"** will cause 401 errors for unauthenticated requests
3. **Deployments expire** after a period of inactivity - you may need a new deployment
4. **Test in browser first** with GET request to verify deployment is working

## 🧪 Verification

After fixing deployment:

1. Test GET endpoint in browser
2. Should return JSON, not 401 or HTML
3. Then test upload from Android app
4. Check Logcat - should get 200 OK, not 401

