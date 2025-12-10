package com.example.policemobiledirectory

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PoliceMobileDirectoryApp : Application() {

    @Inject
    lateinit var constantsRepository: com.example.policemobiledirectory.repository.ConstantsRepository

    override fun onCreate() {
        super.onCreate()
        
        // 🔐 Verify app signature to prevent tampering (after Hilt injection)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait for Hilt to inject dependencies
                kotlinx.coroutines.delay(1000)
                
                // Get signature verifier via manual injection (since we can't inject in Application onCreate)
                val signatureVerifier = com.example.policemobiledirectory.utils.AppSignatureVerifier(
                    applicationContext,
                    com.example.policemobiledirectory.utils.SecurityConfig(applicationContext)
                )
                
                if (!signatureVerifier.verifySignature()) {
                    Log.e("AppInit", "⚠️ WARNING: App signature verification failed!")
                    // In production, you may want to block app usage or show warning
                    // For now, we log the warning and continue
                } else {
                    Log.d("AppInit", "✅ App signature verified")
                }
            } catch (e: Exception) {
                Log.e("AppInit", "Signature verification error", e)
                // Continue app startup even if verification fails (for development)
            }
        }

        // Initialize PDFBox resources (loads glyphlist, fonts, etc.)
        try {
            PDFBoxResourceLoader.init(applicationContext)
            Log.d("AppInit", "PDFBox resources initialized")
        } catch (e: Exception) {
            Log.e("AppInit", "Failed to initialize PDFBox", e)
        }

        // Configure StAX providers so Apache POI (poi-android) can create DOCX packages on Android.
        try {
            System.setProperty("javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl")
            System.setProperty("javax.xml.stream.XMLInputFactory", "com.fasterxml.aalto.stax.InputFactoryImpl")
            System.setProperty("javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl")
            Log.d("AppInit", "Configured StAX providers for Apache POI")
        } catch (e: Exception) {
            Log.w("AppInit", "Unable to configure StAX providers", e)
        }

        // 🔹 Prevent Firebase from auto-restoring anonymous/guest sessions
        // This ensures clean logout state on app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                // Only sign out if it's an anonymous user or has no email
                if (currentUser != null && (currentUser.isAnonymous || currentUser.email.isNullOrBlank())) {
                    auth.signOut()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 🔹 Initialize constants synchronization
        // Fetch constants on first launch or if cache is expired (>30 days)
        // This runs in background and doesn't block app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait for Hilt to inject dependencies
                // Using a small delay to ensure injection is complete
                kotlinx.coroutines.delay(500)
                
                if (constantsRepository.shouldRefreshCache()) {
                    Log.d("AppInit", "Constants cache expired or missing, refreshing...")
                    val success = constantsRepository.refreshConstants()
                    if (success) {
                        Log.d("AppInit", "✅ Constants refreshed successfully")
                    } else {
                        Log.w("AppInit", "⚠️ Constants refresh failed, using cached/hardcoded values")
                    }
                } else {
                    val age = constantsRepository.getCacheAgeDays()
                    Log.d("AppInit", "Constants cache is fresh (age: ${age} days)")
                }
            } catch (e: Exception) {
                Log.e("AppInit", "Failed to initialize constants sync", e)
                // App continues with hardcoded constants as fallback
            }
        }
    }
}
