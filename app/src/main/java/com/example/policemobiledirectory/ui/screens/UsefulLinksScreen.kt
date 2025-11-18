package com.example.policemobiledirectory.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.policemobiledirectory.R
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsefulLinksScreen(
    navController: NavController,
    viewModel: EmployeeViewModel
) {
    val usefulLinks by viewModel.usefulLinks.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current

    // Fetch links when screen loads
    LaunchedEffect(Unit) { viewModel.fetchUsefulLinks() }

    Scaffold(
        topBar = {
            CommonTopAppBar(title = "Useful Links", navController = navController)
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { navController.navigate("add_useful_link") }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Link")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (usefulLinks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No links available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 90.dp),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(usefulLinks) { link ->
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .clickable {
                                    handleLinkClick(context, link.playStoreUrl, link.apkUrl, link.name)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val iconModel = link.iconUrl?.takeIf { it.isNotBlank() }
                                ?: link.playStoreUrl?.takeIf { it.isNotBlank() }?.let {
                                    "https://www.google.com/s2/favicons?sz=128&domain_url=$it"
                                }
                                ?: "https://cdn-icons-png.flaticon.com/512/732/732225.png"

                            val painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(context)
                                    .data(iconModel)
                                    .crossfade(true)
                                    .build(),
                                placeholder = painterResource(id = R.drawable.app_logo),
                                error = painterResource(id = R.drawable.app_logo)
                            )

                            Image(
                                painter = painter,
                                contentDescription = link.name,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = link.name,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                        }
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------
   ✅ Utility functions (unchanged)
---------------------------------------------- */

fun handleLinkClick(context: Context, playStoreUrl: String?, apkUrl: String?, appName: String) {
    try {
        when {
            !playStoreUrl.isNullOrEmpty() && playStoreUrl.contains("id=") -> {
                openAppOrStore(context, playStoreUrl)
            }
            !apkUrl.isNullOrEmpty() && apkUrl.contains("firebasestorage.googleapis.com") -> {
                downloadAndInstallApk(context, apkUrl, appName)
            }
            !playStoreUrl.isNullOrEmpty() -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                context.startActivity(intent)
            }
            else -> Toast.makeText(context, "No valid link found.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun openAppOrStore(context: Context, playStoreUrl: String) {
    val packageName = getPackageNameFromPlayUrl(playStoreUrl)
    if (packageName != null) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) context.startActivity(launchIntent)
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
    } else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl)))
}

fun downloadAndInstallApk(context: Context, apkUrl: String, appName: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(apkUrl), "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
    }
}

fun getPackageNameFromPlayUrl(url: String): String? =
    try { Uri.parse(url).getQueryParameter("id") } catch (_: Exception) { null }
