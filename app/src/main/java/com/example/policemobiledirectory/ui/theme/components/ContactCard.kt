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
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.utils.IntentUtils

/**
 * Unified contact card that works for both Employee and Officer
 */
@Composable
fun ContactCard(
    employee: Employee? = null,
    officer: Officer? = null,
    fontScale: Float = 1.0f,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Use employee if available, otherwise officer
    val name = employee?.name ?: officer?.name ?: ""
    // Use displayRank for employees (includes metal number when applicable), regular rank for officers
    val rank = employee?.displayRank ?: officer?.rank
    val station = employee?.station ?: officer?.station
    val district = employee?.district ?: officer?.district
    val mobileNumber = employee?.mobile1 ?: officer?.mobile
    val landlineNumber = officer?.landline
    val photoUrl = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    val placeholderRes = if (employee != null) R.drawable.officer else R.drawable.ic_officer_building

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
            // 🔹 Blood Group badge in red circle at top right corner of card (for employees only)
            employee?.bloodGroup?.takeIf { it.isNotBlank() }?.let { bg ->
                val formattedBg = if (bg.trim() == "??") {
                    "??"
                } else {
                    bg.uppercase()
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
                        model = photoUrl,
                        contentDescription = "Contact Photo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = com.example.policemobiledirectory.ui.theme.CardShadow,
                                ambientColor = com.example.policemobiledirectory.ui.theme.CardShadow.copy(alpha = 0.5f)
                            ),
                        placeholder = painterResource(placeholderRes),
                        error = painterResource(placeholderRes)
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = (16 * fontScale).sp,
                            color = Color.Black
                        )

                        val rankText = rank ?: ""
                        if (rankText.isNotBlank()) {
                            Text(
                                text = rankText,
                                fontSize = (13 * fontScale).sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black.copy(alpha = 0.9f)
                            )
                        }
                    }

                    if (!station.isNullOrBlank() || !district.isNullOrBlank()) {
                        Text(
                            text = listOfNotNull(station, district)
                                .filter { it.isNotBlank() }
                                .joinToString(", "),
                            fontSize = (13 * fontScale).sp,
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!mobileNumber.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mobileNumber,
                                    color = Color.Black.copy(alpha = 0.9f),
                                    fontSize = (12 * fontScale).sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { IntentUtils.dial(context, mobileNumber) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Call,
                                            contentDescription = "Call mobile",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { IntentUtils.sendSms(context, mobileNumber) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = "SMS",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { IntentUtils.openWhatsApp(context, mobileNumber) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_whatsapp),
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No mobile number",
                                color = Color.Black.copy(alpha = 0.7f),
                                fontSize = (12 * fontScale).sp
                            )
                        }

                        if (!landlineNumber.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = landlineNumber,
                                    color = Color.Black.copy(alpha = 0.9f),
                                    fontSize = (12 * fontScale).sp,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { IntentUtils.dial(context, landlineNumber) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Call,
                                        contentDescription = "Call landline",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

