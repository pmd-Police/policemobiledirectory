// =========================================================
// 🔹 Imports & Initialization
// =========================================================
require("dotenv").config();

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {onCall} = require("firebase-functions/v2/https");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {logger} = require("firebase-functions");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const functions = require("firebase-functions");

// Initialize Admin SDK
initializeApp();
const db = getFirestore();

// =========================================================
// 🔹 Config: Gmail Credentials (Supports OAuth2 or App Password)
// =========================================================
let gmailEmail = process.env.GMAIL_EMAIL || "";
let gmailPassword = process.env.GMAIL_PASSWORD || "";
let gmailClientId = process.env.GMAIL_CLIENT_ID || "";
let gmailClientSecret = process.env.GMAIL_CLIENT_SECRET || "";
let gmailRefreshToken = process.env.GMAIL_REFRESH_TOKEN || "";

// Fallback to legacy Firebase functions config
try {
  const config = functions.config();
  if (config.gmail) {
    gmailEmail = gmailEmail || config.gmail.email;
    gmailPassword = gmailPassword || config.gmail.password;
    gmailClientId = gmailClientId || config.gmail.client_id;
    gmailClientSecret = gmailClientSecret || config.gmail.client_secret;
    gmailRefreshToken = gmailRefreshToken || config.gmail.refresh_token;
  }
} catch (e) {
  logger.warn("⚠️ No Firebase config() found. Using env vars if any.");
}

if (!gmailEmail) logger.error("❌ Missing Gmail account. Emails will fail.");

// =========================================================
// 🔹 Nodemailer Transporter (App Password or OAuth2)
// =========================================================
let transporter;

if (gmailClientId && gmailClientSecret && gmailRefreshToken) {
  transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {
      type: "OAuth2",
      user: gmailEmail,
      clientId: gmailClientId,
      clientSecret: gmailClientSecret,
      refreshToken: gmailRefreshToken,
    },
  });
  logger.info("📧 Using Gmail OAuth2 for email sending");
} else if (gmailPassword) {
  transporter = nodemailer.createTransport({
    service: "gmail",
    auth: {user: gmailEmail, pass: gmailPassword},
  });
  logger.info("📧 Using Gmail App Password for email sending");
} else {
  logger.error("❌ No valid Gmail credentials configured. Emails will be skipped.");
}

// =========================================================
// 🔹 Function 1: Send Notification via FCM
// =========================================================
exports.sendNotification = onDocumentCreated(
    {document: "notifications_queue/{docId}", region: "asia-south1"},
    async (event) => {
      const {docId} = event.params;
      const data = event.data.data();
      const {
        title,
        body,
        targetType,
        targetKgid,
        targetStation,
        targetDistrict,
        timestamp,
        requesterKgid,
      } = data;

      logger.info("📨 New notification request:", {
        docId,
        targetType,
        targetKgid,
        targetDistrict,
        targetStation,
      });

      try {
        let query = db.collection("employees");

        // ✅ Fixed: Match Android app's NotificationTarget enum values
        // Android uses: ALL, SINGLE, DISTRICT, STATION, ADMIN
        if (targetType === "SINGLE" && targetKgid) {
          // Send to specific user by KGID
          query = query.where("kgid", "==", targetKgid);
          logger.info(`🎯 Targeting SINGLE user: ${targetKgid}`);
        } else if (targetType === "STATION" && targetDistrict && targetStation) {
          // Send to users in specific station (requires both district and station)
          query = query
              .where("district", "==", targetDistrict)
              .where("station", "==", targetStation);
          logger.info(`🎯 Targeting STATION: ${targetDistrict} - ${targetStation}`);
        } else if (targetType === "DISTRICT" && targetDistrict) {
          // Send to all users in a district
          query = query.where("district", "==", targetDistrict);
          logger.info(`🎯 Targeting DISTRICT: ${targetDistrict}`);
        } else if (targetType === "ADMIN") {
          // Send to all admin users
          query = query.where("isAdmin", "==", true);
          logger.info("🎯 Targeting ADMIN users");
        } else if (targetType === "ALL") {
          // Send to all users (no filter needed)
          logger.info("🎯 Targeting ALL users");
        } else {
          logger.warn(
              `⚠️ Invalid or incomplete target parameters: targetType=${targetType}, ` +
              `targetKgid=${targetKgid}, targetDistrict=${targetDistrict}, targetStation=${targetStation}`
          );
          await db.collection("notifications_queue").doc(docId).update({
            status: "invalid_params",
            error: "Invalid or incomplete target parameters",
          });
          return;
        }

        const querySnapshot = await query.get();

        if (querySnapshot.empty) {
          logger.warn("⚠️ No employees found matching criteria. Skipping notification.");
          await db.collection("notifications_queue").doc(docId).update({
            status: "no_recipients",
          });
          return;
        }

        // Extract FCM tokens and filter out invalid ones
        const tokens = querySnapshot.docs
            .map((d) => {
              const empData = d.data();
              return empData.fcmToken;
            })
            .filter((token) => token && typeof token === "string" && token.length > 0);

        if (tokens.length === 0) {
          logger.warn("⚠️ No valid FCM tokens found. Skipping notification.");
          await db.collection("notifications_queue").doc(docId).update({
            status: "no_tokens",
            recipientCount: querySnapshot.size,
          });
          return;
        }

        logger.info(`📱 Found ${tokens.length} valid FCM tokens out of ${querySnapshot.size} employees`);

        // ✅ Send notifications in batches (FCM limit: 500 per batch)
        const BATCH_SIZE = 500;
        let totalSuccess = 0;
        let totalFailure = 0;

        for (let i = 0; i < tokens.length; i += BATCH_SIZE) {
          const batchTokens = tokens.slice(i, i + BATCH_SIZE);

          const message = {
            notification: {
              title: title || "Notification",
              body: body || "You have a new message.",
            },
            data: {
              targetType: targetType || "ALL",
              timestamp: (timestamp || Date.now()).toString(),
              requesterKgid: requesterKgid || "unknown",
            },
            android: {
              priority: "high",
              notification: {
                sound: "default",
                channelId: "default_notification_channel",
              },
            },
          };

          try {
            const response = await getMessaging().sendEachForMulticast({
              tokens: batchTokens,
              ...message,
            });

            totalSuccess += response.successCount;
            totalFailure += response.failureCount;

            // Clean up invalid tokens
            response.responses.forEach((res, idx) => {
              if (
                !res.success &&
                res.error &&
                (res.error.code === "messaging/registration-token-not-registered" ||
                 res.error.code === "messaging/invalid-registration-token")
              ) {
                const badToken = batchTokens[idx];
                // Remove invalid token from employee document
                db.collection("employees")
                    .where("fcmToken", "==", badToken)
                    .get()
                    .then((qs) => {
                      const batch = db.batch();
                      qs.forEach((doc) => {
                        batch.update(doc.ref, {fcmToken: admin.firestore.FieldValue.delete()});
                      });
                      return batch.commit();
                    })
                    .catch((err) => {
                      logger.error(`❌ Failed to remove invalid token: ${err.message}`);
                    });
              }
            });

            logger.info(
                `📦 Batch ${Math.floor(i / BATCH_SIZE) + 1}: ` +
                `Success=${response.successCount}, Failure=${response.failureCount}`
            );
          } catch (batchError) {
            logger.error(`❌ Batch ${Math.floor(i / BATCH_SIZE) + 1} failed:`, batchError);
            totalFailure += batchTokens.length;
          }
        }

        // Update document with final status
        await db.collection("notifications_queue").doc(docId).update({
          status: totalFailure === 0 ? "processed" : "partial_success",
          sentCount: totalSuccess,
          failedCount: totalFailure,
          totalRecipients: querySnapshot.size,
          processedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        logger.info(
            `✅ Notification processing completed: ` +
            `Success=${totalSuccess}, Failure=${totalFailure}, Total=${tokens.length}`
        );
      } catch (error) {
        logger.error("❌ Error sending notification:", error);
        await db.collection("notifications_queue").doc(docId).update({
          status: "failed",
          error: error.message,
          failedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    }
);

// =========================================================
// 🔹 Function 2: Send OTP Email
// =========================================================
exports.sendOtpEmail = onCall({region: "asia-south1"}, async (request) => {
  const {email, code} = request.data || {};
  const logoUrl =
      "https://firebasestorage.googleapis.com/v0/b/pmd-police-mobile-directory.firebasestorage.app/o/public%2Fapp_logo.png?alt=media";

  if (!email || !code) {
    return {success: false, message: "Email and code are required."};
  }

  if (!transporter) {
    return {success: false, message: "Email credentials missing on server."};
  }

  const mailOptions = {
    from: `Police Mobile Directory <${gmailEmail}>`,
    to: email,
    subject: "Your OTP Code",
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto; border: 1px solid #ddd; border-radius: 8px; padding: 20px;">
        <div style="text-align: center; margin-bottom: 20px;">
          <img src="${logoUrl}" alt="Police Mobile Directory Logo" style="width: 120px; height: auto;" />
        </div>
        <h2 style="text-align: center; color: #333;">Your Verification Code</h2>
        <p style="font-size: 16px; color: #555;">Dear User,</p>
        <p style="font-size: 16px; color: #555;">
          Your one-time password (OTP) for verification is:
        </p>
        <div style="text-align: center; margin: 20px 0;">
          <span style="display: inline-block; background-color: #004aad; color: #fff; padding: 10px 20px; font-size: 24px; font-weight: bold; border-radius: 6px;">
            ${code}
          </span>
        </div>
        <p style="font-size: 14px; color: #777;">
          This code will expire in <strong>5 minutes</strong>. Please do not share it with anyone.
        </p>
        <p style="text-align: center; font-size: 12px; color: #aaa; margin-top: 30px;">
          © ${new Date().getFullYear()} Police Mobile Directory
        </p>
      </div>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    logger.info(`✅ OTP email sent to: ${email}`);

    const expiresAt = new Date(Date.now() + 5 * 60 * 1000);
    await db.collection("otp_requests").doc(email).set({
      email,
      otp: code,
      status: "pending",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt,
    });

    return {
      success: true,
      message: `OTP email sent successfully to ${email}`,
    };
  } catch (error) {
    logger.error("❌ sendOtpEmail failed:", error);
    return {
      success: false,
      message: `Failed to send verification code: ${error.message}`,
    };
  }
});

// =========================================================
// 🔹 Function 3: Cleanup Expired OTPs (Optional if using TTL index)
// =========================================================
exports.cleanExpiredOtps = onSchedule(
    {schedule: "every 1 hours", region: "asia-south1"},
    async () => {
      const now = new Date();
      const snapshot = await db
          .collection("otp_requests")
          .where("expiresAt", "<", now)
          .get();

      if (snapshot.empty) {
        logger.info("🧹 No expired OTPs to clean.");
        return;
      }

      const batch = db.batch();
      snapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();

      logger.info(`✅ Cleaned ${snapshot.size} expired OTPs.`);
    }
);

// =========================================================
// 🔹 Function 4: Verify OTP Email
// =========================================================
exports.verifyOtpEmail = onCall({region: "asia-south1"}, async (request) => {
  const {email, code} = request.data || {};

  if (!email || !code) {
    return {success: false, message: "Email and code are required."};
  }

  try {
    const otpRef = db.collection("otp_requests").doc(email);
    const doc = await otpRef.get();

    if (!doc.exists) {
      return {success: false, message: "No OTP found for this email."};
    }

    const otpData = doc.data();
    const now = new Date();

    // 🧩 Check if OTP already used
    if (otpData.status === "used") {
      return {success: false, message: "OTP already used."};
    }

    // ⏰ Check expiry
    if (otpData.expiresAt.toDate() < now) {
      return {success: false, message: "OTP has expired."};
    }

    // ❌ Wrong OTP
    if (otpData.otp !== code) {
      return {success: false, message: "Invalid OTP code."};
    }

    // ✅ OTP correct — mark as used
    await otpRef.update({
      status: "used",
      verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {success: true, message: "OTP verified successfully."};
  } catch (error) {
    logger.error("❌ verifyOtpEmail failed:", error);
    return {success: false, message: `Verification failed: ${error.message}`};
  }
});

// =========================================================
// 🔹 Function 5: Update User PIN
// =========================================================
exports.updateUserPin = onCall({region: "asia-south1"}, async (request) => {
  const {email, newPinHash, oldPinHash, isForgot} = request.data || {};

  logger.info("🟢 updateUserPin called with:", {email, isForgot});

  if (!email || !newPinHash) {
    return {success: false, message: "Email and newPinHash are required."};
  }

  try {
    const normalizedEmail = email.trim().toLowerCase();
    const empRef = db.collection("employees").where("email", "==", normalizedEmail).limit(1);
    const snap = await empRef.get();

    if (snap.empty) {
      logger.warn(`❌ No employee found for email: ${normalizedEmail}`);
      return {success: false, message: "Employee not found."};
    }

    const doc = snap.docs[0];
    const empData = doc.data();

    if (!isForgot && oldPinHash) {
      if (empData.pin !== oldPinHash) {
        logger.warn(`⚠️ Incorrect old PIN for ${normalizedEmail}`);
        return {success: false, message: "Incorrect old PIN."};
      }
    }

    await doc.ref.update({
      pin: newPinHash,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    logger.info(`✅ PIN updated for ${normalizedEmail} (isForgot=${isForgot})`);
    return {success: true, message: "PIN updated successfully."};
  } catch (error) {
    logger.error("❌ updateUserPin failed:", error);
    return {success: false, message: `Failed to update PIN: ${error.message}`};
  }
});
