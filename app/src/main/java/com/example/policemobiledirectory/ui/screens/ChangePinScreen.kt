package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.ui.theme.components.PinVisualTransformation
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Brightness6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    navController: NavController,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userEmail = viewModel.currentUser.collectAsState().value?.email  // ✅ added this

    var currentPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmNewPin by rememberSaveable { mutableStateOf("") }

    var currentPinVisible by rememberSaveable { mutableStateOf(false) }
    var newPinVisible by rememberSaveable { mutableStateOf(false) }
    var confirmNewPinVisible by rememberSaveable { mutableStateOf(false) }

    val pinChangeState by viewModel.pinChangeState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change PIN") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (pinChangeState is OperationStatus.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PinTextField(
                    label = "Current PIN",
                    pin = currentPin,
                    onPinChange = { currentPin = it },
                    visible = currentPinVisible,
                    onVisibilityChange = { currentPinVisible = it }
                )

                PinTextField(
                    label = "New PIN",
                    pin = newPin,
                    onPinChange = { newPin = it },
                    visible = newPinVisible,
                    onVisibilityChange = { newPinVisible = it }
                )

                PinTextField(
                    label = "Confirm New PIN",
                    pin = confirmNewPin,
                    onPinChange = { confirmNewPin = it },
                    visible = confirmNewPinVisible,
                    onVisibilityChange = { confirmNewPinVisible = it }
                )

                Button(
                    onClick = {
                        when {
                            newPin.length < 4 -> {
                                Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                            }
                            newPin != confirmNewPin -> {
                                Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                if (!userEmail.isNullOrBlank()) {
                                    viewModel.changePin(userEmail, currentPin, newPin)
                                } else {
                                    Toast.makeText(context, "Email not found. Please log in again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pinChangeState !is OperationStatus.Loading
                ) {
                    Text("Submit")
                }

                TextButton(onClick = { navController.navigate("forgot_pin") }) {
                    Text("Forgot your current PIN? Reset here.")
                }

                when (pinChangeState) {
                    is OperationStatus.Success -> {
                        Text(
                            text = (pinChangeState as OperationStatus.Success<String>).data,
                            color = Color.Green
                        )
                        LaunchedEffect(Unit) { viewModel.resetPinChangeState() }
                    }
                    is OperationStatus.Error -> {
                        Text(
                            text = (pinChangeState as OperationStatus.Error).message,
                            color = Color.Red
                        )
                        LaunchedEffect(Unit) { viewModel.resetPinChangeState() }
                    }
                    else -> Unit
                }
            }
        }
    }
}


@Composable
fun PinTextField(
    label: String,
    pin: String,
    onPinChange: (String) -> Unit,
    visible: Boolean,
    onVisibilityChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = pin,
        onValueChange = onPinChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PinVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            IconButton(onClick = { onVisibilityChange(!visible) }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) "Hide PIN" else "Show PIN"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
