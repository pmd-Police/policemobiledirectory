package com.example.policemobiledirectory.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFCMService"
        const val PREFS_NAME = "fcm_prefs"
        const val KEY_PENDING_FCM_TOKEN = "fcm_token_pending_sync"

        // Method to be called after successful login from your Activity/ViewModel
        fun syncPendingToken(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingToken = prefs.getString(KEY_PENDING_FCM_TOKEN, null)
            if (pendingToken != null) {
                Log.d(TAG, "Found pending token in Prefs: $pendingToken. Attempting to sync.")
                // Context no longer needed for sendRegistrationToServer itself
                sendRegistrationToServer(pendingToken) {
                    prefs.edit().remove(KEY_PENDING_FCM_TOKEN).apply()
                    Log.d(TAG, "Pending token synced and cleared from Prefs.")
                }
            } else {
                Log.d(TAG, "No pending FCM token found in Prefs to sync.")
            }
        }

        // Context parameter removed
        fun sendRegistrationToServer(token: String?, onSuccess: (() -> Unit)? = null) {
            if (token == null) {
                Log.e(TAG, "FCM token is null, cannot send to server.")
                return
            }

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                Log.d(
                    TAG,
                    "(Static Send) No user logged in, cannot update FCM token on server yet."
                )
                return
            }

            val userId = firebaseUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("employees")
                .whereEqualTo("firebaseUid", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.d(
                            TAG,
                            "(Static Send) No employee document found for UID: $userId to update FCM token."
                        )
                        return@addOnSuccessListener
                    }
                    for (document in documents) {
                        Log.d(
                            TAG,
                            "(Static Send) Updating FCM token for employee document: ${document.id}"
                        )
                        db.collection("employees").document(document.id)
                            .update("fcmToken", token)
                            .addOnSuccessListener {
                                Log.d(
                                    TAG,
                                    "(Static Send) FCM token updated successfully for firebaseUid: $userId"
                                )
                                onSuccess?.invoke() // Invoke callback on success
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    TAG,
                                    "(Static Send) Error updating FCM token for firebaseUid: $userId",
                                    e
                                )
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(
                        TAG,
                        "(Static Send) Error fetching employee document by firebaseUid: $userId",
                        e
                    )
                }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received in onNewToken: $token")

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            Log.d(TAG, "User is logged in. Sending token directly from onNewToken.")
            sendRegistrationToServer(token) // Context no longer needed
        } else {
            Log.d(TAG, "User not logged in. Storing token in SharedPreferences.")
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PENDING_FCM_TOKEN, token).apply()
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "ON_MESSAGE_RECEIVED_RAW_MESSAGE!")
        Log.d(TAG, "Raw message FROM: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Raw message DATA: ${remoteMessage.data}")
        }
        remoteMessage.notification?.let {
            Log.d(TAG, "Raw message NOTIFICATION: title=${it.title}, body=${it.body}")
        }

        val title =
            remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Notification"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"]
        ?: "You have a new message."
        val kgid = remoteMessage.data["kgid"]

        Log.d(TAG, "Notification Title: $title, Body: $body, KGID: $kgid")
        sendNotificationDisplay(title, body, kgid)
    }

    private fun sendNotificationDisplay(title: String?, messageBody: String?, kgid: String?) {
        val channelId = "pending_approval_channel_id"
        val channelName = "Pending Approvals"

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_action", "view_pending_approvals")
            kgid?.let { putExtra("target_kgid", it) }
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, pendingIntentFlags)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(mainPendingIntent)

        // Optional: Add Approve / Reject actions
        kgid?.let {
            val approveIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = "APPROVE"
                putExtra("KGID", kgid)
            }
            val approvePendingIntent =
                PendingIntent.getBroadcast(this, kgid.hashCode(), approveIntent, pendingIntentFlags)
            notificationBuilder.addAction(0, "APPROVE", approvePendingIntent)

            val rejectIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                action = "REJECT"
                putExtra("KGID", kgid)
            }
            val rejectPendingIntent = PendingIntent.getBroadcast(
                this,
                kgid.hashCode() + 1,
                rejectIntent,
                pendingIntentFlags
            )
            notificationBuilder.addAction(0, "REJECT", rejectPendingIntent)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for items pending approval"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = kgid?.hashCode() ?: Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}