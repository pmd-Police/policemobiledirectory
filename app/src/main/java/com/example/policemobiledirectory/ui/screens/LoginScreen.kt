package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.ui.screens.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: EmployeeViewModel,
    onLoginSuccess: (Boolean) -> Unit,
    onRegisterNewUser: (String?) -> Unit,
    onForgotPinClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit,
    onThemeToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current

    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var emailToRegister by remember { mutableStateOf<String?>(null) }
    val authStatus by viewModel.authStatus.collectAsState()
    val googleSignInEvent by viewModel.googleSignInUiEvent.collectAsState()

    // --- STATE OBSERVERS ---

    // Observer for standard PIN-based login
    LaunchedEffect(authStatus) {
        when (val status = authStatus) {
            is OperationStatus.Loading -> isLoading = true
            is OperationStatus.Success<*> -> {
                isLoading = false
                val user = status.data as? Employee
                if (user != null && viewModel.isLoggedIn.value) {
                    Toast.makeText(context, "Welcome ${user.name}", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(viewModel.isAdmin.value)
                }
            }
            is OperationStatus.Error -> {
                isLoading = false
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
            }
            else -> isLoading = false
        }
    }

    // --- Listen for Google Sign-In Events ---
    LaunchedEffect(googleSignInEvent) {
        when (val event = googleSignInEvent) {
            is GoogleSignInUiEvent.Loading -> {
                isLoading = true
            }

            is GoogleSignInUiEvent.SignInSuccess -> {
                isLoading = false
                if (viewModel.isLoggedIn.value) {
                    Toast.makeText(context, "Welcome ${event.user.name}", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(viewModel.isAdmin.value)
                }
            }

            is GoogleSignInUiEvent.RegistrationRequired -> {
                isLoading = false
                emailToRegister = event.email
                showRegisterDialog = true   // ✅ trigger dialog declaratively
            }

            is GoogleSignInUiEvent.Error -> {
                isLoading = false
                Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }

            else -> isLoading = false
        }
    }

// --- Registration Required Dialog ---
    if (showRegisterDialog && emailToRegister != null) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Account Not Registered") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The Google account below isn’t registered. You can register it now or choose another account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = emailToRegister ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    onRegisterNewUser(emailToRegister!!)
                }) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRegisterDialog = false
                    Toast.makeText(
                        context,
                        "Please sign in with a registered account.",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("Use another account")
                }
            }
        )
    }

    // --- UI ---

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.95f
        )

        // Foreground content with padding
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // The main login form UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))
                // Police Mobile Directory title (larger, bright yellow, no box)
                Text(
                    text = "Police Mobile Directory",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = androidx.compose.ui.graphics.Color(0xFFF8D722)
                )
                
                // Disclaimer text with zoom animation and red color
                Spacer(Modifier.height(8.dp))
                val infiniteTransition = rememberInfiniteTransition(label = "zoom")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                Text(
                    text = "This app is exclusively for Karnataka State Police Department personnel.\nIf you are not a member of KSP, please uninstall the PMD app.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color(0xFFFF0000), // Red color
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Spacer(Modifier.height(24.dp))

                // Expandable "Login with email and pin" button
                var isEmailPinExpanded by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEmailPinExpanded = !isEmailPinExpanded },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFE3F2FD) // Light blue background
                    )
                ) {
                    Column {
                        // Button header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Login with email and pin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isEmailPinExpanded) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isEmailPinExpanded) "Collapse" else "Expand"
                            )
                        }
                        
                        // Expandable content
                        AnimatedVisibility(visible = isEmailPinExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Email Input
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it.trim() },
                                    label = { Text("Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    )
                                )

                                // PIN Input
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                                    label = { Text("6-digit PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val icon = if (pinVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                                        IconButton(onClick = { pinVisible = !pinVisible }) {
                                            Icon(icon, contentDescription = "Toggle PIN visibility")
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                            viewModel.loginWithPin(email, pin)
                                        } else {
                                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                )

                                // Login Button
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                            viewModel.loginWithPin(email, pin)
                                        } else {
                                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = email.isNotBlank() && pin.length == 6
                                ) { Text("Login") }

                                // Forgot PIN Link
                                Text(
                                    "Forgot PIN?",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onForgotPinClicked() },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(" or ", modifier = Modifier.padding(horizontal = 8.dp))
                    Divider(modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))

                // Google Sign-In/Register Button
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGoogleSignInClicked()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Sign in with Google/ Register")
                }
                Spacer(Modifier.height(32.dp))
                
                // Developer Information
                Text(
                    text = "Developed By Ravikumar J, AHC, DAR Chikkaballapura",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
