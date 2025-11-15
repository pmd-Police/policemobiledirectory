package com.example.policemobiledirectory

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class PoliceMobileDirectoryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 🔒 Force logout to prevent anonymous auto-login
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user != null && (user.isAnonymous || user.email.isNullOrEmpty())) {
                    Log.w("AppInit", "⚠️ Anonymous Firebase session found — signing out.")
                    auth.signOut()
                }
            } catch (e: Exception) {
                Log.e("AppInit", "❌ Error during forced sign-out: ${e.message}")
            }
        }
    }
}
