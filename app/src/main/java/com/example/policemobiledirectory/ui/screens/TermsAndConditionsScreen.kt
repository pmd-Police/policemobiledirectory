package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Employee Consent – Terms & Conditions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "(For Personal/Private App Use)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "By submitting my personal information, documents, photographs, or any related data (\"Information\"), I agree to the following Terms & Conditions:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TermsSection(
                number = "1",
                title = "Voluntary Submission",
                content = "I confirm that all information and documents I provide are true, accurate, and voluntarily submitted for inclusion in the Police Mobile Directory App developed and maintained by the owner of this application."
            )

            TermsSection(
                number = "2",
                title = "Permission to Store & Use My Data",
                content = "I give consent for the app owner to store, process, and use my information only for the following purposes:\n\n• Creating and maintaining the mobile directory\n\n• Providing quick contact and reference details to authorised app users\n\n• Verification and communication related to directory updates\n\n• Internal management and improvement of the application"
            )

            TermsSection(
                number = "3",
                title = "Display of Limited Information",
                content = "I allow my non-sensitive details (such as name, designation, unit/station, office mobile number, and profile photo) to be displayed within the app to authorised users only."
            )

            TermsSection(
                number = "4",
                title = "Secure Handling of Data",
                content = "I understand that the app owner will take reasonable measures to protect my information.\n\nHowever, no electronic or cloud-based storage (Google Sheets / Google Drive / Firebase / App storage) can guarantee 100% security."
            )

            TermsSection(
                number = "5",
                title = "No Unauthorised Sharing",
                content = "My information will not be sold, shared, or disclosed to any external party except:\n\n• When required by law\n\n• For technical processing within trusted platforms (Google Sheets, Drive, Firebase)"
            )

            TermsSection(
                number = "6",
                title = "Right to Update or Request Deletion",
                content = "I may request correction or deletion of my information by contacting the app owner or administrator at any time."
            )

            TermsSection(
                number = "7",
                title = "Revoking Consent",
                content = "I may withdraw my consent at any time.\n\nI understand that withdrawal may result in my data being removed and my details no longer appearing in the directory."
            )

            TermsSection(
                number = "8",
                title = "Use of Uploaded Photos/Documents",
                content = "Any photo or document I upload may be:\n\n• Stored in secure cloud drives (Google Drive / Firebase Storage)\n\n• Linked to the directory\n\n• Used for identification and reference in the app only"
            )

            TermsSection(
                number = "9",
                title = "App Owner's Rights",
                content = "The app owner reserves the right to:\n\n• Verify submitted information\n\n• Correct inaccurate entries\n\n• Remove or modify information to maintain accuracy and app performance\n\n• Restrict access for misuse or false information"
            )

            TermsSection(
                number = "10",
                title = "Acceptance",
                content = "By submitting my data or continuing with the registration/upload process, I acknowledge that I have read, understood, and agree to these Terms & Conditions."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TermsSection(number: String, title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$number.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
























