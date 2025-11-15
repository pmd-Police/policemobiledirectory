package com.example.policemobiledirectory.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

/**
 * 🎨 DeleteEmployeeDialog
 *
 * Simple reusable confirmation dialog shown before deleting an employee.
 *
 * ✅ Features:
 * - Clear warning message
 * - Cancel + Delete buttons
 * - Material3 styling
 *
 * 🧩 Usage:
 * ```
 * var showDialog by remember { mutableStateOf(false) }
 *
 * DeleteEmployeeDialog(
 *     showDialog = showDialog,
 *     onDismiss = { showDialog = false },
 *     onConfirm = {
 *         showDialog = false
 *         viewModel.deleteEmployee(employee.kgid, employee.photoUrl)
 *     }
 * )
 * ```
 */
@Composable
fun DeleteEmployeeDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = "Delete Employee",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this employee? " +
                            "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = MaterialTheme.shapes.medium
        )
    }
}
