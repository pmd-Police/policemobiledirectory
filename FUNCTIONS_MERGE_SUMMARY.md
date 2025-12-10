# Cloud Functions Merge Summary

## Overview
Successfully merged three Cloud Functions folders into a single unified `functions/` folder:
- ✅ `functions/` (original)
- ✅ `pol/` 
- ✅ `pol_functions/`

## Merged Functions

### 1. **sendNotification** (from `pol_functions/`)
- **Trigger**: `onDocumentCreated` on `notifications_queue/{docId}`
- **Purpose**: Sends FCM push notifications to employees based on target criteria (SINGLE, STATION, DISTRICT, ADMIN, ALL)
- **Features**:
  - Batch processing (500 tokens per batch)
  - Invalid token cleanup
  - Status tracking in Firestore

### 2. **sendOtpOnCreate** (from `pol_functions/` + `functions/`)
- **Trigger**: `onDocumentCreated` on `otp_codes/{email}`
- **Purpose**: Sends OTP email when a document is created in `otp_codes` (Option A - secure backend flow)
- **Features**:
  - Checks if employee exists and is approved
  - Sends HTML email with logo
  - Updates status in Firestore

### 3. **sendOtpEmail** (from `pol_functions/`)
- **Trigger**: `onCall` (HTTP callable)
- **Purpose**: Legacy support for sending OTP via HTTP call
- **Features**:
  - HTML email template
  - Stores OTP in `otp_requests` collection

### 4. **verifyOtpEmail** (from `pol_functions/`)
- **Trigger**: `onCall` (HTTP callable)
- **Purpose**: Verifies OTP code for email verification
- **Features**:
  - Checks expiry
  - Prevents reuse
  - Updates status to "used"

### 5. **updateUserPin** (from `pol_functions/`)
- **Trigger**: `onCall` (HTTP callable)
- **Purpose**: Updates user PIN (supports forgot PIN flow)
- **Features**:
  - Validates old PIN (if not forgot flow)
  - Updates PIN hash in Firestore

### 6. **cleanExpiredOtps** (from `pol_functions/`)
- **Trigger**: `onSchedule` (every 1 hour)
- **Purpose**: Cleans up expired OTPs from `otp_requests` collection
- **Features**:
  - Batch deletion
  - Logs cleanup count

### 7. **notifyAdminOfNewRegistration** (from `pol/`)
- **Trigger**: `onDocumentCreated` on `pending_registrations/{registrationId}`
- **Purpose**: Sends push notification to all admin users when a new registration is created
- **Features**:
  - Fetches all admin users
  - Collects FCM tokens
  - Sends multicast notification

## Configuration

### Email Credentials (Priority Order)
1. **Firebase Secrets** (v2): `GMAIL_USER`, `GMAIL_PASS`
2. **OAuth2**: `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`
3. **App Password**: `GMAIL_EMAIL`, `GMAIL_PASSWORD`
4. **Legacy Firebase Config**: `functions.config().gmail.*`

### Dependencies
- `firebase-admin`: ^13.5.0
- `firebase-functions`: ^6.6.0
- `nodemailer`: ^7.0.10
- `dotenv`: ^16.6.1

### Node.js Version
- **Node**: 20

## Firebase Configuration

### `firebase.json` Changes
- Removed `pol/` and `pol_functions/` codebases
- Single `functions/` codebase remains

### Deployment
```bash
cd functions
npm install
firebase deploy --only functions
```

## Next Steps

1. **Install Dependencies**:
   ```bash
   cd functions
   npm install
   ```

2. **Set Secrets** (if using Firebase Secrets):
   ```bash
   firebase functions:secrets:set GMAIL_USER
   firebase functions:secrets:set GMAIL_PASS
   ```

3. **Or Set Environment Variables** (if using .env):
   - Create `.env` file in `functions/` directory
   - Add: `GMAIL_EMAIL`, `GMAIL_PASSWORD`, etc.

4. **Deploy**:
   ```bash
   firebase deploy --only functions
   ```

5. **Clean Up** (after verifying deployment):
   - Delete `pol/` folder (optional)
   - Delete `pol_functions/` folder (optional)

## Notes

- All functions use `asia-south1` region
- Email functions support both secrets and environment variables
- The `sendOtpOnCreate` function is the recommended approach for "Forgot PIN" flow (Option A)
- Legacy `sendOtpEmail` (onCall) is kept for backward compatibility


