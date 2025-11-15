package com.example.policemobiledirectory.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DrivePublicHelper {

    // Replace with your Drive Folder ID
    private const val FOLDER_ID = "13qNrVmJQeFgcC_Q90yyD3fhTZq8j0GXK"

    suspend fun fetchFiles(): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val apiUrl = "https://www.googleapis.com/drive/v3/files?q='$FOLDER_ID'+in+parents&key=YOUR_API_KEY&fields=files(id,name,mimeType,webViewLink,modifiedTime)"

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val files = mutableListOf<Map<String, String>>()

        val json = JSONObject(response)
        val jsonFiles = json.getJSONArray("files")
        for (i in 0 until jsonFiles.length()) {
            val file = jsonFiles.getJSONObject(i)
            files.add(
                mapOf(
                    "id" to file.getString("id"),
                    "name" to file.getString("name"),
                    "mimeType" to file.getString("mimeType"),
                    "webViewLink" to file.getString("webViewLink"),
                    "modifiedTime" to file.optString("modifiedTime")
                )
            )
        }
        files
    }
}
