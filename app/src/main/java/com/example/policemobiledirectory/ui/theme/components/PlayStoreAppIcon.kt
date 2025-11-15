package com.example.policemobiledirectory.ui.theme.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.policemobiledirectory.data.local.AppIconEntity
import com.example.policemobiledirectory.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 🔹 Play Store App Icon Loader + Offline Cache
 * Fetches icon from Play Store once, caches it in Room (AppIconEntity),
 * and loads via Coil with offline support.
 */
@Composable
fun PlayStoreAppIcon(
    playStoreUrl: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playStoreUrl) {
        val db = AppDatabase.getInstance(context)
        val dao = db.appIconDao()
        val repo = AppIconRepository(dao)
        iconUrl = repo.getOrFetchAppIcon(playStoreUrl)
        isLoading = false
    }

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            iconUrl != null -> Image(
                painter = rememberAsyncImagePainter(iconUrl),
                contentDescription = "$appName Icon",
                modifier = Modifier.fillMaxSize()
            )
            else -> Text("❌ No Icon", modifier = Modifier.align(Alignment.Center))
        }
    }
}

/**
 * 🔹 Repository: Handles local cache + network fetch
 */
class AppIconRepository(private val dao: com.example.policemobiledirectory.data.local.AppIconDao) {

    private val client = OkHttpClient()

    suspend fun getOrFetchAppIcon(playStoreUrl: String): String? = withContext(Dispatchers.IO) {
        val pkg = extractPackageName(playStoreUrl)
        val existing = dao.getIcon(pkg)

        if (existing != null &&
            System.currentTimeMillis() - existing.lastUpdated < 7 * 24 * 60 * 60 * 1000
        ) {
            Log.d("AppIconRepo", "✅ Loaded cached icon for $pkg")
            return@withContext existing.iconUrl
        }

        val fetched = fetchPlayStoreIcon(playStoreUrl)
        if (fetched != null) {
            dao.insertIcon(AppIconEntity(pkg, fetched, System.currentTimeMillis()))
            Log.d("AppIconRepo", "🆕 Saved icon for $pkg: $fetched")
        } else {
            Log.e("AppIconRepo", "⚠️ Failed to fetch icon for $pkg")
        }

        fetched ?: existing?.iconUrl
    }

    private fun extractPackageName(url: String): String {
        val regex = Regex("id=([a-zA-Z0-9._]+)")
        return regex.find(url)?.groups?.get(1)?.value ?: url
    }

    private fun fetchPlayStoreIcon(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string()

            Log.d("PlayStoreFetch", "Fetched HTML length: ${html?.length ?: 0}")

            val regex = Regex("<meta[^>]*property=\"og:image\"[^>]*content=\"([^\"]+)\"")
            val match = regex.find(html ?: "")
            val iconUrl = match?.groups?.get(1)?.value

            Log.d("PlayStoreFetch", "Extracted Logo URL: $iconUrl")
            iconUrl
        } catch (e: Exception) {
            Log.e("PlayStoreFetch", "Error fetching icon: ${e.message}")
            null
        }
    }
}
