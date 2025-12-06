package com.example.policemobiledirectory.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * CommonEmployeeForm
 *
 * - isAdmin: admin add/edit
 * - isSelfEdit: user editing own profile
 * - isRegistration: registration mode (shows PIN/terms)
 *
 * Callbacks:
 * - onSubmit(employee, photoUri) for admin/self-edit
 * - onRegisterSubmit(pendingEntity, photoUri) for registration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonEmployeeForm(
    isAdmin: Boolean,
    isSelfEdit: Boolean,
    isRegistration: Boolean,
    initialEmployee: Employee? = null,
    initialKgid: String? = null,
    initialEmail: String = "", // ✅ Add initialEmail parameter for prefilling
    onSubmit: (Employee, Uri?) -> Unit,
    onRegisterSubmit: ((PendingRegistrationEntity, Uri?) -> Unit)? = null,
    isLoading: Boolean = false, // ✅ Add loading state parameter
    onNavigateToTerms: (() -> Unit)? = null, // ✅ Callback to navigate to terms
    constantsViewModel: ConstantsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) } // ✅ Track submission state

    // Get constants from ViewModel
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val bloodGroups by constantsViewModel.bloodGroups.collectAsStateWithLifecycle()
    val ranksRequiringMetalNumber by constantsViewModel.ranksRequiringMetalNumber.collectAsStateWithLifecycle()

    // fields
    var kgid by remember(initialEmployee, initialKgid) { mutableStateOf(initialEmployee?.kgid ?: initialKgid.orEmpty()) }
    var name by remember(initialEmployee) { mutableStateOf(initialEmployee?.name ?: "") }
    // ✅ Use initialEmail if provided, otherwise use initialEmployee.email
    var email by remember(initialEmployee, initialEmail) { 
        mutableStateOf(initialEmployee?.email ?: initialEmail) 
    }
    var mobile1 by remember(initialEmployee) { mutableStateOf(initialEmployee?.mobile1 ?: "") }
    var mobile2 by remember(initialEmployee) { mutableStateOf(initialEmployee?.mobile2 ?: "") }
    var rank by remember(initialEmployee) { mutableStateOf(initialEmployee?.rank ?: "") }
    var metalNumber by remember(initialEmployee) { mutableStateOf(initialEmployee?.metalNumber ?: "") }
    var district by remember(initialEmployee) { mutableStateOf(initialEmployee?.district ?: "") }
    var station by remember(initialEmployee) { mutableStateOf(initialEmployee?.station ?: "") }
    var bloodGroup by remember(initialEmployee) { mutableStateOf(initialEmployee?.bloodGroup ?: "") }
    var currentPhotoUrl by remember(initialEmployee) { mutableStateOf(initialEmployee?.photoUrl) }
    var croppedPhotoUri by remember(initialEmployee) { mutableStateOf<Uri?>(null) }

    // registration extras
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    // UI states
    var rankExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var bloodGroupExpanded by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }

    val showMetalNumberField = remember(rank, ranksRequiringMetalNumber) { ranksRequiringMetalNumber.contains(rank) }
    val stationsForSelectedDistrict = remember(district, stationsByDistrict) {
        if (district.isNotBlank()) {
            // Try exact match first
            stationsByDistrict[district] 
                ?: stationsByDistrict.keys.find { it.equals(district, ignoreCase = true) }?.let { stationsByDistrict[it] }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    // UCrop launcher
    val uCropResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            UCrop.getOutput(result.data!!)?.let { croppedPhotoUri = it }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val err = UCrop.getError(result.data!!)
            Toast.makeText(context, "Crop error: ${err?.message}", Toast.LENGTH_SHORT).show()
        } else {
            Unit
        }
    }

    // camera preview -> cache -> uCrop
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let {
            val src = saveBitmapToCacheAndGetUri(context, it)
            if (src != null) launchUCrop(context, src, uCropResultLauncher)
            else Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    // gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { launchUCrop(context, it, uCropResultLauncher) }
    }

    // validators
    fun isValidEmail(v: String) = v.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(v).matches()
    fun isValidMobile(v: String) = v.filter { it.isDigit() }.length in 10..13
    fun isKgidValid(v: String) = v.isNotBlank()

    val fieldSpacing = 6.dp
    val sectionSpacing = 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // photo
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            when {
                croppedPhotoUri != null -> Image(
                    painter = rememberAsyncImagePainter(croppedPhotoUri),
                    contentDescription = "Selected",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                !currentPhotoUrl.isNullOrBlank() -> Image(
                    painter = rememberAsyncImagePainter(currentPhotoUrl),
                    contentDescription = "Existing",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                else -> Text("Tap to select")
            }
        }

        Spacer(Modifier.height(sectionSpacing))

        // Registration form custom order
        if (isRegistration) {
            // Row 2: Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name*") },
                modifier = Modifier.fillMaxWidth(),
                isError = showValidationErrors && name.isBlank()
            )
            if (showValidationErrors && name.isBlank()) {
                Text("Name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 3: Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = showValidationErrors && !isValidEmail(email)
            )
            if (showValidationErrors && !isValidEmail(email)) {
                Text("Enter valid email", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 4: Mobile 1
            OutlinedTextField(
                value = mobile1,
                onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile 1*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = showValidationErrors && !isValidMobile(mobile1)
            )
            if (showValidationErrors && !isValidMobile(mobile1)) {
                Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 5: Mobile 2
            OutlinedTextField(
                value = mobile2,
                onValueChange = { mobile2 = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile 2 (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(Modifier.height(fieldSpacing))

            // Row 6: KGID, Rank, Metal Number (conditional) - all in same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KGID
                OutlinedTextField(
                    value = kgid,
                    onValueChange = { kgid = it.filter { ch -> ch.isDigit() } },
                    label = { Text("KGID*") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && !isKgidValid(kgid),
                    enabled = isAdmin || isRegistration
                )

                // Rank
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = rank.ifEmpty { "Rank*" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank*") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        isError = showValidationErrors && rank.isBlank()
                    )
                    ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                        ranks.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                rank = selection
                                if (!ranksRequiringMetalNumber.contains(selection)) metalNumber = ""
                                rankExpanded = false
                            })
                        }
                    }
                }

                // Metal Number (conditional - only show when required)
                if (showMetalNumberField) {
                    OutlinedTextField(
                        value = metalNumber,
                        onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Metal No.*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showValidationErrors && metalNumber.isBlank()
                    )
                }
            }
            // Error messages below the row
            if (showValidationErrors) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!isKgidValid(kgid)) {
                        Text("KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (rank.isBlank()) {
                        Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (showMetalNumberField && metalNumber.isBlank()) {
                        Text("Metal no. required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 7: District
            ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = {
                if (!isSelfEdit) districtExpanded = !districtExpanded
            }) {
                OutlinedTextField(
                    value = district.ifEmpty { "Select District" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District*") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    isError = showValidationErrors && district.isBlank()
                )
                if (!isSelfEdit) {
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        districts.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                if (district != selection) station = ""
                                district = selection
                                districtExpanded = false
                            })
                        }
                    }
                }
            }
            if (showValidationErrors && district.isBlank()) {
                Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 8: Station, Blood (on same row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Station
                ExposedDropdownMenuBox(
                    expanded = stationExpanded,
                    onExpandedChange = {
                        if (district.isNotBlank() && stationsForSelectedDistrict.isNotEmpty()) stationExpanded = !stationExpanded
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = station.ifEmpty { if (district.isNotBlank()) "Select Station" else "Select District First" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Station/Unit*") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                        enabled = district.isNotBlank() && stationsForSelectedDistrict.isNotEmpty(),
                        isError = showValidationErrors && station.isBlank()
                    )
                    ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                        stationsForSelectedDistrict.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                station = selection
                                stationExpanded = false
                            })
                        }
                    }
                }

                // Blood Group
                ExposedDropdownMenuBox(
                    expanded = bloodGroupExpanded,
                    onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = bloodGroup.ifEmpty { "Blood Group" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood Group") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) }
                    )
                    ExposedDropdownMenu(expanded = bloodGroupExpanded, onDismissRequest = { bloodGroupExpanded = false }) {
                        bloodGroups.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                bloodGroup = selection
                                bloodGroupExpanded = false
                            })
                        }
                    }
                }
            }
            if (showValidationErrors && station.isBlank()) {
                Text("Station required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 9: Create PIN, Confirm PIN (on same row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Create PIN*") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = showValidationErrors && (pin.length != 6 || (pin.isNotEmpty() && pin != confirmPin))
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("Confirm PIN*") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = showValidationErrors && (confirmPin.length != 6 || (confirmPin.isNotEmpty() && pin != confirmPin))
                )
            }
            if (showValidationErrors && (pin.length != 6 || pin != confirmPin)) {
                Text("PIN must be 6 digits and match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 10: Terms and condition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("I accept ")
                    Text(
                        text = "Terms & Conditions",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable {
                            onNavigateToTerms?.invoke()
                        }
                    )
                }
            }
            Spacer(Modifier.height(sectionSpacing))
        } else {
            // Non-registration form (admin/self-edit) - keep original order
            // KGID (admin & registration only)
            if (!isSelfEdit) {
                OutlinedTextField(
                    value = kgid,
                    onValueChange = { kgid = it.filter { ch -> ch.isDigit() } },
                    label = { Text("KGID*") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && !isKgidValid(kgid),
                    enabled = isAdmin || isRegistration
                )
                if (showValidationErrors && !isKgidValid(kgid)) {
                    Text("KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // Name
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name*") }, modifier = Modifier.fillMaxWidth(), isError = showValidationErrors && name.isBlank())
            Spacer(Modifier.height(fieldSpacing))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = showValidationErrors && !isValidEmail(email)
            )
            if (showValidationErrors && !isValidEmail(email)) Text("Enter valid email", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Mobile1
            OutlinedTextField(
                value = mobile1,
                onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile 1*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = showValidationErrors && !isValidMobile(mobile1)
            )
            if (showValidationErrors && !isValidMobile(mobile1)) Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Mobile2
            OutlinedTextField(value = mobile2, onValueChange = { mobile2 = it.filter { ch -> ch.isDigit() } }, label = { Text("Mobile 2 (Optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(Modifier.height(fieldSpacing))

            // Rank
            ExposedDropdownMenuBox(expanded = rankExpanded, onExpandedChange = { rankExpanded = !rankExpanded }) {
                OutlinedTextField(
                    value = rank.ifEmpty { "Select Rank" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rank*") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    isError = showValidationErrors && rank.isBlank()
                )
                ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                    ranks.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            rank = selection
                            if (!ranksRequiringMetalNumber.contains(selection)) metalNumber = ""
                            rankExpanded = false
                        })
                    }
                }
            }
            if (showValidationErrors && rank.isBlank()) Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Metal number
            if (showMetalNumberField) {
                OutlinedTextField(
                    value = metalNumber,
                    onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Metal Number${if (isRegistration) "*" else ""}") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && metalNumber.isBlank()
                )
                if (showValidationErrors && metalNumber.isBlank()) Text("Metal number required for this rank", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(fieldSpacing))
            }

            // District (admin & registration editable; self-edit disabled)
            ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = {
                if (!isSelfEdit) districtExpanded = !districtExpanded
            }) {
                OutlinedTextField(
                    value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District*") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    isError = showValidationErrors && district.isBlank()
                )
                if (!isSelfEdit) {
                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                        districts.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                if (district != selection) station = ""
                                district = selection
                                districtExpanded = false
                            })
                        }
                    }
                }
            }
            if (showValidationErrors && district.isBlank()) Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Station (editable for admin and self-edit; uses district)
            ExposedDropdownMenuBox(expanded = stationExpanded, onExpandedChange = {
                if (district.isNotBlank() && stationsForSelectedDistrict.isNotEmpty()) stationExpanded = !stationExpanded
            }) {
                OutlinedTextField(
                    value = station.ifEmpty { if (district.isNotBlank()) "Select Station" else "Select District First" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Station/Unit*") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                    enabled = district.isNotBlank() && stationsForSelectedDistrict.isNotEmpty(),
                    isError = showValidationErrors && station.isBlank()
                )
                ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                    stationsForSelectedDistrict.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            station = selection
                            stationExpanded = false
                        })
                    }
                }
            }
            if (showValidationErrors && station.isBlank()) Text("Station required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Blood group
            ExposedDropdownMenuBox(expanded = bloodGroupExpanded, onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded }) {
                OutlinedTextField(value = bloodGroup.ifEmpty { "Select Blood Group" }, onValueChange = {}, readOnly = true, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) })
                ExposedDropdownMenu(expanded = bloodGroupExpanded, onDismissRequest = { bloodGroupExpanded = false }) {
                    bloodGroups.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            bloodGroup = selection
                            bloodGroupExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(sectionSpacing))
        }

        // Submit
        Button(
            onClick = {
                android.util.Log.d("CommonEmployeeForm", "🔵🔵🔵 BUTTON CLICKED 🔵🔵🔵")
                android.util.Log.d("CommonEmployeeForm", "isSubmitting: $isSubmitting, isLoading: $isLoading")
                android.util.Log.d("CommonEmployeeForm", "hasPhoto: ${croppedPhotoUri != null}, photoUri: $croppedPhotoUri")
                
                // ✅ Prevent duplicate submissions
                if (isSubmitting || isLoading) {
                    android.util.Log.d("CommonEmployeeForm", "⚠️ Already submitting, returning early")
                    Toast.makeText(context, "Please wait, submission in progress...", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                android.util.Log.d("CommonEmployeeForm", "✅ Starting validation...")
                showValidationErrors = true
                
                // Basic validation
                if (!isValidEmail(email) || !isValidMobile(mobile1) || name.isBlank()) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                // KGID validation (required for admin add and registration, optional for self-edit)
                if (!isSelfEdit && kgid.isBlank()) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                // Registration-specific validation
                if (isRegistration) {
                    // Validate rank
                    if (rank.isBlank()) {
                        Toast.makeText(context, "Rank is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate metal number if required for selected rank
                    if (showMetalNumberField && metalNumber.isBlank()) {
                        Toast.makeText(context, "Metal number is required for this rank", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate district
                    if (district.isBlank()) {
                        Toast.makeText(context, "District is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate station
                    if (station.isBlank()) {
                        Toast.makeText(context, "Station is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate PIN
                    if (pin.length != 6 || pin != confirmPin) {
                        Toast.makeText(context, "PIN mismatch or invalid", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate terms acceptance
                    if (!acceptedTerms) {
                        Toast.makeText(context, "Accept terms to continue", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                }

                // ✅ Set submitting state to prevent duplicate clicks
                android.util.Log.d("CommonEmployeeForm", "✅ Validation passed, setting isSubmitting = true")
                isSubmitting = true

                val finalKgid = if (kgid.isBlank()) "TEMP-${System.currentTimeMillis()}" else kgid
                android.util.Log.d("CommonEmployeeForm", "📝 Final KGID: $finalKgid")

                val emp = Employee(
                    kgid = finalKgid,
                    name = name.trim(),
                    email = email.trim(),
                    mobile1 = mobile1.trim(),
                    mobile2 = mobile2.trim().takeIf { it.isNotBlank() },
                    rank = rank.trim(),
                    district = district.trim(),
                    station = station.trim(),
                    bloodGroup = bloodGroup.ifBlank { null },
                    metalNumber = metalNumber.trim().takeIf { it.isNotBlank() },
                    isAdmin = initialEmployee?.isAdmin ?: false,
                    photoUrl = croppedPhotoUri?.toString() ?: currentPhotoUrl
                )

                // ✅ Submit in coroutine scope
                android.util.Log.d("CommonEmployeeForm", "🚀 Launching coroutine for submission...")
                coroutineScope.launch {
                    try {
                        android.util.Log.d("CommonEmployeeForm", "📋 Inside coroutine, isRegistration: $isRegistration")
                        if (isRegistration) {
                            // Build PendingRegistrationEntity and call callback
                            val firebaseUid = "" // wrapper will add actual uid
                            val pending = PendingRegistrationEntity(
                                name = emp.name,
                                kgid = emp.kgid,
                                email = emp.email,
                                mobile1 = emp.mobile1 ?: "",
                                mobile2 = emp.mobile2,
                                pin = pin,
                                rank = emp.rank ?: "",
                                metalNumber = emp.metalNumber,
                                district = emp.district.orEmpty(),
                                station = emp.station.orEmpty(),
                                bloodGroup = emp.bloodGroup.orEmpty(),
                                firebaseUid = firebaseUid,
                                photoUrl = emp.photoUrl
                            )
                            onRegisterSubmit?.invoke(pending, croppedPhotoUri)
                        } else {
                            android.util.Log.d("CommonEmployeeForm", "═══════════════════════════════════════")
                            android.util.Log.d("CommonEmployeeForm", "📤 Calling onSubmit with photo: ${croppedPhotoUri != null}")
                            android.util.Log.d("CommonEmployeeForm", "📤 Employee KGID: ${emp.kgid}")
                            onSubmit(emp, croppedPhotoUri)
                            android.util.Log.d("CommonEmployeeForm", "✅ onSubmit call completed")
                        }
                        // Reset after 3 seconds (allowing time for operation)
                        delay(3000)
                        isSubmitting = false
                    } catch (e: Exception) {
                        android.util.Log.e("CommonEmployeeForm", "❌ Submission error: ${e.message}", e)
                        e.printStackTrace()
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting && !isLoading, // ✅ Disable button during submission
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting || isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Submitting...")
                }
            } else {
                Text(
                    when {
                        isRegistration -> "Submit for approval"
                        initialEmployee != null -> "Submit update for approval"
                        else -> "Submit for approval"
                    }
                )
            }
        }

        if (isSubmitting || isLoading) {
            val statusTitle = when {
                isLoading -> "Uploading details to server..."
                else -> "Preparing submission..."
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload in progress",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(sectionSpacing))

        // Image chooser
        if (showSourceDialog) {
            AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Select Image Source") }, text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showSourceDialog = false
                        cameraLauncher.launch(null)
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(Modifier.width(12.dp))
                        Text("Camera")
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(Modifier.width(12.dp))
                        Text("Gallery")
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Close") }
            })
        }
    }
}

/* util functions */
private fun launchUCrop(context: Context, sourceUri: Uri, launcher: ActivityResultLauncher<Intent>) {
    try {
        val destFile = File(context.cacheDir, "ucrop_${UUID.randomUUID()}.jpg")
        val destUri = try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
        } catch (e: Exception) {
            android.util.Log.e("CommonEmployeeForm", "FileProvider error: ${e.message}", e)
            Toast.makeText(context, "Failed to access file provider", Toast.LENGTH_SHORT).show()
            return
        }

        val options = UCrop.Options().apply {
            setToolbarTitle("Crop Image")
            setCircleDimmedLayer(true)
            setFreeStyleCropEnabled(false)
            setCompressionQuality(90)
        }

        val intent = UCrop.of(sourceUri, destUri).withAspectRatio(1f, 1f).withOptions(options).getIntent(context)
        launcher.launch(intent)
    } catch (e: Exception) {
        android.util.Log.e("CommonEmployeeForm", "UCrop launch error: ${e.message}", e)
        Toast.makeText(context, "Failed to launch image cropper: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("CommonEmployeeForm", "FileProvider error: ${e.message}", e)
            Toast.makeText(context, "Failed to access file provider", Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: IOException) {
        android.util.Log.e("CommonEmployeeForm", "Failed to save bitmap: ${e.message}", e)
        Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        null
    } catch (e: Exception) {
        android.util.Log.e("CommonEmployeeForm", "Unexpected error: ${e.message}", e)
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        null
    }
}
