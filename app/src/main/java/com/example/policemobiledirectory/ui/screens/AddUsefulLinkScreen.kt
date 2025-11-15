package com.example.policemobiledirectory.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.helper.FirebaseStorageHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
import androidx.navigation.NavController
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUsefulLinkScreen(
    navController: androidx.navigation.NavController,
    viewModel: com.example.policemobiledirectory.viewmodel.EmployeeViewModel
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(TextFieldValue("")) }
    var playStoreUrl by remember { mutableStateOf(TextFieldValue("")) }
    var apkUrl by remember { mutableStateOf(TextFieldValue("")) }
    var iconUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { imageUri = it } }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let { imageUri = getImageUri(context, it) }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun extractPlayStoreIcon(playStoreUrl: String): String? {
        return try {
            val idPart = playStoreUrl.substringAfter("id=").substringBefore("&")
            "https://play-lh.googleusercontent.com/a-/AD5-WCn$idPart=w480-h960"
        } catch (e: Exception) {
            null
        }
    }

    fun isPlayStoreLink(link: String): Boolean =
        link.contains("play.google.com/store/apps/details")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Useful Link", fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("App / Website Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = playStoreUrl,
            onValueChange = {
                playStoreUrl = it
                if (isPlayStoreLink(it.text)) {
                    extractPlayStoreIcon(it.text)?.let { icon ->
                        iconUrl = icon
                        Toast.makeText(context, "Play Store logo fetched automatically!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            label = { Text("Play Store or Website URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = apkUrl,
            onValueChange = { apkUrl = it },
            label = { Text("Firebase APK URL (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Logo preview
        when {
            imageUri != null -> Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = "Selected Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            iconUrl.isNotEmpty() -> Image(
                painter = rememberAsyncImagePainter(iconUrl),
                contentDescription = "Fetched Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Gallery")
            }

            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Camera")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.text.isEmpty() || playStoreUrl.text.isEmpty()) {
                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                scope.launch {
                    try {
                        isLoading = true
                        var finalIconUrl = iconUrl

                        if (finalIconUrl.isEmpty() && imageUri != null) {
                            finalIconUrl = FirebaseStorageHelper.uploadPhoto(imageUri!!)
                        }

                        val data = hashMapOf(
                            "name" to name.text.trim(),
                            "playStoreUrl" to playStoreUrl.text.trim(),
                            "apkUrl" to apkUrl.text.trim(),
                            "iconUrl" to finalIconUrl
                        )

                        firestore.collection("useful_links").add(data)
                            .addOnSuccessListener {
                                Toast.makeText(context, "✅ Link added successfully!", Toast.LENGTH_SHORT).show()
                                name = TextFieldValue("")
                                playStoreUrl = TextFieldValue("")
                                apkUrl = TextFieldValue("")
                                iconUrl = ""
                                imageUri = null
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                            .addOnCompleteListener { isLoading = false }

                    } catch (e: Exception) {
                        isLoading = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Save Link")
            }
        }
    }
}

private fun getImageUri(context: Context, bitmap: Bitmap): Uri {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(
        context.contentResolver,
        bitmap,
        UUID.randomUUID().toString(),
        null
    )
    return Uri.parse(path)
}
