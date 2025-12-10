# 🚀 Quick Fix: Deploy Image Upload

## The Problem
You're getting HTML error: "The script completed but did not return anything"

This means `doPost()` is not returning a value.

## ✅ Quick Fix (5 minutes)

### Step 1: Copy Files to Apps Script

1. Go to https://script.google.com
2. Open your project (or create new one)
3. **Delete all existing files** (or rename them)
4. Create **2 new files**:

#### File 1: `Code.gs` (or `EMPLOYEE_SYNC_FINAL.gs`)
Copy the entire contents of `EMPLOYEE_SYNC_FINAL.gs`

#### File 2: `IMAGE_UPLOAD.gs`
Copy the entire contents of `IMAGE_UPLOAD.gs`

### Step 2: Save All Files
- Press `Ctrl+S` (or `Cmd+S` on Mac)
- Make sure both files are saved

### Step 3: Test Function Exists

1. In Apps Script editor, click the function dropdown (top right)
2. Select `uploadProfileImage`
3. Click **Run**
4. If it shows an error, fix it
5. If it says "No function selected" or similar, the function doesn't exist - check the file

### Step 4: Deploy as Web App

1. Click **Deploy** → **New deployment**
2. Click the gear icon ⚙️ (Settings)
3. Select **Web app**
4. Configure:
   - **Description**: "Image Upload API"
   - **Execute as**: `Me (your-email@gmail.com)`
   - **Who has access**: `Anyone` ⚠️ **CRITICAL - MUST BE "Anyone"**
5. Click **Deploy**
6. **Authorize** if prompted
7. **Copy the Web app URL** (looks like: `https://script.google.com/macros/s/AKfycbw.../exec`)

### Step 5: Test the Deployment

Open the Web app URL in a browser. You should see:
```json
{"error":"No parameters. Use ?action=..."}
```

If you see HTML, the deployment is wrong - go back to Step 4.

### Step 6: Update Android App

1. Open `app/src/main/java/com/example/policemobiledirectory/data/remote/GDriveUploadService.kt`
2. Find the `baseUrl` in the Retrofit builder
3. Update it to match your new deployment URL (without `/exec` at the end)
4. Rebuild the app

## 🧪 Test with Browser

Test the upload endpoint directly:

```bash
# Replace YOUR_URL with your deployment URL
curl -X POST "YOUR_URL?action=uploadImage" \
  -H "Content-Type: application/json" \
  -d '{"image":"data:image/jpeg;base64,/9j/4AAQ...","filename":"12345.jpg"}'
```

You should get JSON, not HTML.

## 🔍 Verify in Apps Script

1. Click **Executions** (clock icon) in Apps Script
2. Find your latest execution
3. Click on it to see logs
4. Look for:
   - "=== doPost called ==="
   - "Action: uploadImage"
   - Any error messages

## ⚠️ Common Mistakes

1. **Wrong deployment type**: Must be "Web app", not "API executable"
2. **Wrong access**: Must be "Anyone", not "Only myself"
3. **Old deployment**: Using old URL with old code
4. **Missing file**: `IMAGE_UPLOAD.gs` not in project
5. **Not saved**: Files not saved before deploying

## 📝 Checklist

- [ ] Both files in Apps Script project
- [ ] Both files saved
- [ ] `uploadProfileImage` function exists (check dropdown)
- [ ] Deployed as **Web app**
- [ ] **Who has access** = **"Anyone"**
- [ ] **Execute as** = **"Me"**
- [ ] New deployment created (not just updated)
- [ ] New URL copied
- [ ] Android app URL updated
- [ ] Tested in browser (returns JSON, not HTML)

## 🆘 Still Not Working?

1. **Check Execution Logs**:
   - View → Executions
   - Look for error messages
   - Check if `doPost` is being called

2. **Test with Simple doPost**:
   ```javascript
   function doPost(e) {
     return ContentService.createTextOutput(JSON.stringify({test: "working"}))
       .setMimeType(ContentService.MimeType.JSON);
   }
   ```
   Deploy this and test. If it works, the issue is in `uploadProfileImage`.

3. **Verify Function Name**:
   - In `IMAGE_UPLOAD.gs`, the function must be named exactly `uploadProfileImage`
   - Check for typos

4. **Check File Names**:
   - Files can be named anything (`.gs` extension)
   - But function names must match exactly



