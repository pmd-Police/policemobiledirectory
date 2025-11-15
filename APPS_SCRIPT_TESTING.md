# Testing Your Apps Script

## ✅ Good News: Your Script is Working!

The `{"error":"Invalid action"}` response means:
- ✅ Your script is deployed correctly
- ✅ It's accessible via the web
- ✅ The `doGet()` function is working
- ✅ It's returning JSON (not HTML)

## 🧪 How to Test

### 1. **Test GET Request (in Browser)**

Test the `getEmployees` action:
```
https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec?action=getEmployees
```

**Expected Result:** JSON array of employees
```json
[
  {
    "kgid": "12345",
    "name": "John Doe",
    ...
  }
]
```

### 2. **Test POST Request (uploadImage)**

You **CANNOT** test POST requests directly in the browser. You need to:

#### Option A: Use Your Android App
1. Upload an image from your profile
2. Check Logcat for the response

#### Option B: Use Postman/curl
```bash
curl -X POST \
  "https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/exec?action=uploadImage" \
  -F "file=@/path/to/image.jpg"
```

**Expected Result:**
```json
{
  "success": true,
  "url": "https://drive.google.com/uc?export=view&id=FILE_ID",
  "id": "FILE_ID",
  "error": null
}
```

### 3. **Check Apps Script Execution Logs**

1. Go to your Apps Script project: https://script.google.com
2. Click **Executions** (clock icon) in the left sidebar
3. Click on recent executions to see logs
4. Look for `Logger.log()` output

## ⚠️ Important: Update Your Android App URL

Make sure your Android app is using the **correct** script URL:

**Current URL in your app:**
```
https://script.google.com/macros/s/AKfycbwdcT20-H47RgFkip4hOpiVukfOmowudIP4lf3GOlTyCUd0BTDkHJtN6HmPwjpmjs0/
```

**New URL (from browser test):**
```
https://script.google.com/macros/s/AKfycbzgajJyErgoetTjD3BsPQe4GWIQOagQ11WrOcCeN9y4NoSj7aBaMoZT91jF2bzxbPqG/
```

If these are different, you need to:
1. Update `ImageRepository.kt` with the new script URL, OR
2. Use the same deployment for both

## 📋 Next Steps

1. ✅ Verify your script has the corrected `uploadProfileImage()` function (with correct response format)
2. ✅ Test `getEmployees` in browser to confirm script works
3. ✅ Update Android app URL if needed
4. ✅ Try uploading from your app again
5. ✅ Check Apps Script execution logs if upload fails

## 🔍 Debugging Tips

If upload still fails:
1. Check Apps Script **Executions** log for errors
2. Look for `Logger.log()` output in execution details
3. Verify the `uploadProfileImage()` function is called
4. Check if blob is created successfully
5. Verify Drive folder permissions

