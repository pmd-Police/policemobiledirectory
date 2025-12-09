package com.example.policemobiledirectory.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.theme.*

@Composable
fun EmployeeCardAdmin(
    employee: Employee,
    isAdmin: Boolean,
    fontScale: Float,
    navController: NavController,
    onDelete: (Employee) -> Unit,
    context: Context
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = CardShadow,
                ambientColor = CardShadow.copy(alpha = 0.5f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Using custom shadow instead
        shape = RoundedCornerShape(16.dp), // More rounded corners
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = EmployeeCardBackground.copy(alpha = GlassOpacity)
                )
        ) {
            // 🔹 Blood Group badge in red circle at top right corner of card
            val bloodText = formatBloodGroup(employee.bloodGroup)
            if (bloodText.isNotBlank()) {
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
                            text = bloodText,
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
                                spotColor = CardShadow,
                                ambientColor = CardShadow.copy(alpha = 0.5f)
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

                // 🔹 Info section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = employee.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = (16 * fontScale).sp,
                            color = Color.Black
                        )

                        Spacer(Modifier.width(6.dp))

                        val rankText = employee.displayRank.ifBlank { employee.rank.orEmpty() }
                        if (rankText.isNotBlank()) {
                            Text(
                                text = rankText,
                                fontSize = (13 * fontScale).sp,
                                color = Color.Black.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // 🧾 KGID visible only for admin
                    if (isAdmin && employee.kgid.isNotBlank()) {
                        Text(
                            text = "KGID: ${employee.kgid}",
                            fontSize = (12 * fontScale).sp,
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

                    Spacer(Modifier.height(1.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = employee.mobile1 ?: "No mobile",
                            color = Color.Black,
                            fontSize = (13 * fontScale).sp,
                            modifier = Modifier.weight(1f)
                        )

                        // 📞 Call
                        IconButton(
                            onClick = { employee.mobile1?.let { openDialer(context, it) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // 💬 WhatsApp
                        IconButton(
                            onClick = { employee.mobile1?.let { openWhatsApp(context, it) } },
                            modifier = Modifier.size(36.dp)
                        ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp_logo),
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(24.dp)
                        )
                        }

                        // 🛠️ Admin-only actions
                        if (isAdmin) {
                            IconButton(
                                onClick = {
                                    if (employee.kgid.isNotBlank()) {
                                        try {
                                            navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=${employee.kgid}")
                                        } catch (e: Exception) {
                                            android.util.Log.e("EmployeeCardAdmin", "Edit navigation failed: ${e.message}")
                                        }
                                    } else {
                                        android.util.Log.e("EmployeeCardAdmin", "Edit failed: Missing KGID for ${employee.name}")
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Black
                                )
                            }

                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Black
                                )
                            }

                            DeleteEmployeeDialog(
                                showDialog = showDeleteDialog,
                                onDismiss = { showDeleteDialog = false },
                                onConfirm = {
                                    showDeleteDialog = false
                                    onDelete(employee)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 🗑️ Delete confirmation dialog
    DeleteEmployeeDialog(
        showDialog = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            showDeleteDialog = false
            onDelete(employee)
        }
    )
}

/* ===================== HELPERS ===================== */

private fun openDialer(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}

private fun openWhatsApp(context: Context, phone: String) {
    val normalized = phone.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/$normalized")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

private fun formatBloodGroup(value: String?): String {
    if (value.isNullOrBlank()) return ""
    
    // If value is "??" (placeholder for not updated), return it as-is
    if (value.trim() == "??") return "??"
    
    val clean = value.uppercase()
        .replace("POSITIVE", "+")
        .replace("NEGATIVE", "–")
        .replace("VE", "")
        .replace("(", "")
        .replace(")", "")
        .trim()
    return when (clean) {
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
