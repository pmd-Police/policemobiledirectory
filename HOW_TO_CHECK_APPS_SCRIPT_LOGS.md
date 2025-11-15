# How to Check Apps Script Execution Logs

## 🔍 Why Check Logs?

The error "No image data received" means the script is running but can't parse the multipart data. The logs will show:
- What Content-Type is received
- What the boundary is
- How many parts are found
- Whether the file part is detected
- What errors occur during parsing

## 📋 Steps to Check Logs

### 1. **Open Apps Script**
1. Go to: https://script.google.com
2. Find your project
3. Click on it to open

### 2. **View Executions**
1. In the left sidebar, click **Executions** (clock icon ⏱️)
2. You'll see a list of recent executions
3. Find the most recent one (should be from when you tried to upload)

### 3. **View Logs**
1. Click on the execution to expand it
2. You'll see:
   - **Status**: Success, Failed, or Timeout
   - **Duration**: How long it took
   - **Logs**: Click **View logs** to see the detailed logs

### 4. **Look for These Logs**

The updated function logs everything:
```
=== uploadProfileImage START ===
Content-Type: multipart/form-data; boundary=...
Boundary: --...
Parts found: X
✅ Found file part at index X
Filename: 98765.jpg
✅ Extracted kgid: 98765
Header ends at position: XXX
File data length: XXX
✅ Converted to XXX bytes
✅ Blob created: XXX bytes
```

## 🐛 Common Issues to Look For

### Issue 1: "No boundary found"
**Meaning:** Content-Type header is missing or malformed
**Fix:** Check if the request is actually multipart

### Issue 2: "Parts found: 0" or "Parts found: 1"
**Meaning:** Boundary splitting isn't working
**Fix:** The boundary string might not match exactly

### Issue 3: "No file part found"
**Meaning:** The part with `name="file"` isn't found
**Fix:** Check if the part name matches exactly

### Issue 4: "File data length: 0"
**Meaning:** The file data extraction is failing
**Fix:** The header/body separator might be wrong

## 📤 Share the Logs

If you want me to help debug:
1. Copy the logs from the execution
2. Share them with me
3. I'll identify the exact issue

## ⚠️ Important Note

**The logs might contain sensitive data!** Before sharing:
- Remove any file IDs, URLs, or personal information
- Only share the log messages (not the full request/response)

