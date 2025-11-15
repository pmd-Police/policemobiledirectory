package com.example.policemobiledirectory.repository

import android.content.Context
import android.util.Log
import com.example.policemobiledirectory.data.local.AppDatabase
import com.example.policemobiledirectory.data.local.AppIconDao
import com.example.policemobiledirectory.data.local.AppIconEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Repository to fetch and cache Play Store app icons.
 */
class AppIconRepository(private val dao: AppIconDao) {

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

    companion object {
        fun create(context: Context): AppIconRepository {
            val db = AppDatabase.getInstance(context)
            return AppIconRepository(db.appIconDao())
        }
    }
}
