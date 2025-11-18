package com.example.policemobiledirectory.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.absoluteValue

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

    // ✅ Dynamic gradient colors per employee
    val gradientColors = listOf(
        GradientStartBlue to GradientEndBlue,
        GradientStartGreen to GradientEndBlue,
        GradientStartPurple to GradientEndPurple,
        GradientStartOrange to GradientEndOrange,
        GradientStartTeal to GradientEndTeal
    )
    val colorIndex = (employee.kgid ?: "").hashCode().absoluteValue % gradientColors.size
    val (startColor, endColor) = gradientColors[colorIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(listOf(startColor, endColor))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Profile image
                AsyncImage(
                    model = employee.photoUrl ?: employee.photoUrlFromGoogle,
                    contentDescription = "Employee Photo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(R.drawable.officer),
                    error = painterResource(R.drawable.officer)
                )

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
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(Modifier.width(6.dp))

                        val rankText = employee.displayRank.ifBlank { employee.rank.orEmpty() }
                        if (rankText.isNotBlank()) {
                            Text(
                                text = rankText,
                                fontSize = (13 * fontScale).sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        val bloodText = formatBloodGroup(employee.bloodGroup)
                        if (bloodText.isNotBlank()) {
                            Text(
                                text = bloodText,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = (13 * fontScale).sp
                            )
                        }
                    }

                    // 🧾 KGID visible only for admin
                    if (isAdmin && employee.kgid.isNotBlank()) {
                        Text(
                            text = "KGID: ${employee.kgid}",
                            fontSize = (12 * fontScale).sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }

                    Text(
                        text = listOfNotNull(employee.station, employee.district)
                            .filter { it.isNotBlank() }
                            .joinToString(", "),
                        fontSize = (13 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )

                    Spacer(Modifier.height(1.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = employee.mobile1 ?: "No mobile",
                            color = MaterialTheme.colorScheme.onPrimary,
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
                                tint = MaterialTheme.colorScheme.onPrimary,
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
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onPrimary
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
