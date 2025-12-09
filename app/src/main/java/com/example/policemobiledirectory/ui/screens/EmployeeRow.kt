package com.example.policemobiledirectory.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.utils.IntentUtils
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun EmployeeRow(
    employee: Employee,
    isAdmin: Boolean,
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = EmployeeCardBackground
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Employee Photo with colorful border
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(CardAccentGold, CardAccentSilver)
                                ),
                                shape = CircleShape
                            )
                            .padding(3.dp)
                    ) {
                        AsyncImage(
                            model = employee.photoUrl ?: employee.photoUrlFromGoogle,
                            contentDescription = "Employee Photo",
                            placeholder = painterResource(R.drawable.officer),
                            error = painterResource(R.drawable.officer),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Employee Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.name, 
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "KGID: ${employee.kgid}", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                        Text(
                            text = "Mobile: ${employee.mobile1}", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        )
                        Text(
                            text = "District: ${employee.district}", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = "Station: ${employee.station}", 
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val hasPhoneNumber = !employee.mobile1.isNullOrEmpty()

                    // Call Button
                    IconButton(
                        onClick = { if (hasPhoneNumber) IntentUtils.dial(context, employee.mobile1!!) else Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show() },
                        enabled = hasPhoneNumber,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.Call, 
                            contentDescription = "Call",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // SMS Button
                    IconButton(
                        onClick = { if (hasPhoneNumber) IntentUtils.sendSms(context, employee.mobile1!!) else Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show() },
                        enabled = hasPhoneNumber,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message, 
                            contentDescription = "SMS",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // WhatsApp Button
                    IconButton(
                        onClick = { if (hasPhoneNumber) IntentUtils.openWhatsApp(context, employee.mobile1!!) else Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show() },
                        enabled = hasPhoneNumber,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_whatsapp), 
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isAdmin) {
                        // Edit Button
                        IconButton(
                            onClick = { navController.navigate("edit_employee/${employee.kgid}") },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Filled.Edit, 
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Delete Button
                        IconButton(
                            onClick = {
                                viewModel.deleteEmployee(employee.kgid, employee.photoUrl)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Filled.Delete, 
                                contentDescription = "Delete", 
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
