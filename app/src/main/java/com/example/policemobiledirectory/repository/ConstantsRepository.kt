package com.example.policemobiledirectory.repository

import android.content.Context
import android.util.Log
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.api.RemoteConstantsResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ConstantsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)

    // ✅ Google Apps Script Web App URL (READ-ONLY)
    private val api = Retrofit.Builder()
        .baseUrl("https://script.google.com/macros/s/AKfycbzIW69Yz1BzjVbKD83SpOHqy7KecIG9WQP2DqLrsYJOfPWcVCDEIpNQoia997fV_Jzeng/") // ✅ Your new live script URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ConstantsApiService::class.java)


    // ✅ Fetch remote constants and cache locally
    suspend fun refreshConstants(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.getRemoteConstants()
            val json = Gson().toJson(response)
            prefs.edit().putString("remote_constants", json).apply()
            Log.d("ConstantsRepository", "✅ Constants refreshed from Google Sheet.")
            true
        } catch (e: Exception) {
            Log.e("ConstantsRepository", "❌ Failed to fetch constants: ${e.message}")
            false
        }
    }

    // 🟡 (Removed uploadConstantsToGoogleSheetOnce — no longer needed)

    // ✅ Fetch Ranks (with fallback to local)
    fun getRanks(): List<String> {
        val json = prefs.getString("remote_constants", null)
        if (json.isNullOrEmpty()) return Constants.allRanksList

        return try {
            val data = Gson().fromJson(json, RemoteConstantsResponse::class.java)
            data.ranks ?: Constants.allRanksList
        } catch (e: Exception) {
            e.printStackTrace()
            Constants.allRanksList
        }
    }

    // ✅ Fetch Stations (with fallback)
    fun getStationsByDistrict(): Map<String, List<String>> {
        val json = prefs.getString("remote_constants", null)
        if (json.isNullOrEmpty()) return Constants.stationsByDistrictMap

        return try {
            val data = Gson().fromJson(json, RemoteConstantsResponse::class.java)
            data.stationsByDistrict ?: Constants.stationsByDistrictMap
        } catch (e: Exception) {
            e.printStackTrace()
            Constants.stationsByDistrictMap
        }
    }
}
