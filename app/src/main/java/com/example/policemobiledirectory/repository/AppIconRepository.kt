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
import java.util.concurrent.TimeUnit

/**
 * Repository to fetch and cache Play Store app icons.
 * Tries to scrape the high-quality icon from the Play Store page first.
 */
class AppIconRepository(private val dao: AppIconDao) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getOrFetchAppIcon(playStoreUrl: String): String? = withContext(Dispatchers.IO) {
        val pkg = extractPackageName(playStoreUrl)
        if (pkg.isBlank()) return@withContext null

        val existing = dao.getIcon(pkg)

        // Return cached icon if valid.
        // We treat "google.com/s2/favicons" urls as placeholders and always try to refresh them
        // to see if we can get a better scraped icon.
        if (existing != null &&
            System.currentTimeMillis() - existing.lastUpdated < 7L * 24 * 60 * 60 * 1000
        ) {
            val cachedUrl = existing.iconUrl
            if (!cachedUrl.isNullOrBlank() && !cachedUrl.contains("google.com/s2/favicons")) {
                return@withContext cachedUrl
            } else {
                Log.d("AppIconRepo", "Attempting to upgrade placeholder icon for $pkg")
            }
        }

        // 1. Try scraping first (Better Quality)
        val fetched = scrapePlayStoreIcon(pkg)

        if (fetched != null) {
            dao.insertIcon(AppIconEntity(pkg, fetched, System.currentTimeMillis()))
            Log.d("AppIconRepo", "Successfully scraped icon for $pkg")
            return@withContext fetched
        }

        // 2. If scraping failed, fall back to existing cache if available
        if (existing != null) {
             // Even if it's a favicon, better than nothing if scraping failed
             return@withContext existing.iconUrl
        }

        // 3. If no cache, generate a favicon URL as a last resort so we have something to show
        val faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain_url=https://play.google.com/store/apps/details?id=$pkg"
        
        Log.w("AppIconRepo", "Scraping failed for $pkg, returning favicon fallback")
        return@withContext faviconUrl
    }

    private fun extractPackageName(url: String): String {
        return try {
            val regex = Regex("id=([a-zA-Z0-9._]+)")
            regex.find(url)?.groups?.get(1)?.value ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun scrapePlayStoreIcon(packageName: String): String? {
        return try {
            // Use Desktop User-Agent to potentially get cleaner HTML
            val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                )
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null

                // Helper to find content with flexible quoting and ordering
                fun findMetaContent(property: String, value: String): String? {
                    // Matches property="value" ... content="url" (allow single or double quotes)
                    val p1 = "$property=['\"]$value['\"][^>]*content=['\"]([^'\"]+)['\"]"
                    // Matches content="url" ... property="value"
                    val p2 = "content=['\"]([^'\"]+)['\"][^>]*$property=['\"]$value['\"]"

                    Regex(p1, RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.let { return it }
                    Regex(p2, RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.let { return it }
                    return null
                }

                // 1. Open Graph
                findMetaContent("property", "og:image")?.let { return normalizeIconUrl(it) }
                // 2. Twitter Card
                findMetaContent("name", "twitter:image")?.let { return normalizeIconUrl(it) }
                // 3. Itemprop
                findMetaContent("itemprop", "image")?.let { return normalizeIconUrl(it) }

                // 4. Fallback: brute force search for Google User Content images (likely the icon)
                // Typically play-lh.googleusercontent.com
                val imgRegex = Regex("src=['\"](https://play-lh\\.googleusercontent\\.com/[^'\"]+)['\"]", RegexOption.IGNORE_CASE)
                val matches = imgRegex.findAll(html)
                for (match in matches) {
                    val src = match.groupValues[1]
                    // Simple heuristic: return the first valid image url found
                    return normalizeIconUrl(src) 
                }
                
                null
            }
        } catch (e: Exception) {
            Log.w("AppIconRepo", "Scraping failed for $packageName: ${e.message}")
            null
        }
    }

    private fun normalizeIconUrl(raw: String): String {
        var url = raw
        if (url.startsWith("//")) url = "https:$url"
        url = url.replace("&amp;", "&")
        
        // Force high resolution
        if (url.contains("=s")) {
            url = url.replace(Regex("=s\\d+.*"), "=s256") 
        } else if (!url.contains("=")) {
            url += "=s256"
        }
        return url
    }

    companion object {
        fun create(context: Context): AppIconRepository {
            val db = AppDatabase.getInstance(context)
            return AppIconRepository(db.appIconDao())
        }
    }
}
