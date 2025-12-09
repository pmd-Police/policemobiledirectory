package com.example.policemobiledirectory

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.policemobiledirectory.navigation.AppNavGraph
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.services.MyFirebaseMessagingService
import com.example.policemobiledirectory.ui.theme.PMDTheme
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EmployeeViewModel by viewModels()
    private var wasLoggedOut = false

    // ✅ Permission launcher for notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("Permission", "POST_NOTIFICATIONS granted = $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ TEST LOG - This should ALWAYS appear when app starts
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        Log.e("TEST_LOG", "🚀🚀🚀 MAINACTIVITY ONCREATE CALLED 🚀🚀🚀")
        Log.e("TEST_LOG", "═══════════════════════════════════════")
        android.util.Log.e("TEST_LOG2", "Android Log test - MainActivity started")
        System.out.println("SYSOUT: MainActivity onCreate called")

        // 🚫 1️⃣ Clean up any leftover anonymous Firebase sessions
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null && (currentUser.isAnonymous || currentUser.email.isNullOrBlank())) {
            Log.w("StartupAuth", "⚠️ Clearing anonymous Firebase session on startup")
            auth.signOut()
        }

        // ✅ 2️⃣ Continue normal setup
        setupContent()
        askNotificationPermission()
        observeUserLoginForFCM()
    }

    /**
     * ✅ Launch Google Sign-In (only called if user selects Google login)
     */
    private suspend fun launchGoogleSignIn() {
        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(!wasLoggedOut)
            .setAutoSelectEnabled(!wasLoggedOut)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result: GetCredentialResponse = credentialManager.getCredential(this, request)
            val credential = result.credential
            val googleIdToken =
                credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
            val email =
                credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID") ?: ""

            if (googleIdToken != null) {
                viewModel.handleGoogleSignIn(email, googleIdToken)
                wasLoggedOut = false
            } else {
                Toast.makeText(this, "No ID token found.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * ✅ Sets up Compose UI with automatic navigation based on login session.
     */
    private fun setupContent() {
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            PMDTheme(darkTheme = isDarkTheme) {

                // ✅ Always start with splash video
                val startDestination = Routes.SPLASH

                // ✅ Define logout action (manual only)
                val logoutAction: () -> Unit = {
                    scope.launch {
                        viewModel.logout {
                            Toast.makeText(
                                this@MainActivity,
                                "Logged out successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            // ✅ Navigate to login only after clearing session
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // ✅ Observe session changes (only after logout/login actions)
                // Use a key to track logout state and prevent auto-navigation after logout
                var lastLoggedInState by remember { mutableStateOf(isLoggedIn) }
                
                LaunchedEffect(isLoggedIn, viewModel.currentUser.collectAsState().value) {
                    val currentUser = viewModel.currentUser.value
                    
                    // ✅ If user just logged out, don't navigate
                    if (lastLoggedInState && !isLoggedIn) {
                        Log.d("MainActivity", "🔒 User logged out, staying on current screen")
                        lastLoggedInState = false
                        return@LaunchedEffect
                    }
                    
                    delay(300) // ⏳ wait for ViewModel to fully restore session

                    // Skip auto-navigation while splash is showing; splash handles routing
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Routes.SPLASH) return@LaunchedEffect

                    if (isLoggedIn && currentUser != null) {
                        // Only navigate if we're not already on EMPLOYEE_LIST
                        if (currentRoute != Routes.EMPLOYEE_LIST) {
                            Log.d("MainActivity", "🏠 Logged in as ${currentUser.name}, navigating to employee list")
                            navController.navigate(Routes.EMPLOYEE_LIST) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                        lastLoggedInState = true
                    } else {
                        Log.d("MainActivity", "🔒 No valid session, staying on login")
                        lastLoggedInState = false
                    }
                }

                // ✅ App Navigation
                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    employeeViewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onGoogleSignInClicked = { scope.launch { launchGoogleSignIn() } },
                    onThemeToggle = { viewModel.toggleTheme() }
                )
            }
        }
    }

    /**
     * ✅ Ask permission for notifications (Android 13+)
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> Log.d("Permission", "Granted")

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * ✅ Sync FCM Token whenever a new user logs in
     */
    private fun observeUserLoginForFCM() {
        var previousUserUid: String? = viewModel.currentUser.value?.firebaseUid
        lifecycleScope.launch {
            viewModel.currentUser.collectLatest { employeeUser ->
                val currentUserUid = employeeUser?.firebaseUid
                if (currentUserUid != null && currentUserUid != previousUserUid) {
                    MyFirebaseMessagingService.syncPendingToken(this@MainActivity)
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        MyFirebaseMessagingService.sendRegistrationToServer(token)
                    }
                }
                previousUserUid = currentUserUid
            }
        }
    }
}
