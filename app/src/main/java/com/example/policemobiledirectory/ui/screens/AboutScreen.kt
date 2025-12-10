package com.example.policemobiledirectory.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.BuildConfig
import com.example.policemobiledirectory.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val versionName = BuildConfig.VERSION_NAME

    // Handle back button to navigate to home screen
    BackHandler {
        // Navigate to home screen, clearing back stack up to (but not including) EMPLOYEE_LIST
        navController.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About App") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 🔹 App Logo
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 App Name
            Text(
                text = "Police Mobile Directory",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 🔹 App Version & Package Info
            Text(
                text = "Version: ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Text(
                text = "Package: ${BuildConfig.APPLICATION_ID}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 App Description
            Text(
                text = """
                    Police Mobile Directory is a simple and secure app designed 
                    to make communication within the police department faster 
                    and easier. It helps officers find and contact personnel 
                    across various stations quickly.
                    
                    Key Features:
                    • Employee Directory with search and filters  
                    • Admin management tools  
                    • Notifications and approvals  
                    • Useful links and document uploads
                    • Nudi Converter: ASCII to Unicode and Unicode to ASCII conversion with PDF and DOCX support
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Developer Info
            Text(
                text = "Developed by",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Ravikumar J",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Clickable Email
            val email = "noreply.policemobiledirectory@gmail.com"
            val annotatedText = buildAnnotatedString {
                append("For feedback or support:\n")
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                ) {
                    append(email)
                }
            }

            Text(
                text = annotatedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(Intent.EXTRA_SUBJECT, "Police Mobile Directory - Feedback")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
