package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPinScreen(
    viewModel: EmployeeViewModel,
    onPinResetSuccess: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var showNewPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) } // ✅ For mismatch message

    val otpState by viewModel.otpUiState.collectAsState()
    val verifyOtpState by viewModel.verifyOtpUiState.collectAsState()
    val pinResetState by viewModel.pinResetUiState.collectAsState()
    val remainingMillis by viewModel.remainingTime.collectAsState()

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Forgot PIN", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // --- STEP 1: EMAIL ---
        if (otpState !is OperationStatus.Success) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter your Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.sendOtp(email) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send OTP")
            }

            when (otpState) {
                is OperationStatus.Loading -> CircularProgressIndicator()
                is OperationStatus.Error -> Text(
                    text = (otpState as OperationStatus.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        }

        // --- STEP 2: VERIFY OTP ---
        if (otpState is OperationStatus.Success && verifyOtpState !is OperationStatus.Success) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it },
                label = { Text("Enter OTP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val minutes = (remainingMillis / 1000) / 60
            val seconds = (remainingMillis / 1000) % 60
            val timeDisplay = String.format("%02d:%02d", minutes, seconds)

            if (remainingMillis > 0) {
                Text(
                    text = "OTP expires in $timeDisplay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "OTP expired. Please request a new one.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.verifyOtp(email, otp) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify OTP")
            }

            when (verifyOtpState) {
                is OperationStatus.Loading -> CircularProgressIndicator()
                is OperationStatus.Error -> Text(
                    text = (verifyOtpState as OperationStatus.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        }

        // --- STEP 3: RESET PIN ---
        if (verifyOtpState is OperationStatus.Success) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it },
                label = { Text("New PIN") },
                visualTransformation = if (showNewPin) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPin = !showNewPin }) {
                        Icon(
                            imageVector = if (showNewPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showNewPin) "Hide PIN" else "Show PIN"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { confirmPin = it },
                label = { Text("Confirm PIN") },
                visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                        Icon(
                            imageVector = if (showConfirmPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPin) "Hide PIN" else "Show PIN"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    pinError = null // clear old error
                    if (newPin.isBlank() || confirmPin.isBlank()) {
                        pinError = "Please enter and confirm your new PIN"
                    } else if (newPin != confirmPin) {
                        pinError = "PINs do not match"
                    } else {
                        viewModel.updatePinAfterOtp(email, newPin)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset PIN")
            }

            // ✅ Show mismatch or validation error (local)
            pinError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            when (pinResetState) {
                is OperationStatus.Loading -> CircularProgressIndicator()

                is OperationStatus.Error -> Text(
                    text = (pinResetState as OperationStatus.Error).message,
                    color = MaterialTheme.colorScheme.error
                )

                is OperationStatus.Success -> {
                    Text(
                        text = "✅ PIN reset successfully!",
                        color = MaterialTheme.colorScheme.primary
                    )
                    // ✅ 2-second delay to let user see the message
                    LaunchedEffect(Unit) {
                        scope.launch {
                            delay(2000)
                            onPinResetSuccess()
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
