const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
setGlobalOptions({ region: "asia-south1" });
admin.initializeApp();

/**
 * Trigger when a new document is added to 'pending_registrations'
 * Sends a notification to all admin users.
 */
exports.notifyAdminOfNewRegistration = onDocumentCreated(
  "pending_registrations/{registrationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return null;

    const registrationData = snapshot.data();
    console.log(`👮‍♂️ New registration for: ${registrationData.name}`);

    const db = admin.firestore();

    // 1️⃣ Get admin users
    const adminsSnapshot = await db
      .collection("employees")
      .where("isAdmin", "==", true)
      .get();

    if (adminsSnapshot.empty) {
      console.log("No admin users found. No notifications will be sent.");
      return null;
    }

    // 2️⃣ Collect FCM tokens
    const tokens = [];
    adminsSnapshot.forEach((doc) => {
      const adminUser = doc.data();
      if (adminUser.fcmToken) {
        tokens.push(adminUser.fcmToken);
      }
    });

    if (tokens.length === 0) {
      console.log("No admin users have registered FCM tokens.");
      return null;
    }

    // 3️⃣ Notification payload
    const payload = {
      notification: {
        title: "New User Registration",
        body: `${registrationData.name} has registered and is waiting for approval.`,
        icon: "ic_police_shield_logo",
      },
    };

    // 4️⃣ Send notifications
    try {
      console.log(`Sending notification to ${tokens.length} admin(s)...`);
      const response = await admin.messaging().sendToDevice(tokens, payload);
      console.log("✅ Successfully sent message:", response);
    } catch (error) {
      console.error("❌ Error sending message:", error);
    }

    return null;
  }
);
