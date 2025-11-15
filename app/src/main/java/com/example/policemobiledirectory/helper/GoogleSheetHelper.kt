package com.example.policemobiledirectory.helper

import com.example.policemobiledirectory.model.Employee // Ensure this is the correct Employee model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GoogleSheetHelper {
    // Updated SCRIPT_URL to the newest one provided
    private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzp4BKmuOsupQ5fj2pc2K7BBaLRNsdg6D_8H5FQpi2bokUU4wpRo53Fq0IuIg1LLK6v/exec"

    suspend fun postEmployeeData(employee: Employee): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("kgid", employee.kgid)
                    put("name", employee.name)
                    put("mobile", employee.mobile1) // Changed from employee.mobile
                    // IMPORTANT: The app expects employee.rank to be the Job Title
                    // and employee.station to be the Work Location.
                    // The Apps Script must provide JSON mapping these correctly based on the sheet order.
                    put("Rank", employee.rank)
                    put("station", employee.station) 
                    put("photoUrl", employee.photoUrl ?: "") 
                }

                val url = URL(SCRIPT_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.bufferedWriter().use {
                    it.write(json.toString())
                    it.flush()
                }

                val responseCode = conn.responseCode
                conn.disconnect()

                responseCode == 200
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
