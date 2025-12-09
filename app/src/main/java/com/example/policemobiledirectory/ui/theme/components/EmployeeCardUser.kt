package com.example.policemobiledirectory.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.utils.IntentUtils

@Composable
fun EmployeeCardUser(
    employee: Employee,
    fontScale: Float,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = com.example.policemobiledirectory.ui.theme.CardShadow,
                ambientColor = com.example.policemobiledirectory.ui.theme.CardShadow.copy(alpha = 0.5f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Using custom shadow instead
        shape = RoundedCornerShape(16.dp), // More rounded corners
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.background(
                color = com.example.policemobiledirectory.ui.theme.EmployeeCardBackground.copy(
                    alpha = com.example.policemobiledirectory.ui.theme.GlassOpacity
                )
            )
        ) {
            // 🔹 Blood Group badge in red circle at top right corner of card
            val bloodGroup = employee.bloodGroup
            if (!bloodGroup.isNullOrBlank()) {
                val formattedBg = if (bloodGroup.trim() == "??") {
                    "??"
                } else {
                    bloodGroup.uppercase()
                        .replace("POSITIVE", "+")
                        .replace("NEGATIVE", "–")
                        .replace("VE", "")
                        .replace("(", "")
                        .replace(")", "")
                        .trim()
                        .let { clean ->
                            when (clean) {
                                "A" -> "A+"
                                "B" -> "B+"
                                "O" -> "O+"
                                "AB" -> "AB+"
                                "A-" -> "A–"
                                "B-" -> "B–"
                                "O-" -> "O–"
                                "AB-" -> "AB–"
                                else -> clean
                            }
                        }
                }
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = formattedBg,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            fontSize = 9.sp
                        )
                    }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Profile image with white border and shadow
                Box {
                    AsyncImage(
                        model = employee.photoUrl ?: employee.photoUrlFromGoogle,
                        contentDescription = "Employee Photo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = com.example.policemobiledirectory.ui.theme.CardShadow,
                                ambientColor = com.example.policemobiledirectory.ui.theme.CardShadow.copy(alpha = 0.5f)
                            ),
                        placeholder = painterResource(R.drawable.officer),
                        error = painterResource(R.drawable.officer)
                    )
                    // White border around avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * fontScale).sp,
                        color = Color.Black
                    )

                    val rankText = employee.displayRank.ifBlank { employee.rank.orEmpty() }
                    if (rankText.isNotBlank()) {
                        Text(
                            text = rankText,
                            fontSize = (13 * fontScale).sp,
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }

                    Text(
                        text = listOfNotNull(employee.station, employee.district)
                            .filter { it.isNotBlank() }
                            .joinToString(", "),
                        fontSize = (13 * fontScale).sp,
                        color = Color.Black.copy(alpha = 0.9f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = employee.mobile1 ?: "No mobile",
                            color = Color.Black.copy(alpha = 0.9f),
                            fontSize = (12 * fontScale).sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Call and Message icons for users
                        val hasPhoneNumber = !employee.mobile1.isNullOrEmpty()
                        
                        // Call Button
                        IconButton(
                            onClick = {
                                if (hasPhoneNumber) {
                                    IntentUtils.dial(context, employee.mobile1!!)
                                } else {
                                    Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasPhoneNumber,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Message Button
                        IconButton(
                            onClick = {
                                if (hasPhoneNumber) {
                                    IntentUtils.sendSms(context, employee.mobile1!!)
                                } else {
                                    Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasPhoneNumber,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = "Message",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // WhatsApp Button
                        IconButton(
                            onClick = {
                                if (hasPhoneNumber) {
                                    IntentUtils.openWhatsApp(context, employee.mobile1!!)
                                } else {
                                    Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasPhoneNumber,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
