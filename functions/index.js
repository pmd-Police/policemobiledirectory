/* =========================================================
   🔹 Unified PMD Cloud Functions — FINAL STABLE VERSION
   ========================================================= */

/* ---------------------------------------
   1️⃣ Firebase Secret Manager
---------------------------------------- */
const { defineSecret } = require("firebase-functions/params");
const GMAIL_USER_SECRET = defineSecret("GMAIL_USER");
const GMAIL_PASS_SECRET = defineSecret("GMAIL_PASS");

/* ---------------------------------------
   2️⃣ Global Options
---------------------------------------- */
const { setGlobalOptions } = require("firebase-functions/v2");
setGlobalOptions({
  maxInstances: 10,
});

/* ---------------------------------------
   3️⃣ Firebase Functions v2
---------------------------------------- */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { logger } = require("firebase-functions");

/* ---------------------------------------
   4️⃣ Admin SDK
---------------------------------------- */
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

/* ---------------------------------------
   5️⃣ Other libraries
---------------------------------------- */
const nodemailer = require("nodemailer");

/* ---------------------------------------
   6️⃣ Initialize Firebase Admin
---------------------------------------- */
initializeApp();
const db = getFirestore();
const OTP_EXPIRY_MINUTES = 10;

/* ---------------------------------------
   7️⃣ Helper — Generate OTP
---------------------------------------- */
function generateOtp() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/* =========================================================
   🔹 Gmail Transporter (async, safe secrets)
   ========================================================= */
async function getTransporter() {
  const gmailUser = await GMAIL_USER_SECRET.value();
  const gmailPass = await GMAIL_PASS_SECRET.value();

  return nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: gmailUser,
      pass: gmailPass,
    },
  });
}

/* =========================================================
   🔹 Shared HTML Email Template
   ========================================================= */
function otpHtmlTemplate(otp, expiryMinutes) {
  return `
    <div style="font-family: 'Segoe UI', Arial, sans-serif; background:#f4f6fa; padding:30px;">
      <div style="max-width:600px; background:#ffffff; margin:auto; border-radius:12px; padding:30px; box-shadow:0 4px 20px rgba(0,0,0,0.08);">

        <div style="text-align:center; margin-bottom:20px;">
          <img src="cid:pmdLogoCid" alt="PMD Logo" style="width:120px;"/>
        </div>

        <h2 style="text-align:center; color:#0b3d91; margin-top:0;">Police Mobile Directory</h2>

        <hr style="border:none; border-bottom:1px solid #d9d9d9; margin:25px 0;"/>

        <p style="font-size:16px; color:#333;">
          Dear Officer,<br><br>
          Your One-Time Password (OTP) for secure login is:
        </p>

        <div style="
          font-size:36px;
          font-weight:700;
          text-align:center;
          padding:20px;
          background:#eef4ff;
          border:2px solid #0b3d91;
          border-radius:12px;
          color:#0b3d91;
          letter-spacing:4px;
          margin:20px auto;
          width:60%;
        ">
          ${otp}
        </div>

        <p style="font-size:15px; color:#444; text-align:center;">
          This OTP will expire in <strong>${expiryMinutes} minutes</strong>.
        </p>

        <div style="
          background:#fff7e6;
          padding:15px;
          border-left:4px solid #ff9900;
          border-radius:8px;
          font-size:14px;
          color:#7a5a00;
          margin:20px 0;
        ">
          <strong>Security Notice:</strong> Never share your OTP with anyone.
          The Police Mobile Directory team will never ask for your OTP, PIN, or password.
        </div>

        <div style="text-align:center; margin-top:30px; font-size:12px; color:#777;">
          © ${new Date().getFullYear()} Police Mobile Directory<br>
          Karnataka State Police • Digital Services Division
        </div>

      </div>
    </div>
  `;
}

/* =========================================================
   🔹 1) Send Push Notification via FCM
   ========================================================= */
exports.sendNotification = onDocumentCreated(
  { document: "notifications_queue/{docId}", region: "asia-south1" },
  async (event) => {
    const data = event.data.data();
    logger.info("📨 Notification request received:", data);

    try {
      let query = db.collection("employees");

      switch (data.targetType) {
        case "SINGLE":
          query = query.where("kgid", "==", data.targetKgid);
          break;
        case "STATION":
          query = query
            .where("district", "==", data.targetDistrict)
            .where("station", "==", data.targetStation);
          break;
        case "DISTRICT":
          query = query.where("district", "==", data.targetDistrict);
          break;
        case "ADMIN":
          query = query.where("isAdmin", "==", true);
          break;
        case "ALL":
          break;
        default:
          return event.data.ref.update({ status: "invalid_params" });
      }

      const users = await query.get();
      if (users.empty)
        return event.data.ref.update({ status: "no_recipients" });

      const tokens = users.docs
        .map((d) => d.data().fcmToken)
        .filter((t) => t);

      if (!tokens.length)
        return event.data.ref.update({ status: "no_tokens" });

      const response = await getMessaging().sendEachForMulticast({
        tokens,
        notification: { title: data.title, body: data.body },
      });

      await event.data.ref.update({
        status: "processed",
        sentCount: response.successCount,
        failedCount: response.failureCount,
      });
    } catch (error) {
      logger.error("❌ Notification error:", error);
      await event.data.ref.update({
        status: "failed",
        error: error.message,
      });
    }
  }
);

/* =========================================================
   🔹 2) Send OTP on Document Create
   ========================================================= */
exports.sendOtpOnCreate = onDocumentCreated(
  {
    document: "otp_codes/{email}",
    region: "asia-south1",
    secrets: [GMAIL_USER_SECRET, GMAIL_PASS_SECRET],
  },
  async (event) => {
    const email = event.params.email.toLowerCase();
    const otp = event.data.data()?.otp;
    if (!otp) return;

    try {
      const gmailUser = await GMAIL_USER_SECRET.value();
      const transporter = await getTransporter();

      try {
        await transporter.sendMail({
          from: `Police Mobile Directory <${gmailUser}>`,
          to: email,
          subject: "Your One-Time Password (OTP)",
          attachments: [
            {
              filename: "app_logo.png",
              path: "https://firebasestorage.googleapis.com/v0/b/pmd-police-mobile-directory.appspot.com/o/app_logo.png?alt=media",
              cid: "pmdLogoCid",
            },
          ],
          html: otpHtmlTemplate(otp, OTP_EXPIRY_MINUTES),
        });
      } catch (err) {
        logger.warn("Logo fetch failed, retrying without attachment:", err.message);
        await transporter.sendMail({
          from: `Police Mobile Directory <${gmailUser}>`,
          to: email,
          subject: "Your One-Time Password (OTP)",
          html: otpHtmlTemplate(otp, OTP_EXPIRY_MINUTES),
        });
      }

      await event.data.ref.update({ status: "sent" });
    } catch (err) {
      logger.error("❌ OTP email failed:", err);
      await event.data.ref.update({ status: "error", error: err.message });
    }
  }
);

/* =========================================================
   🔹 3) Legacy sendOtpEmail
   ========================================================= */
exports.sendOtpEmail = onCall(
  { region: "asia-south1", secrets: [GMAIL_USER_SECRET, GMAIL_PASS_SECRET] },
  async (request) => {
    try {
      const gmailUser = await GMAIL_USER_SECRET.value();
      const transporter = await getTransporter();

      await transporter.sendMail({
        from: `Police Mobile Directory <${gmailUser}>`,
        to: request.data.email,
        subject: "Your One-Time Password (OTP)",
        attachments: [
          {
            filename: "app_logo.png",
            path: "https://firebasestorage.googleapis.com/v0/b/pmd-police-mobile-directory.appspot.com/o/app_logo.png?alt=media",
            cid: "pmdLogoCid",
          },
        ],
        html: otpHtmlTemplate(request.data.code, OTP_EXPIRY_MINUTES),
      });

      return { success: true };
    } catch (err) {
      return { success: false, message: err.message };
    }
  }
);

/* =========================================================
   🔹 4) requestOtp
   ========================================================= */
exports.requestOtp = onCall(
  { region: "asia-south1", secrets: [GMAIL_USER_SECRET, GMAIL_PASS_SECRET] },
  async (req) => {
    const email = (req.data.email || "").toLowerCase().trim();
    if (!email)
      return { success: false, message: "Email is required" };

    try {
      const snap = await db
        .collection("employees")
        .where("email", "==", email)
        .limit(1)
        .get();

      if (snap.empty)
        return { success: false, message: "No account found with this email" };

      const user = snap.docs[0].data();
      if (!user.isApproved)
        return { success: false, message: "Account not approved yet" };

      const code = generateOtp();
      const expiresAt = new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000);

      await db.collection("otp_requests").doc(email).set({
        email,
        otp: code,
        status: "pending",
        createdAt: new Date(),
        expiresAt,
      });

      const gmailUser = await GMAIL_USER_SECRET.value();
      const transporter = await getTransporter();

      try {
        await transporter.sendMail({
          from: `Police Mobile Directory <${gmailUser}>`,
          to: email,
          subject: "Your One-Time Password (OTP)",
          attachments: [
            {
              filename: "app_logo.png",
              path: "https://firebasestorage.googleapis.com/v0/b/pmd-police-mobile-directory.appspot.com/o/app_logo.png?alt=media",
              cid: "pmdLogoCid",
            },
          ],
          html: otpHtmlTemplate(code, OTP_EXPIRY_MINUTES),
        });
      } catch (err) {
        logger.warn("Logo fetch failed, retrying without attachment:", err.message);
        await transporter.sendMail({
          from: `Police Mobile Directory <${gmailUser}>`,
          to: email,
          subject: "Your One-Time Password (OTP)",
          html: otpHtmlTemplate(code, OTP_EXPIRY_MINUTES),
        });
      }

      return { success: true, message: "OTP sent to your email" };
    } catch (err) {
      logger.error("❌ requestOtp failed:", err);
      return { success: false, message: err.message };
    }
  }
);

/* =========================================================
   🔹 5) verifyOtpEmail
   ========================================================= */
exports.verifyOtpEmail = onCall({ region: "asia-south1" }, async (req) => {
  const { email, code } = req.data || {};
  if (!email || !code) {
    return { success: false, message: "Email and OTP code are required." };
  }

  const normalizedEmail = email.toLowerCase().trim();
  logger.info("verifyOtpEmail: normalizedEmail =", normalizedEmail);

  // Fetch OTP record
  const doc = await db.collection("otp_requests").doc(normalizedEmail).get();
  if (!doc.exists) {
    return { success: false, message: "No OTP found for this email." };
  }

  const data = doc.data();
  if (data.otp !== code) {
    return { success: false, message: "Invalid OTP." };
  }

  // Check expiration
  const expiresAt = data.expiresAt?.toDate ? data.expiresAt.toDate() : data.expiresAt;
  if (expiresAt && expiresAt < new Date()) {
    return { success: false, message: "OTP has expired." };
  }

  /* =========================================================
     🔥 CRITICAL FIX:
     Do NOT trust Firestore formatting. Perform a safe scan.
     This ensures matching even if Firestore email has:
     - trailing spaces
     - leading spaces
     - invisible unicode spaces
     - uppercase letters
  ========================================================= */
  const allEmployees = await db.collection("employees").get();
  let matchedDoc = null;
  allEmployees.forEach((empDoc) => {
    const storedEmail = (empDoc.data().email || "").toLowerCase().trim();
    if (storedEmail === normalizedEmail) {
      matchedDoc = empDoc;
    }
  });

  // If still not found
  if (!matchedDoc) {
    return { success: false, message: "No employee found for this email." };
  }

  const emp = matchedDoc.data();

  // Check approval
  if (!emp.isApproved) {
    return { success: false, message: "Account not approved yet." };
  }

  // Mark OTP used
  await doc.ref.update({
    status: "used",
    usedAt: new Date(),
  });

  // Prepare clean employee object
  const employeePayload = {
    kgid: emp.kgid || matchedDoc.id,
    name: emp.name || "",
    email: emp.email || normalizedEmail,
    pin: emp.pin || "",
    mobile1: emp.mobile1 || "",
    mobile2: emp.mobile2 || "",
    rank: emp.rank || "",
    metalNumber: emp.metalNumber || "",
    district: emp.district || "",
    station: emp.station || "",
    bloodGroup: emp.bloodGroup || "",
    photoUrl: emp.photoUrl || "",
    photoUrlFromGoogle: emp.photoUrlFromGoogle || "",
    fcmToken: emp.fcmToken || "",
    firebaseUid: emp.firebaseUid || "",
    isAdmin: !!emp.isAdmin,
    isApproved: emp.isApproved !== false,
  };

  return {
    success: true,
    message: "OTP verified successfully.",
    employee: employeePayload,
  };
});

/* =========================================================
   🔹 6) updateUserPin
   ========================================================= */
exports.updateUserPin = onCall(
  { region: "asia-south1" },
  async (req) => {
    const { email, newPinHash, oldPinHash, isForgot } = req.data;

    const snap = await db
      .collection("employees")
      .where("email", "==", email.toLowerCase())
      .limit(1)
      .get();

    if (snap.empty)
      return { success: false, message: "User not found" };

    const doc = snap.docs[0];
    const data = doc.data();

    if (!isForgot && data.pin !== oldPinHash)
      return { success: false, message: "Incorrect old PIN" };

    await doc.ref.update({ pin: newPinHash });
    return { success: true };
  }
);

/* =========================================================
   🔹 7) Clean expired OTPs
   ========================================================= */
exports.cleanExpiredOtps = onSchedule(
  { schedule: "every 1 hours", region: "asia-south1" },
  async () => {
    const now = new Date();
    const expired = await db
      .collection("otp_requests")
      .where("expiresAt", "<", now)
      .get();

    const batch = db.batch();
    expired.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }
);

/* =========================================================
   🔹 8) Notify Admin on New Registration
   ========================================================= */
exports.notifyAdminOfNewRegistration = onDocumentCreated(
  { document: "pending_registrations/{id}", region: "asia-south1" },
  async (event) => {
    const data = event.data.data();

    const admins = await db
      .collection("employees")
      .where("isAdmin", "==", true)
      .get();

    const tokens = admins.docs
      .map((d) => d.data().fcmToken)
      .filter(Boolean);

    if (!tokens.length) return;

    await getMessaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "New Registration",
        body: `${data.name} has registered.`,
      },
    });
  }
);
