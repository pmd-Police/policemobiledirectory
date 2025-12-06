# 📊 Google Sheets Configuration

## ✅ Separate Sheets Created

You have created separate Google Sheets for:
1. **Employees** - Employee profiles
2. **Officers** - Read-only officer contacts
3. **Useful Links** - App links/APKs
4. **Documents Links** - Document links

---

## 🔗 Apps Script URLs Configuration

### Current URLs in Android App:

#### 1. **Employees Sheet**
- **URL**: `AKfycbzDEbLQCWoow9-FImWNAyiFDYCTdKZnIaX4tOioIoiEdVKL5LS8TVP8bej-8VjJSgLK/`
- **Location**: `NetworkModule.kt` → `SYNC_BASE_URL`
- **Used for**: Employee sync (Sheet ↔ Firestore)
- **Script**: Employee profile sync script

#### 2. **Officers Sheet**
- **URL**: `AKfycbyYb-m0egcqz69JNbBYQj0Qv8qStnn6GlntPfK47Nj75bN7K3u2onqUaPgvAtPQjH8V/`
- **Location**: `NetworkModule.kt` → `OFFICERS_SYNC_BASE_URL`
- **Used for**: Officers sync (Sheet → Firestore)
- **Script**: Officers sync script (`OFFICERS_APPS_SCRIPT_COMPLETE.js`)
- **Sheet Name**: "Office Profiles"

#### 3. **Useful Links Sheet**
- **URL**: `AKfycbzDEbLQCWoow9-FImWNAyiFDYCTdKZnIaX4tOioIoiEdVKL5LS8TVP8bej-8VjJSgLK/` (needs separate URL)
- **Location**: Currently using same as employees (needs update)
- **Used for**: Useful links management
- **Script**: Needs separate Apps Script

#### 4. **Documents Links Sheet**
- **URL**: Not configured yet
- **Location**: Needs to be added
- **Used for**: Document links management
- **Script**: Needs separate Apps Script

#### 5. **Other URLs Found**:
- **BASE_URL**: `AKfycby-7jOc_naI1_XDVzG1qAGvNc9w3tIU4ZwmCFGUUCLdg0_DEJh7oouF8a9iy5E93-p9zg/` (EmployeeApiService)
- **Constants**: `AKfycbzIW69Yz1BzjVbKD83SpOHqy7KecIG9WQP2DqLrsYJOfPWcVCDEIpNQoia997fV_Jzeng/` (ConstantsRepository)
- **Image Upload**: `AKfycbw3BybPar7IpUPm10nEDlT1UEbYMTiMsDvnxQyv9l3sf916Mk9DuDZcc4u_h8DV7vSI9w/` (ImageRepository)

---

## ⚠️ Action Required

Please provide the Apps Script URLs for:
1. ✅ **Officers** - Already configured
2. ❓ **Useful Links** - Need URL
3. ❓ **Documents Links** - Need URL

---

## 📝 Next Steps

1. **Share the Apps Script URLs** for Useful Links and Documents Links
2. **Update NetworkModule.kt** with separate Retrofit instances for each
3. **Create/Update Apps Scripts** for each sheet with proper sync functions
4. **Test each sync** independently

---

## 🔧 Configuration Template

Once you provide the URLs, I'll update:
- `NetworkModule.kt` - Add separate Retrofit instances
- Create separate API services if needed
- Update repositories to use correct URLs


